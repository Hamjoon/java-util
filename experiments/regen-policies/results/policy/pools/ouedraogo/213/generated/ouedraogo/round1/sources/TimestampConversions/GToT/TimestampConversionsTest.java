package com.cedarsoftware.util.convert;

import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TimestampConversions}.
 *
 * <p>The tests exercise every static conversion method, covering:
 * <ul>
 *   <li>normal (happy‑path) usage</li>
 *   <li>edge cases such as timestamps before the epoch, nanosecond vs. millisecond precision,
 *       and the maximum {@code long} value for a timestamp</li>
 *   <li>exceptional situations (e.g. {@code null} arguments)</li>
 * </ul>
 *
 * <p>All tests live in the same package as {@code TimestampConversions} so that package‑private
 * static methods are directly accessible.
 */
public class TimestampConversionsTest {

    /** A {@link Converter} that always uses UTC – this makes the results deterministic. */
    private static final Converter UTC_CONVERTER;

    static {
        // The library supplies a {@code ConverterOptions} class (not shown).  It has a public
        // no‑arg constructor and setters for zone and time‑zone.  Using those setters we build
        // an options instance that forces UTC for both the {@code ZoneId} and {@code TimeZone}.
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(ZoneId.of("UTC"));
        opts.setTimeZone(TimeZone.getTimeZone("UTC"));
        UTC_CONVERTER = new Converter(opts);
    }

