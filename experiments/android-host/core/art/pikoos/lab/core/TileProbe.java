package art.pikoos.lab.core;

import java.util.regex.Pattern;

/** A visible ordinary-Lua recipe, not a runtime component or physics engine. */
public final class TileProbe {
    private TileProbe(){}
    public static String code(String name,String flag,String width,String height,String outside){
        return "function "+name+"(x,y)\n"
            +" x=flr(x/8) y=flr(y/8)\n"
            +" if x<0 or y<0 or\n"
            +"  x>="+width+" or y>="+height+" then\n"
            +"  return "+outside+"\n"
            +" end\n"
            +" return fget(mget(x,y),"+flag+")\n"
            +"end\n";
    }
    private static final String RESERVED=" _init _update _update60 _draw _env _ENV _G "
        +"abs add all assert atan2 band bnot bor btn btnp bxor camera cartdata ceil chr circ circfill clip cls cocreate coresume costatus cos cstore count cwrap dget dset del deli draw_map export extcmd fget fillp flip flr foreach fset getmetatable ipairs line load log lshr map max memcpy memset menuitem mget mid min mset music next ord oval ovalfill pack pairs pal palt peek peek2 peek4 pget poke poke2 poke4 print printh pset rawequal rawget rawlen rawset rect rectfill reload reset rnd rotl rotr run save select setmetatable sfx sget sgn shl shr sin split spr sqrt srand sset sspr stat stop sub t time tline tonum tostr trace type unpack yield ";
    private static boolean used(String mask,String name){return Pattern.compile("(?<![A-Za-z0-9_])"+Pattern.quote(name)+"(?![A-Za-z0-9_])").matcher(mask).find();}
    public static void validateSource(String source,String name){
        validateSource(source,name,false);
    }
    public static void validateSource(String source,String name,boolean area){
        validateAvailableName(source,name);
        LuaContext context=new LuaContext(source);
        LuaSymbols symbols=new LuaSymbols(source);
        for(String api:area?new String[]{"flr","mget","fget","ceil","min"}:new String[]{"flr","mget","fget"})if(symbols.shadows(api)||context.defines(api))
            throw new IllegalArgumentException("Имя API "+api+" переопределено. Вставка не применена.");
    }
    static void validateAvailableName(String source,String name){
        LuaContext context=new LuaContext(source);String mask=context.masked();
        if(RESERVED.contains(" "+name+" "))throw new IllegalArgumentException("Выбери своё имя: "+name+" используется Lua/PICO-8.");
        if(used(mask,name))throw new IllegalArgumentException("Имя "+name+" уже встречается в коде. Выбери другое; исходник сохранён.");
        if(Pattern.compile("(?m)^\\s*#include\\b").matcher(mask).find()||used(mask,"_ENV")||used(mask,"_G"))
            throw new IllegalArgumentException("Для includes/изменённого окружения проверка имён пока недоступна. Исходник сохранён.");
    }
    /** Half-open rectangle [x,x+w) x [y,y+h), including every overlapped tile. */
    public static String areaCode(String name,String flag,String width,String height,String outside){
        int mx=Integer.parseInt(width)*8,my=Integer.parseInt(height)*8;
        String empty=" if w<=0 or h<=0 then\n  return false\n end\n";
        String code="function "+name+"(x,y,w,h)\n"+empty;
        if(outside.equals("true")){
            code+=" if x<0 or y<0 or\n  x>="+mx+" or y>="+my+" then\n  return true\n end\n"
                +" if w>"+mx+"-x or h>"+my+"-y then\n  return true\n end\n";
        }else{
            code+=" if x>="+mx+" or y>="+my+" then\n  return false\n end\n"
                +" if x<0 then w+=x x=0 end\n if y<0 then h+=y y=0 end\n"+empty
                +" w=min(w,"+mx+"-x)\n h=min(h,"+my+"-y)\n";
        }
        // Clip before adding to avoid 16.16 overflow; ceil is applied before /8
        // so even a one-unit (0x0.0001) overlap survives fixed-point division.
        return code+" local r=ceil(x+w)-1\n local b=ceil(y+h)-1\n"
            +" for j=flr(y/8),flr(b/8) do\n  for i=flr(x/8),flr(r/8) do\n"
            +"   if fget(mget(i,j),"+flag+") then\n    return true\n   end\n  end\n end\n return false\nend\n";
    }
}
