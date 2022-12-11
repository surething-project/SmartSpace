package org.ds2os.vsl.json.mapper;

import java.io.IOException;
import java.util.UUID;

import org.ds2os.vsl.core.transport.PostOperation;
import org.ds2os.vsl.core.transport.PostOperation.OperationType;
import org.ds2os.vsl.json.JsonTypeMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Implementation of {@link JsonTypeMapper} for {@link PostOperation}.
 *
 * @author felix
 */
public final class JsonPostOperationMapper implements JsonTypeMapper<PostOperation> {

    @Override
    public void writeValue(final PostOperation value, final JsonGenerator jsonGenerator)
            throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("operation", value.getOperation().name());
        if (value.getCallbackId() != null) {
            jsonGenerator.writeStringField("callbackId", value.getCallbackId().toString());
        }
        jsonGenerator.writeEndObject();
    }

    @Override
    public PostOperation readValue(final JsonParser jsonParser) throws IOException {
        if (!JsonToken.START_OBJECT.equals(jsonParser.getCurrentToken())) {
            throw new IOException("No Json object start token found.");
        }

        OperationType operation = null;
        UUID callbackId = null;

        while (jsonParser.nextValue() != null) {
            if (JsonToken.END_OBJECT.equals(jsonParser.getCurrentToken())) {
                break;
            }
            final String currentName = jsonParser.getCurrentName();
            if ("operation".equals(currentName)) {
                operation = OperationType.valueOf(jsonParser.getValueAsString());
            } else if ("callbackId".equals(currentName)) {
                try {
                    callbackId = UUID.fromString(jsonParser.getValueAsString());
                } catch (final IllegalArgumentException e) {
                    callbackId = null;
                }
            }
        }

        if (operation == null) {
            throw new IOException("Json post operation did not contain mandatory fields.");
        }
        return new PostOperation(operation, callbackId);
    }
}
