package com.cedarsoftware.util.convert;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * JUnit4 test suite for {@link AtomicLongConversions}.
 * <p>
 * The class under test is package‑private, therefore the test class lives in the same
 * package to gain access to its static methods.
 */
public class AtomicLongConversionsTest {

    /** Helper to create a minimal {@link Converter} instance. */
    private static Converter createConverter() {
        // ConverterOptions is part of the public API; using its default constructor
        // provides a functional Converter that does not interfere with the methods
        // we are testing (the converter is only forwarded to LongConversions).
        return new Converter(new ConverterOptions());
    }

    @Test
    public void testToAtomicLongCreatesNewInstanceWithSameValue() {
        AtomicLong original = new AtomicLong(12345L);
        Converter converter = createConverter();

        AtomicLong result = AtomicLongConversions.toAtomicLong(original, converter);

        // The result must be a different instance but hold the same numeric value.
        assertNotSame("Resulting AtomicLong should be a new instance", original, result);
        assertEquals("Resulting AtomicLong should contain the same value",
                original.get(), result.get());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLongWithInvalidSourceThrows() {
        // Passing a non‑AtomicLong object should cause a ClassCastException
        // because the method unconditionally casts the argument.
        Converter converter = createConverter();
        AtomicLongConversions.toAtomicLong("not an atomic long", converter);
    }

    @Test
    public void testToLocalTimeDelegatesToLongConversions() {
        Converter converter = createConverter();

        // Test a range of values, including edge cases.
        long[] testValues = {
                0L,                                 // midnight
                1_000L,                             // 1 second (1000 ms)
                12 * 60 * 60 * 1_000L,              // noon (12h in ms)
                -5_000L,                            // negative value
                Long.MAX_VALUE / 2                  // large positive value
        };

        for (long millis : testValues) {
            AtomicLong atomic = new AtomicLong(millis);

            // Expected value obtained by calling the underlying conversion directly.
            LocalTime expected = LongConversions.toLocalTime(millis, converter);
            LocalTime actual = AtomicLongConversions.toLocalTime(atomic, converter);

            assertEquals("LocalTime conversion should match LongConversions for millis = " + millis,
                    expected, actual);
        }
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalTimeWithInvalidSourceThrows() {
        Converter converter = createConverter();
        // Provide an object that is not an AtomicLong; the method should cast and fail.
        AtomicLongConversions.toLocalTime(42, converter);
    }
}
