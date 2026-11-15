package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import java.sql.Timestamp;
import java.sql.Date as SqlDate;

/**
 * JUnit‑4 test suite for {@code BigIntegerConversions}.
 *
 * The tests are placed in the same package as the class under test so that the
 * package‑private static methods are accessible.
 *
 * Each public (actually package‑private) conversion method is exercised with:
 * <ul>
 *   <li>a basic “happy‑path” test that verifies the expected result, and</li>
 *   <li>additional tests that cover edge‑cases and the documented exception
 *       behaviour.</li>
 * </ul>
 */
public class BigIntegerConversionsTest {

    // ------------------------------------------------------------------------
    // Helper – a Converter with default options (system zone / time‑zone)
    // ------------------------------------------------------------------------
    private static final Converter CONVERTER = new Converter(new ConverterOptions());

    // ------------------------------------------------------------------------
    // 1. toBigDecimal
    // ------------------------------------------------------------------------
    @Test
    public void testToBigDecimal_HappyPath() {
        BigInteger from = new BigInteger("12345678901234567890");
        BigDecimal expected = new BigDecimal(from);
        BigDecimal actual = BigIntegerConversions.toBigDecimal(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    // ------------------------------------------------------------------------
    // 2. toUUID
    // ------------------------------------------------------------------------
    @Test
    public void testToUUID_ZeroValue() {
        // 0 -> the all‑zero UUID
        BigInteger from = BigInteger.ZERO;
        UUID expected = new UUID(0L, 0L);
        UUID actual = BigIntegerConversions.toUUID(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToUUID_RandomPositiveValue() {
        // A known positive value (0x0123456789ABCDEF0123456789ABCDEF)
        BigInteger from = new BigInteger("0123456789ABCDEF0123456789ABCDEF", 16);
        // Build the expected UUID by hand – the same algorithm that the code uses
        String hex = String.format("%032x", from);
        String uuidString = hex.substring(0, 8) + "-" +
                            hex.substring(8, 12) + "-" +
                            hex.substring(12, 16) + "-" +
                            hex.substring(16, 20) + "-" +
                            hex.substring(20, 32);
        UUID expected = UUID.fromString(uuidString);
        UUID actual = BigIntegerConversions.toUUID(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUUID_NegativeValueThrows() {
        BigInteger from = BigInteger.valueOf(-1);
        BigIntegerConversions.toUUID(from, CONVERTER);
    }

    // ------------------------------------------------------------------------
    // 3. toDate
    // ------------------------------------------------------------------------
    @Test
    public void testToDate_EpochNanos() {
        BigInteger from = BigInteger.ZERO; // 0 nanoseconds since epoch
        Date expected = Date.from(Instant.EPOCH);
        Date actual = BigIntegerConversions.toDate(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToDate_SomeInstant() {
        Instant instant = Instant.parse("2023-03-25T12:34:56.123456789Z");
        BigInteger from = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        Date expected = Date.from(instant);
        Date actual = BigIntegerConversions.toDate(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    // ------------------------------------------------------------------------
    // 4. toSqlDate
    // ------------------------------------------------------------------------
    @Test
    public void testToSqlDate_EpochNanos() {
        BigInteger from = BigInteger.ZERO;
        java.sql.Date expected = java.sql.Date.valueOf(LocalDate.ofEpochDay(0));
        java.sql.Date actual = BigIntegerConversions.toSqlDate(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToSqlDate_TypicalDate() {
        LocalDate ld = LocalDate.of(2021, 12, 31);
        Instant instant = ld.atStartOfDay(ZoneId.systemDefault()).toInstant();
        BigInteger from = BigInteger.valueOf(instant.toEpochMilli()).multiply(BigIntegerConversions.MILLION);
        java.sql.Date expected = java.sql.Date.valueOf(ld);
        java.sql.Date actual = BigIntegerConversions.toSqlDate(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    // ------------------------------------------------------------------------
    // 5. toTimestamp
    // ------------------------------------------------------------------------
    @Test
    public void testToTimestamp_EpochNanos() {
        BigInteger from = BigInteger.ZERO;
        Timestamp expected = Timestamp.from(Instant.EPOCH);
        Timestamp actual = BigIntegerConversions.toTimestamp(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToTimestamp_RandomInstant() {
        Instant instant = Instant.parse("1999-12-31T23:59:59.999999999Z");
        BigInteger from = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        Timestamp expected = Timestamp.from(instant);
        Timestamp actual = BigIntegerConversions.toTimestamp(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    // ------------------------------------------------------------------------
    // 6. toCalendar
    // ------------------------------------------------------------------------
    @Test
    public void testToCalendar_EpochNanos() {
        BigInteger from = BigInteger.ZERO;
        Calendar cal = BigIntegerConversions.toCalendar(from, CONVERTER);
        Calendar expected = Calendar.getInstance(ConverterOptions.DEFAULT_TIME_ZONE);
        expected.setTimeInMillis(0L);
        Assert.assertEquals(expected.getTimeInMillis(), cal.getTimeInMillis());
        Assert.assertEquals(expected.getTimeZone(), cal.getTimeZone());
    }

    @Test
    public void testToCalendar_RandomInstant() {
        Instant instant = Instant.parse("2005-05-05T05:05:05.005Z");
        BigInteger from = BigInteger.valueOf(instant.toEpochMilli()).multiply(BigIntegerConversions.MILLION);
        Calendar cal = BigIntegerConversions.toCalendar(from, CONVERTER);
        Calendar expected = Calendar.getInstance(ConverterOptions.DEFAULT_TIME_ZONE);
        expected.setTimeInMillis(instant.toEpochMilli());
        Assert.assertEquals(expected.getTimeInMillis(), cal.getTimeInMillis());
    }

    // ------------------------------------------------------------------------
    // 7. toZonedDateTime
    // ------------------------------------------------------------------------
    @Test
    public void testToZonedDateTime_EpochNanos() {
        BigInteger from = BigInteger.ZERO;
        ZonedDateTime expected = Instant.EPOCH.atZone(ConverterOptions.DEFAULT_ZONE_ID);
        ZonedDateTime actual = BigIntegerConversions.toZonedDateTime(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    // ------------------------------------------------------------------------
    // 8. toLocalTime
    // ------------------------------------------------------------------------
    @Test
    public void testToLocalTime_Zero() {
        BigInteger from = BigInteger.ZERO;
        LocalTime expected = LocalTime.MIDNIGHT;
        LocalTime actual = BigIntegerConversions.toLocalTime(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime_Maximum() {
        // 86_399_999_999_999 nanoseconds = 23:59:59.999999999
        BigInteger from = BigInteger.valueOf(86_399_999_999_999L);
        LocalTime expected = LocalTime.of(23, 59, 59, 999_999_999);
        LocalTime actual = BigIntegerConversions.toLocalTime(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_NegativeThrows() {
        BigInteger from = BigInteger.valueOf(-1);
        BigIntegerConversions.toLocalTime(from, CONVERTER);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_TooLargeThrows() {
        // One nanosecond beyond the end of a day
        BigInteger from = BigInteger.valueOf(86_400_000_000_000L);
        BigIntegerConversions.toLocalTime(from, CONVERTER);
    }

    // ------------------------------------------------------------------------
    // 9. toLocalDate
    // ------------------------------------------------------------------------
    @Test
    public void testToLocalDate_EpochNanos() {
        BigInteger from = BigInteger.ZERO;
        LocalDate expected = LocalDate.ofEpochDay(0);
        LocalDate actual = BigIntegerConversions.toLocalDate(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDate_RandomDate() {
        LocalDate ld = LocalDate.of(2022, 2, 28);
        Instant instant = ld.atStartOfDay(ConverterOptions.DEFAULT_ZONE_ID).toInstant();
        BigInteger from = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        LocalDate actual = BigIntegerConversions.toLocalDate(from, CONVERTER);
        Assert.assertEquals(ld, actual);
    }

    // ------------------------------------------------------------------------
    // 10. toLocalDateTime
    // ------------------------------------------------------------------------
    @Test
    public void testToLocalDateTime_EpochNanos() {
        BigInteger from = BigInteger.ZERO;
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.EPOCH, ConverterOptions.DEFAULT_ZONE_ID);
        LocalDateTime actual = BigIntegerConversions.toLocalDateTime(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    // ------------------------------------------------------------------------
    // 11. toOffsetTime
    // ------------------------------------------------------------------------
    @Test
    public void testToOffsetTime_Zero() {
        BigInteger from = BigInteger.ZERO;
        OffsetTime expected = OffsetTime.ofInstant(Instant.EPOCH, ConverterOptions.DEFAULT_ZONE_ID);
        OffsetTime actual = BigIntegerConversions.toOffsetTime(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_NegativeThrows() {
        BigInteger from = BigInteger.valueOf(-100);
        BigIntegerConversions.toOffsetTime(from, CONVERTER);
    }

    // ------------------------------------------------------------------------
    // 12. toOffsetDateTime
    // ------------------------------------------------------------------------
    @Test
    public void testToOffsetDateTime_EpochNanos() {
        BigInteger from = BigInteger.ZERO;
        OffsetDateTime expected = OffsetDateTime.ofInstant(Instant.EPOCH, ConverterOptions.DEFAULT_ZONE_ID);
        OffsetDateTime actual = BigIntegerConversions.toOffsetDateTime(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    // ------------------------------------------------------------------------
    // 13. toInstant
    // ------------------------------------------------------------------------
    @Test
    public void testToInstant_EpochNanos() {
        BigInteger from = BigInteger.ZERO;
        Instant expected = Instant.EPOCH;
        Instant actual = BigIntegerConversions.toInstant(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToInstant_Random() {
        Instant instant = Instant.parse("2010-01-02T03:04:05.006007008Z");
        BigInteger from = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        Instant actual = BigIntegerConversions.toInstant(from, CONVERTER);
        Assert.assertEquals(instant, actual);
    }

    // ------------------------------------------------------------------------
    // 14. toDuration
    // ------------------------------------------------------------------------
    @Test
    public void testToDuration_EpochNanos() {
        BigInteger from = BigInteger.ZERO;
        Duration expected = Duration.ZERO;
        Duration actual = BigIntegerConversions.toDuration(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToDuration_Random() {
        Duration expected = Duration.ofDays(5).plusHours(4).plusMinutes(3).plusSeconds(2).plusNanos(1);
        long totalNanos = expected.getSeconds() * 1_000_000_000L + expected.getNano();
        BigInteger from = BigInteger.valueOf(totalNanos);
        Duration actual = BigIntegerConversions.toDuration(from, CONVERTER);
        Assert.assertEquals(expected, actual);
    }
}
