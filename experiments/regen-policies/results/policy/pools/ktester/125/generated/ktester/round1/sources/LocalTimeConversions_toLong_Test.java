package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toLong_Test {

    // not used by the method but required by signature
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The implementation ignores the converter, but we create a default instance
        // to use in tests that require a non‑null value.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference
        converter = null;
    }

    /**
     * Provides a rich set of inputs covering normal, edge‑case and error scenarios.
     *
     * @return a stream of arguments: description, source object, converter (may be null),
     *         expected result (Long) or expected exception class.
     */
    private static Stream<Arguments> provideToLongCases() {
        // Helper to create a non‑null, but otherwise default, Converter instance.
        Converter nonNullConverter = new Converter(new ConverterOptions());
        return Stream.of(// Normal conversions
        Arguments.of("Midnight (00:00:00.000)", LocalTime.of(0, 0, 0, 0), null, 0L, null), Arguments.of("Less than one millisecond (999 999 ns)", LocalTime.of(0, 0, 0, 999_999), null, 0L, null), Arguments.of("Exactly one millisecond (1 000 000 ns)", LocalTime.of(0, 0, 0, 1_000_000), null, 1L, null), Arguments.of("Typical time with nanoseconds", LocalTime.of(12, 34, 56, 789_000_000), null, 45_296_789L, null), Arguments.of("Maximum LocalTime", LocalTime.MAX, null, 86_399_999L, null), Arguments.of("Just under two milliseconds (1 999 999 ns)", LocalTime.of(0, 0, 0, 1_999_999), null, 1L, null), Arguments.of("Exactly two milliseconds (2 000 000 ns)", LocalTime.of(0, 0, 0, 2_000_000), null, 2L, null), Arguments.of("One full second", LocalTime.of(0, 0, 1, 0), null, 1_000L, null), Arguments.of("One full minute", LocalTime.of(0, 1, 0, 0), null, 60_000L, null), Arguments.of("One full hour", LocalTime.of(1, 0, 0, 0), null, 3_600_000L, null), Arguments.of("Just under max millisecond of the day (23:59:59.9995)", LocalTime.of(23, 59, 59, 999_500_000), null, 86_399_999L, null), // Converter argument variations (the method ignores it)
        Arguments.of("Mid‑day with non‑null converter", LocalTime.of(12, 0, 0, 0), nonNullConverter, 43_200_000L, null), Arguments.of("Midnight with non‑null converter", LocalTime.of(0, 0, 0, 0), nonNullConverter, 0L, null), // Error scenarios
        Arguments.of("Null source object", null, null, null, NullPointerException.class), Arguments.of("Non‑LocalTime source (String)", "12:34:56.789", null, null, ClassCastException.class));
    }

    @Test
    void toLong_midnight_returnsZero() {
        // GIVEN
        // 00:00:00.000000000
        LocalTime midnight = LocalTime.MIDNIGHT;
        // WHEN
        long result = LocalTimeConversions.toLong(midnight, converter);
        // THEN
        assertEquals(0L, result, "Midnight should be converted to 0 milliseconds");
    }

    @Test
    void toLong_noon_returnsCorrectMilliseconds() {
        // GIVEN
        // 12:00:00.000000000
        LocalTime noon = LocalTime.NOON;
        // WHEN
        long result = LocalTimeConversions.toLong(noon, converter);
        // THEN
        // 12 hours * 60 min * 60 sec * 1000 ms = 43_200_000 ms
        assertEquals(43_200_000L, result);
    }

    @Test
    void toLong_randomTime_correctConversion() {
        // GIVEN
        // 13:45:30.123456789
        LocalTime time = LocalTime.of(13, 45, 30, 123_456_789);
        // WHEN
        long result = LocalTimeConversions.toLong(time, converter);
        // THEN
        // Compute expected milliseconds:
        // (13 * 3600 + 45 * 60 + 30) seconds = 49530 seconds
        // 49530 * 1000 = 49_530_000 ms
        // plus nanoseconds part 123_456_789 / 1_000_000 = 123 ms
        long expected = 49_530_000L + 123L;
        assertEquals(expected, result);
    }

    @Test
    void toLong_maxTime_returnsMillisecondsInDayMinusOne() {
        // GIVEN
        // 23:59:59.999999999 is the maximum LocalTime value
        LocalTime max = LocalTime.of(23, 59, 59, 999_999_999);
        // WHEN
        long result = LocalTimeConversions.toLong(max, converter);
        // THEN
        // Total nanoseconds in a day = 86_400_000_000_000
        // Minus 1 nanosecond = 86_399_999_999_999
        // Convert to milliseconds = 86_399_999 (since integer division truncates)
        long expected = 86_399_999L;
        assertEquals(expected, result);
    }

    @Test
    void toLong_nullInput_throwsClassCastException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toLong(nullObj, converter));
    }

    @Test
    void toLong_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toLong(nullObj, converter));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideToLongCases")
    void toLong_parameterised(String description, Object from, Converter conv, Long expectedResult, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            // Expect an exception – the converter argument may be null.
            assertThrows(expectedException, () -> LocalTimeConversions.toLong(from, conv), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
        } else {
            // Normal conversion – ensure the result matches the expected milliseconds.
            long actual = LocalTimeConversions.toLong(from, conv);
            assertEquals(expectedResult.longValue(), actual, () -> "Incorrect conversion for case: " + description);
        }
    }
}
