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
 * The tests cover:
 *   • normal (happy‑path) behaviour,
 *   • null arguments,
 *   • arguments of the wrong type,
 *   • fixed‑offset zones,
 *   • variable‑offset zones (current DST handling),
 *   • conversion to {@link TimeZone} for both region‑based and offset‑based IDs.
 *
 * The class is placed in the same package as {@code ZoneIdConversions}
 * so that the package‑private static methods are accessible.
 */
public class ZoneIdConversionsTest {

    // The Converter parameter is unused by the implementation; a null reference is sufficient.
    private Converter dummyConverter;

    @Before
    public void setUp() {
        dummyConverter = null;   // no configuration needed
    }

    /* -------------------------------------------------------
     *  Tests for toMap(...)
     * ------------------------------------------------------- */

    @Test
    public void testToMap_WithValidZoneId_ReturnsCorrectMap() {
        ZoneId zone = ZoneId.of("UTC");
        Map<String, Object> result = ZoneIdConversions.toMap(zone, dummyConverter);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("zone"));
        assertEquals(zone.toString(), result.get("zone"));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullArgument_ThrowsNPE() {
        ZoneIdConversions.toMap(null, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongType_ThrowsClassCastException() {
        ZoneIdConversions.toMap("not-a-zone-id", dummyConverter);
    }

    /* -------------------------------------------------------
     *  Tests for toTimeZone(...)
     * ------------------------------------------------------- */

    @Test
    public void testToTimeZone_WithRegionBasedZoneId_ReturnsMatchingTimeZone() {
        ZoneId zone = ZoneId.of("America/New_York");
        TimeZone tz = ZoneIdConversions.toTimeZone(zone, dummyConverter);
        assertNotNull(tz);
        // TimeZone IDs for region based zones are identical to the ZoneId string
        assertEquals(zone.getId(), tz.getID());
    }

    @Test
    public void testToTimeZone_WithFixedOffsetZoneId_ReturnsGMTFormattedTimeZone() {
        ZoneOffset offset = ZoneOffset.ofHours(5); // +05:00
        ZoneId zone = ZoneId.ofOffset("UTC", offset); // "UTC+05:00"
        TimeZone tz = ZoneIdConversions.toTimeZone(zone, dummyConverter);
        assertNotNull(tz);
        // Expected format for offset based TimeZone IDs is "GMT+05:00"
        assertEquals("GMT+05:00", tz.getID());
    }

    @Test(expected = NullPointerException.class)
    public void testToTimeZone_NullArgument_ThrowsNPE() {
        ZoneIdConversions.toTimeZone(null, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToTimeZone_WrongType_ThrowsClassCastException() {
        ZoneIdConversions.toTimeZone(12345, dummyConverter);
    }

    /* -------------------------------------------------------
     *  Tests for toZoneOffset(...)
     * ------------------------------------------------------- */

    @Test
    public void testToZoneOffset_WithFixedOffsetZoneId_ReturnsExactOffset() {
        ZoneOffset expected = ZoneOffset.ofHours(-7); // -07:00
        ZoneId zone = ZoneId.ofOffset("UTC", expected); // "UTC-07:00"
        ZoneOffset actual = ZoneIdConversions.toZoneOffset(zone, dummyConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToZoneOffset_WithVariableOffsetZoneId_ReturnsCurrentOffset() {
        ZoneId zone = ZoneId.of("America/Los_Angeles"); // Pacific Time (may be PST or PDT)
        ZoneOffset expected = zone.getRules().getOffset(Instant.now());
        ZoneOffset actual = ZoneIdConversions.toZoneOffset(zone, dummyConverter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneOffset_NullArgument_ThrowsNPE() {
        ZoneIdConversions.toZoneOffset(null, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneOffset_WrongType_ThrowsClassCastException() {
        ZoneIdConversions.toZoneOffset(new Object(), dummyConverter);
    }
}
