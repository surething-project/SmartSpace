package org.ds2os.vsl.service.security;

import org.apache.commons.lang3.time.DateUtils;
import org.bouncycastle.asn1.misc.NetscapeCertType;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class SignedCertificateBuilder extends CertificateBuilder {
    private final X509Certificate authorityCertificate;
    private final PrivateKey authorityCertificateKey;
    private final PKCS10CertificationRequest certificateSigningRequest;
    private final String subjectDN;

    public SignedCertificateBuilder(X509Certificate authorityCertificate, PrivateKey authorityKey, PKCS10CertificationRequest csr, Date validityStart, Date validityEnd) throws Exception {
        this.authorityCertificate = authorityCertificate;
        this.authorityCertificateKey = authorityKey;
        this.certificateSigningRequest = csr;
        this.extUtils = new JcaX509ExtensionUtils();
        RDN[] rdns = csr.getSubject().getRDNs(BCStyle.CN);
        if (rdns.length <= 0)
            throw new Exception("Invalid Certificate Signing Request. CN subject expected but not found");
        this.subjectDN = rdns[0].getFirst().getValue().toString();
        X500Name name = SecurityUtils.buildCertificateNameLittleEndian(this.subjectDN, "I8", "TUM", "Munich", "Germany", "DE");
        JcaPKCS10CertificationRequest jcaRequest = new JcaPKCS10CertificationRequest(csr);
        this

                .builder = new JcaX509v3CertificateBuilder(authorityCertificate, BigInteger.valueOf(System.currentTimeMillis()), validityStart, validityEnd, name, jcaRequest.getPublicKey());
    }

    public SignedCertificateBuilder(X509Certificate authorityCertificate, PrivateKey authorityKey, PKCS10CertificationRequest csr) throws Exception {
        this(authorityCertificate, authorityKey, csr, new Date(), DateUtils.addYears(new Date(), 1));
    }

    public SignedCertificateBuilder(X509Certificate authorityCertificate, PrivateKey authorityKey, PKCS10CertificationRequest csr, int validity) throws Exception {
        this(authorityCertificate, authorityKey, csr, new Date(), DateUtils.addSeconds(new Date(), validity));
    }

    public void addDefaultExtensions() throws Exception {
        addDefaultExtensions(new String[]{this.subjectDN});
    }

    public void addDefaultExtensions(String[] accessIds) throws Exception {
        addBasicConstraints(false, false, null);
        addSubjectKeyIdentifier(false, this.certificateSigningRequest.getSubjectPublicKeyInfo());
        addAuthorityKeyIdentifier(false, this.authorityCertificate);
        addKeyUsage(true, new KeyUsage(224));
        addExtendedKeyUsage(false, new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth));
        addNetscapeCertificateType(false, new NetscapeCertType(128));
        addNetscapeComment(false, "DS2OS service certificate");
        addDs2osServiceManifest(false, "sha256:" + this.subjectDN + "Manifest");
        addDs2osIsKnowledgeAgent(false, false);

        ArrayList<String> accessIdsList = new ArrayList<>(Arrays.asList(accessIds));
        if (!accessIdsList.contains(this.subjectDN)) accessIdsList.add(this.subjectDN);
        addDs2osAccessIds(false, accessIdsList.toArray(new String[0]));
    }

    public Certificate build() throws Exception {
        ContentSigner signer = (new JcaContentSignerBuilder(this.algorithm)).setProvider(new BouncyCastleProvider()).build(this.authorityCertificateKey);
        return (new JcaX509CertificateConverter()).setProvider(new BouncyCastleProvider()).getCertificate(this.builder.build(signer));
    }
}
