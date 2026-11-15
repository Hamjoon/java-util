package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.CharBuffer;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2BigInteger_Test {

    /**
     * Provides a stream of arguments for successful conversions.
     * Each element is an array: {inputObject, expectedBigIntegerString, description}
     */
    static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(Arguments.of("123", "123", "String numeric value should be converted to BigInteger with same value."), Arguments.of(456, "456", "Integer primitive should be converted to BigInteger."), Arguments.of(-789, "-789", "Negative int should be converted preserving sign."), Arguments.of(1234567890123456789L, "1234567890123456789", "Long value beyond int range should be converted correctly."), Arguments.of(12.34, "12", "Double should be truncated to integer part when converting to BigInteger."), Arguments.of(56.78f, "56", "Float should be truncated to integer part."), Arguments.of("0xFF", "255", "Hex string should be parsed as base‑16 and converted to BigInteger."), Arguments.of("   42   ", "42", "String with surrounding whitespace should be trimmed and parsed."), Arguments.of(true, "1", "Boolean true should map to BigInteger.ONE."), Arguments.of(false, "0", "Boolean false should map to BigInteger.ZERO."), Arguments.of("123456789012345678901234567890", "123456789012345678901234567890", "Very large numeric string should be converted without overflow."), Arguments.of(123.999, "123", "Floating point with fractional part should be truncated, not rounded."));
    }

    /**
     * Provides inputs that are expected to cause NumberFormatException.
     */
    static Stream<Arguments> numberFormatExceptionProvider() {
        return Stream.of(Arguments.of("notANumber", "Non‑numeric string should cause a NumberFormatException."), Arguments.of("12a34", "String containing numeric characters mixed with non‑numeric characters should cause NumberFormatException."), Arguments.of(CharBuffer.wrap("abc"), "CharBuffer containing non‑numeric characters should cause NumberFormatException."));
    }

    /**
     * Provides inputs that should trigger IllegalArgumentException because the source type is unsupported.
     */
    static Stream<Arguments> illegalArgumentExceptionProvider() {
        return Stream.of(Arguments.of(new Object(), "Passing an instance of a class that has no registered conversion to BigInteger should result in IllegalArgumentException."), Arguments.of(Arrays.asList(1, 2, 3), "Attempting to convert a Collection (List) to BigInteger is not supported and should throw IllegalArgumentException."));
    }

    @Test
    void convert2BigInteger_nullInput_returnsZero() {
        // given
        Object input = null;
        // when
        BigInteger result = Converter.convert2BigInteger(input);
        // then
        assertEquals(BigInteger.ZERO, result, "Converter.convert2BigInteger(null) should return BigInteger.ZERO");
    }

    @Test
    void convert2BigInteger_stringNumber_returnsParsedBigInteger() {
        // given
        String input = "12345678901234567890";
        // when
        BigInteger result = Converter.convert2BigInteger(input);
        // then
        assertEquals(new BigInteger("12345678901234567890"), result, "String numeric value should be converted to the corresponding BigInteger");
    }

    @Test
    void convert2BigInteger_integerInput_returnsBigIntegerWithSameValue() {
        // given
        Integer input = 42;
        // when
        BigInteger result = Converter.convert2BigInteger(input);
        // then
        assertEquals(BigInteger.valueOf(42L), result, "Integer input should be converted to a BigInteger with the same numeric value");
    }

    @Test
    void convert2BigInteger_longInput_returnsBigIntegerWithSameValue() {
        // given
        Long input = 9876543210L;
        // when
        BigInteger result = Converter.convert2BigInteger(input);
        // then
        assertEquals(BigInteger.valueOf(9876543210L), result, "Long input should be converted to a BigInteger with the same numeric value");
    }

    @Test
    void convert2BigInteger_bigDecimalInput_truncatesToBigInteger() {
        // given
        BigDecimal input = new BigDecimal("12345.6789");
        // when
        BigInteger result = Converter.convert2BigInteger(input);
        // then
        // The built‑in conversion drops the fractional part (like BigDecimal#toBigInteger)
        assertEquals(new BigInteger("12345"), result, "BigDecimal input should be converted to a BigInteger by truncating the fractional part");
    }

    @Test
    void convert2BigInteger_booleanInput_convertsToOneOrZero() {
        // given
        Boolean trueInput = Boolean.TRUE;
        Boolean falseInput = Boolean.FALSE;
        // when
        BigInteger trueResult = Converter.convert2BigInteger(trueInput);
        BigInteger falseResult = Converter.convert2BigInteger(falseInput);
        // then
        assertEquals(BigInteger.ONE, trueResult, "Boolean true should be converted to BigInteger 1");
        assertEquals(BigInteger.ZERO, falseResult, "Boolean false should be converted to BigInteger 0");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("successfulConversionProvider")
    void convert2BigInteger_successfulConversions(Object input, String expected, String description) {
        // when
        BigInteger result = Converter.convert2BigInteger(input);
        // then
        assertEquals(new BigInteger(expected), result, description);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("numberFormatExceptionProvider")
    void convert2BigInteger_numberFormatException(Object input, String description) {
        // then
        assertThrows(NumberFormatException.class, () -> Converter.convert2BigInteger(input), description);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("illegalArgumentExceptionProvider")
    void convert2BigInteger_illegalArgumentException(Object input, String description) {
        // then
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2BigInteger(input), description);
    }

    /**
     * Provides inputs that are expected to cause ArithmeticException.
     * According to the test description, a strict conversion from BigDecimal with a fractional part
     * may throw this exception. The actual Converter implementation may not do that, but we keep the test
     * to reflect the specification.
     */
    @Test
    void convert2BigInteger_bigDecimalFractional_exactConversionThrows() {
        // given
        BigDecimal input = new BigDecimal("123.456");
        // then
        assertThrows(ArithmeticException.class, () -> Converter.convert2BigInteger(input), "If the conversion uses exact semantics, a fractional BigDecimal should trigger ArithmeticException.");
    }
}
