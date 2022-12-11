package org.ds2os.vsl.core;

import org.ds2os.vsl.core.impl.TransportConnector;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Description of a transport module's connector, i.e. one URL with which this transport can be used
 * (usually among others).
 *
 * @author felix
 */
@JsonTypeInfo(use = Id.CLASS, include = As.EXISTING_PROPERTY, defaultImpl = TransportConnector.class)
public interface VslTransportConnector {

    /**
     * Every transport must be accessible by some URL.
     *
     * @return the URL of the transport.
     */
    String getURL();
}
