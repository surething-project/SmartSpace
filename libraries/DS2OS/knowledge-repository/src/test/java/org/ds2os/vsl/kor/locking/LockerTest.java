package org.ds2os.vsl.kor.locking;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.Map;

import org.ds2os.vsl.core.VslIdentity;
import org.ds2os.vsl.core.VslLockHandler;
import org.ds2os.vsl.core.config.VslKORLockingConfig;
import org.ds2os.vsl.core.impl.ServiceIdentity;
import org.ds2os.vsl.core.node.VslMutableNode;
import org.ds2os.vsl.core.node.VslNodeFactory;
import org.ds2os.vsl.core.node.VslNodeFactoryImpl;
import org.ds2os.vsl.exception.VslException;
import org.ds2os.vsl.kor.VslNodeDatabase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

/**
 * @author liebald
 */
public class LockerTest {

    /**
     * Mock of the VslNodeDatabase.
     */
    private VslNodeDatabase dbMock;

    /**
     * VslIdentity for tests.
     */
    private VslIdentity id1;

    /**
     * VslIdentity for tests.
     */
    private VslIdentity id2;

    /**
     * Unit under Test.
     */
    private Locker locker;

    /**
     * Mock for callbacks.
     */
    private VslLockHandler lockHandlerMock;

    /**
     * Mock for the configuration.
     */
    private VslKORLockingConfig configMock;

    /**
     * The {@link VslNodeFactory} for creating VslNodes.
     */
    private final VslNodeFactory nodeFactory = new VslNodeFactoryImpl();

    /**
     * Setup function.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Before
    public final void setUp() throws Exception {
        dbMock = mock(VslNodeDatabase.class);
        lockHandlerMock = mock(VslLockHandler.class);
        configMock = mock(VslKORLockingConfig.class);
        when(configMock.getLockExpirationTime()).thenReturn(60);
        when(configMock.getLockExpirationWarningTime()).thenReturn(10);
        locker = new Locker(dbMock, configMock);
        id1 = new ServiceIdentity("id1", "id1");
        id2 = new ServiceIdentity("id2", "id2");
        locker.activate();
    }

    /**
     * Test method for {@link Locker#commitSubtree(String, VslIdentity)} .
     *
     * @throws VslException
     *             shouldn't happen.
     */
    @Test
    public final void testCommitSubtree() throws VslException {
        final String address = "/ka/service";
        locker.lockSubtree(address, id1, new LinkedList<String>(id1.getAccessIDs()),
                lockHandlerMock);
        locker.addNodeValueForCommit(address, "111");
        locker.addNodeValueForCommit(address + "/child1", "222");
        locker.addNodeValueForCommit(address, "333");
        locker.commitSubtree(address, id1);
        verify(dbMock).setValueTree(Matchers.<Map<String, String>>any());

        assertThat(locker.isLocked(address), is(equalTo(false)));
        verify(lockHandlerMock).lockAcquired(address);
        verify(lockHandlerMock, never()).lockExpired(address);
        verify(lockHandlerMock, never()).lockWillExpire(address);
    }

    /**
     * Test method for {@link Locker#isChildLocked(String)}.
     *
     * @throws VslException
     *             shouldn't happen.
     */
    @Test
    public final void testIsChildLocked() throws VslException {

        final String address = "/ka/service";
        locker.lockSubtree(address + "/child1", id1, new LinkedList<String>(id1.getAccessIDs()),
                lockHandlerMock);

        assertThat(locker.isChildLocked(address), is(equalTo(true)));
        assertThat(locker.isChildLocked(address + "/child1"), is(equalTo(false)));
        assertThat(locker.isChildLocked("/ka/service2"), is(equalTo(false)));
        assertThat(locker.isChildLocked(address + "/child2"), is(equalTo(false)));
        assertThat(locker.isChildLocked("/"), is(equalTo(false)));
    }

