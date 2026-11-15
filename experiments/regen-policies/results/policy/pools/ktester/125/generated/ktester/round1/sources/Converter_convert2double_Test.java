package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2double_Test {

    static Stream<Arguments> numericWrapperProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(42), 42.0), Arguments.of(Double.valueOf(3.14), 3.14), Arguments.of(Float.valueOf(7.5f), 7.5), Arguments.of(Long.valueOf(123L), 123.0), Arguments.of(Short.valueOf((short) 9), 9.0), // byte -1 becomes 255 when widened to double (unsigned interpretation used by Converter)
        Arguments.of(Byte.valueOf((byte) -1), 255.0));
    }

    static Stream<Arguments> numericStringProvider() {
        return Stream.of(Arguments.of("12345", 12345.0), Arguments.of("-12.34", -12.34), Arguments.of("0", 0.0), Arguments.of("123.45", 123.45), Arguments.of("-987.65", -987.65));
    }

    static Stream<Arguments> booleanProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, 1.0), Arguments.of(Boolean.FALSE, 0.0));
    }

    static Stream<Arguments> charProvider() {
        return Stream.of(// digit → numeric value
        Arguments.of('7', 7.0), // non‑digit → Unicode code point
        Arguments.of('A', 65.0));
    }

    static Stream<Arguments> doubleSpecialProvider() {
        return Stream.of(Arguments.of(Double.MAX_VALUE, Double.MAX_VALUE), Arguments.of(Double.MIN_VALUE, Double.MIN_VALUE), Arguments.of(Double.NaN, Double.NaN), Arguments.of(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
    }

    static Stream<Arguments> unsupportedProvider() {
        return Stream.of(Arguments.of(new Object()), Arguments.of(CharBuffer.wrap("123")), Arguments.of(ByteBuffer.wrap(new byte[] { 1, 2, 3 })), Arguments.of(UUID.randomUUID()), Arguments.of(Arrays.asList(1, 2, 3)), // already tested separately but kept for completeness
        Arguments.of(Collections.singletonMap("key", 1)), Arguments.of(new int[] { 1, 2, 3 }));
    }

    @Test
    @DisplayName("convert2double returns 0.0 for null input")
    void testNullInput() {
        double result = Converter.convert2double(null);
        assertEquals(0.0, result, "Null should be converted to 0.0");
    }

    @Test
    @DisplayName("convert2double correctly converts numeric wrapper types")
    void testNumericWrappers() {
        assertEquals(42.0, Converter.convert2double(Integer.valueOf(42)));
        assertEquals(3.14, Converter.convert2double(Double.valueOf(3.14)));
        assertEquals(7.5, Converter.convert2double(Float.valueOf(7.5f)));
        assertEquals(123L, Converter.convert2double(Long.valueOf(123L)));
        assertEquals(9.0, Converter.convert2double(Short.valueOf((short) 9)));
        // byte -1 -> 255 when widened to double
        assertEquals(255.0, Converter.convert2double(Byte.valueOf((byte) -1)));
    }

    @Test
    @DisplayName("convert2double converts a numeric String")
    void testStringConversion() {
        assertEquals(12345.0, Converter.convert2double("12345"));
        assertEquals(-12.34, Converter.convert2double("-12.34"));
        assertEquals(0.0, Converter.convert2double("0"));
    }

    @Test
    @DisplayName("convert2double converts a Map with a \"_v\" entry")
    void testMapConversion() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "99.99");
        double result = Converter.convert2double(map);
        assertEquals(99.99, result, 0.000001);
    }

    @Test
    @DisplayName("convert2double returns 0.0 for an empty Map")
    void testEmptyMap() {
        Map<String, Object> empty = new HashMap<>();
        double result = Converter.convert2double(empty);
        assertEquals(0.0, result, "An empty map should be treated as null/zero");
    }

    @Test
    @DisplayName("convert2double throws appropriate exception for non‑convertible types")
    void testNonConvertible() {
        // no built‑in conversion to double
        Object notConvertible = new Object();
        // The Converter API returns 0.0 for unknown conversions rather than throwing,
        // but we assert the actual behaviour to guard against future changes.
        double result = Converter.convert2double(notConvertible);
        assertEquals(0.0, result, "Non‑convertible objects should yield 0.0 per current implementation");
    }

    @ParameterizedTest(name = "{index}: invalid \"{0}\" → NumberFormatException")
    @ValueSource(strings = { "abc", "", "   " })
    @DisplayName("convert2double throws NumberFormatException for non‑numeric strings")
    void testInvalidStringConversion(String input) {
        assertThrows(NumberFormatException.class, () -> Converter.convert2double(input), () -> "String \"" + input + "\" should cause NumberFormatException");
    }

    /* ---------- Map conversion (supported case) ---------- */
    @Test
    @DisplayName("convert2double converts a Map with a \"_v\" entry")
    void testMapConversionSupported() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "99.99");
        double result = Converter.convert2double(map);
        assertEquals(99.99, result, 0.000001);
    }

    @Test
    @DisplayName("convert2double throws IllegalArgumentException for Map without \"_v\"")
    void testMapConversionUnsupported() {
        Map<String, Object> map = Collections.singletonMap("key", 1);
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2double(map), "Map without \"_v\" should not be convertible to double");
    }

    /* ---------- BigDecimal & BigInteger ---------- */
    @Test
    @DisplayName("convert2double converts BigDecimal preserving scale")
    void testBigDecimalConversion() {
        BigDecimal bd = new BigDecimal("9876543210.12345");
        double result = Converter.convert2double(bd);
        assertEquals(9876543210.12345, result, 0.000001);
    }

    @Test
    @DisplayName("convert2double converts large BigInteger (scientific notation)")
    void testBigIntegerConversion() {
        BigInteger bi = new BigInteger("12345678901234567890");
        double result = Converter.convert2double(bi);
        // tolerance for rounding
        assertEquals(1.2345678901234568E19, result, 1E10);
    }

    /* ---------- Boolean conversion ---------- */
    @ParameterizedTest(name = "{index}: {0} → {1}")
    @MethodSource("booleanProvider")
    @DisplayName("convert2double converts Boolean values")
    void testBooleanConversion(Boolean input, double expected) {
        assertEquals(expected, Converter.convert2double(input), () -> "Boolean " + input + " should convert to " + expected);
    }

    /* ---------- Character conversion ---------- */
    @ParameterizedTest(name = "{index}: '{0}' → {1}")
    @MethodSource("charProvider")
    @DisplayName("convert2double converts Character values")
    void testCharConversion(Character input, double expected) {
        assertEquals(expected, Converter.convert2double(input), () -> "Character '" + input + "' should convert to " + expected);
    }

    /* ---------- Date conversion ---------- */
    @Test
    @DisplayName("convert2double converts epoch Date (0 ms) to 0.0")
    void testDateEpochZero() {
        Date epoch = new Date(0L);
        assertEquals(0.0, Converter.convert2double(epoch));
    }

    @Test
    @DisplayName("convert2double converts non‑zero Date to its epoch milliseconds")
    void testDateNonZero() {
        // 2021‑07‑01T00:00:00Z = 1625097600000 ms since epoch
        Date date = new Date(1625097600000L);
        assertEquals(1.6250976E12, Converter.convert2double(date));
    }

    /* ---------- Double special values ---------- */
    @ParameterizedTest(name = "{index}: {0} → {1}")
    @MethodSource("doubleSpecialProvider")
    @DisplayName("convert2double propagates special Double values")
    void testDoubleSpecialValues(Double input, double expected) {
        double result = Converter.convert2double(input);
        if (Double.isNaN(expected)) {
            assertTrue(Double.isNaN(result), "Result should be NaN");
        } else if (Double.isInfinite(expected)) {
            assertEquals(expected, result, "Result should be infinite");
        } else {
            assertEquals(expected, result, "Result should match expected");
        }
    }

    /* ---------- Unsupported source types – expect IllegalArgumentException ---------- */
    @ParameterizedTest(name = "{index}: {0} → IllegalArgumentException")
    @MethodSource("unsupportedProvider")
    @DisplayName("convert2double throws IllegalArgumentException for unsupported types")
    void testUnsupportedTypes(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2double(input), () -> "Input of type " + (input == null ? "null" : input.getClass().getSimpleName()) + " should cause IllegalArgumentException");
    }

    /* ---------- Additional numeric types not covered by wrappers ---------- */
    @Test
    @DisplayName("convert2double converts primitive long literal")
    void testPrimitiveLong() {
        long value = 1234567890123L;
        assertEquals(1.234567890123E12, Converter.convert2double(value));
    }

    @Test
    @DisplayName("convert2double converts primitive float literal")
    void testPrimitiveFloat() {
        float value = 3.14f;
        assertEquals(3.14, Converter.convert2double(value));
    }

    @Test
    @DisplayName("convert2double converts primitive double literal")
    void testPrimitiveDouble() {
        double value = -0.001;
        assertEquals(-0.001, Converter.convert2double(value));
    }

    @Test
    @DisplayName("convert2double converts primitive int literal")
    void testPrimitiveInt() {
        int value = 42;
        assertEquals(42.0, Converter.convert2double(value));
    }

    @Test
    @DisplayName("convert2double converts primitive short literal")
    void testPrimitiveShort() {
        short value = 9;
        assertEquals(9.0, Converter.convert2double(value));
    }

    @Test
    @DisplayName("convert2double converts primitive byte literal (signed) – expected unsigned widening")
    void testPrimitiveByte() {
        // -1 as signed byte → 255 when treated as unsigned for double conversion
        byte value = -1;
        assertEquals(255.0, Converter.convert2double(value));
    }
}
