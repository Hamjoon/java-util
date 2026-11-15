package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link BigIntegerConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic happy‑path functionality for every conversion method.</li>
 *   <li>Edge‑case values (zero, maximum‑day nanoseconds, large timestamps, etc.).</li>
 *   <li>Exception handling for illegal arguments (negative numbers, out‑of‑range times).</li>
 * </ul>
 *
 * All tests use real objects – no mocking – and rely only on the public API
 * of {@link Converter} and the classes referenced in {@code BigIntegerConversions}.
 */
public class BigIntegerConversionsTest {

    private Converter converter;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // The ConverterOptions class is part of the library; we rely on its default
        // constructor which supplies the system default time‑zone and zone‑id.
        converter = new Converter(new ConverterOptions());
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- toBigDecimal ----------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBigDecimal_basic() {
        BigInteger bi = new BigInteger("12345678901234567890");
        BigDecimal result = BigIntegerConversions.toBigDecimal(bi, converter);
        assertEquals(new BigDecimal(bi), result);
    }

    /* ---------------------------------------------------------------------- */
    /* ---------------------------- toUUID --------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToUUID_zero() {
        BigInteger zero = BigInteger.ZERO;
        UUID uuid = BigIntegerConversions.toUUID(zero, converter);
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), uuid);
    }

    @Test
    public void testToUUID_knownValue() {
        // 0x12345678_90abcdef_12345678_90abcdef  (32 hex digits = 128 bits)
        BigInteger value = new BigInteger("1234567890abcdef1234567890abcdef", 16);
        UUID uuid = BigIntegerConversions.toUUID(value, converter);
        assertEquals(UUID.fromString("12345678-90ab-cdef-1234-567890abcdef"), uuid);
    }

    @Test
    public void testToUUID_negativeThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot convert a negative number");
        BigInteger negative = new BigInteger("-1");
        BigIntegerConversions.toUUID(negative, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- toInstant -------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToInstant_basic() {
        // 5 seconds + 250,000,123 nanoseconds
        BigInteger nanos = BigInteger.valueOf(5).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(250_000_123));
        Instant instant = BigIntegerConversions.toInstant(nanos, converter);
        assertEquals(Instant.ofEpochSecond(5, 250_000_123), instant);
    }

    @Test
    public void testToInstant_zero() {
        Instant instant = BigIntegerConversions.toInstant(BigInteger.ZERO, converter);
        assertEquals(Instant.EPOCH, instant);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- toDuration -------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToDuration_basic() {
        // 2 seconds + 123,456,789 nanoseconds
        BigInteger nanos = BigInteger.valueOf(2).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(123_456_789));
        Duration duration = BigIntegerConversions.toDuration(nanos, converter);
        assertEquals(Duration.ofSeconds(2, 123_456_789), duration);
    }

    /* ---------------------------------------------------------------------- */
    /* ---------------------------- toDate ---------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToDate_basic() {
        // 1,600,000,000 seconds (approx 2020‑09‑13T12:26:40Z)
        BigInteger nanos = BigInteger.valueOf(1_600_000_000L).multiply(BigIntegerConversions.BILLION);
        Date date = BigIntegerConversions.toDate(nanos, converter);
        assertEquals(Date.from(Instant.ofEpochSecond(1_600_000_000L)), date);
    }

    /* ---------------------------------------------------------------------- */
    /* -------------------------- toSqlDate --------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToSqlDate_basic() {
        // 2021‑01‑01T00:00:00Z = 1609459200 seconds
        BigInteger nanos = BigInteger.valueOf(1_609_459_200L).multiply(BigIntegerConversions.BILLION);
        java.sql.Date sqlDate = BigIntegerConversions.toSqlDate(nanos, converter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2021, 1, 1)), sqlDate);
    }

    /* ---------------------------------------------------------------------- */
    /* -------------------------- toTimestamp -------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToTimestamp_basic() {
        // 2022‑02‑22T22:22:22.123456789Z
        Instant instant = Instant.parse("2022-02-22T22:22:22.123456789Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        Timestamp ts = BigIntegerConversions.toTimestamp(nanos, converter);
        assertEquals(Timestamp.from(instant), ts);
    }

    /* ---------------------------------------------------------------------- */
    /* -------------------------- toCalendar --------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCalendar_utc() {
        // Force UTC to make the test deterministic
        TimeZone utc = TimeZone.getTimeZone("UTC");
        Converter utcConverter = new Converter(new ConverterOptions() {
            @Override public TimeZone getTimeZone() { return utc; }
            @Override public ZoneId getZoneId() { return utc.toZoneId(); }
        });

        // 2023‑03‑15T12:34:56.789Z  -> epoch millis = 1678889696789
        Instant instant = Instant.parse("2023-03-15T12:34:56.789Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        Calendar cal = BigIntegerConversions.toCalendar(nanos, utcConverter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(instant.toEpochMilli(), cal.getTimeInMillis());
    }

    /* ---------------------------------------------------------------------- */
    /* ------------------------ toZonedDateTime ------------------------------ */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToZonedDateTime_basic() {
        Instant instant = Instant.parse("2024-01-01T00:00:00Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION);
        ZonedDateTime zdt = BigIntegerConversions.toZonedDateTime(nanos, converter);
        ZoneId expectedZone = converter.getOptions().getZoneId();
        assertEquals(ZonedDateTime.ofInstant(instant, expectedZone), zdt);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- toLocalTime ------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToLocalTime_midnight() {
        BigInteger nanos = BigInteger.ZERO;
        LocalTime lt = BigIntegerConversions.toLocalTime(nanos, converter);
        assertEquals(LocalTime.MIDNIGHT, lt);
    }

    @Test
    public void testToLocalTime_endOfDay() {
        // 23:59:59.999999999 = 86_399_999_999_999 nanoseconds
        BigInteger nanos = BigInteger.valueOf(86_399_999_999_999L);
        LocalTime lt = BigIntegerConversions.toLocalTime(nanos, converter);
        assertEquals(LocalTime.of(23, 59, 59, 999_999_999), lt);
    }

    @Test
    public void testToLocalTime_negativeThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must be >= 0 && <= 86399999999999");
        BigInteger negative = new BigInteger("-1");
        BigIntegerConversions.toLocalTime(negative, converter);
    }

    @Test
    public void testToLocalTime_tooLargeThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must be >= 0 && <= 86399999999999");
        // One nanosecond beyond the last nanosecond of the day
        BigInteger tooLarge = BigInteger.valueOf(86_400_000_000_000L);
        BigIntegerConversions.toLocalTime(tooLarge, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* -------------------------- toLocalDate -------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToLocalDate_basic() {
        // 2025‑12‑31T00:00:00Z
        Instant instant = Instant.parse("2025-12-31T00:00:00Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION);
        LocalDate ld = BigIntegerConversions.toLocalDate(nanos, converter);
        assertEquals(LocalDate.of(2025, 12, 31), ld);
    }

    /* ---------------------------------------------------------------------- */
    /* ----------------------- toLocalDateTime ------------------------------ */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToLocalDateTime_basic() {
        // 2026‑11‑13T15:45:30.250Z
        Instant instant = Instant.parse("2026-11-13T15:45:30.250Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        LocalDateTime ldt = BigIntegerConversions.toLocalDateTime(nanos, converter);
        ZoneId zone = converter.getOptions().getZoneId();
        assertEquals(LocalDateTime.ofInstant(instant, zone), ldt);
    }

    /* ---------------------------------------------------------------------- */
    /* ----------------------- toOffsetDateTime ----------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToOffsetDateTime_basic() {
        // 2027‑07‑04T10:20:30+02:00  (represented as an instant)
        Instant instant = Instant.parse("2027-07-04T08:20:30Z"); // UTC equivalent
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION);
        OffsetDateTime odt = BigIntegerConversions.toOffsetDateTime(nanos, converter);
        ZoneId zone = converter.getOptions().getZoneId();
        assertEquals(OffsetDateTime.ofInstant(instant, zone), odt);
    }

    /* ---------------------------------------------------------------------- */
    /* ---------------------------- Helper Methods --------------------------- */
    /* ---------------------------------------------------------------------- */

    /**
     * Provides a minimal {@link ConverterOptions} implementation that simply
     * returns the system default time‑zone and zone‑id.  This is sufficient for
     * the conversion methods which only need those two values.
     */
    private static final class ConverterOptions {
        private final TimeZone timeZone = TimeZone.getDefault();
        private final ZoneId zoneId = timeZone.toZoneId();

        public TimeZone getTimeZone() {
            return timeZone;
        }

        public ZoneId getZoneId() {
            return zoneId;
        }
    }
}
