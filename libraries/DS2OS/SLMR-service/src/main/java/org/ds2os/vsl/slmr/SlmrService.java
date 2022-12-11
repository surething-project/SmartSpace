package org.ds2os.vsl.slmr;

import org.ds2os.vsl.core.AbstractVslModule;
import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslServiceManifest;
import org.ds2os.vsl.core.config.VslModelRepositoryConfig;
import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Site Local Model Repository that allows for querying for models via virtualNodes.
 *
 * @author liebald
 */
public class SlmrService extends AbstractVslModule {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SlmrService.class);

    /**
     * The {@link VslConnector} used for accessing the local KA.
     */
    private final VslConnector con;

    /**
     * Access to the model repository related configuration.
     */
    private final VslModelRepositoryConfig config;

    /**
     * Model ID of the service.
     */
    private static final String MODEL_ID = "/system/slmr";

    /**
     * Address of the service.
     */
    private String serviceAddress;

    /**
     * Service manifest object. At the moment, created dummy instance.
     */
    private static final VslServiceManifest MANIFEST = new VslServiceManifest() {

        @Override
        public String getModelId() {
            return MODEL_ID;
        }

        @Override
        public String getModelHash() {
            return "";
        }

        @Override
        public String getBinaryHash() {
            return "";
        }
    };

    /**
     * Constructor.
     *
     * @param con
     *            The {@link VslConnector} used for accessing the Vsl.
     * @param config
     *            Access to the model repository related configuration.
     */
    public SlmrService(final VslConnector con, final VslModelRepositoryConfig config) {
        this.con = con;
        this.config = config;

    }

    @Override
    public final void activate() throws Exception {
        try {
            serviceAddress = con.registerService(MANIFEST);
            LOGGER.debug("Registered slmr service at {}", serviceAddress);
        } catch (final VslException e) {
            LOGGER.error("Error on slmr service registration: ", e);
            throw e;
        }
        try {
            con.registerVirtualNode(serviceAddress, new SlmrHandler(serviceAddress, config, con));
            LOGGER.debug("Registered virtual Node {}", serviceAddress);
        } catch (final VslException e) {
            LOGGER.error("Error on registering virtual node at {} ", serviceAddress, e);
            throw e;
        }
    }

    @Override
    public void shutdown() {
    }

}
