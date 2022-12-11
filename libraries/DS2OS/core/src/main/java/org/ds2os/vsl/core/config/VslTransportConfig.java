package org.ds2os.vsl.core.config;

import java.util.Set;

/**
 * Interface for general transport configurations.
 *
 * @author liebald
 * @author felix
 */
public interface VslTransportConfig {

    /**
     * Returns a set of interfaces that should be used by the KA in order to broad/multicast
     * packets. Interfaces are compared in lower case and multiple can be specified comma separated.
     * The suffix "*" can be used to match all interfaces with a certain prefix, like "eth*" for all
     * interfaces starting with "eth" or just "*" for all interfaces.
     *
     * @return Set of Strings containing the identifier of all usable interfaces (e.g. to only send
     *         over wlan or localhost.)
     */
    @ConfigDescription(
            description = "If specified, only the given interfaces are used "
                    + "for broad/multicasting.",
            id = "transport.interfaces", defaultValue = "*",
            restrictions = "comma separated list of interfaces.")
    Set<String> getUsableInterfaces();

    /**
     * Whether loopback addresses and interfaces should be allowed for transports.
     *
     * @return true iff loopback is enabled.
     */
    @ConfigDescription(description = "Should transports use loopback addresses and interfaces?",
            id = "transport.allowLoopback", defaultValue = "false", restrictions = "true|false")
    boolean isLoopbackAllowed();

    /**
     * Returns the current callback timeout used for virtualNode and subscription callbacks. The
     * default is 30000 milliseconds.
     *
     * @return callback timeout in milliseconds.
     */
    @ConfigDescription(
            description = "The callback timeout for virtual nodes"
                    + " and subscriptions in milliseconds",
            id = "transport.callbackTimeout" + "", defaultValue = "30000", restrictions = ">5")
    int getCallbackTimeout();
}
