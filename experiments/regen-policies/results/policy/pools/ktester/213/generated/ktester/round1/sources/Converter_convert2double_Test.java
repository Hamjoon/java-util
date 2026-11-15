package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import com.cedarsoftware.util.convert.CommonValues;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class Converter_convert2double_Test {

    /**
     * Provides a mixture of source objects that are known to be convertible
     * to {@code double} by the underlying {@link Converter}.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> numberConversionProvider() {
        return Stream.of(// Integer → double
        arguments(Integer.valueOf(42), 42.0), // Double (already a double) → double (identity)
        arguments(Double.valueOf(3.14), 3.14), // BigDecimal → double (exact value for the test data)
        arguments(new BigDecimal("123.456"), 123.456), // String containing a valid numeric representation → double
        arguments("7.89", 7.89));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> nonNumericStringProvider() {
        return Stream.of(arguments("abc"), arguments(""), arguments("123abc"));
    }

    @Test
    @DisplayName("convert2double returns DOUBLE_ZERO for null input")
    void testNullInputReturnsZero() {
        double result = Converter.convert2double(null);
        assertEquals(com.cedarsoftware.util.convert.CommonValues.DOUBLE_ZERO, result, "Null input should yield CommonValues.DOUBLE_ZERO");
    }

    @Test
    @DisplayName("convert2double correctly converts a numeric String")
    void testStringConversion() {
        double result = Converter.convert2double("123.45");
        assertEquals(123.45, result, 0.000001, "String \"123.45\" should be converted to 123.45");
    }

    @Test
    @DisplayName("convert2double correctly converts an Integer")
    void testIntegerConversion() {
        double result = Converter.convert2double(Integer.valueOf(7));
        assertEquals(7.0, result, "Integer 7 should be converted to 7.0");
    }

    @Test
    @DisplayName("convert2double correctly converts a Long")
    void testLongConversion() {
        double result = Converter.convert2double(Long.valueOf(42L));
        assertEquals(42.0, result, "Long 42 should be converted to 42.0");
    }

    @Test
    @DisplayName("convert2double correctly converts a BigDecimal")
    void testBigDecimalConversion() {
        double result = Converter.convert2double(new java.math.BigDecimal("3.14159"));
        assertEquals(3.14159, result, 0.000001, "BigDecimal should be converted to double with same value");
    }

    @Test
    @DisplayName("convert2double correctly extracts value from a Map")
    void testMapConversion() {
        // According to the documentation, a Map can be converted if it contains a "value" key.
        Map<String, Object> map = new HashMap<>();
        map.put("value", "99.9");
        double result = Converter.convert2double(map);
        assertEquals(99.9, result, 0.000001, "Map with key \"value\" containing \"99.9\" should convert to 99.9");
    }

    @Test
    @DisplayName("convert2double returns 0.0 for non‑numeric, non‑convertible objects")
    void testNonConvertibleObject() {
        // An object that has no conversion path will fall back to the default for primitives: 0.0
        Object unknown = new Object();
        double result = Converter.convert2double(unknown);
        assertEquals(0.0, result, "Unknown object should convert to 0.0 (default for primitive double)");
    }

    @ParameterizedTest(name = "[{index}] convert2double({0}) → {1}")
    @MethodSource("numberConversionProvider")
    @DisplayName("convert2double correctly converts supported numeric sources")
    void testNumberConversions(Object input, double expected) {
        double result = Converter.convert2double(input);
        assertEquals(expected, result, 0.000001, () -> "Conversion of " + input + " should yield " + expected);
    }

    @Test
    @DisplayName("convert2double throws IllegalArgumentException for unsupported source type")
    void testUnsupportedObjectConversion() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2double(unsupported), "An Object with no conversion path should trigger IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] convert2double(\"{0}\") throws NumberFormatException")
    @MethodSource("nonNumericStringProvider")
    @DisplayName("convert2double throws NumberFormatException for malformed numeric strings")
    void testNonNumericStringConversion(String badString) {
        assertThrows(NumberFormatException.class, () -> Converter.convert2double(badString), () -> "String \"" + badString + "\" should cause NumberFormatException");
    }
}
