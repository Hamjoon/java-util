/***************************************************************************************************
 * JUnit‑4 test suite for {@code CalendarConversions}
 *
 * The test class lives in the same package as {@code CalendarConversions} so that it can access
 * the package‑private static methods.  All tests use real objects – no mocking – and cover:
 *
 *   • Typical values (a known UTC date‑time)
 *   • Edge cases (null arguments, time‑zone offsets)
 *   • Behaviour of each conversion method
 *
 * The library classes used are limited to those that appear in the source file or its public
 * dependencies (Converter, ConverterOptions, CompactMap, etc.).
 **************************************************************************************************/
package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test suite for {@link CalendarConversions}.  All tests are static – the class under test only
 * contains static conversion helpers.
 */
public class CalendarConversionsTest
{
    /* ----------------------------------------------------------------------
     * Helper – a deterministic Calendar instance (UTC) used by most tests.
     * ---------------------------------------------------------------------- */
    private static Calendar utcCalendar()
    {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        // 2022‑03‑14T15:09:26.123Z
        cal.set(2022, Calendar.MARCH, 14, 15, 9, 26);
        cal.set(Calendar.MILLISECOND, 123);
        return cal;
    }

    /* ----------------------------------------------------------------------
     * Helper – a Converter with default options (default time‑zone = system default).
     * The concrete type of ConverterOptions is not important for the tests that
     * do not depend on its configuration.
     * ---------------------------------------------------------------------- */
    private static Converter defaultConverter()
    {
        return new Converter(new ConverterOptions());
    }

