package org.ds2os.vsl.typeSearch;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ds2os.vsl.core.VslTypeSearchProvider;
import org.ds2os.vsl.core.node.VslStructureNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link VslTypeSearchProvider}, storing a in memory mapping of all addresses and
 * their types in the VSL.
 *
 * @author liebald
 */
public class TypeSearchProvider implements VslTypeSearchProvider {

    /**
     * The SLF4J logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TypeSearchProvider.class);

    /**
     * The map used for storing the type<->address correlations in memory.
     */
    private final Map<String, Set<String>> typeMap;

    /**
     * Constructor.
     */
    public TypeSearchProvider() {
        typeMap = new HashMap<String, Set<String>>();
        LOGGER.info("Initialized TypeSearchProvider.");
    }

    @Override
    public final synchronized void addTypes(final String address,
            final VslStructureNode vslStructureNode) {
        // LOGGER.debug("AddTypes called for {}",address);
        addAddress(address, vslStructureNode.getTypes());
        for (final Entry<String, VslStructureNode> entry : vslStructureNode.getAllChildren()) {
            if (address.equals("/")) {
                addAddress(address + entry.getKey(), entry.getValue().getTypes());
            } else {
                addAddress(address + "/" + entry.getKey(), entry.getValue().getTypes());
            }
        }
        // LOGGER.debug("All types stored: {}", typeMap.entrySet().toString());
    }

    /**
     * Helper method to add all types of a single node to the internal mapping.
     *
     * @param address
     *            The node to add.
     * @param types
     *            The types of that node.
     */
    private synchronized void addAddress(final String address, final List<String> types) {
        for (final String type : types) {
            if (!typeMap.containsKey(type)) {
                typeMap.put(type, new HashSet<String>());
            }
            typeMap.get(type).add(address);
            // LOGGER.debug("Added types for address {}", address);

        }

    }

    @Override
    public final synchronized void removeAddress(final String toRemove) {
        // LOGGER.debug("removeAddress called for {}", toRemove);
        // LOGGER.debug("before removal: {}",typeMap.entrySet().toString());
        final Map<String, Set<String>> intermediateToRemove = new HashMap<String, Set<String>>();
        // first collect all elements that should be removed
        for (final String type : typeMap.keySet()) {
            for (final String address : typeMap.get(type)) {
                if (address.equals(toRemove) || address.startsWith(toRemove + "/")) {
                    if (!intermediateToRemove.containsKey(type)) {
                        intermediateToRemove.put(type, new HashSet<String>());
                    }
                    intermediateToRemove.get(type).add(address);
                }
            }
        }
        // then remove them from the internal typeMap.
        for (final Entry<String, Set<String>> entry : intermediateToRemove.entrySet()) {
            for (final String address : entry.getValue()) {
                typeMap.get(entry.getKey()).remove(address);
            }
            if (typeMap.get(entry.getKey()).isEmpty()) {
                typeMap.remove(entry.getKey());
            }
        }
        // LOGGER.debug("after removal: {}", typeMap.entrySet().toString());

    }

    @Override
    public final synchronized Collection<String> getAddressesOfType(final String type) {
        // address, other?
        // TODO: access control
        final List<String> addresses = new LinkedList<String>();
        if (typeMap.containsKey(type)) {
            addresses.addAll(typeMap.get(type));
            // result = nodeFactory.createImmutableLeaf(StringUtils.join(typeMap.get(type), ","));
        }
        Collections.sort(addresses);
        return addresses;

    }

    @Override
    public final synchronized Collection<String> getTypesOfAddress(final String address) {
        // address, other?
        // TODO: access control
        final List<String> types = new LinkedList<String>();
        for (final String type : typeMap.keySet()) {
            if (typeMap.get(type).contains(address)) {
                types.add(type);
            }
        }
        return types;
    }

}
