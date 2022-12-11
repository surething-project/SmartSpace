package org.ds2os.vsl.cache.metaCache;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ds2os.vsl.cache.replacement.ReplacementPolicies;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslKORCacheHandler;
import org.ds2os.vsl.core.config.VslCacheConfig;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslStructureNode;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for storage of Cache metaData.
 *
 * @author liebald
 */
public class MetaCache implements VslMetaCache {

    /**
     * Get the logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaCache.class);

    /**
     * Pattern used to extract the TTL from the cache String.
     */
    private static Pattern ttlPattern = Pattern.compile("ttl='\\d+'");

    /**
     * Access to the KOR for cache specific requests.
     */
    private final VslKORCacheHandler cacheHandler;

    /**
     * The map where the actual content is cached.
     */
    final Map<String, CachedNode> cachedNodes;

    /**
     * This collection contains all nodes that were removed from the metacache and should be removed
     * from the dataCache too.
     */
    Collection<String> removedFromCache;

    /**
     * The configuration service for the cache.
     */
    private final VslCacheConfig cacheConfig;

    /**
     * The {@link VslNodeFactory} used for creating cached nodes.
     */
    private final VslNodeFactory nodeFactory;

    /**
     * Constructor.
     *
     * @param cacheHandler
     *            access to the KOR (structure).
     * @param cacheConfig
     *            The configuration service for the cache.
     * @param nodeFactory
     *            the {@link VslNodeFactory} used for creating cached nodes.
     */
    public MetaCache(final VslKORCacheHandler cacheHandler, final VslCacheConfig cacheConfig,
            final VslNodeFactory nodeFactory) {
        this.cacheHandler = cacheHandler;
        cachedNodes = new ConcurrentHashMap<String, CachedNode>(cacheConfig.getCacheCapacity());
        this.cacheConfig = cacheConfig;
        removedFromCache = new LinkedList<String>();
        this.nodeFactory = nodeFactory;
    }

