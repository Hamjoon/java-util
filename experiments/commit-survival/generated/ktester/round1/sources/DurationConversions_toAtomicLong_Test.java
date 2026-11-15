package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toAtomicLong_Test {

    /**
     * Mocked Converter – the method under test does not use it, but it must be supplied.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh mock before each test.
        converter = mock(Converter.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid any accidental interference between tests.
        clearInvocations(converter);
    }

    /**
     * Provides a stream of successful conversion cases.
     * Each argument consists of:
     *   1. The {@link Duration} to convert.
     *   2. The expected long value that the resulting {@link AtomicLong} should contain.
     */
    private static Stream<Arguments> successfulConversionCases() {
        return Stream.of(// zero duration
        Arguments.of(Duration.ofMillis(0), 0L), // positive whole‑second duration
        Arguments.of(Duration.ofSeconds(5), 5_000L), // mixed minutes + seconds
        Arguments.of(Duration.ofMinutes(2).plusSeconds(30), 150_000L), // sub‑millisecond expressed as nanos (1.5 ms -> truncates to 1)
        Arguments.of(Duration.ofNanos(1_500_000), 1L), // large positive duration (23 hours)
        Arguments.of(Duration.ofDays(1).minusHours(1), 82_800_000L), // maximum long milliseconds (no overflow)
        Arguments.of(Duration.ofMillis(Long.MAX_VALUE), Long.MAX_VALUE), // minimum long milliseconds (no overflow)
        Arguments.of(Duration.ofMillis(Long.MIN_VALUE), Long.MIN_VALUE), // negative sub‑millisecond (‑0.5 ms -> truncates toward zero → -1)
        Arguments.of(Duration.ofNanos(-500_000), -1L), // negative whole‑second duration
        Arguments.of(Duration.ofSeconds(-3), -3_000L), // explicitly zero using all components
        Arguments.of(Duration.ofDays(0).plusHours(0).plusMinutes(0).plusSeconds(0).plusNanos(0), 0L), // full day (24 hours)
        Arguments.of(Duration.ofHours(24), 86_400_000L), // a year (365 days)
        Arguments.of(Duration.ofDays(365), 31_536_000_000L), // just under one millisecond (0.999999 ms) → truncates to 0
        Arguments.of(Duration.ofNanos(999_999), 0L), // exactly one millisecond expressed in nanos
        Arguments.of(Duration.ofNanos(1_000_000), 1L), // half‑second (500,000,000 ns) → not a whole ms, truncates to 0
        Arguments.of(Duration.ofSeconds(0).plusNanos(500_000_000), 0L));
    }

    /**
     * Provides a stream of cases that are expected to throw an exception.
     * Each argument consists of:
     *   1. The input object (may be {@code null}, a {@link String}, or a {@link Duration} that overflows).
     *   2. The {@link Class} of the expected exception.
     */
    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// null source → NullPointerException
        Arguments.of(null, NullPointerException.class), // wrong type → ClassCastException
        Arguments.of("not a duration", ClassCastException.class), // overflow positive (millis > Long.MAX_VALUE)
        Arguments.of(Duration.ofSeconds(Long.MAX_VALUE / 1000 + 1), ArithmeticException.class), // overflow negative (millis < Long.MIN_VALUE)
        Arguments.of(Duration.ofSeconds(Long.MIN_VALUE / 1000 - 1), ArithmeticException.class));
    }

    @Test
    void testToAtomicLongReturnsCorrectValue() {
        // GIVEN a Duration instance
        // 5.123456789 seconds
        Duration duration = Duration.ofSeconds(5, 123_456_789L);
        // Duration truncates nanos when converting to millis
        long expectedMillis = duration.toMillis();
        // WHEN the conversion method is invoked
        AtomicLong result = DurationConversions.toAtomicLong(duration, converter);
        // THEN the resulting AtomicLong holds the expected millisecond value
        assertNotNull(result, "Result should not be null");
        assertEquals(expectedMillis, result.get(), "AtomicLong should contain the duration expressed in milliseconds");
    }

    @Test
    void testToAtomicLongNullInputThrowsNullPointerException() {
        // GIVEN a null source object
        // WHEN the conversion method is invoked
        // THEN a NullPointerException is thrown because the method casts and dereferences the value
        assertThrows(NullPointerException.class, () -> DurationConversions.toAtomicLong(null, converter), "Converting a null Duration should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] Duration {0} → AtomicLong {1}")
    @MethodSource("successfulConversionCases")
    void testToAtomicLongSuccessfulConversions(Duration from, long expectedMillis) {
        // WHEN
        AtomicLong result = DurationConversions.toAtomicLong(from, converter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(expectedMillis, result.get(), () -> "Expected " + expectedMillis + " ms for duration " + from);
    }

    @ParameterizedTest(name = "[{index}] Input {0} throws {1}")
    @MethodSource("exceptionCases")
    void testToAtomicLongExceptionScenarios(Object from, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> DurationConversions.toAtomicLong(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
