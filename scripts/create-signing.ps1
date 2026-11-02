param([string]$JavaHome='D:\java\jdk\jdk-21.0.11')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$keyDirectory=Join-Path $projectRoot 'signing'
New-Item -ItemType Directory -Path $keyDirectory -Force | Out-Null
$keyFile=Join-Path $keyDirectory 'tax-learning-release.p12'
if(Test-Path -LiteralPath $keyFile){ throw '签名已存在。为保留升级能力，不能覆盖。' }
$randomBytes=New-Object byte[] 32
$rng=[Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($randomBytes); $rng.Dispose()
$password=[Convert]::ToBase64String($randomBytes)
$env:TAX_SIGNING_PASSWORD=$password
try {
    & "$JavaHome\bin\keytool.exe" -genkeypair -keystore $keyFile -storetype PKCS12 -storepass:env TAX_SIGNING_PASSWORD -keypass:env TAX_SIGNING_PASSWORD -alias tax-learning -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Tax Learning Course, OU=Student Project, O=Independent Learning, C=CN'
    if($LASTEXITCODE -ne 0){throw '签名密钥生成失败'}
    # Local secret file, excluded from Git/source packages and never printed.
    [IO.File]::WriteAllText((Join-Path $keyDirectory 'release.properties'),"storePassword=$password`nkeyPassword=$password`n",[Text.UTF8Encoding]::new($false))
    & icacls.exe $keyDirectory /inheritance:r /grant:r "${env:USERNAME}:(OI)(CI)F" 'SYSTEM:(OI)(CI)F' | Out-Null
    Write-Output '独立 Release 密钥已生成；密码只保存在 signing/release.properties。'
} finally { Remove-Item Env:TAX_SIGNING_PASSWORD -ErrorAction SilentlyContinue; $password=$null }
