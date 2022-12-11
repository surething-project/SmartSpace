package org.ds2os.vsl.core;

import java.util.Collection;

import org.ds2os.vsl.core.impl.ServiceIdentity;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Identity of an service which issues operations on the VSL.
 *
 * @author felix
 */
@JsonTypeInfo(use = Id.CLASS, include = As.EXISTING_PROPERTY, defaultImpl = ServiceIdentity.class)
public interface VslIdentity {

    /**
     * Get the client identifier of the service.
     *
     * @return the client id.
     */
    String getClientId();

    /**
     * Get the access identifiers this identity belongs to.
     *
     * @return Collection of the identifiers, represented by strings.
     */
    Collection<String> getAccessIDs();

    /**
     * Returns whether this {@link VslIdentity} belongs to a KA.
     *
     * @return true if the {@link VslIdentity} belongs to a KA, false otherwise.
     */
    boolean isKA();
}
