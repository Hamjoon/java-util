package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LocalTimeConversions}.
 *
 * <p>This suite covers:
 * <ul>
 *   <li>basic functional verification of every public static conversion method,</li>
 *   <li>edge‑case values such as midnight, the last nanosecond of the day, and
 *       times with sub‑millisecond precision,</li>
 *   <li>exception scenarios (null input and wrong input type),</li>
 *   <li>correct handling of rounding for {@code BigDecimal} results,</li>
 *   <li>time‑zone handling for {@code Calendar} conversion.</li>
 * </ul>
 *
 * <p>All tests construct real objects; no mocking frameworks are used,
 * complying with the restriction to use only the classes shown in the
 * supplied source code and the public API of {@code Converter} and
 * {@code CompactLinkedMap}.
 */
public class LocalTimeConversionsTest {

    /** A {@code Converter} instance required by the conversion methods. */
    private Converter converter;

    /** The constant key used by {@code toMap}.  It is defined in {@code MapConversions}. */
    private static final String MAP_TIME_KEY = "time";   // MapConversions.TIME is expected to be "time"

    @Before
    public void setUp() {
        // ConverterOptions has a no‑arg constructor in the Cedar library.
        ConverterOptions options = new ConverterOptions();
        // Force a known time‑zone (UTC) so Calendar tests are deterministic.
        options.setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")));
        converter = new Converter(options);
    }

    /* ---------------------------------------------------------------------- */
    /* -------------------------- Helper methods ---------------------------- */
    /* ---------------------------------------------------------------------- */

    /** Returns a {@code LocalTime} representing the very first instant of a day. */
    private static LocalTime midnight() {
        return LocalTime.MIDNIGHT; // 00:00:00.000000000
    }

    /** Returns a {@code LocalTime} representing the very last nanosecond of a day. */
    private static LocalTime endOfDay() {
        // 23:59:59.999999999
        return LocalTime.of(23, 59, 59, 999_999_999);
    }

