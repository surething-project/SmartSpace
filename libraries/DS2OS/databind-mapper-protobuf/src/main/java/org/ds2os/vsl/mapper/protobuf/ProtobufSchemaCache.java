package org.ds2os.vsl.mapper.protobuf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;

/**
 * Protobuf schema cache and loader, reusing loaded schemas as much as possible.
 *
 * @author felix
 */
public class ProtobufSchemaCache {

    /**
     * The map of loaded schemas by name.
     */
    private final Map<String, ProtobufSchema> schemaMap = new HashMap<String, ProtobufSchema>();

    /**
     * Load a schema by name or reuse it from cache if it is already loaded.
     *
     * @param schemaName
     *            the schema name (must be a resource in the package of this class).
     * @return the loaded or cached schema.
     * @throws IOException
     *             If the loading or reading of the schema fails.
     */
    public final ProtobufSchema loadSchema(final String schemaName) throws IOException {
        if (schemaMap.containsKey(schemaName)) {
            return schemaMap.get(schemaName);
        }
        final InputStream resource = ProtobufSchemaCache.class.getResourceAsStream(schemaName);
        if (resource == null) {
            throw new IOException("Resource " + schemaName + " not found by classloader "
                    + ProtobufSchemaCache.class.getClassLoader() + ".");
        }
        final BufferedReader reader = new BufferedReader(new InputStreamReader(resource));
        final StringBuilder protofile = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                protofile.append(line);
            }
        } finally {
            reader.close();
        }
        final ProtobufSchema schema = ProtobufSchemaLoader.std.parse(protofile.toString());
        schemaMap.put(schemaName, schema);
        return schema;
    }
}
