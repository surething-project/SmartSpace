package org.ds2os.vsl.core;

import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.SubscriptionNotSupportedException;
import org.ds2os.vsl.exception.VslException;

import java.io.InputStream;

/**
 * This interface enables services to define nodes for piping and associate all operations that are
 * issued on this subtree with an implementation of this interface. This enables highly dynamic
 * data. To issue notifications to all subscribers use {@link VslConnector#notify}.
 *
 * @author borchers
 * @author felix
 * @author ugurlu
 * @author pahl
 */
public interface VslVirtualNodeHandler extends VslCallback {

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
     * Called to set data on the virtual node.
     *
     * @param address
     *            The address of the data to be set.
     * @param value
     *            The data to be set as a VslNode object (this may be very sparse, i.e. only
     *            containing what should be set/changed, esp. it does not contain types!).
     * @param identity
     *            the identity of the issuer of this operation.
     * @throws VslException
     *             Throws an VslException if the implementing handler decides someting isn't right
     *             (e.g. access or virtual subnode not existing).
     */
    void set(String address, VslNode value, VslIdentity identity) throws VslException;

    /**
     * Called to retrieve a stream on the virtual node.
     * The caller of this method must close the returned {@link InputStream}.
     *
     * @param address
     *      the address of the node in the VSL.
     * @param identity
     *      the identity of the issuer of this request.
     * @return
     *      the {@link InputStream} handle the data can be read off of.
     * @throws VslException
     *      if a VslException occurs.
     */
    InputStream getStream(String address, VslIdentity identity) throws VslException;

    /**
     * Called to write data via a stream to the virtual node.
     * Closes the provided {@link InputStream}.
     *
     * @param address
     *      the address of the node in the VSL.
     * @param stream
     *      the {@link InputStream} handle which delivers the data to be written.
     * @param identity
     *      the identity of the issuer of this request.
     * @throws VslException
     *      if a VslException occurs.
     */
    void setStream(String address, InputStream stream, VslIdentity identity) throws VslException;

    /**
     * Called by the KA when someone subscribes to an address of the virtual node. The service must
     * then start to notify the KA about changes or throw an exception here if subscriptions are not
     * allowed.
     *
     * @param address
     *            the address which is subscribed.
     * @throws SubscriptionNotSupportedException
     *             Special {@link VslException} for the case that this virtual node does not support
     *             subscriptions in general.
     * @throws VslException
     *             Throws an VslException if the subscription change cannot be performed.
     */
    void subscribe(String address) throws SubscriptionNotSupportedException, VslException;

    /**
     * Called by the KA when the last service unsubscribes a subscribed address of the virtual node.
     * The service can then stop to notify the KA about changes.
     *
     * @param address
     *            the address which is unsubscribed.
     * @throws VslException
     *             Throws an VslException if the subscription change cannot be performed.
     */
    void unsubscribe(String address) throws VslException;
}
