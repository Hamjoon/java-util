/*********************************************************************************************
 *  JUnit‑4 test suite for {@code com.cedarsoftware.util.convert.BigDecimalConversions}
 *
 *  The tests cover:
 *   • All static conversion methods (basic happy‑path verification)
 *   • Edge‑case handling and expected exceptions
 *   • Verification of the helper method {@code secondsAndNanosToDouble}
 *
 *  The test class resides in the same package as the class under test so that package‑private
 *  static methods are directly accessible without reflection or mocking.
 *
 *  NOTE:  The library’s {@code Converter} and {@code ConverterOptions} classes are part of the
 *         public API supplied with the source code snippet.  They are instantiated using their
 *         public constructors.  No external mocking frameworks are used – real objects are
 *         created as required.
 *********************************************************************************************/

package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import static org.junit.Assert.*;

public class BigDecimalConversionsTest {

    /** A Converter instance with default options (system default time‑zone). */
    private Converter converter;

    @Before
    public void setUp() {
        // The Converter class provides a public constructor that accepts ConverterOptions.
        // Using the default constructor of ConverterOptions gives us the platform defaults.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------------------------------
     *  Helper methods used by multiple tests
     * ---------------------------------------------------------------------- */

    /** Creates a {@code BigDecimal} representing the supplied seconds (including fraction). */
    private static BigDecimal bd(double seconds) {
        return BigDecimal.valueOf(seconds);
    }

    /** Returns a {@code BigDecimal} that exactly represents the supplied nanos as a fraction
     *  of a second (i.e. nanos / 1_000_000_000). */
    private static BigDecimal bdFromNanos(long nanos) {
        return BigDecimal.valueOf(nanos, 9); // scale 9 -> nanos / 10^9
    }

    /* -------------------------------------------------------------------------
     *  BASIC FUNCTIONALITY TESTS
     * ---------------------------------------------------------------------- */

    @Test
    public void testToCalendar() {
        // 123.456 seconds  -> 123456 milliseconds
        BigDecimal input = bd(123.456);
        Calendar cal = BigDecimalConversions.toCalendar(input, converter);
        assertEquals(TimeZone.getDefault(), cal.getTimeZone());
        assertEquals(123456L, cal.getTimeInMillis());
    }

    @Test
    public void testToInstant() {
        // 10.5 seconds  -> 10 seconds + 500_000_000 nanos
        BigDecimal input = bd(10.5);
        Instant instant = BigDecimalConversions.toInstant(input, converter);
        assertEquals(10L, instant.getEpochSecond());
        assertEquals(500_000_000L, instant.getNano());
    }

    @Test
    public void testToDuration() {
        // 2.75 seconds -> 2 seconds + 750_000_000 nanos
        BigDecimal input = bd(2.75);
        Duration duration = BigDecimalConversions.toDuration(input, converter);
        assertEquals(2L, duration.getSeconds());
        assertEquals(750_000_000L, duration.getNano());
    }

    @Test
    public void testToLocalTime() {
        // 01:02:03.123456789 -> 3723.123456789 seconds
        BigDecimal seconds = bd(1 * 3600 + 2 * 60 + 3 + 0.123456789);
        LocalTime lt = BigDecimalConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.of(1, 2, 3, 123_456_789), lt);
    }

    @Test
    public void testToLocalDate() {
        // Use a known epoch second that maps to a date in UTC (e.g., 2021‑03‑14)
        long epochSecond = LocalDate.of(2021, 3, 14).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        BigDecimal input = BigDecimal.valueOf(epochSecond);
        LocalDate ld = BigDecimalConversions.toLocalDate(input, converter);
        assertEquals(LocalDate.of(2021, 3, 14), ld);
    }

    @Test
    public void testToLocalDateTime() {
        // 2020‑12‑31T23:59:59.999999999
        long epochSecond = LocalDateTime.of(2020, 12, 31, 23, 59, 59).toEpochSecond(ZoneOffset.UTC);
        BigDecimal input = bdFromNanos(999_999_999L).add(BigDecimal.valueOf(epochSecond));
        LocalDateTime ldt = BigDecimalConversions.toLocalDateTime(input, converter);
        assertEquals(LocalDateTime.of(2020, 12, 31, 23, 59, 59, 999_999_999), ldt);
    }

    @Test
    public void testToOffsetTime() {
        // 12:34:56.789012345 (seconds since epoch = 12*3600+34*60+56.789012345)
        double totalSeconds = 12 * 3600 + 34 * 60 + 56.789012345;
        BigDecimal input = bd(totalSeconds);
        OffsetTime ot = BigDecimalConversions.toOffsetTime(input, converter);
        ZoneId zone = converter.getOptions().getZoneId();
        OffsetTime expected = OffsetTime.ofInstant(Instant.ofEpochSecond((long) totalSeconds,
                (long) ((totalSeconds - (long) totalSeconds) * 1_000_000_000)), zone);
        assertEquals(expected, ot);
    }

