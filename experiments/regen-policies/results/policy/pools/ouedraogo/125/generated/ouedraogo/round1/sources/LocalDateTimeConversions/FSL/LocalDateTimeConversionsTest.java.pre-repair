package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link LocalDateTimeConversions}.
 *
 * <p>The tests exercise every public static conversion method in the class.
 * They use a real {@link Converter} instance with a configurable {@link ConverterOptions}
 * that supplies the required {@link ZoneId} and {@link TimeZone}.  All objects are
 * created via their public constructors or factory methods – no mocking or subclassing
 * of library types is performed.</p>
 */
public class LocalDateTimeConversionsTest {

    /** A sample {@link LocalDateTime} used by all test cases. */
    private static final LocalDateTime SAMPLE_LDT = LocalDateTime.of(2023, 3, 25, 12, 34, 56, 789_000_000);

    /** Converter that works in UTC – makes expected values easy to calculate. */
    private Converter utcConverter;

    /** Converter that works in a non‑UTC zone (America/New_York) – used for offset‑specific tests. */
    private Converter nyConverter;

    @Before
    public void setUp() {
        // ---------- UTC converter ----------
        ConverterOptions utcOptions = new ConverterOptions();
        utcOptions.setZoneId(ZoneId.of("UTC"));
        utcOptions.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(utcOptions);

        // ---------- New York converter ----------
        ConverterOptions nyOptions = new ConverterOptions();
        nyOptions.setZoneId(ZoneId.of("America/New_York"));
        nyOptions.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        nyConverter = new Converter(nyOptions);
    }

    // -----------------------------------------------------------------------
    // Helper assertions
    // -----------------------------------------------------------------------
    private void assertZonedDateTimeEquals(ZonedDateTime expected, ZonedDateTime actual) {
        assertEquals("ZonedDateTime instant mismatch", expected.toInstant(), actual.toInstant());
        assertEquals("ZonedDateTime zone mismatch", expected.getZone(), actual.getZone());
    }

    // -----------------------------------------------------------------------
    // Tests for each conversion method
    // -----------------------------------------------------------------------

    @Test
    public void testToZonedDateTime_Utc() {
        ZonedDateTime expected = ZonedDateTime.of(SAMPLE_LDT, ZoneId.of("UTC"));
        ZonedDateTime result = LocalDateTimeConversions.toZonedDateTime(SAMPLE_LDT, utcConverter);
        assertZonedDateTimeEquals(expected, result);
    }

    @Test
    public void testToZonedDateTime_NewYork() {
        ZonedDateTime expected = ZonedDateTime.of(SAMPLE_LDT, ZoneId.of("America/New_York"));
        ZonedDateTime result = LocalDateTimeConversions.toZonedDateTime(SAMPLE_LDT, nyConverter);
        assertZonedDateTimeEquals(expected, result);
    }

    @Test
    public void testToOffsetDateTime_Utc() {
        OffsetDateTime expected = SAMPLE_LDT.atOffset(ZoneOffset.UTC);
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(SAMPLE_LDT, utcConverter);
        assertEquals("OffsetDateTime mismatch (UTC)", expected, result);
    }

    @Test
    public void testToOffsetDateTime_NewYork() {
        // The offset for New York on 2023‑03‑25 is -04:00 (EDT)
        ZoneOffset nyOffset = ZoneOffset.ofHours(-4);
        OffsetDateTime expected = SAMPLE_LDT.atOffset(nyOffset);
        OffsetDateTime result = LocalDateTimeConversions.toOffsetDateTime(SAMPLE_LDT, nyConverter);
        assertEquals("OffsetDateTime mismatch (New York)", expected, result);
    }

    @Test
    public void testToInstant_Utc() {
        Instant expected = SAMPLE_LDT.atZone(ZoneId.of("UTC")).toInstant();
        Instant result = LocalDateTimeConversions.toInstant(SAMPLE_LDT, utcConverter);
        assertEquals("Instant conversion (UTC) failed", expected, result);
    }

    @Test
    public void testToInstant_NewYork() {
        Instant expected = SAMPLE_LDT.atZone(ZoneId.of("America/New_York")).toInstant();
        Instant result = LocalDateTimeConversions.toInstant(SAMPLE_LDT, nyConverter);
        assertEquals("Instant conversion (New York) failed", expected, result);
    }

    @Test
    public void testToLong_Utc() {
        long expected = SAMPLE_LDT.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
        long result = LocalDateTimeConversions.toLong(SAMPLE_LDT, utcConverter);
        assertEquals("Epoch milli (UTC) mismatch", expected, result);
    }

    @Test
    public void testToLong_NewYork() {
        long expected = SAMPLE_LDT.atZone(ZoneId.of("America/New_York")).toInstant().toEpochMilli();
        long result = LocalDateTimeConversions.toLong(SAMPLE_LDT, nyConverter);
        assertEquals("Epoch milli (New York) mismatch", expected, result);
    }

    @Test
    public void testToDouble_Utc() {
        // InstantConversions.toDouble simply returns the epoch milli as a double
        double expected = (double) LocalDateTimeConversions.toLong(SAMPLE_LDT, utcConverter);
        double result = LocalDateTimeConversions.toDouble(SAMPLE_LDT, utcConverter);
        assertEquals("Double conversion (UTC) mismatch", expected, result, 0.0);
    }

    @Test
    public void testToDouble_NewYork() {
        double expected = (double) LocalDateTimeConversions.toLong(SAMPLE_LDT, nyConverter);
        double result = LocalDateTimeConversions.toDouble(SAMPLE_LDT, nyConverter);
        assertEquals("Double conversion (New York) mismatch", expected, result, 0.0);
    }

