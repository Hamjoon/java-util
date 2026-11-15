package com.cedarsoftware.util.convert;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link AtomicLongConversions}.
 *
 * The class under test has package‑private static methods, therefore the test
 * class resides in the same package so that it can access them directly.
 *
 * The tests cover:
 *   • Normal behaviour (value is copied correctly)
 *   • Instance identity (a new {@code AtomicLong} is created)
 *   • Handling of {@code null} arguments (should throw {@link NullPointerException})
 *   • Handling of arguments of the wrong type (should throw {@link ClassCastException})
 *   • Edge values such as negative numbers and {@code Long.MAX_VALUE}/{@code Long.MIN_VALUE}
 */
public class AtomicLongConversionsTest {

    /* --------------------------------------------------------------------- */
    /*  Helper – a minimal Converter instance (the methods under test do not
     *  use the Converter, but the signature requires it).  The library
     *  provides a public constructor that accepts ConverterOptions; we can
     *  safely pass {@code null} because the conversion methods never dereference
     *  it.
     */
    private static final Converter DUMMY_CONVERTER = new Converter(null);
    /* --------------------------------------------------------------------- */

    /* -------------------------- toAtomicLong() -------------------------- */

    @Test
    public void testToAtomicLongCopiesValue() {
        AtomicLong source = new AtomicLong(12345L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(source, DUMMY_CONVERTER);
        assertEquals("The returned AtomicLong must contain the same value as the source",
                     source.get(), result.get());
    }

    @Test
    public void testToAtomicLongCreatesNewInstance() {
        AtomicLong source = new AtomicLong(9876L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(source, DUMMY_CONVERTER);
        assertNotSame("A new AtomicLong instance must be created (not the same reference)",
                      source, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNullSourceThrowsNPE() {
        AtomicLongConversions.toAtomicLong(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLongWrongTypeThrowsClassCastException() {
        // Passing a String instead of an AtomicLong should cause a cast failure.
        AtomicLongConversions.toAtomicLong("not an AtomicLong", DUMMY_CONVERTER);
    }

    @Test
    public void testToAtomicLongWithBoundaryValues() {
        AtomicLong max = new AtomicLong(Long.MAX_VALUE);
        AtomicLong min = new AtomicLong(Long.MIN_VALUE);
        AtomicLong zero = new AtomicLong(0L);

        assertEquals(Long.MAX_VALUE, AtomicLongConversions.toAtomicLong(max, DUMMY_CONVERTER).get());
        assertEquals(Long.MIN_VALUE, AtomicLongConversions.toAtomicLong(min, DUMMY_CONVERTER).get());
        assertEquals(0L, AtomicLongConversions.toAtomicLong(zero, DUMMY_CONVERTER).get());
    }

    /* -------------------------- toLocalTime() -------------------------- */

    @Test
    public void testToLocalTimeReturnsNonNull() {
        // 10:15:30.123 = 10*3600 + 15*60 + 30 seconds + 123 milliseconds
        long millisOfDay = ((10 * 60L + 15) * 60 + 30) * 1000 + 123;
        AtomicLong source = new AtomicLong(millisOfDay);
        LocalTime result = AtomicLongConversions.toLocalTime(source, DUMMY_CONVERTER);
        assertNotNull("Conversion should produce a non‑null LocalTime", result);
    }

    @Test
    public void testToLocalTimeNegativeValue() {
        // Negative values represent times before the epoch; LocalTime should still be produced.
        AtomicLong source = new AtomicLong(-1L);
        LocalTime result = AtomicLongConversions.toLocalTime(source, DUMMY_CONVERTER);
        assertNotNull("Conversion with negative value should not return null", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTimeNullSourceThrowsNPE() {
        AtomicLongConversions.toLocalTime(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalTimeWrongTypeThrowsClassCastException() {
        // Passing an Integer instead of an AtomicLong should cause a cast failure.
        AtomicLongConversions.toLocalTime(Integer.valueOf(5), DUMMY_CONVERTER);
    }

    @Test
    public void testToLocalTimeBoundaryValues() {
        // Long.MAX_VALUE milliseconds is far beyond a single day; LongConversions
        // is expected to wrap or truncate according to its implementation.
        // The test only verifies that no exception is thrown and a LocalTime is returned.
        AtomicLong max = new AtomicLong(Long.MAX_VALUE);
        LocalTime maxResult = AtomicLongConversions.toLocalTime(max, DUMMY_CONVERTER);
        assertNotNull("Conversion of Long.MAX_VALUE should return a LocalTime", maxResult);

        AtomicLong min = new AtomicLong(Long.MIN_VALUE);
        LocalTime minResult = AtomicLongConversions.toLocalTime(min, DUMMY_CONVERTER);
        assertNotNull("Conversion of Long.MIN_VALUE should return a LocalTime", minResult);
    }
}
