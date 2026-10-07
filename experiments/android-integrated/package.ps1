param(
    [Parameter(Mandatory=$true)][string]$Dex,
    [Parameter(Mandatory=$true)][string]$Output,
    [string]$JdkRoot = 'C:\Program Files\Android\Android Studio\jbr',
    [string]$SdkRoot = (Join-Path $env:LOCALAPPDATA 'Android\Sdk')
)
$ErrorActionPreference = 'Stop'
$pikoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$pikoBuild = Join-Path $pikoRoot ('.local\integrated-build\'+[guid]::NewGuid().ToString('N'))
$pikoJava = Join-Path $JdkRoot 'bin\java.exe'
$pikoTools = Join-Path $SdkRoot 'build-tools\36.0.0'
$pikoApktool = Join-Path $pikoRoot '.local\restart-check\apktool.jar'
$pikoInput = Join-Path $pikoRoot '.local\downloads\pico8-frontend.apk'
$pikoRun = Join-Path $pikoRoot '.local\downloads\run_pico_cmd-1.6.6.gd'
$pikoBoot = Join-Path $pikoRoot '.local\downloads\boot-1.6.6.gd'
function Check([string]$Path,[string]$Hash) { if((Get-FileHash -LiteralPath $Path).Hash -ne $Hash){throw "Wrong pinned input: $Path"} }
function Run([string]$Program,[string[]]$Arguments) { & $Program @Arguments; if($LASTEXITCODE -ne 0){throw "$Program failed ($LASTEXITCODE)"} }
Check $pikoInput '916330f7f4a8349c3630d207be73b402f3161a2717a8d1884228ca2c0f94da2b'
Check $pikoApktool 'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423'
Check $pikoBoot '3841e28ef49e53e88b09d55110ff4587f39a8e1c9f71fcebffad4982ab68da58'
New-Item -ItemType Directory -Force $pikoBuild | Out-Null
$pikoDecoded = Join-Path $pikoBuild 'app'
Run $pikoJava @('-jar',$pikoApktool,'d',$pikoInput,'-s','-o',$pikoDecoded)
$pikoRuntime = Join-Path $pikoRoot 'experiments\runtime-restart'
$pikoAssets = Join-Path $pikoDecoded 'assets'
Copy-Item (Join-Path $pikoAssets 'package.dat') (Join-Path $pikoBuild 'original.dat')
Run $pikoJava @((Join-Path $pikoRuntime 'PatchBootstrap.java'),(Join-Path $pikoBuild 'original.dat'),(Join-Path $pikoAssets 'package.dat'),(Join-Path $pikoRuntime 'audio-session.sh'))
& (Join-Path $pikoRuntime 'PatchLaunch.ps1') -Source $pikoRun -Assets $pikoAssets
Run py @('-3',(Join-Path $PSScriptRoot 'prepare.py'),$pikoDecoded,$pikoRoot,$pikoBoot)
Copy-Item -LiteralPath $Dex -Destination (Join-Path $pikoDecoded 'classes2.dex')
$pikoUnsigned = Join-Path $pikoBuild 'unsigned.apk'
$pikoAligned = Join-Path $pikoBuild 'aligned.apk'
Run $pikoJava @('-jar',$pikoApktool,'b',$pikoDecoded,'-o',$pikoUnsigned)
Run (Join-Path $pikoTools 'zipalign.exe') @('-f','-P','16','4',$pikoUnsigned,$pikoAligned)
Run $pikoJava @('-jar',(Join-Path $pikoTools 'lib\apksigner.jar'),'sign','--ks',(Join-Path $pikoRoot '.local\artifacts\runtime-lab-debug.keystore'),'--ks-key-alias','pikoos-lab','--ks-pass','pass:android','--key-pass','pass:android','--out',$Output,$pikoAligned)
Run $pikoJava @('-jar',(Join-Path $pikoTools 'lib\apksigner.jar'),'verify',$Output)
Run py @('-3',(Join-Path $PSScriptRoot 'verify.py'),$Output)
Get-FileHash -LiteralPath $Output
