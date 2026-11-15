package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Comprehensive JUnit‑4 test suite for {@code ZoneOffsetConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical positive, negative and zero offsets.</li>
 *   <li>Offsets that contain a non‑zero seconds component.</li>
 *   <li>Verification that the {@code SECONDS} entry is omitted when seconds == 0.</li>
 *   <li>Behaviour when {@code null} is supplied for the source object.</li>
 *   <li>Behaviour when an object of an unexpected type is supplied.</li>
 *   <li>Simple pass‑through of {@code ZoneId} via {@code toZoneId}.</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, therefore
 * a {@code null} reference is passed in all test cases.
 */
public class ZoneOffsetConversionsTest {

    /*** --------------------------------------------------------------------
     *  Tests for {@link ZoneOffsetConversions#toMap(Object, Converter)}
     *  ----------------------------------------------------------------- ***/

    @Test
    public void testToMap_WithPositiveOffset_NoSeconds() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(5, 30); // +05:30
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        Assert.assertEquals("Hours should be 5", 5, map.get(HOURS));
        Assert.assertEquals("Minutes should be 30", 30, map.get(MINUTES));
        Assert.assertFalse("Seconds entry must be absent when seconds == 0", map.containsKey(SECONDS));
    }

    @Test
    public void testToMap_WithNegativeOffset_NoSeconds() {
        // -04:45
        ZoneOffset offset = ZoneOffset.ofTotalSeconds((-4 * 3600) + (-45 * 60));
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        Assert.assertEquals("Hours should be -4", -4, map.get(HOURS));
        Assert.assertEquals("Minutes should be -45", -45, map.get(MINUTES));
        Assert.assertFalse("Seconds entry must be absent when seconds == 0", map.containsKey(SECONDS));
    }

    @Test
    public void testToMap_WithPositiveOffset_WithSeconds() {
        // +01:02:03
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(1, 2, 3);
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        Assert.assertEquals("Hours should be 1", 1, map.get(HOURS));
        Assert.assertEquals("Minutes should be 2", 2, map.get(MINUTES));
        Assert.assertEquals("Seconds should be 3", 3, map.get(SECONDS));
    }

    @Test
    public void testToMap_WithNegativeOffset_WithSeconds() {
        // -02:03:04  (total seconds = -(2*3600 + 3*60 + 4))
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(-(2 * 3600 + 3 * 60 + 4));
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        Assert.assertEquals("Hours should be -2", -2, map.get(HOURS));
        Assert.assertEquals("Minutes should be -3", -3, map.get(MINUTES));
        Assert.assertEquals("Seconds should be -4", -4, map.get(SECONDS));
    }

    @Test
    public void testToMap_WithZeroOffset() {
        ZoneOffset offset = ZoneOffset.UTC; // +00:00
        Map<String, Object> map = ZoneOffsetConversions.toMap(offset, null);

        Assert.assertEquals("Hours should be 0", 0, map.get(HOURS));
        Assert.assertEquals("Minutes should be 0", 0, map.get(MINUTES));
        Assert.assertFalse("Seconds entry must be absent for zero offset", map.containsKey(SECONDS));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullSource() {
        ZoneOffsetConversions.toMap(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongSourceType() {
        // Passing a String instead of a ZoneOffset forces a ClassCastException.
        ZoneOffsetConversions.toMap("not a ZoneOffset", null);
    }

    /*** --------------------------------------------------------------------
     *  Tests for {@link ZoneOffsetConversions#toZoneId(Object, Converter)}
     *  ----------------------------------------------------------------- ***/

    @Test
    public void testToZoneId_WithZoneIdImplementation() {
        ZoneId zoneId = ZoneId.of("America/New_York");
        ZoneId result = ZoneOffsetConversions.toZoneId(zoneId, null);
        Assert.assertSame("Result should be the same instance that was passed in", zoneId, result);
    }

    @Test
    public void testToZoneId_WithZoneOffset() {
        // ZoneOffset is a subclass of ZoneId, so this should succeed.
        ZoneOffset offset = ZoneOffset.ofHours(2);
        ZoneId result = ZoneOffsetConversions.toZoneId(offset, null);
        Assert.assertSame("Result should be the same ZoneOffset instance", offset, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneId_NullSource() {
        ZoneOffsetConversions.toZoneId(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneId_WrongSourceType() {
        // Passing a non‑ZoneId object triggers a ClassCastException.
        ZoneOffsetConversions.toZoneId(Integer.valueOf(42), null);
    }
}
