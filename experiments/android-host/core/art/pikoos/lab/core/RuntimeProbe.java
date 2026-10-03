package art.pikoos.lab.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.zip.*;

/** Private runtime-test transport. Fixed outputs, no archive-controlled extraction paths. */
public final class RuntimeProbe {
    public static final String[] FILES={"pico8_64","pico8.dat","probe.p8"};
    public static final long MAX_BYTES=128L*1024*1024+65536+128;
    private static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(Exception e){throw new IllegalStateException(e);}}
    public static void prepare(File zipFile,String expectedHash,byte[] cart,OutputStream output,BooleanSupplier cancelled)throws IOException{
        RuntimeArchive.Result checked=RuntimeArchive.inspect(zipFile,cancelled);
        if(!checked.sha256.equals(expectedHash))throw new IOException("Сохранённый архив изменился. Выбери ZIP снова.");
        if(cart.length==0||cart.length>65536)throw new IOException("Недопустимый тестовый картридж");
        DataOutputStream out=new DataOutputStream(output);out.write("PIKORUN1".getBytes(StandardCharsets.US_ASCII));
        try(ZipFile zip=new ZipFile(zipFile)){
            for(String name:new String[]{"pico8_64","pico8.dat"}){
                ZipEntry entry=zip.getEntry("pico-8/"+name);MessageDigest hash=digest();byte[] buffer=new byte[32768];
                try(InputStream in=zip.getInputStream(entry)){for(int n;(n=in.read(buffer))!=-1;){RuntimeArchive.check(cancelled);hash.update(buffer,0,n);}}
                out.writeInt((int)entry.getSize());out.write(hash.digest());
                try(InputStream in=zip.getInputStream(entry)){RuntimeArchive.copy(in,out,cancelled);}
            }
        }
        out.writeInt(cart.length);out.write(digest().digest(cart));out.write(cart);out.flush();
    }
    /** Caller creates a fresh private directory and owns cleanup on error. No input path is extracted. */
    public static void unpack(InputStream input,File directory,BooleanSupplier cancelled)throws IOException{
        String[] present=directory.list();if(present==null||present.length!=0)throw new IOException("Нужна новая пустая папка проверки");
        DataInputStream in=new DataInputStream(input);byte[] magic=new byte[8];in.readFully(magic);
        if(!Arrays.equals(magic,"PIKORUN1".getBytes(StandardCharsets.US_ASCII)))throw new IOException("Некорректный пакет проверки PICO-8");
        long total=0;
        for(int i=0;i<FILES.length;i++){
            RuntimeArchive.check(cancelled);int length=in.readInt();int limit=i==2?65536:(int)RuntimeArchive.MAX_ZIP;
            if(length<1||length>limit||(total+=length)>MAX_BYTES-128)throw new IOException("Недопустимый размер пакета PICO-8");
            byte[] expected=new byte[32];in.readFully(expected);MessageDigest hash=digest();byte[] header=new byte[64];int kept=0;
            File file=new File(directory,FILES[i]);if(!file.createNewFile())throw new IOException("Файл проверки уже существует");
            try(FileOutputStream out=new FileOutputStream(file)){
                byte[] buffer=new byte[32768];int remaining=length;
                while(remaining>0){RuntimeArchive.check(cancelled);int n=in.read(buffer,0,Math.min(remaining,buffer.length));if(n<0)throw new EOFException("Неполный пакет PICO-8");
                    int take=Math.min(n,64-kept);if(take>0){System.arraycopy(buffer,0,header,kept,take);kept+=take;}
                    hash.update(buffer,0,n);out.write(buffer,0,n);remaining-=n;
                }out.getFD().sync();
            }
            if(!MessageDigest.isEqual(expected,hash.digest()))throw new IOException("Файл проверки повреждён");
            if(i==0&&(kept<64||header[0]!=127||header[1]!='E'||header[2]!='L'||header[3]!='F'||header[4]!=2||header[5]!=1||(header[18]&255)!=183||header[19]!=0))throw new IOException("Нужен PICO-8 ARM64");
            if(i==2&&!new String(header,0,kept,StandardCharsets.US_ASCII).startsWith("pico-8 cartridge"))throw new IOException("Нет тестового картриджа");
        }
        if(in.read()!=-1)throw new IOException("Лишние данные пакета PICO-8");RuntimeArchive.check(cancelled);
    }
}
