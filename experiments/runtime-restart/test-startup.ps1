param([string]$Adb = (Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'))
$ErrorActionPreference = 'Stop'
$pikoPackage = 'art.pikoos.runtimeexperiment'
if ((& $Adb shell pidof $pikoPackage | Out-String).Trim()) { throw 'Exit the runtime test app first.' }
$pikoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$pikoWork = Join-Path $pikoRoot ('.local\audio-fault-test\'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force $pikoWork | Out-Null
$pikoBackup = 'files/package/pulsar.pikoos-test-backup'
& $Adb shell run-as $pikoPackage test -e $pikoBackup
if ($LASTEXITCODE -eq 0) { throw 'An earlier backup exists; recover it before testing.' }
& $Adb shell run-as $pikoPackage cp files/package/pulsar.sh $pikoBackup
if ($LASTEXITCODE -ne 0) { throw 'Cannot back up test bootstrap; nothing changed.' }
$pikoRemote = '/data/local/tmp/pikoos-audio-test-'+[guid]::NewGuid().ToString('N')+'.sh'
try {
    foreach ($pikoCase in @(
        @{Name='early-exit'; Script="exit 7`n"; Expected='audio server exited before socket readiness'},
        @{Name='timeout'; Script="exec sleep 30`n"; Expected='audio socket readiness timed out'}
    )) {
        $pikoFile = Join-Path $pikoWork ($pikoCase.Name+'.sh')
        [IO.File]::WriteAllText($pikoFile,$pikoCase.Script,(New-Object Text.UTF8Encoding($false)))
        & $Adb push $pikoFile $pikoRemote
        if ($LASTEXITCODE -ne 0) { throw 'Fault fixture transfer failed.' }
        & $Adb shell run-as $pikoPackage cp $pikoRemote files/package/pulsar.sh
        if ($LASTEXITCODE -ne 0) { throw 'Fault fixture setup failed.' }
        $pikoResult = (& $Adb shell "run-as $pikoPackage /system/bin/sh -c 'cd files/package; LD_LIBRARY_PATH=. ./busybox ash start_pico_proot.sh 2>&1'" | Out-String)
        $pikoExit = $LASTEXITCODE
        [IO.File]::WriteAllText((Join-Path $pikoWork ($pikoCase.Name+'.txt')),$pikoResult)
        if ($pikoExit -ne 1 -or $pikoResult -notmatch [regex]::Escape($pikoCase.Expected)) {
            throw "Missing failure/diagnostic for $($pikoCase.Name): $pikoExit $pikoResult"
        }
        if ($pikoResult -match 'Using FD 8 strategy') { throw 'proot launched despite failed audio.' }
        # The trap must reap even the sleeping fake server, not leave it running.
        $pikoRemaining = (& $Adb shell run-as $pikoPackage ps -o COMM | Out-String)
        if ($pikoRemaining -match '(?m)^\s*(sleep|pulseaudio|proot|pico8_64)\s*$') { throw 'Runtime test leaked a child.' }
        "PASS: $($pikoCase.Name) refuses runtime startup and reaps child"
    }
} finally {
    & $Adb shell run-as $pikoPackage cp $pikoBackup files/package/pulsar.sh
    if ($LASTEXITCODE -eq 0) { & $Adb shell run-as $pikoPackage rm $pikoBackup }
    else { Write-Warning "Restore failed: backup retained at $pikoBackup" }
    & $Adb shell rm $pikoRemote
}
