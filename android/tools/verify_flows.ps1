# Drives the app on the emulator and captures one screenshot per surface.
#
# Written as a flat script with no nested function calls on the hot path: nesting functions and
# returning values through the pipeline made tap results silently disappear, which looked like
# "every screenshot is identical". Each adb call goes directly to the executable with an explicit
# -s serial, because a physical phone may also be attached to this machine.
#
# Tap targets are fixed coordinates for the emulator's 1080x2400 pixel_6 profile, and every step is
# verified by re-reading the live UI tree afterwards (see the STATE log lines).
#
# Usage:  tools\verify_flows.ps1 [-Serial emulator-5556] [-OutDir path]
param(
    [string]$Serial = 'emulator-5556',
    [string]$OutDir = ''
)

$ErrorActionPreference = 'Continue'
$ProgressPreference = 'SilentlyContinue'

$Root = Split-Path -Parent $PSScriptRoot
if (-not $OutDir) { $OutDir = Join-Path $Root 'tools\out\flows' }
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$env:ANDROID_HOME = 'D:\Android\sdk'
$env:ANDROID_SDK_ROOT = 'D:\Android\sdk'
$env:PATH = "D:\Android\sdk\platform-tools;$env:PATH"

$adb = 'D:\Android\sdk\platform-tools\adb.exe'
$pkg = 'com.jordan.wailaixifu.debug'
$act = "$pkg/com.jordan.wailaixifu.MainActivity"
$dumpLocal = Join-Path $env:TEMP 'dsh-verify-ui.xml'

function Log($message) { Write-Host $message }

function DumpUi {
    Remove-Item $dumpLocal -Force -ErrorAction SilentlyContinue
    & $adb -s $Serial shell rm -f /sdcard/ui.xml 2>&1 | Out-Null
    & $adb -s $Serial shell uiautomator dump /sdcard/ui.xml 2>&1 | Out-Null
    & $adb -s $Serial pull /sdcard/ui.xml $dumpLocal 2>&1 | Out-Null
    if (-not (Test-Path $dumpLocal)) { return '' }
    return [System.IO.File]::ReadAllText($dumpLocal, [System.Text.Encoding]::UTF8)
}

