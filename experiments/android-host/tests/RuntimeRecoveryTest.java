import art.pikoos.lab.core.RuntimeRecovery;
public final class RuntimeRecoveryTest {
    static int count;static void check(boolean expected,String text){count++;if(RuntimeRecovery.idle(text,10123,60,61,62)!=expected)throw new AssertionError("census "+count);}
    public static void main(String[] args){
        String idle="UID PID NAME\n0 1 init\n10123 60 provider\n10123 61 host\n10123 62 ps\n";
        check(true,idle);check(true,idle+"10124 72 pico8_64\n");
        for(String name:new String[]{"proot","pico8_64","sh","pulseaudio","provider"})check(false,idle+"10123 70 "+name+"\n");
        check(false,"");check(false,null);check(false,idle.replace("10123 62 ps\n",""));
        check(false,idle.replace("10123 60 provider\n",""));check(false,idle.replace("10123 62 ps","10123 62 fake"));
        check(false,idle+"broken\n");check(false,idle+"10123 62 ps\n");check(false,idle.replace("10123","u0_a123"));
        check(false,idle.replace("UID PID NAME","USER PID NAME"));
        check(false,idle+"-1 77 invalid\n");
        System.out.println("Runtime recovery: "+count+" checks passed");
    }
}
