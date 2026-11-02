package art.pikoos.runtimelab;

import android.content.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.AtomicFile;
import art.pikoos.lab.core.*;
import java.io.*;
import java.util.*;
import org.json.JSONObject;

/** Ordinary external game.p8 files, staged creation and journaled retained-version saves. */
final class SafProjectStore implements ProjectRepository.Storage {
    final StorageCopy.Tree tree;final Context context;final String key;
    SafProjectStore(Context c,String uri,String root)throws Exception{
        context=c.getApplicationContext();tree=new StorageCopy.Tree(context.getContentResolver(),Uri.parse(uri),root);
        key=RecoverableReplace.hash((uri+"\n"+root).getBytes("UTF-8"));
    }
    private String base(String id){if(!LibrarySession.validId(id))throw new IllegalArgumentException("Invalid project address");return "projects/"+id+"/";}
    private byte[] bytes(String path)throws Exception{try(InputStream in=tree.read(path)){return CartridgeImport.readBounded(in);}}
    private void create(String path,byte[] bytes)throws Exception{
        try(OutputStream out=tree.create(path)){if(out==null)throw new IOException("Cannot open project output");out.write(bytes);}
        if(!Arrays.equals(bytes,bytes(path)))throw new IOException("Could not verify project file. Previous data kept.");
    }
    public List<String> ids()throws Exception{
        synchronized(SafProjectStore.class){
            Uri dir=tree.resolve("projects",false);if(dir==null)throw new IOException("Project folder missing. Reconnect storage.");
            List<String> result=new ArrayList<>();Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree.root,DocumentsContract.getDocumentId(dir));
            try(android.database.Cursor c=tree.resolver.query(children,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){
                if(c==null)throw new IOException("Cannot read external Projects");
                while(c.moveToNext())if(LibrarySession.validId(c.getString(0))&&DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(1)))result.add(c.getString(0));
            }return result;
        }
    }
    public boolean exists(String id)throws Exception{synchronized(SafProjectStore.class){return tree.exists(base(id).substring(0,base(id).length()-1));}}
    public byte[] read(String id)throws Exception{synchronized(SafProjectStore.class){RecoverableReplace.recover(port(id));return bytes(base(id)+"game.p8");}}
    public String importedTitle(String id)throws Exception{return new String(bytes(base(id)+".pikoos/import-name"),"UTF-8");}
    public void publish(String id,byte[] data,String title)throws Exception{if(BackupActivity.inProgress())throw new IOException("Finish backup or restore before editing.");
        synchronized(SafProjectStore.class){
            if(exists(id))throw new IOException("Project already exists");
            String stage="projects/.creating-"+UUID.randomUUID();tree.directory(stage);
            create(stage+"/game.p8",data);tree.renameSupported(tree.resolve(stage+"/game.p8",false));
            if(title!=null){tree.directory(stage+"/.pikoos");create(stage+"/.pikoos/import-name",title.getBytes("UTF-8"));}
            tree.rename(stage,"projects/"+id);
        }
    }
    public void replace(String id,byte[] expected,byte[] next)throws Exception{if(BackupActivity.inProgress())throw new IOException("Finish backup or restore before editing.");
        synchronized(SafProjectStore.class){
            try{RecoverableReplace.replace(port(id),expected,next);}
            catch(Exception e){throw new IOException("External save interrupted. Reopen this project to recover; versions were kept. "+e.getMessage(),e);}
        }
    }
    private RecoverableReplace.Port port(String id)throws Exception{
        String prefix=base(id);File dir=new File(context.getFilesDir(),"project-save-journals/"+key);
        if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot open save recovery storage");
        AtomicFile journal=new AtomicFile(new File(dir,id+".json"));
        return new RecoverableReplace.Port(){
            public boolean exists(String name)throws Exception{return tree.exists(prefix+name);}
            public byte[] read(String name)throws Exception{return bytes(prefix+name);}
            public void create(String name,byte[] bytes)throws Exception{SafProjectStore.this.create(prefix+name,bytes);tree.renameSupported(tree.resolve(prefix+name,false));}
            public void rename(String from,String to)throws Exception{tree.rename(prefix+from,prefix+to);}
            public RecoverableReplace.Record pending()throws Exception{
                byte[] data;try{data=journal.readFully();}catch(FileNotFoundException absent){return null;}
                if(data.length==0)return null;JSONObject json=new JSONObject(new String(data,"UTF-8"));
                return new RecoverableReplace.Record(json.getString("token"),json.getString("old"),json.getString("next"));
            }
            private void write(byte[] data)throws Exception{FileOutputStream out=null;try{out=journal.startWrite();out.write(data);journal.finishWrite(out);}catch(Exception e){journal.failWrite(out);throw e;}if(!Arrays.equals(data,journal.readFully()))throw new IOException("Cannot verify save journal");}
            public void journal(RecoverableReplace.Record r)throws Exception{write(new JSONObject().put("token",r.token).put("old",r.oldHash).put("next",r.nextHash).toString().getBytes("UTF-8"));}
            public void clear()throws Exception{write(new byte[0]);}
        };
    }
}
