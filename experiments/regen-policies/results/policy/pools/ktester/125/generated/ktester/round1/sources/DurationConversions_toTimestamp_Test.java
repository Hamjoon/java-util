package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toTimestamp_Test {

    @Mock
    private Converter // Converter is not used by the method but required for signature
    mockConverter;

    /* --------------------------------------------------------------------- */
    private static Duration parseIsoDuration(String iso) {
        if (iso == null) {
            return null;
        }
        // Simple day based format, e.g. "P100D"
        if (iso.matches("P\\d+D")) {
            long days = Long.parseLong(iso.substring(1, iso.length() - 1));
            return Duration.ofDays(days);
        }
        // Negative duration expressed as "PT-2S"
        if (iso.startsWith("PT-")) {
            // Transform to the standard "-PT2S"
            return Duration.parse("-PT" + iso.substring(3));
        }
        // Standard ISO‑8601 duration (including leading minus)
        return Duration.parse(iso);
    }

    private static Stream<Arguments> validDurationProvider() {
        return Stream.of(// Positive whole‑second duration
        arguments("Positive duration of 5 seconds", "PT5S", "1970-01-01T00:00:05Z"), // Zero duration
        arguments("Zero duration", "PT0S", "1970-01-01T00:00:00Z"), // Negative duration (2 seconds before epoch)
        arguments("Negative duration of 2 seconds", "PT-2S", "1969-12-31T23:59:58Z"), // Large duration of 100 days
        arguments("Large duration of 100 days", "P100D", "1970-04-11T00:00:00Z"), // Fractional seconds – milliseconds precision
        arguments("Duration with milliseconds (1.234 seconds)", "PT1.234S", "1970-01-01T00:00:01.234Z"), // Fractional seconds – half‑second
        arguments("Duration with half‑second (1.5 seconds)", "PT1.5S", "1970-01-01T00:00:01.5Z"), // Sub‑microsecond precision (1 microsecond)
        arguments("Duration of one microsecond", "PT0.000001S", "1970-01-01T00:00:00.000001Z"));
    }

    private static Stream<Arguments> overflowDurationProvider() {
        return Stream.of(arguments("Positive overflow – exceeds Instant.MAX", "PT9223372036854775807S", DateTimeException.class), arguments("Negative overflow – below Instant.MIN", "-PT9223372036854775807S", DateTimeException.class));
    }

    @Test
    void toTimestamp_zeroDuration_returnsEpoch() {
        Duration duration = Duration.ZERO;
        Timestamp ts = DurationConversions.toTimestamp(duration, mockConverter);
        assertEquals(Instant.EPOCH, ts.toInstant(), "Timestamp for zero duration should be the epoch instant");
    }

    @Test
    void toTimestamp_positiveDuration_returnsCorrectInstant() {
        Duration duration = Duration.ofSeconds(123).plusNanos(456_789);
        Timestamp ts = DurationConversions.toTimestamp(duration, mockConverter);
        Instant expected = Instant.EPOCH.plus(duration);
        assertEquals(expected, ts.toInstant(), "Timestamp should represent epoch plus the given positive duration");
    }

    @Test
    void toTimestamp_negativeDuration_returnsCorrectInstant() {
        Duration duration = Duration.ofSeconds(-10).minusNanos(250_000_000);
        Timestamp ts = DurationConversions.toTimestamp(duration, mockConverter);
        Instant expected = Instant.EPOCH.plus(duration);
        assertEquals(expected, ts.toInstant(), "Timestamp should represent epoch plus the given negative duration");
    }

    @Test
    void toTimestamp_nonDuration_throwsClassCastException() {
        Object notADuration = "not a duration";
        assertThrows(ClassCastException.class, () -> DurationConversions.toTimestamp(notADuration, mockConverter), "Passing a non‑Duration object should result in ClassCastException");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0} → {2}")
    @MethodSource("validDurationProvider")
    void toTimestamp_validDurations(String description, String isoDuration, String expectedIso) {
        Duration duration = parseIsoDuration(isoDuration);
        Timestamp ts = DurationConversions.toTimestamp(duration, mockConverter);
        Instant expected = Instant.parse(expectedIso);
        assertEquals(expected, ts.toInstant(), () -> "Failed for case: " + description);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toTimestamp_nullFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DurationConversions.toTimestamp(null, mockConverter), "Passing null as the 'from' argument must result in NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] overflow {0}")
    @MethodSource("overflowDurationProvider")
    void toTimestamp_overflowDurations(String description, String isoDuration, Class<? extends Throwable> expectedException) {
        Duration duration = parseIsoDuration(isoDuration);
        assertThrows(expectedException, () -> DurationConversions.toTimestamp(duration, mockConverter), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
