import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** Read the final APK like Godot: every indexed asset must have its declared bytes.
 * Catches stale lengths that silently truncate source/remap/bootstrap reads. */
public final class VerifySparsePack {
    public static void main(String[] args)throws Exception{
        try(ZipFile apk=new ZipFile(args[0])){
            ByteBuffer index=ByteBuffer.wrap(read(apk,"assets.sparsepck")).order(ByteOrder.LITTLE_ENDIAN);
            index.position(Math.toIntExact(index.getLong(32)));int count=index.getInt();
            if(count!=262)throw new IOException("Expected patched index");
            Set<String> names=new HashSet<>();
            for(int i=0;i<count;i++){
                byte[] path=new byte[index.getInt()];index.get(path);
                String name=new String(path,StandardCharsets.UTF_8).replace("\0","");
                long offset=index.getLong(),size=index.getLong();byte[] md5=new byte[16];index.get(md5);
                int flags=index.getInt();byte[] data=read(apk,name);
                if(!names.add(name)||offset!=0||flags!=0||size!=data.length
                    ||!Arrays.equals(md5,MessageDigest.getInstance("MD5").digest(data)))
                    throw new IOException("Invalid sparse asset: "+name);
            }
            if(index.hasRemaining())throw new IOException("Trailing index data");
            if(!new String(read(apk,"run_pico_cmd.gd.remap"),StandardCharsets.UTF_8).contains("res://run_pico_cmd_pikoos.gd"))
                throw new IOException("Wrong runtime script mapping");
            System.out.println("APK sparse resources: all "+count+" sizes and hashes match packaged payloads");
        }
    }
    static byte[] read(ZipFile apk,String name)throws IOException{
        ZipEntry entry=apk.getEntry("assets/"+name);
        if(entry==null)throw new IOException("Missing asset: "+name);
        try(InputStream in=apk.getInputStream(entry)){return in.readAllBytes();}
    }
}
