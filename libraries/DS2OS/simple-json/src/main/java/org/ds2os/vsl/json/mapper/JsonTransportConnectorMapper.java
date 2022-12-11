package org.ds2os.vsl.json.mapper;

import java.io.IOException;

import org.ds2os.vsl.core.VslTransportConnector;
import org.ds2os.vsl.core.impl.TransportConnector;
import org.ds2os.vsl.json.JsonTypeMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Implementation of {@link JsonTypeMapper} for {@link VslTransportConnector}.
 *
 * @author felix
 */
public final class JsonTransportConnectorMapper implements JsonTypeMapper<VslTransportConnector> {

    @Override
    public void writeValue(final VslTransportConnector value, final JsonGenerator jsonGenerator)
            throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("url", value.getURL());
        jsonGenerator.writeEndObject();
    }

    @Override
    public VslTransportConnector readValue(final JsonParser jsonParser) throws IOException {
        if (!JsonToken.START_OBJECT.equals(jsonParser.getCurrentToken())) {
            throw new IOException("No Json object start token found.");
        }

        String url = null;
        while (jsonParser.nextValue() != null) {
            if (JsonToken.END_OBJECT.equals(jsonParser.getCurrentToken())) {
                break;
            }
            final String currentName = jsonParser.getCurrentName();
            if ("url".equals(currentName)) {
                url = jsonParser.getValueAsString();
            }
        }

        if (url == null) {
            throw new IOException("Json transport connector did not contain an URL.");
        }
        return new TransportConnector(url);
    }
}
