import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.*;
import java.io.*;

public final class ParameterPresetTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static LuaInsert form(String id){LuaInsert f=new LuaInsert();for(int n=0;n<LuaInsert.ITEMS.length;n++)if(LuaInsert.ITEMS[n].id.equals(id))f.choose(n);f.screen=LuaInsert.Screen.FIELDS;return f;}
    static class Port implements WorkshopSession.Port {
        List<ParameterPreset> saved=new ArrayList<>();boolean fail;int launches,writes;
        public void save(byte[] b){writes++;}public void launch(byte[] b){launches++;}
        public List<ParameterPreset> presets(){return new ArrayList<>(saved);}
        public void storePreset(ParameterPreset p)throws Exception{if(fail)throw new IOException("disk");for(ParameterPreset old:saved)if(old.id.equals(p.id))return;saved.add(p);}
        public void updatePreset(ParameterPreset a,ParameterPreset b)throws Exception{if(fail)throw new IOException("disk");for(int n=0;n<saved.size();n++)if(saved.get(n).id.equals(a.id))saved.set(n,b);}
        public String projectOrigin(){return "Source cart";}
    }
    public static void main(String[] args)throws Exception{
        LuaInsert original=form("print");original.set(0,"target-score");original.set(1,"24");original.set(2,"104");original.set(3,"12");
        ParameterPreset preset=ParameterPreset.capture(original,"Counter","Source");
        check(preset.value(0)==null,"expression excluded");check(preset.value(3).equals("12"),"color retained");
        ParameterPreset disk=ParameterPreset.decode(preset.encode());check(Arrays.equals(disk.encode(),preset.encode()),"lossless independent metadata");
        LuaInsert destination=form("print");destination.set(0,"lives");destination.set(2,"baseline");
        disk.apply(destination);check(destination.value(0).equals("lives")&&destination.value(2).equals("baseline"),"both source and destination bindings protected");
        check(destination.value(1).equals("24")&&destination.value(3).equals("12"),"literal settings reused");
        for(String id:new String[]{"sprite","sspr","map","solid","move_box","door_pair"}){
            LuaInsert f=form(id);ParameterPreset p=ParameterPreset.capture(f,"Settings","Source");
            for(int n=0;n<f.item().fields.length;n++)if(f.item().fields[n].kind==LuaInsert.Kind.NAME||f.item().fields[n].kind==LuaInsert.Kind.FLAG)check(p.value(n)==null,"name/flag references omitted");
            if(id.equals("sprite")||id.equals("sspr")||id.equals("map"))check(p.value(0)==null,"resource address omitted");
        }
        int captures=0;
        for(LuaInsert.Item item:LuaInsert.ITEMS){LuaInsert f=form(item.id);try{
            ParameterPreset p=ParameterPreset.capture(f,"Settings","Source");ParameterPreset.decode(p.encode()).apply(f);captures++;
        }catch(IllegalArgumentException e){check(!ParameterPreset.available(f)||item.id.equals("function")||item.id.equals("if")||item.id.equals("call"),"only forms without independent settings refused: "+item.id);}}
        check(captures>20,"catalogue breadth");
        byte[] trailing=Arrays.copyOf(preset.encode(),preset.encode().length+1);
        try{ParameterPreset.decode(trailing);throw new AssertionError();}catch(IOException expected){checks++;}
        try{ParameterPreset.decode(new byte[]{1,2});throw new AssertionError();}catch(IOException expected){checks++;}
        LuaInsert foreign=form("circle");String before=foreign.code();try{preset.apply(foreign);throw new AssertionError();}catch(IllegalArgumentException expected){check(before.equals(foreign.code()),"wrong tool untouched");}
        Port port=new Port();PresetPanel panel=new PresetPanel(original,port);
        panel.act(Action.CONFIRM);check(panel.page==PresetPanel.Page.NAME,"explicit save naming");
        panel.name.restoreText("Blue counter");panel.name.latin=true;panel.name.key=8;panel.name.replaceAll=false;
        panel=PresetPanel.restore(panel.encode(),original,port);check(panel.name.text().equals("Blue counter")&&panel.name.key==8,"name recovery");
        panel.act(Action.TEST);check(panel.page==PresetPanel.Page.REVIEW&&port.saved.isEmpty(),"name confirmation only previews");
        panel=PresetPanel.restore(panel.encode(),original,port);panel.act(Action.TEST);check(port.saved.isEmpty(),"Start does not save review");
        port.fail=true;try{panel.act(Action.CONFIRM);throw new AssertionError();}catch(IOException expected){check(panel.page==PresetPanel.Page.REVIEW&&port.saved.isEmpty(),"failed store keeps proposal");}
        port.fail=false;panel.act(Action.CONFIRM);check(port.saved.size()==1&&panel.index==1,"one durable named preset");
        panel.act(Action.UNDO);check(port.saved.get(0).favorite,"favorite stored");panel.act(Action.RIGHT);panel.act(Action.DOWN);check(panel.selected()!=null,"favorite filter");
        panel.act(Action.UNDO);check(panel.items().isEmpty()&&panel.index==0,"unfavorite empty filter");
        LuaInsert other=form("print");other.set(0,"lives");PresetPanel apply=new PresetPanel(other,port);apply.act(Action.DOWN);apply.act(Action.CONFIRM);
        check(other.value(3).equals("7"),"review changes nothing");apply=PresetPanel.restore(apply.encode(),other,port);apply.act(Action.CANCEL);check(other.value(3).equals("7"),"cancel preview unchanged");
        apply.act(Action.CONFIRM);apply.act(Action.CONFIRM);check(apply.closed&&other.value(3).equals("12")&&other.value(0).equals("lives"),"apply targets form");
        LuaInsert changed=form("print");changed.set(0,"different");try{PresetPanel.restore(new PresetPanel(original,port).encode(),changed,port);throw new AssertionError();}catch(IOException expected){checks++;}
        WorkshopCartridge cart=LuaNavigationTest.cart("function _draw()\n if active then\n  print(lives,1,2,7)\n end\nend\n");
        WorkshopSession s=new WorkshopSession(cart,port);s.restoreCode(new LuaDraft(cart,2).encode());s.codeDraft.beginParameters();s.act(Action.MENU);
        String source=s.codeDraft.text();s.act(Action.TEST);s.codeCommand(1);s.codeText("oops");check(source.equals(s.codeDraft.text())&&port.writes==0&&port.launches==0,"modal commands trapped");
        s.act(Action.DOWN);s.act(Action.CONFIRM);s.restorePresets(s.presets.encode());s.act(Action.CONFIRM);
        check(source.equals(s.codeDraft.text())&&s.codeDraft.insertion.value(0).equals("lives"),"existing call proposal preserves source/binding");
        s.codeDraft.insertion.field=4;s.act(Action.CONFIRM);check(s.codeDraft.text().contains("print(lives,24,104,12)"),"normal call edit applies preset");
        s.codeDraft.history(false);check(s.codeDraft.text().equals(source),"single undo exact source");s.codeDraft.history(true);
        s.codeDraft.beginBranches();s.codeDraft.branchAction(true);s.codeDraft.insertion.choose(11);s.codeDraft.insertion.screen=LuaInsert.Screen.FIELDS;s.codeDraft.insertion.set(0,"lives");
        s.act(Action.MENU);s.act(Action.DOWN);s.act(Action.CONFIRM);s.act(Action.CONFIRM);check(s.codeDraft.branchInsertion(),"branch target retained");s.codeDraft.insertion.field=4;s.act(Action.CONFIRM);
        check(s.codeDraft.text().contains("  print(lives,24,104,12)\n end"),"branch insertion stays in branch");
        System.out.println("ParameterPresetTest: "+checks+" checks passed");
    }
}
