package art.pikoos.p8;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/** Read-only inspect, or write a byte-identical copy to a NEW output path. */
public final class P8Tool {
    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3 ||
            !(args[0].equals("inspect") && args.length == 2 || args[0].equals("copy") && args.length == 3))
            throw new IllegalArgumentException("Usage: P8Tool inspect INPUT | copy INPUT NEW_OUTPUT");
        Path input = Paths.get(args[1]);
        byte[] original = Files.readAllBytes(input);
        P8Document document = P8Document.parse(original);
        if (!Arrays.equals(original, document.bytes())) throw new AssertionError("Round-trip changed bytes");
        System.out.println(input.getFileName() + ": version=" + document.version() +
            " bytes=" + original.length + " roundtrip=identical (not runtime verification)");
        for (P8Document.Section section : document.sections()) {
            System.out.println("  " + section.name + " [" + section.bodyStart + "," +
                section.bodyEnd + ") bytes=" + section.size() +
                (section.isStandard() ? "" : " unknown/preserved"));
        }
        if (args[0].equals("copy"))
            Files.write(Paths.get(args[2]), document.bytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }
}
