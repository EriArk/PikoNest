import art.pikoos.lab.core.FolderSetup;
import art.pikoos.lab.core.WorkshopSession.Action;

public final class FolderSetupTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static class Port implements FolderSetup.Port {
        int saves,picks,checks,leaves;boolean fail;
        public void pick(FolderSetup.Role r){picks++;}
        public void check(FolderSetup.Role r,String uri){checks++;}
        public void save(FolderSetup.Role r,String uri,String name)throws Exception{if(fail)throw new Exception("disk full");saves++;}
        public void leave(){leaves++;}
    }
    public static void main(String[] args){
        Port p=new Port();FolderSetup s=new FolderSetup(p,new String[]{"old","","",""},new String[]{"Games","","",""});
        check(s.entry(0).state==FolderSetup.State.UNKNOWN,"cold start never claims access verified");
        s.act(Action.CONFIRM);check(p.picks==1&&s.entry(0).location.equals("old"),"picker/cancel cannot mutate location");
        s.begin(FolderSetup.Role.GAMES,"new");
        for(Action a:Action.values())if(a!=Action.CANCEL)s.act(a);
        check(s.selected==0&&p.picks==1&&p.leaves==0&&p.saves==0,"busy traps changes");
        s.complete(null,"denied");check(s.entry(0).location.equals("old")&&s.entry(0).state==FolderSetup.State.UNKNOWN,"rejected replacement retains previous choice");
        s.act(Action.CONTEXT);s.complete(null,"revoked");
        check(s.entry(0).state==FolderSetup.State.UNAVAILABLE&&s.entry(0).location.equals("old"),"revocation preserves recovery handle");
        s.begin(FolderSetup.Role.GAMES,"old");s.complete("Games",null);
        check(s.entry(0).state==FolderSetup.State.READY&&p.saves==1,"successful reconnect");
        p.fail=true;s.begin(FolderSetup.Role.GAMES,"other");s.complete("Other",null);
        check(s.entry(0).location.equals("old")&&s.entry(0).state==FolderSetup.State.READY,"failed persistence retains good folder");
        p.fail=false;s.act(Action.DOWN);s.act(Action.CONTEXT);check(!s.busy,"empty folder has no probe");
        for(FolderSetup.Role role:FolderSetup.Role.values()){
            s.begin(role,"same-handle");s.complete("Shared",null);
            check(s.entry(role.ordinal()).location.equals("same-handle"),"roles may share a tree; no indexing implied");
        }
        s.act(Action.CANCEL);check(p.leaves==1,"leave to shelf");
        s.begin(FolderSetup.Role.DATA,"cancelled");s.act(Action.CANCEL);s.complete("Late result",null);
        check(!s.busy&&p.leaves==2&&s.entry(3).location.equals("same-handle"),"cancelled worker cannot publish late result");
        check(!FolderSetup.Role.GAMES.writable()&&FolderSetup.Role.PROJECTS.writable()&&FolderSetup.Role.DATA.writable()&&FolderSetup.Role.DOWNLOADS.writable(),"read only games; writable destinations");
        System.out.println("FolderSetupTest: "+checks+" checks passed");
    }
}
