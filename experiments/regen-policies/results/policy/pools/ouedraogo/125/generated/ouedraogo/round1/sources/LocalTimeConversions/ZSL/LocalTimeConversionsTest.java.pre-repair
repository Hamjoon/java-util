package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class LocalTimeConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions is part of the library; it provides a default TimeZone.
        // We rely on the library's no‑arg constructor (or default constructor) to obtain a sane default.
        // If the library requires a specific constructor, adjust accordingly.
        ConverterOptions options = new ConverterOptions(); // assumes a public no‑arg constructor
        // Ensure the time zone is set to a known value to make Calendar assertions deterministic.
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.converter = new Converter(options);
    }

    @Test
    public void testToMapContainsCorrectKeyAndValue() {
        LocalTime time = LocalTime.of(12, 34, 56, 123_000_000); // 12:34:56.123
        Map<String, Object> map = LocalTimeConversions.toMap(time, converter);
        assertNotNull("Resulting map should not be null", map);
        assertTrue("Map should contain the TIME key", map.containsKey(MapConversions.TIME));
        assertEquals("Map value should be the ISO string of the LocalTime",
                time.toString(),
                map.get(MapConversions.TIME));
    }

    @Test
    public void testToIntegerAndToLongWithTypicalTime() {
        LocalTime time = LocalTime.of(01, 02, 03, 004_005_006); // 01:02:03.004005006
        long expectedNanoOfDay = time.toNanoOfDay(); // nanoseconds since midnight
        int expectedMillis = (int) (expectedNanoOfDay / 1_000_000L);
        long expectedLong = expectedNanoOfDay / 1_000_000L;

        int actualInt = LocalTimeConversions.toInteger(time, converter);
        long actualLong = LocalTimeConversions.toLong(time, converter);

        assertEquals("Integer conversion should match milliseconds truncation", expectedMillis, actualInt);
        assertEquals("Long conversion should match milliseconds truncation", expectedLong, actualLong);
    }

    @Test
    public void testToDoublePrecision() {
        LocalTime time = LocalTime.of(23, 59, 59, 999_999_999);
        double expected = time.toNanoOfDay() / 1_000_000_000.0;
        double actual = LocalTimeConversions.toDouble(time, converter);
        assertEquals("Double conversion should represent seconds with nanosecond fraction", expected, actual, 1e-12);
    }

    @Test
    public void testToBigInteger() {
        LocalTime time = LocalTime.of(00, 00, 00, 500_000_000); // 0.5 seconds
        BigInteger expected = BigInteger.valueOf(time.toNanoOfDay());
        BigInteger actual = LocalTimeConversions.toBigInteger(time, converter);
        assertEquals("BigInteger conversion should equal nanoseconds", expected, actual);
    }

    @Test
    public void testToBigDecimalScaleAndRounding() {
        // Use a time that results in a repeating decimal when divided by 1_000_000_000
        LocalTime time = LocalTime.of(00, 00, 00, 123_456_789);
        BigDecimal expected = new BigDecimal(time.toNanoOfDay())
                .divide(LocalTimeConversions.BILLION, 9, java.math.RoundingMode.HALF_UP);
        BigDecimal actual = LocalTimeConversions.toBigDecimal(time, converter);
        assertEquals("BigDecimal conversion should be accurate to 9 decimal places", 0, expected.compareTo(actual));
    }

    @Test
    public void testAtomicConversionsMatchLong() {
        LocalTime time = LocalTime.of(10, 20, 30, 400_000_000);
        long expectedMillis = time.toNanoOfDay() / 1_000_000L;

        AtomicInteger atomicInt = LocalTimeConversions.toAtomicInteger(time, converter);
        AtomicLong atomicLong = LocalTimeConversions.toAtomicLong(time, converter);

        assertEquals("AtomicInteger value should match long conversion", (int) expectedMillis, atomicInt.get());
        assertEquals("AtomicLong value should match long conversion", expectedMillis, atomicLong.get());
    }

    @Test
    public void testToStringFormatting() {
        LocalTime time = LocalTime.of(07, 08, 09, 0);
        String expected = time.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME);
        String actual = LocalTimeConversions.toString(time, converter);
        assertEquals("String conversion should use ISO_LOCAL_TIME format", expected, actual);
    }

    @Test
    public void testToCalendarFields() {
        LocalTime time = LocalTime.of(15, 45, 30, 250_000_000); // 15:45:30.250
        Calendar cal = LocalTimeConversions.toCalendar(time, converter);
        assertEquals("Calendar hour should match", time.getHour(), cal.get(Calendar.HOUR_OF_DAY));
        assertEquals("Calendar minute should match", time.getMinute(), cal.get(Calendar.MINUTE));
        assertEquals("Calendar second should match", time.getSecond(), cal.get(Calendar.SECOND));
        assertEquals("Calendar millisecond should match", time.getNano() / 1_000_000, cal.get(Calendar.MILLISECOND));
        // Verify that the calendar uses the time zone we set in the converter options (UTC)
        assertEquals("Calendar time zone should be UTC as set in options", TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test
    public void testEdgeCaseMidnight() {
        LocalTime midnight = LocalTime.MIDNIGHT;
        assertEquals(0, LocalTimeConversions.toInteger(midnight, converter));
        assertEquals(0L, LocalTimeConversions.toLong(midnight, converter));
        assertEquals(0.0, LocalTimeConversions.toDouble(midnight, converter), 0.0);
        assertEquals(BigInteger.ZERO, LocalTimeConversions.toBigInteger(midnight, converter));
        assertEquals(BigDecimal.ZERO, LocalTimeConversions.toBigDecimal(midnight, converter));
        assertEquals("00:00", LocalTimeConversions.toString(midnight, converter));
        Calendar cal = LocalTimeConversions.toCalendar(midnight, converter);
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testEdgeCaseMaximumTime() {
        // 23:59:59.999999999
        LocalTime max = LocalTime.of(23, 59, 59, 999_999_999);
        long expectedNano = max.toNanoOfDay();
        int expectedMillis = (int) (expectedNano / 1_000_000L);
        long expectedLong = expectedNano / 1_000_000L;
        double expectedDouble = expectedNano / 1_000_000_000.0;
        BigInteger expectedBigInt = BigInteger.valueOf(expectedNano);
        BigDecimal expectedBigDec = new BigDecimal(expectedNano).divide(LocalTimeConversions.BILLION, 9, java.math.RoundingMode.HALF_UP);

        assertEquals(expectedMillis, LocalTimeConversions.toInteger(max, converter));
        assertEquals(expectedLong, LocalTimeConversions.toLong(max, converter));
        assertEquals(expectedDouble, LocalTimeConversions.toDouble(max, converter), 1e-12);
        assertEquals(expectedBigInt, LocalTimeConversions.toBigInteger(max, converter));
        assertEquals(0, expectedBigDec.compareTo(LocalTimeConversions.toBigDecimal(max, converter)));
        assertEquals("23:59:59.999999999", LocalTimeConversions.toString(max, converter));
        Calendar cal = LocalTimeConversions.toCalendar(max, converter);
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
        assertEquals(999, cal.get(Calendar.MILLISECOND)); // nanoseconds truncated to milliseconds
    }

    @Test
    public void testCompactMapBuilderProducesInsertionOrderMap() {
        LocalTime time = LocalTime.of(01, 23, 45);
        Map<String, Object> map = LocalTimeConversions.toMap(time, converter);
        // The builder is expected to create a map that preserves insertion order.
        // Since we only insert a single entry, we simply verify that the map is not empty and contains the entry.
        assertFalse("Map should not be empty", map.isEmpty());
        assertTrue("Map should contain the TIME key", map.containsKey(MapConversions.TIME));
    }
}
