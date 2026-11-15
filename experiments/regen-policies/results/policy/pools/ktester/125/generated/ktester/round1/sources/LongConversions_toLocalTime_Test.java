package com.cedarsoftware.util.convert;

import java.time.LocalTime;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class LongConversions_toLocalTime_Test {

    /**
     * Mocked Converter – not used by the method under test but required for the call signature.
     */
    @Mock
    Converter converter;

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Zero milliseconds → midnight
        Arguments.of(0L, LocalTime.MIDNIGHT), // Typical positive value within the day range
        // 12_345_678 ms = 3h 25m 45.678s
        Arguments.of(12_345_678L, LocalTime.of(3, 25, 45, 678_000_000)), // Maximum allowed millisecond value (one millisecond before next day)
        Arguments.of(86_399_999L, LocalTime.of(23, 59, 59, 999_000_000)));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// Negative values
        Arguments.of(-1L, IllegalArgumentException.class), Arguments.of(-1000L, IllegalArgumentException.class), // Null 'from' – unboxing a null Long leads to NPE
        Arguments.of(null, NullPointerException.class), // Wrong type – ClassCastException from the cast (Long) from
        Arguments.of("123", ClassCastException.class), // Out‑of‑range positive values
        // exactly one day
        Arguments.of(86_400_000L, IllegalArgumentException.class), // clearly beyond
        Arguments.of(100_000_000L, IllegalArgumentException.class), // Overflow scenario – multiplication overflows long range
        Arguments.of(Long.MAX_VALUE, IllegalArgumentException.class));
    }

    @Test
    void shouldConvertMidnight() {
        // given
        Long millis = 0L;
        // when
        LocalTime result = LongConversions.toLocalTime(millis, converter);
        // then
        assertEquals(LocalTime.MIDNIGHT, result, "0 milliseconds should map to LocalTime.MIDNIGHT");
    }

    @Test
    void shouldConvertNoon() {
        // given
        // 12:00:00.000
        Long millis = 12L * 60 * 60 * 1000;
        // when
        LocalTime result = LongConversions.toLocalTime(millis, converter);
        // then
        assertEquals(LocalTime.NOON, result, "12 hours in milliseconds should map to LocalTime.NOON");
    }

    @Test
    void shouldConvertMaximumValidValue() {
        // given
        // 23:59:59.999
        Long millis = 86_399_999L;
        // when
        LocalTime result = LongConversions.toLocalTime(millis, converter);
        // then
        LocalTime expected = LocalTime.of(23, 59, 59, 999_000_000);
        assertEquals(expected, result, "Maximum valid millisecond value should map to 23:59:59.999");
    }

    @Test
    void shouldThrowWhenNegativeMillis() {
        // given
        Long millis = -1L;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> LongConversions.toLocalTime(millis, converter));
        assertTrue(ex.getMessage().contains("Input value [-1]"), "Exception message should contain the offending value");
    }

    @Test
    void shouldThrowWhenMillisExceedDayLength() {
        // given
        // 24:00:00.000 – beyond the allowed range
        Long millis = 86_400_000L;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> LongConversions.toLocalTime(millis, converter));
        assertTrue(ex.getMessage().contains("Input value [86400000]"), "Exception message should contain the offending value");
    }

    /**
     * Valid conversion scenarios.
     *
     * <p>Each argument consists of the {@code from} value (a {@link Long})
     * and the expected {@link LocalTime} result.</p>
     */
    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("validConversionProvider")
    void shouldConvertValidValues(Object from, LocalTime expected) {
        // The method does not use the Converter, so we can safely pass {@code null}
        LocalTime actual = LongConversions.toLocalTime(from, null);
        assertEquals(expected, actual, () -> "Conversion of " + from + " should yield " + expected);
    }

    /**
     * Invalid conversion scenarios – each entry defines the {@code from}
     * argument and the type of exception that must be thrown.
     *
     * <p>For {@code IllegalArgumentException} we also verify that the
     * exception message contains the offending value.</p>
     */
    @ParameterizedTest(name = "[{index}] from={0} → throws {1}")
    @MethodSource("invalidConversionProvider")
    void shouldThrowForInvalidValues(Object from, Class<? extends Throwable> expectedException) {
        if (expectedException == IllegalArgumentException.class) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> LongConversions.toLocalTime(from, null));
            // The message should contain the raw input value (even if it is null)
            String valueString = String.valueOf(from);
            assertTrue(ex.getMessage().contains(valueString), () -> "Exception message should contain the offending value [" + valueString + "]");
        } else {
            assertThrows(expectedException, () -> LongConversions.toLocalTime(from, null));
        }
    }
}
