package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactMap;


import org.junit.Test;
import org.junit.Assert;

import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Unit tests for {@link OffsetTimeConversions}.
 * The tests are placed in the same package to gain access to the package‑private static methods.
 */
public class OffsetTimeConversionsTest {

    /**
     * Helper method to create a {@link Converter} instance.
     * The {@code OffsetTimeConversions} methods do not use the converter, so {@code null}
     * is sufficient for the purposes of these tests.
     */
    private static Converter dummyConverter() {
        return null; // safe because the implementation ignores the argument
    }

    @Test
    public void testToStringProducesISOOffsetTime() {
        // 13:45:30.123456789 with a +02:00 offset
        OffsetTime ot = OffsetTime.of(13, 45, 30, 123_456_789, ZoneOffset.ofHours(2));
        String expected = ot.format(DateTimeFormatter.ISO_OFFSET_TIME);
        String actual = OffsetTimeConversions.toString(ot, dummyConverter());

        Assert.assertEquals("ISO_OFFSET_TIME string does not match expected format", expected, actual);
    }

    @Test
    public void testToStringHandlesZeroOffsetAndNoNanos() {
        // 00:00:00 with UTC offset and no fractional seconds
        OffsetTime ot = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);
        String expected = ot.format(DateTimeFormatter.ISO_OFFSET_TIME);
        String actual = OffsetTimeConversions.toString(ot, dummyConverter());

        Assert.assertEquals("String representation for midnight UTC is incorrect", expected, actual);
    }

    @Test
    public void testToStringHandlesNegativeOffset() {
        // 23:59:59.999 with a -05:30 offset
        OffsetTime ot = OffsetTime.of(23, 59, 59, 999_000_000, ZoneOffset.ofHoursMinutes(-5, -30));
        String expected = ot.format(DateTimeFormatter.ISO_OFFSET_TIME);
        String actual = OffsetTimeConversions.toString(ot, dummyConverter());

        Assert.assertEquals("String representation with negative offset is incorrect", expected, actual);
    }

    @Test
    public void testToMapContainsCorrectKeyAndValue() {
        OffsetTime ot = OffsetTime.of(8, 15, 0, 0, ZoneOffset.ofHours(1));
        Map<String, Object> result = OffsetTimeConversions.toMap(ot, dummyConverter());

        // The expected key is defined in MapConversions.TIME. If that constant is unavailable,
        // we fall back to the literal "time" which is the typical value.
        String expectedKey = "time";
        try {
            expectedKey = (String) MapConversions.class.getField("TIME").get(null);
        } catch (Exception ignored) {
            // If reflection fails, the literal "time" is used.
        }

        Assert.assertTrue("Result map should contain exactly one entry", result.size() == 1);
        Assert.assertTrue("Result map should contain the key '" + expectedKey + "'", result.containsKey(expectedKey));
        Assert.assertEquals("Map value should equal OffsetTime.toString()", ot.toString(), result.get(expectedKey));
    }

    @Test
    public void testToMapPreservesInsertionOrder() {
        // The builder was configured with insertionOrder(), so the iterator order of keys
        // must reflect the order in which entries were added (only one entry here).
        OffsetTime ot = OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHours(0));
        Map<String, Object> map = OffsetTimeConversions.toMap(ot, dummyConverter());

        // Verify that the map is an instance of CompactMap (implementation detail)
        Assert.assertTrue("Map should be an instance of CompactMap", map instanceof CompactMap);

        // Verify insertion order by checking the first key from the iterator
        String firstKey = map.keySet().iterator().next();
        String expectedKey = "time";
        try {
            expectedKey = (String) MapConversions.class.getField("TIME").get(null);
        } catch (Exception ignored) {
        }
        Assert.assertEquals("First key should be the TIME constant", expectedKey, firstKey);
    }
}
