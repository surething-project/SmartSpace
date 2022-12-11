package org.ds2os.vsl.cache;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.ds2os.vsl.core.VslConnector;
import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslKORCacheHandler;
import org.ds2os.vsl.core.VslServiceManifest;
import org.ds2os.vsl.core.config.VslCacheConfig;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.node.VslMutableStructureNode;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.node.VslStructureNodeImpl;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.ds2os.vsl.exception.VslException;
import org.junit.Before;
import org.junit.Test;

/**
 * IntegrationTest for {@link Cache} based on the KOR storage for cached nodes.
 *
 * @author liebald
 */
public class ITCacheTestKOR {

    /**
     * The Cache object that is tested.
     */
    private Cache unitUnderTest;

    /**
     * Mock of the {@link VslKORCacheHandler} for KOR access.
     */
    private VslKORCacheHandler cacheHandlerMock;

    /**
     * Mock of the {@link VslCacheConfig} for accessing the config.
     */
    private VslCacheConfig cacheConfigMock;

    /**
     * Mock of the {@link VslConnector} for accessing the KOR.
     */
    private VslConnector connectorMock;

    /**
     * Setup the tests.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Before
    public final void setUp() throws Exception {
        cacheHandlerMock = mock(VslKORCacheHandler.class);
        cacheConfigMock = mock(VslCacheConfig.class);
        connectorMock = mock(VslConnector.class);
        when(cacheConfigMock.getDefaultTTL()).thenReturn(10);
        when(cacheConfigMock.getCharset()).thenReturn("UTF-8");
        when(cacheConfigMock.isCacheEnabled()).thenReturn(true);
        when(cacheConfigMock.getCacheCapacity()).thenReturn(100);
        when(cacheConfigMock.getReplacementPolicy()).thenReturn("rr");
        when(connectorMock.registerService(any(VslServiceManifest.class)))
                .thenReturn("/agent1/system/cache");
        when(connectorMock.getNodeFactory()).thenReturn(new VslNodeFactoryImpl());

        unitUnderTest = new Cache(connectorMock, cacheHandlerMock, cacheConfigMock);
        unitUnderTest.activate();
    }

    /**
     * Test of the {@link Cache#cacheNode(String, VslNode)} method.
     *
     * @throws NodeNotExistingException
     *             Shouldn't happen;
     */
    @Test
    public final void testCacheNode() throws NodeNotExistingException {
        final List<String> readers = Arrays.asList("ID1", "ID2");
        final List<String> writers = Arrays.asList("ID2", "ID3");
        final String restrictions = "";
        final List<String> types = Arrays.asList("/basic/text");

        final VslMutableStructureNode structure = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        structure.putChild("b",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='30'"));
        structure.putChild("b/c",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='10'"));
        structure.putChild("b/d",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='15'"));
        structure.putChild("e",
                new VslStructureNodeImpl(readers, writers, restrictions, types, ""));
        when(cacheHandlerMock.getStructure("/a")).thenReturn(structure);

        final VslMutableNode toCache = connectorMock.getNodeFactory()
                .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(types,
                        "root", new Date(1234L), 5L, null, Collections.<String, String>emptyMap()));
        toCache.putChild("b",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_b", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));
        toCache.putChild("b/c",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_b_c", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));
        toCache.putChild("b/d",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_b_d", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));
        toCache.putChild("e",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_e", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));

        unitUnderTest.cacheNode("/a", toCache);
        verify(cacheHandlerMock).getStructure("/a");
        verify(cacheHandlerMock).cacheVslNodes(eq("/a"), any(VslNode.class));
    }

    /**
     * Test of the {@link Cache#cacheNode(String, VslNode)} method.
     *
     * @throws NodeNotExistingException
     *             Shouldn't happen;
     */
    @Test
    public final void testCacheNodeInvalid() throws NodeNotExistingException {
        final List<String> readers = Arrays.asList("ID1", "ID2");
        final List<String> writers = Arrays.asList("ID2", "ID3");
        final String restrictions = "";
        final List<String> types = Arrays.asList("/basic/text");

        // TTL of /a/b/c is 0, therefore the node shouldn't be cached.
        final VslMutableStructureNode structure = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        structure.putChild("b",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='30'"));
        structure.putChild("b/c",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='0'"));
        structure.putChild("b/d",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='15'"));
        structure.putChild("e",
                new VslStructureNodeImpl(readers, writers, restrictions, types, ""));
        when(cacheHandlerMock.getStructure("/a")).thenReturn(structure);

        final VslMutableNode toCache = connectorMock.getNodeFactory()
                .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(types,
                        "root", new Date(1234L), 5L, null, Collections.<String, String>emptyMap()));
        toCache.putChild("b",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_b", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));
        toCache.putChild("b/c",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_b_c", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));
        toCache.putChild("b/d",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_b_d", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));
        toCache.putChild("e",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_e", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));

        unitUnderTest.cacheNode("/a", toCache);
        verify(cacheHandlerMock).getStructure("/a");
        verify(cacheHandlerMock, never()).cacheVslNodes("/a", toCache);

    }

    /**
     * Tests if the get operations on cached nodes work correctly.
     *
     * @throws VslException
     *             shouldn't happen.
     */
    @Test
    public final void testGetCachedNode() throws VslException {
        final List<String> readers = Arrays.asList("ID1", "ID2");
        final List<String> writers = Arrays.asList("ID2", "ID3");
        final String restrictions = "";
        final List<String> types = Arrays.asList("/basic/text");
        final VslIdentity id = new ServiceIdentity("clientID", "ID1");

        final VslMutableStructureNode structure = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        structure.putChild("b",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='30'"));
        structure.putChild("b/c",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='10'"));
        structure.putChild("b/d",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='15'"));
        structure.putChild("e",
                new VslStructureNodeImpl(readers, writers, restrictions, types, ""));
        when(cacheHandlerMock.getStructure("/a")).thenReturn(structure);

        final VslMutableNode toCache = connectorMock.getNodeFactory()
                .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(types,
                        "root", new Date(1234L), 5L, null, Collections.<String, String>emptyMap()));
        toCache.putChild("b",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_b", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));
        toCache.putChild("b/c",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_b_c", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));
        toCache.putChild("b/d",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_b_d", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));
        toCache.putChild("e",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_e", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));

        unitUnderTest.cacheNode("/a", toCache);

        when(cacheHandlerMock.get("/a", id)).thenReturn(toCache);

        final VslNode node = unitUnderTest.getCachedNode("/a", id);
        verify(cacheHandlerMock).get("/a", id);
        assertThat(node, is(equalTo(node)));
    }

    /**
     * Test of the {@link Cache#handleNotification(String)} method.
     *
     * @throws VslException
     *             shouldn't happen.
     */
    @Test
    public final void testHandleNotification() throws VslException {
        final List<String> readers = Arrays.asList("ID1", "ID2");
        final List<String> writers = Arrays.asList("ID2", "ID3");
        final String restrictions = "";
        final List<String> types = Arrays.asList("/basic/text");

        final VslIdentity id = new ServiceIdentity("clientID", "ID1");

        final VslMutableStructureNode structure = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        final VslStructureNodeImpl structureB = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        final VslStructureNodeImpl structureBC = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        structureB.putChild("c", structureBC);
        final VslStructureNodeImpl structureBD = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        structureB.putChild("d", structureBD);
        structure.putChild("b", structureB);
        final VslStructureNodeImpl structureE = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        structure.putChild("e", structureE);
        when(cacheHandlerMock.getStructure("/a")).thenReturn(structure);
        when(cacheHandlerMock.getStructure("/a/b")).thenReturn(structureB);
        when(cacheHandlerMock.getStructure("/a/b/c")).thenReturn(structureBC);
        when(cacheHandlerMock.getStructure("/a/b/d")).thenReturn(structureBD);
        when(cacheHandlerMock.getStructure("/a/e")).thenReturn(structureE);
        final VslNode res1 = connectorMock.getNodeFactory().createImmutableLeaf("dummy");
        when(cacheHandlerMock.get("/a/e", id)).thenReturn(res1);

        final VslMutableNode toCache = connectorMock.getNodeFactory()
                .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(types,
                        "root", new Date(1234L), 5L, null, Collections.<String, String>emptyMap()));
        toCache.putChild("b",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_b", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));
        toCache.putChild("b/c",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_b_c", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));
        toCache.putChild("b/d",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_b_d", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));
        toCache.putChild("e",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_e", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));

        unitUnderTest.cacheNode("/a", toCache);
        unitUnderTest.handleNotification("/a/b");
        verify(cacheHandlerMock).getStructure("/a");

        // full node /a can't be served, since a/b and the children a/b/c and a/b/d were removed due
        // to the notification.
        assertNull(unitUnderTest.getCachedNode("/a", id));
        assertNull(unitUnderTest.getCachedNode("/a/b", id));
        assertNull(unitUnderTest.getCachedNode("/a/b/c", id));
        assertNull(unitUnderTest.getCachedNode("/a/b/d", id));

        assertThat(unitUnderTest.getCachedNode("/a/e", id).getValue(), is(equalTo("dummy")));
    }

    /**
     * Test of the {@link Cache#handleSet(String, VslNode)} method.
     *
     * @throws VslException
     *             shouldn't happen.
     */
    @Test
    public final void testHandleSet() throws VslException {
        final List<String> readers = Arrays.asList("ID1", "ID2");
        final List<String> writers = Arrays.asList("ID2", "ID3");
        final String restrictions = "";
        final List<String> types = Arrays.asList("/basic/text");
        final VslIdentity id = new ServiceIdentity("clientID", "ID1");

        final VslMutableStructureNode structure = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        final VslStructureNodeImpl structureB = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        final VslStructureNodeImpl structureBC = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        structureB.putChild("c", structureBC);
        final VslStructureNodeImpl structureBD = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        structureB.putChild("d", structureBD);
        structure.putChild("b", structureB);
        final VslStructureNodeImpl structureE = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        structure.putChild("e", structureE);
        when(cacheHandlerMock.getStructure("/a")).thenReturn(structure);
        when(cacheHandlerMock.getStructure("/a/b")).thenReturn(structureB);
        when(cacheHandlerMock.getStructure("/a/b/c")).thenReturn(structureBC);
        when(cacheHandlerMock.getStructure("/a/b/d")).thenReturn(structureBD);
        when(cacheHandlerMock.getStructure("/a/e")).thenReturn(structureE);
        final VslNode res1 = connectorMock.getNodeFactory().createImmutableLeaf("dummy1");
        final VslNode res2 = connectorMock.getNodeFactory().createImmutableLeaf("dummy2");

        when(cacheHandlerMock.get("/a/b/c", id)).thenReturn(res1);
        when(cacheHandlerMock.get("/a/e", id)).thenReturn(res2);

        final VslMutableNode toCache = connectorMock.getNodeFactory()
                .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(types,
                        "root", new Date(1234L), 5L, null, Collections.<String, String>emptyMap()));
        toCache.putChild("b",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_b", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));
        toCache.putChild("b/c",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_b_c", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));
        toCache.putChild("b/d",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_b_d", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));
        toCache.putChild("e",
                connectorMock.getNodeFactory()
                        .createMutableClone(connectorMock.getNodeFactory().createImmutableLeaf(
                                types, "child_e", new Date(1234L), 5L, null,
                                Collections.<String, String>emptyMap())));

        final VslMutableNode set = connectorMock.getNodeFactory().createMutableNode("updateB");
        set.putChild("c", connectorMock.getNodeFactory().createMutableNode());
        set.putChild("d", connectorMock.getNodeFactory().createMutableNode("updateBD"));

        unitUnderTest.cacheNode("/a", toCache);
        unitUnderTest.handleSet("/a/b", set);
        verify(cacheHandlerMock).getStructure("/a");

        // full node /a can't be served, since a/b and a/b/d were removed due to the set.
        // /a/b/c is still cached and /a/e also.
        assertNull(unitUnderTest.getCachedNode("/a", id));
        assertNull(unitUnderTest.getCachedNode("/a/b", id));
        assertNull(unitUnderTest.getCachedNode("/a/b/d", id));

        assertThat(unitUnderTest.getCachedNode("/a/b/c", id).getValue(), is(equalTo("dummy1")));
        assertThat(unitUnderTest.getCachedNode("/a/e", id).getValue(), is(equalTo("dummy2")));

    }

}
