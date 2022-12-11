package org.ds2os.vsl.core.impl;

import org.ds2os.vsl.core.VslTransportConnector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Default implementation of {@link VslTransportConnector}.
 *
 * @author felix
 */
public final class TransportConnector implements VslTransportConnector {

    /**
     * The URL on which the transport is reachable.
     */
    private final String url;

    /**
     * Constructor with URL.
     *
     * @param url
     *            the URL on which the transport is reachable.
     */
    @JsonCreator
    public TransportConnector(@JsonProperty("url") final String url) {
        this.url = url;
    }

    @Override
    public String getURL() {
        return url;
    }
}
