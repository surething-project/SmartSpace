package org.ds2os.vsl.modelCache;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.ds2os.vsl.exception.InvalidModelException;
import org.ds2os.vsl.exception.ModelIDExistsAlreadyException;
import org.ds2os.vsl.exception.ModelNotFoundException;
import org.ds2os.vsl.exception.RequiredDataMissingException;
import org.ds2os.vsl.kor.dataStructures.InternalNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Testclass for the Modelcache.
 *
 * @author liebald
 *
 */
public class ModelCacheTest {

    /**
     * The modelCache instance.
     */
    private ModelCache mc;

    /**
     * modelID of testmodel2.
     */
    private final String testModel2ID = "/test/testmodel2";

    /**
     * An advanced model with testmodel as type.
     */
    private final String testModel2XML = "<testmodel2 reader=\"lalelu\""
            + " type=\"/test/testmodel,/test/testmodel3\">3"
            + "<testsubnode1 reader=\"foo\" writer=\"bar\" restriction=\"regex='.*'\">"
            + "foo bar</testsubnode1>" + "</testmodel2>";

    /**
     * modelID of testmodel3.
     */
    private final String testModel3ID = "/test/testmodel3";

    /**
     * An advanced model with testmodel as type.
     */
    private final String testModel3XML = "<testmodel type=\"/basic/text\" reader=\"*\" "
            + "writer=\"*\">8"
            + "<testsubnode3 reader=\"*\" writer=\"*\" type=\"/basic/text\">sub3</testsubnode3>"
            + "<testsubnode4 reader=\"*\" writer=\"*\" type=\"/test/testmodel\">sub4</testsubnode4>"
            + "</testmodel>";

    /**
     * modelID of testmodel.
     */
    private final String testModelID = "/test/testmodel";

    /**
     * A simple testmodel with two subnodes.
     */
    private final String testModelXML = "<testmodel type=\"/basic/text\" reader=\"*\" "
            + "writer=\"*\">8"
            + "<testsubnode1 reader=\"*\" writer=\"*\" type=\"/basic/text\">sub1</testsubnode1>"
            + "<testsubnode2 reader=\"*\" writer=\"*\" type=\"/basic/text\">sub2</testsubnode2>"
            + "</testmodel>";

    /**
     * Testmodel for inheritance testing.
     */
    private final String testInheritance = "<model type=\"/derived/boolean,/basic/text\" "
            + "reader=\"a\" ></model>";

    /**
     * ID of model testInheritance.
     */
    private final String testInheritanceID = "/test/inheritance";

    /**
     * Testmodel for inheritance testing.
     */
    private final String testInheritance2 = "<model type=\"/test/inheritance, /derived/boolean\">"
            + "</model>";

    /**
     * ID of model testInheritance2.
     */
    private final String testInheritance2ID = "/test/inheritance2";

    /**
     * Setup the models, store them in the cache before the tests begin.
     */
    @Before
    public final void setUp() {
        mc = ModelCache.getInstance();
        try {
            mc.setModel(testModelID, testModelXML);
            mc.setModel(testModel2ID, testModel2XML);
            mc.setModel(testModel3ID, testModel3XML);
            mc.setModel(testInheritanceID, testInheritance);
            mc.setModel(testInheritance2ID, testInheritance2);
        } catch (final Exception e) {
            fail("Unexpected Exception: " + e.getMessage());
        }

    }

    /**
     * Remove models from cache when the test is finished.
     */
    @After
    public final void shutDown() {
        mc.removeModel(testModelID);
        mc.removeModel(testModel2ID);
        mc.removeModel(testModel3ID);
        mc.removeModel(testInheritanceID);
        mc.removeModel(testInheritance2ID);

    }

    // @Test
    // public final void test() {
    //
    // TreeMap<String, InternalNode> nodes = new TreeMap<String, InternalNode>();
    // try {
    // nodes = mc.getCompleteModelNodes(testModel2ID, "testService");
    // } catch (ModelNotFoundException e) {
    // fail(e.getMessage());
    // }
    // for (Entry<String, InternalNode> a : nodes.entrySet()) {
    // System.out.println(a.getKey());
    // System.out.println(a.getValue());
    // System.out.println();
    //
    // }
    //
    // }

    /**
     * Test adding and removing a model from/to the modelcache.
     */
    @Test
    public final void testAddRemoveModel() {

        try {
            mc.setModel(testModelID, "");
        } catch (final RequiredDataMissingException e1) {
            // expected;
        } catch (final Exception e) {
            fail("Unexpected Exception: " + e.getMessage());
        }

        try {
            mc.setModel(testModelID, testModelXML);
            fail("Expected ModelIDExistsAlreadyException");
        } catch (final ModelIDExistsAlreadyException e) {
            // Expected
        } catch (final Exception e) {
            fail("Unexpected Exception: " + e.getMessage());
        }
    }

    /**
     * Test the retrieval of models.
     */
    @Test
    public final void testSetGetModel() {

        mc.removeModel(testModelID);
        try {
            mc.getModel(testModelID);
            fail("Expected ModelNotFoundException");
        } catch (final ModelNotFoundException e) {
            // Expected
        }

        try {
            mc.setModel(testModelID, testModelXML);
        } catch (final Exception e) {
            fail("Unexpected Exception: " + e.getMessage());
        }

        try {
            assertThat(mc.getModel(testModelID), is(equalTo(testModelXML)));
        } catch (final ModelNotFoundException e) {
            fail("Unexpected Exception: " + e.getMessage());
        }

    }

    /**
     * Test if type inheritance works correctly.
     *
     * @throws ModelNotFoundException
     *             shouldn't happen.
     * @throws InvalidModelException
     *             shouldn't happen.
     */
    @Test
    public final void testTypeInheritance() throws ModelNotFoundException, InvalidModelException {
        final LinkedHashMap<String, InternalNode> s = mc.getCompleteModelNodes(testInheritance2ID,
                "test");
        final List<String> types = s.get("test").getType();
        final Set<String> typeSet = new HashSet<String>(types);
        System.out.println(types);

        assertThat(types.contains("/test/inheritance2"), is(equalTo(true)));
        assertThat(types.contains("/test/inheritance"), is(equalTo(true)));
        assertThat(types.contains("/derived/boolean"), is(equalTo(true)));
        assertThat(types.contains("/basic/text"), is(equalTo(true)));
        assertThat(types.contains("/basic/number"), is(equalTo(true)));
        System.out.println(types);
        // if the set and the list don't have the same size, the list has duplicates in it, which
        // shouldn't happen.
        assertThat(types.size() == typeSet.size(), is(equalTo(true)));

        // TODO test ordering
    }

}
