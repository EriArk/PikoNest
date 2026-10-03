package art.pikoos.lab.core;

import java.util.regex.*;

/** Bounded textual evidence from an official-runtime trial, never a whole-game certificate. */
public final class RuntimeDiagnostic {
    public enum Kind { ERROR, OBSERVED, UNKNOWN, CANCELLED }
    public final Kind kind;
    public final String message,log;
    public final int line,tab;
    private RuntimeDiagnostic(Kind kind,String message,String log,int line,int tab){this.kind=kind;this.message=message;this.log=log;this.line=line;this.tab=tab;}
    public static RuntimeDiagnostic read(String log,boolean cancelled,boolean completed,boolean windowEnded,String source){
        if(log==null)log="";if(log.length()>32768)log=log.substring(0,32768);
        if(cancelled)return new RuntimeDiagnostic(Kind.CANCELLED,"Проверка отменена",log,-1,-1);
        String[] rows=log.split("\\r\\n|\\r|\\n",-1),code=source.split("\\r\\n|\\r|\\n",-1);
        Pattern header=Pattern.compile("^(?:syntax|runtime) error line ([0-9]+) ?\\(?tab ([0-9]+)\\)?$");
        for(int n=0;n<rows.length;n++){
            Matcher m=header.matcher(rows[n]);if(!m.matches())continue;
            int line,tab;try{line=Integer.parseInt(m.group(1));tab=Integer.parseInt(m.group(2));}catch(NumberFormatException ex){continue;}
            String message=n+2<rows.length?rows[n+2]:rows[n];
            // This first slice maps only tab 0, and only with the runtime's exact source excerpt.
            int mapped=tab==0&&line>0&&line<=code.length&&n+1<rows.length&&rows[n+1].equals(code[line-1])?line-1:-1;
            return new RuntimeDiagnostic(Kind.ERROR,message,log,mapped,tab);
        }
        boolean ran=log.contains("RUNNING: /work/game.p8\n")||log.contains("RUNNING: /work/game.p8\r\n");
        boolean unfamiliar=Pattern.compile("(?i)error|failed|cannot|exception|unavailable").matcher(log).find();
        return ran&&!unfamiliar&&log.length()<32768&&(completed||windowEnded)?new RuntimeDiagnostic(Kind.OBSERVED,"В коротком запуске ошибок не получено",log,-1,-1)
            :new RuntimeDiagnostic(Kind.UNKNOWN,"Не удалось подтвердить результат проверки",log,-1,-1);
    }
}
