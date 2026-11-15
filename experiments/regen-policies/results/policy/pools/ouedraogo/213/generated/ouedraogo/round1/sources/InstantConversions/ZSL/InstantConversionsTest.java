/****************************************************************************************
 *  InstantConversionsTest
 *
 *  Comprehensive JUnit‑4 test suite for the package‑private static methods in
 *  com.cedarsoftware.util.convert.InstantConversions.
 *
 *  The test class resides in the same package so that it has access to the
 *  package‑private members.  All tests create real objects – no mocks or
 *  subclassing of library types is performed.
 *
 *  The tests cover:
 *   – toMap
 *   – toZonedDateTime
 *   – toOffsetDateTime
 *   – toLong
 *   – toDouble
 *   – toAtomicLong
 *   – toTimestamp
 *   – toSqlDate
 *   – toDate
 *   – toCalendar
 *   – toBigInteger
 *   – toBigDecimal
 *   – toLocalDateTime / toLocalDate / toLocalTime
 *
 *  Both “normal” instants (positive epoch) and edge‑case instants
 *  (epoch‑zero and a negative instant) are exercised, together with
 *  nanosecond‑level precision.
 *
 *  Author: ChatGPT (OpenAI) – generated for the purpose of this exercise.
 *****************************************************************************************/

package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.convert.MapConversions.INSTANT;
import static org.junit.Assert.*;

public class InstantConversionsTest {

    /** Converter used for all tests – constructed with the library’s default options. */
    private Converter converter;

    /** ZoneId and TimeZone derived from the ConverterOptions – cached for readability. */
    private ZoneId zoneId;
    private TimeZone timeZone;

    @Before
    public void setUp() {
        // The library provides a no‑arg ConverterOptions constructor that defaults to the
        // system default time‑zone.  This is sufficient for our tests because we always
        // compute the expected values using the same options.
        ConverterOptions options = new ConverterOptions(); // defaults to system zone
        converter = new Converter(options);
        zoneId = converter.getOptions().getZoneId();
        timeZone = converter.getOptions().getTimeZone();
    }

    /* ---------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* ---------------------------------------------------------------------- */

    /** Returns an Instant with the supplied epoch second and nano‑adjustment. */
    private static Instant instantOf(long epochSecond, int nano) {
        return Instant.ofEpochSecond(epochSecond, nano);
    }

    /** Expected BigInteger representation: (seconds * 1_000_000_000) + nanos */
    private static BigInteger expectedBigInteger(Instant i) {
        BigInteger seconds = BigInteger.valueOf(i.getEpochSecond());
        BigInteger nanos   = BigInteger.valueOf(i.getNano());
        return seconds.multiply(BigInteger.valueOf(1_000_000_000L)).add(nanos);
    }

