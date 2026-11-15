package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class UniqueIdGenerator_getDate_Test {

    private static Stream<Arguments> provideBasicFunctionality() {
        return Stream.of(// zero → epoch
        arguments(0L, new Date(0L)), // typical positive value
        arguments(123_456_789L, new Date(123_456_789L / 100_000L)), // negative value
        arguments(-987_654_321L, new Date(-987_654_321L / 100_000L)), // Long.MAX_VALUE
        arguments(Long.MAX_VALUE, new Date(Long.MAX_VALUE / 100_000L)), // Long.MIN_VALUE
        arguments(Long.MIN_VALUE, new Date(Long.MIN_VALUE / 100_000L)));
    }

    private static Stream<Arguments> provideIntegerDivisionTruncation() {
        return Stream.of(// values < 100_000 → 0 ms
        arguments(1L, new Date(0L)), arguments(99_999L, new Date(0L)), // exact multiples
        arguments(100_000L, new Date(1L)), arguments(200_000L, new Date(2L)), // just above a multiple
        arguments(100_001L, new Date(1L)));
    }

    private static Stream<Arguments> provideNearLimitValues() {
        return Stream.of(// Long.MAX_VALUE - 1 (remainder < 100_000, same division result)
        arguments(Long.MAX_VALUE - 1L, new Date((Long.MAX_VALUE - 1L) / 100_000L)), // Long.MIN_VALUE + 1 (remainder > -100_000, same division result)
        arguments(Long.MIN_VALUE + 1L, new Date((Long.MIN_VALUE + 1L) / 100_000L)), // Half of Long.MAX_VALUE
        arguments(Long.MAX_VALUE / 2L, new Date((Long.MAX_VALUE / 2L) / 100_000L)), // Small negative value whose magnitude < 100_000 → division yields 0
        arguments(-1L, new Date(0L)));
    }

    @Test
    void getDate_returnsCorrectDateForTypicalValue() {
        // arbitrary positive value
        long uniqueId = 1_234_567_890L;
        // integer division as in the implementation
        long expectedMillis = uniqueId / 100_000L;
        Date expected = new Date(expectedMillis);
        Date actual = UniqueIdGenerator.getDate(uniqueId);
        assertEquals(expected, actual, "The Date returned should correspond to uniqueId / 100_000 milliseconds since epoch");
    }

    @Test
    void getDate_returnsCorrectDateForZero() {
        long uniqueId = 0L;
        Date expected = new Date(0L);
        Date actual = UniqueIdGenerator.getDate(uniqueId);
        assertEquals(expected, actual, "Zero uniqueId should map to epoch start (Date(0))");
    }

    @Test
    void getDate_handlesNegativeUniqueId() {
        long uniqueId = -123_456_789L;
        // negative division truncates toward zero
        long expectedMillis = uniqueId / 100_000L;
        Date expected = new Date(expectedMillis);
        Date actual = UniqueIdGenerator.getDate(uniqueId);
        assertEquals(expected, actual, "Negative uniqueId should produce a Date representing the negative epoch offset");
    }

    @Test
    void getDate_isConsistentAcrossMultipleCalls() {
        long uniqueId = 9_876_543_210L;
        Date first = UniqueIdGenerator.getDate(uniqueId);
        Date second = UniqueIdGenerator.getDate(uniqueId);
        assertEquals(first, second, "Multiple invocations with the same uniqueId must return equal Dates");
    }

    @Test
    void getDate_doesNotThrowForVeryLargeValues() {
        // extreme upper bound
        long uniqueId = Long.MAX_VALUE;
        assertDoesNotThrow(() -> {
            Date d = UniqueIdGenerator.getDate(uniqueId);
            // The resulting date will be far in the future; we just ensure no exception occurs
            assertNotNull(d);
        });
    }

    /* -------------------------------------------------------------
     *  Basic functionality tests
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "basic [{index}] uniqueId={0} → expected={1}")
    @MethodSource("provideBasicFunctionality")
    void getDate_basicFunctionality(long uniqueId, Date expected) {
        Date actual = UniqueIdGenerator.getDate(uniqueId);
        assertEquals(expected, actual, () -> "Date for uniqueId " + uniqueId + " should be " + expected);
    }

    /* -------------------------------------------------------------
     *  Integer‑division truncation tests
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "truncation [{index}] uniqueId={0} → expected={1}")
    @MethodSource("provideIntegerDivisionTruncation")
    void getDate_integerDivisionTruncation(long uniqueId, Date expected) {
        Date actual = UniqueIdGenerator.getDate(uniqueId);
        assertEquals(expected, actual, () -> "Division truncation failed for uniqueId " + uniqueId);
    }

    /* -------------------------------------------------------------
     *  Near‑limit values tests
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "near‑limit [{index}] uniqueId={0} → expected={1}")
    @MethodSource("provideNearLimitValues")
    void getDate_nearLimitValues(long uniqueId, Date expected) {
        Date actual = UniqueIdGenerator.getDate(uniqueId);
        assertEquals(expected, actual, () -> "Near‑limit handling failed for uniqueId " + uniqueId);
    }
}
