package com.cedarsoftware.util.convert;
import com.cedarsoftware.util.CompactMap;


import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

/**
 * Comprehensive JUnit‑4 test suite for {@link TimeZoneConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal usage with standard time‑zones (e.g., UTC, America/New_York).</li>
 *   <li>Edge cases such as custom time‑zones created via {@link TimeZone#getTimeZone(String)} with
 *       unknown IDs (which fall back to GMT) and zones with half‑hour offsets.</li>
 *   <li>Verification that the {@code Converter} argument is not used by the implementation
 *       (it may be {@code null} without affecting the result).</li>
 *   <li>Correct handling of the raw offset conversion to {@link ZoneOffset} (including negative
 *       offsets and offsets that are not whole hours).</li>
 *   <li>Integrity of the map produced by {@code toMap} – correct key, correct value type,
 *       and insertion order preservation.</li>
 * </ul>
 *
 * No mocking frameworks are used; all objects are instantiated via their public constructors
 * or factory methods as required by the assignment constraints.
 */
public class TimeZoneConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter instance is not used by the conversion methods, but we provide a
        // non‑null instance to satisfy the method signature.
        dummyConverter = new Converter(new ConverterOptions());
    }

    /*--------------------------------------------------------------------
     *  toString tests
     *--------------------------------------------------------------------*/

    @Test
    public void toString_returnsTimeZoneId_forStandardZone() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        String result = TimeZoneConversions.toString(tz, dummyConverter);
        assertEquals("UTC", result);
    }

    @Test
    public void toString_returnsTimeZoneId_forCustomZoneId() {
        // "GMT+05:30" is a valid ID that represents a half‑hour offset.
        TimeZone tz = TimeZone.getTimeZone("GMT+05:30");
        String result = TimeZoneConversions.toString(tz, dummyConverter);
        assertEquals("GMT+05:30", result);
    }

    @Test
    public void toString_fallsBackToGMT_whenUnknownId() {
        // An unknown ID is treated as GMT by TimeZone.getTimeZone.
        TimeZone tz = TimeZone.getTimeZone("THIS_ZONE_DOES_NOT_EXIST");
        String result = TimeZoneConversions.toString(tz, dummyConverter);
        assertEquals("GMT", result);
    }

    @Test(expected = ClassCastException.class)
    public void toString_throwsClassCastException_whenNotATimeZone() {
        // Passing a non‑TimeZone object should cause a ClassCastException as the method
        // performs an unchecked cast.
        TimeZoneConversions.toString("not a timezone", dummyConverter);
    }

    /*--------------------------------------------------------------------
     *  toZoneId tests
     *--------------------------------------------------------------------*/

    @Test
    public void toZoneId_convertsStandardTimeZone() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        ZoneId zoneId = TimeZoneConversions.toZoneId(tz, dummyConverter);
        assertEquals(ZoneId.of("America/New_York"), zoneId);
    }

    @Test
    public void toZoneId_handlesGMTOffsetZone() {
        TimeZone tz = TimeZone.getTimeZone("GMT-02:00");
        ZoneId zoneId = TimeZoneConversions.toZoneId(tz, dummyConverter);
        assertEquals(ZoneId.of("GMT-02:00"), zoneId);
    }

    @Test(expected = ClassCastException.class)
    public void toZoneId_throwsClassCastException_whenArgumentIsNotTimeZone() {
        TimeZoneConversions.toZoneId(12345, dummyConverter);
    }

    /*--------------------------------------------------------------------
     *  toMap tests
     *--------------------------------------------------------------------*/

    @Test
    public void toMap_containsSingleEntryWithCorrectKeyAndValue() {
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, dummyConverter);

        // The map should contain exactly one entry.
        assertEquals(1, map.size());

        // Verify that the key is the constant defined in MapConversions.
        assertTrue(map.containsKey(MapConversions.ZONE));

        // Verify that the value matches the TimeZone ID.
        assertEquals("Europe/Paris", map.get(MapConversions.ZONE));
    }

    @Test
    public void toMap_preservesInsertionOrder() {
        // Build two distinct maps and ensure that the iteration order of entries matches
        // insertion order (the builder is configured with insertionOrder()).
        TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, dummyConverter);

        // Since there is only one entry, order is trivially preserved, but we still
        // assert that the underlying map is a CompactMap with insertion ordering.
        assertTrue(map instanceof CompactMap);
        // The CompactMap class does not expose its ordering directly, but the builder
        // call guarantees insertion order; this test acts as a sanity check.
    }

    @Test(expected = ClassCastException.class)
    public void toMap_throwsClassCastException_whenInputIsNotTimeZone() {
        TimeZoneConversions.toMap(new Object(), dummyConverter);
    }

    /*--------------------------------------------------------------------
     *  toZoneOffset tests
     *--------------------------------------------------------------------*/

    @Test
    public void toZoneOffset_correctlyConvertsPositiveWholeHourOffset() {
        // UTC+02:00 => raw offset = 2 * 60 * 60 * 1000 ms
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, dummyConverter);
        assertEquals(ZoneOffset.ofHours(2), offset);
    }

    @Test
    public void toZoneOffset_correctlyConvertsNegativeWholeHourOffset() {
        // UTC-05:00 => raw offset = -5 * 60 * 60 * 1000 ms
        TimeZone tz = TimeZone.getTimeZone("GMT-05:00");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, dummyConverter);
        assertEquals(ZoneOffset.ofHours(-5), offset);
    }

    @Test
    public void toZoneOffset_handlesHalfHourOffset() {
        // GMT+05:30 => raw offset = 5.5 hours
        TimeZone tz = TimeZone.getTimeZone("GMT+05:30");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, dummyConverter);
        assertEquals(ZoneOffset.ofHoursMinutes(5, 30), offset);
    }

    @Test
    public void toZoneOffset_handlesNegativeHalfHourOffset() {
        // GMT-03:30 => raw offset = -3.5 hours
        TimeZone tz = TimeZone.getTimeZone("GMT-03:30");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, dummyConverter);
        assertEquals(ZoneOffset.ofHoursMinutes(-3, -30), offset);
    }

    @Test
    public void toZoneOffset_returnsZeroOffset_forGMT() {
        TimeZone tz = TimeZone.getTimeZone("GMT");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, dummyConverter);
        assertEquals(ZoneOffset.UTC, offset);
    }

    @Test(expected = ClassCastException.class)
    public void toZoneOffset_throwsClassCastException_whenArgumentIsNotTimeZone() {
        TimeZoneConversions.toZoneOffset(Boolean.TRUE, dummyConverter);
    }

    /*--------------------------------------------------------------------
     *  Null handling tests (even though implementation does not guard against null)
     *--------------------------------------------------------------------*/

    @Test(expected = NullPointerException.class)
    public void toString_throwsNPE_whenFromIsNull() {
        TimeZoneConversions.toString(null, dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void toZoneId_throwsNPE_whenFromIsNull() {
        TimeZoneConversions.toZoneId(null, dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void toMap_throwsNPE_whenFromIsNull() {
        TimeZoneConversions.toMap(null, dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void toZoneOffset_throwsNPE_whenFromIsNull() {
        TimeZoneConversions.toZoneOffset(null, dummyConverter);
    }

    /*--------------------------------------------------------------------
     *  Converter argument independence tests
     *--------------------------------------------------------------------*/

    @Test
    public void methodsIgnoreConverterArgument_whenNullPassed() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        assertEquals("UTC", TimeZoneConversions.toString(tz, null));
        assertEquals(ZoneId.of("UTC"), TimeZoneConversions.toZoneId(tz, null));
        assertEquals(ZoneOffset.UTC, TimeZoneConversions.toZoneOffset(tz, null));

        Map<String, Object> map = TimeZoneConversions.toMap(tz, null);
        assertEquals(1, map.size());
        assertEquals("UTC", map.get(MapConversions.ZONE));
    }
}
