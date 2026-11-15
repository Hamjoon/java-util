package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import org.junit.Test;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;

/**
 * Unit tests for {@link UUIDConversions}.
 */
public class UUIDConversionsTest {

    /** Helper to create a dummy {@link Converter} instance. The methods under test never use the
     *  converter, so passing {@code null} is safe and keeps the test independent from the
     *  Converter implementation. */
    private static final Converter DUMMY_CONVERTER = null;

    @Test
    public void testToBigInteger_WithStandardUuid() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        // Expected hex without dashes
        String expectedHex = "123e4567e89b12d3a456426614174000";
        BigInteger expected = new BigInteger(expectedHex, 16);

        BigInteger actual = UUIDConversions.toBigInteger(uuid, DUMMY_CONVERTER);
        Assert.assertEquals("BigInteger conversion from UUID failed", expected, actual);
    }

    @Test
    public void testToBigInteger_WithUuidHavingLeadingZeros() {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
        BigInteger expected = BigInteger.ONE; // hex "1"
        BigInteger actual = UUIDConversions.toBigInteger(uuid, DUMMY_CONVERTER);
        Assert.assertEquals("BigInteger conversion should handle leading zeros", expected, actual);
    }

    @Test
    public void testToBigInteger_WithStringInput() {
        // The method accepts any Object; using a String that looks like a UUID should work.
        String uuidString = "ffffffff-ffff-ffff-ffff-ffffffffffff";
        String expectedHex = "ffffffffffffffffffffffffffffffff";
        BigInteger expected = new BigInteger(expectedHex, 16);

        BigInteger actual = UUIDConversions.toBigInteger(uuidString, DUMMY_CONVERTER);
        Assert.assertEquals("BigInteger conversion from String failed", expected, actual);
    }

    @Test
    public void testToBigDecimal_DelegatesToBigInteger() {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        // hex "a" => decimal 10
        BigDecimal expected = new BigDecimal(BigInteger.TEN);
        BigDecimal actual = UUIDConversions.toBigDecimal(uuid, DUMMY_CONVERTER);
        Assert.assertEquals("BigDecimal conversion should match BigInteger conversion", expected, actual);
    }

    @Test
    public void testToMap_ReturnsExpectedStructure() {
        UUID uuid = UUID.randomUUID();
        Map<String, Object> result = UUIDConversions.toMap(uuid, DUMMY_CONVERTER);

        // Verify the map implementation type (CompactLinkedMap) is used
        Assert.assertTrue("Resulting map should be an instance of CompactLinkedMap",
                result instanceof CompactLinkedMap);

        // Verify key/value
        Assert.assertEquals("Map should contain exactly one entry", 1, result.size());
        Assert.assertTrue("Map should contain the UUID key",
                result.containsKey(MapConversions.UUID));
        Assert.assertEquals("Stored UUID string should match the original",
                uuid.toString(), result.get(MapConversions.UUID));
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WithInvalidObjectThrowsClassCastException() {
        // Passing a non‑UUID object should cause a ClassCastException inside the method.
        String notAUuid = "not-a-uuid";
        UUIDConversions.toMap(notAUuid, DUMMY_CONVERTER);
    }

    @Test
    public void testToMap_IsIndependentCopy() {
        UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Map<String, Object> map = UUIDConversions.toMap(uuid, DUMMY_CONVERTER);
        // Mutate the returned map
        map.put("extra", "value");
        // Ensure the original UUID string is still correct and unchanged
        Assert.assertEquals("Original UUID entry must remain unchanged",
                uuid.toString(), map.get(MapConversions.UUID));
        Assert.assertEquals("Map should now contain two entries after mutation", 2, map.size());
    }
}
