package art.pikoos.runtimelab;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.DocumentsContract;
import art.pikoos.lab.core.*;
import java.util.function.BooleanSupplier;

/** Reads named linked files through the user's existing Games tree grant. */
final class IncludeSource implements PicoIncludes.Source {
    private final ContentResolver resolver;private final Uri tree;private final String parent;
    private final BooleanSupplier cancelled;private int examined;
    private final java.util.Map<String,byte[]> snapshots=new java.util.HashMap<>();
    private long snapshotBytes;
    private IncludeSource(ContentResolver resolver,Uri tree,String parent,BooleanSupplier cancelled){
        this.resolver=resolver;this.tree=tree;this.parent=parent;this.cancelled=cancelled;
    }
    static IncludeSource open(ContentResolver resolver,String location,Uri selected,BooleanSupplier cancelled)throws Exception{
        if(location.isEmpty())throw new IllegalArgumentException("Для связанных файлов подключи папку игры через Папки → Игры");
        Uri tree=Uri.parse(location);FolderAccess.verify(resolver,tree,false);
        String selectedId;
        try{
            if(!tree.getAuthority().equals(selected.getAuthority()))throw new IllegalArgumentException();
            selectedId=DocumentsContract.getDocumentId(selected);
        }catch(Exception e){throw new IllegalArgumentException("Открой игру из подключённой папки Игры");}
        GameTree.Result index=GameTree.scan(id->GameFolder.children(resolver,tree,id),DocumentsContract.getTreeDocumentId(tree),cancelled);
        for(GameTree.Cart cart:index.carts)if(cart.id.equals(selectedId))return new IncludeSource(resolver,tree,cart.parent,cancelled);
        throw new IllegalArgumentException("Игра не найдена в доступной части папки Игры");
    }
    static final class Prepared {
        final byte[] bytes;final RuntimeFileSet files;
        Prepared(byte[] bytes,RuntimeFileSet files){this.bytes=bytes;this.files=files;}
    }
    static Prepared launch(ContentResolver resolver,String location,Uri selected,String name,byte[] bytes,BooleanSupplier cancelled)throws Exception{
        if(CartridgeFormat.of(name)!=CartridgeFormat.P8){
            PlayCartridge checked=new PlayCartridge(name,bytes);
            if(!checked.problem.isEmpty())throw new IllegalArgumentException(checked.problem);
            return new Prepared(bytes,null);
        }
        byte[] original=bytes;
        IncludeSource source=null;
        if(PicoIncludes.needed(bytes)){source=open(resolver,location,selected,cancelled);bytes=PicoIncludes.prepare(bytes,source);}
        if(!RuntimeDependencies.collect(bytes).isEmpty()){
            if(source==null)source=open(resolver,location,selected,cancelled);
            RuntimeFileSet files=RuntimeDependencies.prepare(name,original,source);GameTree.check(cancelled);
            return new Prepared(null,files);
        }
        PlayCartridge checked=new PlayCartridge(name,bytes,true,true);
        if(!checked.problem.isEmpty())throw new IllegalArgumentException(checked.problem);
        return new Prepared(bytes,null);
    }
    @Override public byte[] read(String path)throws Exception{
        GameTree.check(cancelled);
        if(snapshots.containsKey(path))return snapshots.get(path).clone();
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
            if(found==null)throw new IllegalArgumentException("Нет связанного файла: "+path);
            if(found.id==null||found.directory!=(i<parts.length-1))throw new IllegalArgumentException("Неверный путь #include: "+path);
            directory=found.id;
        }
        GameTree.check(cancelled);
        byte[] bytes;
        try{bytes=GameFolder.read(resolver,DocumentsContract.buildDocumentUriUsingTree(tree,directory).toString());}
        catch(Exception e){throw new IllegalArgumentException("Не удалось прочитать связанный файл: "+path);}
        if(snapshotBytes+bytes.length>RuntimeFileSet.MAX_BYTES)throw new IllegalArgumentException("Связанные файлы превышают лимит чтения PIKOOS 8 МиБ");
        snapshotBytes+=bytes.length;snapshots.put(path,bytes.clone());return bytes;
    }
}
