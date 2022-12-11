package org.ds2os.vsl.core.transport;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.VslAddressParameters;

import java.io.InputStream;

/**
 * Callback method enumeration for referring to callback methods in transport data.
 *
 * @author felix
 */
public enum CallbackMethod {

    /**
     * {@link org.ds2os.vsl.core.VslSubscriber#notificationCallback(String)}.
     */
    NOTIFY,

    /**
     * {@link org.ds2os.vsl.core.VslLockHandler#lockAcquired(String)}.
     */
    LOCK_AQUIRED,

    /**
     * {@link org.ds2os.vsl.core.VslLockHandler#lockWillExpire(String)}.
     */
    LOCK_WILL_EXPIRE,

    /**
     * {@link org.ds2os.vsl.core.VslLockHandler#lockExpired(String)}.
     */
    LOCK_EXPIRED,

    /**
     * {@link VslVirtualNodeHandler#get(String, VslAddressParameters, VslIdentity)}.
     */
    VGET,

    /**
     * {@link VslVirtualNodeHandler#set(String, VslNode, VslIdentity)}.
     */
    VSET,

    /**
     * {@link VslVirtualNodeHandler#getStream(String, VslIdentity)}.
     */
    VGET_STREAM,

    /**
     * {@link VslVirtualNodeHandler#setStream(String, InputStream, VslIdentity)}.
     */
    VSET_STREAM,

    /**
     * {@link VslVirtualNodeHandler#subscribe(String)}.
     */
    VSUBSCRIBE,

    /**
     * {@link VslVirtualNodeHandler#unsubscribe(String)}.
     */
    VUNSUBSCRIBE,
}
