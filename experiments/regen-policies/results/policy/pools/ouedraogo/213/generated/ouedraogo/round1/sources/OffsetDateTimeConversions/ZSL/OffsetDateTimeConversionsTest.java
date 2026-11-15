package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
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
 * Unit tests for {@link OffsetDateTimeConversions}.
 * The tests cover every public static conversion method and verify that the
 * returned values are consistent with the Java Time API and the conversion
 * logic used inside the class.
 *
 * Note: The library’s {@link Converter} class requires a {@link ConverterOptions}
 * instance.  The default constructor of {@code ConverterOptions} (provided by the
 * library) configures the system default time‑zone and {@code ZoneId}, which is
 * sufficient for the purpose of these tests.
 */
public class OffsetDateTimeConversionsTest {

    private Converter converter;
    private ZoneId zoneId;
    private TimeZone timeZone;
    private OffsetDateTime sampleOffsetDateTime;
    private Instant sampleInstant;

    @Before
    public void setUp() {
        // Initialise a Converter with default options (system default zone)
        ConverterOptions options = new ConverterOptions(); // assumes a public no‑arg ctor
        converter = new Converter(options);
        zoneId = converter.getOptions().getZoneId();          // should be system default
        timeZone = converter.getOptions().getTimeZone();    // should be system default

        // Create a deterministic OffsetDateTime (UTC) for repeatable assertions
        sampleOffsetDateTime = OffsetDateTime.of(
                2021, 5, 15,                // 2021‑05‑15
                10, 30, 45,                // 10:30:45
                123_456_789,                // 123,456,789 nanoseconds
                ZoneOffset.UTC);           // UTC offset
        sampleInstant = sampleOffsetDateTime.toInstant();
    }

    @Test
    public void testToInstant() {
        Instant result = OffsetDateTimeConversions.toInstant(sampleOffsetDateTime, converter);
        assertEquals(sampleInstant, result);
    }

    @Test
    public void testToLong() {
        long expected = sampleInstant.toEpochMilli();
        long result = OffsetDateTimeConversions.toLong(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(sampleOffsetDateTime, converter);
        assertEquals(sampleInstant.toEpochMilli(), result.get());
    }

    @Test
    public void testToDouble() {
        double expected = sampleInstant.getEpochSecond() + sampleInstant.getNano() / 1_000_000_000.0;
        double result = OffsetDateTimeConversions.toDouble(sampleOffsetDateTime, converter);
        assertEquals(expected, result, 0.0);
    }

    @Test
    public void testToBigInteger() {
        // Expected value: epoch seconds * 1_000_000_000 + nanos
        BigInteger expected = BigInteger.valueOf(sampleInstant.getEpochSecond())
                .multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(sampleInstant.getNano()));
        BigInteger result = OffsetDateTimeConversions.toBigInteger(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToBigDecimal() {
        // Expected value: seconds + nanos / 1_000_000_000 as a BigDecimal
        BigDecimal expected = BigDecimal.valueOf(sampleInstant.getEpochSecond())
                .add(BigDecimal.valueOf(sampleInstant.getNano(), 9));
        BigDecimal result = OffsetDateTimeConversions.toBigDecimal(sampleOffsetDateTime, converter);
        assertEquals(0, expected.compareTo(result));
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime expected = sampleOffsetDateTime.toInstant()
                .atZone(zoneId)
                .toLocalDateTime();
        LocalDateTime result = OffsetDateTimeConversions.toLocalDateTime(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalDate() {
        LocalDate expected = sampleOffsetDateTime.toInstant()
                .atZone(zoneId)
                .toLocalDate();
        LocalDate result = OffsetDateTimeConversions.toLocalDate(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalTime() {
        LocalTime expected = sampleOffsetDateTime.toInstant()
                .atZone(zoneId)
                .toLocalTime();
        LocalTime result = OffsetDateTimeConversions.toLocalTime(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToTimestamp() {
        Timestamp expected = Timestamp.from(sampleInstant);
        Timestamp result = OffsetDateTimeConversions.toTimestamp(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToCalendar() {
        Calendar result = OffsetDateTimeConversions.toCalendar(sampleOffsetDateTime, converter);
        assertEquals(timeZone, result.getTimeZone());
        assertEquals(sampleInstant.toEpochMilli(), result.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date expected = java.sql.Date.valueOf(
                sampleOffsetDateTime.atZoneSameInstant(zoneId).toLocalDate()
        );
        java.sql.Date result = OffsetDateTimeConversions.toSqlDate(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime expected = sampleInstant.atZone(zoneId);
        ZonedDateTime result = OffsetDateTimeConversions.toZonedDateTime(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToDate() {
        Date expected = new Date(sampleInstant.toEpochMilli());
        Date result = OffsetDateTimeConversions.toDate(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToOffsetTime() {
        OffsetTime expected = sampleOffsetDateTime.toOffsetTime();
        OffsetTime result = OffsetDateTimeConversions.toOffsetTime(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToYear() {
        Year expected = Year.from(sampleOffsetDateTime.atZoneSameInstant(zoneId).toLocalDate());
        Year result = OffsetDateTimeConversions.toYear(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToYearMonth() {
        YearMonth expected = YearMonth.from(sampleOffsetDateTime.atZoneSameInstant(zoneId).toLocalDate());
        YearMonth result = OffsetDateTimeConversions.toYearMonth(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToMonthDay() {
        MonthDay expected = MonthDay.from(sampleOffsetDateTime.atZoneSameInstant(zoneId).toLocalDate());
        MonthDay result = OffsetDateTimeConversions.toMonthDay(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToString() {
        String expected = sampleOffsetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String result = OffsetDateTimeConversions.toString(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToMap() {
        Map<String, Object> result = OffsetDateTimeConversions.toMap(sampleOffsetDateTime, converter);
        assertNotNull(result);
        assertTrue(result.containsKey(MapConversions.OFFSET_DATE_TIME));
        assertEquals(OffsetDateTimeConversions.toString(sampleOffsetDateTime, converter),
                     result.get(MapConversions.OFFSET_DATE_TIME));
    }
}
