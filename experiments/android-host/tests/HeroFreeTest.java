import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class HeroFreeTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static byte[] bytes(String s){return s.getBytes(StandardCharsets.ISO_8859_1);}
    static String text(byte[] b){return new String(b,StandardCharsets.ISO_8859_1);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static class Port implements WorkshopSession.Port {
        byte[] saved,launched;boolean fail;int writes;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("full");saved=b.clone();writes++;}
        public void launch(byte[] b){launched=b.clone();}
    }
    static void noHero(WorkshopCartridge c){
        check(!c.hasHero()&&c.heroSlot()==-1&&c.line(0)==-1,"no invented binding");
        check(!c.code().contains("hero_")&&!c.code().contains("speed=")&&!c.code().contains("jump="),"no generated platformer Lua");
    }
    public static void main(String[] args)throws Exception{
        byte[] blank=Files.readAllBytes(Paths.get(args[0]));
        byte[] puzzle=Files.readAllBytes(Paths.get(args[1]));
        byte[] garden=Files.readAllBytes(Paths.get(args[2]));
        String header="pico-8 cartridge // http://www.pico-8.com\nversion 43\n";
        for(String source:new String[]{text(blank),header,header+"__lua__\n",header+"__lua__\n__gfx__\n",text(blank).replace("\n","\r\n")+"__future__\r\n\u0081opaque\r\n"}){
            WorkshopCartridge c=new WorkshopCartridge(bytes(source));noHero(c);same(c.bytes(),bytes(source),"open byte exact");
            same(c.withPixel(0,0,0,0).bytes(),c.bytes(),"empty erase does not add gfx");
            WorkshopCartridge changed=c.withPixel(new SpriteRegion(24,16,24,24),3,7,12);
            noHero(changed);check(changed.sheetPixel(27,23)==12,"write sparse sheet at actual coordinates");
            check(text(changed.bytes()).startsWith(source),"first graphics edit appends without rewriting existing sections");
            check(changed.code().equals(c.code()),"Lua untouched when gfx created");
            WorkshopCartridge fill=changed.withFill(new SpriteRegion(24,16,24,24),0,0,8);
            check(fill.sheetPixel(27,23)==12&&fill.sheetPixel(24,16)==8&&fill.sheetPixel(23,16)==0,"fill bounded on newly created section");
        }
        WorkshopCartridge c=new WorkshopCartridge(puzzle);noHero(c);
        WorkshopCartridge changed=c.withPixel(0,7,7,14);
        check(c.code().equals(changed.code()),"puzzle sprite edit does not rewrite rules");
        P8Document before=P8Document.parse(puzzle),after=P8Document.parse(changed.bytes());
        same(before.body(before.uniqueSection("lua")),after.body(after.uniqueSection("lua")),"exact puzzle Lua preservation");
        check(c.firstFreeSlot()==-1,"no guessed free slots for arbitrary Lua");
        try{c.withHero(1);throw new AssertionError("hero injected");}catch(IllegalArgumentException expected){checks++;}
        Port p=new Port();WorkshopSession s=new WorkshopSession(c,p);
        s.act(Action.CONFIRM);check(s.tool==2&&s.mode==Mode.SHEET,"generic workshop opens sprite sheet");
        s.act(Action.ASSIGN_HERO);check(p.writes==0&&s.mode==Mode.SHEET,"hero action unavailable");
        s.selectSheet(10);check(s.mode==Mode.HELP,"generic help");s.act(Action.CONFIRM);
        s.selectSheet(9);check(s.mode==Mode.COPY_PLACE,"generic copy available");s.act(Action.CANCEL);s.switchTool(1);
        s.act(Action.CONFIRM);check(s.mode==Mode.NAVIGATE&&p.writes==0,"code is read only");
        s.act(Action.CONTEXT);s.act(Action.CONFIRM);check(s.codeLine>=0&&s.mode==Mode.NAVIGATE,"generic help returns to valid code line");
        s.openSprite(0);s.color=14;s.paintAt(7,7);check(p.writes==1&&s.cart().pixel(0,7,7)==14,"controller/editor write");
        s.act(Action.TEST);same(p.saved,p.launched,"runtime receives saved puzzle");
        s.act(Action.UNDO);same(puzzle,s.cart().bytes(),"undo exact puzzle");
        p.fail=true;s.paintAt(7,7);check(s.mode==Mode.ERROR&&!s.canUndo(),"failed save does not publish history");same(puzzle,s.cart().bytes(),"failed save preserves cart");
        s.act(Action.CANCEL);p.fail=false;s.paintAt(7,7);s.act(Action.CANCEL);
        s.act(Action.REGION);s.act(Action.CONFIRM);s.act(Action.RIGHT);s.act(Action.DOWN);s.act(Action.CONFIRM);
        check(s.mode==Mode.CANVAS&&s.selection().width==16,"region editor works without hero");
        noHero(s.cart());
        Port bp=new Port();WorkshopSession bs=new WorkshopSession(new WorkshopCartridge(blank),bp);
        bs.openSprite(0);bs.color=11;bs.paintAt(0,0);bs.act(Action.UNDO);same(blank,bs.cart().bytes(),"undo removes newly added gfx section");
        // Every surfaced navigation route can run without touching template-only fields.
        for(int tool=0;tool<3;tool++)for(int focus=0;focus<7;focus++){
            WorkshopSession nav=new WorkshopSession(c,new Port());nav.switchTool(tool);
            if(tool==2)nav.openSprite(0);nav.select(focus);
            nav.act(Action.CONTEXT);nav.act(Action.CANCEL);nav.act(Action.TEST);
            check(nav.mode!=Mode.ERROR,"generic navigation route "+tool+":"+focus);
        }
        LibraryWorkflowTest.Port lp=new LibraryWorkflowTest.Port();
        LibrarySession lib=new LibrarySession(lp,garden,blank,puzzle);lib.refresh(null);
        lib.act(Action.CONFIRM);lib.act(Action.RIGHT);lib.act(Action.CANCEL);check(lp.creates==0,"template choice cancel writes nothing");
        lib.act(Action.CONFIRM);lib.act(Action.CONFIRM);check(lp.opened.equals("blank-0001"),"blank creation controller path");same(blank,lp.cart.bytes(),"blank created exact");
        lib.refresh(lp.opened);lib.command(1);lib.act(Action.RIGHT);lib.act(Action.CONFIRM);
        check(lp.opened.equals("puzzle-0001"),"puzzle creation controller path");same(puzzle,lp.cart.bytes(),"puzzle created exact");
        lib.refresh(lp.opened);lib.act(Action.CONTEXT);same(puzzle,lp.cart.bytes(),"hero-free cart copy exact");
        check(lp.opened.equals("remix-0001"),"copy uses independent ID");
        check(new WorkshopCartridge(garden).hasHero(),"existing platformer binding retained");
        System.out.println("HeroFreeTest: "+checks+" checks passed");
    }
}
