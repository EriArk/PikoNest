package art.pikoos.lab.core;

import java.util.*;

/** Read-only outline of one branch. Nested/multiline code stays grouped, never flattened. */
public final class LuaBranchActions {
    public static final class Entry {
        public final int line,end;
        public final String title,source;
        public final boolean fields;
        Entry(int line,int end,String title,String source,boolean fields){this.line=line;this.end=end;this.title=title;this.source=source;this.fields=fields;}
    }
    public final int header;
    public final String title,scope;
    public final List<Entry> entries;
    public int index;
    public LuaBranchActions(String source,int header,int selectedLine){
        LuaBranches outline=new LuaBranches(source,header);LuaBranches.Branch branch=outline.atHeader(header);
        if(branch==null)throw new IllegalArgumentException("Ветвь изменилась. Выбери её заново.");
        this.header=header;title=branch.title;scope=branch.scope;
        LuaContext context=new LuaContext(source);String[] raw=source.split("\\r\\n|\\r|\\n",-1),mask=context.masked().split("\\r\\n|\\r|\\n",-1);
        int[] starts=new int[raw.length];int at=0;
        for(int n=0;n<raw.length;n++){starts[n]=at;at+=raw[n].length();if(at<source.length()&&source.charAt(at)=='\r')at++;if(at<source.length()&&source.charAt(at)=='\n')at++;}
        List<Entry> result=new ArrayList<>();
        for(int n=header+1;n<branch.end;){
            if(mask[n].trim().isEmpty()){n++;continue;}
            int end=outline.blockEnds.containsKey(n)?outline.blockEnds.get(n):n+1;boolean block=end>n+1;
            if(!block){
                int depth=0;
                for(int row=n;row<branch.end;row++){
                    for(char c:mask[row].toCharArray()){if(c=='('||c=='['||c=='{')depth++;if(c==')'||c==']'||c=='}')depth--;}
                    end=row+1;
                    boolean continued=mask[row].trim().matches(".*(?:[,+*/%=<>~^-]|\\band|\\bor|\\bnot|\\.\\.)$");
                    if(depth<=0&&!continued&&(end>=raw.length||context.allowsLine(starts[end])))break;
                }
            }
            end=Math.min(end,branch.end);boolean fields=false;String label=block?"Блок · "+raw[n].trim():"Lua · "+raw[n].trim();
            if(end==n+1&&!block)try{
                LuaCall call=LuaCall.parse(source,starts[n],starts[n]+raw[n].length());
                if(call.end<=starts[n]+raw[n].length()){fields=true;label=(call.form.item().fields.length>0?call.form.value(0)+" · ":"")+call.form.item().title;}
            }catch(IllegalArgumentException ignored){/* Unsupported source has an explicit code entry. */}
            int stop=end<starts.length?starts[end]:source.length();
            result.add(new Entry(n,end,label,source.substring(starts[n],stop),fields));n=end;
        }
        entries=Collections.unmodifiableList(result);
        for(int n=0;n<entries.size();n++)if(entries.get(n).line<=selectedLine)index=n;
    }
    public Entry current(){return entries.isEmpty()?null:entries.get(index);}
    public void move(int step){index=Math.max(0,Math.min(Math.max(0,entries.size()-1),index+step));}
}
