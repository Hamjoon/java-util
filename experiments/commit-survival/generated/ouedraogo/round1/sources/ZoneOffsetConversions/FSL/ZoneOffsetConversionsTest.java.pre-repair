package com.cedarsoftware.util.convert;

import static com.cedarsoftware.util.convert.MapConversions.HOURS;
import static com.cedarsoftware.util.convert.MapConversions.MINUTES;
import static com.cedarsoftware.util.convert.MapConversions.SECONDS;
import static org.junit.Assert.*;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link ZoneOffsetConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Conversion of a {@link ZoneOffset} to a {@link Map} with correct hour, minute and optional second entries.</li>
 *   <li>Handling of positive, negative and zero offsets.</li>
 *   <li>Omission of the {@code SECONDS} entry when the offset contains no seconds.</li>
 *   <li>Simple pass‑through conversion of a {@link ZoneId} via {@code toZoneId}.</li>
 * </ul>
 *
 * No external mocking frameworks are used – real objects are created through their public
 * constructors or static factory methods as required by the specification.
 */
public class ZoneOffsetConversionsTest {

    // ----------------------------------------------------------------------
    // toMap() tests
    // ----------------------------------------------------------------------

    @Test
    public void testToMap_ZeroOffset() {
        ZoneOffset offset = ZoneOffset.UTC; // totalSeconds = 0
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        assertEquals("Hours for UTC should be 0", 0, map.get(HOURS));
        assertEquals("Minutes for UTC should be 0", 0, map.get(MINUTES));
        assertFalse("Seconds entry must be omitted for zero seconds", map.containsKey(SECONDS));
    }

    @Test
    public void testToMap_PositiveWholeHours() {
        ZoneOffset offset = ZoneOffset.ofHours(5); // +05:00
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        assertEquals(5, map.get(HOURS));
        assertEquals(0, map.get(MINUTES));
        assertFalse("Seconds entry must be omitted when seconds are zero", map.containsKey(SECONDS));
    }

    @Test
    public void testToMap_PositiveHoursAndMinutes() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(2, 45); // +02:45
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        assertEquals(2, map.get(HOURS));
        assertEquals(45, map.get(MINUTES));
        assertFalse("Seconds entry must be omitted when seconds are zero", map.containsKey(SECONDS));
    }

    @Test
    public void testToMap_PositiveHoursMinutesSeconds() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(3, 15, 30); // +03:15:30
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        assertEquals(3, map.get(HOURS));
        assertEquals(15, map.get(MINUTES));
        assertTrue("Seconds entry must be present when seconds are non‑zero", map.containsKey(SECONDS));
        assertEquals(30, map.get(SECONDS));
    }

    @Test
    public void testToMap_NegativeWholeHours() {
        ZoneOffset offset = ZoneOffset.ofHours(-4); // -04:00
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        assertEquals(-4, map.get(HOURS));
        assertEquals(0, map.get(MINUTES));
        assertFalse(map.containsKey(SECONDS));
    }

    @Test
    public void testToMap_NegativeHoursAndMinutes() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(-5, 30); // -05:30
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        assertEquals(-5, map.get(HOURS));
        // For negative offsets the minutes are positive in the API but the total seconds are negative,
        // resulting in a negative minute value after the division performed in the implementation.
        // The implementation calculates minutes as (totalSeconds % 3600) / 60, which yields -30.
        assertEquals(-30, map.get(MINUTES));
        assertFalse(map.containsKey(SECONDS));
    }

    @Test
    public void testToMap_NegativeHoursMinutesSeconds() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(-2, 15, 45); // -02:15:45
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        assertEquals(-2, map.get(HOURS));
        // totalSeconds = -2*3600 - 15*60 - 45 = -8145
        // minutes = ( -8145 % 3600 ) / 60 = (-8145 % 3600) = -1545 ; -1545/60 = -25
        assertEquals(-25, map.get(MINUTES));
        assertTrue(map.containsKey(SECONDS));
        // seconds = totalSeconds % 60 = -45
        assertEquals(-45, map.get(SECONDS));
    }

    // ----------------------------------------------------------------------
    // toZoneId() tests
    // ----------------------------------------------------------------------

    @Test
    public void testToZoneId_WithZoneIdInstance() {
        ZoneId original = ZoneId.of("Europe/Paris");
        ZoneId result = ZoneOffsetConversions.toZoneId(original, null);
        assertSame("toZoneId should return the exact same instance", original, result);
    }

    @Test
    public void testToZoneId_WithZoneOffsetInstance() {
        // ZoneOffset is a subclass of ZoneId, so it is also a valid input.
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(1, 0, 0);
        ZoneId result = ZoneOffsetConversions.toZoneId(offset, null);
        assertSame("toZoneId should return the same ZoneOffset instance when passed as ZoneId", offset, result);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneId_WithInvalidTypeThrows() {
        // Passing a non‑ZoneId object should cause a ClassCastException,
        // because the method performs an unchecked cast.
        Object notAZoneId = "not a zone";
        ZoneOffsetConversions.toZoneId(notAZoneId, null);
    }
}
