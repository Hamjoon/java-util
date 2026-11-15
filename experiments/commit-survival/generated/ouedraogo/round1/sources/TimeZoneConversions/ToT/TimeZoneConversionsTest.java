package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TimeZoneConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical usage with standard time‑zone IDs (e.g., "UTC", "America/New_York")</li>
 *   <li>Edge cases such as custom GMT offsets (e.g., "GMT+02:00")</li>
 *   <li>Null input handling – all three conversion methods should throw {@link NullPointerException}</li>
 *   <li>Verification that {@code toMap} returns a {@link CompactLinkedMap} containing the expected key/value</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the current implementation, therefore a {@code null}
 * reference is supplied – this matches the library’s real‑world usage where the argument can be omitted.
 */
public class TimeZoneConversionsTest {

    /** Helper constant – the map key used by {@code TimeZoneConversions.toMap}. */
    private static final String ZONE_KEY = ZONE; // imported from MapConversions

    /** A dummy {@code Converter} instance. The implementation under test does not use it. */
    private static final Converter DUMMY_CONVERTER = null;

    /* ----------------------------------------------------------------------
     * toString(Object, Converter) tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToString_WithStandardZone_ReturnsId() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        String result = TimeZoneConversions.toString(tz, DUMMY_CONVERTER);
        assertEquals("UTC", result);
    }

    @Test
    public void testToString_WithRegionZone_ReturnsId() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        String result = TimeZoneConversions.toString(tz, DUMMY_CONVERTER);
        assertEquals("America/New_York", result);
    }

    @Test
    public void testToString_WithCustomGmtOffset_ReturnsId() {
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        String result = TimeZoneConversions.toString(tz, DUMMY_CONVERTER);
        // Java normalises the ID to "GMT+02:00"
        assertEquals("GMT+02:00", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullTimeZone_ThrowsNPE() {
        TimeZoneConversions.toString(null, DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * toZoneId(Object, Converter) tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToZoneId_WithStandardZone_ReturnsMatchingZoneId() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        ZoneId zoneId = TimeZoneConversions.toZoneId(tz, DUMMY_CONVERTER);
        assertEquals(ZoneId.of("UTC"), zoneId);
    }

    @Test
    public void testToZoneId_WithRegionZone_ReturnsMatchingZoneId() {
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        ZoneId zoneId = TimeZoneConversions.toZoneId(tz, DUMMY_CONVERTER);
        assertEquals(ZoneId.of("Europe/Paris"), zoneId);
    }

    @Test
    public void testToZoneId_WithCustomGmtOffset_ReturnsMatchingZoneId() {
        TimeZone tz = TimeZone.getTimeZone("GMT-05:00");
        ZoneId zoneId = TimeZoneConversions.toZoneId(tz, DUMMY_CONVERTER);
        assertEquals(ZoneId.of("GMT-05:00"), zoneId);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneId_NullTimeZone_ThrowsNPE() {
        TimeZoneConversions.toZoneId(null, DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * toMap(Object, Converter) tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToMap_WithStandardZone_ReturnsMapWithCorrectEntry() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, DUMMY_CONVERTER);

        // Verify the concrete implementation
        assertTrue("Result should be a CompactLinkedMap", map instanceof CompactLinkedMap);

        // Verify the expected key/value
        assertTrue("Map must contain the ZONE key", map.containsKey(ZONE_KEY));
        assertEquals("Map value for ZONE key must match the TimeZone ID", "UTC", map.get(ZONE_KEY));
    }

    @Test
    public void testToMap_WithRegionZone_ReturnsMapWithCorrectEntry() {
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, DUMMY_CONVERTER);

        assertTrue(map instanceof CompactLinkedMap);
        assertEquals("America/Los_Angeles", map.get(ZONE_KEY));
    }

    @Test
    public void testToMap_WithCustomGmtOffset_ReturnsMapWithCorrectEntry() {
        TimeZone tz = TimeZone.getTimeZone("GMT+03:30");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, DUMMY_CONVERTER);

        assertTrue(map instanceof CompactLinkedMap);
        assertEquals("GMT+03:30", map.get(ZONE_KEY));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullTimeZone_ThrowsNPE() {
        TimeZoneConversions.toMap(null, DUMMY_CONVERTER);
    }
}
