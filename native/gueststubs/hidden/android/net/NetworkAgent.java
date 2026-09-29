package android.net;

import android.content.Context;
import android.os.Looper;

/** Compile-only copy of the hidden framework class (5.0–7.1 signature); the real one is used at run time. */
public abstract class NetworkAgent extends android.os.Handler {
    public NetworkAgent(Looper looper, Context context, String logTag, NetworkInfo ni, NetworkCapabilities nc,
                        LinkProperties lp, int score) { super(looper); }
    public void sendNetworkInfo(NetworkInfo networkInfo) {}
    public void sendNetworkCapabilities(NetworkCapabilities networkCapabilities) {}
    public void sendLinkProperties(LinkProperties linkProperties) {}
    public void sendNetworkScore(int score) {}
    protected abstract void unwanted();
}
