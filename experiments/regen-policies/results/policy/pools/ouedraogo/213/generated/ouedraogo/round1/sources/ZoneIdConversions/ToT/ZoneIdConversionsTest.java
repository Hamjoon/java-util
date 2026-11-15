package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;
import java.util.LinkedHashMap;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ZoneIdConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical successful conversions for each method.</li>
 *   <li>Edge‑case handling such as {@code null} arguments.</li>
 *   <li>Invalid input types that should result in {@code ClassCastException}.</li>
 *   <li>Verification that the supplied {@link Converter} argument is ignored (it may be {@code null}).</li>
 * </ul>
 */
public class ZoneIdConversionsTest {

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter instance is never used by the methods under test,
        // but we provide a non‑null instance to avoid any accidental NPEs
        // should the implementation change in the future.
        dummyConverter = new Converter(new ConverterOptions());
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // -----------------------------------------------------------------
    // toMap tests
    // -----------------------------------------------------------------
    @Test
    public void testToMap_Typical() {
        ZoneId zone = ZoneId.of("America/New_York");
        Map<String, Object> result = ZoneIdConversions.toMap(zone, dummyConverter);

        // Verify that the map is a LinkedHashMap (preserves insertion order)
        assertTrue("Result should be a LinkedHashMap", result instanceof LinkedHashMap);

        // Verify the content
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map should contain key 'zone'", result.containsKey("zone"));
        assertEquals("Value should be the zone's string representation",
                "America/New_York", result.get("zone"));
    }

    @Test
    public void testToMap_NullArgument() {
        thrown.expect(NullPointerException.class);
        ZoneIdConversions.toMap(null, dummyConverter);
    }

    @Test
    public void testToMap_InvalidType() {
        thrown.expect(ClassCastException.class);
        ZoneIdConversions.toMap("not a ZoneId", dummyConverter);
    }

    // -----------------------------------------------------------------
    // toTimeZone tests
    // -----------------------------------------------------------------
    @Test
    public void testToTimeZone_Typical() {
        ZoneId zone = ZoneId.of("Europe/Paris");
        TimeZone tz = ZoneIdConversions.toTimeZone(zone, dummyConverter);

        assertNotNull("TimeZone should not be null", tz);
        assertEquals("TimeZone ID must match ZoneId ID",
                "Europe/Paris", tz.getID());
    }

    @Test
    public void testToTimeZone_Utc() {
        ZoneId utc = ZoneId.of("UTC");
        TimeZone tz = ZoneIdConversions.toTimeZone(utc, dummyConverter);
        assertEquals("UTC TimeZone ID", "UTC", tz.getID());
        assertEquals("UTC offset hours", 0, tz.getRawOffset() / (60 * 60 * 1000));
    }

    @Test
    public void testToTimeZone_NullArgument() {
        thrown.expect(NullPointerException.class);
        ZoneIdConversions.toTimeZone(null, dummyConverter);
    }

    @Test
    public void testToTimeZone_InvalidType() {
        thrown.expect(ClassCastException.class);
        ZoneIdConversions.toTimeZone(12345, dummyConverter);
    }

    // -----------------------------------------------------------------
    // toZoneOffset tests
    // -----------------------------------------------------------------
    @Test
    public void testToZoneOffset_Utc() {
        ZoneId utc = ZoneId.of("UTC");
        ZoneOffset offset = ZoneIdConversions.toZoneOffset(utc, dummyConverter);
        assertEquals("UTC offset must be zero", ZoneOffset.UTC, offset);
    }

    @Test
    public void testToZoneOffset_FixedOffset() {
        // Etc/GMT+2 is a fixed‑offset zone of -02:00 (the sign is inverted per TZDB rules)
        ZoneId fixed = ZoneId.of("Etc/GMT+2");
        ZoneOffset expected = ZoneOffset.ofHours(-2);
        ZoneOffset actual = ZoneIdConversions.toZoneOffset(fixed, dummyConverter);
        assertEquals("Fixed offset should match expected", expected, actual);
    }

    @Test
    public void testToZoneOffset_NullArgument() {
        thrown.expect(NullPointerException.class);
        ZoneIdConversions.toZoneOffset(null, dummyConverter);
    }

    @Test
    public void testToZoneOffset_InvalidType() {
        thrown.expect(ClassCastException.class);
        ZoneIdConversions.toZoneOffset(new Object(), dummyConverter);
    }

    // -----------------------------------------------------------------
    // Converter argument handling (null is allowed because the methods never use it)
    // -----------------------------------------------------------------
    @Test
    public void testMethods_WithNullConverter() {
        ZoneId zone = ZoneId.of("UTC");

        // toMap
        Map<String, Object> map = ZoneIdConversions.toMap(zone, null);
        assertEquals("UTC", map.get("zone"));

        // toTimeZone
        TimeZone tz = ZoneIdConversions.toTimeZone(zone, null);
        assertEquals("UTC", tz.getID());

        // toZoneOffset
        ZoneOffset offset = ZoneIdConversions.toZoneOffset(zone, null);
        assertEquals(ZoneOffset.UTC, offset);
    }
}
