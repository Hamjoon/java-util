package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive JUnit‑4 test suite for {@code LocalDateTimeConversions}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>basic happy‑path conversion for every public static method</li>
 *   <li>edge‑case handling such as {@code null} arguments and wrong source types</li>
 *   <li>verification of zone‑related behaviour (UTC is used as the default test zone)</li>
 * </ul>
 *
 * <p>All tests use only the public API that is visible in the supplied source code
 * (the {@code Converter} class and its {@code ConverterOptions} constructor).
 * No mocking frameworks are used – real objects are instantiated.
 */
public class LocalDateTimeConversionsTest {

    /** A {@code Converter} that works with the UTC time‑zone – the default for the library. */
    private Converter converter;

    /** Sample {@code LocalDateTime} used by most test cases (2020‑01‑01T12:34:56). */
    private static final LocalDateTime SAMPLE_LDT = LocalDateTime.of(2020, 1, 1, 12, 34, 56, 0);

    /** The instant that corresponds to {@link #SAMPLE_LDT} in UTC. */
    private static final Instant SAMPLE_INSTANT = SAMPLE_LDT.toInstant(ZoneOffset.UTC);

    @Before
    public void setUp() {
        // The library provides a public no‑arg constructor for ConverterOptions that defaults to UTC.
        // This is the only way to obtain a usable Converter without using reflection or mocks.
        ConverterOptions options = new ConverterOptions();           // defaults to UTC
        converter = new Converter(options);
    }

