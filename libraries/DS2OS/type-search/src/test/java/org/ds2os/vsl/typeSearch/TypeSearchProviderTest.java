package org.ds2os.vsl.typeSearch;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import java.util.Arrays;
import java.util.Collections;

import org.ds2os.vsl.core.node.VslStructureNodeImpl;
import org.junit.Before;
import org.junit.Test;

/**
 * TestClass for the {@link TypeSearchProvider}.
 *
 * @author liebald
 */
public class TypeSearchProviderTest {

    /**
     * Unit under test.
     */
    private TypeSearchProvider unitUnderTest;

    /**
     * @throws Exception
     *             shouldn't happen.
     */
    @Before
    public final void setUp() throws Exception {
        unitUnderTest = new TypeSearchProvider();

        final VslStructureNodeImpl vslStructureNode = new VslStructureNodeImpl(
                Collections.<String>emptyList(), Collections.<String>emptyList(), "",
                Arrays.asList("/basic/text", "/basic/number"), "");
        vslStructureNode.putChild("child1",
                new VslStructureNodeImpl(Collections.<String>emptyList(),
                        Collections.<String>emptyList(), "",
                        Arrays.asList("/basic/text", "/basic/number"), ""));
        vslStructureNode.putChild("child2",
                new VslStructureNodeImpl(Collections.<String>emptyList(),
                        Collections.<String>emptyList(), "",
                        Arrays.asList("/derived/boolean", "/basic/number"), ""));

        final VslStructureNodeImpl vslStructureNode2 = new VslStructureNodeImpl(
                Collections.<String>emptyList(), Collections.<String>emptyList(), "",
                Arrays.asList("/basic/text"), "");
        vslStructureNode2.putChild("child1", new VslStructureNodeImpl(
                Collections.<String>emptyList(), Collections.<String>emptyList(), "",
                Arrays.asList("/searchProvider/type", "/system/searchProvider", "/basic/composed"),
                ""));
        unitUnderTest.addTypes("/agent1", vslStructureNode);
        unitUnderTest.addTypes("/agent2", vslStructureNode2);
    }

    /**
     * Test method for {@link TypeSearchProvider#getAddressesOfType(String)}.
     */
    @Test
    public final void testGetAddressesOfType() {

        assertThat(unitUnderTest.getAddressesOfType("/not/existing").isEmpty(), is(equalTo(true)));
        assertThat(unitUnderTest.getAddressesOfType("/basic/composed").contains("/agent2/child1"),
                is(equalTo(true)));
        assertThat(unitUnderTest.getAddressesOfType("/system/searchProvider")
                .contains("/agent2/child1"), is(equalTo(true)));
        assertThat(
                unitUnderTest.getAddressesOfType("/searchProvider/type").contains("/agent2/child1"),
                is(equalTo(true)));
        assertThat(unitUnderTest.getAddressesOfType("/derived/boolean").contains("/agent1/child2"),
                is(equalTo(true)));
        assertThat(unitUnderTest.getAddressesOfType("/basic/number"),
                containsInAnyOrder("/agent1", "/agent1/child1", "/agent1/child2"));
        assertThat(unitUnderTest.getAddressesOfType("/basic/text"),
                containsInAnyOrder("/agent1", "/agent1/child1", "/agent2"));
    }

    /**
     * Test method for {@link TypeSearchProvider#getTypesOfAddress(String)}.
     */
    @Test
    public final void testGetTypesOfAddress() {
        assertThat(unitUnderTest.getTypesOfAddress("/not/existing").isEmpty(), is(equalTo(true)));
        assertThat(unitUnderTest.getTypesOfAddress("/agent1"),
                containsInAnyOrder("/basic/text", "/basic/number"));
        assertThat(unitUnderTest.getTypesOfAddress("/agent1/child1"),
                containsInAnyOrder("/basic/text", "/basic/number"));
        assertThat(unitUnderTest.getTypesOfAddress("/agent1/child2"),
                containsInAnyOrder("/derived/boolean", "/basic/number"));
        assertThat(unitUnderTest.getTypesOfAddress("/agent2"), containsInAnyOrder("/basic/text"));
        assertThat(unitUnderTest.getTypesOfAddress("/agent2/child1"), containsInAnyOrder(
                "/searchProvider/type", "/system/searchProvider", "/basic/composed"));
    }

    /**
     * Test method for {@link TypeSearchProvider#removeAddress(String)}.
     */
    @Test
    public final void testRemoveAddress() {

        unitUnderTest.removeAddress("/agent1/child1");

        assertThat(unitUnderTest.getAddressesOfType("/basic/composed").contains("/agent2/child1"),
                is(equalTo(true)));
        assertThat(unitUnderTest.getAddressesOfType("/system/searchProvider")
                .contains("/agent2/child1"), is(equalTo(true)));
        assertThat(
                unitUnderTest.getAddressesOfType("/searchProvider/type").contains("/agent2/child1"),
                is(equalTo(true)));
        assertThat(unitUnderTest.getAddressesOfType("/derived/boolean").contains("/agent1/child2"),
                is(equalTo(true)));
        assertThat(unitUnderTest.getAddressesOfType("/basic/number"),
                containsInAnyOrder("/agent1", "/agent1/child2"));
        assertThat(unitUnderTest.getAddressesOfType("/basic/text"),
                containsInAnyOrder("/agent1", "/agent2"));

        unitUnderTest.removeAddress("/agent2");

        assertThat(unitUnderTest.getAddressesOfType("/basic/composed").isEmpty(),
                is(equalTo(true)));
        assertThat(unitUnderTest.getAddressesOfType("/system/searchProvider").isEmpty(),
                is(equalTo(true)));
        assertThat(unitUnderTest.getAddressesOfType("/searchProvider/type").isEmpty(),
                is(equalTo(true)));
        assertThat(unitUnderTest.getAddressesOfType("/derived/boolean").contains("/agent1/child2"),
                is(equalTo(true)));
        assertThat(unitUnderTest.getAddressesOfType("/basic/number"),
                containsInAnyOrder("/agent1", "/agent1/child2"));
        assertThat(unitUnderTest.getAddressesOfType("/basic/text"), containsInAnyOrder("/agent1"));

    }

}
