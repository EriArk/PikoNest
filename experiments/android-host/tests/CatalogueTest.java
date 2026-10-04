import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.*;

public final class CatalogueTest {
    static int checks;
    static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe branch accepted");}catch(IllegalArgumentException expected){checks++;}}
    static LuaDraft proposal(String source){return WorldCameraTest.proposal(source,"if_else");}
    static final class Port implements WorkshopSession.Port {
        final List<SpriteAsset> records=new ArrayList<>();boolean fail;int writes;
        public void save(byte[] bytes){}
        public void launch(byte[] bytes){}
        public List<SpriteAsset> assets(){return new ArrayList<>(records);}
        public void categorizeAsset(SpriteAsset expected,SpriteAsset.Category category)throws Exception{
            if(fail)throw new Exception("disk full");int n=-1;for(int j=0;j<records.size();j++)if(Arrays.equals(records.get(j).encode(),expected.encode()))n=j;
            if(n<0)throw new Exception("stale asset");records.set(n,expected.withCategory(category));writes++;
        }
    }
    public static void main(String[] args)throws Exception{
        for(String nl:new String[]{"\n","\r\n","\r"}){
            String src="function _update()"+nl+"  -- existing"+nl+"end"+nl;
            LuaDraft d=proposal(src);d.point(1,0);d.insertion.beginPreview();LuaDraft restored=LuaDraft.restore(d.encode());d.applyInsert();restored.applyInsert();
            check(d.text().equals(restored.text()),"restore preview");check(d.lineText(d.line()).trim().isEmpty()&&d.line()==2,"caret in true branch");
            String inserted=d.text();check(inserted.contains("  else"+nl),"indented else");d.history(false);check(d.text().equals(src),"single undo");d.history(true);check(d.text().equals(inserted),"redo");
            d.replace("score+=1");d.point(4,4);d.replace("score=0");String bodies=d.text();d.point(1,0);d.beginParameters();
            check(d.insertion.item().id.equals("compare"),"existing comparison reused");d.insertion.set(2,"2");restored=LuaDraft.restore(d.encode());d.applyInsert();restored.applyInsert();
            check(d.text().equals(restored.text()),"restore header edit");check(d.text().equals(bodies.replace("phase==0","phase==2")),"both bodies preserved");
            d.history(false);check(d.text().equals(bodies),"header undo");
        }
        for(String value:new String[]{"0 or ready","0 then evil() end if true","0 -- hide","0,2","f("}){LuaDraft d=proposal("\n");d.insertion.set(2,value);refused(d::applyInsert);check(!d.dirty(),"source preserved");}
        LuaDraft grouped=proposal("\n");grouped.insertion.set(0,"(a or b)");grouped.applyInsert();check(grouped.text().contains("if (a or b)==0 then"),"explicit grouping");
        ToolCatalogue c=new ToolCatalogue();LuaInsert i=new LuaInsert();Set<Integer> seen=new HashSet<>();
        for(int group=2;group<ToolCatalogue.CATEGORIES.length;group++){c.category=group;for(int n:c.items())check(seen.add(n),"single semantic group");}
        check(seen.size()==LuaInsert.ITEMS.length,"all tools categorized");c.category=1;check(c.items().isEmpty(),"empty favorites");c.move(1,i);c.toggle(30);c.normalize(i);check(i.conditionalBranches(),"favorite selectable");
        c.toggle(7);String saved=c.encode();ToolCatalogue again=new ToolCatalogue();again.restore(saved,1);check(again.items().equals(c.items()),"favorites persist stable IDs");again.toggle(30);again.normalize(i);check(i.selected==7,"removed favorite moves to remaining tool");again.toggle(7);check(again.items().isEmpty(),"remove last favorite");again.restore("unknown,if_else,if_else",100);check(again.encode().equals("if_else")&&again.category==8,"unknown IDs and bounds");
        WorkshopSession session=new WorkshopSession(LuaCallTest.cart("\n"),new Port());session.switchTool(1);session.act(Action.CONFIRM);session.act(Action.CONTEXT);session.act(Action.RIGHT);session.act(Action.CONFIRM);check(session.codeDraft.insertion.screen==LuaInsert.Screen.CATALOG,"empty favorites cannot insert");session.act(Action.RIGHT);check(session.toolCatalogue.category==2,"category navigation");
        for(int n=0;n<5;n++)session.act(Action.DOWN);check(session.codeDraft.insertion.conditionalBranches(),"branch rule");session.act(Action.UNDO);check(session.toolCatalogue.favorite(30),"controller favorite");session.act(Action.CONFIRM);for(int n=0;n<3;n++)session.act(Action.DOWN);session.act(Action.CONFIRM);session.act(Action.TEST);check(!session.codeDraft.dirty(),"Start cannot apply preview");session.act(Action.CONFIRM);check(session.codeDraft.text().contains("else"),"controller apply");
        SpriteAsset old=SpriteAsset.capture(LuaCallTest.cart("\n"),new SpriteRegion(0,0,8,8),"tile","source");byte[] v1=old.encode();
        check(SpriteAsset.decode(v1).category==SpriteAsset.Category.UNFILED&&Arrays.equals(v1,SpriteAsset.decode(v1).encode()),"old record byte stable");
        for(SpriteAsset.Category category:SpriteAsset.Category.values()){
            SpriteAsset a=old.withCategory(category),b=SpriteAsset.decode(a.encode());check(b.category==category&&Arrays.equals(a.encode(),b.encode()),"category round trip");
            check(Arrays.equals(b.colors(),old.colors())&&b.sourceHash.equals(old.sourceHash)&&b.id.equals(old.id),"pixel and provenance preservation");
            check(b.withTitle("renamed").category==category,"rename preserves category");
        }
        check(Arrays.equals(old.withCategory(SpriteAsset.Category.UI).withCategory(SpriteAsset.Category.UNFILED).encode(),v1),"remove category restores v1");
        Port port=new Port();port.records.add(old);WorkshopSession s=new WorkshopSession(LuaCallTest.cart("\n"),port);s.act(Action.ASSETS);s.act(Action.MENU);s.act(Action.DOWN);s.act(Action.CANCEL);check(port.writes==0,"cancel category");
        s.act(Action.MENU);s.act(Action.DOWN);port.fail=true;s.act(Action.CONFIRM);check(s.mode==WorkshopSession.Mode.ERROR&&port.records.get(0).category==SpriteAsset.Category.UNFILED,"failed write preserves category");s.act(Action.CANCEL);port.fail=false;s.act(Action.CONFIRM);check(port.writes==1&&s.currentAsset().category==SpriteAsset.Category.TILES,"retry category");
        s.act(Action.NEXT);check(s.assets().isEmpty(),"unfiled filter excludes tile");s.act(Action.CONFIRM);check(s.mode==WorkshopSession.Mode.ASSETS,"empty filter safe");s.act(Action.NEXT);check(s.currentAsset().id.equals(old.id),"tile filter");s.act(Action.MENU);s.act(Action.UP);s.act(Action.CONFIRM);check(s.currentAsset()==null&&s.assets().isEmpty(),"retag last item in filtered view");s.act(Action.PREVIOUS);check(s.currentAsset()!=null,"unfiled category visible");
        System.out.println("CatalogueTest: "+checks+" checks passed");
    }
}
