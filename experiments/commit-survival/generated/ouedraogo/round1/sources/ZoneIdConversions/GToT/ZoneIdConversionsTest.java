package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;

/**
 * JUnit‑4 test suite for {@code ZoneIdConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal (happy‑path) usage of each conversion method.</li>
 *   <li>Edge‑case handling such as {@code null} arguments.</li>
 *   <li>Incorrect source types that trigger {@code ClassCastException}.</li>
 *   <li>Verification that the supplied {@code Converter} argument is ignored.</li>
 * </ul>
 *
 * The test class lives in the same package as {@code ZoneIdConversions}
 * so it can invoke the package‑private static methods directly.
 */
public class ZoneIdConversionsTest {

    /* ---------------------------------------------------------------------- */
    /*  toMap() – basic functionality                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMapBasic() {
        ZoneId zone = ZoneId.of("America/New_York");
        // the conversion method does not use the Converter argument
        Map<String, Object> result = ZoneIdConversions.toMap(zone, null);

        assertNotNull("Result map must not be null", result);
        assertEquals("Map must contain exactly one entry", 1, result.size());
        assertTrue("Map must contain key \"zone\"", result.containsKey("zone"));
        assertEquals("Value must be the string form of the ZoneId",
                zone.toString(), result.get("zone"));
    }

    @Test
    public void testToMapWithOffsetZone() {
        ZoneId zone = ZoneId.of("+02:00"); // a fixed‑offset zone
        Map<String, Object> result = ZoneIdConversions.toMap(zone, null);
        assertEquals("+02:00", result.get("zone"));
    }

    /* ---------------------------------------------------------------------- */
    /*  toMap() – edge cases & exception handling                             */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToMapNullFrom() {
        // Casting null to ZoneId yields null; the subsequent call to toString()
        // throws NullPointerException.
        ZoneIdConversions.toMap(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapInvalidSourceType() {
        // Pass an object that is not a ZoneId – the explicit cast will fail.
        ZoneIdConversions.toMap("not a ZoneId", null);
    }

    @Test
    public void testToMapIgnoresConverterArgument() {
        ZoneId zone = ZoneId.of("UTC");
        Converter dummy = new Converter(null); // Converter is not used internally
        Map<String, Object> result = ZoneIdConversions.toMap(zone, dummy);
        assertEquals("UTC", result.get("zone"));
    }

    /* ---------------------------------------------------------------------- */
    /*  toTimeZone() – basic functionality                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToTimeZoneBasic() {
        ZoneId zone = ZoneId.of("Europe/Paris");
        TimeZone tz = ZoneIdConversions.toTimeZone(zone, null);
        assertNotNull("TimeZone must not be null", tz);
        assertEquals("TimeZone ID must match ZoneId ID", zone.getId(), tz.getID());
    }

    @Test
    public void testToTimeZoneWithUtc() {
        ZoneId zone = ZoneId.of("UTC");
        TimeZone tz = ZoneIdConversions.toTimeZone(zone, null);
        assertEquals("UTC", tz.getID());
    }

    @Test
    public void testToTimeZoneWithFixedOffset() {
        ZoneId zone = ZoneId.of("+05:30");
        TimeZone tz = ZoneIdConversions.toTimeZone(zone, null);
        // TimeZone represents fixed offsets as "GMT+05:30"
        assertEquals("GMT+05:30", tz.getID());
    }

    /* ---------------------------------------------------------------------- */
    /*  toTimeZone() – edge cases & exception handling                        */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToTimeZoneNullFrom() {
        // TimeZone.getTimeZone(null) throws NPE
        ZoneIdConversions.toTimeZone(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToTimeZoneInvalidSourceType() {
        // The explicit cast to ZoneId will fail.
        ZoneIdConversions.toTimeZone(12345, null);
    }

    @Test
    public void testToTimeZoneIgnoresConverterArgument() {
        ZoneId zone = ZoneId.of("Asia/Tokyo");
        Converter dummy = new Converter(null);
        TimeZone tz = ZoneIdConversions.toTimeZone(zone, dummy);
        assertEquals("Asia/Tokyo", tz.getID());
    }
}
