package org.ds2os.vsl.service.db;

import java.security.cert.X509Certificate;

public interface IServicePackage {
    IContextModel getContextModel() throws Exception;

    X509Certificate getServiceCertificate() throws Exception;

    String getServiceExecutablePath() throws Exception;

    IServiceManifest getServiceManifest() throws Exception;

    String getServicePackagePath();

    void save(String paramString) throws Exception;

    void verify() throws Exception;
}
