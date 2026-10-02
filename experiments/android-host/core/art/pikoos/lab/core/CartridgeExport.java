package art.pikoos.lab.core;

import java.io.*;

/** Immutable saved-cart snapshot. Export verification checks bytes, never Lua behavior. */
public final class CartridgeExport {
    public enum State { PREVIEW, WRITING, SAVED, UNCERTAIN }
    public final String sourceId,title,filename,resultName;
    public final State state;
    public final WorkshopCartridge cart;
    public CartridgeExport(String sourceId,String title,byte[] bytes){this(sourceId,title,bytes,State.PREVIEW,"");}
    private CartridgeExport(String sourceId,String title,byte[] bytes,State state,String resultName){
        if(!LibrarySession.validId(sourceId))throw new IllegalArgumentException("Неизвестный проект");
        if(title==null||title.length()>160||resultName==null||resultName.length()>160)throw new IllegalArgumentException("Слишком длинное имя файла");
        if(bytes==null||bytes.length>CartridgeImport.MAX_BYTES)throw new IllegalArgumentException("Экспорт PIKOOS пока ограничен 2 МиБ");
        this.sourceId=sourceId;this.title=title;this.state=state;this.resultName=resultName;
        String name=title.replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]"," ").trim().replaceAll("[. ]+$","");
        if(name.length()>76)name=name.substring(0,76);
        filename=(name.isEmpty()?"game":name)+".p8";
        cart=new WorkshopCartridge(bytes);
    }
    public CartridgeExport withState(State state,String resultName){return new CartridgeExport(sourceId,title,cart.bytes(),state,resultName);}
    public void writeTo(OutputStream out)throws IOException{
        if(out==null)throw new IOException("Не удалось открыть файл для записи");
        byte[] bytes=cart.bytes();for(int offset=0;offset<bytes.length;offset+=8192)out.write(bytes,offset,Math.min(8192,bytes.length-offset));
        out.flush();
    }
    public void verify(InputStream in)throws IOException{
        if(in==null)throw new IOException("Не удалось проверить записанный файл");
        byte[] expected=cart.bytes(),buffer=new byte[8192];int offset=0,n;
        while((n=in.read(buffer))!=-1){
            if(n>expected.length-offset)throw new IOException("Записанный файл отличается от проекта");
            for(int i=0;i<n;i++)if(buffer[i]!=expected[offset+i])throw new IOException("Записанный файл отличается от проекта");
            offset+=n;
        }
        if(offset!=expected.length)throw new IOException("Файл записан не полностью");
    }
    public byte[] encode()throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(out);
        d.writeInt(0x504b4531);d.writeUTF(sourceId);d.writeUTF(title);d.writeUTF(resultName);d.writeInt(state.ordinal());
        byte[] bytes=cart.bytes();d.writeInt(bytes.length);d.write(bytes);d.flush();return out.toByteArray();
    }
    public static CartridgeExport decode(byte[] record)throws IOException{
        if(record.length>CartridgeImport.MAX_BYTES+2048)throw new IOException("Слишком большой черновик экспорта");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(record));
        if(in.readInt()!=0x504b4531)throw new IOException("Неизвестный черновик экспорта");
        String id=in.readUTF(),title=in.readUTF(),name=in.readUTF();int state=in.readInt(),size=in.readInt();
        if(state<0||state>=State.values().length||size<0||size>CartridgeImport.MAX_BYTES||size!=in.available())throw new IOException("Повреждённый черновик экспорта");
        byte[] bytes=new byte[size];in.readFully(bytes);return new CartridgeExport(id,title,bytes,State.values()[state],name);
    }
}
