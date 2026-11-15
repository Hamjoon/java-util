package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.LocalTime;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class IntegerConversions_toLocalTime_Test {

    @Mock
    private Converter // a mock is sufficient – the method only forwards it
    mockConverter;

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// 0 ms → 00:00:00
        arguments(0, LocalTime.ofSecondOfDay(0)), // 3 600 000 ms → 01:00:00
        arguments(3_600_000, LocalTime.of(1, 0, 0)), // 86 399 000 ms → 23:59:59
        arguments(86_399_000, LocalTime.of(23, 59, 59, 0)));
    }

    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(// -1 ms → last millisecond of previous day (23:59:59.999)
        arguments(-1, LocalTime.of(23, 59, 59, 999_000_000)), // Integer.MAX_VALUE ms → remainder after full days
        arguments(Integer.MAX_VALUE, computeLocalTimeFromMillis(Integer.MAX_VALUE)), // exactly one day → wraps to midnight
        arguments(86_400_000, LocalTime.ofSecondOfDay(0)));
    }

    /**
     * Computes the {@link LocalTime} that corresponds to the given millisecond count
     * taking the 24‑hour wrap‑around into account and handling negative values.
     *
     * @param ms milliseconds since midnight (may be negative or larger than a day)
     * @return the LocalTime representing that instant of day
     */
    private static LocalTime computeLocalTimeFromMillis(long ms) {
        final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;
        long normalized = ((ms % MILLIS_PER_DAY) + MILLIS_PER_DAY) % MILLIS_PER_DAY;
        long seconds = normalized / 1000;
        int nanos = (int) ((normalized % 1000) * 1_000_000);
        return LocalTime.ofSecondOfDay(seconds).plusNanos(nanos);
    }

    /**
     * Verify that a valid {@link Integer} value is correctly converted to {@link LocalTime}
     * by delegating to {@link LongConversions#toLocalTime(long, Converter)}.
     */
    @Test
    void toLocalTime_ValidInteger_DelegatesToLongConversions() {
        // GIVEN
        // some arbitrary millisecond count
        Integer millis = 12345;
        // WHEN
        LocalTime result = IntegerConversions.toLocalTime(millis, mockConverter);
        // THEN
        LocalTime expected = LongConversions.toLocalTime(millis.longValue(), mockConverter);
        assertEquals(expected, result, "IntegerConversions should delegate to LongConversions with the same millisecond value");
    }

    /**
     * Passing {@code null} as the source object should result in a {@link NullPointerException}
     * because the method attempts to unbox the {@link Integer}.
     */
    @Test
    void toLocalTime_NullSource_ThrowsNullPointerException() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> IntegerConversions.toLocalTime(nullSource, mockConverter), "Converting a null source should throw NullPointerException");
    }

    /**
     * Passing an object that is not an {@link Integer} should result in a {@link ClassCastException}.
     */
    @Test
    void toLocalTime_NonIntegerSource_ThrowsClassCastException() {
        // GIVEN
        Object notAnInteger = "not an integer";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> IntegerConversions.toLocalTime(notAnInteger, mockConverter), "Converting a non‑Integer source should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("normalConversionProvider")
    void toLocalTime_NormalConversions(Integer from, LocalTime expected) {
        // WHEN
        LocalTime actual = IntegerConversions.toLocalTime(from, mockConverter);
        // THEN
        assertEquals(expected, actual, () -> "Integer " + from + " ms should be converted to " + expected);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("edgeCaseProvider")
    void toLocalTime_EdgeCases(Integer from, LocalTime expected) {
        // WHEN
        LocalTime actual = IntegerConversions.toLocalTime(from, mockConverter);
        // THEN
        assertEquals(expected, actual, () -> "Edge case Integer " + from + " ms should be converted to " + expected);
    }

    @Test
    void toLocalTime_NullConverter_ThrowsNullPointerException() {
        // GIVEN
        Integer millis = 123;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> IntegerConversions.toLocalTime(millis, null), "Passing a null converter should result in NullPointerException");
    }
}
