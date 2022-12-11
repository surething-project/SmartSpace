package org.ds2os.vsl.core;

import org.ds2os.vsl.core.impl.AlivePing;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Alive ping data object.
 *
 * @author felix
 */
@JsonTypeInfo(use = Id.CLASS, include = As.EXISTING_PROPERTY, defaultImpl = AlivePing.class)
public interface VslAlivePing extends VslKAInfo {

    /**
     * Get the number of KAs in the established overlay.
     *
     * @return the number of KAs in the established overlay.
     */
    int getNumKAs();

    /**
     * Get the public key of the CA certificate.
     *
     * @return public key information as string.
     */
    String getCaPub();

    /**
     * Get the groupID of the KA. At the moment this is the hash of the groupKey that is currently
     * in use.
     *
     * @return groupID as a string.
     */
    String getGroupID();
}
