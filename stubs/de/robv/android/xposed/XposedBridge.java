package de.robv.android.xposed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Compile-time stub of the Xposed API. The real implementation is provided at
 * runtime by the Xposed framework (Vector / LSPosed / EdXposed).
 */
public class XposedBridge {

    public static void log(String text) {
    }

    public static void log(Throwable t) {
    }

    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> c, XC_MethodHook cb) {
        return null;
    }

    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> c, String methodName, XC_MethodHook cb) {
        return null;
    }

    public static XC_MethodHook.Unhook hookMethod(Member m, XC_MethodHook cb) {
        return null;
    }

    public static XC_MethodHook.Unhook hookConstructor(Constructor<?> c, XC_MethodHook cb) {
        return null;
    }
}
