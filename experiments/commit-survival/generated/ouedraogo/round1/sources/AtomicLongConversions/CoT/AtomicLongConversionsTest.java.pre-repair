package com.cedarsoftware.util.convert;

import org.junit.Test;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class AtomicLongConversionsTest {

    // Helper method to create a default Converter instance.
    private Converter createDefaultConverter() {
        // Assuming ConverterOptions has a public no‑arg constructor.
        return new Converter(new ConverterOptions());
    }

    /* ----------------------------------------------------------------------
     * Tests for toAtomicLong(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicLongCopiesValue() {
        Converter converter = createDefaultConverter();
        AtomicLong source = new AtomicLong(42L);

        AtomicLong result = AtomicLongConversions.toAtomicLong(source, converter);

        assertNotNull("Result must not be null", result);
        assertEquals("Value must be copied from source", source.get(), result.get());
        assertNotSame("Result must be a new instance, not the same as source", source, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNullSource() {
        Converter converter = createDefaultConverter();
        // 'from' is null – the cast succeeds (null) but .get() on the null reference
        // throws NullPointerException.
        AtomicLongConversions.toAtomicLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLongInvalidSource() {
        Converter converter = createDefaultConverter();
        // Passing an object that is not an AtomicLong should cause a ClassCastException.
        AtomicLongConversions.toAtomicLong("not an AtomicLong", converter);
    }

    /* ----------------------------------------------------------------------
     * Tests for toLocalTime(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void testToLocalTimeDelegatesCorrectly() {
        Converter converter = createDefaultConverter();
        AtomicLong source = new AtomicLong(12_345_678L); // arbitrary epoch‑based value

        // Expected value obtained by calling the underlying LongConversions directly.
        LocalTime expected = LongConversions.toLocalTime(source.get(), converter);
        LocalTime actual = AtomicLongConversions.toLocalTime(source, converter);

        assertEquals("toLocalTime must delegate to LongConversions with the same value",
                     expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTimeNullSource() {
        Converter converter = createDefaultConverter();
        // Null source leads to a NullPointerException when the cast is dereferenced.
        AtomicLongConversions.toLocalTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalTimeInvalidSource() {
        Converter converter = createDefaultConverter();
        // Passing a non‑AtomicLong should cause a ClassCastException.
        AtomicLongConversions.toLocalTime(12345, converter);
    }

    @Test
    public void testToLocalTimeNegativeValue() {
        Converter converter = createDefaultConverter();
        AtomicLong source = new AtomicLong(-1L); // negative epoch value

        LocalTime expected = LongConversions.toLocalTime(source.get(), converter);
        LocalTime actual = AtomicLongConversions.toLocalTime(source, converter);

        assertEquals("Negative long values must be converted consistently",
                     expected, actual);
    }

    @Test
    public void testToLocalTimeZeroValue() {
        Converter converter = createDefaultConverter();
        AtomicLong source = new AtomicLong(0L); // epoch start

        LocalTime expected = LongConversions.toLocalTime(0L, converter);
        LocalTime actual = AtomicLongConversions.toLocalTime(source, converter);

        assertEquals("Zero value conversion must match LongConversions result",
                     expected, actual);
    }
}
