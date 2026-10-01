package app.aemu.setup;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.os.Binder;
import java.lang.reflect.Method;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/** System-UID app_process helper. Uses guest providers, not host writes to a live settings database. */
public final class SetupCtl {
    private static final File STATE = new File("/data/system/aemu-setup-skip.properties");
    private static void save(Properties state) throws Exception {
        File tmp = new File(STATE.getPath() + ".tmp");
        FileOutputStream out = new FileOutputStream(tmp);
        try { state.store(out, "AEmulator setup option: previous enabled states"); out.getFD().sync(); }
        finally { out.close(); }
        if (!tmp.renameTo(STATE)) throw new IllegalStateException("cannot save setup state");
    }
    private static Object provider() throws Exception {
        Object am = Class.forName("android.app.ActivityManagerNative").getMethod("getDefault").invoke(null);
        Class<?> iface = Class.forName("android.app.IActivityManager");
        String name = "getContentProviderExternal";
        for (Method m : iface.getMethods()) if (m.getName().equals(name)) {
            Class<?>[] types = m.getParameterTypes();
            Object[] args = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                if (types[i] == String.class) args[i] = "settings";
                else if (types[i] == int.class) args[i] = 0;
                else if (types[i] == boolean.class) args[i] = true;
                else if (types[i].getName().equals("android.os.IBinder")) args[i] = new Binder();
            }
            Object holder = m.invoke(am, args);
            if (holder == null) throw new IllegalStateException("settings provider not ready");
            return holder.getClass().getField("provider").get(holder);
        }
        throw new NoSuchMethodException(name);
    }
    private static Object invokeProvider(Object provider, String operation, Uri uri, ContentValues values) throws Exception {
        for (Method m : Class.forName("android.content.IContentProvider").getMethods()) {
            if (!m.getName().equals(operation)) continue;
            Class<?>[] types = m.getParameterTypes();
            Object[] args = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                if (types[i] == Uri.class) args[i] = uri;
                else if (types[i] == ContentValues.class) args[i] = values;
                else if (types[i] == String[].class && operation.equals("query")) {
                    // First array is projection, second is selection arguments.
                    boolean first = true;
                    for (int j = 0; j < i; j++) if (types[j] == String[].class) first = false;
                    if (first) args[i] = new String[]{"value"};
                } else if (types[i] == String.class && i == 0) args[i] = "android";
            }
            return m.invoke(provider, args);
        }
        throw new NoSuchMethodException(operation);
    }
    private static void put(Object provider, String table, String key) throws Exception {
        Uri uri = Uri.parse("content://settings/" + table);
        ContentValues v = new ContentValues();
        v.put("name", key); v.put("value", "1");
        if (invokeProvider(provider, "insert", uri, v) == null) throw new IllegalStateException("settings not ready: " + key);
        Cursor c = (Cursor) invokeProvider(provider, "query", Uri.withAppendedPath(uri, key), null);
        try {
            if (c == null || !c.moveToFirst() || !"1".equals(c.getString(0)))
                throw new IllegalStateException("setting verification failed: " + key);
        } finally { if (c != null) c.close(); }
    }
    public static void main(String[] args) {
        try {
            Looper.prepare();
            boolean skip = args.length == 1 && "skip".equals(args[0]);
            Properties state = new Properties();
            if (STATE.isFile()) {
                FileInputStream in = new FileInputStream(STATE);
                try { state.load(in); } finally { in.close(); }
            }
            Throwable failure = null;
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object thread = at.getMethod("systemMain").invoke(null);
            Context context = (Context) at.getMethod("getSystemContext").invoke(thread);
            Object settingsProvider = null;
            for (int attempt = 0; attempt < 60; attempt++) {
                try {
                    PackageManager pm = context.getPackageManager();
                    // PackageManager and SettingsProvider may not exist yet on a fresh /data boot.
                    if (pm.getInstalledPackages(0).isEmpty()) throw new IllegalStateException("packages not ready");
                    if (skip) {
                        // Like the stock settings CLI: this standalone process has no AMS app record.
                        if (settingsProvider == null) settingsProvider = provider();
                        put(settingsProvider, Build.VERSION.SDK_INT >= 17 ? "global" : "secure", "device_provisioned");
                        put(settingsProvider, "secure", "user_setup_complete");
                    }
                    for (String p : SetupPolicy.PACKAGES) {
                        try { pm.getApplicationInfo(p, 0); }
                        catch (PackageManager.NameNotFoundException absent) { continue; }
                        int current = pm.getApplicationEnabledSetting(p);
                        if (skip) {
                            if (current == 2 || current == 3 || current == 4) continue;
                            if (!state.containsKey(p)) { state.setProperty(p, Integer.toString(current)); save(state); }
                            pm.setApplicationEnabledSetting(p, 2, 0);
                            System.out.println("disabled " + p);
                        } else if (state.containsKey(p)) {
                            int previous = Integer.parseInt(state.getProperty(p));
                            if (SetupPolicy.restore(current, previous)) pm.setApplicationEnabledSetting(p, previous, 0);
                            state.remove(p); save(state);
                            System.out.println("restored " + p);
                        }
                    }
                    System.out.println(skip ? "SETUP_SKIP_OK" : "SETUP_RESTORE_OK");
                    System.exit(0);
                } catch (Throwable pending) { failure = pending; Thread.sleep(2000); }
            }
            throw new IllegalStateException("setup services did not become ready", failure);
        } catch (Throwable t) { t.printStackTrace(); System.exit(1); }
    }
}
