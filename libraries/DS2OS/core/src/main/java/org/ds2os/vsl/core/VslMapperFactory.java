package org.ds2os.vsl.core;

import java.util.Collection;

/**
 * Creates fully initialized {@link VslMapper} instances.
 *
 * @author felix
 */
public interface VslMapperFactory {

    /**
     * Get the MIME types for which a mapper is available in this factory.
     *
     * @return a collection of the MIME type strings.
     */
    Collection<String> getSupportedMimeTypes();

    /**
     * Get a mapper for the MIME type.
     *
     * @param mimeType
     *            the MIME type string.
     * @return a mapper instance that supports this type or null if the type is not supported.
     */
    VslMapper getMapper(String mimeType);
}
