package org.ds2os.vsl.rest.jetty.client;

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

public class JettyClientTest {
    final JettyClient unitUnderTest;
    final VslParametrizedConnector testConnector;

    public JettyClientTest() throws IOException, GeneralSecurityException {
        final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();
        final VslMapperFactory mapperFactory = new DatabindMapperFactory(nodeFactory);
        final RestTransportContext context = new RestTransportContext(
                StaticConfig.DEFAULT_REST_CONFIG, SSLUtils.loadKeyStore("system.jks", "K3yst0r3"),
                "K3yst0r3", mapperFactory);

        unitUnderTest = new JettyClient(context);
        unitUnderTest.start();
        testConnector = new RestConnector(unitUnderTest, "https://localhost:8080/", context,
                nodeFactory);
    }

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

    @Test
    @Ignore("Needs running KA")
    public final void testSubscribe() throws VslException {
        testConnector.subscribe("/", null);
    }
}
