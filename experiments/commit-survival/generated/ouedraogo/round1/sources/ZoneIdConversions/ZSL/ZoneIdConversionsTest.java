/**
 * Unit tests for {@link ZoneIdConversions}.
 *
 * The tests are placed in the same package as {@code ZoneIdConversions}
 * so that the package‑private static methods can be accessed directly.
 *
 * The {@code Converter} argument is not used by the methods under test,
 * therefore a minimal {@code Converter} instance is created with a {@code null}
 * {@code ConverterOptions}.  This satisfies the compiler without invoking any
 * conversion logic.
 */
package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactLinkedMap;


import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class ZoneIdConversionsTest {

    /** A dummy {@link Converter} instance – the conversion methods ignore it. */
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance.
        // Passing null is acceptable for the purpose of these tests because
        // the methods under test never touch the Converter.
        dummyConverter = new Converter(null);
    }

    @After
    public void tearDown() {
        dummyConverter = null;
    }

    /* --------------------------------------------------------------------- */
    /*                     Tests for toMap(Object, Converter)                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToMap_WithStandardZoneId_ReturnsCorrectMap() {
        ZoneId zone = ZoneId.of("America/New_York");
        Map<String, Object> result = ZoneIdConversions.toMap(zone, dummyConverter);

        assertNotNull("Resulting map must not be null", result);
        // The implementation uses CompactLinkedMap; verify the concrete type.
        assertTrue("Result should be an instance of CompactLinkedMap",
                result instanceof CompactLinkedMap);

        // Exactly one entry with key "zone".
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map must contain the key 'zone'", result.containsKey("zone"));

        // The value must be the string representation of the ZoneId.
        assertEquals("ZoneId string value must match", zone.toString(),
                result.get("zone"));
    }

    @Test
    public void testToMap_WithUtcZoneId_ReturnsCorrectMap() {
        ZoneId utc = ZoneId.of("UTC");
        Map<String, Object> result = ZoneIdConversions.toMap(utc, dummyConverter);

        assertEquals("Map size must be 1", 1, result.size());
        assertEquals("Key 'zone' must map to UTC", "UTC", result.get("zone"));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_WithNullZoneId_ThrowsNPE() {
        // The implementation calls zoneID.toString(); a null argument will
        // cause a NullPointerException.
        ZoneIdConversions.toMap(null, dummyConverter);
    }

    /* --------------------------------------------------------------------- */
    /*                  Tests for toTimeZone(Object, Converter)              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToTimeZone_WithStandardZoneId_ReturnsMatchingTimeZone() {
        ZoneId zone = ZoneId.of("Europe/Paris");
        TimeZone tz = ZoneIdConversions.toTimeZone(zone, dummyConverter);

        assertNotNull("Returned TimeZone must not be null", tz);
        // TimeZone#getID returns the same string used to create it.
        assertEquals("TimeZone ID must match ZoneId ID", zone.getId(), tz.getID());
    }

    @Test
    public void testToTimeZone_WithUtcZoneId_ReturnsUtcTimeZone() {
        ZoneId utc = ZoneId.of("UTC");
        TimeZone tz = ZoneIdConversions.toTimeZone(utc, dummyConverter);

        assertEquals("UTC ZoneId should map to UTC TimeZone", "UTC", tz.getID());
        // UTC has a raw offset of 0 milliseconds.
        assertEquals("UTC offset must be zero", 0, tz.getRawOffset());
    }

    @Test
    public void testToTimeZone_WithNullZoneId_ReturnsGmtTimeZone() {
        // According to java.util.TimeZone#getTimeZone(null) the result is GMT.
        TimeZone tz = ZoneIdConversions.toTimeZone(null, dummyConverter);

        assertNotNull("Resulting TimeZone must not be null", tz);
        assertEquals("When null is supplied, TimeZone should be GMT", "GMT", tz.getID());
    }

    @Test
    public void testToTimeZone_ConsistencyBetweenMethods() {
        // Verify that converting a ZoneId to a Map and then back to a TimeZone
        // yields the same TimeZone ID.
        ZoneId zone = ZoneId.of("Asia/Tokyo");
        Map<String, Object> map = ZoneIdConversions.toMap(zone, dummyConverter);
        // Re‑create the ZoneId from the map entry.
        ZoneId reconstructed = ZoneId.of((String) map.get("zone"));
        TimeZone tzFromMap = ZoneIdConversions.toTimeZone(reconstructed, dummyConverter);
        TimeZone tzDirect = ZoneIdConversions.toTimeZone(zone, dummyConverter);

        assertEquals("TimeZone IDs must be identical", tzDirect.getID(), tzFromMap.getID());
    }
}
