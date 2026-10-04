package art.pikoos.lab.core;

import java.util.*;

/** User-owned catalogue view; never changes tool availability in a project. */
public final class ToolCatalogue {
    public static final String[] CATEGORIES={"Все","Избранное","Правила","Структура Lua","Рисование","Карта / стены","Движение","Анимация","Камера / фон"};
    private final Set<String> favorites=new LinkedHashSet<>();
    public int category;
    public boolean favorite(int item){return favorites.contains(LuaInsert.ITEMS[item].id);}
    public void toggle(int item){String id=LuaInsert.ITEMS[item].id;if(!favorites.remove(id))favorites.add(id);}
    public String encode(){return String.join(",",favorites);}
    public void restore(String encoded,int category){
        favorites.clear();for(String id:encoded.split(","))for(LuaInsert.Item i:LuaInsert.ITEMS)if(i.id.equals(id))favorites.add(id);
        this.category=Math.max(0,Math.min(CATEGORIES.length-1,category));
    }
    private int group(String id){
        if(id.startsWith("camera")||id.equals("background"))return 8;
        if(id.equals("animation"))return 7;
        if(id.equals("move_box")||id.equals("move_call")||id.equals("door_pair"))return 6;
        if(id.equals("map")||id.equals("solid")||id.equals("solid_box"))return 5;
        if(Arrays.asList("cls","print","text","circle","rect","sprite","sspr").contains(id))return 4;
        if(Arrays.asList("init","update","draw","function","for","while","call").contains(id))return 3;
        return 2;
    }
    public List<Integer> items(){
        List<Integer> result=new ArrayList<>();
        for(int n=0;n<LuaInsert.ITEMS.length;n++)if(category==0||category==1&&favorite(n)||category>1&&group(LuaInsert.ITEMS[n].id)==category)result.add(n);
        return result;
    }
    public void normalize(LuaInsert form){List<Integer> list=items();if(!list.isEmpty()&&!list.contains(form.selected))form.choose(list.get(0));}
    public void change(int step,LuaInsert form){category=Math.floorMod(category+step,CATEGORIES.length);normalize(form);}
    public void move(int step,LuaInsert form){
        List<Integer> list=items();if(list.isEmpty())return;int at=list.indexOf(form.selected);
        form.choose(list.get(Math.max(0,Math.min(list.size()-1,Math.max(0,at)+step))));
    }
}
