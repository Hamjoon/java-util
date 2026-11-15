package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactMap;


import static org.junit.Assert.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Test;

/**
 * Unit tests for {@link ZoneIdConversions}.  All public methods are exercised,
 * including normal operation, edge‑cases and expected failure modes.
 *
 * The class under test is package‑private, therefore the test resides in the
 * same package to gain access to the static methods.
 */
public class ZoneIdConversionsTest {

    // -----------------------------------------------------------------------
    // Helper – a no‑op Converter instance (the methods under test do not use it)
    // -----------------------------------------------------------------------
    private static final Converter DUMMY_CONVERTER = new Converter(null);

    // -----------------------------------------------------------------------
    // toMap(..) tests
    // -----------------------------------------------------------------------

    @Test
    public void testToMapWithStandardZoneId() {
        ZoneId zone = ZoneId.of("America/New_York");
        Map<String, Object> result = ZoneIdConversions.toMap(zone, DUMMY_CONVERTER);

        assertNotNull("Result map must not be null", result);
        assertEquals("Map must contain exactly one entry", 1, result.size());
        assertTrue("Map must contain key \"zone\"", result.containsKey("zone"));
        assertEquals("Value must be the zone's string representation",
                "America/New_York", result.get("zone"));
    }

    @Test
    public void testToMapWithUtcZoneId() {
        ZoneId utc = ZoneId.of("UTC");               // yields ZoneOffset.UTC ("Z")
        Map<String, Object> result = ZoneIdConversions.toMap(utc, DUMMY_CONVERTER);

        assertEquals("ZoneId.of(\"UTC\") should be represented as \"Z\"",
                "Z", result.get("zone"));
    }

    @Test
    public void testToMapWithOffsetZoneId() {
        ZoneOffset offset = ZoneOffset.ofHours(5);
        ZoneId zone = ZoneId.ofOffset("UTC", offset); // toString => "+05:00"
        Map<String, Object> result = ZoneIdConversions.toMap(zone, DUMMY_CONVERTER);

        assertEquals("Offset zone should be rendered as \"+05:00\"",
                "+05:00", result.get("zone"));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapWithNull() {
        ZoneIdConversions.toMap(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------------
    // toTimeZone(..) tests
    // -----------------------------------------------------------------------

    @Test
    public void testToTimeZoneWithStandardZoneId() {
        ZoneId zone = ZoneId.of("Europe/Paris");
        TimeZone tz = ZoneIdConversions.toTimeZone(zone, DUMMY_CONVERTER);

        assertNotNull("TimeZone must not be null", tz);
        // TimeZone ID should be identical to the ZoneId ID
        assertEquals("Europe/Paris", tz.getID());
    }

    @Test
    public void testToTimeZoneWithUtc() {
        ZoneId utc = ZoneId.of("UTC");
        TimeZone tz = ZoneIdConversions.toTimeZone(utc, DUMMY_CONVERTER);

        assertEquals("UTC", tz.getID());
    }

    @Test
    public void testToTimeZoneWithOffsetZoneId() {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(2, 30);
        ZoneId zone = ZoneId.ofOffset("UTC", offset); // e.g. "+02:30"
        TimeZone tz = ZoneIdConversions.toTimeZone(zone, DUMMY_CONVERTER);

        assertNotNull(tz);
        // For offset based IDs TimeZone uses the "GMT+02:30" format
        assertEquals("GMT+02:30", tz.getID());
    }

    @Test(expected = NullPointerException.class)
    public void testToTimeZoneWithNull() {
        ZoneIdConversions.toTimeZone(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------------
    // toZoneOffset(..) tests
    // -----------------------------------------------------------------------

    @Test
    public void testToZoneOffsetWithOffsetZoneId() {
        ZoneOffset expected = ZoneOffset.ofHours(-7);
        ZoneId zone = ZoneId.ofOffset("UTC", expected);
        ZoneOffset result = ZoneIdConversions.toZoneOffset(zone, DUMMY_CONVERTER);

        assertEquals("Offset zone must return the exact offset supplied",
                expected, result);
    }

    @Test
    public void testToZoneOffsetWithStandardZoneId() {
        ZoneId zone = ZoneId.of("America/Los_Angeles");
        ZoneOffset expected = zone.getRules().getOffset(Instant.now());

        ZoneOffset result = ZoneIdConversions.toZoneOffset(zone, DUMMY_CONVERTER);
        assertEquals("Resulting offset must match the zone's rules at current instant",
                expected, result);
    }

    @Test
    public void testToZoneOffsetWithUtc() {
        ZoneId utc = ZoneId.of("UTC");
        ZoneOffset result = ZoneIdConversions.toZoneOffset(utc, DUMMY_CONVERTER);
        assertEquals("UTC should always have zero offset", ZoneOffset.UTC, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneOffsetWithNull() {
        ZoneIdConversions.toZoneOffset(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------------
    // Additional sanity check: the map returned by toMap is a CompactMap
    // -----------------------------------------------------------------------
    @Test
    public void testToMapReturnsCompactMapImplementation() {
        ZoneId zone = ZoneId.of("Asia/Tokyo");
        Map<String, Object> map = ZoneIdConversions.toMap(zone, DUMMY_CONVERTER);
        assertTrue("Returned map should be an instance of CompactMap",
                map instanceof CompactMap);
    }
}
