package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

/**
 * Unit tests for {@link TimeZoneConversions}.
 *
 * The tests cover all static conversion methods:
 * <ul>
 *   <li>{@code toString}</li>
 *   <li>{@code toZoneId}</li>
 *   <li>{@code toMap}</li>
 *   <li>{@code toZoneOffset}</li>
 * </ul>
 *
 * The {@link Converter} argument is not used by the implementation, therefore a
 * minimal instance created with default {@link ConverterOptions} is sufficient.
 */
public class TimeZoneConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library.
        // The concrete behaviour of the Converter is irrelevant for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @Test
    public void testToStringReturnsTimeZoneId() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        assertEquals("UTC", TimeZoneConversions.toString(utc, converter));

        TimeZone gmtPlus2 = TimeZone.getTimeZone("GMT+02:00");
        assertEquals("GMT+02:00", TimeZoneConversions.toString(gmtPlus2, converter));

        TimeZone unknown = TimeZone.getTimeZone("THIS_ID_DOES_NOT_EXIST");
        // java.util.TimeZone falls back to "GMT" for unknown IDs
        assertEquals("GMT", TimeZoneConversions.toString(unknown, converter));
    }

    @Test
    public void testToZoneIdMatchesTimeZoneToZoneId() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        ZoneId expected = tz.toZoneId();
        ZoneId actual = TimeZoneConversions.toZoneId(tz, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMapContainsZoneKeyWithId() {
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, converter);

        // The MapConversions class defines ZONE as the key for the time‑zone id.
        // We reference it directly to avoid hard‑coding the literal.
        assertTrue("Result map must contain the ZONE key", map.containsKey(ZONE));
        assertEquals(tz.getID(), map.get(ZONE));
        // No extra entries should be present.
        assertEquals(1, map.size());
    }

    @Test
    public void testToZoneOffsetZero() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(utc, converter);
        assertEquals(ZoneOffset.UTC, offset);
    }

    @Test
    public void testToZoneOffsetPositiveWholeHours() {
        TimeZone gmtPlus2 = TimeZone.getTimeZone("GMT+02:00");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(gmtPlus2, converter);
        assertEquals(ZoneOffset.ofHours(2), offset);
    }

    @Test
    public void testToZoneOffsetNegativeWithMinutes() {
        TimeZone gmtMinus530 = TimeZone.getTimeZone("GMT-05:30");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(gmtMinus530, converter);
        // -5 hours 30 minutes = -19800 seconds
        assertEquals(ZoneOffset.ofHoursMinutes(-5, -30), offset);
    }

    @Test
    public void testToZoneOffsetHandlesRawOffsetCorrectly() {
        // Verify that the conversion uses the raw offset (no DST adjustments)
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles"); // PST = -08:00 raw offset
        int rawSeconds = tz.getRawOffset() / 1000;
        ZoneOffset expected = ZoneOffset.ofTotalSeconds(rawSeconds);
        ZoneOffset actual = TimeZoneConversions.toZoneOffset(tz, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testAllMethodsAcceptNullConverter() {
        // The implementation does not dereference the Converter argument.
        TimeZone tz = TimeZone.getTimeZone("UTC");
        assertEquals("UTC", TimeZoneConversions.toString(tz, null));
        assertEquals(tz.toZoneId(), TimeZoneConversions.toZoneId(tz, null));
        assertEquals(ZoneOffset.UTC, TimeZoneConversions.toZoneOffset(tz, null));
        Map<String, Object> map = TimeZoneConversions.toMap(tz, null);
        assertEquals(tz.getID(), map.get(ZONE));
    }
}
