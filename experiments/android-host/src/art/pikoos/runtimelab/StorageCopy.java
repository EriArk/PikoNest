package art.pikoos.runtimelab;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import art.pikoos.lab.core.VerifiedTransfer;
import java.io.*;
import java.util.*;

/** SAF remains an opaque document tree; local canonical paths are never inferred from it. */
final class StorageCopy {
    static VerifiedTransfer.Source source(File files,boolean projects){
        return new VerifiedTransfer.Source(){
            private File safe(String path)throws Exception{
                VerifiedTransfer.path(path);File f=new File(files.getCanonicalFile(),path);
                if(!f.getCanonicalFile().equals(f.getAbsoluteFile()))throw new IOException("Linked source path cannot be copied: "+path);
                return f;
            }
            private void walk(File file,String path,List<String> result)throws Exception{
                safe(path);
                if(file.isDirectory()){
                    File[] children=file.listFiles();if(children==null)throw new IOException("Cannot list "+path);
                    for(File child:children)walk(child,path+"/"+child.getName(),result);
                }else if(file.isFile())result.add(path);else throw new IOException("Source unavailable: "+path);
            }
            public List<String> files()throws Exception{
                List<String> result=new ArrayList<>();
                for(String name:projects?new String[]{"projects"}:new String[]{"asset-library","sprite-buffer.pksp"}){
                    File f=new File(files.getCanonicalFile(),name);if(f.exists())walk(f,name,result);
                }return result;
            }
            public InputStream open(String path)throws Exception{return new FileInputStream(safe(path));}
        };
    }
    static final class Tree implements VerifiedTransfer.Target {
        final ContentResolver resolver;final Uri root;
        Tree(ContentResolver resolver,Uri tree,String rootId){this.resolver=resolver;root=DocumentsContract.buildDocumentUriUsingTree(tree,rootId);}
        private Uri find(Uri parent,String name)throws Exception{
            Uri query=DocumentsContract.buildChildDocumentsUriUsingTree(root,DocumentsContract.getDocumentId(parent));Uri found=null;
            try(Cursor c=resolver.query(query,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
                if(c==null)throw new IOException("Cannot list destination");
                while(c.moveToNext())if(name.equals(c.getString(1))){if(found!=null)throw new IOException("Duplicate destination name");found=DocumentsContract.buildDocumentUriUsingTree(root,c.getString(0));}
            }return found;
        }
        private Uri resolve(String path,boolean folders)throws Exception{
            VerifiedTransfer.path(path);Uri at=root;String[] bits=path.split("/");
            for(int i=0;i<bits.length;i++){
                Uri next=find(at,bits[i]);
                if(next==null&&folders&&i<bits.length-1)next=DocumentsContract.createDocument(resolver,at,DocumentsContract.Document.MIME_TYPE_DIR,bits[i]);
                if(next==null)return null;at=next;
            }return at;
        }
        public boolean exists(String path)throws Exception{return resolve(path,false)!=null;}
        public InputStream read(String path)throws Exception{Uri uri=resolve(path,false);if(uri==null)throw new IOException("Copy missing: "+path);return resolver.openInputStream(uri);}
        public OutputStream create(String path)throws Exception{
            if(exists(path))throw new IOException("Copy already exists");
            int slash=path.lastIndexOf('/');Uri parent=root;
            if(slash>=0){resolve(path,true);parent=resolve(path.substring(0,slash),false);}
            Uri file=DocumentsContract.createDocument(resolver,parent,"application/octet-stream",path.substring(slash+1));
            if(file==null)throw new IOException("Cannot create destination file");return resolver.openOutputStream(file,"w");
        }
    }
}
