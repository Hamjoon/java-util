package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;
import static com.cedarsoftware.util.convert.MapConversions.ZONE;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Test;

/**
 * Unit tests for {@link TimeZoneConversions}.
 *
 * The tests exercise every public static method in the class using
 * real {@link TimeZone} instances and a minimal {@link Converter}
 * (constructed with a {@code null} options object – the implementation
 * under test does not use the converter argument at all).
 *
 * The test suite covers:
 * <ul>
 *   <li>Correct string representation of the zone ID.</li>
 *   <li>Accurate conversion to {@link ZoneId}.</li>
 *   <li>Proper map creation with the {@code ZONE} key.</li>
 *   <li>Correct calculation of {@link ZoneOffset} from the raw offset.</li>
 *   <li>Positive, zero and negative offsets.</li>
 * </ul>
 */
public class TimeZoneConversionsTest {

    /** Helper that creates a {@link Converter} instance – the conversion logic does not depend on the options. */
    private static Converter converter() {
        // The Converter constructor requires a ConverterOptions instance; passing {@code null}
        // is acceptable for the purposes of these tests because the methods under test never use it.
        return new Converter(null);
    }

    @Test
    public void toString_returnsExactTimeZoneId() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        assertEquals("UTC", TimeZoneConversions.toString(utc, converter()));

        TimeZone newYork = TimeZone.getTimeZone("America/New_York");
        assertEquals("America/New_York", TimeZoneConversions.toString(newYork, converter()));

        TimeZone gmtPlus2 = TimeZone.getTimeZone("GMT+02:00");
        assertEquals("GMT+02:00", TimeZoneConversions.toString(gmtPlus2, converter()));
    }

    @Test
    public void toZoneId_returnsMatchingZoneId() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        ZoneId utcId = TimeZoneConversions.toZoneId(utc, converter());
        assertEquals(utc.toZoneId(), utcId);

        TimeZone paris = TimeZone.getTimeZone("Europe/Paris");
        ZoneId parisId = TimeZoneConversions.toZoneId(paris, converter());
        assertEquals(paris.toZoneId(), parisId);
    }

    @Test
    public void toMap_containsSingleEntryWithZoneKey() {
        TimeZone tz = TimeZone.getTimeZone("GMT-05:00");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, converter());

        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(ZONE));
        assertEquals(tz.getID(), map.get(ZONE));
    }

    @Test
    public void toZoneOffset_correctlyCalculatesPositiveOffset() {
        // GMT+02:00 → 2 hours × 3600 seconds = 7200 seconds
        TimeZone plusTwo = TimeZone.getTimeZone("GMT+02:00");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(plusTwo, converter());

        assertEquals(ZoneOffset.ofHours(2), offset);
    }

    @Test
    public void toZoneOffset_correctlyCalculatesZeroOffset() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(utc, converter());

        assertEquals(ZoneOffset.UTC, offset);
    }

    @Test
    public void toZoneOffset_correctlyCalculatesNegativeOffset() {
        // GMT-05:00 → -5 hours × 3600 seconds = -18000 seconds
        TimeZone minusFive = TimeZone.getTimeZone("GMT-05:00");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(minusFive, converter());

        assertEquals(ZoneOffset.ofHours(-5), offset);
    }

    @Test(expected = NullPointerException.class)
    public void toString_nullTimeZone_throwsNPE() {
        TimeZoneConversions.toString(null, converter());
    }

    @Test(expected = NullPointerException.class)
    public void toZoneId_nullTimeZone_throwsNPE() {
        TimeZoneConversions.toZoneId(null, converter());
    }

    @Test(expected = NullPointerException.class)
    public void toMap_nullTimeZone_throwsNPE() {
        TimeZoneConversions.toMap(null, converter());
    }

    @Test(expected = NullPointerException.class)
    public void toZoneOffset_nullTimeZone_throwsNPE() {
        TimeZoneConversions.toZoneOffset(null, converter());
    }
}
