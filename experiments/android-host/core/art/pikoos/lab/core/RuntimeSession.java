package art.pikoos.lab.core;

/** Observation of a launched snapshot, not a game result. Unknown evidence never means exit. */
public final class RuntimeSession {
    public enum Phase { UNKNOWN, PREPARING, RUNNING, EXITED, INTERRUPTED }
    public final String token,start;public final Phase phase;public final int pid;
    private RuntimeSession(String token,Phase phase,int pid,String start){this.token=token;this.phase=phase;this.pid=pid;this.start=start;}
    public static boolean validToken(String value){return value!=null&&value.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}");}
    public static RuntimeSession read(String text){
        try{
            if(text.length()>256)throw new IllegalArgumentException();
            String[] f=text.split("\n",-1);if(f.length!=4||!validToken(f[0]))throw new IllegalArgumentException();
            Phase phase=Phase.valueOf(f[1]);int pid=Integer.parseInt(f[2]);
            if(pid<=0||!f[3].matches("[0-9]+"))throw new IllegalArgumentException();
            return new RuntimeSession(f[0],phase,pid,f[3]);
        }catch(Exception invalid){return new RuntimeSession("",Phase.UNKNOWN,0,"");}
    }
    public Phase observe(String expected,boolean sameProcess){
        if(!validToken(expected)||!token.equals(expected))return Phase.UNKNOWN;
        if((phase==Phase.RUNNING||phase==Phase.PREPARING)&&!sameProcess)return Phase.INTERRUPTED;
        return phase;
    }
    public static boolean ended(Phase phase){return phase==Phase.EXITED||phase==Phase.INTERRUPTED;}
}
