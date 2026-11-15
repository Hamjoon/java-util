package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
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
     * Supplies the valid test data extracted from the JSON specification.
     *
     * @return a stream of arguments: {@code uniqueId19, expectedEpochMilli}
     */
    private static Stream<Arguments> validInputs() {
        return Stream.of(// Zero and sub‑million values – all map to epoch (0 ms)
        Arguments.of(0L, 0L), Arguments.of(1L, 0L), // Values just above the threshold
        // 1 500 000 / 1_000_000 = 1 ms
        Arguments.of(1_500_000L, 1L), // floor division
        Arguments.of(1_234_567_890_123L, 1_234_567L), // Maximum long value – still fits into a long after division
        Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE / 1_000_000L), // Additional edge cases from the second group
        // just below 1 000 000
        Arguments.of(999_999L, 0L), // exactly 1 ms
        Arguments.of(1_000_000L, 1L), // just under 2 ms
        Arguments.of(1_999_999L, 1L), // exactly 2 ms
        Arguments.of(2_000_000L, 2L), // 5 seconds
        Arguments.of(5_000_000_000L, 5_000L), // exactly one day
        Arguments.of(86_400_000_000_000L, 86_400_000L));
    }

    /**
     * Supplies the negative test data extracted from the JSON specification.
     *
     * @return a stream of arguments: {@code uniqueId19}
     */
    private static Stream<Arguments> invalidInputs() {
        return Stream.of(Arguments.of(-1L), // -9_223_372_036_854_775_808L
        Arguments.of(Long.MIN_VALUE));
    }

    @Test
    @DisplayName("Valid uniqueId19 is converted to the correct Instant")
    void testValidConversion() {
        // Arrange: current epoch milliseconds and a valid 19‑digit unique ID.
        long nowMillis = System.currentTimeMillis();
        // sequence & serverId = 0
        long uniqueId19 = nowMillis * 1_000_000L;
        // Add a non‑zero remainder to ensure it is ignored by the method.
        long remainder = 123_456L;
        long uniqueId19WithRemainder = uniqueId19 + remainder;
        // Act
        Instant instantFromExact = getInstant19(uniqueId19);
        Instant instantFromRemainder = getInstant19(uniqueId19WithRemainder);
        // Assert
        assertEquals(nowMillis, instantFromExact.toEpochMilli(), "Instant should represent the original epoch milliseconds");
        assertEquals(nowMillis, instantFromRemainder.toEpochMilli(), "Remainder bits must not affect the calculated Instant");
    }

    @Test
    @DisplayName("Negative uniqueId19 throws IllegalArgumentException")
    void testNegativeIdThrows() {
        // Arrange
        long negativeId = -1L;
        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> getInstant19(negativeId), "Method should reject negative uniqueId19 values");
        assertTrue(ex.getMessage().contains("Invalid uniqueId19"), "Exception message should indicate the cause");
    }

    @Test
    @DisplayName("Valid uniqueId19 is converted to the correct Instant (real‑time example)")
    void testValidConversionRealTime() {
        // Arrange: current epoch milliseconds and a valid 19‑digit unique ID.
        long nowMillis = System.currentTimeMillis();
        // sequence & serverId = 0
        long uniqueId19 = nowMillis * 1_000_000L;
        // Add a non‑zero remainder to ensure it is ignored by the method.
        long remainder = 123_456L;
        long uniqueId19WithRemainder = uniqueId19 + remainder;
        // Act
        Instant instantFromExact = getInstant19(uniqueId19);
        Instant instantFromRemainder = getInstant19(uniqueId19WithRemainder);
        // Assert
        assertEquals(nowMillis, instantFromExact.toEpochMilli(), "Instant should represent the original epoch milliseconds");
        assertEquals(nowMillis, instantFromRemainder.toEpochMilli(), "Remainder bits must not affect the calculated Instant");
    }

    @Test
    @DisplayName("Negative uniqueId19 throws IllegalArgumentException (single case)")
    void testNegativeIdThrowsSingle() {
        // Arrange
        long negativeId = -1L;
        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> getInstant19(negativeId), "Method should reject negative uniqueId19 values");
        assertTrue(ex.getMessage().contains("Invalid uniqueId19"), "Exception message should indicate the cause");
    }
}
