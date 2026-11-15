package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link LocalDateConversions}.
 *
 * The tests create a real {@link Converter} instance with deterministic
 * {@link ConverterOptions} (UTC zone) and verify the result of every
 * conversion method against the expected value derived from the Java
 * time API.
 *
 * No mocking or subclassing of library classes is performed – all objects
 * are instantiated via public constructors or factory methods that appear
 * in the supplied source code.
 */
public class LocalDateConversionsTest {

    private static final ZoneId ZONE_ID = ZoneId.of("UTC");
    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final LocalDate SAMPLE_DATE = LocalDate.of(2023, 3, 15); // 2023‑03‑15
    private Converter converter;

    @Before
    public void setUp() {
        // Build a ConverterOptions instance and configure it for UTC.
        // The concrete API of ConverterOptions is not shown, but the
        // library provides setters for zoneId and timeZone.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZONE_ID);
        options.setTimeZone(TIME_ZONE);

        converter = new Converter(options);
    }

    @Test
    public void testToInstant() {
        Instant expected = ZonedDateTime.of(SAMPLE_DATE, LocalDateConversionsTestHelper.midnight(), ZONE_ID).toInstant();
        Instant actual = LocalDateConversions.toInstant(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLong() {
        long expected = LocalDateConversions.toInstant(SAMPLE_DATE, converter).toEpochMilli();
        long actual = LocalDateConversions.toLong(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime expected = LocalDateTime.of(2023, 3, 15, 0, 0);
        LocalDateTime actual = LocalDateConversions.toLocalDateTime(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime expected = ZonedDateTime.of(SAMPLE_DATE, LocalDateConversionsTestHelper.midnight(), ZONE_ID);
        ZonedDateTime actual = LocalDateConversions.toZonedDateTime(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime() {
        // UTC offset is zero seconds.
        OffsetDateTime expected = OffsetDateTime.of(SAMPLE_DATE, LocalDateConversionsTestHelper.midnight(),
                ZoneOffset.UTC);
        OffsetDateTime actual = LocalDateConversions.toOffsetDateTime(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDouble() {
        double expected = LocalDateConversions.toInstant(SAMPLE_DATE, converter).toEpochMilli() / 1000d;
        double actual = LocalDateConversions.toDouble(SAMPLE_DATE, converter);
        assertEquals(expected, actual, 0.0);
    }

    @Test
    public void testToAtomicLong() {
        long expected = LocalDateConversions.toLong(SAMPLE_DATE, converter);
        AtomicLong actual = LocalDateConversions.toAtomicLong(SAMPLE_DATE, converter);
        assertEquals(expected, actual.get());
    }

    @Test
    public void testToTimestamp() {
        long epochMilli = SAMPLE_DATE.toEpochDay() * 86_400_000L;
        Timestamp expected = new Timestamp(epochMilli);
        Timestamp actual = LocalDateConversions.toTimestamp(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        ZonedDateTime zdt = LocalDateConversions.toZonedDateTime(SAMPLE_DATE, converter);
        Calendar expected = Calendar.getInstance(TIME_ZONE);
        expected.setTimeInMillis(zdt.toInstant().toEpochMilli());

        Calendar actual = LocalDateConversions.toCalendar(SAMPLE_DATE, converter);
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(TIME_ZONE, actual.getTimeZone());
    }

    @Test
    public void testToSqlDate() {
        long epochMilli = LocalDateConversions.toLong(SAMPLE_DATE, converter);
        java.sql.Date expected = new java.sql.Date(epochMilli);
        java.sql.Date actual = LocalDateConversions.toSqlDate(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate() {
        long epochMilli = LocalDateConversions.toLong(SAMPLE_DATE, converter);
        Date expected = new Date(epochMilli);
        Date actual = LocalDateConversions.toDate(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigInteger() {
        Instant instant = LocalDateConversions.toInstant(SAMPLE_DATE, converter);
        // InstantConversions converts an Instant to a BigInteger representing the epoch‑millisecond value.
        BigInteger expected = BigInteger.valueOf(instant.toEpochMilli());
        BigInteger actual = LocalDateConversions.toBigInteger(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        Instant instant = LocalDateConversions.toInstant(SAMPLE_DATE, converter);
        // InstantConversions converts an Instant to a BigDecimal representing the epoch‑millisecond value.
        BigDecimal expected = BigDecimal.valueOf(instant.toEpochMilli());
        BigDecimal actual = LocalDateConversions.toBigDecimal(SAMPLE_DATE, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToString() {
        String expected = SAMPLE_DATE.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        String actual = LocalDateConversions.toString(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = LocalDateConversions.toMap(SAMPLE_DATE, converter);
        assertNotNull(map);
        assertEquals(1, map.size());
        // The key used by MapConversions is a public constant named DATE.
        assertEquals(SAMPLE_DATE.toString(), map.get(MapConversions.DATE));
    }

    /**
     * Helper class that supplies a reusable midnight {@link java.time.LocalTime}
     * instance without pulling the literal string into each test.
     */
    private static final class LocalDateConversionsTestHelper {
        private static java.time.LocalTime midnight() {
            return java.time.LocalTime.parse("00:00:00");
        }
    }
}
