package com.cedarsoftware.util.convert;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

import static com.cedarsoftware.util.convert.MapConversions.ZONE_OFFSET;
import static org.junit.Assert.*;

public class ZoneOffsetConversionsTest {

    /* -------------------------------------------------
     *  toMap(...)
     * ------------------------------------------------- */

    @Test
    public void testToMap_WithVariousZoneOffsets() {
        ZoneOffset[] offsets = new ZoneOffset[] {
                ZoneOffset.UTC,                       // Z
                ZoneOffset.ofHours(2),                // +02:00
                ZoneOffset.ofHoursMinutes(-5, 30),    // -05:30
                ZoneOffset.ofHoursMinutesSeconds(1, 15, 45) // +01:15:45
        };

        for (ZoneOffset offset : offsets) {
            Map<String, Object> result = ZoneOffsetConversions.toMap(offset, null);
            assertNotNull("Result map must not be null", result);
            assertEquals("Map must contain exactly one entry", 1, result.size());
            assertTrue("Map must contain the expected key", result.containsKey(ZONE_OFFSET));
            assertEquals("Stored value must be the ISO‑8601 id of the offset",
                    offset.getId(), result.get(ZONE_OFFSET));
        }
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WithInvalidTypeThrowsException() {
        // The method casts the input to ZoneOffset; passing a non‑ZoneOffset should fail.
        ZoneOffsetConversions.toMap("not a ZoneOffset", null);
    }

    /* -------------------------------------------------
     *  toZoneId(...)
     * ------------------------------------------------- */

    @Test
    public void testToZoneId_ReturnsSameInstance_ForZoneId() {
        ZoneId original = ZoneId.of("America/New_York");
        ZoneId result = ZoneOffsetConversions.toZoneId(original, null);
        assertSame("The returned ZoneId must be the same instance that was supplied", original, result);
    }

    @Test
    public void testToZoneId_ReturnsSameInstance_ForZoneOffset() {
        ZoneOffset original = ZoneOffset.ofHoursMinutesSeconds(3, 0, 0);
        ZoneId result = ZoneOffsetConversions.toZoneId(original, null);
        assertSame("The returned ZoneId must be the same instance that was supplied (ZoneOffset)", original, result);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneId_WithInvalidTypeThrowsException() {
        // The method casts the input to ZoneId; passing an incompatible type should trigger a CCE.
        ZoneOffsetConversions.toZoneId(12345, null);
    }

    /* -------------------------------------------------
     *  toTimeZone(...)
     * ------------------------------------------------- */

    @Test
    public void testToTimeZone_UtcSpecialCase() {
        ZoneOffset utc = ZoneOffset.UTC;
        TimeZone tz = ZoneOffsetConversions.toTimeZone(utc, null);
        assertEquals("UTC must be represented as plain \"GMT\"", "GMT", tz.getID());
        assertEquals("Raw offset for UTC must be zero milliseconds", 0, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_PositiveOffset() {
        ZoneOffset offset = ZoneOffset.ofHours(2); // +02:00
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, null);
        assertEquals("ID must be GMT+02:00 for +02:00 offset", "GMT+02:00", tz.getID());
        assertEquals("Raw offset must match the offset in milliseconds",
                offset.getTotalSeconds() * 1000L, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_NegativeOffset() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(-5, 30); // -05:30
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, null);
        assertEquals("ID must be GMT-05:30 for -05:30 offset", "GMT-05:30", tz.getID());
        assertEquals("Raw offset must match the offset in milliseconds",
                offset.getTotalSeconds() * 1000L, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_WithSecondsComponent() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(1, 15, 45); // +01:15:45
        TimeZone tz = ZoneOffsetConversions.toTimeZone(offset, null);
        assertEquals("ID must include seconds when present", "GMT+01:15:45", tz.getID());
        assertEquals("Raw offset must reflect hour, minute and second components",
                offset.getTotalSeconds() * 1000L, tz.getRawOffset());
    }

    @Test(expected = ClassCastException.class)
    public void testToTimeZone_WithInvalidTypeThrowsException() {
        // The method expects a ZoneOffset; supplying another type should cause a CCE.
        ZoneOffsetConversions.toTimeZone("not a ZoneOffset", null);
    }
}
