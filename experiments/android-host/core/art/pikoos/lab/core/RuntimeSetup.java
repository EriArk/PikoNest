package art.pikoos.lab.core;

import art.pikoos.lab.core.WorkshopSession.Action;

/** Archive readiness is separate from installed adapter and successful runtime execution. */
public final class RuntimeSetup {
    public interface Port {void pick();void recheck();void cancel();void leave();default void test(){}}
    public final Port port;
    public boolean busy,hasArchive,verified,adapterPresent,arm64;
    public String filename="",phase="",problem="",notice="";
    public long archiveBytes;
    public boolean testing,returned;
    public RuntimeSetup(Port port){this.port=port;}
    public void act(Action action){
        if(testing){if(action==Action.CONFIRM||action==Action.TEST)port.test();else if(action==Action.CANCEL)port.leave();return;}
        if(action==Action.CANCEL){if(busy)port.cancel();else port.leave();return;}
        if(busy)return;
        if(action==Action.CONFIRM||action==Action.TEST){if(verified&&arm64)port.test();else if(arm64)port.pick();else problem="This runtime backend requires Android ARM64.";}
        if(action==Action.CONTEXT)port.pick();
        if(action==Action.UNDO&&hasArchive)port.recheck();
    }
    public void begin(String text){busy=true;phase=text;problem="";notice="";}
    public void complete(String name,long bytes,String error){
        busy=false;phase="";
        if(error==null){hasArchive=true;verified=true;filename=name;archiveBytes=bytes;problem="";notice="Archive copy saved in the app";}
        else{problem=error;notice=hasArchive?"Previous archive kept":"You can choose another archive";}
    }
    public void cancelled(){busy=false;phase="";problem="";notice="Cancelled / previous settings kept";}
}
