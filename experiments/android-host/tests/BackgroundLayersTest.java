import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.*;

public final class BackgroundLayersTest {
    static int checks;
    static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe change");}catch(IllegalArgumentException expected){checks++;}}
    static String layer(int y,String nl){LuaInsert f=BackgroundLayerTest.form();f.set(4,""+y);return f.code().replace("\n",nl);}
    static LuaDraft draft(String source){return new LuaDraft(LuaNavigationTest.cart(source),0);}
    public static void main(String[] args){
        for(String nl:new String[]{"\n","\r\n","\r"}){
            String prefix="function _draw()"+nl+"cls(1)"+nl,a=layer(12,nl),b=layer(123,nl),suffix="print('hud') -- keep"+nl+"end"+nl;
            String src=prefix+a+nl+b+suffix;
            LuaDraft d=draft(src);d.beginLayers();check(d.layers.entries.size()==2,"find pair");
            check(d.layers.line(0)==3,"source line");d.layerMenu=1;d.reviewLayer();check(!d.dirty(),"review read only");
            d.layerBefore=true;LuaDraft restored=LuaDraft.restore(d.encode());check(restored.layerBefore&&restored.layerChange!=null,"review restore");
            restored.applyLayer();d.applyLayer();String swapped=prefix+b+nl+a+suffix;
            check(d.text().equals(swapped)&&restored.text().equals(swapped),"swap full unequal blocks only");
            check(d.layers.index==1&&d.layers.current().form.value(4).equals("12"),"selected block follows swap");
            d.layerHistory(false);check(d.text().equals(src),"one undo");d.layerHistory(true);check(d.text().equals(swapped),"redo");
            d.layerMenu=0;d.reviewLayer();d.applyLayer();check(d.text().equals(src),"reverse restores exact bytes");
            d.layers.index=1;d.openLayer(false);d.insertion.set(5,"-7");d.insertion.beginPreview();
            restored=LuaDraft.restore(d.encode());restored.applyInsert();check(restored.panel==LuaDraft.Panel.LAYERS&&restored.layers.index==1,"form returns to same layer");
            check(restored.text().equals(src.substring(0,prefix.length()+a.length()+nl.length())+b.replace("*4)","*-7)")+suffix),"only selected layer fields");
            d.cancelInsert();check(d.text().equals(src)&&d.layers.index==1,"field cancel");
            d.openLayer(false);d.beginBackgroundPick();d.backgroundPicker.point(16,16);d.acceptBackgroundPick();d.backgroundPicker.point(31,31);
            restored=LuaDraft.restore(d.encode());check(restored.backgroundPicker.phase==1,"nested picker recovery");restored.acceptBackgroundPick();restored.cancelInsert();check(restored.layers.index==1&&!restored.dirty(),"nested cancel return");
            d.cancelInsert();d.layers.index=0;d.layerMenu=2;d.reviewLayer();restored=LuaDraft.restore(d.encode());restored.applyLayer();
            check(restored.text().equals(prefix+a+a+nl+b+suffix),"duplicate exact block");check(restored.layers.index==1,"duplicate selected");
            restored.layerMenu=3;restored.reviewLayer();restored.applyLayer();check(restored.text().equals(src),"delete duplicate exact");
            restored.layers.index=0;restored.newLayer(new SpriteRegion(32,16,24,8));check(restored.layerForm(),"new layer proposal");
            restored.insertion.beginPreview();LuaDraft newRestore=LuaDraft.restore(restored.encode());newRestore.applyInsert();
            check(newRestore.panel==LuaDraft.Panel.LAYERS&&newRestore.layers.index==1&&newRestore.layers.entries.size()==3,"new layer returns to list");
            check(newRestore.layers.current().form.value(0).equals("32"),"new region");newRestore.layerHistory(false);check(newRestore.text().equals(src),"new layer one undo");
            check(Arrays.equals(d.edit().candidate(LuaNavigationTest.cart(src)).bytes(),LuaNavigationTest.cart(src).bytes()),"cancel preserves cartridge and unknown section");
        }
        String a=layer(12,"\n"),b=layer(40,"\n");
        for(String gap:new String[]{"camera()\n","-- comment\n","end\nfunction _draw()\n","if ready then\n","pal(1,2)\n"}){
            BackgroundLayers list=new BackgroundLayers(a+gap+b,0);refused(()->list.change(1));check(list.entries.size()==2,"blocked but editable");
        }
        BackgroundLayers edge=new BackgroundLayers(a,0);refused(()->edge.change(-1));refused(()->edge.change(1));
        check(new BackgroundLayers("--[[\n"+a+"]]\n"+b,0).entries.size()==1,"comment masked");
        check(new BackgroundLayers(a.replace("127,_bg_w","126,_bg_w")+b,0).entries.size()==1,"custom formula excluded");
        check(new BackgroundLayers(a.replace("sspr(0,0,16,16","sspr(0,0,8,16")+b,0).entries.size()==1,"mismatched width does not hide later layer");
        check(new BackgroundLayers("local camera=fn\n"+a,0).entries.isEmpty(),"shadowed API excluded");
        LuaDraft only=draft(a);only.beginLayers();only.layerMenu=3;only.reviewLayer();only.applyLayer();check(only.text().isEmpty()&&only.layers.entries.isEmpty(),"delete last");
        only.layerHistory(false);check(only.text().equals(a),"undo last deletion");
        LuaDraft empty=draft("function _draw()\n cls()\nend\n");empty.point(2,0);empty.beginLayers();empty=LuaDraft.restore(empty.encode());empty.newLayer(new SpriteRegion(0,0,8,8));
        empty.cancelInsert();check(empty.panel==LuaDraft.Panel.LAYERS&&!empty.dirty(),"empty new cancel");
        empty.newLayer(new SpriteRegion(0,0,8,8));empty.insertion.beginPreview();empty.applyInsert();check(empty.layers.entries.size()==1,"first layer added");
        LuaDraft stale=draft(a+b);stale.beginLayers();stale.layerMenu=1;stale.reviewLayer();stale.replace("-- changed\n");refused(stale::applyLayer);
        LuaDraft malformed=draft(a);malformed.beginLayers();byte[] valid=malformed.encode(),trailing=Arrays.copyOf(valid,valid.length+1);refused(()->LuaDraft.restore(trailing));byte[] bad=valid.clone();bad[valid.length-3]=127;final byte[] invalid=bad;refused(()->LuaDraft.restore(invalid));
        LuaNavigationTest.Port port=new LuaNavigationTest.Port();WorkshopSession s=new WorkshopSession(LuaNavigationTest.cart(a+b),port);s.switchTool(1);s.act(Action.CONFIRM);s.codeCommand(24);
        check(s.codeDraft.panel==LuaDraft.Panel.LAYERS,"menu entry");s.act(Action.DOWN);s.act(Action.CONFIRM);s.act(Action.CANCEL);check(s.codeDraft.layers.index==1,"controller fields return");
        s.act(Action.MENU);s.act(Action.CONFIRM);s.act(Action.TEST);s.act(Action.UNDO);s.codeCommand(1);s.codeText("evil");
        check(port.writes==0&&port.launches==0&&!s.codeDraft.dirty(),"modal traps writes and history");s.act(Action.CANCEL);s.act(Action.CANCEL);s.act(Action.NEXT);s.act(Action.CANCEL);check(s.codeDraft.panel==LuaDraft.Panel.LAYERS,"controller new cancel");
        s.act(Action.CONTEXT);check(s.codeDraft.panel==LuaDraft.Panel.CURSOR,"source escape");
        s.act(Action.CONTEXT);s.codeDraft.insertion.choose(LuaInsert.ITEMS.length-1);s.act(Action.CONFIRM);check(s.codeDraft.panel==LuaDraft.Panel.LAYERS,"catalogue entry");
        System.out.println("BackgroundLayersTest: "+checks+" checks passed");
    }
}
