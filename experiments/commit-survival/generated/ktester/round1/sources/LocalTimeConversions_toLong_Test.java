package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toLong_Test {

    /**
     * Mocked {@link Converter} – not used by {@code toLong} but required by the signature.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Provider for normal and truncation cases that should return a long value.
     */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// ----- Normal conversion group -----
        Arguments.of(LocalTime.of(0, 0, 0, 0), 0L, "Midnight (00:00:00.000) → 0 ms"), Arguments.of(LocalTime.of(12, 34, 56, 789_000_000), 45_286_789L, "Typical time 12:34:56.789 → 45,286,789 ms"), Arguments.of(LocalTime.of(23, 59, 59, 999_999_999), 86_399_999L, "One nanosecond before next day → 86,399,999 ms (truncates)"), // ----- Nanosecond truncation group -----
        Arguments.of(LocalTime.of(0, 0, 0, 999_999_999), 0L, "Just under 1 ms (999 999 999 ns) → 0 ms"), Arguments.of(LocalTime.of(0, 0, 0, 1_000_000), 1L, "Exactly 1 ms (1 000 000 ns) → 1 ms"), Arguments.of(LocalTime.of(0, 0, 0, 1_500_000), 1L, "1.5 ms (1 500 000 ns) → 1 ms (integer division truncates)"), Arguments.of(LocalTime.of(0, 0, 0, 999_000_000), 999L, "999 ms (999 000 000 ns) → 999 ms"));
    }

    /**
     * Provider for null input and invalid‑type cases.
     */
    private static Stream<Arguments> exceptionalConversionProvider() {
        return Stream.of(// Null input
        Arguments.of(null, NullPointerException.class, "Null 'from' argument should cause NullPointerException"), // Invalid type – String
        Arguments.of("not a LocalTime", ClassCastException.class, "String input should trigger ClassCastException"), // Invalid type – Integer
        Arguments.of(Integer.valueOf(5), ClassCastException.class, "Integer input should trigger ClassCastException"));
    }

    @Test
    void toLong_returnsZeroForMidnight() {
        // GIVEN
        // 00:00:00.000000000
        LocalTime midnight = LocalTime.MIDNIGHT;
        // WHEN
        long result = LocalTimeConversions.toLong(midnight, mockConverter);
        // THEN
        assertEquals(0L, result, "Midnight should convert to 0 milliseconds");
    }

    @Test
    void toLong_convertsTypicalTimeCorrectly() {
        // GIVEN
        // 13:45:30.123 (13 hours, 45 minutes, 30 seconds, 123 milliseconds)
        // 123_000_000 ns = 123 ms
        LocalTime time = LocalTime.of(13, 45, 30, 123_000_000);
        // WHEN
        long result = LocalTimeConversions.toLong(time, mockConverter);
        // THEN
        // Expected milliseconds = (13 * 60 * 60 + 45 * 60 + 30) * 1000 + 123
        long expected = ((13L * 60 * 60) + (45L * 60) + 30L) * 1000 + 123;
        assertEquals(expected, result, "The conversion should return the correct number of milliseconds since start of day");
    }

    @Test
    void toLong_handlesMaximumTimeOfDay() {
        // GIVEN
        // 23:59:59.999 (the last millisecond of the day)
        // 999 ms
        LocalTime time = LocalTime.of(23, 59, 59, 999_000_000);
        // WHEN
        long result = LocalTimeConversions.toLong(time, mockConverter);
        // THEN
        // Expected milliseconds = (24 * 60 * 60 * 1000) - 1 = 86_399_999
        long expected = 86_400_000L - 1;
        assertEquals(expected, result, "The conversion should correctly handle the last millisecond of the day");
    }

    @Test
    void toLong_throwsClassCastExceptionWhenObjectIsNotLocalTime() {
        // GIVEN
        Object notATime = "12:34:56";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toLong(notATime, mockConverter), "Passing a non‑LocalTime object should result in a ClassCastException");
    }

    /**
     * Successful conversions – normal times and nanosecond‑truncation cases.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulConversionProvider")
    void toLong_successfulConversions(LocalTime from, long expected, String description) {
        long actual = LocalTimeConversions.toLong(from, mockConverter);
        assertEquals(expected, actual, description);
    }

    /**
     * Cases that are expected to throw an exception (null or wrong type).
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionalConversionProvider")
    void toLong_throwsExpectedException(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> LocalTimeConversions.toLong(from, mockConverter), description);
    }
}
