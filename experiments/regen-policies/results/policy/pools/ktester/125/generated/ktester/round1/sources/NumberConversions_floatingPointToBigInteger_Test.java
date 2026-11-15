package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_floatingPointToBigInteger_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter works out‑of‑the‑box for the
        // primitive and common object conversions required by the tests.
        converter = new Converter(new ConverterOptions());
    }

    static Stream<Arguments> positiveNumbers() {
        // No‑op conversion – just use the default instance.
        Converter conv = converter;
        return Stream.of(// 12.7 → floor → 12
        Arguments.of("12.7", conv, BigInteger.valueOf(12)), // 5.0 → floor → 5
        Arguments.of("5.0", conv, BigInteger.valueOf(5)), // 1.9999999999999998 → floor → 1
        Arguments.of("1.9999999999999998", conv, BigInteger.valueOf(1)));
    }

    static Stream<Arguments> zeroAndNegativeNumbers() {
        Converter conv = converter;
        return Stream.of(// 0.0 → ceil → 0
        Arguments.of("0.0", conv, BigInteger.ZERO), // -3.0 → ceil → -3
        Arguments.of("-3.0", conv, BigInteger.valueOf(-3)), // -2.9 → ceil → -2
        Arguments.of("-2.9", conv, BigInteger.valueOf(-2)), // -0.000001 → ceil → 0
        Arguments.of("-0.000001", conv, BigInteger.ZERO));
    }

    static Stream<Arguments> specialFloatingPointValues() {
        Converter conv = converter;
        return Stream.of(Arguments.of(Double.NaN, conv), Arguments.of(Double.POSITIVE_INFINITY, conv), Arguments.of(Double.NEGATIVE_INFINITY, conv));
    }

    static Stream<Arguments> invalidTypeConversions() {
        Converter conv = converter;
        return Stream.of(Arguments.of(Boolean.TRUE, conv), Arguments.of(new Object(), conv));
    }

    static Stream<Arguments> nonNumericStringInputs() {
        Converter conv = converter;
        return Stream.of(Arguments.of("notANumber", conv), Arguments.of("123abc", conv));
    }

    /* --------------------------------------------------------------------- *
     *  Helper that decides whether the expected outcome is a value or an
     *  exception class and performs the appropriate assertion.
     * --------------------------------------------------------------------- */
    private void executeTest(Object from, Converter conv, Object expected) {
        if (expected instanceof Class) {
            // Expected exception path
            assertThrows((Class<? extends Throwable>) expected, () -> NumberConversions.floatingPointToBigInteger(from, conv));
        } else {
            // Expected successful conversion
            BigInteger result = NumberConversions.floatingPointToBigInteger(from, conv);
            assertEquals(expected, result);
        }
    }

    @Test
    void positiveFloatingPointIsFloored() {
        // > 0, should be floored to 123
        Double source = 123.9;
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(123), result);
    }

    @Test
    void negativeFloatingPointIsCeiled() {
        // < 0, should be ceiled to -123
        Double source = -123.9;
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(-123), result);
    }

    @Test
    void integerValueIsReturnedUnchanged() {
        // already an integer
        Integer source = 42;
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(42), result);
    }

    @Test
    void stringRepresentationIsConverted() {
        // will be parsed to double 56.78, then floored to 56
        String source = "56.78";
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(56), result);
    }

    @Test
    void bigDecimalIsHandledCorrectly() {
        // negative, should be ceiled to -99
        BigDecimal source = new BigDecimal("-99.3");
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(-99), result);
    }

    @Test
    void nullInputYieldsZero() {
        // Converter returns 0.0 for a null source when converting to double.
        BigInteger result = NumberConversions.floatingPointToBigInteger(null, converter);
        assertEquals(BigInteger.ZERO, result);
    }

    /* --------------------------------------------------------------------- *
     *  Positive numbers – floor is applied
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → expected={2}")
    @MethodSource("positiveNumbers")
    void positiveNumbers(Object from, Converter conv, Object expected) {
        executeTest(from, conv, expected);
    }

    /* --------------------------------------------------------------------- *
     *  Zero and negative numbers – ceil is applied
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → expected={2}")
    @MethodSource("zeroAndNegativeNumbers")
    void zeroAndNegativeNumbers(Object from, Converter conv, Object expected) {
        executeTest(from, conv, expected);
    }

    /* --------------------------------------------------------------------- *
     *  Special floating‑point values – formatting produces a non‑numeric
     *  string, therefore a NumberFormatException is expected.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → throws NumberFormatException")
    @MethodSource("specialFloatingPointValues")
    void specialFloatingPointValues(Object from, Converter conv) {
        assertThrows(NumberFormatException.class, () -> NumberConversions.floatingPointToBigInteger(from, conv));
    }

    /* --------------------------------------------------------------------- *
     *  Large magnitude values – ensure the method works beyond long range.
     * --------------------------------------------------------------------- */
    @Test
    void largeMagnitudeValue() {
        // > Long.MAX_VALUE but still a valid double
        Double source = 9.223372036854776E18;
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        assertEquals(new BigInteger("9223372036854775808"), result);
    }

    /* --------------------------------------------------------------------- *
     *  Overflow to Infinity – the formatted string cannot be parsed as a
     *  BigInteger, so a NumberFormatException is expected.
     * --------------------------------------------------------------------- */
    @Test
    void overflowToInfinityLeadsToParsingFailure() {
        Double source = Double.MAX_VALUE;
        assertThrows(NumberFormatException.class, () -> NumberConversions.floatingPointToBigInteger(source, converter));
    }

    /* --------------------------------------------------------------------- *
     *  Null arguments – both the source object and the Converter may be null.
     * --------------------------------------------------------------------- */
    @Test
    void nullSourceThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.floatingPointToBigInteger(null, converter));
    }

    @Test
    void nullConverterThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.floatingPointToBigInteger("123.45", null));
    }

    /* --------------------------------------------------------------------- *
     *  Invalid type conversion – Boolean and generic Object are not convertible
     *  to a double, therefore IllegalArgumentException is expected.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} (type {0.getClass()}) → throws IllegalArgumentException")
    @MethodSource("invalidTypeConversions")
    void invalidTypeConversions(Object from, Converter conv) {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.floatingPointToBigInteger(from, conv));
    }

    /* --------------------------------------------------------------------- *
     *  Non‑numeric String input – parsing fails and a NumberFormatException is
     *  thrown.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from=\"{0}\" → throws NumberFormatException")
    @MethodSource("nonNumericStringInputs")
    void nonNumericStringInputs(String from, Converter conv) {
        assertThrows(NumberFormatException.class, () -> NumberConversions.floatingPointToBigInteger(from, conv));
    }
}
