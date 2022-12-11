package org.ds2os.vsl.kor.locking;

import java.util.List;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslLockHandler;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.exception.AlreadyLockedException;
import org.ds2os.vsl.exception.NodeNotLockedException;
import org.ds2os.vsl.exception.VslException;

/**
 * public interface for Locking.
 *
 * @author liebald
 */
public interface VslLocker {

    /**
     * Stores the given node until it is committed or a rollback occurs. The node is NOT set to the
     * database yet.
     *
     * @param address
     *            The address of the node.
     * @param value
     *            The value of the node.
     * @throws NodeNotLockedException
     *             Thrown if one tries to store a node for a non existing lock.
     */
    void addNodeValueForCommit(String address, String value) throws NodeNotLockedException;

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
     * Checks if a child of the given node is locked.
     *
     * @param address
     *            The address to check.
     * @return Returns true if any child is locked.
     */
    boolean isChildLocked(String address);

    /**
     * Returns if the specified address or any parent is currently locked.
     *
     * @param address
     *            The address to be checked.
     * @return returns if the address is locked.
     */
    boolean isLocked(String address);

    /**
     * Returns if the specified address or any parent is currently locked by lockerId.
     *
     * @param address
     *            The address to be checked.
     * @param lockerId
     *            The id of the expected lock holder.
     * @return returns if the address is locked.
     */
    boolean isLockedBy(String address, String lockerId);

    /**
     * Tries to set an exclusive lock on the node/subtree at the specified address.
     *
     * @param address
     *            the address of the node/subtree to lock.
     * @param identity
     *            the identity of the issuer of this operation.
     * @param accessIds
     *            reader and writer ids of the node.
     * @param lockHandler
     *            The {@link VslLockHandler} used to issue callbacks on certain events (e.g. lock
     *            expiration).
     * @throws AlreadyLockedException
     *             If the node is already locked via the same address by this service, i.e. the lock
     *             is acquired a second time on the same address and connector.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void lockSubtree(String address, VslIdentity identity, List<String> accessIds,
            VslLockHandler lockHandler) throws AlreadyLockedException, VslException;

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
     * Takes a VslNode and updates his and his childs values/versions with the values stored in the
     * locks that were created by the issuer of the get request and not yet commited. Locks hold by
     * other entities are ommited. The VslNode is directly changed/manipulated to mirror the correct
     * values, versions and timestamps.
     *
     * @param address
     *            The address of the node.
     * @param node
     *            The node to update.
     * @param issuerId
     *            The Id of the issuer of the get Request.
     */
    void updateGetResultWithLockedData(String address, VslMutableNode node, String issuerId);

    /**
     * Activates the LockControl, starting the thread that manages outdated locks, notifications,
     * etc.
     */
    void activate();

}