    @Override
    public final VslNode cache(final String address, final VslNode node) {
        VslStructureNode structure;
        VslMutableNode cachable = null;
        int newNodeSize = 0;
        try {
            structure = cacheHandler.getStructure(address);
        } catch (final NodeNotExistingException e) {
            return null;
        }

        if (getTTLfromStructure(structure.getCacheParameters()) <= 0
                || (cachedNodes.containsKey(address)
                        && cachedNodes.get(address).getNodeVersion() <= node.getVersion())) {
            // if the TTL is 0, don't cache this node -> set it to null
            // same if the node is already cached and the version is lower or equal.
            cachable = nodeFactory.createMutableNode();
        } else {
            cachable = nodeFactory.createMutableClone(nodeFactory.createImmutableLeaf(
                    node.getTypes(), node.getValue(), node.getTimestamp(), node.getVersion(),
                    node.getAccess(), node.getRestrictions()));
            newNodeSize++;
        }

        // check if all child nodes that should be cached actually exist (virtual Subtree) and have
        // a TTL>0
        for (final Entry<String, VslNode> child : node.getAllChildren()) {
            if (child.getValue().getValue() == null) {
                continue;
            }
            if (!structure.hasChild(child.getKey())) {
                return null;
            }
            if (getTTLfromStructure(structure.getChild(child.getKey()).getCacheParameters()) <= 0
                    || (cachedNodes.containsKey(address + "/" + child.getKey())
                            && cachedNodes.get(address + "/" + child.getKey())
                                    .getNodeVersion() <= child.getValue().getVersion())) {
                // if the TTL is 0, don't cache this node -> set it to null
                // same if the node is already cached and the version is lower or equal.
                cachable.putChild(child.getKey(), nodeFactory.createMutableNode());
            } else {
                cachable.putChild(child.getKey(),
                        nodeFactory.createMutableClone(nodeFactory.createImmutableLeaf(
                                child.getValue().getTypes(), child.getValue().getValue(),
                                child.getValue().getTimestamp(), child.getValue().getVersion(),
                                child.getValue().getAccess(), child.getValue().getRestrictions())));
                newNodeSize++;
            }
        }

        final int capacity = cacheConfig.getCacheCapacity();

        // if the requested node is larger than the available maximum capacity of the cache, nothing
        // can be cached.
        if (newNodeSize > capacity) {
            return null;
        }
        makePlaceIfCacheFull(newNodeSize, capacity);

        // add the node information to the metaCache.
        // also make sure when stuff is added it's not in the removedFromCache list.

        if (cachable.getValue() != null) {
            try {
                cachedNodes.put(address,
                        new CachedNode(node.getVersion(),
                                getTTLfromStructure(structure.getCacheParameters()),
                                node.getValue().getBytes(cacheConfig.getCharset()).length));
            } catch (final UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            removedFromCache.remove(address);
        }
        for (final Entry<String, VslNode> child : cachable.getAllChildren()) {
            if (child.getValue().getValue() != null) {
                try {
                    cachedNodes.put(address + "/" + child.getKey(), new CachedNode(
                            child.getValue().getVersion(),
                            getTTLfromStructure(
                                    structure.getChild(child.getKey()).getCacheParameters()),
                            child.getValue().getValue().getBytes(cacheConfig.getCharset()).length));
                } catch (final UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                removedFromCache.remove(address + "/" + child.getKey());
            }
        }
        return cachable;
    }

    @Override
    public final Map<String, CachedNode> getAllCachedNodes() {
        return cachedNodes;
    }

    @Override
    public final long getCurrentCacheSize() {
        long size = 0;
        for (final Entry<String, CachedNode> cachedNodesEntries : cachedNodes.entrySet()) {
            size += cachedNodesEntries.getValue().getSize();
        }
        return size;
    }

    @Override
    public final Collection<String> getNodesForRemoval() {
        final Collection<String> toRemove = removedFromCache;
        removedFromCache = new LinkedList<String>();
        return toRemove;
    }

    /**
     * Extracts the TTL as integer from the given cacheParameters.
     *
     * @param cacheParameters
     *            The cacheParameters containing the result.
     * @return The TTL as integer.
     */
    private int getTTLfromStructure(final String cacheParameters) {
        final Matcher matcher = ttlPattern.matcher(cacheParameters.toLowerCase());
        if (matcher.find()) {
            // LOGGER.debug("TTL: {}", matcher.group(0).substring(5, matcher.group(0).length() -
            // 1));
            return Integer.parseInt(matcher.group(0).substring(5, matcher.group(0).length() - 1));
        } else {
            return cacheConfig.getDefaultTTL();
        }
    }

    @Override
    public final boolean isCached(final String address, final VslIdentity identity) {
        VslStructureNode structure;
        try {
            structure = cacheHandler.getStructure(address);
        } catch (final NodeNotExistingException e) {
            return false;
        }
        // bugged: throws NPEs! <-- couldn't verify, maybe fixed?
        // try {
        // If a identity has access to a node that is not cached, we can't serve the request,
        // even
        // if other nodes would be cached.
        if (!Collections.disjoint(structure.getReaderIds(), identity.getAccessIDs())
                && !cachedNodes.containsKey(address)) {
            LOGGER.debug("not cached: {}", address);
            return false;
        }
        // } catch (final RuntimeException e) {
        // // DIRTY HACK: avoid NPEs..
        // return false;
        // }
        for (final Entry<String, VslStructureNode> child : structure.getAllChildren()) {
            if (!Collections.disjoint(child.getValue().getReaderIds(), identity.getAccessIDs())
                    && !cachedNodes.containsKey(address + "/" + child.getKey())) {
                LOGGER.debug("not cached: {}", address + "/" + child.getKey());
                return false;
            }
        }
        return true;
    }

    /**
     * This method makes room for the specified amount of nodes in the cache if necessary.
     *
     * @param newNodeSize
     *            The size of the new node.
     * @param capacity
     *            The capacity of the cache.
     */
    private void makePlaceIfCacheFull(final int newNodeSize, final int capacity) {
        // FIXME: replace node count based cache size by actual memory used
        // if (toCache.getValue() != null) {
        // newNodeSize += CachedNode.SIZE + toCache.getValue().getBytes().length
        // + 2 * Long.SIZE / 8;
        // // add two longs for version and timestamp
        // }
        //
        // for (final Entry<String, VslNode> child : toCache.getChildren().entrySet()) {
        // if (child.getValue().getValue() != null) {
        // newNodeSize += CachedNode.SIZE + child.getValue().getValue().getBytes().length
        // + 2 * Long.SIZE / 8;
        // // add two longs for version and timestamp
        // }
        // }

        // if (getCurrentCacheSize() + newNodeSize > capacity) {
        // final String policy = cacheConfig.getReplacementPolicy();
        // Collection<String> toRemove;
        // if (policy.equals("fifo")) {
        // toRemove = ReplacementPolicies.getNodesToRemoveFIFO(cachedNodes,
        // getCurrentCacheSize() + newNodeSize - capacity);
        // } else if (policy.equals("lru")) {
        // toRemove = ReplacementPolicies.getNodesToRemoveLRU(cachedNodes,
        // getCurrentCacheSize() + newNodeSize - capacity);
        // } else if (policy.equals("lfu")) {
        // toRemove = ReplacementPolicies.getNodesToRemoveLFU(cachedNodes,
        // getCurrentCacheSize() + newNodeSize - capacity);
        // } else {
        // toRemove = ReplacementPolicies.getNodesToRemoveRR(cachedNodes,
        // getCurrentCacheSize() + newNodeSize - capacity);
        // }
        // for (final String node : toRemove) {
        // cachedNodes.remove(node);
        // }
        // removedFromCache.addAll(toRemove);
        // }

        if (getCurrentCacheSize() + newNodeSize > capacity) {
            final String policy = cacheConfig.getReplacementPolicy();
            Collection<String> toRemove;
            if (policy.equals("fifo")) {
                toRemove = ReplacementPolicies.getNodesToRemoveFIFO(cachedNodes,
                        cachedNodes.size() + newNodeSize - capacity);
            } else if (policy.equals("lru")) {
                toRemove = ReplacementPolicies.getNodesToRemoveLRU(cachedNodes,
                        cachedNodes.size() + newNodeSize - capacity);
            } else if (policy.equals("lfu")) {
                toRemove = ReplacementPolicies.getNodesToRemoveLFU(cachedNodes,
                        cachedNodes.size() + newNodeSize - capacity);
            } else {
                toRemove = ReplacementPolicies.getNodesToRemoveRR(cachedNodes,
                        cachedNodes.size() + newNodeSize - capacity);
            }
            for (final String node : toRemove) {
                cachedNodes.remove(node);
            }
            removedFromCache.addAll(toRemove);
        }

    }

    @Override
    public final void removeFromCache(final String address) {
        cachedNodes.remove(address);
    }

    @Override
    public final void updatedCachedData(final String address, final VslNode node) {
        if (node.getValue() != null) {
            cachedNodes.get(address).nodeAccessed();
        }
        for (final Entry<String, VslNode> child : node.getAllChildren()) {
            if (child.getValue().getValue() != null) {
                cachedNodes.get(address + "/" + child.getKey()).nodeAccessed();
            }
        }
    }

    @Override
    public final void clearCache() {
        for (final String string : cachedNodes.keySet()) {
            removedFromCache.add(string);
        }
        cachedNodes.clear();
        LOGGER.debug("cleared cache, size: {}", cachedNodes.size());
    }

}
