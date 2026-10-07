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
    static AtomicFile bootFile(Context c){return new AtomicFile(new File(c.getFilesDir(),"session-boot.txt"));}
    static String boot()throws IOException{return new String(Files.readAllBytes(Paths.get("/proc/sys/kernel/random/boot_id")),"US-ASCII").trim();}
    static String start(int pid)throws IOException{
        String stat=new String(Files.readAllBytes(Paths.get("/proc/"+pid+"/stat")),"US-ASCII");
        String[] f=stat.substring(stat.lastIndexOf(')')+2).split(" ");
        if(f[0].equals("Z")||f[0].equals("X"))throw new Gone();return f[19];
    }
    static Phase phase(Context c,String token){
        if(RuntimeSession.validToken(token)){
            File recovered=new File(c.getFilesDir(),"recovered-sessions/"+token);
            try{if(new String(Files.readAllBytes(recovered.toPath()),"US-ASCII").equals("INTERRUPTED"))return Phase.INTERRUPTED;}catch(IOException absent){}
        }
        RuntimeSession r=read(c);if(r.phase!=Phase.PREPARING&&r.phase!=Phase.RUNNING)return r.observe(token,false);
        try{if(!art.pikoos.lab.core.RuntimeBootIdentity.matches(new String(bootFile(c).readFully(),"US-ASCII"),r.token,boot()))return Phase.UNKNOWN;}catch(Exception unknown){return Phase.UNKNOWN;}
        boolean same=false;
        try{same=start(r.pid).equals(r.start);}catch(NoSuchFileException|Gone exited){}catch(Exception unknown){return Phase.UNKNOWN;}
        if(!same)try{if(RuntimeControls.gameOpen()||!idle(c,android.os.Binder.getCallingPid()))return Phase.UNKNOWN;}catch(IOException unknown){return Phase.UNKNOWN;}
        return r.observe(token,same);
    }
    static synchronized void begin(Context c,String token)throws IOException{
        if(!RuntimeSession.validToken(token))throw new IllegalArgumentException("Invalid session");
        RuntimeSession previous=read(c);Phase phase=phase(c,previous.token);
        if(phase==Phase.RUNNING||phase==Phase.PREPARING||(phase==Phase.UNKNOWN&&file(c).getBaseFile().exists()))throw new IllegalStateException("A session is already active or unconfirmed");
        int pid=android.os.Process.myPid();String value=token+"\nPREPARING\n"+pid+"\n"+start(pid);
        AtomicFile stamp=bootFile(c);FileOutputStream identity=null;
        try{identity=stamp.startWrite();identity.write(art.pikoos.lab.core.RuntimeBootIdentity.record(token,boot()).getBytes("US-ASCII"));stamp.finishWrite(identity);}catch(IOException e){stamp.failWrite(identity);throw e;}
        AtomicFile file=file(c);FileOutputStream out=null;
        try{out=file.startWrite();out.write(value.getBytes("US-ASCII"));file.finishWrite(out);}catch(IOException e){file.failWrite(out);throw e;}
    }
    static synchronized void cancel(Context c,String token){
        RuntimeSession r=read(c);if(r.token.equals(token)&&r.phase==Phase.PREPARING)file(c).delete();
    }
    /** Explicit recovery only: preserve all journals, never kill a process or claim game success. */
    static synchronized boolean recover(Context c,String token,int caller)throws IOException{
        if(!RuntimeSession.validToken(token)||RuntimeControls.gameOpen())return false;
        RuntimeSession current=read(c);Phase latest=phase(c,current.token);
        if(latest==Phase.RUNNING||latest==Phase.PREPARING)return false;
        if(!idle(c,caller))return false;
        File dir=new File(c.getFilesDir(),"recovered-sessions");if(!dir.isDirectory()&&!dir.mkdir())throw new IOException("Cannot preserve recovery evidence");
        File[] previous=dir.listFiles();if(previous==null||previous.length>=64)return false;
        AtomicFile record=new AtomicFile(new File(dir,token));FileOutputStream out=null;
        try{out=record.startWrite();out.write("INTERRUPTED".getBytes("US-ASCII"));record.finishWrite(out);}catch(IOException e){record.failWrite(out);throw e;}
        return true;
    }
    static boolean idle(Context c,int caller)throws IOException{
        java.lang.Process scanner=new ProcessBuilder("/system/bin/ps","-A","-n","-o","UID,PID,NAME").redirectErrorStream(true).start();
        java.util.concurrent.FutureTask<byte[]> reader=new java.util.concurrent.FutureTask<>(()->{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(InputStream in=scanner.getInputStream()){byte[] b=new byte[4096];for(int n;(n=in.read(b))!=-1;){if(bytes.size()+n>1024*1024)throw new IOException("Process census is too large");bytes.write(b,0,n);}}
            return bytes.toByteArray();
        });
        Thread drain=new Thread(reader,"pikonest-process-census");drain.setDaemon(true);drain.start();String census;
        try{
            if(!scanner.waitFor(2,java.util.concurrent.TimeUnit.SECONDS)||scanner.exitValue()!=0)return false;
            census=new String(reader.get(500,java.util.concurrent.TimeUnit.MILLISECONDS),"US-ASCII");
        }catch(InterruptedException e){Thread.currentThread().interrupt();return false;}
        catch(java.util.concurrent.ExecutionException|java.util.concurrent.TimeoutException e){return false;}
        finally{scanner.destroyForcibly();try{scanner.getInputStream().close();}catch(IOException ignored){}reader.cancel(true);}
        // Android Java 8 has no Process.pid(); the platform exposes the spawned PID privately.
        // Obtain it from the census: one unique ps owned by this UID, otherwise refuse.
        int pid=0;for(String line:census.split("\n")){
            String[] f=line.trim().split("\\s+",3);
            if(f.length==3&&f[0].equals(Integer.toString(android.os.Process.myUid()))&&f[2].equals("ps")){
                if(pid!=0)return false;try{pid=Integer.parseInt(f[1]);}catch(NumberFormatException bad){return false;}
            }
        }
        return art.pikoos.lab.core.RuntimeRecovery.idle(census,android.os.Process.myUid(),android.os.Process.myPid(),caller,pid);
    }
}
