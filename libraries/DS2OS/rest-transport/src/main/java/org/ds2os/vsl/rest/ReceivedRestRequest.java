package org.ds2os.vsl.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ReceivedRestRequest {

    String getAccept();

    InputStream getContent() throws IOException;

    String getContentType();

    OutputStream respond(int code, String contentType) throws IOException;

    // TODO: we should get rid of this...
    void sendError(int code, String message) throws IOException;
}
