package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toLocalTime_Test {

    @Mock
    private static Converter // Converter is not used by the method, a mock is sufficient
    mockConverter;

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Minimum valid value
        Arguments.of("Minimum valid value: 0 seconds → midnight", BigDecimal.valueOf(0), LocalTime.of(0, 0, 0, 0)), // Maximum valid value
        Arguments.of("Maximum valid value: 86 399.999999999 seconds → last nanosecond", new BigDecimal("86399.999999999"), LocalTime.of(23, 59, 59, 999_999_999)), // Typical midday with half‑second
        Arguments.of("Typical midday value with half‑second fraction", new BigDecimal("43200.5"), LocalTime.of(12, 0, 0, 500_000_000)), // Exactly one nanosecond
        Arguments.of("Exactly one nanosecond (1e‑9 seconds)", new BigDecimal("0.000000001"), LocalTime.ofNanoOfDay(1)), // Fractional nanosecond truncated to zero
        Arguments.of("Fractional nanosecond (0.5 ns) truncates to midnight", new BigDecimal("0.0000000005"), LocalTime.ofNanoOfDay(0)), // Just below maximum
        Arguments.of("Just below maximum (86 399.999999998 s)", new BigDecimal("86399.999999998"), LocalTime.of(23, 59, 59, 998_000_000)), // Zero with scale
        Arguments.of("Zero expressed with a scale (\"0.0\")", new BigDecimal("0.0"), LocalTime.of(0, 0, 0, 0)), // Exact noon, no nanos
        Arguments.of("Exact noon (43 200 s)", new BigDecimal("43200"), LocalTime.of(12, 0, 0, 0)), // Scientific notation for one nanosecond
        Arguments.of("Scientific‑notation one nanosecond (\"1E-9\")", new BigDecimal("1E-9"), LocalTime.ofNanoOfDay(1)), // Negative zero should be treated as zero
        Arguments.of("Negative zero (\"-0.0\") maps to midnight", new BigDecimal("-0.0"), LocalTime.of(0, 0, 0, 0)), // Non‑null Converter is ignored
        Arguments.of("Non‑null Converter ignored", new BigDecimal("43200.5"), LocalTime.of(12, 0, 0, 500_000_000)));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Negative value
        Arguments.of("Negative value should trigger IllegalArgumentException", new BigDecimal("-0.000000001"), IllegalArgumentException.class), // Value equal to 86400 seconds (out of range)
        Arguments.of("Value equal to 86400 seconds exceeds allowed range", new BigDecimal("86400"), IllegalArgumentException.class), // Extremely large value that overflows long nanoseconds
        Arguments.of("Extremely large value should cause IllegalArgumentException", new BigDecimal("1e20"), IllegalArgumentException.class), // Null source object
        Arguments.of("Null source should result in NullPointerException", null, NullPointerException.class), // Type mismatch – Integer
        Arguments.of("Integer source causes ClassCastException", Integer.valueOf(5), ClassCastException.class), // Type mismatch – String
        Arguments.of("String source causes ClassCastException", "123.45", ClassCastException.class), // Type mismatch – generic Object
        Arguments.of("Generic Object source causes ClassCastException", new Object(), ClassCastException.class));
    }

    @Test
    void convertsZeroSecondsToMidnight() {
        BigDecimal input = BigDecimal.ZERO;
        LocalTime result = BigDecimalConversions.toLocalTime(input, mockConverter);
        assertEquals(LocalTime.MIDNIGHT, result);
    }

    @Test
    void convertsFractionalSecondsToLocalTime() {
        // 12.345678901 seconds → 12_345_678_901 nanoseconds
        BigDecimal input = new BigDecimal("12.345678901");
        LocalTime expected = LocalTime.ofNanoOfDay(12_345_678_901L);
        LocalTime result = BigDecimalConversions.toLocalTime(input, mockConverter);
        assertEquals(expected, result);
    }

    @Test
    void convertsMaximumAllowedSecondsToLocalTime() {
        // 86 399.999 999 999 seconds is the last nanosecond of the day
        BigDecimal input = new BigDecimal("86399.999999999");
        long nanos = 86_399_999_999_999L;
        LocalTime expected = LocalTime.ofNanoOfDay(nanos);
        LocalTime result = BigDecimalConversions.toLocalTime(input, mockConverter);
        assertEquals(expected, result);
    }

    @Test
    void throwsWhenNegativeSecondsProvided() {
        BigDecimal input = new BigDecimal("-0.1");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toLocalTime(input, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"));
    }

    @Test
    void throwsWhenSecondsExceedDayLength() {
        // exactly one day – out of range
        BigDecimal input = new BigDecimal("86400");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toLocalTime(input, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"));
    }

    /**
     * Conversion tests – valid inputs **
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void validConversions(String description, BigDecimal input, LocalTime expected) {
        // The converter argument is deliberately null in the JSON spec,
        // but the method does not use it, so we can safely pass null.
        LocalTime result = BigDecimalConversions.toLocalTime(input, null);
        assertEquals(expected, result, description);
    }

    /**
     * Exception tests – invalid inputs **
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void exceptionScenarios(String description, Object from, Class<? extends Throwable> expectedException) {
        // Cast to Object because the method expects Object; we deliberately pass wrong types.
        assertThrows(expectedException, () -> BigDecimalConversions.toLocalTime(from, null), description);
    }
}
