package org.ds2os.vsl.cache.replacement;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ds2os.vsl.cache.metaCache.CachedNode;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Testclass ofr {@link ReplacementPolicies}.
 *
 * @author liebald
 */
public class ReplacementPoliciesTest {

    /**
     * Helper to add some delay between creating the used nodes.
     *
     * @param millisec
     *            The amount of milliseconds to wait.
     */
    private void delay(final int millisec) {
        try {
            Thread.sleep(millisec);
        } catch (final InterruptedException e) {

        }
    }

    /**
     * Test {@link ReplacementPolicies#getNodesToRemoveFIFO(Map, long)}.
     */
    @Test
    @Ignore
    public final void testGetNodesToRemoveFIFO() {
        final HashMap<String, CachedNode> currentlyCached = new HashMap<String, CachedNode>();

        /**
         * Assume each node needs a storage size of 100 byte total. (cachedNode.SIZE is added again
         * by the constructor.)
         */
        currentlyCached.put("a", new CachedNode(1, 5, 100 - CachedNode.SIZE));
        delay(10);
        currentlyCached.put("b", new CachedNode(1, 5, 100 - CachedNode.SIZE));
        delay(10);
        currentlyCached.put("c", new CachedNode(1, 5, 100 - CachedNode.SIZE));
        delay(10);
        currentlyCached.put("d", new CachedNode(1, 5, 100 - CachedNode.SIZE));
        delay(10);
        currentlyCached.put("e", new CachedNode(1, 5, 100 - CachedNode.SIZE));
        delay(10);
        currentlyCached.put("f", new CachedNode(1, 5, 100 - CachedNode.SIZE));

        final Collection<String> toRemove1 = ReplacementPolicies
                .getNodesToRemoveFIFO(currentlyCached, 100);
        final Collection<String> toRemove3 = ReplacementPolicies
                .getNodesToRemoveFIFO(currentlyCached, 300);
        final Collection<String> toRemove6 = ReplacementPolicies
                .getNodesToRemoveFIFO(currentlyCached, 600);

        assertThat(toRemove1.size(), is(equalTo(1)));
        assertThat(toRemove1.contains("a"), is(equalTo(true)));
        assertThat(toRemove1.contains("b"), is(equalTo(false)));
        assertThat(toRemove1.contains("c"), is(equalTo(false)));
        assertThat(toRemove1.contains("d"), is(equalTo(false)));
        assertThat(toRemove1.contains("e"), is(equalTo(false)));
        assertThat(toRemove1.contains("f"), is(equalTo(false)));

        assertThat(toRemove3.size(), is(equalTo(3)));
        assertThat(toRemove3.contains("a"), is(equalTo(true)));
        assertThat(toRemove3.contains("b"), is(equalTo(true)));
        assertThat(toRemove3.contains("c"), is(equalTo(true)));
        assertThat(toRemove3.contains("d"), is(equalTo(false)));
        assertThat(toRemove3.contains("e"), is(equalTo(false)));
        assertThat(toRemove3.contains("f"), is(equalTo(false)));

        assertThat(toRemove6.size(), is(equalTo(6)));
        assertThat(toRemove6.contains("a"), is(equalTo(true)));
        assertThat(toRemove6.contains("b"), is(equalTo(true)));
        assertThat(toRemove6.contains("c"), is(equalTo(true)));
        assertThat(toRemove6.contains("d"), is(equalTo(true)));
        assertThat(toRemove6.contains("e"), is(equalTo(true)));
        assertThat(toRemove6.contains("f"), is(equalTo(true)));
    }

