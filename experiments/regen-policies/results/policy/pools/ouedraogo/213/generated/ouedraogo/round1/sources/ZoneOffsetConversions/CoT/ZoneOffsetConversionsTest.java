package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.*;
import static com.cedarsoftware.util.convert.MapConversions.ZONE_OFFSET;

/**
 * JUnit‑4 test suite for {@link ZoneOffsetConversions}.
 *
 * The class under test contains three static conversion methods that operate on
 * {@link ZoneOffset} / {@link ZoneId} objects.  The tests cover:
 * <ul>
 *   <li>Normal (happy‑path) behaviour.</li>
 *   <li>Edge cases such as {@code null} arguments.</li>
 *   <li>Invalid argument types that provoke {@code ClassCastException}.</li>
 *   <li>Special handling of {@link ZoneOffset#UTC} in {@code toTimeZone()}.</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, therefore a
 * {@code null} reference is sufficient for all tests.
 */
public class ZoneOffsetConversionsTest {

    // -----------------------------------------------------------------------
    //  Helper constants
    // -----------------------------------------------------------------------
    private static final Converter DUMMY_CONVERTER = null; // not used by the methods

    // -----------------------------------------------------------------------
    //  Tests for toMap(...)
    // -----------------------------------------------------------------------

    @Test
    public void testToMap_HappyPath() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(2, 0); // "+02:00"
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, DUMMY_CONVERTER);

        assertNotNull("Result map must not be null", result);
        assertTrue("Result map must contain the zone‑offset key", result.containsKey(ZONE_OFFSET));
        assertEquals("Map value must be the offset ID", offset.getId(), result.get(ZONE_OFFSET));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullFrom() {
        ZoneOffsetConversions.toMap(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongType() {
        ZoneOffsetConversions.toMap("not a ZoneOffset", DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------------
    //  Tests for toZoneId(...)
    // -----------------------------------------------------------------------

    @Test
    public void testToZoneId_HappyPath() {
        ZoneId zoneId = ZoneOffset.ofHours(5); // "+05:00"
        ZoneId result = ZoneOffsetConversions.toZoneId(zoneId, DUMMY_CONVERTER);
        assertSame("Returned ZoneId must be the same instance supplied", zoneId, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneId_NullFrom() {
        ZoneOffsetConversions.toZoneId(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneId_WrongType() {
        ZoneOffsetConversions.toZoneId(Integer.valueOf(42), DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------------
    //  Tests for toTimeZone(...)
    // -----------------------------------------------------------------------

    @Test
    public void testToTimeZone_HappyPath_NonUtc() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(-4, -30); // "-04:30"
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, DUMMY_CONVERTER);

        assertNotNull("TimeZone must not be null", tz);
        assertEquals("TimeZone ID must follow the GMT±HH:MM pattern",
                "GMT" + offset.getId(), tz.getID());
        // Verify that the raw offset (in milliseconds) matches the ZoneOffset value
        assertEquals("Raw offset must equal ZoneOffset total seconds * 1000",
                offset.getTotalSeconds() * 1000L, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_UtcSpecialCase() {
        ZoneOffset utc = ZoneOffset.UTC; // "Z"
        TimeZone tz = ZoneOffsetConversions.toTimeZone(utc, DUMMY_CONVERTER);

        assertNotNull("TimeZone must not be null", tz);
        assertEquals("UTC must be represented as plain \"GMT\"", "GMT", tz.getID());
        assertEquals("UTC raw offset must be zero", 0, tz.getRawOffset());
    }

    @Test(expected = NullPointerException.class)
    public void testToTimeZone_NullFrom() {
        ZoneOffsetConversions.toTimeZone(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToTimeZone_WrongType() {
        ZoneOffsetConversions.toTimeZone("not a ZoneOffset", DUMMY_CONVERTER);
    }
}
