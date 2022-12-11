package org.ds2os.vsl.rest.californium;

import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ds2os.vsl.netutils.SSLUtils;
import org.ds2os.vsl.rest.RestTransportContext;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;

/**
 * Context of Scandium to extend Californium with DTLS.
 *
 * @author felix
 */
public class ScandiumContext {

    /**
     * The DTLS connector for Californium.
     */
    private final DTLSConnector dtlsConnector;

    /**
     * Create a new Scandium context for DTLS.
     *
     * @param context
     *            the {@link RestTransportContext} with common configuration and tools.
     * @throws KeyStoreException
     *             If KeyStore failed to load.
     * @throws NoSuchAlgorithmException
     *             If algorithm for checking certificate integrity is not valid.
     * @throws UnrecoverableKeyException
     *             If the key cannot be recovered (e.g., the given password is wrong).
     */
    public ScandiumContext(final RestTransportContext context)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        final DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(
                new InetSocketAddress(context.getConfig().getPort()));

        // filter only DTLS supported cipher suites
        final List<CipherSuite> cipherSuites = new ArrayList<CipherSuite>();
        for (final String tlsCipher : SSLUtils.getTLSCiphers()) {
            final CipherSuite knownSuite = CipherSuite.getTypeByName(tlsCipher);
            if (knownSuite != null) {
                cipherSuites.add(knownSuite);
            }
        }
        builder.setSupportedCipherSuites(
                cipherSuites.toArray(new CipherSuite[cipherSuites.size()]));

        // get identity and all root certificates
        final KeyStore keystore = context.getKeystore();
        final List<Certificate> trustedRoots = new ArrayList<Certificate>();
        for (final String alias : Collections.list(keystore.aliases())) {
            if (keystore.isKeyEntry(alias)) {
                builder.setIdentity(
                        (PrivateKey) keystore.getKey(alias, context.getKeyPassword().toCharArray()),
                        keystore.getCertificateChain(alias), false);
            } else {
                trustedRoots.add(keystore.getCertificate(alias));
            }
        }

        // build DTLS connector
        builder.setTrustStore(trustedRoots.toArray(new Certificate[trustedRoots.size()]));
        dtlsConnector = new DTLSConnector(builder.build());
    }

    /**
     * Get the DTLS connector.
     *
     * @return the {@link DTLSConnector}.
     */
    public final DTLSConnector getDtlsConnector() {
        return dtlsConnector;
    }
}
