package org.ds2os.vsl.service.model;

import java.security.cert.X509Certificate;

public interface INlsm {
    X509Certificate requestNewServiceCertificate(String paramString) throws Exception;
}
