package org.ds2os.vsl.mapper.xml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.ds2os.vsl.core.VslMapper;
import org.ds2os.vsl.core.VslMimeTypes;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeData;
import org.ds2os.vsl.core.node.VslNodeImpl;
import org.ds2os.vsl.mapper.keyvaluemap.KeyValueMapModule;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;

/**
 * Content mapper with XML format using the Jackson databind API.
 *
 * @author felix
 */
public final class XmlMapper implements VslMapper {

    /**
     * The used object mapper.
     */
    private final ObjectMapper mapper;

    /**
     * Constructor with injected module for VSL node deserialization.
     *
     * @param nodeDeserializer
     *            the VSL node deserializer module to use.
     */
    public XmlMapper(final Module nodeDeserializer) {
        final JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(true);

        mapper = new com.fasterxml.jackson.dataformat.xml.XmlMapper(xmlModule);
        mapper.registerModule(nodeDeserializer);
        mapper.registerModule(new KeyValueMapModule());
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // add the VslNodeXmlMixin
        mapper.addMixIn(VslNode.class, VslNodeXmlMixin.class);
        mapper.addMixIn(VslNodeData.class, VslNodeXmlMixin.class);
        mapper.addMixIn(VslNodeImpl.class, VslNodeXmlMixin.class);
    }

    @Override
    public String getContentType() {
        return VslMimeTypes.XML;
    }

    @Override
    public String getContentEncoding() {
        return "UTF-8";
    }

    @Override
    public <T> void writeValue(final OutputStream output, final T value) throws IOException {
        final Writer writer = new BufferedWriter(
                new OutputStreamWriter(output, getContentEncoding()));
        try {
            mapper.writeValue(writer, value);
        } finally {
            writer.close();
        }
    }

    @Override
    public <T> T readValue(final InputStream input, final Class<T> valueType) throws IOException {
        final Reader reader = new BufferedReader(
                new InputStreamReader(input, getContentEncoding()));
        try {
            return mapper.readValue(reader, valueType);
        } finally {
            reader.close();
        }
    }
}
