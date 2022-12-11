package org.ds2os.vsl.core.node;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Unit test for {@link VslNode} objects and their subtree building.
 *
 * @author felix
 */
public final class VslNodeTest {

    /**
     * Factory for {@link VslNode}.
     */
    private final VslNodeFactory factory = new VslNodeFactoryImpl();

    /**
     * Type of the main VslNode.
     */
    private final List<String> type = Arrays.asList("type");

    /**
     * Type of the a child of the main VslNode.
     */
    private final List<String> atype = Arrays.asList("atype");

    /**
     * Type of the a/b child of the main VslNode.
     */
    private final List<String> btype = Arrays.asList("btype");

    /**
     * Type of the x/c child of the main VslNode.
     */
    private final List<String> ctype = Arrays.asList("ctype");

    /**
     * Type of the d child of the main VslNode.
     */
    private final List<String> dtype = Arrays.asList("dtype");

    /**
     * Create example structure in a map.
     *
     * @return Map with example data for a VSL.
     */
    public Map<String, VslNode> getTestMap() {
        final Map<String, VslNode> result = new HashMap<String, VslNode>();
        result.put("", factory.createImmutableLeaf(type, "this"));
        result.put("a", factory.createImmutableLeaf(atype, "avalue"));
        result.put("a/b", factory.createImmutableLeaf(btype, "bvalue"));
        result.put("x/c", factory.createImmutableLeaf(ctype, "cvalue"));
        result.put("d", factory.createImmutableLeaf(dtype, "dvalue"));
        return result;
    }

    /**
     * Test of {@link VslNode#getChild(String)}.
     */
    @Test
    public void testGetChild() {
        final VslNode testNode = factory.createImmutableNode(getTestMap().entrySet());

        // get "a"
        final VslNode aChild = testNode.getChild("a");
        assertThat(aChild, is(not(nullValue())));
        assertThat(aChild.getTypes(), is(equalTo(atype)));
        assertThat(aChild.getValue(), is(equalTo("avalue")));

        // get "x/c"
        final VslNode cChild = testNode.getChild("x/c");
        assertThat(cChild, is(not(nullValue())));
        assertThat(cChild.getTypes(), is(equalTo(ctype)));
        assertThat(cChild.getValue(), is(equalTo("cvalue")));

        // get "b" from node "a" (must be equal to "a/b")
        final VslNode bChild = aChild.getChild("b");
        assertThat(bChild, is(not(nullValue())));
        assertThat(bChild, is(equalTo(testNode.getChild("a/b"))));
        assertThat(bChild.getTypes(), is(equalTo(btype)));
        assertThat(bChild.getValue(), is(equalTo("bvalue")));

        // get "d"
        final VslNode dChild = testNode.getChild("d");
        assertThat(dChild, is(not(nullValue())));
        assertThat(dChild.getTypes(), is(equalTo(dtype)));
        assertThat(dChild.getValue(), is(equalTo("dvalue")));

        // some non-existent nodes (must not raise NPE)
        assertThat(testNode.getChild("e"), is(nullValue()));
        assertThat(testNode.getChild("e/x"), is(nullValue()));
        assertThat(testNode.getChild("x/b"), is(nullValue()));
    }

    /**
     * Test of {@link VslMutableTreeComponent#putChild(String, VslMutableTreeComponent)}.
     */
    @Test
    public void testPutChild() {
        final VslMutableNode testNode = factory.createMutableNode(getTestMap().entrySet());
        final List<String> newType = Arrays.asList("newType");

        // put a new node at a, replacing old node (and subtree)
        final VslNode putAResult = testNode.putChild("a",
                factory.createMutableNode(newType, "newValue"));
        assertThat(putAResult, is(not(nullValue())));
        assertThat(putAResult.getTypes(), is(equalTo(atype)));
        assertThat(putAResult.getValue(), is(equalTo("avalue")));

        // check new a
        final VslNode aChild = testNode.getChild("a");
        assertThat(aChild, is(not(nullValue())));
        assertThat(aChild.getTypes(), is(equalTo(newType)));
        assertThat(aChild.getValue(), is(equalTo("newValue")));

        // a/b no longer exists
        assertThat(aChild.isLeaf(), is(equalTo(true)));
        assertThat(testNode.getChild("a/b"), is(nullValue()));
        assertThat(aChild.getChild("b"), is(nullValue()));

        // save reference to old intermediary node x
        final VslNode oldX = testNode.getChild("x");

        // put a new node at x/c, replacing old node
        final VslNode putCResult = testNode.putChild("x/c",
                factory.createMutableNode(newType, "newValue"));
        assertThat(putCResult, is(not(nullValue())));
        assertThat(putCResult.getTypes(), is(equalTo(ctype)));
        assertThat(putCResult.getValue(), is(equalTo("cvalue")));

        // check new x/c
        final VslNode cChild = testNode.getChild("x/c");
        assertThat(cChild, is(not(nullValue())));
        assertThat(cChild.getTypes(), is(equalTo(newType)));
        assertThat(cChild.getValue(), is(equalTo("newValue")));

        // check that x was not replaced
        assertThat(testNode.getChild("x"), is(oldX));

        // create new node e/f/g
        final VslMutableNode insert = factory.createMutableNode();
        insert.setValue("onlyValue");
        final VslNode putGResult = testNode.putChild("e/f/g", insert);
        assertThat(putGResult, is(nullValue()));

        // check e/f/g
        final VslNode gChild = testNode.getChild("e/f/g");
        assertThat(gChild, is(not(nullValue())));
        assertNull(gChild.getTypes());
        assertThat(gChild.getValue(), is(equalTo("onlyValue")));

        // remove e/f
        final VslNode removed = testNode.putChild("e/f", null);
        assertThat(removed, is(not(nullValue())));
        assertThat(testNode.getChild("e/f"), is(nullValue()));
        assertThat(testNode.getChild("e/f/g"), is(nullValue()));
    }

