param([string]$JavaHome='D:\java\jdk\jdk-21.0.11',[string]$Sdk='D:\tax_project\Android SDK')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME=$JavaHome
$env:ANDROID_HOME=$Sdk
$env:GRADLE_USER_HOME=Join-Path $projectRoot '.gradle-home'
Set-Location -LiteralPath $projectRoot
$installedGradle=Get-ChildItem -Path (Join-Path $env:USERPROFILE '.gradle\wrapper\dists\gradle-8.7-all') -Filter gradle.bat -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
if($installedGradle){ & $installedGradle.FullName --no-daemon testDebugUnitTest lintDebug assembleDebug assembleRelease }
else { & .\gradlew.bat --no-daemon testDebugUnitTest lintDebug assembleDebug assembleRelease }
if($LASTEXITCODE -ne 0){throw '构建失败，请检查日志'}
$releaseVersion=(Select-String -LiteralPath 'app\build.gradle' -Pattern "versionName '([^']+)'").Matches.Groups[1].Value
$versionOutput=Join-Path $projectRoot "output\v$releaseVersion"
New-Item -ItemType Directory -Path $versionOutput -Force | Out-Null
Copy-Item -LiteralPath 'app\build\outputs\apk\release\app-release.apk' -Destination (Join-Path $versionOutput "tax-learning-$releaseVersion-release.apk")
Copy-Item -LiteralPath 'app\build\outputs\apk\debug\app-debug.apk' -Destination (Join-Path $versionOutput "tax-learning-$releaseVersion-debug.apk")
