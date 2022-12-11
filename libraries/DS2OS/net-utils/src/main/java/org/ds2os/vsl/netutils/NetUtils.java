package org.ds2os.vsl.netutils;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.ds2os.vsl.core.config.VslTransportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for network handling.
 *
 * @author felix
 */
public final class NetUtils {

    /**
     * SLF4J logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(NetUtils.class);

    /**
     * No instantiation of utility classes.
     */
    private NetUtils() {
        // do nothing
    }

    /**
     * Enumerate all local network addresses.
     *
     * @param config
     *            the {@link VslTransportConfig} to check for allowed interfaces.
     * @return the network addresses where this host can be reached.
     * @throws SocketException
     *             If a socket exception occurs during the enumeration.
     */
    public static Map<InetAddress, NetworkInfo> getLocalAddresses(final VslTransportConfig config)
            throws SocketException {
        final Map<InetAddress, NetworkInfo> result = new HashMap<InetAddress, NetworkInfo>();
        final Collection<String> usableInterfaces = config.getUsableInterfaces();
        LOG.debug("Starting network interface enumeration");
        for (final NetworkInterface netIf : Collections
                .list(NetworkInterface.getNetworkInterfaces())) {
            final String name = netIf.getName().toLowerCase(Locale.ROOT);

            if (!netIf.isUp()) {
                LOG.debug("Network interface {} is not up.", name);
                continue;
            }

            // check if interface is in usable interfaces
            boolean usable = false;
            for (final String usableIf : usableInterfaces) {
                if (name.equals(usableIf) || usableIf.endsWith("*")
                        && name.startsWith(usableIf.substring(0, usableIf.length() - 1))) {
                    usable = true;
                    break;
                }
            }
            if (!usable) {
                LOG.debug("Network interface {} not included in usable interfaces ({})", name,
                        usableInterfaces);
                continue;
            }

            // only add default loopback address once (IPv4 usually)
            if (netIf.isLoopback()) {
                if (config.isLoopbackAllowed()) {
                    try {
                        result.put(InetAddress.getByName("127.0.0.1"), new NetworkInfo(name,
                                InetAddress.getByName("127.255.255.255"), netIf));
                        LOG.debug("Found loopback interface {} with address 127.0.0.1"
                                + " (no IP detection)", name);
                    } catch (final UnknownHostException e) {
                        // usually can not happen.
                        LOG.error("Unknown host exception with constant IP string:", e);
                    }
                } else {
                    LOG.debug("Ignoring loopback interface {} (disabled by config)", name);
                }
                continue;
            }

            // for normal interfaces, iterate through all at least site local
            // unicast addresses
            for (final InterfaceAddress ifAddr : netIf.getInterfaceAddresses()) {
                final InetAddress address = ifAddr.getAddress();
                if (address != null && !address.isMulticastAddress()
                        && !address.isLinkLocalAddress()) {
                    result.put(address, new NetworkInfo(name, ifAddr.getBroadcast(), netIf));
                    LOG.debug("Found interface {} with address {}", name, address.getHostAddress());
                }
            }
        }
        return result;
    }
}
