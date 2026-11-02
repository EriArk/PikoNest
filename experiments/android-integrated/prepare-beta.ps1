param(
    [string]$JdkRoot='C:\Program Files\Android\Android Studio\jbr',
    [string]$SdkRoot=(Join-Path $env:LOCALAPPDATA 'Android\Sdk'),
    [string]$SigningDirectory=(Join-Path $env:USERPROFILE '.pikonest\signing'),
    [switch]$SkipCoreTests
)
$ErrorActionPreference='Stop'
$pikoRepo=(Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$pikoKeytool=Join-Path $JdkRoot 'bin\keytool.exe'
$pikoLegacy=Join-Path $pikoRepo '.local\artifacts\runtime-lab-debug.keystore'
$pikoKey=Join-Path $SigningDirectory 'pikonest.jks'
$pikoSecret=Join-Path $SigningDirectory 'password.clixml'
$pikoPriorPassword=$env:PIKONEST_SIGNING_PASSWORD
function Run-Keytool([string[]]$Arguments){& $pikoKeytool @Arguments;if($LASTEXITCODE -ne 0){throw 'Signing-key operation failed; existing files retained'}}
try{
    if(-not (Test-Path -LiteralPath $pikoKey)){
        if(-not (Test-Path -LiteralPath $pikoLegacy)){throw 'Existing development signing identity is required; do not create a new identity silently'}
        New-Item -ItemType Directory -Force -Path $SigningDirectory | Out-Null
        if(-not (Test-Path -LiteralPath $pikoSecret)){
            $pikoRandom=New-Object byte[] 32
            $pikoRng=[Security.Cryptography.RandomNumberGenerator]::Create();try{$pikoRng.GetBytes($pikoRandom)}finally{$pikoRng.Dispose()}
            $pikoPassword=[Convert]::ToBase64String($pikoRandom)
            ConvertTo-SecureString -String $pikoPassword -AsPlainText -Force | Export-Clixml -LiteralPath $pikoSecret
            $pikoPassword=$null
        }
        $pikoProtected=Import-Clixml -LiteralPath $pikoSecret
        $env:PIKONEST_SIGNING_PASSWORD=[Net.NetworkCredential]::new('', $pikoProtected).Password
        Run-Keytool @('-importkeystore','-srckeystore',$pikoLegacy,'-srcstorepass','android','-srcalias','pikoos-lab','-srckeypass','android','-destkeystore',$pikoKey,'-deststoretype','JKS','-deststorepass:env','PIKONEST_SIGNING_PASSWORD','-destkeypass:env','PIKONEST_SIGNING_PASSWORD','-destalias','pikoos-lab','-noprompt')
    }
    if(-not (Test-Path -LiteralPath $pikoSecret)){throw 'Protected signing password is unavailable'}
    $pikoProtected=Import-Clixml -LiteralPath $pikoSecret
    $env:PIKONEST_SIGNING_PASSWORD=[Net.NetworkCredential]::new('', $pikoProtected).Password
    # Public certificate comparison proves update continuity without exposing private key bytes.
    $pikoCertificate=Join-Path $SigningDirectory 'certificate.der'
    Run-Keytool @('-exportcert','-keystore',$pikoKey,'-storepass:env','PIKONEST_SIGNING_PASSWORD','-alias','pikoos-lab','-file',$pikoCertificate)
    if(Test-Path -LiteralPath $pikoLegacy){
        $pikoOldCertificate=Join-Path $SigningDirectory 'development-certificate.der'
        Run-Keytool @('-exportcert','-keystore',$pikoLegacy,'-storepass','android','-alias','pikoos-lab','-file',$pikoOldCertificate)
        if((Get-FileHash -LiteralPath $pikoCertificate).Hash -ne (Get-FileHash -LiteralPath $pikoOldCertificate).Hash){throw 'Signing identity changed; candidate build refused'}
    }
    & (Join-Path $pikoRepo 'experiments\android-host\build.ps1') -JdkRoot $JdkRoot -SdkRoot $SdkRoot -Beta -SigningKeystore $pikoKey -SkipCoreTests:$SkipCoreTests
}finally{$env:PIKONEST_SIGNING_PASSWORD=$pikoPriorPassword}
