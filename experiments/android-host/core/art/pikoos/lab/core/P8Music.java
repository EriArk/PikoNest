package art.pikoos.lab.core;

import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Standard text-cart patterns. Muted channels retain their underlying SFX reference. */
public final class P8Music {
    public static final class Pattern {
        public int flags;
        public final int[] channels={65,66,67,68};
        public Pattern copy(){Pattern p=new Pattern();p.flags=flags;System.arraycopy(channels,0,p.channels,0,4);return p;}
        public boolean active(int channel){return (channels[channel]&64)==0;}
        public int sound(int channel){return channels[channel]&63;}
        public void mute(int channel,boolean muted){channels[channel]=sound(channel)|(muted?64:0);}
        public boolean silent(){for(int n:channels)if((n&64)==0)return false;return true;}
        public void validate(){if(flags<0||flags>7)throw new IllegalArgumentException("Unsupported music flags; original data kept.");for(int n:channels)if(n<0||n>127)throw new IllegalArgumentException("Unsupported channel data; original kept.");}
        public String line(){return String.format(Locale.ROOT,"%02x %02x%02x%02x%02x",flags,channels[0],channels[1],channels[2],channels[3]);}
        public static Pattern parse(String line){if(!line.matches("[0-9a-fA-F]{2} [0-9a-fA-F]{8}"))throw new IllegalArgumentException("Unsupported music row; original kept.");Pattern p=new Pattern();p.flags=Integer.parseInt(line.substring(0,2),16);for(int c=0;c<4;c++)p.channels[c]=Integer.parseInt(line.substring(3+c*2,5+c*2),16);return p;}
    }
    private final P8Document document;
    private final int section;
    private final String body,newline;
    private final List<Integer> starts=new ArrayList<>();
    private final List<Pattern> patterns=new ArrayList<>();
    public P8Music(P8Document doc){
        document=doc;int found=-1;for(P8Document.Section s:doc.sections())if(s.name.equals("music"))found=doc.uniqueSection("music");section=found;
        body=found<0?"":new String(doc.body(found),StandardCharsets.US_ASCII);newline=new String(doc.bytes(),StandardCharsets.ISO_8859_1).contains("\r\n")?"\r\n":"\n";
        int at=0;while(at<body.length()){if(patterns.size()==64||at+11>body.length())throw new IllegalArgumentException("Unsupported music section; original kept.");starts.add(at);patterns.add(Pattern.parse(body.substring(at,at+11)));at+=11;if(at<body.length()){if(body.charAt(at)=='\r')at++;if(at>=body.length()||body.charAt(at++)!='\n')throw new IllegalArgumentException("Unsupported music line ending.");}}
    }
    public Pattern get(int index){check(index);return index<patterns.size()?patterns.get(index).copy():new Pattern();}
    public P8Document withPattern(int index,Pattern pattern){
        Pattern before=get(index);String wanted=pattern.line();if(before.line().equals(wanted))return document;before.validate();pattern.validate();StringBuilder changed=new StringBuilder(body);int offset;
        if(index<patterns.size())offset=starts.get(index);else{if(changed.length()>0&&changed.charAt(changed.length()-1)!='\n')changed.append(newline);offset=changed.length();for(int n=patterns.size();n<=index;n++){offset=changed.length();changed.append(get(n).line()).append(newline);}}
        for(int n=0;n<11;n++)if(Character.toLowerCase(changed.charAt(offset+n))!=wanted.charAt(n))changed.setCharAt(offset+n,wanted.charAt(n));
        if(section>=0)return document.edit(section,0,body.length(),changed.toString().getBytes(StandardCharsets.US_ASCII));
        String source=new String(document.bytes(),StandardCharsets.ISO_8859_1);if(!source.endsWith("\n")&&!source.endsWith("\r"))source+=newline;return P8Document.parse((source+"__music__"+newline+changed).getBytes(StandardCharsets.ISO_8859_1));
    }
    /** Reachable pattern slots; timing remains entirely the official runtime's job. */
    public List<Integer> track(int start){
        check(start);List<Integer> result=new ArrayList<>();boolean[] seen=new boolean[64];int n=start;
        while(n<64&&!seen[n]){Pattern p=get(n);p.validate();seen[n]=true;result.add(n);if(p.silent()||(p.flags&4)!=0)break;
            if((p.flags&2)!=0){int back=n;while(back>0&&(get(back).flags&1)==0)back--;n=back;}else n++;
        }return result;
    }
    public void requireThreeParts(int start){for(int n:track(start))if(get(n).active(3))throw new IllegalArgumentException("Pattern "+n+" uses part 4. Choose the three-part game policy to reserve it for effects, or use four parts explicitly.");}
    private static void check(int index){if(index<0||index>63)throw new IllegalArgumentException("Pattern must be 0..63");}
}
