package org.ds2os.vsl.core;

/**
 * Offers access to the current hash of the KOR.
 *
 * @author liebald
 */
public interface VslKORHash {
    /**
     * The function returns the hash of the current version of KOR. The system subtree is excluded
     * from this hash (/KA/system/...)
     *
     * @return hash The string representation of the hash of current version of the KOR.
     */
    String getCurrentKORHash();

}
