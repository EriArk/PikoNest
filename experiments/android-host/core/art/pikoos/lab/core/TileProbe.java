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
        LuaContext context=new LuaContext(source);String mask=context.masked();
        if(RESERVED.contains(" "+name+" "))throw new IllegalArgumentException("Выбери своё имя: "+name+" используется Lua/PICO-8.");
        if(used(mask,name))throw new IllegalArgumentException("Имя "+name+" уже встречается в коде. Выбери другое; исходник сохранён.");
        if(Pattern.compile("(?m)^\\s*#include\\b").matcher(mask).find()||used(mask,"_ENV")||used(mask,"_G"))
            throw new IllegalArgumentException("Для includes/изменённого окружения проверка имён пока недоступна. Исходник сохранён.");
        LuaSymbols symbols=new LuaSymbols(source);
        for(String api:new String[]{"flr","mget","fget"})if(symbols.shadows(api)||context.defines(api))
            throw new IllegalArgumentException("Имя API "+api+" переопределено. Вставка не применена.");
    }
}
