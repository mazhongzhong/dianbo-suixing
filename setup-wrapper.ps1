$ErrorActionPreference = 'Stop'
$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$wrapperDir = Join-Path $projectDir 'gradle\wrapper'
$wrapperJar = Join-Path $wrapperDir 'gradle-wrapper.jar'

New-Item -ItemType Directory -Force -Path $wrapperDir | Out-Null
Invoke-WebRequest `
    -Uri 'https://github.com/gradle/gradle/raw/refs/tags/v9.4.1/gradle/wrapper/gradle-wrapper.jar' `
    -OutFile $wrapperJar

if ((Get-Item -LiteralPath $wrapperJar).Length -lt 40000) {
    Remove-Item -LiteralPath $wrapperJar -Force
    throw 'Downloaded Gradle wrapper is unexpectedly small.'
}

Write-Host "Gradle wrapper installed at $wrapperJar"
