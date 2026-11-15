package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * JUnit 4 test suite for {@link ByteUtilities}.
 *
 * Covers:
 *  - Normal functionality of all public static methods.
 *  - Edge‑case handling and expected exceptions.
 *  - Verification that the class cannot be instantiated.
 */
public class ByteUtilitiesTest {

    /* ----------------------------------------------------------------------
     *  toHexChar()
     * ---------------------------------------------------------------------- */

    @Test
    public void testToHexCharBasic() {
        assertEquals('0', ByteUtilities.toHexChar(0));
        assertEquals('A', ByteUtilities.toHexChar(10));
        assertEquals('F', ByteUtilities.toHexChar(15));
    }

    @Test
    public void testToHexCharMasking() {
        // Values outside 0‑15 are masked with 0x0F
        assertEquals('F', ByteUtilities.toHexChar(-1));      // 0xFFFFFFFF -> low 4 bits = 0xF
        assertEquals('0', ByteUtilities.toHexChar(16));      // 0x10 -> low 4 bits = 0x0
        assertEquals('F', ByteUtilities.toHexChar(255));     // 0xFF -> low 4 bits = 0xF
        assertEquals('5', ByteUtilities.toHexChar(0x25));    // low 4 bits = 0x5
    }

    /* ----------------------------------------------------------------------
     *  encode()
     * ---------------------------------------------------------------------- */

    @Test
    public void testEncodeBasic() {
        byte[] data = { (byte)0x1F, (byte)0x8B, (byte)0x3C };
        assertEquals("1F8B3C", ByteUtilities.encode(data));
    }

    @Test
    public void testEncodeEmptyArray() {
        assertEquals("", ByteUtilities.encode(new byte[0]));
    }

    @Test(expected = NullPointerException.class)
    public void testEncodeNullArray() {
        ByteUtilities.encode(null);
    }

    @Test
    public void testEncodeNegativeBytes() {
        byte[] data = { (byte)-1, (byte)-128, (byte)0 };
        // -1 -> 0xFF, -128 -> 0x80, 0 -> 0x00
        assertEquals("FF8000", ByteUtilities.encode(data));
    }

    /* ----------------------------------------------------------------------
     *  decode()
     * ---------------------------------------------------------------------- */

    @Test
    public void testDecodeBasic() {
        String hex = "1F8B3C";
        byte[] expected = { (byte)0x1F, (byte)0x8B, (byte)0x3C };
        assertArrayEquals(expected, ByteUtilities.decode(hex));
    }

    @Test
    public void testDecodeEmptyString() {
        assertArrayEquals(new byte[0], ByteUtilities.decode(""));
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        assertNull(ByteUtilities.decode("ABC"));
    }

    @Test
    public void testDecodeInvalidCharactersReturnNull() {
        assertNull(ByteUtilities.decode("GG"));
        assertNull(ByteUtilities.decode("12Z4"));
    }

    @Test
    public void testDecodeCharactersBeyondAsciiReturnNull() {
        // Unicode characters with code point > 127
        String s = "\u0100\u0101"; // both > 127
        assertNull(ByteUtilities.decode(s));
    }

    @Test
    public void testDecodeMixedCaseIsAccepted() {
        String mixed = "aBcD";
        byte[] expected = { (byte)0xAB, (byte)0xCD };
        assertArrayEquals(expected, ByteUtilities.decode(mixed));
    }

    @Test(expected = NullPointerException.class)
    public void testDecodeNullStringThrowsNPE() {
        ByteUtilities.decode(null);
    }

    /* ----------------------------------------------------------------------
     *  isGzipped()
     * ---------------------------------------------------------------------- */

    @Test
    public void testIsGzippedTrue() {
        byte[] gz = { (byte)0x1F, (byte)0x8B, (byte)0x00 };
        assertTrue(ByteUtilities.isGzipped(gz));
    }

    @Test
    public void testIsGzippedFalseWrongMagic() {
        byte[] notGz = { (byte)0x1F, (byte)0x00 };
        assertFalse(ByteUtilities.isGzipped(notGz));
    }

    @Test
    public void testIsGzippedFalseTooShort() {
        byte[] shortArray = { (byte)0x1F };
        assertFalse(ByteUtilities.isGzipped(shortArray));
    }

    @Test
    public void testIsGzippedFalseNull() {
        assertFalse(ByteUtilities.isGzipped(null));
    }

    /* ----------------------------------------------------------------------
     *  Private constructor – ensure utility class cannot be instantiated
     * ---------------------------------------------------------------------- */

    @Test
    public void testConstructorIsPrivate() throws Exception {
        Constructor<ByteUtilities> ctor = ByteUtilities.class.getDeclaredConstructor();
        assertTrue("Constructor should be private", Modifier.isPrivate(ctor.getModifiers()));
        ctor.setAccessible(true);
        // The constructor does nothing; just ensure we can invoke it without side‑effects.
        ctor.newInstance();
    }
}
