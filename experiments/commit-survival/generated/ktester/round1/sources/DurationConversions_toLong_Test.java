package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toLong_Test {

    @Mock
    private Converter // Converter is not used by toLong, but we provide a mock
    mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Verify that the mock was not interacted with (to ensure the method truly ignores it)
        verifyNoInteractions(mockConverter);
    }

    private static Stream<Arguments> provideValidConversions() {
        return Stream.of(// Positive duration – 5 seconds → 5,000 ms
        Arguments.of(Duration.ofSeconds(5), 5_000L), // Zero duration → 0 ms
        Arguments.of(Duration.ZERO, 0L), // Negative duration – -3 seconds → -3,000 ms
        Arguments.of(Duration.ofSeconds(-3), -3_000L), // Maximum long value as milliseconds
        Arguments.of(Duration.ofMillis(Long.MAX_VALUE), Long.MAX_VALUE), // Duration that fits exactly after conversion (floor division)
        // Long.MAX_VALUE / 1000 seconds → 9_223_372_036_854_775 ms
        Arguments.of(Duration.ofSeconds(Long.MAX_VALUE / 1000), 9_223_372_036_854_775L));
    }

    private static Stream<Arguments> provideSubMillisecondCases() {
        return Stream.of(// 999 999 ns → 0 ms (just under one millisecond)
        Arguments.of(Duration.ofNanos(999_999), 0L), // 1 000 000 ns → 1 ms (exactly one millisecond)
        Arguments.of(Duration.ofNanos(1_000_000), 1L));
    }

    private static Stream<Arguments> provideOverflowDurations() {
        return Stream.of(// Positive overflow: a duration larger than Long.MAX_VALUE milliseconds
        Arguments.of(Duration.ofDays(106751991167300L)), // Negative overflow: a duration whose millisecond value is less than Long.MIN_VALUE
        Arguments.of(Duration.ofSeconds(Long.MIN_VALUE / 1000 - 1)));
    }

    @Test
    void toLong_returnsMilliseconds_forPositiveDuration() {
        // given
        // 5,250 ms
        Duration duration = Duration.ofSeconds(5).plusMillis(250);
        // when
        long result = DurationConversions.toLong(duration, mockConverter);
        // then
        assertEquals(5_250L, result, "Duration should be converted to its total milliseconds");
    }

    @Test
    void toLong_returnsZero_forZeroDuration() {
        // given
        Duration zero = Duration.ZERO;
        // when
        long result = DurationConversions.toLong(zero, mockConverter);
        // then
        assertEquals(0L, result, "Zero duration must yield zero milliseconds");
    }

    @Test
    void toLong_returnsNegativeMilliseconds_forNegativeDuration() {
        // given
        Duration negative = Duration.ofMillis(-1234);
        // when
        long result = DurationConversions.toLong(negative, mockConverter);
        // then
        assertEquals(-1234L, result, "Negative duration should preserve sign in milliseconds");
    }

    @Test
    void toLong_throwsNullPointerException_whenFromIsNull() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> DurationConversions.toLong(nullObj, mockConverter), "Calling toLong with a null source should throw NPE");
    }

    @Test
    void toLong_throwsClassCastException_whenFromIsNotDuration() {
        // given
        Object notADuration = "not a duration";
        // when / then
        assertThrows(ClassCastException.class, () -> DurationConversions.toLong(notADuration, mockConverter), "Calling toLong with a non‑Duration object should throw ClassCastException");
    }

    /**
     * Valid conversions that fit into a {@code long} without overflow.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideValidConversions")
    void toLong_returnsExpectedMilliseconds(Duration input, long expected) {
        long actual = DurationConversions.toLong(input, mockConverter);
        assertEquals(expected, actual, () -> "Expected " + expected + " ms for duration " + input);
    }

    /**
     * Sub‑millisecond values are truncated (floor) to whole milliseconds.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSubMillisecondCases")
    void toLong_truncatesSubMillisecondDurations(Duration input, long expected) {
        long actual = DurationConversions.toLong(input, mockConverter);
        assertEquals(expected, actual, () -> "Sub‑millisecond truncation failed for " + input);
    }

    /**
     * The {@code Converter} argument is ignored – the method must still
     * return the correct milliseconds even when a non‑null converter is supplied.
     */
    @Test
    void toLong_ignoresNonNullConverter() {
        // 2 000 ms
        Duration duration = Duration.ofSeconds(2);
        // Use the mockConverter (non‑null) to prove it is ignored
        long result = DurationConversions.toLong(duration, mockConverter);
        assertEquals(2_000L, result, "Converter argument should be ignored and not affect the result");
    }

    /**
     * Durations whose millisecond value exceeds {@link Long#MAX_VALUE}
     * or is less than {@link Long#MIN_VALUE} must cause an {@link ArithmeticException}.
     */
    @ParameterizedTest(name = "[{index}] overflow case")
    @MethodSource("provideOverflowDurations")
    void toLong_throwsArithmeticException_onOverflow(Duration overflowDuration) {
        assertThrows(ArithmeticException.class, () -> DurationConversions.toLong(overflowDuration, mockConverter), () -> "Expected ArithmeticException for overflow duration: " + overflowDuration);
    }
}
