package art.pikoos.lab.core;

/** Portable exit-menu intent. Only the runtime can establish that execution ended. */
public final class RuntimeMenu {
    public enum Action { MENU, PREVIOUS, NEXT, CONFIRM, CANCEL }
    public enum State { CLOSED, OPEN, EXIT_REQUESTED }
    public State state=State.CLOSED;
    public int selection;
    public boolean failed;
    public void act(Action action){
        if(state==State.EXIT_REQUESTED)return;
        if(state==State.CLOSED){if(action==Action.MENU){state=State.OPEN;selection=0;failed=false;}return;}
        switch(action){
            case MENU:case CANCEL:state=State.CLOSED;break;
            case PREVIOUS:selection=0;break;
            case NEXT:selection=1;break;
            case CONFIRM:state=selection==0?State.CLOSED:State.EXIT_REQUESTED;break;
        }
    }
    public void unconfirmed(){if(state==State.EXIT_REQUESTED){state=State.OPEN;selection=0;failed=true;}}
    /** Backgrounding must never replay an exit request on resume. */
    public void background(){state=State.CLOSED;selection=0;failed=false;}
}
