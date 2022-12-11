package org.ds2os.vsl.netutils;

import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * Network information to associate with a local {@link InetAddress} during enumeration.
 *
 * @author felix
 */
public final class NetworkInfo {

    /**
     * The name of the network interface.
     */
    private final String interfaceName;

    /**
     * The broadcast address of this network, if any.
     */
    private final InetAddress broadcastAddress;

    /**
     * The network interface of the address (important: network interface may have multiple networks
     * attached!).
     */
    private final NetworkInterface networkInterface;

    /**
     * Constructor. Takes a broadcast address and the corresponding interface.
     *
     * @param interfaceName
     *            The name of the network interface.
     * @param broadcastAddress
     *            The broadcast address of the given interface.
     * @param networkInterface
     *            The interface for the given broadcast address.
     */
    public NetworkInfo(final String interfaceName, final InetAddress broadcastAddress,
            final NetworkInterface networkInterface) {
        this.interfaceName = interfaceName;
        this.broadcastAddress = broadcastAddress;
        this.networkInterface = networkInterface;
    }

    /**
     * Get the name of the network interface.
     *
     * @return the name string in lower case.
     */
    public String getInterfaceName() {
        return interfaceName;
    }

    /**
     * Returns the broadcast address of this {@link NetworkInfo} object.
     *
     * @return The broadcast address.
     */
    public InetAddress getBroadcastAddress() {
        return broadcastAddress;
    }

    /**
     * Returns the network interface of this {@link NetworkInfo} object.
     *
     * @return The network interface.
     */
    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }
}
