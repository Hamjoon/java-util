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
import java.util.Map;

import static org.junit.Assert.*;

public class TimestampConversionsTest
{
    private Converter converter;

    /**
     * Helper that creates a {@link Timestamp} from an {@link Instant}.
     * The {@code Timestamp} constructor only stores milliseconds, so we
     * explicitly set the nanoseconds part to match the supplied {@code Instant}.
     */
    private static Timestamp tsFromInstant(Instant instant)
    {
        Timestamp ts = new Timestamp(instant.toEpochMilli());
        ts.setNanos(instant.getNano());   // preserve full nanosecond precision
        return ts;
    }

    @Before
    public void setUp()
    {
        // The library provides a public constructor that accepts a ConverterOptions instance.
        // Using the default constructor (no‑arg) gives us the system default zone and
        // time‑zone, which is sufficient for the expectations we calculate in the tests.
        converter = new Converter(new ConverterOptions());
    }

    @Test
    public void testToDouble_MillisecondPrecision()
    {
        // 2023‑03‑25T12:34:56.123Z  (exactly millisecond precision)
        Instant instant = Instant.parse("2023-03-25T12:34:56.123Z");
        Timestamp ts = tsFromInstant(instant);

        double actual = TimestampConversions.toDouble(ts, converter);
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;

        assertEquals(expected, actual, 1e-12);
    }

    @Test
    public void testToDouble_NanosecondPrecision()
    {
        // 2023‑03‑25T12:34:56.123456789Z  (nanosecond precision)
        Instant instant = Instant.parse("2023-03-25T12:34:56.123456789Z");
        Timestamp ts = tsFromInstant(instant);

        double actual = TimestampConversions.toDouble(ts, converter);
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;

        assertEquals(expected, actual, 1e-12);
    }

