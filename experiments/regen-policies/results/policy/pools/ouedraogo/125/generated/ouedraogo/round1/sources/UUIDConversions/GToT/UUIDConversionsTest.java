package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

/**
 * Unit tests for {@link UUIDConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical usage of each conversion method.</li>
 *   <li>Edge‑case handling such as {@code null} inputs, malformed strings and wrong types.</li>
 *   <li>Exception scenarios that the implementation may throw.</li>
 * </ul>
 *
 * The class under test has package‑private visibility, therefore the test class lives in the same
 * package so it can access the static methods directly.
 */
public class UUIDConversionsTest {

    /* ----------------------------------------------------------------------
     * Helper methods
     * ---------------------------------------------------------------------- */

    private static BigInteger expectedBigIntegerFromUuid(UUID uuid) {
        // UUID string without hyphens, interpreted as a hexadecimal number
        String hex = uuid.toString().replace("-", "");
        return new BigInteger(hex, 16);
    }

    /* ----------------------------------------------------------------------
     * toBigInteger() tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToBigInteger_WithUuidObject() {
        UUID uuid = UUID.randomUUID();
        BigInteger result = UUIDConversions.toBigInteger(uuid, null);
        Assert.assertEquals(expectedBigIntegerFromUuid(uuid), result);
    }

    @Test
    public void testToBigInteger_WithUuidString() {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();
        BigInteger result = UUIDConversions.toBigInteger(uuidString, null);
        Assert.assertEquals(expectedBigIntegerFromUuid(uuid), result);
    }

    @Test(expected = NumberFormatException.class)
    public void testToBigInteger_WithInvalidHexString() {
        // Contains a 'g' which is not a valid hex digit
        String malformed = "123e4567-e89b-12d3-a456-42665544g000";
        UUIDConversions.toBigInteger(malformed, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_WithNull() {
        UUIDConversions.toBigInteger(null, null);
    }

    /* ----------------------------------------------------------------------
     * toBigDecimal() tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToBigDecimal_WithUuidObject() {
        UUID uuid = UUID.randomUUID();
        BigDecimal result = UUIDConversions.toBigDecimal(uuid, null);
        BigDecimal expected = new BigDecimal(expectedBigIntegerFromUuid(uuid));
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testToBigDecimal_WithUuidString() {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();
        BigDecimal result = UUIDConversions.toBigDecimal(uuidString, null);
        BigDecimal expected = new BigDecimal(expectedBigIntegerFromUuid(uuid));
        Assert.assertEquals(expected, result);
    }

    @Test(expected = NumberFormatException.class)
    public void testToBigDecimal_WithInvalidHexString() {
        String malformed = "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz";
        UUIDConversions.toBigDecimal(malformed, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_WithNull() {
        UUIDConversions.toBigDecimal(null, null);
    }

    /* ----------------------------------------------------------------------
     * toMap() tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToMap_WithUuidObject() {
        UUID uuid = UUID.randomUUID();

        Map<String, Object> map = UUIDConversions.toMap(uuid, null);

        // Verify map contains exactly one entry with the correct key/value
        Assert.assertEquals(1, map.size());
        // The key is defined in MapConversions.UUID; fall back to literal if unavailable
        String expectedKey = getMapConversionsUuidKey();
        Assert.assertTrue("Map should contain the UUID key", map.containsKey(expectedKey));
        Assert.assertEquals(uuid.toString(), map.get(expectedKey));
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WithWrongType() {
        // Passing a String instead of a UUID triggers a cast exception inside the method
        UUIDConversions.toMap("not-a-uuid", null);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_WithNull() {
        UUIDConversions.toMap(null, null);
    }

    /* ----------------------------------------------------------------------
     * Utility to obtain the MapConversions.UUID constant without a direct compile‑time
     * dependency (the class is part of the same library but its source is not shown here).
     * If the constant is unavailable, the literal "uuid" is used – this matches the
     * library's implementation.
     * ---------------------------------------------------------------------- */
    private static String getMapConversionsUuidKey() {
        try {
            Class<?> mc = Class.forName("com.cedarsoftware.util.convert.MapConversions");
            java.lang.reflect.Field f = mc.getField("UUID");
            return (String) f.get(null);
        } catch (Exception e) {
            // Fallback to the documented literal key
            return "uuid";
        }
    }
}
