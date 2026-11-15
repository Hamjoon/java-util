package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import static com.cedarsoftware.util.UniqueIdGenerator.getInstant19;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UniqueIdGenerator_getInstant19_Test {

    /**
     * Supplies the positive test data.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>the raw {@code uniqueId19} value</li>
     *   <li>the {@link Instant} that should be produced (computed as
     *       {@code Instant.ofEpochMilli(uniqueId19 / 1_000_000L)})</li>
     * </ul>
     */
    private static Stream<Arguments> positiveCasesProvider() {
        return Stream.of(// Edge case: smallest non‑negative value
        Arguments.of(0L, Instant.ofEpochMilli(0L)), // Typical case from the JSON spec
        Arguments.of(1_234_567_890L, Instant.ofEpochMilli(1_234L)), // Large value still within long range
        Arguments.of(999_999_999_999L, Instant.ofEpochMilli(999_999L)), // Exact multiples of 1_000_000 (separate group but also valid here)
        Arguments.of(1_000_000L, Instant.ofEpochMilli(1L)), Arguments.of(2_000_000L, Instant.ofEpochMilli(2L)));
    }

    /**
     * Supplies the negative test data defined in the JSON specification.
     */
    private static Stream<Arguments> negativeCasesProvider() {
        return Stream.of(// Simple negative value
        Arguments.of(-1L), // Minimum long value (Long.MIN_VALUE)
        Arguments.of(Long.MIN_VALUE), // Negative multiple of 1_000_000
        Arguments.of(-1_000_000L));
    }

    /**
     * Verify that a positive uniqueId19 value is correctly converted to an {@link Instant}
     * using the same logic as the production method.
     */
    @Test
    void testGetInstant19Valid() {
        // Choose an arbitrary positive value that is comfortably within the range of a long.
        // corresponds to epoch milli = 1_638_123_456 (≈ 2021‑11‑23)
        long uniqueId19 = 1_638_123_456_789_012L;
        Instant expected = Instant.ofEpochMilli(uniqueId19 / 1_000_000L);
        Instant actual = getInstant19(uniqueId19);
        assertEquals(expected, actual, "Instant derived from uniqueId19 does not match expected value");
    }

    /**
     * Verify that a negative uniqueId19 value triggers the documented {@link IllegalArgumentException}.
     */
    @Test
    void testGetInstant19Negative() {
        long negativeId = -1L;
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> getInstant19(negativeId), "Expected getInstant19(negative) to throw, but it didn't");
        assertTrue(thrown.getMessage().contains("Invalid uniqueId19"), "Exception message should contain indication of invalid input");
    }

    /**
     * Edge‑case test: the smallest positive value (1) should be handled without overflow.
     */
    @Test
    void testGetInstant19SmallestPositive() {
        // epoch milli = 0 (since integer division truncates)
        long uniqueId19 = 1L;
        Instant expected = Instant.ofEpochMilli(0L);
        Instant actual = getInstant19(uniqueId19);
        assertEquals(expected, actual, "Instant for the smallest positive uniqueId19 should be epoch start");
    }

    /**
     * Parameterized test that verifies correct {@link Instant} values for a range
     * of positive {@code uniqueId19} inputs, including the smallest non‑negative
     * value (0) and typical large values.
     */
    @ParameterizedTest(name = "[{index}] uniqueId19={0} → expected={1}")
    @MethodSource("positiveCasesProvider")
    void testGetInstant19_Positive(long uniqueId19, Instant expected) {
        Instant actual = getInstant19(uniqueId19);
        assertEquals(expected, actual, () -> "Instant derived from uniqueId19=" + uniqueId19 + " does not match expected");
    }

    /**
     * Parameterized test that ensures any negative {@code uniqueId19} value
     * results in an {@link IllegalArgumentException} with the expected message.
     */
    @ParameterizedTest(name = "[{index}] uniqueId19={0} → throws IAE")
    @MethodSource("negativeCasesProvider")
    void testGetInstant19_Negative(long negativeUniqueId19) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> getInstant19(negativeUniqueId19), "Expected IllegalArgumentException for negative uniqueId19=" + negativeUniqueId19);
        assertTrue(ex.getMessage().contains("Invalid uniqueId19"), () -> "Exception message should contain 'Invalid uniqueId19', but was: " + ex.getMessage());
    }
}
