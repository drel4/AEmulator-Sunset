import app.aemu.setup.SetupPolicy;
public class PolicyTest {
    public static void main(String[] args) {
        check(SetupPolicy.recognized("com.sonyericsson.initialbootsetup"));
        check(SetupPolicy.recognized("com.sec.android.app.SecSetupWizard"));
        check(!SetupPolicy.recognized("com.google.android.gsf.login"));
        check(!SetupPolicy.recognized("com.android.launcher"));
        check(SetupPolicy.restore(2, 0));
        check(SetupPolicy.restore(2, 1));
        check(!SetupPolicy.restore(2, 2));
        check(!SetupPolicy.restore(1, 0));
        System.out.println("8 setup policy checks passed");
    }
    private static void check(boolean b) { if (!b) throw new AssertionError(); }
}
