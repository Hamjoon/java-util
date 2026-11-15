package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unit tests for {@link AtomicLongConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Normal operation of each conversion method.</li>
 *   <li>Edge‑case values (zero, negative, {@code Long.MAX_VALUE}, {@code Long.MIN_VALUE}).</li>
 *   <li>Exception scenarios (null source, source of wrong type).</li>
 * </ul>
 * The class under test has package‑private static methods, therefore the test class
 * resides in the same package to allow direct access without reflection.
 * </p>
 */
public class AtomicLongConversionsTest {

    /** Helper to create a minimal {@link Converter} instance. */
    private static Converter createConverter() {
        // ConverterOptions has a public no‑arg constructor in the library.
        return new Converter(new ConverterOptions());
    }

    /* ---------------------------------------------------------------------- */
    /*                       toAtomicLong tests                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicLongCopiesValue() {
        AtomicLong original = new AtomicLong(12345L);
        Converter converter = createConverter();

        AtomicLong result = AtomicLongConversions.toAtomicLong(original, converter);

        Assert.assertNotNull("Result should not be null", result);
        Assert.assertEquals("Value should be copied correctly",
                original.get(), result.get());
    }

    @Test
    public void testToAtomicLongCreatesNewInstance() {
        AtomicLong original = new AtomicLong(999L);
        Converter converter = createConverter();

        AtomicLong result = AtomicLongConversions.toAtomicLong(original, converter);

        Assert.assertNotSame("A new AtomicLong instance must be created",
                original, result);
    }

    @Test
    public void testToAtomicLongWithZero() {
        AtomicLong original = new AtomicLong(0L);
        Converter converter = createConverter();

        AtomicLong result = AtomicLongConversions.toAtomicLong(original, converter);
        Assert.assertEquals(0L, result.get());
    }

    @Test
    public void testToAtomicLongWithLongMinValue() {
        AtomicLong original = new AtomicLong(Long.MIN_VALUE);
        Converter converter = createConverter();

        AtomicLong result = AtomicLongConversions.toAtomicLong(original, converter);
        Assert.assertEquals(Long.MIN_VALUE, result.get());
    }

    @Test
    public void testToAtomicLongWithLongMaxValue() {
        AtomicLong original = new AtomicLong(Long.MAX_VALUE);
        Converter converter = createConverter();

        AtomicLong result = AtomicLongConversions.toAtomicLong(original, converter);
        Assert.assertEquals(Long.MAX_VALUE, result.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNullSourceThrowsNPE() {
        Converter converter = createConverter();
        // The method attempts a cast before any null‑check, so a NPE is expected.
        AtomicLongConversions.toAtomicLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLongInvalidSourceThrowsClassCastException() {
        Converter converter = createConverter();
        // Pass an object that is not an AtomicLong.
        AtomicLongConversions.toAtomicLong("not an AtomicLong", converter);
    }

    /* ---------------------------------------------------------------------- */
    /*                       toLocalTime tests                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToLocalTimeDelegatesCorrectly() {
        AtomicLong source = new AtomicLong(13_500_000L); // 13.5 seconds past midnight
        Converter converter = createConverter();

        LocalTime expected = LongConversions.toLocalTime(source.get(), converter);
        LocalTime actual   = AtomicLongConversions.toLocalTime(source, converter);

        Assert.assertEquals("The conversion should delegate to LongConversions",
                expected, actual);
    }

    @Test
    public void testToLocalTimeWithZero() {
        AtomicLong source = new AtomicLong(0L);
        Converter converter = createConverter();

        LocalTime expected = LongConversions.toLocalTime(0L, converter);
        LocalTime actual   = AtomicLongConversions.toLocalTime(source, converter);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTimeWithNegativeValue() {
        AtomicLong source = new AtomicLong(-1L);
        Converter converter = createConverter();

        LocalTime expected = LongConversions.toLocalTime(-1L, converter);
        LocalTime actual   = AtomicLongConversions.toLocalTime(source, converter);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTimeWithLongMaxValue() {
        AtomicLong source = new AtomicLong(Long.MAX_VALUE);
        Converter converter = createConverter();

        LocalTime expected = LongConversions.toLocalTime(Long.MAX_VALUE, converter);
        LocalTime actual   = AtomicLongConversions.toLocalTime(source, converter);

        Assert.assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTimeNullSourceThrowsNPE() {
        Converter converter = createConverter();
        AtomicLongConversions.toLocalTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalTimeInvalidSourceThrowsClassCastException() {
        Converter converter = createConverter();
        AtomicLongConversions.toLocalTime(Integer.valueOf(10), converter);
    }
}
