package org.ds2os.vsl.multicasttransport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.ds2os.vsl.agentregistry.cryptography.SymmetricCryptographyHandler;
import org.ds2os.vsl.core.AbstractVslModule;
import org.ds2os.vsl.core.VslAgentRegistryService;
import org.ds2os.vsl.core.VslAlivePing;
import org.ds2os.vsl.core.VslAlivePingHandler;
import org.ds2os.vsl.core.VslAlivePingSender;
import org.ds2os.vsl.core.VslKASyncConnector;
import org.ds2os.vsl.core.VslKORSyncHandler;
import org.ds2os.vsl.core.VslKORUpdate;
import org.ds2os.vsl.core.VslKORUpdateSender;
import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslRequestHandler;
import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.core.VslTransport;
import org.ds2os.vsl.core.VslTransportConnector;
import org.ds2os.vsl.core.VslX509Authenticator;
import org.ds2os.vsl.core.config.VslMulticastTransportConfig;
import org.ds2os.vsl.core.impl.AlivePing;
import org.ds2os.vsl.core.impl.TransportConnector;
import org.ds2os.vsl.exception.KeyNotInKeystoreException;
import org.ds2os.vsl.exception.NumberOfSendersOverflowException;
import org.ds2os.vsl.multicasttransport.fragmentation.Assembler;
import org.ds2os.vsl.multicasttransport.fragmentation.AssemblerCallback;
import org.ds2os.vsl.multicasttransport.fragmentation.Fragmenter;
import org.ds2os.vsl.netutils.KeyStringParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Multicast Transport module main class.
 *
 * @author felix
 * @author Johannes Straßer
 */
