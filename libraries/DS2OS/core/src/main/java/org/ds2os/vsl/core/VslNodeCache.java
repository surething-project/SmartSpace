package org.ds2os.vsl.core;

import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.exception.VslException;

/**
 * Interface for the Vsl Cache module.
 *
 * @author liebald
 */
public interface VslNodeCache {

    /**
     * Adds the given VslNode to the cache.
     *
     * @param address
     *            The address the VslNode belongs to in the Vsl.
     * @param node
     *            The VslNode to cache.
     */
    void cacheNode(String address, VslNode node);

    /**
     * Returns the VslNode at the given address. Null if the node isn't completely cached.
     *
     * @param address
     *            Address in the Vsl to check.
     * @param identity
     *            The Identity of the issuer of the operation, to check if he has access to the
     *            node.
     * @return The VslNode cache at the given address. Returns Null if the cache doesn't contain all
     *         necessary nodes.
     * @throws VslException
     *             Thrown if there is a differrent problem than not having the node cached, e.g. no
     *             access.
     */
    VslNode getCachedNode(String address, VslIdentity identity) throws VslException;

    /**
     * Handles a set operation. Can e.g. invalidate affected nodes or cache the set, depending on
     * implementation.
     *
     * @param address
     *            The address of the node that is set.
     * @param node
     *            The node that is set.
     */
    void handleSet(String address, VslNode node);

    /**
     * Handles a notification for the given address. Could e.g. invalidate affected nodes or request
     * the newest version, depending on implementation. All children of the given address are
     * affected, since it isn't sure whether the node itself changed or one of the children.
     *
     * @param address
     *            The address for which a notification was received.
     */
    void handleNotification(String address);
}
