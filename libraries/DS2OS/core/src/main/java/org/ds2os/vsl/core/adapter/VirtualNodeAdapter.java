package org.ds2os.vsl.core.adapter;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.InvalidOperationException;
import org.ds2os.vsl.exception.SubscriptionNotSupportedException;
import org.ds2os.vsl.exception.VslException;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Adapter for {@link VslVirtualNodeHandler} with default implementations for all methods, so
 * services can only overwrite those, which they need.
 *
 * @author felix
 */
public abstract class VirtualNodeAdapter implements VslVirtualNodeHandler {

    @Override
    public VslNode get(final String address, final VslAddressParameters params,
            final VslIdentity identity) throws VslException {
        throw new InvalidOperationException("This virtual node does not provide values for get.");
    }

    @Override
    public void set(final String address, final VslNode value, final VslIdentity identity)
            throws VslException {
        throw new InvalidOperationException("This virtual node does not support set operations.");
    }

    @Override
    public InputStream getStream(String address, VslIdentity identity) throws VslException {
        throw new InvalidOperationException("This virtual node does not support get stream operations.");
    }

    @Override
    public void setStream(String address, InputStream stream, VslIdentity identity) throws VslException {
        throw new InvalidOperationException("This virtual node does not support set stream operations.");
    }

    @Override
    public void subscribe(final String address)
            throws SubscriptionNotSupportedException, VslException {
        throw new SubscriptionNotSupportedException(
                "This virtual node does not support subscriptions.");
    }

    @Override
    public void unsubscribe(final String address) throws VslException {
        throw new SubscriptionNotSupportedException(
                "This virtual node does not support subscriptions.");
    }
}
