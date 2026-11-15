package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigInteger;
import java.util.Date;
import java.util.stream.Stream;

class NumberConversions_floatingPointToBigDecimal_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance; using the default options is sufficient for these tests
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validFloatingPointProvider() {
        return Stream.of(// Double instance
        Arguments.of(123.45, BigDecimal.valueOf(123.45)), // Float (auto‑boxed)
        Arguments.of(12.3f, BigDecimal.valueOf(12.3)), // Numeric String
        Arguments.of("456.78", BigDecimal.valueOf(456.78)), // Existing BigDecimal
        Arguments.of(new BigDecimal("987.65"), new BigDecimal("987.65")), // Null source → zero
        Arguments.of(null, BigDecimal.ZERO));
    }

    private static Stream<Arguments> integerInputProvider() {
        return Stream.of(// Integer
        Arguments.of(42, new BigDecimal("42")), // Long
        Arguments.of(1234567890123L, new BigDecimal("1234567890123")), // Short (negative)
        Arguments.of((short) -7, new BigDecimal("-7")), // Byte zero
        Arguments.of((byte) 0, BigDecimal.ZERO));
    }

    private static Stream<Arguments> scientificNotationProvider() {
        return Stream.of(// lower‑case exponent
        Arguments.of("1e-6", new BigDecimal("0.000001")), // spaces + upper‑case exponent
        Arguments.of(" -2.5E3 ", new BigDecimal("-2500")));
    }

    private static Stream<Arguments> specialDoubleProvider() {
        return Stream.of(Arguments.of(Double.NaN), Arguments.of(Double.POSITIVE_INFINITY), Arguments.of(Double.NEGATIVE_INFINITY));
    }

    private static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("abc"), Arguments.of(""));
    }

    private static Stream<Arguments> largeNumericProvider() {
        // Double.MAX_VALUE
        BigDecimal maxDouble = BigDecimal.valueOf(Double.MAX_VALUE);
        // Very large BigInteger – conversion via double loses precision
        BigInteger hugeInt = new BigInteger("123456789012345678901234567890");
        BigDecimal hugeIntApprox = BigDecimal.valueOf(hugeInt.doubleValue());
        return Stream.of(Arguments.of(Double.MAX_VALUE, maxDouble), Arguments.of(hugeInt, hugeIntApprox));
    }

    private static Stream<Arguments> unsupportedTypeProvider() {
        return Stream.of(// Boolean
        Arguments.of(Boolean.TRUE), // Character
        Arguments.of('A'), // Date
        Arguments.of(new Date()));
    }

    private static Stream<Arguments> overflowProvider() {
        // 1 followed by 400 zeros (far beyond Double.MAX_VALUE)
        String hugeNumber = "1" + "0".repeat(400);
        BigInteger positiveOverflow = new BigInteger(hugeNumber);
        BigInteger negativeOverflow = positiveOverflow.negate();
        return Stream.of(Arguments.of(positiveOverflow), Arguments.of(negativeOverflow));
    }

    @Test
    void shouldConvertDoubleToBigDecimal() {
        Double source = 123.45;
        BigDecimal expected = BigDecimal.valueOf(123.45);
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(source, converter);
        assertEquals(expected, actual, "Double value should be converted to equivalent BigDecimal");
    }

    @Test
    void shouldConvertFloatToBigDecimal() {
        Float source = 12.3f;
        // Float is first widened to double inside the conversion, so we compare with the double value
        BigDecimal expected = BigDecimal.valueOf(12.3);
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(source, converter);
        assertEquals(expected, actual, "Float value should be converted to equivalent BigDecimal");
    }

    @Test
    void shouldConvertStringNumberToBigDecimal() {
        String source = "78.9";
        BigDecimal expected = BigDecimal.valueOf(78.9);
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(source, converter);
        assertEquals(expected, actual, "String representing a number should be converted to equivalent BigDecimal");
    }

    @Test
    void shouldConvertIntegerToBigDecimal() {
        Integer source = 5;
        BigDecimal expected = BigDecimal.valueOf(5);
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(source, converter);
        assertEquals(expected, actual, "Integer value should be converted to equivalent BigDecimal");
    }

    @Test
    void shouldReturnZeroWhenSourceIsNull() {
        Object source = null;
        // Converter returns 0.0 for null numeric conversion
        BigDecimal expected = BigDecimal.ZERO;
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(source, converter);
        assertEquals(expected, actual, "Null source should be converted to BigDecimal.ZERO");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("validFloatingPointProvider")
    void shouldConvertValidFloatingPoint(Object input, BigDecimal expected) {
        // The method under test should return a BigDecimal equal to the expected value.
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(input, converter);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Integer input {0} → {1}")
    @MethodSource("integerInputProvider")
    void shouldConvertIntegerTypes(Object input, BigDecimal expected) {
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(input, converter);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] \"{0}\" → {1}")
    @MethodSource("scientificNotationProvider")
    void shouldParseScientificNotationStrings(String input, BigDecimal expected) {
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(input, converter);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0} should throw NumberFormatException")
    @MethodSource("specialDoubleProvider")
    void shouldRejectSpecialDoubleValues(Object input) {
        assertThrows(NumberFormatException.class, () -> NumberConversions.floatingPointToBigDecimal(input, converter));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] \"{0}\" should throw NumberFormatException")
    @MethodSource("invalidStringProvider")
    void shouldRejectInvalidStrings(String input) {
        assertThrows(NumberFormatException.class, () -> NumberConversions.floatingPointToBigDecimal(input, converter));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Large value {0}")
    @MethodSource("largeNumericProvider")
    void shouldHandleLargeNumericValues(Object input, BigDecimal expected) {
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(input, converter);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @Test
    void shouldNormalizeNegativeZeroToZero() {
        double input = -0.0;
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(input, converter);
        assertEquals(BigDecimal.ZERO, actual, "Negative zero should be normalized to a BigDecimal representing 0");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0} ({1}) should throw NumberFormatException")
    @MethodSource("unsupportedTypeProvider")
    void shouldRejectUnsupportedTypes(Object input) {
        assertThrows(NumberFormatException.class, () -> NumberConversions.floatingPointToBigDecimal(input, converter));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Overflowing BigInteger should throw NumberFormatException")
    @MethodSource("overflowProvider")
    void shouldThrowWhenBigIntegerOverflowsToInfinity(BigInteger input) {
        assertThrows(NumberFormatException.class, () -> NumberConversions.floatingPointToBigDecimal(input, converter));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldThrowWhenConverterIsNull() {
        Double source = 12.34;
        assertThrows(NullPointerException.class, () -> NumberConversions.floatingPointToBigDecimal(source, null));
    }

    // -----------------------------------------------------------------
    @Test
    void shouldRejectWhitespaceOnlyString() {
        String whitespace = "   \t  \n  ";
        assertThrows(NumberFormatException.class, () -> NumberConversions.floatingPointToBigDecimal(whitespace, converter));
    }
}
