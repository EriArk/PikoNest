package art.pikoos.runtimelab;

import android.content.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.AtomicFile;
import art.pikoos.lab.core.*;
import java.io.*;
import java.util.*;
import org.json.JSONObject;

/** Encoded library resources with retained binary versions; never mistaken for cartridges. */
final class SafLibraryStorage implements LibraryRepository.Storage {
    final StorageCopy.Tree tree;final File journals;
    SafLibraryStorage(Context context,String uri,String root)throws Exception{
        tree=new StorageCopy.Tree(context.getContentResolver(),Uri.parse(uri),root);
        journals=new File(context.getFilesDir(),"library-save-journals/"+RecoverableReplace.hash((uri+"\n"+root).getBytes("UTF-8")));
        if(!journals.isDirectory()&&!journals.mkdirs())throw new IOException("Cannot open library recovery records");
    }
    private byte[] bytes(String path)throws Exception{try(InputStream in=tree.read(path)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];for(int n;(n=in.read(b))!=-1;){if(out.size()+n>20000)throw new IOException("Library resource too large");out.write(b,0,n);}return out.toByteArray();}}
    private RecoverableReplace.Port port(String path)throws Exception{
        LibraryRepository.path(path);int slash=path.lastIndexOf('/');String dir=slash<0?"":path.substring(0,slash+1),base=path.substring(slash+1);
        AtomicFile record=new AtomicFile(new File(journals,path.replace('/','_')+".json"));
        return new RecoverableReplace.Port(){
            private String physical(String name){return name.equals("game.p8")?path:dir+"."+base+name.substring(0,name.length()-3)+".bin";}
            public boolean exists(String name)throws Exception{return tree.exists(physical(name));}
            public byte[] read(String name)throws Exception{return bytes(physical(name));}
            public void create(String name,byte[] data)throws Exception{writeNew(physical(name),data);tree.renameSupported(tree.resolve(physical(name),false));}
            public void rename(String a,String b)throws Exception{tree.rename(physical(a),physical(b));}
            public RecoverableReplace.Record pending()throws Exception{
                byte[] b;try{b=record.readFully();}catch(FileNotFoundException e){return null;}if(b.length==0)return null;
                JSONObject json=new JSONObject(new String(b,"UTF-8"));return new RecoverableReplace.Record(json.getString("token"),json.getString("old"),json.getString("next"));
            }
            private void write(byte[] data)throws Exception{FileOutputStream out=null;try{out=record.startWrite();out.write(data);record.finishWrite(out);}catch(Exception e){record.failWrite(out);throw e;}if(!Arrays.equals(data,record.readFully()))throw new IOException("Cannot verify library recovery record");}
            public void journal(RecoverableReplace.Record r)throws Exception{write(new JSONObject().put("token",r.token).put("old",r.oldHash).put("next",r.nextHash).toString().getBytes("UTF-8"));}
            public void clear()throws Exception{write(new byte[0]);}
        };
    }
    private void recoverPending()throws Exception{
        File[] files=journals.listFiles();if(files==null)throw new IOException("Cannot list library recovery records");
        for(File f:files)if(f.getName().endsWith(".json")||f.getName().endsWith(".json.bak")){
            String name=f.getName();if(name.endsWith(".bak"))name=name.substring(0,name.length()-4);
            String path=name.substring(0,name.length()-5).replace('_','/');LibraryRepository.path(path);RecoverableReplace.recover(port(path));
        }
    }
    public List<String> list(String directory)throws Exception{
        synchronized(SafLibraryStorage.class){
            if(!directory.equals("asset-library/sprites")&&!directory.equals("asset-library/parameters"))throw new IOException("Invalid library section");
            recoverPending();List<String> paths=new ArrayList<>();Uri folder=tree.resolve(directory,false);if(folder==null)return paths;
            Uri query=DocumentsContract.buildChildDocumentsUriUsingTree(tree.root,DocumentsContract.getDocumentId(folder));String ext=directory.endsWith("sprites")?".pksp":".pkpr";
            try(android.database.Cursor c=tree.resolver.query(query,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
                if(c==null)throw new IOException("Cannot read external library");
                while(c.moveToNext()){String n=c.getString(0);if(n.endsWith(ext)){String path=directory+"/"+n;LibraryRepository.path(path);paths.add(path);}}
            }return paths;
        }
    }
    public byte[] read(String path)throws Exception{synchronized(SafLibraryStorage.class){LibraryRepository.path(path);RecoverableReplace.recover(port(path));return tree.exists(path)?bytes(path):null;}}
    private void writeNew(String path,byte[] data)throws Exception{try(OutputStream out=tree.create(path)){if(out==null)throw new IOException("Cannot open resource output");out.write(data);}if(!Arrays.equals(data,bytes(path)))throw new IOException("Could not verify resource candidate");}
    public void create(String path,byte[] data)throws Exception{
        synchronized(SafLibraryStorage.class){
            LibraryRepository.path(path);if(tree.exists(path))throw new IOException("Resource already exists");int slash=path.lastIndexOf('/');String dir=slash<0?"":path.substring(0,slash+1);
            if(slash>=0)tree.directory(path.substring(0,slash));String stage=dir+".creating-"+UUID.randomUUID()+".bin";writeNew(stage,data);tree.rename(stage,path);
        }
    }
    public void replace(String path,byte[] a,byte[] b)throws Exception{synchronized(SafLibraryStorage.class){try{RecoverableReplace.replace(port(path),a,b);}catch(Exception e){throw new IOException("Resource save interrupted. Reopen the library to recover. Versions kept. "+e.getMessage(),e);}}}
}
