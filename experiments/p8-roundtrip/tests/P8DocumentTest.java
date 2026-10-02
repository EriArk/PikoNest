import art.pikoos.p8.P8Document;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

public final class P8DocumentTest {
    private static final String HEADER = "pico-8 cartridge // http://www.pico-8.com\nversion 42\n";
    private static int checks, documents;
    private static byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }
    private static void check(boolean pass, String message) {
        checks++;
        if (!pass) throw new AssertionError(message);
    }
    private static void rejects(Runnable call) {
        try { call.run(); throw new AssertionError("Unsafe operation accepted"); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    private static void roundtrip(byte[] original) {
        P8Document document = P8Document.parse(original);
        check(Arrays.equals(original, document.bytes()), "No-op changed bytes");
        for (int i = 0; i < document.sections().size(); i++) {
            byte[] body = document.body(i);
            check(Arrays.equals(original, document.edit(i, 0, body.length, body).bytes()), "Body no-op changed bytes");
            if (body.length == 0) continue;
            // Length-changing edit at the first byte; every other section/header must survive.
            P8Document edited = document.edit(i, 0, 0, bytes("-- probe "));
            P8Document.Section before = document.sections().get(i), after = edited.sections().get(i);
            check(Arrays.equals(Arrays.copyOfRange(original, 0, before.bodyStart),
                Arrays.copyOfRange(edited.bytes(), 0, after.bodyStart)), "Prefix changed");
            check(Arrays.equals(Arrays.copyOfRange(original, before.bodyEnd, original.length),
                Arrays.copyOfRange(edited.bytes(), after.bodyEnd, edited.bytes().length)), "Suffix changed");
            check(Arrays.equals(original, edited.edit(i, 0, bytes("-- probe ").length, new byte[0]).bytes()),
                "Edit/revert changed bytes");
        }
        documents++;
    }
    private static String sha(byte[] data) throws Exception {
        StringBuilder hex = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(data)) hex.append(String.format("%02x", b & 255));
        return hex.toString();
    }
    public static void main(String[] args) throws Exception {
        String all = HEADER + "__lua__\n-- comment\n#include chapter.lua\nif(a!=b) x+=1\n" +
            "s=[==[long\nstring]==]\nprint(\"♥ 🐱\")\n\t-- tabs\n" +
            "__gfx__\n1234\n__gff__\n00ff\n__label__\n00\n__map__\n0f\n" +
            "__sfx__\n0000\n__music__\n00 41424344\n__meta__\n__meta:other_tool__\nuntouched\n" +
            "__future__\nopaque\u0000\n";
        for (String eol : new String[]{"\n", "\r\n", "\r"}) {
            roundtrip(bytes(all.replace("\n", eol)));
            roundtrip(bytes(all.replace("\n", eol).replace("opaque\u0000" + eol, "opaque\u0000")));
        }
        roundtrip(bytes("\ufeff" + all));
        roundtrip(bytes(HEADER + "__lua__\r\n-- mixed\nprint(1)\r__gfx__\r\n00"));
        roundtrip(bytes(HEADER));
        roundtrip(bytes(HEADER + "__lua__"));
        roundtrip(bytes(HEADER + "__lua__\n__gfx__\n__sfx__\n"));
        roundtrip(bytes(all.replace("version 42", "version 99999999999999999999")));
        P8Document doc = P8Document.parse(bytes(all));
        check(doc.sections().size() == 9, "Wrong section count (meta subsection must stay opaque)");
        for (int i = 0; i < 8; i++) check(doc.sections().get(i).isStandard(), "Standard section missed");
        check(!doc.sections().get(8).isStandard(), "Unknown section classified standard");
        int lua = doc.uniqueSection("lua");
        rejects(() -> doc.edit(lua, -1, 0, new byte[0]));
        rejects(() -> doc.edit(lua, 1, 0, new byte[0]));
        rejects(() -> doc.edit(lua, 0, doc.body(lua).length + 1, new byte[0]));
        rejects(() -> doc.edit(lua, 0, 0, bytes("__gfx__\n")));
        int luaSize = doc.body(lua).length;
        rejects(() -> doc.edit(lua, luaSize - 1, luaSize, new byte[0])); // would merge next header
        rejects(() -> doc.uniqueSection("absent"));
        P8Document duplicates = P8Document.parse(bytes(HEADER + "__lua__\na=1\n__lua__\nb=2\n"));
        roundtrip(duplicates.bytes());
        rejects(() -> duplicates.uniqueSection("lua"));
        rejects(() -> P8Document.parse(bytes("not a cart")));
        rejects(() -> P8Document.parse(bytes(HEADER.replace("version 42", "version nope"))));
        byte[] owner = bytes(all);
        P8Document immutable = P8Document.parse(owner);
        owner[0] = 0;
        byte[] exposed = immutable.bytes(); exposed[0] = 0;
        byte[] exposedBody = immutable.body(lua); exposedBody[0] = 0;
        check(Arrays.equals(bytes(all), immutable.bytes()), "External mutation changed document");
        try { immutable.sections().clear(); throw new AssertionError("Mutable sections"); }
        catch (UnsupportedOperationException expected) { checks++; }

        // Deterministic raw-byte corpus exercises encoding independence and framing.
        Random random = new Random(20261002L);
        for (int n = 0; n < 200; n++) {
            byte[] prefix = bytes(HEADER + "__lua__\n");
            byte[] raw = new byte[prefix.length + random.nextInt(8192)];
            random.nextBytes(raw);
            System.arraycopy(prefix, 0, raw, 0, prefix.length);
            roundtrip(raw);
        }
        StringBuilder large = new StringBuilder(HEADER + "__lua__\n");
        for (int i = 0; i < 8000; i++) large.append("a+=1 -- preserve this line\n");
        large.append("__gfx__\n");
        for (int i = 0; i < 128; i++) {
            for (int j = 0; j < 128; j++) large.append("0123456789abcdef".charAt(j % 16));
            large.append('\n');
        }
        roundtrip(bytes(large.toString()));

        int corpus = 0;
        for (String argument : args) {
            Path path = Paths.get(argument);
            List<Path> paths = new ArrayList<>();
            if (Files.isDirectory(path)) {
                try (Stream<Path> walk = Files.walk(path)) {
                    walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".p8")).sorted().forEach(paths::add);
                }
            } else paths.add(path);
            for (Path cart : paths) {
                byte[] original = Files.readAllBytes(cart);
                roundtrip(original);
                System.out.println("CORPUS " + cart.getFileName() + " bytes=" + original.length + " sha256=" + sha(original));
                corpus++;
            }
        }
        System.out.println("PASS: " + documents + " documents, " + checks + " assertions, " + corpus + " corpus carts");
    }
}
