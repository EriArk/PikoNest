package art.pikoos.runtimelab;

import android.content.Context;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.os.Handler;
import android.os.Looper;
import android.util.AtomicFile;
import art.pikoos.lab.core.RuntimeArchive;
import art.pikoos.lab.core.RuntimeProbe;
import java.io.*;
import java.util.UUID;
import org.json.JSONObject;

/** One application-owned worker, independent of Activity recreation. Never touches runtime files. */
final class RuntimeArchiveJob {
    interface Listener {void changed(RuntimeArchiveJob job);}
    static RuntimeArchiveJob current;
    static final class Saved {
        final String file,name,hash;final long bytes;
        Saved(String file,String name,String hash,long bytes){this.file=file;this.name=name;this.hash=hash;this.bytes=bytes;}
    }
    private final Context context;private final Uri uri;private final String displayName;final boolean recheck;boolean probe,dispatched;
    private final Handler ui=new Handler(Looper.getMainLooper());private Listener listener;
    volatile boolean done,cancelled;volatile String phase="Читаем архив…",error="";volatile Saved result;
    private RuntimeArchiveJob(Context c,Uri uri,String name){context=c.getApplicationContext();this.uri=uri;displayName=name;recheck=uri==null;}
    static File directory(Context c){return new File(c.getFilesDir(),"runtime-archives");}
    static AtomicFile journal(Context c){return new AtomicFile(new File(c.getFilesDir(),"runtime-archive.json"));}
    static Saved saved(Context c)throws Exception{
        AtomicFile journal=journal(c);if(!journal.getBaseFile().exists()&&!new File(journal.getBaseFile()+".bak").exists())return null;
        JSONObject value=new JSONObject(new String(journal.readFully(),"UTF-8"));
        String file=value.getString("file"),hash=value.getString("sha256");long bytes=value.getLong("bytes");
        if(!file.matches("[0-9a-f-]{36}\\.zip")||!hash.matches("[0-9a-f]{64}")||bytes<1||bytes>RuntimeArchive.MAX_ZIP)throw new IOException("Запись об архиве недоступна. Выбери архив снова.");
        return new Saved(file,value.getString("name"),hash,bytes);
    }
    static RuntimeArchiveJob start(Context c,Uri uri,String name){
        if(current!=null&&!current.done)return current;
        RuntimeArchiveJob job=new RuntimeArchiveJob(c,uri,name);current=job;
        new Thread(job::run,"pikoos-runtime-archive").start();return job;
    }
    static RuntimeArchiveJob prepareProbe(Context c){
        if(current!=null&&!current.done)return current;
        RuntimeArchiveJob job=new RuntimeArchiveJob(c,null,"");job.probe=true;current=job;
        new Thread(job::run,"pikoos-runtime-prepare").start();return job;
    }
    void attach(Listener next){listener=next;next.changed(this);}
    void detach(Listener old){if(listener==old)listener=null;}
    synchronized void cancel(){if(!done){cancelled=true;phase="Отменяем проверку…";notifyUi();}}
    private void notifyUi(){ui.post(()->{if(listener!=null)listener.changed(this);});}
    private void phase(String text){phase=text;notifyUi();}
    private void run(){
        File temporary=null;Saved previous=null;boolean readableJournal=true;
        try{
            try{previous=saved(context);}catch(Exception ignored){readableJournal=false;/* Preserve candidate files if their journal cannot be read. */}
            File root=directory(context);if(!root.isDirectory()&&!root.mkdir())throw new IOException("Не удалось создать место для архива");
            // Only app-created orphan snapshots, never arbitrary paths or directories. No active competing job.
            File[] orphans=root.listFiles();if(readableJournal&&orphans!=null)for(File f:orphans){
                if(f.isFile()&&f.getName().matches("[0-9a-f-]{36}\\.zip")&&(previous==null||!f.getName().equals(previous.file)))f.delete();
            }
            RuntimeArchive.check(()->cancelled);
            File file;String name=displayName;
            if(recheck){
                if(previous==null)throw new IOException("Выбери архив PICO-8 для проверки");
                file=new File(root,previous.file);
                if(!file.isFile()||file.length()!=previous.bytes)throw new IOException("Сохранённая копия архива недоступна. Выбери ZIP снова.");
            }else{
                // Providers can be remote: metadata queries also belong off the UI thread.
                try(Cursor cursor=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
                    if(cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0))name=cursor.getString(0);
                }catch(Exception ignored){}
                name=name.replaceAll("[\\p{Cntrl}]"," ");if(name.length()>160)name=name.substring(0,160);
                RuntimeArchive.check(()->cancelled);
                file=temporary=new File(root,UUID.randomUUID().toString()+".zip");
                if(!file.createNewFile())throw new IOException("Не удалось подготовить копию архива");
                try(InputStream input=context.getContentResolver().openInputStream(uri);FileOutputStream output=new FileOutputStream(file)){
                    RuntimeArchive.copy(input,output,()->cancelled);output.getFD().sync();
                }
            }
            phase("Проверяем ZIP и ARM64…");
            RuntimeArchive.Result inspected=RuntimeArchive.inspect(file,()->cancelled);
            if(recheck&&!inspected.sha256.equals(previous.hash))throw new IOException("Сохранённый архив изменился. Выбери исходный ZIP снова.");
            if(probe){
                phase("Готовим пробный запуск…");byte[] cart;
                try(InputStream input=context.getAssets().open("runtime-probe.p8");ByteArrayOutputStream buffer=new ByteArrayOutputStream()){
                    byte[] bytes=new byte[4096];for(int n;(n=input.read(bytes))!=-1;)buffer.write(bytes,0,n);cart=buffer.toByteArray();
                }
                AtomicFile snapshot=new AtomicFile(new File(context.getFilesDir(),"runtime-probe.pikorun"));FileOutputStream stream=null;
                try{stream=snapshot.startWrite();RuntimeProbe.prepare(file,previous.hash,cart,stream,()->cancelled);
                    synchronized(this){RuntimeArchive.check(()->cancelled);snapshot.finishWrite(stream);}
                }catch(Exception error){snapshot.failWrite(stream);throw error;}
            }
            Saved candidate=recheck?previous:new Saved(file.getName(),name,inspected.sha256,inspected.bytes);
            synchronized(this){
                RuntimeArchive.check(()->cancelled);
                if(!recheck){
                    JSONObject value=new JSONObject().put("file",candidate.file).put("name",candidate.name).put("sha256",candidate.hash).put("bytes",candidate.bytes);
                    AtomicFile journal=journal(context);FileOutputStream output=null;
                    try{output=journal.startWrite();output.write(value.toString().getBytes("UTF-8"));journal.finishWrite(output);}
                    catch(Exception failure){journal.failWrite(output);throw failure;}
                    temporary=null;
                }
                result=candidate;done=true;
            }
            if(!recheck&&previous!=null&&!previous.file.equals(candidate.file))new File(root,previous.file).delete();
        }catch(java.util.concurrent.CancellationException ignored){cancelled=true;}
        catch(Exception failure){error=failure.getMessage()==null?"Не удалось проверить архив. Попробуй выбрать его снова.":failure.getMessage();}
        finally{if(temporary!=null)temporary.delete();done=true;notifyUi();}
    }
}
