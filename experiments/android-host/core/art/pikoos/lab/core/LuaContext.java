package art.pikoos.lab.core;

/** Conservative lexical masking only; never a Lua compiler or scope resolver. */
public final class LuaContext {
    private final String source,mask;
    private final boolean[] protectedAt;
    public LuaContext(String source){
        this.source=source;char[] result=source.toCharArray();protectedAt=new boolean[source.length()+1];
        int i=0;
        while(i<source.length()){
            int start=i,end=i;char c=source.charAt(i);String close=null;boolean unclosed=false;
            if(c=='\''||c=='"'){
                end=i+1;unclosed=true;
                while(end<source.length()){
                    char next=source.charAt(end++);
                    if(next=='\\'){if(end<source.length())end++;}
                    else if(next==c){unclosed=false;break;}
                }
            }else if(source.startsWith("--",i)){
                close=longClose(source,i+2);
                if(close==null){end=i+2;while(end<source.length()&&source.charAt(end)!='\n'&&source.charAt(end)!='\r')end++;}
                else {int at=source.indexOf(close,i+2+close.length());unclosed=at<0;end=unclosed?source.length():at+close.length();}
            }else if((close=longClose(source,i))!=null){int at=source.indexOf(close,i+close.length());unclosed=at<0;end=unclosed?source.length():at+close.length();}
            else {i++;continue;}
            for(int n=start;n<end;n++){if(result[n]!='\n'&&result[n]!='\r')result[n]=' ';if(n>start)protectedAt[n]=true;}
            // An unfinished string/comment protects the EOF insertion point too.
            if(unclosed)protectedAt[end]=true;
            i=end;
        }
        mask=new String(result);
    }
    private static String longClose(String s,int pos){
        if(pos>=s.length()||s.charAt(pos)!='[')return null;
        int n=pos+1;while(n<s.length()&&s.charAt(n)=='=')n++;
        return n<s.length()&&s.charAt(n)=='['?"]"+s.substring(pos+1,n)+"]":null;
    }
    public boolean allowsLine(int pos){return pos>=0&&pos<=source.length()&&!protectedAt[pos];}
    public boolean defines(String name){
        String n=java.util.regex.Pattern.quote(name);
        return java.util.regex.Pattern.compile("(?<![A-Za-z0-9_])(?:function\\s+"+n+"\\s*\\(|"+n+"\\s*=(?!=))").matcher(mask).find();
    }
}
