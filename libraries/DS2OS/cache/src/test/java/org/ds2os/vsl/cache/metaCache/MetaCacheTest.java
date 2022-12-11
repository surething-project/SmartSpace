package org.ds2os.vsl.cache.metaCache;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslKORCacheHandler;
import org.ds2os.vsl.core.config.VslCacheConfig;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.node.VslMutableStructureNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.node.VslStructureNodeImpl;
import org.ds2os.vsl.exception.NodeCannotBeCachedException;
import org.ds2os.vsl.exception.NodeNotExistingException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * TestClass for {@link MetaCache} implementation of {@link VslMetaCache}.
 *
 * @author liebald
 */
public class MetaCacheTest {

    /**
     * Object that is tested. Used for Storage of Cache meta information.
     */
    private VslMetaCache unitUnderTest;

    /**
     * Mock of the {@link VslKORCacheHandler} for KOR access.
     */
    private VslKORCacheHandler cacheHandlerMock;

    /**
     * Mock of the {@link VslCacheConfig} for accessing the config.
     */
    private VslCacheConfig cacheConfigMock;

    /**
     * The {@link VslNodeFactory} to use.
     */
    private VslNodeFactory nodeFactory;

    /**
     * Rule for Exception testing. By default no Exception is expected.
     */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
        when(cacheConfigMock.getDefaultTTL()).thenReturn(60);
        when(cacheConfigMock.getCharset()).thenReturn("UTF-8");
        when(cacheConfigMock.getReplacementPolicy()).thenReturn("rr");
        when(cacheConfigMock.getCacheCapacity()).thenReturn(100);

