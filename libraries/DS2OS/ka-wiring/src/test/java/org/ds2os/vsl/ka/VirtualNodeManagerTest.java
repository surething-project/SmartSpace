package org.ds2os.vsl.ka;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslVirtualNodeHandler;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.utils.VslAddressParameters;
import org.ds2os.vsl.exception.NoVirtualNodeException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author liebald
 *
 */
public class VirtualNodeManagerTest {

    /**
     * The {@link VirtualNodeManager} used for the tests.
     */
    private VirtualNodeManager vManager;

    /**
     * The address of the service which registered the virtualNode.
     */
    private String service;

    /**
     * Rule for Exception testing. By default no Exception is expected.
     */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * @throws Exception
     *             shouldn't happen.
     */
    @Before
    public final void setUp() throws Exception {
        vManager = new VirtualNodeManager();
        service = "/KA/service";
    }

    /**
     * Test method for {@link VirtualNodeManager#registerVirtualNode(String, VslVirtualNodeHandler)}
     * .
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testRegisterVirtualNode() throws Exception {
        final VslVirtualNodeHandler handlerMock = mock(VslVirtualNodeHandler.class);
        vManager.registerVirtualNode(service + "/vNode", handlerMock);
        assertThat(vManager.isVirtualNode(service + "/vNode"), is(equalTo(true)));
        verify(handlerMock, never()).get(any(String.class), any(VslAddressParameters.class),
                any(VslIdentity.class));
        verify(handlerMock, never()).set(any(String.class), any(VslNode.class),
                any(VslIdentity.class));
    }

    /**
     * Test method for {@link VirtualNodeManager#registerVirtualNode(String, VslVirtualNodeHandler)}
     * .
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testRegisterVirtualNodeAddressToShort() throws Exception {
        final VslVirtualNodeHandler handlerMock = mock(VslVirtualNodeHandler.class);
        vManager.registerVirtualNode("/A", handlerMock);
        assertThat(vManager.isVirtualNode("/A"), is(equalTo(false)));
        verify(handlerMock, never()).get(any(String.class), any(VslAddressParameters.class),
                any(VslIdentity.class));
        verify(handlerMock, never()).set(any(String.class), any(VslNode.class),
                any(VslIdentity.class));
    }

    /**
     * Test method for {@link VirtualNodeManager#registerVirtualNode(String, VslVirtualNodeHandler)}
     * .
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testRegisterVirtualNodeHandlerNull() throws Exception {
        final VslVirtualNodeHandler handlerMock = mock(VslVirtualNodeHandler.class);
        vManager.registerVirtualNode(service + "/vNode", null);
        assertThat(vManager.isVirtualNode(service + "/vNode"), is(equalTo(false)));
        verify(handlerMock, never()).get(any(String.class), any(VslAddressParameters.class),
                any(VslIdentity.class));
        verify(handlerMock, never()).set(any(String.class), any(VslNode.class),
                any(VslIdentity.class));
    }

    /**
     * Test method for {@link VirtualNodeManager#registerVirtualNode(String, VslVirtualNodeHandler)}
     * .
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testRegisterVirtualNodeAlreadyRegistered() throws Exception {
        final VslVirtualNodeHandler handlerMock = mock(VslVirtualNodeHandler.class);
        final VslVirtualNodeHandler handlerMock2 = mock(VslVirtualNodeHandler.class);
        vManager.registerVirtualNode("/address/test", handlerMock);
        vManager.registerVirtualNode("/address/test", handlerMock2);
        assertThat(vManager.getVirtualNodeHandler("/address/test"), is(equalTo(handlerMock2)));

    }

    /**
     * Test method for {@link VirtualNodeManager#unregisterVirtualNode(String)}.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testUnregisterVirtualNode() throws Exception {
        final VslVirtualNodeHandler handlerMock = mock(VslVirtualNodeHandler.class);
        vManager.registerVirtualNode(service + "/vNode", handlerMock);
        vManager.unregisterVirtualNode(service + "/vNode");
        assertThat(vManager.isVirtualNode(service + "/vNode"), is(equalTo(false)));

        verify(handlerMock, never()).get(any(String.class), any(VslAddressParameters.class),
                any(VslIdentity.class));
        verify(handlerMock, never()).set(any(String.class), any(VslNode.class),
                any(VslIdentity.class));
    }

    /**
     * Test method for {@link VirtualNodeManager#unregisterVirtualNode(String)}.
     *
     * @throws NoVirtualNodeException
     *             tested
     */
    @Test
    public final void testUnregisterVirtualNodeException() throws NoVirtualNodeException {
        expectedException.expect(NoVirtualNodeException.class);
        vManager.unregisterVirtualNode(service + "/vNode");
    }

    /**
     * Test method for {@link VirtualNodeManager#unregisterVirtualNode(String)}.
     *
     * @throws NoVirtualNodeException
     *             tested
     */
    @Test
    public final void testUnregisterVirtualNodeExceptionEmpty() throws NoVirtualNodeException {
        expectedException.expect(NoVirtualNodeException.class);
        vManager.unregisterVirtualNode("");
    }

