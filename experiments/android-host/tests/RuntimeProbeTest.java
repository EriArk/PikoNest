import art.pikoos.lab.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class RuntimeProbeTest {
    static int checks;static void check(boolean b,String text){checks++;if(!b)throw new AssertionError(text);}
    static void rejected(byte[] bytes,Path parent)throws Exception{
        Path root=Files.createTempDirectory(parent,"bad-");
        try{RuntimeProbe.unpack(new ByteArrayInputStream(bytes),root.toFile(),()->false);throw new AssertionError("invalid payload accepted");}catch(IOException expected){checks++;}
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("pikoos-probe-");
        try{
            File archive=RuntimeArchiveTest.zip(root,RuntimeArchiveTest.valid());byte[] source=Files.readAllBytes(archive.toPath());
            byte[] cart="pico-8 cartridge // http://www.pico-8.com\nversion 42\n__lua__\nprint('ok')\n".getBytes("US-ASCII");
            ByteArrayOutputStream output=new ByteArrayOutputStream();String hash=RuntimeArchive.hash(archive,()->false);
            RuntimeProbe.prepare(archive,hash,cart,output,()->false);byte[] transport=output.toByteArray();
            Path target=Files.createDirectory(root.resolve("valid"));RuntimeProbe.unpack(new ByteArrayInputStream(transport),target.toFile(),()->false);
            check(Arrays.equals(Files.readAllBytes(target.resolve("pico8_64")),RuntimeArchiveTest.elf()),"exact binary bytes");
            check(Arrays.equals(Files.readAllBytes(target.resolve("pico8.dat")),new byte[]{1,2,3,4}),"exact data bytes");
            check(Arrays.equals(Files.readAllBytes(target.resolve("probe.p8")),cart),"ordinary test cart");
            check(Arrays.equals(Files.readAllBytes(archive.toPath()),source),"source ZIP unchanged");
            for(int offset:new int[]{0,8,12,50,transport.length-1}){byte[] b=transport.clone();b[offset]^=127;rejected(b,root);}
            rejected(Arrays.copyOf(transport,transport.length-1),root);rejected(Arrays.copyOf(transport,transport.length+1),root);
            try{RuntimeProbe.unpack(new ByteArrayInputStream(transport),target.toFile(),()->false);throw new AssertionError("overwrite");}catch(IOException expected){checks++;}
            try{RuntimeProbe.prepare(archive,"00",cart,new ByteArrayOutputStream(),()->false);throw new AssertionError("changed archive");}catch(IOException expected){checks++;}
            try{RuntimeProbe.unpack(new ByteArrayInputStream(transport),Files.createDirectory(root.resolve("cancel")).toFile(),()->true);throw new AssertionError("cancel");}catch(java.util.concurrent.CancellationException expected){checks++;}
            int[] calls={0,0};RuntimeSetup s=new RuntimeSetup(new RuntimeSetup.Port(){public void pick(){calls[0]++;}public void test(){calls[1]++;}public void cancel(){}public void leave(){}public void recheck(){}});
            s.arm64=true;s.verified=true;s.act(art.pikoos.lab.core.WorkshopSession.Action.CONFIRM);check(calls[1]==1,"verified archive primary action starts probe");
            s.testing=true;s.act(art.pikoos.lab.core.WorkshopSession.Action.CONTEXT);check(calls[0]==0,"cannot replace archive during probe");
        }finally{try(java.util.stream.Stream<Path> all=Files.walk(root)){for(Path p:(Iterable<Path>)all.sorted(Comparator.reverseOrder())::iterator)Files.delete(p);}}
        System.out.println("RuntimeProbeTest: "+checks+" checks passed");
    }
}
