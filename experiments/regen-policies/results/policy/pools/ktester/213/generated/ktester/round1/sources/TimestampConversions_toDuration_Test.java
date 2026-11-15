package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.DateTimeException;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toDuration_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter is required by the signature but not used by the
        // implementation of toDuration.  A default instance is sufficient.
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper that creates a {@link Timestamp} from a ISO‑date string and a
     * time‑of‑day expressed in nanoseconds.
     */
    private static Timestamp ts(String isoDate, int hour, int minute, int second, int nanos) {
        return Timestamp.from(LocalDate.parse(isoDate).atTime(hour, minute, second, nanos).atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Provides a stream of normal test cases extracted from the JSON specification.
     * Each case supplies a {@link Timestamp}, the expected {@link Duration},
     * and a short description used for readability.
     */
    private static Stream<Arguments> normalDurationCases() {
        return Stream.of(// epoch (0 ms)
        arguments(new Timestamp(0L), Duration.ZERO, "Timestamp at epoch (0 ms) should produce a zero duration."), // 1 second after epoch (1 000 ms)
        arguments(new Timestamp(1_000L), Duration.ofMillis(1_000), "Timestamp 1 second after epoch should produce a duration of 1 second."), // negative timestamp (‑5 000 ms)
        arguments(new Timestamp(-5_000L), Duration.ofMillis(-5_000), "Timestamp before epoch (negative milliseconds) should produce a negative duration."), // maximum positive long value
        arguments(new Timestamp(Long.MAX_VALUE), Duration.ofMillis(Long.MAX_VALUE), "Maximum positive timestamp value should produce a Duration representing the same number of milliseconds."), // minimum (most negative) long value
        arguments(new Timestamp(Long.MIN_VALUE), Duration.ofMillis(Long.MIN_VALUE), "Minimum (most negative) timestamp value should produce a Duration with the corresponding negative milliseconds."), // timestamp with additional nanoseconds (500 ms)
        arguments(ts(1_234L, 500_000_000), Duration.ofMillis(1_734), "Timestamp with non‑zero nanoseconds (500 ms) should be added to the millisecond part, yielding a total of 1 734 ms."), // negative timestamp with positive nanoseconds (250 ms)
        arguments(ts(-2_000L, 250_000_000), Duration.ofMillis(-1_750), "Negative timestamp with positive nanoseconds should correctly offset the negative base, resulting in -1 750 ms."), // non‑null converter (should have no effect)
        arguments(new Timestamp(5_000L), Duration.ofMillis(5_000), "Providing a non‑null Converter does not affect the conversion; the result should still be a Duration of 5 seconds."));
    }

    /**
     * Provides cases where {@link Timestamp#toInstant()} is expected to throw
     * {@link DateTimeException} because the underlying epoch millisecond value
     * lies outside the range supported by {@link Instant}.
     */
    private static Stream<Arguments> instantExceptionCases() {
        return Stream.of(arguments(// Instant.MAX plus 1 ms (beyond the maximum supported instant)
        new Timestamp(Instant.MAX.plusMillis(1).toEpochMilli()), DateTimeException.class, "Timestamp value just beyond Instant.MAX should cause Timestamp.toInstant() to throw DateTimeException."), arguments(// Instant.MIN minus 1 ms (before the minimum supported instant)
        new Timestamp(Instant.MIN.minusMillis(1).toEpochMilli()), DateTimeException.class, "Timestamp value just before Instant.MIN should cause Timestamp.toInstant() to throw DateTimeException."));
    }

    @Test
    void testToDurationMatchesInstantSinceEpoch() {
        // epoch
        Timestamp t1 = ts("1970-01-01", 0, 0, 0, 0);
        // +1 second
        Timestamp t2 = ts("1970-01-01", 0, 0, 1, 0);
        // arbitrary
        Timestamp t3 = ts("2000-02-29", 12, 34, 56, 123_456_789);
        assertEquals(Duration.between(Instant.EPOCH, t1.toInstant()), TimestampConversions.toDuration(t1, converter));
        assertEquals(Duration.between(Instant.EPOCH, t2.toInstant()), TimestampConversions.toDuration(t2, converter));
        assertEquals(Duration.between(Instant.EPOCH, t3.toInstant()), TimestampConversions.toDuration(t3, converter));
    }

    @Test
    void testToDurationWithNegativeEpochTimes() {
        // Dates before 1970‑01‑01 produce negative durations.
        Timestamp beforeEpoch = ts("1965-07-20", 15, 30, 45, 500_000_000);
        Duration expected = Duration.between(Instant.EPOCH, beforeEpoch.toInstant());
        assertEquals(expected, TimestampConversions.toDuration(beforeEpoch, converter));
    }

    @Test
    void testToDurationRejectsNullSource() {
        assertThrows(NullPointerException.class, () -> TimestampConversions.toDuration(null, converter));
    }

    @Test
    void testToDurationRejectsWrongType() {
        // The implementation casts directly to Timestamp, so any other type
        // should cause a ClassCastException.
        Object notATimestamp = "2023-01-01T00:00:00Z";
        assertThrows(ClassCastException.class, () -> TimestampConversions.toDuration(notATimestamp, converter));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("normalDurationCases")
    void testToDuration_NormalCases(Timestamp timestamp, Duration expected, String description) {
        // Act
        Duration actual = TimestampConversions.toDuration(timestamp, converter);
        // Assert
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("instantExceptionCases")
    void testToDuration_InstantExceptions(Timestamp timestamp, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> TimestampConversions.toDuration(timestamp, converter), description);
    }

    @Test
    void testToDuration_InvalidNanosThrowsIllegalArgumentException() {
        // Setting nanoseconds outside the 0‑999_999_999 range should fail immediately.
        assertThrows(IllegalArgumentException.class, () -> {
            Timestamp ts = new Timestamp(0L);
            // invalid: exactly 1 second in nanos
            ts.setNanos(1_000_000_000);
        });
    }
}
