package org.ds2os.vsl.service.registry;

import org.osgi.framework.Bundle;

public interface IServiceRegistry {
    Bundle installServiceFromJar(String paramString);

    void printServices();

    void removeBundle(String paramString);

    void removeBundles();

    String toStatusName(int paramInt);
}
