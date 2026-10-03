/* AEmulator Sunset addition, 2026-10-03. GPL-3.0. */
package app.aemu.setup;

import android.os.IBinder;

/** Guest-side rotation: no host orientation change, settings CLI or numeric Binder transactions. */
public final class RotationCtl {
    public static void main(String[] args) {
        try {
            IBinder binder = (IBinder) Class.forName("android.os.ServiceManager")
                .getMethod("getService", String.class).invoke(null, "window");
            if (binder == null) throw new IllegalStateException("window service not ready");
            Object wm = Class.forName("android.view.IWindowManager$Stub")
                .getMethod("asInterface", IBinder.class).invoke(null, binder);
            int next = RotationPolicy.rotate(wm, Class.forName("android.view.IWindowManager"));
            System.out.println("ROTATION_OK " + next);
        } catch (Throwable error) { error.printStackTrace(); System.exit(1); }
    }
}
