import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import java.nio.file.*;
import java.util.*;
import java.io.*;

public final class AssetLibraryTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    interface Throwing {void run()throws Exception;}
    static void rejects(Throwing action)throws Exception{try{action.run();throw new AssertionError("invalid record accepted");}catch(IOException|IllegalArgumentException expected){checks++;}}
    static class Port implements WorkshopSession.Port {
        final Map<String,byte[]> library;int writes,stores,launches;boolean failCart,failAsset,failRead;byte[] launched;
        Port(Map<String,byte[]> shared){library=shared;}
        public void save(byte[] b)throws Exception{if(failCart)throw new IOException("disk full");writes++;}
        public void launch(byte[] b){launched=b.clone();launches++;}
        public String projectOrigin(){return "Fixture · test-project";}
        public List<SpriteAsset> assets()throws Exception{
            if(failRead)throw new IOException("unreadable library");List<SpriteAsset> result=new ArrayList<>();
            for(byte[] b:library.values())result.add(SpriteAsset.decode(b));return result;
        }
        public void storeAsset(SpriteAsset a)throws Exception{if(failAsset)throw new IOException("library full");library.put(a.id,a.encode());stores++;}
    }
    public static void main(String[] args)throws Exception{
        WorkshopCartridge source=new WorkshopCartridge(Files.readAllBytes(Paths.get(args[0])));
        SpriteRegion region=new SpriteRegion(0,0,32,24);
        source=source.withPixel(region,31,23,12);byte[] original=source.bytes();
        Map<String,byte[]> shared=new LinkedHashMap<>();Port p=new Port(shared);
        WorkshopSession s=new WorkshopSession(source,p);s.openSprite(0);s.region=region;
        s.act(Action.MENU);for(int i=0;i<5;i++)s.act(Action.DOWN);s.act(Action.CONFIRM);
        check(s.mode==Mode.ASSETS&&s.assets().isEmpty(),"menu reaches empty library with controller");
        s.act(Action.CONFIRM);check(s.mode==Mode.ASSET_SAVE&&p.stores==0,"empty library confirm previews current selection");
        s.act(Action.TEST);s.act(Action.NEXT);s.act(Action.UNDO);check(p.launches==0&&p.writes==0,"export preview traps runtime and mutations");
        check(s.mode==Mode.ASSET_CATEGORY,"Y opens category without saving");s.act(Action.CANCEL);
        check(s.mode==Mode.ASSET_SAVE&&shared.isEmpty(),"cancel category retains unsaved export");
        s.act(Action.CANCEL);check(s.mode==Mode.ASSETS&&shared.isEmpty(),"cancel export creates nothing");
        s.act(Action.CONTEXT);String id=s.assetDraft.id;p.failAsset=true;s.act(Action.CONFIRM);
        check(s.mode==Mode.ERROR&&s.assetDraft.id.equals(id)&&shared.isEmpty(),"failed export preserves draft for retry");
        p.failAsset=false;s.act(Action.CANCEL);s.act(Action.CONFIRM);
        check(s.mode==Mode.ASSETS&&shared.size()==1&&p.stores==1&&p.writes==0&&!s.canUndo(),"asset save does not change cart/history");
        same(s.cart().bytes(),original,"source byte exact after extraction");
        SpriteAsset asset=SpriteAsset.decode(shared.get(id));
        check(asset.width==32&&asset.height==24&&asset.pixel(31,23)==12&&asset.origin.equals(p.projectOrigin()),"size/pixels/origin survive serialization");
        same(asset.encode(),SpriteAsset.decode(asset.encode()).encode(),"deterministic record roundtrip");
        int[] colors=asset.colors();colors[0]=15;check(asset.pixel(0,0)!=15,"pixel data cannot be mutated by caller");
        byte[] invalid=asset.encode().clone();invalid[7]=2;rejects(()->SpriteAsset.decode(invalid));
        rejects(()->SpriteAsset.decode(Arrays.copyOf(asset.encode(),asset.encode().length-1)));
        rejects(()->SpriteAsset.decode(Arrays.copyOf(asset.encode(),asset.encode().length+1)));
        byte[] badColor=asset.encode().clone();badColor[badColor.length-1]=16;rejects(()->SpriteAsset.decode(badColor));
        byte[] zeros=new byte[8*8];
        rejects(()->new SpriteAsset("../bad","X","origin",asset.sourceHash,0,0,8,8,zeros));
        rejects(()->new SpriteAsset(id,"X\nY","origin",asset.sourceHash,0,0,8,8,zeros));
        rejects(()->new SpriteAsset(id,"X","origin",asset.sourceHash,127,0,8,8,zeros));
        // Project and UI instances can be discarded. The record owns all required pixels.
        source=null;s=null;p=null;
        for(int a=1;a<args.length;a++){
            byte[] cartBytes=Files.readAllBytes(Paths.get(args[a]));WorkshopCartridge cart=new WorkshopCartridge(cartBytes);
            Port targetPort=new Port(shared);WorkshopSession target=new WorkshopSession(cart,targetPort);
            target.act(Action.ASSETS);check(target.mode==Mode.ASSETS&&target.currentAsset()!=null,"library available without sprite tab or hero");
            target.act(Action.CONFIRM);check(target.mode==Mode.COPY_PLACE&&!target.copyOverlaps(),"independent source may occupy same coordinates");
            target.pointCopy(0,0);target.act(Action.CONFIRM);target.act(Action.CANCEL);target.act(Action.CANCEL);
            check(target.mode==Mode.ASSETS&&!target.copying()&&target.tool==0,"cancel restores browser and prior editing context");
            same(cartBytes,target.cart().bytes(),"cancel insert exact");
            target.act(Action.CONFIRM);target.pointCopy(0,0);target.act(Action.CONFIRM);targetPort.failCart=true;target.act(Action.CONFIRM);
            check(target.mode==Mode.ERROR&&target.copyAsset!=null&&!target.canUndo(),"failed insertion retains snapshot");
            targetPort.failCart=false;target.act(Action.CANCEL);target.act(Action.CONFIRM);
            check(target.mode==Mode.CANVAS&&target.selection().width==32&&targetPort.writes==1,"insert opens destination once saved");
            check(target.cart().hasHero()==cart.hasHero()&&target.cart().code().equals(cart.code()),"no role or Lua injection");
            for(int y=0;y<128;y++)for(int x=0;x<128;x++)check(target.cart().sheetPixel(x,y)==(x<32&&y<24?asset.pixel(x,y):cart.sheetPixel(x,y)),"only destination graphics changed");
            byte[] record=shared.get(id).clone();target.paintAt(31,23);same(shared.get(id),record,"editing insertion leaves library unchanged");
            target.act(Action.UNDO);target.act(Action.UNDO);same(target.cart().bytes(),cartBytes,"undo edits and entire insertion, including new gfx");
            target.act(Action.ASSETS);target.act(Action.CONFIRM);target.pointCopy(0,0);
            byte[] draft=target.copyAsset.encode();shared.clear();
            // Restore requires no originating cart, asset store record or template binding.
            WorkshopSession restored=new WorkshopSession(cart,targetPort);
            restored.restoreAssets("NAVIGATE",0);restored.restoreInsertion(SpriteAsset.decode(draft),0,0);
            check(restored.mode==Mode.COPY_PLACE,"restored insertion rechecks preview with independent snapshot");
            restored.act(Action.CONFIRM);restored.act(Action.CONFIRM);restored.act(Action.TEST);
            check(new WorkshopCartridge(targetPort.launched).sheetPixel(31,23)==12,"ordinary launched cart contains pixels without library");
            restored.act(Action.UNDO);same(restored.cart().bytes(),cartBytes,"restored insertion can undo byte exactly");
            shared.put(id,record);
        }
        Port readFail=new Port(shared);readFail.failRead=true;WorkshopSession failure=new WorkshopSession(new WorkshopCartridge(original),readFail);
        failure.act(Action.ASSETS);check(failure.mode==Mode.ERROR&&readFail.writes==0,"library read error cannot mutate project");
        failure.act(Action.CANCEL);failure.act(Action.CANCEL);check(failure.mode==Mode.NAVIGATE,"library error can return to project");
        System.out.println("AssetLibraryTest: "+checks+" checks passed");
    }
}
