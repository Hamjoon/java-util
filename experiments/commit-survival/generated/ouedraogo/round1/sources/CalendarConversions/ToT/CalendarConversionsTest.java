package com.cedarsoftware.util.convert;

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

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link CalendarConversions}.
 *
 * <p>
 * The tests are written by three imagined experts, each contributing a
 * different perspective (typical usage, edge‑case handling and error
 * scenarios).  All tests use only the public API that is visible in the
 * source code supplied for {@code CalendarConversions} and the public
 * signatures of {@code Converter}, {@code ConverterOptions},
 * {@code CompactLinkedMap} and related classes.
 * </p>
 *
 * <p>
 * The {@code Converter} instance is required only for the {@code create},
 * {@code toString} and {@code toMap} methods.  A minimal {@code
 * ConverterOptions} implementation is provided that supplies a
 * configurable {@link TimeZone}.  This avoids the need for any mocking
 * framework while keeping the tests self‑contained.
 * </p>
 */
public class CalendarConversionsTest {

    /** Fixed moment used by most tests – 2022‑03‑14 15:09:26.123 UTC */
    private static final long FIXED_EPOCH_MILLIS = 1647274166123L;
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone PST = TimeZone.getTimeZone("America/Los_Angeles");

    private Calendar utcCalendar;
    private Calendar pstCalendar;
    private Converter utcConverter;
    private Converter pstConverter;

    /**
     * Minimal {@code ConverterOptions} implementation needed for
     * {@link Converter#getOptions()}.
     */
    private static final class SimpleConverterOptions {
        private final TimeZone timeZone;

        SimpleConverterOptions(TimeZone timeZone) {
            this.timeZone = timeZone;
        }

        public TimeZone getTimeZone() {
            return timeZone;
        }
    }

    /**
     * Simple wrapper that satisfies the {@code Converter} constructor.
     * The real {@code Converter} class is {@code final}, therefore we
     * cannot subclass it.  Instead we rely on the public constructor that
     * accepts a {@code ConverterOptions} object.  Because the source of
     * {@code ConverterOptions} is not shown, we use reflection to set the
     * required {@code timeZone} field – this works for the current library
     * version and keeps the test free from external dependencies.
     */
    private static Converter buildConverter(TimeZone tz) {
        try {
            // The real ConverterOptions class is assumed to have a constructor
            // that takes a TimeZone or a setter for it.  If that is not the
            // case, reflection can still inject the field.
            Class<?> optionsClass = Class.forName("com.cedarsoftware.util.convert.ConverterOptions");
            Object optionsInstance = optionsClass.getDeclaredConstructor().newInstance();

            // Try a setter first …
            try {
                optionsClass.getMethod("setTimeZone", TimeZone.class).invoke(optionsInstance, tz);
            } catch (NoSuchMethodException ignored) {
                // …otherwise set the private field directly.
                java.lang.reflect.Field tzField = optionsClass.getDeclaredField("timeZone");
                tzField.setAccessible(true);
                tzField.set(optionsInstance, tz);
            }

            return new Converter((ConverterOptions) optionsInstance);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create Converter for tests", e);
        }
    }

    @Before
    public void setUp() {
        utcCalendar = Calendar.getInstance(UTC);
        utcCalendar.setTimeInMillis(FIXED_EPOCH_MILLIS);
        utcCalendar.setLenient(false);

        pstCalendar = Calendar.getInstance(PST);
        pstCalendar.setTimeInMillis(FIXED_EPOCH_MILLIS);
        pstCalendar.setLenient(false);

        utcConverter = buildConverter(UTC);
        pstConverter = buildConverter(PST);
    }

    // -----------------------------------------------------------------
    // 1. Typical usage tests
    // -----------------------------------------------------------------

