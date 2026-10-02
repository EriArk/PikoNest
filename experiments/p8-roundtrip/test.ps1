param(
    [string]$JdkRoot = (Split-Path (Split-Path (Get-Command javac -ErrorAction Stop).Source)),
    [string[]]$Corpus = @()
)
$ErrorActionPreference = 'Stop'
$pikoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$pikoClasses = Join-Path $pikoRoot ('.local\p8-tests\' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $pikoClasses -Force | Out-Null
$pikoSources = @(Get-ChildItem $PSScriptRoot -Filter '*.java' -Recurse | ForEach-Object FullName)
& (Join-Path $JdkRoot 'bin\javac.exe') --release 8 -encoding UTF-8 -d $pikoClasses @pikoSources
if ($LASTEXITCODE -ne 0) { throw 'P8 proof compilation failed' }
$pikoFixtures = @((Join-Path $pikoRoot 'experiments\runtime-smoke\runtime_smoke.p8'),
    (Join-Path $pikoRoot 'experiments\android-host\assets\workshop.p8')) + $Corpus
& (Join-Path $JdkRoot 'bin\java.exe') -cp $pikoClasses P8DocumentTest @pikoFixtures
if ($LASTEXITCODE -ne 0) { throw 'P8 proof tests failed' }
Write-Output "CLI classes: $pikoClasses"
