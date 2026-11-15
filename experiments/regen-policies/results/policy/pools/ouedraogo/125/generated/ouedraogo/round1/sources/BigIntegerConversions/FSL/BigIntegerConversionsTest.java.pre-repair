package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import org.junit.Test;

/**
 * Unit tests for {@link BigIntegerConversions}.  All public static methods are exercised,
 * including normal, edge‑case and error conditions.
 */
public class BigIntegerConversionsTest {

    /** Helper that creates a {@link Converter} with default options (system time‑zone). */
    private static Converter newDefaultConverter() {
        // The library provides a public constructor that accepts a ConverterOptions instance.
        // A no‑arg constructor for ConverterOptions exists in the library, giving the
        // default system time‑zone and zone‑id.  If the library changes this constructor,
        // the test will need to be updated accordingly.
        return new Converter(new ConverterOptions());
    }

    /*-----------------------------------------------------*
     *                toBigDecimal tests                    *
     *-----------------------------------------------------*/

    @Test
    public void testToBigDecimal_simpleValues() {
        Converter converter = newDefaultConverter();

        assertEquals(BigDecimal.valueOf(0), BigIntegerConversions.toBigDecimal(BigInteger.ZERO, converter));
        assertEquals(BigDecimal.valueOf(42), BigIntegerConversions.toBigDecimal(BigInteger.valueOf(42), converter));
        assertEquals(new BigDecimal("12345678901234567890"),
                     BigIntegerConversions.toBigDecimal(new BigInteger("12345678901234567890"), converter));
    }

    /*-----------------------------------------------------*
     *                toUUID tests                          *
     *-----------------------------------------------------*/

