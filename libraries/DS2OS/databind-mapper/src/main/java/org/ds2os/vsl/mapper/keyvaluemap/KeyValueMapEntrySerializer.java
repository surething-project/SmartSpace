package org.ds2os.vsl.mapper.keyvaluemap;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for map entries that uses explicit key and value fields.
 *
 * @author felix
 * @param <K>
 *            the key type.
 * @param <V>
 *            the value type.
 */
public final class KeyValueMapEntrySerializer<K, V> extends StdSerializer<Map.Entry<K, V>> {

    /**
     * Jackson deserializer must be serializable.
     */
    private static final long serialVersionUID = -7531157328326836462L;

    /**
     * Constructor of the key value map entry serializer.
     */
    public KeyValueMapEntrySerializer() {
        super(Map.Entry.class, false);
    }

    @Override
    public void serialize(final Map.Entry<K, V> value, final JsonGenerator gen,
            final SerializerProvider provider) throws IOException {
        final ObjectMapper mapper = (ObjectMapper) gen.getCodec();
        mapper.writeValue(gen, new KeyValueMapEntry<K, V>(value.getKey(), value.getValue()));
    }
}
