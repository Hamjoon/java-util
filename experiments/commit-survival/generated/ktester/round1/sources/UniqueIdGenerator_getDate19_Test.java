package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UniqueIdGenerator_getDate19_Test {

    /**
     * Supplies the test data extracted from the JSON test‑case list.
     */
    private static Stream<Arguments> provideGetDate19Cases() {
        return Stream.of(// Zero
        Arguments.of(0L, 0L, "Zero uniqueId should return the epoch (1970‑01‑01T00:00:00.000Z)"), // Small positive values around the 1 000 000 boundary
        Arguments.of(1_500_000L, 1L, "uniqueId just above one million yields 1 ms after epoch"), Arguments.of(999_999L, 0L, "uniqueId just below one million is truncated to 0 ms, returning the epoch"), Arguments.of(1_000_000L, 1L, "exactly one million divided by 1 000 000 yields 1 ms"), // Larger positive values
        Arguments.of(2_000_000L, 2L, "Exactly two million yields 2 ms after epoch"), Arguments.of(2_500_000L, 2L, "Two‑and‑a‑half million is truncated to 2 ms (integer division)"), Arguments.of(9_223_372_036_854_775_807L, 9_223_372_036_854L, "Long.MAX_VALUE divided by 1 000 000 results in the largest representable date (~2262‑02‑24)"), Arguments.of(1_234_567_890_123_456L, 1_234_567_890L, "Large positive value demonstrates correct truncation (1234567890123456 / 1 000 000)"), Arguments.of(999_999_999_999L, 999_999L, "Just under one trillion yields 999 999 ms (~16 min 39 s) after epoch")).flatMap(args -> Stream.of(// Negative counterparts – each positive case also has a negative variant where appropriate
        // (Only those defined in the original JSON are added)
        args, // Negative specific cases
        // (These are added separately to keep the order readable)
        // Note: we only add the negative cases that exist in the JSON spec.
        // The `flatMap` ensures they are combined with the positive cases above.
        // The following lines are separate arguments for clarity.
        // -------------------------------------------------------------
        // Negative values around the 1 000 000 boundary
        Arguments.of(-1_500_000L, -1L, "Negative uniqueId just below –1 000 000 yields –1 ms"), Arguments.of(-500_000L, 0L, "Negative uniqueId with absolute value < 1 000 000 truncates to 0 ms"), Arguments.of(-1_000_000L, -1L, "Exactly –1 000 000 yields –1 ms"), // Larger negative values
        Arguments.of(-2_000_000L, -2L, "Exactly –2 000 000 yields –2 ms"), Arguments.of(-1_999_999L, -1L, "Negative just above –2 000 000 yields –1 ms"), Arguments.of(-9_223_372_036_854_775_808L, -9_223_372_036_854L, "Long.MIN_VALUE divided by 1 000 000 results in the most negative representable date"), Arguments.of(-1_234_567_890_123_456L, -1_234_567_890L, "Large negative value demonstrates correct truncation toward zero")));
    }

    @Test
    @DisplayName("getDate19 returns correct Date for a typical positive uniqueId")
    void testGetDate19TypicalPositive() {
        // Arrange: a known epoch millisecond value (2020‑09‑13T12:26:40.000Z)
        long epochMillis = 1_600_000_000_000L;
        // the generator multiplies by 1,000,000
        long uniqueId = epochMillis * 1_000_000L;
        Date expected = new Date(epochMillis);
        // Act
        Date actual = UniqueIdGenerator.getDate19(uniqueId);
        // Assert
        assertEquals(expected, actual, "The Date returned by getDate19 should correspond to uniqueId / 1_000_000 milliseconds");
    }

    @Test
    @DisplayName("getDate19 returns correct Date for zero uniqueId")
    void testGetDate19Zero() {
        long uniqueId = 0L;
        // epoch start
        Date expected = new Date(0L);
        Date actual = UniqueIdGenerator.getDate19(uniqueId);
        assertEquals(expected, actual, "Zero uniqueId should map to the epoch start date");
    }

    @Test
    @DisplayName("getDate19 returns correct Date for a negative uniqueId")
    void testGetDate19Negative() {
        // Arrange: a negative epoch millisecond value (e.g., 1960‑01‑01T00:00:00.000Z)
        // 1960‑01‑01
        long epochMillis = -315_619_200_000L;
        long uniqueId = epochMillis * 1_000_000L;
        Date expected = new Date(epochMillis);
        Date actual = UniqueIdGenerator.getDate19(uniqueId);
        assertEquals(expected, actual, "Negative uniqueId should correctly produce a Date before the epoch");
    }

    /**
     * Parameterized test that iterates over every case defined in the JSON payload.
     * Each argument consists of:
     * <ul>
     *   <li>the {@code uniqueId} to feed into {@link UniqueIdGenerator#getDate19(long)}</li>
     *   <li>the expected millisecond value (i.e. {@code uniqueId / 1_000_000})</li>
     *   <li>a human‑readable description used as the test name</li>
     * </ul>
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("provideGetDate19Cases")
    void testGetDate19(long uniqueId, long expectedMillis, String description) {
        // Act
        Date actual = UniqueIdGenerator.getDate19(uniqueId);
        // Assert – compare the underlying epoch millisecond value
        assertEquals(expectedMillis, actual.getTime(), () -> "For uniqueId=" + uniqueId + " expected millis=" + expectedMillis + " but got " + actual.getTime());
    }
}
