package art.pikoos.lab.core;

import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** A transient ordinary .p8 for one-level #include. Never writes source files. */
public final class PicoIncludes {
    public interface Source {byte[] read(String relativePath)throws Exception;}
    public static final int MAX_INCLUDES=32,MAX_INPUT_BYTES=8*1024*1024;
    private static final Pattern DIRECTIVE=Pattern.compile("[ \\t]*#include[ \\t]+([^ \\t\\r\\n]+)[ \\t]*");
    private static final Pattern HINT=Pattern.compile("(?i)#include\\b");
    public static boolean needed(byte[] cart){
        P8Document doc=P8Document.parse(cart);
        for(int i=0;i<doc.sections().size();i++)if(doc.sections().get(i).name.equals("lua")&&HINT.matcher(text(doc.body(i))).find())return true;
        return false;
    }
    public static String path(String reference){
        String filename=reference;int colon=filename.indexOf(':');
        if(colon>=0){
            if(!filename.substring(colon).matches(":[0-9]{1,3}"))throw problem("Не удалось разобрать номер вкладки #include");
            filename=filename.substring(0,colon);
            if(!filename.toLowerCase(Locale.ROOT).endsWith(".p8"))throw problem("Вкладку можно подключить только из .p8");
        }
        // Explicit bounded adapter scope. No absolute paths, URI tricks or parent traversal.
        if(filename.length()>240||!filename.matches("[A-Za-z0-9_./-]+")||filename.startsWith("/"))
            throw problem("Путь #include пока должен быть относительным, без пробелов и кириллицы");
        for(String part:filename.split("/",-1))if(part.isEmpty()||part.equals(".")||part.equals(".."))
            throw problem("#include за пределами папки игры пока не поддерживается");
        String lower=filename.toLowerCase(Locale.ROOT);
        if(!lower.endsWith(".lua")&&!lower.endsWith(".p8"))throw problem("Пока подключаем #include из .lua и .p8");
        if(filename.split("/").length>16)throw problem("Слишком глубокий путь #include для этой версии PIKOOS");
        return filename;
    }
    public static byte[] prepare(byte[] cart,Source source)throws Exception{
        if(cart.length>CartridgeImport.MAX_BYTES)throw problem("Картридж превышает лимит запуска PIKOOS 2 МиБ");
        if(!needed(cart))return cart.clone();
        P8Document doc=P8Document.parse(cart);int lua=doc.uniqueSection("lua");
        String code=text(doc.body(lua));StringBuilder output=new StringBuilder();
        Map<String,byte[]> cache=new HashMap<>();int count=0,total=cart.length;
        // Keep original line endings and byte positions outside the replaced directive.
        String masked=mask(code);
        for(int start=0;start<code.length();){
            int end=start;while(end<code.length()&&code.charAt(end)!='\n'&&code.charAt(end)!='\r')end++;
            int next=end;if(next<code.length()&&code.charAt(next++)=='\r'&&next<code.length()&&code.charAt(next)=='\n')next++;
            String line=code.substring(start,end),active=masked.substring(start,end);
            if(HINT.matcher(active).find()){
                Matcher match=DIRECTIVE.matcher(line);
                if(!match.matches())throw problem("Не удалось безопасно разобрать строку #include");
                if(++count>MAX_INCLUDES)throw problem("Лимит PIKOOS: 32 подключения #include за запуск");
                String ref=match.group(1),filename=path(ref);
                byte[] bytes=cache.get(filename);
                if(bytes==null){
                    bytes=source.read(filename);
                    if(bytes==null)throw problem("Нет файла #include: "+filename);
                    total+=bytes.length;
                    if(bytes.length>CartridgeImport.MAX_BYTES||total>MAX_INPUT_BYTES)throw problem("Файлы #include превышают лимит чтения PIKOOS");
                    cache.put(filename,bytes.clone());
                }
                String included=text(bytes);
                if(filename.toLowerCase(Locale.ROOT).endsWith(".p8")){
                    P8Document linked=P8Document.parse(bytes);included=text(linked.body(linked.uniqueSection("lua")));
                    int colon=ref.indexOf(':');
                    if(colon>=0){
                        String[] tabs=included.split("(?m)^-->8(?:\\r\\n|\\r|\\n|$)",-1);int tab=Integer.parseInt(ref.substring(colon+1));
                        if(tab>=tabs.length)throw problem("Нет вкладки #include: "+ref);
                        included=tabs[tab];
                    }
                }
                if(HINT.matcher(mask(included)).find())throw problem("Вложенный #include не разворачивается PICO-8");
                if(output.length()+(long)included.length()+2>CartridgeImport.MAX_BYTES)throw problem("Собранный картридж превышает лимит PIKOOS 2 МиБ");
                output.append(included);
                if(!included.endsWith("\n")&&!included.endsWith("\r"))output.append('\n');
            }else output.append(code,start,next);
            if(output.length()>CartridgeImport.MAX_BYTES)throw problem("Собранный картридж превышает лимит PIKOOS 2 МиБ");
            start=next;
        }
        if(cart.length-doc.body(lua).length+(long)output.length()>CartridgeImport.MAX_BYTES)throw problem("Собранный картридж превышает лимит PIKOOS 2 МиБ");
        return doc.edit(lua,0,doc.body(lua).length,output.toString().getBytes(StandardCharsets.ISO_8859_1)).bytes();
    }
    /** Only recognizes lexical exclusions; not a Lua parser or dependency analyzer. */
    private static String mask(String code){
        char[] result=code.toCharArray();int i=0;
        while(i<code.length()){
            int start=i;char c=code.charAt(i);boolean comment=code.startsWith("--",i);
            if(comment)i+=2;
            int bracket=i;
            if(i<code.length()&&code.charAt(i)=='['){
                int k=i+1;while(k<code.length()&&code.charAt(k)=='=')k++;
                if(k<code.length()&&code.charAt(k)=='['){
                    String close="]"+code.substring(i+1,k)+"]";int at=code.indexOf(close,k+1);
                    i=at<0?code.length():at+close.length();
                }
            }
            if(i==bracket){
                if(comment){while(i<code.length()&&code.charAt(i)!='\n'&&code.charAt(i)!='\r')i++;}
                else if(c=='\''||c=='"'){
                    i++;while(i<code.length()){char ch=code.charAt(i++);if(ch=='\\'&&i<code.length())i++;else if(ch==c)break;}
                }else{i++;continue;}
            }
            for(int j=start;j<i;j++)if(result[j]!='\r'&&result[j]!='\n')result[j]=' ';
        }
        return new String(result);
    }
    private static String text(byte[] bytes){return new String(bytes,StandardCharsets.ISO_8859_1);}
    private static IllegalArgumentException problem(String message){return new IllegalArgumentException(message);}
}