function NodeCenter([string]$xml, [string]$needle, [int]$MinY = 400) {
    # Compose reports every semantic node as clickable="false"; it does its own hit testing. So the
    # tap target is the text label's own rectangle. Substring matching alone is unsafe because the
    # cover subtitle repeats the mode names ("... 主线剧情 · 34位角色"), hence the MinY guard, which
    # skips the cover: everything below the cover strip is body content.
    foreach ($m in [regex]::Matches($xml, '<node[^>]*>')) {
        $tag = $m.Value
        $text = [regex]::Match($tag, 'text="([^"]*)"').Groups[1].Value
        if (-not $text -or ($text -notlike "*$needle*")) { continue }
        $bounds = [regex]::Match($tag, 'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
        if (-not $bounds.Success) { continue }
        $t = [int]$bounds.Groups[2].Value
        $b = [int]$bounds.Groups[4].Value
        if ($t -lt $MinY) { continue }
        $x = [int](([int]$bounds.Groups[1].Value + [int]$bounds.Groups[3].Value) / 2)
        $y = [int](($t + $b) / 2)
        return "$x $y"
    }
    return ''
}

function NodeByClass([string]$xml, [string]$className, [int]$MinY = 400) {
    # The search field carries empty text in the semantics tree: a BasicTextField's placeholder is a
    # sibling node, so the only reliable handle on it is its class.
    foreach ($m in [regex]::Matches($xml, '<node[^>]*>')) {
        $tag = $m.Value
        if ([regex]::Match($tag, 'class="([^"]*)"').Groups[1].Value -ne $className) { continue }
        $bounds = [regex]::Match($tag, 'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
        if (-not $bounds.Success) { continue }
        $t = [int]$bounds.Groups[2].Value
        $b = [int]$bounds.Groups[4].Value
        if ($t -lt $MinY) { continue }
        $x = [int](([int]$bounds.Groups[1].Value + [int]$bounds.Groups[3].Value) / 2)
        $y = [int](($t + $b) / 2)
        return "$x $y"
    }
    return ''
}

function Shot([string]$name) {
    $remote = "/sdcard/$name.png"
    & $adb -s $Serial shell screencap -p $remote 2>&1 | Out-Null
    $target = Join-Path $OutDir "$name.png"
    & $adb -s $Serial pull $remote $target 2>&1 | Out-Null
    & $adb -s $Serial shell rm -f $remote 2>&1 | Out-Null
    if (Test-Path $target) {
        Log ("   shot {0} ({1:N0} B)" -f $name, (Get-Item $target).Length)
    } else {
        Log ("   shot {0} FAILED" -f $name)
    }
}

function TapAt([int]$x, [int]$y, [string]$label) {
    & $adb -s $Serial shell input tap $x $y 2>&1 | Out-Null
    Log ("   tap {0} at ({1},{2})" -f $label, $x, $y)
    Start-Sleep -Milliseconds 1400
}

function AppPid { return ((((& $adb -s $Serial shell pidof $pkg) 2>&1) -join '')).Trim() }

# ---- geometry for 1080x2400 @ 420dpi -------------------------------------------------------
$navY = 2274
$navT = 173
$navM = 540
$navW = 907
$modeY = 700

Log '== preparation =='
& $adb -s $Serial logcat -c 2>&1 | Out-Null
& $adb -s $Serial shell am force-stop $pkg 2>&1 | Out-Null
Start-Sleep -Seconds 1
& $adb -s $Serial shell am start -n $act 2>&1 | Out-Null
Start-Sleep -Seconds 5
Log ("   pid = " + (AppPid))

$xml = DumpUi
Log ("   STATE launch: traditional=" + [bool](NodeCenter $xml '传统') + " modern=" + [bool](NodeCenter $xml '现代') + " wailai=" + [bool](NodeCenter $xml '外来'))
Shot '01-wailai'

Log '== -> 传统 =='
TapAt $navT $navY 'nav 传统'
$xml = DumpUi
Log ("   STATE 传统: modes(4)=" + [bool](NodeCenter $xml '主角') + " seasons=" + [bool](NodeCenter $xml '全部季') + " chars=" + [bool](NodeCenter $xml '角色查询'))
Shot '02-traditional'

Log '== -> 现代 =='
TapAt $navM $navY 'nav 现代'
$xml = DumpUi
Log ("   STATE 现代: seasons=" + [bool](NodeCenter $xml '全部季'))
Shot '03-modern'

Log '== -> 传统, mode 主线剧情 =='
TapAt $navT $navY 'nav 传统'
$xml = DumpUi
$mode = NodeCenter $xml '主线剧情'
Log ("   mode 主线剧情 node = " + $(if ($mode) { $mode } else { 'MISS' }))
if ($mode) {
    $parts = $mode -split ' '
    TapAt ([int]$parts[0]) ([int]$parts[1]) 'mode 主线剧情'
    $xml = DumpUi
    Log ("   STATE storylines: 条主线=" + [bool](NodeCenter $xml '条主线'))
    Shot '04-storylines'
}

Log '== mode 角色查询 =='
$xml = DumpUi
$mode = NodeCenter $xml '角色查询'
if ($mode) {
    $parts = $mode -split ' '
    TapAt ([int]$parts[0]) ([int]$parts[1]) 'mode 角色查询'
    $xml = DumpUi
    Log ("   STATE characters: 房东与当铺=" + [bool](NodeCenter $xml '房东与当铺'))
    Shot '05-characters'
    $ch = NodeCenter $xml '谭宪炳'
    if ($ch) {
        $parts = $ch -split ' '
        TapAt ([int]$parts[0]) ([int]$parts[1]) 'character 谭宪炳'
        Shot '06-character-detail'
    }
}

Log '== mode 相似抽取 =='
$xml = DumpUi
$mode = NodeCenter $xml '相似抽取'
if ($mode) {
    $parts = $mode -split ' '
    TapAt ([int]$parts[0]) ([int]$parts[1]) 'mode 相似抽取'
    $xml = DumpUi
    Log ("   STATE picker: 抽取器=" + [bool](NodeCenter $xml '抽取'))
    Shot '07-picker'
}

Log '== share card =='
$xml = DumpUi
$mode = NodeCenter $xml '剧集查询'
if ($mode) {
    $parts = $mode -split ' '
    TapAt ([int]$parts[0]) ([int]$parts[1]) 'mode 剧集查询'
}
$xml = DumpUi
$share = NodeCenter $xml '分享'
if ($share) {
    $parts = $share -split ' '
    TapAt ([int]$parts[0]) ([int]$parts[1]) 'share'
    Start-Sleep -Seconds 4
    $xml = DumpUi
    Log ("   STATE share modal: 分享卡片=" + [bool](NodeCenter $xml '分享卡片'))
    Shot '08-share-card'
    TapAt 540 150 'dismiss modal (backdrop)'
}

Log '== search: numeric episode lookup =='
$xml = DumpUi
$field = NodeByClass $xml 'android.widget.EditText'
if ($field) {
    $parts = $field -split ' '
    TapAt ([int]$parts[0]) ([int]$parts[1]) 'search field'
    & $adb -s $Serial shell input text 45 2>&1 | Out-Null
    Start-Sleep -Seconds 2
    $xml = DumpUi
    Log ("   STATE search: keyword echo=" + [bool](NodeCenter $xml '关键词'))
    Shot '09-search-45'
    # The IME covers the result list and android 16 will not dismiss it from adb here, so assert on
    # the semantics tree rather than on a screenshot: a numeric lookup must narrow 1552 cards to the
    # single card covering episode 45 (season 1, "炳哥醉酒" -- see tools/out/qiershi.json).
    $target = NodeCenter $xml '炳哥醉酒'
    $unrelated = NodeCenter $xml '停水风波'
    Log ("   ASSERT numeric lookup: 炳哥醉酒 present=" + [bool]$target + ", 停水风波 filtered out=" + (-not [bool]$unrelated))
    & $adb -s $Serial shell input swipe 540 1400 540 700 400 2>&1 | Out-Null
    Start-Sleep -Seconds 2
    Shot '09b-search-45-results'
} else {
    Log '   MISS: search field. Text nodes below y=400:'
    foreach ($m in [regex]::Matches($xml, '<node[^>]*>')) {
        $t = [regex]::Match($m.Value, 'text="([^"]*)"').Groups[1].Value
        $c = [regex]::Match($m.Value, 'class="([^"]*)"').Groups[1].Value
        $b = [regex]::Match($m.Value, 'bounds="\[(\d+),(\d+)\]')
        if (-not $b.Success) { continue }
        if ([int]$b.Groups[2].Value -lt 400) { continue }
        Log ("      [{0}] '{1}' y={2}" -f $c, $t, $b.Groups[2].Value)
    }
}

Log '== dark mode =='
& $adb -s $Serial shell cmd uimode night yes 2>&1 | Out-Null
Start-Sleep -Seconds 3
& $adb -s $Serial shell am start -n $act 2>&1 | Out-Null
Start-Sleep -Seconds 3
Shot '10-dark'
& $adb -s $Serial shell cmd uimode night no 2>&1 | Out-Null
Start-Sleep -Seconds 3

Log '== crash scan =='
$log = & $adb -s $Serial logcat -d -v brief 2>&1
$log | Set-Content -Path (Join-Path $OutDir 'logcat.txt') -Encoding UTF8
if ($log | Select-String -Pattern 'FATAL EXCEPTION') {
    Log '   CRASH DETECTED - see logcat.txt'
} else {
    Log '   no FATAL EXCEPTION'
}
Log ("   final pid = " + $(if ((AppPid) -ne '') { AppPid } else { 'NOT RUNNING' }))
Log 'VERIFY_FLOWS_DONE'
