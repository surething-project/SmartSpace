package org.ds2os.vsl.cache.replacement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.ds2os.vsl.cache.metaCache.CachedNode;

/**
 * Class with implemenations for different replacement policies.
 *
 * @author liebald
 */
public final class ReplacementPolicies {

    /**
     * private utilitiy class constructor.
     */
    private ReplacementPolicies() {

    }

    /**
     * This method is used to decide which nodes to drop from the cache and should be implemented by
     * classes that implement the replacement. At least the specified amount of nodes that should be
     * removed from the cache will be returned. Implements a FIFO replacement strategy.
     *
     * @param currentlyCached
     *            The nodes that are currently cached.
     *
     * @param toFree
     *            The amount of space that should be freed at minimum.
     * @return All nodes that should be dropped.
     */
    public static Collection<String> getNodesToRemoveFIFO(
            final Map<String, CachedNode> currentlyCached, final long toFree) {
        /*
         * Idea:
         *
         * create Treemap where the key is the time the nodes were cached and the value is a list of
         * addresses that were cached at that time.
         *
         * The Treemap automatically orders these on iterating by the key, so the oldest come first.
         *
         * Then iterate over the map until enough addresses for dropping are gathered.
         */

        // create a map
        final Map<Long, List<String>> mapByInitialTimestamp = new TreeMap<Long, List<String>>();
        for (final Entry<String, CachedNode> cachedNode : currentlyCached.entrySet()) {
            List<String> addresses;
            if (mapByInitialTimestamp
                    .containsKey(cachedNode.getValue().getInitialCacheTimestamp())) {
                addresses = mapByInitialTimestamp
                        .get(cachedNode.getValue().getInitialCacheTimestamp());
            } else {
                addresses = new LinkedList<String>();
            }
            addresses.add(cachedNode.getKey());

            mapByInitialTimestamp.put(cachedNode.getValue().getInitialCacheTimestamp(), addresses);
        }
        final List<String> toRemove = new LinkedList<String>();

        long freed = 0;
        for (final Entry<Long, List<String>> entry : mapByInitialTimestamp.entrySet()) {
            toRemove.addAll(entry.getValue());
            freed += entry.getValue().size();
            // for (final String address : entry.getValue()) {
            // freed += currentlyCached.get(address).getSize();
            // }
            if (freed >= toFree) {
                break;
            }
        }
        return toRemove;
    }

    /**
     * This method is used to decide which nodes to drop from the cache and should be implemented by
     * classes that implement the replacement. At least the specified amount of nodes that should be
     * removed from the cache will be returned. Implements a LFU replacement strategy.
     *
     * @param currentlyCached
     *            The nodes that are currently cached.s
     *
     * @param toFree
     *            The amount of space that should be freed at minimum.
     * @return All nodes that should be dropped.
     */
    public static Collection<String> getNodesToRemoveLFU(
            final Map<String, CachedNode> currentlyCached, final long toFree) {
        /*
         * Idea:
         *
         * create Treemap where the key is the time the nodes were accessed the last time and the
         * value is a list of addresses that were cached at that time.
         *
         * The Treemap automatically orders these on iterating by the key, so the ones used the
         * least frequently come first.
         *
         * Then iterate over the map until enough addresses for dropping are gathered.
         */
        final Map<Integer, List<String>> mapByAccessFrequency = new TreeMap<Integer, List<String>>();
        for (final Entry<String, CachedNode> cachedNode : currentlyCached.entrySet()) {
            List<String> addresses;
            if (mapByAccessFrequency.containsKey(cachedNode.getValue().getAmountAccessed())) {
                addresses = mapByAccessFrequency.get(cachedNode.getValue().getAmountAccessed());
            } else {
                addresses = new LinkedList<String>();
            }
            addresses.add(cachedNode.getKey());

            mapByAccessFrequency.put(cachedNode.getValue().getAmountAccessed(), addresses);
        }
        final List<String> toRemove = new ArrayList<String>();

        long freed = 0;
        for (final Entry<Integer, List<String>> entry : mapByAccessFrequency.entrySet()) {
            toRemove.addAll(entry.getValue());
            freed += entry.getValue().size();
            // for (final String address : entry.getValue()) {
            // freed += currentlyCached.get(address).getSize();
            // }
            if (freed >= toFree) {
                break;
            }
        }
        return toRemove;
    }

    /**
     * This method is used to decide which nodes to drop from the cache and should be implemented by
     * classes that implement the replacement. At least the specified amount of nodes that should be
     * removed from the cache will be returned. Implements a LRU replacement strategy.
     *
     * @param currentlyCached
     *            The nodes that are currently cached.s
     *
     * @param toFree
     *            The amount of space that should be freed at minimum.
     * @return All nodes that should be dropped.
     */
    public static Collection<String> getNodesToRemoveLRU(
            final Map<String, CachedNode> currentlyCached, final long toFree) {
        /*
         * Idea:
         *
         * create Treemap where the key is the time the nodes were accessed the last time and the
         * value is a list of addresses that were cached at that time.
         *
         * The Treemap automatically orders these on iterating by the key, so the ones used the
         * least recently come first.
         *
         * Then iterate over the map until enough addresses for dropping are gathered.
         */
        final Map<Long, List<String>> mapBylastAccess = new TreeMap<Long, List<String>>();
        for (final Entry<String, CachedNode> cachedNode : currentlyCached.entrySet()) {
            List<String> addresses;
            if (mapBylastAccess.containsKey(cachedNode.getValue().getLastAccessed())) {
                addresses = mapBylastAccess.get(cachedNode.getValue().getLastAccessed());
            } else {
                addresses = new LinkedList<String>();
            }
            addresses.add(cachedNode.getKey());

            mapBylastAccess.put(cachedNode.getValue().getLastAccessed(), addresses);
        }
        final List<String> toRemove = new ArrayList<String>();

        long freed = 0;
        for (final Entry<Long, List<String>> entry : mapBylastAccess.entrySet()) {
            toRemove.addAll(entry.getValue());
            freed += entry.getValue().size();
            // for (final String address : entry.getValue()) {
            // freed += currentlyCached.get(address).getSize();
            // }
            if (freed >= toFree) {
                break;
            }
        }
        return toRemove;
    }

    /**
     * This method is used to decide which nodes to drop from the cache and should be implemented by
     * classes that implement the replacement. At least the specified amount of nodes that should be
     * removed from the cache will be returned. Implements a Random Replacement replacement
     * strategy.
     *
     * @param currentlyCached
     *            The nodes that are currently cached.s
     *
     * @param toFree
     *            The amount of space that should be freed at minimum.
     * @return All nodes that should be dropped.
     */
    public static Collection<String> getNodesToRemoveRR(
            final Map<String, CachedNode> currentlyCached, final long toFree) {
        // no meta information necessary, get all addresses currently cached
        final List<String> currentlyCachedAddresses = new ArrayList<String>(
                currentlyCached.keySet());
        // shuffle them
        Collections.shuffle(currentlyCachedAddresses);
        // and choose the first X for removal, where X is the specified amount that should be
        // dropped.
        final List<String> toRemove = new LinkedList<String>();
        long freed = 0;
        for (final String address : currentlyCachedAddresses) {
            toRemove.add(address);
            freed += 1; // currentlyCached.get(address).getSize();
            if (freed >= toFree) {
                break;
            }
        }
        return toRemove;
    }

}
