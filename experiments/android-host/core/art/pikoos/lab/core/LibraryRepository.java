package art.pikoos.lab.core;

import java.util.*;

/** Typed reusable data over a location-neutral, compare-and-save file port. */
public final class LibraryRepository {
    public interface Storage {
        List<String> list(String directory)throws Exception;
        byte[] read(String path)throws Exception; // null only when absent, never on lost access
        void create(String path,byte[] bytes)throws Exception;
        void replace(String path,byte[] expected,byte[] next)throws Exception;
    }
    private final Storage storage;
    public LibraryRepository(Storage storage){this.storage=Objects.requireNonNull(storage);}
    public static void path(String value){
        if(!value.equals("sprite-buffer.pksp")&&!value.matches("asset-library/(?:sprites/[0-9a-f-]{36}\\.pksp|parameters/[0-9a-f-]{36}\\.pkpr)"))throw new IllegalArgumentException("Invalid library address");
    }
    private static String sprite(String id){String p="asset-library/sprites/"+id+".pksp";path(p);return p;}
    private static String preset(String id){String p="asset-library/parameters/"+id+".pkpr";path(p);return p;}
    public List<SpriteAsset> sprites()throws Exception{
        List<SpriteAsset> result=new ArrayList<>();
        for(String p:storage.list("asset-library/sprites")){path(p);if(!p.startsWith("asset-library/sprites/"))throw new IllegalStateException("Wrong library section");byte[] b=storage.read(p);if(b==null)throw new IllegalStateException("Resource disappeared. Reopen the library.");SpriteAsset a=SpriteAsset.decode(b);if(!p.equals(sprite(a.id)))throw new IllegalStateException("Resource ID differs from its file");result.add(a);}
        result.sort((a,b)->{int c=a.title.compareTo(b.title);return c==0?a.id.compareTo(b.id):c;});return result;
    }
    public List<ParameterPreset> presets()throws Exception{
        List<ParameterPreset> result=new ArrayList<>();
        for(String p:storage.list("asset-library/parameters")){path(p);if(!p.startsWith("asset-library/parameters/"))throw new IllegalStateException("Wrong library section");byte[] b=storage.read(p);if(b==null)throw new IllegalStateException("Preset disappeared. Reopen the library.");ParameterPreset a=ParameterPreset.decode(b);if(!p.equals(preset(a.id)))throw new IllegalStateException("Preset ID differs from its file");result.add(a);}
        result.sort((a,b)->{int c=a.title.compareTo(b.title);return c==0?a.id.compareTo(b.id):c;});return result;
    }
    private void create(String p,byte[] bytes)throws Exception{
        byte[] actual=storage.read(p);if(actual!=null){if(Arrays.equals(actual,bytes))return;throw new IllegalStateException("Resource already exists. Existing data kept.");}
        storage.create(p,bytes.clone());verify(p,bytes);
    }
    private void verify(String p,byte[] bytes)throws Exception{if(!Arrays.equals(bytes,storage.read(p)))throw new IllegalStateException("Could not verify saved resource. Reopen the library to check its stored version.");}
    private void replace(String p,byte[] expected,byte[] next)throws Exception{
        byte[] actual=storage.read(p);if(Arrays.equals(actual,next))return;
        if(!Arrays.equals(actual,expected))throw new IllegalStateException("Resource changed. Reopen the library; stored data was kept.");
        storage.replace(p,expected.clone(),next.clone());verify(p,next);
    }
    public synchronized void create(SpriteAsset a)throws Exception{create(sprite(a.id),a.encode());}
    public synchronized void create(ParameterPreset a)throws Exception{create(preset(a.id),a.encode());}
    public synchronized void update(SpriteAsset a,SpriteAsset b)throws Exception{
        if(!a.id.equals(b.id))throw new IllegalArgumentException("Resource ID cannot change");String p=sprite(a.id);byte[] actual=storage.read(p);
        if(actual==null||!Arrays.equals(SpriteAsset.decode(actual).encode(),a.encode())){if(Arrays.equals(actual,b.encode()))return;throw new IllegalStateException("Resource changed. Reopen the library.");}
        replace(p,actual,b.encode());
    }
    public synchronized void update(ParameterPreset a,ParameterPreset b)throws Exception{
        if(!a.id.equals(b.id))throw new IllegalArgumentException("Preset ID cannot change");String p=preset(a.id);byte[] actual=storage.read(p);
        if(actual==null||!Arrays.equals(ParameterPreset.decode(actual).encode(),a.encode())){if(Arrays.equals(actual,b.encode()))return;throw new IllegalStateException("Preset changed. Reopen the library.");}
        replace(p,actual,b.encode());
    }
    public SpriteAsset buffer()throws Exception{byte[] bytes=storage.read("sprite-buffer.pksp");return bytes==null?null:SpriteAsset.decode(bytes);}
    public synchronized void buffer(SpriteAsset a)throws Exception{
        byte[] bytes=a.encode();SpriteAsset.decode(bytes);byte[] old=storage.read("sprite-buffer.pksp");
        if(old==null)create("sprite-buffer.pksp",bytes);else replace("sprite-buffer.pksp",old,bytes);
    }
    public Map<String,byte[]> snapshot()throws Exception{
        Map<String,byte[]> result=new TreeMap<>();for(SpriteAsset a:sprites())result.put(sprite(a.id),storage.read(sprite(a.id)).clone());for(ParameterPreset a:presets())result.put(preset(a.id),storage.read(preset(a.id)).clone());
        SpriteAsset b=buffer();if(b!=null)result.put("sprite-buffer.pksp",storage.read("sprite-buffer.pksp").clone());return result;
    }
}
