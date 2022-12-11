package org.ds2os.vsl.core.impl;

import java.util.Collection;
import java.util.Collections;

import org.ds2os.vsl.core.VslHandshakeData;
import org.ds2os.vsl.core.VslKAInfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Implementation of {@link VslHandshakeData}.
 *
 * @author felix
 */
public class HandshakeData implements VslHandshakeData {

    /**
     * Collection of all contained {@link VslKAInfo} objects.
     */
    private final Collection<VslKAInfo> kaInfo;

    /**
     * The new group key, if any.
     */
    private final String newGroupKey;

    /**
     * The tlsString associated with the newGroupKey.
     */
    private final String newTLSString;

    /**
     * Creates a new HandshakeData object.
     *
     * @param kaInfo
     *            the collection of all contained {@link VslKAInfo} objects.
     * @param newGroupKey
     *            the new group key, if any.
     * @param newTLSString
     *            The tlsString associated with the newGroupKey.
     */
    @JsonCreator
    public HandshakeData(@JsonProperty("kaInfo") final Collection<VslKAInfo> kaInfo,
            @JsonProperty("newGroupKey") final String newGroupKey,
            @JsonProperty("newTLSString") final String newTLSString) {
        if (kaInfo == null) {
            this.kaInfo = Collections.emptySet();
        } else {
            this.kaInfo = Collections.unmodifiableCollection(kaInfo);
        }
        this.newGroupKey = newGroupKey;
        this.newTLSString = newTLSString;
    }

    @Override
    public final Collection<VslKAInfo> getKaInfo() {
        return kaInfo;
    }

    @Override
    public final String getNewGroupKey() {
        return newGroupKey;
    }

    @Override
    public final String getNewTLSString() {
        return newTLSString;
    }
}
