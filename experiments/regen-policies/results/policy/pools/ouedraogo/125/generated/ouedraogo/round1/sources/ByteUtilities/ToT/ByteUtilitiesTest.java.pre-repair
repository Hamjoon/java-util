package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.lang.reflect.Method;

/**
 * JUnit‑4 test suite for {@link ByteUtilities}.
 *
 * <p>The tests cover the public API (decode, encode, isGzipped) as well as the
 * private {@code convertDigit} helper via reflection.  Both typical usage
 * scenarios and edge‑case/error conditions are exercised.</p>
 */
public class ByteUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /*  decode() tests                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDecodeTypicalUpperCase() {
        byte[] expected = new byte[] {(byte)0x1F, (byte)0x8B, (byte)0x3C};
        Assert.assertArrayEquals(expected, ByteUtilities.decode("1F8B3C"));
    }

    @Test
    public void testDecodeTypicalMixedCase() {
        byte[] expected = new byte[] {(byte)0xAB, (byte)0xCD, (byte)0xEF};
        Assert.assertArrayEquals(expected, ByteUtilities.decode("aBcDeF"));
    }

    @Test
    public void testDecodeEmptyString() {
        byte[] result = ByteUtilities.decode("");
        Assert.assertNotNull("Result should never be null for empty input", result);
        Assert.assertEquals("Empty input must produce zero‑length array", 0, result.length);
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        Assert.assertNull("Odd‑length hex string must return null", ByteUtilities.decode("ABC"));
    }

    @Test
    public void testDecodeInvalidCharactersBehaviour() {
        // According to the Javadoc invalid chars should yield null,
        // but the current implementation does NOT validate them.
        // The test documents the actual behaviour.
        byte[] result = ByteUtilities.decode("ZZ");
        // Character.digit('Z',16) == -1, so hi = lo = -1 -> (byte)(-17) == 0xEF
        byte[] expected = new byte[] {(byte)0xEF};
        Assert.assertArrayEquals("Invalid hex characters are interpreted as -1", expected, result);
    }

    @Test(expected = NullPointerException.class)
    public void testDecodeNullThrowsNPE() {
        ByteUtilities.decode(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  encode() tests                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEncodeTypical() {
        byte[] data = new byte[] {(byte)0x00, (byte)0x7F, (byte)0x80, (byte)0xFF};
        String expected = "007F80FF";
        Assert.assertEquals(expected, ByteUtilities.encode(data));
    }

    @Test
    public void testEncodeEmptyArray() {
        Assert.assertEquals("Empty byte array must yield empty string",
                "", ByteUtilities.encode(new byte[0]));
    }

    @Test
    public void testEncodeNegativeBytes() {
        // -1 == 0xFF, -128 == 0x80
        byte[] data = new byte[] {(byte)-1, (byte)-128};
        String expected = "FF80";
        Assert.assertEquals(expected, ByteUtilities.encode(data));
    }

    /* ---------------------------------------------------------------------- */
    /*  convertDigit() private helper (via reflection)                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConvertDigitAllValues() throws Exception {
        Method m = ByteUtilities.class.getDeclaredMethod("convertDigit", int.class);
        m.setAccessible(true);
        char[] expected = {
                '0','1','2','3','4','5','6','7',
                '8','9','A','B','C','D','E','F'
        };
        for (int i = 0; i < 16; i++) {
            char actual = (char) m.invoke(null, i);
            Assert.assertEquals("Digit " + i + " should map to " + expected[i],
                    expected[i], actual);
        }
        // values outside 0‑15 are masked with 0x0F, e.g. 16 -> 0
        Assert.assertEquals('0', (char) m.invoke(null, 16));
        Assert.assertEquals('F', (char) m.invoke(null, 31));
    }

    /* ---------------------------------------------------------------------- */
    /*  isGzipped() tests                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsGzippedTrue() {
        byte[] gzipHeader = new byte[] {(byte)0x1F, (byte)0x8B, 0x00, 0x01};
        Assert.assertTrue(ByteUtilities.isGzipped(gzipHeader));
    }

    @Test
    public void testIsGzippedFalseDifferentHeader() {
        byte[] notGzip = new byte[] {(byte)0x1F, (byte)0x00, 0x00};
        Assert.assertFalse(ByteUtilities.isGzipped(notGzip));
    }

    @Test
    public void testIsGzippedFalseTooShort() {
        byte[] oneByte = new byte[] {(byte)0x1F};
        Assert.assertFalse(ByteUtilities.isGzipped(oneByte));
    }

    @Test
    public void testIsGzippedEmptyArray() {
        Assert.assertFalse(ByteUtilities.isGzipped(new byte[0]));
    }
}
