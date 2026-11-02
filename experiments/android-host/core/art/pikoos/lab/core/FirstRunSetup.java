package art.pikoos.lab.core;

import art.pikoos.lab.core.WorkshopSession.Action;

/** Optional setup journey. Completion means leaving the guide, never runtime acceptance. */
public final class FirstRunSetup {
    public interface Port {
        void save(int step, boolean dismissed) throws Exception;
        void games(); void creations(); void runtime(); void leave();
    }
    public final Port port;
    public int step;
    public boolean busy, gamesChecked, runtimePrepared,projectsExternal,libraryExternal;
    public String gamesName="", problem="";
    private long generation;
    public FirstRunSetup(Port port,int step){this.port=port;this.step=Math.max(0,Math.min(3,step));}
    public static boolean shouldOpen(boolean dismissed,boolean pendingWork,boolean runtimeActive){
        return !dismissed&&!pendingWork&&!runtimeActive;
    }
    public void act(Action a){
        if(a==Action.CANCEL){if(busy){generation++;busy=false;problem="Check cancelled. Previous folder kept.";}else leave();return;}
        if(busy)return;
        if(a==Action.NEXT||a==Action.RIGHT){move(1);return;}
        if(a==Action.PREVIOUS||a==Action.LEFT){move(-1);return;}
        if(a==Action.CONFIRM){if(step==0)port.games();else if(step==1)port.creations();else if(step==2)port.runtime();else leave();}
        if(a==Action.CONTEXT&&step==2)port.runtime();
    }
    private void move(int delta){
        int next=Math.max(0,Math.min(3,step+delta));
        try{port.save(next,false);step=next;problem="";}catch(Exception e){problem="Cannot save setup progress. Try again.";}
    }
    private void leave(){
        try{port.save(step,true);generation++;port.leave();}catch(Exception e){problem="Cannot save setup progress. Try again.";}
    }
    public long beginCheck(){busy=true;problem="";return ++generation;}
    public boolean accepts(long request){return busy&&request==generation;}
    public void checked(long request,String name,String error){
        if(!accepts(request))return;
        busy=false;
        if(error==null){gamesName=name;gamesChecked=true;problem="";}
        else problem=error;
    }
    public String title(){return new String[]{"Your games","Your creations","Your PICO-8","Make yourself at home"}[step];}
    public String body(){
        if(step==0)return "Connect a folder of PICO-8 games for the Play shelf. Your original files stay unchanged. No games yet? Continue and try a starter.";
        if(step==1&&libraryExternal)return (projectsExternal?"Projects and library use their selected external workspaces.":"The library uses its selected external workspace; projects stay inside PikoNest.")+" Splore, settings, drafts and recovery records remain inside the app. Keep the app installed.";
        if(step==1&&projectsExternal)return "Projects use your selected external workspace. Assets and Splore still stay inside PikoNest. Keep the app installed: settings, drafts and save recovery records remain inside it.";
        if(step==1)return "Projects and assets currently stay inside PikoNest. Open Folders to choose and activate their external workspaces, or continue with Right. Splore, settings and drafts stay inside the app. Keep the app installed. Reopen this guide with Select in Folders.";
        if(step==2)return "Choose the Raspberry Pi ZIP with ARM64 support from your PICO-8 purchase page. The version and ZIP name can vary. Preparation checks files; Test checks playback. You can create now and set up play later.";
        return "Browse games in Play, or open Workshop to make your own. Setup is always available from Folders. Runtime preparation does not confirm picture, sound or controls.";
    }
    public String status(){
        if(step==0)return busy?"Checking folder...":gamesChecked?"Connected: "+gamesName:gamesName.isEmpty()?"No games folder connected":"Saved folder: check access";
        if(step==1&&libraryExternal)return projectsExternal?"Projects + library: external":"Library: external / projects: inside";
        if(step==1)return projectsExternal?"Projects: external / assets: inside app":"Current storage: inside the app";
        if(step==2)return runtimePrepared?"Prepared / optional test available":"Play needs your purchased runtime";
        return runtimePrepared?"Runtime prepared / try a game":"You can create / set up play later";
    }
    public String primary(){return step==0?"Choose games folder":step==1?"Open creation folders":step==2?"Set up PICO-8":"Enter PikoNest";}
}