    /* -------------------------------------------------------------
     *  Basic “happy‑path” tests – one per conversion method
     * ------------------------------------------------------------- */

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = LocalDateTimeConversions.toZonedDateTime(SAMPLE_LDT, converter);
        assertEquals(ZoneId.of("Z"), zdt.getZone());                // UTC zone
        assertEquals(SAMPLE_INSTANT, zdt.toInstant());
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(SAMPLE_LDT, converter);
        assertEquals(ZoneOffset.UTC, odt.getOffset());
        assertEquals(SAMPLE_INSTANT, odt.toInstant());
    }

    @Test
    public void testToInstant() {
        Instant instant = LocalDateTimeConversions.toInstant(SAMPLE_LDT, converter);
        assertEquals(SAMPLE_INSTANT, instant);
    }

    @Test
    public void testToLong() {
        long epochMilli = LocalDateTimeConversions.toLong(SAMPLE_LDT, converter);
        assertEquals(SAMPLE_INSTANT.toEpochMilli(), epochMilli);
    }

    @Test
    public void testToDouble() {
        double d = LocalDateTimeConversions.toDouble(SAMPLE_LDT, converter);
        // The library’s InstantConversions.toDouble returns the epoch‑millis as a double.
        // Verify that the conversion is consistent with {@link #toLong()}.
        assertEquals((double) LocalDateTimeConversions.toLong(SAMPLE_LDT, converter), d, 0.0);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = LocalDateTimeConversions.toLocalDateTime(SAMPLE_LDT, converter);
        assertEquals(SAMPLE_LDT, ldt);
    }

    @Test
    public void testToLocalDate() {
        LocalDate date = LocalDateTimeConversions.toLocalDate(SAMPLE_LDT, converter);
        assertEquals(SAMPLE_LDT.toLocalDate(), date);
    }

    @Test
    public void testToLocalTime() {
        LocalTime time = LocalDateTimeConversions.toLocalTime(SAMPLE_LDT, converter);
        assertEquals(SAMPLE_LDT.toLocalTime(), time);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong atomic = LocalDateTimeConversions.toAtomicLong(SAMPLE_LDT, converter);
        assertEquals(LocalDateTimeConversions.toLong(SAMPLE_LDT, converter), atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = LocalDateTimeConversions.toTimestamp(SAMPLE_LDT, converter);
        assertEquals(SAMPLE_INSTANT.toEpochMilli(), ts.getTime());
    }

    @Test
    public void testToCalendar() {
        Calendar cal = LocalDateTimeConversions.toCalendar(SAMPLE_LDT, converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(SAMPLE_INSTANT.toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = LocalDateTimeConversions.toSqlDate(SAMPLE_LDT, converter);
        assertEquals(java.sql.Date.valueOf(SAMPLE_LDT.toLocalDate()), sqlDate);
    }

    @Test
    public void testToDate() {
        Date date = LocalDateTimeConversions.toDate(SAMPLE_LDT, converter);
        assertEquals(SAMPLE_INSTANT.toEpochMilli(), date.getTime());
    }

    @Test
    public void testToBigInteger() {
        BigInteger bi = LocalDateTimeConversions.toBigInteger(SAMPLE_LDT, converter);
        assertEquals(BigInteger.valueOf(SAMPLE_INSTANT.toEpochMilli()), bi);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = LocalDateTimeConversions.toBigDecimal(SAMPLE_LDT, converter);
        assertEquals(BigDecimal.valueOf(SAMPLE_INSTANT.toEpochMilli()), bd);
    }

    @Test
    public void testToYear() {
        Year year = LocalDateTimeConversions.toYear(SAMPLE_LDT, converter);
        assertEquals(Year.of(2020), year);
    }

    @Test
    public void testToYearMonth() {
        YearMonth ym = LocalDateTimeConversions.toYearMonth(SAMPLE_LDT, converter);
        assertEquals(YearMonth.of(2020, 1), ym);
    }

    @Test
    public void testToMonthDay() {
        MonthDay md = LocalDateTimeConversions.toMonthDay(SAMPLE_LDT, converter);
        assertEquals(MonthDay.of(1, 1), md);
    }

    @Test
    public void testToString() {
        String s = LocalDateTimeConversions.toString(SAMPLE_LDT, converter);
        assertEquals(SAMPLE_LDT.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), s);
    }

    @Test
    public void testToMap() {
        Map<?, ?> map = LocalDateTimeConversions.toMap(SAMPLE_LDT, converter);
        assertTrue(map.containsKey(MapConversions.LOCAL_DATE_TIME));
        assertEquals(SAMPLE_LDT.toString(), map.get(MapConversions.LOCAL_DATE_TIME));
    }

    /* -------------------------------------------------------------
     *  Edge‑case / exception tests
     * ------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullFrom() {
        LocalDateTimeConversions.toZonedDateTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullConverter() {
        LocalDateTimeConversions.toZonedDateTime(SAMPLE_LDT, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongSourceType() {
        LocalDateTimeConversions.toZonedDateTime("not a LocalDateTime", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullFrom() {
        LocalDateTimeConversions.toOffsetDateTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullConverter() {
        LocalDateTimeConversions.toInstant(SAMPLE_LDT, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullFrom() {
        LocalDateTimeConversions.toLong(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullFrom() {
        LocalDateTimeConversions.toDouble(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullFrom() {
        LocalDateTimeConversions.toLocalDate(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullFrom() {
        LocalDateTimeConversions.toLocalTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullFrom() {
        LocalDateTimeConversions.toAtomicLong(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullFrom() {
        LocalDateTimeConversions.toTimestamp(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullFrom() {
        LocalDateTimeConversions.toCalendar(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullFrom() {
        LocalDateTimeConversions.toSqlDate(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullFrom() {
        LocalDateTimeConversions.toDate(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullFrom() {
        LocalDateTimeConversions.toBigInteger(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullFrom() {
        LocalDateTimeConversions.toBigDecimal(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToYear_NullFrom() {
        LocalDateTimeConversions.toYear(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToYearMonth_NullFrom() {
        LocalDateTimeConversions.toYearMonth(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMonthDay_NullFrom() {
        LocalDateTimeConversions.toMonthDay(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullFrom() {
        LocalDateTimeConversions.toString(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullFrom() {
        LocalDateTimeConversions.toMap(null, converter);
    }

    /* -------------------------------------------------------------
     *  Additional logical edge cases
     * ------------------------------------------------------------- */

    @Test
    public void testLeapYearDate() {
        LocalDateTime leapDay = LocalDateTime.of(2020, 2, 29, 23, 59, 59);
        ZonedDateTime zdt = LocalDateTimeConversions.toZonedDateTime(leapDay, converter);
        assertEquals(2020, zdt.getYear());
        assertEquals(2, zdt.getMonthValue());
        assertEquals(29, zdt.getDayOfMonth());
    }

    @Test
    public void testEndOfYearTransition() {
        LocalDateTime endOfYear = LocalDateTime.of(1999, 12, 31, 23, 59, 59, 999_000_000);
        long epochMilli = LocalDateTimeConversions.toLong(endOfYear, converter);
        Instant expected = Instant.ofEpochMilli(epochMilli);
        assertEquals(endOfYear.toInstant(ZoneOffset.UTC), expected);
    }

    @Test
    public void testVeryEarlyDate() {
        LocalDateTime early = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        assertEquals(0L, LocalDateTimeConversions.toLong(early, converter));
    }

    @Test
    public void testVeryLateDate() {
        LocalDateTime farFuture = LocalDateTime.of(3000, 1, 1, 0, 0);
        long epochMilli = LocalDateTimeConversions.toLong(farFuture, converter);
        // The value must be positive and greater than the epoch for 2020‑01‑01.
        assertTrue(epochMilli > LocalDateTimeConversions.toLong(SAMPLE_LDT, converter));
    }
}
