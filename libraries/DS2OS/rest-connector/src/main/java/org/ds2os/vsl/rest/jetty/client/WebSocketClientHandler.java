package org.ds2os.vsl.rest.jetty.client;

import org.ds2os.vsl.exception.VslException;

import java.io.InputStream;
import java.util.UUID;

/**
 * A handler for a WebSocketClient.
 * Needed so that actions outside of the WebSocketClient's scope can be triggered.
 */
public interface WebSocketClientHandler {

    /**
     * Requests an awaited stream from the connected Knowledge Agent. The stream is identified
     * on the KA using the provided id parameters, which were sent in the callback message.
     *
     * @param callbackId
     *      the id of the WebSocket callback message.
     * @param serial
     *      the serial of the WebSocket callback message.
     * @return
     *      the {@link InputStream} handle
     * @throws VslException
     *      if a VslException occurs.
     */
    InputStream getStreamFromCallback(UUID callbackId, long serial) throws VslException;

    /**
     * Sends the given awaited stream to the connected Knowledge Agent. The stream is identified
     * on the KA using the provided id parameters, which were sent in the callback message.
     *
     * @param stream
     *      the {@link InputStream} that should be sent.
     * @param callbackId
     *      the id of the WebSocket callback message.
     * @param serial
     *      the serial of the WebSocket callback message.
     * @throws VslException
     *      if a VslException occurs.
     */
    void setStreamForCallback(InputStream stream, UUID callbackId, long serial) throws VslException;
}
