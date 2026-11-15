package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unit tests for {@link AtomicLongConversions}.
 * The tests cover:
 * <ul>
 *   <li>{@link AtomicLongConversions#toAtomicLong(Object, Converter)}</li>
 *   <li>{@link AtomicLongConversions#toLocalTime(Object, Converter)}</li>
 * </ul>
 * No mocking frameworks are used; real objects are created as required.
 */
public class AtomicLongConversionsTest {

    private Converter converter; // a minimal converter instance (may be null‑acceptable)

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance.
        // Passing null is acceptable for the purposes of these tests because
        // the conversion methods under test do not depend on any Converter state.
        this.converter = new Converter(null);
    }

    /**
     * Verify that {@code toAtomicLong} returns a new {@link AtomicLong}
     * containing the same numeric value as the source, but is not the same instance.
     */
    @Test
    public void testToAtomicLongCreatesNewInstanceWithSameValue() {
        long[] testValues = {0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 123456789L};

        for (long val : testValues) {
            AtomicLong original = new AtomicLong(val);
            AtomicLong result = AtomicLongConversions.toAtomicLong(original, converter);

            assertNotNull("Resulting AtomicLong should not be null", result);
            assertNotSame("Result should be a different instance than the source", original, result);
            assertEquals("Resulting AtomicLong should contain the same value as the source", original.get(), result.get());
        }
    }

    /**
     * Verify that {@code toLocalTime} correctly delegates to {@link LongConversions#toLocalTime}.
     * The test uses the same static method directly to compute the expected value.
     */
    @Test
    public void testToLocalTimeDelegatesToLongConversions() {
        // Choose a variety of epoch‑millisecond values that map to distinct LocalTime values.
        long[] epochMillis = {
                0L,                         // 00:00:00
                1_000L,                     // 00:00:01
                60_000L,                    // 00:01:00
                3_600_000L,                 // 01:00:00
                12_345_678L,                // arbitrary time
                86_399_999L,                // one millisecond before midnight
                86_400_000L,                // exactly midnight next day (wraps to 00:00:00)
        };

        for (long ms : epochMillis) {
            AtomicLong source = new AtomicLong(ms);
            LocalTime expected = LongConversions.toLocalTime(ms, converter);
            LocalTime actual = AtomicLongConversions.toLocalTime(source, converter);

            assertNotNull("Converted LocalTime should not be null", actual);
            assertEquals("LocalTime conversion should match LongConversions result for millis: " + ms,
                         expected, actual);
        }
    }

    /**
     * Edge case: ensure that passing a null source throws a {@link ClassCastException}
     * because the implementation casts the input to {@link AtomicLong} without a null check.
     */
    @Test(expected = ClassCastException.class)
    public void testToAtomicLongNullSourceThrowsException() {
        AtomicLongConversions.toAtomicLong(null, converter);
    }

    /**
     * Edge case: ensure that passing an object of the wrong type throws a {@link ClassCastException}.
     */
    @Test(expected = ClassCastException.class)
    public void testToLocalTimeWrongTypeThrowsException() {
        // Pass a plain Long instead of AtomicLong.
        AtomicLongConversions.toLocalTime(Long.valueOf(123L), converter);
    }
}
