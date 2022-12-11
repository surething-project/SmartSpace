package org.ds2os.vsl.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ds2os.vsl.core.VslKORUpdate;
import org.ds2os.vsl.core.impl.KORUpdate;
import org.ds2os.vsl.core.node.VslStructureNode;
import org.ds2os.vsl.core.node.VslStructureNodeImpl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * JsonKORUpdateMapper with Json encoding. This class uses the {@link JsonFactory} to map VslNodes.
 *
 * @author Johannes Straßer
 */
// FIXME: convert this into simple mapper style
@Deprecated
public class JsonKORUpdateMapper {

    /**
     * Factory used to create Json generators and parsers.
     */
    private final JsonFactory factory;

    /**
     * Simple constructor.
     */
    public JsonKORUpdateMapper() {
        factory = new JsonFactory();
        factory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        factory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
    }

    /**
     * Constructor with injected {@link JsonFactory}.
     *
     * @param factory
     *            the factory for Json generators and parsers.
     */
    public JsonKORUpdateMapper(final JsonFactory factory) {
        this.factory = factory;
    }

    /**
     * Writes a {@link VslKORUpdate} to the given writer.
     *
     * @param korUpdate
     *            The {@link VslKORUpdate} to write
     * @param writer
     *            The writer to write to.
     * @throws IOException
     *             thrown if an {@link IOException} occurs.
     */
    public final void writeKORUpdate(final VslKORUpdate korUpdate, final Writer writer)
            throws IOException {
        final JsonGenerator generator = factory.createGenerator(writer);
        try {
            generator.writeStartObject();
            generator.writeStringField("hashFrom", korUpdate.getHashFrom());
            generator.writeStringField("hashTo", korUpdate.getHashTo());
            generator.writeStringField("agentName", korUpdate.getAgentName());
            generator.writeArrayFieldStart("removedNodes");
            for (final String removedNode : korUpdate.getRemovedNodes()) {
                generator.writeStartObject();
                generator.writeStringField("nodeName", removedNode);
                generator.writeEndObject();
            }
            generator.writeEndArray();
            generator.writeArrayFieldStart("addedNodes");
            final Map<String, VslStructureNode> addedNodes = korUpdate.getAddedNodes();
            for (final Entry<String, VslStructureNode> entry : addedNodes.entrySet()) {
                writeVslStructureNode(generator, entry);
            }
            generator.writeEndArray();
            generator.writeEndObject();
        } finally {
            generator.close();
        }

    }

