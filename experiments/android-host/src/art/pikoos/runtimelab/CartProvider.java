package art.pikoos.runtimelab;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Shares only one immutable-per-run snapshot; no arbitrary path resolution. */
public final class CartProvider extends ContentProvider {
    public static final Uri CART = Uri.parse("content://art.pikoos.runtimelab.carts/run.p8");
    @Override public boolean onCreate() { return true; }
    private File cart(Uri uri) {
        if (!CART.equals(uri)) throw new IllegalArgumentException("Unknown cartridge URI");
        return new File(getContext().getFilesDir(), "run.p8");
    }
    @Override public String getType(Uri uri) { cart(uri); return "text/plain"; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read-only cartridge");
        return ParcelFileDescriptor.open(cart(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection,
                                  String[] args, String sortOrder) {
        File file = cart(uri);
        String[] columns = projection != null ? projection :
            new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor result = new MatrixCursor(columns);
        Object[] row = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) row[i] = "pikoos-lab.p8";
            else if (OpenableColumns.SIZE.equals(columns[i])) row[i] = file.length();
        }
        result.addRow(row);
        return result;
    }
    @Override public Uri insert(Uri u, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri u, String s, String[] a) { throw new UnsupportedOperationException(); }
}