        nodeFactory = new VslNodeFactoryImpl();
        unitUnderTest = new MetaCache(cacheHandlerMock, cacheConfigMock, nodeFactory);
    }

    /**
     * Test of the Cache operation.
     *
     * @throws NodeCannotBeCachedException
     *             shouldn't happen.
     * @throws NodeNotExistingException
     *             shouldn't happen.
     */
    @Test
    public final void testCache() throws NodeCannotBeCachedException, NodeNotExistingException {
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

        final VslMutableNode toCache = nodeFactory
                .createMutableClone(nodeFactory.createImmutableLeaf(types, "root", new Date(1234L),
                        5L, null, Collections.<String, String>emptyMap()));
        toCache.putChild("b", nodeFactory.createMutableClone(nodeFactory.createImmutableLeaf(types,
                "child_b", new Date(1234L), 5L, null, Collections.<String, String>emptyMap())));
        toCache.putChild("b/c",
                nodeFactory.createMutableClone(nodeFactory.createImmutableLeaf(types, "child_b_c",
                        new Date(1234L), 5L, null, Collections.<String, String>emptyMap())));
        toCache.putChild("b/d",
                nodeFactory.createMutableClone(nodeFactory.createImmutableLeaf(types, "child_b_d",
                        new Date(1234L), 5L, null, Collections.<String, String>emptyMap())));
        toCache.putChild("e", nodeFactory.createMutableClone(nodeFactory.createImmutableLeaf(types,
                "child_e", new Date(1234L), 5L, null, Collections.<String, String>emptyMap())));

        unitUnderTest.cache("/a", toCache);

        final Map<String, CachedNode> cached = unitUnderTest.getAllCachedNodes();
        assertThat(cached.keySet().contains("/a"), is(equalTo(true)));
        assertThat(cached.keySet().contains("/a/b"), is(equalTo(true)));
        assertThat(cached.keySet().contains("/a/b/c"), is(equalTo(true)));
        assertThat(cached.keySet().contains("/a/b/d"), is(equalTo(true)));
        assertThat(cached.keySet().contains("/a/e"), is(equalTo(true)));
    }

    /**
     * Test of the Cache operation. If any child of the node (or the node itself) that should be
     * cached has a TTL of 0, this part is not cached, the rest should be.
     *
     * @throws NodeCannotBeCachedException
     *             tested for TTL 0
     * @throws NodeNotExistingException
     *             shouldn't happen.
     */
    @Test
    public final void testCacheTTLZero()
            throws NodeCannotBeCachedException, NodeNotExistingException {
        final List<String> readers = Arrays.asList("ID1", "ID2");
        final List<String> writers = Arrays.asList("ID2", "ID3");
        final String restrictions = "";
        final List<String> types = Arrays.asList("/basic/text");

        final VslMutableStructureNode structure = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        structure.putChild("b",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='0'"));
        structure.putChild("b/c",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='10'"));
        when(cacheHandlerMock.getStructure("/a")).thenReturn(structure);

        // The VslNode to be cached has the child b/c, which isn't part of the structure defined
        // above -> virtual subtree.
        final VslMutableNode toCache = nodeFactory
                .createMutableClone(nodeFactory.createImmutableLeaf(types, "root", new Date(1234L),
                        5L, null, Collections.<String, String>emptyMap()));
        toCache.putChild("b", nodeFactory.createMutableClone(nodeFactory.createImmutableLeaf(types,
                "child_b", new Date(1234L), 5L, null, Collections.<String, String>emptyMap())));
        toCache.putChild("b/c",
                nodeFactory.createMutableClone(nodeFactory.createImmutableLeaf(types, "child_b_c",
                        new Date(1234L), 5L, null, Collections.<String, String>emptyMap())));
        unitUnderTest.cache("/a", toCache);

        final Map<String, CachedNode> cached = unitUnderTest.getAllCachedNodes();
        System.out.println(cached);
        assertThat(cached.keySet().contains("/a"), is(equalTo(true)));
        assertThat(cached.keySet().contains("/a/b"), is(equalTo(false)));
        assertThat(cached.keySet().contains("/a/b/c"), is(equalTo(true)));

    }

    /**
     * Test if the isCached Method works correct.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen.
     * @throws NodeCannotBeCachedException
     *             shouldn't happen.
     */
    @Test
    public final void testIsCached() throws NodeNotExistingException, NodeCannotBeCachedException {
        final List<String> readers = Arrays.asList("ID1", "ID2");
        final List<String> writers = Arrays.asList("ID2", "ID3");
        final String restrictions = "";
        final List<String> types = Arrays.asList("/basic/text");
        final VslIdentity id = new ServiceIdentity("client", "ID1");

        final VslMutableStructureNode structure = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        structure.putChild("b",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='10'"));
        structure.putChild("b/c",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='10'"));
        structure.putChild("b/d", new VslStructureNodeImpl(Arrays.asList("ID2"), writers,
                restrictions, types, "TTL='10'"));
        when(cacheHandlerMock.getStructure("/a")).thenReturn(structure);

        // The structure contains b/d as child which isn't cached. However, the given VslIdentity
        // does not have access to this node, so no problem.
        final VslMutableNode toCache = nodeFactory
                .createMutableClone(nodeFactory.createImmutableLeaf(types, "root", new Date(1234L),
                        5L, null, Collections.<String, String>emptyMap()));
        toCache.putChild("b", nodeFactory.createMutableClone(nodeFactory.createImmutableLeaf(types,
                "child_b", new Date(1234L), 5L, null, Collections.<String, String>emptyMap())));
        toCache.putChild("b/c",
                nodeFactory.createMutableClone(nodeFactory.createImmutableLeaf(types, "child_b_c",
                        new Date(1234L), 5L, null, Collections.<String, String>emptyMap())));
        unitUnderTest.cache("/a", toCache);
        assertThat(unitUnderTest.isCached("/a", id), is(equalTo(true)));
    }

    /**
     * Test if the isCached Method works correct. When not all accessible nodes are cached, the
     * cache shouldn't be able to serve the request.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen.
     * @throws NodeCannotBeCachedException
     *             shouldn't happen.
     */
    @Test
    public final void testIsCachedPartly()
            throws NodeNotExistingException, NodeCannotBeCachedException {
        final List<String> readers = Arrays.asList("ID1", "ID2");
        final List<String> writers = Arrays.asList("ID2", "ID3");
        final String restrictions = "";
        final List<String> types = Arrays.asList("/basic/text");
        final VslIdentity id = new ServiceIdentity("client", "ID1");

        final VslMutableStructureNode structure = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        structure.putChild("b",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='10'"));
        structure.putChild("b/c",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='10'"));
        when(cacheHandlerMock.getStructure("/a")).thenReturn(structure);

        // b/c is not cached, therefore the complete /a isn't seen as cached.
        final VslMutableNode toCache = nodeFactory
                .createMutableClone(nodeFactory.createImmutableLeaf(types, "root", new Date(1234L),
                        5L, null, Collections.<String, String>emptyMap()));
        toCache.putChild("b", nodeFactory.createMutableClone(nodeFactory.createImmutableLeaf(types,
                "child_b", new Date(1234L), 5L, null, Collections.<String, String>emptyMap())));
        unitUnderTest.cache("/a", toCache);
        assertThat(unitUnderTest.isCached("/a", id), is(equalTo(false)));
    }

    /**
     * Tests the removal of items from the metaCache.
     *
     * @throws NodeNotExistingException
     *             shouldn't happen.
     * @throws NodeCannotBeCachedException
     *             shouldn't happen.
     */
    @Test
    public final void testRemoveFromCache()
            throws NodeNotExistingException, NodeCannotBeCachedException {

        final List<String> readers = Arrays.asList("ID1", "ID2");
        final List<String> writers = Arrays.asList("ID2", "ID3");
        final String restrictions = "";
        final List<String> types = Arrays.asList("/basic/text");

        final VslMutableStructureNode structure = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        structure.putChild("b",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='10'"));
        structure.putChild("b/c",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='10'"));
        structure.putChild("b/d",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='10'"));
        when(cacheHandlerMock.getStructure("/a")).thenReturn(structure);

        final VslMutableNode toCache = nodeFactory
                .createMutableClone(nodeFactory.createImmutableLeaf(types, "root", new Date(1234L),
                        5L, null, Collections.<String, String>emptyMap()));
        toCache.putChild("b", nodeFactory.createMutableClone(nodeFactory.createImmutableLeaf(types,
                "child_b", new Date(1234L), 5L, null, Collections.<String, String>emptyMap())));
        toCache.putChild("b/c",
                nodeFactory.createMutableClone(nodeFactory.createImmutableLeaf(types, "child_b_c",
                        new Date(1234L), 5L, null, Collections.<String, String>emptyMap())));
        unitUnderTest.cache("/a", toCache);

        final Map<String, CachedNode> cached = unitUnderTest.getAllCachedNodes();

        assertThat(cached.size(), is(equalTo(3)));
        unitUnderTest.removeFromCache("/a/b");
        assertThat(cached.size(), is(equalTo(2)));
        assertThat(cached.containsKey("/a"), is(equalTo(true)));
        assertThat(cached.containsKey("/a/b"), is(equalTo(false)));
        assertThat(cached.containsKey("/a/b/c"), is(equalTo(true)));

    }

    /**
     * Test if the metaData of all cached nodes affected by an access are updated correctly on
     * {@link MetaCache#updatedCachedData(String, org.ds2os.vsl.core.node.VslNode)}.
     *
     * @throws NodeCannotBeCachedException
     *             shouldn't happen.
     * @throws NodeNotExistingException
     *             shouldn't happen.
     */
    @Test
    public final void testUpdatedCachedData()
            throws NodeCannotBeCachedException, NodeNotExistingException {
        final List<String> readers = Arrays.asList("ID1", "ID2");
        final List<String> writers = Arrays.asList("ID2", "ID3");
        final String restrictions = "";
        final List<String> types = Arrays.asList("/basic/text");

        final VslMutableStructureNode structure = new VslStructureNodeImpl(readers, writers,
                restrictions, types, "TTL='30'");
        structure.putChild("b",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='10'"));
        structure.putChild("b/c",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='10'"));
        structure.putChild("b/d",
                new VslStructureNodeImpl(readers, writers, restrictions, types, "TTL='10'"));
        when(cacheHandlerMock.getStructure("/a")).thenReturn(structure);

        final VslMutableNode toCache = nodeFactory
                .createMutableClone(nodeFactory.createImmutableLeaf(types, "root", new Date(1234L),
                        5L, null, Collections.<String, String>emptyMap()));
        toCache.putChild("b", nodeFactory.createMutableClone(nodeFactory.createImmutableLeaf(types,
                "child_b", new Date(1234L), 5L, null, Collections.<String, String>emptyMap())));
        toCache.putChild("b/c",
                nodeFactory.createMutableClone(nodeFactory.createImmutableLeaf(types, "child_b_c",
                        new Date(1234L), 5L, null, Collections.<String, String>emptyMap())));
        unitUnderTest.cache("/a", toCache);

        unitUnderTest.updatedCachedData("/a", toCache);

        Map<String, CachedNode> cached = unitUnderTest.getAllCachedNodes();
        assertThat(cached.get("/a").getAmountAccessed(), is(equalTo(1)));
        assertThat(cached.get("/a/b").getAmountAccessed(), is(equalTo(1)));
        assertThat(cached.get("/a/b/c").getAmountAccessed(), is(equalTo(1)));

        unitUnderTest.updatedCachedData("/a", toCache);
        cached = unitUnderTest.getAllCachedNodes();
        assertThat(cached.get("/a").getAmountAccessed(), is(equalTo(2)));
        assertThat(cached.get("/a/b").getAmountAccessed(), is(equalTo(2)));
        assertThat(cached.get("/a/b/c").getAmountAccessed(), is(equalTo(2)));

    }

}
