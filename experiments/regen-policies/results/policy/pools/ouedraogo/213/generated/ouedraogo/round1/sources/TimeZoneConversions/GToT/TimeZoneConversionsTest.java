package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * JUnit‑4 test suite for {@link TimeZoneConversions}.
 *
 * The tests cover:
 *   • Normal behaviour for each conversion method.
 *   • Edge cases (null source, wrong type, extreme offsets, non‑hour offsets).
 *   • Verification that {@code toMap} returns a mutable, ordered map.
 *
 * The tests run in the same package as {@code TimeZoneConversions} so that the
 * package‑private static methods are directly accessible.
 */
public class TimeZoneConversionsTest {

    /* ---------------------------------------------------------------------- */
    /* Helper – a dummy Converter instance (the class under test never uses it). */
    /* ---------------------------------------------------------------------- */
    private static final Converter DUMMY_CONVERTER = null; // acceptable because not used

    /* ---------------------------------------------------------------------- */
    /* 1. toString() */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toString_returnsTimeZoneId() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        String result = TimeZoneConversions.toString(tz, DUMMY_CONVERTER);
        assertEquals("UTC", result);
    }

    @Test(expected = NullPointerException.class)
    public void toString_nullArgument_throwsNPE() {
        TimeZoneConversions.toString(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void toString_wrongType_throwsClassCastException() {
        TimeZoneConversions.toString("NotATimeZone", DUMMY_CONVERTER);
    }

    /* ---------------------------------------------------------------------- */
    /* 2. toZoneId() */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toZoneId_returnsEquivalentZoneId() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        ZoneId zid = TimeZoneConversions.toZoneId(tz, DUMMY_CONVERTER);
        assertEquals(ZoneId.of("America/New_York"), zid);
    }

    @Test
    public void toZoneId_handlesOffsetId() {
        TimeZone tz = TimeZone.getTimeZone("GMT+05:30");
        ZoneId zid = TimeZoneConversions.toZoneId(tz, DUMMY_CONVERTER);
        assertEquals(ZoneId.of("GMT+05:30"), zid);
    }

    @Test(expected = NullPointerException.class)
    public void toZoneId_nullArgument_throwsNPE() {
        TimeZoneConversions.toZoneId(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void toZoneId_wrongType_throwsClassCastException() {
        TimeZoneConversions.toZoneId(Integer.valueOf(42), DUMMY_CONVERTER);
    }

    /* ---------------------------------------------------------------------- */
    /* 3. toMap() */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toMap_containsSingleEntryWithZoneKey() {
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, DUMMY_CONVERTER);

        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.ZONE));
        assertEquals("Europe/Paris", map.get(MapConversions.ZONE));
    }

    @Test
    public void toMap_returnsMutableLinkedHashMap() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, DUMMY_CONVERTER);

        // Verify implementation class
        assertTrue(map instanceof LinkedHashMap);

        // Verify mutability
        map.put("extra", "value");
        assertEquals("value", map.get("extra"));
        assertEquals(2, map.size());

        // Verify order – original entry should still be first
        assertEquals("UTC", map.values().iterator().next());
    }

    @Test(expected = NullPointerException.class)
    public void toMap_nullArgument_throwsNPE() {
        TimeZoneConversions.toMap(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void toMap_wrongType_throwsClassCastException() {
        TimeZoneConversions.toMap(new Object(), DUMMY_CONVERTER);
    }

    /* ---------------------------------------------------------------------- */
    /* 4. toZoneOffset() */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toZoneOffset_utcIsZero() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, DUMMY_CONVERTER);
        assertEquals(ZoneOffset.UTC, offset);
    }

    @Test
    public void toZoneOffset_positiveWholeHour() {
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, DUMMY_CONVERTER);
        assertEquals(ZoneOffset.ofHours(2), offset);
    }

    @Test
    public void toZoneOffset_negativeWholeHour() {
        TimeZone tz = TimeZone.getTimeZone("GMT-07:00");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, DUMMY_CONVERTER);
        assertEquals(ZoneOffset.ofHours(-7), offset);
    }

    @Test
    public void toZoneOffset_positiveNonHour() {
        TimeZone tz = TimeZone.getTimeZone("GMT+05:30");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, DUMMY_CONVERTER);
        assertEquals(ZoneOffset.ofHoursMinutes(5, 30), offset);
    }

    @Test
    public void toZoneOffset_maxPositive() {
        TimeZone tz = TimeZone.getTimeZone("GMT+14:00");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, DUMMY_CONVERTER);
        assertEquals(ZoneOffset.ofHours(14), offset);
    }

    @Test
    public void toZoneOffset_maxNegative() {
        TimeZone tz = TimeZone.getTimeZone("GMT-12:00");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, DUMMY_CONVERTER);
        assertEquals(ZoneOffset.ofHours(-12), offset);
    }

    @Test(expected = NullPointerException.class)
    public void toZoneOffset_nullArgument_throwsNPE() {
        TimeZoneConversions.toZoneOffset(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void toZoneOffset_wrongType_throwsClassCastException() {
        TimeZoneConversions.toZoneOffset(Boolean.TRUE, DUMMY_CONVERTER);
    }
}