    /**
     * Write a VslStructureNode and its child VslNodes using the given generator.
     *
     * @param generator
     *            The generator used for writing
     * @param entry
     *            The VslStructureNode and its name
     * @throws IOException
     *             Thrown if there is an error using the generator
     */
    private void writeVslStructureNode(final JsonGenerator generator,
            final Entry<String, VslStructureNode> entry) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("nodeName", entry.getKey());
        generator.writeArrayFieldStart("newReaderIDs");
        for (final String newReaderID : entry.getValue().getReaderIds()) {
            generator.writeStartObject();
            generator.writeStringField("newReaderID", newReaderID);
            generator.writeEndObject();
        }
        generator.writeEndArray();
        generator.writeArrayFieldStart("newWriterIDs");
        for (final String newWriterID : entry.getValue().getWriterIds()) {
            generator.writeStartObject();
            generator.writeStringField("newWriterID", newWriterID);
            generator.writeEndObject();
        }
        generator.writeEndArray();
        generator.writeStringField("newRestriction", entry.getValue().getRestrictions());
        generator.writeStringField("newCacheParameters", entry.getValue().getCacheParameters());
        generator.writeArrayFieldStart("newTypes");
        for (final String newType : entry.getValue().getTypes()) {
            generator.writeStartObject();
            generator.writeStringField("newType", newType);
            generator.writeEndObject();
        }
        generator.writeEndArray();
        final Iterable<Map.Entry<String, VslStructureNode>> directChildren = entry.getValue()
                .getDirectChildren();
        if (directChildren.iterator().hasNext()) {
            generator.writeObjectFieldStart("children");
            for (final Map.Entry<String, VslStructureNode> child : directChildren) {
                generator.writeFieldName(child.getKey());
                writeVslStructureNode(generator, child);
                // vslNodeMapper.writeNode(child.getValue(), generator);
            }
            generator.writeEndObject();
        }
        generator.writeEndObject();
    }

    /**
     * Reads a {@link KORUpdate} from the given reader.
     *
     * @param reader
     *            The {@link Reader} to read from.
     * @return The {@link VslKORUpdate} read from the reader
     * @throws IOException
     *             thrown if an {@link IOException} occurs.
     */
    public final VslKORUpdate readKORUpdate(final Reader reader) throws IOException {
        final JsonParser parser = factory.createParser(reader);
        try {
            if (!JsonToken.START_OBJECT.equals(parser.nextToken())) {
                throw new IOException("No Json object start token found.");
            }
            String hashFrom = null;
            String hashTo = null;
            final Map<String, VslStructureNode> addedNodes = new HashMap<String, VslStructureNode>();
            final Set<String> removedNodes = new HashSet<String>();
            String agentName = null;

            while (parser.nextValue() != null) {
                if (JsonToken.END_OBJECT.equals(parser.getCurrentToken())) {
                    break;
                }
                final String currentName = parser.getCurrentName();
                if ("hashFrom".equals(currentName)) {
                    hashFrom = parser.getValueAsString();
                } else if ("hashTo".equals(currentName)) {
                    hashTo = parser.getValueAsString();
                } else if ("agentName".equals(currentName)) {
                    agentName = parser.getValueAsString();
                } else if ("addedNodes".equals(currentName)) {
                    if (!JsonToken.START_ARRAY.equals(parser.getCurrentToken())) {
                        throw new IOException("Malformed transport structure in Json object.");
                    }
                    while (parser.nextToken() != null) {
                        if (JsonToken.END_ARRAY.equals(parser.getCurrentToken())) {
                            break;
                        }
                        readAddedNodes(parser, addedNodes);
                    }
                } else if ("removedNodes".equals(currentName)) {
                    if (!JsonToken.START_ARRAY.equals(parser.getCurrentToken())) {
                        throw new IOException("Malformed transport structure in Json object.");
                    }
                    while (parser.nextToken() != null) {
                        if (JsonToken.END_ARRAY.equals(parser.getCurrentToken())) {
                            break;
                        }
                        removedNodes.add(readStringField(parser, "nodeName"));
                    }
                }
            }
            if (hashFrom == null || hashTo == null || agentName == null) {
                throw new IOException("Json KOR update did not contain mandatory fields.");
            }
            return new KORUpdate(hashFrom, hashTo, addedNodes, removedNodes, agentName);
        } finally {
            parser.close();
        }
    }

    /**
     * Read a VslStructureNode from the Json parser and store it into the given map.
     *
     * @param parser
     *            the {@link JsonParser} to read from.
     * @param addedNodes
     *            Map the store the results
     * @throws IOException
     *             If an I/O exception occurs or the Json is invalid.
     */
    private void readAddedNodes(final JsonParser parser,
            final Map<String, VslStructureNode> addedNodes) throws IOException {
        if (!JsonToken.START_OBJECT.equals(parser.getCurrentToken())) {
            throw new IOException("No Json object start token found.");
        }
        String nodeName = null;
        VslStructureNodeImpl node = null;
        while (parser.nextValue() != null) {
            if (JsonToken.END_OBJECT.equals(parser.getCurrentToken())) {
                break;
            }
            final String currentName = parser.getCurrentName();
            if ("nodeName".equals(currentName)) {
                nodeName = parser.getValueAsString();

            } else {
                node = readAddedNodes(parser);
            }
            if (JsonToken.END_OBJECT.equals(parser.getCurrentToken())) {
                break;
            }
        }
        addedNodes.put(nodeName, node);
    }

    /**
     * Read a VslStructureNode from the Json parser and store it into the given map.
     *
     * @param parser
     *            the {@link JsonParser} to read from.
     *
     * @throws IOException
     *             If an I/O exception occurs or the Json is invalid.
     * @return A {@link VslStructureNodeImpl} containing the added nodes.
     */
    private VslStructureNodeImpl readAddedNodes(final JsonParser parser) throws IOException {

        final List<String> newReaderIDs = new ArrayList<String>();
        final List<String> newWriterIDs = new ArrayList<String>();
        String newRestriction = null;
        String newCacheParameters = null;
        final List<String> newType = new ArrayList<String>();
        final Map<String, VslStructureNodeImpl> children = new HashMap<String, VslStructureNodeImpl>();

        do {
            if (JsonToken.END_OBJECT.equals(parser.getCurrentToken())) {
                break;
            }
            final String currentName = parser.getCurrentName();
            if ("newReaderIDs".equals(currentName)) {
                if (!JsonToken.START_ARRAY.equals(parser.getCurrentToken())) {
                    throw new IOException("Malformed transport structure in Json object.");
                }
                while (parser.nextToken() != null) {
                    if (JsonToken.END_ARRAY.equals(parser.getCurrentToken())) {
                        break;
                    }
                    newReaderIDs.add(readStringField(parser, "newReaderID"));
                }
            } else if ("newWriterIDs".equals(currentName)) {
                if (!JsonToken.START_ARRAY.equals(parser.getCurrentToken())) {
                    throw new IOException("Malformed transport structure in Json object.");
                }
                while (parser.nextToken() != null) {
                    if (JsonToken.END_ARRAY.equals(parser.getCurrentToken())) {
                        break;
                    }
                    newWriterIDs.add(readStringField(parser, "newWriterID"));
                }
            } else if ("newRestriction".equals(currentName)) {
                newRestriction = parser.getValueAsString();
            } else if ("newCacheParameters".equals(currentName)) {
                newCacheParameters = parser.getValueAsString();
            } else if ("newTypes".equals(currentName)) {
                if (!JsonToken.START_ARRAY.equals(parser.getCurrentToken())) {
                    throw new IOException("Malformed transport structure in Json object.");
                }
                while (parser.nextToken() != null) {
                    if (JsonToken.END_ARRAY.equals(parser.getCurrentToken())) {
                        break;
                    }
                    newType.add(readStringField(parser, "newType"));
                }
            } else if ("children".equals(currentName)) {
                while (parser.nextToken() != null) {
                    if (JsonToken.END_OBJECT.equals(parser.getCurrentToken())) {
                        break;
                    }
                    if (!JsonToken.FIELD_NAME.equals(parser.getCurrentToken())
                            || parser.nextToken() == null) {
                        throw new IOException("Malformed children structure in Json object!");
                    }
                    final String address = parser.getCurrentName();
                    if (children.put(address, readAddedNodes(parser)) != null) {
                        throw new IOException("Child address with two children detected!");
                    }

                }
            }
        } while (parser.nextValue() != null);

        final VslStructureNodeImpl result = new VslStructureNodeImpl(newReaderIDs, newWriterIDs,
                newRestriction, newType, newCacheParameters);
        for (final Entry<String, VslStructureNodeImpl> child : children.entrySet()) {
            result.putChild(child.getKey(), child.getValue());
        }
        return result;
    }

    /**
     * Read a field with single String entry from the Json parser.
     *
     * @param parser
     *            the {@link JsonParser} to read from.
     * @param name
     *            the name of the field
     * @return the contained String.
     * @throws IOException
     *             If an I/O exception occurs or the Json is invalid.
     */
    private String readStringField(final JsonParser parser, final String name) throws IOException {
        if (!JsonToken.START_OBJECT.equals(parser.getCurrentToken())) {
            throw new IOException("No Json object start token found.");
        }
        String result = null;
        while (parser.nextValue() != null) {
            if (JsonToken.END_OBJECT.equals(parser.getCurrentToken())) {
                break;
            }
            final String currentName = parser.getCurrentName();
            if (name.equals(currentName)) {
                result = parser.getValueAsString();
            }
        }
        if (result == null) {
            throw new IOException("Json list did not contain the entry " + name + ".");
        }
        return result;
    }

}
