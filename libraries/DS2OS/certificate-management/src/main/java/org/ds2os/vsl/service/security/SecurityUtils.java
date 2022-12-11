package org.ds2os.vsl.service.security;

import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class SecurityUtils {
    private static final int BUFFER_SIZE = 4096;

    public static void addCertificateToKeyStore(KeyStore keyStore, String keystorePassword, String alias, Certificate certificate, Certificate authorityCertificate, PrivateKey privateKey) throws Exception {
        Certificate[] chain = {certificate, authorityCertificate};
        KeyStore.PrivateKeyEntry newEntry = new KeyStore.PrivateKeyEntry(privateKey, chain);
        keyStore.setEntry(alias, newEntry, new KeyStore.PasswordProtection(keystorePassword.toCharArray()));
    }

    public static X500Name buildCertificateName(String subjectDN, String organizationUnit, String organization, String locality, String state, String country) {
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
        if (!subjectDN.isEmpty())
            builder.addRDN(BCStyle.CN, subjectDN);
        if (!organizationUnit.isEmpty())
            builder.addRDN(BCStyle.OU, organizationUnit);
        if (!organization.isEmpty())
            builder.addRDN(BCStyle.O, organization);
        if (!locality.isEmpty())
            builder.addRDN(BCStyle.L, locality);
        if (!state.isEmpty())
            builder.addRDN(BCStyle.ST, state);
        if (!country.isEmpty())
            builder.addRDN(BCStyle.C, country);
        return builder.build();
    }

    public static X500Name buildCertificateName(String subjectDN) {
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
        if (!subjectDN.isEmpty())
            builder.addRDN(BCStyle.CN, subjectDN);
        return builder.build();
    }

    public static X500Name buildCertificateNameLittleEndian(String subjectDN, String organizationUnit, String organization, String locality, String state, String country) {
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
        if (!country.isEmpty())
            builder.addRDN(BCStyle.C, country);
        if (!state.isEmpty())
            builder.addRDN(BCStyle.ST, state);
        if (!locality.isEmpty())
            builder.addRDN(BCStyle.L, locality);
        if (!organization.isEmpty())
            builder.addRDN(BCStyle.O, organization);
        if (!organizationUnit.isEmpty())
            builder.addRDN(BCStyle.OU, organizationUnit);
        if (!subjectDN.isEmpty())
            builder.addRDN(BCStyle.CN, subjectDN);
        return builder.build();
    }

    public static KeyStore createKeyStore(String keystorePath, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        FileOutputStream fos = new FileOutputStream(keystorePath);
        keyStore.store(fos, password.toCharArray());
        fos.flush();
        fos.close();
        return keyStore;
    }

    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    public static String getSubjectCommonName(X500Name name) {
        RDN cn = name.getRDNs(BCStyle.CN)[0];
        DERUTF8String str = (DERUTF8String) cn.getFirst().getValue();
        return str.getString();
    }

    public static String hashFile(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists())
            throw new Exception("File " + filePath + " does not exist");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        InputStream is = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        while (is.read(buffer) != -1)
            md.update(buffer);
        is.close();
        byte[] digest = md.digest();
        return String.format("%064x", new BigInteger(1, digest));
    }

    public static X509Certificate loadCertificateFromFile(String path) throws Exception {
        PEMParser pemParser = new PEMParser(new FileReader(path));
        Object object = pemParser.readObject();
        pemParser.close();
        JcaX509CertificateConverter converter = (new JcaX509CertificateConverter()).setProvider(new BouncyCastleProvider());
        return converter.getCertificate((X509CertificateHolder) object);
    }

    public static X509Certificate loadCertificateFromFile(InputStream input) throws Exception {
        PEMParser pemParser = new PEMParser(new InputStreamReader(input));
        Object object = pemParser.readObject();
        pemParser.close();
        JcaX509CertificateConverter converter = (new JcaX509CertificateConverter()).setProvider(new BouncyCastleProvider());
        return converter.getCertificate((X509CertificateHolder) object);
    }

    public static X509Certificate loadCertificateFromKeystore(KeyStore keyStore, String alias) throws Exception {
        Certificate certificate = keyStore.getCertificate(alias);
        return (X509Certificate) certificate;
    }

    public static X509Certificate loadGenericCertificateFromFile(String path) throws Exception {
        InputStream in = new FileInputStream(path);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(in);
    }

    public static KeyStore loadKeyStore(String keystorePath, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        FileInputStream fis = new FileInputStream(keystorePath);
        keyStore.load(fis, password.toCharArray());
        fis.close();
        return keyStore;
    }

    public static PrivateKey loadPrivateKeyFromFile(String path) throws Exception {
        PrivateKeyInfo pkInfo;
        PEMParser pemParser = new PEMParser(new FileReader(path));
        Object object = pemParser.readObject();
        if (object instanceof PEMKeyPair) {
            pkInfo = ((PEMKeyPair) object).getPrivateKeyInfo();
        } else {
            pkInfo = (PrivateKeyInfo) object;
        }
        JcaPEMKeyConverter converter = (new JcaPEMKeyConverter()).setProvider(new BouncyCastleProvider());
        return converter.getPrivateKey(pkInfo);
    }

    public static PublicKey loadPublicKeyFromFile(String path) throws Exception {
        SubjectPublicKeyInfo pkInfo;
        PEMParser pemParser = new PEMParser(new FileReader(path));
        Object object = pemParser.readObject();
        if (object instanceof PEMKeyPair) {
            pkInfo = ((PEMKeyPair) object).getPublicKeyInfo();
        } else {
            pkInfo = (SubjectPublicKeyInfo) object;
        }
        JcaPEMKeyConverter converter = (new JcaPEMKeyConverter()).setProvider(new BouncyCastleProvider());
        return converter.getPublicKey(pkInfo);
    }

    public static String md5Hex(String string) {
        return DigestUtils.md5Hex(string);
    }

    public static X509Certificate parseCertificate(byte[] content) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(content));
    }

    public static X509Certificate parsePemCertificate(String string) throws Exception {
        PemReader reader = new PemReader(new StringReader(string));
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(reader.readPemObject().getContent()));
    }

    public static Object parsePemObject(String string) throws Exception {
        StringReader reader = new StringReader(string);
        PEMParser pemParser = new PEMParser(reader);
        Object pem = pemParser.readObject();
        pemParser.close();
        reader.close();
        return pem;
    }

    public static void printPEMObject(Object o) throws Exception {
        OutputStreamWriter output = new OutputStreamWriter(System.out);
        JcaPEMWriter pem = new JcaPEMWriter(output);
        pem.writeObject(o);
        pem.close();
    }

    public static void saveKeyStore(KeyStore keyStore, String keystorePath, String keystorePassword) throws Exception {
        OutputStream os = new FileOutputStream(keystorePath);
        keyStore.store(os, keystorePassword.toCharArray());
        os.flush();
        os.close();
    }

    public static void saveToFile(Object obj, String path) throws Exception {
        OutputStreamWriter output = new OutputStreamWriter(new FileOutputStream(path));
        JcaPEMWriter writer = new JcaPEMWriter(output);
        writer.writeObject(obj);
        writer.flush();
        writer.close();
    }

    public static String toBase64String(PemObject obj) throws Exception {
        return Base64.getEncoder().encodeToString(obj.getContent());
    }

    public static String toString(Certificate certificate) throws Exception {
        StringWriter stringWriter = new StringWriter();
        JcaPEMWriter writer = new JcaPEMWriter(stringWriter);
        writer.writeObject(certificate);
        writer.flush();
        writer.close();
        return stringWriter.toString();
    }

    public static String toString(PemObject obj) throws Exception {
        StringWriter stringWriter = new StringWriter();
        JcaPEMWriter writer = new JcaPEMWriter(stringWriter);
        writer.writeObject(obj);
        writer.flush();
        writer.close();
        return stringWriter.toString();
    }

    public static boolean verifyFileHash(String filePath, String hash) throws Exception {
        String computedHash = hashFile(filePath);
        return computedHash.equals(hash);
    }
}
