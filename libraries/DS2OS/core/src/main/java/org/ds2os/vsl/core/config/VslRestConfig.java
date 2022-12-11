package org.ds2os.vsl.core.config;

/**
 * Interface for generic REST related configurations.
 *
 * @author felix
 */
public interface VslRestConfig extends VslTransportConfig {

    /**
     * Get the port of the RESTful transports.
     *
     * @return the port number.
     */
    @ConfigDescription(description = "The port on which the RESTful transports will listen.",
            id = "transport.rest.port", defaultValue = "8080")
    int getPort();

    /**
     * Get the content type preference ordered from the most wanted to the least wanted type.
     * Unsupported types will be ignored and supported types not listed here will be supported if
     * requested by the client, but not requested from a server. Entries are comma separated.
     *
     * @return the list of content types, separated with commas.
     */
    @ConfigDescription(
            description = "The content type preference ordered from the most wanted to the least"
                    + " wanted type. Unsupported types will be ignored and supported types not"
                    + " listed here will be supported if requested by the client, but not"
                    + " requested from a server. Entries are comma separated.",
            id = "transport.rest.contentTypePreference",
            defaultValue = "application/cbor,application/json")
    String getContentTypePreference();
}
