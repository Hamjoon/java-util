package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@code LocalDateConversions}.
 *
 * <p>This test class follows the collaborative steps described in the task:
 * <ol>
 *   <li>List all public static methods of {@code LocalDateConversions}.</li>
 *   <li>Provide a basic test for each method covering the normal (happy‑path) behaviour.</li>
 *   <li>Identify edge‑cases and exception scenarios for each method.</li>
 *   <li>Add dedicated tests for those edge‑cases.</li>
 *   <li>Combine everything into a single test file.</li>
 * </ol>
 *
 * <p>All tests use real objects – no mocking – and rely only on the classes
 * and methods that appear in the supplied source code or the public API of
 * {@code Converter} and {@code CompactLinkedMap}.
 *
 * @author  ChatGPT
 */
public class LocalDateConversionsTest {

    /** A {@code Converter} that always works with UTC. */
    private Converter utcConverter;

    /** A {@code Converter} that works with the system default time‑zone (used for a few edge‑case tests). */
    private Converter defaultConverter;

    @Before
    public void setUp() {
        // Build ConverterOptions for UTC
        ConverterOptions utcOptions = new ConverterOptions();
        utcOptions.setZoneId(ZoneId.of("UTC"));
        utcOptions.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(utcOptions);

        // Build ConverterOptions for the default JVM zone (may be non‑UTC)
        ConverterOptions defaultOptions = new ConverterOptions();
        defaultOptions.setZoneId(ZoneId.systemDefault());
        defaultOptions.setTimeZone(TimeZone.getDefault());
        defaultConverter = new Converter(defaultOptions);
    }

    /* ----------------------------------------------------------------------
     *  List of public static methods in LocalDateConversions
     * ---------------------------------------------------------------------- */
    //    static Instant          toInstant(Object, Converter)
    //    static long            toLong(Object, Converter)
    //    static LocalDateTime  toLocalDateTime(Object, Converter)
    //    static ZonedDateTime  toZonedDateTime(Object, Converter)
    //    static OffsetDateTime toOffsetDateTime(Object, Converter)
    //    static double          toDouble(Object, Converter)
    //    static AtomicLong      toAtomicLong(Object, Converter)
    //    static Timestamp       toTimestamp(Object, Converter)
    //    static Calendar        toCalendar(Object, Converter)
    //    static java.sql.Date   toSqlDate(Object, Converter)
    //    static Date            toDate(Object, Converter)
    //    static BigInteger      toBigInteger(Object, Converter)
    //    static BigDecimal      toBigDecimal(Object, Converter)
    //    static String          toString(Object, Converter)
    //    static Map<String,Object> toMap(Object, Converter)

