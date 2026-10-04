package art.pikoos.lab.core;

/** Explicit proposal for bounded statement removal/reordering, preserving all other bytes. */
public final class LuaActionChange {
    public final String original,result,before,after,title;
    public final int header,selectedLine,operation,firstLine;
    public LuaActionChange(String source,int header,int line,int operation){
        if(operation<0||operation>2)throw new IllegalArgumentException("Неизвестная операция");
        this.original=source;this.header=header;this.operation=operation;
        LuaBranchActions list=new LuaBranchActions(source,header,line);
        LuaBranchActions.Entry selected=list.current();
        if(selected==null||selected.line!=line)throw new IllegalArgumentException("Выбери действие");
        for(LuaBranchActions.Entry e:list.entries){
            String mask=new LuaContext(e.source).masked().trim();
            if(!e.fields||e.end!=e.line+1||mask.matches("(?s)^local\\b.*"))
                throw new IllegalArgumentException("Пока меняем порядок и удаляем только в простых ветвях. Вложенный блок, local или незнакомую строку открой через X в Lua.");
        }
        int start=offset(source,line),end=offset(source,selected.end),chosen=line;
        String replacement="";
        if(operation!=2){
            int neighbor=list.index+(operation==0?-1:1);
            if(neighbor<0||neighbor>=list.entries.size())throw new IllegalArgumentException(operation==0?"Это уже первое действие ветви":"Это уже последнее действие ветви");
            LuaBranchActions.Entry a=list.entries.get(Math.min(list.index,neighbor)),b=list.entries.get(Math.max(list.index,neighbor));
            start=offset(source,a.line);end=offset(source,b.end);
            String gap=source.substring(offset(source,a.end),offset(source,b.line));
            if(!gap.trim().isEmpty())throw new IllegalArgumentException("Между действиями есть комментарий. Переставь их в Lua, чтобы сохранить его смысл.");
            replacement=b.source+gap+a.source;chosen=operation==0?a.line:b.line;
        }
        firstLine=operation==0?chosen:line;
        before=source.substring(start,end);after=replacement;
        result=source.substring(0,start)+replacement+source.substring(end);
        LuaBranchActions next=new LuaBranchActions(result,header,chosen);
        if(operation==2){next.index=Math.min(list.index,Math.max(0,next.entries.size()-1));chosen=next.current()==null?header:next.current().line;}
        selectedLine=chosen;
        title=operation==0?"Поднять действие":operation==1?"Опустить действие":"Удалить действие";
    }
    private static int offset(String source,int line){
        int n=0,at=0;while(n<line&&at<source.length()){
            char c=source.charAt(at++);if(c=='\r'){if(at<source.length()&&source.charAt(at)=='\n')at++;n++;}else if(c=='\n')n++;
        }
        if(n!=line)throw new IllegalArgumentException("Строка изменилась");return at;
    }
}
