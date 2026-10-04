package art.pikoos.lab.core;

/** Axis-separated swept AABB movement, emitted as ordinary editable PICO-8 Lua. */
public final class TileMotion {
    private TileMotion(){}
    public static void validateCall(String source,String name,String x,String y){
        if(x.equals(y)||name.equals(x)||name.equals(y))throw new IllegalArgumentException("Для функции, X и Y нужны разные имена");
        String arg="[A-Za-z_][A-Za-z_0-9]*",params=arg;for(int n=1;n<6;n++)params+="\\s*,\\s*"+arg;
        if(!java.util.regex.Pattern.compile("(?m)^\\s*function\\s+"+java.util.regex.Pattern.quote(name)+"\\s*\\(\\s*"+params+"\\s*\\)").matcher(new LuaContext(source).masked()).find())
            throw new IllegalArgumentException("Сначала добавь функцию движения "+name+" с шестью аргументами. Код не изменён.");
    }
    public static void validateSource(String source,String name){
        for(String n:new String[]{name,name+"_solid",name+"_axis"})TileProbe.validateSource(source,n,true);
        LuaSymbols symbols=new LuaSymbols(source);LuaContext context=new LuaContext(source);
        for(String api:new String[]{"abs","sgn","assert"})if(symbols.shadows(api)||context.defines(api))
            throw new IllegalArgumentException("Имя API "+api+" переопределено. Движение не вставлено.");
    }
    public static String code(String name,String flag,String width,String height,String outside){
        String solid=name+"_solid",axis=name+"_axis";
        return "local "+TileProbe.areaCode(solid,flag,width,height,outside)
            +"local function "+axis+"(x,y,w,h,d,v)\n"
            +" local function blocked(t)\n"
            +"  if v then\n   return "+solid+"(\n    x,min(y,y+t),w,h+abs(t))\n  end\n"
            +"  return "+solid+"(\n   min(x,x+t),y,w+abs(t),h)\n end\n"
            +" if not blocked(d) then\n  return d,false\n end\n"
            +" local lo,hi=0,abs(d)\n local dir=sgn(d)\n"
            +" for i=1,31 do\n  if hi-lo<=0x0.0001 then\n   break\n  end\n"
            +"  local mid=lo+(hi-lo)/2\n  if blocked(mid*dir) then\n   hi=mid\n  else\n   lo=mid\n  end\n end\n"
            +" return lo*dir,true\nend\n"
            +"function "+name+"(x,y,w,h,dx,dy)\n"
            +" if w<=0 or h<=0 then\n  return x,y,false,false\n end\n"
            +" assert(dx>-32768 and dy>-32768,\n  \"movement step range\")\n"
            +" assert(abs(dx)<=0x7fff.ffff-w\n  and abs(dy)<=0x7fff.ffff-h,\n  \"movement sweep range\")\n"
            +" local nx,ny=x+dx,y+dy\n"
            +" assert((dx>=0 and nx>=x or\n  dx<0 and nx<=x) and\n  (dy>=0 and ny>=y or\n  dy<0 and ny<=y),\n  \"movement position overflow\")\n"
            +" if "+solid+"(x,y,w,h) then\n  return x,y,true,true\n end\n"
            +" local mx,hx="+axis+"(\n  x,y,w,h,dx,false)\n x+=mx\n"
            +" local my,hy="+axis+"(\n  x,y,w,h,dy,true)\n"
            +" return x,y+my,hx,hy\nend\n";
    }
}
