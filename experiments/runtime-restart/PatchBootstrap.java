import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Changes only two known shell members, preserving every other tar byte/header.
 * No filesystem extraction, no symlink traversal, no proprietary runtime input.
 */
public class PatchBootstrap {
    static String replaceOnce(String s,String old,String replacement){
        int p=s.indexOf(old);
        if(p<0||s.indexOf(old,p+old.length())>=0)throw new IllegalArgumentException("Unexpected bootstrap contents");
        return s.substring(0,p)+replacement+s.substring(p+old.length());
    }
    static byte[] read(InputStream in,int size)throws IOException{
        byte[] b=new byte[size];int n=0;
        while(n<size){int r=in.read(b,n,size-n);if(r<0)throw new EOFException();n+=r;}return b;
    }
    public static void main(String[] args)throws Exception{
        String session=new String(Files.readAllBytes(Paths.get(args[2])),StandardCharsets.UTF_8).replace("\r\n","\n");
        Set<String> changed=new HashSet<>();
        try(InputStream in=new GZIPInputStream(Files.newInputStream(Paths.get(args[0])));
            OutputStream out=new GZIPOutputStream(Files.newOutputStream(Paths.get(args[1])))){
            while(true){
                byte[] h=read(in,512);
                if(h[0]==0){out.write(h);byte[] tail=new byte[8192];int n;while((n=in.read(tail))!=-1)out.write(tail,0,n);break;}
                int end=0;while(end<100&&h[end]!=0)end++;
                String name=new String(h,0,end,StandardCharsets.US_ASCII);
                int size=Integer.parseInt(new String(h,124,12,StandardCharsets.US_ASCII).replace("\0","").trim(),8);
                if(size<0||size>64*1024*1024)throw new IOException("Unexpected member size");
                byte[] body=read(in,size),pad=read(in,(512-size%512)%512);
                boolean patch=name.equals("package/start_pico_proot.sh")||name.equals("package/pulsar.sh");
                if(patch){
                    if(!changed.add(name))throw new IOException("Duplicate script");
                    String s=new String(body,StandardCharsets.UTF_8);
                    if(name.endsWith("start_pico_proot.sh"))s=replaceOnce(s,
                        "LD_LIBRARY_PATH=. ./busybox ash ./pulsar.sh > \"$LOG_DIR/pulse.log\" 2>&1 &\n\n\nwhile [ ! -d tmp/pulse ]; do\n    sleep 0.02\ndone",session);
                    else{
                        s=replaceOnce(s,"./busybox killall pulseaudio\n./busybox rm -rf tmp/pulse", "# Cleanup is synchronous in start_pico_proot.sh before launch.");
                        s=replaceOnce(s,"$PKG_DIR/pulseaudio -nF $PKG_DIR/pulse.pa --exit-idle-time=-1", "exec $PKG_DIR/pulseaudio -nF $PKG_DIR/pulse.pa --exit-idle-time=-1");
                    }
                    body=s.getBytes(StandardCharsets.UTF_8);
                    byte[] octal=String.format(Locale.ROOT,"%011o\0",body.length).getBytes(StandardCharsets.US_ASCII);
                    System.arraycopy(octal,0,h,124,12);Arrays.fill(h,148,156,(byte)' ');
                    int sum=0;for(byte b:h)sum+=b&255;
                    byte[] checksum=String.format(Locale.ROOT,"%06o\0 ",sum).getBytes(StandardCharsets.US_ASCII);
                    System.arraycopy(checksum,0,h,148,8);pad=new byte[(512-body.length%512)%512];
                }
                out.write(h);out.write(body);out.write(pad);
            }
        }
        if(changed.size()!=2)throw new IOException("Required scripts not found");
        System.out.println("Patched two bootstrap scripts; other members preserved");
    }
}
