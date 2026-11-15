package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ByteUtilities}.
 *
 * <p>This test class covers:
 * <ul>
 *   <li>All public static methods (decode, encode, isGzipped)</li>
 *   <li>Typical usage scenarios</li>
 *   <li>Edge‑case handling (empty inputs, nulls, odd length strings, invalid characters, short byte arrays)</li>
 *   <li>Behaviour that differs from the Javadoc contract (invalid hex characters)</li>
 * </ul>
 * </p>
 */
public class ByteUtilitiesTest {

    /* -------------------------------------------------
     *  decode(String) – normal / happy‑path cases
     * ------------------------------------------------- */

    @Test
    public void testDecodeValidHexEvenLength() {
        byte[] expected = new byte[] { 0x1F, (byte)0x8B, 0x3C };
        assertArrayEquals(expected, ByteUtilities.decode("1F8B3C"));
    }

    @Test
    public void testDecodeAllFHex() {
        byte[] expected = new byte[] { (byte)0xFF, (byte)0xFF };
        assertArrayEquals(expected, ByteUtilities.decode("FFFF"));
    }

    @Test
    public void testDecodeEmptyStringReturnsEmptyArray() {
        byte[] result = ByteUtilities.decode("");
        assertNotNull("Result must not be null for empty input", result);
        assertEquals("Empty string should produce zero‑length array", 0, result.length);
    }

    /* -------------------------------------------------
     *  decode(String) – error / edge cases
     * ------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testDecodeNullStringThrowsNPE() {
        ByteUtilities.decode(null);
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        assertNull("String with odd length must return null", ByteUtilities.decode("ABC"));
    }

    /**
     * The Javadoc says that non‑hex characters should cause a {@code null} return,
     * but the current implementation simply feeds {@code Character.digit} results
     * (‑1) into the arithmetic, producing a byte value of {@code -17}.
     * This test documents the actual behaviour.
     */
    @Test
    public void testDecodeInvalidCharactersProducesByteMinus17() {
        byte[] result = ByteUtilities.decode("ZZ");
        assertNotNull("Result should not be null for non‑hex characters (implementation detail)", result);
        assertEquals("Result length must be 1 (two chars → one byte)", 1, result.length);
        assertEquals("Byte value for invalid hex chars should be -17 (0xEF)", (byte) -17, result[0]);
    }

    /* -------------------------------------------------
     *  encode(byte[]) – normal / happy‑path cases
     * ------------------------------------------------- */

    @Test
    public void testEncodeTypicalArray() {
        byte[] data = new byte[] { 0x1F, (byte)0x8B, 0x3C };
        assertEquals("1F8B3C", ByteUtilities.encode(data));
    }

    @Test
    public void testEncodeAllNegativeBytes() {
        byte[] data = new byte[] { (byte)0xFF, (byte)0x00, 0x7F };
        assertEquals("FF007F", ByteUtilities.encode(data));
    }

    @Test
    public void testEncodeEmptyArrayReturnsEmptyString() {
        assertEquals("", ByteUtilities.encode(new byte[0]));
    }

    /* -------------------------------------------------
     *  isGzipped(byte[]) – normal / happy‑path cases
     * ------------------------------------------------- */

    @Test
    public void testIsGzippedTrueForValidHeader() {
        byte[] gzipped = new byte[] { 0x1F, (byte)0x8B, 0x00, 0x01 };
        assertTrue("Valid GZIP header must be recognised", ByteUtilities.isGzipped(gzipped));
    }

    @Test
    public void testIsGzippedFalseWhenHeaderIsWrong() {
        byte[] notGzipped = new byte[] { 0x00, 0x00 };
        assertFalse("Incorrect header must return false", ByteUtilities.isGzipped(notGzipped));
    }

    @Test
    public void testIsGzippedFalseForShortArray() {
        byte[] oneByte = new byte[] { 0x1F };
        assertFalse("Array shorter than 2 bytes cannot be GZIP", ByteUtilities.isGzipped(oneByte));
    }

    @Test
    public void testIsGzippedFalseForNullArray() {
        assertFalse("Null array must be treated as non‑gzip", ByteUtilities.isGzipped(null));
    }
}
