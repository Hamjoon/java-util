package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.Date;
import java.sql.Timestamp;
import java.sql.Date as SqlDate;
import java.util.Calendar;
import java.time.*;
import java.util.UUID;

/**
 * JUnit 4 test suite for {@link BigIntegerConversions}.
 * The tests cover normal operation, edge‑cases and expected exceptions.
 */
public class BigIntegerConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The library provides a no‑arg constructor for ConverterOptions that
        // initialises the default time‑zone (system default) and zone‑id.
        // Using it gives deterministic behaviour for the conversion methods.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------------------
     *  List of public (package‑private static) methods in BigIntegerConversions
     * -------------------------------------------------------------
     *
     * static BigDecimal toBigDecimal(Object from, Converter converter)
     * static UUID       toUUID(Object from, Converter converter)
     * static Date       toDate(Object from, Converter converter)
     * static java.sql.Date toSqlDate(Object from, Converter converter)
     * static Timestamp  toTimestamp(Object from, Converter converter)
     * static Calendar   toCalendar(Object from, Converter converter)
     * static ZonedDateTime toZonedDateTime(Object from, Converter converter)
     * static LocalTime  toLocalTime(Object from, Converter converter)
     * static LocalDate  toLocalDate(Object from, Converter converter)
     * static LocalDateTime toLocalDateTime(Object from, Converter converter)
     * static OffsetDateTime toOffsetDateTime(Object from, Converter converter)
     * static Instant    toInstant(Object from, Converter converter)
     * static Duration  toDuration(Object from, Converter converter)
     */

    /* -------------------------------------------------------------
     *  Basic functional tests
     * ------------------------------------------------------------- */

    @Test
    public void testToBigDecimal() {
        BigInteger bi = new BigInteger("12345678901234567890");
        BigDecimal expected = new BigDecimal(bi);
        assertEquals(expected, BigIntegerConversions.toBigDecimal(bi, converter));
    }

    @Test
    public void testToUUID_PositiveNumber() {
        // BigInteger.ONE -> hex "1" -> padded 32‑char string -> UUID "00000000-0000-0000-0000-000000000001"
        UUID uuid = BigIntegerConversions.toUUID(BigInteger.ONE, converter);
        assertEquals("00000000-0000-0000-0000-000000000001", uuid.toString());
    }

    @Test
    public void testToUUID_LargeNumber() {
        // 0x1234567890abcdef1234567890abcdef (128‑bit value)
        BigInteger bi = new BigInteger("1234567890abcdef1234567890abcdef", 16);
        UUID uuid = BigIntegerConversions.toUUID(bi, converter);
        assertEquals("12345678-90ab-cdef-1234-567890abcdef", uuid.toString());
    }

    @Test
    public void testToUUID_NegativeNumberThrows() {
        try {
            BigIntegerConversions.toUUID(new BigInteger("-1"), converter);
            fail("Expected IllegalArgumentException for negative BigInteger");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Cannot convert a negative number"));
        }
    }

    @Test
    public void testToDate_EpochZero() {
        Date date = BigIntegerConversions.toDate(BigInteger.ZERO, converter);
        assertEquals(0L, date.getTime());
    }

    @Test
    public void testToSqlDate_EpochZero() {
        java.sql.Date sqlDate = BigIntegerConversions.toSqlDate(BigInteger.ZERO, converter);
        assertEquals(0L, sqlDate.getTime());
    }

    @Test
    public void testToTimestamp_EpochZero() {
        Timestamp ts = BigIntegerConversions.toTimestamp(BigInteger.ZERO, converter);
        assertEquals(0L, ts.getTime());
        assertEquals(0, ts.getNanos());
    }

    @Test
    public void testToCalendar_EpochNanos() {
        // 1_500_000_000 nanoseconds = 1.5 seconds = 1500 milliseconds
        BigInteger nanos = BigInteger.valueOf(1_500_000_000L);
        Calendar cal = BigIntegerConversions.toCalendar(nanos, converter);
        assertEquals(1500L, cal.getTimeInMillis());
    }

    @Test
    public void testToZonedDateTime_EpochZero() {
        ZonedDateTime zdt = BigIntegerConversions.toZonedDateTime(BigInteger.ZERO, converter);
        // Should be the epoch instant in the converter's default zone.
        Instant instant = Instant.ofEpochSecond(0, 0);
        assertEquals(instant, zdt.toInstant());
    }

    @Test
    public void testToLocalTime_Zero() {
        LocalTime lt = BigIntegerConversions.toLocalTime(BigInteger.ZERO, converter);
        assertEquals(LocalTime.MIDNIGHT, lt);
    }

    @Test
    public void testToLocalTime_MaxOfDay() {
        // 86_399_999_999_999 nanoseconds = 23:59:59.999999999
        BigInteger maxNanos = BigInteger.valueOf(86_399_999_999_999L);
        LocalTime lt = BigIntegerConversions.toLocalTime(maxNanos, converter);
        assertEquals(LocalTime.of(23, 59, 59, 999_999_999), lt);
    }

    @Test
    public void testToLocalDate_FromEpoch() {
        LocalDate ld = BigIntegerConversions.toLocalDate(BigInteger.ZERO, converter);
        // Epoch (1970‑01‑01) in the default zone
        assertEquals(LocalDate.of(1970, 1, 1), ld);
    }

    @Test
    public void testToLocalDateTime_FromEpoch() {
        LocalDateTime ldt = BigIntegerConversions.toLocalDateTime(BigInteger.ZERO, converter);
        assertEquals(LocalDateTime.ofInstant(Instant.EPOCH, converter.getOptions().getZoneId()), ldt);
    }

    @Test
    public void testToOffsetDateTime_FromEpoch() {
        OffsetDateTime odt = BigIntegerConversions.toOffsetDateTime(BigInteger.ZERO, converter);
        assertEquals(OffsetDateTime.ofInstant(Instant.EPOCH, converter.getOptions().getZoneId()), odt);
    }

    @Test
    public void testToInstant_SimpleValue() {
        // 3_000_000_001 nanoseconds = 3 seconds + 1 nanosecond
        BigInteger nanos = BigInteger.valueOf(3_000_000_001L);
        Instant instant = BigIntegerConversions.toInstant(nanos, converter);
        assertEquals(Instant.ofEpochSecond(3, 1), instant);
    }

    @Test
    public void testToDuration_SimpleValue() {
        // 5_000_000_000 nanoseconds = 5 seconds
        BigInteger nanos = BigInteger.valueOf(5_000_000_000L);
        Duration dur = BigIntegerConversions.toDuration(nanos, converter);
        assertEquals(Duration.ofSeconds(5), dur);
    }

    /* -------------------------------------------------------------
     *  Edge‑case / exception tests
     * ------------------------------------------------------------- */

    @Test
    public void testToLocalTime_NegativeThrows() {
        try {
            BigIntegerConversions.toLocalTime(BigInteger.valueOf(-1), converter);
            fail("Expected IllegalArgumentException for negative nanoseconds");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0"));
        }
    }

    @Test
    public void testToLocalTime_AboveDayRangeThrows() {
        // One nanosecond beyond the maximum of a day
        BigInteger tooBig = BigInteger.valueOf(86_400_000_000_000L);
        try {
            BigIntegerConversions.toLocalTime(tooBig, converter);
            fail("Expected IllegalArgumentException for nanoseconds > max of day");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399999999999"));
        }
    }

    @Test
    public void testToInstant_LargeSecondsOverflow() {
        // 2^63 nanoseconds ≈ 9.22e18 nanoseconds = 9,223,372,036,854,775,808 ns
        // This is larger than Long.MAX_VALUE seconds when divided by 1_000_000_000.
        BigInteger huge = new BigInteger("9223372036854775808"); // 2^63
        // The method uses longValue() on the seconds part, which will overflow.
        // Verify that the overflow produces the same result as Java's long overflow.
        Instant instant = BigIntegerConversions.toInstant(huge, converter);
        long expectedSeconds = huge.divide(BigIntegerConversions.BILLION).longValue(); // overflowed value
        int expectedNanos = huge.remainder(BigIntegerConversions.BILLION).intValue();
        assertEquals(Instant.ofEpochSecond(expectedSeconds, expectedNanos), instant);
    }

    @Test
    public void testToCalendar_NegativeNanos() {
        // Negative epoch nanoseconds are allowed – the Calendar should represent a
        // time before the epoch.
        BigInteger negativeNanos = BigInteger.valueOf(-1_000_000L); // -1 ms
        Calendar cal = BigIntegerConversions.toCalendar(negativeNanos, converter);
        assertEquals(-1L, cal.getTimeInMillis());
    }
}
