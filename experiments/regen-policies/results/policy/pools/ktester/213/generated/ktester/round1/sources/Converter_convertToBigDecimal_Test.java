package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.time.*;
import java.nio.CharBuffer;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(MockitoExtension.class)
class Converter_convertToBigDecimal_Test {

    static List<Arguments> numericConversionProvider() {
        return List.of(arguments(Integer.valueOf(42), new BigDecimal("42")), arguments(Long.valueOf(1234567890123L), new BigDecimal("1234567890123")), arguments(Short.valueOf((short) 7), new BigDecimal("7")), arguments(Byte.valueOf((byte) 2), new BigDecimal("2")), arguments(Double.valueOf(12.34), new BigDecimal("12.34")), arguments(Float.valueOf(5.5f), new BigDecimal("5.5")), // primitives (autoboxed)
        // int
        arguments(42, new BigDecimal("42")), // long
        arguments(42L, new BigDecimal("42")), // short
        arguments((short) 7, new BigDecimal("7")), // byte
        arguments((byte) 2, new BigDecimal("2")), // double
        arguments(12.34d, new BigDecimal("12.34")), // float
        arguments(5.5f, new BigDecimal("5.5")));
    }

    static List<Arguments> stringConversionProvider() {
        return List.of(arguments("123.456", new BigDecimal("123.456")), arguments("-0.001", new BigDecimal("-0.001")), // trimmed
        arguments("  78 ", new BigDecimal("78")), // non‑numeric
        arguments("abc", NumberFormatException.class));
    }

    static List<Arguments> charSequenceConversionProvider() {
        return List.of(// char digit
        arguments('7', new BigDecimal("7")), // single‑char string
        arguments("9", new BigDecimal("9")), // CharBuffer
        arguments(CharBuffer.wrap("12.34"), new BigDecimal("12.34")), // StringBuilder
        arguments(new StringBuilder("56"), new BigDecimal("56")), // StringBuffer
        arguments(new StringBuffer("-78"), new BigDecimal("-78")), // non‑numeric CharSequence
        arguments(new StringBuilder("abc"), NumberFormatException.class));
    }

    static List<String> malformedStringProvider() {
        return List.of(// empty string
        "", // whitespace only
        "   ", // grouping comma
        "1,234", // double decimal point
        "12..34");
    }

