package art.pikoos.lab.core;

import java.util.*;

/** Canonical cart operations; storage locations and atomic publication belong to the port. */
public final class ProjectRepository {
    public interface Storage {
        List<String> ids()throws Exception;
        boolean exists(String id)throws Exception;
        byte[] read(String id)throws Exception;
        String importedTitle(String id)throws Exception;
        void publish(String id,byte[] bytes,String importedTitle)throws Exception;
        /** Must compare expected source and publish atomically, preserving old data on failure. */
        void replace(String id,byte[] expected,byte[] next)throws Exception;
    }
    private final Storage storage;
    public ProjectRepository(Storage storage){this.storage=Objects.requireNonNull(storage);}
    private static String id(String value){if(!LibrarySession.validId(value))throw new IllegalArgumentException("Invalid project address");return value;}
    public boolean exists(String value)throws Exception{return storage.exists(id(value));}
    public List<String> ids()throws Exception{
        List<String> result=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(String value:storage.ids()){id(value);if(!seen.add(value))throw new IllegalStateException("Duplicate project address");result.add(value);}return result;
    }
    public byte[] read(String value)throws Exception{return storage.read(id(value)).clone();}
    public String title(String value)throws Exception{
        id(value);return CartridgeImport.validId(value)?storage.importedTitle(value):LibrarySession.title(value);
    }
    public synchronized void create(String value,byte[] bytes)throws Exception{
        id(value);if(storage.exists(value))throw new IllegalStateException("Project already exists. Refresh Projects.");
        storage.publish(value,bytes.clone(),null);
    }
    public synchronized void importProject(CartridgeImport draft)throws Exception{
        id(draft.id);byte[] bytes=draft.cart.bytes();
        if(storage.exists(draft.id)){
            if(Arrays.equals(storage.read(draft.id),bytes)&&title(draft.id).equals(draft.title()))return;
            throw new IllegalStateException("This import has changed. Existing project kept.");
        }
        storage.publish(draft.id,bytes.clone(),draft.title());
    }
    public synchronized void save(String value,byte[] expected,byte[] next)throws Exception{
        id(value);byte[] old=expected.clone(),candidate=next.clone();
        byte[] actual=storage.read(value);
        if(!Arrays.equals(old,actual))throw new IllegalStateException("Project changed outside this edit. Reopen it; the saved source was kept.");
        if(Arrays.equals(actual,candidate))return;
        storage.replace(value,old,candidate);
        if(!Arrays.equals(candidate,storage.read(value)))throw new IllegalStateException("Could not verify saved project. Reopen Projects to check its stored version.");
    }
}
