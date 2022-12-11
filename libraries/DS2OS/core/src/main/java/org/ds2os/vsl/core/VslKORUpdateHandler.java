package org.ds2os.vsl.core;

/**
 * Generates {@link VslKORUpdate}s for the {@link VslKORSyncHandler} and the necessary hashes.
 * Applies updates to the KOR.
 *
 * @author liebald
 */
public interface VslKORUpdateHandler {

    /**
     * Returns an {@link VslKORUpdate} of the local KA subtree which starts on the given hash and
     * contains all updates until the now. If the given hash isn't found in the local log, a full
     * update on the local node is returned.
     *
     * @param hash
     *            The hash from which on the update is required.
     * @return The {@link VslKORUpdate} from the given hash to the current version.
     */
    VslKORUpdate getKORUpdateFromHash(String hash);

    /**
     * Applies the given {@link VslKORUpdate} to the local KOR. Only done for Updates on remote KA
     * subtrees.
     *
     * @param update
     *            The update that should be applied.
     */
    void applyKORUpdate(VslKORUpdate update);

    /**
     * The function returns the hash of the current version of KOR. The system subtree is excluded
     * from this hash (/KA/system/...)
     *
     * @return hash The string representation of the hash of current version of the KOR.
     */
    String getCurrentKORHash();

    /**
     * The function returns the hash of the current version of the structure of the given KA. The
     * system subtree is excluded from this hash (/KA/system/...)
     *
     * @param kaAddress
     *            The root address of the KA for which the current hash should be returned.
     * @return hash The string representation of the hash of current version of the KOR.
     */
    String getCurrentKORHashOf(String kaAddress);

    /**
     * Used to set the {@link VslKORSyncHandler} used by KOR to send incremental updates. Must be
     * called once before the KOR can send updates.
     *
     * @param korSyncHandler
     *            The {@link VslKORSyncHandler} to use.
     */
    void setKORSyncHandler(VslKORSyncHandler korSyncHandler);
}
