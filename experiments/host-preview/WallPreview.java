package art.pikoos.runtimelab;
import art.pikoos.lab.core.*;
import art.pikoos.lab.core.WorkshopSession.Action;
import java.nio.file.*;
import javax.imageio.*;
import java.awt.image.*;
import android.graphics.*;
/** Production wall controls rendered on the host, not Android/runtime evidence. */
public final class WallPreview {
 public static void main(String[] args)throws Exception {
  Paint.font=java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT,new java.io.File("experiments/android-host/assets/Monocraft.ttf"));
  WorkshopCartridge cart=new WorkshopCartridge(Files.readAllBytes(Paths.get("docs/showcase/0.0.94-en/maze.p8")));
  for(String mode:new String[]{"wall-map","wall-rule","wall-options","wall-picker"})for(int[] size:new int[][]{{360,480},{620,540},{960,540}}){
   WorkshopSession s=new WorkshopSession(cart,new WorkshopSession.Port(){public void save(byte[] b){}public void launch(byte[] b){}});s.openGame();s.game.openWorld();WorldEditor e=s.game.world;
   e.point(4,2);e.brush.tile=e.tile(4,2);e.page=WorldEditor.Page.PAINT;
   if(mode.equals("wall-rule")){e.act(Action.MENU);e.focus=8;e.act(Action.CONFIRM);}
   if(mode.equals("wall-options")){e.act(Action.MENU);e.focus=8;}
   if(mode.equals("wall-picker")){e.act(Action.CONTEXT);}
   s.game.focus=e.focus;
   BufferedImage image=new BufferedImage(size[0]*2,size[1]*2,BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D g=image.createGraphics();g.scale(2,2);
   new GameSurface(new WorkshopView(),s,"Wall workshop").draw(new Canvas(g),2,size[0],size[1]);g.dispose();
   ImageIO.write(image,"png",new java.io.File(args[0]+"/"+mode+"-"+size[0]+"x"+size[1]+".png"));
  }
 }
}
