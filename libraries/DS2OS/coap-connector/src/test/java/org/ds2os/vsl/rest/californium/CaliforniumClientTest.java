package org.ds2os.vsl.rest.californium;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.ds2os.vsl.core.VslMapperFactory;
import org.ds2os.vsl.core.VslParametrizedConnector;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.core.utils.StaticConfig;
import org.ds2os.vsl.core.utils.VslAddressParameters.NodeInformationScope;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.mapper.DatabindMapperFactory;
import org.ds2os.vsl.netutils.SSLUtils;
import org.ds2os.vsl.rest.RestTransportContext;
import org.ds2os.vsl.rest.client.RestConnector;
import org.junit.Ignore;
import org.junit.Test;

public class CaliforniumClientTest {
    final CaliforniumClient unitUnderTest;
    final VslParametrizedConnector testConnector;

    public CaliforniumClientTest() throws IOException, GeneralSecurityException, VslException {
        System.setProperty("tls.ephemeralDHKeySize", "2048");

        final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();
        final VslMapperFactory mapperFactory = new DatabindMapperFactory(nodeFactory);
        final RestTransportContext context = new RestTransportContext(
                StaticConfig.DEFAULT_REST_CONFIG, SSLUtils.loadKeyStore("system.jks", "K3yst0r3"),
                "K3yst0r3", mapperFactory);

        unitUnderTest = new CaliforniumClient(context);
        testConnector = new RestConnector(unitUnderTest, "coaps://localhost:8081/", context,
                nodeFactory);
    }

    @Test
    @Ignore("Needs running KA")
    public final void testSimpleGet() throws VslException {
        final VslNode node = testConnector.get("/agent1/benchmark/simple");
        assertThat(node.getValue(), is(equalTo("1234")));
    }

    // Not implemented yet
    @Test
    @Ignore("Needs running KA")
    public final void testGet() throws VslException {
        final VslNode node = testConnector.get("/KA1",
                new AddressParameters().withNodeInformationScope(NodeInformationScope.COMPLETE));
        assertThat(node.getTypes(), contains("/treeRoot"));
    }

    // TODO: match for NodeNotFoundException once the factory is ready
    @Test(expected = VslException.class)
    @Ignore("Needs running KA")
    public final void testSet() throws VslException {
        testConnector.set("/nonexistentNode",
                testConnector.getNodeFactory().createImmutableLeaf("test"));
    }

    // Not implemented yet
    @Test
    @Ignore("Needs running KA")
    public final void testSubscribe() throws VslException {
        testConnector.subscribe("/", null);
    }
}
