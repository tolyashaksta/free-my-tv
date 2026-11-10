package io.freemytv;

import java.util.HashMap;
import java.util.Map;

/** Transport state for the status icons. Multiple networks may share a transport (including VPN). */
final class StatusNetworks {
    static final int WIFI = 1, ETHERNET = 2, VPN = 4;
    private final Map<Object, Integer> networks = new HashMap<>();
    private int transports;

    void update(Object network, int value) { networks.put(network, value); refresh(); }
    void remove(Object network) { networks.remove(network); refresh(); }
    void clear() { networks.clear(); transports = 0; }

    int networkType() { return (transports & ETHERNET) != 0 ? 2 : (transports & WIFI) != 0 ? 1 : 0; }
    boolean vpnConnected() { return (transports & VPN) != 0; }

    private void refresh() {
        transports = 0;
        for (int value : networks.values()) transports |= value;
    }
}
