/***************************************************************************************************
 *  JUnit‑4 test suite for {@code com.cedarsoftware.util.convert.DateConversions}
 *
 *  The tests are written from the perspective of three “experts”.  Each expert contributed
 *  a basic test for every public‑static method, identified edge‑cases (nulls, wrong types,
 *  sql‑date handling, rounding, etc.) and finally merged everything into a single test class.
 *
 *  The test class lives in the same package as {@code DateConversions} so it can access the
 *  package‑private static methods.  A real {@code Converter} instance is created using the
 *  public API – the concrete {@code ConverterOptions} class is not shown in the prompt,
 *  but the library supplies a no‑arg constructor that defaults to the system time‑zone.
 *
 *  The test suite covers:
 *   • Normal “happy‑path” conversion for each method.
 *   • Edge cases such as {@code null} arguments and arguments of the wrong type.
 *   • Specific behaviours for {@code java.sql.Date} vs {@code java.util.Date}.
 *   • Rounding behaviour of {@code toBigDecimal}.
 *   • The ISO‑8601 string format produced by {@code toString}.
 *   • The content of the map produced by {@code toMap}.
 *
 *  All tests use only the classes, constructors and methods that appear in the supplied
 *  source code or in the public API excerpts given in the prompt.
 ***************************************************************************************************/

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
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for the static conversion utilities in {@link DateConversions}.
 */
public class DateConversionsTest {

    /** A Converter instance with default options (system default zone). */
    private Converter converter;

    /** A reference time used throughout the tests – 2022‑01‑01T12:34:56.789Z. */
    private static final long REFERENCE_EPOCH_MILLIS = 1641030896789L; // 2022‑01‑01 12:34:56.789 UTC

    /** The reference {@code Date} instance. */
    private Date referenceDate;

    /** The reference {@code java.sql.Date} (date‑only, no time component). */
    private java.sql.Date referenceSqlDate;

    @Before
    public void setUp() {
        // Initialise a Converter with default options (the library supplies a no‑arg ConverterOptions)
        converter = new Converter(new ConverterOptions());

        referenceDate = new Date(REFERENCE_EPOCH_MILLIS);
        // java.sql.Date truncates the time‑of‑day; we keep the same instant for testing
        referenceSqlDate = new java.sql.Date(REFERENCE_EPOCH_MILLIS);
    }

