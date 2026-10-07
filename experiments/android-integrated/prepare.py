"""Compose pinned runtime resources with our host; never consumes purchased binaries."""
from pathlib import Path
import sys, hashlib, struct, shutil, tarfile, io, xml.etree.ElementTree as ET

app, repo, boot_source = map(Path, sys.argv[1:])
assets = app / 'assets'
# Private runtime home, independent of the old wrapper's shared folder.
bootstrap=assets/'package.dat'; buffer=io.BytesIO()
with tarfile.open(fileobj=io.BytesIO(bootstrap.read_bytes()),mode='r:gz') as source, tarfile.open(fileobj=buffer,mode='w:gz') as target:
    for member in source:
        stream=source.extractfile(member) if member.isfile() else None
        if member.name=='package/start_pico_proot.sh':
            code=stream.read().decode(); assert code.count('/sdcard/Documents/pico8')==2
            code=code.replace('/sdcard/Documents/pico8','${PIKONEST_DATA:?Missing runtime home}')
            content=code.encode(); member.size=len(content); stream=io.BytesIO(content)
        target.addfile(member,stream)
bootstrap.write_bytes(buffer.getvalue())
launch=assets/'run_pico_cmd_pikoos.gd'
code=launch.read_text(encoding='utf-8')
needle="var cmdline = env_setup + extra_bind_export"
assert code.count(needle)==1
code=code.replace(needle,'var cmdline = "export PIKONEST_DATA=\'" + PicoBootManager.PUBLIC_FOLDER + "\'; " + env_setup + extra_bind_export')
launch.write_text(code,encoding='utf-8')
ns = 'http://schemas.android.com/apk/res/android'
ET.register_namespace('android', ns)
def a(name): return '{'+ns+'}'+name
manifest = ET.parse(app / 'AndroidManifest.xml')
root = manifest.getroot()
root.set('package', 'art.pikoos.runtimelab')
root.set(a('installLocation'), 'internalOnly')
host = ET.parse(repo / 'experiments/android-host/AndroidManifest.xml').getroot()
application = root.find('application')
application.set(a('label'), 'PikoNest')
application.set(a('debuggable'), 'true')
application.set(a('allowBackup'), 'false')
application.set(a('theme'), '@android:style/Theme.Material.Light.NoActionBar')
application.attrib.pop(a('requestLegacyExternalStorage'), None)
application.set(a('usesCleartextTraffic'), 'false')
for node in list(root.findall('uses-permission')):
    if node.get(a('name')) not in ('android.permission.INTERNET','android.permission.VIBRATE'):
        root.remove(node)
for node in list(application):
    if node.tag == 'activity-alias':
        application.remove(node)
    elif node.tag == 'activity' and node.get(a('name')) == 'com.godot.game.GodotApp':
        node.set(a('process'), ':runtime')
        node.set(a('taskAffinity'),'art.pikoos.runtimelab.runtime')
        node.set(a('exported'), 'false')
        for intent in list(node.findall('intent-filter')): node.remove(intent)
    elif node.tag == 'provider':
        node.set(a('authorities'), node.get(a('authorities')).replace('io.wip.pico8','art.pikoos.runtimelab'))
for node in host.find('application'):
    application.append(node)
root.append(host.find('queries'))
# Only for migration of an existing signed lab session; new launches are internal.
for permission in host.findall('uses-permission'):
    root.append(permission)
ET.SubElement(application, 'meta-data', {a('name'):'art.pikoos.integrated',a('value'):'true'})
for kind, name, extra in [
    ('activity','ProbeActivity',{a('theme'):'@android:style/Theme.Translucent.NoTitleBar'}),
    ('activity','DiagnosticActivity',{a('theme'):'@android:style/Theme.Translucent.NoTitleBar'}),
    ('provider','ProbeStatusProvider',{a('authorities'):'art.pikoos.runtimelab.runtime'}),
]:
    ET.SubElement(application,kind,{a('name'):'art.pikoos.runtimeexperiment.'+name,
        a('exported'):'false',a('process'):':runtime',**extra})
