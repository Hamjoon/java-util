package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToAtomicInteger_Test {

    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// null → null (handled separately)
        Arguments.of(new AtomicInteger(42), 42), Arguments.of(Integer.valueOf(7), 7), Arguments.of(Long.valueOf(123L), 123), // truncates
        Arguments.of(Double.valueOf(3.99), 3), Arguments.of("256", 256), Arguments.of(Boolean.TRUE, 1), Arguments.of(Boolean.FALSE, 0), Arguments.of(new BigInteger("987654321"), 987654321), Arguments.of(new BigDecimal("12345.67"), 12345), Arguments.of(Short.valueOf((short) -123), -123), Arguments.of(Byte.valueOf((byte) 7), 7), Arguments.of(Float.valueOf(9.99f), 9), Arguments.of(new AtomicLong(55L), 55));
    }

    private static Stream<Arguments> edgeNumericProvider() {
        return Stream.of(Arguments.of(Integer.MAX_VALUE, Integer.MAX_VALUE), Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE));
    }

    private static Stream<Arguments> overflowProvider() {
        return Stream.of(// Long.MAX_VALUE does not fit into int
        Arguments.of(Long.MAX_VALUE, ArithmeticException.class), // BigInteger just above Integer.MAX_VALUE
        Arguments.of(new BigInteger("2147483648"), ArithmeticException.class), // BigDecimal just above Integer.MAX_VALUE
        Arguments.of(new BigDecimal("2147483648"), ArithmeticException.class));
    }

    private static Stream<Arguments> stringParsingProvider() {
        return Stream.of(// whitespace trimmed
        Arguments.of("  42  ", 42), // parsed as double then truncated
        Arguments.of("3.14", 3), // plain numeric string
        Arguments.of("123", 123));
    }

    private static Stream<Arguments> stringParsingExceptionProvider() {
        return Stream.of(Arguments.of("abc", NumberFormatException.class), Arguments.of("", NumberFormatException.class));
    }

    private static Stream<Arguments> unsupportedSourceProvider() {
        return Stream.of(Arguments.of(new Object()), Arguments.of(new java.util.Date()));
    }

    @Test
    void convertFromInteger() {
        // given
        Integer source = 42;
        // when
        AtomicInteger result = Converter.convertToAtomicInteger(source);
        // then
        assertNotNull(result, "Result should not be null for a non‑null source");
        assertEquals(42, result.get(), "AtomicInteger should contain the same int value as the source Integer");
    }

    @Test
    void convertFromString() {
        // given
        String source = "123";
        // when
        AtomicInteger result = Converter.convertToAtomicInteger(source);
        // then
        assertNotNull(result);
        assertEquals(123, result.get());
    }

    @Test
    void convertFromLong() {
        // given
        Long source = 7L;
        // when
        AtomicInteger result = Converter.convertToAtomicInteger(source);
        // then
        assertNotNull(result);
        assertEquals(7, result.get());
    }

    @Test
    void convertFromNullReturnsNull() {
        // given
        Object source = null;
        // when
        AtomicInteger result = Converter.convertToAtomicInteger(source);
        // then
        assertNull(result, "convertToAtomicInteger should return null when the source is null");
    }

    @Test
    void convertFromUnsupportedTypeReturnsNull() {
        // given
        // No conversion rule for plain Object to AtomicInteger
        Object source = new Object();
        // when
        AtomicInteger result = Converter.convertToAtomicInteger(source);
        // then
        // The library returns null for unsupported conversions via the convertTo* API
        assertNull(result, "Unsupported source type should result in null");
    }

    @ParameterizedTest(name = "[{index}] convert {0} → AtomicInteger({1})")
    @MethodSource("validConversionsProvider")
    void convertValidValues(Object source, int expected) {
        // when
        AtomicInteger result = Converter.convertToAtomicInteger(source);
        // then
        assertNotNull(result, "Result must not be null for source: " + source);
        assertEquals(expected, result.get(), "AtomicInteger value mismatch for source: " + source);
    }

    @ParameterizedTest(name = "[{index}] convert {0} → expected {1}")
    @MethodSource("edgeNumericProvider")
    void convertEdgeNumeric(Object source, int expected) {
        AtomicInteger result = Converter.convertToAtomicInteger(source);
        assertNotNull(result);
        assertEquals(expected, result.get(), "Edge numeric conversion failed for source: " + source);
    }

    @ParameterizedTest(name = "[{index}] convert {0} → throws {1}")
    @MethodSource("overflowProvider")
    void convertOverflowThrows(Object source, Class<? extends Exception> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToAtomicInteger(source), "Expected exception for source: " + source);
    }

    @ParameterizedTest(name = "[{index}] parse string \"{0}\" → expected {1}")
    @MethodSource("stringParsingProvider")
    void convertStringParsing(Object source, int expected) {
        AtomicInteger result = Converter.convertToAtomicInteger(source);
        assertNotNull(result);
        assertEquals(expected, result.get(), "String parsing failed for source: \"" + source + "\"");
    }

    @ParameterizedTest(name = "[{index}] parse string \"{0}\" → throws {1}")
    @MethodSource("stringParsingExceptionProvider")
    void convertStringParsingThrows(String source, Class<? extends Exception> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToAtomicInteger(source), "Expected exception for source string: \"" + source + "\"");
    }

    @ParameterizedTest(name = "[{index}] convert unsupported {0} → throws IllegalArgumentException")
    @MethodSource("unsupportedSourceProvider")
    void convertUnsupportedThrows(Object source) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToAtomicInteger(source), "Unsupported source type should cause IllegalArgumentException: " + source);
    }
}
