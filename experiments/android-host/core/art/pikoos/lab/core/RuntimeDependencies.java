package art.pikoos.lab.core;

import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Conservative literal-call collector. It does not certify arbitrary Lua programs. */
public final class RuntimeDependencies {
    private static final class Token {
        final String text;final boolean string;
        Token(String text,boolean string){this.text=text;this.string=string;}
    }
    public static List<String> collect(byte[] cart){
        P8Document doc=P8Document.parse(cart);StringBuilder code=new StringBuilder();
        for(int i=0;i<doc.sections().size();i++)if(doc.sections().get(i).name.equals("lua"))code.append(new String(doc.body(i),StandardCharsets.ISO_8859_1)).append('\n');
        // Catalogue inspection can precede include preparation. Paths are not Lua identifiers.
        List<Token> tokens=lex(code.toString().replaceAll("(?mi)^[ \\t]*#include[^\\r\\n]*",""));LinkedHashSet<String> paths=new LinkedHashSet<>();
        for(int i=0;i<tokens.size();i++){
            Token token=tokens.get(i);if(token.string)continue;String call=token.text;
            if(call.equals("save")||call.equals("cstore"))throw error("Запись через save/cstore в наборе картриджей пока не поддерживается");
            if(!call.equals("load")&&!call.equals("reload"))continue;
            if(i+1>=tokens.size()||!tokens.get(i+1).text.equals("("))throw error("Пока нужны прямые вызовы load/reload с именем файла");
            List<List<Token>> args=new ArrayList<>();List<Token> arg=new ArrayList<>();int depth=1,j=i+2;
            for(;j<tokens.size();j++){
                Token t=tokens.get(j);
                if(!t.string&&t.text.equals("("))depth++;
                if(!t.string&&t.text.equals(")")){if(--depth==0){if(!arg.isEmpty()||!args.isEmpty())args.add(arg);break;}}
                if(!t.string&&t.text.equals(",")&&depth==1){args.add(arg);arg=new ArrayList<>();}else arg.add(t);
            }
            if(depth!=0)throw error("Не удалось разобрать вызов "+call);
            int at=call.equals("load")?0:3;
            if(call.equals("reload")&&args.size()<=3)continue;
            if(args.size()<=at||args.size()>(at==0?3:4)||args.get(at).size()!=1||!args.get(at).get(0).string)
                throw error("Пока указывай имя файла строкой в "+call);
            paths.add(RuntimeFileSet.name(args.get(at).get(0).text));
        }
        return new ArrayList<>(paths);
    }
    public static RuntimeFileSet prepare(String entry,byte[] selected,PicoIncludes.Source source)throws Exception{
        RuntimeFileSet.name(entry);Map<String,byte[]> files=new LinkedHashMap<>();
        Map<String,byte[]> cache=new HashMap<>();cache.put(entry,selected);long[] readBytes={selected.length};
        PicoIncludes.Source snapshots=path->{
            if(cache.containsKey(path))return cache.get(path);
            byte[] bytes=source.read(path);
            if(bytes==null)throw error("Нет части игры: "+path);
            if(bytes.length>CartridgeImport.MAX_BYTES||(readBytes[0]+=bytes.length)>RuntimeFileSet.MAX_BYTES)throw error("Исходники набора превышают лимит PIKOOS 8 МиБ");
            cache.put(path,bytes.clone());return bytes;
        };
        ArrayDeque<String> queue=new ArrayDeque<>();queue.add(entry);long total=0;
        while(!queue.isEmpty()){
            String name=queue.remove();if(files.containsKey(name))continue;
            if(files.size()>=RuntimeFileSet.MAX_FILES)throw error("Лимит запуска PIKOOS: 32 связанных картриджа");
            byte[] raw=snapshots.read(name);
            if(raw==null||raw.length>CartridgeImport.MAX_BYTES)throw error("Не удалось прочитать часть игры: "+name);
            byte[] cart=PicoIncludes.prepare(raw,snapshots);
            if((total+=cart.length)>RuntimeFileSet.MAX_BYTES)throw error("Набор игры превышает 8 МиБ — лимит PIKOOS");
            files.put(name,cart);
            for(String dependency:collect(cart))if(!files.containsKey(dependency)&&!queue.contains(dependency))queue.add(dependency);
        }
        return new RuntimeFileSet(entry,files);
    }
    private static List<Token> lex(String s){
        List<Token> out=new ArrayList<>();
        for(int i=0;i<s.length();){
            char c=s.charAt(i);if(Character.isWhitespace(c)){i++;continue;}
            boolean comment=s.startsWith("--",i);if(comment)i+=2;
            int start=i;
            if(i<s.length()&&s.charAt(i)=='['){
                int k=i+1;while(k<s.length()&&s.charAt(k)=='=')k++;
                if(k<s.length()&&s.charAt(k)=='['){String close="]"+s.substring(i+1,k)+"]";int end=s.indexOf(close,k+1);
                    if(end<0)throw error("Незавершённая строка или комментарий");
                    if(!comment)out.add(new Token(s.substring(k+1,end),true));i=end+close.length();continue;}
            }
            if(comment){while(i<s.length()&&s.charAt(i)!='\n'&&s.charAt(i)!='\r')i++;continue;}
            if(c=='\''||c=='"'){
                StringBuilder value=new StringBuilder();boolean escaped=false,closed=false;i++;
                while(i<s.length()){char next=s.charAt(i++);if(next==c){closed=true;break;}if(next=='\\'){escaped=true;if(i<s.length())value.append(s.charAt(i++));}else value.append(next);}
                if(!closed)throw error("Незавершённая строка");
                // Escapes in a file literal are outside this preparer's accepted syntax.
                out.add(new Token(escaped?"!escaped!":value.toString(),true));continue;
            }
            if(Character.isLetter(c)||c=='_'){i++;while(i<s.length()&&(Character.isLetterOrDigit(s.charAt(i))||s.charAt(i)=='_'))i++;out.add(new Token(s.substring(start,i),false));}
            else{out.add(new Token(String.valueOf(c),false));i++;}
        }
        return out;
    }
    private static IllegalArgumentException error(String message){return new IllegalArgumentException(message);}
}
