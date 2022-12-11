package org.ds2os.vsl.cache;

import java.util.Map.Entry;

import org.ds2os.vsl.cache.cacheCleaner.CacheCleaner;
import org.ds2os.vsl.cache.dataCache.KORDataCache;
import org.ds2os.vsl.cache.dataCache.VslDataCache;
import org.ds2os.vsl.cache.metaCache.MetaCache;
import org.ds2os.vsl.cache.metaCache.VslMetaCache;
import org.ds2os.vsl.core.AbstractVslModule;
import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslKORCacheHandler;
import org.ds2os.vsl.core.VslNodeCache;
import org.ds2os.vsl.core.VslServiceManifest;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.adapter.VirtualNodeAdapter;
import org.ds2os.vsl.core.config.VslCacheConfig;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslStructureNode;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.exception.VslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link VslNodeCache}. This Class is responsible for managing the
 * interactions between the {@link VslMetaCache} which stores metadata of cached items (what is
 * cached, last access, access frequency,...). and the {@link VslDataCache} which stores the actual
 * cached content (e.g. using the KOR.).
 *
 * @author liebald
 */
public class Cache extends AbstractVslModule implements VslNodeCache {

    /**
     * Get the logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Cache.class);

    /**
     * Used to synchronize access to the cache.
     */
    private final Object threadLock = new Object();

    /**
     * Access to the KOR for cache specific requests.
     */
    private final VslKORCacheHandler cacheHandler;

    /**
     * The configuration service for the cache.
     */
    private final VslCacheConfig cacheConfig;

    /**
     * Stores metaInformatiosn for cached content (e.g. is a node cached, how often is it
     * accessed,...)
     */
    private final VslMetaCache metaCache;

    /**
     * Stores the actual content (VslNodes).
     */
    private final VslDataCache dataCache;

    /**
     * The Thread responsible for cleaning the cache from outdated items.
     */
    private final Thread cleanerThread;

    /**
     * Name of the local agent.
     */
    private final String agentName;

    /**
     * dummy manifest.
     */
    private final VslServiceManifest manifest = new VslServiceManifest() {

        @Override
        public String getModelId() {
            return "/system/cache";
        }

        @Override
        public String getModelHash() {
            return "";
        }

        @Override
        public String getBinaryHash() {
            return "";
        }
    };

    /**
     * Connector for local KOR access.
     */
    private final VslConnector connector;

    /**
     * address of the registered model.
     */
    private String modelAddress;

    /**
     * Virtualnode handler for the reseting the cache.
     */
    private final VslVirtualNodeHandler virtualNodeHandler = new VirtualNodeAdapter() {
        @Override
        public void set(final String address, final VslNode value, final VslIdentity identity)
                throws VslException {
            synchronized (threadLock) {
                metaCache.clearCache();
            }

        }

        @Override
        public VslNode get(final String address, final VslAddressParameters params,
                final VslIdentity identity) throws VslException {
            synchronized (threadLock) {
                metaCache.clearCache();
            }
            return connector.getNodeFactory().createImmutableLeaf("cache cleaned");
        }
    };

    /**
     * Constructor.
     *
     * @param connector
     *            {@link VslConnector} for accessing the local KOR
     * @param cacheHandler
     *            {@link VslKORCacheHandler} for accessing the KOR for cache specific methods.
     * @param cacheConfig
     *            The {@link VslCacheConfig} service for the cache.
     */
    public Cache(final VslConnector connector, final VslKORCacheHandler cacheHandler,
            final VslCacheConfig cacheConfig) {
        this.connector = connector;
        this.cacheHandler = cacheHandler;
        this.cacheConfig = cacheConfig;
        this.metaCache = new MetaCache(this.cacheHandler, this.cacheConfig,
                connector.getNodeFactory());
        this.dataCache = new KORDataCache(this.cacheHandler);
        cleanerThread = new Thread(new CacheCleaner(this.cacheConfig, metaCache, dataCache));
        agentName = cacheConfig.getAgentName();
    }

