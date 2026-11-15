package com.cedarsoftware.util.convert;

import org.junit.Test;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.HOURS;
import static com.cedarsoftware.util.convert.MapConversions.MINUTES;
import static com.cedarsoftware.util.convert.MapConversions.SECONDS;
import static org.junit.Assert.*;

public class ZoneOffsetConversionsTest {

    /*** ----------------------------------------------------------------------
     *  List of package‑private static methods in ZoneOffsetConversions
     *  ----------------------------------------------------------------------*/
    // static Map<String,Object> toMap(Object from, Converter converter)
    // static ZoneId toZoneId(Object from, Converter converter)

    /*** ----------------------------------------------------------------------
     *  Basic functionality tests
     *  ----------------------------------------------------------------------*/

    @Test
    public void testToMap_WithWholeHours() {
        ZoneOffset offset = ZoneOffset.ofHours(5);          // +05:00
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        assertEquals(5, map.get(HOURS));
        assertEquals(0, map.get(MINUTES));
        assertFalse("SECONDS entry should be omitted when zero", map.containsKey(SECONDS));
    }

    @Test
    public void testToMap_WithHoursAndMinutes() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes( -3, 45); // -03:45
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        // total seconds = -3*3600 - 45*60 = -13500
        assertEquals(-3, map.get(HOURS));
        assertEquals(-45, map.get(MINUTES));
        assertFalse("SECONDS entry should be omitted when zero", map.containsKey(SECONDS));
    }

    @Test
    public void testToMap_WithSecondsComponent() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(2, 30, 45); // +02:30:45
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        assertEquals(2, map.get(HOURS));
        assertEquals(30, map.get(MINUTES));
        assertTrue("SECONDS entry must be present when non‑zero", map.containsKey(SECONDS));
        assertEquals(45, map.get(SECONDS));
    }

    @Test
    public void testToMap_ZeroOffset() {
        ZoneOffset offset = ZoneOffset.UTC; // +00:00
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        assertEquals(0, map.get(HOURS));
        assertEquals(0, map.get(MINUTES));
        assertFalse("SECONDS entry should be omitted for zero offset", map.containsKey(SECONDS));
    }

    @Test
    public void testToMap_MaximumPositiveOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(18, 0, 0); // +18:00
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        assertEquals(18, map.get(HOURS));
        assertEquals(0, map.get(MINUTES));
        assertFalse(map.containsKey(SECONDS));
    }

    @Test
    public void testToMap_MaximumNegativeOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(-18, 0, 0); // -18:00
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        assertEquals(-18, map.get(HOURS));
        assertEquals(0, map.get(MINUTES));
        assertFalse(map.containsKey(SECONDS));
    }

    @Test
    public void testToZoneId_ReturnsSameInstance() {
        ZoneId zoneId = ZoneId.of("Europe/Paris");
        ZoneId result = ZoneOffsetConversions.toZoneId(zoneId, null);
        assertSame("toZoneId should return the exact same instance passed in", zoneId, result);
    }

    /*** ----------------------------------------------------------------------
     *  Edge‑case & exception handling tests
     *  ----------------------------------------------------------------------*/

    @Test(expected = NullPointerException.class)
    public void testToMap_NullFrom() {
        // The method casts the argument to ZoneOffset, so a null should trigger NPE.
        ZoneOffsetConversions.toMap(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongObjectType() {
        // Passing an object that is not a ZoneOffset must cause a ClassCastException.
        ZoneOffsetConversions.toMap("not a ZoneOffset", null);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneId_NullFrom() {
        // Direct cast to ZoneId – null leads to NPE.
        ZoneOffsetConversions.toZoneId(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneId_WrongObjectType() {
        // Passing something that cannot be cast to ZoneId should throw ClassCastException.
        ZoneOffsetConversions.toZoneId(Integer.valueOf(5), null);
    }

    /*** ----------------------------------------------------------------------
     *  Additional sanity checks for sign handling in toMap
     *  ----------------------------------------------------------------------*/

    @Test
    public void testToMap_NegativeWithSeconds() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(-5, -20, -15); // -05:20:15
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        // Java's ZoneOffset normalises the sign, so we expect:
        // totalSeconds = -(5*3600 + 20*60 + 15) = -19215
        assertEquals(-5, map.get(HOURS));
        assertEquals(-20, map.get(MINUTES));
        assertEquals(-15, map.get(SECONDS));
    }

    @Test
    public void testToMap_PositiveWithZeroSecondsExplicit() {
        // Even if we construct with seconds=0, the map must not contain SECONDS.
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(3, 15, 0);
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        assertEquals(3, map.get(HOURS));
        assertEquals(15, map.get(MINUTES));
        assertFalse("SECONDS entry must be omitted when seconds are zero", map.containsKey(SECONDS));
    }
}
