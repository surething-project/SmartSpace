package org.ds2os.vsl.service.tools;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ds2os.vsl.service.security.SecurityUtils;
import org.ds2os.vsl.service.security.SelfSignedCertificateBuilder;

import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;

public class CACreator {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar cagen <output folder>");
            System.exit(-1);
        }
        Security.addProvider(new BouncyCastleProvider());
        String outputFolder = args[0];
        if (outputFolder.charAt(outputFolder.length() - 1) != '/')
            outputFolder = outputFolder + '/';
        try {
            KeyPair authorityKeyPair = SecurityUtils.generateKeyPair();
            SelfSignedCertificateBuilder selfSignedCertificateBuilder = new SelfSignedCertificateBuilder("DS2OS CA", authorityKeyPair.getPublic(), authorityKeyPair.getPrivate());
            selfSignedCertificateBuilder.addDefaultExtensions();
            X509Certificate authorityCertificate = (X509Certificate) selfSignedCertificateBuilder.build();
            SecurityUtils.saveToFile(authorityKeyPair.getPrivate(), outputFolder + "ca.key");
            SecurityUtils.saveToFile(authorityCertificate, outputFolder + "ca.crt");
            System.out.println("Generated new Certificate Authority key and certificate in folder " + outputFolder);
        } catch (Exception e) {
            System.err.println("Error creating CA " + e.getMessage());
        }
    }
}
