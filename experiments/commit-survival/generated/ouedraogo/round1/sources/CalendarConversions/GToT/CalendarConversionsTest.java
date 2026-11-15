package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link CalendarConversions}.
 *
 * The tests are written in the same package as the class under test so that the
 * package‑private static methods are directly accessible.
 *
 * The suite covers:
 * <ul>
 *   <li>basic “happy‑path” verification for each conversion method</li>
 *   <li>edge‑case scenarios such as null input, extreme epoch values and
 *       unusual time‑zones</li>
 *   <li>exception handling – passing an object that is not a {@link Calendar}
 *       must result in a {@link ClassCastException}</li>
 * </ul>
 *
 * The {@link Converter} instance required by the API is created with a default
 * {@link ConverterOptions} object – the conversion methods themselves do not
 * depend on any behaviour of the {@code Converter} other than the presence of
 * the object.
 */
public class CalendarConversionsTest
{
    private Converter converter;          // a real Converter – its behaviour is not used
    private ConverterOptions options;    // default options (time‑zone can be overridden)

    @Before
    public void setUp()
    {
        options = new ConverterOptions();               // uses the library’s default ctor
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------------- *
     *  Helper utilities
     * --------------------------------------------------------------------- */

    /** Creates a {@link Calendar} set to the supplied epoch millisecond in the given time‑zone. */
    private static Calendar calendarAt(long epochMilli, TimeZone tz)
    {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    /** Returns a {@link Calendar} representing the given {@link LocalDateTime} in the supplied zone. */
    private static Calendar calendarOf(LocalDateTime ldt, ZoneId zone)
    {
        ZonedDateTime zdt = ldt.atZone(zone);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zone));
        cal.clear();
        cal.setTimeInMillis(zdt.toInstant().toEpochMilli());
        return cal;
    }

    /* --------------------------------------------------------------------- *
     *  1. toZonedDateTime
     * --------------------------------------------------------------------- */

    @Test
    public void testToZonedDateTime_Basic()
    {
        Calendar cal = calendarAt(1_600_000_000_000L, TimeZone.getTimeZone("UTC"));
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(Instant.ofEpochMilli(1_600_000_000_000L), zdt.toInstant());
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
    }

