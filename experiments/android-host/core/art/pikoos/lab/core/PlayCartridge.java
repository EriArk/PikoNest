package art.pikoos.lab.core;

import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/** Read-only launch inspection, independent of every editor/template constraint. */
public final class PlayCartridge {
    public final String problem;
    public final byte[] cover;
    public PlayCartridge(String filename,byte[] bytes){
        String failure="";byte[] image=null;
        if(!filename.toLowerCase(Locale.ROOT).endsWith(".p8"))failure="Запуск .p8.png ещё не подключён";
        else try{
            P8Document doc=P8Document.parse(bytes);
            String code="";
            for(int i=0;i<doc.sections().size();i++){
                if(doc.sections().get(i).name.equals("lua"))code+=new String(doc.body(i),StandardCharsets.ISO_8859_1)+"\n";
                if(doc.sections().get(i).name.equals("label"))image=label(doc.body(i));
            }
            // Conservative hint, not a dependency analyzer: includes and file API references
            // (even in comments/strings) require the future dependency-aware backend.
            // reload() reloads the current snapshot and needs no sibling file.
            String inspected=code.replaceAll("\\breload\\s*\\(\\s*\\)","");
            if(Pattern.compile("(?i)#include\\b|\\b(load|reload|cstore|save)\\s*\\(").matcher(inspected).find())
                failure="Возможны внешние файлы · их запуск ещё не готов";
        }catch(IllegalArgumentException e){failure="Не удалось распознать текстовый .p8";}
        problem=failure;cover=image;
    }
    private static byte[] label(byte[] body){
        String[] rows=new String(body,StandardCharsets.US_ASCII).trim().split("\\r?\\n");
        if(rows.length!=128)return null;byte[] result=new byte[128*128];
        for(int y=0;y<128;y++){
            if(rows[y].length()!=128)return null;
            for(int x=0;x<128;x++){int color=Character.digit(rows[y].charAt(x),16);if(color<0)return null;result[y*128+x]=(byte)color;}
        }return result;
    }
}
