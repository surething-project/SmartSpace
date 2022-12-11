package org.ds2os.vsl.agentregistry.cryptography;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ds2os.vsl.core.VslSymmetricKeyStore;
import org.ds2os.vsl.netutils.KeyStringParser;

/**
 * Class to check for which TLS parameters ciphers can be generated.
 *
 * @author Johannes Straßer
 *
 */
public final class TLSParameterChecker {

    /**
     * Dummy constructor to hide default one.
     */
    private TLSParameterChecker() {
        // Dummy
    }

    /**
     * Prints a list indication which TLS_Strings can be used on this machine.
     *
     * Before printing, the BouncyCastle provider is dynamically added.
     *
     * @param args
     *            irrelevant command line parameters
     */
    public static void main(final String[] args) {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        final VslSymmetricKeyStore keyStore = new SymmetricKeyStore();
        final String[] tlsStrings = new String[] { ("TLS_PSK_WITH_NULL_SHA"),
                ("TLS_PSK_WITH_AES_128_CBC_SHA256"), ("TLS_PSK_WITH_AES_128_GCM_SHA256"),
                ("TLS_PSK_WITH_AES_256_CBC_SHA384"), ("TLS_PSK_WITH_AES_256_GCM_SHA384"),
                ("TLS_PSK_WITH_ARIA_128_CBC_SHA256"), ("TLS_PSK_WITH_ARIA_128_GCM_SHA256"),
                ("TLS_PSK_WITH_ARIA_256_CBC_SHA384"), ("TLS_PSK_WITH_ARIA_256_GCM_SHA384"),
                ("TLS_PSK_WITH_CAMELLIA_128_CBC_SHA256"),
                ("TLS_PSK_WITH_CAMELLIA_256_CBC_SHA384") };
        for (final String tlsString : tlsStrings) {
            System.out
                    .println((keyStore.addKey(new KeyStringParser(tlsString).createKey(), tlsString)
                            ? "YES" : "NO ") + " " + tlsString);
        }

    }

}
