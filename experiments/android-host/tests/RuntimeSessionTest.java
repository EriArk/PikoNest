import art.pikoos.lab.core.RuntimeSession;
import art.pikoos.lab.core.RuntimeSession.Phase;
public final class RuntimeSessionTest {
    static int n;static void check(boolean value){n++;if(!value)throw new AssertionError("Session check "+n);}
    public static void main(String[] args){
        String token="4acf2f32-274b-4caa-800f-44626ef44655";
        RuntimeSession live=RuntimeSession.read(token+"\nRUNNING\n423\n99876");
        check(live.observe(token,true)==Phase.RUNNING);
        check(live.observe(token,false)==Phase.INTERRUPTED);
        check(live.observe("5acf2f32-274b-4caa-800f-44626ef44655",true)==Phase.UNKNOWN);
        check(live.observe(null,true)==Phase.UNKNOWN);
        check(!RuntimeSession.ended(Phase.RUNNING));check(!RuntimeSession.ended(Phase.PREPARING));check(!RuntimeSession.ended(Phase.UNKNOWN));
        check(RuntimeSession.ended(Phase.EXITED));check(RuntimeSession.ended(Phase.INTERRUPTED));
        for(String invalid:new String[]{"",token+"\nRUNNING\n0\n1",token+"\nRUNNING\n42\n",token+"\nSUCCESS\n42\n1",token+"\nEXITED\n42\n1\nextra"})check(RuntimeSession.read(invalid).phase==Phase.UNKNOWN);
        check(RuntimeSession.read(token+"\nEXITED\n42\n1").observe(token,false)==Phase.EXITED);
        check(RuntimeSession.read(token+"\nPREPARING\n42\n1").observe(token,true)==Phase.PREPARING);
        System.out.println("Runtime session: "+n+" checks passed");
    }
}
