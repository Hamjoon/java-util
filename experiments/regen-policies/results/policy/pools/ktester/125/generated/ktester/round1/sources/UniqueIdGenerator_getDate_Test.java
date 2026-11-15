package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.Date;
import static com.cedarsoftware.util.UniqueIdGenerator.getDate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class UniqueIdGenerator_getDate_Test {

    /**
     * Supplies the test data for {@link #getDate_basicAndEdgeCases(long, long, String)}.
     */
    private static Stream<Arguments> basicAndEdgeCases() {
        return Stream.of(// Zero
        Arguments.of(0L, 0L, "Zero input should produce the epoch date because 0 / 100000 = 0."), // Just below divisor
        Arguments.of(99_999L, 0L, "Value just below the divisor threshold; integer division truncates to 0."), // Exact divisor
        Arguments.of(100_000L, 1L, "Exact divisor value; result should be 1 millisecond after epoch."), // Typical positive
        Arguments.of(1_234_567_890L, 12_345L, "Typical positive value; verifies normal division and Date construction."), // Negative magnitude < divisor
        Arguments.of(-1L, 0L, "Negative value whose absolute magnitude is less than the divisor; division truncates toward zero, yielding 0."), // Negative equal to divisor
        Arguments.of(-100_000L, -1L, "Negative value equal to divisor; result should be -1 millisecond (one millisecond before epoch)."), // Typical negative
        Arguments.of(-1_234_567_890L, -12_345L, "Typical negative value; ensures correct handling of sign and truncation."), // Maximum long
        Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE / 100_000L, "Maximum long value; tests that division does not overflow and Date can be created with the resulting epoch."), // Minimum long
        Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE / 100_000L, "Minimum long value; verifies correct handling of extreme negative numbers."));
    }

    /**
     * Supplies the test data for {@link #getDate_additionalBoundaryCases(long, long, String)}.
     */
    private static Stream<Arguments> additionalBoundaryCases() {
        return Stream.of(Arguments.of(1L, 0L, "Small positive value not reaching the divisor; integer division truncates to 0."), Arguments.of(100_001L, 1L, "Value just above the divisor; remainder is ignored, result is 1 millisecond."), Arguments.of(199_999L, 1L, "Value just below two times the divisor; integer division yields 1."), Arguments.of(200_000L, 2L, "Exact multiple of the divisor; result is 2 milliseconds."), Arguments.of(-100_001L, -1L, "Negative value just beyond the divisor; truncates toward zero giving -1."), Arguments.of(-199_999L, -1L, "Negative value just below two times the divisor; integer division yields -1."), Arguments.of(-200_000L, -2L, "Exact negative multiple of the divisor; result is -2 milliseconds."), Arguments.of(1_000_000_000_000L, 10_000_000L, "Large positive value; verifies correct scaling to several seconds/minutes."), Arguments.of(-1_000_000_000_000L, -10_000_000L, "Large negative value; checks correct handling of negative large timestamps."), // Re‑checking max long for completeness
        Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE / 100_000L, "Maximum long value already covered in basic cases; retained for completeness in this group."), // Max long minus one divisor
        Arguments.of(9_223_372_036_854_675_807L, 92_233_720_368_546L, "Maximum long minus one divisor; ensures quotient decreases by one."), // Min long already covered
        Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE / 100_000L, "Minimum long value already covered; kept for reference."), // Min long plus one divisor
        Arguments.of(-9_223_372_036_854_675_808L, -92_233_720_368_546L, "Minimum long plus one divisor; verifies quotient increments toward zero."));
    }

    private static Stream<Arguments> consistentIdProvider() {
        return Stream.of(Arguments.of(System.currentTimeMillis() * 100_000L + 42L), Arguments.of(0L), Arguments.of(-123_456_789L));
    }

    @Test
    void getDate_convertsUniqueIdToDateCorrectly() {
        // Example value – the division should truncate toward zero.
        // arbitrary positive number
        long uniqueId = 1_234_567_890L;
        // 12_345 ms
        long expectedMillis = uniqueId / 100_000L;
        Date result = getDate(uniqueId);
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(expectedMillis, result.getTime(), "Date.getTime() should equal uniqueId / 100_000");
    }

    @Test
    void getDate_handlesZeroCorrectly() {
        long uniqueId = 0L;
        Date result = getDate(uniqueId);
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "Zero uniqueId must produce epoch Date (time = 0)");
    }

    @Test
    void getDate_handlesNegativeUniqueId() {
        long uniqueId = -987_654_321L;
        // integer division rounds toward zero
        long expectedMillis = uniqueId / 100_000L;
        Date result = getDate(uniqueId);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), "Negative uniqueId should be divided correctly preserving sign");
    }

    @Test
    void getDate_isConsistentAcrossMultipleCalls() {
        // construct a realistic ID
        long uniqueId = System.currentTimeMillis() * 100_000L + 42L;
        Date first = getDate(uniqueId);
        Date second = getDate(uniqueId);
        assertEquals(first, second, "Multiple invocations with the same ID must return equal Dates");
    }

    /**
     * Verifies that {@link UniqueIdGenerator#getDate(long)} correctly converts a
     * variety of {@code uniqueId} values (including zero, small positives,
     * small negatives and the extreme {@code long} values) into the expected
     * {@link Date}.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("basicAndEdgeCases")
    void getDate_basicAndEdgeCases(long uniqueId, long expectedMillis, String description) {
        Date result = getDate(uniqueId);
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(expectedMillis, result.getTime(), () -> "Failed for case: " + description + " (uniqueId=" + uniqueId + ")");
    }

    /**
     * Additional boundary and typical cases (values around the divisor,
     * large magnitudes, and values that differ by a single divisor unit).
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("additionalBoundaryCases")
    void getDate_additionalBoundaryCases(long uniqueId, long expectedMillis, String description) {
        Date result = getDate(uniqueId);
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(expectedMillis, result.getTime(), () -> "Failed for case: " + description + " (uniqueId=" + uniqueId + ")");
    }
}
