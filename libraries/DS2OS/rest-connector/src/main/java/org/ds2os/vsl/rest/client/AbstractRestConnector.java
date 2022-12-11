package org.ds2os.vsl.rest.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMimeTypes;
import org.ds2os.vsl.core.transport.ExceptionMessage;
import org.ds2os.vsl.core.utils.AddressParser;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.UnexpectedErrorException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.rest.RestTransportContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for REST connectors.
 *
 * @author felix
 */
public abstract class AbstractRestConnector {

    /**
     * SLF4J logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRestConnector.class);

    /**
     * The base URL.
     */
    private final String baseURL;

    /**
     * The {@link RestTransportContext}.
     */
    private final RestTransportContext context;

    /**
     * Content type that has been preferred within the communication by the server.
     */
    private String preferredContentType;

    /**
     * Internal constructor for subclasses.
     *
     * @param baseURL
     *            the base URL.
     * @param context
     *            the {@link RestTransportContext}.
     */
    protected AbstractRestConnector(final String baseURL, final RestTransportContext context) {
        if (baseURL.endsWith("/")) {
            this.baseURL = baseURL.substring(0, baseURL.length() - 1);
        } else {
            this.baseURL = baseURL;
        }
        this.context = context;
    }

    /**
     * Get the used {@link RestTransportContext}.
     *
     * @return the context.
     */
    protected final RestTransportContext getRestTransportContext() {
        return context;
    }

    /**
     * Get the URI of an address based on the base URL.
     *
     * @param address
     *            the address.
     * @return the URI.
     */
    protected final URI getURI(final String address) {
        try {
            if (address.startsWith("/")) {
                return new URI(baseURL + "/vsl" + address);
            } else {
                return new URI(baseURL + "/vsl/" + address);
            }
        } catch (final URISyntaxException e) {
            LOG.error("URI syntax exception during getURI:", e);
            throw new IllegalArgumentException(
                    "Invalid URI with base URL " + baseURL + " and address " + address, e);
        }
    }

    /**
     * Get the URI of an address based on the base URL.
     *
     * @param address
     *            the address.
     * @param params
     *            Parameters attached to the request. Ignored if null.
     * @return the URI.
     */
    protected final URI getURI(final String address, final VslAddressParameters params) {
        String query = address;
        if (params != null) {
            query += AddressParser.getParametersAsURIQuery(params.getParametersAsMap());
        }
        return getURI(query);
    }

    /**
     * Get the URI of a global operation based on the base URL.
     *
     * @param operation
     *            the operation.
     * @return the URI.
     */
    protected final URI getOperationURI(final String operation) {
        try {
            return new URI(baseURL + "/" + operation);
        } catch (final URISyntaxException e) {
            LOG.error("URI syntax exception during getOperationURI:", e);
            throw new IllegalArgumentException(
                    "Invalid URI with base URL " + baseURL + " and operation " + operation, e);
        }
    }

    /**
     * Get the URI for callback registration.
     *
     * @return the callbacks URI.
     */
    protected final URI getCallbacksURI() {
        try {
            return new URI(baseURL.replaceFirst("https://", "wss://") + "/callbacks");
        } catch (final URISyntaxException e) {
            LOG.error("URI syntax exception during getCallbacksURI:", e);
            throw new IllegalArgumentException("Invalid callback URI built.", e);
        }
    }

    /**
     * Helper to send the given request and write a body object using the negotiated serialization.
     *
     * @param request
     *            the request to send.
     * @param body
     *            the body object to serialize and send.
     * @param <T>
     *            the body object's type.
     * @throws IOException
     *             If an I/O error occurs.
     */
    protected final <T> void sendWithBody(final RestTransportRequest request, final T body)
            throws IOException {
        final VslMapper mapper;
        if (preferredContentType == null || "".equals(preferredContentType)) {
            mapper = context.getMapper(VslMimeTypes.JSON);
        } else {
            mapper = context.getMapper(preferredContentType);
        }
        if (mapper == null) {
            LOG.error("Mapper is null. Preferred content type was {}.", preferredContentType);
            throw new IOException("Could not find suitable mapper for the content type.");
        }

        final OutputStream output;
        if ("".equals(mapper.getContentEncoding())) {
            output = request.getRequestStream(mapper.getContentType());
        } else {
            output = request
                    .getRequestStream(mapper.getContentType() + "; " + mapper.getContentEncoding());
        }

        request.send();

        try {
            mapper.writeValue(output, body);
        } finally {
            output.close();
        }
    }

    /**
     * Read the body of an response from a sent REST request.
     *
     * @param request
     *            the request, which must be already sent.
     * @param httpCode
     *            the HTTP code retrieved as response. The code is not checked.
     * @param responseType
     *            the type of the response body for serialization.
     * @param <T>
     *            the body object's type.
     * @return the deserialized body.
     * @throws IOException
     *             If an I/O error occurs.
     */
    protected final <T> T readResponse(final RestTransportRequest request, final int httpCode,
            final Class<T> responseType) throws IOException {
        final String responseContentType = request.getResponseType();
        final VslMapper mapper;
        if (responseContentType == null) {
            throw new IOException("HTTP response with code " + httpCode
                    + " did not include Content-Type header.");
        } else {
            final String contentType = responseContentType.split(";")[0];
            mapper = context.getMapper(contentType);
            if (mapper == null) {
                throw new IOException("No mapper available for content type " + contentType);
            }
            if (preferredContentType == null) {
                preferredContentType = contentType;
            }
        }
        return mapper.readValue(request.getResponseStream(), responseType);
    }

    /**
     * Translates a received error to a {@link VslException}.
     *
     * @param httpCode
     *            The HTTP code to parse.
     * @param request
     *            The RestTransportRequest
     * @return The VslException.
     */
    protected final VslException toVslException(final int httpCode,
            final RestTransportRequest request) {
        try {
            return readResponse(request, httpCode, ExceptionMessage.class).toException();
        } catch (final IOException e) {
            return toVslException(e);
        }
    }

    /**
     * Creates an {@link VslException} out of an {@link IOException}.
     *
     * @param ioException
     *            The given {@link IOException}
     * @return A {@link VslException} object.
     */
    protected final VslException toVslException(final IOException ioException) {
        // FIXME: proper exceptions!
        return new VslException(ioException) {
            private static final long serialVersionUID = 720279036202122373L;

            @Override
            public byte getErrorCodeMajor() {
                return 5;
            }

            @Override
            public byte getErrorCodeMinor() {
                return 0;
            }
        };
    }
}