    /**
     * Test method for {@link Locker#isLocked(String)}.
     *
     * @throws VslException
     *             shouldn't happen.
     */
    @Test
    public final void testIsLocked() throws VslException {
        final String address = "/ka/service";
        locker.lockSubtree(address, id1, new LinkedList<String>(id1.getAccessIDs()),
                lockHandlerMock);
        assertThat(locker.isLocked(address), is(equalTo(true)));
        assertThat(locker.isLocked(address + "/child1"), is(equalTo(true)));
        assertThat(locker.isLocked("/ka/service2"), is(equalTo(false)));
        assertThat(locker.isLocked("/ka"), is(equalTo(false)));
    }

    /**
     * Test method for {@link Locker#isLockedBy(String, String)}.
     *
     * @throws VslException
     *             shouldn't happen.
     */
    @Test
    public final void testIsLockedBy() throws VslException {
        final String address = "/ka/service";
        locker.lockSubtree(address, id1, new LinkedList<String>(id1.getAccessIDs()),
                lockHandlerMock);
        assertThat(locker.isLockedBy(address, id1.getClientId()), is(equalTo(true)));
        assertThat(locker.isLockedBy(address + "/child1", id1.getClientId()), is(equalTo(true)));
        assertThat(locker.isLockedBy("/ka/service2", id1.getClientId()), is(equalTo(false)));
        assertThat(locker.isLockedBy("/ka", id1.getClientId()), is(equalTo(false)));

        assertThat(locker.isLockedBy(address, id2.getClientId()), is(equalTo(false)));
        assertThat(locker.isLockedBy(address + "/child1", id2.getClientId()), is(equalTo(false)));

    }

    /**
     * Test method for {@link Locker#rollbackSubtree(String, VslIdentity)} .
     *
     * @throws VslException
     *             shouldn't happen.
     */
    @Test
    public final void testRollbackSubtree() throws VslException {
        final String address = "/ka/service";
        locker.lockSubtree(address, id1, new LinkedList<String>(id1.getAccessIDs()),
                lockHandlerMock);
        locker.addNodeValueForCommit(address, "123");
        locker.addNodeValueForCommit(address, "456");
        locker.rollbackSubtree(address, id1);
        final VslMutableNode testNode = nodeFactory.createMutableNode("0");
        testNode.setVersion(1);
        locker.updateGetResultWithLockedData(address, testNode, id1.getClientId());

        assertThat(locker.isLocked(address), is(equalTo(false)));
        assertThat(testNode.getValue(), is(equalTo("0")));
        assertThat(testNode.getVersion(), is(equalTo(1L)));

        verify(lockHandlerMock).lockAcquired(address);
        verify(lockHandlerMock, never()).lockExpired(address);
        verify(lockHandlerMock, never()).lockWillExpire(address);

    }

