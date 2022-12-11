package org.ds2os.vsl.ka;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.VslVirtualNodeManager;
import org.ds2os.vsl.exception.NoVirtualNodeException;
import org.ds2os.vsl.exception.NodeAlreadyVirtualException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VirtualNodeManager Implementation. FIXME: get rid of apache commons usage.
 *
 * @author liebald
 */
public class VirtualNodeManager implements VslVirtualNodeManager {

    /**
     * Get the logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualNodeManager.class);
    /**
     * Map of registered VirtualNodes by their addresses.
     */
    private final ConcurrentMap<String, VslVirtualNodeHandler> virtualNodeHandlers;

    /**
     * Constructor of the VirtualNodeManager.
     */
    public VirtualNodeManager() {
        virtualNodeHandlers = new ConcurrentHashMap<String, VslVirtualNodeHandler>();
    }

    @Override
    public final void registerVirtualNode(final String address,
            final VslVirtualNodeHandler virtualNodeHandler) throws NodeAlreadyVirtualException {
        if (address != null && !address.isEmpty() && virtualNodeHandler != null
                && StringUtils.countMatches(address, "/") > 1) {
            if (isVirtualNode(address)) {
                LOGGER.debug("Replaced existing virtualNodeHandler at {} with new handler.",
                        address);
                // throw new NodeAlreadyVirtualException(
                // "Node at " + address + "is already registered as a Virtual Node.");
            }
            virtualNodeHandlers.put(address, virtualNodeHandler);
            LOGGER.debug("registered {} as virtual Node", address);
        } else {
            LOGGER.debug(
                    "Couldn't register Virtualnode at {} with handler {}."
                            + " address was empty, null or the virtualNodehandler was null.",
                    address, virtualNodeHandler);
        }
    }

    @Override
    public final void unregisterVirtualNode(final String address) throws NoVirtualNodeException {
        if (address != null && !address.isEmpty()) {
            if (virtualNodeHandlers.containsKey(address)) {
                virtualNodeHandlers.remove(address);
            } else {
                throw new NoVirtualNodeException(
                        "Couldn't unregister a virtualnode at address " + address);
            }
        } else {
            throw new NoVirtualNodeException("Invalid address, was either null or emtpy.");
        }

    }

    @Override
    public final VslVirtualNodeHandler getVirtualNodeHandler(final String address)
            throws NoVirtualNodeException {
        if (address != null && !address.isEmpty()) {
            if (virtualNodeHandlers.containsKey(address)) {
                return virtualNodeHandlers.get(address);
            } else {
                LOGGER.error("No virtualnode at address {}", address);
                throw new NoVirtualNodeException("No virtualnode at address " + address);
            }
        } else {
            throw new NoVirtualNodeException("Invalid address, was either null or emtpy.");
        }
    }

    @Override
    public final boolean isVirtualNode(final String address) {
        // LOGGER.debug("isVirtualNode: {} {}", address, virtualNodeHandlers.containsKey(address));
        if (address == null || address.isEmpty()) {
            return false;
        } else {
            return virtualNodeHandlers.containsKey(address);
        }
    }

    @Override
    public final String getFirstVirtualParent(final String address) {
        if (address == null || address.isEmpty() || StringUtils.countMatches(address, "/") <= 2) {
            return null;
        }
        String currentNode = address.substring(0, address.lastIndexOf("/"));

        while (StringUtils.countMatches(currentNode, "/") > 1) {
            if (isVirtualNode(currentNode)) {
                return currentNode;
            }
            currentNode = currentNode.substring(0, currentNode.lastIndexOf("/"));
        }
        return null;
    }

    @Override
    public final void unregisterAllVirtualNodes(final String address) {
        for (final Iterator<Entry<String, VslVirtualNodeHandler>> iterator = virtualNodeHandlers
                .entrySet().iterator(); iterator.hasNext();) {
            final Entry<String, VslVirtualNodeHandler> node = iterator.next();
            if (node.getKey().startsWith(address + "/") || node.getKey().equals(address)) {
                iterator.remove();
            }
        }
    }
}
