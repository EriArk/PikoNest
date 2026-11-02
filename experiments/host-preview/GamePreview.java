package art.pikoos.runtimelab;
import art.pikoos.lab.core.*;
import java.nio.file.*;import java.util.*;import javax.imageio.*;import java.awt.image.*;import android.graphics.*;
public class GamePreview {
 public static void main(String[] args)throws Exception{
  Paint.font=java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT,new java.io.File("experiments/android-host/assets/Monocraft.ttf"));
  LibrarySession shelf=LibrarySession.curated(new LibrarySession.Port(){public List<String> ids(){return new ArrayList<>();}public byte[] read(String id){return null;}public void create(String id,byte[] b){}public void open(String id,WorkshopCartridge c){}public void resume(){}},Files.readAllBytes(Paths.get("experiments/android-host/assets/moon-garden.p8")),Files.readAllBytes(Paths.get("experiments/android-host/assets/blank.p8")),Files.readAllBytes(Paths.get("experiments/android-host/assets/lights.p8")));
  for(String mode:new String[]{"explorer","puzzle","blank","explorer-game","puzzle-game","guide","guide-world"})for(int[] size:new int[][]{{360,480},{620,540},{360,800},{960,540}}){
   BufferedImage image=new BufferedImage(size[0]*2,size[1]*2,BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D g=image.createGraphics();g.scale(2,2);Canvas canvas=new Canvas(g);
   if(mode.equals("explorer")||mode.equals("puzzle")||mode.equals("blank")){shelf.mode=LibrarySession.Mode.CREATE;shelf.templateChoice=mode.equals("blank")?0:mode.equals("explorer")?1:2;android.view.View.width=size[0];android.view.View.height=size[1];new LibraryView(null,shelf,"",false,()->{},()->{},()->{}).onDraw(canvas);}
   else{WorkshopSession session=new WorkshopSession(StarterGames.create(mode.startsWith("puzzle")),new WorkshopSession.Port(){public void save(byte[] b){}public void launch(byte[] b){}});session.openGame();if(mode.startsWith("guide")){session.game.act(WorkshopSession.Action.CONTEXT);session.game.focus=mode.equals("guide-world")?5:3;}new GameSurface(new WorkshopView(),session,"Little explorer").draw(canvas,2,size[0],size[1]);}
   g.dispose();ImageIO.write(image,"png",new java.io.File(args[0]+"/"+mode+"-"+size[0]+"x"+size[1]+".png"));
  }
 }
}
