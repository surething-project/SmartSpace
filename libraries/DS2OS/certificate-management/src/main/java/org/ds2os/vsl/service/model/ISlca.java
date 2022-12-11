package org.ds2os.vsl.service.model;

import java.util.List;

public interface ISlca {
    List<String> getAccessRights(String paramString1, String paramString2) throws Exception;

    void invalidateCertificate(String paramString) throws Exception;

    void invalidateCertificateForService(String paramString) throws Exception;

    String requestCertificate(String paramString1, String paramString2) throws Exception;

    void setAccessRights(String paramString1, String paramString2, List<String> paramList) throws Exception;

    void setupUser(String paramString) throws Exception;
}
