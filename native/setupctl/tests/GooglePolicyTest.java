package app.aemu.setup;

public final class GooglePolicyTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        for (String name : new String[]{"com.google.android.gms", "com.google.android.gsf", "com.google.android.gsf.login",
                "com.google.android.youtube", "com.android.vending", "com.android.chrome", "com.google.android.apps.maps"})
            check(GooglePolicy.candidate(name));
        for (String name : new String[]{"com.android.systemui", "com.android.settings", "com.sonyericsson.home",
                "org.example.google", "com.google.android.webview", "com.google.android.packageinstaller",
                "com.google.android.permissioncontroller", "com.google.android.networkstack", "com.google.android.setupwizard"})
            check(!GooglePolicy.candidate(name));
        check(GooglePolicy.disable("com.google.android.gms", 0, false));
        check(!GooglePolicy.disable("com.google.android.googlequicksearchbox", 0, true));
        for (int current : new int[]{2,3,4}) check(!GooglePolicy.disable("com.google.android.gms", current, false));
        check(GooglePolicy.restore(2, 0)); check(GooglePolicy.restore(2, 1));
        check(!GooglePolicy.restore(1, 0)); check(!GooglePolicy.restore(3, 0));
        for (int previous : new int[]{2,3,4}) check(!GooglePolicy.restore(2, previous));
        System.out.println("Google policy passed: discovery, protected plumbing/roles, previous disabled states, restore ownership");
    }
}
