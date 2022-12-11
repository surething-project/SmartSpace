package org.ds2os.vsl.aliveping;

/**
 * The interface that will perform certificate validation procedures.
 *
 * @author jay
 *
 */
public interface CertificateValidator {

    /**
     * Method for verifying a Public Key whether it is known by the CertificateValidator or not.
     *
     * @param pubKey
     *            The public key to be checked
     * @return true if the Certificate Validator has encountered the passed Public key previously.
     *         false if it has not seen the public key ever.
     */
    boolean verifyPubKey(String pubKey);
}
