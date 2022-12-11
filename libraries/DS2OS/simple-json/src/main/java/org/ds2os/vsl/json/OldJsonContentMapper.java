package org.ds2os.vsl.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Content mapper with Json format. Encodes and decodes Vsl nodes and operations in Json.
 *
 * @author felix
 */
public final class OldJsonContentMapper {

    /**
     * Factory used to create Vsl nodes.
     */
    private final VslNodeFactory nodeFactory;

    /**
     * Factory used to create Json generators and parsers.
     */
    private final JsonFactory factory;

    /**
     * Simple constructor.
     *
     * @param nodeFactory
     *            the Vsl node factory.
     */
    public OldJsonContentMapper(final VslNodeFactory nodeFactory) {
        this.nodeFactory = nodeFactory;
        factory = new JsonFactory();
        factory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        factory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
    }

    /**
     * Constructor with injected node and {@link JsonFactory}.
     *
     * @param nodeFactory
     *            the Vsl node factory.
     * @param factory
     *            the factory for Json generators and parsers.
     */
    public OldJsonContentMapper(final VslNodeFactory nodeFactory, final JsonFactory factory) {
        this.nodeFactory = nodeFactory;
        this.factory = factory;
    }

    /**
     * Write a {@link VslNode} to the output writer with the type and encoding of this mapper.
     *
     * @param node
     *            the Vsl node to write.
     * @param writer
     *            the writer used to write the output to.
     * @throws IOException
     *             If an I/O error occurs.
     */
    // TODO: handle special VslNode types
    public void writeNode(final VslNode node, final Writer writer) throws IOException {
        final JsonGenerator generator = factory.createGenerator(writer);
        try {
            writeNode(node, generator);
        } finally {
            generator.close();
        }
    }

    /**
     * Recursive method writing the specified node and recursing for its children.
     *
     * @param node
     *            the node to write.
     * @param generator
     *            the Json generator where the object is written.
     * @throws IOException
     *             If an I/O error occurs.
     */
    protected void writeNode(final VslNode node, final JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("value", node.getValue());
        if (node.getTypes() != null) {
            generator.writeStringField("type", node.getTypes().toString().substring(1,
                    node.getTypes().toString().length() - 1));
        }
        if (node.getTimestamp() != null) {
            generator.writeNumberField("timestamp", node.getTimestamp().getTime());
        }
        if (node.getVersion() > -1) {
            generator.writeNumberField("version", node.getVersion());
        }
        final Iterable<Map.Entry<String, VslNode>> directChildren = node.getDirectChildren();
        if (directChildren.iterator().hasNext()) {
            generator.writeObjectFieldStart("children");
            for (final Map.Entry<String, VslNode> child : directChildren) {
                generator.writeFieldName(child.getKey());
                writeNode(child.getValue(), generator);
            }
            generator.writeEndObject();
        }
        generator.writeEndObject();
    }

    /**
     * Read a {@link VslNode} from the reader with the type and encoding of this mapper.
     *
     * @param reader
     *            the reader used to read input from.
     * @return the Vsl node.
     * @throws IOException
     *             If an I/O error occurs.
     */
    public VslNode readNode(final Reader reader) throws IOException {
        final JsonParser parser = factory.createParser(reader);
        try {
            if (parser.nextToken() == null
                    || !JsonToken.START_OBJECT.equals(parser.getCurrentToken())) {
                throw new IOException("No Json object start token found.");
            }
            return readNode(parser);
        } finally {
            parser.close();
        }
    }

    /**
     * Recursive method reading a node and recursing to its children.
     *
     * @param parser
     *            the Json parser to use.
     * @return the Vsl node.
     * @throws IOException
     *             If an I/O error occurs.
     */
    // TODO: that's NOT efficient allocation and NOT clean code!
    protected VslNode readNode(final JsonParser parser) throws IOException {
        List<String> type = null;
        String value = null;
        Timestamp timestamp = null;
        Long version = -1L;
        Map<String, VslNode> childMap = null;
        while (parser.nextValue() != null) {
            if (JsonToken.END_OBJECT.equals(parser.getCurrentToken())) {
                break;
            }
            final String currentName = parser.getCurrentName();
            if ("type".equals(currentName)) {
                type = new ArrayList<String>();
                for (final String t : parser.getValueAsString().split("[,;]")) {
                    type.add(t.trim());
                }
            } else if ("value".equals(currentName)) {
                value = parser.getValueAsString();
            } else if ("version".equals(currentName)) {
                version = Long.valueOf(parser.getValueAsString());
            } else if ("timestamp".equals(currentName)) {
                timestamp = new Timestamp(Long.parseLong(parser.getValueAsString()));
            } else if ("children".equals(currentName)) {
                childMap = new HashMap<String, VslNode>();
                while (parser.nextToken() != null) {
                    if (JsonToken.END_OBJECT.equals(parser.getCurrentToken())) {
                        break;
                    }
                    if (!JsonToken.FIELD_NAME.equals(parser.getCurrentToken())
                            || parser.nextToken() == null) {
                        throw new IOException("Malformed children structure in Json object!");
                    }
                    final String address = parser.getCurrentName();
                    if (childMap.put(address, readNode(parser)) != null) {
                        throw new IOException("Child address with two children detected!");
                    }
                }
            }
        }
        final VslNode result;
        if (childMap == null) {
            result = nodeFactory.createImmutableLeaf(type, value, timestamp, version, null, null);
        } else {
            childMap.put("",
                    nodeFactory.createImmutableLeaf(type, value, timestamp, version, null, null));
            result = nodeFactory.createImmutableNode(childMap.entrySet());
        }
        return result;
    }

    /**
     * Write an error message with the content type.
     *
     * @param status
     *            the HTTP error status code.
     * @param message
     *            the error message.
     * @param writer
     *            the writer used to write the output to.
     * @throws IOException
     *             If an I/O error occurs.
     */
    // TODO: Standard format for error messages...
    public void writeErrorMessage(final int status, final String message, final Writer writer)
            throws IOException {
        final JsonGenerator generator = factory.createGenerator(writer);
        try {
            generator.writeStartObject();
            generator.writeNumberField("errorCode", status);
            generator.writeStringField("message", message);
            generator.writeEndObject();
        } finally {
            generator.close();
        }
    }
}
