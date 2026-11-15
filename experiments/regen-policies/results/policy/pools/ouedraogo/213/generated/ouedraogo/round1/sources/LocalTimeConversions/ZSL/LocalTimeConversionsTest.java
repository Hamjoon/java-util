package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class LocalTimeConversionsTest {

    private LocalTime sampleTime;
    private Converter converter;

    @Before
    public void setUp() {
        // 12:34:56.789123456
        sampleTime = LocalTime.of(12, 34, 56, 789_123_456);
        // Converter requires ConverterOptions – use the default constructor (library provides it)
        converter = new Converter(new ConverterOptions());
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = LocalTimeConversions.toMap(sampleTime, converter);
        assertNotNull(map);
        // The key is defined in MapConversions.LOCAL_TIME – we rely on that constant being "localTime"
        assertTrue(map.containsKey(MapConversions.LOCAL_TIME));
        assertEquals(sampleTime.toString(), map.get(MapConversions.LOCAL_TIME));
    }

    @Test
    public void testToInteger() {
        int expectedMillis = (int) (sampleTime.toNanoOfDay() / 1_000_000L);
        int result = LocalTimeConversions.toInteger(sampleTime, converter);
        assertEquals(expectedMillis, result);
    }

    @Test
    public void testToLong() {
        long expectedMillis = sampleTime.toNanoOfDay() / 1_000_000L;
        long result = LocalTimeConversions.toLong(sampleTime, converter);
        assertEquals(expectedMillis, result);
    }

    @Test
    public void testToDouble() {
        double expectedSeconds = sampleTime.toNanoOfDay() / 1_000_000_000.0;
        double result = LocalTimeConversions.toDouble(sampleTime, converter);
        assertEquals(expectedSeconds, result, 0.0);
    }

    @Test
    public void testToBigInteger() {
        BigInteger expected = BigInteger.valueOf(sampleTime.toNanoOfDay());
        BigInteger result = LocalTimeConversions.toBigInteger(sampleTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal expected = new BigDecimal(sampleTime.toNanoOfDay())
                .divide(LocalTimeConversions.BILLION, 9, java.math.RoundingMode.HALF_UP);
        BigDecimal result = LocalTimeConversions.toBigDecimal(sampleTime, converter);
        assertEquals(0, expected.compareTo(result));
    }

    @Test
    public void testToAtomicInteger() {
        AtomicInteger ai = LocalTimeConversions.toAtomicInteger(sampleTime, converter);
        int expected = (int) (sampleTime.toNanoOfDay() / 1_000_000L);
        assertEquals(expected, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong al = LocalTimeConversions.toAtomicLong(sampleTime, converter);
        long expected = sampleTime.toNanoOfDay() / 1_000_000L;
        assertEquals(expected, al.get());
    }

    @Test
    public void testToString() {
        String expected = sampleTime.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME);
        String result = LocalTimeConversions.toString(sampleTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToCalendar() {
        Calendar cal = LocalTimeConversions.toCalendar(sampleTime, converter);
        assertNotNull(cal);
        // Calendar should reflect the same hour, minute, second and millisecond
        assertEquals(sampleTime.getHour(), cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(sampleTime.getMinute(), cal.get(Calendar.MINUTE));
        assertEquals(sampleTime.getSecond(), cal.get(Calendar.SECOND));
        assertEquals(sampleTime.getNano() / 1_000_000, cal.get(Calendar.MILLISECOND));
        // The date part (year, month, day) is whatever the current date was when the Calendar was created.
        // We only verify that the time fields match.
    }
}
