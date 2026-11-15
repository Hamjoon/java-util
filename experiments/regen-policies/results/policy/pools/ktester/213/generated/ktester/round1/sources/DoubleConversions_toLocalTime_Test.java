package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toLocalTime_Test {

    /**
     * The Converter instance required by the method under test.
     *  It is not used by {@code DoubleConversions.toLocalTime}, so a mock is sufficient.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// Zero seconds – already tested above but kept for completeness
        Arguments.of("Zero seconds → midnight", 0.0, "00:00:00"), // One whole second
        Arguments.of("One second", 1.0, "00:00:01"), // Fractional seconds with nanosecond precision
        Arguments.of("Fractional seconds (12.3456789)", 12.3456789, "00:00:12.345678900"), // Maximum allowed value (just before next day)
        Arguments.of("Maximum allowed (86399.999999999)", 86_399.999_999_999, "23:59:59.999999999"), // Smallest positive nanosecond
        Arguments.of("Smallest positive nanosecond (0.000000001)", 0.000000001, "00:00:00.000000001"), // Just under one second
        Arguments.of("Just under one second (0.999999999)", 0.999999999, "00:00:00.999999999"), // Maximum minus one nanosecond
        Arguments.of("Maximum minus one nanosecond (86399.999999998)", 86_399.999_999_998, "23:59:59.999999998"));
    }

    private static Stream<Arguments> illegalArgumentProvider() {
        return Stream.of(Arguments.of("Negative value slightly below zero", -0.000000001), Arguments.of("Exactly 24 hours (86400.0)", 86_400.0), Arguments.of("Very large number (1e10)", 1e10), Arguments.of("NaN", Double.NaN), Arguments.of("Positive Infinity", Double.POSITIVE_INFINITY));
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of("Null reference", null, NullPointerException.class), Arguments.of("String value", "123.45", ClassCastException.class), Arguments.of("Boolean value", true, ClassCastException.class), Arguments.of("Float object", new Float(12.34f), ClassCastException.class), Arguments.of("BigDecimal object", new BigDecimal("123.45"), ClassCastException.class));
    }

    @Test
    void toLocalTime_zeroSeconds_returnsMidnight() {
        // given
        Double from = 0.0;
        // when
        LocalTime result = DoubleConversions.toLocalTime(from, mockConverter);
        // then
        assertEquals(LocalTime.MIDNIGHT, result);
    }

    @Test
    void toLocalTime_oneHour_returns01_00() {
        // given
        // 1 hour = 3600 seconds
        Double from = 3600.0;
        // when
        LocalTime result = DoubleConversions.toLocalTime(from, mockConverter);
        // then
        assertEquals(LocalTime.of(1, 0), result);
    }

    @Test
    void toLocalTime_fractionalSeconds_returnsCorrectNanos() {
        // given
        // half a second
        Double from = 0.5;
        // when
        LocalTime result = DoubleConversions.toLocalTime(from, mockConverter);
        // then
        // 0.5 seconds = 500_000_000 nanoseconds
        assertEquals(LocalTime.ofNanoOfDay(500_000_000L), result);
    }

    @Test
    void toLocalTime_maximumValidValue_returnsLastNanosecondOfDay() {
        // given
        // 86 399.999 999 999 seconds = one day minus one nanosecond
        Double from = 86_399.999_999_999;
        // when
        LocalTime result = DoubleConversions.toLocalTime(from, mockConverter);
        // then
        assertEquals(LocalTime.ofNanoOfDay(86_399_999_999_999_999L), result);
    }

    @Test
    void toLocalTime_negativeSeconds_throwsIllegalArgumentException() {
        // given
        Double from = -1.0;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toLocalTime(from, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"));
    }

    @Test
    void toLocalTime_exceedsMaximum_throwsIllegalArgumentException() {
        // given
        // exactly one day – out of range
        Double from = 86_400.0;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toLocalTime(from, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"));
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("validConversionsProvider")
    void toLocalTime_validConversions(String description, Object from, String expectedIso) {
        // when
        LocalTime result = DoubleConversions.toLocalTime(from, mockConverter);
        // then
        LocalTime expected = LocalTime.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("illegalArgumentProvider")
    void toLocalTime_illegalArgumentValues(String description, Object from) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toLocalTime(from, mockConverter), description);
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"), "Exception message should explain the allowed range");
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("invalidTypeProvider")
    void toLocalTime_invalidTypes(String description, Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DoubleConversions.toLocalTime(from, mockConverter), description);
    }

    @Test
    void toLocalTime_Integer_input_throwsClassCastException() {
        Integer from = Integer.valueOf(12_345);
        assertThrows(ClassCastException.class, () -> DoubleConversions.toLocalTime(from, mockConverter), "Integer cannot be cast to double directly");
    }

    @Test
    void toLocalTime_Long_input_throwsClassCastException() {
        Long from = Long.valueOf(43_210L);
        assertThrows(ClassCastException.class, () -> DoubleConversions.toLocalTime(from, mockConverter), "Long cannot be cast to double directly");
    }
}
