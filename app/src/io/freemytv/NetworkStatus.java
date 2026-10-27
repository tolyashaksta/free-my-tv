package io.freemytv;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;

/** Reads connected Wi-Fi and VPN networks while the home screen is visible. Never changes either connection. */
final class NetworkStatus {
    interface Listener { void changed(boolean wifi, boolean vpn); }

    private final ConnectivityManager manager;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final HandlerThread thread = new HandlerThread("HomeNetwork");
    private final Handler worker;
    private final Map<Network, NetworkCapabilities> networks = new HashMap<>();
    private ConnectivityManager.NetworkCallback callback;
    private volatile int generation;
    private boolean active;

    NetworkStatus(Context context, Listener listener) {
        manager = context.getApplicationContext().getSystemService(ConnectivityManager.class);
        this.listener = listener;
        thread.start();
        worker = new Handler(thread.getLooper());
    }

    void start() {
        if (active) return;
        active = true;
        int session = ++generation;
        worker.post(() -> watch(session));
    }

    void stop() {
        if (!active) return;
        active = false;
        generation++;
        worker.post(this::unwatch);
    }

    void close() {
        stop();
        thread.quitSafely();
    }

    private void watch(int session) {
        if (session != generation) return;
        networks.clear();
        if (manager != null) {
            ConnectivityManager.NetworkCallback next = new ConnectivityManager.NetworkCallback() {
                @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                    if (session != generation) return;
                    networks.put(network, capabilities);
                    publish(session);
                }

                @Override public void onLost(Network network) {
                    if (session != generation) return;
                    networks.remove(network);
                    publish(session);
                }
            };
            try {
                // Requests exclude VPNs by default. Include them, and keep Wi-Fi visible underneath a VPN.
                NetworkRequest request = new NetworkRequest.Builder()
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build();
                manager.registerNetworkCallback(request, next, worker);
                callback = next;
            } catch (RuntimeException ignored) { }
            try {
                for (Network network : manager.getAllNetworks()) {
                    NetworkInfo info = manager.getNetworkInfo(network);
                    NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
                    if (info != null && info.isConnected() && capabilities != null) networks.put(network, capabilities);
                }
            } catch (RuntimeException ignored) { }
        }
        publish(session);
    }

    private void unwatch() {
        if (callback != null) {
            try { manager.unregisterNetworkCallback(callback); } catch (RuntimeException ignored) { }
            callback = null;
        }
        networks.clear();
    }

    private void publish(int session) {
        boolean wifi = false, vpn = false;
        for (NetworkCapabilities capabilities : networks.values()) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) vpn = true;
            else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) wifi = true;
        }
        final boolean wifiOn = wifi, vpnOn = vpn;
        main.post(() -> {
            if (active && generation == session) listener.changed(wifiOn, vpnOn);
        });
    }
}
