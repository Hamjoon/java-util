package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link AtomicLongConversions}.
 *
 * These tests verify:
 * <ul>
 *   <li>Correct copying of the value when converting to a new {@link AtomicLong}.</li>
 *   <li>Proper handling of {@code null} and invalid source types.</li>
 *   <li>Delegation of the conversion to {@link LongConversions#toLocalTime(long, Converter)}.</li>
 *   <li>Behaviour for edge‑case numeric values (zero, negative, large values).</li>
 * </ul>
 */
public class AtomicLongConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The Converter class requires a ConverterOptions instance; use the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    /* ---------------------------------------------------------------------- */
    /* toAtomicLong tests                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toAtomicLongCreatesNewInstanceWithSameValue() {
        AtomicLong original = new AtomicLong(123L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(original, converter);

        assertNotSame("Result should be a new instance, not the original", original, result);
        assertEquals("Result should contain the same long value as the original", original.get(), result.get());
    }

    @Test
    public void toAtomicLongHandlesZeroValue() {
        AtomicLong original = new AtomicLong(0L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(original, converter);
        assertEquals(0L, result.get());
    }

    @Test
    public void toAtomicLongHandlesNegativeValue() {
        AtomicLong original = new AtomicLong(-987654321L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(original, converter);
        assertEquals(-987654321L, result.get());
    }

    @Test(expected = NullPointerException.class)
    public void toAtomicLongThrowsNPEWhenSourceIsNull() {
        AtomicLongConversions.toAtomicLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void toAtomicLongThrowsClassCastWhenSourceIsNotAtomicLong() {
        AtomicLongConversions.toAtomicLong("not an atomic long", converter);
    }

    /* ---------------------------------------------------------------------- */
    /* toLocalTime tests                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toLocalTimeDelegatesToLongConversions() {
        // Choose a value that maps cleanly to a LocalTime (seconds since start of day)
        long secondsSinceMidnight = 12 * 60 * 60 + 34 * 60 + 56; // 12:34:56
        AtomicLong source = new AtomicLong(secondsSinceMidnight);

        // Expected value computed directly via LongConversions (the class under test delegates to this)
        LocalTime expected = LongConversions.toLocalTime(secondsSinceMidnight, converter);

        LocalTime actual = AtomicLongConversions.toLocalTime(source, converter);
        assertEquals("AtomicLongConversions should return the same LocalTime as LongConversions",
                     expected, actual);
    }

    @Test
    public void toLocalTimeHandlesZero() {
        AtomicLong source = new AtomicLong(0L);
        LocalTime expected = LongConversions.toLocalTime(0L, converter);
        LocalTime actual = AtomicLongConversions.toLocalTime(source, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void toLocalTimeHandlesLargeValueWithinDay() {
        // 23:59:59 = 86,399 seconds
        long maxSecondsInDay = 23 * 60 * 60 + 59 * 60 + 59;
        AtomicLong source = new AtomicLong(maxSecondsInDay);
        LocalTime expected = LongConversions.toLocalTime(maxSecondsInDay, converter);
        LocalTime actual = AtomicLongConversions.toLocalTime(source, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void toLocalTimeThrowsNPEWhenSourceIsNull() {
        AtomicLongConversions.toLocalTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void toLocalTimeThrowsClassCastWhenSourceIsNotAtomicLong() {
        AtomicLongConversions.toLocalTime(12345, converter); // plain Integer, not AtomicLong
    }
}