    @Test
    public void testToLocalDateTime_Utc() {
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(SAMPLE_LDT, utcConverter);
        assertEquals("LocalDateTime conversion (UTC) should be identity", SAMPLE_LDT, result);
    }

    @Test
    public void testToLocalDate_Utc() {
        LocalDate expected = SAMPLE_LDT.toLocalDate();
        LocalDate result = LocalDateTimeConversions.toLocalDate(SAMPLE_LDT, utcConverter);
        assertEquals("LocalDate conversion (UTC) failed", expected, result);
    }

    @Test
    public void testToLocalTime_Utc() {
        LocalTime expected = SAMPLE_LDT.toLocalTime();
        LocalTime result = LocalDateTimeConversions.toLocalTime(SAMPLE_LDT, utcConverter);
        assertEquals("LocalTime conversion (UTC) failed", expected, result);
    }

    @Test
    public void testToAtomicLong_Utc() {
        long expected = LocalDateTimeConversions.toLong(SAMPLE_LDT, utcConverter);
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(SAMPLE_LDT, utcConverter);
        assertEquals("AtomicLong value mismatch (UTC)", expected, result.get());
    }

    @Test
    public void testToTimestamp_Utc() {
        Timestamp expected = Timestamp.from(SAMPLE_LDT.atZone(ZoneId.of("UTC")).toInstant());
        Timestamp result = LocalDateTimeConversions.toTimestamp(SAMPLE_LDT, utcConverter);
        assertEquals("Timestamp instant mismatch (UTC)", expected, result);
    }

    @Test
    public void testToCalendar_Utc() {
        Calendar cal = LocalDateTimeConversions.toCalendar(SAMPLE_LDT, utcConverter);
        long expectedMillis = SAMPLE_LDT.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
        assertEquals("Calendar millis (UTC) mismatch", expectedMillis, cal.getTimeInMillis());
        assertEquals("Calendar timezone (UTC) mismatch", TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test
    public void testToSqlDate_Utc() {
        java.sql.Date expected = new java.sql.Date(LocalDateTimeConversions.toLong(SAMPLE_LDT, utcConverter));
        java.sql.Date result = LocalDateTimeConversions.toSqlDate(SAMPLE_LDT, utcConverter);
        assertEquals("java.sql.Date value mismatch (UTC)", expected, result);
    }

    @Test
    public void testToDate_Utc() {
        Date expected = new Date(LocalDateTimeConversions.toLong(SAMPLE_LDT, utcConverter));
        Date result = LocalDateTimeConversions.toDate(SAMPLE_LDT, utcConverter);
        assertEquals("java.util.Date value mismatch (UTC)", expected, result);
    }

    @Test
    public void testToBigInteger_Utc() {
        // InstantConversions.toBigInteger returns the epoch milli as a BigInteger
        BigInteger expected = BigInteger.valueOf(LocalDateTimeConversions.toLong(SAMPLE_LDT, utcConverter));
        BigInteger result = LocalDateTimeConversions.toBigInteger(SAMPLE_LDT, utcConverter);
        assertEquals("BigInteger conversion (UTC) mismatch", expected, result);
    }

    @Test
    public void testToBigDecimal_Utc() {
        // InstantConversions.toBigDecimal returns the epoch milli as a BigDecimal
        BigDecimal expected = BigDecimal.valueOf(LocalDateTimeConversions.toLong(SAMPLE_LDT, utcConverter));
        BigDecimal result = LocalDateTimeConversions.toBigDecimal(SAMPLE_LDT, utcConverter);
        assertEquals("BigDecimal conversion (UTC) mismatch", expected, result);
    }

    @Test
    public void testToString_Utc() {
        String expected = SAMPLE_LDT.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String result = LocalDateTimeConversions.toString(SAMPLE_LDT, utcConverter);
        assertEquals("String representation mismatch (UTC)", expected, result);
    }

    @Test
    public void testToMap_Utc() {
        Map<?, ?> map = LocalDateTimeConversions.toMap(SAMPLE_LDT, utcConverter);
        assertNotNull("Map result should not be null", map);
        assertEquals("Map should contain exactly 2 entries", 2, map.size());

        // Expected keys are defined in MapConversions
        Object dateKey = MapConversions.DATE;
        Object timeKey = MapConversions.TIME;

        assertTrue("Map should contain DATE key", map.containsKey(dateKey));
        assertTrue("Map should contain TIME key", map.containsKey(timeKey));

        assertEquals("Date string value mismatch", SAMPLE_LDT.toLocalDate().toString(), map.get(dateKey));
        assertEquals("Time string value mismatch", SAMPLE_LDT.toLocalTime().toString(), map.get(timeKey));
    }

    // -----------------------------------------------------------------------
    // Additional edge‑case tests
    // -----------------------------------------------------------------------

    @Test
    public void testZonedDateTimePreservesNanos() {
        // Verify that nanosecond precision is kept throughout the conversion chain.
        ZonedDateTime zdt = LocalDateTimeConversions.toZonedDateTime(SAMPLE_LDT, utcConverter);
        assertEquals("Nanoseconds lost during toZonedDateTime", SAMPLE_LDT.getNano(), zdt.getNano());
    }

    @Test
    public void testOffsetDateTimePreservesNanos() {
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(SAMPLE_LDT, utcConverter);
        assertEquals("Nanoseconds lost during toOffsetDateTime", SAMPLE_LDT.getNano(), odt.getNano());
    }

    @Test
    public void testTimestampHasSameNanos() {
        Timestamp ts = LocalDateTimeConversions.toTimestamp(SAMPLE_LDT, utcConverter);
        assertEquals("Timestamp nanos differ", SAMPLE_LDT.getNano() / 1_000_000, ts.getNanos() / 1_000_000);
    }
}
