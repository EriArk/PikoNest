package art.pikoos.runtimelab;

import android.content.*;
import org.json.JSONObject;
import art.pikoos.lab.core.*;
import android.util.AtomicFile;
import java.io.*;
import java.util.Arrays;

/** One durable location switch. Missing access never silently selects the internal copies. */
final class LibraryStorage {
    static volatile boolean activating;
    static void checkLocalWrite()throws Exception{if(activating)throw new Exception("Library activation is in progress. Finish or cancel it before editing.");}
    private static AtomicFile location(Context c){return new AtomicFile(new File(c.getFilesDir(),"library-location.json"));}
    static synchronized String identity(Context c){
        try{return new String(location(c).readFully(),"UTF-8");}
        catch(FileNotFoundException absent){return c.getSharedPreferences("library-location",0).getString("location","");}
        catch(Exception unavailable){return "!library-location-needs-recovery";}
    }
    static boolean selectedActive(Context c,String uri){try{return new JSONObject(identity(c)).getString("tree").equals(uri);}catch(Exception e){return false;}}
    static String description(Context c){if(identity(c).isEmpty())return "Library: inside PikoNest";try{return "External library: "+new JSONObject(identity(c)).optString("label","selected workspace");}catch(Exception e){return "Library location needs recovery";}}
    static synchronized void select(Context c,String value)throws Exception{
        AtomicFile file=location(c);byte[] bytes=value.getBytes("UTF-8");FileOutputStream out=null;
        try{out=file.startWrite();out.write(bytes);file.finishWrite(out);}
        catch(Exception e){file.failWrite(out);throw new IOException("Could not commit library location. Reopen storage controls to check the selection.",e);}
        if(!Arrays.equals(bytes,file.readFully()))throw new IOException("Could not verify library location. Reopen storage controls to check the selection.");
    }
    static LibraryRepository open(Context c)throws Exception{
        String value=identity(c);if(value.isEmpty())return new LibraryRepository(new LocalLibraryStorage(c.getFilesDir()));
        JSONObject json=new JSONObject(value);return new LibraryRepository(new SafLibraryStorage(c,json.getString("tree"),json.getString("root")));
    }
}
