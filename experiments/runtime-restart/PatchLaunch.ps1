param([Parameter(Mandatory=$true)][string]$Source, [Parameter(Mandatory=$true)][string]$Assets)
$ErrorActionPreference = 'Stop'
# Upstream 1.6.6, commit 562662e35727ae7706fe97fed3390db249a35263.
# Keep the full MIT source outside the repo; patch cold-intent consumption and file-set launch.
if ((Get-FileHash -LiteralPath $Source -Algorithm SHA256).Hash -ne '71ef40406dc66f8f590542cdbbc37263734141ae337ae9b3d2297ec5f42cd2cc') {
    throw 'Unexpected run_pico_cmd.gd source; use the pinned 1.6.6 input.'
}
$pikoScript = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $Source))
$pikoNeedle = "`t`t`turi = data`n"
if (($pikoScript.Split(@($pikoNeedle),[StringSplitOptions]::None)).Length -ne 2) { throw 'Cold intent assignment is not unique.' }
$pikoReplacement = $pikoNeedle + @'
			# PIKOOS: cold dispatch is already consumed. Resume must not replay it.
			# Set both observers before decoding/awaiting or launching the runtime.
			Applinks.last_data = data
			last_received_data = data
'@ + "`n"
$pikoScript = $pikoScript.Replace($pikoNeedle,$pikoReplacement)
# The same patched script owns a bounded file-set decoder and a per-session root.
function Replace-PikoOnce([string]$Needle,[string]$Replacement) {
    if (($script:pikoScript.Split(@($Needle),[StringSplitOptions]::None)).Length -ne 2) { throw "Patch anchor is not unique: $Needle" }
    $script:pikoScript = $script:pikoScript.Replace($Needle,$Replacement)
}
Replace-PikoOnce "func _launch_pico8(target_path: String) -> void:`n" "func _launch_pico8(target_path: String) -> void:`n`tif target_path == PIKOOS_INVALID_SET:`n`t`treturn`n"
Replace-PikoOnce "`t# Ensure clean slate (in case force kill was needed or cold boot)`n" "`t_pikoos_active_probe = target_path.get_base_dir() if _pikoos_probe_path(target_path) else `"`"`n`t# Ensure clean slate (in case force kill was needed or cold boot)`n"
Replace-PikoOnce "`t`t`t`t`tget_tree().quit()`n" "`t`t`t`t`t_pikoos_release_set()`n`t`t`t`t`tget_tree().quit()`n"
Replace-PikoOnce "`tpending_restart_path = await _decode_and_fix_path(data)`n" "`tpending_restart_path = await _decode_and_fix_path(data)`n`tif pending_restart_path == PIKOOS_INVALID_SET:`n`t`treturn`n"
Replace-PikoOnce "`tprint(`"Final Target Path: `", uri)`n" "`tif uri.ends_with(`".pikoset`"):`n`t`treturn _pikoos_unpack_set(uri)`n`tprint(`"Final Target Path: `", uri)`n"
Replace-PikoOnce "`t`tif err == OK:`n" "`t`tif err == OK and not target_path.begins_with(PicoBootManager.APPDATA_FOLDER + `"/pikoos-sets/`"):`n"
Replace-PikoOnce "`t# Finalize export string`n" "`tif target_path.begins_with(PicoBootManager.APPDATA_FOLDER + `"/pikoos-sets/`"):`n`t`trun_arg += `" -root_path /home/custom_mount`"`n`t# Finalize export string`n"
Replace-PikoOnce "`t# Finalize export string`n" "`tif _pikoos_probe_path(target_path):`n`t`troot_bind_export = `"export PROOT_ROOT_BIND='--bind=`" + target_path.get_base_dir() + `"/pico8_64:/home/pico/pico-8/pico8_64'; `"`n`t`tbbs_bind_export = `"export PROOT_BBS_BIND='--bind=`" + target_path.get_base_dir() + `"/pico8.dat:/home/pico/pico-8/pico8.dat'; `"`n`t`trun_arg = `" -run /home/custom_mount/probe.p8 -root_path /home/custom_mount -home /home/custom_mount/probe-home -desktop /home/custom_mount/probe-home`"`n`t# Finalize export string`n"
Replace-PikoOnce "`n`tprint(`"executing as pid `" + str(pico_pid) + `"\n`" + cmdline)`n" "`n`tif not _pikoos_active_probe.is_empty():`n`t`t_pikoos_probe_status(`"RUNNING`", pico_pid)`n`tprint(`"executing as pid `" + str(pico_pid) + `"\n`" + cmdline)`n"
Replace-PikoOnce "`t`t`t`t`t_pikoos_release_set()`n" "`t`t`t`t`t_pikoos_release_set()`n`t`t`t`t`t_pikoos_finish_probe()`n"
$pikoScript += "`n" + [IO.File]::ReadAllText((Join-Path $PSScriptRoot 'file-set.gd'))
$pikoScript += "`n" + [IO.File]::ReadAllText((Join-Path $PSScriptRoot 'probe.gd'))
$pikoRemapPath = Join-Path $Assets 'run_pico_cmd.gd.remap'
$pikoRemap = [IO.File]::ReadAllText($pikoRemapPath)
if ($pikoRemap.Trim() -ne "[remap]`n`npath=`"res://run_pico_cmd.gdc`"") { throw 'Unexpected script remap.' }
$pikoUtf8 = New-Object Text.UTF8Encoding($false)
[IO.File]::WriteAllText((Join-Path $Assets 'run_pico_cmd_pikoos.gd'),$pikoScript,$pikoUtf8)
[IO.File]::WriteAllText($pikoRemapPath,$pikoRemap.Replace('run_pico_cmd.gdc','run_pico_cmd_pikoos.gd'),$pikoUtf8)
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'UPSTREAM-LICENSE.txt') -Destination (Join-Path $Assets 'PIKOOS-UPSTREAM-LICENSE.txt')
