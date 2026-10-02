package art.pikoos.lab.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Optional adapter for the owned platformer. Never a requirement of a cartridge. */
final class MoonGardenBinding {
    final HeroCode hero;
    final int[] fields=new int[2];
    private MoonGardenBinding(String code){
        String[] names={"speed","jump"};
        for(int i=0;i<names.length;i++){
            Matcher m=Pattern.compile("(?m)^-- pikoos-"+names[i]+"\\r?\\n"+names[i]+"=([1-4])(?=\\r?$)").matcher(code);
            if(!m.find())throw new IllegalArgumentException("Missing template field");
            fields[i]=m.start(1);
            if(m.find())throw new IllegalArgumentException("Ambiguous template field");
        }
        hero=new HeroCode(code);
    }
    static MoonGardenBinding detect(String code){
        if(!code.startsWith("-- moon garden / pikoos\n")&&!code.startsWith("-- moon garden / pikoos\r\n"))return null;
        try{return new MoonGardenBinding(code);}catch(IllegalArgumentException unsupported){return null;}
    }
}
