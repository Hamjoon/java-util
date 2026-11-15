package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link LocalDateTimeConversions}.
 *
 * <p>The tests exercise every conversion method using a fixed {@link LocalDateTime}
 * instance and a {@link Converter} whose options are configured to UTC.  This
 * eliminates any variability caused by the host JVM’s default time‑zone.</p>
 *
 * <p>All assertions compare the result of the conversion method with an
 * equivalent construction performed directly with the JDK time API (or the
 * corresponding {@code InstantConversions} helper where required).</p>
 */
public class LocalDateTimeConversionsTest {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    private Converter converter;
    private LocalDateTime sampleLdt;

    @Before
    public void setUp() {
        // Build a Converter whose options force UTC for both ZoneId and TimeZone.
        // ConverterOptions is part of the library; we only use its public mutators.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(UTC);
        options.setTimeZone(UTC_TZ);
        converter = new Converter(options);

        // Sample LocalDateTime includes date, time and a non‑zero nanosecond component.
        sampleLdt = LocalDateTime.of(2021, Month.MARCH, 14, 15, 9, 26, 123_456_789);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime expected = ZonedDateTime.of(sampleLdt, UTC);
        ZonedDateTime actual = LocalDateTimeConversions.toZonedDateTime(sampleLdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime() {
        ZoneOffset offset = ZoneOffset.UTC; // UTC has zero offset
        OffsetDateTime expected = sampleLdt.atOffset(offset);
        OffsetDateTime actual = LocalDateTimeConversions.toOffsetDateTime(sampleLdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToInstant() {
        Instant expected = sampleLdt.atZone(UTC).toInstant();
        Instant actual = LocalDateTimeConversions.toInstant(sampleLdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLong() {
        long expected = sampleLdt.atZone(UTC).toInstant().toEpochMilli();
        long actual = LocalDateTimeConversions.toLong(sampleLdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDouble() {
        // InstantConversions.toDouble ultimately returns the epoch‑millis as a double.
        double expected = (double) LocalDateTimeConversions.toLong(sampleLdt, converter);
        double actual = LocalDateTimeConversions.toDouble(sampleLdt, converter);
        assertEquals(expected, actual, 0.0);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime actual = LocalDateTimeConversions.toLocalDateTime(sampleLdt, converter);
        assertEquals(sampleLdt, actual);
    }

    @Test
    public void testToLocalDate() {
        LocalDate expected = sampleLdt.toLocalDate();
        LocalDate actual = LocalDateConversions.toLocalDate(sampleLdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime() {
        LocalTime expected = sampleLdt.toLocalTime();
        LocalTime actual = LocalDateTimeConversions.toLocalTime(sampleLdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToAtomicLong() {
        long expected = LocalDateTimeConversions.toLong(sampleLdt, converter);
        AtomicLong actual = LocalDateTimeConversions.toAtomicLong(sampleLdt, converter);
        assertEquals(expected, actual.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp expected = Timestamp.from(sampleLdt.atZone(UTC).toInstant());
        Timestamp actual = LocalDateTimeConversions.toTimestamp(sampleLdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        long expectedMillis = LocalDateTimeConversions.toLong(sampleLdt, converter);
        Calendar actual = LocalDateTimeConversions.toCalendar(sampleLdt, converter);
        assertEquals(UTC_TZ, actual.getTimeZone());
        assertEquals(expectedMillis, actual.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date expected = java.sql.Date.valueOf(sampleLdt.toLocalDate());
        java.sql.Date actual = LocalDateTimeConversions.toSqlDate(sampleLdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate() {
        Date expected = new Date(LocalDateTimeConversions.toLong(sampleLdt, converter));
        Date actual = LocalDateTimeConversions.toDate(sampleLdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigInteger() {
        BigInteger expected = BigInteger.valueOf(LocalDateTimeConversions.toLong(sampleLdt, converter));
        BigInteger actual = LocalDateTimeConversions.toBigInteger(sampleLdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal expected = BigDecimal.valueOf(LocalDateTimeConversions.toLong(sampleLdt, converter));
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(sampleLdt, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToYear() {
        Year expected = Year.of(sampleLdt.getYear());
        Year actual = LocalDateTimeConversions.toYear(sampleLdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToYearMonth() {
        YearMonth expected = YearMonth.of(sampleLdt.getYear(), sampleLdt.getMonth());
        YearMonth actual = LocalDateTimeConversions.toYearMonth(sampleLdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMonthDay() {
        MonthDay expected = MonthDay.of(sampleLdt.getMonth(), sampleLdt.getDayOfMonth());
        MonthDay actual = LocalDateTimeConversions.toMonthDay(sampleLdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToString() {
        String expected = sampleLdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String actual = LocalDateTimeConversions.toString(sampleLdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap() {
        Map<?, ?> map = LocalDateTimeConversions.toMap(sampleLdt, converter);
        assertNotNull(map);
        assertTrue(map.containsKey(MapConversions.LOCAL_DATE_TIME));
        assertEquals(sampleLdt.toString(), map.get(MapConversions.LOCAL_DATE_TIME));
    }
}
