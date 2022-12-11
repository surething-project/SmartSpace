package org.ds2os.vsl.rest.jetty;

import org.conscrypt.OpenSSLProvider;
import org.ds2os.vsl.netutils.SSLUtils;
import org.ds2os.vsl.rest.RestTransportContext;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.security.Security;

/**
 * Common context of Jetty client and server implementations.
 *
 * @author felix
 */
public class JettyContext {

    /**
     * The Jetty {@link SslContextFactory}.
     */
    private SslContextFactory sslContextFactory;

    /**
     * Create a Jetty context with an SSL context factory from an existing key store.
     *
     * @param transportContext
     *            the {@link RestTransportContext} from which this {@link JettyContext} is derived.
     */
    public JettyContext(final RestTransportContext transportContext) {
        // Add Conscrypt JSP including the BoringSSL implementation.
        Security.addProvider(new OpenSSLProvider());

        sslContextFactory = new SslContextFactory();
        sslContextFactory.setTrustStore(transportContext.getKeystore());
        sslContextFactory.setKeyStore(transportContext.getKeystore());
        sslContextFactory.setKeyManagerPassword(transportContext.getKeyPassword());
        sslContextFactory.setNeedClientAuth(false);
        sslContextFactory.setWantClientAuth(true);
        sslContextFactory.setRenegotiationAllowed(false);
        sslContextFactory.setIncludeProtocols(SSLUtils.getTLSVersions());
        sslContextFactory.setIncludeCipherSuites(SSLUtils.getTLSCiphers());
        sslContextFactory.setSessionCachingEnabled(true);
        sslContextFactory.setSslSessionTimeout(300000);
        sslContextFactory.setProvider("Conscrypt");
    }

    /**
     * Get the Jetty {@link SslContextFactory}.
     *
     * @return the SSL context factory.
     */
    public final SslContextFactory getSslContextFactory() {
        return sslContextFactory;
    }
}