    /** Ensure the JVM default zone / time‑zone is UTC for any code that falls back to it. */
    @BeforeClass
    public static void setUpClass() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.US);
    }

    /** Helper – creates a {@link Timestamp} from the supplied fields using UTC. */
    private static Timestamp ts(int year, int month, int day,
                               int hour, int minute, int second, int nano) {
        LocalDateTime ldt = LocalDateTime.of(year, month, day, hour, minute, second, nano);
        Instant instant = ldt.toInstant(ZoneOffset.UTC);
        Timestamp ts = Timestamp.from(instant);
        ts.setNanos(nano);               // ensure the nanosecond part is exactly as requested
        return ts;
    }

    /*-----------------------------------------------------------------------
     *  Normal (happy‑path) tests
     *---------------------------------------------------------------------*/

    @Test
    public void testToDouble() {
        Timestamp ts = ts(2021, 3, 14, 15, 9, 26, 123_456_789);
        double actual = TimestampConversions.toDouble(ts, UTC_CONVERTER);

        Duration d = Duration.between(Instant.EPOCH, ts.toInstant());
        double expected = d.getSeconds() + d.getNano() / 1_000_000_000d;
        assertEquals(expected, actual, 1e-12);
    }

    @Test
    public void testToBigDecimal() {
        Timestamp ts = ts(1970, 1, 1, 0, 0, 0, 500_000_000);
        BigDecimal bd = TimestampConversions.toBigDecimal(ts, UTC_CONVERTER);
        assertNotNull(bd);
        // Verify consistency with toDouble (within the precision of a BigDecimal)
        double asDouble = bd.doubleValue();
        double expected = TimestampConversions.toDouble(ts, UTC_CONVERTER);
        assertEquals(expected, asDouble, 1e-12);
    }

    @Test
    public void testToBigInteger() {
        Timestamp ts = ts(2000, 2, 29, 12, 0, 0, 0);
        BigInteger bi = TimestampConversions.toBigInteger(ts, UTC_CONVERTER);
        assertNotNull(bi);
        // The BigInteger should represent the epoch seconds (no nanos)
        long epochSeconds = ts.toInstant().getEpochSecond();
        assertEquals(BigInteger.valueOf(epochSeconds), bi);
    }

    @Test
    public void testToLocalDateTime() {
        Timestamp ts = ts(1999, 12, 31, 23, 59, 59, 0);
        LocalDateTime ldt = TimestampConversions.toLocalDateTime(ts, UTC_CONVERTER);
        LocalDateTime expected = ts.toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime();
        assertEquals(expected, ldt);
    }

    @Test
    public void testToDuration() {
        Timestamp ts = ts(2010, 5, 20, 8, 30, 15, 250_000_000);
        Duration d = TimestampConversions.toDuration(ts, UTC_CONVERTER);
        Duration expected = Duration.between(Instant.EPOCH, ts.toInstant());
        assertEquals(expected, d);
    }

    @Test
    public void testToOffsetDateTime() {
        Timestamp ts = ts(2022, 7, 4, 16, 45, 0, 0);
        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(ts, UTC_CONVERTER);
        OffsetDateTime expected = ts.toInstant().atZone(ZoneId.of("UTC")).toOffsetDateTime();
        assertEquals(expected, odt);
    }

    @Test
    public void testToCalendar() {
        Timestamp ts = ts(2015, 10, 1, 0, 0, 0, 0);
        Calendar cal = TimestampConversions.toCalendar(ts, UTC_CONVERTER);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(ts.getTime(), cal.getTimeInMillis());
    }

    @Test
    public void testToDate() {
        Timestamp ts = ts(1995, 6, 15, 12, 30, 45, 0);
        Date date = TimestampConversions.toDate(ts, UTC_CONVERTER);
        assertEquals(Date.from(ts.toInstant()), date);
    }

    @Test
    public void testToSqlDate() {
        Timestamp ts = ts(2020, 1, 1, 0, 0, 0, 0);
        java.sql.Date sqlDate = TimestampConversions.toSqlDate(ts, UTC_CONVERTER);
        java.sql.Date expected = java.sql.Date.valueOf(
                ts.toInstant().atZone(ZoneId.of("UTC")).toLocalDate());
        assertEquals(expected, sqlDate);
    }

    @Test
    public void testToLong() {
        Timestamp ts = ts(2001, 9, 11, 8, 46, 0, 0);
        long l = TimestampConversions.toLong(ts, UTC_CONVERTER);
        assertEquals(ts.getTime(), l);
    }

    @Test
    public void testToYear() {
        Timestamp ts = ts(1984, 12, 31, 23, 59, 59, 0);
        Year y = TimestampConversions.toYear(ts, UTC_CONVERTER);
        Year expected = Year.from(ts.toInstant().atZone(ZoneId.of("UTC")).toLocalDate());
        assertEquals(expected, y);
    }

    @Test
    public void testToYearMonth() {
        Timestamp ts = ts(1977, 5, 20, 0, 0, 0, 0);
        YearMonth ym = TimestampConversions.toYearMonth(ts, UTC_CONVERTER);
        YearMonth expected = YearMonth.from(ts.toInstant().atZone(ZoneId.of("UTC")).toLocalDate());
        assertEquals(expected, ym);
    }

    @Test
    public void testToMonthDay() {
        Timestamp ts = ts(2005, 2, 28, 0, 0, 0, 0);
        MonthDay md = TimestampConversions.toMonthDay(ts, UTC_CONVERTER);
        MonthDay expected = MonthDay.from(ts.toInstant().atZone(ZoneId.of("UTC")).toLocalDate());
        assertEquals(expected, md);
    }

    @Test
    public void testToString_MillisecondPrecision() {
        // 123_000_000 nanos = exactly 123 ms → millisecond pattern
        Timestamp ts = ts(2023, 1, 2, 3, 4, 5, 123_000_000);
        String s = TimestampConversions.toString(ts, UTC_CONVERTER);
        String expected = ts.toInstant()
                .atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        assertEquals(expected, s);
    }

    @Test
    public void testToString_NanosecondPrecision() {
        // 123_456_789 nanos → not a whole millisecond → nanosecond pattern
        Timestamp ts = ts(2023, 1, 2, 3, 4, 5, 123_456_789);
        String s = TimestampConversions.toString(ts, UTC_CONVERTER);
        String expected = ts.toInstant()
                .atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'"));
        assertEquals(expected, s);
    }

    @Test
    public void testToMap() {
        Timestamp ts = ts(2022, 12, 31, 23, 59, 59, 999_000_000);
        Map<String, Object> map = TimestampConversions.toMap(ts, UTC_CONVERTER);
        assertNotNull(map);
        assertTrue(map.containsKey(MapConversions.TIMESTAMP));
        assertEquals(TimestampConversions.toString(ts, UTC_CONVERTER), map.get(MapConversions.TIMESTAMP));
    }

    /*-----------------------------------------------------------------------
     *  Edge‑case and exception tests
     *---------------------------------------------------------------------*/

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullTimestamp() {
        TimestampConversions.toDouble(null, UTC_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullTimestamp() {
        TimestampConversions.toLocalDateTime(null, UTC_CONVERTER);
    }

    @Test
    public void testToDuration_PreEpoch() {
        // 1965‑07‑01 00:00:00 UTC (negative epoch seconds)
        Timestamp ts = ts(1965, 7, 1, 0, 0, 0, 0);
        Duration d = TimestampConversions.toDuration(ts, UTC_CONVERTER);
        Duration expected = Duration.between(Instant.EPOCH, ts.toInstant());
        assertEquals(expected, d);
        assertTrue(d.isNegative());
    }

    @Test
    public void testToString_MaxLongTimestamp() {
        // Timestamp that uses Long.MAX_VALUE milliseconds since epoch
        Timestamp ts = new Timestamp(Long.MAX_VALUE);
        ts.setNanos(999_999_999); // set to max nanos
        String s = TimestampConversions.toString(ts, UTC_CONVERTER);
        // The string should still be produced without throwing and must match the pattern.
        // We only verify that parsing it back yields the same instant.
        Instant parsed = Instant.from(DateTimeFormatter.ofPattern(
                (ts.getNanos() % 1_000_000 == 0) ?
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" :
                        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'")
                .withZone(ZoneOffset.UTC)
                .parse(s));
        assertEquals(ts.toInstant(), parsed);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullTimestamp() {
        TimestampConversions.toString(null, UTC_CONVERTER);
    }

    @Test
    public void testToMap_NullTimestamp() {
        try {
            TimestampConversions.toMap(null, UTC_CONVERTER);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
            // expected
        }
    }

    @Test
    public void testToYearMonth_EdgeOfMonth() {
        // February 29 on a leap year
        Timestamp ts = ts(2020, 2, 29, 12, 0, 0, 0);
        YearMonth ym = TimestampConversions.toYearMonth(ts, UTC_CONVERTER);
        assertEquals(YearMonth.of(2020, 2), ym);
    }

    @Test
    public void testToMonthDay_NonLeapYearFeb28() {
        // February 28 on a non‑leap year
        Timestamp ts = ts(2019, 2, 28, 0, 0, 0, 0);
        MonthDay md = TimestampConversions.toMonthDay(ts, UTC_CONVERTER);
        assertEquals(MonthDay.of(2, 28), md);
    }

    @Test
    public void testToLong_NegativeTimestamp() {
        // 1960‑01‑01 00:00:00 UTC → negative milliseconds
        Timestamp ts = ts(1960, 1, 1, 0, 0, 0, 0);
        long l = TimestampConversions.toLong(ts, UTC_CONVERTER);
        assertTrue(l < 0);
        assertEquals(ts.getTime(), l);
    }
}
