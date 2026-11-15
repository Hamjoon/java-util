package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Test;

/**
 * Unit tests for {@link ZoneIdConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Correct conversion of a {@link ZoneId} to a {@link Map} representation.</li>
 *   <li>Correct conversion of a {@link ZoneId} to a {@link TimeZone}.</li>
 *   <li>Handling of {@code null} and invalid input values.</li>
 *   <li>Verification that the {@code Converter} argument is ignored.</li>
 * </ul>
 */
public class ZoneIdConversionsTest {

    /* -------------------------------------------------
     * Tests for toMap(...)
     * ------------------------------------------------- */

    @Test
    public void testToMapWithUtcZoneId() {
        ZoneId utc = ZoneId.of("UTC");
        Map<String, Object> result = ZoneIdConversions.toMap(utc, null);

        assertNotNull("Result map must not be null", result);
        assertEquals("Map must contain exactly one entry", 1, result.size());
        assertTrue("Result should be a CompactLinkedMap", result.getClass().getSimpleName().equals("CompactLinkedMap"));
        assertEquals("UTC", result.get("zone"));
    }

    @Test
    public void testToMapWithAmericaNewYork() {
        ZoneId ny = ZoneId.of("America/New_York");
        Map<String, Object> result = ZoneIdConversions.toMap(ny, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("America/New_York", result.get("zone"));
    }

    @Test
    public void testToMapIgnoresConverterArgument() {
        ZoneId utc = ZoneId.of("UTC");
        // Pass a non‑null Converter (the implementation is not needed for this test)
        Converter dummyConverter = null; // the method does not use it, null is acceptable
        Map<String, Object> result = ZoneIdConversions.toMap(utc, dummyConverter);

        assertEquals("UTC", result.get("zone"));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapWithNullFrom() {
        ZoneIdConversions.toMap(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWithInvalidFromType() {
        ZoneIdConversions.toMap("not-a-zone-id", null);
    }

    /* -------------------------------------------------
     * Tests for toTimeZone(...)
     * ------------------------------------------------- */

    @Test
    public void testToTimeZoneWithUtcZoneId() {
        ZoneId utc = ZoneId.of("UTC");
        TimeZone tz = ZoneIdConversions.toTimeZone(utc, null);

        assertNotNull("TimeZone must not be null", tz);
        assertEquals("UTC", tz.getID());
    }

    @Test
    public void testToTimeZoneWithAmericaNewYork() {
        ZoneId ny = ZoneId.of("America/New_York");
        TimeZone tz = ZoneIdConversions.toTimeZone(ny, null);

        assertNotNull(tz);
        // TimeZone IDs for region based zones keep the same string
        assertEquals("America/New_York", tz.getID());
    }

    @Test
    public void testToTimeZoneWithOffsetZoneId() {
        ZoneId offset = ZoneId.of("+02:00");
        TimeZone tz = ZoneIdConversions.toTimeZone(offset, null);

        assertNotNull(tz);
        // Java's TimeZone represents a fixed offset as "GMT+02:00"
        assertEquals("GMT+02:00", tz.getID());
    }

    @Test
    public void testToTimeZoneIgnoresConverterArgument() {
        ZoneId utc = ZoneId.of("UTC");
        // Pass a non‑null Converter (method does not use it)
        Converter dummyConverter = null;
        TimeZone tz = ZoneIdConversions.toTimeZone(utc, dummyConverter);

        assertEquals("UTC", tz.getID());
    }

    @Test(expected = NullPointerException.class)
    public void testToTimeZoneWithNullFrom() {
        ZoneIdConversions.toTimeZone(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToTimeZoneWithInvalidFromType() {
        ZoneIdConversions.toTimeZone(12345, null);
    }
}
