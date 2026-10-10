# Builds the Android app and copies the APK to release-dist\.
#
# Nothing machine-specific is hardcoded: JAVA_HOME / ANDROID_HOME / GRADLE_HOME are taken from the
# environment when set, otherwise from the conventional per-user locations. Override by exporting
# the variables before running this script.
#
# The build prefers an installed Gradle distribution over gradlew. The wrapper resolves its
# distribution into $GRADLE_USER_HOME/wrapper/dists/<name>/<hash> and, on a miss, downloads it with
# the 10 s timeout configured in gradle-wrapper.properties -- which fails on a slow link and looks
# exactly like a build failure. gradlew is used as the fallback.
#
# Usage:  tools\build.ps1 [gradle tasks...]        (default: assembleDebug)
$ErrorActionPreference = 'Continue'

$AndroidRoot = Split-Path -Parent $PSScriptRoot
$RepoRoot = Split-Path -Parent $AndroidRoot

function Resolve-Jdk {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) { return $env:JAVA_HOME }
    # Gradle 8.14.3 cannot run its script compiler on JDK 25, so a 17..21 JDK is required.
    foreach ($candidate in @(
            "$env:USERPROFILE\.jdks",
            'C:\Program Files\Microsoft',
            'C:\Program Files\Java',
            'C:\Program Files\Eclipse Adoptium',
            'D:\Java'
        )) {
        if (-not (Test-Path $candidate)) { continue }
        $found = Get-ChildItem $candidate -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match 'jdk-?(17|18|19|20|21)' } |
            Sort-Object Name -Descending | Select-Object -First 1
        if ($found -and (Test-Path (Join-Path $found.FullName 'bin\java.exe'))) { return $found.FullName }
    }
    return $null
}

function Resolve-AndroidSdk {
    if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) { return $env:ANDROID_HOME }
    if ($env:ANDROID_SDK_ROOT -and (Test-Path $env:ANDROID_SDK_ROOT)) { return $env:ANDROID_SDK_ROOT }
    foreach ($candidate in @(
            "$env:LOCALAPPDATA\Android\Sdk",
            "$env:USERPROFILE\AppData\Local\Android\Sdk",
            'C:\Android\Sdk',
            'D:\Android\sdk'
        )) {
        if (Test-Path $candidate) { return $candidate }
    }
    return $null
}

function Resolve-Gradle {
    if ($env:GRADLE_HOME -and (Test-Path (Join-Path $env:GRADLE_HOME 'bin\gradle.bat'))) { return $env:GRADLE_HOME }
    if ($env:GRADLE_USER_HOME) {
        $wrapped = Get-ChildItem (Join-Path $env:GRADLE_USER_HOME 'wrapper\dists') -Directory -ErrorAction SilentlyContinue |
            ForEach-Object { Get-ChildItem $_.FullName -Directory -Filter 'gradle-*' -ErrorAction SilentlyContinue } |
            Select-Object -First 1
        if ($wrapped -and (Test-Path (Join-Path $wrapped.FullName 'bin\gradle.bat'))) { return $wrapped.FullName }
    }
    foreach ($candidate in @('D:\gradle-8.14.3', 'C:\gradle-8.14.3')) {
        if (Test-Path (Join-Path $candidate 'bin\gradle.bat')) { return $candidate }
    }
    return $null
}

$jdk = Resolve-Jdk
if (-not $jdk) { throw 'No JDK 17..21 found. Set JAVA_HOME to a JDK in that range.' }
$env:JAVA_HOME = $jdk

$sdk = Resolve-AndroidSdk
if (-not $sdk) { throw 'No Android SDK found. Set ANDROID_HOME, or write sdk.dir into local.properties.' }
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk

if (-not $env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle' }
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"

Write-Output "JAVA_HOME        = $env:JAVA_HOME"
Write-Output "ANDROID_HOME     = $env:ANDROID_HOME"
Write-Output "GRADLE_USER_HOME = $env:GRADLE_USER_HOME"

$gradleHome = Resolve-Gradle
$gradleCmd = if ($gradleHome) { Join-Path $gradleHome 'bin\gradle.bat' } else { Join-Path $AndroidRoot 'gradlew.bat' }
if (-not (Test-Path $gradleCmd)) { throw 'Neither an installed Gradle nor gradlew.bat is available.' }
Write-Output "gradle           = $gradleCmd"

$log = Join-Path $RepoRoot 'build\build.log'
New-Item -ItemType Directory -Force -Path (Split-Path $log) | Out-Null

$tasks = if ($args.Count -gt 0) { $args } else { @('assembleDebug') }
Write-Output ("== gradle {0} ==" -f ($tasks -join ' '))

# Output goes to a file so a failure can be shown without PowerShell wrapping native stderr into
# ErrorRecords, which would also obscure gradle's own exit status.
$gradleArgs = @('--project-dir', $AndroidRoot, '--console=plain', '--no-daemon') + $tasks
& $gradleCmd @gradleArgs *> $log
$code = $LASTEXITCODE
Write-Output "== gradle exit code: $code =="

if ($code -ne 0) {
    Write-Output '---- last 40 log lines ----'
    Get-Content $log | Select-Object -Last 40
}

if ($code -eq 0) {
    $apk = Join-Path $AndroidRoot 'app\build\outputs\apk\debug\app-debug.apk'
    if (Test-Path $apk) {
        $dist = Join-Path $RepoRoot 'release-dist'
        New-Item -ItemType Directory -Force -Path $dist | Out-Null
        $out = Join-Path $dist 'wailaixifu-debug.apk'
        Copy-Item $apk $out -Force
        Write-Output ("APK -> {0} ({1:N2} MB)" -f $out, ((Get-Item $out).Length / 1MB))
    } else {
        Write-Output 'WARNING: gradle succeeded but no debug APK was found'
        $code = 1
    }
}
exit $code
