package art.pikoos.lab.core;

import java.util.Arrays;

/** Immutable compare-before-commit operation. Storage and history belong to the workflow. */
public final class CartEdit {
    private final byte[] before;
    private final WorkshopCartridge after;
    public CartEdit(WorkshopCartridge before, WorkshopCartridge after) {
        this.before=before.bytes();this.after=after;
    }
    public WorkshopCartridge candidate(WorkshopCartridge current) {
        if(!Arrays.equals(before,current.bytes()))
            throw new IllegalStateException("Проект изменился. Черновик сохранён; скопируй нужный текст перед повторной правкой.");
        return after;
    }
    public boolean changed(){return !Arrays.equals(before,after.bytes());}
}
