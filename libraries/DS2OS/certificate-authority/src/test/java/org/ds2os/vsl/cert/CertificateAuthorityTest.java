package org.ds2os.vsl.cert;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.exception.FailedAuthenticationException;
import org.junit.Test;

/**
 * @author jay
 *
 */
public class CertificateAuthorityTest {

    /**
     * KeyStore file for service.
     */
    private static final String KEYSTORE_FILE_SERVICE = "service1.jks";

    /**
     * KeyStore file for agent.
     */
    private static final String KEYSTORE_FILE_AGENT = "agent1.jks";

    /**
     * KeyStore password.
     */
    private static final String KEYSTORE_PASSWORD = "K3yst0r3";

    /**
     * Certificate Authority object.
     */
    private CertificateAuthority certAuth;

    /**
     * Test method for {@link CertificateAuthority#authenticate(java.security.cert.X509Certificate)}
     * .
     *
     * @throws IOException
     *             IOException if streams failed.
     * @throws CertificateException
     *             CertificateException if certificate is not loaded.
     * @throws NoSuchAlgorithmException
     *             NoSuchAlgorithmException if certificate has invalid signing algorithm.
     * @throws KeyStoreException
     *             KeyStoreException if keystore failed to load.
     * @throws FailedAuthenticationException
     *             FailedAuthenticationException if authentication has failed on certificate.
     */
    @Test
    public final void testAuthenticate() throws KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException, FailedAuthenticationException {
        certAuth = new CertificateAuthority(KEYSTORE_FILE_SERVICE, KEYSTORE_PASSWORD);
        certAuth.selectCertificate("service1");
        final VslIdentity identity = certAuth.authenticate(certAuth.getSelectedCertificate());
        assertThat(identity.getClientId(), is(equalTo("service1")));
        // assertThat(identity.getAccessIDs(), containsInAnyOrder("service", "anotherid"));
    }

    /**
     * Test method for
     * {@link org.ds2os.vsl.cert.CertificateAuthority#selectCertificate(java.lang.String)}.
     *
     * @throws IOException
     *             Shouldn't happen.
     * @throws CertificateException
     *             Shouldn't happen.
     * @throws NoSuchAlgorithmException
     *             Shouldn't happen.
     * @throws KeyStoreException
     *             Shouldn't happen.
     */
    @Test
    public final void testSelectCertificate()
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        certAuth = new CertificateAuthority(KEYSTORE_FILE_AGENT, KEYSTORE_PASSWORD);
        certAuth.selectCertificate("agent1");
        assertThat(certAuth.getSelectedCertificate().getSubjectDN().getName(),
                containsString("agent1"));
    }
}
