package org.ds2os.vsl.service.db;

import org.ds2os.vsl.service.model.ResourceType;

import java.util.List;
import java.util.Map;

public interface IServiceManifest {
    String getContextModelHash();

    List<? extends IAccessRight> getDeclaredAccessRights();

    String getDeveloperId();

    String getExecutableHash();

    String getManifestPath();

    List<String> getRequiredContextModels();

    Map<ResourceType, Object> getResourceRequirements();

    List<String> getServiceDependencies();

    String getServiceId();

    String getVersionNumber();
}
