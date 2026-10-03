package art.pikoos.runtimeexperiment;
import android.content.*;
import android.os.*;
import android.util.AtomicFile;
import art.pikoos.lab.core.RuntimeSession;
import art.pikoos.lab.core.RuntimeSession.Phase;
import java.io.*;
import java.nio.file.*;

/** Signature-gated host handshake; the pinned process monitor writes RUNNING/EXITED. */
final class SessionStatus {
    private static final class Gone extends IOException {}
    static AtomicFile file(Context c){return new AtomicFile(new File(c.getFilesDir(),"play-session.txt"));}
    static RuntimeSession read(Context c){try{return RuntimeSession.read(new String(file(c).readFully(),"US-ASCII"));}catch(Exception e){return RuntimeSession.read("");}}
    static String start(int pid)throws IOException{
        String stat=new String(Files.readAllBytes(Paths.get("/proc/"+pid+"/stat")),"US-ASCII");
        String[] f=stat.substring(stat.lastIndexOf(')')+2).split(" ");
        if(f[0].equals("Z")||f[0].equals("X"))throw new Gone();return f[19];
    }
    static Phase phase(Context c,String token){
        RuntimeSession r=read(c);if(r.phase!=Phase.PREPARING&&r.phase!=Phase.RUNNING)return r.observe(token,false);
        boolean same=false;
        try{same=start(r.pid).equals(r.start);}catch(NoSuchFileException|Gone exited){}catch(Exception unknown){return Phase.UNKNOWN;}
        return r.observe(token,same);
    }
    static synchronized void begin(Context c,String token)throws IOException{
        if(!RuntimeSession.validToken(token))throw new IllegalArgumentException("Invalid session");
        RuntimeSession previous=read(c);Phase phase=phase(c,previous.token);
        if(phase==Phase.RUNNING||phase==Phase.PREPARING||(phase==Phase.UNKNOWN&&file(c).getBaseFile().exists()))throw new IllegalStateException("A session is already active or unconfirmed");
        int pid=android.os.Process.myPid();String value=token+"\nPREPARING\n"+pid+"\n"+start(pid);
        AtomicFile file=file(c);FileOutputStream out=null;
        try{out=file.startWrite();out.write(value.getBytes("US-ASCII"));file.finishWrite(out);}catch(IOException e){file.failWrite(out);throw e;}
    }
    static synchronized void cancel(Context c,String token){
        RuntimeSession r=read(c);if(r.token.equals(token)&&r.phase==Phase.PREPARING)file(c).delete();
    }
}
