package org.ds2os.vsl.service.model;

import java.util.Map;

public interface INode extends IEntity {
    void addService(String paramString) throws Exception;

    String[] getAllServiceNames() throws Exception;

    long getHeartBeatTimestamp();

    String getNodeAddress();

    String getNodeIdentifier();

    NodeStatus getNodeStatus();

    void setNodeStatus(NodeStatus paramNodeStatus);

    String getResource(ResourceType paramResourceType);

    Map<ResourceType, String> getResourceUsage();

    String[] getServiceIds();

    Map<String, IService> getServices();

    void removeService(String paramString) throws Exception;

    void setResourceValue(ResourceType paramResourceType, String paramString);

    void startService(String paramString) throws Exception;

    void stopService(String paramString) throws Exception;

    void updateHeartbeatTimestamp(long paramLong);

    enum NodeStatus {
        ALIVE, FAILED
    }
}
