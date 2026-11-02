package art.pikoos.runtimelab;
import art.pikoos.lab.core.*;import java.nio.file.*;import javax.imageio.*;import java.awt.image.*;import android.graphics.*;
public class MusicPreview {
 public static void main(String[] args)throws Exception{
  Paint.font=java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT,new java.io.File("experiments/android-host/assets/Monocraft.ttf"));
  WorkshopCartridge cart=new WorkshopCartridge(Files.readAllBytes(Paths.get("docs/showcase/0.0.77-en/collection.p8")));
  GameElementNames names=GameElementNames.read(cart,new GameUses(PlayModel.read(cart).layout(cart),0));names.values.set(0,"Heart");names.values.set(1,"Star");names.values.set(2,"Spikes");cart=names.apply(cart);WorkshopSession s;
  for(String mode:new String[]{"music-bank","music-pattern","music-part","music-policy","music-review","music-note"})for(int[] size:new int[][]{{620,540},{360,480},{360,800},{960,540}}){
   s=new WorkshopSession(cart,new WorkshopSession.Port(){public void save(byte[] b){}public void launch(byte[] b){}});s.openGame();s.game.openMusic(false);MusicEditor m=s.game.music;
   if(!mode.equals("music-bank")){m.act(WorkshopSession.Action.CONFIRM);m.act(WorkshopSession.Action.CONFIRM);m.focus=2;m.act(WorkshopSession.Action.CONFIRM);for(int n=0;n<16;n++)m.sound.data.notes[n]=new int[]{24+n,n%8,5,0};
    if(mode.equals("music-note")){m.focus=4;m.act(WorkshopSession.Action.CONFIRM);}else{m.focus=35;m.act(WorkshopSession.Action.CONFIRM);m.focus=0;m.act(WorkshopSession.Action.CONFIRM);
     if(!mode.equals("music-part")){m.focus=5;m.act(WorkshopSession.Action.CONFIRM);if(mode.equals("music-policy")||mode.equals("music-review")){m.focus=9;m.act(WorkshopSession.Action.CONFIRM);if(mode.equals("music-review"))m.act(WorkshopSession.Action.CONFIRM);}}
    }s.game.focus=m.focus;
   }
   BufferedImage image=new BufferedImage(size[0]*2,size[1]*2,BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D g=image.createGraphics();g.scale(2,2);
   new GameSurface(new WorkshopView(),s,"Collection").draw(new Canvas(g),2,size[0],size[1]);g.dispose();
   ImageIO.write(image,"png",new java.io.File(args[0]+"/"+mode+"-"+size[0]+"x"+size[1]+".png"));
  }
 }
}