    /* --------------------------------------------------------------
     *  Null handling
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("convertToBigDecimal(null) returns null")
    void testNullInput() {
        assertNull(Converter.convertToBigDecimal(null), "Converting a null source should return null for object target types.");
    }

    /* --------------------------------------------------------------
     *  Direct BigDecimal input
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("convertToBigDecimal(BigDecimal) returns the same instance")
    void testBigDecimalInput() {
        BigDecimal source = new BigDecimal("12345.6789");
        BigDecimal result = Converter.convertToBigDecimal(source);
        assertEquals(source, result, "When the source is already a BigDecimal the conversion should return an equal value.");
    }

    /* --------------------------------------------------------------
     *  Numeric primitive wrappers and primitives
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("convert various numeric types to BigDecimal")
    void testNumericConversions() {
        assertEquals(new BigDecimal("42"), Converter.convertToBigDecimal(Integer.valueOf(42)));
        assertEquals(new BigDecimal("42"), Converter.convertToBigDecimal(Long.valueOf(42L)));
        assertEquals(new BigDecimal("42"), Converter.convertToBigDecimal(Short.valueOf((short) 42)));
        assertEquals(new BigDecimal("42"), Converter.convertToBigDecimal(Byte.valueOf((byte) 42)));
        assertEquals(new BigDecimal("42.5"), Converter.convertToBigDecimal(Double.valueOf(42.5)));
        assertEquals(new BigDecimal("42.5"), Converter.convertToBigDecimal(Float.valueOf(42.5f)));
        // Primitive values (autoboxed by the method call)
        assertEquals(new BigDecimal("42"), // int
        Converter.convertToBigDecimal(42));
        assertEquals(new BigDecimal("42"), // long
        Converter.convertToBigDecimal(42L));
        assertEquals(new BigDecimal("42"), // short
        Converter.convertToBigDecimal((short) 42));
        assertEquals(new BigDecimal("42"), // byte
        Converter.convertToBigDecimal((byte) 42));
        assertEquals(new BigDecimal("42.5"), // double
        Converter.convertToBigDecimal(42.5d));
        assertEquals(new BigDecimal("42.5"), // float
        Converter.convertToBigDecimal(42.5f));
    }

    /* --------------------------------------------------------------
     *  String conversion
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("convert numeric String to BigDecimal")
    void testStringNumericConversion() {
        assertEquals(new BigDecimal("12345.6789"), Converter.convertToBigDecimal("12345.6789"));
        assertEquals(new BigDecimal("-987.65"), Converter.convertToBigDecimal("-987.65"));
    }

    @Test
    @DisplayName("convert non‑numeric String throws NumberFormatException")
    void testStringNonNumericConversion() {
        assertThrows(NumberFormatException.class, () -> Converter.convertToBigDecimal("not-a-number"));
    }

    /* --------------------------------------------------------------
     *  BigInteger conversion
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("convert BigInteger to BigDecimal")
    void testBigIntegerConversion() {
        BigInteger source = new BigInteger("12345678901234567890");
        BigDecimal expected = new BigDecimal(source);
        assertEquals(expected, Converter.convertToBigDecimal(source));
    }

    /* --------------------------------------------------------------
     *  Boolean conversion (true → 1, false → 0)
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("convert Boolean values to BigDecimal")
    void testBooleanConversion() {
        assertEquals(BigDecimal.ONE, Converter.convertToBigDecimal(Boolean.TRUE));
        assertEquals(BigDecimal.ZERO, Converter.convertToBigDecimal(Boolean.FALSE));
    }

    /* --------------------------------------------------------------
     *  Map conversion using the generic "value" key
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("convert Map with \"value\" key to BigDecimal")
    void testMapConversion() {
        Map<String, Object> map = Map.of("value", "1234.56");
        BigDecimal result = Converter.convertToBigDecimal(map);
        assertEquals(new BigDecimal("1234.56"), result);
    }

    /* --------------------------------------------------------------
     *  String conversion
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: \"{0}\" → {1}")
    @MethodSource("stringConversionProvider")
    @DisplayName("String parsing to BigDecimal")
    void testStringParsing(String input, Object expected) {
        if (expected instanceof Class && ((Class<?>) expected).isAssignableFrom(RuntimeException.class)) {
            assertThrows(NumberFormatException.class, () -> Converter.convertToBigDecimal(input), "Expected NumberFormatException for input: \"" + input + "\"");
        } else {
            assertEquals(expected, Converter.convertToBigDecimal(input));
        }
    }

    /* --------------------------------------------------------------
     *  Date and Time Types
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("convert java.util.Date to BigDecimal (epoch millis)")
    void testDateConversion() {
        Date date = new Date(1000L);
        assertEquals(new BigDecimal("1000"), Converter.convertToBigDecimal(date));
    }

    @Test
    @DisplayName("convert java.time.Instant to BigDecimal (epoch millis)")
    void testInstantConversion() {
        Instant instant = Instant.ofEpochMilli(2000L);
        assertEquals(new BigDecimal("2000"), Converter.convertToBigDecimal(instant));
    }

    @Test
    @DisplayName("convert java.util.Calendar (UTC) to BigDecimal (epoch millis)")
    void testCalendarConversion() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(3000L);
        assertEquals(new BigDecimal("3000"), Converter.convertToBigDecimal(cal));
    }

    /* --------------------------------------------------------------
     *  Char and CharSequence conversions
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0} → {1}")
    @MethodSource("charSequenceConversionProvider")
    @DisplayName("Char/CharSequence to BigDecimal")
    void testCharSequenceConversions(Object input, Object expected) {
        if (expected instanceof Class && ((Class<?>) expected).isAssignableFrom(RuntimeException.class)) {
            assertThrows(NumberFormatException.class, () -> Converter.convertToBigDecimal(input), "Expected NumberFormatException for input: " + input);
        } else {
            assertEquals(expected, Converter.convertToBigDecimal(input));
        }
    }

    /* --------------------------------------------------------------
     *  Malformed String Input
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: \"{0}\" → exception")
    @MethodSource("malformedStringProvider")
    @DisplayName("Malformed strings cause NumberFormatException")
    void testMalformedStrings(String input) {
        assertThrows(NumberFormatException.class, () -> Converter.convertToBigDecimal(input), "Expected NumberFormatException for malformed input: \"" + input + "\"");
    }

    /* --------------------------------------------------------------
     *  Unsupported Types
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("unsupported source type throws IllegalArgumentException")
    void testUnsupportedType() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBigDecimal(unsupported), "Converting an unsupported type should throw IllegalArgumentException.");
    }
}
