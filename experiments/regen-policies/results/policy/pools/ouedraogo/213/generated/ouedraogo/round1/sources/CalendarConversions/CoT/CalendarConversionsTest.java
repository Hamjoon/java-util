package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.DateUtilities;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.*;

import static org.junit.Assert.*;

public class CalendarConversionsTest {

    /* ----------------------------------------------------------------------
     * Helper methods
     * ---------------------------------------------------------------------- */

    /** Creates a {@link Calendar} set to the supplied epoch‑millis and time‑zone. */
    private static Calendar calendarOf(long epochMilli, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    /** Returns a {@link Converter} with default options (system zone). */
    private static Converter defaultConverter() {
        // The library provides a public constructor that accepts a ConverterOptions.
        // Using the no‑arg constructor of ConverterOptions gives the system defaults.
        return new Converter(new ConverterOptions());
    }

    /** Returns a {@link Converter} whose options use the supplied {@link ZoneId}. */
    private static Converter converterWithZone(ZoneId zoneId) {
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(zoneId);
        opts.setTimeZone(TimeZone.getTimeZone(zoneId));
        return new Converter(opts);
    }

    /* ----------------------------------------------------------------------
     * 1. List of public (package‑private static) methods in CalendarConversions
     * ---------------------------------------------------------------------- */

    // The signatures are extracted directly from the source code:
    // static Long               toLong(Object, Converter)
    // static AtomicLong         toAtomicLong(Object, Converter)
    // static double             toDouble(Object, Converter)
    // static BigDecimal         toBigDecimal(Object, Converter)
    // static BigInteger         toBigInteger(Object, Converter)
    // static Date               toDate(Object, Converter)
    // static java.sql.Date      toSqlDate(Object, Converter)
    // static Timestamp          toTimestamp(Object, Converter)
    // static Instant            toInstant(Object, Converter)
    // static ZonedDateTime      toZonedDateTime(Object, Converter)
    // static LocalDateTime      toLocalDateTime(Object, Converter)
    // static OffsetDateTime     toOffsetDateTime(Object, Converter)
    // static LocalDate          toLocalDate(Object, Converter)
    // static LocalTime          toLocalTime(Object, Converter)
    // static Calendar           clone(Object, Converter)
    // static Calendar           create(long, Converter)
    // static Year               toYear(Object, Converter)
    // static YearMonth          toYearMonth(Object, Converter)
    // static MonthDay           toMonthDay(Object, Converter)
    // static String             toString(Object, Converter)
    // static Map<String,Object> toMap(Object, Converter)

    /* ----------------------------------------------------------------------
     * 2. Basic functional tests for each method
     * ---------------------------------------------------------------------- */

    @Test
    public void testToLong() {
        Calendar cal = calendarOf(1_600_000_000_000L, TimeZone.getTimeZone("UTC"));
        Long result = CalendarConversions.toLong(cal, defaultConverter());
        assertEquals(cal.getTimeInMillis(), result.longValue());
    }

    @Test
    public void testToAtomicLong() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("UTC"));
        AtomicLong result = CalendarConversions.toAtomicLong(cal, defaultConverter());
        assertEquals(cal.getTimeInMillis(), result.get());
    }

    @Test
    public void testToDouble() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("UTC"));
        double result = CalendarConversions.toDouble(cal, defaultConverter());
        double expected = cal.getTimeInMillis() / 1000.0;
        assertEquals(expected, result, 0.0000001);
    }

    @Test
    public void testToBigDecimal() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("UTC"));
        BigDecimal result = CalendarConversions.toBigDecimal(cal, defaultConverter());

        // The conversion divides by BigDecimalConversions.GRAND (seconds in a millisecond = 1000)
        BigDecimal expected = new BigDecimal(cal.getTimeInMillis())
                .divide(BigDecimalConversions.GRAND);
        assertEquals(0, expected.compareTo(result));
    }

    @Test
    public void testToBigInteger() {
        Calendar cal = calendarOf(1_600_000_123L, TimeZone.getTimeZone("UTC"));
        BigInteger result = CalendarConversions.toBigInteger(cal, defaultConverter());
        BigInteger expected = BigInteger.valueOf(cal.getTimeInMillis() * 1_000_000L);
        assertEquals(expected, result);
    }

    @Test
    public void testToDate() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("UTC"));
        Date result = CalendarConversions.toDate(cal, defaultConverter());
        assertEquals(cal.getTime(), result);
    }

    @Test
    public void testToSqlDate() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("America/New_York"));
        Converter conv = converterWithZone(ZoneId.of("America/New_York"));
        java.sql.Date result = CalendarConversions.toSqlDate(cal, conv);
        LocalDate expected = cal.toInstant()
                .atZone(conv.getOptions().getZoneId())
                .toLocalDate();
        assertEquals(java.sql.Date.valueOf(expected), result);
    }

    @Test
    public void testToTimestamp() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("UTC"));
        Timestamp result = CalendarConversions.toTimestamp(cal, defaultConverter());
        assertEquals(cal.getTimeInMillis(), result.getTime());
    }

    @Test
    public void testToInstant() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("UTC"));
        Instant result = CalendarConversions.toInstant(cal, defaultConverter());
        assertEquals(cal.toInstant(), result);
    }

    @Test
    public void testToZonedDateTime() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("Europe/Paris"));
        ZonedDateTime result = CalendarConversions.toZonedDateTime(cal, defaultConverter());
        ZonedDateTime expected = cal.toInstant()
                .atZone(cal.getTimeZone().toZoneId());
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalDateTime() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("Europe/Paris"));
        LocalDateTime result = CalendarConversions.toLocalDateTime(cal, defaultConverter());
        LocalDateTime expected = CalendarConversions.toZonedDateTime(cal, defaultConverter())
                .toLocalDateTime();
        assertEquals(expected, result);
    }

    @Test
    public void testToOffsetDateTime() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("America/Los_Angeles"));
        OffsetDateTime result = CalendarConversions.toOffsetDateTime(cal, defaultConverter());

        ZoneOffset offset = ZoneOffset.ofTotalSeconds(
                cal.getTimeZone().getOffset(cal.getTimeInMillis()) / 1000);
        OffsetDateTime expected = cal.toInstant().atOffset(offset);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalDate() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("Asia/Tokyo"));
        LocalDate result = CalendarConversions.toLocalDate(cal, defaultConverter());
        LocalDate expected = CalendarConversions.toZonedDateTime(cal, defaultConverter())
                .toLocalDate();
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalTime() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("Asia/Tokyo"));
        LocalTime result = CalendarConversions.toLocalTime(cal, defaultConverter());
        LocalTime expected = CalendarConversions.toZonedDateTime(cal, defaultConverter())
                .toLocalTime();
        assertEquals(expected, result);
    }

    @Test
    public void testClone() {
        Calendar original = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("UTC"));
        Calendar copy = CalendarConversions.clone(original, defaultConverter());
        assertNotSame(original, copy);
        assertEquals(original.getTimeInMillis(), copy.getTimeInMillis());
        assertEquals(original.getTimeZone(), copy.getTimeZone());
    }

    @Test
    public void testCreate() {
        long epoch = 1_600_000_123_456L;
        Converter conv = converterWithZone(ZoneId.of("UTC"));
        Calendar cal = CalendarConversions.create(epoch, conv);
        assertEquals(epoch, cal.getTimeInMillis());
        assertEquals(conv.getOptions().getTimeZone(), cal.getTimeZone());
    }

    @Test
    public void testToYear() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("UTC"));
        Converter conv = converterWithZone(ZoneId.of("UTC"));
        Year result = CalendarConversions.toYear(cal, conv);
        Year expected = Year.from(cal.toInstant().atZone(conv.getOptions().getZoneId()).toLocalDate());
        assertEquals(expected, result);
    }

    @Test
    public void testToYearMonth() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("UTC"));
        Converter conv = converterWithZone(ZoneId.of("UTC"));
        YearMonth result = CalendarConversions.toYearMonth(cal, conv);
        YearMonth expected = YearMonth.from(cal.toInstant().atZone(conv.getOptions().getZoneId()).toLocalDate());
        assertEquals(expected, result);
    }

    @Test
    public void testToMonthDay() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("UTC"));
        Converter conv = converterWithZone(ZoneId.of("UTC"));
        MonthDay result = CalendarConversions.toMonthDay(cal, conv);
        MonthDay expected = MonthDay.from(cal.toInstant().atZone(conv.getOptions().getZoneId()).toLocalDate());
        assertEquals(expected, result);
    }

    @Test
    public void testToString_WithZoneOffset() {
        // UTC is a ZoneOffset (zero offset)
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("UTC"));
        Converter conv = defaultConverter();
        String result = CalendarConversions.toString(cal, conv);

        ZonedDateTime zdt = cal.toInstant().atZone(ZoneOffset.UTC);
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .appendPattern("XXX")
                .toFormatter();

        assertEquals(fmt.format(zdt), result);
    }

    @Test
    public void testToString_WithRegionZone() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("Europe/Paris"));
        Converter conv = defaultConverter();
        String result = CalendarConversions.toString(cal, conv);

        ZonedDateTime zdt = cal.toInstant().atZone(ZoneId.of("Europe/Paris"));
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .appendPattern("XXX'['VV']'")
                .toFormatter();

        assertEquals(fmt.format(zdt), result);
    }

    @Test
    public void testToString_AbbreviationMapping() {
        // "EST" is an abbreviation that maps to "America/New_York"
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("EST"));
        Converter conv = defaultConverter();

        // Ensure the map contains the expected entry (defensive, not a test of CalendarConversions)
        assertTrue(DateUtilities.ABBREVIATION_TO_TIMEZONE.containsKey("EST"));

        String result = CalendarConversions.toString(cal, conv);

        // Expected zone after mapping
        ZoneId mappedZone = ZoneId.of(DateUtilities.ABBREVIATION_TO_TIMEZONE.get("EST"));
        ZonedDateTime expectedZdt = cal.toInstant().atZone(mappedZone);
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .appendPattern("XXX'['VV']'")
                .toFormatter();

        assertEquals(fmt.format(expectedZdt), result);
    }

    @Test
    public void testToMap() {
        Calendar cal = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("UTC"));
        Converter conv = defaultConverter();
        Map<String, Object> map = CalendarConversions.toMap(cal, conv);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.CALENDAR));
        assertEquals(CalendarConversions.toString(cal, conv), map.get(MapConversions.CALENDAR));
    }

    /* ----------------------------------------------------------------------
     * 3. Edge‑case & exception tests
     * ---------------------------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToLong_NonCalendarThrows() {
        CalendarConversions.toLong("not a calendar", defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_NullInputThrows() {
        CalendarConversions.toDouble(null, defaultConverter());
    }

    @Test
    public void testToString_FractionalSecondsPresent() {
        // Create a Calendar with nanosecond precision via a LocalDateTime that has nanos.
        LocalDateTime ldt = LocalDateTime.of(2020, 9, 13, 12, 34, 56, 123_456_789);
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("UTC"));
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(zdt.toInstant().toEpochMilli());

        // Manually add the nanos that are not stored in the Calendar (they are lost),
        // so we simulate by using a ZonedDateTime directly for expected format.
        String result = CalendarConversions.toString(cal, defaultConverter());

        // Expected format must contain the fractional part because the original
        // Instant had nanos, but Calendar truncates to milliseconds, therefore the
        // formatted string will contain only the millisecond fraction (i.e., 123 ms -> .123)
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .appendPattern("XXX")
                .toFormatter();

        ZonedDateTime expected = cal.toInstant().atZone(ZoneOffset.UTC);
        assertEquals(fmt.format(expected), result);
    }

    @Test
    public void testCreate_ZeroEpoch() {
        Converter conv = converterWithZone(ZoneId.of("UTC"));
        Calendar cal = CalendarConversions.create(0L, conv);
        assertEquals(0L, cal.getTimeInMillis());
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test
    public void testClone_Independence() {
        Calendar original = calendarOf(1_600_000_123_456L, TimeZone.getTimeZone("UTC"));
        Calendar clone = CalendarConversions.clone(original, defaultConverter());

        // Change the original and verify the clone does NOT change.
        original.setTimeInMillis(2_000_000_000_000L);
        assertNotEquals(original.getTimeInMillis(), clone.getTimeInMillis());
    }

    @Test
    public void testToString_NullCalendarThrows() {
        try {
            CalendarConversions.toString(null, defaultConverter());
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }
}
