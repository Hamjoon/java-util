package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toTimestamp_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter is not used by the method under test, but we provide a real instance
        // to keep the contract realistic.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    /**
     * Normal conversion cases (positive, zero, negative, large but still in range).
     */
    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// positive 5 seconds, null converter
        Arguments.of(Duration.ofSeconds(5), null, "Convert a positive Duration of 5 seconds to a Timestamp; should be epoch plus 5 seconds."), // zero duration
        Arguments.of(Duration.ZERO, null, "Convert a zero Duration; result must be the epoch Timestamp."), // negative 3 seconds
        Arguments.of(Duration.ofSeconds(-3), null, "Convert a negative Duration of -3 seconds; Timestamp should be before the epoch."), // large 10‑day duration
        Arguments.of(Duration.ofDays(10), null, "Convert a large Duration of 10 days; verifies handling of values that exceed typical short ranges."));
    }

    /**
     * Sub‑second precision cases.
     */
    private static Stream<Arguments> subSecondProvider() {
        return Stream.of(// 1500 ms = 1.5 s
        Arguments.of(Duration.ofMillis(1500), null, "Convert a Duration of 1500 milliseconds (1.5 seconds)."), // 500 000 ns = 0.5 ms
        Arguments.of(Duration.ofNanos(500_000), null, "Convert a Duration of 500,000 nanoseconds (0.5 ms)."), // 60 s + 123 456 789 ns
        Arguments.of(Duration.ofSeconds(60).plusNanos(123_456_789), null, "Convert a Duration that combines whole seconds with a nanosecond component."));
    }

    /**
     * Cases that should result in an exception because the Duration cannot be represented as a Timestamp.
     */
    private static Stream<Arguments> overflowProvider() {
        return Stream.of(// extremely large positive duration – overflow while adding to epoch
        Arguments.of(Duration.ofSeconds(Long.MAX_VALUE), null, ArithmeticException.class, "Attempt to convert an extremely large positive Duration that exceeds the range representable by java.sql.Timestamp."), // extremely large negative duration – overflow while adding to epoch
        Arguments.of(Duration.ofSeconds(Long.MIN_VALUE), null, ArithmeticException.class, "Attempt to convert an extremely large negative Duration that is out of Timestamp range."));
    }

    /**
     * Cases that trigger Timestamp.from's range checks (far beyond the year 292278994).
     */
    private static Stream<Arguments> timestampRangeOverflowProvider() {
        return Stream.of(Arguments.of(Duration.ofSeconds(10_000_000_000_000_000L), null, IllegalArgumentException.class, "Convert a Duration far exceeding Timestamp's maximum instant (≈317 million years)."), Arguments.of(Duration.ofSeconds(-10_000_000_000_000_000L), null, IllegalArgumentException.class, "Convert a large negative Duration beyond Timestamp's minimum instant."));
    }

    /**
     * Cases that should throw NullPointerException or ClassCastException.
     */
    private static Stream<Arguments> errorHandlingProvider() {
        return Stream.of(// null 'from' argument
        Arguments.of(null, null, NullPointerException.class, "Pass a null 'from' argument. The cast to Duration should trigger a NullPointerException."), // wrong type (String)
        Arguments.of("PT5S", null, ClassCastException.class, "Pass an object that is not a Duration (a String). Should throw ClassCastException."));
    }

    @Test
    void zeroDurationReturnsEpochTimestamp() {
        // given
        Duration duration = Duration.ZERO;
        // when
        Timestamp ts = DurationConversions.toTimestamp(duration, converter);
        // then
        assertNotNull(ts, "Timestamp should not be null");
        assertEquals(Instant.EPOCH, ts.toInstant(), "Timestamp for zero duration must represent the epoch");
        assertEquals(0L, ts.getTime(), "Epoch timestamp should have a millisecond value of 0");
    }

    @Test
    void positiveSecondsDuration() {
        // given
        // 5 seconds
        Duration duration = Duration.ofSeconds(5);
        // when
        Timestamp ts = DurationConversions.toTimestamp(duration, converter);
        // then
        Instant expected = Instant.EPOCH.plusSeconds(5);
        assertEquals(expected, ts.toInstant(), "Timestamp should represent epoch plus 5 seconds");
        assertEquals(5_000L, ts.getTime(), "Milliseconds part should be 5,000");
    }

    @Test
    void positiveNanosDuration() {
        // given
        // 0.123456789 seconds
        Duration duration = Duration.ofNanos(123_456_789);
        // when
        Timestamp ts = DurationConversions.toTimestamp(duration, converter);
        // then
        Instant expected = Instant.EPOCH.plusNanos(123_456_789);
        assertEquals(expected, ts.toInstant(), "Timestamp should represent epoch plus 123,456,789 nanoseconds");
        // Milliseconds part is truncated to whole ms; nanoseconds are stored separately.
        assertEquals(0L, ts.getTime(), "Millisecond part should be 0 for sub‑millisecond duration");
        assertEquals(123_456_789, ts.getNanos(), "Nanosecond part should match the original duration");
    }

    @Test
    void negativeDuration() {
        // given
        // -10 seconds
        Duration duration = Duration.ofSeconds(-10);
        // when
        Timestamp ts = DurationConversions.toTimestamp(duration, converter);
        // then
        Instant expected = Instant.EPOCH.minusSeconds(10);
        assertEquals(expected, ts.toInstant(), "Timestamp should represent epoch minus 10 seconds");
        assertEquals(-10_000L, ts.getTime(), "Milliseconds should be negative for a negative duration");
    }

    @Test
    void nullConverterIsHandledGracefully() {
        // given
        Duration duration = Duration.ofSeconds(1);
        // when
        Timestamp ts = DurationConversions.toTimestamp(duration, null);
        // then
        Instant expected = Instant.EPOCH.plusSeconds(1);
        assertEquals(expected, ts.toInstant(), "Method should not depend on the Converter argument");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("normalConversionProvider")
    void toTimestamp_normalConversions(Duration duration, Converter ignored, String description) {
        // When
        Timestamp ts = DurationConversions.toTimestamp(duration, null);
        // Then
        assertNotNull(ts, "Timestamp must not be null");
        Instant expected = Instant.EPOCH.plus(duration);
        assertEquals(expected, ts.toInstant(), "Timestamp instant does not match expected value.");
        // Verify millisecond part matches Instant's epoch milli conversion
        assertEquals(expected.toEpochMilli(), ts.getTime(), "Timestamp.getTime() (millis) mismatch.");
        // Verify nanosecond part (Timestamp stores nanos separately)
        assertEquals(expected.getNano(), ts.getNanos(), "Timestamp nanosecond component mismatch.");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("subSecondProvider")
    void toTimestamp_subSecondPrecision(Duration duration, Converter ignored, String description) {
        Timestamp ts = DurationConversions.toTimestamp(duration, null);
        assertNotNull(ts, "Timestamp must not be null");
        Instant expected = Instant.EPOCH.plus(duration);
        assertEquals(expected, ts.toInstant(), "Instant representation mismatch for sub‑second duration.");
        assertEquals(expected.toEpochMilli(), ts.getTime(), "Millisecond part mismatch.");
        assertEquals(expected.getNano(), ts.getNanos(), "Nanosecond part mismatch.");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("overflowProvider")
    void toTimestamp_overflowExceptions(Duration duration, Converter ignored, Class<? extends Throwable> expectedException, String description) {
        // The addition of the duration to Instant.EPOCH overflows and should propagate an exception.
        assertThrows(expectedException, () -> DurationConversions.toTimestamp(duration, null), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("timestampRangeOverflowProvider")
    void toTimestamp_timestampRangeOverflow(Duration duration, Converter ignored, Class<? extends Throwable> expectedException, String description) {
        // Timestamp.from checks the range of the resulting Instant and throws IllegalArgumentException
        assertThrows(expectedException, () -> DurationConversions.toTimestamp(duration, null), description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("errorHandlingProvider")
    void toTimestamp_errorHandling(Object from, Converter ignored, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> DurationConversions.toTimestamp(from, null), description);
    }
}