    /**
     * Test {@link ReplacementPolicies#getNodesToRemoveLFU(Map, long)}.
     */
    @Test
    @Ignore
    public final void testGetNodesToRemoveLFU() {
        /**
         * Assume each node needs a storage size of 100 byte total. (cachedNode.SIZE is added again
         * by the constructor.)
         */
        final CachedNode a = new CachedNode(1, 5, 100 - CachedNode.SIZE);
        final CachedNode b = new CachedNode(1, 5, 100 - CachedNode.SIZE);
        final CachedNode c = new CachedNode(1, 5, 100 - CachedNode.SIZE);
        final CachedNode d = new CachedNode(1, 5, 100 - CachedNode.SIZE);
        final CachedNode e = new CachedNode(1, 5, 100 - CachedNode.SIZE);
        final CachedNode f = new CachedNode(1, 5, 100 - CachedNode.SIZE);

        a.nodeAccessed();
        c.nodeAccessed();
        b.nodeAccessed();
        c.nodeAccessed();
        d.nodeAccessed();
        e.nodeAccessed();
        f.nodeAccessed();
        a.nodeAccessed();
        a.nodeAccessed();
        f.nodeAccessed();

        final HashMap<String, CachedNode> currentlyCached = new HashMap<String, CachedNode>();
        currentlyCached.put("a", a);
        currentlyCached.put("b", b);
        currentlyCached.put("c", c);
        currentlyCached.put("d", d);
        currentlyCached.put("e", e);
        currentlyCached.put("f", f);

        final Collection<String> toRemove1 = ReplacementPolicies
                .getNodesToRemoveLFU(currentlyCached, 100);
        final Collection<String> toRemove3 = ReplacementPolicies
                .getNodesToRemoveLFU(currentlyCached, 300);
        final Collection<String> toRemove4 = ReplacementPolicies
                .getNodesToRemoveLFU(currentlyCached, 400);
        final Collection<String> toRemove6 = ReplacementPolicies
                .getNodesToRemoveLFU(currentlyCached, 600);

        // b,d and e were all accessed only once, which is the lowest access count ->remove all
        assertThat(toRemove1.size(), is(equalTo(3)));
        assertThat(toRemove1.contains("a"), is(equalTo(false)));
        assertThat(toRemove1.contains("b"), is(equalTo(true)));
        assertThat(toRemove1.contains("c"), is(equalTo(false)));
        assertThat(toRemove1.contains("d"), is(equalTo(true)));
        assertThat(toRemove1.contains("e"), is(equalTo(true)));
        assertThat(toRemove1.contains("f"), is(equalTo(false)));

        assertThat(toRemove3.size(), is(equalTo(3)));
        assertThat(toRemove3.contains("a"), is(equalTo(false)));
        assertThat(toRemove3.contains("b"), is(equalTo(true)));
        assertThat(toRemove3.contains("c"), is(equalTo(false)));
        assertThat(toRemove3.contains("d"), is(equalTo(true)));
        assertThat(toRemove3.contains("e"), is(equalTo(true)));
        assertThat(toRemove3.contains("f"), is(equalTo(false)));

        // lowest count of accesses is b,d,e, but since at least 4 should be removed, also remove
        // c,f which have an access count of 2
        assertThat(toRemove4.size(), is(equalTo(5)));
        assertThat(toRemove4.contains("a"), is(equalTo(false)));
        assertThat(toRemove4.contains("b"), is(equalTo(true)));
        assertThat(toRemove4.contains("c"), is(equalTo(true)));
        assertThat(toRemove4.contains("d"), is(equalTo(true)));
        assertThat(toRemove4.contains("e"), is(equalTo(true)));
        assertThat(toRemove4.contains("f"), is(equalTo(true)));

        assertThat(toRemove6.size(), is(equalTo(6)));
        assertThat(toRemove6.contains("a"), is(equalTo(true)));
        assertThat(toRemove6.contains("b"), is(equalTo(true)));
        assertThat(toRemove6.contains("c"), is(equalTo(true)));
        assertThat(toRemove6.contains("d"), is(equalTo(true)));
        assertThat(toRemove6.contains("e"), is(equalTo(true)));
        assertThat(toRemove6.contains("f"), is(equalTo(true)));
    }

