package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

/**
 * JUnit‑4 test suite for {@link TimeZoneConversions}.
 *
 * The suite covers:
 * <ul>
 *   <li>Typical usage of each conversion method.</li>
 *   <li>Edge cases such as {@code null} input.</li>
 *   <li>Special time‑zone offsets (negative, zero, and non‑hourly offsets).</li>
 *   <li>Verification that the {@code Converter} argument is truly ignored.</li>
 * </ul>
 *
 * No mocking or subclassing of library classes is performed – real {@link TimeZone}
 * instances are created via the public API.
 */
public class TimeZoneConversionsTest {

    /** Helper that returns a dummy {@link Converter}.  The conversion methods do not use it. */
    private static Converter dummyConverter() {
        // {@code ConverterOptions} is not part of the public contract required for these tests,
        // and the methods under test never touch the instance, so passing {@code null} is safe.
        return null;
    }

    /* --------------------------------------------------------------------- *
     *  toString(Object, Converter)
     * --------------------------------------------------------------------- */

    @Test
    public void testToStringTypical() {
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        String result = TimeZoneConversions.toString(tz, dummyConverter());
        assertEquals("America/Los_Angeles", result);
    }

    @Test
    public void testToStringNullInput() {
        try {
            TimeZoneConversions.toString(null, dummyConverter());
            fail("Expected NullPointerException when converting null TimeZone to String");
        } catch (NullPointerException expected) {
            // expected behaviour
        }
    }

    /* --------------------------------------------------------------------- *
     *  toZoneId(Object, Converter)
     * --------------------------------------------------------------------- */

    @Test
    public void testToZoneIdTypical() {
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        ZoneId zoneId = TimeZoneConversions.toZoneId(tz, dummyConverter());
        assertEquals(ZoneId.of("Europe/Paris"), zoneId);
    }

    @Test
    public void testToZoneIdNullInput() {
        try {
            TimeZoneConversions.toZoneId(null, dummyConverter());
            fail("Expected NullPointerException when converting null TimeZone to ZoneId");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- *
     *  toMap(Object, Converter)
     * --------------------------------------------------------------------- */

    @Test
    public void testToMapTypical() {
        TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, dummyConverter());

        // The map should contain exactly one entry with key MapConversions.ZONE
        assertEquals(1, map.size());
        assertTrue(map.containsKey(ZONE));
        assertEquals("Asia/Tokyo", map.get(ZONE));
    }

    @Test
    public void testToMapNullInput() {
        try {
            TimeZoneConversions.toMap(null, dummyConverter());
            fail("Expected NullPointerException when converting null TimeZone to Map");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- *
     *  toZoneOffset(Object, Converter)
     * --------------------------------------------------------------------- */

    @Test
    public void testToZoneOffsetZero() {
        TimeZone tz = TimeZone.getTimeZone("GMT");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, dummyConverter());
        assertEquals(ZoneOffset.UTC, offset);
    }

    @Test
    public void testToZoneOffsetNegative() {
        // New York (standard time) is UTC‑05:00 => -18000 seconds
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, dummyConverter());

        // The raw offset does not include DST; for EST it is -5 * 3600 = -18000 seconds
        assertEquals(ZoneOffset.ofHours(-5), offset);
    }

    @Test
    public void testToZoneOffsetPositiveNonWholeHour() {
        // Kathmandu is UTC+05:45 => 5*3600 + 45*60 = 20700 seconds
        TimeZone tz = TimeZone.getTimeZone("Asia/Kathmandu");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, dummyConverter());

        assertEquals(ZoneOffset.ofHoursMinutes(5, 45), offset);
    }

    @Test
    public void testToZoneOffsetNullInput() {
        try {
            TimeZoneConversions.toZoneOffset(null, dummyConverter());
            fail("Expected NullPointerException when converting null TimeZone to ZoneOffset");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- *
     *  Verify that the Converter argument truly does not affect the result.
     * --------------------------------------------------------------------- */

    @Test
    public void testConverterArgumentIgnored() {
        TimeZone tz = TimeZone.getTimeZone("Europe/London");
        // Pass a non‑null dummy Converter (even though its internal state is irrelevant)
        Converter dummy = new Converter(new ConverterOptions());

        // All conversion methods should give the same result as when passing null.
        assertEquals(TimeZoneConversions.toString(tz, null),
                     TimeZoneConversions.toString(tz, dummy));

        assertEquals(TimeZoneConversions.toZoneId(tz, null),
                     TimeZoneConversions.toZoneId(tz, dummy));

        assertEquals(TimeZoneConversions.toMap(tz, null),
                     TimeZoneConversions.toMap(tz, dummy));

        assertEquals(TimeZoneConversions.toZoneOffset(tz, null),
                     TimeZoneConversions.toZoneOffset(tz, dummy));
    }
}
