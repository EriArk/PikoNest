package art.pikoos.lab.core;

import java.util.*;
import java.util.regex.*;

/** Source-order lexical outline of the current Lua body, not symbol resolution. */
public final class LuaNavigation {
    public static final class Entry {
        public final String name; public final int line;
        Entry(String name,int line){this.name=name;this.line=line;}
    }
    private static final class Token {
        final String text;final int line;
        Token(String text,int line){this.text=text;this.line=line;}
        boolean name(){return text.matches("[A-Za-z_][A-Za-z_0-9]*")&&!text.equals("function");}
    }
    public final List<Entry> entries;
    public final int lines;
    public final boolean truncated;
    public int tab,index,target,digit;
    public LuaNavigation(String source,int line){
        String mask=new LuaContext(source).masked();List<Token> tokens=new ArrayList<>();
        Matcher m=Pattern.compile("[A-Za-z_][A-Za-z_0-9]*|==|!=|~=|[^\\s]").matcher(mask);
        int scanned=0,row=0;
        while(tokens.size()<100000&&m.find()){
            while(scanned<m.start()){
                char c=mask.charAt(scanned++);
                if(c=='\r'){row++;if(scanned<mask.length()&&mask.charAt(scanned)=='\n')scanned++;}
                else if(c=='\n')row++;
            }
            tokens.add(new Token(m.group(),row));
        }
        truncated=tokens.size()==100000&&m.find();
        List<Entry> result=new ArrayList<>();
        for(int n=0;n<tokens.size();n++)if(tokens.get(n).text.equals("function")){
            int at=n+1;StringBuilder name=new StringBuilder();
            if(at<tokens.size()&&tokens.get(at).name()){
                name.append(tokens.get(at++).text);
                while(at+1<tokens.size()&&(tokens.get(at).text.equals(".")||tokens.get(at).text.equals(":"))&&tokens.get(at+1).name()){
                    name.append(tokens.get(at).text).append(tokens.get(at+1).text);at+=2;
                }
            }else if(n>=2&&tokens.get(n-1).text.equals("=")&&tokens.get(n-2).name()){
                int start=n-2;
                while(start>=2&&tokens.get(start-1).text.equals(".")&&tokens.get(start-2).name()){
                    start-=2;
                }
                for(int i=start;i<=n-2;i++)name.append(tokens.get(i).text);
            }
            if(name.length()>0&&at<tokens.size()&&tokens.get(at).text.equals("("))
                result.add(new Entry(name.toString(),tokens.get(n).line));
        }
        entries=Collections.unmodifiableList(result);
        int count=1;for(int n=0;n<source.length();n++)if(source.charAt(n)=='\r'){count++;if(n+1<source.length()&&source.charAt(n+1)=='\n')n++;}else if(source.charAt(n)=='\n')count++;
        lines=count;target=Math.max(1,Math.min(lines,line+1));
        for(int n=0;n<entries.size();n++)if(entries.get(n).line<=line)index=n;
        if(entries.isEmpty())tab=1;
    }
    public int selectedLine(){return tab==0&&!entries.isEmpty()?entries.get(index).line:target-1;}
    public int digits(){return Integer.toString(lines).length();}
    public void move(int dx,int dy){
        if(tab==0)index=Math.max(0,Math.min(Math.max(0,entries.size()-1),index+dy+dx*6));
        else{
            digit=Math.max(0,Math.min(digits()-1,digit-dx));
            int step=1;for(int n=0;n<digit;n++)step*=10;
            target=(int)Math.max(1,Math.min(lines,(long)target+dy*step));
        }
    }
}
