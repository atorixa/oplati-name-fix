package love.atorixa.oplatinamefix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plain key=value store in the module's own files directory.
 *
 * Deliberately not SharedPreferences: the file is also written from outside
 * (scripts, adb) during setup, and a plain text file never fights the framework
 * cache about it.
 */
final class ConfigStore {

    private static final String FILE = "config.properties";

    private ConfigStore() {
    }

    static File file(android.content.Context ctx) {
        return new File(ctx.getFilesDir(), FILE);
    }

    static Map<String, String> read(android.content.Context ctx) {
        Map<String, String> map = new LinkedHashMap<>();
        File f = file(ctx);
        if (!f.exists()) {
            return map;
        }
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq > 0) {
                    map.put(line.substring(0, eq).trim(), line.substring(eq + 1));
                }
            }
        } catch (Throwable ignored) {
        }
        return map;
    }

    static void write(android.content.Context ctx, Map<String, String> values) {
        File f = file(ctx);
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(f), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : values.entrySet()) {
                sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
            }
            w.write(sb.toString());
        } catch (Throwable ignored) {
        }
    }
}
