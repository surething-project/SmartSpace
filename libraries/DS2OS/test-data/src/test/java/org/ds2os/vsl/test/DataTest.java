package org.ds2os.vsl.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.ds2os.vsl.core.node.VslNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test if the data structures actually load.
 *
 * @author felix
 */
public class DataTest {

    /**
     * SLF4J logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DataTest.class);

    /**
     * Load {@link TestNodes#BIG_STRUCTURE}.
     */
    @Test
    public void loadGiantStructure() {
        final long freeMemoryBefore = Runtime.getRuntime().freeMemory();
        final VslNode node = TestNodes.BIG_STRUCTURE;
        LOG.debug("Big node structure uses approximately {} MiB of RAM.",
                (freeMemoryBefore - Runtime.getRuntime().freeMemory()) / (1024 * 1024));
        assertThat(node, is(not(nullValue())));
    }
}
