package org.ds2os.vsl.core;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

/**
 * @author jay
 * @author Johannes Straßer
 * @author liebald
 *
 */
public interface VslAgentRegistryService {

    /**
     * Checks if the agent is already in the list of connected KAs.
     *
     * @param agentID
     *            String object that represents name/ID of the agent.
     * @return True if the agentID is found in connectedKA list, false otherwise
     */
    boolean isAgentConnected(String agentID);

    /**
     * Checks if the _whole_ group defined by the received groupIDs is reachable by this node. The
     * calculation is done using information from the unconnected list.
     *
     * @param groupID
     *            The ping whose group is checked
     * @param numKAsExpected
     *            amount of KAs that should be in the group.
     * @return True if all group members are reachable, false if not
     */
    boolean isGroupReachable(String groupID, int numKAsExpected);

    /**
     * Checks if self is the leader of the current multicast overlay.
     *
     * @return True if self is the leader, false if not.
     */
    boolean isLeader();

    /**
     * Stores the received AlivePing message in the Connected KA list with the time stamp on which
     * it was received.
     *
     * @param receivedPing
     *            The {@link VslAlivePing} object as a received ping
     * @param timestamp
     *            The {@link Date} object with absolute time in milliseconds
     */
    void storeAlivePingToConnectedKAs(VslAlivePing receivedPing, Date timestamp);

    /**
     * Stores the received AlivePing message in the Unconnected KA list with the time stamp on which
     * it was received.
     *
     * @param receivedPing
     *            The {@link VslAlivePing} object as a received ping
     * @param timestamp
     *            The {@link Date} object with absolute time in milliseconds
     */
    void storeAlivePingToUnConnectedKAs(VslAlivePing receivedPing, Date timestamp);

    /**
     * Creates a new node in the AgentRegistryService model subtree rooted at the unConnectedKAs
     * list and sets agentID, and time stamp.
     *
     * @param agentID
     *            String object as name of the KA
     * @param timestamp
     *            The time in milliseconds describing when the ping was received
     */
    void addAgentToUnconnectedKAList(String agentID, Date timestamp);

    /**
     * Creates a new node in the AgentRegistryService model subtree rooted at the connectedKAs list
     * and sets agentID, time stamp of received request, and the certificate expiration fields.
     * Calling this function will remove the KA from the unConnectedKAs list.
     *
     * @param agentID
     *            String object as name of the KA.
     * @param timestamp
     *            The time in milliseconds describing when the ping was received
     * @param certificateExpiration
     *            The value represents the sender KA's certificate expiration time
     */
    void addAgentToConnectedKAList(String agentID, Date timestamp, Date certificateExpiration);

    /**
     * Fetches the hash of the current multicast group key stored in the AgentRegistryService model
     * and returns it. The hash also serves as groupID.
     *
     * @return Hash of the current multicast group key
     */
    String getMulticastGroupKeyHash();

    /**
     * Fetches the hashes of all stored multicast group keys.
     *
     * @return An array containing all multicast group key hashes
     */
    String[] getAllMulticastGroupKeyHashes();

    /**
     * Fetches the multicast group key with the given hash stored in the AgentRegistryService model.
     *
     * @param hash
     *            The hash value of the requested multicast group key
     * @return Multicast group key
     */
    String getMulticastGroupKey(String hash);

    /**
     * Fetches the keyString associated with the key obtained from the given hash from the
     * AgentRegistry.
     *
     * @param hash
     *            The hash value of the requested key string's multicast group key
     * @return The TLS key String
     */
    String getMulticastGroupKeyString(String hash);

    /**
     * Stores the received Multicast group key into the KOR. The key is received generally as a part
     * of initial KOR sync messages during handshakes. The given hash can be used later to retrieve
     * the key.
     *
     * @param receivedMulticastKey
     *            The Key that was received in the Authentication reply message.
     * @param keyHash
     *            The hash of the given key
     * @param keyString
     *            The keyString describing the use of the given key
     */
    void setMulticastGroupKey(String receivedMulticastKey, String keyHash, String keyString);

    /**
     * Returns the number of nodes in the current multicast network. This number includes this KA.
     *
     * @return The network size.
     */
    int getNetworkSize();

    /**
     * Gets the name of the leader of the group specified by the groupID.
     *
     * @param groupID
     *            The ID of the group
     * @return The ID of the group leader
     */
    String getLeader(String groupID);

    /**
     * Gets the list of stored transports for the given agentID. The list will be empty if the agent
     * could not be found.
     *
     * @param agentID
     *            The ID of the agent
     * @return The collection of transports
     */
    HashSet<VslTransportConnector> getTransports(String agentID);

    /**
     * Gets the IDs of all connected KAs (KA1, KA2,...).
     *
     * @return IDs of all connected KAs as Array.
     */
    String[] getConnectedAgentIds();

    /**
     * Fetches a specific attribute of a saved KA.
     *
     * @param agentID
     *            The agentID of the KA
     * @param attributeName
     *            The name of the attribute
     * @return The value of the Attribute
     */
    String getAttribute(String agentID, String attributeName);

    /**
     * Fetches the VslSymmetricKeyStore this class uses for non persistent key storage.
     *
     * @return The VslSymmetricKeyStore
     */
    VslSymmetricKeyStore getKeyStore();

    /**
     * Get information about all locally connected KAs. Excludes the own KA.
     *
     * @return Collection of VslKAInfo.
     */
    Collection<VslKAInfo> getConnectedKAInfo();

    /**
     * Removes all agents form the connected and unconnected lists for which no AlivePing has been
     * received for the last timeout milliseconds.
     *
     * @param timeout
     *            Timeout in milliseconds
     */
    void cleanStaleAgents(long timeout);

    /**
     * Returns the ids of all unconnected Agents.
     *
     * @return the ids of all unconencted Agents.
     */
    String[] getUnConnectedAgentIds();

    /**
     * Checks if an agent is both in the connected and unconnected list and removes him from
     * unconnected if this is the case.
     */
    void cleanDoubleConnectedAgents();
}
