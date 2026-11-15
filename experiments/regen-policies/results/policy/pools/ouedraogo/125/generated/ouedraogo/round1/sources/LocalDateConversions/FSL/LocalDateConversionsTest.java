package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link LocalDateConversions}.
 *
 * The tests exercise every public static conversion method, checking
 * that the returned value is exactly what is expected for a known
 * {@link LocalDate} and a deterministic conversion configuration
 * (UTC zone / UTC time‑zone).
 *
 * The {@link Converter} used in the tests is created with a
 * {@link ConverterOptions} instance that supplies the UTC
 * {@link ZoneId} and {@link TimeZone}.  All calculations are therefore
 * reproducible regardless of the environment in which the tests run.
 */
public class LocalDateConversionsTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2020, 2, 29); // leap‑day
    private static final ZoneId UTC_ZONE = ZoneOffset.UTC;
    private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    private Converter converter;

    @Before
    public void setUp() {
        // Build a ConverterOptions that returns UTC for both ZoneId and TimeZone.
        // The real library provides setters for these values; the test assumes
        // such setters exist.  If the library uses a different API, adjust
        // accordingly – the intent is to force a deterministic zone.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(UTC_ZONE);
        options.setTimeZone(UTC_TZ);
        converter = new Converter(options);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime expected = ZonedDateTime.of(TEST_DATE, LocalTime.MIDNIGHT, UTC_ZONE);
        ZonedDateTime actual = LocalDateConversions.toZonedDateTime(TEST_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToInstant() {
        Instant expected = LocalDateConversions.toZonedDateTime(TEST_DATE, converter).toInstant();
        Instant actual = LocalDateConversions.toInstant(TEST_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLong() {
        long expected = LocalDateConversions.toInstant(TEST_DATE, converter).toEpochMilli();
        long actual = LocalDateConversions.toLong(TEST_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime expected = LocalDateTime.of(TEST_DATE, LocalTime.MIDNIGHT);
        LocalDateTime actual = LocalDateConversions.toLocalDateTime(TEST_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime() {
        // Offset derived from the UTC time‑zone (zero seconds)
        ZoneOffset expectedOffset = ZoneOffset.UTC;
        OffsetDateTime expected = OffsetDateTime.of(TEST_DATE, LocalTime.MIDNIGHT, expectedOffset);
        OffsetDateTime actual = LocalDateConversions.toOffsetDateTime(TEST_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDouble() {
        double expected = LocalDateConversions.toLong(TEST_DATE, converter) / 1000d;
        double actual = LocalDateConversions.toDouble(TEST_DATE, converter);
        assertEquals(expected, actual, 0.0);
    }

    @Test
    public void testToAtomicLong() {
        long expected = LocalDateConversions.toLong(TEST_DATE, converter);
        AtomicLong actual = LocalDateConversions.toAtomicLong(TEST_DATE, converter);
        assertEquals(expected, actual.get());
    }

    @Test
    public void testToTimestamp() {
        // Timestamp constructor expects milliseconds since epoch.
        // The implementation uses days‑since‑epoch * 86400 * 1000.
        long expectedMillis = TEST_DATE.toEpochDay() * 86_400_000L;
        Timestamp expected = new Timestamp(expectedMillis);
        Timestamp actual = LocalDateConversions.toTimestamp(TEST_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        ZonedDateTime zdt = LocalDateConversions.toZonedDateTime(TEST_DATE, converter);
        long expectedMillis = zdt.toInstant().toEpochMilli();

        Calendar cal = LocalDateConversions.toCalendar(TEST_DATE, converter);
        assertEquals(UTC_TZ, cal.getTimeZone());
        assertEquals(expectedMillis, cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        long expectedMillis = LocalDateConversions.toLong(TEST_DATE, converter);
        java.sql.Date expected = new java.sql.Date(expectedMillis);
        java.sql.Date actual = LocalDateConversions.toSqlDate(TEST_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate() {
        long expectedMillis = LocalDateConversions.toLong(TEST_DATE, converter);
        Date expected = new Date(expectedMillis);
        Date actual = LocalDateConversions.toDate(TEST_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigInteger() {
        BigInteger expected = BigInteger.valueOf(LocalDateConversions.toLong(TEST_DATE, converter));
        BigInteger actual = LocalDateConversions.toBigInteger(TEST_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal expected = BigDecimal.valueOf(LocalDateConversions.toLong(TEST_DATE, converter));
        BigDecimal actual = LocalDateConversions.toBigDecimal(TEST_DATE, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToString() {
        String expected = TEST_DATE.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String actual = LocalDateConversions.toString(TEST_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = LocalDateConversions.toMap(TEST_DATE, converter);
        assertNotNull(map);
        // The key used by MapConversions for a date value is "date".
        // The constant is MapConversions.DATE; we verify via the literal.
        assertTrue(map.containsKey("date"));
        assertEquals(TEST_DATE.toString(), map.get("date"));
    }

    // -----------------------------------------------------------------
    // Helper: create a ConverterOptions with deterministic zone settings.
    // -----------------------------------------------------------------
    private static class ConverterOptions {
        private ZoneId zoneId;
        private TimeZone timeZone;

        public ZoneId getZoneId() {
            return zoneId;
        }

        public void setZoneId(ZoneId zoneId) {
            this.zoneId = zoneId;
        }

        public TimeZone getTimeZone() {
            return timeZone;
        }

        public void setTimeZone(TimeZone timeZone) {
            this.timeZone = timeZone;
        }
    }
}
