import art.pikoos.lab.core.RuntimeMenu;
import art.pikoos.lab.core.RuntimeMenu.*;
public final class RuntimeMenuTest {
    static int n;
    static void check(boolean b,String why){n++;if(!b)throw new AssertionError(why);}
    public static void main(String[] args){
        RuntimeMenu m=new RuntimeMenu();
        for(Action a:new Action[]{Action.CONFIRM,Action.NEXT,Action.CANCEL})m.act(a);
        check(m.state==State.CLOSED,"game input cannot request exit");
        m.act(Action.MENU);m.act(Action.CONFIRM);check(m.state==State.CLOSED,"first confirmation continues");
        m.act(Action.MENU);m.act(Action.NEXT);m.act(Action.CANCEL);check(m.state==State.CLOSED,"cancel exit selection");
        m.act(Action.MENU);check(m.selection==0,"reopen resets safe selection");
        m.act(Action.NEXT);m.act(Action.PREVIOUS);m.act(Action.CONFIRM);check(m.state==State.CLOSED,"navigate back continues");
        m.act(Action.MENU);m.act(Action.NEXT);m.act(Action.CONFIRM);check(m.state==State.EXIT_REQUESTED,"explicit exit request");
        for(Action a:Action.values())m.act(a);check(m.state==State.EXIT_REQUESTED,"pending request cannot repeat or claim completion");
        m.unconfirmed();check(m.failed&&m.state==State.OPEN&&m.selection==0,"timeout offers safe retry");
        m.act(Action.NEXT);m.act(Action.CONFIRM);check(m.state==State.EXIT_REQUESTED,"explicit retry");
        m.background();m.unconfirmed();check(m.state==State.CLOSED&&!m.failed,"background cancels delayed work");
        m.act(Action.MENU);m.act(Action.MENU);check(m.state==State.CLOSED,"menu toggles without exit");
        System.out.println("Runtime menu: "+n+" checks passed");
    }
}
