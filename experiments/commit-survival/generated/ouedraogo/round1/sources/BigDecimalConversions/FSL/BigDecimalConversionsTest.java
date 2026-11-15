package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link BigDecimalConversions}.
 * <p>
 * The tests exercise every public‑static conversion method, checking
 * normal, edge‑case and error‑condition behaviour.  A single {@link Converter}
 * instance with default options is reused for all tests.
 */
public class BigDecimalConversionsTest {

    /** Converter with default options (system default time‑zone & zone‑id). */
    private static Converter converter;

    @BeforeClass
    public static void setUpConverter() {
        // The library provides a no‑arg constructor for ConverterOptions that
        // yields the JVM default TimeZone and ZoneId.  Using it keeps the test
        // independent of external configuration.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /* -------------------------------------------------
     *  toCalendar()
     * ------------------------------------------------- */
    @Test
    public void testToCalendar_WholeAndFractionalSeconds() {
        BigDecimal seconds = new BigDecimal("2.75"); // 2 seconds + 750 ms
        Calendar cal = BigDecimalConversions.toCalendar(seconds, converter);
        assertEquals("Milliseconds must be seconds * 1000", 2750L, cal.getTimeInMillis());
    }

    @Test
    public void testToCalendar_NegativeSeconds() {
        BigDecimal seconds = new BigDecimal("-1.2"); // -1.2 s = -1200 ms
        Calendar cal = BigDecimalConversions.toCalendar(seconds, converter);
        assertEquals(-1200L, cal.getTimeInMillis());
    }

    /* -------------------------------------------------
     *  toInstant()
     * ------------------------------------------------- */
    @Test
    public void testToInstant_IntegralAndFractional() {
        BigDecimal seconds = new BigDecimal("123.456789012");
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        assertEquals(123L, instant.getEpochSecond());
        assertEquals(456_789_012L, instant.getNano());
    }

    @Test
    public void testToInstant_NegativeFraction() {
        BigDecimal seconds = new BigDecimal("-0.5"); // -0.5 s = -500_000_000 ns
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        // Instant normalises the negative nanos into the epoch second field
        assertEquals(-1L, instant.getEpochSecond());
        assertEquals(500_000_000L, instant.getNano());
    }

    /* -------------------------------------------------
     *  toDuration()
     * ------------------------------------------------- */
    @Test
    public void testToDuration_StandardValue() {
        BigDecimal seconds = new BigDecimal("10.001002003");
        Duration dur = BigDecimalConversions.toDuration(seconds, converter);
        assertEquals(10L, dur.getSeconds());
        assertEquals(1_002_003L, dur.getNano());
    }

    @Test
    public void testToDuration_NegativeValue() {
        BigDecimal seconds = new BigDecimal("-5.250");
        Duration dur = BigDecimalConversions.toDuration(seconds, converter);
        // Duration handles negative values similarly to Instant
        assertEquals(-6L, dur.getSeconds());
        assertEquals(750_000_000L, dur.getNano()); // because -5.250 = -6 + 0.75
    }

    /* -------------------------------------------------
     *  toLocalTime()
     * ------------------------------------------------- */
    @Test
    public void testToLocalTime_ValidMidday() {
        // 12:34:56.123456789 = 45296.123456789 seconds
        BigDecimal seconds = new BigDecimal("45296.123456789");
        LocalTime time = BigDecimalConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.of(12, 34, 56, 123_456_789), time);
    }

