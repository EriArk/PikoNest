"""Render current production Java Views via Java2D. These are not Android screenshots."""
from pathlib import Path
import subprocess,argparse
p=argparse.ArgumentParser();p.add_argument('--jdk',default='C:/Program Files/Android/Android Studio/jbr');p.add_argument('--output',default='.local/alpha93/previews');args=p.parse_args()
root=Path(__file__).resolve().parents[2];out=(root/args.output).resolve();out.mkdir(parents=True,exist_ok=True)
build=root/'.local/host-preview';build.mkdir(exist_ok=True)
sources=list((root/'experiments/host-preview/stubs').rglob('*.java'))+list((root/'experiments/host-preview').glob('*.java'))
sources+=list((root/'experiments/android-host/core').rglob('*.java'))+list((root/'experiments/p8-roundtrip/core').rglob('*.java'))
sources+=[root/'experiments/android-host/src/art/pikoos/runtimelab'/f'{n}.java' for n in ['GameSurface','LibraryView','ShelfSurface','PlayView','FirstRunView','RuntimeSetupView','BackupView','FolderView']]
argfile=build/'sources.args';argfile.write_text('\n'.join('"'+s.as_posix()+'"' for s in sources),encoding='utf-8')
bin=Path(args.jdk)/'bin';classes=build/'classes';classes.mkdir(exist_ok=True)
subprocess.run([str(bin/'javac.exe'),'-encoding','UTF-8','-d',str(classes),'@'+str(argfile)],cwd=root,check=True)
for name in ['GamePreview','MusicPreview','TilePreview','SetupPreview','PlayPreview']:
 subprocess.run([str(bin/'java.exe'),'-cp',str(classes),'art.pikoos.runtimelab.'+name,str(out)],cwd=root,check=True)
print('Host previews written to',out)
