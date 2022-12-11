package org.ds2os.vsl.agentregistry.cryptography;

import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Helper class to determine available security algorithms.
 *
 * @author Johannes Straßer
 *
 */
public final class AlgorithmNameChecker {

    /**
     * Hides default constructor.
     */
    private AlgorithmNameChecker() {
        // Dummy
    }

    /**
     * Prints a list of installed security providers and which algorithms they provide.
     *
     * Before printing, the BouncyCastle provider is dynamically added.
     *
     * @param args
     *            irrelevant command line parameters
     */
    public static void main(final String[] args) {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        for (final Provider prov : Security.getProviders()) {
            System.out.println(prov.getInfo());
            for (final Service serv : prov.getServices()) {
                System.out.println("    " + serv.getType() + ":" + serv.getAlgorithm());
            }

        }

    }

}
