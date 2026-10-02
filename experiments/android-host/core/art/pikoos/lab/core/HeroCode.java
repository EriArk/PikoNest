package art.pikoos.lab.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Narrow adapter for the owned Moon Garden Lua. This is not a general Lua transformer. */
final class HeroCode {
    static final String DRAW=" sspr(hero_sx,hero_sy,hero_sw,hero_sh,x-hero_left,y-hero_top)";
    static final String OLD_INIT="function _init()\n x=46 y=85 vy=0 grounded=true\n platforms={{0,101,128},{9,73,26},\n  {57,62,26},{96,81,24}}\nend";
    static final String OLD_UPDATE="function _update60()\n if btn(0) then x-=speed end\n if btn(1) then x+=speed end\n x=mid(0,x,112)\n if (btnp(4) or btnp(5)) and grounded then\n  vy=-(jump+1) grounded=false\n end\n local old_y=y\n vy=min(vy+0.18,4)\n y+=vy grounded=false\n for p in all(platforms) do\n  if vy>=0 and old_y+16<=p[2]\n   and y+16>=p[2] and x+12>p[1]\n   and x+4<p[1]+p[3] then\n   y=p[2]-16 vy=0 grounded=true\n  end\n end\n if y>128 then x=46 y=85 vy=0 end\nend";
    static final String SPAWN="x=mid(0,flr(54-hero_w/2),128-hero_w) y=101-hero_h vy=0";
    static final String NEW_INIT=OLD_INIT.replace("x=46 y=85 vy=0",SPAWN);
    static final String NEW_UPDATE=OLD_UPDATE.replace("x=mid(0,x,112)","x=mid(0,x,128-hero_w)")
        .replace("old_y+16","old_y+hero_h").replace("y+16","y+hero_h").replace("x+12","x+hero_w")
        .replace("x+4<p[1]+p[3]","x<p[1]+p[3]").replace("y=p[2]-16","y=p[2]-hero_h")
        .replace("x=46 y=85 vy=0",SPAWN);
    private static final String[] NAMES={"sx","sy","sw","sh","left","top","w","h"};
    final HeroBinding binding;
    final boolean legacy;
    private final int numberFrom,numberTo;
    private final String draw,data;
    HeroCode(String code){
        Matcher old=Pattern.compile("(?m)^ spr\\(([0-9]{1,2}),x,y,2,2\\)(?=\\r?$)").matcher(code);
        if(old.find()){
            int n=Integer.parseInt(old.group(1));numberFrom=old.start(1);numberTo=old.end(1);draw=old.group();
            if(n%2!=0||n>=16||old.find()||code.contains("-- pikoos-hero\n")||code.contains("-- pikoos-hero\r\n"))throw unsupported();
            binding=HeroBinding.legacy(n/2);legacy=true;data=null;return;
        }
        StringBuilder regex=new StringBuilder("(?m)^-- pikoos-hero\\r?\\n");
        for(String name:NAMES)regex.append("hero_").append(name).append("=([0-9]{1,3})\\r?\\n");
        regex.append("-- /pikoos-hero(?=\\r?$)");
        Matcher m=Pattern.compile(regex.toString()).matcher(code);
        if(!m.find())throw unsupported();
        int[] v=new int[8];for(int i=0;i<8;i++)v[i]=Integer.parseInt(m.group(i+1));
        data=m.group();if(m.find())throw unsupported();
        binding=new HeroBinding(new SpriteRegion(v[0],v[1],v[2],v[3]),v[4],v[5],v[6],v[7]);
        unique(code,DRAW);legacy=false;draw=DRAW;numberFrom=numberTo=-1;
    }
    String withLegacyCard(String code,int slot){
        if(!legacy)throw unsupported();
        return code.substring(0,numberFrom)+(slot*2)+code.substring(numberTo);
    }
    private static IllegalArgumentException unsupported(){return new IllegalArgumentException("Код героя изменён вручную. Привязка не заменяет неизвестную логику.");}
    private static void unique(String code,String fragment){int at=code.indexOf(fragment);if(at<0||code.indexOf(fragment,at+1)>=0)throw unsupported();}
    private static String replace(String code,String old,String next){unique(code,old);return code.replace(old,next);}
    private static String ending(String block,String eol){return block.replace("\n",eol);}
    private static String data(HeroBinding b,String eol){
        int[] values={b.image.x,b.image.y,b.image.width,b.image.height,b.left,b.top,b.width,b.height};
        StringBuilder out=new StringBuilder("-- pikoos-hero"+eol);
        for(int i=0;i<8;i++)out.append("hero_").append(NAMES[i]).append('=').append(values[i]).append(eol);
        return out.append("-- /pikoos-hero").toString();
    }
    String withBinding(String code,HeroBinding next){
        // Refuse partially modified motion/collision functions before any byte is saved.
        String eol=code.contains("\r\n")?"\r\n":"\n";
        String init=ending(legacy?OLD_INIT:NEW_INIT,eol),update=ending(legacy?OLD_UPDATE:NEW_UPDATE,eol);
        unique(code,init);unique(code,update);unique(code,draw);
        for(String name:new String[]{"_init","_update60"}){
            Matcher functions=Pattern.compile("(?m)^function "+name+"\\(").matcher(code);
            if(!functions.find()||functions.find())throw unsupported();
        }
        if(!legacy)return replace(code,data,data(next,eol));
        if(Pattern.compile("\\bhero_(sx|sy|sw|sh|left|top|w|h)\\b").matcher(code).find())throw unsupported();
        String changed=replace(code,draw,DRAW);
        changed=replace(changed,update,ending(NEW_UPDATE,eol));
        return replace(changed,init,data(next,eol)+eol+eol+ending(NEW_INIT,eol));
    }
}
