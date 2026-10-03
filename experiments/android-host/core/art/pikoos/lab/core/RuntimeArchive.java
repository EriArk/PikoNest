package art.pikoos.lab.core;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.zip.*;

/** Bounded structural inspection of a user-supplied Raspberry Pi archive.
 * Does not execute binaries or certify their publisher, version or runtime readiness. */
public final class RuntimeArchive {
    public static final long MAX_ZIP=64L*1024*1024,MAX_EXPANDED=128L*1024*1024;
    public static final int MAX_ENTRIES=512;
    public static final class Result {
        public final long bytes,binaryBytes,dataBytes;
        public final String sha256;
        Result(long bytes,long binary,long data,String hash){this.bytes=bytes;binaryBytes=binary;dataBytes=data;sha256=hash;}
    }
    public static void check(BooleanSupplier cancelled){if(cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();}
    public static long copy(InputStream in,OutputStream out,BooleanSupplier cancelled)throws IOException{
        if(in==null)throw problem("Не удалось открыть архив. Выбери его снова.");
        long total=0;byte[] buffer=new byte[32768];
        for(int n;(n=in.read(buffer))!=-1;){check(cancelled);total+=n;if(total>MAX_ZIP)throw problem("Архив больше 64 МиБ — лимит этой версии PIKOOS");out.write(buffer,0,n);}
        check(cancelled);return total;
    }
    public static Result inspect(File archive,BooleanSupplier cancelled)throws IOException{
        if(archive.length()==0||archive.length()>MAX_ZIP)throw problem("Нужен ZIP-архив размером до 64 МиБ");
        long binary=0,data=0,total=0;boolean readme=false;int count=0;
        Set<String> names=new HashSet<>();
        try(ZipFile zip=new ZipFile(archive)){
            Enumeration<? extends ZipEntry> entries=zip.entries();
            while(entries.hasMoreElements()){
                check(cancelled);ZipEntry entry=entries.nextElement();String name=entry.getName();
                if(++count>MAX_ENTRIES)throw problem("В архиве слишком много файлов");
                path(name,entry.isDirectory());
                String key=(entry.isDirectory()?name.substring(0,name.length()-1):name).toLowerCase(Locale.ROOT);
                if(!names.add(key))throw problem("В архиве повторяются имена файлов");
                if(entry.getSize()<0||entry.getSize()>MAX_ZIP||entry.getCompressedSize()<0)throw problem("Не удалось проверить размер файла в архиве");
                if(entry.isDirectory()){if(entry.getSize()!=0)throw problem("Некорректная папка в архиве");continue;}
                long actual=0;CRC32 crc=new CRC32();byte[] header=new byte[64];int kept=0;byte[] buffer=new byte[32768];
                try(InputStream input=zip.getInputStream(entry)){
                    for(int n;(n=input.read(buffer))!=-1;){
                        check(cancelled);actual+=n;total+=n;
                        if(actual>MAX_ZIP||total>MAX_EXPANDED)throw problem("Распакованные файлы превышают лимит PIKOOS");
                        int take=Math.min(n,header.length-kept);if(take>0){System.arraycopy(buffer,0,header,kept,take);kept+=take;}
                        crc.update(buffer,0,n);
                    }
                }
                if(actual!=entry.getSize()||crc.getValue()!=entry.getCrc())throw problem("Архив повреждён. Скачай его заново.");
                if(name.equals("pico-8/pico8_64")){
                    if(kept<64||header[0]!=127||header[1]!='E'||header[2]!='L'||header[3]!='F'||header[4]!=2||header[5]!=1||header[6]!=1||(header[18]&255)!=183||header[19]!=0||!((header[16]==2||header[16]==3)&&header[17]==0))
                        throw problem("В архиве нет подходящего ARM64 PICO-8. Нужен Raspberry Pi ZIP.");
                    binary=actual;
                }
                if(name.equals("pico-8/pico8.dat"))data=actual;
                if(name.equals("pico-8/readme_raspi.txt"))readme=actual>0;
            }
        }catch(ZipException e){throw problem("Не удалось прочитать ZIP. Выбери целый архив PICO-8.");}
        if(binary==0||data==0||!readme)throw problem("Нужен архив PICO-8 для Raspberry Pi. Linux amd64 и i386 сюда не подходят.");
        check(cancelled);return new Result(archive.length(),binary,data,hash(archive,cancelled));
    }
    private static void path(String value,boolean directory)throws IOException{
        String name=directory?value.substring(0,value.length()-1):value;
        if(name.isEmpty()||name.length()>240||name.startsWith("/")||name.indexOf('\\')>=0||name.indexOf(':')>=0)throw problem("Недопустимый путь внутри архива");
        for(char c:name.toCharArray())if(c<32||c==127)throw problem("Недопустимое имя файла в архиве");
        for(String part:name.split("/",-1))if(part.isEmpty()||part.equals(".")||part.equals(".."))throw problem("Архив содержит путь за пределами своей папки");
    }
    public static String hash(File file,BooleanSupplier cancelled)throws IOException{
        try{
            MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[32768];
            try(InputStream in=new FileInputStream(file)){for(int n;(n=in.read(buffer))!=-1;){check(cancelled);digest.update(buffer,0,n);}}
            StringBuilder result=new StringBuilder();for(byte b:digest.digest())result.append(String.format(Locale.ROOT,"%02x",b&255));return result.toString();
        }catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
    }
    private static IOException problem(String text){return new IOException(text);}
}
