package art.pikoos.lab.core;

import java.io.*;
import java.util.*;
import art.pikoos.lab.core.WorkshopSession.Action;

/** Modal settings library. Applying changes only the pending form, never the Lua draft. */
public final class PresetPanel {
    public enum Page { LIST, NAME, REVIEW }
    public Page page=Page.LIST;
    public int index,scroll;
    public boolean favorites,saving,closed;
    public String notice="";
    public NameEditor name;
    public ParameterPreset review;
    private List<ParameterPreset> entries;
    private final LuaInsert target;
    private final WorkshopSession.Port port;
    public PresetPanel(LuaInsert target,WorkshopSession.Port port)throws Exception{
        if(target==null||target.screen!=LuaInsert.Screen.FIELDS||!ParameterPreset.available(target))throw new IllegalArgumentException("Сначала открой форму параметров");
        this.target=target;this.port=port;refresh();
    }
    private void refresh()throws Exception{entries=port.presets();index=Math.min(index,items().size());}
    public List<ParameterPreset> items(){List<ParameterPreset> list=new ArrayList<>();for(ParameterPreset p:entries)if(p.tool.equals(target.item().id)&&(!favorites||p.favorite))list.add(p);list.sort(Comparator.comparing((ParameterPreset p)->p.title).thenComparing(p->p.id));return list;}
    public ParameterPreset selected(){List<ParameterPreset> list=items();return index==0||index>list.size()?null:list.get(index-1);}
    public void act(Action a)throws Exception{
        if(page==Page.NAME){
            if(a==Action.UP)name.move(0,-1);if(a==Action.DOWN)name.move(0,1);if(a==Action.LEFT)name.move(-1,0);if(a==Action.RIGHT)name.move(1,0);
            if(a==Action.PREVIOUS||a==Action.NEXT)name.language();if(a==Action.UNDO)name.uppercase=!name.uppercase;
            if(a==Action.MENU)name.replaceAll=!name.replaceAll;if(a==Action.CONFIRM)name.type(name.key);if(a==Action.CONTEXT)name.erase();
            if(a==Action.CANCEL){page=Page.LIST;name=null;review=null;}
            if(a==Action.TEST&&name.value()!=null){review=review.named(name.value());page=Page.REVIEW;scroll=0;}
            return;
        }
        if(page==Page.REVIEW){
            if(a==Action.UP)scroll=Math.max(0,scroll-1);if(a==Action.DOWN)scroll=Math.min(target.item().fields.length-1,scroll+1);
            if(a==Action.CANCEL){page=saving?Page.NAME:Page.LIST;return;}
            if(a==Action.CONFIRM){
                if(saving){port.storePreset(review);refresh();favorites=false;index=1;List<ParameterPreset> list=items();for(int n=0;n<list.size();n++)if(list.get(n).id.equals(review.id))index=n+1;notice="Набор сохранён";page=Page.LIST;review=null;name=null;}
                else{review.apply(target);closed=true;}
            }return;
        }
        if(a==Action.CANCEL){closed=true;return;}
        if(a==Action.LEFT||a==Action.RIGHT||a==Action.PREVIOUS||a==Action.NEXT){favorites=!favorites;index=0;}
        if(a==Action.UP)index=Math.max(0,index-1);if(a==Action.DOWN)index=Math.min(items().size(),index+1);
        if(a==Action.UNDO&&selected()!=null){ParameterPreset old=selected();port.updatePreset(old,old.starred());refresh();}
        if(a==Action.CONFIRM){
            saving=index==0;
            review=saving?ParameterPreset.capture(target,target.item().title,port.projectOrigin()):selected();
            if(saving){name=new NameEditor(review.title);page=Page.NAME;}else{review.proposed(target);page=Page.REVIEW;}scroll=0;
        }
    }
    public String encode(){try{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(1);out.writeUTF(target.item().id);out.writeInt(target.item().fields.length);for(int n=0;n<target.item().fields.length;n++)out.writeUTF(target.value(n));
        out.writeUTF(page.name());out.writeInt(index);out.writeInt(scroll);out.writeBoolean(favorites);out.writeBoolean(saving);
        out.writeBoolean(review!=null);if(review!=null){byte[] data=review.encode();out.writeInt(data.length);out.write(data);}
        out.writeBoolean(name!=null);if(name!=null){out.writeUTF(name.text());out.writeInt(name.key);out.writeBoolean(name.latin);out.writeBoolean(name.uppercase);out.writeBoolean(name.replaceAll);}
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }catch(IOException e){throw new IllegalStateException(e);}}
    public static PresetPanel restore(String encoded,LuaInsert form,WorkshopSession.Port port)throws Exception{
        if(encoded.length()>40000)throw new IOException("Журнал набора слишком велик");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)));
        if(in.readInt()!=1||!in.readUTF().equals(form.item().id)||in.readInt()!=form.item().fields.length)throw new IOException("Форма набора изменилась");
        for(int n=0;n<form.item().fields.length;n++)if(!in.readUTF().equals(form.value(n)))throw new IOException("Параметры формы изменились");
        PresetPanel p=new PresetPanel(form,port);p.page=Page.valueOf(in.readUTF());p.index=in.readInt();p.scroll=in.readInt();p.favorites=in.readBoolean();p.saving=in.readBoolean();
        if(in.readBoolean()){int size=in.readInt();if(size<0||size>20000)throw new IOException("Неверный размер набора");byte[] data=new byte[size];in.readFully(data);p.review=ParameterPreset.decode(data);p.review.proposed(form);}
        if(in.readBoolean()){p.name=new NameEditor(in.readUTF());p.name.key=in.readInt();p.name.latin=in.readBoolean();p.name.uppercase=in.readBoolean();p.name.replaceAll=in.readBoolean();if(p.name.key<0||p.name.key>=p.name.count())throw new IOException("Курсор имени");}
        if(in.available()!=0||p.index<0||p.scroll<0||p.scroll>=form.item().fields.length||p.page!=Page.LIST&&p.review==null||p.page==Page.NAME&&p.name==null||p.page==Page.REVIEW&&p.saving&&p.name==null)throw new IOException("Некорректный журнал набора");
        p.index=Math.min(p.index,p.items().size());return p;
    }
}
