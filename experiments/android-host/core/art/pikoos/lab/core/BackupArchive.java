package art.pikoos.lab.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.zip.*;

/** Portable, bounded user-data archive. Does not capture installed executables or Android grants. */
public final class BackupArchive {
    public static final long MAX_BYTES=512L*1024*1024;
    public static final int MAX_FILES=20000;
    public static final String MANIFEST="pikonest-backup-v1.txt";
    private static final String MAGIC="PikoNest user backup 1\n";
    public static final class Report {
        public final int files;public final long bytes;
        Report(int f,long b){files=f;bytes=b;}
    }
    private static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(Exception e){throw new IllegalStateException(e);}}
    private static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    public static void path(String name)throws IOException{
        VerifiedTransfer.path(name);
        if(name.length()>512||!(name.startsWith("projects/")||name.startsWith("library/")||name.startsWith("runtime/")||name.startsWith("preferences/")||name.startsWith("recovery/")))throw new IOException("Unknown backup section");
    }
    private static void paths(List<String> names)throws IOException{
        if(names.size()>MAX_FILES)throw new IOException("Backup limit: 20,000 files");
        Set<String> found=new HashSet<>();for(String n:names){path(n);if(!found.add(n.toLowerCase(Locale.ROOT)))throw new IOException("Duplicate backup path");}
        for(String n:found){String p=n;while(p.contains("/")){p=p.substring(0,p.lastIndexOf('/'));if(found.contains(p))throw new IOException("Backup file/folder conflict");}}
    }
    private static String copy(InputStream input,OutputStream output,long[] total,BooleanSupplier cancelled)throws Exception{
        if(input==null)throw new IOException("Backup source unavailable");MessageDigest hash=sha();long bytes=0;
        byte[] buffer=new byte[65536];for(int n;(n=input.read(buffer))!=-1;){RuntimeArchive.check(cancelled);bytes+=n;total[0]+=n;if(total[0]>MAX_BYTES)throw new IOException("Backup limit: 512 MiB");hash.update(buffer,0,n);if(output!=null)output.write(buffer,0,n);}
        return hex(hash.digest())+" "+bytes;
    }
    public static Report write(VerifiedTransfer.Source source,File output,BooleanSupplier cancelled)throws Exception{
        if(!output.createNewFile())throw new IOException("Backup candidate already exists");
        List<String> names=new ArrayList<>(source.files());Collections.sort(names);paths(names);long[] total={0};Map<String,String> hashes=new LinkedHashMap<>();
        try(FileOutputStream raw=new FileOutputStream(output);ZipOutputStream zip=new ZipOutputStream(raw)){
            for(String name:names){RuntimeArchive.check(cancelled);zip.putNextEntry(new ZipEntry(name));try(InputStream in=source.open(name)){hashes.put(name,copy(in,zip,total,cancelled));}zip.closeEntry();}
            // Re-read the live source so a changing provider cannot produce a false success.
            long[] again={0};for(String name:names)try(InputStream in=source.open(name)){if(!hashes.get(name).equals(copy(in,null,again,cancelled)))throw new IOException("Data changed during backup. Retry; original data kept.");}
            List<String> latest=new ArrayList<>(source.files());Collections.sort(latest);if(!latest.equals(names))throw new IOException("Backup file list changed. Retry.");
            StringBuilder manifest=new StringBuilder(MAGIC);for(String name:names)manifest.append(hashes.get(name)).append(' ').append(name).append('\n');
            zip.putNextEntry(new ZipEntry(MANIFEST));zip.write(manifest.toString().getBytes(StandardCharsets.UTF_8));zip.closeEntry();zip.finish();zip.flush();raw.getFD().sync();
        }
        return verify(output,null,cancelled);
    }
    /** An extraction directory must be fresh. All members are verified before they may be activated. */
    public static Report verify(File file,File destination,BooleanSupplier cancelled)throws Exception{
        if(file.length()>MAX_BYTES+8L*1024*1024)throw new IOException("Backup ZIP exceeds the supported limit");
        if(destination!=null&&(destination.exists()||!destination.mkdirs()))throw new IOException("Restore needs a fresh staging folder");
        try(ZipFile zip=new ZipFile(file)){
            ZipEntry manifest=zip.getEntry(MANIFEST);if(manifest==null||manifest.getSize()<MAGIC.length()||manifest.getSize()>4*1024*1024)throw new IOException("Not a supported PikoNest backup");
            ByteArrayOutputStream text=new ByteArrayOutputStream();try(InputStream in=zip.getInputStream(manifest)){byte[] b=new byte[8192];for(int n;(n=in.read(b))!=-1;){RuntimeArchive.check(cancelled);if(text.size()+n>4*1024*1024)throw new IOException("Backup manifest too large");text.write(b,0,n);}}
            String description=new String(text.toByteArray(),StandardCharsets.UTF_8);if(!description.startsWith(MAGIC)||!description.endsWith("\n"))throw new IOException("Unknown backup version");
            Map<String,String> expected=new TreeMap<>();String rows=description.substring(MAGIC.length());
            if(!rows.isEmpty())for(String row:rows.split("\n")){
                String[] bits=row.split(" ",3);if(bits.length!=3||!bits[0].matches("[0-9a-f]{64}")||!bits[1].matches("0|[1-9][0-9]{0,9}"))throw new IOException("Invalid backup manifest");
                if(expected.put(bits[2],bits[0]+" "+bits[1])!=null)throw new IOException("Duplicate backup manifest path");
            }
            paths(new ArrayList<>(expected.keySet()));List<String> actual=new ArrayList<>();int manifests=0;
            for(Enumeration<? extends ZipEntry> it=zip.entries();it.hasMoreElements();){ZipEntry e=it.nextElement();if(e.getName().equals(MANIFEST)){manifests++;continue;}if(e.isDirectory())throw new IOException("Unexpected backup directory entry");actual.add(e.getName());if(actual.size()>MAX_FILES)throw new IOException("Too many backup files");}
            paths(actual);if(manifests!=1||!new TreeSet<>(actual).equals(expected.keySet()))throw new IOException("Backup contents differ from manifest");
            long[] total={0};for(String name:expected.keySet()){
                ZipEntry e=zip.getEntry(name);if(e.getSize()<0||e.getSize()>MAX_BYTES)throw new IOException("Invalid backup file size");
                File target=destination==null?null:new File(destination,name);
                if(target!=null&&!target.getParentFile().isDirectory()&&!target.getParentFile().mkdirs())throw new IOException("Cannot stage backup");
                String hash;try(InputStream in=zip.getInputStream(e);FileOutputStream out=target==null?null:new FileOutputStream(target)){hash=copy(in,out,total,cancelled);if(out!=null)out.getFD().sync();}
                if(!hash.equals(expected.get(name))||!hash.endsWith(" "+e.getSize()))throw new IOException("Backup damaged: "+name);
            }
            return new Report(expected.size(),total[0]);
        }catch(ZipException e){throw new IOException("Cannot read this backup ZIP",e);}
    }
    public static VerifiedTransfer.Source directory(File root){return new VerifiedTransfer.Source(){
        public List<String> files()throws Exception{return new ArrayList<>(RuntimeHomeMigration.scan(root,()->false).keySet());}
        public InputStream open(String name)throws Exception{path(name);File f=new File(root,name);if(!f.getCanonicalFile().equals(f.getAbsoluteFile()))throw new IOException("Linked backup path");return new FileInputStream(f);}
    };}
}
