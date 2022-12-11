package org.ds2os.vsl.service.security;

import org.apache.commons.lang3.time.DateUtils;
import org.bouncycastle.asn1.misc.NetscapeCertType;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Date;

public class SelfSignedCertificateBuilder extends CertificateBuilder {
    private final X500Name name;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final BigInteger serial;
    private final String subjectDN;

    public SelfSignedCertificateBuilder(String subjectDN, PublicKey publicKey, PrivateKey privateKey, Date validityStart, Date validityEnd) throws Exception {
        this.subjectDN = subjectDN;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.extUtils = new JcaX509ExtensionUtils();
        this.name = SecurityUtils.buildCertificateNameLittleEndian(subjectDN, "I8", "TUM", "Munich", "Germany", "DE");
        this.serial = BigInteger.valueOf(System.currentTimeMillis());
        this.builder = new JcaX509v3CertificateBuilder(this.name, this.serial, validityStart, validityEnd, this.name, publicKey);
    }

    public SelfSignedCertificateBuilder(String serviceName, PublicKey authorityPublicKey, PrivateKey authorityPrivateKey) throws Exception {
        this(serviceName, authorityPublicKey, authorityPrivateKey, new Date(), DateUtils.addYears(new Date(), 10));
    }

    public SelfSignedCertificateBuilder(String serviceName, PublicKey authorityPublicKey, PrivateKey authorityPrivateKey, int validity) throws Exception {
        this(serviceName, authorityPublicKey, authorityPrivateKey, new Date(), DateUtils.addSeconds(new Date(), validity));
    }

    public void addDefaultExtensions() throws Exception {
        addBasicConstraints(false, true, 0);
        addSubjectKeyIdentifier(false, this.publicKey);
        addAuthorityKeyIdentifier(false, this.publicKey, new X500Principal(this.name.toASN1Primitive().getEncoded()), this.serial);
        addKeyUsage(true, new KeyUsage(6));
        addNetscapeCertificateType(false, new NetscapeCertType(4));
        addNetscapeComment(false, "DS2OS CA");
    }

    public Certificate build() throws Exception {
        ContentSigner signer = (new JcaContentSignerBuilder(this.algorithm)).setProvider(new BouncyCastleProvider()).build(this.privateKey);
        return (new JcaX509CertificateConverter()).setProvider(new BouncyCastleProvider()).getCertificate(this.builder.build(signer));
    }
}