    /**
     * Test of {@link VslNode#hasChild(String)}.
     */
    @Test
    public void testHasChild() {
        final VslMutableNode leafNode = factory.createMutableNode(type, "value");
        final VslNode nodeWithChild = new VslNodeImpl("child",
                new VslNodeImpl("grandchild", leafNode));

        assertThat(leafNode.hasChild("child"), is(false));
        assertThat(nodeWithChild.hasChild("child"), is(true));
        assertThat(nodeWithChild.hasChild("child/grandchild"), is(true));
        assertThat(nodeWithChild.hasChild("child/whatever"), is(false));
        assertThat(nodeWithChild.hasChild("path"), is(false));
    }

    /**
     * /** Test of {@link VslMutableNode#setValue(String)}.
     */
    @Test
    public void testSetValue() {
        final VslMutableNode testNode = factory.createMutableNode("oldValue");
        testNode.setValue("newValue");
        assertThat(testNode.getValue(), is(equalTo("newValue")));
    }

    /**
     * Test of {@link VslNode#isLeaf()}.
     */
    @Test
    public void testIsLeaf() {
        final VslMutableNode testNode = factory.createMutableNode(type, "value");
        assertThat(testNode.isLeaf(), is(true));
        testNode.putChild("a", factory.createMutableNode("new"));
        assertThat(testNode.isLeaf(), is(false));
    }

    /**
     * Test of {@link VslNode#getTimestamp()}.
     */
    @Test
    public void testGetTimestamp() {
        final Date date = new Date();
        final VslNode testNode = factory.createImmutableLeaf(type, "someValue", date, 0, null,
                Collections.<String, String>emptyMap());
        assertThat(testNode.getTimestamp().toString(), is(equalTo(date.toString())));
    }

    /**
     * Test of {@link VslNode#getVersion()}.
     */
    @Test
    public void testGetVersion() {
        final Date date = new Date();
        final VslNode testNode = factory.createImmutableLeaf(type, "someValue", date, 15L, null,
                Collections.<String, String>emptyMap());
        assertThat(testNode.getVersion(), is(equalTo(15L)));
    }

    /**
     * Test of {@link VslNode#getDirectChildren()}.
     */
    @Test
    public void testGetDirectChildren() {
        final VslNode testNode = factory.createImmutableNode(getTestMap().entrySet());
        final Set<String> directChildren = new HashSet<String>();
        for (final Map.Entry<String, VslNode> entry : testNode.getDirectChildren()) {
            directChildren.add(entry.getKey());
        }

        // only direct children must be contained
        assertThat(directChildren.contains("a"), is(true));
        assertThat(directChildren.contains("b"), is(false));
        assertThat(directChildren.contains("a/b"), is(false));
        assertThat(directChildren.contains("c"), is(false));
        assertThat(directChildren.contains("x/c"), is(false));
        assertThat(directChildren.contains("d"), is(true));
        assertThat(directChildren.contains("e"), is(false));
        assertThat(directChildren.contains("x"), is(true));
    }

    /**
     * Test of {@link VslNode#getAllChildren()}.
     */
    @Test
    public void testGetAllChildren() {
        final VslNode testNode = factory.createImmutableNode(getTestMap().entrySet());
        final Set<String> directChildren = new HashSet<String>();
        for (final Map.Entry<String, VslNode> entry : testNode.getAllChildren()) {
            directChildren.add(entry.getKey());
        }

        // all children must be contained
        assertThat(directChildren.contains("a"), is(true));
        assertThat(directChildren.contains("b"), is(false));
        assertThat(directChildren.contains("a/b"), is(true));
        assertThat(directChildren.contains("c"), is(false));
        assertThat(directChildren.contains("x/c"), is(true));
        assertThat(directChildren.contains("d"), is(true));
        assertThat(directChildren.contains("e"), is(false));
        assertThat(directChildren.contains("x"), is(true));
    }
}
