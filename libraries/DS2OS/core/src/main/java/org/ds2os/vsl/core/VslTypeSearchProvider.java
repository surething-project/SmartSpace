package org.ds2os.vsl.core;

import java.util.Collection;

import org.ds2os.vsl.core.node.VslStructureNode;

/**
 * Interface for Type search providers.
 *
 * @author liebald
 */
public interface VslTypeSearchProvider {

    /**
     * Adds the given addresses and their types to the underlying storage queried for requests.
     *
     * @param address
     *            The address of the root Node.
     * @param vslStructureNode
     *            The nodes that should be added.
     */
    void addTypes(String address, VslStructureNode vslStructureNode);

    /**
     * Removes the given address (and all child addresses) and their types from the underlying
     * storage system queried for requests.
     *
     * @param address
     *            The address to remove.
     */
    void removeAddress(String address);

    /**
     * Returns a Collection of Strings that contains all addresses in the local VSL that have the
     * given type.
     *
     * @param type
     *            The type of the queried addresses (e.g. /basic/text)
     * @return Collection of Strings containing all addresses as value (address1,address2,...).
     */
    Collection<String> getAddressesOfType(String type);

    /**
     * Returns a Collection of Strings that contains all types of the node at the given address. Not
     * necessarily in the same order they have in the VSL.
     *
     * @param address
     *            The address for which the types are queried.
     * @return Collection of Strings containing all types as value (type1,type2,...) and type.
     */
    Collection<String> getTypesOfAddress(String address);
}