    @Test
    public void testToLocalTime_EdgeOfDay() {
        // 23:59:59.999999999 = 86_399.999999999 seconds
        BigDecimal seconds = new BigDecimal("86399.999999999");
        LocalTime time = BigDecimalConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.of(23, 59, 59, 999_999_999), time);
    }

    @Test
    public void testToLocalTime_BelowZeroThrows() {
        BigDecimal seconds = new BigDecimal("-0.000000001");
        try {
            BigDecimalConversions.toLocalTime(seconds, converter);
            fail("Expected IllegalArgumentException for negative LocalTime value");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399.999999999"));
        }
    }

    @Test
    public void testToLocalTime_AboveMaxThrows() {
        BigDecimal seconds = new BigDecimal("86400"); // exactly one day
        try {
            BigDecimalConversions.toLocalTime(seconds, converter);
            fail("Expected IllegalArgumentException for out‑of‑range LocalTime value");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399.999999999"));
        }
    }

    /* -------------------------------------------------
     *  toZonedDateTime() and related helpers
     * ------------------------------------------------- */
    @Test
    public void testToZonedDateTime_ConsistentWithInstant() {
        BigDecimal seconds = new BigDecimal("1609459200.123456789"); // 2021‑01‑01T00:00:00.123456789Z
        ZonedDateTime zdt = BigDecimalConversions.toZonedDateTime(seconds, converter);
        Instant instant = Instant.ofEpochSecond(1609459200L, 123_456_789L);
        assertEquals(instant, zdt.toInstant());
        // Zone should be the one supplied by ConverterOptions (system default)
        assertEquals(converter.getOptions().getZoneId(), zdt.getZone());
    }

    @Test
    public void testToLocalDate_FromEpoch() {
        // 2020‑02‑29 (leap day)
        BigDecimal seconds = new BigDecimal("1582934400"); // 2020‑02‑29T00:00:00Z
        LocalDate date = BigDecimalConversions.toLocalDate(seconds, converter);
        assertEquals(LocalDate.of(2020, 2, 29), date);
    }

    @Test
    public void testToLocalDateTime_FromEpoch() {
        // 2022‑12‑31T23:59:59.999999999
        BigDecimal seconds = new BigDecimal("1672531199.999999999");
        LocalDateTime ldt = BigDecimalConversions.toLocalDateTime(seconds, converter);
        assertEquals(LocalDateTime.of(2022, 12, 31, 23, 59, 59, 999_999_999), ldt);
    }

    @Test
    public void testToOffsetDateTime_ZoneOffsetPreserved() {
        BigDecimal seconds = new BigDecimal("0"); // epoch
        OffsetDateTime odt = BigDecimalConversions.toOffsetDateTime(seconds, converter);
        // The offset should match the system default offset at epoch (usually UTC)
        ZoneOffset expected = converter.getOptions().getZoneId().getRules().getOffset(Instant.EPOCH);
        assertEquals(expected, odt.getOffset());
        assertEquals(Instant.EPOCH, odt.toInstant());
    }

    /* -------------------------------------------------
     *  toDate(), toSqlDate(), toTimestamp()
     * ------------------------------------------------- */
    @Test
    public void testToDate_MillisMatchInstant() {
        BigDecimal seconds = new BigDecimal("1622548800.5"); // 2021‑06‑01T12:00:00.5Z
        Date date = BigDecimalConversions.toDate(seconds, converter);
        Instant instant = Instant.ofEpochSecond(1622548800L, 500_000_000L);
        assertEquals(instant.toEpochMilli(), date.getTime());
    }

    @Test
    public void testToSqlDate_OnlyDateComponent() {
        // 2021‑03‑15T00:00:00Z
        BigDecimal seconds = new BigDecimal("1615766400");
        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(seconds, converter);
        // java.sql.Date normalises to midnight UTC of the same day
        assertEquals(1615766400L * 1000, sqlDate.getTime());
    }

    @Test
    public void testToTimestamp_PreservesNanos() {
        BigDecimal seconds = new BigDecimal("1000.000000123");
        Timestamp ts = BigDecimalConversions.toTimestamp(seconds, converter);
        assertEquals(1000_000L, ts.getTime()); // milliseconds part
        assertEquals(123, ts.getNanos());     // nanoseconds part
    }

    /* -------------------------------------------------
     *  toBigInteger()
     * ------------------------------------------------- */
    @Test
    public void testToBigInteger_TruncatesFraction() {
        BigDecimal bd = new BigDecimal("12345.987");
        BigInteger bi = BigDecimalConversions.toBigInteger(bd, converter);
        assertEquals(new BigInteger("12345"), bi);
    }

    @Test
    public void testToBigInteger_NegativeValue() {
        BigDecimal bd = new BigDecimal("-42.9");
        BigInteger bi = BigDecimalConversions.toBigInteger(bd, converter);
        assertEquals(new BigInteger("-42"), bi);
    }

    /* -------------------------------------------------
     *  toString()
     * ------------------------------------------------- */
    @Test
    public void testToString_StripTrailingZeros() {
        BigDecimal bd = new BigDecimal("123.45000");
        String s = BigDecimalConversions.toString(bd, converter);
        assertEquals("123.45", s);
    }

    @Test
    public void testToString_ScientificNotationConverted() {
        BigDecimal bd = new BigDecimal("1E-7"); // 0.0000001
        String s = BigDecimalConversions.toString(bd, converter);
        assertEquals("0.0000001", s);
    }

    /* -------------------------------------------------
     *  toUUID()
     * ------------------------------------------------- */
    @Test
    public void testToUUID_KnownValue() {
        // Use a deterministic 128‑bit integer that maps to a UUID
        BigInteger bigInt = new BigInteger("123456789012345678901234567890123456");
        BigDecimal bd = new BigDecimal(bigInt);
        // Direct conversion via BigIntegerConversions (library helper)
        UUID expected = BigIntegerConversions.toUUID(bigInt, converter);
        UUID actual = BigDecimalConversions.toUUID(bd, converter);
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------
     *  secondsAndNanosToDouble()
     * ------------------------------------------------- */
    @Test
    public void testSecondsAndNanosToDouble_Basic() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(5L, 250_000_000L);
        assertEquals(new BigDecimal("5.250000000"), result);
    }

    @Test
    public void testSecondsAndNanosToDouble_NanosLessThanScale() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(0L, 1L);
        assertEquals(new BigDecimal("0.000000001"), result);
    }

    @Test
    public void testSecondsAndNanosToDouble_NegativeSeconds() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(-3L, 500_000_000L);
        // -3 + 0.5 = -2.5
        assertEquals(new BigDecimal("-2.5"), result);
    }
}
