package art.pikoos.runtimelab;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.AtomicFile;
import art.pikoos.lab.core.CartridgeExport;
import java.io.*;

/** A single user-authorized document write, retained across Activity recreation. */
final class ExportJob {
    interface Listener {void finished(CartridgeExport result);}
    final CartridgeExport draft;
    private final ContentResolver resolver;
    private final Uri target;
    private final AtomicFile journal;
    private final Handler main=new Handler(Looper.getMainLooper());
    private Listener listener;
    private volatile CartridgeExport result;
    ExportJob(ContentResolver resolver,Uri target,AtomicFile journal,CartridgeExport draft){
        this.resolver=resolver;this.target=target;this.journal=journal;this.draft=draft;
    }
    void attach(Listener next){listener=next;if(result!=null)deliver();}
    void detach(Listener old){if(listener==old)listener=null;}
    private void deliver(){if(listener!=null)listener.finished(result);}
    void start(){new Thread(()->{
        String name=draft.filename;CartridgeExport outcome;
        try{
            try(OutputStream out=resolver.openOutputStream(target,"wt")){draft.writeTo(out);}
            try(InputStream in=resolver.openInputStream(target)){draft.verify(in);}
            // Providers may rename collisions; showing their actual filename is best effort.
            try(Cursor c=resolver.query(target,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
                if(c!=null&&c.moveToFirst()&&!c.isNull(0))name=c.getString(0).replaceAll("\\p{Cntrl}"," ");
            }catch(Exception ignored){}
            if(name.length()>160)name=name.substring(0,160);
            outcome=draft.withState(CartridgeExport.State.SAVED,name);
            persist(outcome);
        }catch(Exception e){
            android.util.Log.e("PikoRuntimeLab","Export write or verification incomplete",e);
            outcome=draft.withState(CartridgeExport.State.UNCERTAIN,name);
            try{persist(outcome);}catch(Exception storageError){android.util.Log.e("PikoRuntimeLab","Export journal retained",storageError);}
        }
        result=outcome;main.post(()->deliver());
    },"pikoos-cart-export").start();}
    private void persist(CartridgeExport value)throws Exception{
        FileOutputStream out=null;
        try{out=journal.startWrite();out.write(value.encode());journal.finishWrite(out);}
        catch(Exception e){journal.failWrite(out);throw e;}
    }
}
