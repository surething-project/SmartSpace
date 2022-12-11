package org.ds2os.vsl.agentregistry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ds2os.vsl.core.VslTransportConnector;
import org.ds2os.vsl.core.impl.AlivePing;
import org.ds2os.vsl.core.impl.TransportConnector;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.exception.VslException;

/**
 * Map an agent record inside the {@link AgentRegistryService}.
 *
 * @author felix
 */
public class AgentRecord extends AlivePing {

    /**
     * Timestamp of the last alive ping.
     */
    private final long timestamp;

    /**
     * Constructor using an {@link AlivePing} and timestamp in order to create an
     * {@link AgentRecord}.
     *
     * @param ping
     *            The {@link AlivePing} to use.
     * @param timestamp
     *            The timestamp.
     */
    public AgentRecord(final AlivePing ping, final long timestamp) {
        super(ping.getAgentId(), ping.getNumKAs(), ping.getCaPub(), ping.getTransports(),
                ping.getGroupID(), ping.getKorHash());
        this.timestamp = timestamp;
    }

    /**
     * Constructor using a {@link VslNode} in order to create an AgentRecord.
     *
     * @param node
     *            The {@link VslNode} used to create the AgentRecord
     * @throws VslException
     *             Thrown if the node can't be read sufficiently.
     */
    public AgentRecord(final VslNode node) throws VslException {
        this(readAlivePing(node), readTimestamp(node));
    }

    /**
     * Writes the contents of the {@link AlivePing} to a {@link VslNode}.
     *
     * @param nodeFactory
     *            {@link VslNodeFactory} used to generate a VslNode.
     * @return The VslNode containing the AgentRecord as children.
     */
    public final VslNode write(final VslNodeFactory nodeFactory) {
        final Map<String, VslNode> map = new HashMap<String, VslNode>();
        map.put("agentId", nodeFactory.createImmutableLeaf(getAgentId()));
        map.put("numKAs", nodeFactory.createImmutableLeaf(Integer.toString(getNumKAs())));
        map.put("groupID", nodeFactory.createImmutableLeaf(getGroupID()));
        map.put("korHash", nodeFactory.createImmutableLeaf(getKorHash()));
        map.put("timestamp", nodeFactory.createImmutableLeaf(Long.toString(timestamp)));
        // FIXME: add transports!!!
        return nodeFactory.createImmutableNode(map.entrySet());
    }

    /**
     * Read a timestamp from a {@link VslNode}.
     *
     * @param node
     *            The {@link VslNode} that should be parsed.
     * @return The timestamp from the given {@link VslNode}.
     * @throws VslException
     *             thrown if the timestamp couldn't be read.
     */
    private static long readTimestamp(final VslNode node) throws VslException {
        if (!node.hasChild("timestamp")) {
            throw new NodeNotExistingException("timestamp not found in agent record!");
        }
        return Long.parseLong(node.getChild("timestamp").getValue());
    }

    /**
     * Create an {@link AlivePing} from the given {@link VslNode}.
     *
     * @param node
     *            The {@link VslNode} to parse.
     * @return The parsed {@link AlivePing}.
     * @throws VslException
     *             thrown if not all necessary information is included in the {@link VslNode}.
     */
    private static AlivePing readAlivePing(final VslNode node) throws VslException {
        if (!node.hasChild("agentID")) {
            throw new NodeNotExistingException("agentID not found in agent record!");
        }
        final String agentId = node.getChild("agentID").getValue();

        if (!node.hasChild("numKAs")) {
            throw new NodeNotExistingException("numKAs not found in agent record!");
        }
        int numKAs;
        try {
            numKAs = Integer.parseInt(node.getChild("numKAs").getValue());
        } catch (final NumberFormatException e) {
            numKAs = 1;
        }

        if (!node.hasChild("groupID")) {
            throw new NodeNotExistingException("groupID not found in agent record!");
        }
        final String groupID = node.getChild("groupID").getValue();

        if (!node.hasChild("korHash")) {
            throw new NodeNotExistingException("korHash not found in agent record!");
        }
        final String korHash = node.getChild("korHash").getValue();

        final Set<VslTransportConnector> transports = new HashSet<VslTransportConnector>();
        for (final Map.Entry<String, VslNode> transportEntry : node.getChild("supportedTransports")
                .getDirectChildren()) {
            if ("add".equals(transportEntry.getKey()) || "del".equals(transportEntry.getKey())
                    || "elements".equals(transportEntry.getKey())) {
                continue;
            }
            transports.add(new TransportConnector(transportEntry.getValue().getValue()));
        }

        return new AlivePing(agentId, numKAs, "", transports, groupID, korHash);
    }
}