    @Test
    public void testToUUID_zeroValue() {
        Converter converter = newDefaultConverter();

        UUID expected = new UUID(0L, 0L);
        UUID actual = BigIntegerConversions.toUUID(BigInteger.ZERO, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToUUID_nonZeroValue() {
        Converter converter = newDefaultConverter();

        // Arbitrary positive 128‑bit value (fits into unsigned 128‑bit range)
        BigInteger bi = new BigInteger("123456789012345678901234567890123456", 10);

        // Compute expected UUID using the same algorithm as the production code
        String hex = bi.toString(16);
        while (hex.length() < 32) {
            hex = "0" + hex;
        }
        String high = hex.substring(0, 16);
        String low  = hex.substring(16, 32);
        String uuidString = high.substring(0, 8) + "-" +
                            high.substring(8, 12) + "-" +
                            high.substring(12, 16) + "-" +
                            low.substring(0, 4) + "-" +
                            low.substring(4, 16);
        UUID expected = UUID.fromString(uuidString);

        UUID actual = BigIntegerConversions.toUUID(bi, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUUID_negativeNumberThrows() {
        Converter converter = newDefaultConverter();

        BigInteger negative = BigInteger.valueOf(-1);
        BigIntegerConversions.toUUID(negative, converter);
    }

    /*-----------------------------------------------------*
     *                toInstant tests                      *
     *-----------------------------------------------------*/

    @Test
    public void testToInstant_zero() {
        Converter converter = newDefaultConverter();

        Instant instant = BigIntegerConversions.toInstant(BigInteger.ZERO, converter);
        assertEquals(Instant.EPOCH, instant);
    }

    @Test
    public void testToInstant_secondsAndNanos() {
        Converter converter = newDefaultConverter();

        // 1 second + 500,000,123 nanoseconds = 1_500_000_123 ns
        BigInteger nanos = BigInteger.valueOf(1_500_000_123L);
        Instant instant = BigIntegerConversions.toInstant(nanos, converter);

        assertEquals(1L, instant.getEpochSecond());
        assertEquals(500_000_123, instant.getNano());
    }

    @Test
    public void testToInstant_largeValueBeyondIntRange() {
        Converter converter = newDefaultConverter();

        // 3,000,000,000 seconds (≈95 years) + 250 nanoseconds
        BigInteger nanos = BigInteger.valueOf(3_000_000_000L).multiply(BigIntegerConversions.BILLION)
                               .add(BigInteger.valueOf(250));
        Instant instant = BigIntegerConversions.toInstant(nanos, converter);

        assertEquals(3_000_000_000L, instant.getEpochSecond());
        assertEquals(250, instant.getNano());
    }

    /*-----------------------------------------------------*
     *                toDuration tests                     *
     *-----------------------------------------------------*/

    @Test
    public void testToDuration_simple() {
        Converter converter = newDefaultConverter();

        // 2 seconds + 123,456,789 nanoseconds
        BigInteger nanos = BigInteger.valueOf(2).multiply(BigIntegerConversions.BILLION)
                               .add(BigInteger.valueOf(123_456_789));
        Duration duration = BigIntegerConversions.toDuration(nanos, converter);

        assertEquals(2L, duration.getSeconds());
        assertEquals(123_456_789, duration.getNano());
    }

    /*-----------------------------------------------------*
     *                toDate / toSqlDate / toTimestamp    *
     *-----------------------------------------------------*/

    @Test
    public void testToDate_andRelatedConversions() {
        Converter converter = newDefaultConverter();

        // 5 seconds after epoch = 5_000_000_000 ns
        BigInteger nanos = BigInteger.valueOf(5_000_000_000L);
        Instant expectedInstant = Instant.ofEpochSecond(5);

        Date date = BigIntegerConversions.toDate(nanos, converter);
        assertEquals(Date.from(expectedInstant), date);

        java.sql.Date sqlDate = BigIntegerConversions.toSqlDate(nanos, converter);
        // java.sql.Date truncates the time‑part, but because we are exactly on a whole second,
        // the value should be the same as the epoch day count for 5 seconds.
        assertEquals(new java.sql.Date(expectedInstant.toEpochMilli()), sqlDate);

        Timestamp ts = BigIntegerConversions.toTimestamp(nanos, converter);
        assertEquals(Timestamp.from(expectedInstant), ts);
    }

    /*-----------------------------------------------------*
     *                toCalendar tests                     *
     *-----------------------------------------------------*/

    @Test
    public void testToCalendar_usesProvidedTimeZone() {
        // Use a fixed, non‑UTC time‑zone to make the result deterministic.
        TimeZone utcMinusThree = TimeZone.getTimeZone("GMT-03:00");
        ConverterOptions options = new ConverterOptions();
        options.setTimeZone(utcMinusThree);
        Converter converter = new Converter(options);

        // 10 seconds = 10_000_000_000 ns
        BigInteger nanos = BigInteger.valueOf(10_000_000_000L);
        Calendar cal = BigIntegerConversions.toCalendar(nanos, converter);

        // Calendar should reflect epoch + 10 seconds in the supplied time‑zone.
        assertEquals(utcMinusThree, cal.getTimeZone());
        assertEquals(10_000L, cal.getTimeInMillis()); // 10 seconds -> 10,000 ms
    }

    /*-----------------------------------------------------*
     *                toZonedDateTime tests                *
     *-----------------------------------------------------*/

    @Test
    public void testToZonedDateTime_defaultZone() {
        Converter converter = newDefaultConverter();

        // 1_234_567_890_123 nanoseconds = 1 second + 234,567,890 ns
        BigInteger nanos = BigInteger.valueOf(1_234_567_890_123L);
        Instant instant = Instant.ofEpochSecond(1, 234_567_890);
        ZonedDateTime expected = instant.atZone(converter.getOptions().getZoneId());

        ZonedDateTime actual = BigIntegerConversions.toZonedDateTime(nanos, converter);
        assertEquals(expected, actual);
    }

    /*-----------------------------------------------------*
     *                toLocalDate / toLocalDateTime tests  *
     *-----------------------------------------------------*/

    @Test
    public void testToLocalDate_andLocalDateTime() {
        Converter converter = newDefaultConverter();

        // 2022‑03‑15T12:34:56.789Z  -> epoch nanoseconds
        ZonedDateTime zdt = ZonedDateTime.of(
                2022, 3, 15, 12, 34, 56, 789_000_000,
                ZoneOffset.UTC);
        BigInteger nanos = BigInteger.valueOf(zdt.toInstant().getEpochSecond())
                                   .multiply(BigIntegerConversions.BILLION)
                                   .add(BigInteger.valueOf(zdt.toInstant().getNano()));

        LocalDate expectedDate = LocalDate.of(2022, 3, 15);
        LocalDateTime expectedDateTime = LocalDateTime.of(2022, 3, 15, 12, 34, 56, 789_000_000);

        assertEquals(expectedDate, BigIntegerConversions.toLocalDate(nanos, converter));
        assertEquals(expectedDateTime, BigIntegerConversions.toLocalDateTime(nanos, converter));
    }

    /*-----------------------------------------------------*
     *                toOffsetDateTime tests               *
     *-----------------------------------------------------*/

    @Test
    public void testToOffsetDateTime() {
        Converter converter = newDefaultConverter();

        // Same instant as previous test
        Instant instant = Instant.parse("2021-12-31T23:59:59.999999999Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond())
                                   .multiply(BigIntegerConversions.BILLION)
                                   .add(BigInteger.valueOf(instant.getNano()));

        OffsetDateTime expected = instant.atOffset(converter.getOptions().getZoneId().getRules().getOffset(instant));
        assertEquals(expected, BigIntegerConversions.toOffsetDateTime(nanos, converter));
    }

    /*-----------------------------------------------------*
     *                toLocalTime tests                    *
     *-----------------------------------------------------*/

    @Test
    public void testToLocalTime_midnightAndLastMoment() {
        Converter converter = newDefaultConverter();

        // Midnight (0 nanos)
        assertEquals(LocalTime.MIDNIGHT,
                BigIntegerConversions.toLocalTime(BigInteger.ZERO, converter));

        // 23:59:59.999999999 = 86_399_999_999_999 nanoseconds
        BigInteger maxNanos = BigInteger.valueOf(86_399_999_999_999L);
        LocalTime expected = LocalTime.of(23, 59, 59, 999_999_999);
        assertEquals(expected,
                BigIntegerConversions.toLocalTime(maxNanos, converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_negativeValueThrows() {
        Converter converter = newDefaultConverter();

        BigInteger negative = BigInteger.valueOf(-1);
        BigIntegerConversions.toLocalTime(negative, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_tooLargeValueThrows() {
        Converter converter = newDefaultConverter();

        // One nanosecond beyond the maximum representable LocalTime
        BigInteger tooLarge = BigInteger.valueOf(86_400_000_000_000L);
        BigIntegerConversions.toLocalTime(tooLarge, converter);
    }
}
