package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link InstantConversions}.
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Typical values (positive epoch, zero nanoseconds)</li>
 *   <li>Edge values (negative epoch, maximum nanoseconds)</li>
 *   <li>All conversion paths (Map, Zoned/OffsetDateTime, numeric types, SQL types, Calendar, Big types, Local* types)</li>
 *   <li>Behaviour when {@code null} is supplied (expected NPE)</li>
 *   <li>Time‑zone–specific checks (DST transition)</li>
 * </ul>
 * </p>
 *
 * <p>
 * The test creates a real {@link Converter} instance using the public
 * {@code Converter(ConverterOptions)} constructor.  The {@code ConverterOptions}
 * class is part of the Cedar library (not shown) and provides {@code setZoneId}
 * and {@code setTimeZone} mutators – these are used to control the zone used
 * by the conversion methods.
 * </p>
 */
public class InstantConversionsTest {

    /** Converter that works with UTC (no offset) – used for most tests. */
    private Converter utcConverter;

    /** Converter that uses America/New_York – used for DST‑related tests. */
    private Converter nyConverter;

    @Before
    public void setUp() {
        // ----- UTC converter -------------------------------------------------
        ConverterOptions utcOpts = new ConverterOptions();
        utcOpts.setZoneId(ZoneId.of("UTC"));
        utcOpts.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(utcOpts);

        // ----- New York converter (has DST) ---------------------------------
        ConverterOptions nyOpts = new ConverterOptions();
        nyOpts.setZoneId(ZoneId.of("America/New_York"));
        nyOpts.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        nyConverter = new Converter(nyOpts);
    }

    /* -----------------------------------------------------------------------
     * Helper methods
     * --------------------------------------------------------------------- */

    /** Returns an {@link Instant} with the given epoch seconds and nanoseconds. */
    private static Instant instantOf(long epochSeconds, int nanos) {
        return Instant.ofEpochSecond(epochSeconds, nanos);
    }

    /** Asserts that two {@link ZonedDateTime}s represent the same instant and zone. */
    private static void assertZonedEquals(ZonedDateTime expected, ZonedDateTime actual) {
        assertEquals("Instant mismatch", expected.toInstant(), actual.toInstant());
        assertEquals("ZoneId mismatch", expected.getZone(), actual.getZone());
    }

    /* -----------------------------------------------------------------------
     * Tests for each static conversion method
     * --------------------------------------------------------------------- */

