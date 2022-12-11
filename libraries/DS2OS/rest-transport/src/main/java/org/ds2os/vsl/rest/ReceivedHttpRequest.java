package org.ds2os.vsl.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ReceivedHttpRequest implements ReceivedRestRequest {
    private final HttpServletRequest servletRequest;
    private final HttpServletResponse servletResponse;

    public ReceivedHttpRequest(final HttpServletRequest servletRequest,
            final HttpServletResponse servletResponse) {
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
    }

    @Override
    public String getAccept() {
        return servletRequest.getHeader("accept");
    }

    @Override
    public InputStream getContent() throws IOException {
        return servletRequest.getInputStream();
    }

    @Override
    public String getContentType() {
        return servletRequest.getContentType();
    }

    @Override
    public OutputStream respond(final int code, final String contentType) throws IOException {
        servletResponse.setStatus(code);
        servletResponse.setContentType(contentType);
        final OutputStream output = servletResponse.getOutputStream();
        // Ensure the response is committed, which causes the headers to be sent,
        // preventing further alteration and ensuring the client does not think
        // its request timed out.
        output.flush();
        return output;
    }

    @Override
    public void sendError(final int code, final String message) throws IOException {
        servletResponse.sendError(code, message);
    }
}
