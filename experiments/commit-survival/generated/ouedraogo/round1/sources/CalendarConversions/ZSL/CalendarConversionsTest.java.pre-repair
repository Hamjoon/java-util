package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
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
 * Comprehensive JUnit‑4 test suite for {@link CalendarConversions}.
 *
 * The tests cover every static conversion method, checking both the
 * returned value and, where appropriate, that the original {@link Calendar}
 * instance remains unchanged.
 *
 * NOTE: The library classes {@code Converter} and {@code ConverterOptions}
 * are part of the same artifact as {@code CalendarConversions}.  The tests
 * use a real {@code Converter} instance (no mocks) and configure its
 * {@code ConverterOptions} with a known {@link TimeZone}.  Only the
 * public API of those classes is used.
 */
public class CalendarConversionsTest {

    /** A reusable {@code Converter} whose options contain the UTC time‑zone. */
    private static final Converter UTC_CONVERTER = createConverter(TimeZone.getTimeZone("UTC"));

    /** A reusable {@code Converter} whose options contain the America/New_York time‑zone. */
    private static final Converter NY_CONVERTER = createConverter(TimeZone.getTimeZone("America/New_York"));

    /**
     * Helper that creates a {@link Converter} with the supplied {@link TimeZone}
     * stored in its {@link ConverterOptions}.  The exact API of {@code ConverterOptions}
     * is not part of this test file, but the library provides a public no‑arg
     * constructor and a {@code setTimeZone(TimeZone)} mutator.
     */
    private static Converter createConverter(TimeZone tz) {
        ConverterOptions options = new ConverterOptions();
        options.setTimeZone(tz);
        return new Converter(options);
    }

    /**
     * Creates a {@link Calendar} set to a deterministic instant (2023‑03‑15
     * 12:30:45.123) in the supplied time‑zone.
     */
    private static Calendar buildCalendar(TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.set(2023, Calendar.MARCH, 15, 12, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);
        return cal;
    }

