import art.pikoos.lab.core.LabCartridge;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public final class LabCartridgeTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static void rejected(byte[] source) {
        try { new LabCartridge(source); throw new AssertionError("Invalid document accepted"); }
        catch (IllegalArgumentException expected) { }
    }
    public static void main(String[] args) throws Exception {
        byte[] fixture = Files.readAllBytes(Paths.get(args[0]));
        LabCartridge cart = new LabCartridge(fixture);
        check(Arrays.equals(fixture, cart.bytes()), "Opening rewrites bytes");
        check(Arrays.equals(fixture, cart.withSpeed(cart.speed()).bytes()), "No-op rewrites bytes");
        byte[] faster = cart.withSpeed(3).bytes();
        int changes = 0;
        for (int i=0;i<fixture.length;i++) if (fixture[i]!=faster[i]) changes++;
        check(changes == 1, "Edit touched unrelated bytes");
        check(new LabCartridge(faster).speed() == 3, "Edited speed lost");
        String text = new String(fixture, StandardCharsets.ISO_8859_1);
        String sfx = text.split("__sfx__", -1)[1].trim();
        check(sfx.length() == 168 && sfx.matches("[0-9a-f]+"), "Malformed sound fixture");
        byte[] crlf = (text.replace("\r\n", "\n").replace("\n", "\r\n") +
            "\r\n__future__\r\n\u00ff\u0080 untouched\r\n").getBytes(StandardCharsets.ISO_8859_1);
        LabCartridge unusual = new LabCartridge(crlf);
        check(Arrays.equals(crlf, unusual.withSpeed(4).withSpeed(1).bytes()), "CRLF/unknown bytes damaged");
        fixture[0] = 0;
        check(cart.bytes()[0] == 'p', "Caller can mutate owned bytes");
        byte[] exposed = cart.bytes(); exposed[0] = 0;
        check(cart.bytes()[0] == 'p', "Reader can mutate owned bytes");
        rejected(text.replace("-- pikoos-lab-speed", "-- missing").getBytes(StandardCharsets.ISO_8859_1));
        rejected(text.replace("speed=1", "speed=9").getBytes(StandardCharsets.ISO_8859_1));
        rejected(text.replace("speed=1", "speed=1\n-- pikoos-lab-speed\nspeed=2").getBytes(StandardCharsets.ISO_8859_1));
        rejected(text.replace("__lua__", "__gfx__").getBytes(StandardCharsets.ISO_8859_1));
        try { cart.withSpeed(0); throw new AssertionError("Out-of-range speed accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("PASS: byte preservation, targeted edit, CRLF, unknown bytes, invalid fixtures, ownership");
    }
}
