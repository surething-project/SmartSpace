package org.ds2os.vsl.netutils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.security.InvalidParameterException;

import org.ds2os.vsl.exception.UnkownProtocolVersionException;
import org.ds2os.vsl.netutils.KeyStringParser;
import org.junit.Test;

/**
 * Test class for KeyStringParser.
 *
 * @author Johannes Straßer
 *
 */
public class KeyStringParserTest {

    /**
     * Test using the TLS string TLS_PSK_WITH_NULL_SHA.
     */
    @Test
    public final void testKeyStringParser0() {
        KeyStringParser parser = new KeyStringParser("TLS_PSK_WITH_NULL_SHA");
        assertThat(parser.getCipherString(), is("NULL"));
        assertThat(parser.getModeString(), is(nullValue()));
        assertThat(parser.getCipherKeyLength(), is(0));
        assertThat(parser.getPaddingString(), is(nullValue()));
        assertThat(parser.getMacString(), is("SHA1"));
        assertThat(parser.getMACKeyLength(), is(160));
    }

    /**
     * Test using the TLS string TLS_PSK_WITH_AES_128_GCM_SHA256.
     */
    @Test
    public final void testKeyStringParser1() {
        KeyStringParser parser = new KeyStringParser("TLS_PSK_WITH_AES_128_GCM_SHA256");
        assertThat(parser.getCipherString(), is("AES"));
        assertThat(parser.getModeString(), is("GCM"));
        assertThat(parser.getCipherKeyLength(), is(128));
        assertThat(parser.getPaddingString(), is("NOPADDING"));
        assertThat(parser.getMacString(), is("SHA256"));
        assertThat(parser.getMACKeyLength(), is(256));
    }

    /**
     * Test using the TLS string TLS_PSK_WITH_CAMELLIA_256_CBC_SHA384.
     */
    @Test
    public final void testKeyStringParser2() {
        KeyStringParser parser = new KeyStringParser("TLS_PSK_WITH_CAMELLIA_256_CBC_SHA384");
        assertThat(parser.getCipherString(), is("CAMELLIA"));
        assertThat(parser.getModeString(), is("CBC"));
        assertThat(parser.getCipherKeyLength(), is(256));
        assertThat(parser.getPaddingString(), is("PKCS5Padding"));
        assertThat(parser.getMacString(), is("SHA384"));
        assertThat(parser.getMACKeyLength(), is(384));
    }

    /**
     * Test using the TLS string TLS_PSK_WITH_AES_128_CCM.
     */
    @Test
    public final void testKeyStringParser3() {
        try {
            new KeyStringParser("TLS_PSK_WITH_AES_128_CCM");
            fail("This should not be accepted");
        } catch (InvalidParameterException e) {
            // pass
        }
    }

    /**
     * Test using the TLS string TLS_PSK_WITH_RC4_128_SHA.
     */
    @Test
    public final void testKeyStringParser4() {
        try {
            new KeyStringParser("TLS_PSK_WITH_RC4_128_SHA");
            fail("This should not be accepted");
        } catch (InvalidParameterException e) {
            // pass
        }
    }

    /**
     * Test using the TLS string TLS_PSK_WITH_3DES_EDE_CBC_SHA.
     */
    @Test
    public final void testKeyStringParser5() {
        try {
            new KeyStringParser("TLS_PSK_WITH_3DES_EDE_CBC_SHA");
            fail("This should not be accepted");
        } catch (InvalidParameterException e) {
            // pass
        }
    }

    /**
     * Test correct outputs of versionToMacLength.
     * 
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testVersionToMacLength0() throws UnkownProtocolVersionException {
        assertThat(KeyStringParser.versionToMacLength((byte) 1), is((short) (160 / 8)));
        assertThat(KeyStringParser.versionToMacLength((byte) 2), is((short) (256 / 8)));
        assertThat(KeyStringParser.versionToMacLength((byte) 3), is((short) (384 / 8)));
    }

    /**
     * Test correct fail of versionToMacLength on unknown values.
     */
    @Test
    public final void testVersionToMacLength1() {
        try {
            KeyStringParser.versionToMacLength((byte) 0);
            fail("This should not be accpeted");
        } catch (UnkownProtocolVersionException e) {
            // pass
        }

    }

    /**
     * Test correct outputs of macLengthToVersion.
     * 
     * @throws UnkownProtocolVersionException
     *             Should not be thrown
     */
    @Test
    public final void testMacLengthToVersion0() throws UnkownProtocolVersionException {
        assertThat(KeyStringParser.macLengthToVersion(160 / 8), is((byte) 1));
        assertThat(KeyStringParser.macLengthToVersion(256 / 8), is((byte) 2));
        assertThat(KeyStringParser.macLengthToVersion(384 / 8), is((byte) 3));
    }

    /**
     * Test correct fail of macLengthToVersion on unknown values.
     */
    @Test
    public final void testMacLengthToVersion1() {
        try {
            KeyStringParser.macLengthToVersion(512 / 8);
            fail("This should not be accpeted");
        } catch (UnkownProtocolVersionException e) {
            // pass
        }

    }

}
