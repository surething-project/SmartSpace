package org.ds2os.vsl.core;

import java.util.Collection;

import org.ds2os.vsl.core.impl.HandshakeData;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Data object which is exchanged during handshake (bidirectional).
 *
 * @author felix
 * @author liebald
 */
@JsonTypeInfo(use = Id.CLASS, include = As.EXISTING_PROPERTY, defaultImpl = HandshakeData.class)
public interface VslHandshakeData {

    /**
     * Collection of KAs in this network.
     *
     * @return collection of {@link VslKAInfo} objects.
     */
    Collection<VslKAInfo> getKaInfo();

    /**
     * Get the new group key for multicast transports, if the sending KA is the new leader.
     *
     * @return the new group key or null, if the sending KA won't change the key.
     */
    String getNewGroupKey();

    /**
     * Get the TLSString associated with the key.
     *
     * @return return the TLSString associated with the key.
     */
    String getNewTLSString();
}