    /**
     * Test method for {@link Locker#updateGetResultWithLockedData(String, VslMutableNode, String)}.
     *
     * @throws Exception
     *             shouldn't happen.
     */
    @Test
    public final void testUpdateGetResultWithLockedData() throws Exception {
        final String address = "/Ka1/service";

        final VslMutableNode node = nodeFactory.createMutableNode("root");
        node.putChild("child1", nodeFactory.createMutableNode("child1"));
        node.putChild("child1/child11", nodeFactory.createMutableNode("child11"));
        node.putChild("child2", nodeFactory.createMutableNode("child2"));
        node.putChild("child3", nodeFactory.createMutableNode("child3"));
        node.setVersion(5);
        ((VslMutableNode) node.getChild("child1")).setVersion(2);
        ((VslMutableNode) node.getChild("child1/child11")).setVersion(1);
        ((VslMutableNode) node.getChild("child2")).setVersion(1);
        ((VslMutableNode) node.getChild("child3")).setVersion(1);

        // acquire 3 locks and add some nodes as changes
        // 2 locks belong to the later requester of an get, 1 to another identity.
        locker.lockSubtree(address + "/child1", id1, new LinkedList<String>(id1.getAccessIDs()),
                lockHandlerMock);
        locker.lockSubtree(address + "/child2", id1, new LinkedList<String>(id1.getAccessIDs()),
                lockHandlerMock);
        locker.lockSubtree(address + "/child3", id2, new LinkedList<String>(id2.getAccessIDs()),
                lockHandlerMock);

        locker.addNodeValueForCommit(address + "/child1", "child1Update1");
        locker.addNodeValueForCommit(address + "/child1/child11", "child11Update1");
        locker.addNodeValueForCommit(address + "/child1/child11", "child11Update2");
        locker.addNodeValueForCommit(address + "/child2", "child2Update1");
        locker.addNodeValueForCommit(address + "/child2", "child2Update2");
        // The following update should be ignored, since they belong to another ids lock.
        locker.addNodeValueForCommit(address + "/child3", "child3Update1");

        locker.updateGetResultWithLockedData(address, node, id1.getClientId());

        assertThat(node.getValue(), is(equalTo("root")));
        assertThat(node.getChild("child1").getValue(), is(equalTo("child1Update1")));
        assertThat(node.getChild("child1/child11").getValue(), is(equalTo("child11Update2")));
        assertThat(node.getChild("child2").getValue(), is(equalTo("child2Update2")));
        assertThat(node.getChild("child3").getValue(), is(equalTo("child3")));

        assertThat(node.getVersion(), is(equalTo(8L)));
        assertThat(node.getChild("child1").getVersion(), is(equalTo(4L)));
        assertThat(node.getChild("child1/child11").getVersion(), is(equalTo(2L)));
        assertThat(node.getChild("child2").getVersion(), is(equalTo(2L)));
        assertThat(node.getChild("child3").getVersion(), is(equalTo(1L)));
        verify(lockHandlerMock).lockAcquired(address + "/child1");
        verify(lockHandlerMock, never()).lockWillExpire(address + "/child1");
        verify(lockHandlerMock, never()).lockExpired(address + "/child1");
        verify(lockHandlerMock).lockAcquired(address + "/child2");
        verify(lockHandlerMock, never()).lockWillExpire(address + "/child2");
        verify(lockHandlerMock, never()).lockExpired(address + "/child2");
        verify(lockHandlerMock).lockAcquired(address + "/child3");
        verify(lockHandlerMock, never()).lockWillExpire(address + "/child3");
        verify(lockHandlerMock, never()).lockExpired(address + "/child3");

    }

    /**
     * Test method for {@link Locker} to make sure the lockWillExpired callback is called correctly.
     *
     * @throws VslException
     *             shouldn't happen.
     */
    @Test
    public final void testLockWillExpire() throws VslException {
        final String address = "/ka/service";

        when(configMock.getLockExpirationTime()).thenReturn(60);
        when(configMock.getLockExpirationWarningTime()).thenReturn(59);

        locker.lockSubtree(address, id1, new LinkedList<String>(id1.getAccessIDs()),
                lockHandlerMock);

        try {
            Thread.sleep(2500);
        } catch (final InterruptedException e) {
            // shouldn't happen
            System.out.println(e.getMessage());
        }
        assertThat(locker.isLocked(address), is(equalTo(true)));
        verify(lockHandlerMock).lockAcquired(address);
        verify(lockHandlerMock).lockWillExpire(address);
        verify(lockHandlerMock, never()).lockExpired(address);
    }

    /**
     * Test method for {@link Locker} to make sure the lockExpired callback is called correctly.
     *
     * @throws VslException
     *             shouldn't happen.
     */
    @Test
    public final void testLockExpired() throws VslException {
        final String address = "/ka/service";

        when(configMock.getLockExpirationTime()).thenReturn(1);
        when(configMock.getLockExpirationWarningTime()).thenReturn(1);

        locker.lockSubtree(address, id1, new LinkedList<String>(id1.getAccessIDs()),
                lockHandlerMock);

        try {
            Thread.sleep(3000);
        } catch (final InterruptedException e) {
            // shouldn't happen
            System.out.println(e.getMessage());
        }
        assertThat(locker.isLocked(address), is(equalTo(false)));
        verify(lockHandlerMock).lockAcquired(address);
        verify(lockHandlerMock).lockWillExpire(address);
        verify(lockHandlerMock).lockExpired(address);
    }

}
