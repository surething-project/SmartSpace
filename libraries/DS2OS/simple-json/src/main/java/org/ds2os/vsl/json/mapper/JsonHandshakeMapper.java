package org.ds2os.vsl.json.mapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ds2os.vsl.core.VslHandshakeData;
import org.ds2os.vsl.core.VslKAInfo;
import org.ds2os.vsl.core.impl.HandshakeData;
import org.ds2os.vsl.json.JsonTypeMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Implementation of {@link JsonTypeMapper} for {@link VslHandshakeData}.
 *
 * @author felix
 * @author liebald
 */
public final class JsonHandshakeMapper implements JsonTypeMapper<VslHandshakeData> {

    /**
     * The {@link JsonTypeMapper} for {@link VslKAInfo}.
     */
    private final JsonTypeMapper<VslKAInfo> kaInfoMapper;

    /**
     * Constructor with injected mapper for connectors.
     *
     * @param tcMapper
     *            the {@link JsonTypeMapper} for {@link VslKAInfo}.
     */
    public JsonHandshakeMapper(final JsonTypeMapper<VslKAInfo> tcMapper) {
        this.kaInfoMapper = tcMapper;
    }

    @Override
    public void writeValue(final VslHandshakeData value, final JsonGenerator jsonGenerator)
            throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("newGroupKey", value.getNewGroupKey());
        jsonGenerator.writeStringField("newTLSString", value.getNewTLSString());
        jsonGenerator.writeArrayFieldStart("kaInfo");
        for (final VslKAInfo kaInfo : value.getKaInfo()) {
            kaInfoMapper.writeValue(kaInfo, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }

    @Override
    public VslHandshakeData readValue(final JsonParser jsonParser) throws IOException {
        if (!JsonToken.START_OBJECT.equals(jsonParser.nextToken())) {
            throw new IOException("No Json object start token found.");
        }

        String newGroupKey = null;
        final List<VslKAInfo> kaInfos = new ArrayList<VslKAInfo>();
        String newTLSString = null;

        while (jsonParser.nextValue() != null) {
            if (JsonToken.END_OBJECT.equals(jsonParser.getCurrentToken())) {
                break;
            }
            final String currentName = jsonParser.getCurrentName();
            if ("newGroupKey".equals(currentName)) {
                newGroupKey = jsonParser.getValueAsString();
            } else if ("newTLSString".equals(currentName)) {
                newTLSString = jsonParser.getValueAsString();
            } else if ("kaInfo".equals(currentName)) {
                if (!JsonToken.START_ARRAY.equals(jsonParser.getCurrentToken())) {
                    throw new IOException("Malformed transport structure in Json object.");
                }
                while (jsonParser.nextToken() != null) {
                    if (JsonToken.END_ARRAY.equals(jsonParser.getCurrentToken())) {
                        break;
                    }
                    kaInfos.add(kaInfoMapper.readValue(jsonParser));
                }
            }
        }

        if (kaInfos.isEmpty()) {
            throw new IOException("Json handshake data did not contain mandatory fields.");
        }
        return new HandshakeData(kaInfos, newGroupKey, newTLSString);
    }
}
