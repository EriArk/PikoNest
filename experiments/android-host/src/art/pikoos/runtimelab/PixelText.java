package art.pikoos.runtimelab;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;

/** Shared readable pixel typography. Measurement and rendering use identical sizes. */
final class PixelText {
    private static Typeface face;
    static void configure(Context context, Paint paint) {
        if (face == null) face = Typeface.createFromAsset(context.getAssets(), "Monocraft.ttf");
        paint.setTypeface(face);
        // Lua operators must stay literal; ligatures would also break caret measurements.
        paint.setFontFeatureSettings("'liga' 0, 'calt' 0, 'dlig' 0");
        paint.setAntiAlias(false);
    }
    static float size(float nominal) { return Math.max(18, nominal); }
    static int actionSize(Paint paint, String label, float available) {
        paint.setTextSize(size(20));
        return paint.measureText(label) <= available ? 20 : 18;
    }
    private PixelText() {}
}
