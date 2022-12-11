package org.ds2os.vsl.json.mapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ds2os.vsl.core.VslAlivePing;
import org.ds2os.vsl.core.VslTransportConnector;
import org.ds2os.vsl.core.impl.AlivePing;
import org.ds2os.vsl.json.JsonTypeMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Implementation of {@link JsonTypeMapper} for {@link VslAlivePing}.
 *
 * @author felix
 */
public final class JsonAlivePingMapper implements JsonTypeMapper<VslAlivePing> {

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
    public JsonAlivePingMapper(final JsonTypeMapper<VslTransportConnector> tcMapper) {
        this.transportConnectorMapper = tcMapper;
    }

    @Override
    public void writeValue(final VslAlivePing value, final JsonGenerator jsonGenerator)
            throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("agentId", value.getAgentId());
        jsonGenerator.writeNumberField("numKAs", value.getNumKAs());
        jsonGenerator.writeStringField("caPub", value.getCaPub());
        jsonGenerator.writeStringField("groupID", value.getGroupID());
        jsonGenerator.writeStringField("korHash", value.getKorHash());
        jsonGenerator.writeArrayFieldStart("transports");
        for (final VslTransportConnector transport : value.getTransports()) {
            transportConnectorMapper.writeValue(transport, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }

    @Override
    public VslAlivePing readValue(final JsonParser jsonParser) throws IOException {
        if (!JsonToken.START_OBJECT.equals(jsonParser.nextToken())) {
            throw new IOException("No Json object start token found.");
        }

        String agentId = null;
        int numKAs = 0;
        String caPub = null;
        String groupID = null;
        String korHash = null;
        final List<VslTransportConnector> transports = new ArrayList<VslTransportConnector>();

        while (jsonParser.nextValue() != null) {
            if (JsonToken.END_OBJECT.equals(jsonParser.getCurrentToken())) {
                break;
            }
            final String currentName = jsonParser.getCurrentName();
            if ("agentId".equals(currentName)) {
                agentId = jsonParser.getValueAsString();
            } else if ("numKAs".equals(currentName)) {
                numKAs = jsonParser.getValueAsInt();
            } else if ("caPub".equals(currentName)) {
                caPub = jsonParser.getValueAsString();
            } else if ("groupID".equals(currentName)) {
                groupID = jsonParser.getValueAsString();
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

        if (agentId == null || numKAs <= 0 || caPub == null || groupID == null || korHash == null) {
            throw new IOException("Json alive ping did not contain mandatory fields.");
        }
        return new AlivePing(agentId, numKAs, caPub, transports, groupID, korHash);
    }
}
