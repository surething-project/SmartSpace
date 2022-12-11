package org.ds2os.vsl.kor.locking;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslLockHandler;
import org.ds2os.vsl.core.config.VslKORLockingConfig;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.utils.AddressParser;
import org.ds2os.vsl.exception.AlreadyLockedException;
import org.ds2os.vsl.exception.NoPermissionException;
import org.ds2os.vsl.exception.NodeNotLockedException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.kor.VslNodeDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author liebald
 */
public class Locker implements VslLocker {
    /**
     * Get the logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Locker.class);

    /**
     * List/Map of all active Locks.
     */
    private final Map<String, LockedNode> activeLocks;

    /**
     * The VslNodeDatabase of the running implementation, which is used to commit locks/subtrees.
     */
    private final VslNodeDatabase myNodeDatabase;

    /**
     * Config service.
     */
    private final VslKORLockingConfig configService;

    /**
     * Constructor for the Locker.
     *
     * @param db            The VslNodeDatabase object used for committing locked updates.
     * @param configService The config service for the Locker.
     */
    public Locker(final VslNodeDatabase db, final VslKORLockingConfig configService) {
        myNodeDatabase = db;
        this.configService = configService;
        activeLocks = new HashMap<String, LockedNode>();

    }

    @Override
    public final void activate() {
        // Start the LockExpirationHandler to regularly check for expired locks.
        final LockExpirationHandler expirationHandler = new LockExpirationHandler(activeLocks,
                configService);
        final Thread t = new Thread(expirationHandler);
        t.setDaemon(true);
        t.start();
    }

    @Override
    public final void addNodeValueForCommit(final String address, final String value)
            throws NodeNotLockedException {
        synchronized (activeLocks) {
            activeLocks.get(getResponsibleLockAddress(address)).addNodeToLockedSubtree(address,
                    value);
        }
    }

    /**
     * Helper function that updates the given Vslnode on the given address (child of the node or
     * itself) with the given value. Also adapts the version numbers of all affected nodes.
     *
     * @param node            The node to adapt.
     * @param relativeAddress The relative Address of the value in the given node.
     * @param value           The value to set.
     */
    private void applyUpdateToVslNode(final VslMutableNode node, final String relativeAddress,
                                      final String value) {
        if (relativeAddress.isEmpty()) {
            node.setValue(value);
            node.setTimestamp(null);
        } else {
            ((VslMutableNode) node.getChild(relativeAddress)).setValue(value);
            ((VslMutableNode) node.getChild(relativeAddress)).setTimestamp(null);
        }
        // node.getChild(relativeAddress).setVersion(node.getChild(relativeAddress).getVersion() +
        // 1);
        String nextAddress = relativeAddress;
        while (!nextAddress.isEmpty()) {
            ((VslMutableNode) node.getChild(nextAddress))
                    .setVersion(node.getChild(nextAddress).getVersion() + 1);
            if (nextAddress.contains("/")) {
                nextAddress = nextAddress.substring(0, nextAddress.indexOf("/"));
            } else {
                break;
            }
        }
        node.setVersion(node.getVersion() + 1);
    }

    @Override
    public final void commitSubtree(final String address, final VslIdentity identity)
            throws VslException {
        synchronized (activeLocks) {

            // first check if there is even a lock
            // then check if the one who wants a commit is the owner of the lock
            // if all checks pass, commit all stored node updates to the database
            if (!activeLocks.containsKey(address)) {
                throw new NodeNotLockedException(
                        "Node " + address + " is not locked, cannot rollback Subtree.");
            } else if (!activeLocks.get(address).getLockHolder().equals(identity.getClientId())) {
                throw new NoPermissionException(
                        "No permission to commit/release the lock, lock was acquired by "
                                + activeLocks.get(address).getLockHolder() + ", your ID is "
                                + identity.getClientId());
            } else {
                // Access checks and so on are directly done before adding nodes to the locked
                // updateList, so we can directly add the nodes to the Database.
                // TODO: catch exceptions here instead of handing them over?
                myNodeDatabase.setValueTree(activeLocks.get(address).getUpdatedNodes());
                // remove the lock after commit is finished.
                activeLocks.remove(address);
            }
        }
    }

