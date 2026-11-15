package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;
import static com.cedarsoftware.util.convert.MapConversions.ZONE;

import java.util.Map;
import java.util.TimeZone;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.Test;

/**
 * Unit tests for {@link TimeZoneConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Conversion to {@link String} (the TimeZone ID)</li>
 *   <li>Conversion to {@link ZoneId}</li>
 *   <li>Conversion to a {@link Map} containing the {@code ZONE} entry</li>
 *   <li>Conversion to {@link ZoneOffset} for negative, zero and positive raw offsets</li>
 *   <li>Behaviour when a {@code null} source is supplied (expected NPE)</li>
 * </ul>
 * The {@link Converter} argument is not used by the implementation, therefore {@code null}
 * is passed in all calls.
 */
public class TimeZoneConversionsTest {

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /** Simple helper to create a {@link Converter} instance if needed in the future.
     *  The current implementation does not depend on the converter, so we keep the
     *  method for readability. */
    private static Converter dummyConverter() {
        // The Converter constructor requires a ConverterOptions instance.
        // Passing null is acceptable because the methods under test never dereference it.
        return new Converter(null);
    }

    // -------------------------------------------------------------------------
    // toString(...)
    // -------------------------------------------------------------------------

    @Test
    public void testToStringReturnsId() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        String result = TimeZoneConversions.toString(tz, dummyConverter());
        assertEquals("America/New_York", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringNullInput() {
        TimeZoneConversions.toString(null, dummyConverter());
    }

    // -------------------------------------------------------------------------
    // toZoneId(...)
    // -------------------------------------------------------------------------

    @Test
    public void testToZoneIdMatchesTimeZone() {
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        ZoneId zoneId = TimeZoneConversions.toZoneId(tz, dummyConverter());
        assertEquals(ZoneId.of("Europe/Paris"), zoneId);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneIdNullInput() {
        TimeZoneConversions.toZoneId(null, dummyConverter());
    }

    // -------------------------------------------------------------------------
    // toMap(...)
    // -------------------------------------------------------------------------

    @Test
    public void testToMapContainsSingleZoneEntry() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, dummyConverter());

        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(ZONE));
        assertEquals("UTC", map.get(ZONE));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapNullInput() {
        TimeZoneConversions.toMap(null, dummyConverter());
    }

    // -------------------------------------------------------------------------
    // toZoneOffset(...)
    // -------------------------------------------------------------------------

    @Test
    public void testToZoneOffsetUtcZero() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, dummyConverter());
        assertEquals(ZoneOffset.UTC, offset);
    }

    @Test
    public void testToZoneOffsetNegativeRawOffset() {
        // America/Los_Angeles = UTC-8 (standard time)
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, dummyConverter());

        // Raw offset is -28800000 ms = -8 hours
        ZoneOffset expected = ZoneOffset.ofHours(-8);
        assertEquals(expected, offset);
    }

    @Test
    public void testToZoneOffsetPositiveRawOffset() {
        // Asia/Kolkata = UTC+5:30
        TimeZone tz = TimeZone.getTimeZone("Asia/Kolkata");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, dummyConverter());

        // Raw offset is 19800000 ms = +5 hours 30 minutes
        ZoneOffset expected = ZoneOffset.ofHoursMinutes(5, 30);
        assertEquals(expected, offset);
    }

    @Test
    public void testToZoneOffsetCustomSimpleTimeZone() {
        // GMT+02:00 is a SimpleTimeZone with a raw offset of +7200000 ms
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, dummyConverter());

        ZoneOffset expected = ZoneOffset.ofHours(2);
        assertEquals(expected, offset);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneOffsetNullInput() {
        TimeZoneConversions.toZoneOffset(null, dummyConverter());
    }
}
