import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class FlagsTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("accepted unsafe flags");}catch(IllegalArgumentException expected){checks++;}}
    static final String HEADER="pico-8 cartridge // http://www.pico-8.com\r\nversion 43\r\n__lua__\r\nfunction _draw()\r\n cls(1)\r\nend\r\n";
    static WorkshopCartridge cart(String s){return new WorkshopCartridge(s.getBytes(StandardCharsets.UTF_8));}
    static String text(WorkshopCartridge c){return new String(c.bytes(),StandardCharsets.UTF_8);}
    static String row(){char[] s=new char[256];Arrays.fill(s,'0');return new String(s);}
    static void resources(WorkshopCartridge a,WorkshopCartridge b){P8Document x=P8Document.parse(a.bytes()),y=P8Document.parse(b.bytes());for(P8Document.Section section:x.sections())if(!section.name.equals("gff"))check(Arrays.equals(x.body(x.uniqueSection(section.name)),y.body(y.uniqueSection(section.name))),"unchanged "+section.name);}
    static class Port implements WorkshopSession.Port {
        int saves,launches;boolean fail;byte[] saved;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");saves++;saved=b;}
        public void launch(byte[] b){launches++;}
    }
    public static void main(String[] args){
        WorkshopCartridge blank=cart(HEADER+"__future__\r\nopaque\r\n");
        check(blank.flags(0)==0&&blank.flags(255)==0,"missing flags read zero");check(blank.withFlags(255,0)==blank,"zero no-op does not add section");
        WorkshopCartridge withResources=blank.withPixel(new SpriteRegion(0,64,128,64),2,3,12).withTile(127,31,255);
        for(int tile:new int[]{0,17,127,128,255})for(int bit=0;bit<8;bit++){
            WorkshopCartridge c=withResources.withFlags(tile,1<<bit);P8Flags flags=new P8Flags(P8Document.parse(c.bytes()));
            for(int n=0;n<256;n++)check(flags.get(n)==(n==tile?1<<bit:0),"one byte, one bit, all other flags exact");resources(withResources,c);
        }
        WorkshopCartridge all=blank.withFlags(255,255);check(all.flags(255)==255&&all.flags(0)==0,"all bits in last sprite");
        check(text(all).endsWith("ff\r\n"),"CRLF inherited when expanding rows");resources(blank,all);
        String upper="AB"+row().substring(2),original=HEADER+"__gff__\r\n"+upper+"\r\n__sfx__\r\nuntouched\r\n";
        WorkshopCartridge mixed=cart(original);check(mixed.withFlags(0,171)==mixed,"uppercase no-op exact");
        check(text(mixed.withFlags(0,170)).equals(original.replace(upper,"Aa"+upper.substring(2))),"preserve untouched uppercase nibble and other sections");
        WorkshopCartridge noEnd=cart(HEADER+"__gff__\r\n"+row());check(!text(noEnd.withFlags(17,129)).endsWith("\n"),"no final newline retained");
        check(noEnd.withFlags(255,1).flags(255)==1,"extend missing newline safely");
        check(!text(cart(HEADER.replace("\r\n","\n")).withFlags(128,255)).contains("\r"),"LF preserved");
        for(String bad:new String[]{"00",row()+"\rx",row().replace('0','z'),row()+"\n"+row()+"\n"+row(),row()+"\n__gff__\n"+row()}){
            WorkshopCartridge c=cart(HEADER+"__gff__\n"+bad);refused(()->c.flags(0));check(c.code().contains("_draw"),"unsupported flags do not block code");
        }
        refused(()->blank.flags(256));refused(()->blank.flags(-1));refused(()->blank.withFlags(0,256));refused(()->blank.withFlags(0,-1));
        FlagDraft d=new FlagDraft(blank,17);d.toggle();d.move(3,1);d.toggle();check(d.value==129,"bit 0 and bit 7 are 129");
        FlagDraft restored=FlagDraft.restore(d.encode(),blank);check(restored.value==129&&restored.focus==7&&restored.tile==17,"restore exact draft");
        resources(blank,restored.candidate(blank));refused(()->restored.candidate(blank.withFlags(1,2)));refused(()->FlagDraft.restore(d.encode(),blank.withTile(0,0,1)));
        refused(()->FlagDraft.restore(d.encode()+";bad",blank));d.move(0,1);check(d.focus==8,"down to explicit save");d.move(0,-1);check(d.focus==4,"up back to bits");
        Port port=new Port();WorkshopSession s=new WorkshopSession(blank,port);s.switchTool(3);s.mapEditor.tile=17;
        s.act(Action.MENU);s.act(Action.DOWN);s.act(Action.DOWN);s.act(Action.CONFIRM);for(int i=0;i<3;i++)s.act(Action.DOWN);s.act(Action.CONFIRM);
        check(s.flagDraft!=null&&s.flagDraft.tile==17,"controller route via menu");s.act(Action.CONFIRM);s.act(Action.RIGHT);s.act(Action.CONFIRM);
        check(s.flagDraft.value==3&&port.saves==0&&s.cart().flags(17)==0,"toggles remain draft only");
        s.act(Action.TEST);s.act(Action.NEXT);s.act(Action.MENU);s.act(Action.CONTEXT);s.act(Action.REDO);s.switchTool(1);
        check(port.launches==0&&s.tool==3&&s.flagDraft.value==3,"modal shortcuts cannot apply or discard");
        s.act(Action.CANCEL);check(s.flagDraft==null&&port.saves==0,"cancel leaves bytes and history");
        s.act(Action.CHECK);for(int i=0;i<3;i++)s.act(Action.DOWN);s.act(Action.CONFIRM);s.act(Action.CONFIRM);s.act(Action.DOWN);s.act(Action.DOWN);
        port.fail=true;s.act(Action.CONFIRM);check(s.mode==WorkshopSession.Mode.ERROR&&s.flagDraft!=null&&s.undoCount()==0&&s.cart().flags(17)==0,"failed save retains proposal");
        s.act(Action.CONFIRM);port.fail=false;s.act(Action.CONFIRM);check(s.flagDraft==null&&s.cart().flags(17)==1&&s.undoCount()==1,"one saved operation");
        s.act(Action.UNDO);check(Arrays.equals(blank.bytes(),s.cart().bytes()),"undo removes appended flags exactly");s.act(Action.REDO);check(s.cart().flags(17)==1,"redo restores flags");
        s.act(Action.CHECK);for(int i=0;i<3;i++)s.act(Action.DOWN);s.act(Action.CONFIRM);s.act(Action.CONFIRM);s.act(Action.CONFIRM);s.act(Action.DOWN);s.act(Action.DOWN);
        int saves=port.saves;s.act(Action.CONFIRM);check(port.saves==saves&&s.undoCount()==1,"toggle twice then save no-op");
        System.out.println("FlagsTest: "+checks+" checks passed");
    }
}
