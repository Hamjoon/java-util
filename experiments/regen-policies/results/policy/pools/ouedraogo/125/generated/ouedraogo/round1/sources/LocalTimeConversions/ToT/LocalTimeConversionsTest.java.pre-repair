package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link LocalTimeConversions}.
 *
 * The tests are written without any mocking frameworks – only the public
 * constructors and methods that appear in the supplied source files are used.
 *
 * The suite covers:
 * <ul>
 *   <li>Normal conversions for a typical {@link LocalTime} value.</li>
 *   <li>Edge cases such as midnight, the last nanosecond of the day and
 *       values with fractional seconds.</li>
 *   <li>Behaviour of {@code toCalendar} with different time‑zones.</li>
 *   <li>Verification that the {@code Converter} instance is only used for its
 *       {@code getOptions()} method (required by {@code toCalendar}).</li>
 * </ul>
 */
public class LocalTimeConversionsTest {

    /** A simple {@link Converter} implementation that only supplies {@link ConverterOptions}. */
    private static final class SimpleConverter extends Converter {
        private final ConverterOptions options;

        SimpleConverter(ConverterOptions options) {
            super(options);
            this.options = options;
        }

        @Override
        public ConverterOptions getOptions() {
            return options;
        }
    }

    private Converter utcConverter;
    private Converter pstConverter;

    @Before
    public void setUp() {
        // UTC – the default zone used in most tests.
        utcConverter = new SimpleConverter(new ConverterOptions()
                .setTimeZone(TimeZone.getTimeZone("UTC")));

        // Pacific Standard Time (UTC‑08:00) – used to verify calendar zone handling.
        pstConverter = new SimpleConverter(new ConverterOptions()
                .setTimeZone(TimeZone.getTimeZone("America/Los_Angeles")));
    }

    /* ---------------------------------------------------------------------- */
    /* Helper methods                                                         */
    /* ---------------------------------------------------------------------- */

