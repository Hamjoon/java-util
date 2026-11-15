package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.HashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toMap_Test {

    /**
     * A mock {@link Converter}. The method under test does not use it, but it must be supplied.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Standard durations
        Arguments.of(Duration.ofSeconds(10, 500), mapOf(10L, 500), "Positive duration with seconds and nanos"), Arguments.of(Duration.ZERO, mapOf(0L, 0), "Zero duration"), // Negative and large values
        Arguments.of(Duration.ofSeconds(-5, -300), // Normalised by Duration: -6 seconds + 999,999,700 nanos
        mapOf(-6L, 999_999_700), "Negative duration normalised"), Arguments.of(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999), mapOf(Long.MAX_VALUE, 999_999_999), "Maximum long seconds with max nanos"), // Normalisation edge cases
        Arguments.of(Duration.ofSeconds(0, -1), // Normalises to -1 second + 999,999,999 nanos
        mapOf(-1L, 999_999_999), "Zero seconds with a negative nanosecond adjustment"), Arguments.of(Duration.ofSeconds(-10), mapOf(-10L, 0), "Negative whole‑second duration with no nanos"), // Large values handling
        Arguments.of(Duration.ofSeconds(Long.MIN_VALUE, 0), mapOf(Long.MIN_VALUE, 0), "Minimum long value for seconds with zero nanos"), // Converter usage (non‑null converter should have no effect)
        Arguments.of(Duration.ofSeconds(5, 250), mapOf(5L, 250), "Providing a non‑null Converter should have no effect"), // Nanos‑only duration
        Arguments.of(Duration.ofNanos(123_456_789), mapOf(0L, 123_456_789), "Duration created from nanos only"));
    }

    private static Stream<Arguments> exceptionalConversionProvider() {
        return Stream.of(// Null from argument
        Arguments.of(null, NullPointerException.class, "Null 'from' argument should throw NPE"), // Non‑Duration object
        Arguments.of("not a duration", ClassCastException.class, "Non‑Duration object should cause ClassCastException"), // Overflow caused by adding a negative nanosecond adjustment to Long.MIN_VALUE seconds
        Arguments.of(Duration.ofSeconds(Long.MIN_VALUE, -999_999_999), ArithmeticException.class, "Overflow when normalising Long.MIN_VALUE with negative nanos"));
    }

    /**
     * Convenience method to create the expected map.
     *
     * @param seconds seconds component
     * @param nanos   nanos component
     * @return map containing {@code "seconds"} and {@code "nanos"} entries
     */
    private static Map<String, Object> mapOf(long seconds, int nanos) {
        Map<String, Object> map = new HashMap<>();
        map.put("seconds", seconds);
        map.put("nanos", nanos);
        return map;
    }

    @Test
    void toMap_withPositiveDuration_returnsCorrectMap() {
        // given
        // 12345 seconds + 678 ms
        Duration duration = Duration.ofSeconds(12345L, 678_000_000);
        // when
        Map<String, Object> result = DurationConversions.toMap(duration, mockConverter);
        // then
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(2, result.size(), "Map should contain exactly two entries");
        assertEquals(12345L, result.get("seconds"));
        assertEquals(678_000_000, result.get("nanos"));
    }

    @Test
    void toMap_withZeroDuration_returnsMapWithZeroValues() {
        // given
        Duration zero = Duration.ZERO;
        // when
        Map<String, Object> result = DurationConversions.toMap(zero, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(0L, result.get("seconds"));
        assertEquals(0, result.get("nanos"));
    }

    @Test
    void toMap_withNegativeDuration_returnsCorrectMap() {
        // given
        // -10.5 seconds
        Duration negative = Duration.ofSeconds(-10L, -500_000_000);
        // when
        Map<String, Object> result = DurationConversions.toMap(negative, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(-10L, result.get("seconds"));
        // Duration normalises nanos to be within 0..999_999_999, so -500_000_000 becomes -500_000_000
        // (the Duration API keeps the sign on nanos). We assert the exact value returned.
        assertEquals(-500_000_000, result.get("nanos"));
    }

    @Test
    void toMap_withNonDurationObject_throwsClassCastException() {
        // given
        Object notADuration = "not a duration";
        // when / then
        assertThrows(ClassCastException.class, () -> DurationConversions.toMap(notADuration, mockConverter));
    }

    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("successfulConversionProvider")
    void toMap_successful(Duration input, Map<String, Object> expected, String description) {
        // when
        Map<String, Object> result = DurationConversions.toMap(input, mockConverter);
        // then
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(expected.size(), result.size(), "Result map should contain exactly the expected number of entries");
        assertEquals(expected.get("seconds"), result.get("seconds"), "Seconds value does not match for: " + description);
        assertEquals(expected.get("nanos"), result.get("nanos"), "Nanos value does not match for: " + description);
    }

    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("exceptionalConversionProvider")
    void toMap_exceptional(Object input, Class<? extends Throwable> expectedException, String description) {
        // when / then
        assertThrows(expectedException, () -> DurationConversions.toMap(input, mockConverter), "Expected exception for case: " + description);
    }
}
