package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

/**
 * JUnit‑4 test suite for {@link BigIntegerConversions}.
 * <p>
 * The tests cover every public static conversion method in the class, exercising
 * normal operation as well as error conditions (e.g. negative numbers for UUID,
 * out‑of‑range values for {@code LocalTime}).
 * </p>
 */
public class BigIntegerConversionsTest {

    private Converter converter;               // the real Converter instance
    private ConverterOptions options;          // options used by the converter

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // Create ConverterOptions with deterministic time‑zone/zone‑id (UTC)
        options = new ConverterOptions();
        // Most implementations default to UTC; if setters are present we use them.
        try {
            // try to set the time‑zone via a conventional setter
            options.getClass().getMethod("setTimeZone", TimeZone.class)
                    .invoke(options, TimeZone.getTimeZone("UTC"));
            options.getClass().getMethod("setZoneId", ZoneId.class)
                    .invoke(options, ZoneId.of("UTC"));
        } catch (Exception ignored) {
            // If the options class does not expose setters we rely on its defaults.
        }
        converter = new Converter(options);
    }

    /* ------------------------------------------------------------
     *  toBigDecimal
     * ------------------------------------------------------------ */
    @Test
    public void testToBigDecimal() {
        BigInteger source = new BigInteger("12345678901234567890");
        BigDecimal result = BigIntegerConversions.toBigDecimal(source, converter);
        assertEquals(new BigDecimal(source), result);
    }

    /* ------------------------------------------------------------
     *  toUUID – normal, padded and negative cases
     * ------------------------------------------------------------ */
    @Test
    public void testToUUID_Zero() {
        UUID uuid = BigIntegerConversions.toUUID(BigInteger.ZERO, converter);
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), uuid);
    }

    @Test
    public void testToUUID_Padded() {
        // 0x1234567890abcdef1234567890abcdef (128‑bit value)
        BigInteger source = new BigInteger("1234567890abcdef1234567890abcdef", 16);
        UUID expected = UUID.fromString("12345678-90ab-cdef-1234-567890abcdef");
        UUID actual = BigIntegerConversions.toUUID(source, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToUUID_NegativeThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot convert a negative number");
        BigInteger negative = new BigInteger("-1");
        BigIntegerConversions.toUUID(negative, converter);
    }

    /* ------------------------------------------------------------
     *  toInstant and toDuration – nanoseconds handling
     * ------------------------------------------------------------ */
    @Test
    public void testToInstant() {
        // 5 seconds + 1 nanosecond
        BigInteger nanos = BigInteger.valueOf(5_000_000_001L);
        Instant instant = BigIntegerConversions.toInstant(nanos, converter);
        assertEquals(Instant.ofEpochSecond(5, 1), instant);
    }

    @Test
    public void testToDuration() {
        // 123 seconds + 456,789,012 nanoseconds
        BigInteger nanos = BigInteger.valueOf(123L).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(456_789_012L));
        Duration duration = BigIntegerConversions.toDuration(nanos, converter);
        assertEquals(Duration.ofSeconds(123, 456_789_012L), duration);
    }

    /* ------------------------------------------------------------
     *  toLocalTime – valid and out‑of‑range values
     * ------------------------------------------------------------ */
    @Test
    public void testToLocalTime_Valid() {
        LocalTime expected = LocalTime.of(13, 45, 30, 123_456_789);
        BigInteger nanos = BigInteger.valueOf(expected.toNanoOfDay());
        LocalTime actual = BigIntegerConversions.toLocalTime(nanos, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime_NegativeThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must be >= 0 && <= 86399999999999");
        BigInteger negative = BigInteger.valueOf(-1);
        BigIntegerConversions.toLocalTime(negative, converter);
    }

    @Test
    public void testToLocalTime_TooLargeThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must be >= 0 && <= 86399999999999");
        // exactly one day in nanoseconds (out of range)
        BigInteger oneDayNanos = BigInteger.valueOf(86_400_000_000_000L);
        BigIntegerConversions.toLocalTime(oneDayNanos, converter);
    }

    /* ------------------------------------------------------------
     *  toCalendar – conversion from nanoseconds to millis
     * ------------------------------------------------------------ */
    @Test
    public void testToCalendar() {
        // epoch nanoseconds = 1_600_000_000_000_000_000 (2020‑09‑13T12:26:40Z)
        BigInteger epochNanos = new BigInteger("1600000000000000000");
        Calendar cal = BigIntegerConversions.toCalendar(epochNanos, converter);
        long expectedMillis = epochNanos.divide(BigIntegerConversions.MILLION).longValue();
        assertEquals(expectedMillis, cal.getTimeInMillis());
        // Ensure the time‑zone is the one supplied by the options (UTC)
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    /* ------------------------------------------------------------
     *  toZonedDateTime, toLocalDate, toLocalDateTime, toOffsetDateTime
     * ------------------------------------------------------------ */
    @Test
    public void testZonedDateTimeAndRelatedConversions() {
        // Use the same instant as above (2020‑09‑13T12:26:40Z)
        BigInteger epochNanos = new BigInteger("1600000000000000000");
        ZonedDateTime zdt = BigIntegerConversions.toZonedDateTime(epochNanos, converter);
        assertEquals(Instant.ofEpochSecond(1600000000L), zdt.toInstant());
        assertEquals(ZoneId.of("UTC"), zdt.getZone());

        // LocalDate should be 2020‑09‑13
        LocalDate ld = BigIntegerConversions.toLocalDate(epochNanos, converter);
        assertEquals(LocalDate.of(2020, 9, 13), ld);

        // LocalDateTime should be 2020‑09‑13T12:26:40
        LocalDateTime ldt = BigIntegerConversions.toLocalDateTime(epochNanos, converter);
        assertEquals(LocalDateTime.of(2020, 9, 13, 12, 26, 40), ldt);

        // OffsetDateTime should have +00:00 offset
        OffsetDateTime odt = BigIntegerConversions.toOffsetDateTime(epochNanos, converter);
        assertEquals(OffsetDateTime.ofInstant(zdt.toInstant(), ZoneOffset.UTC), odt);
    }

    /* ------------------------------------------------------------
     *  toDate, toSqlDate, toTimestamp – instant‑based conversions
     * ------------------------------------------------------------ */
    @Test
    public void testDateConversions() {
        // 2021‑01‑01T00:00:00.123456789Z
        BigInteger epochNanos = BigInteger.valueOf(1_609_459_200L).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(123_456_789L));

        Instant instant = BigIntegerConversions.toInstant(epochNanos, converter);

        // java.util.Date (millis precision)
        Date date = BigIntegerConversions.toDate(epochNanos, converter);
        assertEquals(Date.from(instant), date);

        // java.sql.Date (date only, midnight UTC)
        java.sql.Date sqlDate = BigIntegerConversions.toSqlDate(epochNanos, converter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2021, 1, 1)), sqlDate);

        // java.sql.Timestamp (nanosecond precision)
        Timestamp ts = BigIntegerConversions.toTimestamp(epochNanos, converter);
        assertEquals(Timestamp.from(instant), ts);
        assertEquals(123_456_789, ts.getNanos());
    }

    /* ------------------------------------------------------------
     *  Helper – assertEquals overload for BigDecimal (to avoid scale issues)
     * ------------------------------------------------------------ */
    private static void assertEquals(BigDecimal expected, BigDecimal actual) {
        org.junit.Assert.assertTrue(
                "Expected " + expected + " but got " + actual,
                expected.compareTo(actual) == 0);
    }

    /* ------------------------------------------------------------
     *  Helper – generic assertEquals for other types
     * ------------------------------------------------------------ */
    private static void assertEquals(Object expected, Object actual) {
        org.junit.Assert.assertEquals(expected, actual);
    }
}
