import art.pikoos.lab.core.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;

public class LibraryRepositoryTest {
    static int checks;
    static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
    interface Attempt{void run()throws Exception;}
    static void reject(Attempt a,String m)throws Exception{try{a.run();}catch(Exception e){checks++;return;}throw new AssertionError(m);}
    static class Store implements LibraryRepository.Storage {
        Map<String,byte[]> data=new TreeMap<>();boolean fail,revoked,race,truncate;int writes;
        public List<String> list(String dir)throws Exception{if(revoked)throw new IOException("revoked");List<String> r=new ArrayList<>();for(String p:data.keySet())if(p.startsWith(dir+"/"))r.add(p);return r;}
        public byte[] read(String p)throws Exception{if(revoked)throw new IOException("revoked");return data.containsKey(p)?data.get(p).clone():null;}
        public void create(String p,byte[] b)throws Exception{if(fail||data.containsKey(p))throw new IOException("create failed");data.put(p,truncate?new byte[]{0}:b.clone());writes++;}
        public void replace(String p,byte[] a,byte[] b)throws Exception{if(race)data.put(p,new byte[]{99});if(fail||!Arrays.equals(a,read(p)))throw new IOException("save failed");data.put(p,b.clone());writes++;}
    }
    static ParameterPreset preset(){LuaInsert form=new LuaInsert();for(int n=0;n<LuaInsert.ITEMS.length;n++)if(LuaInsert.ITEMS[n].id.equals("print"))form.choose(n);return ParameterPreset.capture(form,"Settings","Source");}
    static byte[] historicalLabels(ParameterPreset preset)throws Exception{
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(preset.encode()));ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(in.readInt());out.writeInt(in.readInt());for(int i=0;i<4;i++)out.writeUTF(in.readUTF());out.writeBoolean(in.readBoolean());int count=in.readInt();out.writeInt(count);
        for(int i=0;i<count;i++){in.readUTF();out.writeUTF("Historical label "+i);out.writeUTF(in.readUTF());boolean present=in.readBoolean();out.writeBoolean(present);if(present)out.writeUTF(in.readUTF());}return bytes.toByteArray();
    }
    public static void main(String[] args)throws Exception{
        WorkshopCartridge cart=new WorkshopCartridge(Files.readAllBytes(Paths.get("experiments/android-host/assets/moon-garden.p8")));byte[] source=cart.bytes();
        SpriteAsset a=SpriteAsset.capture(cart,new SpriteRegion(0,0,16,16),"Tile","Source");String path="asset-library/sprites/"+a.id+".pksp";
        Store store=new Store();LibraryRepository repo=new LibraryRepository(store);check(repo.sprites().isEmpty()&&repo.presets().isEmpty()&&repo.buffer()==null,"empty library usable");
        repo.create(a);int writes=store.writes;repo.create(a);check(writes==store.writes,"retry create exact");check(repo.sprites().get(0).title.equals("Tile"),"reopen resource");
        SpriteAsset edited=a.withTitle("Object").withCategory(SpriteAsset.Category.OBJECTS).withFavorite(true);repo.update(a,edited);SpriteAsset disk=repo.sprites().get(0);
        check(disk.title.equals("Object")&&disk.category==SpriteAsset.Category.OBJECTS&&disk.favorite,"metadata survives shared storage");
        for(int y=0;y<16;y++)for(int x=0;x<16;x++)check(disk.pixel(x,y)==a.pixel(x,y),"metadata edits preserve pixels");
        writes=store.writes;repo.update(a,edited);check(writes==store.writes,"metadata retry idempotent");reject(()->repo.update(a,a.withTitle("Stale")),"stale metadata");check(Arrays.equals(edited.encode(),store.read(path)),"conflict source preserved");
        store.fail=true;reject(()->repo.update(edited,edited.withFavorite(false)),"failed metadata write");check(repo.sprites().get(0).favorite,"failed favorite kept");store.fail=false;
        repo.buffer(edited);check(repo.buffer().favorite&&repo.buffer().id.equals(edited.id),"shared buffer reopened");SpriteAsset second=SpriteAsset.capture(cart,new SpriteRegion(16,0,8,8),"Other","Other cart");repo.buffer(second);check(repo.buffer().id.equals(second.id),"buffer replaces independently of project");check(Arrays.equals(cart.bytes(),source),"library operations do not mutate source cart");
        ParameterPreset preset=preset();String pp="asset-library/parameters/"+preset.id+".pkpr";byte[] historic=historicalLabels(preset);store.data.put(pp,historic);
        Map<String,byte[]> snapshot=repo.snapshot();check(Arrays.equals(historic,snapshot.get(pp)),"migration snapshot preserves historical labels byte for byte");snapshot.get(pp)[0]=0;check(store.read(pp)[0]!=0,"snapshot ownership");
        ParameterPreset read=repo.presets().get(0);repo.update(read,read.named("Renamed").starred());check(repo.presets().get(0).favorite&&repo.presets().get(0).title.equals("Renamed"),"historical labels allow explicit metadata edit");
        ParameterPreset saved=repo.presets().get(0);reject(()->repo.update(preset,preset.named("Stale")),"stale preset metadata");check(Arrays.equals(saved.encode(),store.read(pp)),"preset conflict preserved");
        store.revoked=true;reject(repo::sprites,"lost grant cannot become empty library");reject(repo::buffer,"lost buffer access cannot become empty buffer");store.revoked=false;
        store.race=true;reject(()->repo.update(edited,edited.withTitle("Race")),"CAS rejects mutation between reads");check(Arrays.equals(store.read(path),new byte[]{99}),"racing source not overwritten");store.race=false;store.data.put(path,edited.encode());
        Store bad=new Store();bad.truncate=true;LibraryRepository broken=new LibraryRepository(bad);reject(()->broken.create(a),"truncated create reported");reject(broken::sprites,"corrupt data not hidden");
        for(String p:new String[]{"../asset","asset-library/sprites/../x","sprite-buffer.pksp/child","asset-library/music/foo","asset-library/sprites/wrong.pksp"})reject(()->LibraryRepository.path(p),"unsafe or unsupported resource path");
        SpriteAsset otherId=SpriteAsset.capture(cart,new SpriteRegion(0,0,8,8),"New","Other");reject(()->repo.update(edited,otherId),"resource identity cannot change");
        Store target=new Store();LibraryRepository reopened=new LibraryRepository(target);for(Map.Entry<String,byte[]> entry:repo.snapshot().entrySet())target.create(entry.getKey(),entry.getValue());
        check(reopened.sprites().get(0).favorite&&reopened.presets().get(0).favorite&&reopened.buffer().id.equals(second.id),"independent migrated library reopened");
        FirstRunSetup setup=new FirstRunSetup(new FirstRunSetup.Port(){public void save(int n,boolean b){}public void games(){}public void creations(){}public void runtime(){}public void leave(){}},1);setup.libraryExternal=true;
        check(setup.body().contains("projects stay inside")&&setup.status().contains("Library: external"),"independent library location explained");setup.projectsExternal=true;check(setup.status().contains("Projects + library"),"both external locations explained");
        System.out.println("LibraryRepositoryTest: "+checks+" checks passed");
    }
}