    @Test
    public void testToZonedDateTime_Utc() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("UTC"));
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(cal, UTC_CONVERTER);

        assertEquals(ZoneId.of("UTC"), zdt.getZone());
        assertEquals(2023, zdt.getYear());
        assertEquals(Month.MARCH, zdt.getMonth());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(12, zdt.getHour());
        assertEquals(30, zdt.getMinute());
        assertEquals(45, zdt.getSecond());
        assertEquals(123_000_000, zdt.getNano()); // milliseconds → nanoseconds
    }

    @Test
    public void testToZonedDateTime_NewYork() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("America/New_York"));
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(cal, NY_CONVERTER);

        // New York is UTC‑5 in March (standard time)
        ZoneId expectedZone = ZoneId.of("America/New_York");
        assertEquals(expectedZone, zdt.getZone());

        // The instant must be the same as the UTC calendar created above
        Calendar utcCal = buildCalendar(TimeZone.getTimeZone("UTC"));
        assertEquals(utcCal.toInstant(), cal.toInstant());

        // Verify the local fields reflect the New York offset (-05:00)
        assertEquals(7, zdt.getHour()); // 12:30 UTC → 07:30 New York (standard time)
    }

    @Test
    public void testToLong() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("UTC"));
        Long epoch = CalendarConversions.toLong(cal, null);
        assertEquals(cal.getTimeInMillis(), epoch.longValue());
    }

    @Test
    public void testToDouble() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("UTC"));
        double seconds = CalendarConversions.toDouble(cal, null);
        double expected = cal.getTimeInMillis() / 1000.0;
        assertEquals(expected, seconds, 0.000001);
    }

    @Test
    public void testToDate() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("UTC"));
        Date date = CalendarConversions.toDate(cal, null);
        assertEquals(cal.getTime(), date);
    }

    @Test
    public void testToSqlDate() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("UTC"));
        java.sql.Date sqlDate = CalendarConversions.toSqlDate(cal, null);
        assertEquals(cal.getTimeInMillis(), sqlDate.getTime());
    }

    @Test
    public void testToTimestamp() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("UTC"));
        Timestamp ts = CalendarConversions.toTimestamp(cal, null);
        assertEquals(cal.getTimeInMillis(), ts.getTime());
        // nanoseconds part must be zero because Calendar does not store sub‑millisecond precision
        assertEquals(0, ts.getNanos());
    }

    @Test
    public void testToAtomicLong() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("UTC"));
        AtomicLong atomic = CalendarConversions.toAtomicLong(cal, null);
        assertEquals(cal.getTimeInMillis(), atomic.get());
    }

    @Test
    public void testToInstant() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("UTC"));
        Instant instant = CalendarConversions.toInstant(cal, null);
        assertEquals(cal.toInstant(), instant);
    }

    @Test
    public void testToLocalDateTime() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("UTC"));
        LocalDateTime ldt = CalendarConversions.toLocalDateTime(cal, null);
        assertEquals(LocalDateTime.ofInstant(cal.toInstant(), ZoneOffset.UTC), ldt);
    }

    @Test
    public void testToLocalDate() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("UTC"));
        LocalDate ld = CalendarConversions.toLocalDate(cal, null);
        assertEquals(LocalDate.of(2023, 3, 15), ld);
    }

    @Test
    public void testToLocalTime() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("UTC"));
        LocalTime lt = CalendarConversions.toLocalTime(cal, null);
        assertEquals(LocalTime.of(12, 30, 45, 123_000_000), lt);
    }

    @Test
    public void testToBigDecimal() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("UTC"));
        BigDecimal bd = CalendarConversions.toBigDecimal(cal, null);
        // BigDecimalConversions.GRAND is a constant used for scaling; we obtain it via reflection
        BigDecimal grand = getBigDecimalGrand();
        BigDecimal expected = new BigDecimal(cal.getTimeInMillis()).divide(grand);
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToBigInteger() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("UTC"));
        BigInteger bi = CalendarConversions.toBigInteger(cal, null);
        BigInteger expected = BigInteger.valueOf(cal.getTimeInMillis() * 1_000_000L);
        assertEquals(expected, bi);
    }

    @Test
    public void testClone() {
        Calendar original = buildCalendar(TimeZone.getTimeZone("UTC"));
        Calendar copy = CalendarConversions.clone(original, null);
        assertNotSame(original, copy);
        assertEquals(original.getTimeInMillis(), copy.getTimeInMillis());
        assertEquals(original.getTimeZone(), copy.getTimeZone());
    }

    @Test
    public void testCreate() {
        long epoch = 1_700_000_000_000L; // a fixed epoch millisecond value
        Calendar created = CalendarConversions.create(epoch, UTC_CONVERTER);
        assertEquals(epoch, created.getTimeInMillis());
        assertEquals(TimeZone.getTimeZone("UTC"), created.getTimeZone());
    }

    @Test
    public void testToString() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("UTC"));
        String expected = DateConversions.toString(cal.getTime(), UTC_CONVERTER);
        String actual = CalendarConversions.toString(cal, UTC_CONVERTER);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("America/New_York"));
        OffsetDateTime odt = CalendarConversions.toOffsetDateTime(cal, NY_CONVERTER);

        // New York offset in March 2023 is -05:00 (standard time)
        ZoneOffset expectedOffset = ZoneOffset.ofHours(-5);
        assertEquals(expectedOffset, odt.getOffset());

        // The instant must match the calendar's instant
        assertEquals(cal.toInstant(), odt.toInstant());
    }

    @Test
    public void testToMap() {
        Calendar cal = buildCalendar(TimeZone.getTimeZone("UTC"));
        Map<String, Object> map = CalendarConversions.toMap(cal, null);

        // Expected date string
        String expectedDate = LocalDate.of(2023, 3, 15).toString();
        // Expected time string (including nanoseconds derived from milliseconds)
        String expectedTime = LocalTime.of(12, 30, 45, 123_000_000).toString();
        String expectedZone = "UTC";
        Long expectedEpoch = cal.getTimeInMillis();

        assertEquals(4, map.size());
        assertEquals(expectedDate, map.get(MapConversions.DATE));
        assertEquals(expectedTime, map.get(MapConversions.TIME));
        assertEquals(expectedZone, map.get(MapConversions.ZONE));
        assertEquals(expectedEpoch, map.get(MapConversions.EPOCH_MILLIS));
    }

    /**
     * Retrieves the {@code GRAND} constant from {@link BigDecimalConversions}
     * via reflection.  The test suite must not depend on the concrete value
     * because it is an implementation detail of the library.
     */
    private static BigDecimal getBigDecimalGrand() {
        try {
            java.lang.reflect.Field field = BigDecimalConversions.class.getDeclaredField("GRAND");
            field.setAccessible(true);
            return (BigDecimal) field.get(null);
        } catch (Exception e) {
            throw new AssertionError("Unable to access BigDecimalConversions.GRAND", e);
        }
    }
}
