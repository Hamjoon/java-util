package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unit tests for {@link LocalTimeConversions}.
 * <p>
 * The tests cover every public static conversion method in the class.
 * Real {@link LocalTime} instances are used and the results are verified
 * against the expected values calculated directly from the JDK API.
 * </p>
 */
public class LocalTimeConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The Converter class is part of the library under test.
        // It requires a ConverterOptions instance; the default constructor
        // provides a sensible default TimeZone (the system default).
        converter = new Converter(new ConverterOptions());
    }

    @Test
    public void testToMap() {
        LocalTime lt = LocalTime.of(13, 45, 30, 123_000_000); // 13:45:30.123
        Map<String, Object> map = LocalTimeConversions.toMap(lt, converter);
        Assert.assertNotNull(map);
        Assert.assertEquals(1, map.size());
        Assert.assertEquals(lt.toString(), map.get(MapConversions.TIME));
    }

    @Test
    public void testToInteger() {
        LocalTime lt = LocalTime.of(0, 0, 1, 500_000_000); // 1.5 seconds
        int expected = (int) (lt.toNanoOfDay() / 1_000_000L); // milliseconds
        int actual = LocalTimeConversions.toInteger(lt, converter);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToLong() {
        LocalTime lt = LocalTime.of(23, 59, 59, 999_999_999);
        long expected = lt.toNanoOfDay() / 1_000_000L;
        long actual = LocalTimeConversions.toLong(lt, converter);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToDouble() {
        LocalTime lt = LocalTime.of(12, 30, 15, 250_000_000);
        double expected = lt.toNanoOfDay() / 1_000_000_000.0;
        double actual = LocalTimeConversions.toDouble(lt, converter);
        Assert.assertEquals(expected, actual, 0.0);
    }

    @Test
    public void testToBigInteger() {
        LocalTime lt = LocalTime.of(5, 0, 0, 0);
        BigInteger expected = BigInteger.valueOf(lt.toNanoOfDay());
        BigInteger actual = LocalTimeConversions.toBigInteger(lt, converter);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        LocalTime lt = LocalTime.of(1, 2, 3, 456_789_123);
        BigDecimal expected = new BigDecimal(lt.toNanoOfDay())
                .divide(LocalTimeConversions.BILLION, 9, java.math.RoundingMode.HALF_UP);
        BigDecimal actual = LocalTimeConversions.toBigDecimal(lt, converter);
        Assert.assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToAtomicInteger() {
        LocalTime lt = LocalTime.of(0, 0, 0, 999_000_000); // 0.999 seconds
        AtomicInteger atomic = LocalTimeConversions.toAtomicInteger(lt, converter);
        long expectedLong = lt.toNanoOfDay() / 1_000_000L;
        Assert.assertEquals((int) expectedLong, atomic.get());
    }

    @Test
    public void testToAtomicLong() {
        LocalTime lt = LocalTime.of(0, 0, 0, 1_500_000_000); // 1.5 seconds
        AtomicLong atomic = LocalTimeConversions.toAtomicLong(lt, converter);
        long expected = lt.toNanoOfDay() / 1_000_000L;
        Assert.assertEquals(expected, atomic.get());
    }

    @Test
    public void testToString() {
        LocalTime lt = LocalTime.of(9, 8, 7, 123_456_789);
        String expected = lt.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME);
        String actual = LocalTimeConversions.toString(lt, converter);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        LocalTime lt = LocalTime.of(15, 20, 30, 250_000_000); // 15:20:30.250
        Calendar cal = LocalTimeConversions.toCalendar(lt, converter);
        Assert.assertNotNull(cal);
        // Calendar should reflect the same hour/minute/second/millisecond as the LocalTime.
        Assert.assertEquals(lt.getHour(), cal.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(lt.getMinute(), cal.get(Calendar.MINUTE));
        Assert.assertEquals(lt.getSecond(), cal.get(Calendar.SECOND));
        Assert.assertEquals(lt.getNano() / 1_000_000, cal.get(Calendar.MILLISECOND));
        // Verify that the timezone used is the one supplied by the Converter options.
        TimeZone expectedTZ = converter.getOptions().getTimeZone();
        Assert.assertEquals(expectedTZ, cal.getTimeZone());
    }
}