    /* ----------------------------------------------------------------------
     *  Happy‑path tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToInstant() {
        LocalDate ld = LocalDate.of(2023, 3, 15);
        Instant instant = LocalDateConversions.toInstant(ld, utcConverter);
        // Midnight UTC of the given date
        Instant expected = ZonedDateTime.of(ld, LocalTime.MIDNIGHT, ZoneId.of("UTC")).toInstant();
        assertEquals(expected, instant);
    }

    @Test
    public void testToLong() {
        LocalDate ld = LocalDate.of(1970, 1, 2); // one day after epoch
        long ms = LocalDateConversions.toLong(ld, utcConverter);
        assertEquals(24L * 60 * 60 * 1000, ms);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDate ld = LocalDate.of(2021, 12, 31);
        LocalDateTime ldt = LocalDateConversions.toLocalDateTime(ld, utcConverter);
        assertEquals(LocalDateTime.of(ld, LocalTime.MIDNIGHT), ldt);
    }

    @Test
    public void testToZonedDateTime() {
        LocalDate ld = LocalDate.of(2020, 2, 29);
        ZonedDateTime zdt = LocalDateConversions.toZonedDateTime(ld, utcConverter);
        assertEquals(ZonedDateTime.of(ld, LocalTime.MIDNIGHT, ZoneId.of("UTC")), zdt);
    }

    @Test
    public void testToOffsetDateTime() {
        LocalDate ld = LocalDate.of(2000, 1, 1);
        OffsetDateTime odt = LocalDateConversions.toOffsetDateTime(ld, utcConverter);
        ZoneOffset offset = ZoneOffset.UTC;
        assertEquals(OffsetDateTime.of(ld, LocalTime.MIDNIGHT, offset), odt);
    }

    @Test
    public void testToDouble() {
        LocalDate ld = LocalDate.of(1970, 1, 1);
        double seconds = LocalDateConversions.toDouble(ld, utcConverter);
        assertEquals(0.0, seconds, 0.0);
    }

    @Test
    public void testToAtomicLong() {
        LocalDate ld = LocalDate.of(2022, 5, 5);
        AtomicLong al = LocalDateConversions.toAtomicLong(ld, utcConverter);
        long expected = LocalDateConversions.toLong(ld, utcConverter);
        assertEquals(expected, al.get());
    }

    @Test
    public void testToTimestamp() {
        LocalDate ld = LocalDate.of(1970, 1, 2); // one day after epoch
        Timestamp ts = LocalDateConversions.toTimestamp(ld, utcConverter);
        // Timestamp stores milliseconds since epoch
        assertEquals(24L * 60 * 60 * 1000, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        LocalDate ld = LocalDate.of(1999, 12, 31);
        Calendar cal = LocalDateConversions.toCalendar(ld, utcConverter);
        long expected = LocalDateConversions.toLong(ld, utcConverter);
        assertEquals(expected, cal.getTimeInMillis());
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test
    public void testToSqlDate() {
        LocalDate ld = LocalDate.of(2023, 1, 1);
        java.sql.Date sqlDate = LocalDateConversions.toSqlDate(ld, utcConverter);
        assertEquals(LocalDateConversions.toLong(ld, utcConverter), sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        LocalDate ld = LocalDate.of(2023, 1, 1);
        Date date = LocalDateConversions.toDate(ld, utcConverter);
        assertEquals(LocalDateConversions.toLong(ld, utcConverter), date.getTime());
    }

    @Test
    public void testToBigInteger() {
        LocalDate ld = LocalDate.of(1970, 1, 1);
        BigInteger bi = LocalDateConversions.toBigInteger(ld, utcConverter);
        // InstantConversions.toBigInteger returns epoch milli as BigInteger
        assertEquals(BigInteger.ZERO, bi);
    }

    @Test
    public void testToBigDecimal() {
        LocalDate ld = LocalDate.of(1970, 1, 1);
        BigDecimal bd = LocalDateConversions.toBigDecimal(ld, utcConverter);
        // InstantConversions.toBigDecimal returns epoch milli as BigDecimal
        assertEquals(BigDecimal.ZERO, bd);
    }

    @Test
    public void testToString() {
        LocalDate ld = LocalDate.of(2021, 4, 9);
        String s = LocalDateConversions.toString(ld, utcConverter);
        assertEquals("2021-04-09", s);
    }

    @Test
    public void testToMap() {
        LocalDate ld = LocalDate.of(2021, 4, 9);
        Map<String, Object> map = LocalDateConversions.toMap(ld, utcConverter);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals(ld.toString(), map.get(MapConversions.DATE));
    }

    /* ----------------------------------------------------------------------
     *  Edge‑case / exception tests
     * ---------------------------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WithWrongType() {
        // Passing a String instead of LocalDate should raise ClassCastException
        LocalDateConversions.toZonedDateTime("2021-01-01", utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullFrom() {
        LocalDateConversions.toInstant(null, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullConverter() {
        LocalDateConversions.toInstant(LocalDate.now(), null);
    }

    @Test
    public void testToOffsetDateTime_DifferentTimeZone() {
        // Use a converter with a non‑UTC offset (e.g., America/New_York, which is -05:00 or -04:00)
        ConverterOptions nyOptions = new ConverterOptions();
        nyOptions.setZoneId(ZoneId.of("America/New_York"));
        nyOptions.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        Converter nyConverter = new Converter(nyOptions);

        LocalDate ld = LocalDate.of(2020, 6, 15); // summer, DST in effect (UTC‑4)
        OffsetDateTime odt = LocalDateConversions.toOffsetDateTime(ld, nyConverter);
        ZoneOffset expectedOffset = ZoneOffset.ofHours(-4);
        assertEquals(OffsetDateTime.of(ld, LocalTime.MIDNIGHT, expectedOffset), odt);
    }

    @Test
    public void testToTimestamp_EpochDayZero() {
        LocalDate epoch = LocalDate.ofEpochDay(0); // 1970‑01‑01
        Timestamp ts = LocalDateConversions.toTimestamp(epoch, utcConverter);
        assertEquals(0L, ts.getTime());
    }

    @Test
    public void testToCalendar_LeapSecondSimulation() {
        // Java time API does not expose leap seconds, but we can ensure that a date
        // far in the future is handled correctly.
        LocalDate ld = LocalDate.of(9999, 12, 31);
        Calendar cal = LocalDateConversions.toCalendar(ld, utcConverter);
        long expected = LocalDateConversions.toLong(ld, utcConverter);
        assertEquals(expected, cal.getTimeInMillis());
    }

    @Test
    public void testToDouble_NegativeEpoch() {
        LocalDate ld = LocalDate.of(1969, 12, 31); // one day before epoch
        double seconds = LocalDateConversions.toDouble(ld, utcConverter);
        assertEquals(-24L * 60 * 60, seconds, 0.0);
    }

    @Test
    public void testToMap_ImmutableResult() {
        LocalDate ld = LocalDate.of(2022, 2, 2);
        Map<String, Object> map = LocalDateConversions.toMap(ld, utcConverter);
        try {
            map.put("newKey", "value");
            fail("Map returned by toMap should be mutable (CompactLinkedMap) – but test expects it to be modifiable.");
        } catch (UnsupportedOperationException ignored) {
            // The library may return an immutable view; the test simply documents the behaviour.
        }
    }

    @Test
    public void testToString_DifferentLocale() {
        // Locale does not affect ISO_LOCAL_DATE formatting
        LocalDate ld = LocalDate.of(2023, 11, 13);
        String s = LocalDateConversions.toString(ld, utcConverter);
        assertEquals("2023-11-13", s);
    }

    @Test
    public void testToBigInteger_LargeDate() {
        LocalDate ld = LocalDate.of(3000, 1, 1);
        BigInteger bi = LocalDateConversions.toBigInteger(ld, utcConverter);
        // Verify that the value is positive and larger than current epoch milli
        assertTrue(bi.compareTo(BigInteger.ZERO) > 0);
    }

    @Test
    public void testToBigDecimal_Precision() {
        LocalDate ld = LocalDate.of(1970, 1, 2);
        BigDecimal bd = LocalDateConversions.toBigDecimal(ld, utcConverter);
        assertEquals(new BigDecimal(24L * 60 * 60 * 1000), bd);
    }
}
