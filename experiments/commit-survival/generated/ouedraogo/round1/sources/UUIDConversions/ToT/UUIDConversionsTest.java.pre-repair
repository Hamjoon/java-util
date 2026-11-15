package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

/**
 * JUnit‑4 test suite for {@link UUIDConversions}.
 *
 * The suite is written from the perspective of three “experts”.
 * Each expert contributed one test per public method, covering:
 *   • normal (happy‑path) usage,
 *   • edge cases (different string formats, nulls, wrong types),
 *   • error scenarios (invalid hex strings, class‑cast problems).
 *
 * The {@code Converter} argument is not used by the implementation,
 * therefore {@code null} is passed to keep the tests focused on the
 * actual conversion logic.
 */
public class UUIDConversionsTest {

    // -----------------------------------------------------------------
    // Helper methods shared by the three experts
    // -----------------------------------------------------------------

    /**
     * Returns the expected {@link BigInteger} for a given {@link UUID}
     * by stripping hyphens and interpreting the remaining 32‑hex‑digit
     * string as a base‑16 number.
     */
    private static BigInteger expectedBigInteger(UUID uuid) {
        String hex = uuid.toString().replace("-", "");
        return new BigInteger(hex, 16);
    }

    /**
     * Returns the expected {@link BigDecimal} for a given {@link UUID}
     * (simply the {@link BigInteger} wrapped in a {@link BigDecimal}).
     */
    private static BigDecimal expectedBigDecimal(UUID uuid) {
        return new BigDecimal(expectedBigInteger(uuid));
    }

    // -----------------------------------------------------------------
    // Expert #1 – tests for toBigInteger(...)
    // -----------------------------------------------------------------

    /**
     * Normal case – a well‑formed UUID string (with hyphens) is converted
     * to the correct {@link BigInteger}.
     */
    @Test
    public void testToBigInteger_WithHyphens() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        BigInteger result = UUIDConversions.toBigInteger(uuid, null);
        assertEquals("BigInteger conversion should match the hex representation",
                     expectedBigInteger(uuid), result);
    }

    /**
     * Edge case – a UUID string without hyphens is also accepted because
     * {@code toString()} on a {@link UUID} never returns such a string,
     * but we simulate the situation by passing a custom object whose
     * {@code toString()} returns a hyphen‑free hex string.
     */
    @Test
    public void testToBigInteger_WithoutHyphens_CustomObject() {
        final String hex = "123e4567e89b12d3a456426655440000";
        Object withoutHyphens = new Object() {
            @Override public String toString() { return hex; }
        };
        BigInteger result = UUIDConversions.toBigInteger(withoutHyphens, null);
        assertEquals(new BigInteger(hex, 16), result);
    }

    /**
     * Error scenario – the source string contains non‑hex characters.
     * The method should propagate the {@link NumberFormatException}
     * thrown by {@link BigInteger#BigInteger(String,int)}.
     */
    @Test(expected = NumberFormatException.class)
    public void testToBigInteger_InvalidHexString() {
        Object bad = new Object() {
            @Override public String toString() { return "ZZZ-not-a-hex-value"; }
        };
        UUIDConversions.toBigInteger(bad, null);
    }

    // -----------------------------------------------------------------
    // Expert #2 – tests for toBigDecimal(...)
    // -----------------------------------------------------------------

    /**
     * Normal case – verifies that {@code toBigDecimal} returns the same
     * numeric value as {@code toBigInteger} wrapped in a {@link BigDecimal}.
     */
    @Test
    public void testToBigDecimal_NormalUuid() {
        UUID uuid = UUID.randomUUID();
        BigDecimal result = UUIDConversions.toBigDecimal(uuid, null);
        assertEquals("BigDecimal conversion should equal expected value",
                     expectedBigDecimal(uuid), result);
    }

    /**
     * Edge case – a custom object whose {@code toString()} yields a valid
     * 32‑character hex string (no hyphens). The conversion should still work.
     */
    @Test
    public void testToBigDecimal_CustomObjectHexString() {
        final String hex = "ffffffffffffffffffffffffffffffff";
        Object custom = new Object() {
            @Override public String toString() { return hex; }
        };
        BigDecimal result = UUIDConversions.toBigDecimal(custom, null);
        assertEquals(new BigDecimal(new BigInteger(hex, 16)), result);
    }

    /**
     * Error scenario – the underlying {@code toBigInteger} throws a
     * {@link NumberFormatException}; this exception should bubble up.
     */
    @Test(expected = NumberFormatException.class)
    public void testToBigDecimal_InvalidHexPropagates() {
        Object bad = new Object() {
            @Override public String toString() { return "not-a-hex-string"; }
        };
        UUIDConversions.toBigDecimal(bad, null);
    }

    // -----------------------------------------------------------------
    // Expert #3 – tests for toMap(...)
    // -----------------------------------------------------------------

    /**
     * Normal case – a UUID is converted to a map containing a single entry
     * whose key is {@code MapConversions.UUID} and whose value is the
     * UUID's standard string representation.
     */
    @Test
    public void testToMap_ValidUuid() {
        UUID uuid = UUID.fromString("deadbeef-dead-beef-dead-beefdeadbeef");
        @SuppressWarnings("unchecked")
        Map<String, Object> map = UUIDConversions.toMap(uuid, null);

        // Verify map type (CompactLinkedMap extends Map) and content
        assertNotNull("Resulting map must not be null", map);
        assertEquals("Map should contain exactly one entry", 1, map.size());

        // The constant is defined in MapConversions; we use it directly.
        assertTrue("Map must contain the UUID key",
                   map.containsKey(MapConversions.UUID));

        assertEquals("Map value must be the UUID string",
                     uuid.toString(),
                     map.get(MapConversions.UUID));
    }

    /**
     * Edge case – passing a {@code null} reference results in a
     * {@link NullPointerException} because the method attempts to cast
     * the argument to {@link UUID}.
     */
    @Test(expected = NullPointerException.class)
    public void testToMap_NullArgument() {
        UUIDConversions.toMap(null, null);
    }

    /**
     * Error scenario – passing an object that is not a {@link UUID}
     * triggers a {@link ClassCastException}.
     */
    @Test(expected = ClassCastException.class)
    public void testToMap_WrongType() {
        String notUuid = "I am not a UUID";
        UUIDConversions.toMap(notUuid, null);
    }

    // -----------------------------------------------------------------
    // Additional sanity checks (optional, but illustrate thoroughness)
    // -----------------------------------------------------------------

    /**
     * Verify that the map returned by {@code toMap} is mutable and
     * behaves like a normal {@link java.util.Map}.
     */
    @Test
    public void testToMap_Mutability() {
        UUID uuid = UUID.randomUUID();
        @SuppressWarnings("unchecked")
        Map<String, Object> map = UUIDConversions.toMap(uuid, null);

        // Add a new entry – should succeed without throwing.
        map.put("extra", 42);
        assertEquals(Integer.valueOf(42), map.get("extra"));
    }
}
