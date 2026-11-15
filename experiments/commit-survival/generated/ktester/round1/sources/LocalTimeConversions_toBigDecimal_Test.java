package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toBigDecimal_Test {

    @Mock
    Converter // not used by the method, but required by the signature
    mockConverter;

    /**
     * Provides a stream of valid conversion arguments.
     * Each argument consists of:
     *   1. the {@link LocalTime} to convert,
     *   2. the {@link Converter} to pass (null or a real instance),
     *   3. the expected {@link BigDecimal} string representation (scale 9).
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> validConversionsProvider() {
        return Stream.of(// Midnight – zero seconds
        arguments(LocalTime.of(0, 0, 0, 0), null, "0.000000000"), // Typical time with nanoseconds
        arguments(LocalTime.of(12, 34, 56, 123_456_789), null, "45296.123456789"), // Last possible nanosecond of the day
        arguments(LocalTime.of(23, 59, 59, 999_999_999), null, "86399.999999999"), // One hour after midnight, with a non‑null Converter
        arguments(LocalTime.of(1, 0, 0, 0), new Converter(), "3600.000000000"), // Single nanosecond after midnight
        arguments(LocalTime.of(0, 0, 0, 1), null, "0.000000001"), // Half‑second nanoseconds
        arguments(LocalTime.of(0, 0, 0, 500_000_000), null, "0.500000000"), // Exactly one second after midnight
        arguments(LocalTime.of(0, 0, 1, 0), null, "1.000000000"), // One minute after midnight
        arguments(LocalTime.of(0, 1, 0, 0), null, "60.000000000"), // Mixed hour/minute/second/nanosecond
        arguments(LocalTime.of(2, 30, 15, 250_000_000), null, "9015.250000000"), // Last whole second of the day without nanoseconds
        arguments(LocalTime.of(23, 59, 59, 0), null, "86399.000000000"), // Noon with a non‑null Converter
        arguments(LocalTime.of(12, 0, 0, 0), new Converter(), "43200.000000000"));
    }

    @Test
    void toBigDecimal_midnight_isZero() {
        // given
        LocalTime midnight = LocalTime.MIDNIGHT;
        // when
        BigDecimal result = LocalTimeConversions.toBigDecimal(midnight, mockConverter);
        // then
        BigDecimal expected = BigDecimal.ZERO.setScale(9, RoundingMode.HALF_UP);
        assertEquals(expected, result, "Midnight should convert to 0.000000000");
    }

    @Test
    void toBigDecimal_arbitraryTime_matchesManualCalculation() {
        // given
        // 12:34:56.789123456
        LocalTime time = LocalTime.of(12, 34, 56, 789_123_456);
        // when
        BigDecimal result = LocalTimeConversions.toBigDecimal(time, mockConverter);
        // then
        BigDecimal nanos = new BigDecimal(time.toNanoOfDay());
        BigDecimal expected = nanos.divide(BigDecimal.valueOf(1_000_000_000L), 9, RoundingMode.HALF_UP);
        assertEquals(expected, result, "Conversion should match manual calculation for arbitrary LocalTime");
    }

    @Test
    void toBigDecimal_nullInput_throwsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toBigDecimal(nullObj, mockConverter), "Passing null should result in NullPointerException");
    }

    @Test
    void toBigDecimal_wrongType_throwsClassCastException() {
        // given
        Object notALocalTime = "12:34:56";
        // when / then
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toBigDecimal(notALocalTime, null), "Passing a non‑LocalTime object should cause ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0} → {2}")
    @MethodSource("validConversionsProvider")
    void toBigDecimal_validConversions(LocalTime from, Converter converter, String expectedString) {
        // when
        BigDecimal result = LocalTimeConversions.toBigDecimal(from, converter);
        // then
        BigDecimal expected = new BigDecimal(expectedString);
        assertEquals(expected, result, () -> String.format("LocalTime %s should convert to %s", from, expectedString));
        // also verify the scale is exactly 9
        assertEquals(9, result.scale(), () -> "Resulting BigDecimal must have a scale of 9");
    }

    @Test
    void toBigDecimal_nullFrom_throwsNullPointerException() {
        // given
        Object nullFrom = null;
        // when / then
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toBigDecimal(nullFrom, null), "Null `from` argument should cause NullPointerException");
    }

    @Test
    void toBigDecimal_scaleIsAlwaysNine() {
        // given
        LocalTime time = LocalTime.of(5, 45, 30, 123_456_789);
        // when
        BigDecimal result = LocalTimeConversions.toBigDecimal(time, null);
        // then
        assertEquals(9, result.scale(), "Resulting BigDecimal must have a scale of 9 regardless of the input value.");
    }
}
