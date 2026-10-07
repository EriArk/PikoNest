package art.pikoos.runtimelab;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.nio.file.Files;

/** One atomic pointer; previous runtime homes remain available after activation. */
final class RuntimeHomes {
    static AtomicFile pointer(Context c){return new AtomicFile(new File(c.getFilesDir(),"runtime-home.txt"));}
    static File current(Context c)throws IOException{
        AtomicFile pointer=pointer(c);if(!pointer.getBaseFile().exists()&&!new File(pointer.getBaseFile()+".bak").exists())return new File(c.getFilesDir(),"runtime-data");
        String id=new String(pointer.readFully(),"US-ASCII");
        if(!id.matches("[0-9a-f]{12}"))throw new IOException("Runtime home record needs review; old data was kept");
        File home=new File(c.getFilesDir(),"rh/"+id);
        if(!new File(home,".pikonest-home").isFile()||!new File(home,"data").isDirectory())throw new IOException("Imported runtime home is unavailable; old data was kept");
        return home;
    }
    static void activate(Context c,File home)throws IOException{
        String id=home.getName();if(!id.matches("[0-9a-f]{12}")||!home.getParentFile().equals(new File(c.getFilesDir(),"rh")))throw new IOException("Unexpected runtime home");
        Files.write(new File(home,".pikonest-home").toPath(),"verified-copy-1".getBytes("US-ASCII"));
        AtomicFile pointer=pointer(c);FileOutputStream out=null;
        try{out=pointer.startWrite();out.write(id.getBytes("US-ASCII"));out.getFD().sync();pointer.finishWrite(out);}catch(IOException e){pointer.failWrite(out);throw e;}
    }
}
