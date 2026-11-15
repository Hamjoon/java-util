package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_minimum_2_Test {

    private static Stream<Arguments> provideSingleElementValidCases() {
        return Stream.of(Arguments.of(new BigDecimal[] { new BigDecimal("5.5") }, new BigDecimal("5.5")), Arguments.of(new BigDecimal[] { new BigDecimal("-123.456") }, new BigDecimal("-123.456")), Arguments.of(new BigDecimal[] { new BigDecimal("0") }, new BigDecimal("0")));
    }

    private static Stream<Arguments> provideMultipleElementValidCases() {
        return Stream.of(// Simple positive numbers
        Arguments.of(new BigDecimal[] { new BigDecimal("3.14"), new BigDecimal("2.71"), new BigDecimal("1.618") }, new BigDecimal("1.618")), // Mixed negative and positive numbers
        Arguments.of(new BigDecimal[] { new BigDecimal("-10.0"), new BigDecimal("0"), new BigDecimal("5") }, new BigDecimal("-10.0")), // All equal values
        Arguments.of(new BigDecimal[] { new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0") }, new BigDecimal("0")), // Very large scientific notation numbers
        Arguments.of(new BigDecimal[] { new BigDecimal("1E+1000"), new BigDecimal("9.99E+999") }, new BigDecimal("9.99E+999")), // Very small (negative) numbers
        Arguments.of(new BigDecimal[] { new BigDecimal("-1E-1000"), new BigDecimal("-2E-1000") }, new BigDecimal("-2E-1000")), // High‑precision decimals
        Arguments.of(new BigDecimal[] { new BigDecimal("0.0000000000000000001"), new BigDecimal("0.0000000000000000002") }, new BigDecimal("0.0000000000000000001")), // Mixed zero, positive and negative – negative wins
        Arguments.of(new BigDecimal[] { new BigDecimal("0"), new BigDecimal("5.5"), new BigDecimal("-3.2") }, new BigDecimal("-3.2")));
    }

    private static Stream<Arguments> provideMultipleElementWithNullCases() {
        return Stream.of(Arguments.of(new BigDecimal[] { new BigDecimal("9.99"), null, new BigDecimal("1.23") }), Arguments.of(new BigDecimal[] { null, new BigDecimal("5"), new BigDecimal("-2") }));
    }

    @Test
    void whenSingleNonNullValue_thenReturnedAsIs() {
        BigDecimal value = new BigDecimal("123.456");
        BigDecimal result = MathUtilities.minimum(value);
        assertEquals(value, result, "The single element should be returned unchanged");
    }

    @Test
    void whenMultipleValues_thenSmallestIsReturned() {
        BigDecimal a = new BigDecimal("-10.5");
        BigDecimal b = new BigDecimal("0");
        BigDecimal c = new BigDecimal("7.89");
        BigDecimal d = new BigDecimal("-20.1");
        BigDecimal result = MathUtilities.minimum(a, b, c, d);
        assertEquals(d, result, "The minimum of the supplied values should be returned");
    }

    @Test
    void whenAllPositiveValues_thenFirstIsReturnedIfSmallest() {
        BigDecimal result = MathUtilities.minimum(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0"));
        assertEquals(new BigDecimal("1.0"), result);
    }

    @Test
    void whenAllNegativeValues_thenMostNegativeIsReturned() {
        BigDecimal result = MathUtilities.minimum(new BigDecimal("-1"), new BigDecimal("-5"), new BigDecimal("-3"));
        assertEquals(new BigDecimal("-5"), result);
    }

    @Test
    void whenArrayContainsNull_thenIllegalArgumentExceptionIsThrown() {
        BigDecimal[] values = { new BigDecimal("10"), null, new BigDecimal("5") };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum(values));
        assertTrue(ex.getMessage().contains("null BigDecimal"), "Exception message should indicate a null entry");
    }

    @Test
    void whenFirstArgumentIsNull_thenIllegalArgumentExceptionIsThrown() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum((BigDecimal) null));
        assertTrue(ex.getMessage().contains("null BigDecimal"));
    }

    @Test
    void whenValuesAreVeryLargeOrVerySmall_thenResultIsCorrect() {
        BigDecimal hugePositive = new BigDecimal("1E+1000");
        BigDecimal hugeNegative = new BigDecimal("-1E+1000");
        BigDecimal normal = new BigDecimal("12345.6789");
        // Minimum should be the huge negative number
        assertEquals(hugeNegative, MathUtilities.minimum(hugePositive, normal, hugeNegative));
        // Minimum of two huge positives – the smaller magnitude wins
        assertEquals(normal, MathUtilities.minimum(hugePositive, normal));
    }

    @Test
    void whenValuesContainZero_thenZeroIsChosenIfItIsSmallest() {
        BigDecimal result = MathUtilities.minimum(new BigDecimal("0"), new BigDecimal("5.5"), new BigDecimal("-3.2"));
        assertEquals(new BigDecimal("-3.2"), result);
    }

    @Test
    void whenValuesAreEqual_thenThatValueIsReturned() {
        BigDecimal val = new BigDecimal("42.42");
        BigDecimal result = MathUtilities.minimum(val, val, val);
        assertEquals(val, result);
    }

    @Test
    void whenSingleElementIsNull_thenIllegalArgumentExceptionIsThrown() {
        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MathUtilities.minimum((BigDecimal) null));
        assertTrue(ex.getMessage().contains("null BigDecimal"), "Exception message should indicate a null entry");
    }

    @Test
    void whenEmptyArray_thenArrayIndexOutOfBoundsExceptionIsThrown() {
        // An empty var‑args array translates to a zero‑length array.
        // The implementation accesses values[0] without a guard,
        // therefore an ArrayIndexOutOfBoundsException is expected.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.minimum(new BigDecimal[] {}));
    }
}
