package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JUnit 4 test suite for {@code CalendarConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Basic functional verification for each public static conversion method.</li>
 *   <li>Edge‑case handling such as {@code null} inputs.</li>
 *   <li>Time‑zone specific scenarios.</li>
 *   <li>Verification of numeric scaling for {@code BigDecimal} and {@code BigInteger} conversions.</li>
 *   <li>Correctness of the {@code toMap} representation.</li>
 * </ul>
 */
public class CalendarConversionsTest {

    /** A reusable {@link Converter} instance – the library supplies a default constructor. */
    private Converter converter;

    /** A {@link Calendar} instance set to a known instant (2022‑03‑14 15:09:26.123 UTC). */
    private Calendar utcCalendar;

    /** Epoch milliseconds for the reference instant above. */
    private static final long REFERENCE_EPOCH_MILLIS = 1647264566123L; // 2022‑03‑14T15:09:26.123Z

    @Before
    public void setUp() {
        // Create a simple Converter with default options (the library guarantees a no‑arg options ctor).
        converter = new Converter(new ConverterOptions());

        // Build a Calendar in UTC with the known instant.
        utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.clear();
        utcCalendar.setTimeInMillis(REFERENCE_EPOCH_MILLIS);
    }

    /* ----------------------------------------------------------------------
     *  1. toZonedDateTime
     * ---------------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime_Basic() {
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(utcCalendar, converter);
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
        assertEquals(2022, zdt.getYear());
        assertEquals(3, zdt.getMonthValue());
        assertEquals(14, zdt.getDayOfMonth());
        assertEquals(15, zdt.getHour());
        assertEquals(9, zdt.getMinute());
        assertEquals(26, zdt.getSecond());
        assertEquals(123_000_000, zdt.getNano());
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullInput() {
        CalendarConversions.toZonedDateTime(null, converter);
    }

    /* ----------------------------------------------------------------------
     *  2. toLong
     * ---------------------------------------------------------------------- */
    @Test
    public void testToLong_Basic() {
        Long millis = CalendarConversions.toLong(utcCalendar, converter);
        assertEquals(Long.valueOf(REFERENCE_EPOCH_MILLIS), millis);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        CalendarConversions.toLong(null, converter);
    }

    /* ----------------------------------------------------------------------
     *  3. toDouble
     * ---------------------------------------------------------------------- */
    @Test
    public void testToDouble_Basic() {
        double seconds = CalendarConversions.toDouble(utcCalendar, converter);
        assertEquals(REFERENCE_EPOCH_MILLIS / 1000.0, seconds, 0.000001);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInput() {
        CalendarConversions.toDouble(null, converter);
    }

    /* ----------------------------------------------------------------------
     *  4. toDate
     * ---------------------------------------------------------------------- */
    @Test
    public void testToDate_Basic() {
        Date date = CalendarConversions.toDate(utcCalendar, converter);
        assertEquals(new Date(REFERENCE_EPOCH_MILLIS), date);
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullInput() {
        CalendarConversions.toDate(null, converter);
    }

    /* ----------------------------------------------------------------------
     *  5. toSqlDate
     * ---------------------------------------------------------------------- */
    @Test
    public void testToSqlDate_Basic() {
        java.sql.Date sqlDate = CalendarConversions.toSqlDate(utcCalendar, converter);
        assertEquals(REFERENCE_EPOCH_MILLIS, sqlDate.getTime());
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullInput() {
        CalendarConversions.toSqlDate(null, converter);
    }

    /* ----------------------------------------------------------------------
     *  6. toTimestamp
     * ---------------------------------------------------------------------- */
    @Test
    public void testToTimestamp_Basic() {
        Timestamp ts = CalendarConversions.toTimestamp(utcCalendar, converter);
        assertEquals(REFERENCE_EPOCH_MILLIS, ts.getTime());
        assertEquals(0, ts.getNanos()); // Calendar stores millis only.
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullInput() {
        CalendarConversions.toTimestamp(null, converter);
    }

    /* ----------------------------------------------------------------------
     *  7. toAtomicLong
     * ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicLong_Basic() {
        AtomicLong atomic = CalendarConversions.toAtomicLong(utcCalendar, converter);
        assertEquals(REFERENCE_EPOCH_MILLIS, atomic.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInput() {
        CalendarConversions.toAtomicLong(null, converter);
    }

    /* ----------------------------------------------------------------------
     *  8. toInstant
     * ---------------------------------------------------------------------- */
    @Test
    public void testToInstant_Basic() {
        Instant instant = CalendarConversions.toInstant(utcCalendar, converter);
        assertEquals(Instant.ofEpochMilli(REFERENCE_EPOCH_MILLIS), instant);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullInput() {
        CalendarConversions.toInstant(null, converter);
    }

    /* ----------------------------------------------------------------------
     *  9. toLocalDateTime
     * ---------------------------------------------------------------------- */
    @Test
    public void testToLocalDateTime_Basic() {
        LocalDateTime ldt = CalendarConversions.toLocalDateTime(utcCalendar, converter);
        assertEquals(LocalDateTime.of(2022, 3, 14, 15, 9, 26, 123_000_000), ldt);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullInput() {
        CalendarConversions.toLocalDateTime(null, converter);
    }

    /* ----------------------------------------------------------------------
     * 10. toLocalDate
     * ---------------------------------------------------------------------- */
    @Test
    public void testToLocalDate_Basic() {
        LocalDate ld = CalendarConversions.toLocalDate(utcCalendar, converter);
        assertEquals(LocalDate.of(2022, 3, 14), ld);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullInput() {
        CalendarConversions.toLocalDate(null, converter);
    }

    /* ----------------------------------------------------------------------
     * 11. toLocalTime
     * ---------------------------------------------------------------------- */
    @Test
    public void testToLocalTime_Basic() {
        LocalTime lt = CalendarConversions.toLocalTime(utcCalendar, converter);
        assertEquals(LocalTime.of(15, 9, 26, 123_000_000), lt);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullInput() {
        CalendarConversions.toLocalTime(null, converter);
    }

    /* ----------------------------------------------------------------------
     * 12. toBigDecimal
     * ---------------------------------------------------------------------- */
    @Test
    public void testToBigDecimal_Basic() throws Exception {
        // The method divides epoch millis by BigDecimalConversions.GRAND.
        // Use reflection to obtain the divisor.
        java.lang.reflect.Field grandField = BigDecimalConversions.class.getDeclaredField("GRAND");
        grandField.setAccessible(true);
        BigDecimal grand = (BigDecimal) grandField.get(null);

        BigDecimal bd = CalendarConversions.toBigDecimal(utcCalendar, converter);
        // Verify scaling: bd * GRAND == epochMillis
        BigDecimal reconstructed = bd.multiply(grand);
        assertEquals(BigDecimal.valueOf(REFERENCE_EPOCH_MILLIS), reconstructed);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        CalendarConversions.toBigDecimal(null, converter);
    }

    /* ----------------------------------------------------------------------
     * 13. toBigInteger
     * ---------------------------------------------------------------------- */
    @Test
    public void testToBigInteger_Basic() {
        BigInteger bi = CalendarConversions.toBigInteger(utcCalendar, converter);
        BigInteger expected = BigInteger.valueOf(REFERENCE_EPOCH_MILLIS).multiply(BigInteger.valueOf(1_000_000L));
        assertEquals(expected, bi);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        CalendarConversions.toBigInteger(null, converter);
    }

    /* ----------------------------------------------------------------------
     * 14. clone
     * ---------------------------------------------------------------------- */
    @Test
    public void testClone_Basic() {
        Calendar cloned = CalendarConversions.clone(utcCalendar, converter);
        assertNotSame(utcCalendar, cloned);
        assertEquals(utcCalendar.getTimeInMillis(), cloned.getTimeInMillis());
        assertEquals(utcCalendar.getTimeZone(), cloned.getTimeZone());
    }

    @Test(expected = NullPointerException.class)
    public void testClone_NullInput() {
        CalendarConversions.clone(null, converter);
    }

    /* ----------------------------------------------------------------------
     * 15. create
     * ---------------------------------------------------------------------- */
    @Test
    public void testCreate_Basic() {
        Calendar created = CalendarConversions.create(REFERENCE_EPOCH_MILLIS, converter);
        assertEquals(REFERENCE_EPOCH_MILLIS, created.getTimeInMillis());
        // The time‑zone should be the one supplied by the converter’s options.
        TimeZone expectedTZ = converter.getOptions().getTimeZone();
        assertEquals(expectedTZ, created.getTimeZone());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreate_NegativeEpoch() {
        // Calendar accepts negative epoch values, but we test the library's expected
        // behaviour for an obviously invalid input (e.g., Long.MIN_VALUE + 1 may overflow).
        CalendarConversions.create(Long.MIN_VALUE, converter);
    }

    /* ----------------------------------------------------------------------
     * 16. toString
     * ---------------------------------------------------------------------- */
    @Test
    public void testToString_Basic() {
        String result = CalendarConversions.toString(utcCalendar, converter);
        // DateConversions formats a java.util.Date; we expect the default toString form.
        // The exact format is library‑specific; we verify that the result contains the epoch millis.
        assertTrue(result.contains(String.valueOf(REFERENCE_EPOCH_MILLIS)));
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        CalendarConversions.toString(null, converter);
    }

    /* ----------------------------------------------------------------------
     * 17. toOffsetDateTime
     * ---------------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime_Basic() {
        OffsetDateTime odt = CalendarConversions.toOffsetDateTime(utcCalendar, converter);
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(utcCalendar.getTimeZone().getOffset(utcCalendar.getTimeInMillis()) / 1000);
        assertEquals(offset, odt.getOffset());
        assertEquals(Instant.ofEpochMilli(REFERENCE_EPOCH_MILLIS), odt.toInstant());
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullInput() {
        CalendarConversions.toOffsetDateTime(null, converter);
    }

    /* ----------------------------------------------------------------------
     * 18. toMap
     * ---------------------------------------------------------------------- */
    @Test
    public void testToMap_Basic() {
        Map<String, Object> map = CalendarConversions.toMap(utcCalendar, converter);
        // Verify required keys
        assertTrue(map.containsKey(MapConversions.DATE));
        assertTrue(map.containsKey(MapConversions.TIME));
        assertTrue(map.containsKey(MapConversions.ZONE));
        assertTrue(map.containsKey(MapConversions.EPOCH_MILLIS));

        // Verify values format
        assertEquals("2022-03-14", map.get(MapConversions.DATE));
        assertEquals("15:09:26.123", map.get(MapConversions.TIME));
        assertEquals(utcCalendar.getTimeZone().toZoneId().toString(), map.get(MapConversions.ZONE));
        assertEquals(REFERENCE_EPOCH_MILLIS, map.get(MapConversions.EPOCH_MILLIS));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        CalendarConversions.toMap(null, converter);
    }
}
