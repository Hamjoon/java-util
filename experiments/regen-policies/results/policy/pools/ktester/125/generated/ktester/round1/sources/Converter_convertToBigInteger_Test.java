package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.CharBuffer;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToBigInteger_Test {

    static Stream<Arguments> validNumericConversions() {
        return Stream.of(// Integer
        Arguments.of(123, BigInteger.valueOf(123)), // Long
        Arguments.of(9876543210L, BigInteger.valueOf(9876543210L)), // Double with fractional part – should be truncated
        Arguments.of(12.34, BigInteger.valueOf(12)), // String decimal
        Arguments.of("456789", new BigInteger("456789")), // String with whitespace and sign
        Arguments.of("   -321   ", new BigInteger("-321")), // Boolean true → 1
        Arguments.of(Boolean.TRUE, BigInteger.ONE), // Boolean false → 0
        Arguments.of(Boolean.FALSE, BigInteger.ZERO), // Double that is a whole number
        Arguments.of(12345.0, BigInteger.valueOf(12345)), // Hexadecimal string (lower‑case)
        Arguments.of("0xFF", new BigInteger("255")), // Binary string (lower‑case)
        Arguments.of("0b1010", new BigInteger("10")));
    }

    static Stream<Arguments> additionalNumericConversions() {
        return Stream.of(// Existing BigInteger – should be returned unchanged
        Arguments.of(new BigInteger("12345678901234567890"), new BigInteger("12345678901234567890")), // BigDecimal with fractional part – truncates toward zero
        Arguments.of(new BigDecimal("12345.6789"), BigInteger.valueOf(12345)), // Negative BigDecimal with fractional part
        Arguments.of(new BigDecimal("-12345.9"), BigInteger.valueOf(-12345)), // Primitive byte
        Arguments.of((byte) 7, BigInteger.valueOf(7)), // Primitive short (negative)
        Arguments.of((short) -8, BigInteger.valueOf(-8)), // AtomicInteger
        Arguments.of(new AtomicInteger(42), BigInteger.valueOf(42)), // AtomicLong (negative)
        Arguments.of(new AtomicLong(-99L), BigInteger.valueOf(-99)), // AtomicBoolean true
        Arguments.of(new AtomicBoolean(true), BigInteger.ONE), // AtomicBoolean false
        Arguments.of(new AtomicBoolean(false), BigInteger.ZERO));
    }

    static Stream<Arguments> stringEdgeSuccessful() {
        return Stream.of(// Empty string – according to API doc it should return 0,
        // but the supplied JSON expects NumberFormatException.
        // The implementation of Cedar's Converter treats "" as 0,
        // however we follow the JSON expectation here.
        // This test is therefore omitted; the failure case is covered below.
        // Explicit plus sign
        Arguments.of("+987", BigInteger.valueOf(987)), // Leading zeros
        Arguments.of("0000456", BigInteger.valueOf(456)), // Upper‑case hexadecimal
        Arguments.of("0XFF", new BigInteger("255")), // Upper‑case binary
        Arguments.of("0B1010", new BigInteger("10")));
    }

    static Stream<Arguments> stringEdgeExceptions() {
        return Stream.of(// Empty string – NumberFormatException per JSON spec
        Arguments.of("", NumberFormatException.class), // Non‑numeric string
        Arguments.of("abc", NumberFormatException.class), // Underscore not allowed
        Arguments.of("1_000", NumberFormatException.class));
    }

    static Stream<Arguments> specialFloatingValues() {
        return Stream.of(Arguments.of(Double.NaN, IllegalArgumentException.class), Arguments.of(Double.POSITIVE_INFINITY, IllegalArgumentException.class), Arguments.of(Float.NEGATIVE_INFINITY, IllegalArgumentException.class));
    }

    static Stream<Arguments> malformedStringInputs() {
        return Stream.of(Arguments.of("   "), Arguments.of("+"), Arguments.of("-"), Arguments.of("0xGHI"), Arguments.of("0b102"), Arguments.of("123.45"), Arguments.of("1e3"));
    }

    static Stream<Arguments> unsupportedSourceTypes() {
        return Stream.of(Arguments.of(new ArrayList<>()), Arguments.of(UUID.randomUUID()), Arguments.of(new Date()), Arguments.of(LocalDate.now()), Arguments.of(ZonedDateTime.now()), // CharBuffer – not supported for BigInteger conversion
        Arguments.of(CharBuffer.wrap("123")));
    }

    @Test
    @DisplayName("convert null → null")
    void testConvertNull() {
        // given
        Object source = null;
        // when
        BigInteger result = Converter.convertToBigInteger(source);
        // then
        assertNull(result, "Converting null should yield null for object target types");
    }

    @Test
    @DisplayName("convert String numeric representation → BigInteger")
    void testConvertString() {
        // given
        String source = "12345678901234567890";
        // when
        BigInteger result = Converter.convertToBigInteger(source);
        // then
        assertEquals(new BigInteger("12345678901234567890"), result);
    }

    @Test
    @DisplayName("convert Integer → BigInteger")
    void testConvertInteger() {
        // given
        Integer source = 42;
        // when
        BigInteger result = Converter.convertToBigInteger(source);
        // then
        assertEquals(BigInteger.valueOf(42L), result);
    }

    @Test
    @DisplayName("convert Long → BigInteger")
    void testConvertLong() {
        // given
        // Long.MAX_VALUE
        Long source = 9_223_372_036_854_775_807L;
        // when
        BigInteger result = Converter.convertToBigInteger(source);
        // then
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE), result);
    }

    @Test
    @DisplayName("convert Double (whole number) → BigInteger")
    void testConvertDoubleWholeNumber() {
        // given
        Double source = 12345.0;
        // when
        BigInteger result = Converter.convertToBigInteger(source);
        // then
        assertEquals(BigInteger.valueOf(12_345L), result);
    }

    @Test
    @DisplayName("convert Double (fractional part) → BigInteger (truncates)")
    void testConvertDoubleFractional() {
        // given
        Double source = 12345.987;
        // when
        BigInteger result = Converter.convertToBigInteger(source);
        // then
        // Converter follows Java's narrowing conversion rules – the fractional part is dropped.
        assertEquals(BigInteger.valueOf(12_345L), result);
    }

    @Test
    @DisplayName("convert BigDecimal → BigInteger (exact conversion)")
    void testConvertBigDecimal() {
        // given
        BigDecimal source = new BigDecimal("98765432109876543210");
        // when
        BigInteger result = Converter.convertToBigInteger(source);
        // then
        assertEquals(new BigInteger("98765432109876543210"), result);
    }

    @Test
    @DisplayName("convert Boolean true → BigInteger 1")
    void testConvertBooleanTrue() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        BigInteger result = Converter.convertToBigInteger(source);
        // then
        assertEquals(BigInteger.ONE, result);
    }

    @Test
    @DisplayName("convert Boolean false → BigInteger 0")
    void testConvertBooleanFalse() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        BigInteger result = Converter.convertToBigInteger(source);
        // then
        assertEquals(BigInteger.ZERO, result);
    }

    @Test
    @DisplayName("convert Map with 'value' key → BigInteger")
    void testConvertMapWithValueKey() {
        // given
        Map<String, Object> source = new HashMap<>();
        source.put("value", "55555555555555555555");
        // when
        BigInteger result = Converter.convertToBigInteger(source);
        // then
        assertEquals(new BigInteger("55555555555555555555"), result);
    }

    @Test
    @DisplayName("convert unsupported type → throws IllegalArgumentException")
    void testConvertUnsupportedType() {
        // given
        // no conversion path defined for plain Object → BigInteger
        Object source = new Object();
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBigInteger(source), "Converting an unsupported source type should raise an IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] convert {0} → {1}")
    @MethodSource("validNumericConversions")
    void testValidNumericConversions(Object source, BigInteger expected) {
        // when
        BigInteger result = Converter.convertToBigInteger(source);
        // then
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] convert {0} → {1}")
    @MethodSource("additionalNumericConversions")
    void testAdditionalNumericConversions(Object source, BigInteger expected) {
        BigInteger result = Converter.convertToBigInteger(source);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] convert \"{0}\" → {1}")
    @MethodSource("stringEdgeSuccessful")
    void testStringEdgeSuccessful(String source, BigInteger expected) {
        BigInteger result = Converter.convertToBigInteger(source);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] convert \"{0}\" → throws {1}")
    @MethodSource("stringEdgeExceptions")
    void testStringEdgeExceptions(String source, Class<? extends Exception> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToBigInteger(source));
    }

    @ParameterizedTest(name = "[{index}] convert {0} → throws {1}")
    @MethodSource("specialFloatingValues")
    void testSpecialFloatingValues(Object source, Class<? extends Exception> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToBigInteger(source));
    }

    @ParameterizedTest(name = "[{index}] convert \"{0}\" → throws NumberFormatException")
    @MethodSource("malformedStringInputs")
    void testMalformedStringInputs(String source) {
        assertThrows(NumberFormatException.class, () -> Converter.convertToBigInteger(source));
    }

    @ParameterizedTest(name = "[{index}] convert unsupported type {0} → throws IllegalArgumentException")
    @MethodSource("unsupportedSourceTypes")
    void testUnsupportedSourceTypes(Object source) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBigInteger(source));
    }

    @Test
    @DisplayName("convert Character 'A' → NumberFormatException")
    void testCharacterConversion() {
        assertThrows(NumberFormatException.class, () -> Converter.convertToBigInteger('A'));
    }

    @Test
    @DisplayName("convert unsupported plain Object → IllegalArgumentException")
    void testConvertUnsupportedObject() {
        Object source = new Object();
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBigInteger(source));
    }
}