    @Test
    public void testToBigDecimal()
    {
        Instant instant = Instant.parse("2022-01-01T00:00:01.999999999Z");
        Timestamp ts = tsFromInstant(instant);

        BigDecimal actual = TimestampConversions.toBigDecimal(ts, converter);
        BigDecimal expected = BigDecimal.valueOf(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano(), 9));

        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToBigInteger()
    {
        // Instant with nanoseconds – the conversion to BigInteger should drop the
        // fractional part (only whole seconds are kept).
        Instant instant = Instant.parse("2021-12-31T23:59:59.500Z");
        Timestamp ts = tsFromInstant(instant);

        BigInteger actual = TimestampConversions.toBigInteger(ts, converter);
        BigInteger expected = BigInteger.valueOf(instant.getEpochSecond());

        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDateTime()
    {
        Instant instant = Instant.parse("2020-02-29T08:15:30.250Z");
        Timestamp ts = tsFromInstant(instant);

        LocalDateTime actual = TimestampConversions.toLocalDateTime(ts, converter);
        LocalDateTime expected = instant.atZone(converter.getOptions().getZoneId()).toLocalDateTime();

        assertEquals(expected, actual);
    }

    @Test
    public void testToDuration()
    {
        Instant instant = Instant.parse("1970-01-01T00:00:10.123456789Z");
        Timestamp ts = tsFromInstant(instant);

        Duration actual = TimestampConversions.toDuration(ts, converter);
        Duration expected = Duration.between(Instant.EPOCH, instant);

        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime()
    {
        Instant instant = Instant.parse("2019-07-04T14:00:00.000Z");
        Timestamp ts = tsFromInstant(instant);

        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, converter);
        OffsetDateTime expected = instant.atZone(converter.getOptions().getZoneId()).toOffsetDateTime();

        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar()
    {
        Instant instant = Instant.parse("2018-11-11T11:11:11.111Z");
        Timestamp ts = tsFromInstant(instant);

        Calendar actual = TimestampConversions.toCalendar(ts, converter);
        Calendar expected = Calendar.getInstance(converter.getOptions().getTimeZone());
        expected.setTimeInMillis(ts.getTime());

        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTimeZone(), actual.getTimeZone());
    }

    @Test
    public void testToDate()
    {
        Instant instant = Instant.parse("2017-05-05T05:05:05.555Z");
        Timestamp ts = tsFromInstant(instant);

        Date actual = TimestampConversions.toDate(ts, converter);
        Date expected = Date.from(instant);

        assertEquals(expected, actual);
    }

    @Test
    public void testToSqlDate()
    {
        // The SQL Date contains only the date part, time‑zone conversion is performed
        // using the Converter's ZoneId.
        Instant instant = Instant.parse("2016-12-31T23:00:00Z");
        Timestamp ts = tsFromInstant(instant);

        java.sql.Date actual = TimestampConversions.toSqlDate(ts, converter);
        java.sql.Date expected = java.sql.Date.valueOf(
                instant.atZone(converter.getOptions().getZoneId()).toLocalDate());

        assertEquals(expected, actual);
    }

    @Test
    public void testToLong()
    {
        Instant instant = Instant.parse("2015-03-15T09:30:00.001Z");
        Timestamp ts = tsFromInstant(instant);

        long actual = TimestampConversions.toLong(ts, converter);
        long expected = ts.getTime();   // milliseconds since epoch

        assertEquals(expected, actual);
    }

    @Test
    public void testToYear()
    {
        Instant instant = Instant.parse("2014-02-20T00:00:00Z");
        Timestamp ts = tsFromInstant(instant);

        Year actual = TimestampConversions.toYear(ts, converter);
        Year expected = Year.from(
                instant.atZone(converter.getOptions().getZoneId()).toLocalDate());

        assertEquals(expected, actual);
    }

    @Test
    public void testToYearMonth()
    {
        Instant instant = Instant.parse("2013-08-31T12:00:00Z");
        Timestamp ts = tsFromInstant(instant);

        YearMonth actual = TimestampConversions.toYearMonth(ts, converter);
        YearMonth expected = YearMonth.from(
                instant.atZone(converter.getOptions().getZoneId()).toLocalDate());

        assertEquals(expected, actual);
    }

    @Test
    public void testToMonthDay()
    {
        Instant instant = Instant.parse("2012-02-29T23:59:59Z");
        Timestamp ts = tsFromInstant(instant);

        MonthDay actual = TimestampConversions.toMonthDay(ts, converter);
        MonthDay expected = MonthDay.from(
                instant.atZone(converter.getOptions().getZoneId()).toLocalDate());

        assertEquals(expected, actual);
    }

    @Test
    public void testToString_MillisecondPrecision()
    {
        // Millisecond precision => pattern with 3 fractional digits
        Instant instant = Instant.parse("2021-01-01T00:00:00.500Z");
        Timestamp ts = tsFromInstant(instant); // nanos = 500_000_000 (divisible by 1_000_000)

        String actual = TimestampConversions.toString(ts, converter);
        String expected = instant
                .atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

        assertEquals(expected, actual);
    }

    @Test
    public void testToString_NanosecondPrecision()
    {
        // Nanosecond precision => pattern with 9 fractional digits
        Instant instant = Instant.parse("2021-01-01T00:00:00.123456789Z");
        Timestamp ts = tsFromInstant(instant); // nanos = 123_456_789 (not divisible by 1_000_000)

        String actual = TimestampConversions.toString(ts, converter);
        String expected = instant
                .atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'"));

        assertEquals(expected, actual);
    }

    @Test
    public void testToMap()
    {
        Instant instant = Instant.parse("2020-10-10T10:10:10.010Z");
        Timestamp ts = tsFromInstant(instant);

        Map<String, Object> actual = TimestampConversions.toMap(ts, converter);
        assertNotNull(actual);
        assertEquals(1, actual.size());

        // The key used by TimestampConversions is defined in MapConversions.TIMESTAMP.
        // Since the constant is not visible here we simply verify that the map contains
        // exactly the formatted string value.
        Object value = actual.values().iterator().next();
        assertTrue(value instanceof String);
        assertEquals(TimestampConversions.toString(ts, converter), value);
    }
}
