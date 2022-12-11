package org.ds2os.vsl.cert;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.x509.extension.X509ExtensionUtil;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslX509Authenticator;
import org.ds2os.vsl.core.impl.KAIdentity;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.exception.FailedAuthenticationException;
import org.ds2os.vsl.netutils.SSLUtils;

/**
 * @author jay
 *
 */
public class CertificateAuthority implements VslX509Authenticator {

    /**
     * The pattern for X509Certificate object to look for Subject Distinguishable name which will be
     * used to capture agentID, accessIDs and manifestHashes.
     */
    static Pattern regex = Pattern.compile("(^|.*,)cn=([^,/]*)($|/|,).*");

    /**
     * OID for CertificateFor in X509Certificate.
     */
    static String oidForIsKA = "1.3.6.1.4.1.0";

    /**
     * OID for Manifest Hash in X509Certificate.
     */
    static String oidForManifest = "1.3.6.1.4.1.1";

    /**
     * OID for AccessIDs in X509Certificate.
     */
    static String oidForAccess = "1.3.6.1.4.1.2";

    /**
     * KeyStore variable that contains all loaded certificates.
     */
    private final KeyStore keyStore;

    /**
     * Certificate of the service based upon passed Alias name which is fetched from KeyStore.
     */
    private X509Certificate selectedCertificate = null;

    /**
     * Public Key of the CA which has signed 'selectedCertificate'.
     */
    private String selectedCAPubKey = null;

    /**
     * Constructor for this class.
     *
     * @param file
     *            File for keystore.
     * @param password
     *            Password associated with keystore.
     * @throws KeyStoreException
     *             If KeyStore failed to load.
     * @throws IOException
     *             Failed to open I/O stream.
     * @throws NoSuchAlgorithmException
     *             If algorithm for checking certificate integrity is not valid.
     * @throws CertificateException
     *             Failed to load a certificate
     */
    public CertificateAuthority(final String file, final String password)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        this.keyStore = SSLUtils.loadKeyStore(file, password);
    }

    @Override
    public final VslIdentity authenticate(final X509Certificate cert)
            throws FailedAuthenticationException {
        if (isFromKA(cert)) {
            String agentId;
            final String subjectDistinguishableName = cert.getSubjectDN().getName();
            final Matcher m = regex.matcher(subjectDistinguishableName.toLowerCase(Locale.ROOT));
            if (m.matches()) {
                agentId = m.group(2);
                return new KAIdentity(agentId);
            } else {
                // drop further check, certificate is wrongly created.
                throw new FailedAuthenticationException("The certificate has invalid Common Name.");
            }
        } else {
            String serviceId;
            final String subjectDistinguishableName = cert.getSubjectDN().getName();
            final Matcher m = regex.matcher(subjectDistinguishableName.toLowerCase(Locale.ROOT));
            if (m.matches()) {
                serviceId = m.group(2);
                final byte[] accessIdBytes = cert.getExtensionValue(oidForAccess);
                final byte[] manifestHashBytes = cert.getExtensionValue(oidForManifest);
                String accessIds, manifestHash;
                try {
                    accessIds = readDERString(accessIdBytes);
                    manifestHash = readDERString(manifestHashBytes);
                    if ("".equals(manifestHash)) {
                        throw new IOException("Did not find any manifest hash!");
                    }
                } catch (final IOException e) {
                    throw new FailedAuthenticationException(
                            "The certificate has invalid accessIDs and manifestHash.", e);
                }
                // ManifestHash might be needed in future !!
                return new ServiceIdentity(serviceId, accessIds);
            } else {
                // drop further check, certificate is wrongly created.
                throw new FailedAuthenticationException("The certificate has invalid Common Name.");
            }
        }
    }

    /**
     * Read a DER String.
     *
     * @param bytes
     *            the DER to read
     * @return The parsed DER string.
     * @throws IOException
     *             THrown if the value isn't a ASN.1 String.
     */
    private String readDERString(final byte[] bytes) throws IOException {
        final ASN1Primitive value = X509ExtensionUtil.fromExtensionValue(bytes);
        if (value instanceof ASN1String) {
            return ((ASN1String) value).getString();
        } else {
            throw new IOException("Extension value is not a ASN.1 string.");
        }
    }

    @Override
    public final boolean isFromKA(final X509Certificate cert) {
        final byte[] checkKA = cert.getExtensionValue(oidForIsKA);
        if (checkKA != null) {
            try {
                final String checkIfKA = readDERString(checkKA);
                if ("TRUE".equals(checkIfKA.toUpperCase(Locale.ROOT))) {
                    return true;
                }
            } catch (final IOException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * This function will be called to select a certificate from the KeyStore for which the private
     * key is found. The goal is that any outsider class invoking this function will endup in
     * setting the CA PubKey of the certifying CA of the selected certificate.
     *
     * @param alias
     *            The string object to be queried from the KeyStore to identify if private key was
     *            available.
     * @throws KeyStoreException
     *             The exception that has raised when keystore has not been initialized.
     */
    public final void selectCertificate(final String alias) throws KeyStoreException {

        if (alias == null || !keyStore.containsAlias(alias) || !keyStore.isKeyEntry(alias)) {

            // no alias passed/found. So, just look into entire KeyStore to find
            // the first
            // available certificate for which a private key is found.

            final Enumeration<String> aliasList = keyStore.aliases();
            while (aliasList.hasMoreElements()) {
                final String fetchedAlias = aliasList.nextElement();
                if (keyStore.isKeyEntry(fetchedAlias)) {
                    final Certificate[] certs = keyStore.getCertificateChain(fetchedAlias);
                    if (certs != null & certs.length > 0
                            & certs[certs.length - 1] instanceof X509Certificate) {
                        final PublicKey fetchedPubKey = certs[certs.length - 1].getPublicKey();
                        final byte[] rawBytes = fetchedPubKey.getEncoded();
                        if (rawBytes != null) {
                            selectedCAPubKey = javax.xml.bind.DatatypeConverter
                                    .printBase64Binary(rawBytes);
                            selectedCertificate = (X509Certificate) certs[0];
                            return;
                        }
                    }
                }
            }
        } else {
            final Certificate[] certs = keyStore.getCertificateChain(alias);
            if (certs != null & certs.length > 0) {
                final PublicKey fetchedPubKey = certs[certs.length - 1].getPublicKey();
                final byte[] rawBytes = fetchedPubKey.getEncoded();
                if (rawBytes != null) {
                    selectedCAPubKey = javax.xml.bind.DatatypeConverter.printBase64Binary(rawBytes);
                    selectedCertificate = (X509Certificate) certs[0];
                    return;
                }
            }
        }
    }

    /**
     * Getter method to get the selected Certificate after invocation of selectCertificate method.
     *
     * @return {@link X509Certificate} object that was setted.
     */
    @Override
    public final X509Certificate getSelectedCertificate() {
        return this.selectedCertificate;
    }

    /**
     * Getter method to get the public key of the signing CA of the selected certificate which was
     * set by selectCertificate method.
     *
     * @return Public Key of the signing CA.
     */
    @Override
    public final String getCAPublicKey() {
        return this.selectedCAPubKey;
    }

    /**
     * Get the SSL key store if this entity.
     *
     * @return the SSL key store.
     */
    public final KeyStore getKeyStore() {
        return keyStore;
    }
}
