package org.ds2os.vsl.multicasttransport;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * Handler for received broadcast packets.
 *
 * @author felix
 */
public interface BroadcastReceiver {

    /**
     * This method is called when a broadcast (or IPv6 multicast) packet was received. Note that the
     * same packet may be received multiple times on different local addresses.
     *
     * @param packet
     *            the DatagramPacket which was received
     * @param localAddress
     *            the local address where this packet was received
     */
    void received(DatagramPacket packet, InetAddress localAddress);
}
