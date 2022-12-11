package org.ds2os.vsl.kor.lists;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.core.utils.AddressParameters;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.kor.VslKnowledgeRepository;
import org.ds2os.vsl.kor.VslNodeDatabase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Testclass for {@link ListRootHandler}.
 *
 * @author liebald
 */
public class ListRootHandlerTest {

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
     * Test method for {@link ListRootHandler#get(String, VslIdentity)}.
     *
     * @throws VslException
     *             shouldn't happen.
     */
    @Test
    public final void testGet() throws VslException {
        final ListRootHandler rHandler = new ListRootHandler(service + "/listNode", korMock,
                nodeFactory);

        when(korMock.get(eq(service + "/listNode/elements"), any(VslIdentity.class)))
                .thenReturn(nodeFactory.createImmutableLeaf(
                        "element1" + VslNodeDatabase.LIST_SEPARATOR + "element2"));

        rHandler.get(service + "/listNode/1", new AddressParameters(),
                new ServiceIdentity("", accessIDs));
    }
}
