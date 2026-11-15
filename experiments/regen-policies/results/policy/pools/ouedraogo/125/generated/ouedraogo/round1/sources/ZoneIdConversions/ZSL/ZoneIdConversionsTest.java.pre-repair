package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

/**
 * JUnit‑4 test suite for {@link ZoneIdConversions}.
 *
 * The class under test has package‑private static methods, therefore the test class
 * lives in the same package to obtain access.
 *
 * All three conversion methods ignore the {@code Converter} argument, so a
 * {@code null} reference is sufficient for the tests.
 */
public class ZoneIdConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The conversion methods do not use the Converter instance, so a null reference is safe.
        dummyConverter = null;
    }

    @Test
    public void testToMapReturnsMapWithZoneString() {
        ZoneId zone = ZoneId.of("America/New_York");

        Map<String, Object> result = ZoneIdConversions.toMap(zone, dummyConverter);

        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map must contain exactly one entry", 1, result.size());
        assertTrue("Map must contain the key \"zone\"", result.containsKey("zone"));
        assertEquals("The stored zone string must match ZoneId.toString()", zone.toString(),
                result.get("zone"));
    }

    @Test
    public void testToMapCreatesNewMapEachCall() {
        ZoneId zone = ZoneId.of("UTC");

        Map<String, Object> first = ZoneIdConversions.toMap(zone, dummyConverter);
        Map<String, Object> second = ZoneIdConversions.toMap(zone, dummyConverter);

        assertNotSame("Each call should produce a new map instance", first, second);
        assertEquals(first, second);
    }

    @Test
    public void testToTimeZoneMatchesZoneIdId() {
        // UTC is a simple, well‑known ID
        ZoneId utcZone = ZoneId.of("UTC");
        TimeZone tz = ZoneIdConversions.toTimeZone(utcZone, dummyConverter);
        assertNotNull("TimeZone should not be null", tz);
        assertEquals("TimeZone ID must match the ZoneId string", utcZone.getId(), tz.getID());

        // A region with daylight‑saving rules
        ZoneId newYork = ZoneId.of("America/New_York");
        tz = ZoneIdConversions.toTimeZone(newYork, dummyConverter);
        assertEquals("TimeZone ID must match the ZoneId string for New York", newYork.getId(),
                tz.getID());
    }

    @Test
    public void testToZoneOffsetMatchesCurrentRules() {
        // Test a zone that has a non‑zero offset and DST rules
        ZoneId paris = ZoneId.of("Europe/Paris");
        ZoneOffset expected = paris.getRules().getOffset(Instant.now());
        ZoneOffset actual = ZoneIdConversions.toZoneOffset(paris, dummyConverter);
        assertEquals("ZoneOffset must be the one currently applicable to the zone", expected,
                actual);

        // Test UTC which should always be ZERO
        ZoneId utc = ZoneId.of("UTC");
        expected = utc.getRules().getOffset(Instant.now());
        actual = ZoneIdConversions.toZoneOffset(utc, dummyConverter);
        assertEquals("UTC offset must be ZERO", ZoneOffset.UTC, actual);
        assertEquals("Expected offset for UTC is ZERO", expected, actual);
    }

    @Test
    public void testMethodsAcceptNullConverterWithoutFailure() {
        ZoneId zone = ZoneId.of("Asia/Tokyo");

        // toMap
        Map<String, Object> map = ZoneIdConversions.toMap(zone, null);
        assertEquals(zone.toString(), map.get("zone"));

        // toTimeZone
        TimeZone tz = ZoneIdConversions.toTimeZone(zone, null);
        assertEquals(zone.getId(), tz.getID());

        // toZoneOffset
        ZoneOffset offset = ZoneIdConversions.toZoneOffset(zone, null);
        ZoneOffset expected = zone.getRules().getOffset(Instant.now());
        assertEquals(expected, offset);
    }
}