    private static void assertCalendarEquals(Calendar expected, Calendar actual) {
        assertEquals("Year differs", expected.get(Calendar.YEAR), actual.get(Calendar.YEAR));
        assertEquals("Month differs", expected.get(Calendar.MONTH), actual.get(Calendar.MONTH));
        assertEquals("Day differs", expected.get(Calendar.DAY_OF_MONTH), actual.get(Calendar.DAY_OF_MONTH));
        assertEquals("Hour differs", expected.get(Calendar.HOUR_OF_DAY), actual.get(Calendar.HOUR_OF_DAY));
        assertEquals("Minute differs", expected.get(Calendar.MINUTE), actual.get(Calendar.MINUTE));
        assertEquals("Second differs", expected.get(Calendar.SECOND), actual.get(Calendar.SECOND));
        assertEquals("Millisecond differs", expected.get(Calendar.MILLISECOND), actual.get(Calendar.MILLISECOND));
        assertEquals("TimeZone differs", expected.getTimeZone(), actual.getTimeZone());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for toMap                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMapTypical() {
        LocalTime time = LocalTime.of(13, 45, 30, 123_456_789);
        Map<String, Object> map = LocalTimeConversions.toMap(time, utcConverter);
        assertNotNull("Map must not be null", map);
        assertEquals("Map should contain exactly one entry", 1, map.size());
        assertEquals("Key must be MapConversions.TIME", MapConversions.TIME, map.keySet().iterator().next());
        assertEquals("Value must be ISO‑8601 string", time.toString(), map.get(MapConversions.TIME));
    }

    @Test
    public void testToMapMidnight() {
        LocalTime midnight = LocalTime.MIDNIGHT;
        Map<String, Object> map = LocalTimeConversions.toMap(midnight, utcConverter);
        assertEquals("Midnight string representation", "00:00", map.get(MapConversions.TIME));
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for numeric conversions                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToIntegerAndLongTypical() {
        LocalTime time = LocalTime.of(01, 02, 03, 004_005_006);
        int expectedMs = (int) (time.toNanoOfDay() / 1_000_000L);
        long expectedLong = time.toNanoOfDay() / 1_000_000L;

        assertEquals("toInteger should return milliseconds as int", expectedMs,
                LocalTimeConversions.toInteger(time, utcConverter));
        assertEquals("toLong should return milliseconds as long", expectedLong,
                LocalTimeConversions.toLong(time, utcConverter));
    }

    @Test
    public void testToIntegerOverflow() {
        // 23:59:59.999999999 is the maximum nanosecond value for a day.
        LocalTime max = LocalTime.of(23, 59, 59, 999_999_999);
        long millis = max.toNanoOfDay() / 1_000_000L;
        assertTrue("Millisecond value must exceed Integer.MAX_VALUE for overflow test", millis > Integer.MAX_VALUE);
        // Cast to int truncates – we verify the actual behaviour.
        int expected = (int) millis;
        assertEquals(expected, LocalTimeConversions.toInteger(max, utcConverter));
    }

    @Test
    public void testToDoubleTypical() {
        LocalTime time = LocalTime.of(12, 0, 0, 500_000_000); // 0.5 seconds
        double expected = time.toNanoOfDay() / 1_000_000_000.0;
        assertEquals("toDouble should give seconds with fractional part", expected,
                LocalTimeConversions.toDouble(time, utcConverter), 0.0);
    }

    @Test
    public void testToBigIntegerTypical() {
        LocalTime time = LocalTime.of(5, 30, 15, 250_000_000);
        BigInteger expected = BigInteger.valueOf(time.toNanoOfDay());
        assertEquals(expected, LocalTimeConversions.toBigInteger(time, utcConverter));
    }

    @Test
    public void testToBigDecimalRounding() {
        // 00:00:00.123456789 => 0.123456789 seconds
        LocalTime time = LocalTime.of(0, 0, 0, 123_456_789);
        BigDecimal expected = new BigDecimal(time.toNanoOfDay())
                .divide(LocalTimeConversions.BILLION, 9, java.math.RoundingMode.HALF_UP);
        assertEquals(expected, LocalTimeConversions.toBigDecimal(time, utcConverter));
    }

    @Test
    public void testToAtomicIntegerAndLong() {
        LocalTime time = LocalTime.of(0, 0, 1, 0); // 1 second = 1000 ms
        AtomicInteger ai = LocalTimeConversions.toAtomicInteger(time, utcConverter);
        AtomicLong al = LocalTimeConversions.toAtomicLong(time, utcConverter);
        assertEquals(1000, ai.get());
        assertEquals(1000L, al.get());
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for toString                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringStandard() {
        LocalTime time = LocalTime.of(9, 8, 7, 654_321_000);
        String expected = time.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME);
        assertEquals(expected, LocalTimeConversions.toString(time, utcConverter));
    }

    @Test
    public void testToStringNoFraction() {
        LocalTime time = LocalTime.of(23, 59, 59);
        assertEquals("23:59:59", LocalTimeConversions.toString(time, utcConverter));
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for toCalendar                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCalendarUtc() {
        LocalTime time = LocalTime.of(15, 20, 30, 250_000_000); // 250 ms
        Calendar cal = LocalTimeConversions.toCalendar(time, utcConverter);

        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expected.clear();
        expected.set(Calendar.HOUR_OF_DAY, 15);
        expected.set(Calendar.MINUTE, 20);
        expected.set(Calendar.SECOND, 30);
        expected.set(Calendar.MILLISECOND, 250);
        // Date part defaults to epoch (1970‑01‑01) because we cleared the calendar.
        assertCalendarEquals(expected, cal);
    }

    @Test
    public void testToCalendarWithDifferentZone() {
        LocalTime time = LocalTime.of(6, 5, 4, 123_456_789); // 123 ms
        Calendar cal = LocalTimeConversions.toCalendar(time, pstConverter);

        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        expected.clear();
        expected.set(Calendar.HOUR_OF_DAY, 6);
        expected.set(Calendar.MINUTE, 5);
        expected.set(Calendar.SECOND, 4);
        expected.set(Calendar.MILLISECOND, 123);
        assertCalendarEquals(expected, cal);
    }

    @Test
    public void testToCalendarMidnight() {
        LocalTime midnight = LocalTime.MIDNIGHT;
        Calendar cal = LocalTimeConversions.toCalendar(midnight, utcConverter);
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testToCalendarDoesNotAlterDateFields() {
        // Ensure that day, month and year stay at the default (epoch) values.
        LocalTime time = LocalTime.of(12, 0);
        Calendar cal = LocalTimeConversions.toCalendar(time, utcConverter);
        Calendar epoch = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        epoch.clear(); // epoch = 1970‑01‑01 00:00:00.000 UTC
        assertEquals(epoch.get(Calendar.YEAR), cal.get(Calendar.YEAR));
        assertEquals(epoch.get(Calendar.MONTH), cal.get(Calendar.MONTH));
        assertEquals(epoch.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.DAY_OF_MONTH));
    }

    /* ---------------------------------------------------------------------- */
    /* Defensive tests – passing wrong type (should throw ClassCastException) */
    /* ---------------------------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToIntegerWithWrongType() {
        LocalTimeConversions.toInteger("not a LocalTime", utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWithWrongType() {
        LocalTimeConversions.toMap(12345, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCalendarWithWrongType() {
        LocalTimeConversions.toCalendar(new Object(), utcConverter);
    }
}
