import art.pikoos.lab.core.*;
import java.util.*;

public final class MusicDataTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe edit accepted");}catch(IllegalArgumentException ex){checks++;}}
    public static void main(String[] args)throws Exception{
        for(String nl:new String[]{"\n","\r\n"}){
            WorkshopCartridge base=SfxDataTest.cart(nl,"");P8Music.Pattern empty=base.music().get(63);check(empty.line().equals("00 41424344"),"omitted music defaults");same(base.bytes(),base.withPattern(63,empty).bytes(),"no-op does not materialize section");
            P8Music.Pattern p=new P8Music.Pattern();p.flags=7;p.channels[0]=63;p.channels[1]=0;p.channels[2]=95;p.channels[3]=127;WorkshopCartridge cart=base.withPattern(63,p);check(cart.music().get(63).line().equals("07 3f005f7f"),"full boundary data");same(cart.bytes(),cart.withPattern(63,p).bytes(),"no-op exact");check(cart.code().equals(base.code()),"Lua untouched");
            String source="__music__"+nl+"00 0A4B4C4D"+nl+"02 0F0E0D0C"+nl+"__unknown__"+nl+"kept"+nl;WorkshopCartridge upper=SfxDataTest.cart(nl,source);p=upper.music().get(0);p.flags=1;String expected=new String(upper.bytes(),"UTF-8").replace("00 0A4B4C4D","01 0A4B4C4D");same(expected.getBytes("UTF-8"),upper.withPattern(0,p).bytes(),"targeted digit preserves uppercase and sections");
            p=upper.music().get(0);p.mute(0,true);check(p.channels[0]==74,"mute retains SFX 10");p.mute(0,false);check(p.channels[0]==10,"unmute restores same SFX");
            for(int sound=0;sound<64;sound++)for(int ch=0;ch<4;ch++){p=new P8Music.Pattern();p.channels[ch]=sound;p.flags=sound%8;P8Music.Pattern read=base.withPattern(0,p).music().get(0);check(read.sound(ch)==sound&&read.active(ch)&&read.flags==sound%8,"all SFX/channel/flag values");}
            WorkshopCartridge track=base;for(int n=0;n<4;n++){p=new P8Music.Pattern();p.channels[0]=8;p.flags=n==1?1:n==3?2:0;track=track.withPattern(n,p);}check(track.music().track(2).equals(Arrays.asList(2,3,1)),"loop reaches earlier start");
            p=track.music().get(3);p.flags=6;track=track.withPattern(3,p);check(track.music().track(2).equals(Arrays.asList(2,3)),"stop wins over repeat");
            p=track.music().get(3);p.flags=2;p.channels[3]=8;track=track.withPattern(3,p);final WorkshopCartridge four=track;refused(()->four.music().requireThreeParts(2));p.mute(3,true);track=track.withPattern(3,p);track.music().requireThreeParts(2);checks++;
            WorkshopCartridge advanced=SfxDataTest.cart(nl,"__music__"+nl+"80 41424344"+nl+"00 41424344"+nl);same(advanced.bytes(),advanced.withPattern(0,advanced.music().get(0)).bytes(),"advanced no-op kept");refused(()->advanced.withPattern(0,new P8Music.Pattern()));p=new P8Music.Pattern();p.flags=4;check(advanced.withPattern(1,p).music().get(0).flags==128,"supported row edit preserves advanced neighbor");
            refused(()->SfxDataTest.cart(nl,"__music__"+nl+"00 4142434"+nl).music());refused(()->SfxDataTest.cart(nl,"__music__"+nl+"__music__"+nl).music());
            StringBuilder tooMany=new StringBuilder("__music__"+nl);for(int n=0;n<65;n++)tooMany.append("00 41424344").append(nl);final String huge=tooMany.toString();refused(()->SfxDataTest.cart(nl,huge).music());
        }
        System.out.println("MusicDataTest: "+checks+" checks passed");
    }
}
