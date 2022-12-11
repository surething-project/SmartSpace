package org.ds2os.vsl.kor.structureLogger;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.ds2os.vsl.core.utils.AddressParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Structure Logger is used to log all changes to the local KOR structure (add/del of nodes).
 *
 * @author liebald, pahl
 */
public class StructureLogger {
    /**
     * Defines how many log points should be stored. The last AMOUNT_OF_LOGPOINTS_TO_STORE log
     * points are stored. Older ones are deleted. This seems reasonable as the log points are needed
     * for updates only and if a foreign agent missed too many updates it can get the whole repo
     * anyways.
     */
    public static final int AMOUNT_OF_LOGPOINTS_TO_STORE = 1000;

    /**
     * Get the logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureLogger.class);

    /**
     * The ID of the local KA.
     */
    private final String localID;

    /**
     * The current ID of the log that increases monotonically.
     */
    private String myCurrentLogHash;

    /**
     * The structure to store the updates.
     */
    private final LinkedHashMap<String, LinkedList<String>> myStructureUpdates;

    /**
     * Constructor.
     *
     * @param localID
     *            The ID of the local KA.
     */
    public StructureLogger(final String localID) {
        myStructureUpdates = new LinkedHashMap<String, LinkedList<String>>();
        this.localID = localID;
        LOGGER.trace("Constructed StructureLogger.");
    }

    /**
     * Adds the given address to the StructureUpdate list/map. The address is added to the List of
     * the current hash.
     *
     * @param address
     *            Address that has changed.
     */
    private void addToStructureUpdateList(final String address) {
        final LinkedList<String> changesOnCurrentHash = myStructureUpdates.get(myCurrentLogHash);
        changesOnCurrentHash.add(address);
        myStructureUpdates.put(myCurrentLogHash, changesOnCurrentHash);
    }

    /**
     * Returns all addresses that changed since the KOR structure Hash was fromHash.
     *
     * @param fromHash
     *            The ID of the log point since when the changes should be reported.
     * @return An ArrayList of the changed addresses. Returns the address of the localKA
     *         ("/localka") and if the hash wasn't found a full update.
     */
    public final List<String> getChangeLogSincehash(final String fromHash) {

        final LinkedList<String> changedAddresses = new LinkedList<String>();

        boolean includeHash = false;

        // Iterate through the log list until we reach the fromHash hash as a key.
        // From there on include all changed addresses to the result, aside from the last one (since
        // this one can still change).
        for (final Entry<String, LinkedList<String>> entry : myStructureUpdates.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            if (entry.getKey().equals(fromHash)) {
                includeHash = true;
            }
            // only include changed addresses which changed after fromHash and are already
            // propagated (so not the current hash)
            if (includeHash && !entry.getKey().equals(myCurrentLogHash)) {
                for (final String address : entry.getValue()) {
                    if (!changedAddresses.contains(address)) {
                        // if a child of the current node is already included in the return list,
                        // remove it.

                        for (final Iterator<String> iterator = changedAddresses.iterator(); iterator
                                .hasNext();) {
                            final String add = iterator.next();
                            if (add.startsWith(address + "/")) {
                                iterator.remove();
                            }
                        }

                        // if a parent node is already in the list, we don't need to add it.
                        Boolean parentAlreadyIncluded = false;
                        for (final String add : AddressParser.getAllParentsOfAddress(address)) {
                            if (changedAddresses.contains(add)) {
                                parentAlreadyIncluded = true;
                            }
                        }
                        if (!parentAlreadyIncluded) {
                            changedAddresses.add(address);
                        }
                    }
                }
            }
        }

        if (!includeHash) {
            // indicate that we need a full update by returning "/localKA" as address)
            changedAddresses.add("/" + localID);
            LOGGER.debug("full update requested, fromHash not found in logger");
        }

        return changedAddresses;
    }

    /**
     * Returns the current Hash the Logger works with.
     *
     * @return The current Hash.
     */
    public final String getCurrentLogHash() {
        return myCurrentLogHash;
    }

    /**
     * Adds the address to the log.
     *
     * @param address
     *            Address of the node that changed.
     */
    public final void logChangedAddress(final String address) {
        LOGGER.trace("Adding address {} to changelog.", address);
        addToStructureUpdateList(address);
    }

    /**
     * Adds a new Hash as logpoint to the List of stored StructureChanges. If the hash already
     * exists in the log, no new logpoint is created (avoid collisions).
     *
     * @param hash
     *            The hash to add.
     */
    public final void newLogpointHash(final String hash) {
        LOGGER.trace("Setting new logID: {}", hash);
        if (!myStructureUpdates.containsKey(hash)) {
            this.myCurrentLogHash = hash;
            myStructureUpdates.put(myCurrentLogHash, new LinkedList<String>());
            // Delete older log points.
            while (myStructureUpdates.size() > AMOUNT_OF_LOGPOINTS_TO_STORE) {
                myStructureUpdates.remove(myStructureUpdates.entrySet().iterator().next().getKey());
                LOGGER.debug("Removed an entry from the StructureUpdate map to many (>{}) entries",
                        AMOUNT_OF_LOGPOINTS_TO_STORE);
            }
        }
    }

    @Override
    public final String toString() {
        return myStructureUpdates.toString();
    }

    /**
     * acivates the Structurelogger with an initial hash of the KOR.
     *
     * @param hash
     *            hash of the current local structure
     */
    public final void activate(final String hash) {
        if (hash == null) {
            myCurrentLogHash = "";
        } else {
            myCurrentLogHash = hash;
        }
        myStructureUpdates.put(myCurrentLogHash, new LinkedList<String>());
    }

    // /**
    // * Clears the stored structure update logs.
    // */
    // public final void clear() {
    // LOGGER.trace("Clearing log.");
    // myStructureUpdates.clear();
    // }

}
