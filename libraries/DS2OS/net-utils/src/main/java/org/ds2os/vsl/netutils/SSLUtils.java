package org.ds2os.vsl.netutils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * SSL utils used by multiple components.
 *
 * @author felix
 * @author Johannes Straßer
 */
public final class SSLUtils {

    /**
     * List of allowed TLS Versions.
     */
    private static final String TLS_VERSIONS = "TLSv1.1,TLSv1.2";

    /**
     * List of allowed DHE cipher suites.
     */
    private static final String TLS_DHE_CIPHERS = "TLS_DHE_RSA_WITH_AES_128_CBC_SHA,"
            + "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256," + "TLS_DHE_RSA_WITH_AES_128_CCM,"
            + "TLS_DHE_RSA_WITH_AES_128_CCM_8," + "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,"
            + "TLS_DHE_RSA_WITH_AES_256_CBC_SHA," + "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256,"
            + "TLS_DHE_RSA_WITH_AES_256_CCM," + "TLS_DHE_RSA_WITH_AES_256_CCM_8,"
            + "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384," + "TLS_DHE_RSA_WITH_ARIA_128_CBC_SHA256,"
            + "TLS_DHE_RSA_WITH_ARIA_128_GCM_SHA256," + "TLS_DHE_RSA_WITH_ARIA_256_CBC_SHA384,"
            + "TLS_DHE_RSA_WITH_ARIA_256_GCM_SHA384," + "TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA,"
            + "TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA256,"
            + "TLS_DHE_RSA_WITH_CAMELLIA_128_GCM_SHA256," + "TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA,"
            + "TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA256,"
            + "TLS_DHE_RSA_WITH_CAMELLIA_256_GCM_SHA384";

    /**
     * List of allowed ECDHE cipher suites.
     */
    private static final String TLS_ECDHE_CIPHERS = "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,"
            + "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256," + "TLS_ECDHE_ECDSA_WITH_AES_128_CCM,"
            + "TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8," + "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,"
            + "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA," + "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,"
            + "TLS_ECDHE_ECDSA_WITH_AES_256_CCM," + "TLS_ECDHE_ECDSA_WITH_AES_256_CCM_8,"
            + "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,"
            + "TLS_ECDHE_ECDSA_WITH_ARIA_128_CBC_SHA256,"
            + "TLS_ECDHE_ECDSA_WITH_ARIA_128_GCM_SHA256,"
            + "TLS_ECDHE_ECDSA_WITH_ARIA_256_CBC_SHA384,"
            + "TLS_ECDHE_ECDSA_WITH_ARIA_256_GCM_SHA384,"
            + "TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_CBC_SHA256,"
            + "TLS_ECDHE_ECDSA_WITH_CAMELLIA_128_GCM_SHA256,"
            + "TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_CBC_SHA384,"
            + "TLS_ECDHE_ECDSA_WITH_CAMELLIA_256_GCM_SHA384,"
            + "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA," + "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,"
            + "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256," + "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,"
            + "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384," + "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,"
            + "TLS_ECDHE_RSA_WITH_ARIA_128_CBC_SHA256," + "TLS_ECDHE_RSA_WITH_ARIA_128_GCM_SHA256,"
            + "TLS_ECDHE_RSA_WITH_ARIA_256_CBC_SHA384," + "TLS_ECDHE_RSA_WITH_ARIA_256_GCM_SHA384,"
            + "TLS_ECDHE_RSA_WITH_CAMELLIA_128_CBC_SHA256,"
            + "TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256,"
            + "TLS_ECDHE_RSA_WITH_CAMELLIA_256_CBC_SHA384,"
            + "TLS_ECDHE_RSA_WITH_CAMELLIA_256_GCM_SHA384";

    /**
     * Utility class - must not be instantiated.
     */
    private SSLUtils() {
    }

    /**
     * The function loads the {@link KeyStore}.
     *
     * @param fileName
     *            the path to the keystore file.
     * @param password
     *            the password of the keystore.
     * @return loaded KeyStore object.
     * @throws FileNotFoundException
     *             If file to search for keystore is not existing.
     * @throws KeyStoreException
     *             If KeyStore failed to load.
     * @throws NoSuchAlgorithmException
     *             If algorithm for checking certificate integrity is not valid.
     * @throws CertificateException
     *             Failed to load a certificate from the keystore.
     * @throws IOException
     *             Failed to read I/O stream.
     */
    public static KeyStore loadKeyStore(final String fileName, final String password)
            throws FileNotFoundException, KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        final FileInputStream finstrm = new FileInputStream(fileName);
        try {
            ks.load(finstrm, password.toCharArray());
        } finally {
            finstrm.close();
        }
        return ks;
    }

    /**
     * Returns all allowed TLS versions.
     *
     * @return Array containing all allowed TSL versions
     */
    public static String[] getTLSVersions() {
        return TLS_VERSIONS.split(",");

    }

    /**
     * Returns all allowed TLS cipher suites.
     *
     * @return Array containing all allowed cipher suites
     */
    public static String[] getTLSCiphers() {
        String tlsCiphers = TLS_ECDHE_CIPHERS;
        // Allow DHE only when the system's key size is sufficient
        final String dheKeySize = System.getProperty("tls.ephemeralDHKeySize");
        if (dheKeySize != null && Integer.parseInt(dheKeySize) >= 2048) {
            tlsCiphers += "," + TLS_DHE_CIPHERS;
        }
        return tlsCiphers.split(",");
    }
}
