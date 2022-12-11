package org.ds2os.vsl.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

/**
 * MIME type definitions for the VSL.
 *
 * @author felix
 */
public final class VslMimeTypes {

    /**
     * CBOR MIME type.
     */
    public static final String CBOR = "application/cbor";

    /**
     * Binary MIME type.
     */
    public static final String BINARY = "application/octet-stream";

    /**
     * JSON MIME type.
     */
    public static final String JSON = "application/json";

    /**
     * Protocol buffer's type for Content-type (not an official MIME type).
     */
    public static final String PROTOBUF = "application/x-protobuf-vsl";

    /**
     * XML MIME type.
     */
    public static final String XML = "application/xml";

    /**
     * XML MIME type (alias which is also allowed).
     */
    public static final String XML_ALIAS = "text/xml";

    /**
     * All MIME types (excluding aliases).
     */
    public static final Collection<String> ALL_MIME_TYPES = Collections
            .unmodifiableCollection(Arrays.asList(CBOR, JSON, PROTOBUF, XML));

    /**
     * Utility class - no instantiation.
     */
    private VslMimeTypes() {
        // utility class
    }

    /**
     * Static helper to get the unambiguous MIME type from the specified MIME type.
     *
     * @param mimeType
     *            the MIME type to normalize.
     * @return the MIME type for unambiguous matchin with {@link String#equals(Object)} or
     *         {@link String#hashCode()}.
     */
    public static String getNormalizedMimeType(final String mimeType) {
        if (mimeType == null) {
            return "";
        }
        final String normalized = mimeType.toLowerCase(Locale.ROOT);
        if (VslMimeTypes.XML_ALIAS.equals(normalized)) {
            return VslMimeTypes.XML;
        } else {
            return normalized;
        }
    }
}
