package org.ds2os.vsl.rest;

import java.io.IOException;

import org.ds2os.vsl.core.VslHandshakeData;
import org.ds2os.vsl.core.VslKASyncConnector;
import org.ds2os.vsl.core.VslKORUpdate;
import org.ds2os.vsl.core.impl.HandshakeData;
import org.ds2os.vsl.core.impl.KORUpdateRequest;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.rest.client.AbstractRestConnector;
import org.ds2os.vsl.rest.client.RestTransportClient;
import org.ds2os.vsl.rest.client.RestTransportRequest;

/**
 * {@link VslKASyncConnector} implementation for the REST transport.
 *
 * @author felix
 */
public final class RestKASyncConnector extends AbstractRestConnector implements VslKASyncConnector {

    /**
     * The REST client to use.
     */
    private final RestTransportClient client;

    /**
     * Create a new {@link VslKASyncConnector} from a {@link RestTransportClient}.
     *
     * @param client
     *            the {@link RestTransportClient}.
     * @param baseURL
     *            the base URL of the KA.
     * @param helper
     *            the REST transport helper.
     */
    public RestKASyncConnector(final RestTransportClient client, final String baseURL,
            final RestTransportContext helper) {
        super(baseURL, helper);
        this.client = client;
    }

    @Override
    public VslHandshakeData doHandshake(final VslHandshakeData localData) throws VslException {
        final RestTransportRequest request = client.doPost(getOperationURI("ka/handshake"));
        request.accept(getRestTransportContext().getContentTypePreference());
        try {
            sendWithBody(request, localData);

            final int httpCode = request.syncRequest();
            if (httpCode != 200) {
                throw toVslException(httpCode, request);
            }

            return readResponse(request, httpCode, HandshakeData.class);
        } catch (final IOException e) {
            throw toVslException(e);
        }
    }

    @Override
    public VslKORUpdate requestUpdate(final String kaName, final String hashFrom)
            throws VslException {
        final RestTransportRequest request = client.doPost(getOperationURI("ka/requestKorUpdate"));
        request.accept(getRestTransportContext().getContentTypePreference());
        try {
            sendWithBody(request, new KORUpdateRequest(kaName, hashFrom));

            final int httpCode = request.syncRequest();
            if (httpCode != 200) {
                throw toVslException(httpCode, request);
            }

            return readResponse(request, httpCode, VslKORUpdate.class);
        } catch (final IOException e) {
            throw toVslException(e);
        }
    }
}
