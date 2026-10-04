import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import java.util.*;
import java.io.*;

public final class AssetFavoritesTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static class Port implements WorkshopSession.Port {
        final Map<String,byte[]> records=new LinkedHashMap<>();boolean fail;int writes,launches,stars;
        public void save(byte[] bytes){writes++;}public void launch(byte[] bytes){launches++;}
        public List<SpriteAsset> assets()throws Exception{List<SpriteAsset> out=new ArrayList<>();for(byte[] b:records.values())out.add(SpriteAsset.decode(b));out.sort(Comparator.comparing(a->a.title));return out;}
        void update(SpriteAsset old,SpriteAsset next)throws Exception{
            if(fail)throw new IOException("disk full");if(!Arrays.equals(old.encode(),records.get(old.id)))throw new IOException("stale");records.put(old.id,next.encode());
        }
        public void favoriteAsset(SpriteAsset old,boolean star)throws Exception{update(old,old.withFavorite(star));stars++;}
        public void renameAsset(SpriteAsset old,String name)throws Exception{update(old,old.withTitle(name));}
        public void categorizeAsset(SpriteAsset old,SpriteAsset.Category category)throws Exception{update(old,old.withCategory(category));}
    }
    public static void main(String[] args)throws Exception{
        WorkshopCartridge cart=LuaCallTest.cart("function _draw()\n cls(1)\nend\n");
        SpriteAsset original=SpriteAsset.capture(cart,new SpriteRegion(0,0,8,8),"A","Source");
        for(SpriteAsset.Category c:SpriteAsset.Category.values()){
            SpriteAsset old=original.withCategory(c);byte[] legacy=old.encode();SpriteAsset star=SpriteAsset.decode(old.withFavorite(true).encode());
            check(!SpriteAsset.decode(legacy).favorite,"old assets remain unstarred");
            check(star.favorite&&star.category==c,"favorite and category round trip");
            check(Arrays.equals(star.withFavorite(false).encode(),legacy),"remove restores exact old record");
            check(Arrays.equals(star.colors(),old.colors())&&star.sourceHash.equals(old.sourceHash)&&star.origin.equals(old.origin)&&star.sourceX==old.sourceX&&star.width==old.width,"pixels and provenance intact");
            check(star.withTitle("Renamed").favorite&&star.withCategory(SpriteAsset.Category.UI).favorite,"metadata edits retain favorite");
            byte[] junk=Arrays.copyOf(star.encode(),star.encode().length+1);try{SpriteAsset.decode(junk);throw new AssertionError();}catch(IOException expected){checks++;}
        }
        Port port=new Port();port.records.put(original.id,original.encode());SpriteAsset other=SpriteAsset.capture(cart,new SpriteRegion(8,0,8,8),"B","Other");port.records.put(other.id,other.encode());
        WorkshopSession s=new WorkshopSession(cart,port);s.act(Action.ASSETS);s.act(Action.PREVIOUS);
        check(s.assetFilter==WorkshopSession.favoriteAssetFilter()&&s.currentAsset()==null,"one step from All to empty favorites");
        s.act(Action.CONFIRM);s.act(Action.UNDO);s.act(Action.TEST);check(s.mode==Mode.ASSETS&&port.writes==0&&port.launches==0&&port.stars==0,"empty commands safe");
        s.act(Action.NEXT);port.fail=true;s.act(Action.UNDO);check(s.mode==Mode.ERROR&&!s.currentAsset().favorite&&!SpriteAsset.decode(port.records.get(original.id)).favorite,"failed write keeps visible and durable state");
        s.act(Action.CANCEL);port.fail=false;s.act(Action.UNDO);check(s.currentAsset().favorite&&port.stars==1,"retry favorite");
        s.act(Action.RIGHT);s.act(Action.UNDO);s.act(Action.PREVIOUS);check(s.assets().size()==2&&s.assetIndex==0,"favorites across categories");
        s.act(Action.UNDO);check(s.assets().size()==1&&s.currentAsset().id.equals(other.id)&&s.assetIndex==0,"removing first follows remaining item");
        s.act(Action.UNDO);check(s.assets().isEmpty()&&s.currentAsset()==null&&s.assetIndex==0,"removing last leaves safe empty view");
        s.act(Action.NEXT);s.act(Action.UNDO);s.act(Action.MENU);s.act(Action.UNDO);check(s.mode==Mode.NAME,"rename through properties");
        s.nameEditor.restoreText("Z");s.act(Action.TEST);check(s.currentAsset().favorite&&s.currentAsset().title.equals("Z"),"rename follows selected ID with star");
        s.act(Action.MENU);s.act(Action.DOWN);s.act(Action.CONFIRM);check(s.currentAsset().category==SpriteAsset.Category.TILES&&s.currentAsset().favorite,"category retains star");
        WorkshopSession next=new WorkshopSession(cart,port);next.assetFilter=WorkshopSession.favoriteAssetFilter();next.restoreAssets("NAVIGATE",0);next.selectAssetId(original.id);
        check(next.currentAsset().id.equals(original.id)&&next.currentAsset().favorite,"other project/restart reads same favorite");
        next.act(Action.CONFIRM);check(next.mode==Mode.COPY_PLACE&&next.copyAsset.favorite,"favorite opens ordinary insertion");
        next.act(Action.CANCEL);check(next.mode==Mode.ASSETS&&next.assetFilter==WorkshopSession.favoriteAssetFilter(),"cancel placement restores filter");
        check(Arrays.equals(next.cart().bytes(),cart.bytes())&&port.writes==0&&!next.canUndo(),"library metadata does not modify cart or undo");
        SpriteAsset external=SpriteAsset.decode(port.records.get(original.id)).withTitle("External");port.records.put(original.id,external.encode());next.act(Action.UNDO);
        check(next.mode==Mode.ERROR&&Arrays.equals(external.encode(),port.records.get(original.id)),"stale metadata never overwrites concurrent change");
        next.act(Action.CANCEL);next.act(Action.CANCEL);next.act(Action.ASSETS);check(next.currentAsset().title.equals("External"),"reopening refreshes stale snapshot");
        System.out.println("AssetFavoritesTest: "+checks+" checks passed");
    }
}
