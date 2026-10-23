import art.pikoos.lab.core.*;
import art.pikoos.p8.P8Document;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class SfxDataTest {
    static int checks;
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void same(byte[] a,byte[] b,String why){check(Arrays.equals(a,b),why);}
    static void refused(Runnable r){try{r.run();throw new AssertionError("unsafe edit accepted");}catch(IllegalArgumentException expected){checks++;}}
    static WorkshopCartridge cart(String nl,String body){return new WorkshopCartridge(("pico-8 cartridge // http://www.pico-8.com"+nl+"version 43"+nl+"__lua__"+nl+"-- keep me"+nl+"function _draw()"+nl+" cls(1)"+nl+"end"+nl+body).getBytes(StandardCharsets.ISO_8859_1));}
    public static void main(String[] args){
        for(String nl:new String[]{"\n","\r\n"}){
            WorkshopCartridge empty=cart(nl,"__meta__"+nl+"opaque data"+nl);P8Sfx.Sound sound=empty.sfx().get(12);
            same(empty.bytes(),empty.withSound(12,sound).bytes(),"reading/saving missing default leaves exact source");
            sound.notes[0]=new int[]{24,0,5,0};sound.notes[31]=new int[]{63,15,7,7};sound.speed=255;sound.loopStart=4;sound.loopEnd=32;
            WorkshopCartridge changed=empty.withSound(12,sound);P8Sfx.Sound read=changed.sfx().get(12);
            check(read.speed==255&&read.loopEnd==32&&Arrays.equals(read.notes[31],sound.notes[31]),"limits and custom instrument round trip");
            for(int slot=0;slot<64;slot++)if(slot!=12)check(changed.sfx().get(slot).line().equals(empty.sfx().get(slot).line()),"other SFX untouched");
            P8Document before=P8Document.parse(empty.bytes()),after=P8Document.parse(changed.bytes());for(P8Document.Section section:before.sections())same(before.body(before.uniqueSection(section.name)),after.body(after.uniqueSection(section.name)),"unrelated section preserved");
            same(changed.bytes(),changed.withSound(12,read).bytes(),"exact repeat write");
            String upper=read.line().toUpperCase(Locale.ROOT);WorkshopCartridge imported=cart(nl,"__sfx__"+nl+upper+nl+"__music__"+nl+"03 48494A4B"+nl+"__meta__"+nl+"opaque");
            P8Sfx.Sound edit=imported.sfx().get(0);edit.notes[1][2]=3;byte[] result=imported.withSound(0,edit).bytes(),source=imported.bytes();int differences=0;for(int n=0;n<source.length;n++)if(source[n]!=result[n])differences++;check(differences==1,"one note attribute changes one nibble, keeps uppercase and framing");
            boolean[] referenced=imported.sfx().referencedByResources();for(int n=8;n<12;n++)check(referenced[n],"muted music references reserved");check(imported.sfx().musicUses(8)==1&&imported.sfx().instrumentUses(7)==1,"dependency counts include custom instruments and muted channels");
            String advanced="80"+upper.substring(2);WorkshopCartridge wave=cart(nl,"__sfx__"+nl+advanced+nl);check(!wave.sfx().get(0).editable(),"advanced header preserved, not reinterpreted as notes");
            P8Sfx.Sound unsafe=wave.sfx().get(0);unsafe.mode=0;refused(()->wave.withSound(0,unsafe));same(wave.bytes(),wave.bytes(),"refusal leaves input exact");
            P8Sfx.Sound ordinary=new P8Sfx.Sound();ordinary.notes[0]=new int[]{12,6,4,3};WorkshopCartridge neighbor=wave.withSound(1,ordinary);check(neighbor.sfx().get(0).line().equals(wave.sfx().get(0).line()),"advanced neighboring slot retained while editing a supported slot");
            WorkshopCartridge zero=cart(nl,"__sfx__"+nl+String.join("",Collections.nCopies(168,"0")));P8Sfx.Sound initialized=zero.sfx().get(0);initialized.speed=16;initialized.notes[0][2]=5;check(zero.withSound(0,initialized).sfx().get(0).speed==16,"raw zero empty slot can be explicitly initialized");
            for(int pitch=0;pitch<64;pitch++)for(int instrument=0;instrument<16;instrument++){P8Sfx.Sound n=new P8Sfx.Sound();n.notes[0]=new int[]{pitch,instrument,7,7};P8Sfx.Sound decoded=P8Sfx.Sound.parse(n.line());check(Arrays.equals(decoded.notes[0],n.notes[0]),"pitch and instrument bit ranges do not overlap");}
            for(int[] invalid:new int[][]{{64,0,1,0},{0,16,1,0},{0,0,8,0},{0,0,1,8},{-1,0,1,0}}){P8Sfx.Sound n=new P8Sfx.Sound();n.notes[0]=invalid;refused(()->empty.withSound(1,n));}
            refused(()->cart(nl,"__sfx__"+nl+"invalid").sfx());refused(()->cart(nl,"__sfx__"+nl+upper+nl+"__sfx__"+nl+upper).sfx());
            refused(()->cart(nl,"__sfx__"+nl+String.join(nl,Collections.nCopies(65,upper))).sfx());
        }
        System.out.println("SfxDataTest: "+checks+" checks passed");
    }
}
