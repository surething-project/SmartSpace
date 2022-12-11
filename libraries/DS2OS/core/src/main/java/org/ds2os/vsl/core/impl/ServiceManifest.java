package org.ds2os.vsl.core.impl;

import org.ds2os.vsl.core.VslServiceManifest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Implementation of {@link VslServiceManifest}.
 *
 * @author felix
 */
public final class ServiceManifest implements VslServiceManifest {

    /**
     * Model identifier of the service's model.
     */
    private final String modelId;

    /**
     * Hash of the service's model.
     */
    private final String modelHash;

    /**
     * Hash of the service's binary, if applicable.
     */
    private final String binaryHash;

    /**
     * Constructor for creating a new service manifest with supplied values.
     *
     * @param modelId
     *            the model identifier of the service's model.
     * @param modelHash
     *            the hash of the service's model.
     * @param binaryHash
     *            the hash of the service's binary, if applicable.
     */
    @JsonCreator
    public ServiceManifest(@JsonProperty("modelId") final String modelId,
            @JsonProperty("modelHash") final String modelHash,
            @JsonProperty("binaryHash") final String binaryHash) {
        this.modelId = modelId;
        this.modelHash = modelHash;
        this.binaryHash = binaryHash;
    }

    @Override
    public String getModelId() {
        return modelId;
    }

    @Override
    public String getModelHash() {
        return modelHash;
    }

    @Override
    public String getBinaryHash() {
        return binaryHash;
    }
}
