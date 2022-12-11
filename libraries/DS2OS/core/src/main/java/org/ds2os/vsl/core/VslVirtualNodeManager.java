package org.ds2os.vsl.core;

import org.ds2os.vsl.exception.NoVirtualNodeException;
import org.ds2os.vsl.exception.NodeAlreadyVirtualException;

/**
 * Virtual node manager interface.
 *
 * @author liebald
 */
public interface VslVirtualNodeManager {

    /**
     * Register a virtual node at the specified address. All data that is written to or read from
     * that virtual node will be directly passed to the virtualNodeHandler. The address must be at
     * least on service level ("/KA/service") in order to be registered.
     *
     * @param address
     *            the node address in the VSL.
     * @param virtualNodeHandler
     *            the callback handler which will receive all get and set operations issued on this
     *            node or its children.
     * @throws NodeAlreadyVirtualException
     *             Thrown if there is already a VslVirtualNodeHandler registered under the given
     *             address.
     */
    void registerVirtualNode(String address, VslVirtualNodeHandler virtualNodeHandler)
            throws NodeAlreadyVirtualException;

    /**
     * Unregister the virtual node at the specified address. The node is then usable like any normal
     * node again.
     *
     * @param address
     *            the node address in the VSL.
     * @throws NoVirtualNodeException
     *             If the given address doesn't belong to a virtual Node.
     */
    void unregisterVirtualNode(String address) throws NoVirtualNodeException;

    /**
     * Returns the VslVirtualNodeHandler of the VirtualNode at the specified address. If the
     * specified node is not registered as virtual, an NoVirtualNodeException is thrown.
     *
     * @param address
     *            the node address in the VSL.
     * @return The responsible handler for this VirtualNode.
     * @throws NoVirtualNodeException
     *             If the queried address doesn't belong to a virtualNode.
     */
    VslVirtualNodeHandler getVirtualNodeHandler(String address) throws NoVirtualNodeException;

    /**
     * Checks if the given node is registered as virtual node.
     *
     * @param address
     *            The address of the node that should be checked.
     * @return True if the node is registered as virtualNode, false otherwise.
     */
    boolean isVirtualNode(String address);

    /**
     * Looks for the first parent of the given node that is registered as virtual. The address of
     * the given node is NOT checked if it is virtual. The checks stop at service level. (e.g. for
     * "/KA/service/c/d" "/KA/service/c" and "/KA/service" are checked.) Checks stop on the first
     * parent that is virtual, which is returned.
     *
     * @param address
     *            The address of the node that should be checked.
     * @return the address of the first registered parent address, null if no parent is virtual.
     *
     */
    String getFirstVirtualParent(String address);

    /**
     * Unregisters all virtual Nodes which are located below the given address, including the
     * address itself.
     *
     * @param address
     *            Address of the parent node.
     */
    void unregisterAllVirtualNodes(String address);
}
