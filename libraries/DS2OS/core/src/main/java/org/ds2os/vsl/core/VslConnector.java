package org.ds2os.vsl.core;

import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.exception.VslException;

import java.io.InputStream;

/**
 * Connector to the VSL for services.
 *
 * @author borchers
 * @author felix
 * @author ugurlu
 * @author pahl
 */
public interface VslConnector {

    /**
     * Get the {@link VslNodeFactory} of this connector.
     *
     * @return the {@link VslNodeFactory}.
     */
    VslNodeFactory getNodeFactory();

    /**
     * Returns the address, to which the service is registered, IF
     * {@link #registerService(VslServiceManifest)} was called. Otherwise, an empty string is
     * returned.
     *
     * @return the absolute address or an empty string.
     */
    String getRegisteredAddress();

    /**
     * Register the service with the specified manifest.
     *
     * @param manifest
     *            the serice's manifest.
     * @return The full path of the services structure in the KOR (e.g. /KA1/serviceID).
     * @throws VslException
     *             If a VSL exception occurs.
     */
    String registerService(VslServiceManifest manifest) throws VslException;

    /**
     * Unregister this service.
     *
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void unregisterService() throws VslException;

    /**
     * Poll the knowledge at a given address.
     *
     * @param address
     *            the node address in the VSL.
     * @return VslNode object with all data (including children) of the node at this address.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    VslNode get(String address) throws VslException;

    /**
     * This method is used to set the knowledge at an address to the specified string value.
     *
     * @param address
     *            the node address in the VSL.
     * @param knowledge
     *            VslNode object describing the knowledge to be set (only the information which
     *            should be changed need to be specified here).
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void set(String address, VslNode knowledge) throws VslException;

    /**
     * This method is used to stream data from a virtual node at an address.
     * Callers of this method must close the returned {@link InputStream}.
     *
     * @param address
     *      the node address in the VSL.
     * @return
     *      the {@link InputStream} handle where data can be read from.
     * @throws VslException
     *      If a VSL exception occurs.
     */
    InputStream getStream(String address) throws VslException;

    /**
     * This method is used to stream data to a virtual node at an address.
     * Closes the provided {@link InputStream}.
     *
     * @param address
     *      the node address in the VSL.
     * @param stream
     *      the {@link InputStream} containing the data that should be sent.
     * @throws VslException
     *      If a VSL exception occurs.
     */
    void setStream(String address, InputStream stream) throws VslException;

    /**
     * This method can be used by the service on (child-) nodes which it registered as virtual
     * nodes. The service can send notifications to all subscribers of the specified address by
     * invoking this method.
     *
     * @param address
     *            the address in the VSL of the changed node.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void notify(String address) throws VslException;

    /**
     * Subscribe to changes of the knowledge at the given address (including changes in the subtree
     * of that address if present). A subscription will receive notifications for changes to the
     * direct address as well as all of the knowledge in its subtree. To successfully subscribe and
     * receive notifications, the requesting service needs at least reading rights on the requested
     * address.
     *
     * @param address
     *            the node address in the VSL.
     * @param subscriber
     *            the callback handler which will be notified about changes.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void subscribe(String address, VslSubscriber subscriber) throws VslException;

    /**
     * Unsubscribe from an address so that no more notifications to that subtree are received.
     *
     * @param address
     *            the node address in the VSL.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void unsubscribe(String address) throws VslException;

    /**
     * Tries to set an exclusive lock on the node/subtree at the specified address.
     *
     * @param address
     *            the address of the node/subtree to lock.
     * @param lockHandler
     *            the {@link VslLockHandler} for callbacks of this lock.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void lockSubtree(String address, VslLockHandler lockHandler) throws VslException;

    /**
     * Commit all changes on a locked node/subtree, releasing the lock.
     *
     * @param address
     *            the address of the node/subtree to commit.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void commitSubtree(String address) throws VslException;

    /**
     * Rollback all changes on a locked node/subtree, releasing the lock.
     *
     * @param address
     *            the address of the node/subtree to rollback.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void rollbackSubtree(String address) throws VslException;

    /**
     * Register a virtual node at the specified address. All data that is written to or read from
     * that virtual node will be directly passed to the virtualNodeHandler.
     *
     * @param address
     *            the node address in the VSL.
     * @param virtualNodeHandler
     *            the callback handler which will receive all get and set operations issued on this
     *            node or its children.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void registerVirtualNode(String address, VslVirtualNodeHandler virtualNodeHandler)
            throws VslException;

    /**
     * Unregister the virtual node at the specified address. The node is then usable like any normal
     * node again.
     *
     * @param address
     *            the node address in the VSL.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void unregisterVirtualNode(String address) throws VslException;
}
