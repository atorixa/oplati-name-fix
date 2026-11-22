package love.atorixa.oplatinamefix;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * User configuration. Deliberately contains no personal data: the values that are
 * replaced live only in the on-device SharedPreferences of this module and are
 * edited through SettingsActivity.
 */
public final class Config {

    public static final String AUTHORITY = "love.atorixa.oplatinamefix.config";
    public static final String PREFS = "config";

    public static final String K_OLD_FIRST = "old_first";
    public static final String K_NEW_FIRST = "new_first";
    public static final String K_OLD_LAST = "old_last";
    public static final String K_NEW_LAST = "new_last";
    public static final String K_OLD_INITIALS = "old_initials";
    public static final String K_NEW_INITIALS = "new_initials";
    public static final String K_EXTRA = "extra";

    /** Neutral placeholders, shown until the user enters their own values. */
    public static final String DEF_NEW_FIRST = "Имя";
    public static final String DEF_NEW_LAST = "Фамилия";
    public static final String DEF_NEW_INITIALS = "ИФ";

    public String oldFirst = "";
    public String newFirst = DEF_NEW_FIRST;
    public String oldLast = "";
    public String newLast = DEF_NEW_LAST;
    public String oldInitials = "";
    public String newInitials = DEF_NEW_INITIALS;
    public String extra = "";

    private volatile NameTransformer transformer;

    /** Applies the configured replacements; built once per loaded config. */
    String apply(String text) {
        NameTransformer t = transformer;
        if (t == null) {
            synchronized (this) {
                t = transformer;
                if (t == null) {
                    t = new NameTransformer(this);
                    transformer = t;
                }
            }
        }
        return t.apply(text);
    }

    public static Config load(Context ctx) {
        Config c = new Config();
        if (ctx == null) {
            return c;
        }
        Cursor cur = null;
        try {
            ContentResolver cr = ctx.getContentResolver();
            cur = cr.query(Uri.parse("content://" + AUTHORITY + "/values"), null, null, null, null);
            int rows = 0;
            while (cur != null && cur.moveToNext()) {
                String k = cur.getString(0);
                String v = cur.getString(1);
                c.put(k, v);
                rows++;
            }
            NameFix.log("config rows: " + rows + (cur == null ? " (provider returned null)" : ""));
        } catch (Throwable t) {
            NameFix.log("provider error: " + t);
        } finally {
            if (cur != null) {
                try {
                    cur.close();
                } catch (Throwable ignored) {
                }
            }
        }
        return c;
    }

    public void put(String key, String value) {
        if (key == null) {
            return;
        }
        String v = value == null ? "" : value;
        switch (key) {
            case K_OLD_FIRST:
                oldFirst = v;
                break;
            case K_NEW_FIRST:
                newFirst = v.isEmpty() ? DEF_NEW_FIRST : v;
                break;
            case K_OLD_LAST:
                oldLast = v;
                break;
            case K_NEW_LAST:
                newLast = v.isEmpty() ? DEF_NEW_LAST : v;
                break;
            case K_OLD_INITIALS:
                oldInitials = v;
                break;
            case K_NEW_INITIALS:
                newInitials = v.isEmpty() ? DEF_NEW_INITIALS : v;
                break;
            case K_EXTRA:
                extra = v;
                break;
            default:
                break;
        }
    }
}
