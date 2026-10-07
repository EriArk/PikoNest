import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.*;
import java.util.*;

/** User transactions across the ordinary gfx/map alias, not a custom resource model. */
public final class SharedOperationsTest {
    static int checks;
    static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    static void same(byte[] a,byte[] b,String message){check(Arrays.equals(a,b),message);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("Unsafe recovery accepted");}catch(IllegalArgumentException expected){checks++;}}
    static class Port implements WorkshopSession.Port {
        int saves,launches;boolean fail;byte[] saved,launched;
        public void save(byte[] bytes)throws Exception{if(fail)throw new Exception("disk full");saved=bytes.clone();saves++;}
        public void launch(byte[] bytes){launched=bytes.clone();launches++;}
    }
    static WorkshopSession editor(WorkshopCartridge c,SpriteRegion r,Port p){
        WorkshopSession s=new WorkshopSession(c,p);s.tool=2;s.browsingSprites=false;s.region=r;s.mode=Mode.CANVAS;s.cursorX=s.cursorY=0;return s;
    }
    static WorkshopCartridge patterned(String newline){
        WorkshopCartridge b=SharedEditingTest.blank(newline);int[] pixels=new int[16384];Arrays.fill(pixels,-1);
        for(int y=0;y<128;y++)for(int x=0;x<128;x++)if(x<40||x>111)pixels[y*128+x]=(x*3+y*5+x*y+y/16)%16;
        return b.withSheetPixels(pixels);
    }
    static void staged(WorkshopSession s,Port p,WorkshopCartridge base,WorkshopCartridge expected,Mode origin){
        check(s.mode==Mode.SHARED&&s.sharedEdit.origin.equals(origin.name()),"operation enters matching shared review: "+s.mode+" / "+origin+" / "+s.error+" / identical="+Arrays.equals(base.bytes(),expected.bytes()));
        same(base.bytes(),s.cart().bytes(),"proposal preserves saved bytes");same(expected.bytes(),s.sharedEdit.after.bytes(),"proposal matches full candidate");
        check(p.saves==0&&s.undoCount()==0,"review has no write or history");
        int launches=p.launches;s.act(Action.TEST);same(expected.bytes(),p.launched,"Test receives isolated candidate");check(p.launches==launches+1&&p.saves==0,"Test never applies");
        s.switchTool(0);s.openSprite(1);s.openUses(true);s.act(Action.MENU);s.act(Action.ASSETS);
        check(s.mode==Mode.SHARED&&s.tool==2&&s.uses==null,"direct resource entrances cannot discard the review");
    }
    static void formTest(WorkshopSession s,Port p,WorkshopCartridge expected){
        Mode mode=s.mode;int launches=p.launches;s.act(Action.TEST);
        check(s.mode==mode&&p.launches==launches+1&&p.saves==0&&s.undoCount()==0,"form Test keeps its intent and saved project");
        same(expected.bytes(),p.launched,"form Test receives exact operation preview");
    }
    static void apply(WorkshopSession s,Port p,WorkshopCartridge b,WorkshopCartridge expected){
        p.fail=true;s.act(Action.CONFIRM);check(s.mode==Mode.ERROR&&s.sharedEdit!=null&&s.undoCount()==0,"failed write keeps proposal and history");
        same(b.bytes(),s.cart().bytes(),"failed write does not publish");s.act(Action.CANCEL);check(s.mode==Mode.SHARED,"dismiss returns to review");
        p.fail=false;s.act(Action.CONFIRM);check(s.mode==Mode.CANVAS&&s.sharedEdit==null&&p.saves==1&&s.undoCount()==1,"one durable Apply and one Undo");
        same(expected.bytes(),s.cart().bytes(),"Apply matches reviewed bytes");s.act(Action.UNDO);same(b.bytes(),s.cart().bytes(),"Undo restores both resources exactly");
        s.act(Action.REDO);same(expected.bytes(),s.cart().bytes(),"Redo restores reviewed bytes exactly");
    }
    static void copyPixels(WorkshopCartridge b,WorkshopCartridge after,SpriteRegion source,SpriteRegion target){
        for(int y=0;y<128;y++)for(int x=0;x<128;x++){
            boolean inside=x>=target.x&&x<target.x+target.width&&y>=target.y&&y<target.y+target.height;
            check(after.sheetPixel(x,y)==(inside?b.sheetPixel(source.x+x-target.x,source.y+y-target.y):b.sheetPixel(x,y)),"snapshot copy preserves every pixel outside destination");
        }
    }
    public static void main(String[] args)throws Exception{
        for(String newline:new String[]{"\n","\r\n"}){
            WorkshopCartridge b=patterned(newline);
            for(SpriteTransform op:SpriteTransform.values()){
                SpriteRegion r=new SpriteRegion(0,56,16,16);WorkshopCartridge expected=b.transformed(r,op);Port p=new Port();WorkshopSession s=editor(b,r,p);
                s.restoreTransform(op.name(),"CANVAS");formTest(s,p,expected);s.act(Action.CONFIRM);staged(s,p,b,expected,Mode.TRANSFORM);
                byte[] journal=s.sharedEdit.encode();s.act(Action.CANCEL);check(s.mode==Mode.TRANSFORM&&s.transforming(),"Back retains transform intent");same(expected.bytes(),s.transformPreview().bytes(),"Back retains preview");
                WorkshopSession missing=editor(b,r,new Port());refused(()->missing.restoreShared(journal));
                WorkshopSession wrong=editor(b,r,new Port());wrong.restoreTransform(SpriteTransform.values()[(op.ordinal()+1)%4].name(),"CANVAS");refused(()->wrong.restoreShared(journal));
                Port q=new Port();WorkshopSession restored=editor(b,r,q);restored.restoreTransform(op.name(),"CANVAS");restored.restoreShared(journal);same(expected.bytes(),restored.sharedEdit.after.bytes(),"transform review recovers only matching intent");
                apply(restored,q,b,expected);check(!restored.transforming(),"Apply releases transform intent");
            }
            SpriteRegion rectangle=new SpriteRegion(0,64,24,16);Port p=new Port();WorkshopSession s=editor(b,rectangle,p);
            s.restoreTransform("ROTATE_CLOCKWISE","CANVAS");s.act(Action.CONFIRM);s.act(Action.TEST);check(s.mode==Mode.TRANSFORM&&p.saves==0&&p.launches==0,"rectangular quarter turn remains an explicit editor limitation");
            s.act(Action.CANCEL);s.restoreRecolor(0,14,1,"CANVAS");WorkshopCartridge expected=b.replaceColor(rectangle,0,14);formTest(s,p,expected);s.act(Action.CONFIRM);staged(s,p,b,expected,Mode.RECOLOR);
            byte[] journal=s.sharedEdit.encode();s.act(Action.CANCEL);check(s.mode==Mode.RECOLOR&&s.recoloring(),"Back retains color intent");
            Port q=new Port();WorkshopSession restored=editor(b,rectangle,q);restored.restoreRecolor(0,14,1,"CANVAS");restored.restoreShared(journal);apply(restored,q,b,expected);check(!restored.recoloring(),"Apply releases color intent");
            for(SpriteRegion[] pair:new SpriteRegion[][]{
                {new SpriteRegion(0,0,24,16),new SpriteRegion(96,112,24,16)},
                {new SpriteRegion(0,64,24,16),new SpriteRegion(8,72,24,16)},
                {new SpriteRegion(0,56,16,24),new SpriteRegion(112,104,16,24)}}){
                SpriteRegion source=pair[0],target=pair[1];p=new Port();s=editor(b,source,p);s.act(Action.COPY_SPRITE);s.pointCopy(target.x/8,target.y/8);s.act(Action.CONFIRM);
                check(s.mode==Mode.COPY_CONFIRM,"copy replacement review includes overlap");expected=b.copyRegion(source,target);copyPixels(b,expected,source,target);
                formTest(s,p,expected);s.act(Action.CONFIRM);staged(s,p,b,expected,Mode.COPY_CONFIRM);journal=s.sharedEdit.encode();s.act(Action.CANCEL);check(s.mode==Mode.COPY_CONFIRM&&s.copying(),"Back retains copy destination");
                q=new Port();restored=editor(b,source,q);restored.restoreCopy(target.x,target.y,"CANVAS");restored.restoreShared(journal);
                apply(restored,q,b,expected);check(!restored.copying()&&restored.selection().x==target.x&&restored.selection().y==target.y,"Apply opens copied destination");
            }
            // Reading shared gfx and copying to the upper sheet does not write the alias.
            SpriteRegion source=new SpriteRegion(0,64,16,16),target=new SpriteRegion(64,0,16,16);p=new Port();s=editor(b,source,p);s.act(Action.COPY_SPRITE);s.pointCopy(8,0);s.act(Action.CONFIRM);s.act(Action.CONFIRM);
            check(s.mode==Mode.CANVAS&&s.sharedEdit==null&&p.saves==1,"read-only shared source needs no false conflict review");same(b.copyRegion(source,target).bytes(),s.cart().bytes(),"upper copy exact");
            SpriteRegion scope=new SpriteRegion(0,56,24,24);String intent="2,1,7,5,11,2,9";p=new Port();s=editor(b,scope,p);s.restoreMove(intent,"CANVAS");expected=s.move.preview(b);formTest(s,p,expected);s.act(Action.CONFIRM);staged(s,p,b,expected,Mode.MOVE);
            journal=s.sharedEdit.encode();s.act(Action.CANCEL);check(s.mode==Mode.MOVE&&s.move.phase==2,"Back retains overlapping move intent");
            q=new Port();restored=editor(b,scope,q);restored.restoreMove(intent,"CANVAS");restored.restoreShared(journal);apply(restored,q,b,expected);check(restored.move==null,"Apply releases move intent");
            SpriteAsset asset=SpriteAsset.capture(b,new SpriteRegion(0,0,16,80),"Tall sprite","test");byte[] assetBytes=asset.encode();p=new Port();s=editor(b,source,p);s.restoreAssets("CANVAS",0);s.restoreInsertion(asset,112,48);
            check(s.mode==Mode.COPY_PLACE,"library insertion supports full-sheet height");s.act(Action.CONFIRM);expected=b.insert(asset,new SpriteRegion(112,48,16,80));s.act(Action.CONFIRM);staged(s,p,b,expected,Mode.COPY_CONFIRM);
            journal=s.sharedEdit.encode();q=new Port();restored=editor(b,source,q);restored.restoreAssets("CANVAS",0);restored.restoreInsertion(SpriteAsset.decode(assetBytes),112,48);restored.restoreShared(journal);apply(restored,q,b,expected);same(assetBytes,asset.encode(),"insertion never modifies independent asset pixels");
        }
        WorkshopCartridge blank=SharedEditingTest.blank("\n");SpriteRegion r=new SpriteRegion(0,64,16,16);Port p=new Port();WorkshopSession s=editor(blank,r,p);s.restoreTransform("FLIP_HORIZONTAL","CANVAS");s.act(Action.CONFIRM);
        same(blank.bytes(),s.cart().bytes(),"empty no-op creates no sections");check(s.sharedEdit==null&&p.saves==0&&s.undoCount()==0,"no-op has no conflict or history");
        s.restoreRecolor(0,0,1,"CANVAS");s.act(Action.CONFIRM);check(p.saves==0&&s.sharedEdit==null,"identity color replacement remains exact no-op");
        System.out.println("SharedOperationsTest: "+checks+" checks passed");
    }
}
