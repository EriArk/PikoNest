import art.pikoos.lab.core.*;
import art.pikoos.p8.P8Document;
import art.pikoos.lab.core.WorkshopSession.Action;
import art.pikoos.lab.core.WorkshopSession.Mode;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class LuaEditorTest {
    static int checks;
    static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static class Port implements WorkshopSession.Port {
        byte[] saved,launched;int writes;boolean fail;
        public void save(byte[] b)throws Exception{if(fail)throw new Exception("disk full");saved=b;writes++;}
        public void launch(byte[] b){launched=b;}
    }
    static WorkshopSession open(WorkshopCartridge cart,Port p){WorkshopSession s=new WorkshopSession(cart,p);s.switchTool(1);s.act(Action.CONFIRM);check(s.mode==Mode.CODE,"general code editor opens");return s;}
    static void type(WorkshopSession s,String text){
        s.codeDraft.panel=LuaDraft.Panel.KEYS;
        for(char c:text.toCharArray()){
            boolean found=false;
            for(int page=0;page<LuaDraft.PAGES.length;page++){
                int key=LuaDraft.PAGES[page].indexOf(c);if(key<0)continue;
                s.codeDraft.page=page;s.codeDraft.key=key;s.act(Action.CONFIRM);found=true;break;
            }
            check(found,"controller palette contains "+c);
        }
    }
    public static void main(String[] args)throws Exception{
        for(String path:args){
            String original=new String(Files.readAllBytes(Paths.get(path)),StandardCharsets.UTF_8);
            // Keep opaque bytes in another section and mixed text encodings out of normalization.
            byte[] base=(original.replace("\n","\r\n")+"__future__\r\nopaque\u0081\r\n").getBytes(StandardCharsets.UTF_8);
            WorkshopCartridge cart=new WorkshopCartridge(base);Port p=new Port();WorkshopSession s=open(cart,p);
            s.act(Action.CANCEL);same(base,s.cart().bytes(),"clean exit preserves all bytes");check(p.writes==0,"no-op not saved");
            s.act(Action.CONFIRM);s.codeDraft.home();type(s,"-- my own cart");s.codeCommand(2);
            check(p.writes==0&&!s.canUndo(),"draft input never persists canonical cart");
            s.act(Action.NEXT);check(s.tool==1&&s.mode==Mode.CODE,"tool switch cannot discard draft");
            byte[] draft=s.codeDraft.encode();WorkshopSession restored=new WorkshopSession(cart,p);restored.restoreCode(draft);
            check(restored.codeDraft.text().equals(s.codeDraft.text()),"process recovery text");
            same(s.codeDraft.edit().candidate(cart).bytes(),restored.codeDraft.edit().candidate(cart).bytes(),"process recovery exact candidate");
            p.fail=true;restored.act(Action.TEST);
            check(restored.mode==Mode.ERROR&&restored.codeDraft!=null&&p.launched==null,"failed save retains draft and blocks stale test");
            same(base,restored.cart().bytes(),"failed save preserves canonical state");
            p.fail=false;restored.act(Action.CANCEL);check(restored.mode==Mode.CODE,"error returns to draft");restored.act(Action.TEST);
            same(p.saved,p.launched,"test receives saved candidate");check(restored.undoCount()==1&&p.writes==1,"one transaction for draft");
            P8Document before=P8Document.parse(base),after=P8Document.parse(p.saved);
            for(int i=0;i<before.sections().size();i++)if(!before.sections().get(i).name.equals("lua"))same(before.body(i),after.body(i),"unrelated section exact");
            restored.act(Action.UNDO);same(base,restored.cart().bytes(),"whole-cart undo exact");
            restored.act(Action.REDO);check(restored.cart().code().startsWith("-- my own cart\r\n"),"redo code and CRLF preserved");
            LuaDraft stale=LuaDraft.restore(draft);
            try{stale.edit().candidate(restored.cart());throw new AssertionError("stale draft overwrote cart");}catch(IllegalStateException expected){checks++;}
            s.act(Action.CANCEL);check(s.codeDraft.panel==LuaDraft.Panel.EXIT,"dirty exit asks explicitly");
            s.act(Action.CONFIRM);check(s.codeDraft!=null,"default is keep editing");
            s.act(Action.CANCEL);s.act(Action.DOWN);s.act(Action.DOWN);s.act(Action.CONFIRM);same(base,s.cart().bytes(),"discard exact original");
            s.act(Action.CONFIRM);s.codeDraft.selectAll();s.codeDraft.replace("__gfx__\r\n");s.codeCommand(0);
            check(s.mode==Mode.ERROR&&s.codeDraft!=null,"section injection refused with draft retained");same(base,s.cart().bytes(),"framing refusal preserves cart");
        }
        WorkshopCartridge blank=new WorkshopCartridge(Files.readAllBytes(Paths.get(args[0])));
        LuaDraft d=new LuaDraft(blank,0);d.selectAll();d.replace("a\r\nb\nc\r😀x");
        check(d.lineCount()==4,"mixed newline indexing");d.point(3,1);d.move(-1,0);check(d.column()==0,"unicode cursor never splits surrogate");
        d.erase(true);check(d.lineText(3).equals("x"),"delete whole unicode glyph");d.history(false);check(d.lineText(3).equals("😀x"),"unicode undo");
        d.point(1,0);d.erase(false);check(d.text().startsWith("ab\n"),"backspace removes whole CRLF");d.history(false);
        d.point(0,0);d.select();d.move(1,0);d.copy();d.cut();d.point(1,1);d.paste();check(d.lineText(1).equals("ba"),"selection cut/copy/paste");
        String changed=d.text();d.history(false);d.history(true);check(d.text().equals(changed),"draft redo");
        d.history(false);d.replace("z");check(!d.canRedo(),"new edit clears future");
        byte[] recovery=d.encode();check(Arrays.equals(recovery,LuaDraft.restore(recovery).encode()),"full recovery state");
        try{LuaDraft.restore(Arrays.copyOf(recovery,12));throw new AssertionError("truncation accepted");}catch(IllegalArgumentException expected){checks++;}
        String header="pico-8 cartridge // http://www.pico-8.com\nversion 43\n__lua__\n";
        byte[] invalid=(header+"\u0081\n").getBytes(StandardCharsets.ISO_8859_1);
        try{new LuaDraft(new WorkshopCartridge(invalid),0);throw new AssertionError("invalid UTF8 accepted");}catch(IllegalArgumentException expected){checks++;}
        byte[] resources=(header+"print(1)\n__future__\nunchanged\n").getBytes(StandardCharsets.UTF_8);
        WorkshopCartridge withResources=new WorkshopCartridge(resources);d=new LuaDraft(withResources,0);d.selectAll();d.replace("print(2)");
        check(d.edit().candidate(withResources).code().equals("print(2)\n"),"resource delimiter kept on separate line");
        System.out.println("Lua editor: "+checks+" checks passed");
    }
}
