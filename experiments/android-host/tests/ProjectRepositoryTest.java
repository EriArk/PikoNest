import art.pikoos.lab.core.*;
import java.util.*;
import java.nio.file.*;

public class ProjectRepositoryTest {
    static int checks;
    static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    interface Attempt {void run()throws Exception;}
    static void reject(Attempt a,String message)throws Exception{try{a.run();}catch(Exception e){checks++;return;}throw new AssertionError(message);}
    static class Memory implements ProjectRepository.Storage {
        Map<String,byte[]> carts=new LinkedHashMap<>();Map<String,String> titles=new HashMap<>();boolean fail,race,corrupt;int writes;
        public List<String> ids(){return new ArrayList<>(carts.keySet());}
        public boolean exists(String id){return carts.containsKey(id);}
        public byte[] read(String id){if(!exists(id))throw new IllegalArgumentException("missing");return carts.get(id);}
        public String importedTitle(String id){return titles.get(id);}
        public void publish(String id,byte[] bytes,String title)throws Exception{if(fail||exists(id))throw new Exception("publish failed");carts.put(id,bytes);if(title!=null)titles.put(id,title);writes++;}
        public void replace(String id,byte[] expected,byte[] next)throws Exception{
            if(race)carts.put(id,new byte[]{99});
            if(fail||!Arrays.equals(read(id),expected))throw new Exception("source conflict");
            carts.put(id,corrupt?new byte[]{12}:next);writes++;
        }
    }
    public static void main(String[] args)throws Exception{
        byte[] raw=Files.readAllBytes(Paths.get("experiments/android-host/assets/moon-garden.p8"));
        Memory m=new Memory();ProjectRepository repo=new ProjectRepository(m);
        repo.create("moon-garden",raw);check(repo.exists("moon-garden"),"created through port");
        byte[] read=repo.read("moon-garden");read[0]=0;check(Arrays.equals(raw,repo.read("moon-garden")),"read ownership");
        byte[] input=raw.clone();repo.create("blank-0001",input);input[0]=0;check(Arrays.equals(raw,repo.read("blank-0001")),"create ownership");
        int writes=m.writes;repo.save("moon-garden",raw,raw);check(m.writes==writes,"no-op has no write");
        byte[] changed=new WorkshopCartridge(raw).withValue(0,3).bytes();repo.save("moon-garden",raw,changed);changed[0]=0;
        check(repo.read("moon-garden")[0]=='p',"save ownership");
        byte[] actual=repo.read("moon-garden");reject(()->repo.save("moon-garden",raw,new byte[]{1}),"stale save");check(Arrays.equals(actual,repo.read("moon-garden")),"conflict preserves source");
        m.fail=true;reject(()->repo.save("moon-garden",actual,raw),"write failure");check(Arrays.equals(actual,repo.read("moon-garden")),"failed write kept source");
        reject(()->repo.create("blank-0002",raw),"failed create");check(!repo.exists("blank-0002"),"failed publication hidden");m.fail=false;
        reject(()->repo.create("moon-garden",raw),"duplicate create");check(Arrays.equals(actual,repo.read("moon-garden")),"duplicate kept source");
        CartridgeImport draft=new CartridgeImport("My game.p8",raw);repo.importProject(draft);check(repo.title(draft.id).equals("My game"),"import title metadata");
        writes=m.writes;repo.importProject(draft);check(m.writes==writes,"import retry exact and idempotent");m.titles.put(draft.id,"Changed title");reject(()->repo.importProject(draft),"changed import metadata preserved");
        check(repo.title(draft.id).equals("Changed title"),"title not overwritten");
        for(String id:new String[]{"../escape","/moon-garden","blank-1",null,"moon-garden/child"}){
            reject(()->repo.read(id),"invalid read address");reject(()->repo.create(id,raw),"invalid create address");reject(()->repo.save(id,raw,raw),"invalid save address");
        }
        m.race=true;reject(()->repo.save("moon-garden",actual,raw),"adapter recheck catches mutation after repository read");check(Arrays.equals(repo.read("moon-garden"),new byte[]{99}),"racing external source preserved");m.race=false;
        Memory flows=new Memory();ProjectRepository r=new ProjectRepository(flows);r.create("moon-garden",raw);
        WorkshopSession session=new WorkshopSession(new WorkshopCartridge(r.read("moon-garden")),new WorkshopSession.Port(){
            public void save(byte[] b)throws Exception{throw new AssertionError("expected-source overload required");}
            public void save(byte[] expected,byte[] b)throws Exception{r.save("moon-garden",expected,b);}
            public void launch(byte[] b){}
        });
        session.act(WorkshopSession.Action.CONFIRM);session.act(WorkshopSession.Action.RIGHT);session.act(WorkshopSession.Action.CONFIRM);
        check(session.cart().value(0)==3&&Arrays.equals(session.cart().bytes(),r.read("moon-garden")),"real editor save through repository");
        session.act(WorkshopSession.Action.UNDO);check(Arrays.equals(raw,r.read("moon-garden")),"Undo writes original source through same port");
        session.act(WorkshopSession.Action.REDO);check(session.cart().value(0)==3&&Arrays.equals(session.cart().bytes(),r.read("moon-garden")),"Redo saves through same port");
        flows.fail=true;byte[] before=r.read("moon-garden");session.act(WorkshopSession.Action.UNDO);check(Arrays.equals(before,r.read("moon-garden"))&&Arrays.equals(before,session.cart().bytes()),"failed Undo cannot publish model or source");
        Memory bad=new Memory();bad.carts.put("moon-garden",raw);bad.corrupt=true;ProjectRepository broken=new ProjectRepository(bad);reject(()->broken.save("moon-garden",raw,actual),"readback mismatch reported");
        bad.carts.put("unsafe-id",raw);reject(broken::ids,"unsafe storage listing rejected");
        System.out.println("ProjectRepositoryTest: "+checks+" checks passed");
    }
}
