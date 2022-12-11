package org.ds2os.vsl.service;

import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.node.VslNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceUtils {
    public static String[] getConnectedAgents(VslConnector connector, String connectedAgent) throws Exception {
        VslNode result = connector.get("/" + connectedAgent + "/system/agentRegistryService/connectedKAs/elements");
        List<String> agents = new ArrayList<>();
        Collections.addAll(agents, result.getValue().split(";"));
        agents.add(connectedAgent);
        return agents.toArray(new String[0]);
    }
}
