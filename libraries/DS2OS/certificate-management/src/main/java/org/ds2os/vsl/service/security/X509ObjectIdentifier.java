package org.ds2os.vsl.service.security;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

public enum X509ObjectIdentifier {
    NETSCAPE_COMMENT("2.16.840.1.113730.1.13"),
    DS2OS_IS_KNOWLEDGE_AGENT("1.3.6.1.4.1.0"),
    DS2OS_SERVICE_MANIFEST("1.3.6.1.4.1.1"),
    DS2OS_ACCESS_IDS("1.3.6.1.4.1.2"),
    DS2OS_SERVICE_OPTIONS("1.3.6.1.4.1.3");

    private final String oid;

    X509ObjectIdentifier(String oid) {
        this.oid = oid;
    }

    public ASN1ObjectIdentifier getOid() {
        return new ASN1ObjectIdentifier(this.oid);
    }

    public String getRawOid() {
        return this.oid;
    }
}
