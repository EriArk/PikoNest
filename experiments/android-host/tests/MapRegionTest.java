import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MapRegionTest {
    static int checks;
    static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe edit accepted");}catch(IllegalArgumentException expected){checks++;}}
    static byte[] bytes(String s){return s.getBytes(StandardCharsets.UTF_8);}
    static String text(byte[] b){return new String(b,StandardCharsets.UTF_8);}
    static WorkshopCartridge blank(){return new WorkshopCartridge(bytes("pico-8 cartridge // http://www.pico-8.com\r\nversion 43\r\n__lua__\r\nfunction _draw()\r\n cls(1)\r\nend\r\n"));}
    static int[] sparse(){int[] values=new int[8192];Arrays.fill(values,-1);return values;}
    static MapRegion region(WorkshopCartridge c,MapRegion.Operation op,int x,int y,int x2,int y2,int dx,int dy){
        MapRegion r=new MapRegion(c,op,x,y);r.point(x2,y2);r.confirm();
        if(op!=MapRegion.Operation.CLEAR){r.point(dx,dy);r.confirm();}return r;
    }
    static class Port implements WorkshopSession.Port {
        boolean fail;int saves,launches;byte[] saved;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");saved=b;saves++;}
        public void launch(byte[] b){launches++;}
    }
    static void begin(WorkshopSession s,int operation){s.act(Action.DRAW_TOOLS);s.mapEditor.toolChoice=4+operation;s.act(Action.CONFIRM);}
    public static void main(String[] args){
        WorkshopCartridge b=blank();int[] edits=sparse();
        check(b.withMapCells(edits)==b,"empty sparse batch preserves exact bytes");
        edits[8191]=0;check(b.withMapCells(edits)==b,"zero shared edit does not append gfx");
        edits[4096]=0xab;edits[4159]=0xcd;edits[4160]=0xef;edits[8191]=0x12;edits[4095]=0xfe;
        WorkshopCartridge c=b.withMapCells(edits);
        check(c.map().tile(127,31)==254&&c.map().tile(0,32)==171&&c.map().tile(127,63)==18,"upper and shared in one document");
        check(c.sheetPixel(0,64)==11&&c.sheetPixel(1,64)==10&&c.sheetPixel(126,64)==13&&c.sheetPixel(127,64)==12,"low nibble first");
        check(c.sheetPixel(0,65)==15&&c.sheetPixel(1,65)==14&&c.sheetPixel(126,127)==2&&c.sheetPixel(127,127)==1,"row stride and final shared byte");
        check(!text(c.bytes()).replace("\r\n","").contains("\n"),"CRLF inherited in both added sections");
        check(c.withMapCells(edits)==c,"all unchanged edits preserve identity");
        refused(()->b.withMapCells(new int[4096]));edits[0]=256;refused(()->b.withMapCells(edits));edits[0]=-2;refused(()->b.withMapCells(edits));
        String framed=text(c.bytes()).replace("ba","BA").replace("dc","DC").replace("fe","FE");
        // Use mixed-case data and an opaque trailing section: only requested nibbles may change.
        framed=framed+"__future__\r\nOpaque Payload!\r\n";
        WorkshopCartridge mixed=new WorkshopCartridge(bytes(framed));int[] one=sparse();one[4096]=0xa9;
        String expected=framed.replace("BA000000","9A000000");
        check(text(mixed.withMapCells(one).bytes()).equals(expected),"shared byte changes only differing nibble; unknown section and casing preserved");
        WorkshopCartridge lf=new WorkshopCartridge(bytes(text(c.bytes()).replace("\r\n","\n").trim()));
        check(!text(lf.withMapCells(one).bytes()).endsWith("\n")&&!text(lf.withMapCells(one).bytes()).contains("\r"),"LF and absent trailing newline preserved");
        P8Document doc=P8Document.parse(b.bytes());int[] pixels=new int[16384];Arrays.fill(pixels,-1);pixels[9000]=16;
        refused(()->new P8Graphics(doc).withPixels(pixels));refused(()->new P8Graphics(doc).withPixels(null));

        int[] initial=sparse();initial[0]=1;initial[1]=2;initial[2]=3;initial[3]=4;
        WorkshopCartridge row=b.withMapCells(initial);
        MapRegion move=region(row,MapRegion.Operation.MOVE,0,0,2,0,1,0);
        check(move.proposal().cart.map().tile(0,0)==0&&move.proposal().cart.map().tile(1,0)==1&&move.proposal().cart.map().tile(2,0)==2&&move.proposal().cart.map().tile(3,0)==3,"overlap move snapshots before clearing");
        check(move.proposal().count==4&&move.proposal().overwritten==3,"counts final differing cells and nonzero target replacements");
        MapRegion copy=region(row,MapRegion.Operation.COPY,0,0,3,0,0,32);
        check(copy.proposal().sharedCells==4&&copy.proposal().pixels==4,"shared consequences counted by actual differing pixels");
        refused(()->copy.candidate(row));check(!copy.confirm()&&copy.phase==3,"shared review is a separate confirmation");
        check(copy.confirm()&&copy.candidate(row).map().tile(3,32)==4,"explicit shared commit");
        refused(()->copy.candidate(b));
        check(copy.back()==false&&copy.phase==2&&copy.back()==false&&copy.phase==1,"back from sharing and review keeps selection");
        MapRegion zeros=region(row,MapRegion.Operation.COPY,5,0,7,0,0,0);
        check(zeros.proposal().count==3&&zeros.proposal().overwritten==3&&zeros.proposal().cart.map().tile(0,0)==0,"copy includes empty cells, not a transparent paste");
        MapRegion noOp=region(row,MapRegion.Operation.MOVE,0,0,3,0,0,0);
        check(noOp.proposal().cart==row&&noOp.proposal().count==0,"move onto itself is no-op");
        MapRegion full=new MapRegion(row,MapRegion.Operation.COPY,127,63);full.point(0,0);full.confirm();full.move(999,999);
        check(full.width()==128&&full.height()==64&&full.dx==0&&full.dy==0,"reverse selection and full map bounds");
        full.confirm();check(full.proposal().cart==row,"full map identical paste");
        for(int phase=0;phase<4;phase++){
            MapRegion r=new MapRegion(row,MapRegion.Operation.MOVE,0,0);r.point(3,0);
            if(phase>0){r.confirm();r.point(0,32);}if(phase>1)r.confirm();if(phase>2)r.confirm();
            MapEditor restored=new MapEditor();restored.restore(r.encode(),row);
            check(restored.region.encode().equals(r.encode())&&restored.modal(),"journal preserves phase "+phase);
            refused(()->MapRegion.restore(r.encode(),b));
        }
        refused(()->MapRegion.restore(copy.encode().replace(";32;", ";64;"),row));
        MapEditor legacy=new MapEditor();legacy.tool=MapEditor.Tool.RECTANGLE;legacy.start(row);String old=legacy.encode();
        new MapEditor().restore(old,row);check(old.startsWith("1;"),"existing draft version retained");

        // Independent array oracle exercises overlap, zeros, both edges and the shared boundary.
        Random random=new Random(63);int[] seed=new int[8192];for(int i=0;i<seed.length;i++)seed[i]=random.nextInt(256);
        WorkshopCartridge randomCart=b.withMapCells(seed);
        for(int trial=0;trial<24;trial++){
            int x=random.nextInt(110),y=random.nextInt(48),w=1+random.nextInt(18),h=1+random.nextInt(16);
            int dx=trial%2==0?Math.min(128-w,x+1):random.nextInt(129-w),dy=trial%2==0?Math.min(64-h,y+1):random.nextInt(65-h);
            MapRegion.Operation op=MapRegion.Operation.values()[trial%3];MapRegion r=region(randomCart,op,x,y,x+w-1,y+h-1,dx,dy);
            int[] oracle=seed.clone();if(op!=MapRegion.Operation.COPY)for(int yy=0;yy<h;yy++)Arrays.fill(oracle,(y+yy)*128+x,(y+yy)*128+x+w,0);
            if(op!=MapRegion.Operation.CLEAR)for(int yy=0;yy<h;yy++)System.arraycopy(seed,(y+yy)*128+x,oracle,(dy+yy)*128+dx,w);
            P8Map result=r.proposal().cart.map();boolean equal=true;for(int i=0;i<8192;i++)equal&=result.tile(i%128,i/128)==oracle[i];
            check(equal,"independent full-map oracle "+trial);
        }
        Port port=new Port();WorkshopSession s=new WorkshopSession(row,port);s.switchTool(3);begin(s,0);
        s.act(Action.RIGHT);s.act(Action.CONFIRM);for(int i=0;i<4;i++)s.act(Action.REDO);
        s.act(Action.CONFIRM);s.act(Action.TEST);s.act(Action.MENU);s.switchTool(2);
        check(s.tool==3&&s.mapEditor.region.phase==2&&port.launches==0&&port.saves==0,"pending region traps Test/menu/tool changes");
        String journal=s.mapEditor.encode();s.act(Action.CONFIRM);check(port.saves==0&&s.mapEditor.region.phase==3,"shared review never saves");
        port.fail=true;s.act(Action.CONFIRM);
        check(s.mode==WorkshopSession.Mode.ERROR&&s.mapEditor.region!=null&&s.undoCount()==0&&Arrays.equals(s.cart().bytes(),row.bytes()),"failed write retains draft, source and history");
        port.fail=false;s.act(Action.CONFIRM);s.act(Action.CONFIRM);
        check(port.saves==1&&s.undoCount()==1&&s.mapEditor.region==null,"one atomic save and history for shared edit");
        s.act(Action.UNDO);check(Arrays.equals(s.cart().bytes(),row.bytes()),"Undo restores both sections byte for byte");s.act(Action.REDO);
        check(s.cart().map().tile(1,32)==2&&s.cart().sheetPixel(2,64)==2,"Redo restores gfx alias");
        s.mapEditor.restore(journal,row);s.act(Action.UNDO);check(s.mapEditor.region==null&&port.saves==3,"Y discards draft without invoking document Undo");
        begin(s,2);s.act(Action.CONFIRM);s.act(Action.CONFIRM);s.act(Action.CONFIRM);
        check(s.cart().map().tile(0,32)==0&&s.cart().map().tile(1,32)==2,"clear selected shared cell");
        s.act(Action.UNDO);check(s.cart().map().tile(0,32)==1,"clear Undo");
        System.out.println("MapRegionTest: "+checks+" checks passed");
    }
}
