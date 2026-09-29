package app.aemu.stub;

import android.content.Context;
import android.content.ContextWrapper;
import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.Looper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetAddress;

/**
 * Сеть для гостя 5.0–7.x. Сокеты гостя и так выходят в интернет через телефон, но ConnectivityService без
 * зарегистрированной сети отвечает «активной сети нет»: Chrome, загрузчики и проверки связи считают, что
 * интернета нет. Этот процесс (root) регистрирует «Ethernet» с адресом 10.0.2.15, маршрутом по умолчанию
 * и DNS, как это делает эмулятор из SDK.
 *
 * Запускается движком после загрузки: app_process -Djava.class.path=/system/framework/aemu-stubs.jar
 * /system/bin app.aemu.stub.NetStub
 */
public class NetStub extends NetworkAgent {
    NetStub(Looper l, Context c, NetworkInfo ni, NetworkCapabilities nc, LinkProperties lp) {
        super(l, c, "AEmuNet", ni, nc, lp, 60);
    }

    @Override
    protected void unwanted() { System.out.println("aemu-net: сеть больше не нужна системе"); }

    static Object newObj(String cls, Class<?>[] types, Object... args) throws Exception {
        Constructor<?> k = Class.forName(cls).getDeclaredConstructor(types);
        k.setAccessible(true);
        return k.newInstance(args);
    }

    static void call(Object o, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = o.getClass().getMethod(name, types);
        m.setAccessible(true);
        m.invoke(o, args);
    }

    public static void main(String[] args) throws Exception {
        try { run(); } catch (Throwable t) { t.printStackTrace(System.out); System.out.flush(); }
    }

    static void run() throws Exception {
        Looper.prepare();
        final Class<?> sm = Class.forName("android.os.ServiceManager");
        Method get = sm.getMethod("getService", String.class);
        IBinder b = null;
        for (int i = 0; i < 120 && b == null; i++) {
            b = (IBinder) get.invoke(null, "connectivity");
            if (b == null) Thread.sleep(1000);
        }
        if (b == null) { System.out.println("aemu-net: службы connectivity нет"); return; }
        final Class<?> icm = Class.forName("android.net.IConnectivityManager");
        Object svc = Class.forName("android.net.IConnectivityManager$Stub").getMethod("asInterface", IBinder.class).invoke(null, b);
        final Object[] cmHolder = new Object[1];
        Context ctx = new ContextWrapper(null) {
            @Override public Object getSystemService(String name) { return Context.CONNECTIVITY_SERVICE.equals(name) ? cmHolder[0] : null; }
            @Override public String getPackageName() { return "android"; }
            @Override public Context getApplicationContext() { return this; }
        };
        cmHolder[0] = newObj("android.net.ConnectivityManager", new Class<?>[]{Context.class, icm}, ctx, svc);

        NetworkInfo ni = (NetworkInfo) newObj("android.net.NetworkInfo", new Class<?>[]{int.class, int.class, String.class, String.class},
                9 /* TYPE_ETHERNET */, 0, "ETHERNET", "");
        ni.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
        call(ni, "setIsAvailable", new Class<?>[]{boolean.class}, true);

        NetworkCapabilities nc = (NetworkCapabilities) newObj("android.net.NetworkCapabilities", new Class<?>[0]);
        call(nc, "addTransportType", new Class<?>[]{int.class}, 3 /* ETHERNET */);
        for (int cap : new int[]{12 /* INTERNET */, 13 /* NOT_RESTRICTED */, 14 /* TRUSTED */, 15 /* NOT_VPN */, 11 /* NOT_METERED */})
            call(nc, "addCapability", new Class<?>[]{int.class}, cap);
        call(nc, "setLinkUpstreamBandwidthKbps", new Class<?>[]{int.class}, 100000);
        call(nc, "setLinkDownstreamBandwidthKbps", new Class<?>[]{int.class}, 100000);

        LinkProperties lp = (LinkProperties) newObj("android.net.LinkProperties", new Class<?>[0]);
        call(lp, "setInterfaceName", new Class<?>[]{String.class}, "eth0");
        Object addr = newObj("android.net.LinkAddress", new Class<?>[]{InetAddress.class, int.class},
                InetAddress.getByAddress(new byte[]{10, 0, 2, 15}), 24);
        call(lp, "addLinkAddress", new Class<?>[]{Class.forName("android.net.LinkAddress")}, addr);
        Object route = newObj("android.net.RouteInfo", new Class<?>[]{InetAddress.class}, InetAddress.getByAddress(new byte[]{10, 0, 2, 2}));
        call(lp, "addRoute", new Class<?>[]{Class.forName("android.net.RouteInfo")}, route);
        call(lp, "addDnsServer", new Class<?>[]{InetAddress.class}, InetAddress.getByAddress(new byte[]{8, 8, 8, 8}));
        call(lp, "addDnsServer", new Class<?>[]{InetAddress.class}, InetAddress.getByAddress(new byte[]{1, 1, 1, 1}));

        NetStub agent = new NetStub(Looper.myLooper(), ctx, ni, nc, lp);
        System.out.println("aemu-net: сеть зарегистрирована");
        Looper.loop();
    }
}
