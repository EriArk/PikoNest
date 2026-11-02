package art.pikoos.runtimelab;

import android.app.*;
import android.content.*;
import android.util.AtomicFile;
import art.pikoos.lab.core.BackupSwap;
import java.io.*;
import java.nio.channels.*;
import java.util.*;

/** Cold-start recovery runs before preferences are loaded. Cross-process lock serializes renames. */
final class BackupRecovery {
    static volatile String problem="";
    static volatile boolean needsRestart;
    static File root(Context c){return new File(c.getFilesDir(),"backup-restore");}
    static AtomicFile pointer(Context c){return new AtomicFile(new File(root(c),"pending.txt"));}
    static synchronized void schedule(Context c,File stage,boolean rollback)throws Exception{
        if(!stage.getParentFile().getCanonicalFile().equals(root(c).getCanonicalFile())||!stage.getName().matches("[0-9a-f-]{36}"))throw new IOException("Invalid restore candidate");
        byte[] bytes=(stage.getName()+"\n"+(rollback?"rollback":"restore")).getBytes("US-ASCII");AtomicFile p=pointer(c);FileOutputStream out=null;try{out=p.startWrite();out.write(bytes);p.finishWrite(out);}catch(Exception e){p.failWrite(out);throw e;}
        if(!Arrays.equals(p.readFully(),bytes))throw new IOException("Restore request did not verify");
    }
    static boolean pending(Context c){return pointer(c).getBaseFile().exists()||new File(pointer(c).getBaseFile()+".bak").exists();}
    static void recover(Context c){
        if(!pending(c))return;
        try{root(c).mkdirs();try(RandomAccessFile lockFile=new RandomAccessFile(new File(root(c),"lock"),"rw");FileLock lock=lockFile.getChannel().lock()){
            if(!pending(c))return;String[] parts=new String(pointer(c).readFully(),"US-ASCII").split("\n",-1);
            if(parts.length!=2||!parts[0].matches("[0-9a-f-]{36}")||!parts[1].matches("restore|rollback"))throw new IOException("Restore request needs review");
            File stage=new File(root(c),parts[0]);BackupSwap.apply(new File(c.getApplicationInfo().dataDir),stage,parts[1].equals("rollback"),n->{});
            // Keep a readable receipt and both sets of files; only retire the request.
            try(FileOutputStream out=new FileOutputStream(new File(stage,"completed.txt"))){out.write(parts[1].getBytes("US-ASCII"));out.getFD().sync();}
            pointer(c).delete();if(pending(c))throw new IOException("Cannot retire restore request");problem="";needsRestart=true;
        }}catch(Exception e){problem="Restore paused: "+e.getMessage()+". Original and restored files are retained.";}
    }
    static boolean guard(Activity a){if(problem.isEmpty()&&!pending(a)&&!needsRestart)return false;a.startActivity(new Intent(a,BackupActivity.class).putExtra("recovery",true));a.finish();return true;}
}
