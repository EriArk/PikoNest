package art.pikoos.lab.core;

import java.util.*;
import java.util.regex.*;

/** Conservative rule forms, not a complete PICO-8 parser. Unsupported source stays literal. */
final class LuaRule {
    private static final Pattern NUMBER=Pattern.compile("(?:0[xX][0-9a-fA-F]+(?:\\.[0-9a-fA-F]+)?|0[bB][01]+(?:\\.[01]+)?|(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+))");
    private static final String KEYWORDS=" and break do else elseif end false for function if in local nil not or repeat return then true until while ";
    private static final class Token {
        final String text;final int from,to;final boolean value;
        Token(String text,int from,int to,boolean value){this.text=text;this.from=from;this.to=to;this.value=value;}
        boolean name(){return text.matches("[a-zA-Z_][a-zA-Z_0-9]*")&&!KEYWORDS.contains(" "+text+" ");}
    }
    private static IllegalArgumentException unsupported(){return new IllegalArgumentException("Для этой строки формы пока нет. Вернись к коду и выбери «Ввод».");}
    private static List<Token> tokens(String raw){
        ArrayList<Token> out=new ArrayList<>();int at=0;
        while(at<raw.length()){
            char c=raw.charAt(at);if(c==' '||c=='\t'){at++;continue;}
            if(raw.startsWith("--",at)){
                // Block comments can resume code on this line: do not guess their ownership.
                if(raw.substring(at+2).matches("(?s)^\\[=*\\[.*"))throw unsupported();
                break;
            }
            int start=at;boolean value=false;
            if(c=='\''||c=='"'){
                at++;boolean closed=false;
                while(at<raw.length()){
                    char next=raw.charAt(at++);
                    if(next=='\\'){if(at==raw.length())throw unsupported();at++;}
                    else if(next==c){closed=true;break;}
                }
                if(!closed)throw unsupported();value=true;
            }else if(Character.isDigit(c)||(c=='.'&&at+1<raw.length()&&Character.isDigit(raw.charAt(at+1)))){
                Matcher m=NUMBER.matcher(raw);m.region(at,raw.length());if(!m.lookingAt())throw unsupported();at=m.end();value=true;
            }else if((c>='a'&&c<='z')||(c>='A'&&c<='Z')||c=='_'){
                at++;while(at<raw.length()&&((raw.charAt(at)>='a'&&raw.charAt(at)<='z')||(raw.charAt(at)>='A'&&raw.charAt(at)<='Z')||Character.isDigit(raw.charAt(at))||raw.charAt(at)=='_'))at++;
            }else{
                String op=null;for(String candidate:new String[]{"!=","~=","==","<=",">=","+=",".."})if(raw.startsWith(candidate,at)){op=candidate;break;}
                if(op!=null)at+=op.length();else if("()[],.+-*/%^#<>=;".indexOf(c)>=0)at++;else throw unsupported();
            }
            out.add(new Token(raw.substring(start,at),start,at,value));
        }
        return out;
    }
    static LuaCall parse(String raw,int start,int end){
        // Leave unrelated call forms on their established parser, including its richer arguments.
        if(!raw.matches("(?s)^[ \\t]*(?:(?:if|elseif|local)\\b.*|[a-zA-Z_][a-zA-Z_0-9]*[ \\t]*(?:=|\\+=).*)"))return null;
        List<Token> t=tokens(raw);int size=t.size();if(size==0)throw unsupported();
        String head=t.get(0).text;int item;String name;int[] from,to;
        if(head.equals("if")||head.equals("elseif")){
            if(size<3||!t.get(size-1).text.equals("then"))throw unsupported();
            expression(t,1,size-1);
            int depth=0,comparison=-1;boolean simple=true;
            for(int n=1;n<size-1;n++){
                String v=t.get(n).text;
                if(v.equals("(")||v.equals("["))depth++;
                else if(v.equals(")")||v.equals("]"))depth--;
                else if(depth==0){
                    if(v.equals("and")||v.equals("or"))simple=false;
                    if(Arrays.asList(LuaInsert.COMPARISONS).contains(v)){if(comparison>=0)simple=false;comparison=n;}
                }
            }
            if(simple&&comparison>1&&comparison<size-2){
                expression(t,1,comparison);expression(t,comparison+1,size-1);
                item=17;from=new int[]{t.get(1).from,t.get(comparison).from,t.get(comparison+1).from};
                to=new int[]{t.get(comparison-1).to,t.get(comparison).to,t.get(size-2).to};
            }else{item=4;from=new int[]{t.get(1).from};to=new int[]{t.get(size-2).to};}
            name="rule:"+head;
        }else{
            int at=head.equals("local")?1:0;
            if(size<at+3||!t.get(at).name())throw unsupported();
            String op=t.get(at+1).text;if(!op.equals("=")&&!op.equals("+="))throw unsupported();
            if(at==1&&!op.equals("="))throw unsupported();
            if(t.get(size-1).text.equals(";"))size--;
            expression(t,at+2,size);
            item=op.equals("=")?7:8;name="rule:"+(at==1?"local":"assign")+op;
            from=new int[]{t.get(at).from,t.get(at+2).from};to=new int[]{t.get(at).to,t.get(size-1).to};
        }
        LuaInsert form=new LuaInsert();form.choose(item);form.editing=true;form.screen=LuaInsert.Screen.FIELDS;
        for(int n=0;n<from.length;n++)form.set(n,raw.substring(from[n],to[n]));
        return new LuaCall(start,end,raw,name,from,to,form);
    }
    private static void expression(List<Token> t,int from,int to){
        Expression e=new Expression(t,from,to);e.read(0,0);if(e.at!=to)throw unsupported();
    }
    /** Bounded expression grammar: literals, names, calls/indexing and common Lua operators. */
    private static final class Expression {
        final List<Token> t;final int end;int at;
        Expression(List<Token> t,int at,int end){this.t=t;this.at=at;this.end=end;}
        String peek(){return at<end?t.get(at).text:"";}
        boolean take(String value){if(peek().equals(value)){at++;return true;}return false;}
        void require(String value){if(!take(value))throw unsupported();}
        void read(int minimum,int nesting){
            if(nesting>64||at>=end)throw unsupported();
            String token=peek();
            if(token.equals("not")||token.equals("-")||token.equals("#")){at++;read(7,nesting+1);}
            else{
                Token first=t.get(at);boolean suffix;
                if(take("(")){read(0,nesting+1);require(")");suffix=true;}
                else if(first.name()){at++;suffix=true;}
                else if(first.value||token.equals("true")||token.equals("false")||token.equals("nil")){at++;suffix=false;}
                else throw unsupported();
                while(suffix){
                    if(take(".")){if(at>=end||!t.get(at).name())throw unsupported();at++;}
                    else if(take("[")){read(0,nesting+1);require("]");}
                    else if(take("(")){if(!take(")")){read(0,nesting+1);while(take(","))read(0,nesting+1);require(")");}}
                    else break;
                }
            }
            while(at<end){
                String op=peek();int priority=priority(op);if(priority<minimum)return;
                at++;read(priority+(op.equals("^")||op.equals("..")?0:1),nesting+1);
            }
        }
        int priority(String s){
            if(s.equals("or"))return 1;if(s.equals("and"))return 2;
            if(Arrays.asList(LuaInsert.COMPARISONS).contains(s))return 3;
            if(s.equals(".."))return 4;if(s.equals("+")||s.equals("-"))return 5;
            if(s.equals("*")||s.equals("/")||s.equals("%"))return 6;if(s.equals("^"))return 8;return -1;
        }
    }
}
