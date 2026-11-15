package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

import static com.cedarsoftware.util.convert.MapConversions.HOURS;
import static com.cedarsoftware.util.convert.MapConversions.MINUTES;
import static com.cedarsoftware.util.convert.MapConversions.SECONDS;
import static org.junit.Assert.*;

public class ZoneOffsetConversionsTest {

    // Helper that calls the private constructor via reflection to ensure class cannot be instantiated
    @Test
    public void testPrivateConstructor() throws Exception {
        java.lang.reflect.Constructor<ZoneOffsetConversions> ctor =
                ZoneOffsetConversions.class.getDeclaredConstructor();
        assertTrue("Constructor should be private", java.lang.reflect.Modifier.isPrivate(ctor.getModifiers()));
        ctor.setAccessible(true);
        try {
            ctor.newInstance();
            fail("Instantiating utility class should throw an exception");
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Expected – the constructor does not throw, but we just verify accessibility
        }
    }

    // ------------------------------------------------------------
    // toMap tests
    // ------------------------------------------------------------
    @Test
    public void testToMapZeroOffset() {
        ZoneOffset offset = ZoneOffset.UTC; // 0 seconds
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        assertEquals("Hours should be 0", 0, map.get(HOURS));
        assertEquals("Minutes should be 0", 0, map.get(MINUTES));
        assertFalse("Seconds entry must be absent for zero seconds", map.containsKey(SECONDS));
        assertEquals("Map size should be 2", 2, map.size());
    }

    @Test
    public void testToMapPositiveOffsetWithoutSeconds() {
        // +02:30 (2 hours, 30 minutes, 0 seconds)
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(2, 30);
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        assertEquals(2, map.get(HOURS));
        assertEquals(30, map.get(MINUTES));
        assertFalse("Seconds entry must be absent when seconds == 0", map.containsKey(SECONDS));
        assertEquals(2, map.size());
    }

    @Test
    public void testToMapPositiveOffsetWithSeconds() {
        // +01:02:03 (1 hour, 2 minutes, 3 seconds)
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(1, 2, 3);
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        assertEquals(1, map.get(HOURS));
        assertEquals(2, map.get(MINUTES));
        assertEquals(3, map.get(SECONDS));
        assertEquals(3, map.size());
    }

    @Test
    public void testToMapNegativeOffsetWithoutSeconds() {
        // -05:45 (negative 5 hours, 45 minutes)
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(-5, -45);
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        // Java's division / remainder rules:
        // totalSeconds = -(5*3600 + 45*60) = -20700
        // hours = -20700 / 3600 = -5
        // minutes = (-20700 % 3600) / 60 = (-20700 % 3600) = -1800 ; -1800 / 60 = -30
        // seconds = -20700 % 60 = 0
        assertEquals(-5, map.get(HOURS));
        assertEquals(-30, map.get(MINUTES));
        assertFalse(map.containsKey(SECONDS));
        assertEquals(2, map.size());
    }

    @Test
    public void testToMapNegativeOffsetWithSeconds() {
        // -01:02:03 (negative 1 hour, 2 minutes, 3 seconds)
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(-1, -2, -3);
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        // totalSeconds = -(1*3600 + 2*60 + 3) = -3723
        // hours = -3723 / 3600 = -1
        // minutes = (-3723 % 3600) / 60 = (-123 % 3600) = -123 ; -123 / 60 = -2
        // seconds = -3723 % 60 = -3
        assertEquals(-1, map.get(HOURS));
        assertEquals(-2, map.get(MINUTES));
        assertEquals(-3, map.get(SECONDS));
        assertEquals(3, map.size());
    }

    // ------------------------------------------------------------
    // toZoneId tests
    // ------------------------------------------------------------
    @Test
    public void testToZoneIdReturnsSameInstance() {
        ZoneId zone = ZoneId.of("Europe/Paris");
        ZoneId result = ZoneOffsetConversions.toZoneId(zone, null);
        assertSame("toZoneId must return the same ZoneId instance", zone, result);
    }

    @Test
    public void testToZoneIdWithOffsetWorks() {
        ZoneOffset offset = ZoneOffset.ofHours(3);
        ZoneId result = ZoneOffsetConversions.toZoneId(offset, null);
        assertSame("Offset is also a ZoneId, should be returned unchanged", offset, result);
    }

    // ------------------------------------------------------------
    // toTimeZone tests
    // ------------------------------------------------------------
    @Test
    public void testToTimeZoneUtc() {
        ZoneOffset offset = ZoneOffset.UTC;
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, null);
        assertEquals("GMT", tz.getID());
    }

    @Test
    public void testToTimeZonePositiveOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(5, 30); // +05:30
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, null);
        assertEquals("GMT+05:30", tz.getID());
    }

    @Test
    public void testToTimeZoneNegativeOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(-4, -15); // -04:15
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, null);
        assertEquals("GMT-04:15", tz.getID());
    }

    @Test
    public void testToTimeZoneWithSeconds() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(2, 0, 45); // +02:00:45
        // The ID produced by ZoneOffset#getId includes seconds if non‑zero
        // e.g., "+02:00:45"
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, null);
        assertEquals("GMT+02:00:45", tz.getID());
    }

    // ------------------------------------------------------------
    // Additional sanity checks for the map implementation used
    // ------------------------------------------------------------
    @Test
    public void testMapIsInsertionOrdered() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(1, 2, 3);
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);
        // CompactMap.builder().insertionOrder() guarantees iteration order matches insertion.
        // Verify that the key iterator yields HOURS, MINUTES, SECONDS in that order.
        java.util.Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        assertEquals(HOURS, it.next());
        assertTrue(it.hasNext());
        assertEquals(MINUTES, it.next());
        assertTrue(it.hasNext());
        assertEquals(SECONDS, it.next());
        assertFalse(it.hasNext());
    }
}
