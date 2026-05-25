<#
Build script for Phantom AntiCheat (PowerShell)

Usage:
  .\build.ps1                # build with tests (default)
  .\build.ps1 -SkipTests     # skip tests

Options:
  -SkipTests    Skip running tests (adds -DskipTests)
#>

param(
    [switch]$SkipTests
)

$base = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $base

$mvnArgs = "clean package -U"
if ($SkipTests) { $mvnArgs += " -DskipTests" }

Write-Host "[build] Running: mvn $mvnArgs"
$proc = Start-Process mvn -ArgumentList $mvnArgs -NoNewWindow -Wait -PassThru
if ($proc.ExitCode -ne 0) {
    Write-Error "Maven build failed with exit code $($proc.ExitCode)"
    exit $proc.ExitCode
}

Write-Host "[build] Success. Artifact(s) in: $base\target\"
