package art.pikoos.lab.core;

import java.util.*;

/** Visual descriptions, not inferred gameplay roles or resource addresses. */
public final class GameElementLabels {
    private static final String[] COLORS={"Empty","Navy","Purple","Green","Brown","Gray","Silver","White","Red","Orange","Yellow","Lime","Blue","Lavender","Pink","Peach"};
    private final String[] labels;

    public GameElementLabels(WorkshopCartridge cart,List<SpriteRegion> regions){
        String[] descriptions=new String[regions.size()];
        for(int n=0;n<regions.size();n++){
            SpriteRegion r=regions.get(n);int[] colors=new int[16];
            for(int y=0;y<r.height;y++)for(int x=0;x<r.width;x++)colors[cart.pixel(r,x,y)]++;
            int dominant=0;
            for(int color=1;color<16;color++)if(colors[color]>0&&(dominant==0||colors[color]>colors[dominant]))dominant=color;
            descriptions[n]=COLORS[dominant]+" sprite";
        }
        labels=new String[regions.size()];
        for(int n=0;n<labels.length;n++){
            int count=0,ordinal=0;
            for(int k=0;k<labels.length;k++)if(descriptions[n].equals(descriptions[k])){count++;if(k<n)ordinal++;}
            labels[n]=descriptions[n]+(count>1?" "+suffix(ordinal):"");
        }
    }
    private static String suffix(int ordinal){String result="";do{result=(char)('A'+ordinal%26)+result;ordinal=ordinal/26-1;}while(ordinal>=0);return result;}
    public String get(int index){return index<0||index>=labels.length?"Choose an element":labels[index];}
}
