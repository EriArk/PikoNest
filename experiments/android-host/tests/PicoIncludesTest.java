import art.pikoos.lab.core.*;
import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PicoIncludesTest {
    static int checks;
    static byte[] bytes(String text){return text.getBytes(StandardCharsets.ISO_8859_1);}
    static String text(byte[] bytes){return new String(bytes,StandardCharsets.ISO_8859_1);}
    static final String HEADER="pico-8 cartridge // http://www.pico-8.com\r\nversion 42\r\n__lua__\r\n";
    static byte[] cart(String lua){return bytes(HEADER+lua);}
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    interface Attempt{void run()throws Exception;}
    static void rejects(Attempt attempt,String why)throws Exception{try{attempt.run();}catch(Exception e){checks++;return;}throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        Map<String,byte[]> files=new HashMap<>();int[] reads={0};
        files.put("code/main.lua",bytes("value=7 -- no final newline"));
        files.put("shared.p8",cart("wrong=1\r\n-->8\r\nright=2\r\n-->8\r\nlast=3\r\n__gfx__\r\n00\r\n"));
        PicoIncludes.Source source=path->{reads[0]++;return files.get(path);};
        byte[] original=cart("-- untouched\r\n#include code/main.lua\r\n#include shared.p8:1\r\nfunction _draw() print(value+right) end\r\n__gfx__\r\n0123\r\n__future__\r\n\u00ff\u0080\r\n");
        byte[] saved=original.clone(),ready=PicoIncludes.prepare(original,source);
        check(Arrays.equals(saved,original),"original byte preserving");
        check(text(ready).contains("value=7 -- no final newline\nright=2\r\nfunction"),"file and zero-based tab substitution with line separation");
        check(!text(ready).contains("wrong=")&&!text(ready).contains("last="),"only requested tab");
        P8Document before=P8Document.parse(original),after=P8Document.parse(ready);
        for(String section:new String[]{"gfx","future"})check(Arrays.equals(before.body(before.uniqueSection(section)),after.body(after.uniqueSection(section))),"non-code bytes exact "+section);
        check(!PicoIncludes.needed(ready)&&new PlayCartridge("a.p8",ready).problem.isEmpty(),"plain final cart accepted");
        check(!new PlayCartridge("a.p8",original).problem.isEmpty()&&new PlayCartridge("a.p8",original,true).problem.isEmpty(),"unprepared includes require explicit capable workflow");
        check(reads[0]==2,"only declared files read");
        ready=PicoIncludes.prepare(cart("#include shared.p8\n"),source);
        check(text(ready).contains("wrong=1")&&text(ready).contains("last=3")&&!text(ready).contains("__gfx__"),"all Lua tabs, no resources from linked cart");
        reads[0]=0;PicoIncludes.prepare(cart("#include shared.p8:0\n#include shared.p8:2\n"),source);check(reads[0]==1,"one consistent snapshot per unique dependency");
        for(String invalid:new String[]{"../secret.lua","sub/../../secret.lua","/tmp/a.lua","a//b.lua","./a.lua","file:a.lua","content://x","C:/a.lua","a\\b.lua","%2e%2e/a.lua","x.lua:1","x.p8:-1","x.p8:1:2","a.png","foo bar.lua","файл.lua"})
            rejects(()->PicoIncludes.path(invalid),"unsafe/unsupported path "+invalid);
        check(PicoIncludes.path("code/Test.LUA").equals("code/Test.LUA"),"path case retained");
        rejects(()->PicoIncludes.prepare(cart("#include missing.lua\n"),source),"missing file");
        rejects(()->PicoIncludes.prepare(cart("#include shared.p8:5\n"),source),"missing tab");
        rejects(()->PicoIncludes.prepare(cart("#include shared.p8:1 -- note\n"),source),"unsupported directive syntax explicit");
        files.put("nested.lua",bytes("#include code/main.lua\n"));
        rejects(()->PicoIncludes.prepare(cart("#include nested.lua\n"),source),"never invent recursive includes");
        for(String ignored:new String[]{"-- #include x.lua\n","--[[\n#include x.lua\n]]\n","--[=[\n#include x.lua\n]=]\n","s=[[\n#include x.lua\n]]\n","s='\\\"#include x.lua'\n"}){
            reads[0]=0;byte[] input=cart(ignored);check(Arrays.equals(input,PicoIncludes.prepare(input,source))&&reads[0]==0,"comments/strings never cause dependency reads");
        }
        files.put("marker.lua",bytes("__gfx__\n"));
        rejects(()->PicoIncludes.prepare(cart("#include marker.lua\n__gfx__\n00\n"),source),"section boundary injection refused");
        files.put("load.lua",bytes("load('next.p8')\n"));
        check(!new PlayCartridge("a.p8",PicoIncludes.prepare(cart("#include load.lua\n"),source)).problem.isEmpty(),"runtime dependency in included code still refused");
        StringBuilder many=new StringBuilder();for(int i=0;i<33;i++)many.append("#include code/main.lua\n");
        rejects(()->PicoIncludes.prepare(cart(many.toString()),source),"include count bounded");
        files.put("huge.lua",new byte[CartridgeImport.MAX_BYTES]);
        rejects(()->PicoIncludes.prepare(cart("#include huge.lua\n"),source),"final cart size bounded");
        byte[] resourceCart=new byte[CartridgeImport.MAX_BYTES];Arrays.fill(resourceCart,(byte)'0');
        byte[] smallCode=cart("v=1\n__gfx__\n");System.arraycopy(smallCode,0,resourceCart,0,smallCode.length);
        rejects(()->PicoIncludes.prepare(cart("#include a.p8\n#include b.p8\n#include c.p8\n#include d.p8\n"),p->resourceCart),"aggregate input budget includes unselected resource bytes");
        files.put("code/main.lua",bytes("value=9\n"));
        check(text(PicoIncludes.prepare(cart("#include code/main.lua\n"),source)).contains("value=9"),"new launch reads changed dependency instead of old cache");
        byte[] plain=cart("print(1)\n");check(Arrays.equals(plain,PicoIncludes.prepare(plain,p->{throw new AssertionError("unexpected read");})),"single-file pass through exact");
        System.out.println("PicoIncludesTest: "+checks+" checks passed");
    }
}
