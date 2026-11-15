package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.HOURS;
import static com.cedarsoftware.util.convert.MapConversions.MINUTES;
import static com.cedarsoftware.util.convert.MapConversions.SECONDS;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ZoneOffsetConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Conversion of various {@link ZoneOffset} instances to a {@link Map}
 *       (including zero offset, positive/negative offsets, with and without seconds).</li>
 *   <li>Verification that the resulting map is a {@link CompactLinkedMap} and
 *       contains the expected keys/values.</li>
 *   <li>Behaviour of {@code toZoneId} for both {@link ZoneId} and {@link ZoneOffset}
 *       inputs, including null handling.</li>
 * </ul>
 *
 * No mocking is used – real objects are constructed via public APIs only.
 */
@RunWith(JUnit4.class)
public class ZoneOffsetConversionsTest {

    /* ---------------------------------------------------------------------- */
    /* Helper methods                                                         */
    /* ---------------------------------------------------------------------- */

    /**
     * Calls {@link ZoneOffsetConversions#toMap(Object, Converter)} with a {@code null}
     * converter (the method does not use the converter argument).
     */
    private static Map<String, Object> toMap(ZoneOffset offset) {
        return ZoneOffsetConversions.toMap(offset, null);
    }

    /**
     * Calls {@link ZoneOffsetConversions#toZoneId(Object, Converter)} with a {@code null}
     * converter (the method does not use the converter argument).
     */
    private static ZoneId toZoneId(Object source) {
        return ZoneOffsetConversions.toZoneId(source, null);
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for toMap()                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMap_ZeroOffset() {
        ZoneOffset offset = ZoneOffset.UTC; // +00:00
        Map<String, Object> map = toMap(offset);

        // Verify map type
        assertTrue("Result should be a CompactLinkedMap", map instanceof CompactLinkedMap);

        // Expected values
        assertEquals("Hours for UTC must be 0", 0, map.get(HOURS));
        assertEquals("Minutes for UTC must be 0", 0, map.get(MINUTES));
        assertFalse("SECONDS entry must be omitted when zero", map.containsKey(SECONDS));
    }

    @Test
    public void testToMap_PositiveOffset_WithSeconds() {
        // +05:30:15
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(5, 30, 15);
        Map<String, Object> map = toMap(offset);

        assertEquals("Hours mismatch", 5, map.get(HOURS));
        assertEquals("Minutes mismatch", 30, map.get(MINUTES));
        assertTrue("SECONDS entry must be present", map.containsKey(SECONDS));
        assertEquals("Seconds mismatch", 15, map.get(SECONDS));
    }

    @Test
    public void testToMap_PositiveOffset_NoSeconds() {
        // +02:45 (no seconds)
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(2, 45);
        Map<String, Object> map = toMap(offset);

        assertEquals("Hours mismatch", 2, map.get(HOURS));
        assertEquals("Minutes mismatch", 45, map.get(MINUTES));
        assertFalse("SECONDS entry must be omitted when zero", map.containsKey(SECONDS));
    }

    @Test
    public void testToMap_NegativeOffset_WithSeconds() {
        // -02:15:30
        ZoneOffset offset = ZoneOffset.of("-02:15:30");
        Map<String, Object> map = toMap(offset);

        assertEquals("Hours mismatch (negative)", -2, map.get(HOURS));
        assertEquals("Minutes mismatch (negative)", -15, map.get(MINUTES));
        assertTrue("SECONDS entry must be present for negative offset", map.containsKey(SECONDS));
        assertEquals("Seconds mismatch (negative)", -30, map.get(SECONDS));
    }

    @Test
    public void testToMap_NegativeOffset_NoSeconds() {
        // -07:00 (no seconds)
        ZoneOffset offset = ZoneOffset.ofHours(-7);
        Map<String, Object> map = toMap(offset);

        assertEquals("Hours mismatch (negative)", -7, map.get(HOURS));
        assertEquals("Minutes must be 0 for whole hour offset", 0, map.get(MINUTES));
        assertFalse("SECONDS entry must be omitted when zero", map.containsKey(SECONDS));
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_InvalidInput_NotZoneOffset() {
        // The method casts the input to ZoneOffset; passing a different type should
        // result in a ClassCastException.
        ZoneOffsetConversions.toMap("not a ZoneOffset", null);
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for toZoneId()                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToZoneId_WithZoneId() {
        ZoneId zone = ZoneId.of("Europe/Paris");
        ZoneId result = toZoneId(zone);
        assertSame("Result should be the same instance passed in", zone, result);
    }

    @Test
    public void testToZoneId_WithZoneOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(3, 0, 0);
        ZoneId result = toZoneId(offset);
        assertSame("Result should be the same ZoneOffset instance (ZoneOffset extends ZoneId)", offset, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneId_NullInput() {
        // Casting null to ZoneId triggers NPE.
        toZoneId(null);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneId_InvalidInput_NotZoneId() {
        // Passing an object that is not a ZoneId/ZoneOffset should cause a ClassCastException.
        ZoneOffsetConversions.toZoneId(12345, null);
    }
}
