package art.pikoos.lab.core;

import java.util.*;
import java.util.regex.*;

/** Exact recognition of our ordinary Lua draw block. Never normalizes unfamiliar Lua. */
public final class AnimationEdit {
    public final int start,end,firstLine,lastLine;
    public final String original,indent,newline;
    public final SpriteAnimation initial;
    private final boolean trailingNewline;
    private AnimationEdit(String source,int start,int end,int first,int last,String indent,String newline,SpriteAnimation initial){
        this.start=start;this.end=end;firstLine=first;lastLine=last;this.indent=indent;this.newline=newline;this.initial=initial;
        original=source.substring(start,end);trailingNewline=original.endsWith("\n")||original.endsWith("\r");
    }
    private static IllegalArgumentException unsupported(){return new IllegalArgumentException("Этот блок анимации изменён вручную или не поддерживается формой. Продолжай в Lua; код сохранён.");}
    public static AnimationEdit find(String source,int at){
        ArrayList<String> lines=new ArrayList<>();ArrayList<Integer> offsets=new ArrayList<>();
        Matcher rows=Pattern.compile("[^\\r\\n]*(?:\\r\\n|\\r|\\n|$)").matcher(source);
        while(rows.find()&&rows.start()<source.length()){offsets.add(rows.start());lines.add(rows.group());}
        LuaContext context=new LuaContext(source);
        for(int n=0;n+1<lines.size();n++){
            String head=lines.get(n);Matcher open=Pattern.compile("([ \\t]*)do(\\r\\n|\\r|\\n)").matcher(head);
            if(!open.matches()||!context.allowsLine(offsets.get(n)))continue;
            String indent=open.group(1),newline=open.group(2);
            if(!stripEnd(lines.get(n+1)).equals(indent+" -- animation: sx,sy,w,h,end_seconds"))continue;
            int last=n+2;while(last<lines.size()&&last<n+64&&!stripEnd(lines.get(last)).equals(indent+"end"))last++;
            int end=last<lines.size()?offsets.get(last)+lines.get(last).length():source.length();
            if(at<offsets.get(n)||at>=end)continue;
            if(last>=lines.size()||last>=n+64)throw unsupported();
            StringBuilder normalized=new StringBuilder();
            for(int i=n;i<=last;i++){
                String row=stripEnd(lines.get(i));if(!row.startsWith(indent))throw unsupported();
                // A single block must not mix line endings; opening it must be lossless.
                if(i<last&&!lines.get(i).equals(row+newline))throw unsupported();
                if(i==last&&!lines.get(i).equals(row)&&!lines.get(i).equals(row+newline))throw unsupported();
                normalized.append(row.substring(indent.length())).append('\n');
            }
            SpriteAnimation form=parse(normalized.toString());
            return new AnimationEdit(source,offsets.get(n),end,n+1,last+1,indent,newline,form);
        }
        return null;
    }
    private static String stripEnd(String row){return row.replaceFirst("[\\r\\n]+$","");}
    private static SpriteAnimation parse(String code){
        try{
            Matcher frame=Pattern.compile("(?m)^  \\{([0-9]+),([0-9]+),([0-9]+),([0-9]+),0x([0-9a-f]+)\\.([0-9a-f]{4})\\},$").matcher(code);
            ArrayList<SpriteAnimation.Frame> frames=new ArrayList<>();int previous=0;
            while(frame.find()){
                long fixed=Long.parseLong(frame.group(5),16)*65536+Long.parseLong(frame.group(6),16);
                if(fixed<0||fixed>160L*65536)throw unsupported();
                int total=(int)((fixed*1000+32768)/65536),ms=total-previous;
                if(ms<50||ms>5000||ms%50!=0)throw unsupported();
                frames.add(new SpriteAnimation.Frame(new SpriteRegion(Integer.parseInt(frame.group(1)),Integer.parseInt(frame.group(2)),Integer.parseInt(frame.group(3)),Integer.parseInt(frame.group(4))),ms));previous=total;
            }
            Matcher position=Pattern.compile("(?m)^    (-?[0-9]+),(-?[0-9]+)\\)$").matcher(code);
            if(frames.isEmpty()||frames.size()>SpriteAnimation.MAX_FRAMES||!position.find())throw unsupported();
            int x=Integer.parseInt(position.group(1)),y=Integer.parseInt(position.group(2));
            if(x<-127||x>127||y<-127||y>127||position.find())throw unsupported();
            SpriteAnimation result=SpriteAnimation.fromFrames(frames,x,y,code.contains("\n elapsed%="));
            if(!result.code().equals(code))throw unsupported(); // Whole body, not just data or a marker.
            return result;
        }catch(IllegalArgumentException e){throw unsupported();}
    }
    public String replacement(String source,SpriteAnimation proposed){
        if(start<0||end>source.length()||!source.substring(start,end).equals(original))
            throw new IllegalArgumentException("Код анимации изменился. Закрой форму и открой её снова; правка не применена.");
        if(initial.code().equals(proposed.code()))return source;
        SpriteAnimation.validateSource(source);
        StringBuilder block=new StringBuilder();String[] rows=proposed.code().split("\n");
        for(int n=0;n<rows.length;n++){block.append(indent).append(rows[n]);if(n+1<rows.length||trailingNewline)block.append(newline);}
        return source.substring(0,start)+block+source.substring(end);
    }
}
