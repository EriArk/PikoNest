package art.pikoos.lab.core;

import java.util.*;
import java.util.regex.*;

/** Conservative whole-draft name index, not scope analysis or a Lua validator. */
public final class LuaSymbols {
    public static final class Entry {
        public final String name,value,help;public final int line;public final boolean function;
        Entry(String name,String value,String help,int line,boolean function){this.name=name;this.value=value;this.help=help;this.line=line;this.function=function;}
    }
    private static final String WORDS=" and break do else elseif end false for function if in local nil not or repeat return then true until while ";
    private static final Pattern TOKEN=Pattern.compile("[A-Za-z_][A-Za-z_0-9]*|(?:0[xX][0-9a-fA-F.]+|0[bB][01.]+|[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?)|==|~=|!=|<=|>=|\\+=|-=|\\*=|/=|%=|\\.\\.|[^\\s]");
    private static final class Token {
        final String value;final int line,depth;final boolean name;
        Token(String value,int line,int depth,boolean name){this.value=value;this.line=line;this.depth=depth;this.name=name;}
    }
    private final TreeMap<String,Entry> entries=new TreeMap<>();
    private final Set<String> local=new HashSet<>(),assigned=new HashSet<>();
    public final boolean complete;
    public LuaSymbols(String source){
        String masked=new LuaContext(source).masked();ArrayList<Token> tokens=new ArrayList<>();
        Matcher m=TOKEN.matcher(masked);int line=1,last=0,depth=0;boolean bounded=true;
        while(m.find()){
            if(tokens.size()==100000){bounded=false;break;}
            for(int n=last;n<m.start();n++)if(source.charAt(n)=='\n'||(source.charAt(n)=='\r'&&(n+1==source.length()||source.charAt(n+1)!='\n')))line++;
            last=m.end();String value=m.group();
            boolean name=value.matches("[A-Za-z_][A-Za-z_0-9]*")&&!WORDS.contains(" "+value+" ");
            // Do not turn the ASCII suffix of an unsupported identifier into a name.
            if(m.start()>0&&Character.isUnicodeIdentifierPart(source.charAt(m.start()-1)))name=false;
            if(m.end()<source.length()&&Character.isUnicodeIdentifierPart(source.charAt(m.end())))name=false;
            tokens.add(new Token(value,line,depth,name));
            if(value.equals("{"))depth++;if(value.equals("}"))depth=Math.max(0,depth-1);
        }
        if(!bounded){complete=false;return;}
        for(int n=0;n<tokens.size();n++){
            Token t=tokens.get(n);String v=t.value;
            if(v.equals("local")||v.equals("for")){
                int at=n+1;if(is(tokens,at,"function"))at++;
                while(at<tokens.size()&&tokens.get(at).name){local.add(tokens.get(at).value);at++;if(!is(tokens,at,","))break;at++;}
            }
            if(v.equals("function")){
                int at=n+1;Token name=at<tokens.size()&&tokens.get(at).name?tokens.get(at++):null;
                boolean simple=name!=null&&is(tokens,at,"(");
                int limit=Math.min(tokens.size(),n+256);
                while(at<limit&&!is(tokens,at,"(")){if(is(tokens,at,":"))local.add("self");at++;}
                if(at==limit){complete=false;return;}
                int first=++at;while(at<limit&&!is(tokens,at,")")){if(tokens.get(at).name)local.add(tokens.get(at).value);at++;}
                if(at==limit){complete=false;return;}
                if(simple&&name.depth==0){assigned.add(name.value);record(name,true,at==first);}
                // Anonymous function parameters are also excluded, wherever used.
            }
            if(v.equals("=")&&n>0){
                int at=n-1;boolean first=true;
                while(at>=0&&tokens.get(at).name){
                    Token name=tokens.get(at);
                    if(name.depth==0&&!is(tokens,at-1,".")&&!is(tokens,at-1,":")){
                        assigned.add(name.value);boolean function=first&&is(tokens,n+1,"function");
                        // Multiple RHS values cannot be associated by this bounded index.
                        if(is(tokens,n+1,"function")&&(is(tokens,n-2,",")||!first))local.add(name.value);
                        record(name,function,function&&is(tokens,n+2,"(")&&is(tokens,n+3,")"));
                    }
                    first=false;if(!is(tokens,at-1,","))break;at-=2;
                }
            }
        }
        // Hide ambiguous names globally, even when a particular local is out of scope.
        for(String name:local)entries.remove(name);
        complete=true;
    }
    private void record(Token t,boolean function,boolean zeroArgs){
        Entry old=entries.get(t.value);
        // A reassigned function is no longer a reliably callable no-argument declaration.
        boolean callable=function&&zeroArgs&&(old==null||old.function);
        int line=old==null?t.line:old.line;
        entries.put(t.value,new Entry(t.value,t.value,callable?"Функция без аргументов · строка "+line:
            function?"Функция с аргументами · открой её код":"Переменная · строка "+line,line,callable));
        if(function&&!callable)local.add(t.value); // Not offered as a variable or an invalid zero-argument call.
    }
    private static boolean is(List<Token> tokens,int at,String value){return at>=0&&at<tokens.size()&&tokens.get(at).value.equals(value);}
    public List<Entry> project(boolean functions){
        ArrayList<Entry> result=new ArrayList<>();if(complete)for(Entry e:entries.values())if(e.function==functions)result.add(e);
        return Collections.unmodifiableList(result);
    }
    public boolean shadows(String name){return !complete||local.contains(name)||assigned.contains(name);}
    private static Entry api(String name,String value,String help){return new Entry(name,value,help,0,true);}
    private static final Entry[] API={
        api("time","time()","Время игры в секундах. Считается по обновлениям игры, не по настенным часам."),
        api("rnd","rnd(1)","Случайное число от 0 до 1, не включая 1. Аргумент задаёт верхнюю границу."),
        api("flr","flr(0)","Округляет число вниз: flr(2.8) даёт 2. Измени аргумент в выражении."),
        api("abs","abs(0)","Модуль числа: abs(-3) даёт 3. Измени аргумент в выражении."),
        api("min","min(0,1)","Меньшее из двух чисел. Аргументы можно изменить в выражении."),
        api("max","max(0,1)","Большее из двух чисел. Аргументы можно изменить в выражении."),
        api("btn","btn(4)","Удержание кнопки O: true или false. Номера кнопок — от 0 до 5."),
        api("btnp","btnp(4)","Нажатие кнопки O с автоповтором: true или false. Номер можно изменить.")
    };
    public List<Entry> api(){
        ArrayList<Entry> result=new ArrayList<>();if(complete)for(Entry e:API)if(!local.contains(e.name)&&!assigned.contains(e.name))result.add(e);
        return Collections.unmodifiableList(result);
    }
}
