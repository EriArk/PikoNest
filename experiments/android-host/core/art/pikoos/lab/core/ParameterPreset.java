package art.pikoos.lab.core;

import java.io.*;
import java.util.*;

/** Independent, versioned settings; never contains a cart or executable snippet. */
public final class ParameterPreset {
    public final String id,title,origin,tool;
    public final boolean favorite;
    private final String[] values;
    private ParameterPreset(String id,String title,String origin,String tool,boolean favorite,String[] values){
        if(!id.matches("[0-9a-f-]{36}"))throw new IllegalArgumentException("Некорректный ID набора");
        NameEditor name=new NameEditor(title);if(name.value()==null)throw new IllegalArgumentException("Назови набор");
        if(origin.length()>256)throw new IllegalArgumentException("Слишком длинное имя источника");
        this.id=id;this.title=name.value();this.origin=origin;this.tool=tool;this.favorite=favorite;this.values=values.clone();
        LuaInsert check=form();if(values.length!=check.item().fields.length)throw new IllegalArgumentException("Поля набора изменились");
        int count=0;for(int n=0;n<values.length;n++)if(values[n]!=null){
            if(!portable(check,n,values[n]))throw new IllegalArgumentException("Набор содержит привязку к проекту");
            check.set(n,values[n]);count++;
        }
        if(count==0)throw new IllegalArgumentException("Нет независимых настроек. Имена и выражения остаются в проекте.");
    }
    public static boolean available(LuaInsert f){return f.item().fields.length>0;}
    public static boolean portable(LuaInsert f,int n,String value){
        String id=f.item().id;LuaInsert.Kind kind=f.item().fields[n].kind;
        if(kind==LuaInsert.Kind.NAME||kind==LuaInsert.Kind.FLAG)return false;
        if(id.equals("sprite")&&n==0||(id.equals("sspr")||id.equals("background"))&&n<4||id.equals("map")&&n<2)return false;
        if(kind==LuaInsert.Kind.EXPR||kind==LuaInsert.Kind.COLOR)
            return value.matches("-?(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)")||value.equals("true")||value.equals("false");
        return true;
    }
    public static ParameterPreset capture(LuaInsert form,String title,String origin){
        String[] values=new String[form.item().fields.length];
        for(int n=0;n<values.length;n++)if(portable(form,n,form.value(n)))values[n]=form.value(n);
        return new ParameterPreset(UUID.randomUUID().toString(),title,origin,form.item().id,false,values);
    }
    public ParameterPreset named(String name){return new ParameterPreset(id,name,origin,tool,favorite,values);}
    public ParameterPreset starred(){return new ParameterPreset(id,title,origin,tool,!favorite,values);}
    public LuaInsert form(){LuaInsert f=new LuaInsert();for(int n=0;n<LuaInsert.ITEMS.length;n++)if(LuaInsert.ITEMS[n].id.equals(tool)){f.choose(n);return f;}throw new IllegalArgumentException("Неизвестный инструмент набора");}
    public String value(int n){return values[n];}
    public boolean replaces(LuaInsert target,int n){
        if(!target.item().id.equals(tool))throw new IllegalArgumentException("Набор для другого инструмента");
        // A destination expression/name is a project binding too; keep it for explicit editing.
        return values[n]!=null&&portable(target,n,target.value(n));
    }
    public String[] proposed(LuaInsert target){
        String[] next=new String[values.length];LuaInsert check=form();check.editing=target.editing;
        for(int n=0;n<next.length;n++){next[n]=replaces(target,n)?values[n]:target.value(n);check.set(n,next[n]);}
        return next;
    }
    public void apply(LuaInsert target){String[] next=proposed(target);for(int n=0;n<next.length;n++)target.set(n,next[n]);}
    public byte[] encode(){try{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(0x504b5052);out.writeInt(1);out.writeUTF(id);out.writeUTF(title);out.writeUTF(origin);out.writeUTF(tool);out.writeBoolean(favorite);
        LuaInsert f=form();out.writeInt(values.length);for(int n=0;n<values.length;n++){
            out.writeUTF(f.item().fields[n].label);out.writeUTF(f.item().fields[n].kind.name());out.writeBoolean(values[n]!=null);if(values[n]!=null)out.writeUTF(values[n]);
        }out.close();return bytes.toByteArray();
    }catch(IOException e){throw new IllegalStateException(e);}}
    public static ParameterPreset decode(byte[] bytes)throws IOException{try{
        if(bytes.length>20000)throw new IOException("Набор слишком велик");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));if(in.readInt()!=0x504b5052||in.readInt()!=1)throw new IOException("Версия набора не поддержана");
        String id=in.readUTF(),title=in.readUTF(),origin=in.readUTF(),tool=in.readUTF();boolean favorite=in.readBoolean();
        LuaInsert f=new LuaInsert();boolean found=false;for(int n=0;n<LuaInsert.ITEMS.length;n++)if(LuaInsert.ITEMS[n].id.equals(tool)){f.choose(n);found=true;break;}
        int count=in.readInt();if(!found||count!=f.item().fields.length)throw new IOException("Инструмент или поля изменились");
        String[] values=new String[count];for(int n=0;n<count;n++){
            in.readUTF(); // Historical label is informational; translating the UI must not invalidate settings.
            if(!in.readUTF().equals(f.item().fields[n].kind.name()))throw new IOException("Схема полей изменилась");
            if(in.readBoolean())values[n]=in.readUTF();
        }if(in.available()!=0)throw new IOException("Лишние данные набора");return new ParameterPreset(id,title,origin,tool,favorite,values);
    }catch(IllegalArgumentException e){throw new IOException(e.getMessage(),e);}}
}
