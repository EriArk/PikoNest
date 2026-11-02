import art.pikoos.lab.core.*;
import java.util.*;
import java.nio.file.*;

public class RecoverableReplaceTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    interface Attempt{void run()throws Exception;}
    static void rejects(Attempt a,String why)throws Exception{try{a.run();}catch(Exception e){checks++;return;}throw new AssertionError(why);}
    static class Port implements RecoverableReplace.Port {
        Map<String,byte[]> files=new LinkedHashMap<>();RecoverableReplace.Record record;int step,failAt=-1;boolean after,truncate;
        Port(byte[] old){files.put("game.p8",old.clone());}
        void before()throws Exception{step++;if(!after&&step==failAt)throw new Exception("provider failed before operation");}
        void after()throws Exception{if(after&&step==failAt)throw new Exception("provider failed after operation");}
        public byte[] read(String n)throws Exception{if(!files.containsKey(n))throw new Exception("missing");return files.get(n).clone();}
        public boolean exists(String n){return files.containsKey(n);}
        public void create(String n,byte[] b)throws Exception{before();if(exists(n))throw new Exception("exists");files.put(n,truncate?new byte[]{0}:b.clone());after();}
        public void rename(String a,String b)throws Exception{before();if(!exists(a)||exists(b))throw new Exception("unsafe rename");files.put(b,files.remove(a));after();}
        public RecoverableReplace.Record pending(){return record;}
        public void journal(RecoverableReplace.Record r)throws Exception{before();record=r;after();}
        public void clear()throws Exception{before();record=null;after();}
    }
    public static void main(String[] args)throws Exception{
        byte[] old=new byte[]{1,13,10,-1},next=new byte[]{2,13,10,-1};
        for(boolean after:new boolean[]{false,true})for(int fail=1;fail<=5;fail++){
            Port p=new Port(old);p.after=after;p.failAt=fail;
            rejects(()->RecoverableReplace.replace(p,old,next),"injected provider failure");
            p.failAt=-1;RecoverableReplace.recover(p);byte[] recovered=p.read("game.p8");
            check(Arrays.equals(old,recovered)||Arrays.equals(next,recovered),"complete old or new version after interrupted save");
            check(p.record==null,"journal retired only after recovery");int count=p.files.size();RecoverableReplace.recover(p);check(count==p.files.size(),"recovery is idempotent");
            if(Arrays.equals(recovered,next))check(p.files.values().stream().anyMatch(b->Arrays.equals(b,old)),"prior bytes retained after commit");
        }
        Port normal=new Port(old);RecoverableReplace.replace(normal,old,next);check(Arrays.equals(normal.read("game.p8"),next)&&normal.record==null,"success verified");
        check(normal.files.values().stream().anyMatch(b->Arrays.equals(b,old)),"success retains original");
        RecoverableReplace.replace(normal,next,old);check(Arrays.equals(old,normal.read("game.p8")),"Undo through retained-version protocol");
        Port partial=new Port(old);partial.truncate=true;rejects(()->RecoverableReplace.replace(partial,old,next),"partial candidate");check(Arrays.equals(old,partial.read("game.p8"))&&partial.record==null,"partial candidate never renames old cart");
        Port stale=new Port(old);rejects(()->RecoverableReplace.replace(stale,next,old),"stale source");check(stale.files.size()==1,"stale source creates no candidate");
        Port conflict=new Port(old);conflict.after=true;conflict.failAt=2;rejects(()->RecoverableReplace.replace(conflict,old,next),"interrupt after durable journal");
        conflict.failAt=-1;byte[] external={99};conflict.files.put("game.p8",external);int count=conflict.files.size();rejects(()->RecoverableReplace.recover(conflict),"external modification during interrupted save");
        check(Arrays.equals(external,conflict.read("game.p8"))&&conflict.files.size()==count&&conflict.record!=null,"unfamiliar source and recovery evidence preserved");
        Port missing=new Port(old);missing.after=true;missing.failAt=3;rejects(()->RecoverableReplace.replace(missing,old,next),"interrupt in rename gap");missing.failAt=-1;missing.files.remove(missing.record.backup());rejects(()->RecoverableReplace.recover(missing),"missing backup");check(missing.record!=null,"missing backup does not clear evidence");
        rejects(()->new RecoverableReplace.Record("../escape",RecoverableReplace.hash(old),RecoverableReplace.hash(next)),"journal path validation");
        byte[] raw=Files.readAllBytes(Paths.get("experiments/android-host/assets/moon-garden.p8"));Port files=new Port(raw);
        ProjectRepository repo=new ProjectRepository(new ProjectRepository.Storage(){
            public List<String> ids(){return Arrays.asList("moon-garden");}public boolean exists(String id){return true;}
            public byte[] read(String id)throws Exception{RecoverableReplace.recover(files);return files.read("game.p8");}
            public String importedTitle(String id){return "Moon Garden";}public void publish(String id,byte[] b,String t){throw new IllegalStateException();}
            public void replace(String id,byte[] a,byte[] b)throws Exception{RecoverableReplace.replace(files,a,b);}
        });
        WorkshopSession session=new WorkshopSession(new WorkshopCartridge(repo.read("moon-garden")),new WorkshopSession.Port(){
            public void save(byte[] b){throw new AssertionError("expected source required");}
            public void save(byte[] a,byte[] b)throws Exception{repo.save("moon-garden",a,b);}public void launch(byte[] b){}
        });
        session.act(WorkshopSession.Action.CONFIRM);session.act(WorkshopSession.Action.RIGHT);session.act(WorkshopSession.Action.CONFIRM);
        check(session.cart().value(0)==3&&Arrays.equals(session.cart().bytes(),repo.read("moon-garden")),"real editor save through journaled provider model");
        session.act(WorkshopSession.Action.UNDO);check(Arrays.equals(raw,repo.read("moon-garden")),"real editor Undo exact bytes");session.act(WorkshopSession.Action.REDO);
        check(new WorkshopCartridge(repo.read("moon-garden")).value(0)==3,"reopen/export ordinary cart bytes");
        FirstRunSetup setup=new FirstRunSetup(new FirstRunSetup.Port(){public void save(int n,boolean d){}public void games(){}public void creations(){}public void runtime(){}public void leave(){}},1);setup.projectsExternal=true;check(setup.body().contains("external")&&setup.status().contains("assets: inside"),"onboarding describes split storage accurately");
        System.out.println("RecoverableReplaceTest: "+checks+" checks passed");
    }
}
