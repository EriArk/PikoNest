import art.pikoos.lab.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class RuntimeFileSetTest {
    static int checks;static byte[] cart(String code){return ("pico-8 cartridge // http://www.pico-8.com\nversion 42\n__lua__\n"+code).getBytes(StandardCharsets.US_ASCII);}
    static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    interface Attempt{void run()throws Exception;}
    static void reject(Attempt a,String why)throws Exception{try{a.run();}catch(Exception e){checks++;return;}throw new AssertionError(why);}
    static String name(DataInputStream in)throws Exception{int n=in.readInt();check(n>0&&n<=120,"bounded transport name");byte[] b=new byte[n];in.readFully(b);return new String(b,StandardCharsets.US_ASCII);}
    public static void main(String[] args)throws Exception{
        check(RuntimeDependencies.collect(cart("-- load('no.p8')\ns='cstore(0)'\n--[=[save('x')]=]\nreload()\nreload(0,0,32)\n")).isEmpty(),"ignore lexical exclusions and current ROM reload");
        check(RuntimeDependencies.collect(cart("load('b.p8','back','score=4')\nreload(0,0,1,\"data.p8\")")).equals(Arrays.asList("b.p8","data.p8")),"literal direct calls");
        check(RuntimeDependencies.collect(cart("load([[b.p8]])")).equals(Arrays.asList("b.p8")),"simple long-string filename");
        for(String code:new String[]{"load(name)","load('b'..'.p8')","local f=load","save('a.p8')","cstore()","load('sub/b.p8')","load('../b.p8')","reload(0,0,1,name)","load('b\\046p8')","load('a.p8'","s='unfinished"})
            reject(()->RuntimeDependencies.collect(cart(code)),"unsupported call rejected: "+code);
        for(String path:new String[]{"../a.p8","/a.p8","a/b.p8","a\\b.p8","a.p8.png","a.p8/../x","a'$(x).p8","..p8","тест.p8","a b.p8"})reject(()->RuntimeFileSet.name(path),"unsafe transport name");
        Map<String,byte[]> raw=new HashMap<>();raw.put("a.p8",cart("load('b.p8')\nreload(0,0,1,'data.p8')\n"));raw.put("b.p8",cart("load('a.p8')\n"));raw.put("data.p8",cart("\n__gfx__\nf000\n"));int[] reads={0};
        RuntimeFileSet set=RuntimeDependencies.prepare("a.p8",raw.get("a.p8"),p->{reads[0]++;return raw.get(p);});
        check(set.names().size()==3&&reads[0]==2,"cyclic graph terminates and reads each file once");
        for(String n:set.names())check(Arrays.equals(raw.get(n),set.bytes(n)),"ordinary file bytes exact "+n);
        byte[] mutable=set.bytes("a.p8");mutable[0]=0;check(set.bytes("a.p8")[0]=='p',"payload immutable");
        Map<String,byte[]> linked=new HashMap<>();
        linked.put("a.p8",cart("#include shared.lua\nload('b.p8')\n"));
        linked.put("b.p8",cart("#include shared.lua\nload('a.p8')\n"));
        linked.put("shared.lua","value=14\n".getBytes(StandardCharsets.US_ASCII));
        Map<String,Integer> readCounts=new HashMap<>();
        RuntimeFileSet included=RuntimeDependencies.prepare("a.p8",linked.get("a.p8"),p->{readCounts.put(p,readCounts.getOrDefault(p,0)+1);return linked.get(p);});
        check(included.names().size()==2&&readCounts.get("shared.lua")==1&&readCounts.get("b.p8")==1&&!readCounts.containsKey("a.p8"),"shared include and cyclic cart reuse one snapshot");
        check(new String(included.bytes("b.p8"),StandardCharsets.US_ASCII).contains("value=14\nload('a.p8')"),"includes expanded in downstream cart, runtime load retained");
        check(new String(linked.get("b.p8"),StandardCharsets.US_ASCII).contains("#include shared.lua"),"source untouched after graph preparation");
        DataInputStream input=new DataInputStream(new ByteArrayInputStream(set.transport()));byte[] magic=new byte[8];input.readFully(magic);
        check(new String(magic,StandardCharsets.US_ASCII).equals("PIKOSET1")&&name(input).equals("a.p8")&&input.readInt()==3,"wire header and entry");
        for(int i=0;i<3;i++){String n=name(input);byte[] b=new byte[input.readInt()];input.readFully(b);check(Arrays.equals(b,raw.get(n)),"wire body exact");}
        check(input.available()==0,"wire has no trailing bytes");
        reject(()->RuntimeDependencies.prepare("a.p8",cart("load('missing.p8')"),p->null),"missing dependency before dispatch");
        reject(()->RuntimeDependencies.prepare("a.p8",cart("load('b.p8')"),p->cart("cstore()")),"write in downstream code refused");
        raw.put("B.p8",raw.get("b.p8"));reject(()->new RuntimeFileSet("a.p8",raw),"case collision rejected");
        Map<String,byte[]> count=new LinkedHashMap<>();for(int i=0;i<33;i++)count.put("c"+i+".p8",cart(""));reject(()->new RuntimeFileSet("c0.p8",count),"file cap");
        reject(()->RuntimeDependencies.prepare("c0.p8",cart("load('c1.p8')"),p->{int i=Integer.parseInt(p.substring(1,p.indexOf('.')));return cart("load('c"+(i+1)+".p8')");}),"graph cap");
        Map<String,byte[]> large=new HashMap<>();for(int i=0;i<5;i++)large.put("c"+i+".p8",new byte[CartridgeImport.MAX_BYTES]);reject(()->new RuntimeFileSet("c0.p8",large),"byte cap");
        if(args.length==2){Path folder=Paths.get(args[0]);String entry="Petal-Gate.p8";
            RuntimeFileSet fixture=RuntimeDependencies.prepare(entry,Files.readAllBytes(folder.resolve(entry)),p->Files.readAllBytes(folder.resolve(p)));
            Files.write(Paths.get(args[1]),fixture.transport());
        }
        System.out.println("RuntimeFileSetTest: "+checks+" checks passed");
    }
}
