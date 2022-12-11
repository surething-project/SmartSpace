package org.ds2os.vsl.core;

/**
 * Abstract base class for a module of a Vsl component.
 *
 * @author felix
 */
public abstract class AbstractVslModule {

    /**
     * Activate the module, starting its operation.
     *
     * @throws Exception
     *             If an exception occurs during startup.
     */
    public abstract void activate() throws Exception;

    /**
     * Shutdown the module and free all resources.
     */
    public abstract void shutdown();
}
