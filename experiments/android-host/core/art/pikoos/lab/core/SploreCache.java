package art.pikoos.lab.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Read-only index of the official runtime's persistent BBS cache. No network/scraping. */
public final class SploreCache {
    public static final String PREFIX="splore:";
    // A local cache is cheaper to enumerate than an arbitrary document provider.
    // 512 covers cap artwork memory at 32 MiB; cancellation still applies per file.
    public static final int MAX_GAMES=512;
    public static String path(String relative){
        if(relative==null||!relative.matches("data/bbs/(?:[A-Za-z0-9_-]+/)*[A-Za-z0-9_-]+\\.p8\\.png"))throw new IllegalArgumentException("Unsupported Splore cache path");
        return relative;
    }
    public static File resolve(File home,String relative)throws IOException{
        path(relative);File root=home.getCanonicalFile(),file=new File(root,relative);
        if(!file.getCanonicalFile().equals(file.getAbsoluteFile()))throw new IOException("Linked Splore cache entry");
        return file;
    }
    public static byte[] read(File home,String relative)throws Exception{
        try(InputStream in=new FileInputStream(resolve(home,relative))){return CartridgeImport.readBounded(in);}
    }
    public static final class Result {public final List<PlaySession.Game> games=new ArrayList<>();public boolean limited;public int unreadable;}
    public static Result list(File home,BooleanSupplier cancelled)throws Exception{
        Result result=new Result();File base=new File(home,"data/bbs");if(!base.exists())return result;
        File root=home.getCanonicalFile();
        GameTree.Result tree=GameTree.scan(directory->{
            File folder=new File(root,directory);if(!folder.getCanonicalFile().equals(folder.getAbsoluteFile()))throw new IOException("Linked cache folder");
            final DirectoryStream<Path> stream=Files.newDirectoryStream(folder.toPath());final Iterator<Path> iterator=stream.iterator();
            return new GameTree.Children(){public GameTree.Entry next()throws Exception{
                if(!iterator.hasNext())return null;Path p=iterator.next();String id=root.toPath().relativize(p).toString().replace('\\','/');
                if(Files.isSymbolicLink(p))return new GameTree.Entry(id,p.getFileName().toString()+".link",false);
                return new GameTree.Entry(id,p.getFileName().toString(),Files.isDirectory(p));
            }public void close()throws Exception{stream.close();}};
        },"data/bbs",cancelled,8192,MAX_GAMES);
        result.limited=tree.limited;result.unreadable=tree.unreadable;
        for(GameTree.Cart cart:tree.carts){
            GameTree.check(cancelled);if(!cart.name.endsWith(".p8.png"))continue;
            String relative=cart.id,title=cart.name.substring(0,cart.name.length()-7),problem="";int[] cover=null;
            try{path(relative);PlayCartridge inspected=new PlayCartridge(cart.name,read(home,relative));problem=inspected.problem;cover=inspected.cover;
                File file=resolve(home,relative);
                String label=title(new File(file.getParentFile(),title+".nfo"),title,false);
                if(label.isEmpty())label=title(new File(file.getParentFile(),"temp-"+title.replaceFirst("-[0-9]+$","")+".nfo"),title,true);
                if(!label.isEmpty())title=label;
            }catch(Exception e){problem="Downloaded cart is unavailable or unsupported. Reopen Splore or refresh.";}
            result.games.add(new PlaySession.Game(PREFIX+relative,title,problem,cover,false,0,CartridgeFormat.P8_PNG,"Splore downloads"));
        }
        GameTree.check(cancelled);return result;
    }
    private static String title(File nfo,String lid,boolean requireId)throws IOException{
        if(!nfo.isFile()||Files.isSymbolicLink(nfo.toPath())||nfo.length()>16384)return "";
        String foundId="",label="";
        for(String line:new String(Files.readAllBytes(nfo.toPath()),"UTF-8").split("\\r?\\n")){
            if(line.startsWith("lid:"))foundId=line.substring(4).trim();
            if(line.startsWith("title:"))label=line.substring(6).trim().replaceAll("\\p{Cntrl}"," ");
        }
        if((requireId||!foundId.isEmpty())&&!foundId.equals(lid))return "";
        return label.substring(0,Math.min(120,label.length()));
    }
}
