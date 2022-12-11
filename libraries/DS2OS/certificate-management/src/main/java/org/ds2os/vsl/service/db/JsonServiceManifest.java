package org.ds2os.vsl.service.db;

import org.ds2os.vsl.service.model.ResourceType;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonServiceManifest implements IServiceManifest {
    private final List<String> conflictingContextModels = new ArrayList<>();
    private final List<JsonAccessRight> declaredAccessRights = new ArrayList<>();
    private final String developerId;
    private final List<String> requiredContextModels = new ArrayList<>();
    private final Map<ResourceType, Object> resourceRequirements = new HashMap<>();
    private final List<String> serviceDependencies = new ArrayList<>();
    private final String serviceId;
    private final String versionNumber;
    private String contextModelHash;
    private String executableHash;
    private String path;

    public JsonServiceManifest(@NotNull String serviceId, @NotNull String developerId, @NotNull String versionNumber, String executableHash, String contextModelHash, Map<ResourceType, Object> resourceRequirements, List<String> requiredContextModels, List<String> conflictingContextModels, List<String> serviceDependencies, List<JsonAccessRight> declaredAccessRights) {
        this.serviceId = serviceId;
        this.developerId = developerId;
        this.versionNumber = versionNumber;
        this.executableHash = executableHash;
        this.contextModelHash = contextModelHash;
        if (declaredAccessRights != null)
            this.declaredAccessRights.addAll(declaredAccessRights);
        if (resourceRequirements != null)
            this.resourceRequirements.putAll(resourceRequirements);
        if (requiredContextModels != null)
            this.requiredContextModels.addAll(requiredContextModels);
        if (conflictingContextModels != null)
            this.conflictingContextModels.addAll(conflictingContextModels);
        if (serviceDependencies != null)
            this.serviceDependencies.addAll(serviceDependencies);
    }

    public List<String> getConflictingContextModels() {
        return this.conflictingContextModels;
    }

    public String getContextModelHash() {
        return this.contextModelHash;
    }

    public void setContextModelHash(String contextModelHash) {
        this.contextModelHash = contextModelHash;
    }

    public List<? extends IAccessRight> getDeclaredAccessRights() {
        return this.declaredAccessRights;
    }

    public String getDeveloperId() {
        return this.developerId;
    }

    public String getExecutableHash() {
        return this.executableHash;
    }

    public void setExecutableHash(String executableHash) {
        this.executableHash = executableHash;
    }

    public String getManifestPath() {
        return this.path;
    }

    public List<String> getRequiredContextModels() {
        return this.requiredContextModels;
    }

    public Map<ResourceType, Object> getResourceRequirements() {
        return this.resourceRequirements;
    }

    public List<String> getServiceDependencies() {
        return this.serviceDependencies;
    }

    public String getServiceId() {
        return this.serviceId;
    }

    public String getVersionNumber() {
        return this.versionNumber;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String toString() {
        return "Service Id: " + this.serviceId + "\nDev Id: " + this.developerId + "\nVersion Number: " + this.versionNumber + "\nexecHash: " + this.executableHash + "\ncontextModelHash: " + this.contextModelHash + "\nrequiredContextModels: " + this.requiredContextModels

                .toString() + "\nconflictingContextModels: " + this.conflictingContextModels
                .toString() + "\ndeclaredAccessRights: " + this.declaredAccessRights
                .toString() + "\n";
    }
}
