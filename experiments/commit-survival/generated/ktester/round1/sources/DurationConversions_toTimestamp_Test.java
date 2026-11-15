package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toTimestamp_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    converter;

    /* -------------------------------------------------------------
     *  Helper holder class for parameterized arguments
     * ------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final Duration duration;

        final Timestamp expected;

        ArgumentsHolder(Duration duration, Timestamp expected) {
            this.duration = duration;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return "duration=" + duration + ", expected=" + expected;
        }
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for the "Valid conversions" group
     * ------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> validConversionProvider() {
        return Stream.of(// 1 hour
        new ArgumentsHolder(Duration.parse("PT1H"), Timestamp.valueOf("1970-01-01 01:00:00.0")), // zero
        new ArgumentsHolder(Duration.ZERO, Timestamp.valueOf("1970-01-01 00:00:00.0")), // -30 minutes
        new ArgumentsHolder(Duration.ofMinutes(-30), Timestamp.valueOf("1969-12-31 23:30:00.0")), // 50 years (approx 365*50 days)
        new ArgumentsHolder(Duration.ofDays(365L * 50), Timestamp.from(Instant.EPOCH.plus(Duration.ofDays(365L * 50)))), // 2h30m with a non‑null converter instance (converter is ignored)
        new ArgumentsHolder(Duration.parse("PT2H30M"), Timestamp.valueOf("1970-01-01 02:30:00.0")));
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for the "Boundary and edge cases" group
     * ------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> edgeCaseProvider() {
        return Stream.of(// zero nanos (still epoch)
        new ArgumentsHolder(Duration.ofNanos(0), Timestamp.valueOf("1970-01-01 00:00:00.0")), // 1 nanosecond – Timestamp truncates to milliseconds, still epoch
        new ArgumentsHolder(Duration.ofNanos(1), Timestamp.from(Instant.EPOCH.plus(Duration.ofNanos(1)))), // -1 nanosecond – same truncation behaviour
        new ArgumentsHolder(Duration.ofNanos(-1), Timestamp.from(Instant.EPOCH.plus(Duration.ofNanos(-1)))), // Very large positive duration (seconds = Long.MAX_VALUE / 1000)
        new ArgumentsHolder(Duration.ofSeconds(Long.MAX_VALUE / 1000), Timestamp.from(Instant.EPOCH.plus(Duration.ofSeconds(Long.MAX_VALUE / 1000)))), // Very large negative duration (seconds = Long.MIN_VALUE / 1000)
        new ArgumentsHolder(Duration.ofSeconds(Long.MIN_VALUE / 1000), Timestamp.from(Instant.EPOCH.plus(Duration.ofSeconds(Long.MIN_VALUE / 1000)))), // 999,999,999 ns (just under 1 ms) – still epoch
        new ArgumentsHolder(Duration.ofNanos(999_999_999), Timestamp.valueOf("1970-01-01 00:00:00.0")), // Exactly 1 ms
        new ArgumentsHolder(Duration.ofNanos(1_000_000), Timestamp.valueOf("1970-01-01 00:00:00.001")));
    }

    @Test
    void toTimestamp_zeroDuration_returnsEpochTimestamp() {
        // given
        Duration duration = Duration.ZERO;
        Timestamp expected = Timestamp.from(Instant.EPOCH);
        // when
        Timestamp actual = DurationConversions.toTimestamp(duration, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void toTimestamp_positiveDuration_returnsCorrectTimestamp() {
        // given
        // 5.250 seconds
        Duration duration = Duration.ofSeconds(5).plusMillis(250);
        Timestamp expected = Timestamp.from(Instant.EPOCH.plus(duration));
        // when
        Timestamp actual = DurationConversions.toTimestamp(duration, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void toTimestamp_negativeDuration_returnsCorrectTimestamp() {
        // given
        // -3.5 seconds
        Duration duration = Duration.ofSeconds(-3).minusNanos(500_000_000);
        Timestamp expected = Timestamp.from(Instant.EPOCH.plus(duration));
        // when
        Timestamp actual = DurationConversions.toTimestamp(duration, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void toTimestamp_nonDurationObject_throwsClassCastException() {
        // given
        Object notADuration = "I am not a Duration";
        // when / then
        assertThrows(ClassCastException.class, () -> DurationConversions.toTimestamp(notADuration, converter));
    }

    @Test
    void toTimestamp_nullFrom_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class, () -> DurationConversions.toTimestamp(null, converter));
    }

    @ParameterizedTest(name = "{index}: toTimestamp({0}) => {1}")
    @MethodSource("validConversionProvider")
    void toTimestamp_validConversions(ArgumentsHolder holder) {
        // when
        Timestamp actual = DurationConversions.toTimestamp(holder.duration, converter);
        // then
        assertEquals(holder.expected, actual);
    }

    @ParameterizedTest(name = "{index}: edge case {0}")
    @MethodSource("edgeCaseProvider")
    void toTimestamp_edgeCases(ArgumentsHolder holder) {
        Timestamp actual = DurationConversions.toTimestamp(holder.duration, converter);
        assertEquals(holder.expected, actual);
    }

    /* -------------------------------------------------------------
     *  Tests for overflow / out‑of‑range scenarios
     * ------------------------------------------------------------- */
    @Test
    void toTimestamp_extremelyLargePositiveDuration_throwsDateTimeException() {
        // far beyond Instant.MAX
        Duration huge = Duration.ofSeconds(Long.MAX_VALUE);
        assertThrows(DateTimeException.class, () -> DurationConversions.toTimestamp(huge, converter));
    }

    @Test
    void toTimestamp_extremelyLargeNegativeDuration_throwsDateTimeException() {
        Duration hugeNeg = Duration.ofSeconds(Long.MIN_VALUE);
        assertThrows(DateTimeException.class, () -> DurationConversions.toTimestamp(hugeNeg, converter));
    }

    @Test
    void toTimestamp_durationBeyondInstantMax_throwsDateTimeException() {
        // Construct a duration that pushes the instant past Instant.MAX
        long beyond = Instant.MAX.getEpochSecond() + 1;
        Duration beyondMax = Duration.ofSeconds(beyond);
        assertThrows(DateTimeException.class, () -> DurationConversions.toTimestamp(beyondMax, converter));
    }

    @Test
    void toTimestamp_durationBeyondInstantMin_throwsDateTimeException() {
        long before = Instant.MIN.getEpochSecond() - 1;
        Duration beyondMin = Duration.ofSeconds(before);
        assertThrows(DateTimeException.class, () -> DurationConversions.toTimestamp(beyondMin, converter));
    }
}
