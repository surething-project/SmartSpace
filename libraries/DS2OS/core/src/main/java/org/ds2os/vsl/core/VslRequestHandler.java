package org.ds2os.vsl.core;

import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.VslException;

import java.io.InputStream;

/**
 * Internal VSL request handler to handle the requests a service issued via a {@link VslConnector}.
 * Note: in order to "generate" this interface, use the VslConnector interface and add VslIdentity
 * identity to each method. Everything else is copied.
 *
 * @author almost auto-generated
 */
public interface VslRequestHandler {

    /**
     * Register the service with the specified manifest.
     *
     * @param manifest
     *            the service's manifest.
     * @param identity
     *            the identity of the issuer of this operation.
     * @return The full path of the services structure in the KOR (e.g. /KA1/serviceID).
     * @throws VslException
     *             If a VSL exception occurs.
     */
    String registerService(VslServiceManifest manifest, VslIdentity identity) throws VslException;

    /**
     * Unregister this service.
     *
     * @param identity
     *            the identity of the issuer of this operation.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void unregisterService(VslIdentity identity) throws VslException;

    /**
     * Poll the knowledge at a given address.
     *
     * @param address
     *            the node address in the VSL.
     * @param identity
     *            the identity of the issuer of this operation.
     * @return VslNode object with all data (including children) of the node at this address.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    VslNode get(String address, VslIdentity identity) throws VslException;

    /**
     * Poll the knowledge at a given address.
     *
     * @param address
     *            the node address in the VSL.
     * @param params
     *            The parameters attached to this request.
     * @param identity
     *            the identity of the issuer of this operation.
     * @return VslNode object with all data (including children) of the node at this address.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    VslNode get(String address, VslAddressParameters params, VslIdentity identity)
            throws VslException;

    /**
     * This method is used to set the knowledge at an address to the specified string value.
     *
     * @param address
     *            the node address in the VSL.
     * @param knowledge
     *            VslNode object describing the knowledge to be set (only the information which
     *            should be changed need to be specified here).
     * @param identity
     *            the identity of the issuer of this operation.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void set(String address, VslNode knowledge, VslIdentity identity) throws VslException;

    /**
     * This method is used to request a stream for reading at an address.
     * The node at the address must be virtual.
     * Callers of this method must close the returned {@link InputStream}.
     *
     * @param address
     *      the node address in the VSL.
     * @param identity
     *      the identity of the issuer of this operation.
     * @return
     *      {@link InputStream} handle to read the data off of.
     * @throws VslException
     *      If a VSL exception occurs.
     */
    InputStream getStream(String address, VslIdentity identity) throws VslException;

    /**
     * This method is used to open a stream for writing at an address.
     * The node at the address must be virtual.
     * Closes the provided {@link InputStream}.
     *
     * @param address
     *      the node address in the VSL.
     * @param stream
     *      the {@link InputStream} handle the data is read off of and send to the node
     * @param identity
     *      the identity of the issuer of this operation.
     * @throws VslException
     *      If a VSL exception occurs.
     */
    void setStream(String address, InputStream stream, VslIdentity identity) throws VslException;

    /**
     * This method can be used by the service on (child-) nodes which it registered as virtual
     * nodes. The service can send notifications to all subscribers of the specified address by
     * invoking this method.
     *
     * @param address
     *            the address in the VSL of the changed node.
     * @param identity
     *            the identity of the issuer of this operation.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void notify(String address, VslIdentity identity) throws VslException;

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
     * @param identity
     *            the identity of the issuer of this operation.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void subscribe(String address, VslSubscriber subscriber, VslIdentity identity)
            throws VslException;

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
     *            the callback handler which will be notified about changes.#
     * @param params
     *            The parameters attached to this request.
     * @param identity
     *            the identity of the issuer of this operation.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void subscribe(String address, VslSubscriber subscriber, VslAddressParameters params,
            VslIdentity identity) throws VslException;

    /**
     * Unsubscribe from an address so that no more notifications to that subtree are received.
     *
     * @param address
     *            the node address in the VSL.
     * @param identity
     *            the identity of the issuer of this operation.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void unsubscribe(String address, VslIdentity identity) throws VslException;

    /**
     * Unsubscribe from an address so that no more notifications to that subtree are received.
     *
     * @param address
     *            the node address in the VSL.
     * @param params
     *            The parameters attached to this request.
     * @param identity
     *            the identity of the issuer of this operation.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void unsubscribe(String address, VslAddressParameters params, VslIdentity identity)
            throws VslException;

    /**
     * Tries to set an exclusive lock on the node/subtree at the specified address.
     *
     * @param address
     *            the address of the node/subtree to lock.
     * @param lockHandler
     *            the {@link VslLockHandler} for callbacks of this lock.
     * @param identity
     *            the identity of the issuer of this operation.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void lockSubtree(String address, VslLockHandler lockHandler, VslIdentity identity)
            throws VslException;

    /**
     * Commit all changes on a locked node/subtree, releasing the lock.
     *
     * @param address
     *            the address of the node/subtree to commit.
     * @param identity
     *            the identity of the issuer of this operation.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void commitSubtree(String address, VslIdentity identity) throws VslException;

    /**
     * Rollback all changes on a locked node/subtree, releasing the lock.
     *
     * @param address
     *            the address of the node/subtree to rollback.
     * @param identity
     *            the identity of the issuer of this operation.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void rollbackSubtree(String address, VslIdentity identity) throws VslException;

    /**
     * Register a virtual node at the specified address. All data that is written to or read from
     * that virtual node will be directly passed to the virtualNodeHandler.
     *
     * @param address
     *            the node address in the VSL.
     * @param virtualNodeHandler
     *            the callback handler which will receive all get and set operations issued on this
     *            node or its children.
     * @param identity
     *            the identity of the issuer of this operation.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void registerVirtualNode(String address, VslVirtualNodeHandler virtualNodeHandler,
            VslIdentity identity) throws VslException;

    /**
     * Unregister the virtual node at the specified address. The node is then usable like any normal
     * node again.
     *
     * @param address
     *            the node address in the VSL.
     * @param identity
     *            the identity of the issuer of this operation.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void unregisterVirtualNode(String address, VslIdentity identity) throws VslException;
}
