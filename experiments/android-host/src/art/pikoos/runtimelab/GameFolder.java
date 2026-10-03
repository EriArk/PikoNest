package art.pikoos.runtimelab;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import art.pikoos.lab.core.*;
import java.io.InputStream;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Bounded, read-only SAF listing. No native paths and no ProjectStore calls. */
final class GameFolder {
    static final class Listing {final List<PlaySession.Game> games=new ArrayList<>();int folders,unreadable;boolean limited;}
    static Listing list(ContentResolver resolver,String location,SharedPreferences prefs,BooleanSupplier cancelled)throws Exception{
        Uri tree=Uri.parse(location);FolderAccess.verify(resolver,tree,false);
        Listing result=new Listing();
        GameTree.Result index=GameTree.scan(directory->children(resolver,tree,directory),DocumentsContract.getTreeDocumentId(tree),cancelled);
        result.folders=index.folders;result.unreadable=index.unreadable;result.limited=index.limited;
        for(GameTree.Cart entry:index.carts){
                GameTree.check(cancelled);
                String name=entry.name,lower=name.toLowerCase(Locale.ROOT);
                String id=DocumentsContract.buildDocumentUriUsingTree(tree,entry.id).toString();
                String issue="";int[] cover=null;
                try{PlayCartridge cart=new PlayCartridge(name,read(resolver,id),true);issue=cart.problem;cover=cart.cover;}
                catch(Exception e){issue="Файл недоступен или превышает лимит полки 2 МиБ";}
                String title=name.substring(0,name.length()-(lower.endsWith(".p8.png")?7:3));
                result.games.add(new PlaySession.Game(id,title.replaceAll("\\p{Cntrl}"," "),issue,cover,prefs.getBoolean("favorite:"+id,false),prefs.getLong("recent:"+id,0),CartridgeFormat.of(name),entry.folder.replaceAll("\\p{Cntrl}"," ")));
        }
        GameTree.check(cancelled);return result;
    }
    static byte[] read(ContentResolver resolver,String id)throws Exception{
        try(InputStream in=resolver.openInputStream(Uri.parse(id))){return CartridgeImport.readBounded(in);}
    }
    static GameTree.Children children(ContentResolver resolver,Uri tree,String directory)throws Exception{
        Uri uri=DocumentsContract.buildChildDocumentsUriUsingTree(tree,directory);
        Cursor c=resolver.query(uri,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null);
        if(c==null)throw new Exception("Не удалось прочитать папку игр");
        return new GameTree.Children(){
            public GameTree.Entry next(){return c.moveToNext()?new GameTree.Entry(c.getString(0),c.getString(1),DocumentsContract.Document.MIME_TYPE_DIR.equals(c.getString(2))):null;}
            public void close(){c.close();}
        };
    }
}
