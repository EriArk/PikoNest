import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import art.pikoos.p8.P8Document;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class CopyWorkflowTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static class Port implements WorkshopSession.Port {
        int writes,launches;boolean fail;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");writes++;}
        public void launch(byte[] b){launches++;}
    }
    public static void main(String[] args)throws Exception{
        for(String file:args){
            byte[] input=Files.readAllBytes(Paths.get(file));
            String unusual=new String(input,StandardCharsets.ISO_8859_1).replace("\n","\r\n")+"__future__\r\n\u0081opaque\r\n";
            SpriteRegion src=new SpriteRegion(0,0,24,24),dst=new SpriteRegion(64,32,24,24);
            WorkshopCartridge original=new WorkshopCartridge(unusual.getBytes(StandardCharsets.ISO_8859_1))
                .withPixel(src,23,23,14).withPixel(dst,3,3,12);
            Port p=new Port();WorkshopSession s=new WorkshopSession(original,p);
            s.openSprite(0);s.region=src;s.mode=Mode.CANVAS;
            s.act(Action.COPY_SPRITE);check(s.mode==Mode.COPY_PLACE,"copy in every project, any rectangle");
            s.pointCopy(8,4);s.act(Action.CONFIRM);check(s.mode==Mode.COPY_CONFIRM,"occupied destination reaches explicit preview");
            for(Action a:new Action[]{Action.UNDO,Action.NEXT,Action.MENU,Action.REGION}){
                s.act(a);check(s.mode==Mode.COPY_CONFIRM&&p.writes==0&&p.launches==0,"draft traps unrelated actions");
            }
            s.switchTool(0);check(s.tool==2,"touch tabs cannot escape draft");
            s.act(Action.CANCEL);s.act(Action.CANCEL);
            check(s.mode==Mode.CANVAS&&s.selection()==src&&!s.copying(),"cancel restores source editor");
            same(original.bytes(),s.cart().bytes(),"cancel is byte exact");
            s.act(Action.COPY_SPRITE);s.pointCopy(1,1);s.act(Action.CONFIRM);
            check(s.copyOverlaps()&&s.mode==Mode.COPY_CONFIRM,"overlap requires explicit replacement preview");
            s.act(Action.CANCEL);s.pointCopy(999,999);check(s.copyDestination().x==104&&s.copyDestination().y==104,"destination bounds account for rectangle");
            s.pointCopy(8,4);s.act(Action.CONFIRM);p.fail=true;s.act(Action.CONFIRM);
            check(s.mode==Mode.ERROR&&s.copying()&&!s.canUndo()&&p.writes==0,"failed save retains draft and empty history");
            same(original.bytes(),s.cart().bytes(),"failed copy byte exact");
            s.act(Action.CANCEL);check(s.mode==Mode.COPY_CONFIRM,"retry returns to preview");
            p.fail=false;s.act(Action.CONFIRM);
            check(p.writes==1&&s.mode==Mode.CANVAS&&s.selection().x==64&&!s.copying(),"copy commits once and opens destination");
            for(int y=0;y<128;y++)for(int x=0;x<128;x++){
                boolean inside=x>=64&&x<88&&y>=32&&y<56;
                check(s.cart().sheetPixel(x,y)==(inside?original.sheetPixel(x-64,y-32):original.sheetPixel(x,y)),"only destination graphics change, including lower map-shared half");
            }
            P8Document before=P8Document.parse(original.bytes()),after=P8Document.parse(s.cart().bytes());
            for(int i=0;i<before.sections().size();i++)if(!before.sections().get(i).name.equals("gfx"))
                same(before.body(i),after.body(i),"Lua, other resources and unknown bytes unchanged");
            s.act(Action.TEST);check(p.launches==1,"saved copy can launch");s.act(Action.UNDO);
            same(original.bytes(),s.cart().bytes(),"single undo restores bytes and expanded gfx exactly");
            s.openSprite(0);s.region=src;s.mode=Mode.CANVAS;
            s.restoreCopy(64,32,"CANVAS");check(s.mode==Mode.COPY_PLACE,"restored draft requires preview again");
            s.act(Action.CANCEL);check(s.mode==Mode.CANVAS,"restored cancel returns correctly");
            s.restoreCopy(65,32,"CANVAS");check(!s.copying(),"invalid saved draft ignored");
            s.restoreCopy(64,48,"CANVAS");check(s.copying(),"shared destination restores only a proposal");s.act(Action.CANCEL);
            s.openSprite(0);s.region=dst;
            s.act(Action.COPY_SPRITE);s.pointCopy(0,0);s.act(Action.CONFIRM);s.act(Action.CONFIRM);
            check(s.cart().pixel(0,3,3)==12,"explicit replacement is allowed even for known hero pixels");
            check(s.cart().hasHero()==original.hasHero()&&s.cart().code().equals(original.code()),"copy never invents or rebinds a role");
        }
        System.out.println("CopyWorkflowTest: "+checks+" checks passed");
    }
}
