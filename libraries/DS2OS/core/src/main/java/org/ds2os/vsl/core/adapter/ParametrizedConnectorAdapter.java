package org.ds2os.vsl.core.adapter;

import org.ds2os.vsl.core.VslParametrizedConnector;
import org.ds2os.vsl.core.VslSubscriber;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.exception.VslException;

/**
 * Adapter for {@link VslParametrizedConnector} with default implementations for all the methods
 * that have both a normal and a parametrized version. Extending this class allows Connectors to
 * ignore the simple methods if desired and implement only the extended, parametrized ones, which
 * are called with by the default implementations of this adapter with default parameters.
 *
 * @author liebald
 */
public abstract class ParametrizedConnectorAdapter implements VslParametrizedConnector {

    @Override
    public VslNode get(final String address) throws VslException {
        return get(address, new AddressParameters());
    }

    @Override
    public void subscribe(final String address, final VslSubscriber subscriber)
            throws VslException {
        subscribe(address, subscriber, new AddressParameters());
    }

    @Override
    public void unsubscribe(final String address) throws VslException {
        unsubscribe(address, new AddressParameters());
    }

}
