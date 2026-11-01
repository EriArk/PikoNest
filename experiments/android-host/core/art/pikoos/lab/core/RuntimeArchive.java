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
        if(in==null)throw problem("Cannot open the archive. Choose it again.");
        long total=0;byte[] buffer=new byte[32768];
        for(int n;(n=in.read(buffer))!=-1;){check(cancelled);total+=n;if(total>MAX_ZIP)throw problem("Archive exceeds this PikoNest build's 64 MiB limit");out.write(buffer,0,n);}
        check(cancelled);return total;
    }
    public static Result inspect(File archive,BooleanSupplier cancelled)throws IOException{
        if(archive.length()==0||archive.length()>MAX_ZIP)throw problem("Choose a ZIP archive up to 64 MiB");
        long binary=0,data=0,total=0;boolean readme=false;int count=0;
        Set<String> names=new HashSet<>();
        try(ZipFile zip=new ZipFile(archive)){
            Enumeration<? extends ZipEntry> entries=zip.entries();
            while(entries.hasMoreElements()){
                check(cancelled);ZipEntry entry=entries.nextElement();String name=entry.getName();
                if(++count>MAX_ENTRIES)throw problem("Too many files in the archive");
                path(name,entry.isDirectory());
                String key=(entry.isDirectory()?name.substring(0,name.length()-1):name).toLowerCase(Locale.ROOT);
                if(!names.add(key))throw problem("Duplicate file names in the archive");
                if(entry.getSize()<0||entry.getSize()>MAX_ZIP||entry.getCompressedSize()<0)throw problem("Cannot verify an archived file size");
                if(entry.isDirectory()){if(entry.getSize()!=0)throw problem("Invalid directory in the archive");continue;}
                long actual=0;CRC32 crc=new CRC32();byte[] header=new byte[64];int kept=0;byte[] buffer=new byte[32768];
                try(InputStream input=zip.getInputStream(entry)){
                    for(int n;(n=input.read(buffer))!=-1;){
                        check(cancelled);actual+=n;total+=n;
                        if(actual>MAX_ZIP||total>MAX_EXPANDED)throw problem("Expanded files exceed this PikoNest build's limit");
                        int take=Math.min(n,header.length-kept);if(take>0){System.arraycopy(buffer,0,header,kept,take);kept+=take;}
                        crc.update(buffer,0,n);
                    }
                }
                if(actual!=entry.getSize()||crc.getValue()!=entry.getCrc())throw problem("Archive damaged. Download it again.");
                if(name.equals("pico-8/pico8_64")){
                    if(kept<64||header[0]!=127||header[1]!='E'||header[2]!='L'||header[3]!='F'||header[4]!=2||header[5]!=1||header[6]!=1||(header[18]&255)!=183||header[19]!=0||!((header[16]==2||header[16]==3)&&header[17]==0))
                        throw problem("No supported ARM64 PICO-8 found. Choose the Raspberry Pi ZIP.");
                    binary=actual;
                }
                if(name.equals("pico-8/pico8.dat"))data=actual;
                if(name.equals("pico-8/readme_raspi.txt"))readme=actual>0;
            }
        }catch(ZipException e){throw problem("Cannot read the ZIP. Choose the complete PICO-8 archive.");}
        if(binary==0||data==0||!readme)throw problem("Choose PICO-8 for Raspberry Pi. Linux amd64 and i386 are not supported by this backend.");
        check(cancelled);return new Result(archive.length(),binary,data,hash(archive,cancelled));
    }
    private static void path(String value,boolean directory)throws IOException{
        String name=directory?value.substring(0,value.length()-1):value;
        if(name.isEmpty()||name.length()>240||name.startsWith("/")||name.indexOf('\\')>=0||name.indexOf(':')>=0)throw problem("Invalid path in the archive");
        for(char c:name.toCharArray())if(c<32||c==127)throw problem("Invalid file name in the archive");
        for(String part:name.split("/",-1))if(part.isEmpty()||part.equals(".")||part.equals(".."))throw problem("Archive contains an unsafe path");
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
