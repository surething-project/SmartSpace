package org.ds2os.vsl.mapper.keyvaluemap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Fake Map.Entry which does not implement Map.Entry for the map serialization with explicit key and
 * value fields done by {@link KeyValueMapSerializer} and {@link KeyValueMapDeserializer}.
 *
 * @author felix
 * @param <K>
 *            the key type.
 * @param <V>
 *            the value type.
 */
public final class KeyValueMapEntry<K, V> {

    /**
     * The key.
     */
    private final K key;

    /**
     * The value.
     */
    private final V value;

    /**
     * JSON creator constructor.
     *
     * @param key
     *            the key.
     * @param value
     *            the value.
     */
    @JsonCreator
    public KeyValueMapEntry(@JsonProperty("key") final K key,
            @JsonProperty("value") final V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Get the key.
     *
     * @return the key.
     */
    public K getKey() {
        return key;
    }

    /**
     * Get the value.
     *
     * @return the value.
     */
    public V getValue() {
        return value;
    }
}
