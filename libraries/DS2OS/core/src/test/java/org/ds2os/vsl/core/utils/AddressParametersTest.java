package org.ds2os.vsl.core.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.ds2os.vsl.core.utils.VslAddressParameters.NodeInformationScope;
import org.junit.Test;

/**
 * TestClass for the KORUtility class.
 *
 * @author liebald
 */
public class AddressParametersTest {

    /**
     * Test method for {@link AddressParameters} builder pattern.
     */
    @Test
    public final void testBuilderPattern() {
        final VslAddressParameters params = new AddressParameters().withDepth(5)
                .withNodeInformationScope(NodeInformationScope.METADATA);
        assertThat(params.areParamsGiven(), is(equalTo(true)));
        assertThat(params.getDepth(), is(equalTo(5)));
        assertThat(params.getNodeInformationScope(), is(equalTo(NodeInformationScope.METADATA)));
    }

    /**
     * Test method for {@link AddressParameters} default values.
     */
    @Test
    public final void testDefaultValues() {
        final VslAddressParameters params = new AddressParameters();
        assertThat(params.areParamsGiven(), is(equalTo(false)));
        assertThat(params.getDepth(), is(equalTo(0)));
        assertThat(params.getNodeInformationScope(), is(equalTo(NodeInformationScope.VALUE)));
    }

    /**
     * Test method for {@link AddressParameters} map constructor.
     */
    @Test
    public final void testMapConstructor() {
        final Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("depth", "5");
        parameters.put("scope", "METADATA");

        final VslAddressParameters params = new AddressParameters(parameters);
        assertThat(params.areParamsGiven(), is(equalTo(true)));
        assertThat(params.getDepth(), is(equalTo(5)));
        assertThat(params.getNodeInformationScope(), is(equalTo(NodeInformationScope.METADATA)));
    }

}
