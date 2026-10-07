package art.pikoos.lab.core;

import java.util.*;
import java.util.regex.*;

/** Recognized numeric globals at game start, for coordinate forms and sketches only. */
public final class GameValues {
    public final Map<String,Integer> initial=new LinkedHashMap<>();
    public GameValues(String source){
        try{
            LuaContext context=new LuaContext(source);String mask=context.masked();
            LuaBranches blocks=new LuaBranches(source,0);
            if(!blocks.warning.isEmpty()||Pattern.compile("(?m)^\\s*#include\\b|\\b(?:_ENV|_G)\\b").matcher(mask).find())return;
            String[] rows=source.split("\\r\\n|\\r|\\n",-1),masked=mask.split("\\r\\n|\\r|\\n",-1);
            int start=-1,end=-1,count=0;
            for(int n=0;n<masked.length;n++)if(Pattern.compile("\\b_init\\b").matcher(masked[n]).find()){
                if(masked[n].trim().matches("function\\s+_init\\s*\\(\\s*\\)")){
                    count++;start=n;Integer close=blocks.blockEnds.get(n);if(close==null)return;end=close-1;
                    for(Map.Entry<Integer,Integer> block:blocks.blockEnds.entrySet())if(block.getKey()<n&&n<block.getValue())return;
                }else if(!masked[n].trim().equals("_init()"))return;
            }
            if(count!=1)return;
            Map<String,Integer> found=new LinkedHashMap<>();
            for(int n=start+1;n<end;n++){
                if(masked[n].trim().isEmpty())continue;
                LuaCall call=LuaCall.parse(rows[n],0,rows[n].length());
                if(!call.name.equals("rule:assign=")||!call.form.value(1).matches("-?[0-9]+"))return;
                String name=call.form.value(0);int value=Integer.parseInt(call.form.value(1));
                if(name.startsWith("_")||value<-32768||value>32767||found.containsKey(name))return;
                if(Pattern.compile("\\blocal\\s+(?:function\\s+)?"+Pattern.quote(name)+"\\b|\\bfor\\s+"+Pattern.quote(name)+"\\b").matcher(mask).find())return;
                found.put(name,value);
            }
            Set<String> globals=new HashSet<>();for(LuaSymbols.Entry e:new LuaSymbols(source).project(false))globals.add(e.name);
            if(!globals.containsAll(found.keySet()))return;
            initial.putAll(found);
        }catch(IllegalArgumentException ignored){initial.clear();}
    }
    public int value(String name){Integer value=initial.get(name);if(value==null)throw new IllegalArgumentException("Choose a numeric starting value from Rules & state. Unknown expressions stay in Lua.");return value;}
}
