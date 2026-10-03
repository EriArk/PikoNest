package art.pikoos.lab.core;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** Bounded breadth-first index. Storage IDs stay opaque; paths are display-only. */
public final class GameTree {
    public static final int MAX_ENTRIES=2048,MAX_GAMES=128,MAX_DEPTH=16;
    public static final class Entry {
        public final String id,name;public final boolean directory;
        public Entry(String id,String name,boolean directory){this.id=id;this.name=name;this.directory=directory;}
    }
    public interface Children extends AutoCloseable {Entry next()throws Exception;void close()throws Exception;}
    public interface Source {Children open(String directory)throws Exception;}
    public static final class Cart {
        public final String id,name,folder;
        Cart(Entry entry,String folder){id=entry.id;name=entry.name;this.folder=folder;}
    }
    public static final class Result {
        public final List<Cart> carts=new ArrayList<>();
        public int folders,unreadable;public boolean limited;
    }
    private static final class Folder {
        final String id,path;final int depth;
        Folder(String id,String path,int depth){this.id=id;this.path=path;this.depth=depth;}
    }
    public static void check(BooleanSupplier cancelled){if(cancelled.getAsBoolean())throw new CancellationException();}
    public static Result scan(Source source,String root,BooleanSupplier cancelled)throws Exception{
        Result result=new Result();int seen=0;
        Set<String> visited=new HashSet<>();visited.add(root);
        ArrayDeque<Folder> pending=new ArrayDeque<>();pending.add(new Folder(root,"",0));
        while(!pending.isEmpty()){
            check(cancelled);Folder folder=pending.remove();
            try(Children children=source.open(folder.id)){
                if(folder.depth>0)result.folders++;
                while(true){
                    check(cancelled);Entry entry=children.next();if(entry==null)break;
                    if(++seen>MAX_ENTRIES){result.limited=true;return result;}
                    if(entry.id==null||entry.name==null||!visited.add(entry.id))continue;
                    if(entry.directory){
                        if(folder.depth>=MAX_DEPTH){result.limited=true;continue;}
                        String path=folder.path.isEmpty()?entry.name:folder.path+" / "+entry.name;
                        pending.add(new Folder(entry.id,path,folder.depth+1));
                    }else{
                        String lower=entry.name.toLowerCase(Locale.ROOT);
                        if(!lower.endsWith(".p8")&&!lower.endsWith(".p8.png"))continue;
                        if(result.carts.size()>=MAX_GAMES){result.limited=true;return result;}
                        result.carts.add(new Cart(entry,folder.path));
                    }
                }
            }catch(CancellationException e){throw e;}
            catch(Exception e){if(folder.depth==0)throw e;result.unreadable++;}
        }
        return result;
    }
}
