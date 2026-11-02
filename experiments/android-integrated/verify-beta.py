"""Offline candidate checks. Does not install, tag, upload or grant release approval."""
import subprocess,sys,hashlib,json,zipfile
from pathlib import Path
apk,aapt=map(Path,sys.argv[1:3])
repo=Path(__file__).resolve().parents[2]
badging=subprocess.check_output([str(aapt),'dump','badging',str(apk)],text=True)
manifest=subprocess.check_output([str(aapt),'dump','xmltree',str(apk),'AndroidManifest.xml'],text=True)
assert "package: name='art.pikoos.runtimelab'" in badging
assert "versionName='0.1.0-beta.1'" in badging
assert "sdkVersion:'26'" in badging and "targetSdkVersion:'28'" in badging
assert 'application-debuggable' not in badging
assert badging.count('launchable-activity:')==1
assert 'art.pikoos.runtimelab.PikoApplication' in manifest or '.PikoApplication' in manifest
assert 'BackupActivity' in manifest
assert 'android.permission.MANAGE_EXTERNAL_STORAGE' not in manifest
assert 'android.permission.WRITE_EXTERNAL_STORAGE' not in manifest
with zipfile.ZipFile(apk) as z:
    launch=z.read('assets/run_pico_cmd_pikoos.gd').decode()
    assert 'sploreRequested()' in launch and ' -splore' in launch
    for notice in ['GODOT-LICENSE.txt','GODOT-COPYRIGHT.txt','PressStart2P-LICENSE.txt']:
        assert len(z.read('assets/'+notice))>100
    native_lock=json.loads((repo/'docs/release/beta1/native-source-lock.json').read_text(encoding='utf-8'))
    native_notice=z.read('assets/NATIVE-NOTICES.txt')
    assert hashlib.sha256(native_notice).hexdigest()==native_lock['notice_asset']['sha256']
# Drift check only: partial provenance is allowed for a LOCAL candidate. The
# separate --require-complete audit must pass before any owner-approved release.
subprocess.run([sys.executable,str(repo/'tools/dependencies/audit.py'),'--apk',str(apk)],check=True)
print('Beta candidate: fixed package, version, SDK 26/28, one launcher, backup recovery, no debugging or broad-storage permission')
checksum=hashlib.sha256(apk.read_bytes()).hexdigest()
apk.with_suffix('.apk.sha256').write_text(checksum+'  '+apk.name+'\n',encoding='ascii')
