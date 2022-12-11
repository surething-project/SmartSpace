package org.ds2os.vsl.core.transport;

import org.ds2os.vsl.core.VslCallback;
import org.ds2os.vsl.core.VslLockHandler;
import org.ds2os.vsl.core.VslSubscriber;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.core.utils.AddressParser;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.CallbackInvocationException;
import org.ds2os.vsl.exception.VslException;

/**
 * Helper class for callback invocation and creation in transports.
 *
 * @author felix
 */
public final class CallbackHelper {

    /**
     * Utility class.
     */
    private CallbackHelper() {
    }

    /**
     * Helper for casting the callbacks to the their real type with
     * {@link CallbackInvocationException} in case of type errors.
     *
     * @param callback
     *            the callback.
     * @param clazz
     *            the desired callback type.
     * @param method
     *            the method which causes this conversion (included in exception for convenience).
     * @param <T>
     *            needed for type-safety.
     * @return the properly casted callback.
     * @throws CallbackInvocationException
     *             If the callback casting fails.
     */
    @SuppressWarnings("unchecked")
    public static <T extends VslCallback> T callbackCast(final VslCallback callback,
            final Class<T> clazz, final CallbackMethod method) throws CallbackInvocationException {
        try {
            if (clazz.isAssignableFrom(callback.getClass())) {
                return (T) callback;
            } else {
                throw new CallbackInvocationException("Callback method " + method
                        + " invoked on a callback which is not an instance of " + clazz.getName());
            }
        } catch (final RuntimeException e) {
            // should not happen...
            throw new CallbackInvocationException("Runtime exception during casting to "
                    + clazz.getName() + " for callback method " + method, e);
        }
    }

    /**
     * Invokes a callback method.
     *
     * @param callback
     *            the callback which was registered.
     * @param invocationMessage
     *            the invocation message.
     * @return the {@link VslNode} result of a VGET request, null otherwise.
     * @throws VslException
     *             If the callback or the invocation fails.
     */
    public static VslNode invokeCallbackMethod(final VslCallback callback,
            final CallbackInvocationMessage invocationMessage) throws VslException {
        final CallbackMethod method = invocationMessage.getInvokedMethod();
        switch (method) {
        case LOCK_AQUIRED:
            callbackCast(callback, VslLockHandler.class, method)
                    .lockAcquired(invocationMessage.getAddress());
            break;
        case LOCK_EXPIRED:
            callbackCast(callback, VslLockHandler.class, method)
                    .lockExpired(invocationMessage.getAddress());
            break;
        case LOCK_WILL_EXPIRE:
            callbackCast(callback, VslLockHandler.class, method)
                    .lockWillExpire(invocationMessage.getAddress());
            break;
        case NOTIFY:
            callbackCast(callback, VslSubscriber.class, method)
                    .notificationCallback(invocationMessage.getAddress());
            break;
        case VGET:
            final VslAddressParameters params = new AddressParameters(
                    AddressParser.getParametersFromURIQuery(invocationMessage.getAddress()));
            final int split = invocationMessage.getAddress().indexOf("?");
            final String address;
            if (split < 0) {
                address = invocationMessage.getAddress();
            } else {
                address = invocationMessage.getAddress().substring(0, split);
            }
            return callbackCast(callback, VslVirtualNodeHandler.class, method).get(address, params,
                    invocationMessage.getIdentity());
        case VSET:
            callbackCast(callback, VslVirtualNodeHandler.class, method).set(
                    invocationMessage.getAddress(), invocationMessage.getData(),
                    invocationMessage.getIdentity());
            break;
        case VSUBSCRIBE:
            callbackCast(callback, VslVirtualNodeHandler.class, method)
                    .subscribe(invocationMessage.getAddress());
            break;
        case VUNSUBSCRIBE:
            callbackCast(callback, VslVirtualNodeHandler.class, method)
                    .unsubscribe(invocationMessage.getAddress());
            break;
        default:
            throw new CallbackInvocationException("Callback method " + method
                    + " cannot be handled by CallbackHelper.invokeSimpleMethod.");
        }
        return null;
    }
}
