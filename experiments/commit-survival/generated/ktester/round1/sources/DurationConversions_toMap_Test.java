package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Instant;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toMap_Test {

    /**
     * A mock {@link Converter}. The method under test does not use it, but it must be supplied.
     */
    @Mock
    private Converter converter;

    /**
     * A mock {@link Converter}. The method under test does not use it, but it must be supplied.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple holder for a single test case. Using a custom class makes the
     * {@code @MethodSource} signature cleaner and allows us to attach a
     * description that appears in the parameterised test name.
     */
    private static final class ArgumentsHolder {

        final Duration from;

        // may be null – test will substitute mock
        final Converter converter;

        final Map<String, Object> expected;

        final String description;

        ArgumentsHolder(Duration from, Converter converter, Map<String, Object> expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private static Stream<ArgumentsHolder> validDurationProvider() {
        return Stream.of(// Standard positive duration
        new ArgumentsHolder(Duration.ofSeconds(5, 123_456_789), null, Map.of("seconds", 5L, "nanos", 123_456_789), "Standard positive duration – verifies that seconds and nanos are correctly extracted and placed into the map."), // Zero duration
        new ArgumentsHolder(Duration.ZERO, null, Map.of("seconds", 0L, "nanos", 0), "Zero duration – ensures the method handles the edge case where both fields are zero."), // Negative duration with nanosecond adjustment
        new ArgumentsHolder(Duration.ofSeconds(-5).minusNanos(300), null, Map.of("seconds", -6L, "nanos", 999_999_700), "Negative duration with nanosecond adjustment – checks proper normalization performed by Duration and correct map values."), // Maximum long seconds with maximum nanos
        new ArgumentsHolder(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999), null, Map.of("seconds", Long.MAX_VALUE, "nanos", 999_999_999), "Maximum long seconds with maximum nanos – validates that very large values are handled without overflow."), // Duration consisting solely of nanoseconds (< 1 second)
        new ArgumentsHolder(Duration.ofNanos(500), null, Map.of("seconds", 0L, "nanos", 500), "Duration consisting solely of nanoseconds less than one second – verifies that seconds remain zero and nanos are captured correctly."), // Negative nanos only duration (normalises to -1 sec + 999_999_500 nanos)
        new ArgumentsHolder(Duration.ofNanos(-500), null, Map.of("seconds", -1L, "nanos", 999_999_500), "Duration created with a negative nanosecond value – checks Java's normalization (borrow one second) and correct map representation."), // Overflow nanos (2_000_000_000 ns -> +2 seconds)
        new ArgumentsHolder(Duration.ofSeconds(1, 2_000_000_000), null, Map.of("seconds", 3L, "nanos", 0), "Duration where the nanosecond part exceeds one second – ensures overflow is normalized into seconds."), // Minimum long seconds
        new ArgumentsHolder(Duration.ofSeconds(Long.MIN_VALUE), null, Map.of("seconds", Long.MIN_VALUE, "nanos", 0), "Duration with the smallest possible second value – validates that the method handles extreme negative values without overflow."), // Non‑null converter ignored
        new ArgumentsHolder(Duration.ofSeconds(2, 250), new Converter(), Map.of("seconds", 2L, "nanos", 250), "Supplying a non‑null Converter instance confirms that the method does not use the converter and still returns the correct map."));
    }

    @Test
    void toMap_returnsMapWithSecondsAndNanos_forPositiveDuration() {
        // given
        Duration duration = Duration.ofSeconds(12345, 678_901_234);
        // when
        Map<String, Object> result = DurationConversions.toMap(duration, converter);
        // then
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(2, result.size(), "Map should contain exactly two entries");
        assertEquals(12345L, result.get("seconds"));
        assertEquals(678_901_234, result.get("nanos"));
    }

    @Test
    void toMap_returnsMapWithSecondsAndNanos_forZeroDuration() {
        // given
        Duration duration = Duration.ZERO;
        // when
        Map<String, Object> result = DurationConversions.toMap(duration, converter);
        // then
        assertNotNull(result);
        assertEquals(0L, result.get("seconds"));
        assertEquals(0, result.get("nanos"));
    }

    @Test
    void toMap_returnsMapWithSecondsAndNanos_forNegativeDuration() {
        // given
        Duration duration = Duration.ofSeconds(-10, -500);
        // when
        Map<String, Object> result = DurationConversions.toMap(duration, converter);
        // then
        assertNotNull(result);
        // Duration normalises negative nanos, so we compute expected values:
        // -11 after normalisation
        long expectedSeconds = duration.getSeconds();
        // 999_999_500
        int expectedNanos = duration.getNano();
        assertEquals(expectedSeconds, result.get("seconds"));
        assertEquals(expectedNanos, result.get("nanos"));
    }

    @Test
    void toMap_throwsClassCastException_whenObjectIsNotDuration() {
        // given
        Object notADuration = "I am not a Duration";
        // when / then
        assertThrows(ClassCastException.class, () -> DurationConversions.toMap(notADuration, converter));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validDurationProvider")
    void toMap_validDurations(ArgumentsHolder holder) {
        // given
        Object from = holder.from;
        Converter converter = holder.converter != null ? holder.converter : mockConverter;
        // when
        Map<String, Object> result = DurationConversions.toMap(from, converter);
        // then
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(2, result.size(), "Map should contain exactly two entries");
        assertEquals(holder.expected.get("seconds"), result.get("seconds"), "Seconds value does not match expected");
        assertEquals(holder.expected.get("nanos"), result.get("nanos"), "Nanos value does not match expected");
    }

    @Test
    void toMap_throwsNullPointerException_whenFromIsNull() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> DurationConversions.toMap(from, mockConverter), "A null 'from' argument should cause NullPointerException due to the cast to Duration.");
    }

    @Test
    void toMap_throwsClassCastException_whenFromIsNotDuration() {
        // given
        Object notADuration = "I am not a Duration";
        // when / then
        assertThrows(ClassCastException.class, () -> DurationConversions.toMap(notADuration, mockConverter), "Passing an object of the wrong type must result in ClassCastException.");
    }
}
