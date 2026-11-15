package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

/**
 * Unit tests for {@link UUIDConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Conversion of a {@link UUID} to {@link BigInteger} and {@link BigDecimal}</li>
 *   <li>Handling of {@code null} input (expected {@link NullPointerException})</li>
 *   <li>Conversion of a {@link UUID} to a {@link Map} with the correct key/value</li>
 *   <li>Behaviour when a non‑UUID object is supplied to {@code toMap}</li>
 *   <li>Edge cases such as UUIDs with leading zeros</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, therefore a minimal
 * {@code Converter} instance (created with default options) is passed to each method.
 */
public class UUIDConversionsTest {

    // -------------------------------------------------------------------------
    // Helper – a Converter instance (its behaviour is irrelevant for the tests)
    // -------------------------------------------------------------------------
    private static final Converter DUMMY_CONVERTER = new Converter(new ConverterOptions());

    // -------------------------------------------------------------------------
    // toBigInteger / toBigDecimal tests
    // -------------------------------------------------------------------------

    @Test
    public void testToBigInteger_WithStandardUuid() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        // Expected hex string without hyphens
        String expectedHex = "123e4567e89b12d3a456426655440000";
        BigInteger expected = new BigInteger(expectedHex, 16);

        BigInteger actual = UUIDConversions.toBigInteger(uuid, DUMMY_CONVERTER);
        assertEquals("BigInteger conversion did not match expected value", expected, actual);
    }

    @Test
    public void testToBigDecimal_WithStandardUuid() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        String expectedHex = "123e4567e89b12d3a456426655440000";
        BigInteger expectedInteger = new BigInteger(expectedHex, 16);
        BigDecimal expected = new BigDecimal(expectedInteger);

        BigDecimal actual = UUIDConversions.toBigDecimal(uuid, DUMMY_CONVERTER);
        assertEquals("BigDecimal conversion did not match expected value", expected, actual);
    }

    @Test
    public void testToBigInteger_WithLeadingZeros() {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String expectedHex = "00000000000000000000000000000001";
        BigInteger expected = new BigInteger(expectedHex, 16);

        BigInteger actual = UUIDConversions.toBigInteger(uuid, DUMMY_CONVERTER);
        assertEquals("BigInteger conversion for UUID with leading zeros failed", expected, actual);
    }

    @Test
    public void testToBigDecimal_WithLeadingZeros() {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
        BigInteger expectedInteger = new BigInteger("00000000000000000000000000000001", 16);
        BigDecimal expected = new BigDecimal(expectedInteger);

        BigDecimal actual = UUIDConversions.toBigDecimal(uuid, DUMMY_CONVERTER);
        assertEquals("BigDecimal conversion for UUID with leading zeros failed", expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        UUIDConversions.toBigInteger(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        UUIDConversions.toBigDecimal(null, DUMMY_CONVERTER);
    }

    @Test
    public void testToBigInteger_WithStringRepresentingUuid() {
        // The method works with any object whose toString() returns a UUID‑like string.
        String uuidString = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6";
        BigInteger expected = new BigInteger(uuidString.replace("-", ""), 16);

        BigInteger actual = UUIDConversions.toBigInteger(uuidString, DUMMY_CONVERTER);
        assertEquals("BigInteger conversion from String failed", expected, actual);
    }

    // -------------------------------------------------------------------------
    // toMap tests
    // -------------------------------------------------------------------------

    @Test
    public void testToMap_ValidUuid() {
        UUID uuid = UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6");
        Map<String, Object> map = UUIDConversions.toMap(uuid, DUMMY_CONVERTER);

        assertNotNull("Resulting map should not be null", map);
        assertEquals("Map should contain exactly one entry", 1, map.size());

        // The key is defined by MapConversions.UUID (a public constant in the library)
        assertTrue("Map should contain the UUID key", map.containsKey(MapConversions.UUID));
        assertEquals("Stored UUID string does not match original", uuid.toString(), map.get(MapConversions.UUID));
    }

    @Test
    public void testToMap_ReturnedMapIsModifiableAndIndependent() {
        UUID uuid = UUID.randomUUID();
        Map<String, Object> map = UUIDConversions.toMap(uuid, DUMMY_CONVERTER);

        // Modify the map – it should not affect the original UUID object
        map.put("extraKey", "extraValue");
        assertEquals("Original map should now have two entries", 2, map.size());
        assertEquals("Extra value not stored correctly", "extraValue", map.get("extraKey"));

        // Ensure the UUID entry is still correct
        assertEquals(uuid.toString(), map.get(MapConversions.UUID));
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_NonUuidInput() {
        // Passing a String (or any non‑UUID) should cause a ClassCastException
        UUIDConversions.toMap("not-a-uuid", DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        UUIDConversions.toMap(null, DUMMY_CONVERTER);
    }
}
