import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class PlayWorkflowTest {
    static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static class Port implements PlaySession.Port {
        int launches,workshops,folders,refreshes,favorites;boolean fail;
        public void launch(PlaySession.Game g){launches++;}public void refresh(){refreshes++;}
        public void workshop(){workshops++;}public void folders(){folders++;}
        public void favorite(PlaySession.Game g,boolean value)throws Exception{if(fail)throw new Exception("disk full");favorites++;}
    }
    public static void main(String[] args)throws Exception{
        for(String filename:args){byte[] data=Files.readAllBytes(Paths.get(filename)),copy=data.clone();
            PlayCartridge p=new PlayCartridge("game.p8",data);check(p.problem.isEmpty(),"ordinary fixture launchable");check(Arrays.equals(copy,data),"inspection byte preserving");}
        String header="pico-8 cartridge // http://www.pico-8.com\nversion 42\n__lua__\n";
        check(new PlayCartridge("game.p8",(header+"reload()").getBytes(StandardCharsets.US_ASCII)).problem.isEmpty(),"reload current snapshot needs no sibling file");
        check(new PlayCartridge("game.p8",(header+"function _draw() cls(1) end").getBytes(StandardCharsets.US_ASCII)).problem.isEmpty(),"no gfx or hero required");
        for(String code:new String[]{"#include behavior.lua","load('next.p8')","reload(0,0,1,'data.p8')","cstore(0,0,1,'data.p8')","save('copy.p8')","-- load('comment')"})
            check(!new PlayCartridge("game.p8",(header+code).getBytes(StandardCharsets.US_ASCII)).problem.isEmpty(),"conservative dependency hint");
        check(!new PlayCartridge("a.p8.png",new byte[0]).problem.isEmpty(),"png visible but not sent as text");
        check(!new PlayCartridge("a.p8",new byte[0]).problem.isEmpty(),"bad framing rejected");
        StringBuilder label=new StringBuilder(header+"__label__\n");for(int y=0;y<128;y++){for(int x=0;x<128;x++)label.append("0123456789abcdef".charAt((x+y)%16));label.append('\n');}
        PlayCartridge picture=new PlayCartridge("a.p8",label.toString().getBytes(StandardCharsets.US_ASCII));
        check(picture.cover.length==16384,"full cartridge label");for(int i=0;i<16384;i++)check(picture.cover[i]==(i%128+i/128)%16,"palette pixels");
        Port port=new Port();PlaySession s=new PlaySession(port);
        PlaySession.Game a=new PlaySession.Game("one","Alpha","",null,false,10),b=new PlaySession.Game("two","Beta","not supported",null,true,20),c=new PlaySession.Game("three","Gamma","",null,false,0);
        s.replace(Arrays.asList(c,b,a),"two");check(s.current()==b,"restore by stable opaque id");
        s.act(Action.CONFIRM);check(port.launches==0&&!s.error.isEmpty(),"unsupported launch explicit");
        s.act(Action.LEFT);s.act(Action.CONFIRM);check(port.launches==1&&s.busy,"one launch");
        s.act(Action.CONFIRM);s.act(Action.RIGHT);check(port.launches==1&&s.current()==a,"busy prevents double launch/navigation");
        s.act(Action.NEXT);s.act(Action.MENU);check(port.workshops==1&&port.folders==1,"slow worker escape routes");
        s.fail("read failed");s.act(Action.CANCEL);check(s.error.isEmpty()&&!s.busy,"failure recovery");
        port.fail=true;s.act(Action.CONTEXT);check(!a.favorite,"failed favorite write unchanged");
        port.fail=false;s.act(Action.CONTEXT);check(a.favorite&&port.favorites==1,"favorite saved");
        s.act(Action.UNDO);check(s.filter==1&&s.games().size()==2&&s.games().get(0)==b&&s.current()==a,"recent ordering and retained selection");
        s.act(Action.UNDO);check(s.filter==2&&s.games().size()==2,"favorite filter");s.act(Action.CONTEXT);check(!a.favorite&&s.games().size()==1&&s.current()==b,"unfavorite filtered item");
        s.replace(Collections.emptyList(),"one");for(Action action:new Action[]{Action.LEFT,Action.RIGHT,Action.UP,Action.DOWN,Action.CONFIRM,Action.TEST,Action.CONTEXT})s.act(action);
        check(s.current()==null&&port.launches==1,"empty/missing selection safe");
        System.out.println("PlayWorkflowTest: "+checks+" checks passed");
    }
}
