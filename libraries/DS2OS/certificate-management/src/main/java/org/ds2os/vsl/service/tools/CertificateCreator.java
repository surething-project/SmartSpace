package org.ds2os.vsl.service.tools;

import org.bouncycastle.asn1.misc.NetscapeCertType;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.ds2os.vsl.service.security.CSRBuilder;
import org.ds2os.vsl.service.security.SecurityUtils;
import org.ds2os.vsl.service.security.SignedCertificateBuilder;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

public class CertificateCreator {
    private static final int DEFAULT_VALIDITY = 31536000;

    private static final String KEYSTORE_PW = "K3yst0r3";

    private static Certificate createAgentCertificate(String subject, X509Certificate authorityCert, PrivateKey authorityCertKey, PKCS10CertificationRequest csr) throws Exception {
        SignedCertificateBuilder signedCertificateBuilder = new SignedCertificateBuilder(authorityCert, authorityCertKey, csr, 31536000);
        signedCertificateBuilder.addDs2osIsKnowledgeAgent(false, true);
        signedCertificateBuilder.addNetscapeComment(false, "DS2OS KA certificate");
        signedCertificateBuilder.addAuthorityKeyIdentifier(false, authorityCert);
        signedCertificateBuilder.addBasicConstraints(false, false, null);
        signedCertificateBuilder.addKeyUsage(false, new KeyUsage(224));
        signedCertificateBuilder.addExtendedKeyUsage(false, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth}));
        signedCertificateBuilder.addNetscapeCertificateType(false, new NetscapeCertType(192));
        GeneralNames names = new GeneralNames(new GeneralName[]{new GeneralName(2, subject), new GeneralName(2, subject + ".dev.ds2os.org"), new GeneralName(2, "dev.ds2os.org")});
        signedCertificateBuilder.addSubjectAlternativeName(false, names);
        signedCertificateBuilder.addSubjectKeyIdentifier(false, csr.getSubjectPublicKeyInfo());
        return signedCertificateBuilder.build();
    }

    private static Certificate createServiceCertificate(X509Certificate authorityCert, PrivateKey authorityCertKey, PKCS10CertificationRequest csr, String[] accessIds) throws Exception {
        SignedCertificateBuilder signedCertificateBuilder = new SignedCertificateBuilder(authorityCert, authorityCertKey, csr, 31536000);
        signedCertificateBuilder.addDefaultExtensions(accessIds);
        return signedCertificateBuilder.build();
    }

    private static Certificate createSystemCertificate(X509Certificate authorityCert, PrivateKey authorityCertKey, PKCS10CertificationRequest csr) throws Exception {
        SignedCertificateBuilder signedCertificateBuilder = new SignedCertificateBuilder(authorityCert, authorityCertKey, csr, 31536000);
        signedCertificateBuilder.addDs2osIsKnowledgeAgent(false, true);
        signedCertificateBuilder.addNetscapeComment(false, "DS2OS service certificate");
        signedCertificateBuilder.addAuthorityKeyIdentifier(false, authorityCert);
        signedCertificateBuilder.addBasicConstraints(false, false, null);
        signedCertificateBuilder.addKeyUsage(false, new KeyUsage(224));
        signedCertificateBuilder.addExtendedKeyUsage(false, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth}));
        signedCertificateBuilder.addNetscapeCertificateType(false, new NetscapeCertType(128));
        signedCertificateBuilder.addSubjectKeyIdentifier(false, csr.getSubjectPublicKeyInfo());
        return signedCertificateBuilder.build();
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.err.println("Usage: java -jar certgen <caCertificatePath> <caCertificateKeyPath> <subjectName> <type (agent|service|system)> <outputPath> [accessId1 ...]");
            System.exit(-1);
        }
        Security.addProvider(new BouncyCastleProvider());
        String authCertPath = args[0];
        String authCertKeyPath = args[1];
        String subjectName = args[2];
        String type = args[3];
        String outputPath = args[4];
        if (outputPath.charAt(outputPath.length() - 1) != '/')
            outputPath = outputPath + '/';
        X509Certificate authorityCert = null;
        PrivateKey authorityCertKey = null;
        String[] accessIds = Arrays.copyOfRange(args, 5, args.length);

        try {
            authorityCert = SecurityUtils.loadCertificateFromFile(authCertPath);
            authorityCertKey = SecurityUtils.loadPrivateKeyFromFile(authCertKeyPath);
        } catch (Exception e) {
            System.err.println("Couldn't load authority certificate from file: " + e.getMessage());
            System.exit(-1);
        }
        try {
            KeyPair newKeyPair = SecurityUtils.generateKeyPair();
            CSRBuilder csrBuilder = new CSRBuilder(authorityCert, newKeyPair, subjectName);
            csrBuilder.addDs2osIsKnowledgeAgent(true);
            PKCS10CertificationRequest csr = csrBuilder.build();
            Certificate newCertificate = null;
            switch (type) {
                case "agent":
                    newCertificate = createAgentCertificate(subjectName, authorityCert, authorityCertKey, csr);
                    break;
                case "service":
                    newCertificate = createServiceCertificate(authorityCert, authorityCertKey, csr, accessIds);
                    break;
                case "system":
                    newCertificate = createSystemCertificate(authorityCert, authorityCertKey, csr);
                    break;
                default:
                    System.err.println("Invalid type!");
                    System.err.println("Usage: java -jar certgen <caCertificatePath> <caCertificateKeyPath> <subjectName> <agent|service|system> <outputPath> [accessId1 ...]");
                    System.exit(-1);
            }
            Certificate[] chain = {newCertificate, authorityCert};
            String keystorePath = outputPath + subjectName + ".jks";
            KeyStore keyStore = SecurityUtils.createKeyStore(keystorePath, "K3yst0r3");
            KeyStore.TrustedCertificateEntry ca = new KeyStore.TrustedCertificateEntry(authorityCert);
            keyStore.setEntry("ca", ca, null);
            KeyStore.PrivateKeyEntry newEntry = new KeyStore.PrivateKeyEntry(newKeyPair.getPrivate(), chain);
            keyStore.setEntry(subjectName, newEntry, new KeyStore.PasswordProtection("K3yst0r3".toCharArray()));
            SecurityUtils.saveKeyStore(keyStore, keystorePath, "K3yst0r3");
            System.out.println("Generated new service certificate and stored in keystore " + keystorePath);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
