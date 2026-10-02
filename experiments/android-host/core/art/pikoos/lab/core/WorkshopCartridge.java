package art.pikoos.lab.core;

import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Byte-scoped edits for our owned Moon Garden template, not a general Lua parser. */
public final class WorkshopCartridge {
    private final P8Document document;
    private final int lua, gfx;
    private final int[] fields = new int[2];
    private final int[] rows = new int[16];
    public WorkshopCartridge(byte[] source) {
        document = P8Document.parse(source);
        lua = document.uniqueSection("lua");
        gfx = document.uniqueSection("gfx");
        String code = code();
        String[] names = {"speed", "jump"};
        for (int i = 0; i < 2; i++) {
            Matcher m = Pattern.compile("(?m)^-- pikoos-" + names[i]
                + "\\r?\\n" + names[i] + "=([1-4])(?=\\r?$)").matcher(code);
            if (!m.find()) throw new IllegalArgumentException("Missing owned field: " + names[i]);
            fields[i] = m.start(1);
            if (m.find()) throw new IllegalArgumentException("Ambiguous field: " + names[i]);
        }
        byte[] pixels = document.body(gfx);
        int start = 0;
        for (int y = 0; y < 16; y++) {
            rows[y] = start;
            for (int x = 0; x < 128; x++) {
                if (start + x >= pixels.length || Character.digit((char)pixels[start + x], 16) < 0)
                    throw new IllegalArgumentException("Invalid sprite row " + y);
            }
            start += 128;
            if (start < pixels.length && pixels[start] == '\r') start++;
            if (start >= pixels.length || pixels[start++] != '\n')
                throw new IllegalArgumentException("Missing sprite row ending");
        }
    }
    public byte[] bytes() { return document.bytes(); }
    public String code() { return new String(document.body(lua), StandardCharsets.ISO_8859_1); }
    public int value(int field) { return document.body(lua)[fields[field]] - '0'; }
    public int line(int field) {
        String prefix = code().substring(0, fields[field]);
        return prefix.length() - prefix.replace("\n", "").length();
    }
    public WorkshopCartridge withValue(int field, int value) {
        if (field < 0 || field > 1 || value < 1 || value > 4) throw new IllegalArgumentException("Field out of range");
        return new WorkshopCartridge(document.edit(lua, fields[field], fields[field] + 1,
            new byte[]{(byte)('0' + value)}).bytes());
    }
    public int pixel(int x, int y) {
        checkPixel(x, y);
        return Character.digit((char)document.body(gfx)[rows[y] + x], 16);
    }
    public WorkshopCartridge withPixel(int x, int y, int color) {
        checkPixel(x, y);
        if (color < 0 || color > 15) throw new IllegalArgumentException("Color out of range");
        int at = rows[y] + x;
        return new WorkshopCartridge(document.edit(gfx, at, at + 1,
            new byte[]{(byte)"0123456789abcdef".charAt(color)}).bytes());
    }
    private static void checkPixel(int x, int y) {
        if (x < 0 || y < 0 || x > 15 || y > 15) throw new IllegalArgumentException("Pixel out of range");
    }
}
