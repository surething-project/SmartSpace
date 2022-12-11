package org.ds2os.vsl.core.config;

/**
 *
 * @author liebald
 */
public interface VslCharset {

    /**
     * Method to get the charset used by the VSL. (default UTF-8)
     *
     * @return String representation of the used charset.
     */
    @ConfigDescription(description = "The charset used by the local agent for String operations."
            + "", id = "ds2os.charset", defaultValue = "UTF-8", restrictions = ""
                    + "currently only UTF-8")
    String getCharset();
}
