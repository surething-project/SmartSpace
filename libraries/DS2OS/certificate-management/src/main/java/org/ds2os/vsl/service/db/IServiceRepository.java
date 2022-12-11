package org.ds2os.vsl.service.db;

import java.util.Collection;

public interface IServiceRepository {
    void deleteService(String paramString) throws Exception;

    Collection<IServicePackage> getAllServices();

    IServicePackage getService(String paramString);

    void putService(String paramString1, String paramString2) throws Exception;

    void update() throws Exception;
}