    @Test
    public void testToMap_Typical() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 123_456_789);
        Map<String, Object> map = InstantConversions.toMap(instant, utcConverter);

        assertNotNull("Map should not be null", map);
        assertEquals("Map should contain exactly two entries", 2, map.size());
        assertEquals("seconds entry incorrect",
                Long.valueOf(1_600_000_000L), map.get("seconds"));
        assertEquals("nanos entry incorrect",
                Integer.valueOf(123_456_789), map.get("nanos"));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInstant() {
        InstantConversions.toMap(null, utcConverter);
    }

    @Test
    public void testToZonedDateTime_Typical() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L);
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(instant, utcConverter);
        ZonedDateTime expected = instant.atZone(ZoneId.of("UTC"));
        assertZonedEquals(expected, zdt);
    }

    @Test
    public void testToZonedDateTime_WithDifferentZone() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L);
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(instant, nyConverter);
        ZonedDateTime expected = instant.atZone(ZoneId.of("America/New_York"));
        assertZonedEquals(expected, zdt);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullInstant() {
        InstantConversions.toZonedDateTime(null, utcConverter);
    }

    @Test
    public void testToOffsetDateTime_Utc() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L);
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(instant, utcConverter);
        ZoneOffset offset = ZoneOffset.UTC;
        assertEquals("Offset mismatch", offset, odt.getOffset());
        assertEquals("Instant mismatch", instant, odt.toInstant());
    }

    @Test
    public void testToOffsetDateTime_DaylightSavingTransition() {
        // 2021‑03‑14 02:30 local New York time does not exist (DST forward)
        // Use an instant that is before the jump and one after.
        Instant before = Instant.parse("2021-03-14T06:59:59Z"); // 01:59:59 EST
        Instant after  = Instant.parse("2021-03-14T07:00:00Z"); // 03:00:00 EDT

        OffsetDateTime beforeOdt = InstantConversions.toOffsetDateTime(before, nyConverter);
        OffsetDateTime afterOdt  = InstantConversions.toOffsetDateTime(after, nyConverter);

        // New York offset before DST starts: -05:00
        assertEquals(ZoneOffset.ofHours(-5), beforeOdt.getOffset());
        // After DST starts: -04:00
        assertEquals(ZoneOffset.ofHours(-4), afterOdt.getOffset());
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullInstant() {
        InstantConversions.toOffsetDateTime(null, utcConverter);
    }

    @Test
    public void testToLong_Typical() {
        Instant instant = Instant.ofEpochMilli(1_600_000_123L);
        long epochMilli = InstantConversions.toLong(instant, utcConverter);
        assertEquals(1_600_000_123L, epochMilli);
    }

    @Test
    public void testToLong_NegativeEpoch() {
        Instant instant = Instant.ofEpochMilli(-123_456L);
        long epochMilli = InstantConversions.toLong(instant, utcConverter);
        assertEquals(-123_456L, epochMilli);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInstant() {
        InstantConversions.toLong(null, utcConverter);
    }

    @Test
    public void testToDouble_Typical() {
        Instant instant = instantOf(1_600_000_000L, 500_000_000); // .5 seconds
        double d = InstantConversions.toDouble(instant, utcConverter);
        assertEquals(1_600_000_000.5, d, 0.0);
    }

    @Test
    public void testToDouble_MaxNanos() {
        Instant instant = instantOf(0L, 999_999_999);
        double d = InstantConversions.toDouble(instant, utcConverter);
        assertEquals(0.999_999_999, d, 0.0);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInstant() {
        InstantConversions.toDouble(null, utcConverter);
    }

    @Test
    public void testToAtomicLong() {
        Instant instant = Instant.ofEpochMilli(42L);
        AtomicLong atomic = InstantConversions.toAtomicLong(instant, utcConverter);
        assertEquals(42L, atomic.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInstant() {
        InstantConversions.toAtomicLong(null, utcConverter);
    }

    @Test
    public void testToTimestamp() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 123_456_789);
        Timestamp ts = InstantConversions.toTimestamp(instant, utcConverter);
        assertEquals(Timestamp.from(instant), ts);
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullInstant() {
        InstantConversions.toTimestamp(null, utcConverter);
    }

    @Test
    public void testToSqlDate() {
        Instant instant = Instant.ofEpochMilli(1_600_000_000L);
        java.sql.Date sqlDate = InstantConversions.toSqlDate(instant, utcConverter);
        assertEquals(new java.sql.Date(1_600_000_000L), sqlDate);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullInstant() {
        InstantConversions.toSqlDate(null, utcConverter);
    }

    @Test
    public void testToDate() {
        Instant instant = Instant.ofEpochMilli(9_999L);
        Date date = InstantConversions.toDate(instant, utcConverter);
        assertEquals(new Date(9_999L), date);
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullInstant() {
        InstantConversions.toDate(null, utcConverter);
    }

    @Test
    public void testToCalendar() {
        Instant instant = Instant.ofEpochMilli(1_234_567_890L);
        Calendar cal = InstantConversions.toCalendar(instant, utcConverter);
        assertEquals(instant.toEpochMilli(), cal.getTimeInMillis());
        // Calendar should be in the UTC time zone as per the converter options.
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullInstant() {
        InstantConversions.toCalendar(null, utcConverter);
    }

    @Test
    public void testToBigInteger_Typical() {
        Instant instant = instantOf(2L, 3);
        // Expected: 2 * 1_000_000_000 + 3 = 2_000_000_003
        BigInteger expected = BigInteger.valueOf(2L).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(3L));
        assertEquals(expected, InstantConversions.toBigInteger(instant, utcConverter));
    }

    @Test
    public void testToBigInteger_NegativeSeconds() {
        Instant instant = instantOf(-1L, 500_000_000);
        // (-1 * 1_000_000_000) + 500_000_000 = -500_000_000
        BigInteger expected = BigInteger.valueOf(-1L).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(500_000_000L));
        assertEquals(expected, InstantConversions.toBigInteger(instant, utcConverter));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInstant() {
        InstantConversions.toBigInteger(null, utcConverter);
    }

    @Test
    public void testToBigDecimal_Typical() {
        Instant instant = instantOf(123L, 250_000_000);
        BigDecimal bd = InstantConversions.toBigDecimal(instant, utcConverter);
        // 123 + 0.25 = 123.25
        assertEquals(new BigDecimal("123.25"), bd);
    }

    @Test
    public void testToBigDecimal_MaxNanos() {
        Instant instant = instantOf(0L, 999_999_999);
        BigDecimal bd = InstantConversions.toBigDecimal(instant, utcConverter);
        assertEquals(new BigDecimal("0.999999999"), bd);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInstant() {
        InstantConversions.toBigDecimal(null, utcConverter);
    }

    @Test
    public void testToLocalDateTime() {
        Instant instant = Instant.parse("2022-08-15T12:34:56.789Z");
        LocalDateTime ldt = InstantConversions.toLocalDateTime(instant, utcConverter);
        assertEquals(LocalDateTime.of(2022, 8, 15, 12, 34, 56, 789_000_000), ldt);
    }

    @Test
    public void testToLocalDateTime_WithZone() {
        // Same instant, but New York zone (UTC‑4 at this date)
        Instant instant = Instant.parse("2022-08-15T12:34:56.789Z");
        LocalDateTime ldt = InstantConversions.toLocalDateTime(instant, nyConverter);
        // 2022‑08‑15 08:34:56.789 in America/New_York
        assertEquals(LocalDateTime.of(2022, 8, 15, 8, 34, 56, 789_000_000), ldt);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullInstant() {
        InstantConversions.toLocalDateTime(null, utcConverter);
    }

    @Test
    public void testToLocalDate() {
        Instant instant = Instant.parse("2021-12-31T23:00:00Z");
        LocalDate ld = InstantConversions.toLocalDate(instant, utcConverter);
        assertEquals(LocalDate.of(2021, 12, 31), ld);
    }

    @Test
    public void testToLocalDate_WithZoneCrossMidnight() {
        // Same instant, New York is still previous day (UTC‑5)
        Instant instant = Instant.parse("2021-12-31T23:00:00Z");
        LocalDate ld = InstantConversions.toLocalDate(instant, nyConverter);
        assertEquals(LocalDate.of(2021, 12, 31), ld); // still 2021‑12‑31 in NY (18:00)
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullInstant() {
        InstantConversions.toLocalDate(null, utcConverter);
    }

    @Test
    public void testToLocalTime() {
        Instant instant = Instant.parse("2021-01-01T05:06:07.123Z");
        LocalTime lt = InstantConversions.toLocalTime(instant, utcConverter);
        assertEquals(LocalTime.of(5, 6, 7, 123_000_000), lt);
    }

    @Test
    public void testToLocalTime_WithZoneOffset() {
        // In New York (UTC‑5) the same instant is 00:06:07.123
        Instant instant = Instant.parse("2021-01-01T05:06:07.123Z");
        LocalTime lt = InstantConversions.toLocalTime(instant, nyConverter);
        assertEquals(LocalTime.of(0, 6, 7, 123_000_000), lt);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullInstant() {
        InstantConversions.toLocalTime(null, utcConverter);
    }
}
