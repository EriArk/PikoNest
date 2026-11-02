package art.pikoos.runtimelab;
import art.pikoos.lab.core.*;import android.graphics.*;import java.awt.image.*;import javax.imageio.*;import java.util.*;
public class PlayPreview {
 public static void main(String[] args)throws Exception{
  Paint.font=java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT,new java.io.File("experiments/android-host/assets/Monocraft.ttf"));
  for(String mode:new String[]{"play-splore","play-empty","play-downloads"})for(int[] size:new int[][]{{360,480},{620,540},{960,540}}){
   android.view.View.width=size[0];android.view.View.height=size[1];BufferedImage image=new BufferedImage(size[0]*2,size[1]*2,BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D g=image.createGraphics();g.scale(2,2);
   PlaySession s=new PlaySession(new PlaySession.Port(){public void launch(PlaySession.Game a){}public void refresh(){}public void workshop(){}public void folders(){}public void favorite(PlaySession.Game a,boolean b){}public void splore(){}});
   if(mode.equals("play-splore")){s.menu.show();s.menu.selected=4;}
   if(mode.equals("play-downloads")){byte[] bytes=java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("experiments/android-host/tests/fixtures/lights-0.2.7.p8.png"));PlayCartridge cart=new PlayCartridge("lights.p8.png",bytes);s.replace(Arrays.asList(new PlaySession.Game("splore:data/bbs/carts/lights.p8.png","Lights",cart.problem,cart.cover,true,0,CartridgeFormat.P8_PNG,"Splore downloads")),"");s.notice="1 Splore download on your shelf";}
   new PlayView(null,s,false,()->{}).onDraw(new Canvas(g));g.dispose();ImageIO.write(image,"png",new java.io.File(args[0]+"/"+mode+"-"+size[0]+"x"+size[1]+".png"));
  }
 }
}
