package org.ds2os.vsl.service.security;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.misc.MiscObjectIdentifiers;
import org.bouncycastle.asn1.misc.NetscapeCertType;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public abstract class CertificateBuilder {
    protected String algorithm = "SHA256withRSA";
    protected X509v3CertificateBuilder builder;
    protected JcaX509ExtensionUtils extUtils;

    public void addAuthorityKeyIdentifier(boolean critical, PublicKey publicKey) throws Exception {
        this.builder.addExtension(Extension.authorityKeyIdentifier, critical, this.extUtils.createAuthorityKeyIdentifier(publicKey));
    }

    public void addAuthorityKeyIdentifier(boolean critical, PublicKey publicKey, X500Principal principal, BigInteger serial) throws Exception {
        this.builder.addExtension(Extension.authorityKeyIdentifier, critical, this.extUtils.createAuthorityKeyIdentifier(publicKey, principal, serial));
    }

    public void addAuthorityKeyIdentifier(boolean critical, X509Certificate authorityCert) throws Exception {
        this.builder.addExtension(Extension.authorityKeyIdentifier, critical, this.extUtils.createAuthorityKeyIdentifier(authorityCert));
    }

    public void addBasicConstraints(boolean critical, boolean ca, Integer pathLen) throws Exception {
        if (pathLen != null) {
            this.builder.addExtension(Extension.basicConstraints, critical, new BasicConstraints(pathLen));
        } else {
            this.builder.addExtension(Extension.basicConstraints, critical, new BasicConstraints(ca));
        }
    }

    public abstract void addDefaultExtensions() throws Exception;

    public void addDs2osAccessIds(boolean critical, String[] accessIds) throws Exception {
        DERUTF8String value = new DERUTF8String(String.join(";", accessIds));
        Extension ds2osAccessIds = new Extension(X509ObjectIdentifier.DS2OS_ACCESS_IDS.getOid(), critical, value.getEncoded());
        addExtension(ds2osAccessIds);
    }

    public void addDs2osIsKnowledgeAgent(boolean critical, boolean isKa) throws Exception {
        DERUTF8String value = new DERUTF8String(isKa ? "TRUE" : "FALSE");
        Extension ds2osIsKnowledgeAgent = new Extension(X509ObjectIdentifier.DS2OS_IS_KNOWLEDGE_AGENT.getOid(), critical, value.getEncoded());
        addExtension(ds2osIsKnowledgeAgent);
    }

    public void addDs2osServiceManifest(boolean critical, String manifest) throws Exception {
        Extension ds2osServiceManifest = new Extension(X509ObjectIdentifier.DS2OS_SERVICE_MANIFEST.getOid(), critical, (new DERUTF8String(manifest)).getEncoded());
        addExtension(ds2osServiceManifest);
    }

    public void addExtendedKeyUsage(boolean critical, ExtendedKeyUsage usage) throws Exception {
        this.builder.addExtension(Extension.extendedKeyUsage, critical, usage);
    }

    public void addExtension(Extension extension) throws Exception {
        this.builder.addExtension(extension);
    }

    public void addExtension(ASN1ObjectIdentifier oid, boolean critical, byte[] bytes) throws Exception {
        this.builder.addExtension(oid, critical, bytes);
    }

    public void addKeyUsage(boolean critical, KeyUsage usage) throws Exception {
        this.builder.addExtension(Extension.keyUsage, critical, usage);
    }

    public void addNetscapeCertificateType(boolean critical, NetscapeCertType type) throws Exception {
        this.builder.addExtension(MiscObjectIdentifiers.netscapeCertType, critical, type);
    }

    public void addNetscapeComment(boolean critical, String comment) throws Exception {
        Extension netscapeComment = new Extension(X509ObjectIdentifier.NETSCAPE_COMMENT.getOid(), critical, (new DERIA5String(comment)).getEncoded());
        addExtension(netscapeComment);
    }

    public void addSubjectAlternativeName(boolean critical, GeneralNames alternativeNames) throws Exception {
        this.builder.addExtension(Extension.subjectAlternativeName, critical, alternativeNames);
    }

    public void addSubjectKeyIdentifier(boolean critical, PublicKey publicKey) throws Exception {
        this.builder.addExtension(Extension.subjectKeyIdentifier, critical, this.extUtils.createSubjectKeyIdentifier(publicKey));
    }

    public void addSubjectKeyIdentifier(boolean critical, SubjectPublicKeyInfo publicKeyInfo) throws Exception {
        this.builder.addExtension(Extension.subjectKeyIdentifier, critical, this.extUtils.createSubjectKeyIdentifier(publicKeyInfo));
    }

    public abstract Certificate build() throws Exception;

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
