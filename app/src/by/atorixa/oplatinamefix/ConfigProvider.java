package by.atorixa.oplatinamefix;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;

import java.util.Map;

/**
 * Read-only bridge between the module settings and the hooked process. The target
 * app runs under its own uid and cannot read this package's files, so it asks
 * through this provider instead.
 */
public class ConfigProvider extends ContentProvider {

    private static final String TARGET_PACKAGE = "by.lwo.oplati";
    private static final int VALUES = 1;
    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        MATCHER.addURI(Config.AUTHORITY, "values", VALUES);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if (MATCHER.match(uri) != VALUES || !isAllowedCaller()) {
            return null;
        }
        MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});
        Map<String, String> values = ConfigStore.read(getContext());
        android.util.Log.i("OplatiNameFix", "provider query: entries=" + values.size());
        for (Map.Entry<String, String> e : values.entrySet()) {
            cursor.addRow(new Object[]{e.getKey(), e.getValue()});
        }
        return cursor;
    }

    /** Only this module and the target app may read the settings. */
    private boolean isAllowedCaller() {
        int uid = Binder.getCallingUid();
        if (uid == Process.myUid()) {
            return true;
        }
        try {
            PackageManager pm = getContext().getPackageManager();
            String[] pkgs = pm.getPackagesForUid(uid);
            if (pkgs == null) {
                return false;
            }
            for (String p : pkgs) {
                if (TARGET_PACKAGE.equals(p)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.oplatinamefix.value";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("read-only");
    }
}