# Retain the name used by our pinned adapter, as an internal alias without intent filters.
ET.SubElement(application,'activity-alias',{a('name'):'com.godot.game.GodotAppLauncher',
    a('targetActivity'):'com.godot.game.GodotApp',a('exported'):'false'})
manifest.write(app / 'AndroidManifest.xml',encoding='utf-8',xml_declaration=True)
config = (app/'apktool.yml').read_text(encoding='utf-8')
config = config.replace('minSdkVersion: 24','minSdkVersion: 26')
config = config.replace('versionCode: 1','versionCode: '+host.get(a('versionCode')))
config = config.replace('versionName: 1.6.6','versionName: '+host.get(a('versionName')))
(app/'apktool.yml').write_text(config,encoding='utf-8')

# The host prepares a verified package before Godot starts. Do not invoke upstream
# bootstrap deletion, storage permission requests or another runtime ZIP picker.
boot = boot_source.read_text(encoding='utf-8')
old = 'static var PUBLIC_FOLDER = "/sdcard/Documents/pico8"'
assert boot.count(old)==1
boot = boot.replace(old,'static var PUBLIC_FOLDER: String:\n\tget:\n\t\treturn APPDATA_FOLDER + "/runtime-data"')
start,end=boot.index('func _ready() -> void:'),boot.index('\nconst BOOTSTRAP_PACKAGE_VERSION')
boot = boot[:start]+'''func _ready() -> void:
	await get_tree().process_frame
	set_ui_state()
	if not FileAccess.file_exists("user://package/pikonest-ready"):
		push_error("PikoNest runtime is not prepared")
		get_tree().quit()
		return
	load_sdl_mappings()
	get_tree().change_scene_to_file("res://main.tscn")

'''+boot[end:]
(assets/'boot_pikonest.gd').write_text(boot,encoding='utf-8')
cache=assets/'.godot/global_script_class_cache.cfg'
classes=cache.read_text(encoding='utf-8')
assert classes.count('"path": "res://boot.gd"')==1
cache.write_text(classes.replace('"path": "res://boot.gd"','"path": "res://boot_pikonest.gd"'),encoding='utf-8')
remap=assets/'boot.gd.remap'
assert 'res://boot.gdc' in remap.read_text()
remap.write_text(remap.read_text().replace('res://boot.gdc','res://boot_pikonest.gd'),encoding='utf-8')
for source in (repo/'experiments/android-host/assets').iterdir():
    if source.is_file():
        assert not (assets/source.name).exists(), source.name
        shutil.copy2(source,assets/source.name)
(assets/'pikoos').mkdir(exist_ok=True)
for name in ('Monocraft.ttf','Monocraft-OFL.txt'):
    shutil.copy2(assets/name,assets/'pikoos'/name)
shutil.copy2(repo/'THIRD_PARTY_NOTICES.md',assets/'PIKONEST-NOTICES.md')

# Re-index changed and added Godot assets; preserve every other index record.
index=assets/'assets.sparsepck'; data=index.read_bytes()
assert struct.unpack_from('<6I2Q',data)==(0x43504447,3,4,6,0,6,0,104)
count,=struct.unpack_from('<I',data,104); assert count==260
changed={'package.dat','run_pico_cmd.gd.remap','boot.gd.remap','.godot/global_script_class_cache.cfg'}
added=['run_pico_cmd_pikoos.gd','PIKOOS-UPSTREAM-LICENSE.txt','boot_pikonest.gd']
def entry(name):
    path=name.encode();path+=b'\0'*((-len(path))%4);payload=(assets/name).read_bytes()
    return struct.pack('<I',len(path))+path+struct.pack('<QQ',0,len(payload))+hashlib.md5(payload).digest()+struct.pack('<I',0)
out=data[:104]+struct.pack('<I',count+len(added)); pos=108
for _ in range(count):
    start=pos; length,=struct.unpack_from('<I',data,pos);pos+=4
    name=data[pos:pos+length].rstrip(b'\0').decode();pos+=length+36
    out+=entry(name) if name in changed else data[start:pos]
assert pos==len(data)
index.write_bytes(out+b''.join(entry(name) for name in added))
print('Integrated manifest: one launcher, private runtime process, no broad storage permission; sparse index 263 entries')
