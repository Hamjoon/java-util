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
 * The test class resides in the same package as {@code UUIDConversions}
 * so that the package‑private static methods can be accessed directly.
 */
public class UUIDConversionsTest {

    /** -----------------------------------------------------------------------
     *  Helper methods
     *  ----------------------------------------------------------------------- */
    private static final Converter DUMMY_CONVERTER = null;   // converter is not used by the methods under test

    /** -----------------------------------------------------------------------
     *  1.  List of public (actually package‑private static) methods in UUIDConversions
     *  ----------------------------------------------------------------------- */
    // static BigDecimal toBigDecimal(Object from, Converter converter)
    // static BigInteger toBigInteger(Object from, Converter converter)
    // static Map<String, Object> toMap(Object from, Converter converter)

    /** -----------------------------------------------------------------------
     *  2.  Basic functionality tests
     *  ----------------------------------------------------------------------- */

    @Test
    public void testToBigInteger_WithTypicalUuidString() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        // Expected: hex string without hyphens interpreted as base‑16
        String expectedHex = uuid.toString().replace("-", "");
        BigInteger expected = new BigInteger(expectedHex, 16);

        BigInteger actual = UUIDConversions.toBigInteger(uuid, DUMMY_CONVERTER);
        Assert.assertEquals("BigInteger conversion from UUID failed", expected, actual);
    }

    @Test
    public void testToBigDecimal_WithTypicalUuidString() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        BigInteger bi = new BigInteger(uuid.toString().replace("-", ""), 16);
        BigDecimal expected = new BigDecimal(bi);

        BigDecimal actual = UUIDConversions.toBigDecimal(uuid, DUMMY_CONVERTER);
        Assert.assertEquals("BigDecimal conversion from UUID failed", expected, actual);
    }

    @Test
    public void testToMap_WithUuid() {
        UUID uuid = UUID.randomUUID();
        Map<String, Object> map = UUIDConversions.toMap(uuid, DUMMY_CONVERTER);

        // The map must contain exactly one entry: the UUID string.
        Assert.assertEquals("Map should contain exactly one entry", 1, map.size());

        // Extract the sole key/value pair.
        String key = map.keySet().iterator().next();
        Object value = map.get(key);

        Assert.assertEquals("Map value must be the UUID string", uuid.toString(), value);
        // The key is defined in MapConversions.UUID – we do not know the literal,
        // but we can at least assert it is non‑null and non‑empty.
        Assert.assertNotNull("Map key must not be null", key);
        Assert.assertFalse("Map key must not be empty", key.isEmpty());
    }

    /** -----------------------------------------------------------------------
     *  3.  Edge‑case and exception handling tests
     *  ----------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        UUIDConversions.toBigInteger(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        UUIDConversions.toBigDecimal(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        UUIDConversions.toMap(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_NonUuidInput() {
        // Pass a plain String – the method casts to UUID and should fail.
        UUIDConversions.toMap("not-a-uuid", DUMMY_CONVERTER);
    }

    @Test
    public void testToBigInteger_WithNonUuidHexString() {
        // Any string that can be interpreted as a hex number should work.
        String hex = "deadbeefcafebabe";
        BigInteger expected = new BigInteger(hex, 16);
        BigInteger actual = UUIDConversions.toBigInteger(hex, DUMMY_CONVERTER);
        Assert.assertEquals("BigInteger conversion from pure hex string failed", expected, actual);
    }

    @Test
    public void testToBigDecimal_LargeUuid() {
        // Use the maximum possible UUID value to verify that very large numbers are handled.
        UUID maxUuid = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);
        BigInteger bi = new BigInteger(maxUuid.toString().replace("-", ""), 16);
        BigDecimal expected = new BigDecimal(bi);
        BigDecimal actual = UUIDConversions.toBigDecimal(maxUuid, DUMMY_CONVERTER);
        Assert.assertEquals("BigDecimal conversion for max UUID failed", expected, actual);
    }

    @Test
    public void testToMap_DoesNotModifyOriginalUuid() {
        UUID uuid = UUID.randomUUID();
        Map<String, Object> map = UUIDConversions.toMap(uuid, DUMMY_CONVERTER);
        // Ensure the original UUID object is unchanged.
        Assert.assertEquals("Original UUID should remain unchanged", uuid, UUID.fromString((String) map.values().iterator().next()));
    }
}
