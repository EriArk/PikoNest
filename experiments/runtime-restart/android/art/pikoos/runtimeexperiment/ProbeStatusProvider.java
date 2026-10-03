package art.pikoos.runtimeexperiment;
import android.content.*;
import android.database.*;
import android.net.Uri;
import java.io.*;
/** Signature-protected lifecycle observation, never a claim that the game rendered correctly. */
public final class ProbeStatusProvider extends ContentProvider {
    public boolean onCreate(){RuntimeControls.install((android.app.Application)getContext().getApplicationContext());return true;}
    static void write(Context c,String phase)throws IOException{
        android.util.AtomicFile file=new android.util.AtomicFile(new File(c.getFilesDir(),"probe-status.txt"));FileOutputStream out=null;
        String state=phase;
        if(phase.equals("PREPARING")||phase.equals("READY")){int pid=android.os.Process.myPid();state+="\n"+pid+"\n"+start(pid);}
        try{out=file.startWrite();out.write(state.getBytes("US-ASCII"));file.finishWrite(out);}catch(IOException e){file.failWrite(out);throw e;}
    }
    private static String start(int pid)throws IOException{
        String stat=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("/proc/"+pid+"/stat")),"US-ASCII");
        String[] fields=stat.substring(stat.lastIndexOf(')')+2).split(" ");
        if(fields[0].equals("Z")||fields[0].equals("X"))throw new IOException("Process exited");
        return fields[19]; // Field 22: start time. PID reuse must not revive a stale test.
    }
    public Cursor query(Uri u,String[] p,String s,String[] a,String order){
        if("/session".equals(u.getPath())){
            MatrixCursor rows=new MatrixCursor(new String[]{"phase"});
            rows.addRow(new Object[]{SessionStatus.phase(getContext(),u.getQueryParameter("token")).name()});return rows;
        }
        String phase="UNKNOWN";try{
            byte[] bytes=new android.util.AtomicFile(new File(getContext().getFilesDir(),"probe-status.txt")).readFully();
            if(bytes.length<128){String[] state=new String(bytes,"US-ASCII").split("\n");phase=state[0];
                if(phase.matches("PREPARING|READY|RUNNING")&&state.length==3){
                    try{if(!start(Integer.parseInt(state[1])).equals(state[2]))phase="FAILED";}catch(Exception dead){phase="FAILED";}
                }
            }
        }catch(Exception ignored){}
        if(!phase.matches("PREPARING|READY|RUNNING|EXITED|FAILED"))phase="UNKNOWN";
        MatrixCursor rows=new MatrixCursor(new String[]{"phase"});rows.addRow(new Object[]{phase});return rows;
    }
    public String getType(Uri u){return "vnd.android.cursor.item/vnd.pikoos.probe";}
    @Override public android.os.Bundle call(String method,String token,android.os.Bundle extras){
        getContext().enforceCallingPermission("art.pikoos.runtimeexperiment.PROBE","Session handshake requires the host signature");
        try{
            if("beginSession".equals(method))SessionStatus.begin(getContext(),token);
            else if("cancelSession".equals(method))SessionStatus.cancel(getContext(),token);
            else throw new IllegalArgumentException("Unknown operation");
            android.os.Bundle result=new android.os.Bundle();result.putBoolean("ok",true);return result;
        }catch(java.io.IOException e){throw new IllegalStateException("Cannot persist session",e);}
    }
    public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
