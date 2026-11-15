package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

import static com.cedarsoftware.util.convert.MapConversions.HOURS;
import static com.cedarsoftware.util.convert.MapConversions.MINUTES;
import static com.cedarsoftware.util.convert.MapConversions.SECONDS;
import static org.junit.Assert.*;

public class ZoneOffsetConversionsTest {

    // ----------------------------------------------------------------------
    // Helper – a dummy Converter (the methods under test never use it)
    // ----------------------------------------------------------------------
    private static final Converter DUMMY_CONVERTER = null;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // ----------------------------------------------------------------------
    // 1. Tests for toMap(...)
    // ----------------------------------------------------------------------
    @Test
    public void testToMap_WithWholeHours_NoSecondsKey() {
        ZoneOffset offset = ZoneOffset.ofHours(5); // +05:00, total seconds = 18000
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, DUMMY_CONVERTER);

        assertEquals(3, map.size()); // HOURS, MINUTES, (SECONDS omitted)
        assertEquals(5, map.get(HOURS));
        assertEquals(0, map.get(MINUTES));
        assertFalse("SECONDS key should not be present when seconds are zero", map.containsKey(SECONDS));
    }

    @Test
    public void testToMap_WithHoursMinutesSeconds_SecondsKeyPresent() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(2, 30, 45); // +02:30:45
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, DUMMY_CONVERTER);

        assertEquals(4, map.size()); // HOURS, MINUTES, SECONDS
        assertEquals(2, map.get(HOURS));
        assertEquals(30, map.get(MINUTES));
        assertEquals(45, map.get(SECONDS));
    }

    @Test
    public void testToMap_WithNegativeOffset_ValuesAreNegativeOrPositiveCorrectly() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(-3, -15, -20); // -03:15:20
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, DUMMY_CONVERTER);

        // totalSeconds = -11720  => hours = -3, minutes = -15, seconds = -20
        assertEquals(-3, map.get(HOURS));
        assertEquals(-15, map.get(MINUTES));
        assertEquals(-20, map.get(SECONDS));
    }

    @Test
    public void testToMap_WithInvalidType_ThrowsClassCastException() {
        thrown.expect(ClassCastException.class);
        ZoneOffsetConversions.toMap(Integer.valueOf(10), DUMMY_CONVERTER);
    }

    @Test
    public void testToMap_WithNullFrom_ThrowsNullPointerException() {
        thrown.expect(NullPointerException.class);
        ZoneOffsetConversions.toMap(null, DUMMY_CONVERTER);
    }

    // ----------------------------------------------------------------------
    // 2. Tests for toZoneId(...)
    // ----------------------------------------------------------------------
    @Test
    public void testToZoneId_ReturnsSameInstance() {
        ZoneId original = ZoneOffset.ofHours(3); // any ZoneId works
        ZoneId result = ZoneOffsetConversions.toZoneId(original, DUMMY_CONVERTER);
        assertSame("toZoneId should return the exact same instance", original, result);
    }

    @Test
    public void testToZoneId_WithInvalidType_ThrowsClassCastException() {
        thrown.expect(ClassCastException.class);
        ZoneOffsetConversions.toZoneId("not a ZoneId", DUMMY_CONVERTER);
    }

    @Test
    public void testToZoneId_WithNull_ThrowsNullPointerException() {
        thrown.expect(NullPointerException.class);
        ZoneOffsetConversions.toZoneId(null, DUMMY_CONVERTER);
    }

    // ----------------------------------------------------------------------
    // 3. Tests for toTimeZone(...)
    // ----------------------------------------------------------------------
    @Test
    public void testToTimeZone_Utc_ReturnsGMT() {
        ZoneOffset utc = ZoneOffset.UTC;
        TimeZone tz = ZoneOffsetConversions.toTimeZone(utc, DUMMY_CONVERTER);
        assertEquals("GMT", tz.getID());
        assertEquals(0, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_PositiveOffset_ReturnsCorrectGMTIdAndOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(2, 30); // +02:30
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, DUMMY_CONVERTER);
        assertEquals("GMT+02:30", tz.getID());
        assertEquals(offset.getTotalSeconds() * 1000L, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_NegativeOffset_ReturnsCorrectGMTIdAndOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(-5, -45); // -05:45
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, DUMMY_CONVERTER);
        assertEquals("GMT-05:45", tz.getID());
        assertEquals(offset.getTotalSeconds() * 1000L, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_WithInvalidType_ThrowsClassCastException() {
        thrown.expect(ClassCastException.class);
        ZoneOffsetConversions.toTimeZone(Double.valueOf(1.23), DUMMY_CONVERTER);
    }

    @Test
    public void testToTimeZone_WithNull_ThrowsNullPointerException() {
        thrown.expect(NullPointerException.class);
        ZoneOffsetConversions.toTimeZone(null, DUMMY_CONVERTER);
    }
}