    @Test
    public void testToZonedDateTime_WithNonUtcZone()
    {
        TimeZone tz = TimeZone.getTimeZone("America/New_York"); // UTC‑5 (or ‑4 with DST)
        Calendar cal = calendarAt(1_600_000_000_000L, tz);
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(cal, converter);
        assertEquals(Instant.ofEpochMilli(1_600_000_000_000L), zdt.toInstant());
        assertEquals(tz.toZoneId(), zdt.getZone());
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_NonCalendar()
    {
        CalendarConversions.toZonedDateTime("not a calendar", converter);
    }

    /* --------------------------------------------------------------------- *
     *  2. toLong
     * --------------------------------------------------------------------- */

    @Test
    public void testToLong_Basic()
    {
        long epoch = 1_234_567_890L;
        Calendar cal = calendarAt(epoch, TimeZone.getTimeZone("UTC"));
        Long result = CalendarConversions.toLong(cal, converter);
        assertEquals(Long.valueOf(epoch), result);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_NonCalendar()
    {
        CalendarConversions.toLong(42, converter);
    }

    /* --------------------------------------------------------------------- *
     *  3. toDouble
     * --------------------------------------------------------------------- */

    @Test
    public void testToDouble_Basic()
    {
        long epoch = 1_500_000_000L;                 // milliseconds
        Calendar cal = calendarAt(epoch, TimeZone.getTimeZone("UTC"));
        double expected = epoch / 1000.0;
        assertEquals(expected, CalendarConversions.toDouble(cal, converter), 0.000001);
    }

    @Test
    public void testToDouble_NegativeEpoch()
    {
        long epoch = -1_000L; // before 1970‑01‑01
        Calendar cal = calendarAt(epoch, TimeZone.getTimeZone("UTC"));
        double expected = epoch / 1000.0;
        assertEquals(expected, CalendarConversions.toDouble(cal, converter), 0.0);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_NonCalendar()
    {
        CalendarConversions.toDouble(new Object(), converter);
    }

    /* --------------------------------------------------------------------- *
     *  4. toDate
     * --------------------------------------------------------------------- */

    @Test
    public void testToDate_Basic()
    {
        long epoch = 2_000_000_000L;
        Calendar cal = calendarAt(epoch, TimeZone.getTimeZone("UTC"));
        Date date = CalendarConversions.toDate(cal, converter);
        assertEquals(epoch, date.getTime());
    }

    @Test(expected = ClassCastException.class)
    public void testToDate_NonCalendar()
    {
        CalendarConversions.toDate(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  5. toSqlDate
     * --------------------------------------------------------------------- */

    @Test
    public void testToSqlDate_Basic()
    {
        long epoch = 3_000_000_000L;
        Calendar cal = calendarAt(epoch, TimeZone.getTimeZone("UTC"));
        java.sql.Date sqlDate = CalendarConversions.toSqlDate(cal, converter);
        assertEquals(epoch, sqlDate.getTime());
    }

    @Test(expected = ClassCastException.class)
    public void testToSqlDate_NonCalendar()
    {
        CalendarConversions.toSqlDate("oops", converter);
    }

    /* --------------------------------------------------------------------- *
     *  6. toTimestamp
     * --------------------------------------------------------------------- */

    @Test
    public void testToTimestamp_Basic()
    {
        long epoch = 4_000_000_000L;
        Calendar cal = calendarAt(epoch, TimeZone.getTimeZone("UTC"));
        Timestamp ts = CalendarConversions.toTimestamp(cal, converter);
        assertEquals(epoch, ts.getTime());
    }

    @Test(expected = ClassCastException.class)
    public void testToTimestamp_NonCalendar()
    {
        CalendarConversions.toTimestamp(new Object(), converter);
    }

    /* --------------------------------------------------------------------- *
     *  7. toAtomicLong
     * --------------------------------------------------------------------- */

    @Test
    public void testToAtomicLong_Basic()
    {
        long epoch = 5_000_000_000L;
        Calendar cal = calendarAt(epoch, TimeZone.getTimeZone("UTC"));
        AtomicLong al = CalendarConversions.toAtomicLong(cal, converter);
        assertEquals(epoch, al.get());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLong_NonCalendar()
    {
        CalendarConversions.toAtomicLong(Boolean.TRUE, converter);
    }

    /* --------------------------------------------------------------------- *
     *  8. toInstant
     * --------------------------------------------------------------------- */

    @Test
    public void testToInstant_Basic()
    {
        long epoch = 6_000_000_000L;
        Calendar cal = calendarAt(epoch, TimeZone.getTimeZone("UTC"));
        Instant instant = CalendarConversions.toInstant(cal, converter);
        assertEquals(Instant.ofEpochMilli(epoch), instant);
    }

    @Test(expected = ClassCastException.class)
    public void testToInstant_NonCalendar()
    {
        CalendarConversions.toInstant(new Object(), converter);
    }

    /* --------------------------------------------------------------------- *
     *  9. toLocalDateTime
     * --------------------------------------------------------------------- */

    @Test
    public void testToLocalDateTime_Basic()
    {
        LocalDateTime ldt = LocalDateTime.of(2020, 5, 15, 12, 34, 56, 123_000_000);
        Calendar cal = calendarOf(ldt, ZoneId.of("UTC"));
        LocalDateTime result = CalendarConversions.toLocalDateTime(cal, converter);
        assertEquals(ldt, result);
    }

    @Test
    public void testToLocalDateTime_WithOffsetZone()
    {
        ZoneId zone = ZoneId.of("Asia/Tokyo"); // UTC+9
        LocalDateTime ldt = LocalDateTime.of(2021, 12, 31, 23, 59, 59, 999_000_000);
        Calendar cal = calendarOf(ldt, zone);
        LocalDateTime result = CalendarConversions.toLocalDateTime(cal, converter);
        assertEquals(ldt, result);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalDateTime_NonCalendar()
    {
        CalendarConversions.toLocalDateTime(null, converter);
    }

    /* --------------------------------------------------------------------- *
     * 10. toLocalDate
     * --------------------------------------------------------------------- */

    @Test
    public void testToLocalDate_Basic()
    {
        LocalDate ld = LocalDate.of(1999, 12, 31);
        Calendar cal = calendarOf(ld.atStartOfDay(), ZoneId.of("UTC"));
        LocalDate result = CalendarConversions.toLocalDate(cal, converter);
        assertEquals(ld, result);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalDate_NonCalendar()
    {
        CalendarConversions.toLocalDate(new Object(), converter);
    }

    /* --------------------------------------------------------------------- *
     * 11. toLocalTime
     * --------------------------------------------------------------------- */

    @Test
    public void testToLocalTime_Basic()
    {
        LocalTime lt = LocalTime.of(08, 15, 30, 250_000_000);
        Calendar cal = calendarOf(lt.atDate(LocalDate.of(2022, 1, 1)), ZoneId.of("UTC"));
        LocalTime result = CalendarConversions.toLocalTime(cal, converter);
        assertEquals(lt, result);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalTime_NonCalendar()
    {
        CalendarConversions.toLocalTime(Boolean.FALSE, converter);
    }

    /* --------------------------------------------------------------------- *
     * 12. toBigDecimal
     * --------------------------------------------------------------------- */

    @Test
    public void testToBigDecimal_Basic()
    {
        long epoch = 7_000_000_000L;
        Calendar cal = calendarAt(epoch, TimeZone.getTimeZone("UTC"));
        // Expected = epochMillis / BIG_DECIMAL_GRAND
        BigDecimal expected = new BigDecimal(epoch).divide(BigDecimalConversions.GRAND);
        BigDecimal actual = CalendarConversions.toBigDecimal(cal, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToBigDecimal_NegativeEpoch()
    {
        long epoch = -123_456L;
        Calendar cal = calendarAt(epoch, TimeZone.getTimeZone("UTC"));
        BigDecimal expected = new BigDecimal(epoch).divide(BigDecimalConversions.GRAND);
        BigDecimal actual = CalendarConversions.toBigDecimal(cal, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_NonCalendar()
    {
        CalendarConversions.toBigDecimal(123, converter);
    }

    /* --------------------------------------------------------------------- *
     * 13. toBigInteger
     * --------------------------------------------------------------------- */

    @Test
    public void testToBigInteger_Basic()
    {
        long epoch = 8_000_000_000L;
        Calendar cal = calendarAt(epoch, TimeZone.getTimeZone("UTC"));
        BigInteger expected = BigInteger.valueOf(epoch * 1_000_000L);
        assertEquals(expected, CalendarConversions.toBigInteger(cal, converter));
    }

    @Test
    public void testToBigInteger_NegativeEpoch()
    {
        long epoch = -42L;
        Calendar cal = calendarAt(epoch, TimeZone.getTimeZone("UTC"));
        BigInteger expected = BigInteger.valueOf(epoch * 1_000_000L);
        assertEquals(expected, CalendarConversions.toBigInteger(cal, converter));
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_NonCalendar()
    {
        CalendarConversions.toBigInteger("string", converter);
    }

    /* --------------------------------------------------------------------- *
     * 14. clone
     * --------------------------------------------------------------------- */

    @Test
    public void testClone_Independence()
    {
        Calendar original = calendarAt(9_000_000_000L, TimeZone.getTimeZone("UTC"));
        Calendar copy = CalendarConversions.clone(original, converter);
        assertNotSame(original, copy);
        assertEquals(original.getTimeInMillis(), copy.getTimeInMillis());
        assertEquals(original.getTimeZone(), copy.getTimeZone());

        // Mutate the clone and verify the original stays unchanged
        copy.add(Calendar.DAY_OF_MONTH, 1);
        assertNotEquals(original.getTimeInMillis(), copy.getTimeInMillis());
    }

    @Test(expected = ClassCastException.class)
    public void testClone_NonCalendar()
    {
        CalendarConversions.clone("not a calendar", converter);
    }

    /* --------------------------------------------------------------------- *
     * 15. create
     * --------------------------------------------------------------------- */

    @Test
    public void testCreate_Basic()
    {
        long epoch = 10_000_000_000L;
        Calendar cal = CalendarConversions.create(epoch, converter);
        assertEquals(epoch, cal.getTimeInMillis());
        // The zone used should be whatever the ConverterOptions specifies;
        // by default we expect the JVM default zone.
        assertEquals(options.getTimeZone(), cal.getTimeZone());
    }

    @Test
    public void testCreate_WithCustomTimeZone()
    {
        // Override the options for this test
        ConverterOptions customOpts = new ConverterOptions();
        customOpts.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        Converter customConverter = new Converter(customOpts);

        long epoch = 11_000_000_000L;
        Calendar cal = CalendarConversions.create(epoch, customConverter);
        assertEquals(epoch, cal.getTimeInMillis());
        assertEquals(customOpts.getTimeZone(), cal.getTimeZone());
    }

    @Test(expected = NullPointerException.class)
    public void testCreate_NullConverter()
    {
        CalendarConversions.create(0L, null);
    }

    /* --------------------------------------------------------------------- *
     * 16. toString
     * --------------------------------------------------------------------- */

    @Test
    public void testToString_Basic()
    {
        Calendar cal = calendarAt(12_345_678_901L, TimeZone.getTimeZone("UTC"));
        String result = CalendarConversions.toString(cal, converter);
        // The format is delegated to DateConversions – we only assert that it
        // returns a non‑null, non‑empty string that parses back to the same epoch.
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Verify round‑trip using the same converter (DateConversions.parse is not
        // part of this repository, so we simply compare epoch values).
        Date parsed = DateConversions.toDate(result, converter);
        assertEquals(cal.getTimeInMillis(), parsed.getTime());
    }

    @Test(expected = ClassCastException.class)
    public void testToString_NonCalendar()
    {
        CalendarConversions.toString(12345, converter);
    }

    /* --------------------------------------------------------------------- *
     * 17. toOffsetDateTime
     * --------------------------------------------------------------------- */

    @Test
    public void testToOffsetDateTime_Utc()
    {
        Calendar cal = calendarAt(13_000_000_000L, TimeZone.getTimeZone("UTC"));
        OffsetDateTime odt = CalendarConversions.toOffsetDateTime(cal, converter);
        assertEquals(Instant.ofEpochMilli(13_000_000_000L), odt.toInstant());
        assertEquals(ZoneOffset.UTC, odt.getOffset());
    }

    @Test
    public void testToOffsetDateTime_NonZeroOffset()
    {
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles"); // typically UTC‑8 / UTC‑7
        Calendar cal = calendarAt(13_000_000_000L, tz);
        OffsetDateTime odt = CalendarConversions.toOffsetDateTime(cal, converter);
        ZoneOffset expected = ZoneOffset.ofTotalSeconds(tz.getOffset(13_000_000_000L) / 1000);
        assertEquals(expected, odt.getOffset());
        assertEquals(Instant.ofEpochMilli(13_000_000_000L), odt.toInstant());
    }

    @Test(expected = ClassCastException.class)
    public void testToOffsetDateTime_NonCalendar()
    {
        CalendarConversions.toOffsetDateTime(new Object(), converter);
    }

    /* --------------------------------------------------------------------- *
     * 18. toMap
     * --------------------------------------------------------------------- */

    @Test
    public void testToMap_Basic()
    {
        // 2022‑03‑14 15:09:26.123 in UTC
        Calendar cal = calendarOf(
                LocalDateTime.of(2022, 3, 14, 15, 9, 26, 123_000_000),
                ZoneId.of("UTC"));
        Map<String, Object> map = CalendarConversions.toMap(cal, converter);

        assertEquals("2022-03-14", map.get(MapConversions.DATE));
        assertEquals("15:09:26.123", map.get(MapConversions.TIME));
        assertEquals("UTC", map.get(MapConversions.ZONE));
        assertEquals(cal.getTimeInMillis(), map.get(MapConversions.EPOCH_MILLIS));
    }

    @Test
    public void testToMap_WithNonZeroZoneOffset()
    {
        ZoneId zone = ZoneId.of("Australia/Sydney"); // UTC+10 (or +11 with DST)
        Calendar cal = calendarOf(
                LocalDateTime.of(2022, 12, 25, 0, 0, 0, 0),
                zone);
        Map<String, Object> map = CalendarConversions.toMap(cal, converter);

        assertEquals("2022-12-25", map.get(MapConversions.DATE));
        // The time part should be 00:00:00.000 irrespective of the zone because we
        // read the fields directly from the Calendar.
        assertEquals("00:00:00.000", map.get(MapConversions.TIME));
        assertEquals(zone.toString(), map.get(MapConversions.ZONE));
        assertEquals(cal.getTimeInMillis(), map.get(MapConversions.EPOCH_MILLIS));
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_NonCalendar()
    {
        CalendarConversions.toMap(null, converter);
    }
}
