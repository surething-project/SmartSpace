package org.ds2os.vsl.service.db;

import java.security.cert.X509Certificate;
import java.util.List;

public interface ICertificateRepository {
    void createUser(String paramString) throws Exception;

    void deleteCertificate(String paramString1, String paramString2, String paramString3) throws Exception;

    List<String> getAccessRights(String paramString1, String paramString2) throws Exception;

    X509Certificate getCertificate(String paramString1, String paramString2, String paramString3) throws Exception;

    List<String> getExistingNodeIds();

    List<String> getNodeCertificatePaths(String paramString1, String paramString2) throws Exception;

    List<X509Certificate> getNodeCertificates(String paramString1, String paramString2) throws Exception;

    String getRepositoryPath();

    List<String> getUserIds();

    void putCertificate(String paramString1, String paramString2, String paramString3, X509Certificate paramX509Certificate) throws Exception;

    void setAccessRights(String paramString1, String paramString2, List<String> paramList) throws Exception;
}
