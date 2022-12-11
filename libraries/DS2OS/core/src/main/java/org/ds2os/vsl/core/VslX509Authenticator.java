package org.ds2os.vsl.core;

import java.security.cert.X509Certificate;

import org.ds2os.vsl.exception.FailedAuthenticationException;

/**
 * Interface for authentication of X.509 certificates.
 *
 * @author felix
 * @author jay
 */
public interface VslX509Authenticator {

    /**
     * Authenticates the given X.509 certificate.
     *
     * @param cert
     *            the X.509 certificate.
     * @return the VslIdentity of the certificate owner.
     * @throws FailedAuthenticationException
     *             Throws the exception if the certificate is not valid for VSL authentication.
     */
    VslIdentity authenticate(X509Certificate cert) throws FailedAuthenticationException;

    /**
     * To check if the passed certificate is from a KA or from a service.
     *
     * @param cert
     *            The X.509 certificate.
     * @return True if it is from a KA.
     */
    boolean isFromKA(X509Certificate cert);

    /**
     * Method for fetching public Key of the CA which has signed certificate for this service.
     *
     * @return Public Key of the CA
     */
    String getCAPublicKey();

    /**
     * Method for fetching service's certificate that was set.
     *
     * @return {@link X509Certificate} of the Service
     */
    X509Certificate getSelectedCertificate();
}
