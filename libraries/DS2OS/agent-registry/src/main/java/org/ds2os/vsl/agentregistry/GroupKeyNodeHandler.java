package org.ds2os.vsl.agentregistry;

import javax.xml.bind.DatatypeConverter;

import org.ds2os.vsl.core.VslAgentRegistryService;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.adapter.VirtualNodeAdapter;
import org.ds2os.vsl.core.node.VslNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VslVirtualNodeHandler that monitors the /newGrupKey node for changes and updates the groupKey if
 * necessary (and possible).
 */
class GroupKeyNodeHandler extends VirtualNodeAdapter implements VslVirtualNodeHandler {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupKeyNodeHandler.class);

    /**
     * The agentRegistry this class stores data in.
     */
    private final VslAgentRegistryService agentRegistry;

    /**
     * Time of the last Update of the groupKey. New keys must have a newer timestamp associated to
     * them, otherwise they are not accepted.
     */
    private long lastUpdate;

    /**
     * Simple constructor.
     *
     * @param agentRegistry
     *            The agentRegistry this class stores data in
     */
    protected GroupKeyNodeHandler(final VslAgentRegistryService agentRegistry) {
        this.agentRegistry = agentRegistry;
        this.lastUpdate = System.currentTimeMillis();
    }

    @Override
    public void set(final String address, final VslNode value, final VslIdentity identity) {
        /*
         * Timestamp of the creation of the node must be newer than from the last key that was set.
         */
        if (value.getTimestamp() == null) {
            LOGGER.error(
                    "timestamp of new groupkey wasn't set. Must be the time when it was created.");
            return;
        }
        if (value.getTimestamp().getTime() <= lastUpdate) {
            LOGGER.error("Incoming key was too old ({}), a newer one was already set ({})",
                    value.getTimestamp().getTime(), lastUpdate);
            return;
        }

        /*
         * Value of the node must have the form <TLS_STRING>,<Hex encoded symmetric key>
         */
        final String[] keyParts = value.getValue().split(",");
        if (keyParts.length != 2) {
            LOGGER.error("Incoming key was malformed: Too many parts.");
            return;
        }
        final byte[] key = DatatypeConverter.parseHexBinary(keyParts[1]);
        if (!this.agentRegistry.getKeyStore().addKey(key, keyParts[0])) {
            LOGGER.error("Incoming key was malformed: Could not be added to keyStore");
            return;
        }

        /*
         * FIXME This method also needs to make sure that old keys get invalidated. The key that is
         * being replaced may be kept valid in a transition phase, but this phase must be shorter
         * that the time @link{org.ds2os.vsl.multicasttransport.fragmentation.SubAssembler} need to
         * become stale.
         */
        final String keyHash = DatatypeConverter
                .printHexBinary(this.agentRegistry.getKeyStore().generateKeyHash(key));
        this.agentRegistry.setMulticastGroupKey(keyParts[1], keyHash, keyParts[0]);
        lastUpdate = value.getTimestamp().getTime();
        LOGGER.info("Updated the groupKey. New hash: {}", keyHash);
    }
}
