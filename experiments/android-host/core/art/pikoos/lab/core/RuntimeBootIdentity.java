package art.pikoos.lab.core;

/** A PID/start time from a different boot must never revive a previous session. */
public final class RuntimeBootIdentity {
    private RuntimeBootIdentity(){}
    public static String record(String token,String boot){
        if(!RuntimeSession.validToken(token)||!RuntimeSession.validToken(boot))throw new IllegalArgumentException("Invalid boot/session identity");
        return token+"\n"+boot;
    }
    public static boolean matches(String record,String token,String boot){
        if(record==null||record.length()!=73||!RuntimeSession.validToken(token)||!RuntimeSession.validToken(boot))return false;
        return record.equals(token+"\n"+boot);
    }
}
