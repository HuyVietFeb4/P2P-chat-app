# Run Android instrumented (integration) tests for all backend modules.
# Requires: one emulator/device connected, Java 17, Android SDK.
# Run from repo root: .\meshenger\scripts\run-integration-tests.ps1

$ErrorActionPreference = "Stop"
$androidDir = Join-Path $PSScriptRoot "..\frontend\android" | Resolve-Path

# Use a single device when several are connected (avoids shard/install issues).
if (-not $env:ANDROID_SERIAL) {
    $devices = (& adb devices) | Where-Object { $_ -match "^\S+\s+device$" } | ForEach-Object { ($_ -split "\s+")[0] }
    if ($devices.Count -ge 1) {
        $env:ANDROID_SERIAL = $devices[0]
        Write-Host "Using device: $env:ANDROID_SERIAL"
    } else {
        Write-Error "No Android device/emulator found. Start one or set ANDROID_SERIAL."
    }
}

Push-Location $androidDir
try {
    $modules = @(
        ":backend:security_native:connectedDebugAndroidTest",
        ":backend:transport2:connectedDebugAndroidTest",
        ":backend:network:connectedDebugAndroidTest",
        ":backend:session:connectedDebugAndroidTest",
        ":backend:application:connectedDebugAndroidTest"
    )
    & .\gradlew.bat @modules -Pandroid.testInstrumentationRunnerArguments.clearPackageData=true
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
finally {
    Pop-Location
}
