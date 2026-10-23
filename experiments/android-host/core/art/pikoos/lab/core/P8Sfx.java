package art.pikoos.lab.core;

import art.pikoos.p8.P8Document;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Ordinary text-cart SFX data. Targeted edits preserve every unchanged hex digit. */
public final class P8Sfx {
    public static final int COUNT=64,NOTES=32;
    public static final String[] INSTRUMENTS={"Triangle","Tilted saw","Saw","Square","Pulse","Organ","Noise","Phaser"};
    public static final String[] EFFECTS={"None","Slide","Vibrato","Drop","Fade in","Fade out","Arp fast","Arp slow"};
    public static final class Sound {
        public int mode,speed=16,loopStart,loopEnd;
        public final int[][] notes=new int[NOTES][4]; // pitch, instrument, volume, effect
        public Sound copy(){Sound s=new Sound();s.mode=mode;s.speed=speed;s.loopStart=loopStart;s.loopEnd=loopEnd;for(int n=0;n<NOTES;n++)s.notes[n]=notes[n].clone();return s;}
        public boolean editable(){if(mode<0||mode>1||speed<1||speed>255||loopStart<0||loopStart>32||loopEnd<0||loopEnd>32)return false;for(int[] n:notes)if(n[0]<0||n[0]>63||n[1]<0||n[1]>15||n[2]<0||n[2]>7||n[3]<0||n[3]>7)return false;return true;}
        public void validate(){if(!editable())throw new IllegalArgumentException("This sound uses filters, waveform data or settings this editor cannot safely edit yet. Its source is preserved.");}
        public boolean blank(){if(mode>1||loopStart!=0||loopEnd!=0)return false;for(int[] n:notes)for(int v:n)if(v!=0)return false;return speed==0||speed==1||speed==16;}
        public int audible(){int count=0;for(int[] n:notes)if(n[2]>0)count++;return count;}
        public String line(){StringBuilder b=new StringBuilder();hex(b,mode,2);hex(b,speed,2);hex(b,loopStart,2);hex(b,loopEnd,2);for(int[] note:notes){hex(b,note[0],2);for(int f=1;f<4;f++)hex(b,note[f],1);}return b.toString();}
        private static void hex(StringBuilder b,int v,int width){if(v<0||v>=(1<<(width*4)))throw new IllegalArgumentException("Sound value outside its stored range");String s=Integer.toHexString(v);if(s.length()<width)b.append('0');b.append(s);}
        public static Sound parse(String row){if(row.length()!=168||!row.matches("[0-9a-fA-F]+"))throw new IllegalArgumentException("Unsupported SFX row. Source preserved.");Sound s=new Sound();s.mode=read(row,0,2);s.speed=read(row,2,2);s.loopStart=read(row,4,2);s.loopEnd=read(row,6,2);for(int n=0;n<32;n++){int at=8+n*5;s.notes[n][0]=read(row,at,2);for(int f=1;f<4;f++)s.notes[n][f]=read(row,at+1+f,1);}return s;}
        private static int read(String s,int at,int length){return Integer.parseInt(s.substring(at,at+length),16);}
    }
    private final P8Document document;
    private final int section;
    private final String body,newline;
    private final List<Integer> starts=new ArrayList<>();
    private final List<Sound> sounds=new ArrayList<>();
    public P8Sfx(P8Document doc){
        document=doc;int found=-1;for(P8Document.Section s:doc.sections())if(s.name.equals("sfx"))found=doc.uniqueSection("sfx");section=found;
        body=found<0?"":new String(doc.body(found),StandardCharsets.US_ASCII);newline=new String(doc.bytes(),StandardCharsets.ISO_8859_1).contains("\r\n")?"\r\n":"\n";
        int at=0;while(at<body.length()){if(sounds.size()==64||at+168>body.length())throw new IllegalArgumentException("Unsupported SFX section. Source preserved.");starts.add(at);sounds.add(Sound.parse(body.substring(at,at+168)));at+=168;if(at<body.length()){if(body.charAt(at)=='\r')at++;if(at>=body.length()||body.charAt(at++)!='\n')throw new IllegalArgumentException("Unsupported SFX line ending.");}}
    }
    public Sound get(int slot){check(slot);if(slot<sounds.size())return sounds.get(slot).copy();Sound s=new Sound();if(slot==0)s.speed=1;return s;}
    public P8Document withSound(int slot,Sound sound){
        Sound before=get(slot);String wanted=sound.line();if(before.line().equals(wanted))return document;sound.validate();if(!before.editable()&&!before.blank())throw new IllegalArgumentException("This slot contains advanced sound data. Editing it is not supported yet; source preserved.");
        StringBuilder changed=new StringBuilder(body);int offset;
        if(slot<sounds.size())offset=starts.get(slot);
        else{if(changed.length()>0&&changed.charAt(changed.length()-1)!='\n')changed.append(newline);offset=changed.length();for(int n=sounds.size();n<=slot;n++){offset=changed.length();changed.append(get(n).line()).append(newline);}}
        for(int n=0;n<168;n++)if(Character.toLowerCase(changed.charAt(offset+n))!=wanted.charAt(n))changed.setCharAt(offset+n,wanted.charAt(n));
        if(section>=0)return document.edit(section,0,body.length(),changed.toString().getBytes(StandardCharsets.US_ASCII));
        String source=new String(document.bytes(),StandardCharsets.ISO_8859_1);if(!source.endsWith("\n")&&!source.endsWith("\r"))source+=newline;
        return P8Document.parse((source+"__sfx__"+newline+changed).getBytes(StandardCharsets.ISO_8859_1));
    }
    /** Reserve even muted music references; raw data/unknown sections never imply a free slot. */
    public boolean[] referencedByResources(){
        boolean[] used=new boolean[64];for(int n=0;n<8;n++)used[n]=true;
        for(Sound sound:sounds)for(int[] note:sound.notes)if(note[1]>=8)used[note[1]-8]=true;
        for(P8Document.Section s:document.sections())if(s.name.equals("music")){
            String music=new String(document.body(document.uniqueSection("music")),StandardCharsets.US_ASCII);int count=0;
            for(String line:music.split("\\r?\\n")){if(line.isEmpty())continue;if(++count>64||!line.matches("[0-9a-fA-F]{2} [0-9a-fA-F]{8}"))throw new IllegalArgumentException("Music references need review before allocating a sound.");for(int n=0;n<4;n++)used[Integer.parseInt(line.substring(3+n*2,5+n*2),16)&63]=true;}
        }return used;
    }
    public int musicUses(int slot){check(slot);int count=0;for(P8Document.Section s:document.sections())if(s.name.equals("music")){String music=new String(document.body(document.uniqueSection("music")),StandardCharsets.US_ASCII);for(String line:music.split("\\r?\\n")){if(line.isEmpty())continue;if(!line.matches("[0-9a-fA-F]{2} [0-9a-fA-F]{8}"))return -1;for(int n=0;n<4;n++)if((Integer.parseInt(line.substring(3+n*2,5+n*2),16)&63)==slot)count++;}}return count;}
    public int instrumentUses(int slot){if(slot>=8)return 0;int count=0;for(Sound s:sounds)for(int[] note:s.notes)if(note[1]==slot+8)count++;return count;}
    public static String pitch(int value){return new String[]{"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"}[value%12]+value/12;}
    public static String instrument(int value){return value<8?INSTRUMENTS[value]:"SFX instrument "+(value-8);}
    private static void check(int slot){if(slot<0||slot>=64)throw new IllegalArgumentException("SFX slot must be 0..63");}
}
