package art.pikoos.lab.core;

import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

/** Optional ordinary Lua comments. Names never authorize source recognition or affect execution. */
public final class GameElementNames {
    public static final int LIMIT=24;
    private static final String PREFIX="-- pikonest:name:";
    private static final Pattern LINE=Pattern.compile("(?m)^-- pikonest:name:([^\\r\\n]*)(?:\\r?\\n|$)");
    public final List<String> values;
    private GameElementNames(int count){values=new ArrayList<>(Collections.nCopies(count,""));}
    public static GameElementNames empty(int count){return new GameElementNames(count);}
    public static GameElementNames read(WorkshopCartridge cart,GameUses uses){
        GameElementNames names=empty(uses.entries.size());String code=cart.code();LuaContext context=new LuaContext(code);Matcher m=LINE.matcher(code);Set<Integer> seen=new HashSet<>();
        while(m.find())if(context.allowsLine(m.start())){
            try{
                String[] parts=m.group(1).split(":",-1);if(parts.length!=4||!parts[0].equals("1"))throw new IllegalArgumentException();
                int n=Integer.parseInt(parts[1]);if(n<0||n>=names.values.size()||!seen.add(n)||!parts[2].equals(signature(uses.entries.get(n))))throw new IllegalArgumentException();
                String value=decode(parts[3]);if(value.isEmpty()||!normalize(value).equals(value))throw new IllegalArgumentException();names.values.set(n,value);
            }catch(Exception e){throw new IllegalArgumentException("Element names no longer match this layout. Saved data was kept. Use All tools to review the name comments or undo the external edit.",e);}
        }return names;
    }
    public static String normalize(String value){
        if(value==null||value.length()>LIMIT)throw new IllegalArgumentException("Use up to 24 characters for an element name.");
        for(int n=0;n<value.length();n++){
            char c=value.charAt(n);if(Character.isISOControl(c)||Character.getType(c)==Character.FORMAT)throw new IllegalArgumentException("Use visible characters in an element name.");
            if(Character.isHighSurrogate(c)){if(n+1>=value.length()||!Character.isLowSurrogate(value.charAt(++n)))throw new IllegalArgumentException("Invalid name character.");}
            else if(Character.isLowSurrogate(c))throw new IllegalArgumentException("Invalid name character.");
        }return value.trim();
    }
    public GameElementNames copy(){GameElementNames copy=empty(0);copy.values.addAll(values);return copy;}
    private static String signature(GameUses.Entry e){
        SpriteRegion r=e.kind().equals("spr")?new SpriteRegion(e.values[0]%16*8,e.values[0]/16*8,8,8):new SpriteRegion(e.values[0],e.values[1],e.values[2],e.values[3]);
        return r.x+","+r.y+","+r.width+","+r.height+","+e.x()+","+e.y();
    }
    private static String encode(String value){StringBuilder b=new StringBuilder();for(byte v:value.getBytes(StandardCharsets.UTF_8)){int n=v&255;b.append("0123456789abcdef".charAt(n>>4)).append("0123456789abcdef".charAt(n&15));}return b.toString();}
    private static String decode(String value)throws CharacterCodingException{
        if(value.length()>LIMIT*8||value.length()%2!=0||!value.matches("[0-9a-f]*"))throw new IllegalArgumentException();byte[] bytes=new byte[value.length()/2];
        for(int n=0;n<bytes.length;n++)bytes[n]=(byte)Integer.parseInt(value.substring(n*2,n*2+2),16);
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    }
    /** Called only after a supported, source-checked operation has supplied the new layout. */
    public WorkshopCartridge apply(WorkshopCartridge cart){
        PlayModel model=PlayModel.read(cart);GameUses uses=new GameUses(model==null?cart:model.layout(cart),0);
        if(!uses.blocked.isEmpty()||uses.entries.size()!=values.size())throw new IllegalArgumentException("Element names could not follow this edit. Saved source was kept.");
        String code=cart.code(),nl=code.contains("\r\n")?"\r\n":"\n";LuaContext context=new LuaContext(code);Matcher m=LINE.matcher(code);StringBuilder stripped=new StringBuilder();int at=0;
        while(m.find())if(context.allowsLine(m.start())){stripped.append(code,at,m.start());at=m.end();}stripped.append(code.substring(at));
        StringBuilder result=new StringBuilder();for(int n=0;n<values.size();n++){String value=normalize(values.get(n));if(!value.isEmpty())result.append(PREFIX).append("1:").append(n).append(':').append(signature(uses.entries.get(n))).append(':').append(encode(value)).append(nl);}
        String plain=stripped.toString(),annotations=result.toString();
        if(!annotations.isEmpty()){
            // PICO-8 uses the first two source lines for a cartridge label. Keep title/author
            // comments there; never put encoded editor annotations in those two positions.
            int insert=0,lines=0;
            while(insert<plain.length()){
                int end=plain.indexOf('\n',insert);if(end<0)break;
                String line=plain.substring(insert,end).trim();
                if(!line.isEmpty()&&(!line.startsWith("--")||line.startsWith("--[")))break;
                insert=end+1;lines++;
            }
            String padding=lines>=2?"":lines==1?nl:nl+nl;
            plain=plain.substring(0,insert)+padding+annotations+plain.substring(insert);
        }
        return plain.equals(code)?cart:PlayModel.code(cart,plain);
    }
}
