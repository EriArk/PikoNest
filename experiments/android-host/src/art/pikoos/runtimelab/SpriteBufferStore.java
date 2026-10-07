package art.pikoos.runtimelab;

import android.util.AtomicFile;
import art.pikoos.lab.core.SpriteAsset;
import java.io.*;
import java.util.Arrays;

/** One persistent, independent pixel buffer shared by workshop projects. */
final class SpriteBufferStore {
    private final AtomicFile file;
    SpriteBufferStore(File files){file=new AtomicFile(new File(files,"sprite-buffer.pksp"));}
    synchronized SpriteAsset read()throws IOException{
        try{byte[] bytes=file.readFully();return SpriteAsset.decode(bytes);}
        catch(FileNotFoundException e){return null;}
    }
    synchronized void write(SpriteAsset asset)throws IOException{
        byte[] bytes=asset.encode();SpriteAsset.decode(bytes);FileOutputStream out=null;
        try{out=file.startWrite();out.write(bytes);file.finishWrite(out);}
        catch(IOException e){if(out!=null)file.failWrite(out);throw e;}
        if(!Arrays.equals(file.readFully(),bytes))throw new IOException("Could not verify the sprite buffer. Project pixels were preserved.");
    }
}
