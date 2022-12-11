package org.ds2os.vsl.cache.cacheCleaner;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.ds2os.vsl.cache.dataCache.VslDataCache;
import org.ds2os.vsl.cache.metaCache.CachedNode;
import org.ds2os.vsl.cache.metaCache.VslMetaCache;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Testclass for the {@link CacheCleaner} Class.
 *
 * @author liebald
 */
public class CacheCleanerTest {

    /**
     * Object to synchronize access on the {@link VslMetaCache}.
     */
    private final Object threadLock = new Object();

    /**
     * Mock of the used {@link VslMetaCache}.
     */
    private VslMetaCache metaCacheMock;

    /**
     * Mock of the used {@link VslDataCache}.
     */
    private VslDataCache dataCacheMock;

    /**
     * The unit under test.
     */
    CacheCleaner unitUnderTest;

    /**
     * Set up the tests.
     */
    @Before
    public final void setUp() {
        metaCacheMock = mock(VslMetaCache.class);
        dataCacheMock = mock(VslDataCache.class);
        unitUnderTest = new CacheCleaner(threadLock, metaCacheMock, dataCacheMock);
    }

    /**
     * Test of the cleaner thread.
     */
    @Test
    @Ignore
    public final void test() {
        final Map<String, CachedNode> cachedNodes = new HashMap<String, CachedNode>();
        cachedNodes.put("/a/b", new CachedNode(1, 1, 10));
        cachedNodes.put("/c/d", new CachedNode(2, 2, 10));
        cachedNodes.put("/e/f", new CachedNode(3, 2, 10));
        cachedNodes.put("/g/h", new CachedNode(4, 4, 10));
        cachedNodes.put("/i/j", new CachedNode(5, 6, 10));

        when(metaCacheMock.getAllCachedNodes()).thenReturn(cachedNodes);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                cachedNodes.remove(invocation.getArguments()[0]);
                return null;
            }
        }).when(metaCacheMock).removeFromCache(anyString());

        final Thread t = new Thread(unitUnderTest);
        t.start();
        try {
            Thread.sleep(7000);
        } catch (final InterruptedException e) {

        }
        t.interrupt();

        verify(metaCacheMock, atLeast(2)).getAllCachedNodes();
        verify(metaCacheMock).removeFromCache("/a/b");
        verify(metaCacheMock).removeFromCache("/c/d");
        verify(metaCacheMock).removeFromCache("/e/f");
        verify(metaCacheMock, never()).removeFromCache("/g/h");
        verify(metaCacheMock, never()).removeFromCache("/i/j");
        verify(dataCacheMock).removeFromCache("/a/b");
        verify(dataCacheMock).removeFromCache("/c/d");
        verify(dataCacheMock).removeFromCache("/e/f");
        verify(dataCacheMock, never()).removeFromCache("/g/h");
        verify(dataCacheMock, never()).removeFromCache("/i/j");
    }

}
