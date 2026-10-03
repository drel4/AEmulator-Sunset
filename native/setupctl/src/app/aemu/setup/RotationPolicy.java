/* AEmulator Sunset addition, 2026-10-03. GPL-3.0. */
package app.aemu.setup;

import java.lang.reflect.Method;

/** Named guest APIs: Gingerbread setRotation, ICS+ freezeRotation. */
public final class RotationPolicy {
    public static int rotate(Object wm, Class<?> api) throws Exception {
        Method get;
        try { get = api.getMethod("getDefaultDisplayRotation"); }
        catch (NoSuchMethodException old) { get = api.getMethod("getRotation"); }
        int next = (((Integer) get.invoke(wm)) + 1) & 3;
        try { api.getMethod("freezeRotation", int.class).invoke(wm, next); }
        catch (NoSuchMethodException gingerbread) {
            api.getMethod("setRotation", int.class, boolean.class, int.class).invoke(wm, next, true, 0);
        }
        return next;
    }
}
