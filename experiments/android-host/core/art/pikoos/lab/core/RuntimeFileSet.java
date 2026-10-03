package art.pikoos.lab.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Portable launch payload, not an editable/distribution cartridge format. */
public final class RuntimeFileSet {
    public static final int MAX_FILES=32,MAX_BYTES=8*1024*1024;
    public final String entry;
    private final Map<String,byte[]> files=new LinkedHashMap<>();
    public RuntimeFileSet(String entry,Map<String,byte[]> input){
        name(entry);this.entry=entry;long total=0;
        if(input.isEmpty()||input.size()>MAX_FILES)throw new IllegalArgumentException("Лимит запуска PIKOOS: 32 файла");
        Set<String> folded=new HashSet<>();
        for(Map.Entry<String,byte[]> file:input.entrySet()){
            name(file.getKey());byte[] bytes=file.getValue();
            if(bytes==null||bytes.length==0||bytes.length>CartridgeImport.MAX_BYTES||(total+=bytes.length)>MAX_BYTES)
                throw new IllegalArgumentException("Набор игры превышает лимит запуска PIKOOS");
            if(!folded.add(file.getKey().toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Имена частей различаются только регистром");
            files.put(file.getKey(),bytes.clone());
        }
        if(!files.containsKey(entry))throw new IllegalArgumentException("Нет стартового картриджа");
    }
    public static String name(String name){
        if(name==null||name.length()>120||!name.matches("[A-Za-z0-9_-][A-Za-z0-9_.-]*\\.p8")||name.contains(".."))
            throw new IllegalArgumentException("Мультикарт пока использует имена .p8 в одной папке, без пробелов");
        return name;
    }
    public Set<String> names(){return Collections.unmodifiableSet(files.keySet());}
    public byte[] bytes(String name){return files.get(name).clone();}
    /** Big-endian lengths, ASCII names, raw bytes. Bounded reader also lives in adapter. */
    public byte[] transport()throws IOException{
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(buffer);
        out.write("PIKOSET1".getBytes(StandardCharsets.US_ASCII));writeName(out,entry);out.writeInt(files.size());
        for(Map.Entry<String,byte[]> file:files.entrySet()){writeName(out,file.getKey());out.writeInt(file.getValue().length);out.write(file.getValue());}
        out.flush();return buffer.toByteArray();
    }
    private static void writeName(DataOutputStream out,String name)throws IOException{byte[] bytes=name.getBytes(StandardCharsets.US_ASCII);out.writeInt(bytes.length);out.write(bytes);}
}
