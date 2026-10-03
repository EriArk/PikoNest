param(
    [string]$UpstreamApk,
    [string]$ApktoolJar,
    [string]$RunCmdSource,
    [string]$JdkRoot = 'C:\Program Files\Android\Android Studio\jbr',
    [string]$SdkRoot = (Join-Path $env:LOCALAPPDATA 'Android\Sdk')
)
$ErrorActionPreference = 'Stop'
$pikoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
if (-not $UpstreamApk) { $UpstreamApk = Join-Path $pikoRoot '.local\downloads\pico8-frontend.apk' }
if (-not $ApktoolJar) { $ApktoolJar = Join-Path $pikoRoot '.local\restart-check\apktool.jar' }
if (-not $RunCmdSource) { $RunCmdSource = Join-Path $pikoRoot '.local\downloads\run_pico_cmd-1.6.6.gd' }
function Check-Hash([string]$Path,[string]$Expected) {
    if ((Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash -ne $Expected) { throw "Unexpected input hash: $Path" }
}
Check-Hash $UpstreamApk '916330f7f4a8349c3630d207be73b402f3161a2717a8d1884228ca2c0f94da2b'
Check-Hash $ApktoolJar 'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423'
Check-Hash $RunCmdSource '71ef40406dc66f8f590542cdbbc37263734141ae337ae9b3d2297ec5f42cd2cc'
$pikoBuild = Join-Path $pikoRoot ('.local\runtime-restart-build\'+[guid]::NewGuid().ToString('N'))
$pikoArtifacts = Join-Path $pikoRoot '.local\artifacts'
$pikoKey = Join-Path $pikoArtifacts 'runtime-lab-debug.keystore'
if (-not (Test-Path -LiteralPath $pikoKey)) { throw 'Build android-host first to create the shared local debug key.' }
New-Item -ItemType Directory -Force $pikoBuild,$pikoArtifacts | Out-Null
$pikoJava = Join-Path $JdkRoot 'bin\java.exe'
function Run([string]$Program,[string[]]$Arguments) {
    & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Program failed ($LASTEXITCODE)" }
}
$pikoDecoded = Join-Path $pikoBuild 'wrapper'
Run $pikoJava @('-jar',$ApktoolJar,'d',$UpstreamApk,'-s','-o',$pikoDecoded)
$pikoBootstrap = Join-Path $pikoDecoded 'assets\package.dat'
$pikoOriginal = Join-Path $pikoBuild 'package-original.dat'
Copy-Item -LiteralPath $pikoBootstrap -Destination $pikoOriginal
Run $pikoJava @((Join-Path $PSScriptRoot 'PatchBootstrap.java'),$pikoOriginal,$pikoBootstrap,(Join-Path $PSScriptRoot 'audio-session.sh'))
& (Join-Path $PSScriptRoot 'PatchLaunch.ps1') -Source $RunCmdSource -Assets (Join-Path $pikoDecoded 'assets')
Run $pikoJava @((Join-Path $PSScriptRoot 'PatchSparsePack.java'),(Join-Path $pikoDecoded 'assets'))

# Side-by-side, locally signed experiment. Never uninstall/replace the user's wrapper.
$pikoManifest = Join-Path $pikoDecoded 'AndroidManifest.xml'
$pikoText = [IO.File]::ReadAllText($pikoManifest).Replace('io.wip.pico8','art.pikoos.runtimeexperiment').Replace('<application ','<application android:debuggable="true" ').Replace('android:label="@string/godot_project_name_string"','android:label="PIKOOS Runtime Test"')
[IO.File]::WriteAllText($pikoManifest,$pikoText,(New-Object Text.UTF8Encoding($false)))
$pikoConfig = Join-Path $pikoDecoded 'apktool.yml'
$pikoConfigText = [IO.File]::ReadAllText($pikoConfig).Replace('versionCode: 1','versionCode: 3').Replace('versionName: 1.6.6','versionName: 1.6.6-pikoos.3')
[IO.File]::WriteAllText($pikoConfig,$pikoConfigText,(New-Object Text.UTF8Encoding($false)))
$pikoUnsigned = Join-Path $pikoBuild 'unsigned.apk'
$pikoAligned = Join-Path $pikoBuild 'aligned.apk'
$pikoOutput = Join-Path $pikoArtifacts 'pikoos-runtime-test.apk'
$pikoTools = Join-Path $SdkRoot 'build-tools\36.0.0'
Run $pikoJava @('-jar',$ApktoolJar,'b',$pikoDecoded,'-o',$pikoUnsigned)
Run (Join-Path $pikoTools 'zipalign.exe') @('-f','4',$pikoUnsigned,$pikoAligned)
$pikoSigner = Join-Path $pikoTools 'lib\apksigner.jar'
Run $pikoJava @('-jar',$pikoSigner,'sign','--ks',$pikoKey,'--ks-key-alias','pikoos-lab','--ks-pass','pass:android','--key-pass','pass:android','--out',$pikoOutput,$pikoAligned)
Run $pikoJava @('-jar',$pikoSigner,'verify',$pikoOutput)
Run $pikoJava @((Join-Path $PSScriptRoot 'VerifySparsePack.java'),$pikoOutput)
Get-FileHash -LiteralPath $pikoOutput -Algorithm SHA256
