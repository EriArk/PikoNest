package art.pikoos.runtimelab;

import android.util.AtomicFile;
import art.pikoos.lab.core.LibrarySession;
import art.pikoos.lab.core.CartridgeImport;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Android filesystem adapter. New projects are published only after their cart is complete. */
final class ProjectStore implements art.pikoos.lab.core.ProjectRepository.Storage {
    private final File root;
    ProjectStore(File files)throws Exception {
        root=new File(files,"projects").getCanonicalFile();
        if(!root.isDirectory()&&!root.mkdirs())throw new Exception("Не удалось открыть папку проектов");
    }
    private File directory(String id)throws Exception {
        if(!LibrarySession.validId(id))throw new Exception("Некорректный адрес проекта");
        File result=new File(root,id).getCanonicalFile();
        if(!root.equals(result.getParentFile()))throw new Exception("Проект вне своей папки");
        return result;
    }
    private AtomicFile cart(String id)throws Exception{return new AtomicFile(new File(directory(id),"game.p8"));}
    public boolean exists(String id)throws Exception{return directory(id).exists();}
    public List<String> ids()throws Exception {
        File[] files=root.listFiles();if(files==null)throw new Exception("Не удалось прочитать полку");
        ArrayList<String> result=new ArrayList<>();
        for(File file:files)if(file.isDirectory()&&LibrarySession.validId(file.getName()))result.add(file.getName());
        return result;
    }
    public byte[] read(String id)throws Exception{synchronized(ProjectStore.class){return cart(id).readFully();}}
    public String importedTitle(String id)throws Exception{
        if(!CartridgeImport.validId(id))return LibrarySession.title(id);
        return new String(new AtomicFile(new File(directory(id),".pikoos/import-name")).readFully(),java.nio.charset.StandardCharsets.UTF_8);
    }
    public void replace(String id,byte[] expected,byte[] next)throws Exception {
        synchronized(ProjectStore.class){
            AtomicFile target=cart(id);
            if(!java.util.Arrays.equals(target.readFully(),expected))throw new Exception("Project changed before save. Stored source kept.");
            FileOutputStream output=null;
            try{output=target.startWrite();output.write(next);target.finishWrite(output);}
            catch(Exception failure){target.failWrite(output);throw failure;}
        }
    }
    public void publish(String id,byte[] bytes,String title)throws Exception {
        synchronized(ProjectStore.class){publishLocked(id,bytes,title);}
    }
    private void publishLocked(String id,byte[] bytes,String title)throws Exception {
        File target=directory(id);
        if(target.exists())throw new Exception("Проект уже существует. Обнови полку");
        File staging=new File(root,".creating-"+UUID.randomUUID());
        if(!staging.mkdir())throw new Exception("Не удалось создать проект");
        // Interrupted/failed staging directories stay hidden and never replace an existing cart.
        try(FileOutputStream out=new FileOutputStream(new File(staging,"game.p8"))){out.write(bytes);out.getFD().sync();}
        if(title!=null){
            File meta=new File(staging,".pikoos");if(!meta.mkdir())throw new Exception("Не удалось сохранить имя импорта");
            try(FileOutputStream out=new FileOutputStream(new File(meta,"import-name"))){out.write(title.getBytes(java.nio.charset.StandardCharsets.UTF_8));out.getFD().sync();}
        }
        if(!java.util.Arrays.equals(bytes,java.nio.file.Files.readAllBytes(new File(staging,"game.p8").toPath())))throw new Exception("Could not verify new project. Candidate kept.");
        if(title!=null&&!title.equals(new String(java.nio.file.Files.readAllBytes(new File(staging,".pikoos/import-name").toPath()),"UTF-8")))throw new Exception("Could not verify imported title. Candidate kept.");
        if(target.exists()||!staging.renameTo(target))throw new Exception("Не удалось завершить создание проекта");
    }
}
