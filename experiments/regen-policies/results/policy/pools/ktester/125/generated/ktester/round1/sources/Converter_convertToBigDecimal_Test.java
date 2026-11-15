package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class Converter_convertToBigDecimal_Test {

    private static Stream<Arguments> numericInputs() {
        return Stream.of(// Byte
        arguments(Byte.valueOf((byte) 5), "5"), // Short (negative)
        arguments(Short.valueOf((short) -3), "-3"), // Integer
        arguments(Integer.valueOf(12345), "12345"), // Long.MAX_VALUE
        arguments(Long.valueOf(9_223_372_036_854_775_807L), "9223372036854775807"), // Double
        arguments(Double.valueOf(1.23), "1.23"), // Float
        arguments(Float.valueOf(1.23f), "1.23"), // BigInteger supplied as numeric string (converted to BigInteger)
        arguments(new BigInteger("12345678901234567890"), "12345678901234567890"), // BigDecimal supplied as string (already a BigDecimal)
        arguments(new BigDecimal("0.0001"), "0.0001"));
    }

    private static Stream<Arguments> stringInputs() {
        return Stream.of(arguments("  42 ", "42"), arguments("+7.89", "7.89"), arguments("-0.001", "-0.001"), arguments("1e3", "1000"), arguments("2.5E-2", "0.025"));
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(// Empty string – expected NumberFormatException per specification
        arguments("", NumberFormatException.class), // Non‑numeric string
        arguments("abc", NumberFormatException.class), // Unsupported generic object
        arguments(new Object(), IllegalArgumentException.class));
    }

    private static Stream<Arguments> specialNumberValues() {
        return Stream.of(arguments(Double.NaN, IllegalArgumentException.class), arguments(Double.POSITIVE_INFINITY, IllegalArgumentException.class), arguments(Double.NEGATIVE_INFINITY, IllegalArgumentException.class));
    }

    private static Stream<Arguments> booleanAndCharInputs() {
        return Stream.of(arguments(Boolean.TRUE, "1"), arguments(Boolean.FALSE, "0"), // char literal promoted to Character
        arguments('A', "65"));
    }

    private static Stream<Arguments> unsupportedTemporalInputs() {
        return Stream.of(arguments(LocalDate.now()), arguments(LocalDateTime.now()), arguments(Instant.now()), arguments(ZonedDateTime.now()), arguments(new Date()));
    }

    private static Stream<Arguments> unsupportedCollectionInputs() {
        return Stream.of(arguments(Collections.emptyList()), arguments(Arrays.asList(1, 2, 3)), arguments(Collections.emptyMap()), arguments(Map.of("a", 1)));
    }

    private static Stream<Arguments> bufferInputs() {
        return Stream.of(arguments(CharBuffer.wrap("abc")), arguments(ByteBuffer.wrap(new byte[] { 'x', 'y', 'z' })));
    }

    private static Stream<Arguments> unsupportedAtomicInputs() {
        return Stream.of(arguments(new AtomicBoolean(true)), arguments(new AtomicInteger(5)));
    }

    @Test
    void convertNullReturnsNull() {
        assertNull(Converter.convertToBigDecimal(null), "Converting a null reference should return null for object target types.");
    }

    @Test
    void convertBigDecimalReturnsSameInstance() {
        BigDecimal source = new BigDecimal("12345.6789");
        BigDecimal result = Converter.convertToBigDecimal(source);
        assertSame(source, result, "When the source is already a BigDecimal the same instance should be returned.");
    }

    @Test
    void convertBigInteger() {
        BigInteger source = new BigInteger("9876543210123456789");
        BigDecimal expected = new BigDecimal(source);
        BigDecimal result = Converter.convertToBigDecimal(source);
        assertEquals(expected, result, "BigInteger should be converted to an equivalent BigDecimal.");
    }

    @Test
    void convertInteger() {
        Integer source = 42;
        BigDecimal expected = new BigDecimal(source);
        BigDecimal result = Converter.convertToBigDecimal(source);
        assertEquals(expected, result);
    }

    @Test
    void convertLong() {
        // Long.MAX_VALUE
        Long source = 9_223_372_036_854_775_807L;
        BigDecimal expected = new BigDecimal(source);
        BigDecimal result = Converter.convertToBigDecimal(source);
        assertEquals(expected, result);
    }

    @Test
    void convertDouble() {
        Double source = 12345.6789;
        // Converter uses string representation
        BigDecimal expected = new BigDecimal(source.toString());
        BigDecimal result = Converter.convertToBigDecimal(source);
        assertEquals(0, expected.compareTo(result), "Double should be converted to a numerically equal BigDecimal.");
    }

    @Test
    void convertFloat() {
        Float source = 3.14f;
        BigDecimal expected = new BigDecimal(source.toString());
        BigDecimal result = Converter.convertToBigDecimal(source);
        assertEquals(0, expected.compareTo(result));
    }

    @Test
    void convertStringNumeric() {
        String source = "98765.4321";
        BigDecimal expected = new BigDecimal(source);
        BigDecimal result = Converter.convertToBigDecimal(source);
        assertEquals(expected, result);
    }

    @Test
    void convertStringWithWhitespace() {
        String source = "   123.45   ";
        BigDecimal expected = new BigDecimal("123.45");
        BigDecimal result = Converter.convertToBigDecimal(source);
        assertEquals(expected, result);
    }

    @Test
    void convertStringInvalidThrowsException() {
        String source = "not-a-number";
        // The Converter throws an IllegalArgumentException for unsupported conversions.
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBigDecimal(source), "Converting a non‑numeric String to BigDecimal should throw IllegalArgumentException.");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: convert {0} -> {1}")
    @MethodSource("numericInputs")
    void convertNumericTypes(Object input, String expectedString) {
        BigDecimal expected = new BigDecimal(expectedString);
        BigDecimal result = Converter.convertToBigDecimal(input);
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result + " for input " + input);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: \"{0}\" -> {1}")
    @MethodSource("stringInputs")
    void convertStringInputs(String input, String expectedString) {
        BigDecimal expected = new BigDecimal(expectedString);
        BigDecimal result = Converter.convertToBigDecimal(input);
        assertEquals(0, expected.compareTo(result), () -> "String \"" + input + "\" should convert to " + expected);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: invalid input {0} expects {1}")
    @MethodSource("invalidInputs")
    void convertInvalidInputs(Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToBigDecimal(input), () -> "Input " + input + " should throw " + expectedException.getSimpleName());
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: special value {0}")
    @MethodSource("specialNumberValues")
    void convertSpecialNumberValues(Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToBigDecimal(input), () -> "Special value " + input + " should raise " + expectedException.getSimpleName());
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0} -> {1}")
    @MethodSource("booleanAndCharInputs")
    void convertBooleanAndCharacter(Object input, String expectedString) {
        BigDecimal expected = new BigDecimal(expectedString);
        BigDecimal result = Converter.convertToBigDecimal(input);
        assertEquals(0, expected.compareTo(result), () -> "Input " + input + " should become " + expected);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: temporal {0}")
    @MethodSource("unsupportedTemporalInputs")
    void convertUnsupportedTemporal(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBigDecimal(input), () -> "Temporal type " + input.getClass().getSimpleName() + " should not be convertible.");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: collection {0}")
    @MethodSource("unsupportedCollectionInputs")
    void convertUnsupportedCollections(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBigDecimal(input), () -> "Collection type " + input.getClass().getSimpleName() + " should not be convertible.");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: buffer {0}")
    @MethodSource("bufferInputs")
    void convertBufferInputs(Object input) {
        assertThrows(NumberFormatException.class, () -> Converter.convertToBigDecimal(input), () -> "Buffer " + input.getClass().getSimpleName() + " containing non‑numeric data should throw NumberFormatException.");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: atomic {0}")
    @MethodSource("unsupportedAtomicInputs")
    void convertUnsupportedAtomics(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBigDecimal(input), () -> "Atomic type " + input.getClass().getSimpleName() + " should not be convertible.");
    }
}
