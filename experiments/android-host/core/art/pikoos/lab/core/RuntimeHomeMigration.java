package art.pikoos.lab.core;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Verified copy/merge of ordinary runtime user files. Limits are import limits, not console limits. */
public final class RuntimeHomeMigration {
    private RuntimeHomeMigration(){}
    public static final long MAX_BYTES=512L*1024*1024;
    public static final int MAX_FILES=20000;
    public static final class Entry {
        public final long bytes;public final String hash;
        public Entry(long bytes,String hash){this.bytes=bytes;this.hash=hash;}
        public boolean equals(Object o){return o instanceof Entry&&bytes==((Entry)o).bytes&&hash.equals(((Entry)o).hash);}
        public int hashCode(){return hash.hashCode();}
    }
    public static final class Plan {
        public final Map<String,Entry> current,source;
        public final List<String> added=new ArrayList<>(),same=new ArrayList<>(),conflicts=new ArrayList<>();
        public final long sourceBytes;
        public Plan(Map<String,Entry> current,Map<String,Entry> source){
            this.current=Collections.unmodifiableMap(new TreeMap<>(current));this.source=Collections.unmodifiableMap(new TreeMap<>(source));
            long bytes=0;for(Map.Entry<String,Entry> row:source.entrySet()){
                String path="data/"+row.getKey();Entry existing=current.get(path);bytes+=row.getValue().bytes;
                if(existing==null)added.add(row.getKey());else if(existing.equals(row.getValue()))same.add(row.getKey());else conflicts.add(row.getKey());
            }sourceBytes=bytes;
            Set<String> paths=new HashSet<>(current.keySet());for(String key:source.keySet())paths.add("data/"+key);
            Set<String> folded=new HashSet<>();for(String path:paths)if(!folded.add(path.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Ambiguous merged file names: "+path);
            for(String path:paths){String parent=path;while(parent.contains("/")){parent=parent.substring(0,parent.lastIndexOf('/'));if(paths.contains(parent))throw new IllegalArgumentException("File/folder conflict: "+parent);}}
        }
    }
    public static String path(String name){
        if(name==null||name.isEmpty()||name.length()>512||name.startsWith("/")||name.contains("\\")||name.indexOf(':')>=0)throw new IllegalArgumentException("Unsafe import path");
        for(String part:name.split("/",-1))if(part.isEmpty()||part.equals(".")||part.equals("..")||part.matches(".*[\\p{Cntrl}].*"))throw new IllegalArgumentException("Unsafe import path");
        return name;
    }
    public static Map<String,Entry> scan(File root,BooleanSupplier cancelled)throws IOException{
        Map<String,Entry> found=new TreeMap<>();Set<String> folded=new HashSet<>();long[] bytes={0};
        if(!root.isDirectory()||Files.isSymbolicLink(root.toPath()))throw new IOException("Runtime data folder is unavailable");
        walk(root,root,found,folded,bytes,0,cancelled);return found;
    }
    private static void walk(File root,File dir,Map<String,Entry> found,Set<String> folded,long[] bytes,int depth,BooleanSupplier cancelled)throws IOException{
        RuntimeArchive.check(cancelled);if(depth>32)throw new IOException("Import folder is too deep");
        File[] files=dir.listFiles();if(files==null)throw new IOException("Cannot read runtime data folder");Arrays.sort(files,Comparator.comparing(File::getName));
        for(File file:files){
            String relative=path(root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar,'/'));
            if(!folded.add(relative.toLowerCase(Locale.ROOT)))throw new IOException("Ambiguous file names: "+relative);
            if(Files.isSymbolicLink(file.toPath()))throw new IOException("Symbolic links require manual review: "+relative);
            if(file.isDirectory())walk(root,file,found,folded,bytes,depth+1,cancelled);
            else if(file.isFile()){
                if(found.size()>=MAX_FILES||file.length()>MAX_BYTES||bytes[0]+file.length()>MAX_BYTES)throw new IOException("Import limit: 512 MiB / 20,000 files");
                Entry e=fingerprint(file,cancelled);bytes[0]+=e.bytes;found.put(relative,e);
            }else throw new IOException("Unsupported runtime data entry: "+relative);
        }
    }
    public static Entry fingerprint(File file,BooleanSupplier cancelled)throws IOException{
        try{MessageDigest digest=MessageDigest.getInstance("SHA-256");long count=0;
            try(InputStream in=new FileInputStream(file)){byte[] b=new byte[65536];for(int n;(n=in.read(b))!=-1;){RuntimeArchive.check(cancelled);count+=n;if(count>MAX_BYTES)throw new IOException("Import file is too large");digest.update(b,0,n);}}
            StringBuilder hash=new StringBuilder();for(byte b:digest.digest())hash.append(String.format(Locale.ROOT,"%02x",b&255));return new Entry(count,hash.toString());
        }catch(NoSuchAlgorithmException e){throw new IOException(e);}
    }
    public static void unchanged(File root,Map<String,Entry> expected,BooleanSupplier cancelled)throws IOException{
        if(!scan(root,cancelled).equals(expected))throw new IOException("Runtime data changed. Review a fresh copy before importing.");
    }
    public static void prepare(File current,File source,File candidate,Plan plan,boolean importedWins,BooleanSupplier cancelled)throws IOException{
        unchanged(current,plan.current,cancelled);unchanged(source,plan.source,cancelled);
        if(candidate.exists()||!candidate.mkdir())throw new IOException("Cannot create a new runtime home");
        Map<String,Entry> expected=new TreeMap<>(plan.current);
        for(Map.Entry<String,Entry> row:plan.current.entrySet())copy(current,candidate,row.getKey(),row.getKey(),row.getValue(),cancelled);
        for(Map.Entry<String,Entry> row:plan.source.entrySet()){
            String target="data/"+row.getKey();if(!expected.containsKey(target)||importedWins){copy(source,candidate,row.getKey(),target,row.getValue(),cancelled);expected.put(target,row.getValue());}
        }
        if(!scan(candidate,cancelled).equals(expected))throw new IOException("Runtime copy verification failed");
        unchanged(current,plan.current,cancelled);unchanged(source,plan.source,cancelled);
    }
    private static void copy(File root,File target,String from,String to,Entry expected,BooleanSupplier cancelled)throws IOException{
        File out=new File(target,path(to)),parent=out.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Cannot create imported folder");
        try(InputStream in=new FileInputStream(new File(root,path(from)));FileOutputStream stream=new FileOutputStream(out)){byte[] b=new byte[65536];for(int n;(n=in.read(b))!=-1;){RuntimeArchive.check(cancelled);stream.write(b,0,n);}stream.getFD().sync();}
        if(!fingerprint(out,cancelled).equals(expected))throw new IOException("Source changed while copying: "+from);
    }
}
