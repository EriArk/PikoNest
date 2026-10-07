package art.pikoos.lab.core;

import java.util.*;

/** Fail-closed interpretation of a complete numeric Android process census. */
public final class RuntimeRecovery {
    private RuntimeRecovery(){}
    public static boolean idle(String census,int uid,int observer,int caller,int scanner){
        if(uid<1||observer<1||caller<1||scanner<1||census==null||census.length()>1024*1024)return false;
        String[] lines=census.trim().split("\n");
        if(lines.length<2||!lines[0].trim().matches("UID\\s+PID\\s+NAME"))return false;
        Set<Integer> seen=new HashSet<>();boolean observed=false,scanned=false;
        for(int i=1;i<lines.length;i++){
            String[] f=lines[i].trim().split("\\s+",3);if(f.length!=3)return false;
            try{
                int rowUid=Integer.parseInt(f[0]),pid=Integer.parseInt(f[1]);
                if(rowUid<0||pid<1||!seen.add(pid))return false;
                if(rowUid!=uid)continue;
                if(pid==observer)observed=true;
                else if(pid==scanner){if(!f[2].equals("ps"))return false;scanned=true;}
                else if(pid!=caller)return false; // Includes orphaned/suspended runtime children.
            }catch(NumberFormatException e){return false;}
        }
        return observed&&scanned;
    }
}