    /**
     * Test method for {@link VirtualNodeManager#getVirtualNodeHandler(String)}.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testGetVirtualNodeHandler() throws Exception {
        final VslVirtualNodeHandler handlerMock = mock(VslVirtualNodeHandler.class);
        vManager.registerVirtualNode(service + "/vNode", handlerMock);

        assertThat(vManager.getVirtualNodeHandler(service + "/vNode"), is(equalTo(handlerMock)));

        verify(handlerMock, never()).get(any(String.class), any(VslAddressParameters.class),
                any(VslIdentity.class));
        verify(handlerMock, never()).set(any(String.class), any(VslNode.class),
                any(VslIdentity.class));
    }

    /**
     * Test method for {@link VirtualNodeManager#getVirtualNodeHandler(String)}.
     *
     * @throws NoVirtualNodeException
     *             tested
     */
    @Test
    public final void testGetVirtualNodeHandlerException() throws NoVirtualNodeException {
        expectedException.expect(NoVirtualNodeException.class);
        vManager.getVirtualNodeHandler(service + "/vNode");
    }

    /**
     * Test method for {@link VirtualNodeManager#getVirtualNodeHandler(String)}.
     *
     * @throws NoVirtualNodeException
     *             tested
     */
    @Test
    public final void testGetVirtualNodeHandlerExceptionEmpty() throws NoVirtualNodeException {
        expectedException.expect(NoVirtualNodeException.class);
        vManager.getVirtualNodeHandler("");
    }

    /**
     * Test method for {@link VirtualNodeManager#isVirtualNode(String)}.
     */
    @Test
    public final void testIsVirtualNodeFalse() {
        assertThat(vManager.isVirtualNode(service + "/vNode"), is(equalTo(false)));
        assertThat(vManager.isVirtualNode(""), is(equalTo(false)));
        assertThat(vManager.isVirtualNode(null), is(equalTo(false)));
    }

    /**
     * Test method for {@link VirtualNodeManager#getFirstVirtualParent(String)}.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testGetFirstVirtualParent() throws Exception {
        final VslVirtualNodeHandler handlerMock = mock(VslVirtualNodeHandler.class);
        vManager.registerVirtualNode(service + "/vNode", handlerMock);

        assertThat(vManager.getFirstVirtualParent(service + "/vNode/1/2/3"),
                is(equalTo(service + "/vNode")));

        verify(handlerMock, never()).get(any(String.class), any(VslAddressParameters.class),
                any(VslIdentity.class));
        verify(handlerMock, never()).set(any(String.class), any(VslNode.class),
                any(VslIdentity.class));
    }

    /**
     * Test method for {@link VirtualNodeManager#getFirstVirtualParent(String)}.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testGetFirstVirtualParentNoVirtualParent() throws Exception {
        final VslVirtualNodeHandler handlerMock = mock(VslVirtualNodeHandler.class);
        vManager.registerVirtualNode(service + "/vNode", handlerMock);

        assertThat(vManager.getFirstVirtualParent(service + "/vNode"), is(nullValue()));
        assertThat(vManager.getFirstVirtualParent(""), is(nullValue()));

        verify(handlerMock, never()).get(any(String.class), any(VslAddressParameters.class),
                any(VslIdentity.class));
        verify(handlerMock, never()).set(any(String.class), any(VslNode.class),
                any(VslIdentity.class));
    }

    /**
     * Test method for {@link VirtualNodeManager#unregisterAllVirtualNodes(String)}.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testUnregisterAllVirtualNodes() throws Exception {
        final VslVirtualNodeHandler handlerMock1 = mock(VslVirtualNodeHandler.class);
        final VslVirtualNodeHandler handlerMock2 = mock(VslVirtualNodeHandler.class);
        final VslVirtualNodeHandler handlerMock3 = mock(VslVirtualNodeHandler.class);
        final VslVirtualNodeHandler handlerMock4 = mock(VslVirtualNodeHandler.class);

        vManager.registerVirtualNode(service + "/subnode", handlerMock1);
        vManager.registerVirtualNode(service + "/subnode/vNode1", handlerMock2);
        vManager.registerVirtualNode(service + "/subnode/vNode2", handlerMock3);
        vManager.registerVirtualNode(service + "/vNode3", handlerMock4);

        vManager.unregisterAllVirtualNodes(service + "/subnode");
        assertThat(vManager.isVirtualNode(service + "/subnode"), is(equalTo(false)));
        assertThat(vManager.isVirtualNode(service + "/subnode/vNode1"), is(equalTo(false)));
        assertThat(vManager.isVirtualNode(service + "/subnode/vNode2"), is(equalTo(false)));
        assertThat(vManager.isVirtualNode(service + "/vNode3"), is(equalTo(true)));

    }

}
