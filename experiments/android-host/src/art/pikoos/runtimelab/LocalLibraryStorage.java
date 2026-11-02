package art.pikoos.runtimelab;

import android.util.AtomicFile;
import art.pikoos.lab.core.*;
import java.io.*;
import java.util.*;

/** Original library paths preserved; mutation is blocked while taking an activation copy. */
final class LocalLibraryStorage implements LibraryRepository.Storage {
    final File root;
    LocalLibraryStorage(File root)throws IOException{this.root=root.getCanonicalFile();}
    private File file(String p)throws Exception{LibraryRepository.path(p);File f=new File(root,p);if(!f.getCanonicalFile().equals(f.getAbsoluteFile()))throw new IOException("Linked library path is unavailable");return f;}
    public List<String> list(String directory)throws Exception{
        synchronized(LocalLibraryStorage.class){
            if(!directory.equals("asset-library/sprites")&&!directory.equals("asset-library/parameters"))throw new IOException("Invalid library section");
            File dir=new File(root,directory);if(!dir.getCanonicalFile().equals(dir.getAbsoluteFile()))throw new IOException("Linked library directory is unavailable");
            List<String> result=new ArrayList<>();if(!dir.exists())return result;
            File[] files=dir.listFiles();if(files==null)throw new IOException("Cannot read library");String ext=directory.endsWith("sprites")?".pksp":".pkpr";
            for(File f:files)if(f.getName().endsWith(ext)){String path=directory+"/"+f.getName();file(path);result.add(path);}return result;
        }
    }
    public byte[] read(String p)throws Exception{
        synchronized(LocalLibraryStorage.class){File f=file(p);if(f.length()>20000)throw new IOException("Library resource is too large");try{return new AtomicFile(f).readFully();}catch(FileNotFoundException e){if(f.exists())throw e;return null;}}
    }
    private void write(String p,byte[] data)throws Exception{
        if(BackupActivity.inProgress())throw new IOException("Finish backup or restore before editing resources.");
        if(LibraryStorage.activating)throw new IOException("Library activation is running. Finish or cancel before editing resources.");
        File f=file(p);if(!f.getParentFile().isDirectory()&&!f.getParentFile().mkdirs())throw new IOException("Cannot create library directory");
        AtomicFile target=new AtomicFile(f);FileOutputStream out=null;try{out=target.startWrite();out.write(data);target.finishWrite(out);}catch(Exception e){target.failWrite(out);throw e;}
    }
    public void create(String p,byte[] b)throws Exception{synchronized(LocalLibraryStorage.class){if(read(p)!=null)throw new IOException("Resource already exists");write(p,b);}}
    public void replace(String p,byte[] a,byte[] b)throws Exception{synchronized(LocalLibraryStorage.class){if(!Arrays.equals(a,read(p)))throw new IOException("Resource changed before save");write(p,b);}}
}
