package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toInteger_Test {

    /**
     * The Converter is not used by {@code LocalTimeConversions.toInteger},
     * but the method signature requires it, so we provide a mock.
     */
    @Mock
    private Converter mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Verify that the mock was not interacted with (the method should ignore it).
        verifyNoInteractions(mockConverter);
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> validConversionsProvider() {
        return Stream.of(// Midnight → 0 ms
        arguments(LocalTime.of(0, 0, 0, 0), 0, "Midnight should convert to 0 milliseconds."), // Typical time 12:34:56.789 (nanoseconds = 789_000_000)
        arguments(LocalTime.of(12, 34, 56, 789_000_000), 45_296_789, "Typical time 12:34:56.789 should convert to 45,296,789 ms."), // Last nanosecond of the day → 86,399,999 ms (truncation)
        arguments(LocalTime.of(23, 59, 59, 999_999_999), 86_399_999, "Last nanosecond of the day should convert to 86,399,999 ms."), // One micro‑second after midnight → 0 ms (still < 1 ms)
        arguments(LocalTime.of(0, 0, 0, 1_000_000), 1, "Exactly one millisecond after midnight (1,000,000 ns) → 1 ms."), // One micro‑second after midnight (1_000 ns) → 0 ms
        arguments(LocalTime.of(0, 0, 0, 1_000), 0, "One micro‑second after midnight (1,000 ns) yields 0 ms."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> boundaryConditionsProvider() {
        return Stream.of(// Just under one millisecond (999,999 ns) → 0 ms
        arguments(LocalTime.of(0, 0, 0, 999_999), 0, "Just under one millisecond after midnight should be 0 ms."), // Exactly one millisecond (1,000,000 ns) → 1 ms
        arguments(LocalTime.of(0, 0, 0, 1_000_000), 1, "Exactly one millisecond after midnight should be 1 ms."), // Two nanoseconds before the final nanosecond of the day
        arguments(LocalTime.of(23, 59, 59, 999_999_998), 86_399_999, "Two ns before the last nanosecond yields 86,399,999 ms."), // One full second after midnight → 1,000 ms
        arguments(LocalTime.of(0, 0, 1, 0), 1_000, "One full second after midnight should be 1,000 ms."));
    }

    @Test
    void toInteger_midnight_returnsZero() {
        // GIVEN
        // 00:00:00.000000000
        LocalTime midnight = LocalTime.MIDNIGHT;
        // WHEN
        int result = LocalTimeConversions.toInteger(midnight, mockConverter);
        // THEN
        assertEquals(0, result, "Midnight should convert to 0 milliseconds");
    }

    @Test
    void toInteger_specificTime_returnsExpectedMilliseconds() {
        // GIVEN
        // 12:34:56.789123456
        LocalTime time = LocalTime.of(12, 34, 56, 789_123_456);
        // manual calculation
        int expected = (int) (time.toNanoOfDay() / 1_000_000);
        // WHEN
        int result = LocalTimeConversions.toInteger(time, mockConverter);
        // THEN
        assertEquals(expected, result, "Conversion should match nanoseconds‑to‑milliseconds truncation");
    }

    @Test
    void toInteger_maxTime_returnsMaximumMillisecondsOfDay() {
        // GIVEN
        // 23:59:59.999999999 – the last nanosecond of the day
        LocalTime max = LocalTime.of(23, 59, 59, 999_999_999);
        // should be 86_399_999
        int expected = (int) (max.toNanoOfDay() / 1_000_000);
        // WHEN
        int result = LocalTimeConversions.toInteger(max, mockConverter);
        // THEN
        assertEquals(expected, result, "Maximum time should convert to the highest millisecond value of the day");
        assertEquals(86_399_999, result, "Explicit check: 23:59:59.999999999 → 86,399,999 ms");
    }

    @Test
    void toInteger_nonLocalTime_throwsClassCastException() {
        // GIVEN
        // not a LocalTime
        Object from = "12:34:56";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toInteger(from, mockConverter), "Non‑LocalTime argument should cause ClassCastException.");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionsProvider")
    void toInteger_validConversions(LocalTime from, int expected, String description) {
        // WHEN
        int result = LocalTimeConversions.toInteger(from, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("boundaryConditionsProvider")
    void toInteger_boundaryConditions(LocalTime from, int expected, String description) {
        // WHEN
        int result = LocalTimeConversions.toInteger(from, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    @Test
    void toInteger_nullFrom_throwsNullPointerException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toInteger(from, mockConverter), "Null `from` argument should cause NPE.");
    }
}