    @Test
    public void testToOffsetDateTime() {
        // 2022‑01‑01T10:20:30.123456789Z (epoch seconds = ...)
        Instant instant = Instant.parse("2022-01-01T10:20:30.123456789Z");
        BigDecimal input = bdFromNanos(123_456_789L).add(BigDecimal.valueOf(instant.getEpochSecond()));
        OffsetDateTime odt = BigDecimalConversions.toOffsetDateTime(input, converter);
        ZoneId zone = converter.getOptions().getZoneId();
        assertEquals(OffsetDateTime.ofInstant(instant, zone), odt);
    }

    @Test
    public void testToZonedDateTime() {
        Instant instant = Instant.parse("1999-12-31T23:59:59Z");
        BigDecimal input = bdFromNanos(0).add(BigDecimal.valueOf(instant.getEpochSecond()));
        ZonedDateTime zdt = BigDecimalConversions.toZonedDateTime(input, converter);
        ZoneId zone = converter.getOptions().getZoneId();
        assertEquals(ZonedDateTime.ofInstant(instant, zone), zdt);
    }

    @Test
    public void testToDate() {
        Instant instant = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        BigDecimal input = bdFromNanos(instant.getNano()).add(BigDecimal.valueOf(instant.getEpochSecond()));
        Date date = BigDecimalConversions.toDate(input, converter);
        assertEquals(Date.from(instant), date);
    }

    @Test
    public void testToSqlDate() {
        // 2023‑07‑04 (midnight UTC)
        LocalDate ld = LocalDate.of(2023, 7, 4);
        Instant instant = ld.atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal input = bdFromNanos(0).add(BigDecimal.valueOf(instant.getEpochSecond()));
        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(input, converter);
        assertEquals(java.sql.Date.valueOf(ld), sqlDate);
    }

    @Test
    public void testToTimestamp() {
        Instant instant = Instant.parse("2000-01-01T00:00:00.123456789Z");
        BigDecimal input = bdFromNanos(123_456_789L).add(BigDecimal.valueOf(instant.getEpochSecond()));
        Timestamp ts = BigDecimalConversions.toTimestamp(input, converter);
        assertEquals(Timestamp.from(instant), ts);
    }

    @Test
    public void testToBigInteger() {
        BigDecimal bd = new BigDecimal("12345678901234567890");
        BigInteger bi = BigDecimalConversions.toBigInteger(bd, converter);
        assertEquals(new BigInteger("12345678901234567890"), bi);
    }

    @Test
    public void testToString() {
        // stripTrailingZeros should remove the trailing zeros after the decimal point
        BigDecimal bd = new BigDecimal("123.45000");
        String s = BigDecimalConversions.toString(bd, converter);
        assertEquals("123.45", s);
    }

    @Test
    public void testToUUID() {
        // Create a known UUID, convert to a 128‑bit integer and then to BigDecimal
        UUID original = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        BigInteger bigInt = new BigInteger(original.toString().replace("-", ""), 16);
        BigDecimal bd = new BigDecimal(bigInt);
        UUID result = BigDecimalConversions.toUUID(bd, converter);
        assertEquals(original, result);
    }

    @Test
    public void testSecondsAndNanosToDouble() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(5L, 250_000_000L);
        assertEquals(new BigDecimal("5.250000000"), result);
    }

    /* -------------------------------------------------------------------------
     *  EDGE CASES & EXCEPTION TESTS
     * ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_NegativeValue() {
        BigDecimal negative = bd(-1);
        BigDecimalConversions.toLocalTime(negative, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_TooLargeValue() {
        // 24 hours = 86400 seconds, which is out of range (max 86399.999999999)
        BigDecimal tooLarge = bd(86400);
        BigDecimalConversions.toLocalTime(tooLarge, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_NegativeValue() {
        BigDecimal negative = bd(-0.1);
        BigDecimalConversions.toOffsetTime(negative, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_TooLargeValue() {
        // 24:00:00 is invalid for OffsetTime
        BigDecimal tooLarge = bd(86400);
        BigDecimalConversions.toOffsetTime(tooLarge, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullInput() {
        BigDecimalConversions.toInstant(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullInput() {
        BigDecimalConversions.toCalendar(null, converter);
    }

    @Test
    public void testToString_ZeroScale() {
        BigDecimal bd = new BigDecimal("0.0000");
        String s = BigDecimalConversions.toString(bd, converter);
        // stripTrailingZeros turns "0.0000" into "0"
        assertEquals("0", s);
    }

    @Test
    public void testSecondsAndNanosToDouble_NanosZero() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(42L, 0L);
        assertEquals(new BigDecimal("42.000000000"), result);
    }

    @Test
    public void testSecondsAndNanosToDouble_MaxNanos() {
        // 1 second + 999,999,999 nanos = just under 2 seconds
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(1L, 999_999_999L);
        assertEquals(new BigDecimal("1.999999999"), result);
    }
}
