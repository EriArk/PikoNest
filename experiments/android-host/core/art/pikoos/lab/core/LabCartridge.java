package art.pikoos.lab.core;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import art.pikoos.p8.P8Document;

/** Edits one owned fixture field. Deliberately not a general P8/Lua parser. */
public final class LabCartridge {
    private static final Pattern FIELD = Pattern.compile(
        "(?m)^-- pikoos-lab-speed\\r?\\nspeed=([1-4])(?=\\r?$)");
    private final P8Document document;
    private final int luaSection;
    private final int speedOffset;

    public LabCartridge(byte[] source) {
        document = P8Document.parse(source);
        luaSection = document.uniqueSection("lua");
        String text = new String(document.body(luaSection), StandardCharsets.ISO_8859_1);
        Matcher field = FIELD.matcher(text);
        if (!field.find()) throw new IllegalArgumentException("Lab speed field missing");
        int offset = field.start(1);
        if (field.find()) throw new IllegalArgumentException("Ambiguous lab speed field");
        speedOffset = offset;
    }

    public int speed() { return document.body(luaSection)[speedOffset] - '0'; }
    public byte[] bytes() { return document.bytes(); }
    public LabCartridge withSpeed(int value) {
        if (value < 1 || value > 4) throw new IllegalArgumentException("Speed must be 1..4");
        return new LabCartridge(document.edit(luaSection, speedOffset, speedOffset + 1,
            new byte[]{(byte)('0' + value)}).bytes());
    }
}
