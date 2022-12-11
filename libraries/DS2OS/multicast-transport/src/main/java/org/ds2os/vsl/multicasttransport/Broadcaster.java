package org.ds2os.vsl.multicasttransport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.lang3.SystemUtils;
import org.ds2os.vsl.core.config.VslTransportConfig;
import org.ds2os.vsl.netutils.NetUtils;
import org.ds2os.vsl.netutils.NetworkInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages sending and receiving of IPv4 broadcasts and IPv6 multicasts to a specific
 * site-local multicast address.
 *
 * @author felix
 * @author Johannes Straßer
 */
public class Broadcaster {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Broadcaster.class);

    /**
     * The UDP destination port of the broadcasts and multicasts.
     */
    private final int udpPort;

    /**
     * Site-local IPv6 multicast address.
     */
    private final Inet6Address ipv6MulticastAddress;

    /**
     * Map of the destination addresses to the receiving socket opened for it.
     */
    private final Map<InetAddress, DatagramSocket> receivingSockets;

    /**
     * Map of the destination addresses to the sending socket opened for it.
     */
    private final Map<InetAddress, DatagramSocket> sendingSockets;

    /**
     * Map of the source addresses to the multicast sockets opened for it.
     */
    private final Map<InetAddress, MulticastSocket> multicastSockets;

    /**
     * All threads the broadcaster created.
     */
    private final Set<Thread> threads;

    /**
     * Map of all sourceAdress String to their respective MTUs.
     */
    private final Map<String, Integer> sourceAddresses;

    /**
     * Map of sourceAddress Strings to datagram sockets.
     */
    private final Map<String, DatagramSocket> nameToSocket;

    /**
     * Map of sourceAddress Strings to multicast sockets.
     */
    private final Map<String, MulticastSocket> nameToMcSocket;

    /**
     * Constructor with the used IPv6 multicast address (should be site-local) or null to disable
     * IPv6.
     *
     * @param port
     *            the UDP destination port (1-65535)
     * @param ipv6MulticastAddr
     *            the IPv6 multicast address to use (optional)
     * @throws InvalidParameterException
     *             If the specified ipv6 address is not a site-local multicast address.
     */
    public Broadcaster(final int port, final Inet6Address ipv6MulticastAddr)
            throws InvalidParameterException {
        if (port < 1 || port > 65535) {
            throw new InvalidParameterException("port is not in the valid range 1-65535");
        }
        if (ipv6MulticastAddr != null && !ipv6MulticastAddr.isMCSiteLocal()) {
            throw new InvalidParameterException(
                    "ipv6MulticastAddress is not a site-local multicast");
        }

        udpPort = port;
        ipv6MulticastAddress = ipv6MulticastAddr;
        receivingSockets = new HashMap<InetAddress, DatagramSocket>();
        sendingSockets = new HashMap<InetAddress, DatagramSocket>();
        multicastSockets = new HashMap<InetAddress, MulticastSocket>();
        threads = new HashSet<Thread>();
        sourceAddresses = new HashMap<String, Integer>();
        nameToSocket = new HashMap<String, DatagramSocket>();
        nameToMcSocket = new HashMap<String, MulticastSocket>();
    }

    /**
     * Intializes the broadcaster / multicaster on all network interfaces.
     *
     * @param config
     *            the {@link VslTransportConfig} for listening interfaces
     * @throws SocketException
     *             If a socket exception occurs during enumeration.
     */
    public final void initializeInterfaces(final VslTransportConfig config) throws SocketException {
        LOGGER.info("Begin network interface enumeration");
        final Map<InetAddress, NetworkInfo> addresses = NetUtils.getLocalAddresses(config);
        for (final Map.Entry<InetAddress, NetworkInfo> entry : addresses.entrySet()) {
            final NetworkInfo info = entry.getValue();
            final InetAddress address = entry.getKey();
            final InetAddress broadcast = info.getBroadcastAddress();

            // if this address supports broadcasts, start listening for broadcasts and store
            // broadcast address
            if (broadcast != null && broadcast instanceof Inet4Address) {
                LOGGER.info("Listen for IPv4 broadcasts to {} on interface {}, local address {}",
                        broadcast.getHostAddress(), info.getInterfaceName(),
                        address.getHostAddress());
                try {
                    // Create sending sockets with ephemeral port
                    final DatagramSocket sendingSocket = new DatagramSocket(null);
                    sendingSocket.setReuseAddress(true);
                    sendingSocket.setBroadcast(true);
                    sendingSocket.bind(new InetSocketAddress(address, 0));
                    sendingSocket.connect(broadcast, udpPort);
                    sendingSockets.put(broadcast, sendingSocket);
                    // Create receiving sockets
                    final DatagramSocket receivingSocket = new DatagramSocket(null);
                    receivingSocket.setReuseAddress(true);
                    receivingSocket.setBroadcast(true);
                    receivingSocket.bind(new InetSocketAddress(
                            SystemUtils.IS_OS_WINDOWS ? address : broadcast, udpPort));
                    receivingSockets.put(broadcast, receivingSocket);
                    // Fill data structures
                    final String name = createNameFromSocket(sendingSocket);
                    nameToSocket.put(name, sendingSocket);
                    // Effective MTU is MTU - IPV4 header (20 bytes) - UDP header (8 bytes)
                    sourceAddresses.put(createNameFromSocket(sendingSocket),
                            (info.getNetworkInterface().isLoopback() ? 1500
                                    : info.getNetworkInterface().getMTU()) - 20 - 8);
                    LOGGER.info("Broadcasting on " + name);

                } catch (final SocketException e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Could not open datagram sockets on port " + udpPort
                                + " address " + address.getHostAddress(), e);
                    }
                }
            }

            // join IPv6 multicast group on this interface's IPv6 address
            if (ipv6MulticastAddress != null && info.getNetworkInterface().supportsMulticast()
                    && address instanceof Inet6Address) {
                LOGGER.info("Join IPv6 multicast group {} on interface {}, local address {}",
                        ipv6MulticastAddress.getHostAddress(), info.getInterfaceName(),
                        address.getHostAddress());
                try {
                    // Dual purpose socket
                    final MulticastSocket socket = new MulticastSocket(0);
                    socket.setReuseAddress(true);
                    socket.setInterface(address);
                    socket.joinGroup(ipv6MulticastAddress);
                    multicastSockets.put(address, socket);
                    // Fill data structures
                    final String name = createNameFromSocket(socket);
                    nameToMcSocket.put(name, socket);
                    // Effective MTU is MTU - IPV6 header (20 bytes) - UDP header (8 bytes)
                    sourceAddresses.put(createNameFromSocket(socket),
                            (info.getNetworkInterface().isLoopback() ? 1500
                                    : info.getNetworkInterface().getMTU()) - 40 - 8);
                    LOGGER.info("Multicasting on " + name);
                } catch (final IOException e) {
                    LOGGER.warn("Could not join multicast group {} on local address {}: {}",
                            ipv6MulticastAddress.getHostAddress(), address.getHostAddress());
                }
            }
        }

        if (receivingSockets.isEmpty() || sendingSockets.isEmpty()) {
            LOGGER.error("No suitable network interfaces found!");
        } else {
            LOGGER.info("All interfaces are initialized");

            for (final Entry<String, Integer> entry : getSourceAdresses().entrySet()) {
                LOGGER.info("Source address: " + entry.getKey() + " with MTU: " + entry.getValue());
            }
        }
    }

    /**
     * Start receive threads for broadcast packets.
     *
     * @param receiver
     *            the broadcast receiver which is invoked for received broadcasts.
     */
    public final void startReceiver(final BroadcastReceiver receiver) {
        // TODO this heavy threading should be replaced with NIO

        // Prepare list of sockets
        final HashSet<DatagramSocket> sockets = new HashSet<DatagramSocket>();
        for (final DatagramSocket socket : receivingSockets.values()) {
            sockets.add(socket);
        }
        for (final MulticastSocket socket : multicastSockets.values()) {
            sockets.add(socket);
        }

        // Start receivers
        for (final DatagramSocket socket : receivingSockets.values()) {
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    final InetAddress localAddress = socket.getLocalAddress();
                    while (socket.isBound()) {
                        final byte[] buffer = new byte[65536];
                        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        try {
                            socket.receive(packet);
                            receiver.received(packet, localAddress);
                        } catch (final IOException e) {
                            LOGGER.warn("Receiving on {} failed with IOException: {}",
                                    localAddress.getHostAddress(), e.getMessage());
                            break;
                        }
                    }
                    socket.close();
                    LOGGER.info("Closed socket on address {}", localAddress.getHostAddress());
                }
            });
            thread.setDaemon(true);
            thread.setName(
                    socket instanceof MulticastSocket ? "MulticastReceiver" : "BroadcastReceiver");
            thread.start();
            threads.add(thread);
        }
    }

    /**
     * Shutdown the broadcaster.
     */
    public final void shutdown() {
        for (final DatagramSocket socket : receivingSockets.values()) {
            socket.close();
        }
        for (final DatagramSocket socket : sendingSockets.values()) {
            socket.close();
        }
        for (final MulticastSocket socket : multicastSockets.values()) {
            socket.close();
        }
        for (final Thread thread : threads) {
            thread.interrupt();
        }
        try {
            for (final Thread thread : threads) {
                thread.join();
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Broadcasts a packet on the interface specified by the sourceAddress.
     *
     * @param data
     *            The data to broadcast
     * @param sourceAddress
     *            The source address to use
     * @throws IOException
     *             Thrown if an I/O Exception occurs
     * @throws NoSuchElementException
     *             Thrown if there is no socket open for the given sourceAddress
     */
    public final void broadcast(final byte[] data, final String sourceAddress)
            throws IOException, NoSuchElementException {
        LOGGER.debug("Sending packet from address " + sourceAddress);
        final DatagramSocket socket = nameToSocket.get(sourceAddress);
        if (socket == null) {
            final MulticastSocket mcSocket = nameToMcSocket.get(sourceAddress);
            if (mcSocket == null) {
                throw new NoSuchElementException(
                        "The source address " + sourceAddress + " has no matching sending socket.");
            } else {
                mcSocket.send(new DatagramPacket(data, data.length, ipv6MulticastAddress, udpPort));
            }
        } else {
            socket.send(new DatagramPacket(data, data.length, socket.getInetAddress(), udpPort));
        }

    }

    /**
     * Get all URLs this broadcaster works on.
     *
     * @return Collection of all URLs.
     */
    public final Collection<String> getURLs() {
        final List<String> urls = new ArrayList<String>();
        for (final InetAddress address : receivingSockets.keySet()) {
            final String type;
            if (address instanceof Inet4Address) {
                type = "broadcast";
            } else {
                type = "multicast";
            }
            urls.add(type + "://" + address.getHostAddress() + ":" + udpPort);
        }
        return urls;
    }

    /**
     * Convenience method to access addressToString with DatagramSockets.
     *
     * @param socket
     *            The socket the name is created for
     * @return The canonical name of the socket's source address
     */
    private static String createNameFromSocket(final DatagramSocket socket) {
        return addressToString(socket.getLocalAddress(), socket.getLocalPort());

    }

    /**
     * Convenience method to access addressToString with MulticastSoackets.
     *
     * @param socket
     *            The socket the name is created for
     * @return The canonical name of the socket's source address
     */
    private static String createNameFromSocket(final MulticastSocket socket) {
        try {
            return addressToString(socket.getInterface(), socket.getLocalPort());
        } catch (final SocketException e) {
            // Should not happen
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Create a canonical String for a given InetAddress.
     * <p>
     * For IPv4, the String has the form <code>udp://address:port</code>
     * </p>
     * <p>
     * For IPv6, the String has the form <code>udp://[address]:port</code>.
     * </p>
     *
     * @param address
     *            The address the string is created from
     * @param port
     *            The port the address uses
     * @return A canonical String describing the given address/port combination
     */
    public static String addressToString(final InetAddress address, final int port) {
        final String addressString;
        if (address instanceof Inet6Address) {
            addressString = "udp://[" + address.getHostAddress().split("%")[0] + "]:" + port;
        } else {
            addressString = "udp://" + address.getHostAddress() + ":" + port;
        }
        return addressString;

    }

    /**
     * Get a map of all used sourceAddresses and their respective maximum payload sizes.
     *
     * TODO The given MTU are only link MTUs and not PMTUs. This may lead to vulnerabilities (IPv4)
     * or loss of all packets (PIv6) if the PMTU is smaller than the link MTU.
     *
     * @return The keys are String representations of the source addresses, the values are their
     *         maximum payload sizes;
     */
    public final Map<String, Integer> getSourceAdresses() {
        return sourceAddresses;
    }

}
