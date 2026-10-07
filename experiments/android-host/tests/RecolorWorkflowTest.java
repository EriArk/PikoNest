import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import art.pikoos.lab.core.WorkshopSession.DrawTool;
import art.pikoos.p8.P8Document;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class RecolorWorkflowTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static class Port implements WorkshopSession.Port {
        int writes,launches;boolean fail;byte[] saved;
        public void save(byte[] bytes)throws Exception{if(fail)throw new Exception("disk full");writes++;saved=bytes.clone();}
        public void launch(byte[] bytes){launches++;same(saved,bytes,"launch uses committed pixels");}
    }
    static WorkshopSession editor(WorkshopCartridge cart,SpriteRegion r,Port p){
        WorkshopSession s=new WorkshopSession(cart,p);s.openSprite(0);s.region=r;s.mode=Mode.CANVAS;
        s.cursorX=1;s.cursorY=1;s.color=12;s.drawTool=DrawTool.FILL;s.zoom=true;return s;
    }
    static void begin(WorkshopSession s){
        s.act(Action.DRAW_TOOLS);while(s.drawToolCursor<6)s.act(Action.DOWN);while(s.drawToolCursor>6)s.act(Action.UP);s.act(Action.CONFIRM);
        check(s.mode==Mode.RECOLOR,"replacement menu entry reachable with controller");
    }
    static int verify(WorkshopCartridge before,WorkshopCartridge after,SpriteRegion r,int from,int to){
        int changed=0;
        for(int y=0;y<128;y++)for(int x=0;x<128;x++){
            int old=before.sheetPixel(x,y);boolean inside=x>=r.x&&x<r.x+r.width&&y>=r.y&&y<r.y+r.height;
            int expected=inside&&old==from?to:old;
            if(expected!=old)changed++;
            check(after.sheetPixel(x,y)==expected,"only matching pixels inside region change, including zero");
        }
        P8Document a=P8Document.parse(before.bytes()),b=P8Document.parse(after.bytes());
        for(int i=0;i<a.sections().size();i++)if(!a.sections().get(i).name.equals("gfx"))same(a.body(i),b.body(i),"other sections byte exact");
        return changed;
    }
    public static void main(String[] args)throws Exception{
        for(String file:args){
            String source=new String(Files.readAllBytes(Paths.get(file)),StandardCharsets.ISO_8859_1).replace("\n","\r\n")+"__future__\r\n\u0081opaque\r\n";
            WorkshopCartridge base=new WorkshopCartridge(source.getBytes(StandardCharsets.ISO_8859_1));
            for(SpriteRegion r:new SpriteRegion[]{new SpriteRegion(8,8,24,24),new SpriteRegion(64,32,32,24),new SpriteRegion(0,0,128,64)}){
                WorkshopCartridge original=base.withPixel(r,1,1,8).withPixel(r,r.width-1,r.height-1,8).withPixel(r,0,0,7);
                for(int[] pair:new int[][]{{8,12},{0,7},{7,0},{3,3},{15,2}}){
                    Port p=new Port();WorkshopSession s=editor(original,r,p);begin(s);
                    s.selectRecolorField(0);s.chooseRecolor(pair[0]);s.act(Action.CONTEXT);s.chooseRecolor(pair[1]);
                    for(Action a:new Action[]{Action.UNDO,Action.NEXT,Action.MENU,Action.REGION,Action.ASSETS,Action.SPRITE_SHEET})s.act(a);
                    s.switchTool(0);s.openSprite(1);s.openHero();
                    check(s.mode==Mode.RECOLOR&&s.selection()==r&&p.writes==0&&p.launches==0,"modal draft traps controller and direct touch actions");
                    same(original.bytes(),s.cart().bytes(),"preview never changes canonical cart");
                    int changes=verify(original,s.recolorPreview(),r,pair[0],pair[1]);
                    check(s.recolorCount()==changes,"preview shows exact changed count");byte[] expected=s.recolorPreview().bytes();
                    s.act(Action.CANCEL);same(original.bytes(),s.cart().bytes(),"cancel byte exact");
                    check(!s.recoloring()&&!s.canUndo()&&s.mode==Mode.CANVAS,"cancel clears draft");
                    s.restoreRecolor(pair[0],pair[1],1,"CANVAS");check(s.mode==Mode.RECOLOR&&p.writes==0,"restoration only previews");
                    if(changes>0){
                        p.fail=true;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&s.recoloring()&&!s.canUndo(),"failed save retains draft");
                        same(original.bytes(),s.cart().bytes(),"failed save retains source");s.act(Action.CANCEL);
                        check(s.mode==Mode.RECOLOR&&s.recolorFrom()==pair[0]&&s.recolorTo()==pair[1],"retry preserves selected colors");
                        p.fail=false;
                    }
                    s.act(Action.CONFIRM);same(expected,s.cart().bytes(),"commit matches preview");
                    check(!s.recoloring()&&s.mode==Mode.CANVAS&&p.writes==(changes>0?1:0),"one write or no-op");
                    check(s.drawTool==DrawTool.FILL&&s.cursorX==1&&s.cursorY==1&&s.zoom&&s.region==r&&s.color==12,"editing context preserved");
                    if(changes>0){s.act(Action.TEST);s.act(Action.UNDO);same(original.bytes(),s.cart().bytes(),"single undo restores exact source and missing rows");}
                    check(!s.canUndo(),"no stray history entries");
                }
            }
            SpriteRegion r=new SpriteRegion(0,0,16,16);Port p=new Port();WorkshopSession s=editor(base.withPixel(r,1,1,8),r,p);begin(s);
            check(s.recolorFrom()==8&&s.recolorTo()==12&&s.recolorField()==1,"default source under cursor and target brush color");
            s.chooseRecolor(14);s.chooseRecolor(12);same(s.cart().replaceColor(r,8,12).bytes(),s.recolorPreview().bytes(),"browsing is not cumulative");
            s.act(Action.CONTEXT);s.chooseRecolor(0);s.act(Action.LEFT);check(s.recolorFrom()==7,"horizontal wrap remains on palette row");
            s.act(Action.DOWN);check(s.recolorFrom()==15,"vertical row switch");s.act(Action.RIGHT);check(s.recolorFrom()==8,"second row wrap");
            s.act(Action.CANCEL);s.restoreRecolor(-1,12,1,"CANVAS");s.restoreRecolor(8,16,1,"CANVAS");s.restoreRecolor(8,12,2,"CANVAS");s.restoreRecolor(8,12,1,"ASSETS");
            check(!s.recoloring(),"invalid metadata ignored");s.region=new SpriteRegion(0,56,16,16);s.restoreRecolor(8,12,1,"CANVAS");check(s.recoloring(),"shared color restores a read-only proposal");s.act(Action.CANCEL);
            s.region=r;s.drawTool=DrawTool.LINE;s.lineX=s.lineY=0;s.act(Action.DRAW_TOOLS);check(s.mode==Mode.CANVAS&&s.pendingLine(),"line draft cannot be discarded");
        }
        WorkshopCartridge blank=new WorkshopCartridge(Files.readAllBytes(Paths.get(args[0])));SpriteRegion r=new SpriteRegion(0,0,16,16);
        same(blank.bytes(),blank.replaceColor(r,8,12).bytes(),"absent color does not materialize gfx");
        same(blank.bytes(),blank.replaceColor(r,0,0).bytes(),"zero identity does not materialize gfx");
        for(int invalid:new int[]{-1,16}){boolean rejected=false;try{blank.replaceColor(r,invalid,1);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"source must be a palette index");}
        // Uppercase, mixed row endings and opaque suffix: only replaced hex bytes may differ.
        WorkshopCartridge marked=blank.withPixel(r,1,1,12).withPixel(r,14,14,12).withPixel(r,2,2,10);
        P8Document doc=P8Document.parse(marked.bytes());int gfx=doc.uniqueSection("gfx");
        byte[] upper=new String(doc.body(gfx),StandardCharsets.US_ASCII).toUpperCase(java.util.Locale.ROOT).replaceFirst("\n","\r\n").getBytes(StandardCharsets.US_ASCII);
        byte[] raw=doc.edit(gfx,0,doc.body(gfx).length,upper).bytes(),next=new WorkshopCartridge(raw).replaceColor(r,12,8).bytes();
        check(raw.length==next.length,"byte layout unchanged");int differences=0;
        for(int i=0;i<raw.length;i++)if(raw[i]!=next[i]){differences++;check(raw[i]=='C'&&next[i]=='8',"only targeted color bytes change");}
        check(differences==2,"disconnected matching pixels both replaced");
        System.out.println("RecolorWorkflowTest: "+checks+" checks passed");
    }
}
