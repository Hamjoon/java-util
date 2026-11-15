package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

public class ZoneOffsetConversionsTest {

    private Converter converter;   // a real Converter instance is sufficient – its behaviour is not used

    @Before
    public void setUp() {
        // Converter has a public ctor that requires ConverterOptions; we can pass null because the
        // tested methods never invoke any Converter method.
        this.converter = new Converter(null);
    }

    /* ----------------------------------------------------------------------
     * Tests for toMap( Object, Converter )
     * ---------------------------------------------------------------------- */

    @Test
    public void testToMap_basicPositiveOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(5, 30, 0); // +05:30
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, converter);

        assertEquals(3, map.size());
        assertEquals(5, map.get(MapConversions.HOURS));
        assertEquals(30, map.get(MapConversions.MINUTES));
        assertFalse("SECONDS key should be absent when seconds == 0", map.containsKey(MapConversions.SECONDS));
    }

    @Test
    public void testToMap_basicNegativeOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(-4, -15, 0); // -04:15
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, converter);

        assertEquals(3, map.size());
        assertEquals(-4, map.get(MapConversions.HOURS));
        assertEquals(-15, map.get(MapConversions.MINUTES));
        assertFalse(map.containsKey(MapConversions.SECONDS));
    }

    @Test
    public void testToMap_withSeconds() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(2, 0, 45); // +02:00:45
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, converter);

        assertEquals(3, map.size());
        assertEquals(2, map.get(MapConversions.HOURS));
        assertEquals(0, map.get(MapConversions.MINUTES));
        assertEquals(45, map.get(MapConversions.SECONDS));
    }

    @Test
    public void testToMap_utcZero() {
        ZoneOffset offset = ZoneOffset.UTC; // +00:00
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, converter);

        assertEquals(3, map.size());
        assertEquals(0, map.get(MapConversions.HOURS));
        assertEquals(0, map.get(MapConversions.MINUTES));
        assertFalse(map.containsKey(MapConversions.SECONDS));
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_invalidInputClassCast() {
        // Passing a non‑ZoneOffset object should cause a ClassCastException
        ZoneOffsetConversions.toMap("not a ZoneOffset", converter);
    }

    /* ----------------------------------------------------------------------
     * Tests for toZoneId( Object, Converter )
     * ---------------------------------------------------------------------- */

    @Test
    public void testToZoneId_returnsSameInstance() {
        ZoneId zoneId = ZoneId.of("Europe/Paris");
        ZoneId result = ZoneOffsetConversions.toZoneId(zoneId, converter);
        assertSame("toZoneId should return the exact same ZoneId instance", zoneId, result);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneId_invalidInputClassCast() {
        ZoneOffsetConversions.toZoneId(12345, converter);
    }

    /* ----------------------------------------------------------------------
     * Tests for toTimeZone( Object, Converter )
     * ---------------------------------------------------------------------- */

    @Test
    public void testToTimeZone_positiveOffset() {
        ZoneOffset offset = ZoneOffset.ofHours(3); // +03:00
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, converter);
        assertEquals("GMT+03:00", tz.getID());
    }

    @Test
    public void testToTimeZone_negativeOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(-5, -45); // -05:45
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, converter);
        // TimeZone uses the format GMT-05:45
        assertEquals("GMT-05:45", tz.getID());
    }

    @Test
    public void testToTimeZone_utcUsesGMTOnly() {
        ZoneOffset offset = ZoneOffset.UTC;
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, converter);
        assertEquals("GMT", tz.getID());
    }

    @Test
    public void testToTimeZone_withSecondsIsIgnoredByZoneOffset() {
        // ZoneOffset does not allow a non‑zero seconds component unless the minutes are also zero,
        // but we can still construct one via ofHoursMinutesSeconds.
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(1, 0, 30); // +01:00:30
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, converter);
        // The ID string will contain the full offset string (+01:00:30)
        assertEquals("GMT+01:00:30", tz.getID());
    }

    @Test(expected = ClassCastException.class)
    public void testToTimeZone_invalidInputClassCast() {
        ZoneOffsetConversions.toTimeZone("not a ZoneOffset", converter);
    }

    /* ----------------------------------------------------------------------
     * Edge‑case tests that combine multiple methods
     * ---------------------------------------------------------------------- */

    @Test
    public void testRoundTripMapToZoneOffset() {
        // Build a map that would represent a ZoneOffset of -02:15:20
        Map<String, Object> map = CompactMap.<String, Object>builder()
                .insertionOrder()
                .build();
        map.put(MapConversions.HOURS, -2);
        map.put(MapConversions.MINUTES, -15);
        map.put(MapConversions.SECONDS, -20);

        // Re‑create ZoneOffset from the map manually (simulating what a caller might do)
        int totalSeconds = ((int) map.get(MapConversions.HOURS)) * 3600
                + ((int) map.get(MapConversions.MINUTES)) * 60
                + ((int) map.get(MapConversions.SECONDS));
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(totalSeconds);

        // Verify that toMap produces the same map structure
        Map<String, Object> produced = ZoneOffsetConversions.toMap(offset, converter);
        assertEquals(map, produced);
    }

    @Test
    public void testToTimeZone_idConsistencyWithZoneId() {
        ZoneId zoneId = ZoneId.of("GMT+02:00");
        // Cast to ZoneOffset – this works because the ZoneId string is a valid offset
        ZoneOffset offset = ZoneOffset.of("+02:00");
        TimeZone tzFromOffset = ZoneOffsetConversions.toTimeZone(offset, converter);
        TimeZone tzFromZoneId = TimeZone.getTimeZone(zoneId.getId());

        assertEquals(tzFromOffset.getID(), tzFromZoneId.getID());
    }
}
