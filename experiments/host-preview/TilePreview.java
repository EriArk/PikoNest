package art.pikoos.runtimelab;
import art.pikoos.lab.core.*;import java.nio.file.*;import javax.imageio.*;import java.awt.image.*;import android.graphics.*;
public class TilePreview {
 public static void main(String[] args)throws Exception{
  Paint.font=java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT,new java.io.File("experiments/android-host/assets/Monocraft.ttf"));
  WorkshopCartridge cart=new WorkshopCartridge(Files.readAllBytes(Paths.get("docs/showcase/0.0.77-en/collection.p8")));
  GameElementNames names=GameElementNames.read(cart,new GameUses(PlayModel.read(cart).layout(cart),0));names.values.set(0,"Heart");names.values.set(1,"Star");names.values.set(2,"Spikes");cart=names.apply(cart);WorkshopSession s;
  PlayModel pm=PlayModel.read(cart);pm.world=new PlayWorld();pm.world.width=64;pm.world.height=32;pm.world.camera=1;pm.world.target=0;pm.elements.get(0).x=180;pm.elements.get(0).y=140;pm.elements.get(1).x=244;pm.elements.get(1).y=140;pm.elements.get(2).x=360;pm.elements.get(2).y=140;cart=names.apply(pm.write(cart));
  for(int y=0;y<8;y++)for(int x=0;x<8;x++)cart=cart.withPixel(new SpriteRegion(32,48,8,8),x,y,y<2?11:y==2?3:(x+y)%3==0?4:9);
  for(int x=0;x<64;x++)cart=cart.withTile(x,20,100);
  for(String mode:new String[]{"tile-draw","tile-palette","tile-menu","tile-review","tile-reuse","tile-actions","world-tiles"})for(int[] size:new int[][]{{620,540},{360,480},{360,800},{960,540}}){
   s=new WorkshopSession(cart,new WorkshopSession.Port(){public void save(byte[] b){}public void launch(byte[] b){}});s.openGame();
   if(mode.equals("world-place")){s.game.act(WorkshopSession.Action.DOWN);s.game.act(WorkshopSession.Action.CONFIRM);s.game.focus=1;s.game.act(WorkshopSession.Action.CONFIRM);s.game.point(230,140);}
   else if(!mode.equals("world-field")){s.game.openWorld();WorldEditor e=s.game.world;e.point(24,20);e.brush.tile=100;
    if(mode.equals("world-paint"))e.page=WorldEditor.Page.PAINT;
    if(mode.equals("world-tiles")){e.brush.begin();e.page=WorldEditor.Page.TILES;}
    if(mode.equals("world-camera")){e.page=WorldEditor.Page.CAMERA;e.focus=1;}
    if(mode.equals("world-target")){e.page=WorldEditor.Page.CAMERA;e.focus=1;e.act(WorkshopSession.Action.CONFIRM);}
    if(mode.equals("world-review")){e.paint();e.page=WorldEditor.Page.REVIEW;}
    if(mode.startsWith("tile-")){
     e.page=WorldEditor.Page.OPTIONS;e.focus=6;e.act(WorkshopSession.Action.CONFIRM);
     if(!mode.equals("tile-reuse")){
      e.focus=1;e.act(WorkshopSession.Action.CONFIRM);e.art.color=11;e.art.paint(3,3);e.art.paint(4,3);
      if(mode.equals("tile-palette")){e.art.page=TileArt.Page.PALETTE;e.focus=11;}
      if(mode.equals("tile-menu"))e.art.page=TileArt.Page.MENU;
      if(mode.equals("tile-review"))e.art.page=TileArt.Page.REVIEW;
      if(mode.equals("tile-actions")){e.art=null;e.page=WorldEditor.Page.TILE_ACTIONS;}
     }
    }
    s.game.focus=e.focus;
   }
   BufferedImage image=new BufferedImage(size[0]*2,size[1]*2,BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D g=image.createGraphics();g.scale(2,2);
   new GameSurface(new WorkshopView(),s,"Collection").draw(new Canvas(g),2,size[0],size[1]);g.dispose();
   ImageIO.write(image,"png",new java.io.File(args[0]+"/"+mode+"-"+size[0]+"x"+size[1]+".png"));
  }
 }
}