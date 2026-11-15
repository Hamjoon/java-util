package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class ByteUtilitiesTest {

    /* -------------------------------------------------
     * Tests for decode(String)
     * ------------------------------------------------- */

    @Test
    public void testDecodeTypicalHexString() {
        // "0A1B2C" -> {0x0A, 0x1B, 0x2C}
        byte[] expected = new byte[] { (byte)0x0A, (byte)0x1B, (byte)0x2C };
        assertArrayEquals(expected, ByteUtilities.decode("0A1B2C"));
    }

    @Test
    public void testDecodeLowerCaseHexString() {
        // Lower‑case letters must be accepted as well.
        byte[] expected = new byte[] { (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF };
        assertArrayEquals(expected, ByteUtilities.decode("deadbeef"));
    }

    @Test
    public void testDecodeMixedCaseHexString() {
        // Mixed case should also work.
        byte[] expected = new byte[] { (byte)0xAB, (byte)0xCD };
        assertArrayEquals(expected, ByteUtilities.decode("aBcD"));
    }

    @Test
    public void testDecodeEmptyString() {
        // Empty string => zero‑length array (even length, no error).
        assertArrayEquals(new byte[0], ByteUtilities.decode(""));
    }

    @Test
    public void testDecodeOddLengthStringReturnsNull() {
        // Odd number of characters cannot be parsed.
        assertNull(ByteUtilities.decode("ABC"));
    }

    @Test
    public void testDecodeStringWithNonHexCharacters() {
        // Character.digit returns -1 for non‑hex characters; the method does not guard against it.
        // The resulting byte will contain (hi * 16 + lo) where hi or lo may be -1.
        // We verify the exact behaviour.
        byte[] result = ByteUtilities.decode("G1");
        // 'G' -> -1, '1' -> 1 => (-1 * 16 + 1) = -15 (0xF1)
        assertEquals(1, result.length);
        assertEquals((byte)0xF1, result[0]);
    }

    /* -------------------------------------------------
     * Tests for encode(byte[])
     * ------------------------------------------------- */

    @Test
    public void testEncodeTypicalByteArray() {
        byte[] input = new byte[] { (byte)0x0A, (byte)0x1B, (byte)0x2C };
        assertEquals("0A1B2C", ByteUtilities.encode(input));
    }

    @Test
    public void testEncodeAllPossibleNibbles() {
        // Build a byte array that covers every possible high/low nibble combination.
        byte[] input = new byte[256];
        for (int i = 0; i < 256; i++) {
            input[i] = (byte)i;
        }
        StringBuilder expected = new StringBuilder(512);
        for (int i = 0; i < 256; i++) {
            int hi = (i >> 4) & 0x0F;
            int lo = i & 0x0F;
            expected.append(convertDigitStatic(hi));
            expected.append(convertDigitStatic(lo));
        }
        assertEquals(expected.toString(), ByteUtilities.encode(input));
    }

    @Test
    public void testEncodeEmptyArray() {
        assertEquals("", ByteUtilities.encode(new byte[0]));
    }

    @Test
    public void testEncodeNegativeBytes() {
        // Negative values are just two's‑complement bytes.
        byte[] input = new byte[] { (byte)0xFF, (byte)0x80 };
        assertEquals("FF80", ByteUtilities.encode(input));
    }

    /* -------------------------------------------------
     * Tests for convertDigit(int) – exercised via encode()
     * ------------------------------------------------- */

    @Test
    public void testConvertDigitBoundaries() {
        // Directly test the private static method via reflection to ensure the mapping is correct.
        // Values outside 0‑15 are masked with & 0x0F.
        assertEquals('0', invokeConvertDigit(0));
        assertEquals('F', invokeConvertDigit(15));
        assertEquals('0', invokeConvertDigit(16)); // 16 & 0x0F == 0
        assertEquals('A', invokeConvertDigit(10));
        assertEquals('5', invokeConvertDigit(21)); // 21 & 0x0F == 5
    }

    /* -------------------------------------------------
     * Tests for isGzipped(byte[])
     * ------------------------------------------------- */

    @Test
    public void testIsGzippedTrue() {
        byte[] gzipped = new byte[] { (byte)0x1F, (byte)0x8B, 0x00, 0x01 };
        assertTrue(ByteUtilities.isGzipped(gzipped));
    }

    @Test
    public void testIsGzippedFalseDifferentHeader() {
        byte[] notGzipped = new byte[] { (byte)0x1E, (byte)0x8B };
        assertFalse(ByteUtilities.isGzipped(notGzipped));
    }

    @Test
    public void testIsGzippedFalseShortArray() {
        // According to the implementation, accessing bytes[0] and bytes[1] will throw
        // an ArrayIndexOutOfBoundsException for length < 2. We verify that behaviour.
        try {
            ByteUtilities.isGzipped(new byte[0]);
            fail("Expected ArrayIndexOutOfBoundsException for empty array");
        } catch (ArrayIndexOutOfBoundsException expected) {
            // expected
        }

        try {
            ByteUtilities.isGzipped(new byte[1]);
            fail("Expected ArrayIndexOutOfBoundsException for single‑byte array");
        } catch (ArrayIndexOutOfBoundsException expected) {
            // expected
        }
    }

    /* -------------------------------------------------
     * Helper methods for reflection access
     * ------------------------------------------------- */

    private static char invokeConvertDigit(int value) {
        try {
            java.lang.reflect.Method m = ByteUtilities.class.getDeclaredMethod("convertDigit", int.class);
            m.setAccessible(true);
            return (char) m.invoke(null, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Duplicate of the private static method logic – used only for building expected strings.
    private static char convertDigitStatic(int value) {
        char[] _hex = {
                '0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        return _hex[value & 0x0F];
    }
}
