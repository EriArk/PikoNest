package art.pikoos.runtimelab;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import art.pikoos.lab.core.RuntimeHomeMigration;
import art.pikoos.lab.core.RuntimeArchive;
import java.io.*;
import java.security.*;
import java.util.*;

/** SAF copy/review/activation worker. No writes to the selected source folder. */
final class RuntimeMigrationJob {
    interface Listener{void changed();}
    static RuntimeMigrationJob current;
    static boolean pending(){return current!=null&&!current.finished&&!current.cancelled;}
    final Context context;final Uri tree;final Handler ui=new Handler(Looper.getMainLooper());
    volatile boolean busy=true,finished,cancelled,ready,applied;volatile String phase="Reading your old data...",error="";
    volatile RuntimeHomeMigration.Plan plan;File source,oldHome;Listener listener;
    private RuntimeMigrationJob(Context c,Uri tree){context=c.getApplicationContext();this.tree=tree;}
    static RuntimeMigrationJob start(Context c,Uri tree){
        if(pending())return current;
        RuntimeMigrationJob job=new RuntimeMigrationJob(c,tree);current=job;new Thread(job::read,"pikonest-runtime-data").start();return job;
    }
    void attach(Listener l){listener=l;l.changed();}void detach(Listener l){if(listener==l)listener=null;}
    void notifyUi(){ui.post(()->{if(listener!=null)listener.changed();});}
    synchronized void cancel(){if(!applied){cancelled=true;if(!busy)finished=true;phase="Cancelled / current data kept";notifyUi();}}
    synchronized void apply(boolean importedWins){
        if(!ready||busy||finished||cancelled)return;busy=true;ready=false;phase="Verifying and copying...";notifyUi();
        new Thread(()->{
            try{
                Map<String,RuntimeHomeMigration.Entry> actual=readSource(null);
                if(!actual.equals(plan.source))throw new IOException("Old data changed. Choose the folder again for a fresh review.");
                File root=new File(context.getFilesDir(),"rh");if(!root.isDirectory()&&!root.mkdir())throw new IOException("Cannot prepare runtime homes");
                String[] entries=root.list();if(entries==null||entries.length>=16)throw new IOException("Previous runtime homes need storage review; no data was removed");
                File candidate=new File(root,id());
                RuntimeHomeMigration.prepare(oldHome,source,candidate,plan,importedWins,()->cancelled);
                if(!readSource(null).equals(plan.source))throw new IOException("Old data changed during import. Current data was kept.");
                for(String name:new String[]{"logs","data/carts","data/cdata","data/cstore","data/bbs","data/screenshots"}){
                    File dir=new File(candidate,name);if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot prepare runtime data folder");
                }
                synchronized(this){RuntimeArchive.check(()->cancelled);RuntimeHomeMigration.unchanged(oldHome,plan.current,()->cancelled);RuntimeHomes.activate(context,candidate);applied=true;finished=true;phase="Imported data is now active";}
            }catch(java.util.concurrent.CancellationException e){finished=true;}
            catch(Exception e){error=message(e);finished=true;}
            finally{busy=false;notifyUi();}
        },"pikonest-runtime-data-apply").start();
    }
    private static String id(){return UUID.randomUUID().toString().replace("-","").substring(0,12);}
    private static String message(Exception e){return e.getMessage()==null?"Cannot import runtime data; current data was kept":e.getMessage();}
    private void read(){
        try{
            File imports=new File(context.getFilesDir(),"runtime-imports");if(!imports.isDirectory()&&!imports.mkdir())throw new IOException("Cannot prepare import storage");
            String[] entries=imports.list();if(entries==null||entries.length>=16)throw new IOException("Previous imports need storage review; no data was removed");
            source=new File(imports,id());if(!source.mkdir())throw new IOException("Cannot prepare import copy");
            readSource(source);oldHome=RuntimeHomes.current(context);
            Map<String,RuntimeHomeMigration.Entry> sourceFiles=RuntimeHomeMigration.scan(source,()->cancelled);
            boolean known=sourceFiles.containsKey("config.txt");for(String name:sourceFiles.keySet())known|=name.startsWith("cdata/")||name.startsWith("cstore/")||name.startsWith("bbs/")||name.startsWith("carts/");
            if(!known)throw new IOException("Choose the old PICO-8 data folder (config.txt, carts, cdata or bbs), not its ZIP folder.");
            plan=new RuntimeHomeMigration.Plan(RuntimeHomeMigration.scan(oldHome,()->cancelled),sourceFiles);
            synchronized(this){RuntimeArchive.check(()->cancelled);ready=true;phase="Review your data copy";}
        }catch(java.util.concurrent.CancellationException e){finished=true;}
        catch(Exception e){error=message(e);finished=true;}
        finally{busy=false;notifyUi();}
    }
    private Map<String,RuntimeHomeMigration.Entry> readSource(File destination)throws Exception{
        Uri document=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
        Map<String,RuntimeHomeMigration.Entry> result=new TreeMap<>();Set<String> ids=new HashSet<>(),names=new HashSet<>();long[] bytes={0};
        visit(document,"",destination,result,ids,names,bytes,0);return result;
    }
    private void visit(Uri directory,String prefix,File destination,Map<String,RuntimeHomeMigration.Entry> result,Set<String> ids,Set<String> names,long[] bytes,int depth)throws Exception{
        RuntimeArchive.check(()->cancelled);if(depth>32)throw new IOException("Import folder is too deep");
        String dirId=DocumentsContract.getDocumentId(directory);if(!ids.add(dirId))throw new IOException("Repeated document folder needs review");
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,dirId);
        try(Cursor cursor=context.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){
            if(cursor==null)throw new IOException("Cannot read the selected data folder");
            while(cursor.moveToNext()){
                RuntimeArchive.check(()->cancelled);String name=cursor.getString(1);
                if(name==null||name.contains("/"))throw new IOException("Unsupported document name");
                String relative=RuntimeHomeMigration.path(prefix+name);
                if(!names.add(relative.toLowerCase(Locale.ROOT)))throw new IOException("Ambiguous import names: "+relative);
                Uri file=DocumentsContract.buildDocumentUriUsingTree(tree,cursor.getString(0));
                if(DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2))){visit(file,relative+"/",destination,result,ids,names,bytes,depth+1);continue;}
                if(!ids.add(cursor.getString(0))||result.size()>=RuntimeHomeMigration.MAX_FILES)throw new IOException("Import limit: 20,000 files / no repeated documents");
                File target=destination==null?null:new File(destination,relative);
                if(target!=null&&!target.getParentFile().isDirectory()&&!target.getParentFile().mkdirs())throw new IOException("Cannot prepare imported folders");
                MessageDigest hash=MessageDigest.getInstance("SHA-256");long size=0;
                try(InputStream in=context.getContentResolver().openInputStream(file);FileOutputStream out=target==null?null:new FileOutputStream(target)){
                    if(in==null)throw new IOException("Cannot open imported file: "+relative);
                    byte[] b=new byte[65536];for(int n;(n=in.read(b))!=-1;){RuntimeArchive.check(()->cancelled);size+=n;bytes[0]+=n;if(bytes[0]>RuntimeHomeMigration.MAX_BYTES)throw new IOException("Import limit: 512 MiB");hash.update(b,0,n);if(out!=null)out.write(b,0,n);}
                    if(out!=null)out.getFD().sync();
                }
                StringBuilder digest=new StringBuilder();for(byte b:hash.digest())digest.append(String.format(Locale.ROOT,"%02x",b&255));
                result.put(relative,new RuntimeHomeMigration.Entry(size,digest.toString()));
            }
        }
    }
}
