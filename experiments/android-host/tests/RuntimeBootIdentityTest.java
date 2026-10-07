import art.pikoos.lab.core.RuntimeBootIdentity;
public final class RuntimeBootIdentityTest {
    static int count;static void check(boolean value){count++;if(!value)throw new AssertionError("boot "+count);}
    public static void main(String[] args){
        String token="00000000-0000-0000-0000-000000000001",boot="00000000-0000-0000-0000-000000000002",next="00000000-0000-0000-0000-000000000003";
        String record=RuntimeBootIdentity.record(token,boot);check(RuntimeBootIdentity.matches(record,token,boot));
        check(!RuntimeBootIdentity.matches(record,token,next));check(!RuntimeBootIdentity.matches(record,next,boot));
        check(!RuntimeBootIdentity.matches(record+"\n",token,boot));check(!RuntimeBootIdentity.matches("",token,boot));check(!RuntimeBootIdentity.matches(null,token,boot));check(!RuntimeBootIdentity.matches(record,token,"missing"));
        System.out.println("Runtime boot identity: "+count+" checks passed");
    }
}
