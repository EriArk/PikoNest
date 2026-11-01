package art.pikoos.runtimelab;

import android.content.ContentResolver;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** SAF adapter: verifies a durable grant, directory listing and (when needed) an owned probe. */
final class FolderAccess {
    static String verify(ContentResolver resolver,Uri tree,boolean writable)throws Exception{
        boolean granted=false;
        for(UriPermission p:resolver.getPersistedUriPermissions())
            if(p.getUri().equals(tree)&&p.isReadPermission()&&(!writable||p.isWritePermission()))granted=true;
        if(!granted)throw new Exception("Access was not saved. Choose the folder again.");
        Uri directory=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
        String name;
        try(Cursor c=resolver.query(directory,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){
            if(c==null||!c.moveToFirst()||!DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(1)))throw new Exception("Folder unavailable. Reconnect storage or choose another.");
            name=c.getString(0);
        }
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
        try(Cursor c=resolver.query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},null,null,null)){
            if(c==null)throw new Exception("Cannot read this folder.");c.moveToFirst();
        }
        if(writable){
            Uri probe=null;
            try{
                probe=DocumentsContract.createDocument(resolver,directory,"application/octet-stream","pikoos-access-check-"+UUID.randomUUID()+".tmp");
                if(probe==null)throw new Exception("Cannot create an access-check file.");
                byte[] bytes="PIKOOS folder access check\n".getBytes(StandardCharsets.UTF_8);
                try(OutputStream out=resolver.openOutputStream(probe,"wt")){
                    if(out==null)throw new Exception("Write access is unavailable.");out.write(bytes);
                }
                try(InputStream in=resolver.openInputStream(probe)){
                    if(in==null)throw new Exception("Cannot verify the saved file.");
                    for(byte b:bytes)if(in.read()!=(b&255))throw new Exception("Access-check file contents differ.");
                    if(in.read()!=-1)throw new Exception("Access-check file contents differ.");
                }
            }finally{
                if(probe!=null&&!DocumentsContract.deleteDocument(resolver,probe))
                    throw new Exception("The access-check .tmp file remains in this folder. Deletion was not confirmed.");
            }
        }
        if("com.android.externalstorage.documents".equals(tree.getAuthority())){
            String id=DocumentsContract.getTreeDocumentId(tree);int colon=id.indexOf(':');
            if(colon>=0){String volume=id.substring(0,colon);name=(volume.equals("primary")?"":volume+" / ")+id.substring(colon+1).replace("/"," / ");}
        }
        return name==null?"Folder":name;
    }
}