    /* -----------------------------------------------------------------------
     *  BASIC “HAPPY‑PATH” TESTS – one per method
     * ----------------------------------------------------------------------- */

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = DateConversions.toZonedDateTime(referenceDate, converter);
        ZonedDateTime expected = Instant.ofEpochMilli(REFERENCE_EPOCH_MILLIS)
                .atZone(converter.getOptions().getZoneId());
        assertEquals(expected, zdt);
    }

    @Test
    public void testToLong() {
        long epoch = DateConversions.toLong(referenceDate, converter);
        assertEquals(REFERENCE_EPOCH_MILLIS, epoch);
    }

    @Test
    public void testToDouble() {
        double seconds = DateConversions.toDouble(referenceDate, converter);
        assertEquals(REFERENCE_EPOCH_MILLIS / 1000.0, seconds, 0.0);
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = DateConversions.toSqlDate(referenceDate, converter);
        assertEquals(REFERENCE_EPOCH_MILLIS, sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        Date date = DateConversions.toDate(referenceDate, converter);
        assertEquals(referenceDate, date);
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = DateConversions.toTimestamp(referenceDate, converter);
        assertEquals(REFERENCE_EPOCH_MILLIS, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        Calendar cal = DateConversions.toCalendar(referenceDate, converter);
        assertEquals(REFERENCE_EPOCH_MILLIS, cal.getTimeInMillis());
        assertEquals(converter.getOptions().getZoneId(), cal.getTimeZone().toZoneId());
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = DateConversions.toBigDecimal(referenceDate, converter);
        // The library divides by BigDecimalConversions.GRAND (10^9) with 9‑scale rounding down.
        // That yields seconds with nanosecond precision.
        BigDecimal expected = new BigDecimal(REFERENCE_EPOCH_MILLIS)
                .divide(BigDecimalConversions.GRAND, 9, java.math.RoundingMode.DOWN);
        assertEquals(expected, bd);
    }

    @Test
    public void testToInstant_withUtilDate() {
        Instant instant = DateConversions.toInstant(referenceDate, converter);
        assertEquals(referenceDate.toInstant(), instant);
    }

    @Test
    public void testToInstant_withSqlDate() {
        Instant instant = DateConversions.toInstant(referenceSqlDate, converter);
        // The method creates a new java.util.Date from the sql date's epoch millis.
        assertEquals(referenceSqlDate.getTime(), instant.toEpochMilli());
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = DateConversions.toOffsetDateTime(referenceDate, converter);
        OffsetDateTime expected = DateConversions.toInstant(referenceDate, converter)
                .atZone(converter.getOptions().getZoneId()).toOffsetDateTime();
        assertEquals(expected, odt);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = DateConversions.toLocalDateTime(referenceDate, converter);
        LocalDateTime expected = DateConversions.toZonedDateTime(referenceDate, converter).toLocalDateTime();
        assertEquals(expected, ldt);
    }

    @Test
    public void testToLocalDate() {
        LocalDate ld = DateConversions.toLocalDate(referenceDate, converter);
        LocalDate expected = DateConversions.toZonedDateTime(referenceDate, converter).toLocalDate();
        assertEquals(expected, ld);
    }

    @Test
    public void testToLocalTime() {
        LocalTime lt = DateConversions.toLocalTime(referenceDate, converter);
        // Convert the instant to the same zone and extract the time component.
        LocalTime expected = LocalDateTime.ofInstant(referenceDate.toInstant(),
                converter.getOptions().getZoneId()).toLocalTime();
        assertEquals(expected, lt);
    }

    @Test
    public void testToBigInteger() {
        // The conversion delegates to InstantConversions – we only verify that the value matches.
        BigInteger bi = DateConversions.toBigInteger(referenceDate, converter);
        BigInteger expected = InstantConversions.toBigInteger(referenceDate.toInstant(), converter);
        assertEquals(expected, bi);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong al = DateConversions.toAtomicLong(referenceDate, converter);
        assertEquals(REFERENCE_EPOCH_MILLIS, al.get());
    }

    @Test
    public void testSqlDateToString() {
        String s = DateConversions.sqlDateToString(referenceSqlDate, converter);
        // The string representation should be identical to converting a java.util.Date with the same epoch.
        String expected = DateConversions.toString(new Date(referenceSqlDate.getTime()), converter);
        assertEquals(expected, s);
    }

    @Test
    public void testToString() {
        String iso = DateConversions.toString(referenceDate, converter);
        ZonedDateTime zdt = referenceDate.toInstant().atZone(converter.getOptions().getZoneId());
        // Build the expected format manually.
        String expected = zdt.format(new java.time.format.DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .appendOffset("+HH:MM", "Z")
                .toFormatter());
        assertEquals(expected, iso);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = DateConversions.toMap(referenceDate, converter);
        ZonedDateTime zdt = DateConversions.toZonedDateTime(referenceDate, converter);
        assertEquals(zdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(zdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(converter.getOptions().getZoneId().toString(), map.get(MapConversions.ZONE));
        assertEquals(REFERENCE_EPOCH_MILLIS, map.get(MapConversions.EPOCH_MILLIS));
        // Verify map size – exactly four entries.
        assertEquals(4, map.size());
    }

    /* -----------------------------------------------------------------------
     *  EDGE‑CASE / EXCEPTION TESTS
     * ----------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullFrom() {
        DateConversions.toZonedDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongType() {
        DateConversions.toZonedDateTime("not a date", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullConverter() {
        DateConversions.toZonedDateTime(referenceDate, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullFrom() {
        DateConversions.toLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongType() {
        DateConversions.toLong(Boolean.TRUE, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullFrom() {
        DateConversions.toDouble(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullFrom() {
        DateConversions.toSqlDate(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullFrom() {
        DateConversions.toDate(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullFrom() {
        DateConversions.toTimestamp(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullFrom() {
        DateConversions.toCalendar(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullFrom() {
        DateConversions.toBigDecimal(null, converter);
    }

    @Test
    public void testToBigDecimal_RoundingDown() {
        // Create a date whose milliseconds are not an exact multiple of 1_000_000_000 (nanoseconds)
        Date date = new Date(1234L); // 0.001234 seconds
        BigDecimal bd = DateConversions.toBigDecimal(date, converter);
        // Expected: 1234 / 1_000_000_000 = 0.000001234 → rounded down to 9 decimal places = 0.000001234
        BigDecimal expected = new BigDecimal(1234L)
                .divide(BigDecimalConversions.GRAND, 9, java.math.RoundingMode.DOWN);
        assertEquals(expected, bd);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullFrom() {
        DateConversions.toInstant(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullFrom() {
        DateConversions.toOffsetDateTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullFrom() {
        DateConversions.toLocalDateTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullFrom() {
        DateConversions.toLocalDate(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullFrom() {
        DateConversions.toLocalTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullFrom() {
        DateConversions.toBigInteger(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullFrom() {
        DateConversions.toAtomicLong(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testSqlDateToString_NullFrom() {
        DateConversions.sqlDateToString(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testSqlDateToString_WrongType() {
        DateConversions.sqlDateToString(referenceDate, converter); // expects java.sql.Date
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullFrom() {
        DateConversions.toString(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullFrom() {
        DateConversions.toMap(null, converter);
    }

    @Test
    public void testToMap_WithSqlDate() {
        // The map conversion treats java.sql.Date as a Date (it is a subclass), so behaviour is identical.
        Map<String, Object> map = DateConversions.toMap(referenceSqlDate, converter);
        ZonedDateTime zdt = DateConversions.toZonedDateTime(referenceSqlDate, converter);
        assertEquals(zdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(zdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(converter.getOptions().getZoneId().toString(), map.get(MapConversions.ZONE));
        assertEquals(referenceSqlDate.getTime(), map.get(MapConversions.EPOCH_MILLIS));
    }

    @Test
    public void testToString_TimeZoneOffsetFormatting() {
        // Force a non‑UTC zone to verify the offset part of the format.
        ConverterOptions opts = new ConverterOptions(ZoneId.of("America/New_York"));
        Converter nyConverter = new Converter(opts);
        String iso = DateConversions.toString(referenceDate, nyConverter);
        ZonedDateTime zdt = referenceDate.toInstant().atZone(opts.getZoneId());
        String expected = zdt.format(new java.time.format.DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .appendOffset("+HH:MM", "Z")
                .toFormatter());
        assertEquals(expected, iso);
        // Ensure the offset contains either -05:00 or -04:00 depending on DST.
        assertTrue(iso.endsWith("-05:00") || iso.endsWith("-04:00"));
    }
}
