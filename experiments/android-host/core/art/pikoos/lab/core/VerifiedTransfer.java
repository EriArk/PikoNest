package art.pikoos.lab.core;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Resumable copy into a uniquely owned destination. No source mutation or overwrite. */
public final class VerifiedTransfer {
    public interface Source {List<String> files() throws Exception;InputStream open(String path)throws Exception;}
    public interface Target {
        boolean exists(String path)throws Exception;
        InputStream read(String path)throws Exception;
        OutputStream create(String path)throws Exception;
    }
    public interface Progress {void copied(int done,int total,String path)throws Exception;}
    public static final String COMPLETE="pikonest-copy-complete.txt";
    private static void check(BooleanSupplier cancelled){if(cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();}
    public static void path(String p)throws IOException{
        if(p==null||p.isEmpty()||p.length()>1024||p.startsWith("/")||p.contains("\\")||p.contains(":"))throw new IOException("Unsafe copy path");
        for(String bit:p.split("/",-1)){
            if(bit.isEmpty()||bit.equals(".")||bit.equals(".."))throw new IOException("Unsafe copy path");
            for(char c:bit.toCharArray())if(c<32||c==127)throw new IOException("Unsafe copy path");
        }
    }
    private static String hash(InputStream input,BooleanSupplier cancelled)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] block=new byte[32768];
        try(InputStream in=input){if(in==null)throw new IOException("Cannot read copy");for(int n;(n=in.read(block))!=-1;){check(cancelled);digest.update(block,0,n);}}
        StringBuilder value=new StringBuilder();for(byte b:digest.digest())value.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return value.toString();
    }
    public static void copy(Source source,Target target,BooleanSupplier cancelled,Progress progress)throws Exception{
        List<String> names=new ArrayList<>(source.files());Collections.sort(names);Set<String> seen=new HashSet<>();
        StringBuilder manifest=new StringBuilder("PikoNest verified copy v1\n");
        for(String name:names){path(name);if(name.equalsIgnoreCase(COMPLETE)||!seen.add(name.toLowerCase(java.util.Locale.ROOT)))throw new IOException("Ambiguous copy contents");}
        int done=0;
        for(String name:names){
            check(cancelled);String before=hash(source.open(name),cancelled);
            if(!target.exists(name)){
                if(target.exists(COMPLETE))throw new IOException("Completed copy changed. Start a new copy.");
                try(InputStream in=source.open(name);OutputStream out=target.create(name)){
                    if(in==null||out==null)throw new IOException("Cannot open copy streams");
                    byte[] block=new byte[32768];for(int n;(n=in.read(block))!=-1;){check(cancelled);out.write(block,0,n);}
                }
            }
            String actual=hash(target.read(name),cancelled);
            if(!before.equals(actual)||!before.equals(hash(source.open(name),cancelled)))throw new IOException("Copy differs or source changed: "+name+". Files kept; start a new copy.");
            manifest.append(before).append("  ").append(name).append('\n');progress.copied(++done,names.size(),name);
        }
        StringBuilder finalManifest=new StringBuilder("PikoNest verified copy v1\n");
        for(String name:names){
            String latest=hash(source.open(name),cancelled);
            if(!latest.equals(hash(target.read(name),cancelled)))throw new IOException("Source or copy changed: "+name+". Start a new copy.");
            finalManifest.append(latest).append("  ").append(name).append('\n');
        }
        if(!manifest.toString().equals(finalManifest.toString())||!names.equals(sorted(source.files())))throw new IOException("Source changed. Start a new copy.");
        byte[] receipt=manifest.toString().getBytes("UTF-8");check(cancelled);
        if(!target.exists(COMPLETE))try(OutputStream out=target.create(COMPLETE)){out.write(receipt);}
        if(!hash(new ByteArrayInputStream(receipt),cancelled).equals(hash(target.read(COMPLETE),cancelled)))throw new IOException("Copy receipt could not be verified");
    }
    private static List<String> sorted(List<String> names){List<String> result=new ArrayList<>(names);Collections.sort(result);return result;}
}
