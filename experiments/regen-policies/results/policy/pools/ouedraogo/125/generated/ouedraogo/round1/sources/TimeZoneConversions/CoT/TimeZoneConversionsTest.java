package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;
import java.util.SimpleTimeZone;

/**
 * JUnit‑4 test suite for {@link TimeZoneConversions}.
 *
 * The class under test is package‑private, therefore this test class lives in the same
 * package (com.cedarsoftware.util.convert) so that it can access the static methods.
 *
 * The tests are divided into two groups:
 *   1. “Basic” tests that verify the normal, expected behaviour.
 *   2. “Edge / Exception” tests that exercise null values, wrong types and
 *      unusual raw‑offset values.
 *
 * The {@code Converter} argument is not used by any of the methods, so we pass {@code null}
 * for simplicity.
 */
public class TimeZoneConversionsTest {

    /* --------------------------------------------------------------------- *
     *  Helper utilities
     * --------------------------------------------------------------------- */

    /** A dummy {@code Converter} instance – not used by the conversion methods. */
    private static final Converter DUMMY_CONVERTER = null;

    /** Constant key used by {@code toMap}. */
    private static final String ZONE_KEY = ZONE;   // imported from MapConversions

    /* --------------------------------------------------------------------- *
     *  1.  toString(Object, Converter)
     * --------------------------------------------------------------------- */

    @Test
    public void testToString_basic() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        String result = TimeZoneConversions.toString(tz, DUMMY_CONVERTER);
        assertEquals("America/New_York", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_nullTimeZone() {
        // The cast succeeds (null -> null) but the call to getID() throws NPE.
        TimeZoneConversions.toString(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_wrongObjectType() {
        // Passing a plain String forces the cast to TimeZone to fail.
        TimeZoneConversions.toString("not a timezone", DUMMY_CONVERTER);
    }

    /* --------------------------------------------------------------------- *
     *  2.  toZoneId(Object, Converter)
     * --------------------------------------------------------------------- */

    @Test
    public void testToZoneId_basic() {
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        ZoneId zoneId = TimeZoneConversions.toZoneId(tz, DUMMY_CONVERTER);
        assertEquals(ZoneId.of("Europe/Paris"), zoneId);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneId_nullTimeZone() {
        TimeZoneConversions.toZoneId(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneId_wrongObjectType() {
        TimeZoneConversions.toZoneId(12345, DUMMY_CONVERTER);
    }

    /* --------------------------------------------------------------------- *
     *  3.  toMap(Object, Converter)
     * --------------------------------------------------------------------- */

    @Test
    public void testToMap_basic() {
        TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, DUMMY_CONVERTER);

        // The map should contain exactly one entry: ZONE -> tz.getID()
        assertEquals(1, map.size());
        assertTrue(map.containsKey(ZONE_KEY));
        assertEquals("Asia/Tokyo", map.get(ZONE_KEY));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_nullTimeZone() {
        TimeZoneConversions.toMap(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_wrongObjectType() {
        TimeZoneConversions.toMap(new Object(), DUMMY_CONVERTER);
    }

    /* --------------------------------------------------------------------- *
     *  4.  toZoneOffset(Object, Converter)
     * --------------------------------------------------------------------- */

    @Test
    public void testToZoneOffset_basic_GMT() {
        TimeZone tz = TimeZone.getTimeZone("GMT");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, DUMMY_CONVERTER);
        assertEquals(ZoneOffset.UTC, offset);
    }

    @Test
    public void testToZoneOffset_basic_NegativeOffset() {
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles"); // typically -08:00
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, DUMMY_CONVERTER);
        // raw offset is expressed in milliseconds without DST
        int expectedSeconds = tz.getRawOffset() / 1000;
        assertEquals(ZoneOffset.ofTotalSeconds(expectedSeconds), offset);
    }

    @Test
    public void testToZoneOffset_nonStandardMillisecondOffset() {
        // SimpleTimeZone allows any raw offset, even ones not divisible by 1000.
        // Use 1234 ms → 1 second after integer division.
        SimpleTimeZone custom = new SimpleTimeZone(1234, "Custom/OneSecond");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(custom, DUMMY_CONVERTER);
        assertEquals(ZoneOffset.ofTotalSeconds(1), offset);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneOffset_nullTimeZone() {
        TimeZoneConversions.toZoneOffset(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneOffset_wrongObjectType() {
        TimeZoneConversions.toZoneOffset("not a timezone", DUMMY_CONVERTER);
    }

    /* --------------------------------------------------------------------- *
     *  5.  Method signature list (documented for completeness)
     * --------------------------------------------------------------------- */

    /**
     * Public (package‑private) methods of {@link TimeZoneConversions} that are exercised:
     *
     * <pre>
     * static String          toString(Object from, Converter converter)
     * static ZoneId          toZoneId(Object from, Converter converter)
     * static Map<String,Object> toMap(Object from, Converter converter)
     * static ZoneOffset      toZoneOffset(Object from, Converter converter)
     * </pre>
     *
     * The signatures are reproduced here to satisfy the first bullet point of the
     * assignment.
     */
}
