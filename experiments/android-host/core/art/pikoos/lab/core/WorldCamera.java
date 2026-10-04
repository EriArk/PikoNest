package art.pikoos.lab.core;

import java.util.regex.*;

/** Ordinary camera calls: world origin (0,0), a 128px view, no object/runtime dependency. */
public final class WorldCamera {
    private WorldCamera(){}
    public static String code(String x,String y,String width,String height){
        return "camera("+axis(x,width)+","+axis(y,height)+")\n";
    }
    private static String axis(String target,String cells){
        // Clamp before subtracting: -32768 must not wrap to the far world edge.
        // A world smaller than the display stays at its origin.
        return "mid(64,("+target+"),max(64,"+cells+"*8-64))-64";
    }
    public static void validateSource(String source,boolean follow){
        LuaContext context=new LuaContext(source);String mask=context.masked();
        if(Pattern.compile("(?m)^\\s*#include\\b|(?<![A-Za-z0-9_])(?:_ENV|_G)(?![A-Za-z0-9_])").matcher(mask).find())
            throw new IllegalArgumentException("Для includes/изменённого окружения камера пока проверяется вручную. Код сохранён.");
        LuaSymbols symbols=new LuaSymbols(source);
        for(String name:follow?new String[]{"camera","mid","max"}:new String[]{"camera"})
            if(symbols.shadows(name)||context.defines(name))throw new IllegalArgumentException("Имя API "+name+" переопределено. Камера не изменена.");
    }
    public static void validateForm(LuaInsert form){
        if(form.item().id.equals("camera_reset"))return;
        // Require each input to remain one camera argument. Full Lua semantics are
        // checked by the official runtime, as with the other expression fields.
        for(int n=0;n<2;n++){
            String call="camera(("+form.value(n)+"),0)";
            LuaCall parsed=LuaCall.parse(call,0,call.length());
            if(!parsed.form.item().id.equals("camera")||!parsed.form.value(0).equals("("+form.value(n)+")"))
                throw new IllegalArgumentException("Выражение камеры вышло за границы поля");
        }
    }
    /** Recognize only our exact pair of expressions; edited/custom calls remain raw X/Y forms. */
    static LuaCall follow(String source,String raw,int start,int end,int[] from,int[] to){
        Pattern p=Pattern.compile("mid\\(64,\\((.*)\\),max\\(64,([0-9]+)\\*8-64\\)\\)-64");
        Matcher x=p.matcher(raw.substring(from[0],to[0])),y=p.matcher(raw.substring(from[1],to[1]));
        if(!x.matches()||!y.matches())return null;
        LuaInsert form=new LuaInsert();for(int n=0;n<LuaInsert.ITEMS.length;n++)if(LuaInsert.ITEMS[n].id.equals("camera_follow"))form.choose(n);
        form.editing=true;form.screen=LuaInsert.Screen.FIELDS;
        try{form.set(0,x.group(1));form.set(1,y.group(1));form.set(2,x.group(2));form.set(3,y.group(2));}
        catch(IllegalArgumentException e){return null;}
        validateSource(source,true);
        // LuaCall's spans must be ordered by source, while the friendly form orders X,Y,width,height.
        // Use a mapping so unchanged formatting and comments survive parameter edits.
        return new LuaCall(start,end,raw,"camera_follow",
            new int[]{from[0]+x.start(1),from[0]+x.start(2),from[1]+y.start(1),from[1]+y.start(2)},
            new int[]{from[0]+x.end(1),from[0]+x.end(2),from[1]+y.end(1),from[1]+y.end(2)},form,new int[]{0,2,1,3});
    }
}
