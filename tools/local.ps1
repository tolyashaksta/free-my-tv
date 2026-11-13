param([ValidateSet('Check', 'Build', 'Generic')][string]$Action = 'Check')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
$toolchain = Join-Path $projectRoot '.local/toolchain'
$jdk = Get-ChildItem (Join-Path $toolchain 'jdk') -Directory | Where-Object { Test-Path (Join-Path $_.FullName 'bin/javac.exe') } | Select-Object -First 1
if (!$jdk) { throw 'JDK missing. See docs/FORK_WORKFLOW.md.' }
$env:JAVA_HOME = $jdk.FullName.Replace('\', '/')
$env:ANDROID_HOME = (Join-Path $toolchain 'android-sdk').Replace('\', '/')
$env:PATH = "$($jdk.FullName)\bin;$env:ANDROID_HOME/platform-tools;$env:PATH"
$python = (Get-Command python.exe).Source.Replace('\', '/')
New-Item -ItemType Directory -Force .local/bin | Out-Null
[IO.File]::WriteAllText((Join-Path $projectRoot '.local/bin/python3'), "#!/usr/bin/env bash`nexec `"$python`" `"`$@`"`n", [Text.UTF8Encoding]::new($false))
$env:PATH = "$(Join-Path $projectRoot '.local/bin');$env:PATH"
$bash = Join-Path $env:ProgramFiles 'Git/bin/bash.exe'
function Invoke-Bash([string[]]$Arguments) {
    & $bash @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Command failed: $Arguments" }
}
if ($Action -ne 'Check') {
    if (!(Test-Path freemytv.keystore)) { throw 'Restore your original freemytv.keystore before building an installable APK. See LOCAL_SETUP.md.' }
    if ($Action -eq 'Build' -and !(Test-Path config.properties)) { throw 'Restore config.properties, or use -Action Generic for TV-configured settings.' }
    if ($Action -eq 'Generic') { Invoke-Bash @('build.sh', '--generic') }
    else { Invoke-Bash @('build.sh') }
    exit
}
# Verification uses a separate key and preserves any installable APK already here.
$verifyKey = Join-Path $projectRoot '.local/verification.keystore'
if (!(Test-Path -LiteralPath $verifyKey)) {
    & "$env:JAVA_HOME/bin/keytool.exe" -genkeypair -keystore $verifyKey -alias verification -keyalg RSA -keysize 2048 -validity 30 -storepass verification -keypass verification -dname 'CN=Build verification only'
    if ($LASTEXITCODE -ne 0) { throw 'Could not create verification key' }
}
$previousKey = $env:FREEMYTV_KEYSTORE
$previousPass = $env:FREEMYTV_KEYSTORE_PASS
$backup = Join-Path $projectRoot '.local/apk-before-check.apk'
if (Test-Path -LiteralPath $backup) { throw 'An APK backup already exists; restore it before checking.' }
$hadApk = Test-Path FreeMyTV.apk
if ($hadApk) { Move-Item -LiteralPath FreeMyTV.apk -Destination $backup }
try {
    $env:FREEMYTV_KEYSTORE = $verifyKey.Replace('\', '/')
    $env:FREEMYTV_KEYSTORE_PASS = 'verification'
    Invoke-Bash @('build.sh', '--generic')
    Move-Item -LiteralPath FreeMyTV.apk -Destination .local/verification.apk -Force
    Invoke-Bash @('tools/test-remote.sh')
    Invoke-Bash @('tools/test-order.sh')
    Invoke-Bash @('tools/test-status.sh')
    Invoke-Bash @('tools/test-net.sh')
    Invoke-Bash @('tools/check.sh', 'web')
} finally {
    $env:FREEMYTV_KEYSTORE = $previousKey
    $env:FREEMYTV_KEYSTORE_PASS = $previousPass
    if ($hadApk) { Move-Item -LiteralPath $backup -Destination FreeMyTV.apk -Force }
}
