package org.ds2os.vsl.json.mapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ds2os.vsl.core.VslKAInfo;
import org.ds2os.vsl.core.VslTransportConnector;
import org.ds2os.vsl.core.impl.KAInfo;
import org.ds2os.vsl.json.JsonTypeMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Implementation of {@link JsonTypeMapper} for {@link VslKAInfo}.
 *
 * @author felix
 */
public final class JsonKAInfoMapper implements JsonTypeMapper<VslKAInfo> {

    /**
     * The {@link JsonTypeMapper} for {@link VslTransportConnector}.
     */
    private final JsonTypeMapper<VslTransportConnector> transportConnectorMapper;

    /**
     * Constructor with injected mapper for connectors.
     *
     * @param tcMapper
     *            the {@link JsonTypeMapper} for {@link VslTransportConnector}.
     */
    public JsonKAInfoMapper(final JsonTypeMapper<VslTransportConnector> tcMapper) {
        this.transportConnectorMapper = tcMapper;
    }

    @Override
    public void writeValue(final VslKAInfo value, final JsonGenerator jsonGenerator)
            throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("agentId", value.getAgentId());
        jsonGenerator.writeStringField("korHash", value.getKorHash());
        jsonGenerator.writeArrayFieldStart("transports");
        for (final VslTransportConnector transport : value.getTransports()) {
            transportConnectorMapper.writeValue(transport, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }

    @Override
    public VslKAInfo readValue(final JsonParser jsonParser) throws IOException {
        if (!JsonToken.START_OBJECT.equals(jsonParser.getCurrentToken())) {
            throw new IOException("No Json object start token found.");
        }

        String agentId = null;
        String korHash = null;
        final List<VslTransportConnector> transports = new ArrayList<VslTransportConnector>();

        while (jsonParser.nextValue() != null) {
            if (JsonToken.END_OBJECT.equals(jsonParser.getCurrentToken())) {
                break;
            }
            final String currentName = jsonParser.getCurrentName();
            if ("agentId".equals(currentName)) {
                agentId = jsonParser.getValueAsString();
            } else if ("korHash".equals(currentName)) {
                korHash = jsonParser.getValueAsString();
            } else if ("transports".equals(currentName)) {
                if (!JsonToken.START_ARRAY.equals(jsonParser.getCurrentToken())) {
                    throw new IOException("Malformed transport structure in Json object.");
                }
                while (jsonParser.nextToken() != null) {
                    if (JsonToken.END_ARRAY.equals(jsonParser.getCurrentToken())) {
                        break;
                    }
                    transports.add(transportConnectorMapper.readValue(jsonParser));
                }
            }
        }

        if (agentId == null || korHash == null) {
            throw new IOException("Json KA info did not contain mandatory fields.");
        }
        return new KAInfo(agentId, transports, korHash);
    }
}
