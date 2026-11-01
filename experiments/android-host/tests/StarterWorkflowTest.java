import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;

/** Start, modify through general tools, recover, undo and export ordinary games. */
public final class StarterWorkflowTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static void help(WorkshopSession s,int row){s.act(Action.CONTEXT);check(s.game.screen==GameCanvas.Screen.GUIDE,"help opens from field");s.game.focus=row;s.act(Action.CONFIRM);check(s.mode==Mode.GAME,"guide route: "+s.error);}
    public static void main(String[] args)throws Exception{
        byte[] blank=Files.readAllBytes(Paths.get("experiments/android-host/assets/blank.p8")),garden=Files.readAllBytes(Paths.get("experiments/android-host/assets/moon-garden.p8")),lights=Files.readAllBytes(Paths.get("experiments/android-host/assets/lights.p8"));
        LibraryWorkflowTest.Port files=new LibraryWorkflowTest.Port();files.files.put("moon-garden",garden.clone());
        LibrarySession shelf=LibrarySession.curated(files,garden,blank,lights);shelf.refresh(null);shelf.command(1);check(shelf.templateChoice==0&&shelf.template().title.equals("Blank"),"Blank default");shelf.act(Action.TEST);check(files.creates==0,"no Start bypass");shelf.act(Action.CANCEL);check(files.creates==0,"cancel creates nothing");
        for(int choice=1;choice<=2;choice++){
            shelf.command(1);for(int n=0;n<choice;n++)shelf.act(Action.RIGHT);WorkshopCartridge expected=shelf.template().cart();byte[] original=expected.bytes();shelf.act(Action.CONFIRM);
            check(files.opened.equals(choice==1?"explorer-0001":"switches-0001"),"stable distinct ID");same(original,files.cart.bytes(),"created exact selected preview");same(garden,files.files.get("moon-garden"),"old projects preserved");check(LibrarySession.title(files.opened).contains(choice==1?"Little explorer":"Light puzzle"),"new ID title");
            String id=files.opened;PlayModel model=PlayModel.read(expected);check(model!=null&&new GameCanvas(expected).blocked.isEmpty(),"same recognized model");same(original,model.write(expected).bytes(),"ordinary source roundtrip");check(model.goal==choice,"appropriate goal");if(choice==2)for(PlayModel.Element e:model.elements)check(e.movement==0&&e.rule==4,"hero-free puzzle");
            PlayModelTest.Port port=new PlayModelTest.Port();WorkshopSession s=PlayModelTest.session(expected,port);help(s,0);same(original,port.launched,"guide Test unchanged");s.act(Action.CANCEL);
            s.game.focus=1;s.act(Action.CONFIRM);s.act(Action.CANCEL);help(s,3);check(s.game.screen==GameCanvas.Screen.BEHAVIOR&&s.game.selected==0,"guidance routes selected element");s.act(Action.CANCEL);
            help(s,2);if(s.game.screen==GameCanvas.Screen.SHARED){s.game.focus=0;s.act(Action.CONFIRM);}check(s.game.screen==GameCanvas.Screen.DRAW,"shared/general appearance path");s.game.color=12;s.game.paint(0,0);s.restoreGame(s.game.encode());s.act(Action.CANCEL);s.game.focus=0;s.act(Action.CONFIRM);check(s.mode==Mode.GAME,"save appearance");WorkshopCartridge edited=s.cart();check(PlayModel.read(edited).goal==choice&&PlayModel.read(edited).winSound==9,"appearance preserves rules and audio");s.act(Action.UNDO);same(original,s.cart().bytes(),"one Undo");s.act(Action.REDO);same(edited.bytes(),s.cart().bytes(),"Redo exact");
            s.act(Action.CANCEL);help(s,5);check(s.game.world!=null,"map available regardless of starter");s.act(Action.CANCEL);s.act(Action.CANCEL); // Changed default world is not saved.
            s=PlayModelTest.session(edited,port);help(s,6);check(s.game.sound!=null,"sound available");s=PlayModelTest.session(edited,port);help(s,7);check(s.game.music!=null,"music available");s=PlayModelTest.session(edited,port);help(s,8);check(s.game.screen==GameCanvas.Screen.OVERVIEW,"goal overview available");
            CartridgeExport export=new CartridgeExport(id,LibrarySession.title(id),edited.bytes());ByteArrayOutputStream out=new ByteArrayOutputStream();export.writeTo(out);export.verify(new ByteArrayInputStream(out.toByteArray()));same(edited.bytes(),out.toByteArray(),"export exact");check(new GameCanvas(new WorkshopCartridge(out.toByteArray())).blocked.isEmpty(),"export editable without starter metadata");
            files.files.put(id,edited.bytes());shelf.refresh(id);shelf.command(2);shelf.act(Action.CONFIRM);same(edited.bytes(),files.cart.bytes(),"copy edited saved starter");shelf.command(1);for(int n=0;n<choice;n++)shelf.act(Action.RIGHT);shelf.act(Action.CONFIRM);same(original,files.cart.bytes(),"fresh starter never reuses edits");check(files.opened.endsWith("0002"),"exclusive next ID");
            if(args.length>0){Path dir=Paths.get(args[0]);Files.createDirectories(dir);Files.write(dir.resolve(choice==1?"little-explorer.p8":"light-puzzle.p8"),original);}
        }
        WorkshopSession empty=PlayModelTest.session(new WorkshopCartridge(blank),new PlayModelTest.Port());help(empty,3);check(empty.game.screen==GameCanvas.Screen.SIZE,"Blank offers missing sprite setup");empty.act(Action.CANCEL);help(empty,5);check(empty.game.world!=null,"Blank map tool");
        for(boolean puzzle:new boolean[]{false,true}){GameCanvas g=new GameCanvas(StarterGames.create(puzzle));g.act(Action.CONTEXT);for(int n=0;n<g.rows().length;n++){g.focus=n;GameCanvas r=GameCanvas.restore(g.encode(),g.base);check(r.screen==GameCanvas.Screen.GUIDE&&r.focus==n&&!r.draft(),"help recovers without creating a draft");same(g.base.bytes(),r.candidate(g.base).bytes(),"help does not modify cart");}}
        GameCanvas unknown=new GameCanvas(new WorkshopCartridge(lights));unknown.act(Action.CONTEXT);unknown=GameCanvas.restore(unknown.encode(),unknown.base);unknown.focus=3;check(unknown.act(Action.CONFIRM)==GameCanvas.Command.TOOLS,"unknown Lua offers All tools");
        StringBuilder custom=new StringBuilder("function _update()\n custom()\nend\nfunction _draw()\n cls(1)\n");for(int n=0;n<20;n++)custom.append(" spr(1,").append(n).append(",0)\n");custom.append("end\n");GameCanvas imported=new GameCanvas(GameCanvasTest.cart(custom.toString(),"\n"));check(!imported.blocked.isEmpty(),"custom Lua remains guarded");imported.act(Action.CONTEXT);imported.act(Action.CANCEL);check(imported.focus==0,"unknown layout does not leak hidden element index into home");GameCanvas.restore(imported.encode(),imported.base);
        files.failCreate=true;shelf.refresh(null);shelf.command(1);shelf.act(Action.RIGHT);int before=files.creates;shelf.act(Action.CONFIRM);check(shelf.mode==LibrarySession.Mode.ERROR&&files.creates==before,"create failure has no partial project");
        check(!LibrarySession.validId("explorer-0001/../x")&&!LibrarySession.validId("switches-1"),"new IDs retain path validation");
        System.out.println("StarterWorkflowTest: "+checks+" checks passed");
    }
}
