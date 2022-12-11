package org.ds2os.vsl.rest.californium;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.rest.client.RestTransportRequest;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;

/**
 * Implementation of the {@link RestTransportRequest} interface using Californium for CoAP.
 *
 * @author felix
 */
public final class CaliforniumClientRequest implements RestTransportRequest {

    /**
     * The {@link CoapClient} to use.
     */
    private final CoapClient client;

    /**
     * The request.
     */
    private final Request request;

    /**
     * The response to the request.
     */
    private CoapResponse response;

    /**
     * Buffer for writing the bytes to send.
     */
    private ByteArrayOutputStream sendBuffer;

    /**
     * Constructor.
     *
     * @param coapClient
     *            The client to use for CoAP requests.
     * @param request
     *            The CoAP request to execute.
     */
    protected CaliforniumClientRequest(final CoapClient coapClient, final Request request) {
        this.client = coapClient;
        this.request = request;

        sendBuffer = null;
    }

    @Override
    public void accept(final String... contentTypes) {
        for (final String type : contentTypes) {
            final int code = MediaTypeRegistry.parse(type);
            if (code != MediaTypeRegistry.UNDEFINED) {
                // CoAP can only accept the first type...
                request.getOptions().setAccept(code);
                break;
            }
        }
    }

    @Override
    public void setHeader(final String header, final String value) {
        // NOT POSSIBLE?!
    }

    @Override
    public OutputStream getRequestStream(final String contentType) {
        sendBuffer = new ByteArrayOutputStream();
        final int code = MediaTypeRegistry.parse(contentType);
        if (code != MediaTypeRegistry.UNDEFINED) {
            request.getOptions().setContentFormat(code);
        }
        return sendBuffer;
    }

    @Override
    public InputStream getResponseStream() {
        if (response == null) {
            return new ByteArrayInputStream(new byte[0]);
        } else {
            return new ByteArrayInputStream(response.getPayload());
        }
    }

    @Override
    public String getResponseType() {
        if (response == null || !response.getOptions().hasContentFormat()) {
            return null;
        } else {
            return MediaTypeRegistry.toString(response.getOptions().getContentFormat());
        }
    }

    @Override
    public void send() {
        // doesn't make sense here...
    }

    @Override
    public int syncRequest() throws VslException {
        if (sendBuffer != null) {
            request.setPayload(sendBuffer.toByteArray());
        }

        // what if too much parallel -> URI wrong?!
        response = client.setURI(request.getURI()).advanced(request);
        if (response == null) {
            throw new VslException("CoAP response was null - request failed") {
                private static final long serialVersionUID = 8929741701061887460L;

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
        return toHttpCode(response.getCode());
    }

    /**
     * Convert CoAP response codes to HTTP codes.
     *
     * @param code
     *            the {@link ResponseCode}.
     * @return the HTTP code as integer.
     */
    private int toHttpCode(final ResponseCode code) {
        final int httpCode;
        switch (code) {
        case BAD_GATEWAY:
            httpCode = 502;
            break;
        case BAD_OPTION:
            httpCode = 400;
            break;
        case BAD_REQUEST:
            httpCode = 400;
            break;
        case CHANGED:
            httpCode = 200;
            break;
        case CONTENT:
            httpCode = 200;
            break;
        case CONTINUE:
            httpCode = 100;
            break;
        case CREATED:
            httpCode = 201;
            break;
        case DELETED:
            httpCode = 204;
            break;
        case FORBIDDEN:
            httpCode = 403;
            break;
        case GATEWAY_TIMEOUT:
            httpCode = 504;
            break;
        case INTERNAL_SERVER_ERROR:
            httpCode = 500;
            break;
        case METHOD_NOT_ALLOWED:
            httpCode = 405;
            break;
        case NOT_ACCEPTABLE:
            httpCode = 406;
            break;
        case NOT_FOUND:
            httpCode = 404;
            break;
        case NOT_IMPLEMENTED:
            httpCode = 501;
            break;
        case PRECONDITION_FAILED:
            httpCode = 412;
            break;
        case PROXY_NOT_SUPPORTED:
            httpCode = 502;
            break;
        case REQUEST_ENTITY_INCOMPLETE:
            httpCode = 413;
            break;
        case REQUEST_ENTITY_TOO_LARGE:
            httpCode = 413;
            break;
        case SERVICE_UNAVAILABLE:
            httpCode = 503;
            break;
        case UNAUTHORIZED:
            httpCode = 401;
            break;
        case UNSUPPORTED_CONTENT_FORMAT:
            httpCode = 415;
            break;
        case VALID:
            httpCode = 200;
            break;
        case _UNKNOWN_SUCCESS_CODE:
            httpCode = 200;
            break;
        default:
            httpCode = 0;
            break;
        }
        return httpCode;
    }
}
