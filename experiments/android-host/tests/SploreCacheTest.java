import art.pikoos.lab.core.*;import java.nio.file.*;import java.io.*;import java.util.*;
public class SploreCacheTest {
 static int checks;static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Path home=Files.createTempDirectory("pikonest-splore-");try{
   check(SploreCache.list(home.toFile(),()->false).games.isEmpty(),"no downloads is valid before setup");
   byte[] png=Files.readAllBytes(Paths.get(args[0]));Path folder=Files.createDirectories(home.resolve("data/bbs/0"));
   Files.write(folder.resolve("42.p8.png"),png);Files.write(folder.resolve("42.nfo"),"title: My cached game\nauthor: Example\n".getBytes("UTF-8"));
   Files.write(folder.resolve("43.nfo"),"title: Metadata only".getBytes("UTF-8"));Files.write(folder.resolve("44.p8.png.part"),new byte[]{1});
   SploreCache.Result r=SploreCache.list(home.toFile(),()->false);check(r.games.size()==1,"only fully downloaded carts enter shelf, not metadata or partial files");
   PlaySession.Game g=r.games.get(0);check(g.title.equals("My cached game")&&g.cover.length==16384&&g.problem.isEmpty(),"title and cartridge artwork from local data");
   check(g.id.equals("splore:data/bbs/0/42.p8.png"),"stable ID survives reopen without an Android tree grant");
   check(Arrays.equals(png,SploreCache.read(home.toFile(),g.id.substring(SploreCache.PREFIX.length()))),"direct launch preserves exact cache bytes");
   check(SploreCache.list(home.toFile(),()->false).games.get(0).id.equals(g.id),"offline rescan retains favorite/recents identity");
   for(String p:new String[]{"../42.p8.png","data/bbs/../42.p8.png","data/bbs/a;echo.p8.png","data/bbs/a'b.p8.png","data/bbs//42.p8.png","data/bbs/a\\b.p8.png","data/carts/42.p8.png"}){try{SploreCache.path(p);throw new AssertionError("unsafe path accepted");}catch(IllegalArgumentException ok){checks++;}}
   Files.write(folder.resolve("44.p8.png"),new byte[]{1});r=SploreCache.list(home.toFile(),()->false);check(r.games.size()==2&&r.games.stream().anyMatch(x->!x.problem.isEmpty()),"bad cached file visible with retry problem; healthy download retained");
   try{SploreCache.list(home.toFile(),()->true);throw new AssertionError("cancel ignored");}catch(java.util.concurrent.CancellationException ok){checks++;}
   check(Arrays.equals(png,Files.readAllBytes(folder.resolve("42.p8.png"))),"index never rewrites downloaded originals");
   Files.write(folder.resolve("garden-0.p8.png"),png);
   Files.write(folder.resolve("temp-garden.nfo"),"lid:garden-0\nmid:garden\ntitle:Garden 1.0\nauthor:Example\n".getBytes("UTF-8"));
   r=SploreCache.list(home.toFile(),()->false);
   check(r.games.stream().anyMatch(x->x.id.endsWith("/garden-0.p8.png")&&x.title.equals("Garden 1.0")),"real Splore temp metadata binds through exact lid");
   Files.write(folder.resolve("temp-garden.nfo"),"lid:garden-1\ntitle:Wrong version\n".getBytes("UTF-8"));
   check(SploreCache.list(home.toFile(),()->false).games.stream().anyMatch(x->x.id.endsWith("/garden-0.p8.png")&&x.title.equals("garden-0")),"stale temp metadata cannot rename another revision");
   for(int i=0;i<140;i++)Files.write(folder.resolve("extra-"+i+".p8.png"),png);
   r=SploreCache.list(home.toFile(),()->false);
   check(r.games.size()==143&&!r.limited,"ordinary Splore browsing does not truncate at provider budget of 128");
  }finally{try(java.util.stream.Stream<Path> all=Files.walk(home)){for(Path p:(Iterable<Path>)all.sorted(Comparator.reverseOrder())::iterator)Files.delete(p);}}
  System.out.println("SploreCacheTest: "+checks+" checks passed");
 }
}
