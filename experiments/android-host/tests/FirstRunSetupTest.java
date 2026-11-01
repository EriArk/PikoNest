import art.pikoos.lab.core.FirstRunSetup;
import art.pikoos.lab.core.RuntimeSetup;
import art.pikoos.lab.core.WorkshopSession.Action;

public final class FirstRunSetupTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static final class Port implements FirstRunSetup.Port {
        int step,saves,games,runtime,leaves;boolean dismissed,fail;
        public void save(int next,boolean later)throws Exception{if(fail)throw new Exception("disk full");step=next;dismissed=later;saves++;}
        public void games(){games++;}public void runtime(){runtime++;}public void leave(){leaves++;}
    }
    public static void main(String[] args){
        for(boolean dismissed:new boolean[]{false,true})for(boolean pending:new boolean[]{false,true})for(boolean active:new boolean[]{false,true})
            check(FirstRunSetup.shouldOpen(dismissed,pending,active)==(!dismissed&&!pending&&!active),"entry respects pending work, active runtime and later choice");
        Port p=new Port();FirstRunSetup s=new FirstRunSetup(p,-99);check(s.step==0,"invalid restored step clamps");
        s.act(Action.CONFIRM);check(p.games==1&&!s.gamesChecked&&p.saves==0,"picker opening is not folder acceptance");
        long first=s.beginCheck();for(Action a:Action.values())if(a!=Action.CANCEL)s.act(a);
        check(s.step==0&&p.games==1&&p.saves==0,"checking ignores navigation and duplicate work");
        s.act(Action.CANCEL);check(!s.busy&&p.leaves==0,"first back cancels pending check");
        long second=s.beginCheck();s.checked(first,"Late",null);
        check(s.busy&&!s.gamesChecked&&!s.accepts(first)&&s.accepts(second),"late result cannot publish into retry");
        s.checked(second,"Games",null);check(s.gamesChecked&&s.gamesName.equals("Games")&&!s.busy,"verified folder feedback");
        long third=s.beginCheck();s.checked(third,"Other","Access revoked");
        check(s.gamesName.equals("Games")&&s.gamesChecked&&s.problem.equals("Access revoked"),"failed replacement preserves previous accepted folder");
        s.act(Action.NEXT);check(s.step==1&&p.step==1&&!p.dismissed,"step persisted before moving");
        check(s.body().contains("not active yet")&&s.status().contains("inside the app"),"intentions never presented as active external storage");
        s.act(Action.CONFIRM);check(s.step==2&&p.runtime==0,"storage continuation does not launch runtime");
        check(s.body().contains("Raspberry Pi ZIP"),"exact archive type explained");
        s.act(Action.CONFIRM);check(p.runtime==1&&!s.runtimePrepared,"runtime entry is not readiness");
        s.runtimePrepared=true;check(s.status().contains("optional test"),"prepared does not mean test passed");
        s.act(Action.RIGHT);check(s.step==3&&p.step==3,"finish is reachable without mandatory runtime");
        check(s.body().contains("does not confirm"),"runtime acceptance boundary stated");
        s.act(Action.CONFIRM);check(p.dismissed&&p.leaves==1,"finish stores dismissal");
        FirstRunSetup resumed=new FirstRunSetup(p,p.step);check(resumed.step==3&&!resumed.gamesChecked&&!resumed.runtimePrepared,"reentry restores position but rechecks capability facts");
        resumed.act(Action.PREVIOUS);check(resumed.step==2&&!p.dismissed,"review previous step");
        p.fail=true;resumed.act(Action.RIGHT);check(resumed.step==2&&!resumed.problem.isEmpty(),"failed progress save cannot advance");
        resumed.act(Action.CANCEL);check(p.leaves==1&&!p.dismissed,"failed dismissal save leaves guide available");
        p.fail=false;resumed.act(Action.CANCEL);check(p.leaves==2&&p.dismissed,"later exits without claiming setup completion");
        FirstRunSetup cold=new FirstRunSetup(new Port(),99);check(cold.step==3,"upper step clamp");
        cold.gamesName="Old folder";cold.step=0;long id=cold.beginCheck();cold.checked(id,"","Access unavailable");
        check(!cold.gamesChecked&&cold.status().contains("check access")&&cold.gamesName.equals("Old folder"),"saved URI alone is not fresh access proof");
        int[] runtime={0,0,0,0,0,0};
        RuntimeSetup r=new RuntimeSetup(new RuntimeSetup.Port(){
            public void pick(){runtime[0]++;}public void recheck(){runtime[1]++;}public void cancel(){runtime[2]++;}
            public void leave(){runtime[3]++;}public void prepare(){runtime[4]++;}public void test(){runtime[5]++;}
        });
        r.arm64=true;r.act(Action.CONFIRM);check(runtime[0]==1,"unconfigured runtime chooses ZIP");
        r.complete("raspi.zip",123,null);r.act(Action.CONFIRM);
        check(runtime[4]==1&&runtime[5]==0&&!r.prepared,"archive verification cannot launch a test");
        r.begin("preparing");r.act(Action.TEST);r.act(Action.CANCEL);
        check(runtime[5]==0&&runtime[2]==1,"preparation is cancellable and cannot launch");
        r.cancelled();check(r.verified&&!r.prepared,"cancel preserves verified archive without claiming preparation");
        r.complete("",0,"Not enough space");r.act(Action.CONFIRM);
        check(runtime[4]==2&&runtime[0]==1&&runtime[5]==0,"preparation failure retries saved archive without repick or launch");
        r.prepared=true;r.act(Action.CONFIRM);check(runtime[5]==1,"test only after preparation and user action");
        r.act(Action.CANCEL);check(runtime[3]==1,"prepared runtime can leave without testing");
        r.testing=true;r.act(Action.CONTEXT);check(runtime[0]==1,"running test cannot replace archive");
        r.act(Action.CONFIRM);check(runtime[5]==2,"running test can resume");
        System.out.println("FirstRunSetupTest: "+checks+" checks passed");
    }
}