    @Override
    public final void activate() throws Exception {
        try {
            modelAddress = connector.registerService(manifest);
        } catch (final VslException e) {
            LOGGER.warn("could not register cache service: {}", e.getMessage());
        }
        try {
            connector.registerVirtualNode(modelAddress + "/clear", virtualNodeHandler);
        } catch (final VslException e) {
            LOGGER.warn("could not register clearCache node as virtual: {}", e.getMessage());
        }

        cleanerThread.start();
    }

    @Override
    public final void cacheNode(final String address, final VslNode node) {
        if (!cacheConfig.isCacheEnabled()) {
            return;
        }

        if (address.startsWith("/" + agentName + "/") || address.equals("/" + agentName)
                || address.equals("/")) {
            // LOGGER.debug("cache requests for the local agent or root not supported");
            return;
        }

        synchronized (threadLock) {
            final VslNode cachable = metaCache.cache(address, node);
            if (cachable == null) {
                return;
            }
            dataCache.cache(address, cachable);
            LOGGER.debug("cached VSLnode and children at {}", address);
        }

    }

    @Override
    public final VslNode getCachedNode(final String address, final VslIdentity identity)
            throws VslException {

        if (!cacheConfig.isCacheEnabled()) {
            return null;
        }
        if (address.startsWith("/" + agentName + "/") || address.equals("/" + agentName)
                || address.equals("/")) {
            // LOGGER.debug("cache requests for the local agent or root not supported");
            return null;
        }
        VslNode cachedNode;
        synchronized (threadLock) {
            if (!metaCache.isCached(address, identity)) {
                return null;
            }
            cachedNode = dataCache.get(address, identity);
            LOGGER.debug("dataCache get: {} {}", address, cachedNode);
            metaCache.updatedCachedData(address, cachedNode);
        }
        LOGGER.debug("served from Cache: {}", address);
        // TODO: add config option to enable cache marking returned nodes, used for tests
        // replaceFirstCharacter(cachedNode);
        return cachedNode;
    }

    // /**
    // * replaces the first character of the value of returned nodes with "c", marking them as
    // * returned from cache.
    // *
    // * @param cachedNode
    // * The node that is manipulated.
    // */
    // private void replaceFirstCharacter(final VslMutableNode cachedNode) {
    // if (cachedNode.getValue() != null) {
    // cachedNode.setValue(cachedNode.getValue().replaceFirst(".", "c"));
    // }
    // for (Entry<String, VslNode> node : cachedNode.getAllChildren()) {
    // if (node.getValue().getValue() != null) {
    // ((VslMutableNode) node.getValue()).setValue(node.getValue().getValue()
    // .replaceFirst(".", "c"));
    // }
    // }
    //
    // }

    @Override
    public final void shutdown() {
        cleanerThread.interrupt();
    }

    @Override
    public final void handleSet(final String address, final VslNode node) {
        /*
         * This first implementation invalidates all nodes that were set.
         *
         * Another possibility would be caching the set nodes itself.
         */
        // could also check structure first if set operation is allowed, but would slow down
        // processing a bit
        synchronized (threadLock) {
            if (node.getValue() != null) {
                metaCache.removeFromCache(address);
            }
            for (final Entry<String, VslNode> child : node.getAllChildren()) {
                if (child.getValue().getValue() != null) {
                    metaCache.removeFromCache(address + "/" + child.getKey());
                }
            }
        }
    }

    @Override
    public final void handleNotification(final String address) {
        /*
         * This first implementation simply invalidates the complete subtree of a notified node,
         * since it isn't sure which node changed.
         *
         * Other possibility would be to refresh the item instead of deleting it.
         */
        VslStructureNode structure;
        try {
            structure = cacheHandler.getStructure(address);
        } catch (final NodeNotExistingException e) {
            return;
        }
        synchronized (threadLock) {
            metaCache.removeFromCache(address);
            for (final Entry<String, VslStructureNode> child : structure.getAllChildren()) {
                metaCache.removeFromCache(address + "/" + child.getKey());
            }
        }
    }

}
