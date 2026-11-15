package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;

/**
 * JUnit‑4 test suite for {@link TimeZoneConversions}.
 *
 * The class under test has package‑private static methods, therefore the test class
 * resides in the same package so it can access them directly.
 *
 * The tests cover:
 *   • Normal (happy‑path) behaviour for each conversion method.
 *   • Edge‑case handling such as {@code null} arguments and wrong argument types.
 *   • Verification that the map produced by {@code toMap} contains the expected key/value.
 *
 * The {@code Converter} parameter is not used by any of the methods, so {@code null}
 * is passed in all test cases.
 */
public class TimeZoneConversionsTest {

    // -----------------------------------------------------------------------
    // 1.  Public (package‑private) method signatures extracted from the class
    // -----------------------------------------------------------------------
    // static String toString(Object from, Converter converter)
    // static ZoneId toZoneId(Object from, Converter converter)
    // static Map<String, Object> toMap(Object from, Converter converter)

    private TimeZone utcTimeZone;
    private TimeZone estTimeZone;

    @Before
    public void setUp() {
        utcTimeZone = TimeZone.getTimeZone("UTC");
        estTimeZone = TimeZone.getTimeZone("America/New_York"); // observes DST
    }

    // -----------------------------------------------------------------------
    // 2.  Basic (happy‑path) test cases
    // -----------------------------------------------------------------------

    @Test
    public void testToString_WithUtcTimeZone() {
        String result = TimeZoneConversions.toString(utcTimeZone, null);
        assertEquals("UTC", result);
    }

    @Test
    public void testToString_WithEstTimeZone() {
        String result = TimeZoneConversions.toString(estTimeZone, null);
        assertEquals("America/New_York", result);
    }

    @Test
    public void testToZoneId_WithUtcTimeZone() {
        ZoneId zoneId = TimeZoneConversions.toZoneId(utcTimeZone, null);
        assertEquals(utcTimeZone.toZoneId(), zoneId);
    }

    @Test
    public void testToZoneId_WithEstTimeZone() {
        ZoneId zoneId = TimeZoneConversions.toZoneId(estTimeZone, null);
        assertEquals(estTimeZone.toZoneId(), zoneId);
    }

    @Test
    public void testToMap_WithUtcTimeZone() {
        Map<String, Object> map = TimeZoneConversions.toMap(utcTimeZone, null);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(ZONE));
        assertEquals("UTC", map.get(ZONE));
    }

    @Test
    public void testToMap_WithEstTimeZone() {
        Map<String, Object> map = TimeZoneConversions.toMap(estTimeZone, null);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(ZONE));
        assertEquals("America/New_York", map.get(ZONE));
    }

    // -----------------------------------------------------------------------
    // 3.  Edge‑case / exception handling test cases
    // -----------------------------------------------------------------------

    // ----- Null argument ----------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testToString_NullArgument() {
        TimeZoneConversions.toString(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneId_NullArgument() {
        TimeZoneConversions.toZoneId(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullArgument() {
        TimeZoneConversions.toMap(null, null);
    }

    // ----- Wrong type argument (ClassCastException) -------------------------
    @Test(expected = ClassCastException.class)
    public void testToString_WrongTypeArgument() {
        TimeZoneConversions.toString("NotATimeZone", null);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneId_WrongTypeArgument() {
        TimeZoneConversions.toZoneId(12345, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongTypeArgument() {
        TimeZoneConversions.toMap(new Object(), null);
    }

    // ----- Unusual but valid TimeZone IDs ----------------------------------
    @Test
    public void testToString_WithCustomGMTOffset() {
        // GMT+02:00 is a valid ID; TimeZone normalises it to "GMT+02:00"
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        String result = TimeZoneConversions.toString(tz, null);
        assertEquals("GMT+02:00", result);
    }

    @Test
    public void testToZoneId_WithCustomGMTOffset() {
        TimeZone tz = TimeZone.getTimeZone("GMT-05:00");
        ZoneId zoneId = TimeZoneConversions.toZoneId(tz, null);
        // The ZoneId string representation should match the normalized ID
        assertEquals(tz.toZoneId(), zoneId);
        assertEquals("GMT-05:00", zoneId.getId());
    }

    @Test
    public void testToMap_WithCustomGMTOffset() {
        TimeZone tz = TimeZone.getTimeZone("GMT+03:30");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, null);
        assertEquals("GMT+03:30", map.get(ZONE));
    }
}
