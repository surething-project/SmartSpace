package org.ds2os.vsl.rest;

import java.security.cert.X509Certificate;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.exception.FailedAuthenticationException;

/**
 * Handles REST requests independent of the protocol implementation.
 *
 * @author felix
 */
public interface RestHandler {

    /**
     * Get the {@link RestTransportContext} with common configuration and tools.
     *
     * @return the REST transport context.
     */
    RestTransportContext getRestTransportContext();

    /**
     * Get the mapper for the content type to use based on the accept or content type header.
     *
     * @param accept
     *            the accepted content types.
     * @return the mapper for the preferred content type.
     */
    VslMapper getMapper(String accept);

    /**
     * Authenticate a REST request using the SSL certificate chain of the client and the
     * authorization header, if present.
     *
     * @param certs
     *            the certificate chain of the client. Must contain at least one certificate.
     * @param authorizationHeader
     *            the authorization header value or null if the header was not set.
     * @return the {@link VslIdentity} of the client.
     * @throws FailedAuthenticationException
     *             If the client cannot be authenticated.
     */
    VslIdentity authenticate(X509Certificate[] certs, String authorizationHeader)
            throws FailedAuthenticationException;
}
