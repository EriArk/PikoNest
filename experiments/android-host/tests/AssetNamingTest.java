import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public final class AssetNamingTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static class Port implements WorkshopSession.Port {
        final Map<String,byte[]> records=new LinkedHashMap<>();int writes,renames,creates,launches;boolean fail;
        public void save(byte[] b){writes++;}
        public void launch(byte[] b){launches++;}
        public List<SpriteAsset> assets()throws Exception{
            List<SpriteAsset> list=new ArrayList<>();for(byte[] b:records.values())list.add(SpriteAsset.decode(b));
            list.sort((a,b)->a.title.compareTo(b.title));return list;
        }
        public void storeAsset(SpriteAsset a){creates++;records.put(a.id,a.encode());}
        public void renameAsset(SpriteAsset expected,String title)throws Exception{
            if(fail)throw new IOException("disk full");
            if(!Arrays.equals(records.get(expected.id),expected.encode()))throw new IOException("stale record");
            records.put(expected.id,expected.withTitle(title).encode());renames++;
        }
    }
    static void type(WorkshopSession s,String value){
        for(char c:value.toCharArray()){
            int found=-1;
            for(int i=0;i<s.nameEditor.count();i++)if(s.nameEditor.character(i)==c){found=i;break;}
            check(found>=0,"character available: "+c);s.typeName(found);
        }
    }
    public static void main(String[] args)throws Exception{
        NameEditor e=new NameEditor("Спрайт 1");check(e.count()==50,"Russian grid size");
        e.type(11);check(e.text().equals("К")&&!e.replaceAll,"first key replaces selected title");
        e.uppercase=false;e.type(15);e.type(19);check(e.text().equals("Кот"),"Cyrillic case-aware typing");
        e.erase();check(e.text().equals("Ко"),"erase last character");
        e.language();check(e.count()==50,"Latin grid size");e.type(0);check(e.text().equals("Коa"),"switch language retains text");
        e.replaceAll=true;e.erase();check(e.text().isEmpty()&&e.value()==null&&!e.warning.isEmpty(),"empty cannot commit");
        e.restoreText("x");e.replaceAll=false;for(int i=0;i<100;i++)e.type(0);check(e.text().length()==80,"typing has bounded length");
        e.erase();check(e.text().length()==79,"limit recoverable");
        e.restoreText("X\ud83d\ude00");e.erase();check(e.text().equals("X"),"backspace never splits a surrogate pair");
        e.restoreText("  name  ");check(e.value().equals("name"),"commit trims outer whitespace only");
        for(int i=0;i<200;i++){e.move(1,1);}check(e.key==49,"grid upper bound");
        for(int i=0;i<200;i++){e.move(-1,-1);}check(e.key==0,"grid lower bound");
        try{e.restoreText("bad\nname");throw new AssertionError("control accepted");}catch(IllegalArgumentException expected){checks++;}
        WorkshopCartridge cart=new WorkshopCartridge(Files.readAllBytes(Paths.get(args[0])));byte[] original=cart.bytes();
        Port p=new Port();SpriteAsset asset=SpriteAsset.capture(cart,new SpriteRegion(0,0,16,16),"Z sprite","Origin");
        SpriteAsset second=SpriteAsset.capture(cart,new SpriteRegion(0,0,16,16),"B sprite","Other");
        p.records.put(asset.id,asset.encode());p.records.put(second.id,second.encode());
        WorkshopSession s=new WorkshopSession(cart,p);s.act(Action.ASSETS);s.selectAssetId(asset.id);s.act(Action.UNDO);
        check(s.mode==Mode.NAME&&s.nameEditor.replaceAll,"Y starts rename with entire title selected");
        type(s,"КОТ");s.act(Action.CANCEL);
        same(p.records.get(asset.id),asset.encode(),"cancel leaves record exact");check(s.currentAsset().id.equals(asset.id),"cancel retains selection");
        s.act(Action.UNDO);s.act(Action.TEST);check(p.renames==0&&p.launches==0,"unchanged Done performs no write or launch");
        s.act(Action.UNDO);s.act(Action.CONTEXT);s.act(Action.TEST);check(s.mode==Mode.NAME,"blank title remains in editor");
        s.act(Action.NEXT);check(s.nameEditor.latin&&s.tool==0,"L/R changes alphabet without changing tool");
        type(s,"A CAT");p.fail=true;s.act(Action.TEST);
        check(s.mode==Mode.ERROR&&s.nameEditor.text().equals("A CAT"),"failure retains draft");
        same(p.records.get(asset.id),asset.encode(),"failed rename leaves record exact");
        p.fail=false;s.act(Action.CONFIRM);check(s.mode==Mode.NAME,"error returns to name editor");s.act(Action.TEST);
        SpriteAsset renamed=SpriteAsset.decode(p.records.get(asset.id));
        check(s.mode==Mode.ASSETS&&s.currentAsset().id.equals(asset.id)&&s.assetIndex==0,"rename resorts and follows identity");
        same(renamed.withTitle(asset.title).encode(),asset.encode(),"only title changed; ID, provenance, pixels exact");
        check(!s.canUndo()&&p.writes==0&&p.launches==0,"metadata rename does not pollute cart undo or runtime");same(s.cart().bytes(),original,"cart bytes untouched");
        s.act(Action.CANCEL);p.records.put(second.id,second.withTitle("0 first").encode());s.act(Action.ASSETS);
        check(s.currentAsset().id.equals(asset.id)&&s.assetIndex==1,"refresh follows ID across order changes");
        s.act(Action.UNDO);type(s,"Д");p.records.put(asset.id,renamed.withTitle("External").encode());s.act(Action.TEST);
        check(s.mode==Mode.ERROR&&SpriteAsset.decode(p.records.get(asset.id)).title.equals("External"),"stale snapshot cannot overwrite concurrent rename");
        s.act(Action.CANCEL);s.act(Action.CANCEL);s.act(Action.CANCEL);s.act(Action.ASSETS);s.act(Action.UNDO);
        type(s,"НОВОЕ");
        NameEditor restoredEditor=new NameEditor(s.nameEditor.text());restoredEditor.replaceAll=false;restoredEditor.key=17;
        WorkshopSession restored=new WorkshopSession(cart,p);restored.restoreAssets("NAVIGATE",0);
        restored.restoreName(SpriteAsset.decode(s.nameTarget.encode()),false,restoredEditor);
        check(restored.mode==Mode.NAME&&restored.nameEditor.text().equals("НОВОЕ")&&p.renames==1,"restore never commits");
        restored.act(Action.TEST);check(restored.currentAsset().title.equals("НОВОЕ"),"restored draft renames same resource");
        // Name a new export, while preserving explicit save confirmation.
        WorkshopSession exporting=new WorkshopSession(cart,p);exporting.openSprite(0);exporting.act(Action.ASSETS);exporting.act(Action.CONTEXT);exporting.act(Action.CONTEXT);
        type(exporting,"КОТИК");exporting.act(Action.TEST);
        check(exporting.mode==Mode.ASSET_SAVE&&exporting.assetDraft.title.equals("КОТИК")&&p.creates==0,"new name returns to unsaved export preview");
        String id=exporting.assetDraft.id;exporting.act(Action.CONFIRM);check(p.records.containsKey(id)&&exporting.currentAsset().id.equals(id),"named export saves and stays selected");
        // Crash window: storage succeeded, but old UI preferences still contain export preview.
        WorkshopSession retry=new WorkshopSession(cart,p);retry.restoreAssets("NAVIGATE",0);
        retry.assetDraft=SpriteAsset.decode(p.records.get(id));retry.mode=Mode.ASSET_SAVE;retry.act(Action.CONFIRM);
        check(retry.assets().size()==p.records.size(),"retry does not duplicate already published ID in browser");
        System.out.println("AssetNamingTest: "+checks+" checks passed");
    }
}
