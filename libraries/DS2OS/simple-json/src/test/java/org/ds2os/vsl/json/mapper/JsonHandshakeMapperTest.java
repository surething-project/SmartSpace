package org.ds2os.vsl.json.mapper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;

import org.ds2os.vsl.core.VslHandshakeData;
import org.ds2os.vsl.core.VslKAInfo;
import org.ds2os.vsl.core.VslTransportConnector;
import org.ds2os.vsl.core.impl.HandshakeData;
import org.ds2os.vsl.core.impl.KAInfo;
import org.ds2os.vsl.core.impl.TransportConnector;
import org.ds2os.vsl.json.SimpleJsonMapper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for {@link JsonHandshakeMapper}.
 *
 * @author felix
 */
public class JsonHandshakeMapperTest {

    /**
     * SLF4J logger for Json output to DEBUG level.
     */
    private static final Logger LOG = LoggerFactory.getLogger(JsonHandshakeMapperTest.class);

    /**
     * Example Json for an alive ping message.
     */
    private static final String EXAMPLE_HANDSHAKE_DATA = "{\"newGroupKey\":\"key\","
            + "\"newTLSString\":\"blah\","
            + "\"kaInfo\":[{\"agentId\":\"agent1\",\"korHash\":\"aregh\",\"transports\""
            + ":[{\"url\":\"https://10.0.0.1:1234/\"}]}]}";

    /**
     * The unit under test.
     */
    private final SimpleJsonMapper unitUnderTest = new SimpleJsonMapper();

    /**
     * Prepare the unit under test (only wiring).
     */
    public JsonHandshakeMapperTest() {
        unitUnderTest.addMapper(VslHandshakeData.class,
                new JsonHandshakeMapper(new JsonKAInfoMapper(new JsonTransportConnectorMapper())));
    }

    /**
     * Test writing of handshake data.
     *
     * @throws IOException
     *             should not happen.
     */
    @Test
    public final void testWriteHandshakeData() throws IOException {
        final StringWriter output = new StringWriter();
        try {
            final VslHandshakeData handshakeData = new HandshakeData(
                    Collections.singleton((VslKAInfo) new KAInfo("agent1",
                            Collections.singleton((VslTransportConnector) new TransportConnector(
                                    "https://192.168.0.1:8080/")),
                            "sdht")),
                    "key", "blah");
            unitUnderTest.writeValue(output, handshakeData);
            final String json = output.getBuffer().toString();
            LOG.debug("Handshake data Json: {}", json);
            assertThat(json, containsString("\"agentId\":\"agent1\""));
            assertThat(json, containsString("\"transports\":["));
            assertThat(json, containsString("{\"url\":\"https://192.168.0.1:8080/\""));
            assertThat(json, containsString("\"newGroupKey\":\"key\""));
            assertThat(json, containsString("\"newTLSString\":\"blah\""));

        } finally {
            output.close();
        }
    }

    /**
     * Test reading of handshake data.
     *
     * @throws IOException
     *             should not happen.
     */
    @Test
    public final void testReadHandshakeData() throws IOException {
        final Reader input = new StringReader(EXAMPLE_HANDSHAKE_DATA);
        try {
            final VslHandshakeData handshakeData = unitUnderTest.readValue(input,
                    VslHandshakeData.class);
            final VslKAInfo kaInfo = handshakeData.getKaInfo().iterator().next();
            assertThat(handshakeData.getNewGroupKey(), is("key"));
            assertThat(handshakeData.getNewTLSString(), is("blah"));
            assertThat(kaInfo.getAgentId(), is(equalTo("agent1")));
            assertThat(kaInfo.getTransports().size(), is(equalTo(1)));
            assertThat(kaInfo.getKorHash(), is(equalTo("aregh")));
            final VslTransportConnector[] connectors = kaInfo.getTransports()
                    .toArray(new VslTransportConnector[0]);
            assertThat(connectors[0].getURL(), is(equalTo("https://10.0.0.1:1234/")));
        } finally {
            input.close();
        }
    }
}
