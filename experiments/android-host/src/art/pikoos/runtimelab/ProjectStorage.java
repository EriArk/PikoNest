package art.pikoos.runtimelab;

import android.content.*;
import org.json.JSONObject;
import art.pikoos.lab.core.*;
import android.util.AtomicFile;
import java.io.*;
import java.util.Arrays;

/** One durable location switch. Missing access never silently selects the internal copies. */
final class ProjectStorage {
    static volatile boolean activating;
    static void checkLocalWrite()throws Exception{if(activating)throw new Exception("Project folder activation is in progress. Finish or cancel it before editing.");}
    private static AtomicFile location(Context c){return new AtomicFile(new File(c.getFilesDir(),"project-location.json"));}
    static synchronized String identity(Context c){
        try{return new String(location(c).readFully(),"UTF-8");}
        catch(FileNotFoundException absent){return c.getSharedPreferences("project-location",0).getString("location","");}
        catch(Exception unavailable){return "!project-location-needs-recovery";}
    }
    static boolean selectedActive(Context c,String uri){try{return new JSONObject(identity(c)).getString("tree").equals(uri);}catch(Exception e){return false;}}
    static String description(Context c){if(identity(c).isEmpty())return "Projects: inside PikoNest";try{return "External Projects: "+new JSONObject(identity(c)).optString("label","selected workspace");}catch(Exception e){return "Project location needs recovery";}}
    static synchronized void select(Context c,String value)throws Exception{
        AtomicFile file=location(c);byte[] bytes=value.getBytes("UTF-8");FileOutputStream out=null;
        try{out=file.startWrite();out.write(bytes);file.finishWrite(out);}
        catch(Exception e){file.failWrite(out);throw new IOException("Could not commit project location. Reopen storage controls to check the selection.",e);}
        if(!Arrays.equals(bytes,file.readFully()))throw new IOException("Could not verify project location. Reopen storage controls to check the selection.");
    }
    static ProjectRepository open(Context c)throws Exception{
        String value=identity(c);
        if(value.isEmpty())return new ProjectRepository(new ProjectStore(c.getFilesDir()));
        JSONObject json=new JSONObject(value);return new ProjectRepository(new SafProjectStore(c,json.getString("tree"),json.getString("root")));
    }
}
