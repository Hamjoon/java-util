package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

class Converter_convert2int_Test {

    static Stream<Arguments> validConversionProvider() {
        return Stream.of(// direct Integer
        Arguments.of("Direct Integer", 42, 42), // numeric strings
        Arguments.of("String \"123\"", "123", 123), // double with whole number part
        Arguments.of("Double 99.0", 99.0, 99), // boolean values
        Arguments.of("Boolean true → 1", true, 1), Arguments.of("Boolean false → 0", false, 0), // double with fractional part (truncation)
        Arguments.of("Double 0.75 → 0", 0.75, 0), // hexadecimal string (positive)
        Arguments.of("Hex string \"0xFF\" → 255", "0xFF", 255), // hexadecimal string (negative)
        Arguments.of("Hex string \"-0x1A\" → -26", "-0x1A", -26), // binary string
        Arguments.of("Binary string \"0b1010\" → 10", "0b1010", 10), // octal string
        Arguments.of("Octal string \"0o77\" → 63", "0o77", 63), // string with leading/trailing whitespace
        Arguments.of("String with spaces \"   7   \" → 7", "   7   ", 7), // string with explicit plus sign
        Arguments.of("String \"+42\" → 42", "+42", 42), // max/min int values
        Arguments.of("Maximum int value", Integer.MAX_VALUE, Integer.MAX_VALUE), Arguments.of("Minimum int value", Integer.MIN_VALUE, Integer.MIN_VALUE), // max/min int as strings
        Arguments.of("String max int", String.valueOf(Integer.MAX_VALUE), Integer.MAX_VALUE), Arguments.of("String min int", String.valueOf(Integer.MIN_VALUE), Integer.MIN_VALUE), // double slightly less than 2 (truncates to 1)
        Arguments.of("Double 1.9999999 → 1", 1.9999999, 1), // negative double with fraction (truncates toward zero)
        Arguments.of("Double -1.9999999 → -1", -1.9999999, -1), // map representation of AtomicInteger (value key)
        Arguments.of("Map representing AtomicInteger {value=123}", mapOf("value", 123), 123), // map representation of Date (epochMillis)
        Arguments.of("Map representing Date epoch 0", mapOf("epochMillis", 0L), 0));
    }

    static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// non‑numeric string
        Arguments.of("Non‑numeric string \"not-a-number\"", "not-a-number", NumberFormatException.class), // numeric string overflow
        Arguments.of("Overflow string \"12345678901234567890\"", "12345678901234567890", ArithmeticException.class), // unsupported plain Map
        Arguments.of("Unsupported Map {}", Collections.singletonMap("a", 1), IllegalArgumentException.class), // long just outside int range (positive)
        Arguments.of("Long 2147483648 (overflow)", 2147483648L, ArithmeticException.class), // long just outside int range (negative)
        Arguments.of("Long -2147483649 (underflow)", -2147483649L, ArithmeticException.class), // class object instead of instance (AtomicInteger.class)
        Arguments.of("Class object AtomicInteger.class", AtomicInteger.class, IllegalArgumentException.class), // unsupported LocalDate map
        Arguments.of("Unsupported LocalDate map", mapOf("year", 1970, "month", 1, "day", 1), IllegalArgumentException.class), // array of numbers
        Arguments.of("Array of numbers [1,2,3]", new int[] { 1, 2, 3 }, IllegalArgumentException.class), // Map with numeric values (unsupported)
        Arguments.of("Map with numeric values", mapOf("a", 1), IllegalArgumentException.class), // StringBuilder instance (unsupported type)
        Arguments.of("StringBuilder instance", new StringBuilder("test"), IllegalArgumentException.class), // Collection of booleans (unsupported)
        Arguments.of("List of booleans", Arrays.asList(true, false), IllegalArgumentException.class), // String "NaN"
        Arguments.of("String \"NaN\"", "NaN", NumberFormatException.class), // String "Infinity"
        Arguments.of("String \"Infinity\"", "Infinity", NumberFormatException.class), // Very large double (overflow)
        Arguments.of("Double 1e+20 (overflow)", 1e20, ArithmeticException.class), // Double exceeding int max but whole
        Arguments.of("Double 10000000000.0 (overflow)", 10000000000.0, ArithmeticException.class), // Invalid hexadecimal string
        Arguments.of("Invalid hex \"0xGHI\"", "0xGHI", NumberFormatException.class), // Invalid binary string
        Arguments.of("Invalid binary \"0b2\"", "0b2", NumberFormatException.class), // Empty string
        Arguments.of("Empty string \"\"", "", NumberFormatException.class), // Whitespace only string
        Arguments.of("Whitespace only string \"   \"", "   ", NumberFormatException.class));
    }

    /* --------------------------------------------------------------------- */
    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    @Test
    @DisplayName("convert2int returns 0 for null input")
    void testNullInput() {
        assertEquals(0, Converter.convert2int(null), "Converter.convert2int(null) should return 0");
    }

    @Test
    @DisplayName("convert2int returns same int value when input is Integer")
    void testIntegerInput() {
        Integer source = 42;
        assertEquals(42, Converter.convert2int(source));
    }

    @Test
    @DisplayName("convert2int converts a numeric String to int")
    void testStringNumber() {
        String source = "12345";
        assertEquals(12345, Converter.convert2int(source));
    }

    @Test
    @DisplayName("convert2int truncates a double value to int")
    void testDoubleInput() {
        Double source = 99.99;
        assertEquals(99, Converter.convert2int(source), "Double should be truncated toward zero");
    }

    @Test
    @DisplayName("convert2int converts Boolean true/false to 1/0")
    void testBooleanInput() {
        assertEquals(1, Converter.convert2int(Boolean.TRUE));
        assertEquals(0, Converter.convert2int(Boolean.FALSE));
    }

    @Test
    @DisplayName("convert2int converts a numeric BigDecimal to int")
    void testBigDecimalInput() {
        assertEquals(7, Converter.convert2int(new java.math.BigDecimal("7.3")), "BigDecimal should be truncated toward zero");
    }

    @Test
    @DisplayName("convert2int converts a numeric BigInteger to int")
    void testBigIntegerInput() {
        assertEquals(123, Converter.convert2int(new java.math.BigInteger("123")));
    }

    @Test
    @DisplayName("convert2int converts a numeric String with leading/trailing spaces")
    void testStringWithSpaces() {
        assertEquals(55, Converter.convert2int("  55  "));
    }

    @Test
    @DisplayName("convert2int throws IllegalArgumentException for non‑numeric String")
    void testInvalidString() {
        String source = "not-a-number";
        assertThrows(RuntimeException.class, () -> Converter.convert2int(source), "Non‑numeric strings should cause a conversion exception");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    @DisplayName("Valid conversions to int")
    void testValidConversions(String description, Object input, int expected) {
        // The description is only used for the test name; the assertion is the real check.
        assertEquals(expected, Converter.convert2int(input), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidConversionProvider")
    @DisplayName("Invalid conversions – expected exceptions")
    void testInvalidConversions(String description, Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convert2int(input), description);
    }
}
