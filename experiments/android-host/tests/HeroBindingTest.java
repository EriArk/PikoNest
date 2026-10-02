import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;

public final class HeroBindingTest {
    static int checks;
    static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    interface Call{void run();}
    static void rejects(Call c){try{c.run();throw new AssertionError("unsafe hero edit accepted");}catch(IllegalArgumentException expected){checks++;}}
    static byte[] bytes(String s){return s.getBytes(StandardCharsets.ISO_8859_1);}
    static class Port implements WorkshopSession.Port{
        int writes,launches;byte[] saved,launched;boolean fail;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");saved=b;writes++;}
        public void launch(byte[] b){launches++;launched=b;}
    }
    static void resourcesUnchanged(WorkshopCartridge a,WorkshopCartridge b){
        P8Document before=P8Document.parse(a.bytes()),after=P8Document.parse(b.bytes());
        for(int i=0;i<before.sections().size();i++)if(!before.sections().get(i).name.equals("lua"))
            check(Arrays.equals(before.body(i),after.body(i)),"all graphics, map, sound and unknown bytes preserved");
    }
    public static void main(String[] args)throws Exception{
        byte[] original=Files.readAllBytes(Paths.get(args[0]));WorkshopCartridge base=new WorkshopCartridge(original);
        SpriteRegion r=new SpriteRegion(64,24,32,24);
        WorkshopCartridge source=base.withLine(r,3,3,28,18,7);
        HeroBinding proposal=source.proposeHero(r);
        check(proposal.left==3&&proposal.top==3&&proposal.width==26&&proposal.height==16,"transparent margins excluded from rectangular body");
        check(proposal.spawnX()==41&&proposal.spawnY()==85,"spawn keeps foot on ground and old horizontal centre");
        WorkshopCartridge assigned=source.withHero(proposal);
        check(assigned.heroSlot()==-1&&assigned.hero().sameImage(r)&&!assigned.legacyHero(),"arbitrary region is not an invented card number");
        check(assigned.code().contains("sspr(hero_sx,hero_sy,hero_sw,hero_sh,x-hero_left,y-hero_top)"),"ordinary pixel source rectangle drawn around body origin");
        check(assigned.code().contains("x=mid(0,x,128-hero_w)")&&assigned.code().contains("y=p[2]-hero_h"),"screen bounds and landing use body dimensions");
        check(assigned.code().contains("and y+hero_h>=p[2] and x+hero_w>p[1]")&&assigned.code().contains("and x<p[1]+p[3]"),"platform overlap uses both body edges");
        resourcesUnchanged(source,assigned);
        check(assigned.withValue(0,4).value(0)==4,"existing parameter editor survives binding block insertion");
        check(Arrays.equals(assigned.bytes(),assigned.withHero(proposal).bytes()),"reconfirming same binding changes no bytes");
        WorkshopCartridge repainted=assigned.withPixel(r,0,0,14);
        check(repainted.hero().left==3&&repainted.hero().width==26,"painting does not silently resize collision body");
        check(repainted.proposeHero(r).left==0&&repainted.proposeHero(r).width==29,"reassignment explicitly proposes new body");
        WorkshopCartridge back=assigned.withHero(0);
        check(back.heroSlot()==0&&back.hero().image.width==16&&!back.legacyHero(),"card assignment remains possible after migration");
        check(back.code().indexOf("-- pikoos-hero")==back.code().lastIndexOf("-- pikoos-hero"),"rebinding updates one block");
        resourcesUnchanged(assigned,back);
        String text=new String(source.bytes(),StandardCharsets.ISO_8859_1).replace("\n","\r\n")+"__future__\r\n\u0081opaque\r\n";
        WorkshopCartridge crlf=new WorkshopCartridge(bytes(text));WorkshopCartridge changed=crlf.withHero(crlf.proposeHero(r));
        resourcesUnchanged(crlf,changed);
        check(!changed.code().replace("\r\n","").contains("\n"),"new Lua respects CRLF");
        for(String[] mutation:new String[][]{{"x=mid(0,x,112)","x=mid(0,x,99)"},{"vy=min(vy+0.18,4)","vy=min(vy+0.1,4)"},{"platforms={{0,101,128}","platforms={{0,100,128}"}}){
            WorkshopCartridge custom=new WorkshopCartridge(bytes(new String(source.bytes(),StandardCharsets.ISO_8859_1).replace(mutation[0],mutation[1])));
            rejects(()->custom.withHero(custom.proposeHero(r)));
        }
        WorkshopCartridge collision=new WorkshopCartridge(bytes(new String(source.bytes(),StandardCharsets.ISO_8859_1).replace("__gfx__","hero_w=9\n__gfx__")));
        rejects(()->collision.withHero(collision.proposeHero(r)));
        WorkshopCartridge customNew=new WorkshopCartridge(bytes(new String(assigned.bytes(),StandardCharsets.ISO_8859_1).replace("128-hero_w)","127-hero_w)")));
        rejects(()->customNew.withHero(proposal));
        rejects(()->new HeroBinding(r,3,3,32,24));rejects(()->new HeroBinding(r,-1,0,1,1));
        rejects(()->base.proposeHero(r));rejects(()->base.withHero(proposal));
        // Transparent cells inside an assigned multi-cell image are still reserved from New/Copy.
        SpriteRegion wide=new SpriteRegion(0,0,48,16);WorkshopCartridge reserved=base.withHero(base.proposeHero(wide));
        check(reserved.firstFreeSlot()==3,"all intersecting shortcut cards reserved, including empty margins");
        rejects(()->reserved.copySprite(0,1));
        Port port=new Port();WorkshopSession s=new WorkshopSession(source,port);
        s.openSprite(0);s.region=r;s.mode=Mode.CANVAS;
        s.act(Action.ASSIGN_HERO);check(s.mode==Mode.HERO&&port.writes==0,"preview never writes");
        s.act(Action.NEXT);s.act(Action.UNDO);s.act(Action.COPY_SPRITE);s.switchTool(0);
        check(s.mode==Mode.HERO&&s.tool==2&&port.writes==0,"binding preview traps unrelated actions");
        s.act(Action.CANCEL);check(s.mode==Mode.CANVAS&&s.heroDraft==null&&Arrays.equals(s.cart().bytes(),source.bytes()),"cancel preserves project and prior editor mode");
        s.act(Action.ASSIGN_HERO);port.fail=true;s.act(Action.CONFIRM);
        check(s.mode==Mode.ERROR&&s.heroDraft!=null&&!s.canUndo()&&Arrays.equals(s.cart().bytes(),source.bytes()),"failed migration retains bytes, history and candidate");
        port.fail=false;s.act(Action.CANCEL);check(s.mode==Mode.HERO,"failure returns to candidate for retry");
        s.act(Action.TEST);check(port.writes==1&&port.launches==1&&Arrays.equals(port.saved,port.launched)&&s.heroDraft==null,"test commits picture and physics together before launching");
        s.act(Action.UNDO);check(Arrays.equals(s.cart().bytes(),source.bytes())&&!s.canUndo(),"single undo restores exact pre-migration code and binding");
        s.act(Action.ASSIGN_HERO);s.act(Action.CONFIRM);s.switchTool(0);s.select(2);
        check(s.tool==2&&s.region!=null&&s.selection().width==32&&s.selection().x==64,"workshop opens actual assigned large image");
        s.act(Action.DOWN);s.act(Action.DOWN);s.act(Action.DOWN);s.act(Action.DOWN);s.act(Action.DOWN);s.act(Action.DOWN);s.act(Action.CONFIRM);
        check(s.mode==Mode.COPY_PLACE,"controller reaches common copy action before role binding");s.act(Action.CANCEL);s.act(Action.DOWN);s.act(Action.CONFIRM);
        check(s.mode==Mode.HERO,"controller reaches assignment from large canvas navigation");
        int writes=port.writes;s.act(Action.CONFIRM);check(port.writes==writes,"identical confirmation adds no undo or storage write");
        s.act(Action.SPRITE_SHEET);s.openSprite(0);s.act(Action.ASSIGN_HERO);s.act(Action.CONFIRM);
        check(s.cart().heroSlot()==0,"controller can replace large hero with legacy card");
        Port restoredPort=new Port();WorkshopSession restored=new WorkshopSession(base,restoredPort);
        restored.openSprite(0);restored.previewHero();
        check(restored.mode==Mode.HERO&&restoredPort.writes==0&&Arrays.equals(restored.cart().bytes(),original),"restoring stale preview preferences on a legacy cart never assigns or migrates it");
        restored.act(Action.CANCEL);check(restoredPort.writes==0,"restored preview cancellation is read-only");
        System.out.println("HeroBindingTest: "+checks+" checks passed");
    }
}
