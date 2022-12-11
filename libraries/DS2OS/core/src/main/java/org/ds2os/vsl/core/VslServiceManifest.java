package org.ds2os.vsl.core;

/**
 * Manifest of a service with its model and other service parameters.
 *
 * @author felix
 */
public interface VslServiceManifest {

    /**
     * Get the model id of the service.
     *
     * @return the model id.
     */
    String getModelId();

    /**
     * Get a hash of the model of the service.
     *
     * @return the model's hash.
     */
    String getModelHash();

    /**
     * Get a hash of the binary of the service.
     *
     * @return the binary's hash.
     */
    String getBinaryHash();
}
