package org.ds2os.vsl.rest.jetty.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.ds2os.vsl.exception.UnexpectedErrorException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.rest.client.RestTransportRequest;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.util.OutputStreamContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;

/**
 * Implementation of the {@link RestTransportRequest} interface.
 *
 * @author borchers
 * @author felix
 */
public final class JettyClientRequest implements RestTransportRequest {

    /**
     * The request.
     */
    private final Request request;

    /**
     * The response to the request.
     */
    private Response response;

    /**
     * The listener for the response.
     */
    private final InputStreamResponseListener responseListener;

    /**
     * Constructor.
     *
     * @param jettyClient
     *            The client to use for HTTP requests.
     * @param uri
     *            The uri to use.
     * @param method
     *            The HTTP method to use.
     */
    protected JettyClientRequest(final HttpClient jettyClient, final URI uri,
            final HttpMethod method) {
        request = jettyClient.newRequest(uri).method(method);
        responseListener = new InputStreamResponseListener();
    }

    @Override
    public void accept(final String... contentTypes) {
        request.accept(contentTypes);
    }

    @Override
    public void setHeader(final String header, final String value) {
        request.header(header, value);
    }

    @Override
    public OutputStream getRequestStream(final String contentType) {
        final OutputStreamContentProvider contentProvider = new OutputStreamContentProvider();
        request.content(contentProvider, contentType);
        return contentProvider.getOutputStream();
    }

    @Override
    public InputStream getResponseStream() {
        return responseListener.getInputStream();
    }

    @Override
    public String getResponseType() {
        if (response == null) {
            return null;
        } else {
            return response.getHeaders().get(HttpHeader.CONTENT_TYPE);
        }
    }

    @Override
    public void send() {
        request.send(responseListener);
    }

    // new Response.Listener.Adapter() {
    // https://www.eclipse.org/jetty/documentation/current/http-client-api.html
    //
    // @Override
    // public boolean onHeader(final Response response,
    // final HttpField field) {
    // // TODO parse some response headers
    // return true;
    // }
    //
    // @Override
    // public void onContent(final Response response,
    // final ByteBuffer content) {
    // }
    //
    // @Override
    // public void onSuccess(final Response response) {
    // }
    //
    // @Override
    // public void onFailure(final Response response,
    // final Throwable failure) {
    // }
    //
    // @Override
    // public void onComplete(final Result result) {
    // }
    // }

    @Override
    public int syncRequest() throws VslException {
        try {
            if (response == null) {
                // TODO: fine-tune timeouts etc.
                response = responseListener.get(30, TimeUnit.SECONDS);
            }
        } catch (final TimeoutException e) {
            throw new org.ds2os.vsl.exception.TimeoutException(30, TimeUnit.SECONDS, "");
        } catch (final ExecutionException e) {
            throw new UnexpectedErrorException(e.getMessage());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnexpectedErrorException("Thread was interrupted during synchronous wait for response");
        }

        return response.getStatus();
    }
}
