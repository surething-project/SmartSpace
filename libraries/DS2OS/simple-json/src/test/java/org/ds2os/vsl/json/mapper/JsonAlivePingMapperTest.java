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

import org.ds2os.vsl.core.VslAlivePing;
import org.ds2os.vsl.core.VslTransportConnector;
import org.ds2os.vsl.core.impl.AlivePing;
import org.ds2os.vsl.core.impl.TransportConnector;
import org.ds2os.vsl.json.SimpleJsonMapper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for mapping alive pings.
 *
 * @author felix
 */
public class JsonAlivePingMapperTest {

    /**
     * SLF4J logger for Json output to DEBUG level.
     */
    private static final Logger LOG = LoggerFactory.getLogger(JsonAlivePingMapperTest.class);

    /**
     * Example Json for an alive ping message.
     */
    private static final String EXAMPLE_ALIVE_PING = "{\"agentId\":\"agent1\",\"numKAs\":2,"
            + "\"caPub\":\"...\",\"groupID\":\"sdht\",\"korHash\":\"aregh\",\"transports\""
            + ":[{\"url\":\"https://10.0.0.1:1234/\"},{\"url\":\"broadcast://192.168.255.255/\"}]}";

    /**
     * The unit under test.
     */
    private final SimpleJsonMapper unitUnderTest = new SimpleJsonMapper();;

    /**
     * Prepare the unit under test (only wiring).
     */
    public JsonAlivePingMapperTest() {
        unitUnderTest.addMapper(VslAlivePing.class,
                new JsonAlivePingMapper(new JsonTransportConnectorMapper()));
    }

    /**
     * Test writing an alive ping.
     *
     * @throws IOException
     *             should not happen.
     */
    @Test
    public final void testWriteAlivePing() throws IOException {
        final StringWriter output = new StringWriter();
        try {
            final VslAlivePing alivePing = new AlivePing("agent1", 2, "...", Collections.singleton(
                    (VslTransportConnector) new TransportConnector("https://192.168.0.1:8080/")),
                    "sdht", "aregh");
            unitUnderTest.writeValue(output, alivePing);
            final String json = output.getBuffer().toString();
            LOG.debug("Alive ping Json: {}", json);
            assertThat(json, containsString("\"agentId\":\"agent1\""));
            assertThat(json, containsString("\"numKAs\":2"));
            assertThat(json, containsString("\"caPub\":\"...\""));
            assertThat(json, containsString("\"transports\":["));
            assertThat(json, containsString("{\"url\":\"https://192.168.0.1:8080/\""));
            assertThat(json, containsString("\"groupID\":\"sdht\""));
            assertThat(json, containsString("\"korHash\":\"aregh\""));
        } finally {
            output.close();
        }
    }

    /**
     * Test reading an alive ping.
     *
     * @throws IOException
     *             should not happen.
     */
    @Test
    public final void testReadAlivePing() throws IOException {
        final Reader input = new StringReader(EXAMPLE_ALIVE_PING);
        try {
            final VslAlivePing alivePing = unitUnderTest.readValue(input, VslAlivePing.class);
            assertThat(alivePing.getAgentId(), is(equalTo("agent1")));
            assertThat(alivePing.getNumKAs(), is(equalTo(2)));
            assertThat(alivePing.getCaPub(), is(equalTo("...")));
            assertThat(alivePing.getTransports().size(), is(equalTo(2)));
            assertThat(alivePing.getGroupID(), is(equalTo("sdht")));
            assertThat(alivePing.getKorHash(), is(equalTo("aregh")));
            final VslTransportConnector[] connectors = alivePing.getTransports()
                    .toArray(new VslTransportConnector[0]);
            assertThat(connectors[0].getURL(), is(equalTo("https://10.0.0.1:1234/")));
            assertThat(connectors[1].getURL(), is(equalTo("broadcast://192.168.255.255/")));
        } finally {
            input.close();
        }
    }
}
