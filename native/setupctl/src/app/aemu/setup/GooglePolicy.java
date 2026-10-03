/* AEmulator Sunset addition, 2026-10-03. GPL-3.0. */
package app.aemu.setup;

public final class GooglePolicy {
    private GooglePolicy() {}
    public static boolean candidate(String name) {
        if (name == null) return false;
        // Keep platform plumbing even on ROMs shipping Google-branded implementations.
        if (name.equals("com.google.android.webview") || name.equals("com.google.android.packageinstaller") ||
            name.equals("com.google.android.permissioncontroller") || name.equals("com.google.android.modulemetadata") ||
            name.equals("com.google.android.ext.services") || name.equals("com.google.android.ext.shared") ||
            name.startsWith("com.google.android.networkstack")) return false;
        // Setup has independent state ownership, avoiding conflicting restores.
        if (SetupPolicy.recognized(name)) return false;
        return name.startsWith("com.google.") || name.equals("com.android.vending") ||
            name.equals("com.android.chrome") || name.equals("com.android.providers.partnerbookmarks");
    }
    public static boolean disable(String name, int current, boolean protectedRole) {
        return candidate(name) && !protectedRole && current != 2 && current != 3 && current != 4;
    }
    public static boolean restore(int current, int previous) {
        return current == 2 && previous != 2 && previous != 3 && previous != 4;
    }
}
