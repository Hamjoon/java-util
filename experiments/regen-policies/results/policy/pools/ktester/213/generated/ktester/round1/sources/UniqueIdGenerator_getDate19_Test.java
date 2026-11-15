package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static com.cedarsoftware.util.UniqueIdGenerator.getDate19;
import static com.cedarsoftware.util.UniqueIdGenerator.getUniqueId19;
import static java.lang.Math.abs;
import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UniqueIdGenerator_getDate19_Test {

    private static Stream<Arguments> provideGetDate19BasicCases() {
        return Stream.of(// Boundary case: zero
        Arguments.of(0L, 0L), // Typical case: 123_456_789 / 1_000_000 = 123
        Arguments.of(123_456_789L, 123L), // Just below one million → truncates to 0
        Arguments.of(999_999L, 0L), // Negative value, division preserves sign (truncates toward zero)
        Arguments.of(-5_000_000L, -5L), // Maximum long value – ensure no overflow
        Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE / 1_000_000L));
    }

    private static Stream<Arguments> provideGetDate19AdditionalCases() {
        return Stream.of(// Minimum long value – division yields a negative number without overflow
        Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE / 1_000_000L), // Exact one‑million boundary → 1 ms
        Arguments.of(1_000_000L, 1L), // Just above one‑million → still 1 ms (integer division truncates)
        Arguments.of(1_000_001L, 1L), // Far below one‑million → 0 ms
        Arguments.of(1L, 0L), // Negative magnitude < 1_000_000 → truncates toward zero → 0 ms
        Arguments.of(-1L, 0L), // Negative just beyond one‑million → -1 ms
        Arguments.of(-1_000_001L, -1L), // Large positive value → 5_000 ms (5 seconds)
        Arguments.of(5_000_000_000L, 5_000L), // Large negative value → -5_000 ms
        Arguments.of(-5_000_000_000L, -5_000L));
    }

    /**
     * Verify that {@code getDate19} correctly converts the encoded timestamp
     * part of a 19‑digit unique ID back to a {@link Date}.
     */
    @Test
    void testGetDate19ConvertsKnownId() {
        // Choose a value where the timestamp part is obvious.
        // The timestamp portion is the high‑order 13 digits (milliseconds since epoch).
        // For this test we use a value that yields a clean division.
        // 1_620_000_000_123_456 / 1_000_000 = 1_620_000_000 ms
        long uniqueId19 = 1_620_000_000_123_456L;
        long expectedMillis = uniqueId19 / 1_000_000L;
        Date date = getDate19(uniqueId19);
        assertNotNull(date, "Returned Date should not be null");
        assertEquals(expectedMillis, date.getTime(), "Date.getTime() should equal the timestamp portion of the unique ID");
    }

    /**
     * Verify that converting a freshly generated ID yields a Date that is
     * essentially the current system time (within a small tolerance).
     */
    @Test
    void testGetDate19WithGeneratedId() {
        long generatedId = getUniqueId19();
        Date date = getDate19(generatedId);
        assertNotNull(date, "Returned Date should not be null");
        long now = currentTimeMillis();
        long diff = abs(date.getTime() - now);
        // The conversion loses the sequence and server‑id digits, so the
        // resulting time should be within a few milliseconds of the current time.
        assertTrue(diff < 5, () -> "Date derived from generated ID should be close to now; diff=" + diff + " ms");
    }

    /**
     * Verify that the method behaves correctly for the edge case of zero.
     */
    @Test
    void testGetDate19WithZero() {
        Date date = getDate19(0L);
        assertNotNull(date, "Returned Date should not be null");
        assertEquals(0L, date.getTime(), "When the unique ID is zero the resulting Date should represent the epoch (0 ms).");
    }

    /**
     * Parameterized test for the first group of cases defined in the JSON.
     * Each case supplies a {@code uniqueId19} value and the expected
     * {@link Date#getTime()} after the internal division by 1_000_000.
     */
    @ParameterizedTest(name = "[{index}] uniqueId19={0} → expectedMillis={1}")
    @MethodSource("provideGetDate19BasicCases")
    void testGetDate19_BasicCases(long uniqueId19, long expectedMillis) {
        Date result = getDate19(uniqueId19);
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(expectedMillis, result.getTime(), () -> "For uniqueId19=" + uniqueId19 + " expected millis=" + expectedMillis);
    }

    /**
     * Parameterized test for the additional cases defined in the JSON.
     */
    @ParameterizedTest(name = "[{index}] uniqueId19={0} → expectedMillis={1}")
    @MethodSource("provideGetDate19AdditionalCases")
    void testGetDate19_AdditionalCases(long uniqueId19, long expectedMillis) {
        Date result = getDate19(uniqueId19);
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(expectedMillis, result.getTime(), () -> "For uniqueId19=" + uniqueId19 + " expected millis=" + expectedMillis);
    }
}