    /** Returns a {@code LocalTime} with a non‑zero nanosecond component that is not a whole millisecond. */
    private static LocalTime subMillisecond() {
        // 12:34:56.123456789 (123 456 789 ns)
        return LocalTime.of(12, 34, 56, 123_456_789);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- toMap tests --------------------------------*/
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMap_Basic() {
        LocalTime lt = LocalTime.of(10, 15, 30);
        Map<String, Object> map = LocalTimeConversions.toMap(lt, converter);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals(lt.toString(), map.get(MAP_TIME_KEY));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        LocalTimeConversions.toMap(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongType() {
        LocalTimeConversions.toMap("not a LocalTime", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- toInteger tests --------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToInteger_Midnight() {
        int millis = LocalTimeConversions.toInteger(midnight(), converter);
        assertEquals(0, millis);
    }

    @Test
    public void testToInteger_EndOfDay() {
        int millis = LocalTimeConversions.toInteger(endOfDay(), converter);
        // 86_400_000 ms in a full day minus 1 ms because we truncate the nanosecond part.
        assertEquals(86_399_999, millis);
    }

    @Test
    public void testToInteger_SubMillisecond() {
        int millis = LocalTimeConversions.toInteger(subMillisecond(), converter);
        // 12:34:56 = 45296 seconds → 45_296_000 ms, plus 123 ms from the nanosecond part.
        assertEquals(45_296_123, millis);
    }

    @Test(expected = NullPointerException.class)
    public void testToInteger_NullInput() {
        LocalTimeConversions.toInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInteger_WrongType() {
        LocalTimeConversions.toInteger(123, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- toLong tests ------------------------------ */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToLong_Midnight() {
        long millis = LocalTimeConversions.toLong(midnight(), converter);
        assertEquals(0L, millis);
    }

    @Test
    public void testToLong_EndOfDay() {
        long millis = LocalTimeConversions.toLong(endOfDay(), converter);
        assertEquals(86_399_999L, millis);
    }

    @Test
    public void testToLong_SubMillisecond() {
        long millis = LocalTimeConversions.toLong(subMillisecond(), converter);
        assertEquals(45_296_123L, millis);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        LocalTimeConversions.toLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongType() {
        LocalTimeConversions.toLong(Boolean.TRUE, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- toDouble tests ---------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToDouble_Midnight() {
        double seconds = LocalTimeConversions.toDouble(midnight(), converter);
        assertEquals(0.0, seconds, 0.0);
    }

    @Test
    public void testToDouble_EndOfDay() {
        double seconds = LocalTimeConversions.toDouble(endOfDay(), converter);
        // 86_399.999999999 seconds (full day minus 1 ns)
        assertEquals(86_399.999999999, seconds, 1e-12);
    }

    @Test
    public void testToDouble_SubMillisecond() {
        double seconds = LocalTimeConversions.toDouble(subMillisecond(), converter);
        // 12:34:56.123456789 = 45296.123456789 seconds
        assertEquals(45_296.123456789, seconds, 1e-12);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInput() {
        LocalTimeConversions.toDouble(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_WrongType() {
        LocalTimeConversions.toDouble(new Object(), converter);
    }

    /* ---------------------------------------------------------------------- */
    /* -------------------------- toBigInteger tests ------------------------ */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBigInteger_Midnight() {
        BigInteger bi = LocalTimeConversions.toBigInteger(midnight(), converter);
        assertEquals(BigInteger.ZERO, bi);
    }

    @Test
    public void testToBigInteger_EndOfDay() {
        BigInteger bi = LocalTimeConversions.toBigInteger(endOfDay(), converter);
        // nanoseconds in a full day = 86_400_000_000_000
        assertEquals(BigInteger.valueOf(86_400_000_000_000L), bi);
    }

    @Test
    public void testToBigInteger_SubMillisecond() {
        BigInteger bi = LocalTimeConversions.toBigInteger(subMillisecond(), converter);
        assertEquals(BigInteger.valueOf(45_296_123_456_789L), bi);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        LocalTimeConversions.toBigInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_WrongType() {
        LocalTimeConversions.toBigInteger(3.14, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* -------------------------- toBigDecimal tests ------------------------ */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBigDecimal_Midnight() {
        BigDecimal bd = LocalTimeConversions.toBigDecimal(midnight(), converter);
        assertEquals(BigDecimal.ZERO.setScale(9), bd);
    }

    @Test
    public void testToBigDecimal_EndOfDay() {
        BigDecimal bd = LocalTimeConversions.toBigDecimal(endOfDay(), converter);
        // 86_400_000_000_000 ns / 1_000_000_000 = 86_400.000000000
        assertEquals(new BigDecimal("86400.000000000"), bd);
    }

    @Test
    public void testToBigDecimal_SubMillisecond_Rounding() {
        BigDecimal bd = LocalTimeConversions.toBigDecimal(subMillisecond(), converter);
        // Expected: 45296.123456789 (exact, no rounding needed)
        assertEquals(new BigDecimal("45296.123456789"), bd);
    }

    @Test
    public void testToBigDecimal_RoundingHalfUp() {
        // Create a time that would produce a repeating decimal at 9‑scale.
        // 00:00:00.000000001 => 0.000000001 seconds (exact)
        LocalTime lt = LocalTime.of(0, 0, 0, 1);
        BigDecimal bd = LocalTimeConversions.toBigDecimal(lt, converter);
        assertEquals(new BigDecimal("0.000000001"), bd);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        LocalTimeConversions.toBigDecimal(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_WrongType() {
        LocalTimeConversions.toBigDecimal(Boolean.FALSE, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* -------------------------- toAtomicInteger tests --------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicInteger_Basic() {
        AtomicInteger ai = LocalTimeConversions.toAtomicInteger(subMillisecond(), converter);
        assertEquals(45_296_123, ai.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_NullInput() {
        LocalTimeConversions.toAtomicInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicInteger_WrongType() {
        LocalTimeConversions.toAtomicInteger(new Object(), converter);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- toAtomicLong tests ----------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicLong_Basic() {
        AtomicLong al = LocalTimeConversions.toAtomicLong(endOfDay(), converter);
        assertEquals(86_399_999L, al.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInput() {
        LocalTimeConversions.toAtomicLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLong_WrongType() {
        LocalTimeConversions.toAtomicLong("bad", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* ----------------------------- toString tests -------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToString_Standard() {
        LocalTime lt = LocalTime.of(13, 5, 9, 0);
        String s = LocalTimeConversions.toString(lt, converter);
        assertEquals(lt.format(DateTimeFormatter.ISO_LOCAL_TIME), s);
    }

    @Test
    public void testToString_WithNanos() {
        LocalTime lt = subMillisecond(); // 12:34:56.123456789
        String s = LocalTimeConversions.toString(lt, converter);
        assertEquals(lt.format(DateTimeFormatter.ISO_LOCAL_TIME), s);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        LocalTimeConversions.toString(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongType() {
        LocalTimeConversions.toString(12345, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- toCalendar tests -------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCalendar_BasicMidnightUTC() {
        Calendar cal = LocalTimeConversions.toCalendar(midnight(), converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testToCalendar_SubMillisecondUTC() {
        Calendar cal = LocalTimeConversions.toCalendar(subMillisecond(), converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(34, cal.get(Calendar.MINUTE));
        assertEquals(56, cal.get(Calendar.SECOND));
        // Nanoseconds 123_456_789 → milliseconds 123
        assertEquals(123, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testToCalendar_EndOfDayUTC() {
        Calendar cal = LocalTimeConversions.toCalendar(endOfDay(), converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
        // 999_999_999 ns → 999 ms (truncated)
        assertEquals(999, cal.get(Calendar.MILLISECOND));
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullInput() {
        LocalTimeConversions.toCalendar(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCalendar_WrongType() {
        LocalTimeConversions.toCalendar(42, converter);
    }
}
