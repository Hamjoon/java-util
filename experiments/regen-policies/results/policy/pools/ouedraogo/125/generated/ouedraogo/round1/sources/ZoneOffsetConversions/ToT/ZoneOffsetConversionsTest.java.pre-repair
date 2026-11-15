package com.cedarsoftware.util.convert;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

import static com.cedarsoftware.util.convert.MapConversions.HOURS;
import static com.cedarsoftware.util.convert.MapConversions.MINUTES;
import static com.cedarsoftware.util.convert.MapConversions.SECONDS;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ZoneOffsetConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Conversion of {@link ZoneOffset} to a {@link Map} (positive, negative and
 *       offsets that include seconds).</li>
 *   <li>Direct return of a {@link ZoneId} from {@code toZoneId}.</li>
 *   <li>Creation of a {@link TimeZone} from a {@link ZoneOffset},
 *       including the special handling of UTC.</li>
 *   <li>Edge‑case handling (zero offset, negative offsets, offsets with a
 *       non‑zero seconds component).</li>
 * </ul>
 *
 * The {@code Converter} argument of the static methods is not used in the
 * current implementation, therefore a {@code null} reference is supplied.
 */
public class ZoneOffsetConversionsTest {

    /** A dummy {@code Converter} – the implementation under test does not use it. */
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter API requires a ConverterOptions instance; passing {@code null}
        // is acceptable for the purposes of these tests because the methods under test
        // ignore the converter argument entirely.
        dummyConverter = new Converter(null);
    }

    /* -----------------------------------------------------------
     *  Tests for ZoneOffsetConversions.toMap(...)
     * ----------------------------------------------------------- */

    @Test
    public void testToMap_ZeroOffset_NoSeconds() {
        ZoneOffset offset = ZoneOffset.UTC;               // "+00:00"
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, dummyConverter);

        assertEquals("Map should contain exactly two entries (hours & minutes)", 2, map.size());
        assertEquals("Hours for UTC must be 0", 0, map.get(HOURS));
        assertEquals("Minutes for UTC must be 0", 0, map.get(MINUTES));
        assertFalse("SECONDS entry must be absent when seconds == 0", map.containsKey(SECONDS));
    }

    @Test
    public void testToMap_PositiveOffset_WithSeconds() {
        // Offset of +02:30:45  (2 hours, 30 minutes, 45 seconds)
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(2, 30, 45);
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, dummyConverter);

        assertEquals("Map should contain three entries (hours, minutes, seconds)", 3, map.size());
        assertEquals("Hours should be 2", 2, map.get(HOURS));
        assertEquals("Minutes should be 30", 30, map.get(MINUTES));
        assertEquals("Seconds should be 45", 45, map.get(SECONDS));
    }

    @Test
    public void testToMap_NegativeOffset_NoSeconds() {
        // Offset of -05:15:00  (negative 5 hours, 15 minutes)
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(-5, -15);
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, dummyConverter);

        assertEquals("Map should contain exactly two entries (hours & minutes)", 2, map.size());
        assertEquals("Hours should be -5", -5, map.get(HOURS));
        assertEquals("Minutes should be -15", -15, map.get(MINUTES));
        assertFalse("SECONDS entry must be absent when seconds == 0", map.containsKey(SECONDS));
    }

    /* -----------------------------------------------------------
     *  Tests for ZoneOffsetConversions.toZoneId(...)
     * ----------------------------------------------------------- */

    @Test
    public void testToZoneId_ReturnsSameInstance() {
        ZoneId zoneId = ZoneOffset.ofHours(3); // "+03:00" is a ZoneOffset which implements ZoneId
        ZoneId result = ZoneOffsetConversions.toZoneId(zoneId, dummyConverter);
        assertSame("toZoneId should return the exact same ZoneId instance supplied", zoneId, result);
    }

    /* -----------------------------------------------------------
     *  Tests for ZoneOffsetConversions.toTimeZone(...)
     * ----------------------------------------------------------- */

    @Test
    public void testToTimeZone_UTC_ReturnsGMT() {
        ZoneOffset utc = ZoneOffset.UTC; // "+00:00"
        TimeZone tz = ZoneOffsetConversions.toTimeZone(utc, dummyConverter);
        assertEquals("UTC offset must map to TimeZone ID \"GMT\"", "GMT", tz.getID());
        // Verify that the raw offset (in milliseconds) matches UTC
        assertEquals("GMT raw offset must be 0", 0, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_NonUTC_PositiveOffset() {
        // Offset "+02:00"
        ZoneOffset offset = ZoneOffset.ofHours(2);
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, dummyConverter);
        assertEquals("Positive offset should produce ID \"GMT+02:00\"", "GMT+02:00", tz.getID());
        assertEquals("Raw offset must be +2 hours in ms", 2 * 60 * 60 * 1000, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_NonUTC_NegativeOffset() {
        // Offset "-07:30"
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(-7, -30);
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, dummyConverter);
        assertEquals("Negative offset should produce ID \"GMT-07:30\"", "GMT-07:30", tz.getID());
        // Raw offset is negative 7.5 hours in milliseconds
        assertEquals("Raw offset must be -7.5 hours in ms",
                (int) (-7.5 * 60 * 60 * 1000), tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_OffsetWithSeconds_IgnoresSecondsInID() {
        // Offset "+01:00:30" – seconds are part of the ZoneOffset but the TimeZone ID
        // format does not include seconds; the implementation uses offset.getId()
        // which yields "+01:00:30".
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(1, 0, 30);
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, dummyConverter);
        // According to the implementation, the ID will be "GMT+01:00:30"
        assertEquals("ID should contain seconds part as returned by ZoneOffset.getId()",
                "GMT+01:00:30", tz.getID());

        // The raw offset in milliseconds must include the seconds component.
        int expectedMillis = (int) ( (1 * 3600 + 0 * 60 + 30) * 1000L );
        assertEquals("Raw offset must reflect the full seconds component", expectedMillis, tz.getRawOffset());
    }
}
