package org.ds2os.vsl.service.model;

import org.ds2os.vsl.connector.ServiceConnector;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.exception.VslException;

public class Service implements IService {
    private final ServiceConnector connector;
    private final String nodeManagerRoot;
    private final boolean running = false;
    private final String serviceId;
    String connectedAgent;

    public Service(String serviceId, ServiceConnector connector, String nodeManagerRoot) {
        this.serviceId = serviceId;
        this.connector = connector;
        this.nodeManagerRoot = nodeManagerRoot;
        this.connectedAgent = null;
    }

    public String getServiceId() {
        return this.serviceId;
    }

    public boolean isRunning() {
        return this.running;
    }

    public void setConnectedAgent(String connectedAgent) {
        this.connectedAgent = connectedAgent;
    }

    public void start() {
        try {
            this.connector.get(this.connectedAgent + "/she/startService/serviceName=" + this.serviceId);
        } catch (VslException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if (Boolean.valueOf(this.connector.get(this.connectedAgent + "/she/isServiceRunning/serviceName=" + this.serviceId).getValue()).booleanValue())
                this.connector.get(this.connectedAgent + "/she/stopService/serviceName=" + this.serviceId);
        } catch (VslException e) {
            e.printStackTrace();
        }
    }

    public void update() throws Exception {
        VslNode result = this.connector.get("/" + this.nodeManagerRoot + "/" + this.serviceId, (new AddressParameters())
                .withDepth(1));
        String[] foundServices = result.getValue().split(";");
    }
}
