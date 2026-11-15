package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 tests for {@link UUIDConversions}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>normal (typical) usage of each conversion method,</li>
 *   <li>edge‑case inputs such as {@code null}, malformed strings, and wrong object types,</li>
 *   <li>the exceptions that the implementation is expected to throw in those situations.</li>
 * </ul>
 *
 * <p>The class under test contains only package‑private static methods, therefore the test class
 * resides in the same package so that it can call them directly.  The {@code Converter} argument
 * is not used by any of the methods, so a {@code null} reference is supplied where a {@code Converter}
 * is required.</p>
 */
public class UUIDConversionsTest {

    // -------------------------------------------------------------------------
    //  JUnit infrastructure
    // -------------------------------------------------------------------------
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // -------------------------------------------------------------------------
    //  Helper methods
    // -------------------------------------------------------------------------

    /** Returns the hexadecimal representation of a {@link UUID} without hyphens. */
    private static String uuidToHex(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    /** Creates a {@link Converter} instance required by the API.
     *  The concrete implementation of {@code ConverterOptions} is not needed for these
     *  static methods – they ignore the converter argument – so we pass {@code null}. */
    private static Converter dummyConverter() {
        return null;   // acceptable because the implementation does not dereference it
    }

    // -------------------------------------------------------------------------
    //  Tests for toBigInteger(Object, Converter)
    // -------------------------------------------------------------------------

    @Test
    public void testToBigInteger_WithValidUuidString() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        String hex = uuidToHex(uuid);
        BigInteger expected = new BigInteger(hex, 16);

        BigInteger actual = UUIDConversions.toBigInteger(uuid, dummyConverter());

        assertEquals("Conversion of a UUID string should equal the parsed hex value",
                expected, actual);
    }

    @Test
    public void testToBigInteger_WithPlainHexString() {
        String hex = "deadbeefcafebabe1234567890abcdef";
        BigInteger expected = new BigInteger(hex, 16);

        BigInteger actual = UUIDConversions.toBigInteger(hex, dummyConverter());

        assertEquals("Plain hexadecimal strings should be parsed correctly", expected, actual);
    }

    @Test
    public void testToBigInteger_WithInvalidHexString_ShouldThrowNumberFormatException() {
        thrown.expect(NumberFormatException.class);
        // contains non‑hex character 'Z'
        UUIDConversions.toBigInteger("Z12-34", dummyConverter());
    }

    @Test
    public void testToBigInteger_WithNullArgument_ShouldThrowNullPointerException() {
        thrown.expect(NullPointerException.class);
        UUIDConversions.toBigInteger(null, dummyConverter());
    }

    // -------------------------------------------------------------------------
    //  Tests for toBigDecimal(Object, Converter)
    // -------------------------------------------------------------------------

    @Test
    public void testToBigDecimal_WithValidUuidString() {
        UUID uuid = UUID.randomUUID();
        String hex = uuidToHex(uuid);
        BigDecimal expected = new BigDecimal(new BigInteger(hex, 16));

        BigDecimal actual = UUIDConversions.toBigDecimal(uuid, dummyConverter());

        assertEquals("BigDecimal conversion should delegate to BigInteger conversion",
                expected, actual);
    }

    @Test
    public void testToBigDecimal_WithPlainHexString() {
        String hex = "abcdef1234567890";
        BigDecimal expected = new BigDecimal(new BigInteger(hex, 16));

        BigDecimal actual = UUIDConversions.toBigDecimal(hex, dummyConverter());

        assertEquals("BigDecimal conversion of a hex string should be correct", expected, actual);
    }

    @Test
    public void testToBigDecimal_WithInvalidHexString_ShouldThrowNumberFormatException() {
        thrown.expect(NumberFormatException.class);
        UUIDConversions.toBigDecimal("not-a-hex", dummyConverter());
    }

    @Test
    public void testToBigDecimal_WithNullArgument_ShouldThrowNullPointerException() {
        thrown.expect(NullPointerException.class);
        UUIDConversions.toBigDecimal(null, dummyConverter());
    }

    // -------------------------------------------------------------------------
    //  Tests for toMap(Object, Converter)
    // -------------------------------------------------------------------------

    @Test
    public void testToMap_WithValidUuid() {
        UUID uuid = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
        Map<String, Object> result = UUIDConversions.toMap(uuid, dummyConverter());

        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());

        // The key is defined in MapConversions.UUID – we do not have that class here,
        // but we can verify that the map contains the UUID string as a value.
        assertTrue("Map must contain the UUID string as its value",
                result.containsValue(uuid.toString()));
    }

    @Test
    public void testToMap_WithWrongObjectType_ShouldThrowClassCastException() {
        thrown.expect(ClassCastException.class);
        // Passing a plain String instead of a UUID forces the cast inside toMap to fail.
        UUIDConversions.toMap("not-a-uuid", dummyConverter());
    }

    @Test
    public void testToMap_WithNullArgument_ShouldThrowNullPointerException() {
        thrown.expect(NullPointerException.class);
        UUIDConversions.toMap(null, dummyConverter());
    }
}
