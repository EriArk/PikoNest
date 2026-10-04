package art.pikoos.lab.core;

import java.util.*;
import java.util.regex.Pattern;

/** Conservative outline of complete, line-delimited Lua blocks; never rewrites source. */
public final class LuaBranches {
    public static final class Branch {
        public final String title,indent,scope;
        public final int header;
        public int end,first;
        public boolean terminal;
        Branch(String title,String row,int header,String scope){
            this.title=title;this.header=header;this.scope=scope;int n=0;
            while(n<row.length()&&(row.charAt(n)==' '||row.charAt(n)=='\t'))n++;
            indent=row.substring(0,n)+"  ";
        }
    }
    private static final class Block {
        final String kind,condition;final int start;Branch branch;boolean otherwise;
        Block(String kind,String condition,int start){this.kind=kind;this.condition=condition;this.start=start;}
    }
    private static final Pattern BLOCK_WORD=Pattern.compile("\\b(if|then|else|elseif|end|function|for|while|do|repeat|until)\\b");
    public final List<Branch> entries;
    final Map<Integer,Integer> blockEnds=new HashMap<>();
    public final String warning;
    public int index;
    public LuaBranches(String source,int cursorLine){
        List<Branch> found=new ArrayList<>();String problem="";
        try{
            LuaContext context=new LuaContext(source);
            if(!context.allowsLine(source.length()))throw new IllegalArgumentException();
            String[] rows=source.split("\\r\\n|\\r|\\n",-1),mask=context.masked().split("\\r\\n|\\r|\\n",-1);
            Deque<Block> stack=new ArrayDeque<>();
            for(int n=0;n<mask.length;n++){
                String row=mask[n].trim();if(row.isEmpty())continue;
                if(row.startsWith("#include"))throw new IllegalArgumentException();
                // Complete one-line helpers do not change the surrounding block stack.
                // Their own bodies remain a source-editor operation in this slice.
                if(inlineBlock(row))continue;
                if(row.startsWith("if ")||row.startsWith("if(")){
                    condition(row,"if");Block b=new Block("if",label(rows[n],row,"if"),n);
                    b.branch=new Branch("Если · "+b.condition,rows[n],n,scope(stack));stack.push(b);found.add(b.branch);
                }else if(row.startsWith("elseif ")||row.startsWith("elseif(")){
                    condition(row,"elseif");Block b=top(stack,"if");if(b.otherwise)throw new IllegalArgumentException();
                    close(b.branch,n,mask);b.branch=new Branch("Иначе если · "+label(rows[n],row,"elseif"),rows[n],n,b.branch.scope);found.add(b.branch);
                }else if(row.equals("else")){
                    Block b=top(stack,"if");if(b.otherwise)throw new IllegalArgumentException();b.otherwise=true;
                    close(b.branch,n,mask);b.branch=new Branch("Иначе · "+b.condition,rows[n],n,b.branch.scope);found.add(b.branch);
                }else if(row.equals("end")){
                    if(stack.isEmpty()||stack.peek().kind.equals("repeat"))throw new IllegalArgumentException();
                    Block b=stack.pop();blockEnds.put(b.start,n+1);if(b.branch!=null)close(b.branch,n,mask);
                }else if(row.matches("(?:local\\s+)?function\\s+[A-Za-z_][A-Za-z_0-9.:]*\\s*\\([^()]*\\)"))stack.push(new Block("function",row.substring(row.indexOf("function")+8,row.indexOf('(')).trim(),n));
                else if((row.startsWith("for ")||row.startsWith("while ")||row.startsWith("while("))&&row.endsWith(" do")){
                    String middle=row.substring(row.startsWith("for ")?3:5,row.length()-3);
                    if(BLOCK_WORD.matcher(middle).find())throw new IllegalArgumentException();stack.push(new Block("loop","",n));
                }else if(row.equals("do")||row.equals("repeat"))stack.push(new Block(row,"",n));
                else if(row.startsWith("until ")||row.startsWith("until(")){
                    top(stack,"repeat");if(BLOCK_WORD.matcher(row.substring(5)).find())throw new IllegalArgumentException();blockEnds.put(stack.pop().start,n+1);
                }else{
                    if(BLOCK_WORD.matcher(row).find())throw new IllegalArgumentException();
                    if(!stack.isEmpty()&&stack.peek().branch!=null&&Pattern.compile("\\b(return|break|goto)\\b").matcher(row).find())stack.peek().branch.terminal=true;
                }
            }
            if(!stack.isEmpty())throw new IllegalArgumentException();
        }catch(IllegalArgumentException e){found.clear();blockEnds.clear();problem="Нужны завершённые блоки с if/then, else и end на отдельных строках. Сокращённый Lua и #include открывай в коде.";}
        found.sort(Comparator.comparingInt(b->b.header));entries=Collections.unmodifiableList(found);warning=problem;
        for(int n=0;n<entries.size();n++)if(entries.get(n).header<=cursorLine&&cursorLine<=entries.get(n).end)index=n;
    }
    private static String condition(String row,String keyword){
        if(!row.endsWith(" then"))throw new IllegalArgumentException();String middle=row.substring(keyword.length(),row.length()-5).trim();
        if(middle.isEmpty()||BLOCK_WORD.matcher(middle).find())throw new IllegalArgumentException();return middle;
    }
    private static String label(String original,String masked,String keyword){return original.trim().substring(keyword.length(),masked.length()-5).trim();}
    private static boolean inlineBlock(String row){
        if(!row.matches("(?:(?:local\\s+)?function|if|for|while|do|repeat)\\b.*"))return false;
        Deque<String> blocks=new ArrayDeque<>();java.util.regex.Matcher words=Pattern.compile("\\b(if|then|else|elseif|end|function|for|while|do|repeat|until|return|break|goto)\\b").matcher(row);boolean closed=false;
        while(words.find()){
            String word=words.group(),top=blocks.peek();
            switch(word){
                case "if":blocks.push("if?");break;
                case "then":if(!"if?".equals(top))return false;blocks.pop();blocks.push("if");break;
                case "elseif":if(!"if".equals(top))return false;blocks.pop();blocks.push("if?");break;
                case "else":if(!"if".equals(top))return false;blocks.pop();blocks.push("else");break;
                case "function":blocks.push("function");break;
                case "for":case "while":blocks.push("loop?");break;
                case "do":if("loop?".equals(top)){blocks.pop();blocks.push("loop");}else blocks.push("do");break;
                case "repeat":blocks.push("repeat");break;
                case "until":if(!"repeat".equals(top))return false;blocks.pop();closed=true;break;
                case "end":if(top==null||top.endsWith("?")||top.equals("repeat"))return false;blocks.pop();closed=true;break;
                case "return":case "break":case "goto":if(blocks.isEmpty())return false;break;
                default:return false;
            }
        }
        return closed&&blocks.isEmpty();
    }
    private static String scope(Deque<Block> stack){for(Block b:stack)if(b.kind.equals("function"))return b.condition;return "Начало Lua";}
    private static Block top(Deque<Block> stack,String kind){if(stack.isEmpty()||!stack.peek().kind.equals(kind))throw new IllegalArgumentException();return stack.peek();}
    private static void close(Branch b,int end,String[] mask){b.end=end;b.first=b.header;for(int n=b.header+1;n<end;n++)if(!mask[n].trim().isEmpty()){b.first=n;break;}}
    public Branch current(){return entries.isEmpty()?null:entries.get(index);}
    public void move(int delta){index=Math.max(0,Math.min(Math.max(0,entries.size()-1),index+delta));}
    public Branch atHeader(int line){for(Branch b:entries)if(b.header==line)return b;return null;}
}