    @Test
    public void testToZonedDateTime_Utc() {
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(utcCalendar, utcConverter);
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
        assertEquals(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), zdt.toInstant());
    }

    @Test
    public void testToZonedDateTime_Pst() {
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(pstCalendar, pstConverter);
        assertEquals(PST.toZoneId(), zdt.getZone());
        assertEquals(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), zdt.toInstant());
    }

    @Test
    public void testToLong() {
        Long epoch = CalendarConversions.toLong(utcCalendar, utcConverter);
        assertEquals(FIXED_EPOCH_MILLIS, (long) epoch);
    }

    @Test
    public void testToDouble() {
        double seconds = CalendarConversions.toDouble(utcCalendar, utcConverter);
        double expected = FIXED_EPOCH_MILLIS / 1000.0;
        assertEquals(expected, seconds, 0.000001);
    }

    @Test
    public void testToDate() {
        Date date = CalendarConversions.toDate(utcCalendar, utcConverter);
        assertEquals(new Date(FIXED_EPOCH_MILLIS), date);
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = CalendarConversions.toSqlDate(utcCalendar, utcConverter);
        assertEquals(new java.sql.Date(FIXED_EPOCH_MILLIS), sqlDate);
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = CalendarConversions.toTimestamp(utcCalendar, utcConverter);
        assertEquals(FIXED_EPOCH_MILLIS, ts.getTime());
        // Nanos part should be zero because Calendar only stores millis.
        assertEquals(0, ts.getNanos() % 1_000_000);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong atomic = CalendarConversions.toAtomicLong(utcCalendar, utcConverter);
        assertEquals(FIXED_EPOCH_MILLIS, atomic.get());
    }

    @Test
    public void testToInstant() {
        Instant instant = CalendarConversions.toInstant(utcCalendar, utcConverter);
        assertEquals(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), instant);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = CalendarConversions.toLocalDateTime(utcCalendar, utcConverter);
        assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), ZoneOffset.UTC), ldt);
    }

    @Test
    public void testToLocalDate() {
        LocalDate ld = CalendarConversions.toLocalDate(utcCalendar, utcConverter);
        assertEquals(LocalDate.ofInstant(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), ZoneOffset.UTC), ld);
    }

    @Test
    public void testToLocalTime() {
        LocalTime lt = CalendarConversions.toLocalTime(utcCalendar, utcConverter);
        assertEquals(LocalTime.ofInstant(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), ZoneOffset.UTC), lt);
    }

    @Test
    public void testToBigDecimal() {
        // The constant used by the library – 1000 (i.e. milliseconds → seconds)
        BigDecimal expected = new BigDecimal(FIXED_EPOCH_MILLIS).divide(BigDecimalConversions.GRAND);
        BigDecimal actual = CalendarConversions.toBigDecimal(utcCalendar, utcConverter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToBigInteger() {
        BigInteger expected = BigInteger.valueOf(FIXED_EPOCH_MILLIS * 1_000_000L);
        BigInteger actual = CalendarConversions.toBigInteger(utcCalendar, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testClone() {
        Calendar cloned = CalendarConversions.clone(utcCalendar, utcConverter);
        assertNotSame(utcCalendar, cloned);
        assertEquals(utcCalendar.getTimeInMillis(), cloned.getTimeInMillis());
        assertEquals(utcCalendar.getTimeZone(), cloned.getTimeZone());
    }

    @Test
    public void testCreate() {
        Calendar created = CalendarConversions.create(FIXED_EPOCH_MILLIS, utcConverter);
        assertEquals(FIXED_EPOCH_MILLIS, created.getTimeInMillis());
        assertEquals(UTC, created.getTimeZone());
    }

    @Test
    public void testToString() {
        // DateConversions.toString formats the date using the converter’s
        // locale/formatting options.  For the purpose of this test we only
        // verify that the method delegates and does not throw.
        String s = CalendarConversions.toString(utcCalendar, utcConverter);
        assertNotNull(s);
        // The string must contain the year part of the fixed instant.
        assertTrue(s.contains("2022"));
    }

    @Test
    public void testToOffsetDateTime_Utc() {
        OffsetDateTime odt = CalendarConversions.toOffsetDateTime(utcCalendar, utcConverter);
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(UTC.getOffset(FIXED_EPOCH_MILLIS) / 1000);
        assertEquals(offset, odt.getOffset());
        assertEquals(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), odt.toInstant());
    }

    @Test
    public void testToOffsetDateTime_Pst() {
        OffsetDateTime odt = CalendarConversions.toOffsetDateTime(pstCalendar, pstConverter);
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(PST.getOffset(FIXED_EPOCH_MILLIS) / 1000);
        assertEquals(offset, odt.getOffset());
        assertEquals(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), odt.toInstant());
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = CalendarConversions.toMap(utcCalendar, utcConverter);
        assertEquals(4, map.size());

        // DATE
        String expectedDate = LocalDate.of(2022, 3, 14).toString();
        assertEquals(expectedDate, map.get(MapConversions.DATE));

        // TIME – note that Calendar.MILLISECOND is 123, which becomes 123_000_000 ns
        String expectedTime = LocalTime.of(15, 9, 26, 123_000_000).toString();
        assertEquals(expectedTime, map.get(MapConversions.TIME));

        // ZONE
        assertEquals("UTC", map.get(MapConversions.ZONE));

        // EPOCH_MILLIS
        assertEquals(FIXED_EPOCH_MILLIS, map.get(MapConversions.EPOCH_MILLIS));
    }

    // -----------------------------------------------------------------
    // 2. Edge‑case tests
    // -----------------------------------------------------------------

    @Test
    public void testToZonedDateTime_DaylightSavingTransition() {
        // 2021‑03‑14 02:30:00 in America/New_York does not exist (DST jump).
        TimeZone ny = TimeZone.getTimeZone("America/New_York");
        Calendar cal = Calendar.getInstance(ny);
        cal.set(2021, Calendar.MARCH, 14, 2, 30, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // Force the calendar to resolve the illegal time by adding an hour.
        cal.setLenient(false);
        try {
            cal.getTime(); // will throw an exception if strict
            fail("Expected IllegalArgumentException due to DST gap");
        } catch (IllegalArgumentException ignored) {
            // Expected – we use a lenient calendar to continue.
        }
        cal.setLenient(true);
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(cal, buildConverter(ny));
        // Expected instant is 2021‑03‑14 03:30:00‑04:00 (after the gap)
        ZonedDateTime expected = ZonedDateTime.of(
                LocalDateTime.of(2021, 3, 14, 3, 30, 0),
                ZoneId.of("America/New_York"));
        assertEquals(expected, zdt);
    }

    @Test
    public void testToBigDecimal_NegativeEpoch() {
        Calendar cal = Calendar.getInstance(UTC);
        cal.setTimeInMillis(-123456789L); // before 1970
        BigDecimal expected = new BigDecimal(-123456789L).divide(BigDecimalConversions.GRAND);
        assertEquals(0, expected.compareTo(CalendarConversions.toBigDecimal(cal, utcConverter)));
    }

    @Test
    public void testCreate_WithNonUtcTimezone() {
        Calendar cal = CalendarConversions.create(FIXED_EPOCH_MILLIS, pstConverter);
        assertEquals(PST, cal.getTimeZone());
        assertEquals(FIXED_EPOCH_MILLIS, cal.getTimeInMillis());
    }

    // -----------------------------------------------------------------
    // 3. Error‑scenario tests
    // -----------------------------------------------------------------

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WithInvalidObject() {
        CalendarConversions.toZonedDateTime("not a calendar", utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WithInvalidObject() {
        CalendarConversions.toLong(42, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullInput() {
        CalendarConversions.toZonedDateTime(null, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testCreate_NullConverter() {
        CalendarConversions.create(FIXED_EPOCH_MILLIS, null);
    }

    @Test
    public void testClone_IndependenceAfterMutation() {
        Calendar original = Calendar.getInstance(UTC);
        original.setTimeInMillis(FIXED_EPOCH_MILLIS);
        Calendar cloned = CalendarConversions.clone(original, utcConverter);
        cloned.add(Calendar.DAY_OF_MONTH, 5); // mutate clone
        assertNotEquals(original.getTimeInMillis(), cloned.getTimeInMillis());
    }

    @Test
    public void testToMap_ContainsAllExpectedKeys() {
        Map<String, Object> map = CalendarConversions.toMap(pstCalendar, pstConverter);
        assertTrue(map.containsKey(MapConversions.DATE));
        assertTrue(map.containsKey(MapConversions.TIME));
        assertTrue(map.containsKey(MapConversions.ZONE));
        assertTrue(map.containsKey(MapConversions.EPOCH_MILLIS));
    }

    @Test
    public void testToString_NullCalendar() {
        try {
            CalendarConversions.toString(null, utcConverter);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
            // expected
        }
    }
}
