param(
    [string]$SdkRoot = (Join-Path $env:LOCALAPPDATA 'Android\Sdk'),
    [string]$JdkRoot = (Split-Path (Split-Path (Get-Command javac -ErrorAction Stop).Source)),
    [string]$BuildToolsVersion = '36.0.0',
    [string]$Platform = 'android-34'
)
$ErrorActionPreference = 'Stop'
$pikoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$pikoBuild = Join-Path $pikoRoot ('.local\android-host-build\' + [guid]::NewGuid().ToString('N'))
$pikoArtifacts = Join-Path $pikoRoot '.local\artifacts'
$pikoTools = Join-Path $SdkRoot ('build-tools\' + $BuildToolsVersion)
$pikoAndroid = Join-Path $SdkRoot ('platforms\' + $Platform + '\android.jar')
$pikoJava = Join-Path $JdkRoot 'bin\java.exe'
$pikoJavac = Join-Path $JdkRoot 'bin\javac.exe'
$pikoJar = Join-Path $JdkRoot 'bin\jar.exe'

function Invoke-PikoTool([string]$Program, [string[]]$Arguments) {
    & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Program failed with exit code $LASTEXITCODE" }
}

New-Item -ItemType Directory -Force -Path $pikoBuild,$pikoArtifacts,
    (Join-Path $pikoBuild 'classes'),(Join-Path $pikoBuild 'tests'),(Join-Path $pikoBuild 'dex') | Out-Null
$pikoCore = @(Get-ChildItem (Join-Path $PSScriptRoot 'core') -Recurse -Filter '*.java' | ForEach-Object FullName)
$pikoCore += @(Get-ChildItem (Join-Path $pikoRoot 'experiments\p8-roundtrip\core') -Recurse -Filter '*.java' | ForEach-Object FullName)
$pikoSources = @(Get-ChildItem (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java' | ForEach-Object FullName)
$pikoTests = @(Get-ChildItem (Join-Path $PSScriptRoot 'tests') -Filter '*.java' | ForEach-Object FullName)

# Compile/test the domain with the JDK alone, proving no Android dependency.
Invoke-PikoTool $pikoJavac (@('--release','8','-encoding','UTF-8','-d',(Join-Path $pikoBuild 'tests')) + $pikoCore + $pikoTests)
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'LuaInsertTest',(Join-Path $PSScriptRoot 'assets\blank.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'LuaSymbolsTest')
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'LuaEditorTest',(Join-Path $PSScriptRoot 'assets\blank.p8'),(Join-Path $PSScriptRoot 'assets\lights.p8'),(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'FolderSetupTest')
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'LauncherPathTest')
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'GameTreeTest')
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'PicoIncludesTest')
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'RuntimeFileSetTest')
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'RuntimeArchiveTest')
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'RuntimeProbeTest')
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'P8PngTest',(Join-Path $PSScriptRoot 'tests\fixtures\lights-0.2.7.p8.png'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'PlayWorkflowTest',(Join-Path $PSScriptRoot 'assets\lights.p8'),(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'LabCartridgeTest',(Join-Path $PSScriptRoot 'assets\workshop.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'WorkshopTest',(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'SpriteWorkflowTest',(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'LibraryWorkflowTest',(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'DrawingWorkflowTest',(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'RegionWorkflowTest',(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'HeroBindingTest',(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'HeroFreeTest',(Join-Path $PSScriptRoot 'assets\blank.p8'),(Join-Path $PSScriptRoot 'assets\lights.p8'),(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'CopyWorkflowTest',(Join-Path $PSScriptRoot 'assets\blank.p8'),(Join-Path $PSScriptRoot 'assets\lights.p8'),(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'AssetLibraryTest',(Join-Path $PSScriptRoot 'assets\moon-garden.p8'),(Join-Path $PSScriptRoot 'assets\blank.p8'),(Join-Path $PSScriptRoot 'assets\lights.p8'),(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'AssetNamingTest',(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'CartridgeImportTest',(Join-Path $PSScriptRoot 'assets\lights.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'CartridgeExportTest',(Join-Path $PSScriptRoot 'assets\lights.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'TransformWorkflowTest',(Join-Path $PSScriptRoot 'assets\blank.p8'),(Join-Path $PSScriptRoot 'assets\lights.p8'),(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'RecolorWorkflowTest',(Join-Path $PSScriptRoot 'assets\blank.p8'),(Join-Path $PSScriptRoot 'assets\lights.p8'),(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'RectangleWorkflowTest',(Join-Path $PSScriptRoot 'assets\blank.p8'),(Join-Path $PSScriptRoot 'assets\lights.p8'),(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'HistoryWorkflowTest',(Join-Path $PSScriptRoot 'assets\blank.p8'),(Join-Path $PSScriptRoot 'assets\lights.p8'),(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'MoveWorkflowTest',(Join-Path $PSScriptRoot 'assets\blank.p8'),(Join-Path $PSScriptRoot 'assets\lights.p8'),(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoBuild 'tests'),'OvalWorkflowTest',(Join-Path $PSScriptRoot 'tests\fixtures\oval-0.2.7.sha256'),(Join-Path $PSScriptRoot 'assets\blank.p8'),(Join-Path $PSScriptRoot 'assets\lights.p8'),(Join-Path $PSScriptRoot 'assets\moon-garden.p8'))

Invoke-PikoTool $pikoJavac (@('--release','8','-encoding','UTF-8','-classpath',$pikoAndroid,'-d',(Join-Path $pikoBuild 'classes')) + $pikoCore + $pikoSources)
Invoke-PikoTool $pikoJar @('--create','--file',(Join-Path $pikoBuild 'classes.jar'),'-C',(Join-Path $pikoBuild 'classes'),'.')
Invoke-PikoTool $pikoJava @('-cp',(Join-Path $pikoTools 'lib\d8.jar'),'com.android.tools.r8.D8','--min-api','26','--lib',$pikoAndroid,'--output',(Join-Path $pikoBuild 'dex'),(Join-Path $pikoBuild 'classes.jar'))

$pikoUnsigned = Join-Path $pikoBuild 'unsigned.apk'
Invoke-PikoTool (Join-Path $pikoTools 'aapt.exe') @('package','-f','-M',(Join-Path $PSScriptRoot 'AndroidManifest.xml'),'-A',(Join-Path $PSScriptRoot 'assets'),'-I',$pikoAndroid,'-F',$pikoUnsigned)
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression
$pikoZip = [System.IO.Compression.ZipFile]::Open($pikoUnsigned, [System.IO.Compression.ZipArchiveMode]::Update)
try {
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($pikoZip,(Join-Path $pikoBuild 'dex\classes.dex'),'classes.dex') | Out-Null
} finally { $pikoZip.Dispose() }
$pikoAligned = Join-Path $pikoBuild 'aligned.apk'
Invoke-PikoTool (Join-Path $pikoTools 'zipalign.exe') @('-f','4',$pikoUnsigned,$pikoAligned)

# Local experiment signing key, never a production credential or tracked artifact.
$pikoKey = Join-Path $pikoArtifacts 'runtime-lab-debug.keystore'
if (-not (Test-Path -LiteralPath $pikoKey)) {
    Invoke-PikoTool (Join-Path $JdkRoot 'bin\keytool.exe') @('-genkeypair','-keystore',$pikoKey,'-storepass','android','-keypass','android','-alias','pikoos-lab','-keyalg','RSA','-keysize','2048','-validity','3650','-dname','CN=PIKOOS Runtime Lab')
}
$pikoApk = Join-Path $pikoArtifacts 'pikoos-runtime-lab.apk'
Invoke-PikoTool $pikoJava @('-jar',(Join-Path $pikoTools 'lib\apksigner.jar'),'sign','--ks',$pikoKey,'--ks-key-alias','pikoos-lab','--ks-pass','pass:android','--key-pass','pass:android','--out',$pikoApk,$pikoAligned)
Invoke-PikoTool $pikoJava @('-jar',(Join-Path $pikoTools 'lib\apksigner.jar'),'verify',$pikoApk)
Get-Item -LiteralPath $pikoApk | Select-Object FullName,Length
Get-FileHash -LiteralPath $pikoApk -Algorithm SHA256 | Format-List
