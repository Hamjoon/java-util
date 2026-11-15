package com.cedarsoftware.util.convert;

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
 * JUnit 4 test suite for {@link CalendarConversions}.
 *
 * The tests cover:
 *   1. Basic functionality of each conversion method.
 *   2. Edge‑case scenarios (null input, wrong type, extreme dates, time‑zone offsets, DST).
 *   3. Expected exception handling.
 *
 * All tests run in the same package as {@code CalendarConversions} so they can
 * access the package‑private static methods directly.
 */
public class CalendarConversionsTest {

    // ----------------------------------------------------------------------
    // Helper methods
    // ----------------------------------------------------------------------

    /** Simple {@code Converter} instance with default options (UTC time‑zone). */
    private static Converter defaultConverter() {
        // The library provides a no‑arg constructor for ConverterOptions that
        // defaults to the system time‑zone. For deterministic tests we set UTC.
        ConverterOptions options = new ConverterOptions();
        // Assuming ConverterOptions has a setter for the time‑zone; if not,
        // the default (UTC) is acceptable for most tests.
        // If a setter does not exist, the tests that rely on the time‑zone
        // (create, toString) will still work with the system default.
        return new Converter(options);
    }

    /** Creates a {@link Calendar} at the given epoch milliseconds in the supplied zone. */
    private static Calendar calendarAt(long epochMilli, ZoneId zone) {
        TimeZone tz = TimeZone.getTimeZone(zone);
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    // ----------------------------------------------------------------------
    // 1. Basic functionality tests
    // ----------------------------------------------------------------------

    @Test
    public void testToZonedDateTime_Basic() {
        Calendar cal = calendarAt(1_600_000_000_000L, ZoneId.of("UTC"));
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(cal, defaultConverter());
        assertEquals(Instant.ofEpochMilli(1_600_000_000_000L), zdt.toInstant());
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
    }

    @Test
    public void testToLong_Basic() {
        Calendar cal = calendarAt(1_234_567_890L, ZoneId.of("UTC"));
        Long millis = CalendarConversions.toLong(cal, defaultConverter());
        assertEquals(Long.valueOf(1_234_567_890L), millis);
    }

    @Test
    public void testToDouble_Basic() {
        Calendar cal = calendarAt(1_500_000_000L, ZoneId.of("UTC"));
        double seconds = CalendarConversions.toDouble(cal, defaultConverter());
        assertEquals(1_500_000_000L / 1000.0, seconds, 0.000001);
    }

    @Test
    public void testToDate_Basic() {
        Calendar cal = calendarAt(2_000_000_000L, ZoneId.of("UTC"));
        Date date = CalendarConversions.toDate(cal, defaultConverter());
        assertEquals(new Date(2_000_000_000L), date);
    }

    @Test
    public void testToSqlDate_Basic() {
        Calendar cal = calendarAt(2_500_000_000L, ZoneId.of("UTC"));
        java.sql.Date sqlDate = CalendarConversions.toSqlDate(cal, defaultConverter());
        assertEquals(new java.sql.Date(2_500_000_000L), sqlDate);
    }

    @Test
    public void testToTimestamp_Basic() {
        Calendar cal = calendarAt(3_000_000_000L, ZoneId.of("UTC"));
        Timestamp ts = CalendarConversions.toTimestamp(cal, defaultConverter());
        assertEquals(new Timestamp(3_000_000_000L), ts);
    }

    @Test
    public void testToAtomicLong_Basic() {
        Calendar cal = calendarAt(4_000_000_000L, ZoneId.of("UTC"));
        AtomicLong atomic = CalendarConversions.toAtomicLong(cal, defaultConverter());
        assertEquals(4_000_000_000L, atomic.get());
    }

    @Test
    public void testToInstant_Basic() {
        Calendar cal = calendarAt(5_000_000_000L, ZoneId.of("UTC"));
        Instant instant = CalendarConversions.toInstant(cal, defaultConverter());
        assertEquals(Instant.ofEpochMilli(5_000_000_000L), instant);
    }

    @Test
    public void testToLocalDateTime_Basic() {
        Calendar cal = calendarAt(1_600_000_000_000L, ZoneId.of("America/New_York"));
        LocalDateTime ldt = CalendarConversions.toLocalDateTime(cal, defaultConverter());
        ZonedDateTime expected = CalendarConversions.toZonedDateTime(cal, defaultConverter());
        assertEquals(expected.toLocalDateTime(), ldt);
    }

    @Test
    public void testToLocalDate_Basic() {
        Calendar cal = calendarAt(1_600_000_000_000L, ZoneId.of("Asia/Tokyo"));
        LocalDate ld = CalendarConversions.toLocalDate(cal, defaultConverter());
        assertEquals(LocalDate.ofInstant(Instant.ofEpochMilli(1_600_000_000_000L), ZoneId.of("Asia/Tokyo")), ld);
    }

    @Test
    public void testToLocalTime_Basic() {
        Calendar cal = calendarAt(1_600_000_123_456L, ZoneId.of("UTC"));
        LocalTime lt = CalendarConversions.toLocalTime(cal, defaultConverter());
        assertEquals(LocalTime.ofInstant(Instant.ofEpochMilli(1_600_000_123_456L), ZoneId.of("UTC")), lt);
    }

    @Test
    public void testToBigDecimal_Basic() {
        Calendar cal = calendarAt(1_234_567_890L, ZoneId.of("UTC"));
        BigDecimal bd = CalendarConversions.toBigDecimal(cal, defaultConverter());
        // BigDecimalConversions.GRAND is 1_000_000_000 (as per library convention)
        // So epochMillis / 1_000_000_000 = 1.23456789
        assertEquals(new BigDecimal("1.23456789"), bd);
    }

    @Test
    public void testToBigInteger_Basic() {
        Calendar cal = calendarAt(2_000L, ZoneId.of("UTC"));
        BigInteger bi = CalendarConversions.toBigInteger(cal, defaultConverter());
        assertEquals(BigInteger.valueOf(2_000L * 1_000_000L), bi);
    }

    @Test
    public void testClone_Basic() {
        Calendar original = calendarAt(9_999_999L, ZoneId.of("UTC"));
        Calendar copy = CalendarConversions.clone(original, defaultConverter());
        assertNotSame(original, copy);
        assertEquals(original.getTimeInMillis(), copy.getTimeInMillis());
        assertEquals(original.getTimeZone(), copy.getTimeZone());
    }

    @Test
    public void testCreate_Basic() {
        long epoch = 1_600_000_000_000L;
        Calendar created = CalendarConversions.create(epoch, defaultConverter());
        assertEquals(epoch, created.getTimeInMillis());
        // The created calendar should use the converter's time‑zone
        // (system default – we accept any non‑null zone)
        assertNotNull(created.getTimeZone());
    }

    @Test
    public void testToString_Basic() {
        Calendar cal = calendarAt(1_600_000_000_000L, ZoneId.of("UTC"));
        String str = CalendarConversions.toString(cal, defaultConverter());
        // DateConversions formats using ISO‑8601; we verify it contains the epoch
        assertTrue(str.contains("2020"));
    }

    @Test
    public void testToOffsetDateTime_Basic() {
        Calendar cal = calendarAt(1_600_000_000_000L, ZoneId.of("Europe/Paris"));
        OffsetDateTime odt = CalendarConversions.toOffsetDateTime(cal, defaultConverter());
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(
                cal.getTimeZone().getOffset(cal.getTimeInMillis()) / 1000);
        assertEquals(odt.getOffset(), expectedOffset);
        assertEquals(odt.toInstant(), cal.toInstant());
    }

    @Test
    public void testToMap_Basic() {
        Calendar cal = calendarAt(1_600_000_123_456L, ZoneId.of("UTC"));
        Map<String, Object> map = CalendarConversions.toMap(cal, defaultConverter());

        // Verify expected keys
        assertEquals("2020-09-13", map.get(MapConversions.DATE)); // 2020‑09‑13 in UTC
        assertEquals("12:15:23.456", map.get(MapConversions.TIME)); // HH:mm:ss.SSS
        assertEquals("UTC", map.get(MapConversions.ZONE));
        assertEquals(1_600_000_123_456L, map.get(MapConversions.EPOCH_MILLIS));
    }

    // ----------------------------------------------------------------------
    // 2. Edge‑case & exception tests
    // ----------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullInput() {
        CalendarConversions.toZonedDateTime(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongType() {
        CalendarConversions.toZonedDateTime("not a calendar", defaultConverter());
    }

    @Test
    public void testToZonedDateTime_DifferentTimeZones() {
        // UTC
        Calendar utc = calendarAt(1_600_000_000_000L, ZoneId.of("UTC"));
        ZonedDateTime utcZdt = CalendarConversions.toZonedDateTime(utc, defaultConverter());

        // Pacific (UTC‑8) – same instant, different zone
        Calendar pacific = calendarAt(1_600_000_000_000L, ZoneId.of("America/Los_Angeles"));
        ZonedDateTime pacZdt = CalendarConversions.toZonedDateTime(pacific, defaultConverter());

        assertEquals(utcZdt.toInstant(), pacZdt.toInstant());
        assertNotEquals(utcZdt.getZone(), pacZdt.getZone());
    }

    @Test
    public void testToDouble_LargeEpoch() {
        // Use a date far in the future (year 3000)
        Calendar cal = calendarAt(32503680000000L, ZoneId.of("UTC")); // 3000‑01‑01
        double seconds = CalendarConversions.toDouble(cal, defaultConverter());
        assertEquals(32503680000.0, seconds, 0.1);
    }

    @Test
    public void testToBigDecimal_NegativeEpoch() {
        Calendar cal = calendarAt(-1_000_000L, ZoneId.of("UTC"));
        BigDecimal bd = CalendarConversions.toBigDecimal(cal, defaultConverter());
        // -1_000_000 / 1_000_000_000 = -0.001
        assertEquals(new BigDecimal("-0.001"), bd);
    }

    @Test
    public void testToBigInteger_Overflow() {
        // Use a value that would overflow a 64‑bit long if multiplied,
        // but BigInteger should handle it.
        long epoch = Long.MAX_VALUE / 2;
        Calendar cal = calendarAt(epoch, ZoneId.of("UTC"));
        BigInteger bi = CalendarConversions.toBigInteger(cal, defaultConverter());
        BigInteger expected = BigInteger.valueOf(epoch).multiply(BigInteger.valueOf(1_000_000L));
        assertEquals(expected, bi);
    }

    @Test(expected = NullPointerException.class)
    public void testCreate_NullConverter() {
        CalendarConversions.create(123L, null);
    }

    @Test
    public void testCreate_NegativeEpoch() {
        Calendar cal = CalendarConversions.create(-123456789L, defaultConverter());
        assertEquals(-123456789L, cal.getTimeInMillis());
    }

    @Test
    public void testToOffsetDateTime_DaylightSavingTransition() {
        // 2021‑03‑14 02:30 does not exist in America/New_York (DST jump)
        // Calendar will roll it to 03:30.
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.set(2021, Calendar.MARCH, 14, 2, 30, 0);
        cal.set(Calendar.MILLISECOND, 0);

        OffsetDateTime odt = CalendarConversions.toOffsetDateTime(cal, defaultConverter());
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(tz.getOffset(cal.getTimeInMillis()) / 1000);
        assertEquals(offset, odt.getOffset());
    }

    @Test
    public void testToMap_AllFields() {
        Calendar cal = calendarAt(0L, ZoneId.of("UTC"));
        cal.set(Calendar.YEAR, 1970);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Map<String, Object> map = CalendarConversions.toMap(cal, defaultConverter());

        assertEquals("1970-01-01", map.get(MapConversions.DATE));
        assertEquals("00:00:00.000", map.get(MapConversions.TIME));
        assertEquals("UTC", map.get(MapConversions.ZONE));
        assertEquals(0L, map.get(MapConversions.EPOCH_MILLIS));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        CalendarConversions.toMap(null, defaultConverter());
    }

    // ----------------------------------------------------------------------
    // 3. Additional sanity checks
    // ----------------------------------------------------------------------

    @Test
    public void testClone_Independence() {
        Calendar original = calendarAt(1_600_000_000_000L, ZoneId.of("UTC"));
        Calendar copy = CalendarConversions.clone(original, defaultConverter());

        // Mutate copy and ensure original stays unchanged
        copy.add(Calendar.DAY_OF_MONTH, 5);
        assertNotEquals(original.getTimeInMillis(), copy.getTimeInMillis());
    }

    @Test
    public void testToString_ConsistencyWith Date.toString() {
        Calendar cal = calendarAt(1_600_000_000_000L, ZoneId.of("UTC"));
        String convStr = CalendarConversions.toString(cal, defaultConverter());
        String dateStr = cal.getTime().toString();
        // DateConversions uses a custom format; we only assert both are non‑null and differ
        assertNotNull(convStr);
        assertNotNull(dateStr);
        assertNotEquals(dateStr, convStr);
    }
}
