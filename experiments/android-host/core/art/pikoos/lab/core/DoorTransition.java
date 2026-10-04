package art.pikoos.lab.core;

import java.util.*;
import java.util.regex.*;

/** A pair of point-entry portals, emitted as ordinary Lua with one explicit latch. */
public final class DoorTransition {
    private DoorTransition(){}
    // Repeated placeholders let the exact binding replace all uses of a field together.
    private static final String TEMPLATE="do\n"
        +" local _gate_x,_gate_y=@0@,@1@\n"
        +" local _gate_a=_gate_x>=@3@ and _gate_x<@3@+@7@\n"
        +"  and _gate_y>=@4@ and _gate_y<@4@+@8@\n"
        +" local _gate_b=_gate_x>=@5@ and _gate_x<@5@+@7@\n"
        +"  and _gate_y>=@6@ and _gate_y<@6@+@8@\n"
        +" if not _gate_a and not _gate_b then\n"
        +"  @2@=false\n"
        +" elseif not @2@ then\n"
        +"  @2@=true\n"
        +"  if _gate_a then\n"
        +"   @0@,@1@=@5@,@6@\n"
        +"  else\n"
        +"   @0@,@1@=@3@,@4@\n"
        +"  end\n end\nend";
    private static final Pattern SLOT=Pattern.compile("@([0-8])@");
    private static final ArrayList<Integer> FIELDS=new ArrayList<>();
    private static final Pattern BLOCK;
    static {
        Matcher m=SLOT.matcher(TEMPLATE);StringBuilder p=new StringBuilder("(?m)^[ \\t]*");int at=0;
        while(m.find()){
            literal(p,TEMPLATE.substring(at,m.start()));int f=Integer.parseInt(m.group(1));FIELDS.add(f);
            p.append(f<3?"([A-Za-z_][A-Za-z_0-9]*)":"(-?[0-9]+)");at=m.end();
        }
        literal(p,TEMPLATE.substring(at));p.append("[ \\t]*(?=\\r|\\n|$)");BLOCK=Pattern.compile(p.toString());
    }
    private static void literal(StringBuilder p,String s){
        String[] lines=s.split("\n",-1);
        for(int n=0;n<lines.length;n++){
            if(n>0)p.append("(?:\\r\\n|\\r|\\n)[ \\t]*");
            p.append(Pattern.quote(n>0?lines[n].replaceFirst("^ +",""):lines[n]));
        }
    }
    public static String code(LuaInsert form){
        String s=TEMPLATE;for(int n=0;n<9;n++)s=s.replace("@"+n+"@",form.value(n));return s+"\n";
    }
    public static void validate(String source,LuaInsert form){
        for(int n=0;n<3;n++){
            String name=form.value(n);
            TileProbe.validateAvailableName("",name);
            if(name.matches("_gate_[xyab]"))throw new IllegalArgumentException("Имя "+name+" занято внутри перехода. Выбери другое.");
            for(int j=0;j<n;j++)if(name.equals(form.value(j)))throw new IllegalArgumentException("X, Y и память перехода должны иметь разные имена");
        }
        TileProbe.validateAvailableName(source,form.value(2));
        int ax=Integer.parseInt(form.value(3)),ay=Integer.parseInt(form.value(4)),bx=Integer.parseInt(form.value(5)),by=Integer.parseInt(form.value(6));
        int w=Integer.parseInt(form.value(7)),h=Integer.parseInt(form.value(8));
        if(ax<bx+w&&bx<ax+w&&ay<by+h&&by<ay+h)
            throw new IllegalArgumentException("Области входа пересекаются. Разнеси их; переход не изменён.");
    }
    public static LuaCall find(String source,int point){
        Matcher m=BLOCK.matcher(source);LuaContext context=new LuaContext(source);
        while(m.find()){
            if(point<m.start()||point>=m.end()||!context.allowsLine(m.start()))continue;
            LuaInsert form=new LuaInsert();for(int n=0;n<LuaInsert.ITEMS.length;n++)if(LuaInsert.ITEMS[n].id.equals("door_pair"))form.choose(n);
            form.editing=true;form.screen=LuaInsert.Screen.FIELDS;String[] values=new String[9];int[] from=new int[FIELDS.size()],to=new int[from.length],fields=new int[from.length];
            for(int n=0;n<from.length;n++){
                int f=FIELDS.get(n);String v=m.group(n+1);
                if(values[f]!=null&&!values[f].equals(v))return null;
                values[f]=v;from[n]=m.start(n+1)-m.start();to[n]=m.end(n+1)-m.start();fields[n]=f;
            }
            try{for(int n=0;n<9;n++)form.set(n,values[n]);validate(source.substring(0,m.start())+source.substring(m.end()),form);}
            catch(IllegalArgumentException e){return null;}
            return new LuaCall(m.start(),m.end(),m.group(),"door_pair",from,to,form,fields);
        }
        return null;
    }
}
