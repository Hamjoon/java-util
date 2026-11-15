package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link LocalTimeConversions}. All public static conversion methods are exercised
 * with a variety of {@link LocalTime} values, including edge cases such as midnight and
 * nanosecond‑precision times.
 *
 * The {@link Converter} used in the tests is created with a default {@code ConverterOptions}
 * instance – the real library supplies a no‑arg constructor for {@code ConverterOptions}
 * that defaults to the JVM default time‑zone. This is sufficient for the {@code toCalendar}
 * conversion which only relies on {@code getTimeZone()}.
 */
public class LocalTimeConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The library provides a no‑arg constructor for ConverterOptions that defaults to the
        // system time‑zone.  Using it keeps the test independent of external configuration.
        ConverterOptions options = new ConverterOptions();               // default options
        converter = new Converter(options);
    }

    @Test
    public void testToMap() {
        LocalTime lt = LocalTime.of(13, 45, 30, 123_456_789);
        Map<String, Object> map = LocalTimeConversions.toMap(lt, converter);

        // The map should contain exactly one entry with the key defined in MapConversions.TIME
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.TIME));

        // The stored value must be the standard ISO‑8601 representation of the LocalTime
        assertEquals(lt.toString(), map.get(MapConversions.TIME));
    }

    @Test
    public void testToInteger_NormalValue() {
        LocalTime lt = LocalTime.of(12, 34, 56, 789_000_000); // 12:34:56.789
        int expected = (int) (lt.toNanoOfDay() / 1_000_000L); // milliseconds of day
        assertEquals(expected, LocalTimeConversions.toInteger(lt, converter));
    }

    @Test
    public void testToInteger_Midnight() {
        LocalTime lt = LocalTime.MIDNIGHT; // 00:00:00.000
        assertEquals(0, LocalTimeConversions.toInteger(lt, converter));
    }

    @Test
    public void testToLong() {
        LocalTime lt = LocalTime.of(23, 59, 59, 999_999_999); // last nanosecond of the day
        long expected = lt.toNanoOfDay() / 1_000_000L;
        assertEquals(expected, LocalTimeConversions.toLong(lt, converter));
    }

    @Test
    public void testToDouble() {
        LocalTime lt = LocalTime.of(1, 2, 3, 400_000_000); // 01:02:03.400
        double expected = lt.toNanoOfDay() / 1_000_000_000.0;
        assertEquals(expected, LocalTimeConversions.toDouble(lt, converter), 0.0);
    }

    @Test
    public void testToBigInteger() {
        LocalTime lt = LocalTime.of(5, 6, 7, 800_000_000);
        BigInteger expected = BigInteger.valueOf(lt.toNanoOfDay());
        assertEquals(expected, LocalTimeConversions.toBigInteger(lt, converter));
    }

    @Test
    public void testToBigDecimal_ScaleAndRounding() {
        // Use a time with a nanosecond component that requires rounding to 9 decimal places
        LocalTime lt = LocalTime.of(0, 0, 0, 123_456_789); // 0.123456789 seconds
        BigDecimal expected = new BigDecimal(lt.toNanoOfDay())
                .divide(LocalTimeConversions.BILLION, 9, RoundingMode.HALF_UP);
        assertEquals(0, expected.compareTo(LocalTimeConversions.toBigDecimal(lt, converter)));
    }

    @Test
    public void testToAtomicInteger() {
        LocalTime lt = LocalTime.of(2, 0, 0, 0); // exactly 2 hours -> 7_200_000 ms
        AtomicInteger ai = LocalTimeConversions.toAtomicInteger(lt, converter);
        assertEquals(7_200_000, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        LocalTime lt = LocalTime.of(0, 0, 1, 500_000_000); // 1.5 seconds -> 1_500 ms
        AtomicLong al = LocalTimeConversions.toAtomicLong(lt, converter);
        assertEquals(1_500L, al.get());
    }

    @Test
    public void testToString() {
        LocalTime lt = LocalTime.of(9, 8, 7, 654_321_000);
        String expected = lt.format(DateTimeFormatter.ISO_LOCAL_TIME);
        assertEquals(expected, LocalTimeConversions.toString(lt, converter));
    }

    @Test
    public void testToCalendar_FieldsMatchLocalTime() {
        LocalTime lt = LocalTime.of(15, 30, 45, 250_000_000); // 15:30:45.250
        Calendar cal = LocalTimeConversions.toCalendar(lt, converter);

        // Verify that the calendar uses the same time‑zone as the converter options
        TimeZone tzFromConverter = converter.getOptions().getTimeZone();
        assertEquals(tzFromConverter, cal.getTimeZone());

        // Verify hour, minute, second and millisecond fields
        assertEquals(lt.getHour(), cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(lt.getMinute(), cal.get(Calendar.MINUTE));
        assertEquals(lt.getSecond(), cal.get(Calendar.SECOND));
        assertEquals(lt.getNano() / 1_000_000, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testToCalendar_EdgeCase_NanoRounding() {
        // 00:00:00.999999999 should become 999 ms (floor of nanoseconds → ms)
        LocalTime lt = LocalTime.of(0, 0, 0, 999_999_999);
        Calendar cal = LocalTimeConversions.toCalendar(lt, converter);
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(999, cal.get(Calendar.MILLISECOND));
    }
}
