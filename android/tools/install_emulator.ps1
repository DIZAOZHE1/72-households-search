# Installs the Android Emulator and an x86_64 system image, then creates an AVD.
#
# Chosen image: android-36 google_apis x86_64 - matches the project's compileSdk/targetSdk 36 and
# needs no Play Store account. The google_apis variant is smaller than the playstore one and is
# what a plain `adb install` of a debug APK needs.
$ErrorActionPreference = 'Continue'
$ProgressPreference = 'SilentlyContinue'

$SdkRoot = 'D:\Android\sdk'
$SdkManager = Join-Path $SdkRoot 'cmdline-tools\latest\bin\sdkmanager.bat'
$AvdManager = Join-Path $SdkRoot 'cmdline-tools\latest\bin\avdmanager.bat'
$Image = 'system-images;android-36;google_apis;x86_64'
$AvdName = 'dsh_api36'

if (-not (Test-Path $SdkManager)) { throw "sdkmanager not found at $SdkManager" }

Write-Output '[1/4] Installing emulator + system image (this is the ~2 GB download) ...'
& $SdkManager --sdk_root=$SdkRoot 'emulator' $Image 2>&1 |
    Where-Object { $_ -notmatch 'WARNING: A restricted method|^\s*$' } |
    Select-Object -Last 12

Write-Output '[2/4] Accepting any newly required licenses ...'
$yes = ("y`r`n" * 100)
$yes | & $SdkManager --sdk_root=$SdkRoot --licenses 2>&1 |
    Where-Object { $_ -notmatch 'WARNING: A restricted method' } |
    Select-Object -Last 3

Write-Output '[3/4] Verifying installation ...'
if (-not (Test-Path (Join-Path $SdkRoot 'emulator\emulator.exe'))) {
    Write-Output 'FAILED: emulator.exe missing'
    exit 1
}
if (-not (Test-Path (Join-Path $SdkRoot ($Image -replace ';', '\')))) {
    Write-Output "FAILED: system image directory missing for $Image"
    exit 1
}
Write-Output '      emulator + system image present'

Write-Output '[4/4] Creating AVD ...'
$existing = & $AvdManager list avd 2>&1 | Select-String -Pattern "Name:\s+$AvdName"
if ($existing) {
    Write-Output "      AVD $AvdName already exists"
} else {
    'no' | & $AvdManager create avd --name $AvdName --package $Image --device 'pixel_6' --force 2>&1 |
        Select-Object -Last 5
}

& $AvdManager list avd 2>&1 | Select-String -Pattern 'Name:|Based on:|Path:' | Select-Object -First 6
Write-Output 'EMULATOR_SETUP_DONE'
