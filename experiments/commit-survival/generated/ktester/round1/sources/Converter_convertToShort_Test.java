package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToShort_Test {

    static Stream<Arguments> validConversionProvider() {
        return Stream.of(// identity
        Arguments.of(Short.valueOf((short) 123), Short.valueOf((short) 123), "Short instance should return the same Short value."), // numeric widening/narrowing
        Arguments.of(Integer.valueOf(123), Short.valueOf((short) 123), "Integer within Short range is converted by narrowing cast."), Arguments.of(Long.valueOf(123L), Short.valueOf((short) 123), "Long within Short range is converted by narrowing cast."), Arguments.of(Double.valueOf(123.45), Short.valueOf((short) 123), "Double is truncated toward zero and converted to Short."), // BigDecimal / BigInteger
        Arguments.of(new BigDecimal("123"), Short.valueOf((short) 123), "BigDecimal with integral value is converted to Short."), Arguments.of(new BigInteger("789"), Short.valueOf((short) 789), "BigInteger is converted to Short via narrowing cast."), // String parsing
        Arguments.of("123", Short.valueOf((short) 123), "String containing a valid decimal number is parsed to Short."), Arguments.of("-32768", Short.valueOf(Short.MIN_VALUE), "String representing Short.MIN_VALUE is correctly parsed."), Arguments.of("32767", Short.valueOf(Short.MAX_VALUE), "String representing Short.MAX_VALUE is correctly parsed."), // Atomic wrapper
        Arguments.of(AtomicInteger.valueOf(5), Short.valueOf((short) 5), "AtomicInteger is unwrapped and its int value is converted to Short."), // Boolean values
        Arguments.of(Boolean.TRUE, Short.valueOf((short) 1), "Boolean true is converted to numeric 1 and then to Short."), Arguments.of(Boolean.FALSE, Short.valueOf((short) 0), "Boolean false is converted to numeric 0 and then to Short."), // Character conversion (Unicode code point)
        Arguments.of(Character.valueOf('A'), Short.valueOf((short) 65), "Character is converted to its Unicode code point (65) and then to Short."), // Null handling
        Arguments.of(null, null, "Null input should result in a null Short return value."));
    }

    static Stream<Arguments> overflowConversionProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(Integer.MAX_VALUE), Short.valueOf((short) -1), "Integer.MAX_VALUE exceeds Short.MAX_VALUE; conversion uses Java narrowing cast, resulting in -1."), Arguments.of(Integer.valueOf(Integer.MIN_VALUE), Short.valueOf((short) 0), "Integer.MIN_VALUE is less than Short.MIN_VALUE; narrowing cast yields 0."), Arguments.of(Long.valueOf(Long.MAX_VALUE), Short.valueOf((short) -1), "Long.MAX_VALUE overflows Short range; conversion truncates high bits, producing -1."), Arguments.of(Long.valueOf(Long.MIN_VALUE), Short.valueOf((short) 0), "Long.MIN_VALUE underflows Short range; conversion results in 0 after narrowing cast."));
    }

    static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("abc", "Non‑numeric string should cause NumberFormatException."), Arguments.of("123.45", "String containing a decimal point cannot be parsed as a short integer."), Arguments.of("", "Empty string cannot be parsed to a number."));
    }

    static Stream<Arguments> unsupportedSourceProvider() {
        return Stream.of(Arguments.of(new Object(), "Arbitrary Object is not convertible to Short; should throw IllegalArgumentException."), Arguments.of(java.util.Date.from(java.time.Instant.now()), "java.util.Date does not have a defined conversion to Short; should throw IllegalArgumentException."), Arguments.of(java.time.LocalDate.now(), "LocalDate cannot be converted to Short; should throw IllegalArgumentException."));
    }

    static Stream<Arguments> nonIntegralNumericProvider() {
        return Stream.of(Arguments.of(new BigDecimal("123.99"), "BigDecimal with a fractional part cannot be exactly represented as a short; expect ArithmeticException."), Arguments.of(Float.valueOf(12.34f), "Float with fractional part should cause an ArithmeticException when converting to Short."), Arguments.of(Double.valueOf(56.78), "Double with fractional part should cause an ArithmeticException when converting to Short."));
    }

    @Test
    @DisplayName("Convert Short to Short (identity)")
    void testConvert_ShortIdentity() {
        Short source = 123;
        Short result = Converter.convertToShort(source);
        assertEquals(source, result);
    }

    @Test
    @DisplayName("Convert Integer to Short")
    void testConvert_Integer() {
        Integer source = 321;
        Short expected = source.shortValue();
        Short result = Converter.convertToShort(source);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Convert Long to Short")
    void testConvert_Long() {
        Long source = 400L;
        Short expected = source.shortValue();
        Short result = Converter.convertToShort(source);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Convert Double to Short (truncates fractional part)")
    void testConvert_Double() {
        Double source = 12.99;
        // truncates toward zero
        Short expected = source.shortValue();
        Short result = Converter.convertToShort(source);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Convert String numeric to Short")
    void testConvert_StringNumeric() {
        String source = "256";
        Short expected = Short.valueOf(source);
        Short result = Converter.convertToShort(source);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Convert BigDecimal to Short")
    void testConvert_BigDecimal() {
        BigDecimal source = new BigDecimal("1234.56");
        Short expected = source.shortValue();
        Short result = Converter.convertToShort(source);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Convert BigInteger to Short")
    void testConvert_BigInteger() {
        BigInteger source = new BigInteger("789");
        Short expected = source.shortValue();
        Short result = Converter.convertToShort(source);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Convert Boolean true to Short (1)")
    void testConvert_BooleanTrue() {
        Boolean source = Boolean.TRUE;
        Short expected = 1;
        Short result = Converter.convertToShort(source);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Convert Boolean false to Short (0)")
    void testConvert_BooleanFalse() {
        Boolean source = Boolean.FALSE;
        Short expected = 0;
        Short result = Converter.convertToShort(source);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Convert Character digit to Short")
    void testConvert_CharacterDigit() {
        Character source = '7';
        Short expected = Short.valueOf((short) (source - '0'));
        Short result = Converter.convertToShort(source);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Convert null returns null")
    void testConvert_Null() {
        assertNull(Converter.convertToShort(null));
    }

    @Test
    @DisplayName("Convert non‑numeric String throws IllegalArgumentException")
    void testConvert_NonNumericString() {
        String source = "not-a-number";
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToShort(source));
    }

    @Test
    @DisplayName("Convert unsupported type throws IllegalArgumentException")
    void testConvert_UnsupportedType() {
        // no conversion defined for plain Object
        Object source = new Object();
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToShort(source));
    }

    @Test
    @DisplayName("Convert numeric value out of Short range truncates according to library rules")
    void testConvert_OutOfRange() {
        // The library follows Java's narrowing conversion semantics: high-order bits are discarded.
        // exceeds Short.MAX_VALUE (32767)
        Integer source = 70000;
        // expected truncation
        Short expected = source.shortValue();
        Short result = Converter.convertToShort(source);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void testConvertToShort_Valid(Object source, Short expected, String description) {
        Short result = Converter.convertToShort(source);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("overflowConversionProvider")
    void testConvertToShort_OverflowUnderflow(Object source, Short expected, String description) {
        Short result = Converter.convertToShort(source);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("invalidStringProvider")
    void testConvertToShort_InvalidString(String source, String description) {
        assertThrows(NumberFormatException.class, () -> Converter.convertToShort(source), description);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("unsupportedSourceProvider")
    void testConvertToShort_UnsupportedSource(Object source, String description) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToShort(source), description);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("nonIntegralNumericProvider")
    void testConvertToShort_NonIntegralNumeric(Object source, String description) {
        assertThrows(ArithmeticException.class, () -> Converter.convertToShort(source), description);
    }
}
