package art.pikoos.lab.core;

import java.util.*;

/** Recognized ordinary Lua strips in source order, never an inferred scene graph. */
public final class BackgroundLayers {
    public final String source;
    public final List<LuaCall> entries;
    public int index;
    public BackgroundLayers(String source,int point){
        this.source=source;entries=Collections.unmodifiableList(BackgroundLayer.all(source));
        for(int n=0;n<entries.size();n++)if(entries.get(n).start<=point)index=n;
    }
    public LuaCall current(){return entries.isEmpty()?null:entries.get(index);}
    public void move(int step){index=Math.max(0,Math.min(entries.size()-1,index+step));}
    public int line(int n){return source.substring(0,entries.get(n).start).split("\r\n|\r|\n",-1).length;}
    public String title(int n){LuaInsert f=entries.get(n).form;return (n+1)+" · "+f.value(2)+"×"+f.value(3)+" · Y "+f.value(4);}
    public String detail(int n){LuaInsert f=entries.get(n).form;return f.value(5)+" px/с · "+f.value(6)+" · "+(f.value(7).equals("true")?"виден":"скрыт");}
    public String blocked(int direction){
        if(current()==null)return "Слоёв пока нет";
        if(direction==2||direction==3)return "";
        int other=index+direction;
        if(other<0||other>=entries.size())return direction<0?"Это первый слой списка":"Это последний слой списка";
        LuaCall a=entries.get(Math.min(index,other)),b=entries.get(Math.max(index,other));
        return source.substring(a.end,b.start).trim().isEmpty()?"":"Между слоями другой код или комментарий. Открой Lua через X; перенос здесь не меняет исходник.";
    }
    public Change change(int direction){
        if(direction!=-1&&direction!=1&&direction!=2&&direction!=3)throw new IllegalArgumentException("Операция слоя");
        String reason=blocked(direction);if(!reason.isEmpty())throw new IllegalArgumentException(reason);
        LuaCall selected=current();
        if(direction==2){
            String nl=source.startsWith("\r\n",selected.end)?"\r\n":source.startsWith("\r",selected.end)?"\r":"\n";
            String result=source.substring(0,selected.end)+nl+selected.original+source.substring(selected.end);
            return new Change(source,result,selected.end+nl.length(),index,index+1,direction);
        }
        if(direction==3){
            int end=selected.end;
            if(source.startsWith("\r\n",end))end+=2;else if(end<source.length()&&(source.charAt(end)=='\r'||source.charAt(end)=='\n'))end++;
            String result=source.substring(0,selected.start)+source.substring(end);
            BackgroundLayers next=new BackgroundLayers(result,selected.start);
            int at=next.current()==null?Math.min(selected.start,result.length()):next.current().start;
            return new Change(source,result,at,index,next.index,direction);
        }
        int other=index+direction;LuaCall a=entries.get(Math.min(index,other)),b=entries.get(Math.max(index,other));
        String gap=source.substring(a.end,b.start);
        String result=source.substring(0,a.start)+b.original+gap+a.original+source.substring(b.end);
        int point=direction<0?a.start:a.start+b.original.length()+gap.length();
        return new Change(source,result,point,index,other,direction);
    }
    public static final class Change {
        public final String original,result;
        public final int point,from,to,direction;
        private Change(String original,String result,int point,int from,int to,int direction){this.original=original;this.result=result;this.point=point;this.from=from;this.to=to;this.direction=direction;}
    }
}
