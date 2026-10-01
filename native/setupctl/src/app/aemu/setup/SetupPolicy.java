package app.aemu.setup;

/** Conservative package allowlist: never disable account sign-in or arbitrary HOME apps. */
public final class SetupPolicy {
    private SetupPolicy() {}
    public static final String[] PACKAGES = {
        "com.android.provision", "com.android.setupwizard", "com.google.android.setupwizard",
        "com.sec.android.app.SecSetupWizard", "com.sec.android.app.SecSetupWizard2013",
        "com.sec.android.app.setupwizard", "com.samsung.android.app.setupwizard",
        "com.sonyericsson.setupwizard", "com.sonymobile.setupwizard",
        "com.sonyericsson.initialbootsetup", "com.miui.provision", "com.htc.setupwizard",
        "com.lge.setupwizard", "com.motorola.setupwizard"
    };
    public static boolean recognized(String name) {
        for (String p : PACKAGES) if (p.equals(name)) return true;
        return false;
    }
    public static boolean restore(int current, int previous) {
        return current == 2 && previous != 2;
    }
}
