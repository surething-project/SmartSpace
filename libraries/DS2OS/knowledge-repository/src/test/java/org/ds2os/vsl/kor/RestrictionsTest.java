package org.ds2os.vsl.kor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

/**
 * Testclass for {@link Restrictions}.
 *
 * @author liebald
 *
 */
public class RestrictionsTest {

    /**
     * Test the {@link Restrictions#evaluateNumberText(String, String)} function.
     */
    @Test
    public final void testEvaluateNumberText() {

        // Test empty restrictions
        assertThat(Restrictions.evaluateNumberText("5", null), is(equalTo(true)));
        assertThat(Restrictions.evaluateNumberText("5", ""), is(equalTo(true)));
        assertThat(Restrictions.evaluateNumberText(null, null), is(equalTo(true)));
        assertThat(Restrictions.evaluateNumberText(null, "minimumValue='3'"), is(equalTo(false)));

        // test invalid restrictions/values
        assertThat(Restrictions.evaluateNumberText("f", "minimumValue='3'"), is(equalTo(false)));
        assertThat(Restrictions.evaluateNumberText("f", "minimumValue='f'"), is(equalTo(false)));
        assertThat(Restrictions.evaluateNumberText("5", "maximumValue='t'"), is(equalTo(false)));
        assertThat(Restrictions.evaluateNumberText("e", "minimumValue= '3',maximumValue='e'"),
                is(equalTo(false)));

        // Test valid number evaluations:
        assertThat(Restrictions.evaluateNumberText("5", "minimumValue='3'"), is(equalTo(true)));
        assertThat(Restrictions.evaluateNumberText("5", "maximumValue='6'"), is(equalTo(true)));
        assertThat(Restrictions.evaluateNumberText("5", "minimumValue='3',maximumValue='6'"),
                is(equalTo(true)));
        assertThat(Restrictions.evaluateNumberText("-5", "minimumValue='-6'"), is(equalTo(true)));
        assertThat(Restrictions.evaluateNumberText("-5", "maximumValue='-3'"), is(equalTo(true)));
        assertThat(Restrictions.evaluateNumberText("-5", "minimumValue='-6',maximumValue='-3'"),
                is(equalTo(true)));
        assertThat(Restrictions.evaluateNumberText("abbba", "regularExpression='ab*a'"),
                is(equalTo(true)));

        // Test invalid number evaluations:
        assertThat(Restrictions.evaluateNumberText("2", "minimumValue='3'"), is(equalTo(false)));
        assertThat(Restrictions.evaluateNumberText("7", "maximumValue='6'"), is(equalTo(false)));
        assertThat(Restrictions.evaluateNumberText("2", "minimumValue='3',maximumValue='6'"),
                is(equalTo(false)));
        assertThat(Restrictions.evaluateNumberText("7", "minimumValue='3',maximumValue='6'"),
                is(equalTo(false)));
        assertThat(Restrictions.evaluateNumberText("5", "minimumValue='6',maximumValue='3'"),
                is(equalTo(false)));
        assertThat(Restrictions.evaluateNumberText("abcba", "regularExpression='ab*a'"),
                is(equalTo(false)));

    }

    /**
     * Test the {@link Restrictions#getAllowedListTypes(String)} function.
     */
    @Test
    public final void testgetAllowedListTypes() {
        assertThat(Restrictions.getAllowedListTypes(""), is(nullValue()));
        assertThat(Restrictions.getAllowedListTypes("allowedTypes='/a/b,/b/c'").toString(),
                is(equalTo("[/a/b, /b/c]")));
        assertThat(Restrictions.getAllowedListTypes("allowedTypes='/a/b,/b/c;/f/v'").toString(),
                is(equalTo("[/a/b, /b/c, /f/v]")));
        assertThat(Restrictions.getAllowedListTypes("minimumEntries='7', allowedTypes='/a/b,/b/c'")
                .toString(), is(equalTo("[/a/b, /b/c]")));
        assertThat(Restrictions.getAllowedListTypes("allowedTypes=''").toString(),
                is(equalTo("[]")));
    }

    /**
     * Test the {@link Restrictions#getMaxListEntries(String) } function.
     */
    @Test
    public final void testgetMaxListEntries() {
        assertThat(Restrictions.getMaxListEntries("maximumEntries='7'"), is(equalTo(7)));
        assertThat(Restrictions.getMaxListEntries("maximumEntries='ergre'"),
                is(equalTo(Integer.MAX_VALUE)));
        assertThat(Restrictions.getMaxListEntries("minimumEntries='3', maximumEntries='7'"),
                is(equalTo(7)));
        assertThat(Restrictions.getMaxListEntries("minimumEntries='7'"),
                is(equalTo(Integer.MAX_VALUE)));
        assertThat(Restrictions.getMaxListEntries(""), is(equalTo(Integer.MAX_VALUE)));

    }

    /**
     * Test the {@link Restrictions#getMinListEntries(String) } function.
     */
    @Test
    public final void testgetMinListEntries() {
        assertThat(Restrictions.getMinListEntries("minimumEntries='7'"), is(equalTo(7)));
        assertThat(Restrictions.getMinListEntries("minimumEntries='ergre'"), is(equalTo(0)));
        assertThat(Restrictions.getMinListEntries("minimumEntries='3', maximumEntries='7'"),
                is(equalTo(3)));
        assertThat(Restrictions.getMinListEntries("maximumEntries='7'"), is(equalTo(0)));
        assertThat(Restrictions.getMinListEntries(""), is(equalTo(0)));
    }

}
