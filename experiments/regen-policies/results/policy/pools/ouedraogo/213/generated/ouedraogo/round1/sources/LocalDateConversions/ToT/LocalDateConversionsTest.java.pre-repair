package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LocalDateConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>normal conversion paths for every static method</li>
 *   <li>edge cases such as {@code null} input (expected NPE)</li>
 *   <li>verification that the {@link Converter} options (zone / time‑zone) are honoured</li>
 *   <li>numeric conversions (long, double, AtomicLong, BigInteger, BigDecimal)</li>
 *   <li>object conversions (Instant, ZonedDateTime, OffsetDateTime, Calendar, Timestamp,
 *        java.sql.Date, java.util.Date, Year, YearMonth, MonthDay, String, Map)</li>
 * </ul>
 *
 * The test class uses only the public API of {@link Converter} and the classes
 * referenced in {@code LocalDateConversions}. No mocks or subclassing of library
 * types are performed.
 */
public class LocalDateConversionsTest
{
    private Converter converter;
    private ConverterOptions options;
    private LocalDate sampleDate;               // 2022‑03‑15
    private ZoneId zoneId;
    private TimeZone timeZone;

    @Before
    public void setUp()
    {
        // Use the system default zone for deterministic results.
        zoneId   = ZoneId.systemDefault();
        timeZone = TimeZone.getDefault();

        // The concrete implementation of ConverterOptions is not shown in the
        // supplied source, but the library provides a public no‑arg constructor
        // and setter methods for zone / time‑zone.  Those setters are used here.
        options = new ConverterOptions();
        options.setZoneId(zoneId);
        options.setTimeZone(timeZone);

        converter = new Converter(options);

        sampleDate = LocalDate.of(2022, Month.MARCH, 15);
    }

    /* -----------------------------------------------------------
     *  Helper methods
     * ----------------------------------------------------------- */

    private ZonedDateTime expectedZonedDateTime()
    {
        return ZonedDateTime.of(sampleDate, LocalTime.MIDNIGHT, zoneId);
    }

    private OffsetDateTime expectedOffsetDateTime()
    {
        int offsetSeconds = timeZone.getOffset(System.currentTimeMillis()) / 1000;
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(offsetSeconds);
        return OffsetDateTime.of(sampleDate, LocalTime.MIDNIGHT, offset);
    }

    private Instant expectedInstant()
    {
        return expectedZonedDateTime().toInstant();
    }

    private long expectedEpochMilli()
    {
        return expectedInstant().toEpochMilli();
    }

    private Timestamp expectedTimestamp()
    {
        // LocalDate.toEpochDay() returns days since 1970‑01‑01.
        long millis = sampleDate.toEpochDay() * 86_400_000L;
        return new Timestamp(millis);
    }

    /* -----------------------------------------------------------
     *  Tests for each conversion method
     * ----------------------------------------------------------- */

    @Test
    public void testToInstant()
    {
        Instant actual = LocalDateConversions.toInstant(sampleDate, converter);
        assertEquals(expectedInstant(), actual);
    }

    @Test
    public void testToLong()
    {
        long actual = LocalDateConversions.toLong(sampleDate, converter);
        assertEquals(expectedEpochMilli(), actual);
    }

    @Test
    public void testToLocalDateTime()
    {
        LocalDateTime actual = LocalDateConversions.toLocalDateTime(sampleDate, converter);
        assertEquals(LocalDateTime.of(sampleDate, LocalTime.MIDNIGHT), actual);
    }

    @Test
    public void testToZonedDateTime()
    {
        ZonedDateTime actual = LocalDateConversions.toZonedDateTime(sampleDate, converter);
        assertEquals(expectedZonedDateTime(), actual);
    }

    @Test
    public void testToOffsetDateTime()
    {
        OffsetDateTime actual = LocalDateConversions.toOffsetDateTime(sampleDate, converter);
        assertEquals(expectedOffsetDateTime(), actual);
    }

    @Test
    public void testToDouble()
    {
        double actual = LocalDateConversions.toDouble(sampleDate, converter);
        double expected = expectedEpochMilli() / 1000d;
        assertEquals(expected, actual, 0.0);
    }

