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
 * Testclass for {@link ListDelHandler}.
 *
 * @author liebald
 */
public class ListDelHandlerTest {

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
        accessIDs.add("creator");

    }

    /**
     * Test method for {@link ListDelHandler#get(String, VslIdentity)}.
     *
     * @throws VslException
     *             shouldn't happen.
     */
    @Test
    public final void testGet() throws VslException {
        final ListDelHandler lAdd = new ListDelHandler(service + "/listNode", korMock, "creator",
                "minimumEntries='1',maximumEntries='5'"
                        + ",allowedTypes='/basic/number,/derived/boolean,/basic/text'",
                nodeFactory);

        when(korMock.get(eq(service + "/listNode/elements"), any(VslIdentity.class)))
                .thenReturn(nodeFactory.createImmutableLeaf(
                        "element1" + VslNodeDatabase.LIST_SEPARATOR + "element2"));

        lAdd.get(service + "/listNode/del/element1", new AddressParameters(),
                new ServiceIdentity("", accessIDs));

        verify(korMock).get(eq(service + "/listNode/elements"), any(VslIdentity.class));
        verify(korMock).set(eq(service + "/listNode/elements"), any(VslNode.class),
                any(VslIdentity.class));
        verify(korMock).removeNode(service + "/listNode/element1");
    }

    /**
     * Test method for {@link ListDelHandler#get(String, VslIdentity)}.
     *
     * @throws VslException
     *             Testing remval when list would be too small afterwards.
     */
    @Test
    public final void testGetListToSmall() throws VslException {

        final ListDelHandler lAdd = new ListDelHandler(service + "/listNode", korMock, "creator",
                "minimumEntries='2',maximumEntries='6'"
                        + ",allowedTypes='/basic/number,/derived/boolean'",
                nodeFactory);

        when(korMock.get(eq(service + "/listNode/elements"), any(VslIdentity.class)))
                .thenReturn(nodeFactory.createImmutableLeaf(
                        "element1" + VslNodeDatabase.LIST_SEPARATOR + "element2"));
        expectedException.expectMessage("already has its minimum amount of entries");

        expectedException.expect(ListOperationInvalidException.class);
        lAdd.get(service + "/listNode/del/element1", new AddressParameters(),
                new ServiceIdentity("", accessIDs));
    }

    /**
     * Test method for {@link ListDelHandler#get(String, VslIdentity)}.
     *
     * @throws VslException
     *             Testing removal of a non existent node.
     */
    @Test
    public final void testGetNodeNotExisting() throws VslException {

        final ListDelHandler lAdd = new ListDelHandler(service + "/listNode", korMock, "creator",
                "minimumEntries='1',maximumEntries='6'"
                        + ",allowedTypes='/basic/number,/derived/boolean'",
                nodeFactory);

        when(korMock.get(eq(service + "/listNode/elements"), any(VslIdentity.class)))
                .thenReturn(nodeFactory.createImmutableLeaf(
                        "element1" + VslNodeDatabase.LIST_SEPARATOR + "element2"));
        expectedException
                .expectMessage("doesn't contain an direct child  with relative address element3.");

        expectedException.expect(ListOperationInvalidException.class);
        lAdd.get(service + "/listNode/del/element3", new AddressParameters(),
                new ServiceIdentity("", accessIDs));
    }

    /**
     * Test method for {@link ListDelHandler#get(String, VslIdentity)}.
     *
     * @throws VslException
     *             Testing insufficient rights to remove a node.
     */
    @Test
    public final void testGetNoWriteAccess() throws VslException {
        final List<String> fakeID = new LinkedList<String>();
        fakeID.add("ID3");
        final ListDelHandler lAdd = new ListDelHandler(service + "/listNode", korMock, "creator",
                "minimumEntries='1',maximumEntries='6'"
                        + ",allowedTypes='/basic/number,/derived/boolean'",
                nodeFactory);

        when(korMock.get(eq(service + "/listNode/elements"), any(VslIdentity.class)))
                .thenReturn(nodeFactory.createImmutableLeaf(
                        "element1" + VslNodeDatabase.LIST_SEPARATOR + "element2"));
        expectedException.expectMessage("have the necessary rights to remove the node element1");

        expectedException.expect(ListOperationInvalidException.class);
        lAdd.get(service + "/listNode/del/element1", new AddressParameters(),
                new ServiceIdentity("", fakeID));
    }

    /**
     * Test method for {@link ListDelHandler#set(String, VslNode, VslIdentity)} .
     *
     * @throws VslException
     *             tested
     */
    @Test
    public final void testSet() throws VslException {
        final ListDelHandler del = new ListDelHandler(service + "/listNode", korMock, "creator", "",
                nodeFactory);
        expectedException.expect(InvalidOperationException.class);
        del.set(service + "/listNode/del", nodeFactory.createImmutableLeaf(""),
                new ServiceIdentity("", accessIDs));
    }
}