    /**
     * Helper function to receive all Locks that are below a certain address.
     *
     * @param address Address to check for child locks.
     * @return Map of all locks that are below the address and belong to the lockHolder.
     */
    private Map<String, LockedNode> getLocksBelowAddress(final String address) {
        final Map<String, LockedNode> result = new HashMap<String, LockedNode>();

        synchronized (activeLocks) {

            // if address itself is a lock, there can be no child with a lock, so we can directly
            // return.
            if (activeLocks.containsKey(address)) {
                result.put(address, activeLocks.get(address));
                return result;
            }

            // otherwise check if there are any locks below address.
            for (final Entry<String, LockedNode> entry : activeLocks.entrySet()) {
                if (entry.getKey().startsWith(address + "/")) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    /**
     * Helper function to receive all Locks that are below a certain address and belong to the given
     * ID.
     *
     * @param address    Address to check for child locks.
     * @param lockHolder The owner of the wanted locks.
     * @return Map of all locks that are below the address and belong to the lockHolder.
     */
    private Map<String, LockedNode> getLocksBelowAddress(final String address,
                                                         final String lockHolder) {
        final Map<String, LockedNode> result = new HashMap<String, LockedNode>();
        synchronized (activeLocks) {

            // if address itself is a lock, there can be no child with a lock, so we can directly
            // return.
            if (activeLocks.containsKey(address)
                    && activeLocks.get(address).getLockHolder().equals(lockHolder)) {
                result.put(address, activeLocks.get(address));
                return result;
            }

            // otherwise check if there are any locks below address.
            for (final Entry<String, LockedNode> entry : activeLocks.entrySet()) {
                if (entry.getKey().startsWith(address + "/")
                        && entry.getValue().getLockHolder().equals(lockHolder)) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    /**
     * Returns the lock that is responsible for the given address.
     *
     * @param address The address to get the responsible LockedNode for.
     * @return The responsible LockedNode object.
     * @throws NodeNotLockedException Thrown if one tries to store a node for a non existing lock.
     */
    private String getResponsibleLockAddress(final String address) throws NodeNotLockedException {
        synchronized (activeLocks) {

            if (activeLocks.containsKey(address)) {
                return address;
            }
            for (final String parent : AddressParser.getAllParentsOfAddress(address)) {
                if (activeLocks.containsKey(parent)) {
                    return parent;
                }
            }
            throw new NodeNotLockedException("Can't return responsible lock address, neither "
                    + address + " nor any parent is locked.");
        }
    }

    @Override
    public final boolean isChildLocked(final String address) {
        synchronized (activeLocks) {

            for (final String lockedNode : activeLocks.keySet()) {
                if (lockedNode.startsWith(address + "/")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public final boolean isLocked(final String address) {
        synchronized (activeLocks) {
            return activeLocks.containsKey(address) || isParentLocked(address);
        }
    }

    @Override
    public final boolean isLockedBy(final String address, final String lockerId) {

        if (!isLocked(address)) {
            return false;
        } else {
            try {
                synchronized (activeLocks) {
                    // LOGGER.debug("{},{}", getResponsibleLock(address).getLockHolder(), lockerId);
                    return activeLocks.get(getResponsibleLockAddress(address)).getLockHolder()
                            .equals(lockerId);
                }
            } catch (final NodeNotLockedException e) {
                LOGGER.debug("NodeNotLockedException that shouldn't happen");
                return false;
            }
        }
    }

    /**
     * Checks if a parent of the given node is locked.
     *
     * @param address The address to check.
     * @return Returns true if any parent is locked.
     */
    private boolean isParentLocked(final String address) {
        synchronized (activeLocks) {
            for (final String parentNode : AddressParser.getAllParentsOfAddress(address)) {
                if (activeLocks.containsKey(parentNode)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public final void lockSubtree(final String address, final VslIdentity identity,
                                  final List<String> accessIds, final VslLockHandler lockHandler)
            throws VslException {
        // first check if the identity of the requester matches the creator of the node he wants to
        // lock
        // then check if the lock already exists
        // check if a parent or a child is already locked
        // if all checks pass, create a new lock for the requested address.

        boolean canAcquireLock = false;
        if (accessIds.contains("*")) {
            canAcquireLock = true;

        } else {
            Set<String> identityAccessIds = identity
                    .getAccessIDs()
                    .parallelStream()
                    // .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            for (final String id : accessIds) {
                if (identityAccessIds.contains(id/*.toLowerCase()*/)) {
                    canAcquireLock = true;
                    break;
                }
            }
        }

        if (!canAcquireLock) {
            throw new NoPermissionException("No permission to acquire a lock at " + address
                    + ". You need read or write access in order to lock an subtree.");
        } else {
            synchronized (activeLocks) {

                if (activeLocks.containsKey(address)) {
                    throw new AlreadyLockedException(address + " is already locked");
                } else if (isParentLocked(address)) {
                    // check if there is a higher level lock from the same locker, if so commit it
                    // and
                    // then continue with the smaller lock.
                    if (activeLocks.get(getResponsibleLockAddress(address)).getLockHolder()
                            .equals(identity.getClientId())) {
                        commitSubtree(getResponsibleLockAddress(address), identity);
                    } else {
                        throw new AlreadyLockedException("A Parent of node " + address
                                + " is already locked, wait for the release"
                                + " of this lock before trying again.");
                    }

                } else if (isChildLocked(address)) {
                    // if one or more childs are locked commit all of them (if they belong to the
                    // lockholder) then continue
                    boolean allChildLocksFromSameLocker = true;
                    final Map<String, LockedNode> childLocks = getLocksBelowAddress(address);
                    for (final Entry<String, LockedNode> lock : childLocks.entrySet()) {
                        if (!lock.getValue().getLockHolder().equals(identity.getClientId())) {
                            allChildLocksFromSameLocker = false;
                            break;
                        }
                    }
                    // If all childs that are locked belong to the one who requested the lock on a
                    // parent, commit them all before requesting the new lock.
                    if (allChildLocksFromSameLocker) {
                        for (final Entry<String, LockedNode> lock : childLocks.entrySet()) {
                            commitSubtree(lock.getKey(), identity);
                        }
                    } else {
                        throw new AlreadyLockedException("A Child of node " + address
                                + " is already locked by someone else, wait for the release"
                                + " of this lock before trying again.");
                    }

                }
                // finally acquire the lock.
                activeLocks.put(address, new LockedNode(identity.getClientId(), lockHandler,
                        configService.getLockExpirationTime()));

                try {
                    lockHandler.lockAcquired(address);
                } catch (final VslException e) {
                    LOGGER.error("Error on notifying the lockholder that he aquired his lock: ", e);
                }
            }
        }
    }

    @Override
    public final void rollbackSubtree(final String address, final VslIdentity identity)
            throws VslException {
        synchronized (activeLocks) {

            // first check if there is even a lock
            // then check if the one who wants a rollback is the owner of the lock
            // if all checks pass, remove the lock, which also removes all stored node updates.
            if (!activeLocks.containsKey(address)) {
                throw new NodeNotLockedException(
                        "Node " + address + " is not locked, cannot rollback Subtree.");
            } else if (!activeLocks.get(address).getLockHolder().equals(identity.getClientId())) {
                throw new NoPermissionException(
                        "No permission to rollback/release the lock, lock was acquired by "
                                + activeLocks.get(address).getLockHolder() + ", your ID is "
                                + identity.getClientId());
            } else {
                activeLocks.remove(address);
            }
        }

    }

    @Override
    public final void updateGetResultWithLockedData(final String address, final VslMutableNode node,
                                                    final String issuerId) {
        final Map<String, LockedNode> affectedLocks = getLocksBelowAddress(address, issuerId);

        for (final Entry<String, LockedNode> lockEntry : affectedLocks.entrySet()) {
            for (final Entry<String, String> nodeUpdate : lockEntry.getValue().getUpdatedNodes()
                    .entrySet()) {
                if (nodeUpdate.getKey().equals(address)) {
                    applyUpdateToVslNode(node, "", nodeUpdate.getValue());
                } else {
                    applyUpdateToVslNode(node, nodeUpdate.getKey().substring(address.length() + 1
                    ), nodeUpdate.getValue());
                }
            }
        }
    }

}
