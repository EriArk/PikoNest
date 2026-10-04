package art.pikoos.runtimelab;

import android.util.AtomicFile;
import art.pikoos.lab.core.ParameterPreset;
import java.io.*;
import java.util.*;

/** App-private prototype storage. No cart paths or indexes are needed to read an asset. */
final class ParameterPresetStore {
    private final File root;
    ParameterPresetStore(File files)throws IOException{
        root=new File(files,"asset-library/parameters").getCanonicalFile();
        if(!root.isDirectory()&&!root.mkdirs())throw new IOException("Не удалось открыть библиотеку ресурсов");
    }
    private File file(String id)throws IOException{
        if(!id.matches("[0-9a-f-]{36}"))throw new IOException("Некорректный адрес ресурса");
        File f=new File(root,id+".pkpr").getCanonicalFile();
        if(!root.equals(f.getParentFile()))throw new IOException("Ресурс вне папки библиотеки");return f;
    }
    List<ParameterPreset> list()throws IOException{
        File[] files=root.listFiles();if(files==null)throw new IOException("Не удалось прочитать библиотеку");
        ArrayList<ParameterPreset> result=new ArrayList<>();
        for(File f:files)if(f.getName().endsWith(".pkpr")){
            try{
                String id=f.getName().substring(0,f.getName().length()-5);
                File safe=file(id);
                if(safe.length()>20000)throw new IOException("Ресурс слишком велик");
                ParameterPreset asset=ParameterPreset.decode(new AtomicFile(safe).readFully());
                if(!asset.id.equals(id))throw new IOException("ID не совпадает с именем файла");
                result.add(asset);
            }catch(IOException e){throw new IOException("Не удалось прочитать "+f.getName()+". Файл сохранён.",e);}
        }
        Collections.sort(result,(a,b)->{int t=a.title.compareTo(b.title);return t==0?a.id.compareTo(b.id):t;});
        return result;
    }
    synchronized void create(ParameterPreset asset)throws IOException{
        File f=file(asset.id);AtomicFile target=new AtomicFile(f);
        if(f.exists()){
            if(Arrays.equals(target.readFully(),asset.encode()))return; // Retry after a completed write.
            throw new IOException("Ресурс с этим ID уже существует");
        }
        FileOutputStream out=null;
        try{out=target.startWrite();out.write(asset.encode());target.finishWrite(out);}
        catch(IOException e){target.failWrite(out);throw e;}
    }
    synchronized void update(ParameterPreset expected,ParameterPreset next)throws IOException{
        AtomicFile target=new AtomicFile(file(expected.id));
        byte[] actual=target.readFully();
        if(Arrays.equals(actual,next.encode()))return;
        if(!Arrays.equals(actual,expected.encode()))throw new IOException("Ресурс изменился. Закрой и заново открой библиотеку.");
        FileOutputStream out=null;
        try{out=target.startWrite();out.write(next.encode());target.finishWrite(out);}
        catch(IOException e){target.failWrite(out);throw e;}
    }
}
