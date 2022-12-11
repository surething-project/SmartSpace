package org.ds2os.vsl.core.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ds2os.vsl.core.VslIdentity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Simple Identity Class that implements VslIdentity, based on ArrayList.
 *
 * @author liebald
 */
public final class ServiceIdentity implements VslIdentity {

    /**
     * The clientId of this identity.
     */
    private final String clientId;

    /**
     * Stores the ids this VslIdentity provides.
     */
    private final List<String> ids;

    /**
     * Constructor taking a String to initialize the internal id List.
     *
     * @param clientId
     *            The clientID of this Identity.
     * @param idString
     *            "; " separated list of strings which will be used as initial ids.
     */
    public ServiceIdentity(final String clientId, final String idString) {
        this.clientId = clientId;
        ids = new ArrayList<String>(Arrays.asList(idString.split(";")));
    }

    /**
     * Constructor taking a Collection to initialize the internal id List.
     *
     * @param clientId
     *            The clientID of this Identity.
     * @param idCollection
     *            A Collection of Strings as initial ids.
     */
    @JsonCreator
    public ServiceIdentity(@JsonProperty("clientId") final String clientId,
            @JsonProperty("accessIDs") final Collection<String> idCollection) {
        this.clientId = clientId;
        ids = new ArrayList<String>(idCollection);
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public Collection<String> getAccessIDs() {
        return ids;
    }

    /**
     * Adds an identity String to the stored identity collection.
     *
     * @param id
     *            The identity to add as String.
     */
    public void addIdentity(final String id) {
        if (!ids.contains(id)) {
            ids.add(id);
        }
    }

    /**
     * Removes an identity String from the stored identity collection.
     *
     * @param id
     *            The identity to remove as String.
     */
    public void removeIdentity(final String id) {
        if (ids.contains(id)) {
            ids.remove(id);
        }
    }

    @Override
    public boolean isKA() {
        return false;
    }
}
