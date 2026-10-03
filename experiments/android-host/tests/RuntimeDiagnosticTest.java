import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class RuntimeDiagnosticTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static WorkshopCartridge cart(String s){return new WorkshopCartridge(("pico-8 cartridge // http://www.pico-8.com\nversion 42\n__lua__\n"+s).getBytes(StandardCharsets.UTF_8));}
    static class Port implements WorkshopSession.Port {
        int saves,launches,checks;byte[] source;
        public void save(byte[] b){saves++;}public void launch(byte[] b){launches++;}
        public void diagnose(byte[] b){checks++;source=b;}
    }
    public static void main(String[] args){
        String source="function _draw()\n cls(1)\n if then\nend\n";
        String error="RUNNING: /work/game.p8\nsyntax error line 3 (tab 0)\n if then\nunexpected symbol near 'then'\n";
        for(String eol:new String[]{"\n","\r\n","\r"}){
            RuntimeDiagnostic d=RuntimeDiagnostic.read(error.replace("\n",eol),false,false,true,source.replace("\n",eol));
            check(d.kind==RuntimeDiagnostic.Kind.ERROR&&d.line==2&&d.message.contains("unexpected"),"syntax/source mapping "+eol.length());
        }
        RuntimeDiagnostic d=RuntimeDiagnostic.read("runtime error line 3 tab 0\n print(missing.value,10,10,7)\nattempt to index global 'missing' (a nil value)\n",false,true,false,source.replace("if then","print(missing.value,10,10,7)"));
        check(d.line==2&&d.kind==RuntimeDiagnostic.Kind.ERROR,"runtime format");
        for(String bad:new String[]{error.replace("tab 0","tab 1"),error.replace("line 3","line 900"),error.replace(" if then","other source"),error.replace("line 3","line 0")})
            check(RuntimeDiagnostic.read(bad,false,true,false,source).line==-1,"unmapped error cannot jump");
        check(RuntimeDiagnostic.read(error,true,true,true,source).kind==RuntimeDiagnostic.Kind.CANCELLED,"cancel wins");
        check(RuntimeDiagnostic.read("",false,true,false,source).kind==RuntimeDiagnostic.Kind.UNKNOWN,"exit alone is no proof");
        check(RuntimeDiagnostic.read("RUNNING: /work/game.p8\n",false,false,false,source).kind==RuntimeDiagnostic.Kind.UNKNOWN,"incomplete run unknown");
        check(RuntimeDiagnostic.read("RUNNING: /work/game.p8\n",false,false,true,source).kind==RuntimeDiagnostic.Kind.OBSERVED,"bounded no-error observation");
        check(RuntimeDiagnostic.read("RUNNING: /other.p8\n",false,true,false,source).kind==RuntimeDiagnostic.Kind.UNKNOWN,"other cart marker rejected");
        check(RuntimeDiagnostic.read("RUNNING: /work/game.p8\nunfamiliar error format\n",false,true,false,source).kind==RuntimeDiagnostic.Kind.UNKNOWN,"unknown error format is not clean");
        check(RuntimeDiagnostic.read("runtime error line 999999999999999999 tab 0\n",false,false,false,source).line==-1,"numeric overflow");
        StringBuilder large=new StringBuilder();for(int i=0;i<40000;i++)large.append('x');check(RuntimeDiagnostic.read(large.toString(),false,false,false,source).log.length()==32768,"bounded log");
        check(RuntimeDiagnostic.read("RUNNING: /work/game.p8\n"+large,false,true,true,source).kind==RuntimeDiagnostic.Kind.UNKNOWN,"truncated output is not clean");
        check(RuntimeDiagnostic.read("Check unavailable: output incomplete\nRUNNING: /work/game.p8\nжжж",false,true,true,source).kind==RuntimeDiagnostic.Kind.UNKNOWN,"transport byte truncation is not character truncation");
        WorkshopCartridge cart=cart(source);Port p=new Port();WorkshopSession s=new WorkshopSession(cart,p);s.switchTool(1);s.act(Action.CONFIRM);s.codeDraft.point(3,2);
        s.act(Action.CHECK);check(s.mode==WorkshopSession.Mode.DIAGNOSTIC&&p.checks==0,"explicit review before execution");
        s.act(Action.TEST);check(p.launches==0&&p.saves==0,"start trapped in review");s.act(Action.CONFIRM);
        check(s.diagnosticPending&&p.checks==1&&p.saves==0,"trial copy, no save");s.act(Action.CONFIRM);s.act(Action.CONTEXT);check(p.checks==1,"no concurrent check");
        s.diagnosticResult(p.source,error,false,false,true);s.act(Action.CONFIRM);
        check(s.mode==WorkshopSession.Mode.CODE&&s.codeDraft.line()==2&&!s.codeDraft.dirty()&&!s.codeDraft.canUndo(),"jump no edit");
        s.codeDraft.goBack();check(s.codeDraft.line()==3&&s.codeDraft.column()==2,"error jump return history");
        s.codeDraft.point(2,0);s.codeDraft.select();s.codeDraft.end();s.codeDraft.replace(" print(1)");
        s.diagnosticResult(p.source,error,false,false,true);s.act(Action.CONFIRM);check(s.diagnosticStale&&s.mode==WorkshopSession.Mode.DIAGNOSTIC,"stale result cannot navigate changed source");
        s.act(Action.CANCEL);s.codeCommand(22);s.act(Action.CONFIRM);check(new String(p.source,StandardCharsets.UTF_8).contains("print(1)"),"trial uses unsaved draft");
        check(Arrays.equals(cart.bytes(),s.cart().bytes()),"canonical cart never mutated by check");
        s.diagnosticResult(p.source,"RUNNING: /work/game.p8\n",false,false,true);s.act(Action.MENU);s.act(Action.TEST);check(p.launches==0,"log modal traps test");s.act(Action.CANCEL);
        s.codeDraft.selectAll();s.codeDraft.replace("#include test.lua\n");s.codeCommand(22);s.act(Action.CONFIRM);check(s.mode==WorkshopSession.Mode.ERROR&&!s.diagnosticPending,"includes explicitly unsupported");
        System.out.println("Runtime diagnostics: "+checks+" checks passed");
    }
}
