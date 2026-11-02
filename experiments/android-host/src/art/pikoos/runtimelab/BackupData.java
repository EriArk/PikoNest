package art.pikoos.runtimelab;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Xml;
import art.pikoos.lab.core.*;
import org.json.JSONObject;
import org.xmlpull.v1.XmlSerializer;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Captures active workspaces, typed UI drafts/settings and the current runtime home. */
final class BackupData {
    interface Open {InputStream read()throws Exception;}
    static boolean allowedPreference(String name){return name.matches("[a-zA-Z0-9_.-]{1,120}")&&!Arrays.asList("runtime-session","runtime-setup","external-launch","project-location","library-location").contains(name);}
    static VerifiedTransfer.Source source(Context c)throws Exception{
        // Resolve interrupted external saves before taking a current canonical snapshot.
        ProjectRepository projects=ProjectStorage.open(c);for(String id:projects.ids())projects.read(id);
        LibraryStorage.open(c).snapshot();
        String projectLocation=ProjectStorage.identity(c),libraryLocation=LibraryStorage.identity(c);File home=RuntimeHomes.current(c);
        return new VerifiedTransfer.Source(){
            private Map<String,Open> captured;
            private Map<String,Open> entries()throws Exception{
                if(!projectLocation.equals(ProjectStorage.identity(c))||!libraryLocation.equals(LibraryStorage.identity(c))||!home.equals(RuntimeHomes.current(c)))throw new IOException("Storage selection changed during backup");
                Map<String,Open> files=new TreeMap<>();
                workspace(c,files,projectLocation,true);workspace(c,files,libraryLocation,false);
                local(files,home,"runtime/","");
                File prefs=new File(c.getApplicationInfo().dataDir,"shared_prefs");File[] names=prefs.listFiles();if(names==null&&prefs.exists())throw new IOException("Cannot list settings");
                Set<String> seen=new TreeSet<>();if(names!=null)for(File f:names){String n=f.getName().replaceFirst("\\.xml(\\.bak)?$","");if(!f.getName().equals(n)&&allowedPreference(n))seen.add(n);}
                for(String name:seen)files.put("preferences/"+name+".pkpf",()->new ByteArrayInputStream(BackupPreferences.encode(c.getSharedPreferences(name,0).getAll())));
                for(String name:new String[]{"pending-import.bin","pending-export.bin","project-location.json","library-location.json"}){File f=new File(c.getFilesDir(),name);if(f.exists()||new File(f+".bak").exists())files.put("recovery/"+name,()->new ByteArrayInputStream(new android.util.AtomicFile(f).readFully()));}
                return files;
            }
            public List<String> files()throws Exception{captured=entries();return new ArrayList<>(captured.keySet());}
            public InputStream open(String name)throws Exception{if(captured==null)files();Open o=captured.get(name);if(o==null)throw new IOException("Backup source disappeared");return o.read();}
        };
    }
    private static void workspace(Context c,Map<String,Open> result,String identity,boolean projects)throws Exception{
        String[] roots=projects?new String[]{"projects"}:new String[]{"asset-library","sprite-buffer.pksp"};
        if(identity.isEmpty()){
            for(String root:roots){
                File local=new File(c.getFilesDir(),root);
                if(root.equals("sprite-buffer.pksp")){
                    if(local.exists()||new File(local+".bak").exists())result.put("library/"+root,()->new ByteArrayInputStream(new android.util.AtomicFile(local).readFully()));
                }else local(result,local,projects?"projects/":"library/"+root+"/","");
            }return;
        }
        JSONObject location=new JSONObject(identity);StorageCopy.Tree tree=new StorageCopy.Tree(c.getContentResolver(),Uri.parse(location.getString("tree")),location.getString("root"));
        for(String root:roots){Uri uri=tree.resolve(root,false);if(uri!=null)walkTree(c,tree,uri,root,projects?"projects/":"library/"+root+(root.equals("asset-library")?"/":""),result,new HashSet<>(),0);}
    }
    private static void walkTree(Context c,StorageCopy.Tree tree,Uri uri,String relative,String output,Map<String,Open> result,Set<String> visited,int depth)throws Exception{
        if(depth>32||!visited.add(DocumentsContract.getDocumentId(uri)))throw new IOException("Backup folder cycle or excessive depth");
        String mime;try(Cursor info=c.getContentResolver().query(uri,new String[]{DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){if(info==null||!info.moveToFirst())throw new IOException("Cannot inspect backup source");mime=info.getString(0);}
        if(!DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)){if(result.put(output,()->tree.read(relative))!=null)throw new IOException("Duplicate backup source");return;}
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(uri,DocumentsContract.getDocumentId(uri));Set<String> names=new HashSet<>();
        try(Cursor cursor=c.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
            if(cursor==null)throw new IOException("Cannot list backup folder");while(cursor.moveToNext()){
                String name=cursor.getString(1);if(name==null||name.contains("/")||!names.add(name.toLowerCase(Locale.ROOT)))throw new IOException("Ambiguous backup file name");VerifiedTransfer.path(name);
                Uri child=DocumentsContract.buildDocumentUriUsingTree(uri,cursor.getString(0));
                String childPath=output+name;String childMime;try(Cursor info=c.getContentResolver().query(child,new String[]{DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){if(info==null||!info.moveToFirst())throw new IOException("Cannot inspect backup entry");childMime=info.getString(0);}
                walkTree(c,tree,child,relative+"/"+name,childPath+(DocumentsContract.Document.MIME_TYPE_DIR.equals(childMime)?"/":""),result,visited,depth+1);
                if(result.size()>BackupArchive.MAX_FILES)throw new IOException("Too many backup files");
            }
        }
    }
    private static void local(Map<String,Open> result,File root,String prefix,String unused)throws Exception{
        if(!root.exists())return;if(Files.isSymbolicLink(root.toPath()))throw new IOException("Linked backup source");
        if(root.isFile()){result.put(prefix,()->new FileInputStream(root));return;}
        Map<String,RuntimeHomeMigration.Entry> entries=RuntimeHomeMigration.scan(root,()->false);
        for(String relative:entries.keySet()){if(prefix.equals("runtime/")&&(relative.startsWith("logs/")||relative.equals(".pikonest-home")))continue;File f=new File(root,relative);result.put(prefix+relative,()->new FileInputStream(f));}
    }
    static void prepareRestore(Context c,File extracted,File stage)throws Exception{
        File next=new File(stage,"next"),files=new File(next,"files");if(!files.mkdirs())throw new IOException("Cannot stage restored data");
        move(extracted,"projects",files,"projects");move(extracted,"library/asset-library",files,"asset-library");move(extracted,"library/sprite-buffer.pksp",files,"sprite-buffer.pksp");move(extracted,"runtime",files,"runtime-data");
        for(String name:new String[]{"projects","asset-library","runtime-data/data","runtime-data/logs","project-save-journals","library-save-journals"})new File(files,name).mkdirs();
        // Always restore active content into a new internal copy. Android grants cannot be restored.
        Files.write(new File(files,"project-location.json").toPath(),new byte[0]);Files.write(new File(files,"library-location.json").toPath(),new byte[0]);
        for(String name:new String[]{"pending-import.bin","pending-export.bin"})move(extracted,"recovery/"+name,files,name);
        File prefs=new File(next,"shared_prefs");if(!prefs.mkdir())throw new IOException("Cannot stage restored settings");
        File[] stored=new File(extracted,"preferences").listFiles();if(stored!=null)for(File f:stored){
            String name=f.getName();if(!name.endsWith(".pkpf"))throw new IOException("Unknown backed-up preference file");name=name.substring(0,name.length()-5);if(!allowedPreference(name)||!f.isFile())throw new IOException("Unsupported preference name");
            if(f.length()>8*1024*1024)throw new IOException("Settings file too large");Map<String,Object> values=BackupPreferences.decode(Files.readAllBytes(f.toPath()));
            if(name.equals("folder-setup"))values.clear(); // Reconnect through the guide; do not activate stale provider IDs.
            if(name.equals("first-run")){values.clear();values.put("step",0);values.put("dismissed",false);}
            if(name.equals("library-ui")){values.remove("awaitingReturn");values.remove("diagnosticToken");values.put("shelf",true);}
            if(name.endsWith("-ui"))values.remove("awaitingReturn");
            if(name.equals("play-library")){values.remove("runtime");values.remove("splore");}
            xml(new File(prefs,name+".xml"),values);
        }
        // Reading through the real repositories catches unusable payloads before scheduling restore.
        ProjectRepository restored=new ProjectRepository(new ProjectStore(files));for(String id:restored.ids())restored.read(id);
        new LibraryRepository(new LocalLibraryStorage(files)).snapshot();
        BackupSwap.prepare(new File(c.getApplicationInfo().dataDir),stage);
    }
    private static void move(File source,String from,File target,String to)throws IOException{File f=new File(source,from);if(f.exists()){File dest=new File(target,to);dest.getParentFile().mkdirs();Files.move(f.toPath(),dest.toPath());}}
    private static void xml(File file,Map<String,Object> values)throws Exception{
        try(FileOutputStream out=new FileOutputStream(file)){XmlSerializer x=Xml.newSerializer();x.setOutput(out,"UTF-8");x.startDocument("UTF-8",true);x.startTag(null,"map");
            for(Map.Entry<String,Object> e:values.entrySet()){Object v=e.getValue();String tag=v instanceof String?"string":v instanceof Boolean?"boolean":v instanceof Integer?"int":v instanceof Long?"long":v instanceof Float?"float":"set";x.startTag(null,tag);x.attribute(null,"name",e.getKey());
                if(v instanceof String)x.text((String)v);else if(v instanceof Set){for(Object s:(Set<?>)v){x.startTag(null,"string");x.text((String)s);x.endTag(null,"string");}}else x.attribute(null,"value",v.toString());x.endTag(null,tag);
            }x.endTag(null,"map");x.endDocument();x.flush();out.getFD().sync();
        }
    }
}
