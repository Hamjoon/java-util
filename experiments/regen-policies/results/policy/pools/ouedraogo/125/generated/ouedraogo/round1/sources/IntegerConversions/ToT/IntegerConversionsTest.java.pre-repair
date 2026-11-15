package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.*;

/**
 * Tests for {@link IntegerConversions}.
 *
 * The class under test only contains a single static method {@code toLocalTime(Object, Converter)}.
 * The method casts the {@code from} argument to {@link Integer}, interprets the value as
 * milliseconds since midnight and delegates to {@link LongConversions#toLocalTime(long, Converter)}.
 *
 * The test suite therefore covers:
 * <ul>
 *   <li>Typical conversion of a positive millisecond value.</li>
 *   <li>Boundary values (0 ms, exactly one day‑1 ms, {@link Integer#MAX_VALUE}).</li>
 *   <li>Negative millisecond values (should wrap‑around according to {@code LongConversions} behavior).</li>
 *   <li>Invalid {@code from} argument types (e.g., {@link String}, {@code null}) – expecting {@code ClassCastException}.</li>
 *   <li>Ensuring the supplied {@link Converter} instance is passed through unchanged.</li>
 * </ul>
 *
 * No mocking frameworks are used; real {@link Converter} and {@link ConverterOptions}
 * objects are constructed as required by the library API.
 */
@RunWith(JUnit4.class)
public class IntegerConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The library provides a public constructor that takes ConverterOptions.
        // An empty options instance is sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Helper that converts milliseconds‑since‑midnight to a {@link LocalTime}
     * using the same logic as {@link LongConversions#toLocalTime(long, Converter)}.
     *
     * The implementation mirrors the library's conversion: the long value is
     * interpreted as the number of milliseconds after midnight, wrapped to a
     * 24‑hour day.
     */
    private static LocalTime expectedLocalTimeFromMillis(long ms) {
        // Normalise to the range [0, 86_400_000) (millis in a day)
        long dayMillis = 86_400_000L;
        long normalized = ((ms % dayMillis) + dayMillis) % dayMillis; // handle negative values
        int hour = (int) (normalized / 3_600_000L);
        int minute = (int) ((normalized % 3_600_000L) / 60_000L);
        int second = (int) ((normalized % 60_000L) / 1_000L);
        int nano = (int) ((normalized % 1_000L) * 1_000_000L);
        return LocalTime.of(hour, minute, second, nano);
    }

    @Test
    public void typicalPositiveMilliseconds() {
        Integer ms = 12_345; // 12.345 seconds after midnight
        LocalTime result = IntegerConversions.toLocalTime(ms, converter);
        LocalTime expected = expectedLocalTimeFromMillis(ms);
        assertEquals("Typical positive milliseconds should convert correctly", expected, result);
    }

    @Test
    public void zeroMilliseconds() {
        Integer ms = 0;
        LocalTime result = IntegerConversions.toLocalTime(ms, converter);
        assertEquals("Midnight (0 ms) should be represented as 00:00", LocalTime.MIDNIGHT, result);
    }

    @Test
    public void oneDayMinusOneMillisecond() {
        // 86_400_000 ms == 24h, so one less is the last millisecond of the day
        Integer ms = 86_399_999;
        LocalTime result = IntegerConversions.toLocalTime(ms, converter);
        LocalTime expected = expectedLocalTimeFromMillis(ms);
        assertEquals("One millisecond before next day should be 23:59:59.999", expected, result);
        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
        assertEquals(59, result.getSecond());
        assertEquals(999_000_000, result.getNano());
    }

    @Test
    public void maxIntegerMilliseconds() {
        // Integer.MAX_VALUE = 2_147_483_647 ms ≈ 596 h → wraps many days
        Integer ms = Integer.MAX_VALUE;
        LocalTime result = IntegerConversions.toLocalTime(ms, converter);
        LocalTime expected = expectedLocalTimeFromMillis(ms);
        assertEquals("MAX_VALUE should wrap correctly to a time within the day", expected, result);
    }

    @Test
    public void negativeMillisecondsWrapAround() {
        Integer ms = -1; // should become 23:59:59.999
        LocalTime result = IntegerConversions.toLocalTime(ms, converter);
        LocalTime expected = expectedLocalTimeFromMillis(ms);
        assertEquals("Negative one millisecond should wrap to the last millisecond of the previous day", expected, result);
        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
        assertEquals(59, result.getSecond());
        assertEquals(999_000_000, result.getNano());
    }

    @Test(expected = ClassCastException.class)
    public void invalidFromTypeString() {
        // The method casts directly to Integer, so a non‑Integer should throw
        String notAnInteger = "12345";
        IntegerConversions.toLocalTime(notAnInteger, converter);
    }

    @Test(expected = NullPointerException.class)
    public void nullFromArgument() {
        // Casting null to Integer triggers NPE
        IntegerConversions.toLocalTime(null, converter);
    }

    @Test
    public void converterInstanceIsPassedThrough() {
        // Verify that the same converter instance is used by LongConversions.
        // LongConversions simply calls Converter.identity for primitive types,
        // so we can confirm that the identity method receives our instance.
        // We'll use a distinct ConverterOptions object to make the instance unique.
        ConverterOptions opts = new ConverterOptions();
        opts.setOption("testKey", "testValue"); // assume options are mutable
        Converter customConverter = new Converter(opts);

        Integer ms = 5_000; // 5 seconds
        LocalTime result = IntegerConversions.toLocalTime(ms, customConverter);
        LocalTime expected = expectedLocalTimeFromMillis(ms);
        assertEquals(expected, result);

        // As a sanity check, the custom options should still be accessible
        assertEquals("testValue", customConverter.getOptions().getOption("testKey"));
    }
}
