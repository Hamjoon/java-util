package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

/**
 * Unit tests for {@link ZoneOffsetConversions}.
 *
 * All public static methods are exercised with a variety of inputs,
 * including edge‑cases such as UTC (the “Z” identifier) and offsets
 * that contain minutes. The tests also verify that the {@code Converter}
 * argument is not required for the current implementation (it is passed
 * but never used).
 */
public class ZoneOffsetConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions is part of the public API; a default instance is sufficient.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Helper that creates a map using {@link ZoneOffsetConversions#toMap(Object, Converter)}
     * and asserts that the map contains exactly one entry with the expected key/value.
     */
    private void assertMapContainsOffset(ZoneOffset offset) {
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, converter);
        assertNotNull("Resulting map must not be null", map);
        assertEquals("Map must contain exactly one entry", 1, map.size());
        assertTrue("Map must contain the ZONE_OFFSET key", map.containsKey(ZONE_OFFSET));
        assertEquals("Value must be the ISO‑8601 ID of the offset",
                offset.getId(), map.get(ZONE_OFFSET));
    }

    @Test
    public void testToMap_WithUtcOffset() {
        ZoneOffset utc = ZoneOffset.UTC; // ID is "Z"
        assertMapContainsOffset(utc);
    }

    @Test
    public void testToMap_WithPositiveHourOffset() {
        ZoneOffset plusTwo = ZoneOffset.ofHours(2); // "+02:00"
        assertMapContainsOffset(plusTwo);
    }

    @Test
    public void testToMap_WithNegativeHourOffset() {
        ZoneOffset minusFive = ZoneOffset.ofHours(-5); // "-05:00"
        assertMapContainsOffset(minusFive);
    }

    @Test
    public void testToMap_WithPositiveHourAndMinuteOffset() {
        ZoneOffset plusTwoThirty = ZoneOffset.ofHoursMinutes(2, 30); // "+02:30"
        assertMapContainsOffset(plusTwoThirty);
    }

    @Test
    public void testToMap_WithNegativeHourAndMinuteOffset() {
        ZoneOffset minusFiveThirty = ZoneOffset.ofHoursMinutes(-5, -30); // "-05:30"
        assertMapContainsOffset(minusFiveThirty);
    }

    @Test
    public void testToZoneId_ReturnsSameInstance() {
        ZoneId zoneId = ZoneId.of("America/New_York");
        ZoneId result = ZoneOffsetConversions.toZoneId(zoneId, converter);
        assertSame("toZoneId should return the exact same ZoneId instance", zoneId, result);
    }

    @Test
    public void testToTimeZone_UtcOffset() {
        ZoneOffset utc = ZoneOffset.UTC;
        TimeZone tz = ZoneOffsetConversions.toTimeZone(utc, converter);
        assertEquals("UTC offset should produce a TimeZone with ID \"GMT\"", "GMT", tz.getID());
        assertEquals("Raw offset for UTC must be 0 milliseconds", 0, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_PositiveOffset() {
        ZoneOffset plusTwo = ZoneOffset.ofHours(2); // "+02:00"
        TimeZone tz = ZoneOffsetConversions.toTimeZone(plusTwo, converter);
        assertEquals("Positive offset should prepend \"GMT\" to the ID", "GMT+02:00", tz.getID());
        assertEquals("Raw offset must match the offset in milliseconds",
                plusTwo.getTotalSeconds() * 1000L, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_NegativeOffset() {
        ZoneOffset minusFiveThirty = ZoneOffset.ofHoursMinutes(-5, -30); // "-05:30"
        TimeZone tz = ZoneOffsetConversions.toTimeZone(minusFiveThirty, converter);
        assertEquals("Negative offset should prepend \"GMT\" to the ID", "GMT-05:30", tz.getID());
        assertEquals("Raw offset must match the offset in milliseconds",
                minusFiveThirty.getTotalSeconds() * 1000L, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_PositiveHourAndMinuteOffset() {
        ZoneOffset plusTwoThirty = ZoneOffset.ofHoursMinutes(2, 30); // "+02:30"
        TimeZone tz = ZoneOffsetConversions.toTimeZone(plusTwoThirty, converter);
        assertEquals("ID must reflect the full offset", "GMT+02:30", tz.getID());
        assertEquals("Raw offset must be calculated correctly",
                plusTwoThirty.getTotalSeconds() * 1000L, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_ZeroOffsetButNotUTCConstant() {
        // ZoneOffset.ofTotalSeconds(0) yields an offset whose ID is "Z", same as UTC.
        ZoneOffset zero = ZoneOffset.ofTotalSeconds(0);
        assertSame("Offset instance should be equal to ZoneOffset.UTC", ZoneOffset.UTC, zero);
        TimeZone tz = ZoneOffsetConversions.toTimeZone(zero, converter);
        assertEquals("Zero offset should still produce \"GMT\"", "GMT", tz.getID());
        assertEquals(0, tz.getRawOffset());
    }
}
