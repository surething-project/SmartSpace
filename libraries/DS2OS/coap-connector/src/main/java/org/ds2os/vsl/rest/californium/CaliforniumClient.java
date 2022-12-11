package org.ds2os.vsl.rest.californium;

import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.UUID;

import org.apache.commons.lang3.NotImplementedException;
import org.ds2os.vsl.core.VslCallback;
import org.ds2os.vsl.exception.KeyNotInKeystoreException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.rest.RestTransportContext;
import org.ds2os.vsl.rest.client.RestTransportClient;
import org.ds2os.vsl.rest.client.RestTransportRequest;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic Californium-based CoAP client for VSL REST transports.
 *
 * @author felix
 */
public class CaliforniumClient implements RestTransportClient {

    /**
     * SLF4J logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(CaliforniumClient.class);

    /**
     * The Californium {@link CoapClient} instance.
     */
    private final CoapClient coapClient;

    /**
     * Create a new Californium client instance for executing requests.
     *
     * @param context
     *            the {@link RestTransportContext} with common configuration and tools.
     * @throws VslException
     *             If the initialization of the client fails.
     */
    public CaliforniumClient(final RestTransportContext context) throws VslException {
        try {
            final ScandiumContext scandium = new ScandiumContext(context);

            coapClient = new CoapClient();
            coapClient.setEndpoint(
                    new CoapEndpoint(scandium.getDtlsConnector(), NetworkConfig.getStandard()));
        } catch (final GeneralSecurityException e) {
            // TODO: dirty!
            LOG.error("DTLS initialization failed with exception:", e);
            throw new KeyNotInKeystoreException("DTLS initialization failed");
        }
    }

    @Override
    public final RestTransportRequest doGet(final URI uri) {
        return new CaliforniumClientRequest(coapClient, Request.newGet().setURI(uri.toString()));
    }

    @Override
    public final RestTransportRequest doPut(final URI uri) {
        return new CaliforniumClientRequest(coapClient, Request.newPut().setURI(uri.toString()));
    }

    @Override
    public final RestTransportRequest doPost(final URI uri) {
        return new CaliforniumClientRequest(coapClient, Request.newPost().setURI(uri.toString()));
    }

    @Override
    public final UUID registerCallback(final URI uri, final VslCallback callback)
            throws VslException {
        // FIXME: implement stuff
        throw new NotImplementedException("Callbacks not implemented on CoAP.");
    }
}
