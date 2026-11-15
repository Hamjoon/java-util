package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.Map;
import java.util.TimeZone;

/**
 * Unit tests for {@link ZoneIdConversions}.
 *
 * The tests are placed in the same package as {@code ZoneIdConversions}
 * so that the package‑private static methods can be accessed directly.
 *
 * The test suite covers:
 * <ul>
 *   <li>Normal (happy‑path) usage of each conversion method.</li>
 *   <li>Null input handling – the methods cast the {@code from} argument
 *       without checking for {@code null}, therefore a {@link NullPointerException}
 *       is expected.</li>
 *   <li>Invalid type handling – passing an object that is not a {@link ZoneId}
 *       results in a {@link ClassCastException}.</li>
 *   <li>Edge cases for {@code toZoneOffset} using a ZoneId with a fixed offset
 *       (e.g., {@code UTC+02:00}) to verify that the returned {@link ZoneOffset}
 *       matches the expected constant value.</li>
 * </ul>
 *
 * The {@code Converter} parameter of the conversion methods is not used
 * internally, therefore the tests simply pass {@code null} for brevity.
 */
public class ZoneIdConversionsTest
{
    // -----------------------------------------------------------------------
    // Helper constants
    // -----------------------------------------------------------------------
    private static final Converter DUMMY_CONVERTER = null; // not used by the methods

    // -----------------------------------------------------------------------
    // Tests for toMap(...)
    // -----------------------------------------------------------------------
    @Test
    public void testToMap_WithValidZoneId_ReturnsCorrectMap()
    {
        ZoneId zone = ZoneId.of("UTC");
        Map<String, Object> result = ZoneIdConversions.toMap(zone, DUMMY_CONVERTER);

        assertNotNull("Resulting map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map must contain key 'zone'", result.containsKey("zone"));
        assertEquals("Map value must be the zone ID string", "UTC", result.get("zone"));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput_ThrowsNullPointerException()
    {
        ZoneIdConversions.toMap(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_InvalidType_ThrowsClassCastException()
    {
        ZoneIdConversions.toMap("not-a-zone-id", DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------------
    // Tests for toTimeZone(...)
    // -----------------------------------------------------------------------
    @Test
    public void testToTimeZone_WithValidZoneId_ReturnsMatchingTimeZone()
    {
        ZoneId zone = ZoneId.of("America/New_York");
        TimeZone tz = ZoneIdConversions.toTimeZone(zone, DUMMY_CONVERTER);

        assertNotNull("Returned TimeZone must not be null", tz);
        // TimeZone.getID() returns the original zone ID for known IDs
        assertEquals("TimeZone ID should match the original ZoneId",
                     "America/New_York", tz.getID());
    }

    @Test(expected = NullPointerException.class)
    public void testToTimeZone_NullInput_ThrowsNullPointerException()
    {
        ZoneIdConversions.toTimeZone(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToTimeZone_InvalidType_ThrowsClassCastException()
    {
        ZoneIdConversions.toTimeZone(12345, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------------
    // Tests for toZoneOffset(...)
    // -----------------------------------------------------------------------
    @Test
    public void testToZoneOffset_WithFixedOffsetZone_ReturnsExactOffset()
    {
        // ZoneId.of with a "UTC+02:00" pattern yields a ZoneOffset of +02:00
        ZoneId fixedZone = ZoneId.of("UTC+02:00");
        ZoneOffset offset = ZoneIdConversions.toZoneOffset(fixedZone, DUMMY_CONVERTER);

        assertNotNull("Returned ZoneOffset must not be null", offset);
        assertEquals("Offset should be +02:00", ZoneOffset.ofHours(2), offset);
    }

    @Test
    public void testToZoneOffset_WithUtcZone_ReturnsZeroOffset()
    {
        ZoneId utc = ZoneId.of("UTC");
        ZoneOffset offset = ZoneIdConversions.toZoneOffset(utc, DUMMY_CONVERTER);

        assertNotNull(offset);
        assertEquals(ZoneOffset.UTC, offset);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneOffset_NullInput_ThrowsNullPointerException()
    {
        ZoneIdConversions.toZoneOffset(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneOffset_InvalidType_ThrowsClassCastException()
    {
        ZoneIdConversions.toZoneOffset(new Object(), DUMMY_CONVERTER);
    }
}
