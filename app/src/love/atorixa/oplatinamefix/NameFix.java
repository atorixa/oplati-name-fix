package love.atorixa.oplatinamefix;

import android.app.Application;
import android.content.Context;
import android.widget.TextView;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Display-only name replacement for the Oplati app.
 *
 * Scope: {@code by.lwo.oplati} only. The hook rewrites strings right before they
 * become on-screen text. Nothing is written to the app's storage and nothing is
 * sent anywhere: the server keeps seeing the real account, only the phone's UI
 * shows the configured name.
 */
public class NameFix implements IXposedHookLoadPackage {

    private static final String TAG = "OplatiNameFix";
    private static final String TARGET = "by.lwo.oplati";

    private static volatile Config config = new Config();
    private static volatile boolean composeHooked = false;
    private static volatile ClassLoader appClassLoader;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (lp == null || !TARGET.equals(lp.packageName)) {
            return;
        }
        appClassLoader = lp.classLoader;
        log("loaded into " + lp.packageName);

        try {
            hookActivity(lp.classLoader);
        } catch (Throwable t) {
            log("activity hook failed: " + t);
        }
        try {
            hookTextViews(lp.classLoader);
        } catch (Throwable t) {
            log("textview hook failed: " + t);
        }
    }

    /**
     * The module is injected after the process is already alive, so the config is
     * loaded when the first Activity comes up and refreshed on every resume.
     */
    private void hookActivity(final ClassLoader cl) throws Throwable {
        Class<?> act = XposedHelpers.findClass("android.app.Activity", cl);
        XC_MethodHook cb = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (param.thisObject instanceof Context) {
                        reload((Context) param.thisObject);
                    }
                    ensureComposeHooks(cl);
                } catch (Throwable t) {
                    log("activity callback failed: " + t);
                }
            }
        };
        XposedBridge.hookAllMethods(act, "onCreate", cb);
        XposedBridge.hookAllMethods(act, "onResume", cb);
    }

    private static void reload(Context ctx) {
        try {
            Config c = Config.load(ctx);
            config = c;
        } catch (Throwable t) {
            log("config load failed: " + t);
        }
    }

    private void hookTextViews(ClassLoader cl) throws Throwable {
        Class<?> tv = XposedHelpers.findClass("android.widget.TextView", cl);
        int n = 0;
        for (Method m : tv.getDeclaredMethods()) {
            if (!"setText".equals(m.getName())) {
                continue;
            }
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 0 || !CharSequence.class.isAssignableFrom(p[0])) {
                continue;
            }
            XposedBridge.hookMethod(m, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object a0 = param.args[0];
                    if (a0 instanceof CharSequence) {
                        String src = a0.toString();
                        String out = config.apply(src);
                        if (!out.equals(src)) {
                            param.args[0] = out;
                        }
                    }
                }
            });
            n++;
        }
        log("textview hooks: " + n);
    }

    /**
     * Compose text goes through {@code AnnotatedString}, which is loaded lazily,
     * so the hook is installed the first time the class is actually reachable.
     */
    private static void ensureComposeHooks(ClassLoader cl) {
        if (composeHooked || cl == null) {
            return;
        }
        try {
            Class<?> as = XposedHelpers.findClass("androidx.compose.ui.text.AnnotatedString", cl);
            XposedBridge.hookAllConstructors(as, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof String) {
                        String src = (String) param.args[0];
                        String out = config.apply(src);
                        if (!out.equals(src)) {
                            param.args[0] = out;
                        }
                    }
                }
            });
            composeHooked = true;
            log("compose hook installed");
        } catch (Throwable t) {
            // Not loaded yet; the next onResume will try again.
        }
    }

    static void log(String msg) {
        try {
            XposedBridge.log(TAG + ": " + msg);
        } catch (Throwable ignored) {
        }
    }
}
