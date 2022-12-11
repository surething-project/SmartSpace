package org.ds2os.vsl.kor.lists;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.exception.InvalidOperationException;
import org.ds2os.vsl.exception.ListOperationInvalidException;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.kor.VslKnowledgeRepository;
import org.ds2os.vsl.kor.VslNodeDatabase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Testclass for {@link ListAddHandler}.
 *
 * @author liebald
 */
public class ListAddHandlerTest {

    /**
     * List of access IDs.
     */
    private List<String> accessIDs;

    /**
     * Rule for Exception testing. By default no Exception is expected.
     */
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * The KOR mock used for testing.
     */
    private VslKnowledgeRepository korMock;

    /**
     * address of the service node we use for tests.
     */
    private final String service = "/KA/service";

    /**
     * The {@link VslNodeFactory} for creating VslNodes.
     */
    private final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();

    /**
     * Setup the mocks for the test.
     */
    @Before
    public final void setUp() {
        korMock = mock(VslKnowledgeRepository.class);
        accessIDs = new LinkedList<String>();
        accessIDs.add("ID1");
        accessIDs.add("ID2");

    }

    /**
     * Test method for {@link ListAddHandler#get(String, VslIdentity)}.
     *
     * @throws VslException
     *             shouldn't happen.
     */
    @Test
    public final void testGet() throws VslException {
        final ListAddHandler lAdd = new ListAddHandler(service + "/listNode", korMock, "creator",
                "minimumEntries='1',maximumEntries='5'"
                        + ",allowedTypes='/basic/number,/derived/boolean,/basic/text'",
                accessIDs, nodeFactory);

        when(korMock.get(eq(service + "/listNode/elements"), any(VslIdentity.class)))
                .thenReturn(nodeFactory.createImmutableLeaf(
                        "element1" + VslNodeDatabase.LIST_SEPARATOR + "element2"));

        lAdd.get(service + "/listNode/add/5/basic/text", new AddressParameters(),
                new ServiceIdentity("", accessIDs));

        verify(korMock).get(eq(service + "/listNode/elements"), any(VslIdentity.class));
        verify(korMock).set(eq(service + "/listNode/elements"), any(VslNode.class),
                any(VslIdentity.class));
        verify(korMock).addSubtreeFromModelID(eq(service + "/listNode"), eq("/basic/text"),
                eq("creator"), any(String.class));
    }

    /**
     * Test method for {@link ListAddHandler#get(String, VslIdentity)}.
     *
     * @throws VslException
     *             Testing invalid List Type
     */
    @Test
    public final void testGetInvalidListType() throws VslException {
        final ListAddHandler lAdd = new ListAddHandler(service + "/listNode", korMock, "creator",
                "minimumEntries='1',maximumEntries='6'"
                        + ",allowedTypes='/basic/number,/derived/boolean'",
                accessIDs, nodeFactory);

        when(korMock.get(eq(service + "/listNode/elements"), any(VslIdentity.class)))
                .thenReturn(nodeFactory.createImmutableLeaf(
                        "element1" + VslNodeDatabase.LIST_SEPARATOR + "element2"));
        expectedException.expectMessage("Only models of the following");

        expectedException.expect(ListOperationInvalidException.class);
        lAdd.get(service + "/listNode/add/5/basic/text", new AddressParameters(),
                new ServiceIdentity("", accessIDs));
    }

    /**
     * Test method for {@link ListAddHandler#get(String, VslIdentity)}.
     *
     * @throws VslException
     *             Test if a full list is correctly recognized.
     */
    @Test
    public final void testGetListToBig() throws VslException {
        final ListAddHandler lAdd = new ListAddHandler(service + "/listNode", korMock, "creator",
                "minimumEntries='1',maximumEntries='2'"
                        + ",allowedTypes='/basic/number,/derived/boolean'",
                accessIDs, nodeFactory);

        when(korMock.get(eq(service + "/listNode/elements"), any(VslIdentity.class)))
                .thenReturn(nodeFactory.createImmutableLeaf(
                        "element1" + VslNodeDatabase.LIST_SEPARATOR + "element2"));
        expectedException.expectMessage("The list already has its maximum");

        expectedException.expect(ListOperationInvalidException.class);
        lAdd.get(service + "/listNode/add/5/basic/text", new AddressParameters(),
                new ServiceIdentity("", accessIDs));
    }

    /**
     * Test method for {@link ListAddHandler#get(String, VslIdentity)}.
     *
     * @throws VslException
     *             Test if insufficient write access is recognized.
     */
    @Test
    public final void testGetNoWriteAccess() throws VslException {
        final List<String> fakeID = new LinkedList<String>();
        fakeID.add("ID3");
        final ListAddHandler lAdd = new ListAddHandler(service + "/listNode", korMock, "creator",
                "minimumEntries='1',maximumEntries='5'"
                        + ",allowedTypes='/basic/text,/derived/boolean'",
                accessIDs, nodeFactory);

        when(korMock.get(eq(service + "/listNode/elements"), any(VslIdentity.class)))
                .thenReturn(nodeFactory.createImmutableLeaf(
                        "element1" + VslNodeDatabase.LIST_SEPARATOR + "element2"));

        expectedException.expectMessage("necessary rights to add a node");
        expectedException.expect(ListOperationInvalidException.class);

        lAdd.get(service + "/listNode/add/5/basic/text", new AddressParameters(),
                new ServiceIdentity("", fakeID));
    }

    /**
     * Test method for {@link ListAddHandler#get(String, VslIdentity)}.
     *
     * @throws VslException
     *             Test if invalid names for nodes are correctly filtered.
     */
    @Test
    public final void testGetInvalidNodeName() throws VslException {
        final ListAddHandler lAdd = new ListAddHandler(service + "/listNode", korMock, "creator",
                "minimumEntries='1',maximumEntries='5'"
                        + ",allowedTypes='/basic/text,/derived/boolean'",
                accessIDs, nodeFactory);

        when(korMock.get(eq(service + "/listNode/elements"), any(VslIdentity.class)))
                .thenReturn(nodeFactory.createImmutableLeaf(
                        "element1" + VslNodeDatabase.LIST_SEPARATOR + "element2"));

        expectedException.expectMessage("Invalid custom nodename for new list element");
        expectedException.expect(ListOperationInvalidException.class);

        lAdd.get(service + "/listNode/add/5/basic/text//testname22/;r3fer", new AddressParameters(),
                new ServiceIdentity("", accessIDs));
    }

    /**
     * Test method for {@link ListAddHandler#set(String, VslNode, VslIdentity)} .
     *
     * @throws VslException
     *             tested
     */
    @Test
    public final void testSet() throws VslException {
        final ListAddHandler lAdd = new ListAddHandler(service + "/listNode", korMock, "creator",
                "", accessIDs, nodeFactory);
        expectedException.expect(InvalidOperationException.class);
        lAdd.set(service + "/listNode", nodeFactory.createImmutableLeaf(""),
                new ServiceIdentity("", accessIDs));
    }
}
