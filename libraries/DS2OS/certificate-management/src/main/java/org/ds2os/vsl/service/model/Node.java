package org.ds2os.vsl.service.model;

import org.ds2os.vsl.connector.ServiceConnector;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.AddressParameters;

import javax.validation.constraints.NotNull;
import java.util.*;

public class Node implements INode {
    protected String agentRoot;
    protected ServiceConnector connector;
    protected String nodeManagerRoot;
    protected String publicKey;
    protected Map<ResourceType, String> resourceUsage = new HashMap<>();
    protected VslMutableNode runningServicesNode = null;
    protected Map<String, IService> services = new HashMap<>();
    protected INode.NodeStatus status;
    protected VslMutableNode stoppedServicesNode = null;
    private long heartBeatTimestamp;

    public Node(@NotNull String agentRoot, @NotNull String nodeManagerRoot, @NotNull ServiceConnector connector, @NotNull String publicKey) {
        this.agentRoot = agentRoot;
        this.nodeManagerRoot = nodeManagerRoot;
        this.connector = connector;
        this.publicKey = publicKey;
        this.resourceUsage.put(ResourceType.RUNNING_SERVICES, "0");
        this.heartBeatTimestamp = System.currentTimeMillis();
        this.status = INode.NodeStatus.ALIVE;
    }

    public void addService(String serviceId) throws Exception {
        if (!this.services.containsKey(serviceId)) {
            this.services.put(serviceId, new Service(serviceId, this.connector, this.nodeManagerRoot));
            String value = Arrays.toString(this.services.keySet().toArray());
            this.stoppedServicesNode.setValue(value);
            this.connector.commitSubtree(getStoppedServicesAddress());
        }
    }

    public String getAgentRoot() {
        return this.agentRoot;
    }

    public void setAgentRoot(String agentRoot) {
        this.agentRoot = agentRoot;
    }

    public String[] getAllServiceNames() throws Exception {
        List<String> serviceNames = new ArrayList<>();
        VslNode result = this.connector.get("/" + this.agentRoot + "", (new AddressParameters())
                .withDepth(1));
        for (Map.Entry<String, VslNode> entry : result.getDirectChildren())
            serviceNames.add(entry.getKey());
        return serviceNames.toArray(new String[serviceNames.size()]);
    }

    public long getHeartBeatTimestamp() {
        return this.heartBeatTimestamp;
    }

    public String getNodeAddress() {
        return this.nodeManagerRoot;
    }

    public String getNodeIdentifier() {
        return this.publicKey;
    }

    public String getNodeManagerRoot() {
        return this.nodeManagerRoot;
    }

    public void setNodeManagerRoot(String nodeManagerRoot) {
        this.nodeManagerRoot = nodeManagerRoot;
    }

    public INode.NodeStatus getNodeStatus() {
        return this.status;
    }

    public void setNodeStatus(INode.NodeStatus status) {
        this.status = status;
    }

    public String getResource(ResourceType resourceType) {
        return this.resourceUsage.get(resourceType);
    }

    public Map<ResourceType, String> getResourceUsage() {
        return this.resourceUsage;
    }

    private String getRunningServicesAddress() {
        return this.nodeManagerRoot + "/runningServices";
    }

    public String[] getServiceIds() {
        String[] result = new String[this.services.size()];
        int i = 0;
        for (String s : this.services.keySet())
            result[i++] = s;
        return result;
    }

    public Map<String, IService> getServices() {
        return this.services;
    }

    private String getStoppedServicesAddress() {
        return this.nodeManagerRoot + "/stoppedServices";
    }

    public int hashCode() {
        return getNodeIdentifier().hashCode();
    }

    public void removeService(String serviceId) throws Exception {
        if (this.services.remove(serviceId) != null) {
            String value = Arrays.toString(this.services.keySet().toArray());
            this.stoppedServicesNode.setValue(value);
            this.connector.commitSubtree(getStoppedServicesAddress());
        }
    }

    private String runningServicesToString() {
        List<String> result = new ArrayList<>(this.services.size());
        for (IService s : this.services.values()) {
            if (s.isRunning())
                result.add(s.getServiceId());
        }
        return Arrays.toString(result.toArray());
    }

    public void setResourceValue(ResourceType resourceType, String resourceValue) {
        this.resourceUsage.put(resourceType, resourceValue);
    }

    public void startService(String serviceId) throws Exception {
        IService service = this.services.get(serviceId);
        if (service == null)
            throw new Exception("Service " + serviceId + " does not exist locally");
        if (service.isRunning())
            throw new Exception("Service " + serviceId + " is already running");
        service.start();
        this.runningServicesNode.setValue(runningServicesToString());
        this.connector.commitSubtree(getRunningServicesAddress());
        this.resourceUsage.put(ResourceType.RUNNING_SERVICES, this.resourceUsage.get(ResourceType.RUNNING_SERVICES));
    }

    public void stopService(String serviceId) throws Exception {
        IService service = this.services.get(serviceId);
        if (service == null)
            throw new Exception("Service " + serviceId + " does not exist locally");
        if (service.isRunning())
            throw new Exception("Service " + serviceId + " is not running");
        service.stop();
        this.runningServicesNode.setValue(runningServicesToString());
        this.connector.commitSubtree(getRunningServicesAddress());
    }

    public String toString() {
        return getNodeAddress();
    }

    public void update() throws Exception {
        VslNode result = this.connector.get(
                getStoppedServicesAddress(), (new AddressParameters()).withDepth(1));
        this.services.clear();
        if (result != null && !result.getValue().isEmpty()) {
            this.stoppedServicesNode = (VslMutableNode) result;
            String[] foundServices = result.getValue().split(";");
            for (String serviceId : foundServices) {
                Service service = new Service(serviceId, this.connector, this.nodeManagerRoot);
                service.update();
                this.services.put(serviceId, service);
            }
        }
        result = this.connector.get(getRunningServicesAddress(), (new AddressParameters()).withDepth(1));
        if (result != null && !result.getValue().isEmpty())
            this.runningServicesNode = (VslMutableNode) result;
    }

    public void updateHeartbeatTimestamp(long l) {
        this.heartBeatTimestamp = l;
    }
}