    /**
     * Test {@link ReplacementPolicies#getNodesToRemoveLRU(Map, long)}.
     */
    @Test
    @Ignore
    public final void testGetNodesToRemoveLRU() {
        /**
         * Assume each node needs a storage size of 100 byte total. (cachedNode.SIZE is added again
         * by the constructor.)
         */
        final CachedNode a = new CachedNode(1, 5, 100 - CachedNode.SIZE);
        final CachedNode b = new CachedNode(1, 5, 100 - CachedNode.SIZE);
        final CachedNode c = new CachedNode(1, 5, 100 - CachedNode.SIZE);
        final CachedNode d = new CachedNode(1, 5, 100 - CachedNode.SIZE);
        final CachedNode e = new CachedNode(1, 5, 100 - CachedNode.SIZE);
        final CachedNode f = new CachedNode(1, 5, 100 - CachedNode.SIZE);

        a.nodeAccessed();
        delay(10); // delay after each access to distinguish between different accesses for testing
        c.nodeAccessed();
        delay(10);
        b.nodeAccessed();
        delay(10);
        c.nodeAccessed();
        delay(10);
        d.nodeAccessed();
        delay(10);
        e.nodeAccessed();
        delay(10);
        f.nodeAccessed();
        delay(10);
        a.nodeAccessed();
        delay(10);
        a.nodeAccessed();
        delay(10);
        f.nodeAccessed();

        final HashMap<String, CachedNode> currentlyCached = new HashMap<String, CachedNode>();
        currentlyCached.put("a", a);
        currentlyCached.put("b", b);
        currentlyCached.put("c", c);
        currentlyCached.put("d", d);
        currentlyCached.put("e", e);
        currentlyCached.put("f", f);

        final Collection<String> toRemove1 = ReplacementPolicies
                .getNodesToRemoveLRU(currentlyCached, 100);
        final Collection<String> toRemove3 = ReplacementPolicies
                .getNodesToRemoveLRU(currentlyCached, 300);
        final Collection<String> toRemove4 = ReplacementPolicies
                .getNodesToRemoveLRU(currentlyCached, 400);
        final Collection<String> toRemove6 = ReplacementPolicies
                .getNodesToRemoveLRU(currentlyCached, 600);

        // b was the longest without access -> remove
        assertThat(toRemove1.size(), is(equalTo(1)));
        assertThat(toRemove1.contains("a"), is(equalTo(false)));
        assertThat(toRemove1.contains("b"), is(equalTo(true)));
        assertThat(toRemove1.contains("c"), is(equalTo(false)));
        assertThat(toRemove1.contains("d"), is(equalTo(false)));
        assertThat(toRemove1.contains("e"), is(equalTo(false)));
        assertThat(toRemove1.contains("f"), is(equalTo(false)));

        assertThat(toRemove3.size(), is(equalTo(3)));
        assertThat(toRemove3.contains("a"), is(equalTo(false)));
        assertThat(toRemove3.contains("b"), is(equalTo(true)));
        assertThat(toRemove3.contains("c"), is(equalTo(true)));
        assertThat(toRemove3.contains("d"), is(equalTo(true)));
        assertThat(toRemove3.contains("e"), is(equalTo(false)));
        assertThat(toRemove3.contains("f"), is(equalTo(false)));

        assertThat(toRemove4.size(), is(equalTo(4)));
        assertThat(toRemove4.contains("a"), is(equalTo(false)));
        assertThat(toRemove4.contains("b"), is(equalTo(true)));
        assertThat(toRemove4.contains("c"), is(equalTo(true)));
        assertThat(toRemove4.contains("d"), is(equalTo(true)));
        assertThat(toRemove4.contains("e"), is(equalTo(true)));
        assertThat(toRemove4.contains("f"), is(equalTo(false)));

        assertThat(toRemove6.size(), is(equalTo(6)));
        assertThat(toRemove6.contains("a"), is(equalTo(true)));
        assertThat(toRemove6.contains("b"), is(equalTo(true)));
        assertThat(toRemove6.contains("c"), is(equalTo(true)));
        assertThat(toRemove6.contains("d"), is(equalTo(true)));
        assertThat(toRemove6.contains("e"), is(equalTo(true)));
        assertThat(toRemove6.contains("f"), is(equalTo(true)));
    }

    /**
     * Test {@link ReplacementPolicies#getNodesToRemoveRR(Map, long)}.
     */
    @Test
    @Ignore
    public final void testGetNodesToRemoveRR() {
        final HashMap<String, CachedNode> currentlyCached = new HashMap<String, CachedNode>();

        /**
         * Assume each node needs a storage size of 100 byte total. (cachedNode.SIZE is added again
         * by the constructor.)
         */
        currentlyCached.put("a", new CachedNode(1, 5, 100 - CachedNode.SIZE));
        delay(10);
        currentlyCached.put("b", new CachedNode(1, 5, 100 - CachedNode.SIZE));
        delay(10);
        currentlyCached.put("c", new CachedNode(1, 5, 100 - CachedNode.SIZE));
        delay(10);
        currentlyCached.put("d", new CachedNode(1, 5, 100 - CachedNode.SIZE));
        delay(10);
        currentlyCached.put("e", new CachedNode(1, 5, 100 - CachedNode.SIZE));
        delay(10);
        currentlyCached.put("f", new CachedNode(1, 5, 100 - CachedNode.SIZE));

        assertThat(ReplacementPolicies.getNodesToRemoveRR(currentlyCached, 15).size(),
                is(equalTo(1)));
        assertThat(ReplacementPolicies.getNodesToRemoveRR(currentlyCached, 100).size(),
                is(equalTo(1)));
        assertThat(ReplacementPolicies.getNodesToRemoveRR(currentlyCached, 101).size(),
                is(equalTo(2)));
        assertThat(ReplacementPolicies.getNodesToRemoveRR(currentlyCached, 200).size(),
                is(equalTo(2)));
        assertThat(ReplacementPolicies.getNodesToRemoveRR(currentlyCached, 300).size(),
                is(equalTo(3)));
        assertThat(ReplacementPolicies.getNodesToRemoveRR(currentlyCached, 399).size(),
                is(equalTo(4)));
        assertThat(ReplacementPolicies.getNodesToRemoveRR(currentlyCached, 500).size(),
                is(equalTo(5)));

        final Collection<String> toRemove6 = ReplacementPolicies.getNodesToRemoveRR(currentlyCached,
                600);
        assertThat(toRemove6.size(), is(equalTo(6)));
        assertThat(toRemove6.contains("a"), is(equalTo(true)));
        assertThat(toRemove6.contains("b"), is(equalTo(true)));
        assertThat(toRemove6.contains("c"), is(equalTo(true)));
        assertThat(toRemove6.contains("d"), is(equalTo(true)));
        assertThat(toRemove6.contains("e"), is(equalTo(true)));
        assertThat(toRemove6.contains("f"), is(equalTo(true)));
    }

}