public final class MulticastTransport extends AbstractVslModule implements VslAlivePingSender,
        VslTransport, BroadcastReceiver, AssemblerCallback, VslKORUpdateSender {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MulticastTransport.class);

    /**
     * Value for the type field of a KORUpdate message.
     */
    private static final byte MESSAGETYPE_KORUPDATE = (byte) 0x00;

    /**
     * The {@link Broadcaster} doing the low-level I/O.
     */
    private final Broadcaster broadcaster;

    /**
     * The {@link VslAlivePingHandler} to handle incoming alive pings.
     */
    private final VslAlivePingHandler alivePingHandler;

    /**
     * The {@link VslMapper} to serialize data.
     */
    private final VslMapper mapper;

    /**
     * The {@link VslKORSyncHandler} to handle incoming {@link VslKORUpdate}.
     */
    private final VslKORSyncHandler korSyncHandler;

    /**
     * The agentRegistry used by this class.
     */
    private final VslAgentRegistryService agentRegistry;

    /**
     * The set of all used {@link VslTransportConnector}.
     */
    private final Set<VslTransportConnector> connectors;

    /**
     * A symmetric key store used by this class.
     */
    private final VslSymmetricKeyStore keyStore;

    /**
     * Configuration service used by this module.
     */
    private final VslMulticastTransportConfig config;

    /**
     * The Fragmenters that are used to create fragmentation layer packets.
     */
    private final Set<Fragmenter> fragmenters;

    /**
     * The Assembler that is used to process incoming fragmentation layer packets.
     */
    private Assembler assembler;

    /**
     * The ID of the certificate authority of the KA using this transport.
     */
    private final int certificateAuthorityID;

    /**
     * Constructor for a multicast transport to send Alive Pings and KORUpdates.
     *
     * TODO Currently, the MulticastTransport uses all available links. It would be more efficient
     * to restrict it to a set of links which could be stated in the configuration.
     *
     * @param port
     *            The UDP port to be used
     * @param ipv6MulticastAddr
     *            The IPv6 multicast address to use (optional)
     * @param alivePingHandler
     *            Callback for revceiving alive pings
     * @param mapper
     *            The {@link VslMapper} to serialize data.
     * @param agentRegistry
     *            The agentRegistryService to be used by this transport
     * @param korSyncHandler
     *            Callback for receiving KOR updates
     * @param config
     *            Configuration service providing certain values
     * @param certificateAuthority
     *            Certificate authority used by the KAs of the multicast network
     */
    public MulticastTransport(final int port, final Inet6Address ipv6MulticastAddr,
            final VslAlivePingHandler alivePingHandler, final VslMapper mapper,
            final VslAgentRegistryService agentRegistry, final VslKORSyncHandler korSyncHandler,
            final VslMulticastTransportConfig config,
            final VslX509Authenticator certificateAuthority) {
        this.broadcaster = new Broadcaster(port, ipv6MulticastAddr);
        this.alivePingHandler = alivePingHandler;
        this.mapper = mapper;
        this.korSyncHandler = korSyncHandler;
        this.connectors = new HashSet<VslTransportConnector>();
        this.agentRegistry = agentRegistry;
        this.keyStore = agentRegistry.getKeyStore();
        this.config = config;
        // This may be changed when the VslX509Authenticator provides a better way
        this.certificateAuthorityID = certificateAuthority.getCAPublicKey().hashCode();
        // Must wait for initialized broadcaster to get MTUs
        this.fragmenters = new HashSet<Fragmenter>();
        this.assembler = null;

    }

    @Override
    public void activate() throws Exception {
        broadcaster.initializeInterfaces(config);

        // Provide all URLs
        // TODO check if broadcaster.getSourceAdresses().keySet() may be better here
        for (final String url : broadcaster.getURLs()) {
            connectors.add(new TransportConnector(url));
        }

        // Acquire or create symmetric key
        byte[] key;
        String keyHash = agentRegistry.getMulticastGroupKeyHash();
        String keyString;
        if (keyHash.equals("")) {
            keyString = config.getTLSString();
            key = new KeyStringParser(keyString).createKey();
            keyHash = DatatypeConverter.printHexBinary(keyStore.generateKeyHash(key));
            agentRegistry.setMulticastGroupKey(DatatypeConverter.printHexBinary(key), keyHash,
                    keyString);
            LOGGER.info("Created a new symmetric key.");
        } else {
            key = DatatypeConverter.parseHexBinary(agentRegistry.getMulticastGroupKey(keyHash));
            keyString = agentRegistry.getMulticastGroupKey(keyHash);
            LOGGER.info("Loaded a symmetric key with hash {} from the KOR.", keyHash);
        }

        if (!keyStore.addKey(key, keyString)) {
            LOGGER.error("Could not add new key to the KeyStore.");
            throw new InvalidKeyException("Could not add new key to the KeyStore.");
        }

        // Initialize Fragmenters
        for (final Entry<String, Integer> entry : broadcaster.getSourceAdresses().entrySet()) {
            fragmenters.add(new Fragmenter(entry.getValue(), certificateAuthorityID, keyStore,
                    entry.getKey()));
            LOGGER.info("Initialized Fragmenter with keyHash {}, MTU {}, and source address {}.",
                    keyHash, entry.getValue(), entry.getKey());
        }

        // Initialize Assembler
        this.assembler = new Assembler(config.getMaxAuthorizedBufferSize(),
                this.config.getMaxUnauthorizedBufferSize(), config.getMaxSenders(), this, keyStore,
                config.getBufferStaleInterval(), this.certificateAuthorityID);

        // Start receiver after everything has been initialized
        broadcaster.startReceiver(this);
    }

    @Override
    public void shutdown() {
        broadcaster.shutdown();
    }

    /**
     * Takes data, fragments it and sends it out over the broadcaster. The data is sent using all
     * available Fragmenters. The data is authenticated and encrypted (if specified) with the key
     * belonging to the given keyHash. If this hash is null the current key is used.
     *
     * TODO This method uses all available Fragmenters. For selective sending additional methods
     * must be created.
     *
     * @param data
     *            The data to be sent
     * @param isEncrypted
     *            If the data is encrypted
     * @param keyHash
     *            Hash of the used key. If null the current key is used.
     * @throws KeyNotInKeystoreException
     *             Thrown if the given key is not in the keystore
     */
    private void physicallySend(final byte[] data, final boolean isEncrypted, final byte[] keyHash)
            throws KeyNotInKeystoreException {
        // Get final key hash
        byte[] finalKeyHash;
        if (keyHash == null) {
            finalKeyHash = DatatypeConverter
                    .parseHexBinary(this.agentRegistry.getMulticastGroupKeyHash());
        } else {
            finalKeyHash = keyHash;
        }

        // Encrypt if necessary
        byte[] realData;
        if (isEncrypted) {
            realData = SymmetricCryptographyHandler.encrypt(data, finalKeyHash, this.keyStore);
        } else {
            realData = data;
        }

        // Fragment and send data
        for (final Fragmenter fragmenter : fragmenters) {
            for (final byte[] packet : fragmenter.fragmentData(realData, isEncrypted,
                    finalKeyHash)) {
                try {
                    broadcaster.broadcast(packet, fragmenter.getSourceAddress());
                } catch (final IOException e) {
                    LOGGER.error("Error during packet broadcasting:", e);
                }
            }
        }
    }

    @Override
    public void received(final DatagramPacket packet, final InetAddress localAddress) {
        try {
            final byte[] data = Arrays.copyOfRange(packet.getData(), packet.getOffset(),
                    packet.getOffset() + packet.getLength());
            assembler.digest(data,
                    Broadcaster.addressToString(packet.getAddress(), packet.getPort()));
        } catch (final NumberOfSendersOverflowException e) {
            LOGGER.warn("Cannot process packet: Too many senders.", e);
        }
    }

    @Override
    public void sendAlivePing(final VslAlivePing alivePing) {
        try {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                mapper.writeValue(output, alivePing);
                LOGGER.debug("An alive ping is being sent...");
                physicallySend(output.toByteArray(), false, null);
            } catch (final KeyNotInKeystoreException e) {
                // Should not happen
                LOGGER.error("KeyNotInKeystoreException raised:", e);
            } finally {
                output.close();
            }
        } catch (final IOException e) {
            LOGGER.error("Error during alive ping broadcasting:", e);
        }
    }

    @Override
    public Collection<VslTransportConnector> getConnectors() {
        return Collections.unmodifiableCollection(connectors);
    }

    @Override
    public void messageComplete(final byte[] messageData, final byte[] keyHash,
            final boolean isEncrypted) {
        // All unencrypted messages are AlivePings
        if (!isEncrypted) {
            processAlivePing(messageData, (keyHash != null));
            return;
        }

        // Filter messages that were encrypted using unknown keys or simply had invalid MACS
        if (keyHash == null) {
            LOGGER.warn("Received an encrypted message using an unknown keyHash.");
            return;
        }

        // Decrypt data
        byte[] realData;
        try {
            realData = SymmetricCryptographyHandler.decrypt(messageData, keyHash, this.keyStore);
        } catch (final KeyNotInKeystoreException e) {
            LOGGER.error("Could not decrypt due to unknown key: " + e.getMessage());
            return;
        }

        // Chop off first byte and sort by type
        final byte type = realData[0];
        final byte[] payload = Arrays.copyOfRange(realData, 1, realData.length);
        switch (type) {
        default:
            LOGGER.error("Received message using the unknown message type {}.", type);
            break;
        case (MESSAGETYPE_KORUPDATE):
            processKORUpdate(payload);
            break;
        }

    }

    /**
     * Takes an data array, extracts an AlivePing, and returns the ping to the alivePingHandler.
     *
     * @param messageData
     *            The data array containing the AlivePing
     * @param isAuthenticated
     *            If the alive ping is authenticated
     */
    private void processAlivePing(final byte[] messageData, final boolean isAuthenticated) {
        VslAlivePing alivePing;
        try {
            alivePing = mapper.readValue(
                    new ByteArrayInputStream(messageData, 0, messageData.length), AlivePing.class);
            LOGGER.debug("An alive ping is being received...");
            alivePingHandler.handleAlivePing(alivePing, isAuthenticated);
        } catch (final IOException e) {
            LOGGER.error("IOException during reading the received alive ping:", e);
        }
    }

    /**
     * Takes an data array, extracts a VslKORUpdate and returns the update to the korSyncHandler.
     *
     * @param messageData
     *            The data array containing the VslKORUpdate
     */
    private void processKORUpdate(final byte[] messageData) {
        try {
            LOGGER.info("A KOR update of length {} is being received...", messageData.length);
            final VslKORUpdate korUpdate = mapper.readValue(
                    new ByteArrayInputStream(messageData, 0, messageData.length),
                    VslKORUpdate.class);
            korSyncHandler.handleKORUpdate(korUpdate);
        } catch (final IOException e) {
            LOGGER.error("IOException during reading the received alive ping:", e);
        }
    }

    @Override
    public void sendKORUpdate(final VslKORUpdate korUpdate) {
        try {
            sendKORUpdate(korUpdate, null);
        } catch (final KeyNotInKeystoreException e) {
            // Should not happen
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public void sendKORUpdate(final VslKORUpdate korUpdate, final byte[] keyHash)
            throws KeyNotInKeystoreException {
        try {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                output.write(MESSAGETYPE_KORUPDATE);
                mapper.writeValue(output, korUpdate);
                final byte[] bytes = output.toByteArray();
                LOGGER.info("A KOR update of length {} is being sent.", bytes.length - 1);
                physicallySend(bytes, true, keyHash);
            } finally {
                output.close();
            }
        } catch (final IOException e) {
            LOGGER.error("Error during KOR update broadcasting:", e);
        }
    }

    @Override
    public VslRequestHandler createRequestHandler(final String... remoteURLs) {
        // This transport does not support request handling
        return null;
    }

    @Override
    public VslKASyncConnector createKASyncConnector(final String... remoteURLs) {
        // This transport does not support KA sync
        return null;
    }
}