    /* ----------------------------------------------------------------------
     * 1. toZonedDateTime
     * ---------------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime_Typical()
    {
        Calendar cal = utcCalendar();
        ZonedDateTime expected = ZonedDateTime.ofInstant(cal.toInstant(),
                                                        cal.getTimeZone().toZoneId());

        ZonedDateTime actual = CalendarConversions.toZonedDateTime(cal, defaultConverter());

        Assert.assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_Null()
    {
        CalendarConversions.toZonedDateTime(null, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * 2. toLong
     * ---------------------------------------------------------------------- */
    @Test
    public void testToLong_Typical()
    {
        Calendar cal = utcCalendar();
        Long expected = cal.getTimeInMillis();
        Assert.assertEquals(expected, CalendarConversions.toLong(cal, defaultConverter()));
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_Null()
    {
        CalendarConversions.toLong(null, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * 3. toDouble (epoch seconds as double)
     * ---------------------------------------------------------------------- */
    @Test
    public void testToDouble_Typical()
    {
        Calendar cal = utcCalendar();
        double expected = cal.getTimeInMillis() / 1000.0;
        Assert.assertEquals(expected, CalendarConversions.toDouble(cal, defaultConverter()), 0.0);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_Null()
    {
        CalendarConversions.toDouble(null, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * 4. toDate
     * ---------------------------------------------------------------------- */
    @Test
    public void testToDate_Typical()
    {
        Calendar cal = utcCalendar();
        Assert.assertEquals(cal.getTime(), CalendarConversions.toDate(cal, defaultConverter()));
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_Null()
    {
        CalendarConversions.toDate(null, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * 5. toSqlDate
     * ---------------------------------------------------------------------- */
    @Test
    public void testToSqlDate_Typical()
    {
        Calendar cal = utcCalendar();
        java.sql.Date expected = new java.sql.Date(cal.getTimeInMillis());
        Assert.assertEquals(expected, CalendarConversions.toSqlDate(cal, defaultConverter()));
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_Null()
    {
        CalendarConversions.toSqlDate(null, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * 6. toTimestamp
     * ---------------------------------------------------------------------- */
    @Test
    public void testToTimestamp_Typical()
    {
        Calendar cal = utcCalendar();
        Timestamp expected = new Timestamp(cal.getTimeInMillis());
        Assert.assertEquals(expected, CalendarConversions.toTimestamp(cal, defaultConverter()));
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_Null()
    {
        CalendarConversions.toTimestamp(null, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * 7. toAtomicLong
     * ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicLong_Typical()
    {
        Calendar cal = utcCalendar();
        AtomicLong expected = new AtomicLong(cal.getTimeInMillis());
        Assert.assertEquals(expected, CalendarConversions.toAtomicLong(cal, defaultConverter()));
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_Null()
    {
        CalendarConversions.toAtomicLong(null, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * 8. toInstant
     * ---------------------------------------------------------------------- */
    @Test
    public void testToInstant_Typical()
    {
        Calendar cal = utcCalendar();
        Assert.assertEquals(cal.toInstant(), CalendarConversions.toInstant(cal, defaultConverter()));
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_Null()
    {
        CalendarConversions.toInstant(null, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * 9. toLocalDateTime
     * ---------------------------------------------------------------------- */
    @Test
    public void testToLocalDateTime_Typical()
    {
        Calendar cal = utcCalendar();
        LocalDateTime expected = CalendarConversions.toZonedDateTime(cal, defaultConverter())
                                                    .toLocalDateTime();
        Assert.assertEquals(expected, CalendarConversions.toLocalDateTime(cal, defaultConverter()));
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_Null()
    {
        CalendarConversions.toLocalDateTime(null, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * 10. toLocalDate
     * ---------------------------------------------------------------------- */
    @Test
    public void testToLocalDate_Typical()
    {
        Calendar cal = utcCalendar();
        LocalDate expected = CalendarConversions.toZonedDateTime(cal, defaultConverter())
                                                .toLocalDate();
        Assert.assertEquals(expected, CalendarConversions.toLocalDate(cal, defaultConverter()));
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_Null()
    {
        CalendarConversions.toLocalDate(null, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * 11. toLocalTime
     * ---------------------------------------------------------------------- */
    @Test
    public void testToLocalTime_Typical()
    {
        Calendar cal = utcCalendar();
        LocalTime expected = CalendarConversions.toZonedDateTime(cal, defaultConverter())
                                                .toLocalTime();
        Assert.assertEquals(expected, CalendarConversions.toLocalTime(cal, defaultConverter()));
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_Null()
    {
        CalendarConversions.toLocalTime(null, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * 12. toBigDecimal
     *    (epoch milliseconds divided by BigDecimalConversions.GRAND)
     * ---------------------------------------------------------------------- */
    @Test
    public void testToBigDecimal_Typical()
    {
        Calendar cal = utcCalendar();
        long epochMillis = cal.getTimeInMillis();

        // The library defines BIG_DECIMAL_GRAND = new BigDecimal(1000)
        // (the constant lives in BigDecimalConversions).  We reference it directly.
        BigDecimal expected = new BigDecimal(epochMillis).divide(BigDecimalConversions.GRAND);
        Assert.assertEquals(expected, CalendarConversions.toBigDecimal(cal, defaultConverter()));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_Null()
    {
        CalendarConversions.toBigDecimal(null, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * 13. toBigInteger
     * ---------------------------------------------------------------------- */
    @Test
    public void testToBigInteger_Typical()
    {
        Calendar cal = utcCalendar();
        BigInteger expected = BigInteger.valueOf(cal.getTimeInMillis() * 1_000_000L);
        Assert.assertEquals(expected, CalendarConversions.toBigInteger(cal, defaultConverter()));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_Null()
    {
        CalendarConversions.toBigInteger(null, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * 14. clone
     * ---------------------------------------------------------------------- */
    @Test
    public void testClone_Typical()
    {
        Calendar original = utcCalendar();
        Calendar copy = CalendarConversions.clone(original, defaultConverter());

        // Calendars should be equal but not the same instance.
        Assert.assertNotSame(original, copy);
        Assert.assertEquals(original.getTimeInMillis(), copy.getTimeInMillis());
        Assert.assertEquals(original.getTimeZone(), copy.getTimeZone());

        // Mutating the original must not affect the clone.
        original.add(Calendar.DAY_OF_MONTH, 1);
        Assert.assertNotEquals(original.getTimeInMillis(), copy.getTimeInMillis());
    }

    @Test(expected = NullPointerException.class)
    public void testClone_Null()
    {
        CalendarConversions.clone(null, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * 15. create
     * ---------------------------------------------------------------------- */
    @Test
    public void testCreate_Typical()
    {
        long epoch = 1_640_000_000_123L; // arbitrary epoch millis
        Converter converter = defaultConverter();

        Calendar created = CalendarConversions.create(epoch, converter);
        Assert.assertEquals(epoch, created.getTimeInMillis());

        // The time‑zone of the created calendar must be the one supplied by the ConverterOptions.
        TimeZone expectedTZ = converter.getOptions().getTimeZone();
        Assert.assertEquals(expectedTZ, created.getTimeZone());
    }

    @Test(expected = NullPointerException.class)
    public void testCreate_NullConverter()
    {
        CalendarConversions.create(0L, null);
    }

    /* ----------------------------------------------------------------------
     * 16. toString
     * ---------------------------------------------------------------------- */
    @Test
    public void testToString_Typical()
    {
        Calendar cal = utcCalendar();
        String result = CalendarConversions.toString(cal, defaultConverter());

        // The exact format is defined by DateConversions – we only assert that the
        // result contains the expected ISO‑8601 date‑time components.
        Assert.assertTrue(result.contains("2022-03-14"));
        Assert.assertTrue(result.contains("15:09:26"));
        // milliseconds may be present depending on DateConversions implementation.
    }

    @Test(expected = NullPointerException.class)
    public void testToString_Null()
    {
        CalendarConversions.toString(null, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * 17. toOffsetDateTime
     * ---------------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime_Typical()
    {
        // Use a non‑UTC zone to verify the offset handling.
        TimeZone tz = TimeZone.getTimeZone("America/New_York"); // UTC‑5 (or ‑4 with DST)
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.set(2022, Calendar.JANUARY, 10, 12, 30, 0);
        cal.set(Calendar.MILLISECOND, 0);

        OffsetDateTime expected = cal.toInstant()
                                    .atOffset(ZoneOffset.ofTotalSeconds(tz.getOffset(cal.getTimeInMillis()) / 1000));

        OffsetDateTime actual = CalendarConversions.toOffsetDateTime(cal, defaultConverter());

        Assert.assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_Null()
    {
        CalendarConversions.toOffsetDateTime(null, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * 18. toMap
     * ---------------------------------------------------------------------- */
    @Test
    public void testToMap_Typical()
    {
        Calendar cal = utcCalendar();
        Map<String, Object> map = CalendarConversions.toMap(cal, defaultConverter());

        // Verify required keys exist.
        Assert.assertTrue(map.containsKey(MapConversions.DATE));
        Assert.assertTrue(map.containsKey(MapConversions.TIME));
        Assert.assertTrue(map.containsKey(MapConversions.ZONE));
        Assert.assertTrue(map.containsKey(MapConversions.EPOCH_MILLIS));

        // Verify values are correctly formatted.
        String expectedDate = LocalDate.of(2022, 3, 14).toString();
        String expectedTime = LocalTime.of(15, 9, 26, 123_000_000).toString();
        String expectedZone = cal.getTimeZone().toZoneId().toString();
        Long expectedEpoch = cal.getTimeInMillis();

        Assert.assertEquals(expectedDate, map.get(MapConversions.DATE));
        Assert.assertEquals(expectedTime, map.get(MapConversions.TIME));
        Assert.assertEquals(expectedZone, map.get(MapConversions.ZONE));
        Assert.assertEquals(expectedEpoch, map.get(MapConversions.EPOCH_MILLIS));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_Null()
    {
        CalendarConversions.toMap(null, defaultConverter());
    }
}
