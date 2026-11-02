package art.pikoos.runtimelab;
import art.pikoos.lab.core.*;import android.graphics.*;import java.awt.image.*;import javax.imageio.*;
public class SetupPreview {
 public static void main(String[] args)throws Exception{
  Paint.font=java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT,new java.io.File("experiments/android-host/assets/Monocraft.ttf"));
  for(String mode:new String[]{"runtime","creations","backup","restore-review","folders"})for(int[] size:new int[][]{{360,480},{620,540},{960,540}}){
   android.view.View.width=size[0];android.view.View.height=size[1];BufferedImage image=new BufferedImage(size[0]*2,size[1]*2,BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D g=image.createGraphics();g.scale(2,2);Canvas c=new Canvas(g);
   if(mode.equals("runtime")){RuntimeSetup s=new RuntimeSetup(new RuntimeSetup.Port(){public void pick(){}public void recheck(){}public void cancel(){}public void leave(){}});s.arm64=true;s.adapterPresent=true;new RuntimeSetupView(new RuntimeSetupActivity(),s,false).onDraw(c);}
   else if(mode.equals("creations")){FirstRunSetup s=new FirstRunSetup(new FirstRunSetup.Port(){public void save(int n,boolean b){}public void games(){}public void creations(){}public void runtime(){}public void leave(){}},1);new FirstRunView(new FirstRunActivity(),s,false).onDraw(c);}
   else if(mode.equals("folders")){FolderSetup s=new FolderSetup(new FolderSetup.Port(){public void pick(FolderSetup.Role r){}public void check(FolderSetup.Role r,String s){}public void save(FolderSetup.Role r,String s,String n){}public void leave(){}},new String[]{"uri","","",""},new String[]{"My cartridges","","",""});s.entry(0).state=FolderSetup.State.READY;new FolderView(new android.app.Activity(),s,false,()->{},()->{}).onDraw(c);}
   else{BackupView v=new BackupView(new BackupActivity(),a->{});v.message="Back up projects, reusable assets, unfinished edits, workshop settings, saved games and downloaded carts. The purchased runtime and Android folder permissions are not included.";if(mode.equals("restore-review")){v.message="Verified backup. Restore replaces active work with an internal copy. Current private data is retained for recovery; external folders stay unchanged. PikoNest closes for restoration; reopen it, then use Finish and open it once more to load restored settings.";v.options=new String[]{"Restore and close","Keep current work","Back"};}v.onDraw(c);}
   g.dispose();ImageIO.write(image,"png",new java.io.File(args[0]+"/"+mode+"-"+size[0]+"x"+size[1]+".png"));
  }
 }
}
