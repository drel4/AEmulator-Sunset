/* AEmulator Sunset addition, 2026-10-03. GPL-3.0. */
package app.aemu.setup;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.os.Looper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Reversible guest PackageManager operations; never delete APKs or edit a live database. */
public final class GoogleCtl {
    private static final File STATE = new File("/data/system/aemu-google-disabled.properties");
    private static void save(Properties state) throws Exception {
        File tmp = new File(STATE.getPath() + ".tmp");
        FileOutputStream out = new FileOutputStream(tmp);
        try { state.store(out, "AEmulator Google-app option: previous enabled states"); out.getFD().sync(); }
        finally { out.close(); }
        if (!tmp.renameTo(STATE)) throw new IllegalStateException("cannot save Google-app state");
    }
    public static void main(String[] args) {
        try {
            if (args.length != 1 || !(args[0].equals("disable") || args[0].equals("restore")))
                throw new IllegalArgumentException("expected disable or restore");
            boolean disable = args[0].equals("disable");
            Looper.prepare();
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object thread = at.getMethod("systemMain").invoke(null);
            Context ctx = (Context) at.getMethod("getSystemContext").invoke(thread);
            Properties state = new Properties();
            if (STATE.isFile()) {
                FileInputStream in = new FileInputStream(STATE);
                try { state.load(in); } finally { in.close(); }
            }
            Throwable failure = null;
            for (int attempt = 0; attempt < 60; attempt++) {
                try {
                    PackageManager pm = ctx.getPackageManager();
                    List<PackageInfo> installed = pm.getInstalledPackages(0);
                    if (installed.isEmpty()) throw new IllegalStateException("packages not ready");
                    Set<String> protectedRoles = new HashSet<String>();
                    for (ResolveInfo home : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0))
                        if (home.activityInfo != null) protectedRoles.add(home.activityInfo.packageName);
                    for (ResolveInfo keyboard : pm.queryIntentServices(new Intent("android.view.InputMethod"), 0))
                        if (keyboard.serviceInfo != null) protectedRoles.add(keyboard.serviceInfo.packageName);
                    int changed = 0, failed = 0;
                    if (disable) {
                        for (PackageInfo app : installed) {
                            String name = app.packageName;
                            if (!GooglePolicy.candidate(name)) continue;
                            int current = pm.getApplicationEnabledSetting(name);
                            if (!GooglePolicy.disable(name, current, protectedRoles.contains(name))) continue;
                            if (!state.containsKey(name)) { state.setProperty(name, Integer.toString(current)); save(state); }
                            try {
                                pm.setApplicationEnabledSetting(name, 2, 0);
                                if (pm.getApplicationEnabledSetting(name) != 2) throw new IllegalStateException("disable failed");
                                changed++; System.out.println("disabled " + name);
                            } catch (Exception error) { failed++; System.out.println("cannot disable " + name + ": " + error); }
                        }
                    } else {
                        for (String name : new HashSet<String>(state.stringPropertyNames())) {
                            try { pm.getApplicationInfo(name, 0); }
                            catch (PackageManager.NameNotFoundException absent) { state.remove(name); save(state); continue; }
                            int previous = Integer.parseInt(state.getProperty(name));
                            if (GooglePolicy.restore(pm.getApplicationEnabledSetting(name), previous)) {
                                pm.setApplicationEnabledSetting(name, previous, 0);
                                if (pm.getApplicationEnabledSetting(name) != previous) throw new IllegalStateException("restore failed: " + name);
                                changed++;
                                System.out.println("restored " + name);
                            }
                            state.remove(name); save(state);
                        }
                    }
                    System.out.println((failed == 0 ? "GOOGLE_APPS_OK " : "GOOGLE_APPS_PARTIAL ") + args[0] + " changed=" + changed + " failed=" + failed);
                    System.exit(failed == 0 ? 0 : 2);
                } catch (Throwable pending) { failure = pending; Thread.sleep(2000); }
            }
            throw new IllegalStateException("Google-app option did not complete", failure);
        } catch (Throwable error) { error.printStackTrace(); System.exit(1); }
    }
}
