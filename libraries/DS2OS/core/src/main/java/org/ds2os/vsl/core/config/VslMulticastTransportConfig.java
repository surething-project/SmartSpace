package org.ds2os.vsl.core.config;

/**
 * Configuration values belonging to the Multicast-Transport service.
 *
 * @author Johannes Straßer
 *
 */
public interface VslMulticastTransportConfig extends VslTransportConfig, VslAgentName {

    /**
     * Fetches the TLS string setting the mode of encryption and MACing for this module.
     *
     * @return The configured TLS string
     */
    @ConfigDescription(description = "TODO", id = "multicastTransport.tlsString"
            + "", defaultValue = "TLS_PSK_WITH_AES_256_CBC_SHA384", restrictions = "")
    String getTLSString();

    /**
     * Fetches the maximum number of senders this KA will process multicast messages of.
     *
     * @return The configured maximum number of senders
     */
    @ConfigDescription(description = "TODO", id = "multicastTransport.maxSenders"
            + "", defaultValue = "100", restrictions = "")
    int getMaxSenders();

    /**
     * Fetches the maximum size of the buffer that will be used per sender to cache incoming
     * authorized messages.
     *
     * @return The configured maximum size of the authorizedBuffer
     */
    @ConfigDescription(description = "TODO", id = "multicastTransport.maxAuthorizedBufferSize"
            + "", defaultValue = "50000000", restrictions = ">0")
    int getMaxAuthorizedBufferSize();

    /**
     * Fetches the maximum size of the buffer that will be used per sender to cache incoming
     * unauthorized messages.
     *
     * @return The configured maximum size of the unauthorizedBuffer
     */
    @ConfigDescription(description = "TODO", id = "multicastTransport.maxUnauthorizedBufferSize"
            + "", defaultValue = "10000", restrictions = "")
    int getMaxUnauthorizedBufferSize();

    /**
     * Fetches the interval after which a buffer will considered as stale.
     *
     * @return The configured buffer stale interval in milliseconds.
     */
    @ConfigDescription(description = "TODO", id = "multicastTransport.bufferStaleInterval"
            + "", defaultValue = "10000", restrictions = ">0")
    long getBufferStaleInterval();
}
