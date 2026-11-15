package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;

/**
 * Unit tests for {@link ZoneOffsetConversions}.
 */
public class ZoneOffsetConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The Converter is not used by the methods under test, but a non‑null instance is required.
        // Passing null for the options is acceptable for these tests because the options are never inspected.
        this.converter = new Converter(null);
    }

    @Test
    public void testToMap_WithWholeHours() {
        ZoneOffset offset = ZoneOffset.ofHours(5); // +05:00
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, converter);

        assertEquals("Hours should be 5", 5, result.get(HOURS));
        assertEquals("Minutes should be 0", 0, result.get(MINUTES));
        assertFalse("Seconds entry should be absent when zero", result.containsKey(SECONDS));
    }

    @Test
    public void testToMap_WithHoursAndMinutes() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes( -3, 30); // -03:30
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, converter);

        assertEquals("Hours should be -3", -3, result.get(HOURS));
        assertEquals("Minutes should be 30", 30, result.get(MINUTES));
        assertFalse("Seconds entry should be absent when zero", result.containsKey(SECONDS));
    }

    @Test
    public void testToMap_WithSeconds() {
        // Create an offset with seconds: +02:15:45
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(2, 15, 45);
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, converter);

        assertEquals("Hours should be 2", 2, result.get(HOURS));
        assertEquals("Minutes should be 15", 15, result.get(MINUTES));
        assertEquals("Seconds should be 45", 45, result.get(SECONDS));
    }

    @Test
    public void testToMap_NegativeWithSeconds() {
        // -01:02:03
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(-1, -2, -3);
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, converter);

        // totalSeconds = -(1*3600 + 2*60 + 3) = -3723
        // hours = -1 (integer division truncates toward zero in Java for negatives? actually -3723/3600 = -1)
        // minutes = (-3723 % 3600) / 60 -> -123 % 3600 = -123, /60 = -2
        // seconds = -123 % 60 = -3
        assertEquals("Hours should be -1", -1, result.get(HOURS));
        assertEquals("Minutes should be -2", -2, result.get(MINUTES));
        assertEquals("Seconds should be -3", -3, result.get(SECONDS));
    }

    @Test
    public void testToZoneId_ReturnsSameInstance() {
        ZoneId zoneId = ZoneId.of("America/New_York");
        ZoneId result = ZoneOffsetConversions.toZoneId(zoneId, converter);
        assertSame("toZoneId should return the same ZoneId instance", zoneId, result);
    }

    @Test
    public void testToTimeZone_UtcOffset() {
        ZoneOffset utc = ZoneOffset.UTC;
        TimeZone tz = ZoneOffsetConversions.toTimeZone(utc, converter);
        assertEquals("UTC offset should map to GMT", "GMT", tz.getID());
        assertEquals("Raw offset for UTC should be 0 ms", 0, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_PositiveOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(3, 30); // +03:30
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, converter);
        assertEquals("ID should be GMT+03:30", "GMT+03:30", tz.getID());
        assertEquals("Raw offset should match total seconds", offset.getTotalSeconds() * 1000L, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_NegativeOffset() {
        ZoneOffset offset = ZoneOffset.ofHours(-5); // -05:00
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, converter);
        assertEquals("ID should be GMT-05:00", "GMT-05:00", tz.getID());
        assertEquals("Raw offset should match total seconds", offset.getTotalSeconds() * 1000L, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_WithSeconds() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(1, 0, 30); // +01:00:30
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, converter);
        // TimeZone IDs do not include seconds; Java will truncate to minutes, but the raw offset must still be correct.
        assertEquals("Raw offset must include seconds", offset.getTotalSeconds() * 1000L, tz.getRawOffset());
        // The ID will be GMT+01:00 because TimeZone does not represent seconds in the ID string.
        assertTrue("ID should start with GMT+01:00", tz.getID().startsWith("GMT+01:00"));
    }
}
