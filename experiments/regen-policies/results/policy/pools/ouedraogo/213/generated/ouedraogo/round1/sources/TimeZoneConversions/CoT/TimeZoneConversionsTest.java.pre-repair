package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

/**
 * JUnit 4 test cases for {@link TimeZoneConversions}.
 *
 * The tests are placed in the same package as {@code TimeZoneConversions}
 * so that they can access the package‑private static methods.
 */
public class TimeZoneConversionsTest {

    /* ----------------------------------------------------------------------
     * Helper constants
     * ---------------------------------------------------------------------- */
    private static final Converter DUMMY_CONVERTER = null;   // not used by the conversion methods

    /* ----------------------------------------------------------------------
     * 1. toString(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void testToString_WithUtcTimeZone() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        String result = TimeZoneConversions.toString(tz, DUMMY_CONVERTER);
        Assert.assertEquals("UTC", result);
    }

    @Test
    public void testToString_WithNewYorkTimeZone() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        String result = TimeZoneConversions.toString(tz, DUMMY_CONVERTER);
        Assert.assertEquals("America/New_York", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullFrom() {
        TimeZoneConversions.toString(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_InvalidFromType() {
        Object notATimeZone = new Object();
        TimeZoneConversions.toString(notATimeZone, DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * 2. toZoneId(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void testToZoneId_Utc() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        ZoneId zoneId = TimeZoneConversions.toZoneId(tz, DUMMY_CONVERTER);
        Assert.assertEquals(ZoneId.of("UTC"), zoneId);
    }

    @Test
    public void testToZoneId_EuropeParis() {
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        ZoneId zoneId = TimeZoneConversions.toZoneId(tz, DUMMY_CONVERTER);
        Assert.assertEquals(ZoneId.of("Europe/Paris"), zoneId);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneId_NullFrom() {
        TimeZoneConversions.toZoneId(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneId_InvalidFromType() {
        TimeZoneConversions.toZoneId("not a timezone", DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * 3. toMap(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void testToMap_Utc() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, DUMMY_CONVERTER);
        Assert.assertTrue(map.containsKey(ZONE));
        Assert.assertEquals("UTC", map.get(ZONE));
    }

    @Test
    public void testToMap_CustomZone() {
        TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");
        Map<String, Object> map = TimeZoneConversions.toMap(tz, DUMMY_CONVERTER);
        Assert.assertEquals("Asia/Tokyo", map.get(ZONE));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullFrom() {
        TimeZoneConversions.toMap(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_InvalidFromType() {
        TimeZoneConversions.toMap(12345, DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * 4. toZoneOffset(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void testToZoneOffset_Utc() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, DUMMY_CONVERTER);
        Assert.assertEquals(ZoneOffset.UTC, offset);
    }

    @Test
    public void testToZoneOffset_PlusTwoHours() {
        // GMT+02:00 is represented as "GMT+02:00"
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, DUMMY_CONVERTER);
        Assert.assertEquals(ZoneOffset.ofHours(2), offset);
    }

    @Test
    public void testToZoneOffset_MinusFiveHoursThirtyMinutes() {
        // GMT-05:30 offset (e.g., "America/Caracas" historically)
        TimeZone tz = TimeZone.getTimeZone("GMT-05:30");
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, DUMMY_CONVERTER);
        Assert.assertEquals(ZoneOffset.ofHoursMinutes(-5, -30), offset);
    }

    @Test(expected = NullPointerException.class)
    public void testToZoneOffset_NullFrom() {
        TimeZoneConversions.toZoneOffset(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToZoneOffset_InvalidFromType() {
        TimeZoneConversions.toZoneOffset(Boolean.TRUE, DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * Additional edge‑case checks for raw‑offset conversion precision
     * ---------------------------------------------------------------------- */

    @Test
    public void testToZoneOffset_RawOffsetRounding() {
        // Create a TimeZone with a raw offset that is not a whole second
        // (this is artificial – real TimeZone offsets are multiples of 15 minutes,
        // but we can use a subclass to set the raw offset directly.)
        TimeZone tz = new TimeZone() {
            @Override
            public String getID() { return "Custom/Offset"; }
            @Override
            public int getRawOffset() { return 1234; }               // 1.234 seconds
            @Override
            public void setRawOffset(int offsetMillis) {}
            @Override
            public boolean useDaylightTime() { return false; }
            @Override
            public boolean inDaylightTime(java.util.Date date) { return false; }
            @Override
            public int getDSTSavings() { return 0; }
        };
        // The implementation truncates toward zero when dividing by 1000
        ZoneOffset offset = TimeZoneConversions.toZoneOffset(tz, DUMMY_CONVERTER);
        Assert.assertEquals(ZoneOffset.ofTotalSeconds(1), offset); // 1234 / 1000 = 1 (int division)
    }
}
