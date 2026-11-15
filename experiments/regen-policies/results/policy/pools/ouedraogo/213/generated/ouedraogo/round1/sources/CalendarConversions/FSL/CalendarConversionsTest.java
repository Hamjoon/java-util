package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;

import org.junit.Test;

/**
 * Unit tests for {@link CalendarConversions}.  The tests use only the public
 * constructors and methods that are visible in the source code supplied for the
 * class under test and the two dependency APIs that were listed in the
 * prompt.  No external mocking frameworks are used.
 */
public class CalendarConversionsTest {

    /** Helper that creates a {@link Calendar} fixed at the given epoch milli
     *  in the supplied {@link TimeZone}. */
    private static Calendar calendarAt(long epochMilli, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    /** A {@link Converter} with default options.  The library supplies a
     *  no‑arg constructor that creates a {@link ConverterOptions} instance
     *  using the JVM defaults (system time‑zone, etc.).  Those defaults are
     *  sufficient for the majority of the conversion methods. */
    private static Converter defaultConverter() {
        return new Converter(new ConverterOptions());
    }

    @Test
    public void testToLong_andToAtomicLong() {
        Calendar cal = calendarAt(1_600_000_000_123L, TimeZone.getTimeZone("UTC"));
        Converter conv = defaultConverter();

        Long l = CalendarConversions.toLong(cal, conv);
        assertEquals(Long.valueOf(1_600_000_000_123L), l);

        AtomicLong al = CalendarConversions.toAtomicLong(cal, conv);
        assertEquals(1_600_000_000_123L, al.get());
    }

    @Test
    public void testToDouble() {
        Calendar cal = calendarAt(1_234_567_890L, TimeZone.getTimeZone("UTC"));
        double d = CalendarConversions.toDouble(cal, defaultConverter());
        // epoch / 1000.0
        assertEquals(1_234_567.89, d, 0.000_001);
    }

    @Test
    public void testToBigDecimal() throws Exception {
        Calendar cal = calendarAt(1_000_000_000L, TimeZone.getTimeZone("UTC"));
        // The divisor is BigDecimalConversions.GRAND – obtain it via reflection.
        Class<?> bigDecimalConvClass = Class.forName("com.cedarsoftware.util.convert.BigDecimalConversions");
        java.lang.reflect.Field grandField = bigDecimalConvClass.getDeclaredField("GRAND");
        grandField.setAccessible(true);
        BigDecimal divisor = (BigDecimal) grandField.get(null);

        BigDecimal bd = CalendarConversions.toBigDecimal(cal, defaultConverter());
        BigDecimal expected = new BigDecimal(1_000_000_000L).divide(divisor);
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToBigInteger() {
        Calendar cal = calendarAt(2_000L, TimeZone.getTimeZone("UTC"));
        BigInteger bi = CalendarConversions.toBigInteger(cal, defaultConverter());
        // epoch millis * 1_000_000 = nanoseconds since epoch
        assertEquals(BigInteger.valueOf(2_000L * 1_000_000L), bi);
    }

    @Test
    public void testToDate_andSqlDate_andTimestamp() {
        long epoch = 1_600_000_123_456L; // 2020‑09‑13T12:15:23.456Z
        Calendar cal = calendarAt(epoch, TimeZone.getTimeZone("UTC"));
        Converter conv = defaultConverter();

        Date date = CalendarConversions.toDate(cal, conv);
        assertEquals(epoch, date.getTime());

        java.sql.Date sqlDate = CalendarConversions.toSqlDate(cal, conv);
        // Only the date part matters for java.sql.Date
        assertEquals(LocalDate.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC), sqlDate.toLocalDate());

        Timestamp ts = CalendarConversions.toTimestamp(cal, conv);
        assertEquals(epoch, ts.getTime());
        assertEquals(456_000_000, ts.getNanos()); // nanoseconds fraction preserved
    }

    @Test
    public void testToInstant_andZonedDateTime_andLocalDateTime() {
        long epoch = 1_600_000_123_456L;
        Calendar cal = calendarAt(epoch, TimeZone.getTimeZone("America/New_York"));
        Converter conv = defaultConverter();

        Instant instant = CalendarConversions.toInstant(cal, conv);
        assertEquals(Instant.ofEpochMilli(epoch), instant);

        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(cal, conv);
        assertEquals(ZoneId.of("America/New_York"), zdt.getZone());
        assertEquals(instant, zdt.toInstant());

        LocalDateTime ldt = CalendarConversions.toLocalDateTime(cal, conv);
        assertEquals(zdt.toLocalDateTime(), ldt);
    }

    @Test
    public void testOffsetDateTime() {
        long epoch = 1_600_000_123_456L; // 2020‑09‑13T12:15:23.456Z
        // Use a time‑zone with a non‑zero offset (e.g., Asia/Tokyo = +09:00)
        Calendar cal = calendarAt(epoch, TimeZone.getTimeZone("Asia/Tokyo"));
        Converter conv = defaultConverter();

        OffsetDateTime odt = CalendarConversions.toOffsetDateTime(cal, conv);
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(
                cal.getTimeZone().getOffset(epoch) / 1000);
        assertEquals(expectedOffset, odt.getOffset());
        assertEquals(Instant.ofEpochMilli(epoch), odt.toInstant());
    }

    @Test
    public void testToLocalDate_andLocalTime() {
        long epoch = 1_600_000_123_456L;
        Calendar cal = calendarAt(epoch, TimeZone.getTimeZone("Europe/Paris"));
        Converter conv = defaultConverter();

        LocalDate ld = CalendarConversions.toLocalDate(cal, conv);
        LocalTime lt = CalendarConversions.toLocalTime(cal, conv);

        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(cal, conv);
        assertEquals(zdt.toLocalDate(), ld);
        assertEquals(zdt.toLocalTime(), lt);
    }

    @Test
    public void testClone() {
        Calendar original = calendarAt(12345L, TimeZone.getTimeZone("UTC"));
        Calendar copy = CalendarConversions.clone(original, defaultConverter());
        assertNotSame(original, copy);
        assertEquals(original.getTimeInMillis(), copy.getTimeInMillis());
        assertEquals(original.getTimeZone(), copy.getTimeZone());
    }

    @Test
    public void testCreate() {
        long epoch = 9_876_543_210L;
        Converter conv = defaultConverter();

        Calendar created = CalendarConversions.create(epoch, conv);
        assertEquals(epoch, created.getTimeInMillis());
        // The time‑zone of the created calendar should be the one supplied by the
        // ConverterOptions (system default).  We only verify that it is non‑null.
        assertNotNull(created.getTimeZone());
    }

    @Test
    public void testToYear_andYearMonth_andMonthDay() {
        // 2022‑03‑15
        Calendar cal = calendarAt(1647302400000L, TimeZone.getTimeZone("UTC"));
        Converter conv = defaultConverter();

        Year year = CalendarConversions.toYear(cal, conv);
        assertEquals(2022, year.getValue());

        YearMonth ym = CalendarConversions.toYearMonth(cal, conv);
        assertEquals(2022, ym.getYear());
        assertEquals(Month.MARCH, ym.getMonth());

        MonthDay md = CalendarConversions.toMonthDay(cal, conv);
        assertEquals(Month.MARCH, md.getMonth());
        assertEquals(15, md.getDayOfMonth());
    }

    @Test
    public void testToString_withOffsetZone() {
        // Use UTC (ZoneOffset) – no '/' in the zone id.
        Calendar cal = calendarAt(1_600_000_123_456L, TimeZone.getTimeZone("UTC"));
        Converter conv = defaultConverter();

        String txt = CalendarConversions.toString(cal, conv);
        // Expected format: 2020‑09‑13T12:15:23.456Z
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .appendPattern("XXX")
                .toFormatter()
                .withZone(ZoneOffset.UTC);
        String expected = fmt.format(Instant.ofEpochMilli(1_600_000_123_456L));
        assertEquals(expected, txt);
    }

    @Test
    public void testToString_withRegionZone() {
        // Use a region based zone – contains '/'.
        Calendar cal = calendarAt(1_600_000_123_456L, TimeZone.getTimeZone("America/Los_Angeles"));
        Converter conv = defaultConverter();

        String txt = CalendarConversions.toString(cal, conv);
        // Expected pattern: 2020‑09‑13T05:15:23.456-07:00[America/Los_Angeles]
        ZonedDateTime zdt = cal.toInstant().atZone(ZoneId.of("America/Los_Angeles"));
        DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .appendPattern("XXX'['VV']'")
                .toFormatter()
                .withZone(zdt.getZone());
        String expected = fmt.format(zdt);
        assertEquals(expected, txt);
    }

    @Test
    public void testToMap() {
        Calendar cal = calendarAt(0L, TimeZone.getTimeZone("UTC"));
        Converter conv = defaultConverter();

        Map<String, Object> map = CalendarConversions.toMap(cal, conv);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.CALENDAR));
        assertEquals(CalendarConversions.toString(cal, conv), map.get(MapConversions.CALENDAR));
    }
}
