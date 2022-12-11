package org.ds2os.vsl.core.impl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.Arrays;

import org.ds2os.vsl.core.VslIdentity;
import org.junit.Test;

/**
 * Test the behaviour of the {@link ServiceIdentity} class.
 *
 * @author liebald
 * @author felix
 */
public final class ServiceIdentityTest {

    /**
     * Test {@link ServiceIdentity#isKA()}.
     */
    @Test
    public void testIsKA() {
        final VslIdentity vslId = new ServiceIdentity("service", "ID1");
        assertThat(vslId.isKA(), is(false));
    }

    /**
     * Test {@link ServiceIdentity#getAccessIDs()}.
     */
    @Test
    public void testGetAccessIDs() {
        final VslIdentity vslId = new ServiceIdentity("service", "ID1;ID2");
        assertThat(vslId.getAccessIDs().size(), is(equalTo(2)));
        assertThat(vslId.getAccessIDs(), containsInAnyOrder("ID1", "ID2"));
    }

    /**
     * Test {@link ServiceIdentity#addIdentity(String)}.
     */
    @Test
    public void testAddIdentity() {
        final ServiceIdentity serviceId = new ServiceIdentity("service",
                Arrays.asList("ID1", "ID2"));
        serviceId.addIdentity("ID3");
        final VslIdentity vslId = serviceId;
        assertThat(vslId.getAccessIDs().size(), is(equalTo(3)));
        assertThat(vslId.getAccessIDs(), containsInAnyOrder("ID1", "ID2", "ID3"));
    }

    /**
     * Test {@link ServiceIdentity#removeIdentity(String)}.
     */
    @Test
    public void testRemoveIdentity() {
        final ServiceIdentity serviceId = new ServiceIdentity("service",
                Arrays.asList("ID1", "ID2"));
        serviceId.removeIdentity("ID2");
        final VslIdentity vslId = serviceId;
        assertThat(vslId.getAccessIDs().size(), is(equalTo(1)));
        assertThat(vslId.getAccessIDs(), containsInAnyOrder("ID1"));
    }
}
