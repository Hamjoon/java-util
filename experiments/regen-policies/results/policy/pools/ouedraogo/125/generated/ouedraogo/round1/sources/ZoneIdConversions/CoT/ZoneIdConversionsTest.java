package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ZoneIdConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Extraction of public (package‑private static) methods and their signatures.</li>
 *   <li>Basic functional verification of each conversion method.</li>
 *   <li>Edge‑case handling such as null inputs, unknown zone IDs and extreme offsets.</li>
 * </ul>
 */
public class ZoneIdConversionsTest {

    private Converter dummyConverter;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // Converter has a public constructor that only needs a ConverterOptions instance.
        // For the purpose of these tests the actual behavior of Converter is irrelevant,
        // so we can safely pass a minimal stub (null) because the methods under test never
        // invoke any Converter method.
        dummyConverter = new Converter(null);
    }

    /* ----------------------------------------------------------------------
     *  1. List of public (package‑private static) methods in ZoneIdConversions
     * ---------------------------------------------------------------------- */
    // The class defines three static methods (package‑private visibility):
    //   static Map<String,Object> toMap(Object from, Converter converter)
    //   static TimeZone toTimeZone(Object from, Converter converter)
    //   static ZoneOffset toZoneOffset(Object from, Converter converter)

    /* ----------------------------------------------------------------------
     *  2. Basic functional tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToMap_basicConversion() {
        ZoneId zoneId = ZoneId.of("America/New_York");
        Map<String, Object> result = ZoneIdConversions.toMap(zoneId, dummyConverter);

        assertNotNull("Result map must not be null", result);
        assertEquals("Map should contain exactly one entry", 1, result.size());
        assertTrue("Map must contain key 'zone'", result.containsKey("zone"));
        assertEquals("Value must be the zone ID string", "America/New_York", result.get("zone"));
    }

    @Test
    public void testToTimeZone_basicConversion() {
        ZoneId zoneId = ZoneId.of("Europe/Paris");
        TimeZone tz = ZoneIdConversions.toTimeZone(zoneId, dummyConverter);

        assertNotNull("TimeZone must not be null", tz);
        assertEquals("TimeZone ID must match the original ZoneId", "Europe/Paris", tz.getID());
    }

    @Test
    public void testToZoneOffset_basicConversion() {
        ZoneId zoneId = ZoneId.of("UTC");
        ZoneOffset offset = ZoneIdConversions.toZoneOffset(zoneId, dummyConverter);

        assertNotNull("ZoneOffset must not be null", offset);
        // UTC always has zero offset
        assertEquals("UTC offset should be zero", ZoneOffset.UTC, offset);
    }

    /* ----------------------------------------------------------------------
     *  3. Edge‑case and exception tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToMap_nullInput() {
        thrown.expect(NullPointerException.class);
        ZoneIdConversions.toMap(null, dummyConverter);
    }

    @Test
    public void testToTimeZone_nullInput() {
        thrown.expect(NullPointerException.class);
        ZoneIdConversions.toTimeZone(null, dummyConverter);
    }

    @Test
    public void testToZoneOffset_nullInput() {
        thrown.expect(NullPointerException.class);
        ZoneIdConversions.toZoneOffset(null, dummyConverter);
    }

    @Test
    public void testToMap_nonZoneIdObject() {
        thrown.expect(ClassCastException.class);
        // Pass a String which cannot be cast to ZoneId
        ZoneIdConversions.toMap("not-a-zone", dummyConverter);
    }

    @Test
    public void testToTimeZone_nonZoneIdObject() {
        thrown.expect(ClassCastException.class);
        ZoneIdConversions.toTimeZone(12345, dummyConverter);
    }

    @Test
    public void testToZoneOffset_nonZoneIdObject() {
        thrown.expect(ClassCastException.class);
        ZoneIdConversions.toZoneOffset(new Object(), dummyConverter);
    }

    @Test
    public void testToTimeZone_unknownZoneId() {
        // TimeZone.getTimeZone returns GMT for unknown IDs – verify this behaviour.
        ZoneId unknown = ZoneId.of("GMT+25:00", ZoneId.SHORT_IDS); // forces an unknown ID
        TimeZone tz = ZoneIdConversions.toTimeZone(unknown, dummyConverter);
        assertNotNull(tz);
        assertEquals("GMT", tz.getID()); // fallback ID for unknown zones
    }

    @Test
    public void testToZoneOffset_extremePositiveOffset() {
        // ZoneId.of("+18:00") is the maximum positive offset supported by ZoneOffset.
        ZoneId zoneId = ZoneId.of("+18:00");
        ZoneOffset offset = ZoneIdConversions.toZoneOffset(zoneId, dummyConverter);
        assertEquals(ZoneOffset.ofHoursMinutes(18, 0), offset);
    }

    @Test
    public void testToZoneOffset_extremeNegativeOffset() {
        // Minimum negative offset supported by ZoneOffset is -18:00.
        ZoneId zoneId = ZoneId.of("-18:00");
        ZoneOffset offset = ZoneIdConversions.toZoneOffset(zoneId, dummyConverter);
        assertEquals(ZoneOffset.ofHoursMinutes(-18, 0), offset);
    }

    @Test
    public void testToZoneOffset_currentInstantConsistency() {
        // Verify that the offset returned corresponds to the current instant.
        ZoneId zoneId = ZoneId.systemDefault();
        ZoneOffset offsetNow = ZoneIdConversions.toZoneOffset(zoneId, dummyConverter);
        ZoneOffset expected = zoneId.getRules().getOffset(Instant.now());

        // Offsets may differ by a second if a DST transition occurs exactly at test time;
        // allow equality for typical cases.
        assertEquals("Offset should match the rules for the current instant", expected, offsetNow);
    }
}
