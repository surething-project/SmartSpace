package org.ds2os.vsl.aliveping;

import java.sql.Time;

import org.ds2os.vsl.core.AbstractVslModule;
import org.ds2os.vsl.core.VslAgentRegistryService;
import org.ds2os.vsl.core.VslAlivePing;
import org.ds2os.vsl.core.VslAlivePingHandler;
import org.ds2os.vsl.core.VslAlivePingSender;
import org.ds2os.vsl.core.VslKORHash;
import org.ds2os.vsl.core.VslKORSyncHandler;
import org.ds2os.vsl.core.VslTransportManager;
import org.ds2os.vsl.core.VslX509Authenticator;
import org.ds2os.vsl.core.config.VslAgentName;
import org.ds2os.vsl.core.config.VslAlivePingConfig;
import org.ds2os.vsl.core.impl.AlivePing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class processes incoming AlivePings, stores their content, and initiates group merges if
 * necessary.
 *
 * @author jay
 * @author Johannes Straßer
 * @author liebald
 */
public class AlivePingHandler extends AbstractVslModule implements Runnable, VslAlivePingHandler {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AlivePingHandler.class);

    /**
     * {@link VslTransportManager} instance.
     */
    private final VslTransportManager transportManager;

    /**
     * {@link VslAgentName} variable.
     */
    private final VslAlivePingConfig config;

    /**
     * Certificate validator to be used for authentication of CA-Pubkey and certificate.
     */
    private final VslX509Authenticator validator;

    /**
     * {@link VslKORHash} object to be used for obtaining the current Korhash in order to include it
     * to sent alivepings.
     */
    private final VslKORHash korHash;

    /**
     * AgentRegistryService object to be used to store AlivePing data.
     */
    private final VslAgentRegistryService agentRegistry;

    /**
     * Lock object to make sure alivePings are handled sequentially.
     */
    private final Object pingHandlerLock = new Object();

    /**
     * AlivePingSender thread.
     */
    final Thread pingSenderThread;

    /**
     * The korSyncHandler used for updates if required.
     */
    private final VslKORSyncHandler korSyncHandler;

    /**
     * Constructor with injection of necessary components.
     *
     * @param transportManager
     *            the transport manager. Used to broadcast {@link VslAlivePing} messages and to
     *            retrieve transport list.
     * @param korHash
     *            the {@link VslKORHash} used to retrive the current local KOR hash
     * @param certAuth
     *            {@link VslX509Authenticator} used for performing authentication of Pub Key.
     * @param config
     *            {@link VslAgentName} used to get agent name of the KA.
     * @param agentService
     *            {@link VslAgentRegistryService} to store received AlivePings
     * @param korSyncHandler
     *            {@link VslKORSyncHandler} to check if a korupdate is required.
     */
    public AlivePingHandler(final VslTransportManager transportManager,
            final VslX509Authenticator certAuth, final VslKORHash korHash,
            final VslAgentRegistryService agentService, final VslAlivePingConfig config,
            VslKORSyncHandler korSyncHandler) {
        this.transportManager = transportManager;
        this.validator = certAuth;
        this.korHash = korHash;
        this.agentRegistry = agentService;
        this.config = config;
        this.korSyncHandler = korSyncHandler;
        pingSenderThread = new Thread(this);
    }

    @Override
    public final void handleAlivePing(final VslAlivePing alivePing, final boolean isAuthenticated) {
        LOGGER.debug("Handling an alive ping from {} (authenticated: {})", alivePing.getAgentId(),
                isAuthenticated);

        synchronized (pingHandlerLock) {
            if (alivePing.getAgentId().equals(config.getAgentName())) {
                // received an alivePing from own KA, ignore.
                return;
            }

            if (agentRegistry.isAgentConnected(alivePing.getAgentId())) {
                /*
                 * Handle ping from connected KAs
                 */
                if (isAuthenticated) {
                    agentRegistry.storeAlivePingToConnectedKAs(alivePing,
                            new Time(System.currentTimeMillis()));
                    // check if an korUpdate is required.
                    korSyncHandler.checkKORUpdate(alivePing.getAgentId(), alivePing.getKorHash());
                    LOGGER.debug(
                            "Updated information for the agent " + alivePing.getAgentId() + ".");
                    // } else {
                    // LOGGER.warn("Received unauthenticated alivePing from the connected agent {}",
                    // alivePing.getAgentId());
                    // // FIXME: storing an unauthenticated aliveping is a workaround. currently the
                    // // groupkey distribution may fail due to not existing transports, leading to
                    // // unauthenticated alive pings which can contain valid transports again.
                    // // 29.5.: commented out, group key distribution should be somehow stable
                    // // currently
                    // agentRegistry.storeAlivePingToConnectedKAs(alivePing,
                    // new Time(System.currentTimeMillis()));
                }
            } else {
                /*
                 * Handle ping from unconnected KAs.
                 */

                // Check if the sending KA uses the same CA
                if (!alivePing.getCaPub().equals(validator.getCAPublicKey())) {
                    LOGGER.info("Received an AlivePing from a KA ({}) using another CA.",
                            alivePing.getAgentId());
                    return;
                }

                // Add unconnected KAs with the same groupID to the connected list
                if (alivePing.getGroupID().contentEquals(agentRegistry.getMulticastGroupKeyHash())
                        && isAuthenticated) {
                    LOGGER.info(
                            "Adding previously unconnected KA {} to connected list."
                                    + " Storing AlivePing to connected KA list.",
                            alivePing.getAgentId());
                    /*
                     * TODO the certificate expiration is a dummy value. Is this even needed here?
                     */
                    agentRegistry.addAgentToConnectedKAList(alivePing.getAgentId(),
                            new Time(System.currentTimeMillis()),
                            new Time(System.currentTimeMillis() + 1000 * 1000));
                    agentRegistry.storeAlivePingToConnectedKAs(alivePing,
                            new Time(System.currentTimeMillis()));
                    return;
                }

                // Store AlivePing in the KOR.
                LOGGER.info("Agent ({}) is not connected. Storing the AlivePing "
                        + "to unconnected KA list.", alivePing.getAgentId());
                agentRegistry.addAgentToUnconnectedKAList(alivePing.getAgentId(),
                        new Time(System.currentTimeMillis()));
                agentRegistry.storeAlivePingToUnConnectedKAs(alivePing,
                        new Time(System.currentTimeMillis()));
            }
        }
    }

    @Override
    public final void run() {
        try {
            while (!Thread.interrupted()) {
                LOGGER.debug("alivePingIntervall: " + config.getAlivePingIntervall());
                if (config.getAlivePingIntervall() == 0) {

                    Thread.sleep(2000);
                } else {
                    Thread.sleep(config.getAlivePingIntervall() * 1000);
                    sendPing();
                }
            }
        } catch (final InterruptedException e) {
            LOGGER.info("AlivePing Thread interrupted.");
        }

    }

    /**
     * Sends out an AlivePing on all known transports.
     */
    private void sendPing() {
        final AlivePing pingMessage = new AlivePing(config.getAgentName(),
                this.agentRegistry.getNetworkSize(), validator.getCAPublicKey(),
                transportManager.getAllTransportConnectors(),
                agentRegistry.getMulticastGroupKeyHash(), korHash.getCurrentKORHash());
        for (final VslAlivePingSender sender : this.transportManager.getAlivePingSenders()) {
            sender.sendAlivePing(pingMessage);
        }
        LOGGER.debug("Sent an AlivePing");
    }

    /**
     * Used to broadcast the {@link VslAlivePing} message over the transport.
     */
    private void sendPingThread() {
        LOGGER.info("Starting AlivePing sender thread.");

        pingSenderThread.setName("PingSender");
        pingSenderThread.setDaemon(true);
        pingSenderThread.start();
    }

    @Override
    public final void activate() {
        this.sendPingThread();
    }

    @Override
    public final void shutdown() {
        pingSenderThread.interrupt();
        try {
            pingSenderThread.join();
        } catch (final InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

}
