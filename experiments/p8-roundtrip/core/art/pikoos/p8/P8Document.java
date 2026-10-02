package art.pikoos.p8;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** Byte-preserving P8 framing proof, not a Lua parser or compatibility validator. */
public final class P8Document {
    private static final String SIGNATURE = "pico-8 cartridge // http://www.pico-8.com";
    private static final Pattern MARKER = Pattern.compile("__[a-z][a-z0-9_]*__");
    private static final List<String> STANDARD = Arrays.asList(
        "lua", "gfx", "gff", "label", "map", "sfx", "music", "meta");

    public static final class Section {
        public final String name;
        public final int headerStart, bodyStart, bodyEnd;
        private Section(String name, int headerStart, int bodyStart, int bodyEnd) {
            this.name = name;
            this.headerStart = headerStart;
            this.bodyStart = bodyStart;
            this.bodyEnd = bodyEnd;
        }
        public boolean isStandard() { return STANDARD.contains(name); }
        public int size() { return bodyEnd - bodyStart; }
    }

    private final byte[] source;
    private final List<Section> sections;
    private final String version;

    private P8Document(byte[] input) {
        source = input.clone();
        int first = source.length >= 3 && source[0] == (byte)0xef &&
            source[1] == (byte)0xbb && source[2] == (byte)0xbf ? 3 : 0;
        int end = lineEnd(first);
        if (!ascii(first, end).equals(SIGNATURE))
            throw new IllegalArgumentException("P8 signature missing or unsupported");
        int start = nextLine(end);
        end = lineEnd(start);
        String versionLine = ascii(start, end);
        if (!versionLine.matches("version [0-9]+"))
            throw new IllegalArgumentException("P8 version line missing or unsupported");
        version = versionLine.substring(8);
        start = nextLine(end);
        List<Section> found = new ArrayList<>();
        String name = null;
        int header = -1, body = -1;
        while (start < source.length) {
            end = lineEnd(start);
            String line = ascii(start, end);
            if (MARKER.matcher(line).matches()) {
                if (name != null) found.add(new Section(name, header, body, start));
                name = line.substring(2, line.length() - 2);
                header = start;
                body = nextLine(end);
            }
            start = nextLine(end);
        }
        if (name != null) found.add(new Section(name, header, body, source.length));
        sections = Collections.unmodifiableList(found);
    }

    public static P8Document parse(byte[] source) {
        if (source == null) throw new IllegalArgumentException("Missing cartridge bytes");
        return new P8Document(source);
    }
    public String version() { return version; }
    public List<Section> sections() { return sections; }
    public byte[] bytes() { return source.clone(); }
    public byte[] body(int index) {
        Section section = sections.get(index);
        return Arrays.copyOfRange(source, section.bodyStart, section.bodyEnd);
    }
    /** Reject ambiguity instead of silently choosing one duplicate section. */
    public int uniqueSection(String name) {
        int found = -1;
        for (int i = 0; i < sections.size(); i++) {
            if (!sections.get(i).name.equals(name)) continue;
            if (found != -1) throw new IllegalArgumentException("Duplicate section: " + name);
            found = i;
        }
        if (found == -1) throw new IllegalArgumentException("Missing section: " + name);
        return found;
    }

    /** Offsets are bytes relative to a section body; unrelated bytes stay untouched. */
    public P8Document edit(int index, int from, int to, byte[] replacement) {
        Section target = sections.get(index);
        if (replacement == null || from < 0 || to < from || to > target.size())
            throw new IllegalArgumentException("Edit outside section body");
        long size = (long)source.length - (to - from) + replacement.length;
        if (size > Integer.MAX_VALUE) throw new IllegalArgumentException("Cartridge too large");
        int begin = target.bodyStart + from, finish = target.bodyStart + to;
        byte[] changed = new byte[(int)size];
        System.arraycopy(source, 0, changed, 0, begin);
        System.arraycopy(replacement, 0, changed, begin, replacement.length);
        System.arraycopy(source, finish, changed, begin + replacement.length, source.length - finish);
        P8Document result = parse(changed);
        int delta = replacement.length - (to - from);
        if (result.sections.size() != sections.size()) throw boundaryError();
        for (int i = 0; i < sections.size(); i++) {
            Section before = sections.get(i), after = result.sections.get(i);
            int shift = i > index ? delta : 0;
            if (!before.name.equals(after.name) || after.headerStart != before.headerStart + shift ||
                after.bodyStart != before.bodyStart + shift ||
                after.bodyEnd != before.bodyEnd + (i >= index ? delta : 0)) throw boundaryError();
        }
        return result;
    }
    private static IllegalArgumentException boundaryError() {
        return new IllegalArgumentException("Edit would change P8 section boundaries");
    }
    private int lineEnd(int start) {
        int pos = start;
        while (pos < source.length && source[pos] != '\n' && source[pos] != '\r') pos++;
        return pos;
    }
    private int nextLine(int end) {
        if (end >= source.length) return source.length;
        if (source[end] == '\r' && end + 1 < source.length && source[end + 1] == '\n') return end + 2;
        return end + 1;
    }
    private String ascii(int start, int end) {
        // One char per byte for framing only; Lua text/glyph encoding is not interpreted.
        return new String(source, start, end - start, StandardCharsets.ISO_8859_1);
    }
}
