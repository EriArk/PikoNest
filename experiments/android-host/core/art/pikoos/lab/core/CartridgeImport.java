package art.pikoos.lab.core;

import java.io.*;
import java.util.Locale;
import java.util.UUID;

/** A read-only snapshot awaiting confirmation, not a compatibility verdict. */
public final class CartridgeImport {
    // Lab intake budget, NOT a PICO-8 cartridge size limit.
    public static final int MAX_BYTES=2*1024*1024;
    public final String id, filename;
    public final WorkshopCartridge cart;
    public CartridgeImport(String filename,byte[] bytes){this("import-"+UUID.randomUUID(),filename,bytes);}
    private CartridgeImport(String id,String filename,byte[] bytes){
        if(!validId(id))throw new IllegalArgumentException("Некорректный адрес импорта");
        if(filename==null||!filename.toLowerCase(Locale.ROOT).endsWith(".p8"))
            throw new IllegalArgumentException("Пока выбирай текстовый .p8. Импорт .p8.png и архивов ещё не готов.");
        if(bytes==null||bytes.length>MAX_BYTES)throw new IllegalArgumentException("Импорт PIKOOS пока ограничен 2 МиБ. Это ограничение мастерской.");
        this.id=id;this.filename=displayName(filename);
        try{cart=new WorkshopCartridge(bytes);}
        catch(IllegalArgumentException e){throw new IllegalArgumentException("PIKOOS пока не может открыть этот файл: "+e.getMessage());}
    }
    public static boolean validId(String id){return id!=null&&id.matches("import-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");}
    public static String displayName(String name){
        String clean=name.replaceAll("[\\p{Cntrl}/\\\\]"," ").trim();
        return clean.length()>80?clean.substring(0,76)+".p8":clean;
    }
    public String title(){String title=filename.substring(0,filename.length()-3);return title.trim().isEmpty()?"Картридж":title;}
    public byte[] encode()throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(out);
        d.writeInt(0x504b4931);d.writeUTF(id);d.writeUTF(filename);byte[] bytes=cart.bytes();d.writeInt(bytes.length);d.write(bytes);d.flush();return out.toByteArray();
    }
    public static CartridgeImport decode(byte[] record)throws IOException{
        if(record.length>MAX_BYTES+1024)throw new IOException("Слишком большой черновик импорта");
        DataInputStream d=new DataInputStream(new ByteArrayInputStream(record));
        if(d.readInt()!=0x504b4931)throw new IOException("Неизвестный черновик импорта");
        String id=d.readUTF(),name=d.readUTF();int size=d.readInt();
        if(size<0||size>MAX_BYTES||size!=d.available())throw new IOException("Неполный черновик импорта");
        byte[] bytes=new byte[size];d.readFully(bytes);return new CartridgeImport(id,name,bytes);
    }
    public static byte[] readBounded(InputStream in)throws IOException{
        if(in==null)throw new IOException("Не удалось прочитать выбранный файл");
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1){
            if(out.size()+n>MAX_BYTES)throw new IOException("Импорт PIKOOS пока ограничен 2 МиБ. Это ограничение мастерской.");
            out.write(buffer,0,n);
        }
        return out.toByteArray();
    }
}
