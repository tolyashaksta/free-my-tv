package io.freemytv;

public final class StatusNetworksTest {
    private static int assertions;
    private static void state(StatusNetworks networks, int type, boolean vpn) {
        assertions++;
        if (networks.networkType() != type || networks.vpnConnected() != vpn) throw new AssertionError("Wrong transport icons");
    }
    public static void main(String[] args) {
        StatusNetworks n = new StatusNetworks();
        state(n, 0, false);
        n.update("wifi", StatusNetworks.WIFI); state(n, 1, false);
        n.update("vpn", StatusNetworks.VPN); state(n, 1, true);
        // VPN capability changes must not remove the underlying Wi-Fi.
        n.update("vpn", StatusNetworks.VPN | StatusNetworks.WIFI); state(n, 1, true);
        n.update("vpn", StatusNetworks.VPN); state(n, 1, true);
        n.update("otherVpn", StatusNetworks.VPN);
        n.remove("vpn"); state(n, 1, true);
        n.remove("otherVpn"); state(n, 1, false);
        n.update("cable", StatusNetworks.ETHERNET); state(n, 2, false);
        n.update("vpn", StatusNetworks.VPN); state(n, 2, true);
        n.remove("wifi"); state(n, 2, true);
        n.remove("cable"); state(n, 0, true);
        n.remove("vpn"); state(n, 0, false);
        n.update("wifi", StatusNetworks.WIFI);
        n.update("vpn", StatusNetworks.VPN);
        n.clear(); state(n, 0, false);
        n.update("wifi", StatusNetworks.WIFI); state(n, 1, false);
        n.update("wifi", 0); state(n, 0, false);
        n.remove("already-lost"); state(n, 0, false);
        System.out.println("StatusNetworks: " + assertions + " assertions passed");
    }
}
