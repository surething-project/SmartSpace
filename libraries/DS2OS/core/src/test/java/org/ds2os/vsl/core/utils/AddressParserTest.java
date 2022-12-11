package org.ds2os.vsl.core.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

/**
 * TestClass for the KORUtility class.
 *
 * @author liebald
 */
public class AddressParserTest {

    /**
     * Test method for {@link AddressParser#makeWellFormedAddress(String)}.
     */
    @Test
    public final void testMakeWellFormedAddress() {
        assertThat(AddressParser.makeWellFormedAddress(null), is(equalTo("/")));
        assertThat(AddressParser.makeWellFormedAddress("/"), is(equalTo("/")));
        assertThat(AddressParser.makeWellFormedAddress(""), is(equalTo("/")));
        assertThat(AddressParser.makeWellFormedAddress("a"), is(equalTo("/a")));
        assertThat(AddressParser.makeWellFormedAddress("a/"), is(equalTo("/a")));
        assertThat(AddressParser.makeWellFormedAddress("/a/"), is(equalTo("/a")));
        assertThat(AddressParser.makeWellFormedAddress("/a?3"), is(equalTo("/a")));
        assertThat(AddressParser.makeWellFormedAddress("/a?"), is(equalTo("/a")));
    }

    /**
     * Test method for {@link AddressParser#getAllParentsOfAddress(String)}.
     */
    @Test
    public final void testGetAllParentsOfAddress() {
        assertThat(AddressParser.getAllParentsOfAddress("/").toString(), is(equalTo("[]")));
        assertThat(AddressParser.getAllParentsOfAddress("/KA").toString(), is(equalTo("[/]")));
        assertThat(AddressParser.getAllParentsOfAddress("/KA/service").toString(),
                is(equalTo("[/KA, /]")));

    }

    /**
     * Test method for {@link AddressParser#getAllParentsOfAddress(String, int)}.
     */
    @Test
    public final void testGetAllParentsOfAddressUntilLevel() {
        assertThat(AddressParser.getAllParentsOfAddress("/KA", 1).toString(), is(equalTo("[/]")));
        assertThat(AddressParser.getAllParentsOfAddress("/KA/service", 1).toString(),
                is(equalTo("[/KA, /]")));
        assertThat(AddressParser.getAllParentsOfAddress("/KA/service/node", 1).toString(),
                is(equalTo("[/KA/service, /KA, /]")));
        assertThat(AddressParser.getAllParentsOfAddress("/KA/service/node", 2).toString(),
                is(equalTo("[/KA/service]")));
    }

    /**
     * Test method for {@link AddressParser#getParentAddress(String)}.
     */
    @Test
    public final void testGetParentAddress() {
        assertThat(AddressParser.getParentAddress("/KA"), is(equalTo("/")));
        assertThat(AddressParser.getParentAddress("/KA/service"), is(equalTo("/KA")));
        assertThat(AddressParser.getParentAddress("/KA/service/node"), is(equalTo("/KA/service")));

    }

    /**
     * Test method for {@link AddressParser#getParametersFromURIQuery(String)}.
     */
    @Test
    public final void testGetParametersFromURIQuery() {
        assertThat(AddressParser.getParametersFromURIQuery("").isEmpty(), is(equalTo(true)));
        assertThat(AddressParser.getParametersFromURIQuery("?").isEmpty(), is(equalTo(true)));
        assertThat(AddressParser.getParametersFromURIQuery("example.test?").isEmpty(),
                is(equalTo(true)));
        assertThat(AddressParser.getParametersFromURIQuery("?param1=value1").isEmpty(),
                is(equalTo(false)));
        assertThat(AddressParser.getParametersFromURIQuery("?param1=value1").containsKey("param1"),
                is(equalTo(true)));
        assertThat(AddressParser.getParametersFromURIQuery("?param1=value1").get("param1"),
                is(equalTo("value1")));
        assertThat(AddressParser.getParametersFromURIQuery("?param1=value1&param2=value2")
                .containsKey("param1"), is(equalTo(true)));
        assertThat(AddressParser.getParametersFromURIQuery("?param1=value1&param2=value2")
                .get("param1"), is(equalTo("value1")));
        assertThat(AddressParser.getParametersFromURIQuery("?param1=value1&param2=value2")
                .containsKey("param2"), is(equalTo(true)));
        assertThat(AddressParser.getParametersFromURIQuery("?param1=value1&param2=value2")
                .get("param2"), is(equalTo("value2")));
    }

    /**
     * Test method for {@link AddressParser#getParametersAsURIQuery(Map)}.
     */
    @Test
    public final void testGetParametersAsURIQuery() {
        final Map<String, String> testMap = new LinkedHashMap<String, String>();
        assertThat(AddressParser.getParametersAsURIQuery(testMap), is(equalTo("")));
        testMap.put("param1", "value1");
        assertThat(AddressParser.getParametersAsURIQuery(testMap), is(equalTo("?param1=value1")));
        testMap.put("param2", "value2");
        assertThat(AddressParser.getParametersAsURIQuery(testMap),
                is(equalTo("?param1=value1&param2=value2")));
    }

    /**
     * Test method for the interaction of {@link AddressParser#getParametersAsURIQuery(Map)} and
     * {@link AddressParser#getParentAddress(String)}.
     */
    @Test
    public final void testParameterDoubleParse() {
        final String uri = "example.test?param1=value1&param2=value2";
        final String uri2 = AddressParser
                .getParametersAsURIQuery(AddressParser.getParametersFromURIQuery(uri));
        assertThat(uri2, is(equalTo("?param1=value1&param2=value2")));

    }
}
