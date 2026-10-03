import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class RuntimeArchiveTest {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    interface Attempt{void run()throws Exception;}
    static void reject(Attempt action,String why)throws Exception{try{action.run();}catch(IOException e){checks++;return;}throw new AssertionError(why);}
    static byte[] elf(){byte[] b=new byte[80];b[0]=127;b[1]='E';b[2]='L';b[3]='F';b[4]=2;b[5]=1;b[6]=1;b[16]=3;b[18]=(byte)183;return b;}
    static Map<String,byte[]> valid(){Map<String,byte[]> m=new LinkedHashMap<>();m.put("pico-8/pico8_64",elf());m.put("pico-8/pico8.dat",new byte[]{1,2,3,4});m.put("pico-8/readme_raspi.txt",new byte[]{5});return m;}
    static File zip(Path root,Map<String,byte[]> entries)throws Exception{
        File file=Files.createTempFile(root,"test-",".zip").toFile();
        try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(file))){
            for(Map.Entry<String,byte[]> e:entries.entrySet()){
                ZipEntry entry=new ZipEntry(e.getKey());entry.setMethod(ZipEntry.STORED);entry.setSize(e.getValue().length);CRC32 crc=new CRC32();crc.update(e.getValue());entry.setCrc(crc.getValue());out.putNextEntry(entry);out.write(e.getValue());out.closeEntry();
            }
        }return file;
    }
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("pikoos-runtime-test-");
        try{
            File good=zip(root,valid());byte[] original=Files.readAllBytes(good.toPath());
            RuntimeArchive.Result r=RuntimeArchive.inspect(good,()->false);
            check(r.binaryBytes==80&&r.dataBytes==4&&r.sha256.length()==64&&r.bytes==good.length(),"metadata from archive contents");
            check(Arrays.equals(original,Files.readAllBytes(good.toPath())),"inspection never changes source");
            File renamed=root.resolve("wrong-name.bin").toFile();Files.copy(good.toPath(),renamed.toPath());check(RuntimeArchive.inspect(renamed,()->false).sha256.equals(r.sha256),"filename not authority");
            for(String required:valid().keySet()){Map<String,byte[]> m=valid();m.remove(required);reject(()->RuntimeArchive.inspect(zip(root,m),()->false),"missing required file "+required);}
            for(String name:new String[]{"../outside","/absolute","pico-8/../outside","pico-8//file","pico-8\\file","C:/file","pico-8/./file","pico-8/\nfile"}){
                Map<String,byte[]> m=valid();m.put(name,new byte[]{1});reject(()->RuntimeArchive.inspect(zip(root,m),()->false),"unsafe path");
            }
            Map<String,byte[]> collision=valid();collision.put("PICO-8/PICO8_64",elf());reject(()->RuntimeArchive.inspect(zip(root,collision),()->false),"case collision");
            for(int at:new int[]{0,4,5,6,16,18}){Map<String,byte[]> m=valid();byte[] binary=elf();binary[at]=0;m.put("pico-8/pico8_64",binary);reject(()->RuntimeArchive.inspect(zip(root,m),()->false),"wrong ELF field "+at);}
            Map<String,byte[]> empty=valid();empty.put("pico-8/pico8.dat",new byte[0]);reject(()->RuntimeArchive.inspect(zip(root,empty),()->false),"empty data");
            Map<String,byte[]> many=valid();for(int i=0;i<512;i++)many.put("pico-8/f"+i,new byte[0]);reject(()->RuntimeArchive.inspect(zip(root,many),()->false),"entry count cap");
            File huge=root.resolve("large.zip").toFile();try(RandomAccessFile f=new RandomAccessFile(huge,"rw")){f.setLength(RuntimeArchive.MAX_ZIP+1);}reject(()->RuntimeArchive.inspect(huge,()->false),"archive byte cap before ZIP read");
            byte[] corrupt=original.clone();int offset=30+"pico-8/pico8_64".length();corrupt[offset+65]^=1;File bad=root.resolve("corrupt.zip").toFile();Files.write(bad.toPath(),corrupt);reject(()->RuntimeArchive.inspect(bad,()->false),"CRC payload mismatch");
            Files.write(bad.toPath(),Arrays.copyOf(original,original.length-15));reject(()->RuntimeArchive.inspect(bad,()->false),"truncated central directory");
            try{RuntimeArchive.inspect(good,()->true);throw new AssertionError("cancel inspection");}catch(java.util.concurrent.CancellationException expected){checks++;}
            ByteArrayOutputStream output=new ByteArrayOutputStream();RuntimeArchive.copy(new ByteArrayInputStream(original),output,()->false);check(Arrays.equals(original,output.toByteArray()),"copy exact bytes");
            InputStream infinite=new InputStream(){public int read(){return 0;}public int read(byte[] b){return b.length;}};
            reject(()->RuntimeArchive.copy(infinite,new OutputStream(){public void write(int b){}public void write(byte[] b,int o,int n){}},()->false),"provider stream bounded independent of advertised size");
            int[] actions={0,0,0,0};RuntimeSetup s=new RuntimeSetup(new RuntimeSetup.Port(){public void pick(){actions[0]++;}public void recheck(){actions[1]++;}public void cancel(){actions[2]++;}public void leave(){actions[3]++;}});
            s.act(Action.CONFIRM);check(actions[0]==0&&!s.problem.isEmpty(),"unsupported device ABI");s.arm64=true;s.act(Action.CONFIRM);check(actions[0]==1,"semantic archive picker");
            s.act(Action.CONTEXT);check(actions[1]==0,"no recheck without copy");s.begin("copy");s.act(Action.CONFIRM);s.act(Action.CANCEL);check(actions[0]==1&&actions[2]==1&&actions[3]==0,"busy cancellation instead of duplicate or leave");
            s.complete("pico.zip",123,null);check(s.verified&&s.hasArchive&&!s.busy&&!s.adapterPresent,"archive validation does not claim adapter readiness");
            s.begin("next");s.complete("wrong.zip",0,"wrong ABI");check(s.filename.equals("pico.zip")&&s.verified&&s.archiveBytes==123,"failed replacement keeps accepted archive");
            s.cancelled();check(s.hasArchive&&s.verified&&s.problem.isEmpty(),"picker cancellation preserves candidate");s.act(Action.CONTEXT);s.act(Action.CANCEL);check(actions[1]==1&&actions[3]==1,"recheck and leave");
            if(args.length>0){RuntimeArchive.Result real=RuntimeArchive.inspect(new File(args[0]),()->false);System.out.println("User archive: "+real.bytes+" bytes, ARM64, sha256="+real.sha256);}
            if(args.length>1)reject(()->RuntimeArchive.inspect(new File(args[1]),()->false),"user amd64 archive rejected");
            if(args.length>2)reject(()->RuntimeArchive.inspect(new File(args[2]),()->false),"user i386 archive rejected");
        }finally{try(java.util.stream.Stream<Path> files=Files.list(root)){for(Path p:(Iterable<Path>)files::iterator)Files.delete(p);}Files.delete(root);}
        System.out.println("RuntimeArchiveTest: "+checks+" checks passed");
    }
}
