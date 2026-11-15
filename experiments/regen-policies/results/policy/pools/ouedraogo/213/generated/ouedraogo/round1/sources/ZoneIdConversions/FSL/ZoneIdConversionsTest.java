package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Test;

/**
 * Unit tests for {@link ZoneIdConversions}.
 */
public class ZoneIdConversionsTest {

    // ----------------------------------------------------------------------
    // toMap tests
    // ----------------------------------------------------------------------

    @Test
    public void testToMap_Utc() {
        ZoneId utc = ZoneId.of("UTC");
        Map<String, Object> map = ZoneIdConversions.toMap(utc, null);
        assertNotNull("Result map must not be null", map);
        assertEquals("Map must contain exactly one entry", 1, map.size());
        assertTrue("Map must contain key \"zone\"", map.containsKey("zone"));
        assertEquals("UTC", map.get("zone"));
    }

    @Test
    public void testToMap_RegionId() {
        ZoneId newYork = ZoneId.of("America/New_York");
        Map<String, Object> map = ZoneIdConversions.toMap(newYork, null);
        assertEquals("America/New_York", map.get("zone"));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        ZoneIdConversions.toMap(null, null);
    }

    // ----------------------------------------------------------------------
    // toTimeZone tests
    // ----------------------------------------------------------------------

    @Test
    public void testToTimeZone_Utc() {
        ZoneId utc = ZoneId.of("UTC");
        TimeZone tz = ZoneIdConversions.toTimeZone(utc, null);
        assertNotNull(tz);
        assertEquals("UTC", tz.getID());
    }

    @Test
    public void testToTimeZone_RegionId() {
        ZoneId tokyo = ZoneId.of("Asia/Tokyo");
        TimeZone tz = ZoneIdConversions.toTimeZone(tokyo, null);
        assertNotNull(tz);
        // TimeZone#getID() returns the same string that ZoneId#toString() returns
        assertEquals(tokyo.toString(), tz.getID());
    }

    @Test(expected = NullPointerException.class)
    public void testToTimeZone_NullInput() {
        ZoneIdConversions.toTimeZone(null, null);
    }

    // ----------------------------------------------------------------------
    // toZoneOffset tests
    // ----------------------------------------------------------------------

    @Test
    public void testToZoneOffset_FixedOffset() {
        // ZoneOffset is a subclass of ZoneId
        ZoneOffset offset = ZoneOffset.ofHours(2); // +02:00
        ZoneOffset result = ZoneIdConversions.toZoneOffset(offset, null);
        assertNotNull(result);
        assertEquals(offset, result);
    }

    @Test
    public void testToZoneOffset_RegionId() {
        ZoneId london = ZoneId.of("Europe/London");
        ZoneOffset expected = london.getRules().getOffset(Instant.now());
        ZoneOffset actual = ZoneIdConversions.toZoneOffset(london, null);
        assertNotNull(actual);
        assertEquals("Offset for Europe/London at current instant should match", expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneOffset_NullInput() {
        ZoneIdConversions.toZoneOffset(null, null);
    }
}
