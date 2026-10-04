import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class TileProbeTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe recipe accepted");}catch(IllegalArgumentException expected){checks++;}}
    static WorkshopCartridge cart(String code){return new WorkshopCartridge(("pico-8 cartridge // http://www.pico-8.com\r\nversion 43\r\n__lua__\r\n"+code+"__future__\r\nopaque\r\n").getBytes(StandardCharsets.UTF_8));}
    static int item(){for(int n=0;n<LuaInsert.ITEMS.length;n++)if(LuaInsert.ITEMS[n].id.equals("solid"))return n;throw new AssertionError();}
    static LuaDraft proposal(String code){LuaDraft d=new LuaDraft(cart(code),0);d.beginInsert();d.insertion.choose(item());d.insertion.screen=LuaInsert.Screen.FIELDS;return d;}
    static class Port implements WorkshopSession.Port {int saves,launches;byte[] saved;boolean fail;public void save(byte[] b)throws Exception{if(fail)throw new Exception("full");saves++;saved=b;}public void launch(byte[] b){launches++;}}
    public static void main(String[] args){
        LuaDraft d=proposal("function _draw()\r\n cls(1)\r\nend\r\n");d.point(1,1);String before=d.text();
        d.insertion.set(0,"wall_at");d.insertion.set(1,"7");d.insertion.set(2,"16");d.insertion.set(3,"64");d.insertion.set(4,"false");
        LuaDraft restored=LuaDraft.restore(d.encode());check(restored.insertion.value(4).equals("false"),"restore boundary choice");
        check(restored.text().equals(before),"proposal leaves source unchanged");d.applyInsert();restored.applyInsert();check(restored.text().equals(d.text()),"restored recipe identical");
        check(d.text().startsWith("function wall_at(x,y)\r\n"),"definition at top level even from draw body");
        check(d.text().endsWith(before),"existing code remains byte-identical suffix");
        check(!d.text().replace("\r\n","").contains("\n"),"CRLF preserved");
        P8Document a=P8Document.parse(cart(before).bytes()),b=P8Document.parse(d.edit().candidate(cart(before)).bytes());
        check(Arrays.equals(a.body(a.uniqueSection("future")),b.body(b.uniqueSection("future"))),"unknown data unchanged");
        String inserted=d.text();d.history(false);check(d.text().equals(before)&&d.line()==1,"single undo restores cursor and code");d.history(true);check(d.text().equals(inserted),"redo entire function");
        for(String src:new String[]{"solid_at=1\r\n","function solid_at() end\r\n","local solid_at\r\n","a,solid_at=1,2\r\n","print(solid_at)\r\n","function x(solid_at) end\r\n","obj.solid_at=1\r\n","fget=function() end\r\n","local mget\r\n","function x(flr) end\r\n","#include other.p8\r\n","_ENV.fget=1\r\n","_G['fget']=1\r\n"}){
            LuaDraft conflict=proposal(src);refused(conflict::applyInsert);check(conflict.text().equals(src)&&conflict.insertion!=null,"rejection keeps proposal/source");
        }
        for(String name:new String[]{"_draw","fget","flr","mget","print","sin","camera","load","_ENV","pairs"}){LuaDraft conflict=proposal("\r\n");conflict.insertion.set(0,name);refused(conflict::applyInsert);}
        LuaDraft comment=proposal("-- solid_at fget=1\r\ns=\"solid_at #include\"\r\n");comment.applyInsert();check(comment.text().startsWith("function solid_at"),"comments and strings excluded");
        LuaInsert form=new LuaInsert();form.choose(item());
        for(int field:new int[]{1,2,3}){final int f=field;refused(()->form.set(f,"x"));refused(()->form.set(f,"-1"));refused(()->form.set(f,field==1?"8":field==2?"129":"65"));}
        refused(()->form.set(2,"0"));refused(()->form.set(3,"0"));refused(()->form.set(4,"nil"));
        form.field=1;form.step(-1);check(form.value(1).equals("0"),"lower flag clamped");form.set(1,"7");form.beginText();check(form.value(1).equals("7")&&form.screen!=LuaInsert.Screen.TEXT,"flag uses picker");
        form.field=4;form.step(1);check(form.display(4).equals("Свободно"),"boundary explicit label");form.step(-1);check(form.value(4).equals("true"),"boundary toggle");
        WorkshopCartridge c=cart("function _draw()\r\n cls(1)\r\nend\r\n");Port p=new Port();WorkshopSession s=new WorkshopSession(c,p);s.switchTool(1);s.act(Action.CONFIRM);s.act(Action.CONTEXT);for(int n=0;n<item();n++)s.act(Action.DOWN);s.act(Action.CONFIRM);
        s.act(Action.DOWN);s.act(Action.RIGHT);check(s.codeDraft.insertion.value(1).equals("1"),"controller changes flag");s.act(Action.TEST);check(p.saves==0&&p.launches==0,"proposal traps Start");
        byte[] state=s.codeDraft.encode();s.act(Action.CANCEL);check(s.codeDraft.text().equals(new LuaDraft(c,0).text()),"cancel no edit");s.restoreCode(state);
        for(int n=0;n<4;n++)s.act(Action.DOWN);s.act(Action.CONFIRM);check(s.codeDraft.text().startsWith("function solid_at"),"controller apply");
        p.fail=true;s.act(Action.TEST);check(s.mode==WorkshopSession.Mode.ERROR&&p.saves==0&&s.codeDraft!=null,"save failure keeps draft");p.fail=false;s.act(Action.CANCEL);s.act(Action.TEST);check(p.saves==1&&p.launches==1,"retry save Test");s.act(Action.UNDO);check(Arrays.equals(c.bytes(),s.cart().bytes()),"project undo exact");
        System.out.println("TileProbeTest: "+checks+" checks passed");
    }
}
