import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import art.pikoos.lab.core.WorkshopSession.DrawTool;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class HistoryWorkflowTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static class Port implements WorkshopSession.Port {
        int writes;boolean fail;byte[] saved,launched;
        public void save(byte[] bytes)throws Exception{if(fail)throw new Exception("disk full");saved=bytes.clone();writes++;}
        public void launch(byte[] bytes){launched=bytes.clone();}
    }
    static WorkshopSession editor(WorkshopCartridge base,Port p){
        WorkshopSession s=new WorkshopSession(base,p);s.openSprite(0);s.mode=Mode.CANVAS;return s;
    }
    static void brush(WorkshopSession s,DrawTool tool){s.act(Action.DRAW_TOOLS);s.chooseDrawTool(WorkshopSession.drawMenuIndex(tool));}
    static void point(WorkshopSession s,int x,int y,int color){s.color=color;s.paintAt(x,y);}
    static void step(WorkshopSession s,List<byte[]> states){
        check(!Arrays.equals(states.get(states.size()-1),s.cart().bytes()),"operation actually changes fixture");
        states.add(s.cart().bytes());check(s.undoCount()==states.size()-1&&!s.canRedo(),"one history entry per operation");
    }
    static void menu(WorkshopSession s,boolean redo){
        s.act(Action.MENU);check(s.mode==Mode.MENU&&s.menuItem==0&&!s.menuRedo,"menu starts at undo");
        if(redo)s.act(Action.RIGHT);s.act(Action.CONFIRM);
    }
    public static void main(String[] args)throws Exception{
        for(String file:args){
            String text=new String(Files.readAllBytes(Paths.get(file)),StandardCharsets.ISO_8859_1).replace("\n","\r\n")+"__future__\r\n\u0081opaque\r\n";
            WorkshopCartridge base=new WorkshopCartridge(text.getBytes(StandardCharsets.ISO_8859_1));
            Port p=new Port();WorkshopSession s=editor(base,p);List<byte[]> states=new ArrayList<>();states.add(base.bytes());
            point(s,0,0,(base.pixel(s.selection(),0,0)+1)%16);step(s,states);
            brush(s,DrawTool.LINE);point(s,1,2,9);s.paintAt(13,9);step(s,states);
            brush(s,DrawTool.RECTANGLE);point(s,0,0,10);s.paintAt(15,15);step(s,states);
            brush(s,DrawTool.FILLED_RECTANGLE);point(s,3,12,14);s.paintAt(12,14);step(s,states);
            s.restoreRecolor(10,12,1,"CANVAS");s.act(Action.CONFIRM);step(s,states);
            s.restoreTransform("ROTATE_CLOCKWISE","CANVAS");s.act(Action.CONFIRM);step(s,states);
            s.act(Action.COPY_SPRITE);check(s.mode==Mode.COPY_PLACE,"copy placement opened");
            s.pointCopy(8,4);s.act(Action.CONFIRM);s.act(Action.CONFIRM);step(s,states);
            if(base.hasHero()){
                s.switchTool(0);s.focus=0;s.act(Action.CONFIRM);s.draft=s.cart().value(0)%4+1;
                s.act(Action.CONFIRM);step(s,states);
            }
            for(int i=states.size()-2;i>=0;i--){
                if(i%2==0)menu(s,false);else s.act(Action.UNDO);
                same(states.get(i),s.cart().bytes(),"undo mixed edits restores exact whole cart");
                same(p.saved,s.cart().bytes(),"undo persists before publishing");
                check(s.undoCount()==i&&s.redoCount()==states.size()-1-i,"history position after undo");
            }
            int writes=p.writes;s.act(Action.UNDO);check(p.writes==writes,"empty undo never writes");
            for(int i=1;i<states.size();i++){
                if(i%2==0)menu(s,true);else s.act(Action.REDO);
                same(states.get(i),s.cart().bytes(),"redo returns mixed resource and code edits byte-exactly");
                check(s.undoCount()==i&&s.redoCount()==states.size()-1-i,"history position after redo");
            }
            s.act(Action.TEST);same(states.get(states.size()-1),p.launched,"official runtime port receives redone canonical bytes");
            writes=p.writes;s.act(Action.REDO);check(p.writes==writes,"empty redo never writes");
            s.act(Action.MENU);s.act(Action.UNDO);same(states.get(states.size()-2),s.cart().bytes(),"undo shortcut works while menu is open");
            s.act(Action.MENU);s.act(Action.REDO);same(states.get(states.size()-1),s.cart().bytes(),"redo shortcut works while menu is open");

            // No-op and cancelled previews must not destroy a future edit.
            s=editor(base,p);point(s,0,0,(base.pixel(s.selection(),0,0)+1)%16);byte[] edited=s.cart().bytes();s.act(Action.UNDO);
            writes=p.writes;point(s,0,0,base.pixel(s.selection(),0,0));check(s.canRedo()&&!s.canUndo()&&p.writes==writes,"no-op retains redo");
            for(DrawTool tool:new DrawTool[]{DrawTool.LINE,DrawTool.RECTANGLE,DrawTool.FILLED_RECTANGLE}){
                brush(s,tool);point(s,2,2,10);s.act(Action.RIGHT);byte[] preview=s.canvasPreview().bytes();
                s.act(Action.REDO);same(base.bytes(),s.cart().bytes(),"redo cannot overwrite pending stroke");
                same(preview,s.canvasPreview().bytes(),"redo keeps pending stroke preview");
                s.act(Action.CANCEL);check(s.canRedo()&&!s.canUndo(),"cancelled stroke retains redo");
            }
            s.restoreRecolor(0,12,1,"CANVAS");byte[] preview=s.recolorPreview().bytes();s.act(Action.REDO);
            same(preview,s.recolorPreview().bytes(),"redo cannot change recolor draft");s.act(Action.CANCEL);
            s.restoreTransform("FLIP_HORIZONTAL","CANVAS");preview=s.transformPreview().bytes();s.act(Action.REDO);
            same(preview,s.transformPreview().bytes(),"redo cannot change transform draft");s.act(Action.CANCEL);
            if(!base.empty(s.selection())){
                s.act(Action.COPY_SPRITE);s.act(Action.REDO);check(s.mode==Mode.COPY_PLACE&&s.canRedo(),"copy preview traps redo");s.act(Action.CANCEL);
            }
            s.act(Action.CONTEXT);s.act(Action.REDO);check(s.mode==Mode.PALETTE&&s.canRedo(),"palette traps redo");s.act(Action.CANCEL);
            s.act(Action.REGION);s.act(Action.REDO);check(s.mode==Mode.REGION&&s.canRedo(),"region picker traps redo");s.act(Action.CANCEL);
            s.act(Action.ASSETS);s.act(Action.REDO);check(s.mode==Mode.ASSETS&&s.canRedo(),"asset library traps redo");s.act(Action.CANCEL);
            same(base.bytes(),s.cart().bytes(),"all cancelled previews preserve original cart");check(p.writes==writes,"previews never write history");
            menu(s,true);same(edited,s.cart().bytes(),"redo still available after all cancelled previews");

            // All failure paths leave the current and future durable states reachable.
            p.fail=true;writes=p.writes;s.act(Action.UNDO);
            check(s.mode==Mode.ERROR&&s.undoCount()==1&&!s.canRedo()&&p.writes==writes,"failed undo retains stacks");
            same(edited,s.cart().bytes(),"failed undo keeps published cart");s.act(Action.CANCEL);p.fail=false;s.act(Action.UNDO);
            p.fail=true;s.act(Action.REDO);check(s.mode==Mode.ERROR&&!s.canUndo()&&s.redoCount()==1,"failed redo retains stacks");
            same(base.bytes(),s.cart().bytes(),"failed redo keeps published cart");s.act(Action.CANCEL);
            brush(s,DrawTool.BRUSH);point(s,3,4,(base.pixel(s.selection(),3,4)+1)%16);
            check(s.mode==Mode.ERROR&&s.canRedo()&&!s.canUndo(),"failed branch retains future");
            s.act(Action.CANCEL);p.fail=false;s.act(Action.REDO);same(edited,s.cart().bytes(),"redo can retry after failed branch");
            s.act(Action.UNDO);point(s,3,4,(base.pixel(s.selection(),3,4)+1)%16);byte[] branched=s.cart().bytes();
            check(!s.canRedo()&&s.undoCount()==1,"successful new edit replaces future");s.act(Action.REDO);same(branched,s.cart().bytes(),"old future no longer reachable");
            s.act(Action.UNDO);same(base.bytes(),s.cart().bytes(),"branch still undoable");s.act(Action.REDO);same(branched,s.cart().bytes(),"branch itself redoable");

            WorkshopSession other=editor(base,new Port());other.act(Action.REDO);same(base.bytes(),other.cart().bytes(),"another project has independent history");
            WorkshopSession reopened=editor(s.cart(),new Port());check(!reopened.canUndo()&&!reopened.canRedo(),"new session starts at saved cart without invented history");
        }
        WorkshopCartridge base=new WorkshopCartridge(Files.readAllBytes(Paths.get(args[0])));Port p=new Port();WorkshopSession s=editor(base,p);
        List<byte[]> states=new ArrayList<>();states.add(base.bytes());
        for(int i=1;i<=40;i++){point(s,0,0,i%16);states.add(s.cart().bytes());check(s.undoCount()<=32,"bounded history");}
        for(int i=39;i>=8;i--){s.act(Action.UNDO);same(states.get(i),s.cart().bytes(),"32 retained edits undo in order");}
        check(!s.canUndo()&&s.redoCount()==32,"oldest retained boundary");s.act(Action.UNDO);same(states.get(8),s.cart().bytes(),"cannot cross history boundary");
        for(int i=9;i<=40;i++){s.act(Action.REDO);same(states.get(i),s.cart().bytes(),"32 edits return in order");}
        check(s.undoCount()==32&&!s.canRedo(),"history bound holds after redo");
        point(s,1,1,10);check(s.undoCount()==32,"new edit after full replay remains bounded");
        System.out.println("HistoryWorkflowTest: "+checks+" checks passed");
    }
}
