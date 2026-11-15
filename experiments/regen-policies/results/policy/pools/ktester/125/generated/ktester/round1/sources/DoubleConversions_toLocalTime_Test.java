package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.stream.Stream;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toLocalTime_Test {

    /**
     * The Converter instance is not used by the method under test, but we provide a mock
     *  to satisfy the signature and to guard against accidental future usage.
     */
    @Mock
    Converter mockConverter;

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// whole‑day boundaries
        Arguments.of(0.0, "00:00"), Arguments.of(43200.0, "12:00"), Arguments.of(86_399.999_999_999, "23:59:59.999999999"), // fractional seconds
        Arguments.of(0.5, "00:00:00.5"), Arguments.of(1.0, "00:00:01"), Arguments.of(1.5, "00:00:01.500000000"), Arguments.of(3600.123456789, "01:00:00.123456789"), Arguments.of(43200.000000001, "12:00:00.000000001"));
    }

    private static Stream<Arguments> outOfRangeProvider() {
        return Stream.of(Arguments.of(-0.000000001), Arguments.of(86_400.0), Arguments.of(1.0e10), Arguments.of(Double.NaN), Arguments.of(Double.POSITIVE_INFINITY), Arguments.of(Double.NEGATIVE_INFINITY), Arguments.of(Double.MAX_VALUE));
    }

    private static Stream<Arguments> typeErrorProvider() {
        return Stream.of(// non‑numeric objects
        Arguments.of("not a number", ClassCastException.class, "String cannot be cast to double"), Arguments.of(new Date(), ClassCastException.class, "Date cannot be cast to double"), // null leads to NPE during unboxing
        Arguments.of(null, NullPointerException.class, "null input should cause NPE"), // non‑Double Number subclasses – casting to primitive double fails
        Arguments.of(new Float(123.45f), ClassCastException.class, "Float cannot be cast to double"), Arguments.of(new Byte((byte) 7), ClassCastException.class, "Byte cannot be cast to double"), Arguments.of(new BigDecimal("123.456"), ClassCastException.class, "BigDecimal cannot be cast to double"));
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
    void toLocalTime_fractionalSeconds_returnsCorrectTime() {
        // given: 1.5 seconds = 00:00:01.500000000
        Double from = 1.5;
        // when
        LocalTime result = DoubleConversions.toLocalTime(from, mockConverter);
        // then
        assertEquals(LocalTime.of(0, 0, 1, 500_000_000), result);
    }

    @Test
    void toLocalTime_maximumValidSeconds_returnsJustBeforeMidnight() {
        // given: 86 399.999 999 999 seconds is the largest value that fits in a day
        Double from = 86_399.999_999_999;
        // when
        LocalTime result = DoubleConversions.toLocalTime(from, mockConverter);
        // then
        // The expected nanosecond‑of‑day is (86_399.999_999_999 * 1_000_000_000) ≈ 86_399_999_999_999 ns
        // which corresponds to 23:59:59.999999999
        assertEquals(LocalTime.of(23, 59, 59, 999_999_999), result);
    }

    @Test
    void toLocalTime_negativeSeconds_throwsIllegalArgumentException() {
        // given
        Double from = -0.1;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toLocalTime(from, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"));
    }

    @Test
    void toLocalTime_exceedsMaximumSeconds_throwsIllegalArgumentException() {
        // given: a value just above the allowed maximum
        // 24 hours exactly
        Double from = 86_400.0;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toLocalTime(from, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"));
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("validConversionProvider")
    void toLocalTime_validConversions(Object from, String expectedString) {
        // when
        LocalTime result = DoubleConversions.toLocalTime(from, mockConverter);
        // then
        LocalTime expected = LocalTime.parse(expectedString);
        assertEquals(expected, result);
        // ensure the converter is never touched
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] from={0} → IllegalArgumentException")
    @MethodSource("outOfRangeProvider")
    void toLocalTime_outOfRangeValues(Object from) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toLocalTime(from, mockConverter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"));
    }

    @ParameterizedTest(name = "[{index}] from={0} ({1}) → {2}")
    @MethodSource("typeErrorProvider")
    void toLocalTime_typeErrors(Object from, Class<?> expectedException, String description) {
        assertThrows(expectedException, () -> DoubleConversions.toLocalTime(from, mockConverter), description);
    }

    @Test
    void toLocalTime_nonNullConverter_isIgnored() {
        // given: a non‑null (real) Converter instance – the implementation is irrelevant
        Converter realConverter = new Converter() {
        };
        // corresponds to 03:25:45.678
        Double from = 12_345.678;
        // when
        LocalTime result = DoubleConversions.toLocalTime(from, realConverter);
        // then
        assertEquals(LocalTime.of(3, 25, 45, 678_000_000), result);
    }
}
