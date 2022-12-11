package org.ds2os.vsl.service.security;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.x509.extension.X509ExtensionUtil;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class CSRBuilder {
    private final KeyPair keyPair;
    private final PKCS10CertificationRequestBuilder p10Builder;
    private final String subjectDN;
    private String algorithm = "SHA256withRSA";

    public CSRBuilder(X509Certificate authorityCertificate, KeyPair requesterKeyPair, String subject) {
        this.subjectDN = subject;
        this.keyPair = requesterKeyPair;
        X500Name name = SecurityUtils.buildCertificateName(this.subjectDN);
        this

                .p10Builder = new JcaPKCS10CertificationRequestBuilder(name, this.keyPair.getPublic());
    }

    public void addAttribute(ASN1ObjectIdentifier oid, ASN1Encodable value) throws Exception {
        this.p10Builder.addAttribute(oid, value);
    }

    public void addAttribute(ASN1ObjectIdentifier oid, byte[] encodedValue) throws Exception {
        this.p10Builder.addAttribute(oid, X509ExtensionUtil.fromExtensionValue(encodedValue));
    }

    public void addDs2osIsKnowledgeAgent(boolean isKa) throws Exception {
        DERUTF8String value = new DERUTF8String(isKa ? "TRUE" : "FALSE");
        addAttribute(X509ObjectIdentifier.DS2OS_IS_KNOWLEDGE_AGENT.getOid(), value);
    }

    public PKCS10CertificationRequest build() throws Exception {
        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(this.algorithm);
        ContentSigner signer = csBuilder.build(this.keyPair.getPrivate());
        return this.p10Builder.build(signer);
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
