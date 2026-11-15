package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class Converter_convertToInteger_Test {

    static Stream<Arguments> validNumericConversions() {
        return Stream.of(// Integer (identity)
        arguments(42, 42, "Converts an Integer instance to Integer (identity conversion)."), // Long that fits
        arguments(1234567890L, 1234567890, "Converts a Long value that fits within Integer range to Integer."), // Double with fraction
        arguments(3.14, 3, "Converts a Double value by truncating the fractional part."), // Double whole number
        arguments(7.0, 7, "Converts a Double that represents a whole number to Integer."), // Byte value
        arguments((byte) 127, 127, "Converts a Byte value to Integer."), // Short value
        arguments((short) 32767, 32767, "Converts a Short value to Integer."), // Float whole number
        arguments(12345.0f, 12345, "Converts a Float value (whole number) to Integer."), // Float with fraction
        arguments(12345.67f, 12345, "Converts a Float value with fractional part; fraction is truncated."), // Numeric String
        arguments("256", 256, "Converts a numeric String to Integer."), // String with whitespace
        arguments("   42   ", 42, "Converts a String with surrounding whitespace to Integer."), // Boolean true
        arguments(true, 1, "Converts Boolean true to Integer (1)."), // Boolean false
        arguments(false, 0, "Converts Boolean false to Integer (0)."), // Long overflow – follows Java narrowing cast semantics
        arguments(1234567890123L, 1912276187, "Converts a Long that exceeds Integer.MAX_VALUE; conversion follows Java narrowing cast semantics (value modulo 2^32)."), // Double overflow – truncates then narrows
        arguments(1234567890123.0, 1912276187, "Converts a Double that overflows Integer range; result matches Java narrowing cast after truncation."), // Hexadecimal string
        arguments("0x10", 16, "Converts a hexadecimal String (prefixed with 0x) to Integer."), // Binary string
        arguments("0b1010", 10, "Converts a binary String (prefixed with 0b) to Integer."), // Long literal suffix string
        arguments("12345L", 12345, "Converts a String with a trailing 'L' (Long literal) to Integer."), // Decimal string with .0
        arguments("12345.0", 12345, "Converts a numeric String with a decimal point but no fractional part."), // Decimal string with fraction
        arguments("12345.67", 12345, "Converts a numeric String with fractional part; fraction is truncated."), // Negative string with whitespace
        arguments("   -42   ", -42, "Converts a negative numeric String with whitespace to Integer."), // Negative string without whitespace
        arguments("-123", -123, "Converts a negative numeric String to Integer."), // Scientific notation
        arguments("1e3", 1000, "Scientific notation string is accepted and converted to Integer."), // Explicit plus sign with whitespace
        arguments("  +42", 42, "String with explicit plus sign and whitespace should be parsed to Integer."), // Negative zero
        arguments("-0", 0, "String representing negative zero should be parsed to 0."), // Zero
        arguments("0", 0, "String representing zero should be parsed to 0."), // Max int as hex
        arguments(" 0x7fffffff ", 2147483647, "Maximum Integer value expressed as hexadecimal string should be parsed correctly."), // Min int as hex (overflow)
        arguments(" 0x80000000 ", -2147483648, "Hexadecimal string representing Integer.MIN_VALUE (overflow) should wrap to -2147483648."), // Min int as binary
        arguments("0b10000000000000000000000000000000", -2147483648, "Binary string representing Integer.MIN_VALUE should be parsed to -2147483648."));
    }

    static Stream<Arguments> invalidConversions() {
        return Stream.of(arguments(new Object(), ClassCastException.class, "Passing a plain Object that has no conversion rule should result in ClassCastException."), arguments(Arrays.asList(1, 2, 3), ClassCastException.class, "Passing a List should trigger ClassCastException because collection-to-Integer conversion is unsupported."), arguments("123.45.67", NumberFormatException.class, "Malformed numeric string with multiple decimal points should cause NumberFormatException."), arguments("0x", NumberFormatException.class, "Hex prefix without digits should cause NumberFormatException."), arguments("0b", NumberFormatException.class, "Binary prefix without digits should cause NumberFormatException."), arguments("true", NumberFormatException.class, "String \"true\" is not numeric; conversion should fail."), arguments("false", NumberFormatException.class, "String \"false\" is not numeric; conversion should fail."));
    }

    static Stream<Arguments> atomicConversions() {
        return Stream.of(arguments(new AtomicInteger(55), 55, "AtomicInteger should be converted to its int value."), arguments(new AtomicLong(1234567890123L), 1912276187, "AtomicLong that overflows Integer range should follow Java narrowing cast semantics."), arguments(new AtomicBoolean(true), 1, "AtomicBoolean true converts to Integer 1."), arguments(new AtomicBoolean(false), 0, "AtomicBoolean false converts to Integer 0."));
    }

    static Stream<Arguments> charAndBufferConversions() {
        return Stream.of(arguments('A', 65, "Character should be converted to its Unicode code point."), arguments(CharBuffer.wrap("256"), 256, "CharBuffer containing a numeric string is parsed to Integer."), arguments(CharBuffer.wrap("   42   "), 42, "CharBuffer with surrounding whitespace should be trimmed and parsed."), arguments(CharBuffer.wrap("invalid"), NumberFormatException.class, "CharBuffer with non‑numeric content should trigger NumberFormatException."), arguments(ByteBuffer.wrap(new byte[] { '9', '9' }), ClassCastException.class, "ByteBuffer is not a supported source type for Integer conversion."), arguments('5', 53, "Primitive char representing a digit is converted via its Unicode code point."));
    }

    static Stream<Arguments> charAndBufferExceptionConversions() {
        return Stream.of(arguments(CharBuffer.wrap("invalid"), NumberFormatException.class, "CharBuffer with non‑numeric content should trigger NumberFormatException."), arguments(ByteBuffer.wrap(new byte[] { '9', '9' }), ClassCastException.class, "ByteBuffer is not a supported source type for Integer conversion."));
    }

    static Stream<Arguments> dateAndTimeConversions() {
        return Stream.of(arguments(new Date(1234L), 1234, "java.util.Date is converted using its epoch‑millisecond value, narrowed to int."), arguments(new Date(5000000000L), 705032704, "Date epoch value exceeding Integer.MAX_VALUE wraps according to Java narrowing cast."), arguments(Timestamp.from(Instant.ofEpochMilli(9876L)), 9876, "java.sql.Timestamp is treated like java.util.Date; its millisecond value is converted."), arguments(Instant.ofEpochSecond(42), 42, "Instant is converted using its epoch‑second value."));
    }

    static Stream<Arguments> dateAndTimeExceptionConversions() {
        return Stream.of(arguments(LocalDate.of(1970, 1, 2), ClassCastException.class, "LocalDate is not a supported source type for Integer conversion."), arguments(LocalDateTime.of(1970, 1, 1, 0, 0, 42), ClassCastException.class, "LocalDateTime is not a supported source type for Integer conversion."), arguments(ZonedDateTime.ofInstant(Instant.ofEpochSecond(123), ZoneId.of("UTC")), ClassCastException.class, "ZonedDateTime is not a supported source type for Integer conversion."));
    }

    static Stream<Arguments> numericWrapperConversions() {
        return Stream.of(arguments(new BigDecimal("12345.67"), 12345, "BigDecimal is truncated toward zero when converting to Integer."), arguments(new BigDecimal("1e3"), 1000, "BigDecimal with scientific notation is parsed and truncated to Integer."), arguments(new BigInteger("2147483648"), -2147483648, "BigInteger exceeding Integer.MAX_VALUE wraps according to Java narrowing cast."), arguments(new BigInteger("-2147483649"), 2147483647, "Negative overflow of BigInteger wraps to Integer.MAX_VALUE."));
    }

    static Stream<Arguments> otherUnsupportedConversions() {
        return Stream.of(arguments(UUID.randomUUID(), ClassCastException.class, "UUID has no conversion rule to Integer."), arguments(new StringBuilder("123"), null, "StringBuilder implements CharSequence; its content is parsed as an Integer."), arguments(new StringBuilder("abc"), NumberFormatException.class, "Non‑numeric CharSequence should cause NumberFormatException."), arguments(new StringBuilder(""), NumberFormatException.class, "Empty CharSequence should cause NumberFormatException."));
    }

    static Stream<Arguments> additionalNumberFormatScenarios() {
        return Stream.of(arguments("", NumberFormatException.class, "Empty string should cause NumberFormatException."), arguments("+", NumberFormatException.class, "String containing only a plus sign should cause NumberFormatException."), arguments("-", NumberFormatException.class, "String containing only a minus sign should cause NumberFormatException."), arguments(" + ", NumberFormatException.class, "String with whitespace and a solitary plus sign should cause NumberFormatException."), arguments(".5", NumberFormatException.class, "String representing a fractional number without leading digit should cause NumberFormatException."), arguments("١٢٣", NumberFormatException.class, "String with Unicode (Arabic‑Indic) digits should cause NumberFormatException."));
    }

    static Stream<Arguments> overflowStringScenarios() {
        return Stream.of(arguments("12345678901234567890", "NumberFormatException", "String numeric value exceeds Integer range; conversion should throw NumberFormatException."), arguments("1234567890123456789", "-1097262595", "Converts a large numeric String that overflows Integer range; result follows narrowing cast after parsing to BigInteger."), arguments("0xG1", "NumberFormatException", "Invalid hexadecimal String should cause NumberFormatException."), arguments("0b2", "NumberFormatException", "Invalid binary String should cause NumberFormatException."), arguments("123,456", "NumberFormatException", "String with grouping separator not supported; should throw NumberFormatException."), arguments("Infinity", "NumberFormatException", "String representing Infinity is not a valid integer; should throw NumberFormatException."), arguments("NaN", "NumberFormatException", "String representing NaN is not a valid integer; should throw NumberFormatException."));
    }

    @Test
    @DisplayName("convertToInteger(null) should return null")
    void testNullInput() {
        Integer result = Converter.convertToInteger(null);
        assertNull(result, "Expected null when converting a null source");
    }

    @Test
    @DisplayName("convertToInteger(Integer) should return the same Integer")
    void testIntegerInput() {
        Integer source = 42;
        Integer result = Converter.convertToInteger(source);
        assertEquals(source, result, "Integer conversion should be identity");
    }

    @Test
    @DisplayName("convertToInteger(String) with numeric content should parse the number")
    void testStringNumeric() {
        String source = "12345";
        Integer result = Converter.convertToInteger(source);
        assertEquals(12345, result);
    }

    @Test
    @DisplayName("convertToInteger(String) with leading/trailing spaces should be trimmed and parsed")
    void testStringWithSpaces() {
        String source = "  7  ";
        Integer result = Converter.convertToInteger(source);
        assertEquals(7, result);
    }

    @Test
    @DisplayName("convertToInteger(Double) should truncate toward zero")
    void testDoubleInput() {
        Double source = 45.99;
        Integer result = Converter.convertToInteger(source);
        assertEquals(45, result);
    }

    @Test
    @DisplayName("convertToInteger(Boolean) true should become 1")
    void testBooleanTrue() {
        Boolean source = Boolean.TRUE;
        Integer result = Converter.convertToInteger(source);
        assertEquals(1, result);
    }

    @Test
    @DisplayName("convertToInteger(Boolean) false should become 0")
    void testBooleanFalse() {
        Boolean source = Boolean.FALSE;
        Integer result = Converter.convertToInteger(source);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("convertToInteger(BigDecimal) should use intValue")
    void testBigDecimalInput() {
        java.math.BigDecimal source = new java.math.BigDecimal("9876.543");
        Integer result = Converter.convertToInteger(source);
        assertEquals(9876, result);
    }

    @Test
    @DisplayName("convertToInteger(Character) should return the Unicode code point")
    void testCharacterInput() {
        // Unicode 65
        Character source = 'A';
        Integer result = Converter.convertToInteger(source);
        assertEquals(65, result);
    }

    @Test
    @DisplayName("convertToInteger(Object) with unsupported type should throw an exception")
    void testUnsupportedType() {
        // No conversion defined for plain Object → Integer
        Object source = new Object();
        assertThrows(RuntimeException.class, () -> Converter.convertToInteger(source), "Converting an unsupported type should result in a RuntimeException");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validNumericConversions")
    void testValidNumericConversions(Object fromInstance, Integer expected, String description) {
        Integer result = Converter.convertToInteger(fromInstance);
        assertEquals(expected, result, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidConversions")
    void testInvalidConversions(Object fromInstance, Class<? extends Throwable> exceptionClass, String description) {
        assertThrows(exceptionClass, () -> Converter.convertToInteger(fromInstance), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("atomicConversions")
    void testAtomicConversions(Object fromInstance, Integer expected, String description) {
        Integer result = Converter.convertToInteger(fromInstance);
        assertEquals(expected, result, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("charAndBufferConversions")
    void testCharAndBufferConversions(Object fromInstance, Integer expected, String description) {
        Integer result = Converter.convertToInteger(fromInstance);
        assertEquals(expected, result, description);
    }

    // The CharBuffer/ByteBuffer cases that expect exceptions need a separate overload:
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("charAndBufferExceptionConversions")
    void testCharAndBufferExceptionConversions(Object fromInstance, Class<? extends Throwable> exceptionClass, String description) {
        assertThrows(exceptionClass, () -> Converter.convertToInteger(fromInstance), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("dateAndTimeConversions")
    void testDateAndTimeConversions(Object fromInstance, Integer expected, String description) {
        Integer result = Converter.convertToInteger(fromInstance);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("dateAndTimeExceptionConversions")
    void testDateAndTimeExceptionConversions(Object fromInstance, Class<? extends Throwable> exceptionClass, String description) {
        assertThrows(exceptionClass, () -> Converter.convertToInteger(fromInstance), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("numericWrapperConversions")
    void testNumericWrapperConversions(Object fromInstance, Integer expected, String description) {
        Integer result = Converter.convertToInteger(fromInstance);
        assertEquals(expected, result, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("otherUnsupportedConversions")
    void testOtherUnsupportedConversions(Object fromInstance, Class<? extends Throwable> exceptionClass, String description) {
        assertThrows(exceptionClass, () -> Converter.convertToInteger(fromInstance), description);
    }

    // The StringBuilder numeric case returns a value, not an exception:
    @Test
    @DisplayName("StringBuilder with numeric content should be parsed to Integer")
    void testStringBuilderNumeric() {
        StringBuilder sb = new StringBuilder("123");
        Integer result = Converter.convertToInteger(sb);
        assertEquals(123, result, "StringBuilder numeric content should be parsed to Integer.");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @MethodSource("additionalNumberFormatScenarios")
    void testAdditionalNumberFormatScenarios(String input, Class<? extends Throwable> exceptionClass, String description) {
        assertThrows(exceptionClass, () -> Converter.convertToInteger(input), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @MethodSource("overflowStringScenarios")
    void testOverflowStringScenarios(String input, String expected, String description) {
        if ("NumberFormatException".equals(expected)) {
            assertThrows(NumberFormatException.class, () -> Converter.convertToInteger(input), description);
        } else {
            // Expected numeric result after narrowing conversion
            Integer expectedValue = Integer.valueOf(expected);
            Integer result = Converter.convertToInteger(input);
            assertEquals(expectedValue, result, description);
        }
    }
}
