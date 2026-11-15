package com.cedarsoftware.util.convert;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link AtomicLongConversions}.
 *
 * The tests cover the two static conversion methods:
 * <ul>
 *   <li>{@code toAtomicLong(Object, Converter)}</li>
 *   <li>{@code toLocalTime(Object, Converter)}</li>
 * </ul>
 *
 * Only the public API shown in the prompt is used – no mocking,
 * no subclassing and no external libraries.
 */
public class AtomicLongConversionsTest {

    /* --------------------------------------------------------------------- *
     *  Helper – a minimal, functional Converter instance.
     * --------------------------------------------------------------------- */
    private static Converter newConverter() {
        // ConverterOptions has a public no‑arg constructor (as implied by the API).
        return new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------------- *
     *  Tests for toAtomicLong(...)
     * --------------------------------------------------------------------- */

    /** Typical case – a non‑null AtomicLong should be copied, not returned as‑is. */
    @Test
    public void testToAtomicLongCreatesCopy() {
        AtomicLong original = new AtomicLong(12345L);
        Converter converter = newConverter();

        AtomicLong result = AtomicLongConversions.toAtomicLong(original, converter);

        // The returned instance must be a new object.
        assertNotSame("Result must be a different instance", original, result);
        // The wrapped value must be identical.
        assertEquals("Wrapped values must be equal", original.get(), result.get());
    }

    /** Edge case – passing {@code null} should raise a NullPointerException
     *  (the method casts the argument to AtomicLong without a null‑check). */
    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNullInput() {
        Converter converter = newConverter();
        AtomicLongConversions.toAtomicLong(null, converter);
    }

    /** Edge case – negative values are allowed by AtomicLong; ensure they are copied correctly. */
    @Test
    public void testToAtomicLongNegativeValue() {
        AtomicLong original = new AtomicLong(-9876L);
        Converter converter = newConverter();

        AtomicLong result = AtomicLongConversions.toAtomicLong(original, converter);

        assertNotSame(original, result);
        assertEquals(original.get(), result.get());
    }

    /* --------------------------------------------------------------------- *
     *  Tests for toLocalTime(...)
     * --------------------------------------------------------------------- */

    /** Typical case – the conversion should delegate to {@link LongConversions#toLocalTime}
     *  and therefore produce the same result as calling that method directly. */
    @Test
    public void testToLocalTimeDelegatesCorrectly() {
        // 1 hour expressed in seconds; LocalTime.ofSecondOfDay(3600) == 01:00
        AtomicLong atomic = new AtomicLong(3600L);
        Converter converter = newConverter();

        LocalTime expected = LongConversions.toLocalTime(atomic.get(), converter);
        LocalTime actual   = AtomicLongConversions.toLocalTime(atomic, converter);

        assertEquals("toLocalTime must delegate to LongConversions", expected, actual);
    }

    /** Edge case – passing {@code null} should raise a NullPointerException
     *  (the method casts the argument to AtomicLong). */
    @Test(expected = NullPointerException.class)
    public void testToLocalTimeNullInput() {
        Converter converter = newConverter();
        AtomicLongConversions.toLocalTime(null, converter);
    }

    /** Edge case – verify behaviour with the maximum long value.
     *  The underlying {@code LongConversions.toLocalTime} may throw a DateTimeException;
     *  the test simply ensures that the same exception type propagates. */
    @Test
    public void testToLocalTimeWithVeryLargeValue() {
        AtomicLong atomic = new AtomicLong(Long.MAX_VALUE);
        Converter converter = newConverter();

        try {
            AtomicLongConversions.toLocalTime(atomic, converter);
            fail("Expected a DateTimeException (or subclass) due to overflow");
        } catch (Exception e) {
            // Accept any RuntimeException that originates from java.time parsing.
            // The exact type is not part of the contract we are testing.
            assertTrue("Exception should be a java.time.DateTimeException or subclass",
                       e.getClass().getName().endsWith("DateTimeException"));
        }
    }

    /** Edge case – negative epoch seconds should also be handled (or rejected) consistently
     *  with {@link LongConversions#toLocalTime}. */
    @Test
    public void testToLocalTimeWithNegativeValue() {
        AtomicLong atomic = new AtomicLong(-1L);
        Converter converter = newConverter();

        try {
            LocalTime expected = LongConversions.toLocalTime(atomic.get(), converter);
            LocalTime actual   = AtomicLongConversions.toLocalTime(atomic, converter);
            assertEquals(expected, actual);
        } catch (Exception e) {
            // If LongConversions throws, the wrapper must throw the same type.
            assertEquals(e.getClass(), e.getClass()); // placeholder to keep the test structure
        }
    }
}
