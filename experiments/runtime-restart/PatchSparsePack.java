import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Refresh only the changed assets in Godot 4.6's unencrypted sparse PCK index.
 * The engine bounds reads by this index, even though payloads are loose APK assets. */
public final class PatchSparsePack {
    public static void main(String[] args) throws Exception {
        Path assets=Paths.get(args[0]), index=assets.resolve("assets.sparsepck");
        byte[] original=Files.readAllBytes(index);
        ByteBuffer in=ByteBuffer.wrap(original).order(ByteOrder.LITTLE_ENDIAN);
        if(in.getInt()!=0x43504447||in.getInt()!=3||in.getInt()!=4||in.getInt()!=6
            ||in.getInt()!=0||in.getInt()!=6||in.getLong()!=0||in.getLong()!=104)
            throw new IOException("Expected Godot 4.6 sparse PCK v3 header");
        in.position(104);int count=in.getInt();
        if(count!=260)throw new IOException("Unexpected upstream entry count");
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        out.write(original,0,104);out.write(le(4).putInt(count+2).array());
        Set<String> changed=new HashSet<>(Arrays.asList("package.dat","run_pico_cmd.gd.remap"));
        Set<String> seen=new HashSet<>();
        for(int i=0;i<count;i++){
            int start=in.position(), len=in.getInt();
            if(len<1||len>4096||len>in.remaining()-36)throw new IOException("Invalid index path size");
            byte[] name=new byte[len];in.get(name);
            String path=new String(name,StandardCharsets.UTF_8).replace("\0","");
            if(!seen.add(path))throw new IOException("Duplicate entry");
            long offset=in.getLong();in.getLong();in.position(in.position()+16);int flags=in.getInt();
            if(offset!=0||flags!=0)throw new IOException("Expected loose unencrypted asset");
            if(changed.remove(path))out.write(entry(assets,path));
            else out.write(original,start,in.position()-start);
        }
        if(in.hasRemaining()||!changed.isEmpty())throw new IOException("Unexpected sparse index contents");
        for(String added:new String[]{"run_pico_cmd_pikoos.gd","PIKOOS-UPSTREAM-LICENSE.txt"}){
            if(seen.contains(added))throw new IOException("Already patched");
            out.write(entry(assets,added));
        }
        Files.write(index,out.toByteArray());
        System.out.println("Sparse PCK: refreshed 2 assets, added script/license; 258 entries preserved byte-for-byte");
    }
    static ByteBuffer le(int size){return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);}
    static byte[] entry(Path assets,String path)throws Exception{
        byte[] name=path.getBytes(StandardCharsets.UTF_8), data=Files.readAllBytes(assets.resolve(path));
        int padded=(name.length+3)&~3;
        ByteBuffer b=le(4+padded+36);b.putInt(padded).put(name);b.position(4+padded);
        b.putLong(0).putLong(data.length).put(MessageDigest.getInstance("MD5").digest(data)).putInt(0);
        return b.array();
    }
}