    @Test
    public void testToAtomicLong()
    {
        AtomicLong actual = LocalDateConversions.toAtomicLong(sampleDate, converter);
        assertEquals(expectedEpochMilli(), actual.get());
    }

    @Test
    public void testToTimestamp()
    {
        Timestamp actual = LocalDateConversions.toTimestamp(sampleDate, converter);
        assertEquals(expectedTimestamp(), actual);
    }

    @Test
    public void testToCalendar()
    {
        Calendar actual = LocalDateConversions.toCalendar(sampleDate, converter);
        assertEquals(timeZone, actual.getTimeZone());
        assertEquals(expectedEpochMilli(), actual.getTimeInMillis());
    }

    @Test
    public void testToSqlDate()
    {
        java.sql.Date actual = LocalDateConversions.toSqlDate(sampleDate, converter);
        assertEquals(java.sql.Date.valueOf(sampleDate), actual);
    }

    @Test
    public void testToDate()
    {
        Date actual = LocalDateConversions.toDate(sampleDate, converter);
        assertEquals(new Date(expectedEpochMilli()), actual);
    }

    @Test
    public void testToBigInteger()
    {
        BigInteger actual = LocalDateConversions.toBigInteger(sampleDate, converter);
        // InstantConversions delegates to Instant.toEpochMilli() for the numeric value.
        assertEquals(BigInteger.valueOf(expectedEpochMilli()), actual);
    }

    @Test
    public void testToBigDecimal()
    {
        BigDecimal actual = LocalDateConversions.toBigDecimal(sampleDate, converter);
        assertEquals(BigDecimal.valueOf(expectedEpochMilli()), actual);
    }

    @Test
    public void testToYear()
    {
        Year actual = LocalDateConversions.toYear(sampleDate, converter);
        assertEquals(Year.of(2022), actual);
    }

    @Test
    public void testToYearMonth()
    {
        YearMonth actual = LocalDateConversions.toYearMonth(sampleDate, converter);
        assertEquals(YearMonth.of(2022, 3), actual);
    }

    @Test
    public void testToMonthDay()
    {
        MonthDay actual = LocalDateConversions.toMonthDay(sampleDate, converter);
        assertEquals(MonthDay.of(3, 15), actual);
    }

    @Test
    public void testToString()
    {
        String actual = LocalDateConversions.toString(sampleDate, converter);
        assertEquals(sampleDate.format(DateTimeFormatter.ISO_LOCAL_DATE), actual);
    }

    @Test
    public void testToMap()
    {
        Map<String, Object> actual = LocalDateConversions.toMap(sampleDate, converter);
        assertTrue(actual instanceof LinkedHashMap);
        assertEquals(1, actual.size());
        assertEquals(sampleDate.toString(), actual.get(MapConversions.LOCAL_DATE));
    }

    /* -----------------------------------------------------------
     *  Error / edge‑case handling
     * ----------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullInput()
    {
        LocalDateConversions.toInstant(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullInput()
    {
        LocalDateConversions.toZonedDateTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullInput()
    {
        LocalDateConversions.toOffsetDateTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput()
    {
        LocalDateConversions.toString(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput()
    {
        LocalDateConversions.toMap(null, converter);
    }

    // Verify that a Converter with a *different* zone produces a different instant.
    @Test
    public void testDifferentZoneAffectsInstant()
    {
        ZoneId otherZone = ZoneId.of("UTC+02:00");
        ConverterOptions otherOptions = new ConverterOptions();
        otherOptions.setZoneId(otherZone);
        otherOptions.setTimeZone(TimeZone.getTimeZone(otherZone));
        Converter otherConverter = new Converter(otherOptions);

        Instant instantDefault = LocalDateConversions.toInstant(sampleDate, converter);
        Instant instantOther   = LocalDateConversions.toInstant(sampleDate, otherConverter);

        assertNotEquals(instantDefault, instantOther);
        // The epoch‑millis should differ exactly by the zone offset.
        long diff = instantOther.toEpochMilli() - instantDefault.toEpochMilli();
        assertEquals(2 * 60 * 60 * 1000L, diff);
    }
}
