package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static com.cedarsoftware.util.UniqueIdGenerator.getInstant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UniqueIdGenerator_getInstant_Test {

    private static Stream<Arguments> provideIdsForSuccessfulConversion() {
        return Stream.of(// Edge case: zero
        Arguments.of(0L, Instant.ofEpochMilli(0L)), // Small positive values that truncate to 0 ms
        Arguments.of(1L, Instant.ofEpochMilli(0L)), Arguments.of(99L, Instant.ofEpochMilli(0L)), // Exactly one divisor unit
        Arguments.of(100_000L, Instant.ofEpochMilli(1L)), // Just above divisor, still 1 ms
        Arguments.of(100_001L, Instant.ofEpochMilli(1L)), // Typical positive value
        Arguments.of(123_456_789L, Instant.ofEpochMilli(123_456_789L / 100_000L)), // Large typical value
        Arguments.of(12_345_678_900L, Instant.ofEpochMilli(12_345_678_900L / 100_000L)), // Long.MAX_VALUE
        Arguments.of(Long.MAX_VALUE, Instant.ofEpochMilli(Long.MAX_VALUE / 100_000L)), // Long.MAX_VALUE - 1 (explicit expected millis from JSON)
        Arguments.of(Long.MAX_VALUE - 1, Instant.ofEpochMilli((Long.MAX_VALUE - 1) / 100_000L)));
    }

    private static Stream<Arguments> provideNegativeIds() {
        return Stream.of(Arguments.of(-1L), Arguments.of(Long.MIN_VALUE));
    }

    @Test
    void getInstant_convertsPositiveIdToInstant() {
        // Arrange: choose a deterministic positive uniqueId.
        // arbitrary positive number
        long uniqueId = 12_345_678_900L;
        // integer division as in the method
        long expectedEpochMilli = uniqueId / 100_000L;
        Instant expectedInstant = Instant.ofEpochMilli(expectedEpochMilli);
        // Act
        Instant actualInstant = getInstant(uniqueId);
        // Assert
        assertEquals(expectedInstant, actualInstant, "Instant should represent uniqueId divided by 100_000 milliseconds");
    }

    @Test
    void getInstant_handlesMaximumLongValue() {
        // Arrange: the maximum positive long value.
        long uniqueId = Long.MAX_VALUE;
        long expectedEpochMilli = uniqueId / 100_000L;
        Instant expectedInstant = Instant.ofEpochMilli(expectedEpochMilli);
        // Act
        Instant actualInstant = getInstant(uniqueId);
        // Assert
        assertEquals(expectedInstant, actualInstant, "Instant conversion must work for the maximum long value");
    }

    @Test
    void getInstant_negativeId_throwsIllegalArgumentException() {
        // Arrange
        long negativeId = -1L;
        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> getInstant(negativeId), "Calling getInstant with a negative ID should throw IllegalArgumentException");
        assertTrue(ex.getMessage().contains("Invalid uniqueId"), "Exception message should indicate the ID is invalid");
    }

    /* ------------------------------------------------------------
     *  Parameterized tests for successful conversions
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "[{index}] uniqueId={0} → expectedInstant={1}")
    @MethodSource("provideIdsForSuccessfulConversion")
    void getInstant_convertsIdToInstant_success(long uniqueId, Instant expectedInstant) {
        // Act
        Instant actual = getInstant(uniqueId);
        // Assert
        assertEquals(expectedInstant, actual, () -> "Instant for uniqueId " + uniqueId + " should be " + expectedInstant);
    }

    /* ------------------------------------------------------------
     *  Individual explicit tests matching the original JSON description
     * ------------------------------------------------------------ */
    @Test
    void getInstant_zeroId_returnsEpochInstant() {
        // Arrange
        long uniqueId = 0L;
        Instant expected = Instant.ofEpochMilli(0L);
        // Act
        Instant actual = getInstant(uniqueId);
        // Assert
        assertEquals(expected, actual, "Zero uniqueId must map to the epoch instant");
    }

    @Test
    void getInstant_longMaxMinusOne_matchesDocumentedMillis() {
        // Arrange
        long uniqueId = Long.MAX_VALUE - 1;
        // JSON expects 92233720368547 milliseconds
        long expectedMillis = 92_233_720_368_547L;
        Instant expected = Instant.ofEpochMilli(expectedMillis);
        // Act
        Instant actual = getInstant(uniqueId);
        // Assert
        assertEquals(expected, actual, "Instant for Long.MAX_VALUE-1 should equal the documented millisecond value");
    }
}
