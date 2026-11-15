package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

/**
 * JUnit 4 test suite for {@link UUIDConversions}.
 * The tests are placed in the same package to gain access to the
 * package‑private static methods.
 */
public class UUIDConversionsTest {

    // -----------------------------------------------------------------------
    // Helper constants
    // -----------------------------------------------------------------------
    private static final String UUID_STRING = "123e4567-e89b-12d3-a456-426655440000";
    private static final String UUID_STRING_NO_HYPHENS = "123e4567e89b12d3a456426655440000";
    private static final String UUID_STRING_UPPER = "123E4567-E89B-12D3-A456-426655440000";

    // -----------------------------------------------------------------------
    // Public method list (extracted from UUIDConversions)
    // -----------------------------------------------------------------------
    // static BigDecimal toBigDecimal(Object from, Converter converter)
    // static BigInteger toBigInteger(Object from, Converter converter)
    // static Map<String, Object> toMap(Object from, Converter converter)

    // -----------------------------------------------------------------------
    // Basic functionality tests
    // -----------------------------------------------------------------------

    @Test
    public void testToBigInteger_WithHyphens() {
        BigInteger expected = new BigInteger(UUID_STRING_NO_HYPHENS, 16);
        BigInteger actual = UUIDConversions.toBigInteger(UUID_STRING, null);
        assertEquals("BigInteger conversion should ignore hyphens", expected, actual);
    }

    @Test
    public void testToBigInteger_WithoutHyphens() {
        BigInteger expected = new BigInteger(UUID_STRING_NO_HYPHENS, 16);
        BigInteger actual = UUIDConversions.toBigInteger(UUID_STRING_NO_HYPHENS, null);
        assertEquals("BigInteger conversion should work when hyphens are absent", expected, actual);
    }

    @Test
    public void testToBigInteger_UpperCaseHex() {
        BigInteger expected = new BigInteger(UUID_STRING_NO_HYPHENS, 16);
        BigInteger actual = UUIDConversions.toBigInteger(UUID_STRING_UPPER, null);
        assertEquals("Hex parsing must be case‑insensitive", expected, actual);
    }

    @Test
    public void testToBigDecimal_Basic() {
        BigInteger bi = new BigInteger(UUID_STRING_NO_HYPHENS, 16);
        BigDecimal expected = new BigDecimal(bi);
        BigDecimal actual = UUIDConversions.toBigDecimal(UUID_STRING, null);
        assertEquals("BigDecimal should be created from the BigInteger representation", expected, actual);
    }

    @Test
    public void testToMap_WithValidUUID() {
        UUID uuid = UUID.fromString(UUID_STRING);
        Map<String, Object> map = UUIDConversions.toMap(uuid, null);
        assertNotNull("Resulting map must not be null", map);
        assertEquals("Map should contain exactly one entry", 1, map.size());

        // The constant used inside UUIDConversions is MapConversions.UUID.
        // It is known to be the literal string "UUID".
        assertTrue("Map must contain the key \"UUID\"", map.containsKey("UUID"));
        assertEquals("Stored value must be the string representation of the UUID",
                uuid.toString(), map.get("UUID"));
    }

    // -----------------------------------------------------------------------
    // Edge‑case and exception handling tests
    // -----------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        UUIDConversions.toBigInteger(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        UUIDConversions.toBigDecimal(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        UUIDConversions.toMap(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_InvalidObjectType() {
        // Pass a plain String instead of a UUID – the method casts to UUID internally.
        UUIDConversions.toMap("not-a-uuid", null);
    }

    // Additional edge case: very short/invalid hex string – should still produce a BigInteger
    // (BigInteger will throw NumberFormatException for non‑hex characters).
    @Test(expected = NumberFormatException.class)
    public void testToBigInteger_InvalidHexCharacters() {
        UUIDConversions.toBigInteger("zzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz", null);
    }
}
