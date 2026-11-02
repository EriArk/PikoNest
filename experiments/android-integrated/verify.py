import sys,zipfile,struct,hashlib,tarfile,io,json
from pathlib import Path
repo=Path(__file__).resolve().parents[2]
with zipfile.ZipFile(sys.argv[1]) as apk:
    index=apk.read('assets/assets.sparsepck'); count,=struct.unpack_from('<I',index,104)
    assert count==263; pos=108
    for _ in range(count):
        length,=struct.unpack_from('<I',index,pos);pos+=4
        name=index[pos:pos+length].rstrip(b'\0').decode();pos+=length
        offset,size=struct.unpack_from('<QQ',index,pos);md5=index[pos+16:pos+32];flags,=struct.unpack_from('<I',index,pos+32);pos+=36
        payload=apk.read('assets/'+name)
        assert offset==flags==0 and size==len(payload) and md5==hashlib.md5(payload).digest(),name
    assert pos==len(index)
    forbidden={'pico8','pico8_64','pico8.dat','pico8_dyn','pico8.exe'}
    assert not any(n.rsplit('/',1)[-1] in forbidden for n in apk.namelist())
    with tarfile.open(fileobj=io.BytesIO(apk.read('assets/package.dat')),mode='r:gz') as tar:
        names=tar.getnames()
        assert not any(n.rsplit('/',1)[-1] in forbidden for n in names)
        assert all(n.startswith('package/') or n=='package' for n in names)
        start=tar.extractfile('package/start_pico_proot.sh').read().decode()
        pulse=tar.extractfile('package/pulsar.sh').read().decode()
        session=(repo/'experiments/runtime-restart/audio-session.sh').read_text().replace('\r\n','\n')
        assert session in start, 'Support refresh lost audio startup/session fixes'
        assert '${PIKONEST_DATA:?Missing runtime home}' in start
        assert 'exec $PKG_DIR/pulseaudio' in pulse and './busybox killall pulseaudio' not in pulse
        support=json.loads((repo/'tools/dependencies/runtime-support.json').read_text())
        assert apk.read('assets/runtime-support-id.txt').decode()==support['output']['sha256']
        for row in support['files']:
            data=tar.extractfile(row['path']).read()
            assert len(data)==row['bytes'] and hashlib.sha256(data).hexdigest()==row['sha256']
    assert 'classes2.dex' in apk.namelist()
print('263 Godot assets verified; APK/bootstrap contain no official runtime binaries')
