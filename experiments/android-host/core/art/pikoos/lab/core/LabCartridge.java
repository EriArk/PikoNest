package art.pikoos.lab.core;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Edits one owned fixture field. Deliberately not a general P8/Lua parser. */
public final class LabCartridge {
    private static final Pattern FIELD = Pattern.compile(
        "(?m)^-- pikoos-lab-speed\\r?\\nspeed=([1-4])(?=\\r?$)");
    private final byte[] bytes;
    private final int speedOffset;

    public LabCartridge(byte[] source) {
        String text = new String(source, StandardCharsets.ISO_8859_1);
        if (!text.startsWith("pico-8 cartridge // http://www.pico-8.com")) {
            throw new IllegalArgumentException("Not a PICO-8 text cartridge");
        }
        Matcher lua = Pattern.compile("(?m)^__lua__\\r?$").matcher(text);
        if (!lua.find()) throw new IllegalArgumentException("Lua section missing");
        int start = lua.end();
        if (lua.find()) throw new IllegalArgumentException("Multiple Lua sections");
        Matcher section = Pattern.compile("(?m)^__[a-zA-Z0-9_]+__\\r?$").matcher(text);
        int end = text.length();
        if (section.find(start)) end = section.start();
        Matcher field = FIELD.matcher(text);
        field.region(start, end);
        if (!field.find()) throw new IllegalArgumentException("Lab speed field missing");
        int offset = field.start(1);
        if (field.find()) throw new IllegalArgumentException("Ambiguous lab speed field");
        bytes = source.clone();
        speedOffset = offset;
    }

    public int speed() { return bytes[speedOffset] - '0'; }
    public byte[] bytes() { return bytes.clone(); }
    public LabCartridge withSpeed(int value) {
        if (value < 1 || value > 4) throw new IllegalArgumentException("Speed must be 1..4");
        byte[] changed = bytes.clone();
        changed[speedOffset] = (byte) ('0' + value);
        return new LabCartridge(changed);
    }
}
