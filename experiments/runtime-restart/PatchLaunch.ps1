param([Parameter(Mandatory=$true)][string]$Source, [Parameter(Mandatory=$true)][string]$Assets)
$ErrorActionPreference = 'Stop'
# Upstream 1.6.6, commit 562662e35727ae7706fe97fed3390db249a35263.
# Keep the full MIT source outside the repo; patch only the consumed cold intent.
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
$pikoRemapPath = Join-Path $Assets 'run_pico_cmd.gd.remap'
$pikoRemap = [IO.File]::ReadAllText($pikoRemapPath)
if ($pikoRemap.Trim() -ne "[remap]`n`npath=`"res://run_pico_cmd.gdc`"") { throw 'Unexpected script remap.' }
$pikoUtf8 = New-Object Text.UTF8Encoding($false)
[IO.File]::WriteAllText((Join-Path $Assets 'run_pico_cmd_pikoos.gd'),$pikoScript,$pikoUtf8)
[IO.File]::WriteAllText($pikoRemapPath,$pikoRemap.Replace('run_pico_cmd.gdc','run_pico_cmd_pikoos.gd'),$pikoUtf8)
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'UPSTREAM-LICENSE.txt') -Destination (Join-Path $Assets 'PIKOOS-UPSTREAM-LICENSE.txt')
