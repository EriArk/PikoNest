package art.pikoos.lab.core;

import java.util.*;
import java.util.regex.*;

/** Exact field spans for bounded single-line calls and rules; never rewrites block bodies. */
public final class LuaCall {
    public final int start,end;public final String original,name;
    private final int[] from,to;private final String[] initial;
    public final LuaInsert form;
    LuaCall(int start,int end,String original,String name,int[] from,int[] to,LuaInsert form){
        this.start=start;this.end=end;this.original=original;this.name=name;this.from=from;this.to=to;this.form=form;
        initial=new String[from.length];for(int n=0;n<initial.length;n++)initial[n]=form.value(n);
    }
    private static IllegalArgumentException unsupported(){return new IllegalArgumentException("Для этой строки формы пока нет. Вернись к коду и выбери «Ввод».");}
    public static LuaCall parse(String source,int start,int end){
        LuaContext context=new LuaContext(source);
        if(start<0||end<start||end>source.length()||!context.allowsLine(start))throw unsupported();
        String raw=source.substring(start,end),mask=context.masked().substring(start,end);
        if(raw.indexOf('\n')>=0||raw.indexOf('\r')>=0)throw unsupported();
        LuaCall rule=LuaRule.parse(raw,start,end);
        if(rule!=null){
            int depth=0;String prefix=context.masked().substring(0,start);
            for(int n=0;n<prefix.length();n++){char c=prefix.charAt(n);if(c=='('||c=='['||c=='{')depth++;if(c==')'||c==']'||c=='}')depth--;}
            if(depth!=0)throw unsupported();
            return rule;
        }
        Matcher head=Pattern.compile("^[ \\t]*(cls|print|circfill|rectfill|spr|sspr|map)[ \\t]*\\(").matcher(mask);
        if(!head.find())throw unsupported();
        String name=head.group(1);ArrayList<Integer> starts=new ArrayList<>(),ends=new ArrayList<>();
        if(new LuaSymbols(source).shadows(name))throw new IllegalArgumentException("Этот вызов нельзя уверенно распознать. Вернись к коду и выбери «Ввод».");
        ArrayDeque<Character> stack=new ArrayDeque<>();stack.push('(');int arg=head.end(),close=-1;
        for(int n=arg;n<mask.length();n++){
            char c=mask.charAt(n);
            if(c=='('||c=='['||c=='{')stack.push(c);
            if(c==')'||c==']'||c=='}'){
                char expected=c==')'?'(':c==']'?'[':'{';
                if(stack.isEmpty()||stack.pop()!=expected)throw unsupported();
                if(stack.isEmpty()){starts.add(arg);ends.add(n);close=n;break;}
            }
            if(c==','&&stack.size()==1){starts.add(arg);ends.add(n);arg=n+1;}
        }
        if(close<0||!mask.substring(close+1).matches("[ \\t]*;?[ \\t]*"))throw unsupported();
        int index=name.equals("cls")?10:name.equals("print")?11:name.equals("circfill")?13:name.equals("rectfill")?14:name.equals("sspr")?18:name.equals("map")?19:15;
        LuaInsert form=new LuaInsert();form.choose(index);form.editing=true;form.screen=LuaInsert.Screen.FIELDS;
        if(starts.size()!=form.item().fields.length)throw unsupported();
        int[] from=new int[starts.size()],to=new int[from.length];
        for(int n=0;n<from.length;n++){
            int a=starts.get(n),b=ends.get(n);while(a<b&&Character.isWhitespace(raw.charAt(a)))a++;while(b>a&&Character.isWhitespace(raw.charAt(b-1)))b--;
            if(a==b)throw unsupported();from[n]=a;to[n]=b;
        }
        String literal=name.equals("print")?literal(raw.substring(from[0],to[0])):null;
        if(literal!=null){form.choose(12);form.editing=true;}
        try{for(int n=0;n<from.length;n++)form.set(n,n==0&&literal!=null?literal:raw.substring(from[n],to[n]));}
        catch(IllegalArgumentException e){throw unsupported();}
        return new LuaCall(start,end,raw,name,from,to,form);
    }
    private static String literal(String raw){
        if(raw.length()<2||(raw.charAt(0)!='\"'&&raw.charAt(0)!='\''))return null;
        char quote=raw.charAt(0);if(raw.charAt(raw.length()-1)!=quote)return null;StringBuilder value=new StringBuilder();
        for(int n=1;n<raw.length()-1;n++){
            char c=raw.charAt(n);if(c==quote)return null;
            if(c=='\\'){if(++n>=raw.length()-1)return null;c=raw.charAt(n);if(c!='\\'&&c!=quote)return null;}
            value.append(c);
        }
        return value.toString();
    }
    public String preview(LuaInsert proposal){
        if(proposal.selected!=form.selected)throw unsupported();
        StringBuilder changed=new StringBuilder();int at=0;
        for(int n=0;n<from.length;n++){
            changed.append(original,at,from[n]);String value=proposal.value(n);
            changed.append(value.equals(initial[n])?original.substring(from[n],to[n]):
                proposal.kind(n)==LuaInsert.Kind.STRING?LuaInsert.quote(value):value);at=to[n];
        }
        return changed.append(original.substring(at)).toString();
    }
    public String replacement(String source,LuaInsert proposal){
        if(end>source.length()||!source.substring(start,end).equals(original))throw new IllegalArgumentException("Строка уже изменилась. Открой параметры заново; правка не применена.");
        String changed=preview(proposal);
        // New delimiters must not escape the single call or silently add/remove arguments.
        LuaCall check=parse(changed,0,changed.length());if(!check.name.equals(name)||check.from.length!=from.length)throw unsupported();
        if(name.startsWith("rule:"))for(int n=0;n<from.length;n++)
            if(!proposal.value(n).trim().equals(check.form.value(n)))throw unsupported();
        return source.substring(0,start)+changed+source.substring(end);
    }
}
