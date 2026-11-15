package com.cedarsoftware.util.convert;

import java.time.LocalTime;
import java.time.DateTimeException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link AtomicIntegerConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal operation of each conversion method.</li>
 *   <li>Behaviour when {@code null} is supplied for the {@code from} argument.</li>
 *   <li>Behaviour when an object of an unexpected type is supplied.</li>
 *   <li>Edge‑case values for the {@code toLocalTime} conversion (negative and out‑of‑range seconds).</li>
 * </ul>
 *
 * The {@code Converter} argument is ignored by the implementation, therefore a {@code null}
 * instance is sufficient for the purpose of these tests.
 */
public class AtomicIntegerConversionsTest {

    // ------------------------------------------------------------------------
    // Helper – a dummy Converter instance (the real implementation is not needed)
    // ------------------------------------------------------------------------
    private static final Converter DUMMY_CONVERTER = null; // allowed because methods ignore it

    // ------------------------------------------------------------------------
    //  Tests for toAtomicInteger(...)
    // ------------------------------------------------------------------------

    /** A simple successful conversion – the returned AtomicInteger must contain the same
     *  int value but be a different instance. */
    @Test
    public void testToAtomicInteger_Basic() {
        AtomicInteger source = new AtomicInteger(42);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, DUMMY_CONVERTER);
        assertNotNull(result);
        assertEquals(source.intValue(), result.intValue());
        assertNotSame("A new AtomicInteger instance should be created", source, result);
    }

    /** Passing {@code null} as the source should result in a {@link NullPointerException}
     *  because the method dereferences the casted value. */
    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_NullSource() {
        AtomicIntegerConversions.toAtomicInteger(null, DUMMY_CONVERTER);
    }

    /** Supplying an object of a type other than {@code AtomicInteger} must cause a
     *  {@link ClassCastException} during the cast. */
    @Test(expected = ClassCastException.class)
    public void testToAtomicInteger_WrongType() {
        AtomicIntegerConversions.toAtomicInteger("not an AtomicInteger", DUMMY_CONVERTER);
    }

    // ------------------------------------------------------------------------
    //  Tests for toLocalTime(...)
    // ------------------------------------------------------------------------

    /** Normal conversion – the method delegates to {@code LongConversions.toLocalTime}
     *  using the long value of the AtomicInteger.  For a value representing seconds‑of‑day,
     *  the expected LocalTime is {@code LocalTime.ofSecondOfDay(value)}. */
    @Test
    public void testToLocalTime_Basic() {
        int seconds = 3_600; // 01:00:00
        AtomicInteger source = new AtomicInteger(seconds);
        LocalTime expected = LocalTime.ofSecondOfDay(seconds);
        LocalTime actual = AtomicIntegerConversions.toLocalTime(source, DUMMY_CONVERTER);
        assertEquals(expected, actual);
    }

    /** {@code null} source leads to a {@link NullPointerException} when the code
     *  attempts to call {@code get()} on the casted value. */
    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullSource() {
        AtomicIntegerConversions.toLocalTime(null, DUMMY_CONVERTER);
    }

    /** Passing a non‑AtomicInteger object must raise a {@link ClassCastException}. */
    @Test(expected = ClassCastException.class)
    public void testToLocalTime_WrongType() {
        AtomicIntegerConversions.toLocalTime(12345, DUMMY_CONVERTER);
    }

    /** Negative second values are not valid for {@code LocalTime.ofSecondOfDay}
     *  and should cause a {@link DateTimeException} (thrown by the delegated
     *  {@code LongConversions.toLocalTime}). */
    @Test(expected = DateTimeException.class)
    public void testToLocalTime_NegativeSeconds() {
        AtomicInteger source = new AtomicInteger(-1);
        AtomicIntegerConversions.toLocalTime(source, DUMMY_CONVERTER);
    }

    /** Values equal to or greater than 86 400 (the number of seconds in a day)
     *  are out of the valid range for {@code LocalTime} and must result in a
     *  {@link DateTimeException}. */
    @Test(expected = DateTimeException.class)
    public void testToLocalTime_OverflowSeconds() {
        AtomicInteger source = new AtomicInteger(86_400);
        AtomicIntegerConversions.toLocalTime(source, DUMMY_CONVERTER);
    }
}
