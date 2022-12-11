package org.ds2os.vsl.json.mapper;

import java.io.IOException;

import org.ds2os.vsl.core.VslServiceManifest;
import org.ds2os.vsl.core.impl.ServiceManifest;
import org.ds2os.vsl.json.JsonTypeMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Implementation of {@link JsonTypeMapper} for {@link VslServiceManifest}.
 *
 * @author felix
 */
public final class JsonServiceManifestMapper implements JsonTypeMapper<VslServiceManifest> {

    @Override
    public void writeValue(final VslServiceManifest value, final JsonGenerator jsonGenerator)
            throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("modelId", value.getModelId());
        jsonGenerator.writeStringField("modelHash", value.getModelHash());
        if (value.getBinaryHash() != null) {
            jsonGenerator.writeStringField("binaryHash", value.getBinaryHash());
        }
        jsonGenerator.writeEndObject();
    }

    @Override
    public VslServiceManifest readValue(final JsonParser jsonParser) throws IOException {
        if (!JsonToken.START_OBJECT.equals(jsonParser.getCurrentToken())) {
            throw new IOException("No Json object start token found.");
        }

        String modelId = null;
        String modelHash = null;
        String binaryHash = null;

        while (jsonParser.nextValue() != null) {
            if (JsonToken.END_OBJECT.equals(jsonParser.getCurrentToken())) {
                break;
            }
            final String currentName = jsonParser.getCurrentName();
            if ("modelId".equals(currentName)) {
                modelId = jsonParser.getValueAsString();
            } else if ("modelHash".equals(currentName)) {
                modelHash = jsonParser.getValueAsString();
            } else if ("binaryHash".equals(currentName)) {
                binaryHash = jsonParser.getValueAsString();
            }
        }

        if (modelId == null || modelHash == null) {
            throw new IOException("Json post operation did not contain mandatory fields.");
        }
        return new ServiceManifest(modelId, modelHash, binaryHash);
    }
}
