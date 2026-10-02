package art.pikoos.runtimelab;

import android.util.AtomicFile;
import art.pikoos.lab.core.LibrarySession;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Android filesystem adapter. New projects are published only after their cart is complete. */
final class ProjectStore {
    private final File root;
    ProjectStore(File files)throws Exception {
        root=new File(files,"projects").getCanonicalFile();
        if(!root.isDirectory()&&!root.mkdirs())throw new Exception("Не удалось открыть папку проектов");
    }
    File directory(String id)throws Exception {
        if(!LibrarySession.validId(id))throw new Exception("Некорректный адрес проекта");
        File result=new File(root,id).getCanonicalFile();
        if(!root.equals(result.getParentFile()))throw new Exception("Проект вне своей папки");
        return result;
    }
    AtomicFile cart(String id)throws Exception{return new AtomicFile(new File(directory(id),"game.p8"));}
    List<String> ids()throws Exception {
        File[] files=root.listFiles();if(files==null)throw new Exception("Не удалось прочитать полку");
        ArrayList<String> result=new ArrayList<>();
        for(File file:files)if(file.isDirectory()&&LibrarySession.validId(file.getName()))result.add(file.getName());
        return result;
    }
    byte[] read(String id)throws Exception{return cart(id).readFully();}
    synchronized void create(String id,byte[] bytes)throws Exception {
        File target=directory(id);
        if(target.exists())throw new Exception("Проект уже существует. Обнови полку");
        File staging=new File(root,".creating-"+UUID.randomUUID());
        if(!staging.mkdir())throw new Exception("Не удалось создать проект");
        // Interrupted/failed staging directories stay hidden and never replace an existing cart.
        try(FileOutputStream out=new FileOutputStream(new File(staging,"game.p8"))){out.write(bytes);out.getFD().sync();}
        if(target.exists()||!staging.renameTo(target))throw new Exception("Не удалось завершить создание проекта");
    }
}
