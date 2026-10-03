package art.pikoos.runtimelab;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.DocumentsContract;
import art.pikoos.lab.core.*;
import java.util.function.BooleanSupplier;

/** Resolves sibling/descendant includes through the user's existing Games tree grant. */
final class IncludeSource implements PicoIncludes.Source {
    private final ContentResolver resolver;private final Uri tree;private final String parent;
    private final BooleanSupplier cancelled;private int examined;
    private IncludeSource(ContentResolver resolver,Uri tree,String parent,BooleanSupplier cancelled){
        this.resolver=resolver;this.tree=tree;this.parent=parent;this.cancelled=cancelled;
    }
    static byte[] prepare(ContentResolver resolver,String location,Uri selected,String name,byte[] bytes,BooleanSupplier cancelled)throws Exception{
        PlayCartridge checked=new PlayCartridge(name,bytes,true);
        if(!checked.problem.isEmpty())throw new IllegalArgumentException(checked.problem);
        if(CartridgeFormat.of(name)!=CartridgeFormat.P8||!PicoIncludes.needed(bytes))return bytes;
        if(location.isEmpty())throw new IllegalArgumentException("Для #include подключи папку игры через Папки → Игры");
        Uri tree=Uri.parse(location);FolderAccess.verify(resolver,tree,false);
        String selectedId;
        try{
            if(!tree.getAuthority().equals(selected.getAuthority()))throw new IllegalArgumentException();
            selectedId=DocumentsContract.getDocumentId(selected);
        }catch(Exception e){throw new IllegalArgumentException("Для #include открой игру из подключённой папки Игры");}
        GameTree.Result index=GameTree.scan(id->GameFolder.children(resolver,tree,id),DocumentsContract.getTreeDocumentId(tree),cancelled);
        String parent=null;
        for(GameTree.Cart cart:index.carts)if(cart.id.equals(selectedId)){parent=cart.parent;break;}
        if(parent==null)throw new IllegalArgumentException("Игра с #include не найдена в доступной части папки Игры");
        byte[] ready=PicoIncludes.prepare(bytes,new IncludeSource(resolver,tree,parent,cancelled));
        GameTree.check(cancelled);
        checked=new PlayCartridge(name,ready);
        if(!checked.problem.isEmpty())throw new IllegalArgumentException(checked.problem);
        return ready;
    }
    @Override public byte[] read(String path)throws Exception{
        // Validate even if this adapter is reused independently of PicoIncludes.
        if(!PicoIncludes.path(path).equals(path))throw new IllegalArgumentException("Нужен путь файла #include");
        String directory=parent;String[] parts=path.split("/");
        for(int i=0;i<parts.length;i++){
            GameTree.check(cancelled);GameTree.Entry found=null;
            try(GameTree.Children rows=GameFolder.children(resolver,tree,directory)){
                for(GameTree.Entry entry; (entry=rows.next())!=null;){
                    GameTree.check(cancelled);
                    if(++examined>GameTree.MAX_ENTRIES)throw new IllegalArgumentException("Лимит поиска #include · выбери меньшую папку игры");
                    if(parts[i].equals(entry.name)){
                        if(found!=null)throw new IllegalArgumentException("Два файла с одним именем #include: "+parts[i]);
                        found=entry;
                    }
                }
            }
            if(found==null)throw new IllegalArgumentException("Нет файла #include: "+path);
            if(found.id==null||found.directory!=(i<parts.length-1))throw new IllegalArgumentException("Неверный путь #include: "+path);
            directory=found.id;
        }
        GameTree.check(cancelled);
        try{return GameFolder.read(resolver,DocumentsContract.buildDocumentUriUsingTree(tree,directory).toString());}
        catch(Exception e){throw new IllegalArgumentException("Не удалось прочитать #include: "+path);}
    }
}
