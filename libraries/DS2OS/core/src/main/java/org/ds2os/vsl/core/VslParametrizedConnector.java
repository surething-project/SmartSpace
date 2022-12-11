package org.ds2os.vsl.core;

import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.VslException;

/**
 * Extends the VslConnector with parameterized methods where applicable.
 *
 * @author liebald
 */
public interface VslParametrizedConnector extends VslConnector {

    /**
     * Poll the knowledge at a given address.
     *
     * @param address
     *            the node address in the VSL.
     * @param params
     *            The parameters attached to this request.
     * @return VslNode object with all data (including children) of the node at this address.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    VslNode get(String address, VslAddressParameters params) throws VslException;

    /**
     * Subscribe to changes of the knowledge at the given address (including changes in the subtree
     * of that address if present). A subscription will receive notifications for changes to the
     * direct address as well as all of the knowledge in its subtree. To successfully subscribe and
     * receive notifications, the requesting service needs at least reading rights on the requested
     * address.
     *
     * @param address
     *            the node address in the VSL.
     * @param params
     *            The parameters attached to this request.
     * @param subscriber
     *            the callback handler which will be notified about changes.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void subscribe(String address, VslSubscriber subscriber, VslAddressParameters params)
            throws VslException;

    /**
     * Unsubscribe from an address so that no more notifications to that subtree are received.
     *
     * @param address
     *            the node address in the VSL.
     * @param params
     *            The parameters attached to this request.
     * @throws VslException
     *             If a VSL exception occurs.
     */
    void unsubscribe(String address, VslAddressParameters params) throws VslException;
}
