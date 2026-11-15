package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import static org.junit.Assert.*;
import org.junit.Test;

import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;

/**
 * Unit tests for {@link TimeZoneConversions}.
 *
 * The tests are placed in the same package as the class under test
 * because the methods are package‑private (no public modifier).
 */
public class TimeZoneConversionsTest {

    // -----------------------------------------------------------------
    // toString(Object, Converter)
    // -----------------------------------------------------------------

    @Test
    public void toString_returnsTimeZoneId_forStandardZone() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        String result = TimeZoneConversions.toString(tz, null);
        assertEquals("America/New_York", result);
    }

    @Test
    public void toString_returnsTimeZoneId_forGMTOffsetZone() {
        TimeZone tz = TimeZone.getTimeZone("GMT+05:30");
        String result = TimeZoneConversions.toString(tz, null);
        assertEquals("GMT+05:30", result);
    }

    @Test(expected = NullPointerException.class)
    public void toString_throwsNPE_whenSourceIsNull() {
        TimeZoneConversions.toString(null, null);
    }

    // -----------------------------------------------------------------
    // toZoneId(Object, Converter)
    // -----------------------------------------------------------------

    @Test
    public void toZoneId_returnsEquivalentZoneId() {
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        ZoneId zoneId = TimeZoneConversions.toZoneId(tz, null);
        assertEquals(ZoneId.of("Europe/Paris"), zoneId);
    }

    @Test
    public void toZoneId_returnsEquivalentZoneId_forGMTOffset() {
        TimeZone tz = TimeZone.getTimeZone("GMT-02:00");
        ZoneId zoneId = TimeZoneConversions.toZoneId(tz, null);
        assertEquals(ZoneId.of("GMT-02:00"), zoneId);
    }

    @Test(expected = NullPointerException.class)
    public void toZoneId_throwsNPE_whenSourceIsNull() {
        TimeZoneConversions.toZoneId(null, null);
    }

    // -----------------------------------------------------------------
    // toMap(Object, Converter)
    // -----------------------------------------------------------------

    @Test
    public void toMap_containsExactlyOneEntry_withZoneKey() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, null);

        // Verify the map implementation is CompactLinkedMap
        assertTrue("Map should be instance of CompactLinkedMap", map instanceof CompactLinkedMap);

        // Verify single entry, correct key and value
        assertEquals("Map should contain exactly one entry", 1, map.size());
        assertTrue("Map should contain the ZONE key", map.containsKey(MapConversions.ZONE));
        assertEquals("Value for ZONE key must be the TimeZone ID", "UTC", map.get(MapConversions.ZONE));
    }

    @Test
    public void toMap_preservesDifferentTimeZoneIds() {
        String[] ids = { "GMT+01:00", "America/Los_Angeles", "Asia/Tokyo" };
        for (String id : ids) {
            TimeZone tz = TimeZone.getTimeZone(id);
            Map<String, Object> map = TimeZoneConversions.toMap(tz, null);
            assertEquals(id, map.get(MapConversions.ZONE));
        }
    }

    @Test(expected = NullPointerException.class)
    public void toMap_throwsNPE_whenSourceIsNull() {
        TimeZoneConversions.toMap(null, null);
    }

    // -----------------------------------------------------------------
    // Helper – a dummy Converter instance (not used by the methods)
    // -----------------------------------------------------------------

    /**
     * Provides a non‑null {@link Converter} instance so that callers that
     * prefer to pass a concrete object (instead of {@code null}) can do so.
     * The {@code Converter} API requires a {@code ConverterOptions} argument;
     * passing {@code null} is legal for the purposes of these tests because
     * the conversion methods never interact with the converter.
     */
    private static final Converter DUMMY_CONVERTER = new Converter(null);
}
