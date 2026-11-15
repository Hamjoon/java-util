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
 * The tests cover:
 *   • normal operation of each conversion method,
 *   • handling of {@code null} arguments,
 *   • handling of arguments of the wrong type,
 *   • special edge cases such as UTC and offsets that contain seconds.
 *
 * The {@code Converter} argument is not used by the implementation, but we still
 * pass a valid instance (or {@code null}) to verify that the method does not depend
 * on it.
 */
public class ZoneOffsetConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter is not used by the methods under test, but we create a
        // minimal instance to satisfy the signature.
        dummyConverter = new Converter(null);
    }

    /* ---------------------------------------------------------
     *  Tests for toMap(Object, Converter)
     * -------------------------------------------------------*/

    @Test
    public void testToMap_NormalOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(5, 30); // "+05:30"
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, dummyConverter);

        assertNotNull("Result map must not be null", result);
        assertTrue("Result map must contain the key ZONE_OFFSET",
                result.containsKey(MapConversions.ZONE_OFFSET));
        assertEquals("Map value must be the ISO‑8601 id of the offset",
                offset.getId(), result.get(MapConversions.ZONE_OFFSET));
    }

    @Test
    public void testToMap_NullConverterIsAllowed() {
        ZoneOffset offset = ZoneOffset.UTC;
        Map<String, Object> result = ZoneOffsetConversions.toMap(offset, null);
        assertNotNull(result);
        assertEquals("Z", result.get(MapConversions.ZONE_OFFSET));
    }

    @Test
    public void testToMap_NullFromThrowsNPE() {
        try {
            ZoneOffsetConversions.toMap(null, dummyConverter);
            fail("Expected NullPointerException when 'from' is null");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testToMap_WrongTypeThrowsClassCastException() {
        try {
            ZoneOffsetConversions.toMap("not a ZoneOffset", dummyConverter);
            fail("Expected ClassCastException for wrong argument type");
        } catch (ClassCastException expected) {
            // expected
        }
    }

    /* ---------------------------------------------------------
     *  Tests for toZoneId(Object, Converter)
     * -------------------------------------------------------*/

    @Test
    public void testToZoneId_NormalZoneId() {
        ZoneId zoneId = ZoneId.of("Europe/Paris");
        ZoneId result = ZoneOffsetConversions.toZoneId(zoneId, dummyConverter);
        assertSame("Result must be the same instance that was supplied", zoneId, result);
    }

    @Test
    public void testToZoneId_NormalZoneOffset() {
        ZoneOffset offset = ZoneOffset.ofHours(-4);
        ZoneId result = ZoneOffsetConversions.toZoneId(offset, dummyConverter);
        assertSame("Result must be the same ZoneOffset instance", offset, result);
    }

    @Test
    public void testToZoneId_NullFromThrowsNPE() {
        try {
            ZoneOffsetConversions.toZoneId(null, dummyConverter);
            fail("Expected NullPointerException when 'from' is null");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testToZoneId_WrongTypeThrowsClassCastException() {
        try {
            ZoneOffsetConversions.toZoneId(12345, dummyConverter);
            fail("Expected ClassCastException for non‑ZoneId argument");
        } catch (ClassCastException expected) {
            // expected
        }
    }

    /* ---------------------------------------------------------
     *  Tests for toTimeZone(Object, Converter)
     * -------------------------------------------------------*/

    @Test
    public void testToTimeZone_NormalPositiveOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(2, 45); // "+02:45"
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, dummyConverter);
        assertNotNull(tz);
        assertTrue("TimeZone ID must start with \"GMT\"", tz.getID().startsWith("GMT"));
        assertEquals("GMT+02:45", tz.getID());

        // raw offset is expressed in milliseconds
        int expectedMillis = offset.getTotalSeconds() * 1000;
        assertEquals("Raw offset must match the ZoneOffset", expectedMillis, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_NormalNegativeOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(-3, -15); // "-03:15"
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, dummyConverter);
        assertEquals("GMT-03:15", tz.getID());
        assertEquals(offset.getTotalSeconds() * 1000, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_UTC_IsSpecialCase() {
        ZoneOffset utc = ZoneOffset.UTC;
        TimeZone tz = ZoneOffsetConversions.toTimeZone(utc, dummyConverter);
        assertEquals("UTC must be represented as plain \"GMT\"", "GMT", tz.getID());
        assertEquals(0, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_WithSeconds() {
        // Offset with seconds: +01:02:03
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(1, 2, 3);
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, dummyConverter);
        assertEquals("GMT+01:02:03", tz.getID());
        assertEquals(offset.getTotalSeconds() * 1000, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_NullConverterIsAllowed() {
        ZoneOffset offset = ZoneOffset.ofHours(1);
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, null);
        assertNotNull(tz);
        assertEquals("GMT+01:00", tz.getID());
    }

    @Test
    public void testToTimeZone_NullFromThrowsNPE() {
        try {
            ZoneOffsetConversions.toTimeZone(null, dummyConverter);
            fail("Expected NullPointerException when 'from' is null");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testToTimeZone_WrongTypeThrowsClassCastException() {
        try {
            ZoneOffsetConversions.toTimeZone("not a ZoneOffset", dummyConverter);
            fail("Expected ClassCastException for wrong argument type");
        } catch (ClassCastException expected) {
            // expected
        }
    }
}