    /** Expected double seconds (including fractional nanoseconds) */
    private static double expectedDouble(Instant i) {
        // Use the same library method that InstantConversions delegates to.
        return BigDecimalConversions
                .secondsAndNanosToDouble(i.getEpochSecond(), i.getNano())
                .doubleValue();
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for each conversion method                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMap() {
        Instant now = Instant.now();
        Map<String, Object> map = InstantConversions.toMap(now, converter);
        assertNotNull("Map must not be null", map);
        assertEquals("Map should contain exactly one entry", 1, map.size());
        assertTrue("Map must contain the INSTANT key", map.containsKey(INSTANT));
        assertEquals("Map value must be the ISO‑8601 string of the instant",
                now.toString(), map.get(INSTANT));
    }

    @Test
    public void testToZonedDateTime_positiveInstant() {
        Instant instant = Instant.parse("2022-03-14T15:09:26.123456789Z");
        ZonedDateTime expected = instant.atZone(zoneId);
        ZonedDateTime actual   = InstantConversions.toZonedDateTime(instant, converter);
        assertEquals("ZonedDateTime must match the instant at the converter's ZoneId",
                expected, actual);
    }

    @Test
    public void testToZonedDateTime_negativeInstant() {
        // Instant before the epoch
        Instant instant = Instant.parse("1965-12-31T23:59:59.999999999Z");
        ZonedDateTime expected = instant.atZone(zoneId);
        ZonedDateTime actual   = InstantConversions.toZonedDateTime(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime() {
        Instant instant = Instant.parse("2021-07-01T12:00:00Z");
        // Compute the expected offset using the TimeZone from the ConverterOptions
        int offsetMillis = timeZone.getOffset(instant.toEpochMilli());
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(offsetMillis / 1000);
        OffsetDateTime expected = instant.atOffset(expectedOffset);
        OffsetDateTime actual   = InstantConversions.toOffsetDateTime(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLong() {
        Instant instant = Instant.ofEpochMilli(1_600_000_123L);
        long expected = instant.toEpochMilli();
        long actual   = InstantConversions.toLong(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDouble_withNanoseconds() {
        Instant instant = instantOf(1_600_000_000L, 123_456_789); // 123.456789 ms
        double expected = expectedDouble(instant);
        double actual   = InstantConversions.toDouble(instant, converter);
        assertEquals(expected, actual, 0.0);
    }

    @Test
    public void testToAtomicLong() {
        Instant instant = Instant.ofEpochMilli(9_999_999L);
        AtomicLong atomic = InstantConversions.toAtomicLong(instant, converter);
        assertNotNull(atomic);
        assertEquals(instant.toEpochMilli(), atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Instant instant = Instant.parse("2020-02-29T23:59:59.999999999Z");
        Timestamp expected = Timestamp.from(instant);
        Timestamp actual   = InstantConversions.toTimestamp(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToSqlDate() {
        Instant instant = Instant.parse("2023-01-01T00:00:00Z");
        java.sql.Date expected = java.sql.Date.valueOf(
                instant.atZone(zoneId).toLocalDate()
        );
        java.sql.Date actual = InstantConversions.toSqlDate(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate() {
        Instant instant = Instant.ofEpochMilli(123_456_789L);
        Date expected = new Date(instant.toEpochMilli());
        Date actual   = InstantConversions.toDate(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        Instant instant = Instant.ofEpochMilli(555_555_555L);
        Calendar expected = CalendarConversions.create(instant.toEpochMilli(), converter);
        Calendar actual   = InstantConversions.toCalendar(instant, converter);
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTimeZone(), actual.getTimeZone());
    }

    @Test
    public void testToBigInteger() {
        Instant instant = instantOf(1_234_567_890L, 987_654_321);
        BigInteger expected = expectedBigInteger(instant);
        BigInteger actual   = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        Instant instant = instantOf(2_000_000_000L, 500_000_000);
        BigDecimal expected = BigDecimalConversions
                .secondsAndNanosToDouble(instant.getEpochSecond(), instant.getNano());
        BigDecimal actual   = InstantConversions.toBigDecimal(instant, converter);
        // BigDecimal equality checks scale as well; use compareTo for numeric equality.
        assertTrue("BigDecimal values should be numerically equal",
                expected.compareTo(actual) == 0);
    }

    @Test
    public void testToLocalDateTime() {
        Instant instant = Instant.parse("2022-10-15T08:30:45.123456789Z");
        LocalDateTime expected = instant.atZone(zoneId).toLocalDateTime();
        LocalDateTime actual   = InstantConversions.toLocalDateTime(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDate() {
        Instant instant = Instant.parse("1999-12-31T23:59:59Z");
        LocalDate expected = instant.atZone(zoneId).toLocalDate();
        LocalDate actual   = InstantConversions.toLocalDate(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime() {
        Instant instant = Instant.parse("2005-05-05T12:34:56.789Z");
        LocalTime expected = instant.atZone(zoneId).toLocalTime();
        LocalTime actual   = InstantConversions.toLocalTime(instant, converter);
        assertEquals(expected, actual);
    }

    /* ---------------------------------------------------------------------- */
    /* Additional edge‑case tests                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testZeroEpochInstant() {
        Instant instant = Instant.EPOCH; // 1970‑01‑01T00:00:00Z
        assertEquals("toLong for epoch should be 0", 0L, InstantConversions.toLong(instant, converter));
        assertEquals("toDouble for epoch should be 0.0", 0.0, InstantConversions.toDouble(instant, converter), 0.0);
        assertEquals("toBigInteger for epoch should be 0", BigInteger.ZERO,
                InstantConversions.toBigInteger(instant, converter));
        assertEquals("toBigDecimal for epoch should be 0", BigDecimal.ZERO,
                InstantConversions.toBigDecimal(instant, converter));
    }

    @Test
    public void testNegativeInstant_withNanoseconds() {
        // 1960‑01‑01T00:00:00.123456789Z (negative epoch seconds)
        Instant instant = instantOf(-315619200L, 123_456_789);
        // Verify that each conversion respects the sign.
        assertTrue("toLong must be negative for pre‑1970 instant",
                InstantConversions.toLong(instant, converter) < 0);
        assertTrue("toDouble must be negative for pre‑1970 instant",
                InstantConversions.toDouble(instant, converter) < 0.0);
        assertTrue("toBigInteger must be negative for pre‑1970 instant",
                InstantConversions.toBigInteger(instant, converter).signum() < 0);
    }
}
