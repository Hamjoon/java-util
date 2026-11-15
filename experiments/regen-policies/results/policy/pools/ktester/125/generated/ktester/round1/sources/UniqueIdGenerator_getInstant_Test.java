package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import static com.cedarsoftware.util.UniqueIdGenerator.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UniqueIdGenerator_getInstant_Test {

    /**
     * Tolerance in milliseconds when comparing generated timestamps with {@code System.currentTimeMillis()}.
     */
    private static final long TOLERANCE_MS = 5L;

    /**
     * Helper that turns the expected value from the JSON into a real {@link Instant}.
     */
    private static Instant parseExpected(String expected) {
        if (expected.startsWith("Instant.ofEpochMilli(")) {
            // Extract the long value inside the parentheses.
            String number = expected.substring("Instant.ofEpochMilli(".length(), expected.length() - 1);
            return Instant.ofEpochMilli(Long.parseLong(number));
        }
        // ISO‑8601 literals are directly parseable by Instant.
        return Instant.parse(expected);
    }

    private static Stream<Arguments> invalidUniqueIdValuesProvider() {
        return Stream.of(// from group “Invalid uniqueId values”
        Arguments.of(-1L));
    }

    private static Stream<Arguments> extremeNegativeValueProvider() {
        return Stream.of(// from group “Extreme negative value”
        Arguments.of(Long.MIN_VALUE));
    }

    private static Stream<Arguments> validEdgeAndTypicalProvider() {
        return Stream.of(Arguments.of(0L, "1970-01-01T00:00:00Z"), Arguments.of(99L, "1970-01-01T00:00:00Z"), Arguments.of(100000L, "1970-01-01T00:00:00.001Z"), Arguments.of(123456789L, "1970-01-01T00:00:01.234Z"), Arguments.of(Long.MAX_VALUE, "Instant.ofEpochMilli(92233720368547)"));
    }

    private static Stream<Arguments> divisionBoundaryProvider() {
        return Stream.of(Arguments.of(99999L, "1970-01-01T00:00:00Z"), Arguments.of(100000L, "1970-01-01T00:00:00.001Z"), Arguments.of(100001L, "1970-01-01T00:00:00.001Z"), Arguments.of(199999L, "1970-01-01T00:00:00.001Z"), Arguments.of(200000L, "1970-01-01T00:00:00.002Z"));
    }

    private static Stream<Arguments> justAboveThresholdProvider() {
        return Stream.of(Arguments.of(300001L, "1970-01-01T00:00:00.003Z"), Arguments.of(999999L, "1970-01-01T00:00:00.009Z"), Arguments.of(1000000L, "1970-01-01T00:00:00.010Z"));
    }

    private static Stream<Arguments> largeNonMaxProvider() {
        return Stream.of(Arguments.of(1234567890123L, "1970-01-01T03:25:45.678Z"), Arguments.of(9223372036854675807L, "Instant.ofEpochMilli(92233720368546)"), Arguments.of(9223372036854775806L, "Instant.ofEpochMilli(92233720368547)"));
    }

    @Test
    void getInstantFromUniqueId_returnsCurrentInstant() {
        // Arrange – obtain a fresh unique identifier.
        long id = getUniqueId();
        // Act – convert the identifier to an Instant.
        Instant instant = getInstant(id);
        // Assert – the instant should be very close to the current time.
        long now = System.currentTimeMillis();
        long diff = Math.abs(instant.toEpochMilli() - now);
        assertTrue(diff <= TOLERANCE_MS, () -> "Instant differs from now by " + diff + " ms, which exceeds tolerance of " + TOLERANCE_MS + " ms");
    }

    @Test
    void getInstant19FromUniqueId19_returnsCurrentInstant() {
        // Arrange – obtain a fresh 19‑digit unique identifier.
        long id19 = getUniqueId19();
        // Act – convert the identifier to an Instant using the 19‑digit variant.
        Instant instant = getInstant19(id19);
        // Assert – the instant should be very close to the current time.
        long now = System.currentTimeMillis();
        long diff = Math.abs(instant.toEpochMilli() - now);
        assertTrue(diff <= TOLERANCE_MS, () -> "Instant19 differs from now by " + diff + " ms, which exceeds tolerance of " + TOLERANCE_MS + " ms");
    }

    @Test
    void getInstant_negativeId_throwsIllegalArgumentException() {
        // Arrange – a negative unique identifier.
        long negativeId = -12345L;
        // Act & Assert – the method must reject the negative value.
        assertThrows(IllegalArgumentException.class, () -> getInstant(negativeId));
    }

    @Test
    void getInstant_zeroId_returnsEpochInstant() {
        // Arrange – the edge case of zero.
        long zeroId = 0L;
        // Act – conversion should succeed and yield the epoch instant.
        Instant instant = getInstant(zeroId);
        // Assert – the epoch instant corresponds to 0 milliseconds since the epoch.
        assertEquals(0L, instant.toEpochMilli(), "Instant for ID 0 should represent the epoch (0 ms since 1970‑01‑01T00:00:00Z)");
    }

    @ParameterizedTest(name = "{index} => uniqueId={0} (should throw)")
    @MethodSource("invalidUniqueIdValuesProvider")
    void getInstant_invalidUniqueIdValues_throwsIllegalArgumentException(long uniqueId) {
        assertThrows(IllegalArgumentException.class, () -> getInstant(uniqueId));
    }

    @ParameterizedTest(name = "{index} => uniqueId={0} (extreme negative, should throw)")
    @MethodSource("extremeNegativeValueProvider")
    void getInstant_extremeNegativeValue_throwsIllegalArgumentException(long uniqueId) {
        assertThrows(IllegalArgumentException.class, () -> getInstant(uniqueId));
    }

    @ParameterizedTest(name = "{index} => uniqueId={0} → expected={1}")
    @MethodSource("validEdgeAndTypicalProvider")
    void getInstant_validEdgeAndTypicalValues_returnsExpectedInstant(long uniqueId, String expectedStr) {
        Instant expected = parseExpected(expectedStr);
        Instant actual = getInstant(uniqueId);
        assertEquals(expected, actual, () -> "Unexpected Instant for uniqueId " + uniqueId);
    }

    @ParameterizedTest(name = "{index} => uniqueId={0} → expected={1}")
    @MethodSource("divisionBoundaryProvider")
    void getInstant_divisionBoundaryValues_returnExpectedInstant(long uniqueId, String expectedStr) {
        Instant expected = parseExpected(expectedStr);
        Instant actual = getInstant(uniqueId);
        assertEquals(expected, actual, () -> "Boundary test failed for uniqueId " + uniqueId);
    }

    @ParameterizedTest(name = "{index} => uniqueId={0} → expected={1}")
    @MethodSource("justAboveThresholdProvider")
    void getInstant_valuesJustAboveThresholds_returnExpectedInstant(long uniqueId, String expectedStr) {
        Instant expected = parseExpected(expectedStr);
        Instant actual = getInstant(uniqueId);
        assertEquals(expected, actual, () -> "Just‑above‑threshold test failed for uniqueId " + uniqueId);
    }

    @ParameterizedTest(name = "{index} => uniqueId={0} → expected={1}")
    @MethodSource("largeNonMaxProvider")
    void getInstant_largeNonMaxValues_returnExpectedInstant(long uniqueId, String expectedStr) {
        Instant expected = parseExpected(expectedStr);
        Instant actual = getInstant(uniqueId);
        assertEquals(expected, actual, () -> "Large‑value test failed for uniqueId " + uniqueId);
    }
}
