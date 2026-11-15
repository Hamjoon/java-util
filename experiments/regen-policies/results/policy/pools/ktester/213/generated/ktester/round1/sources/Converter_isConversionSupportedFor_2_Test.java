package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class Converter_isConversionSupportedFor_2_Test {

    /* -------------------------------------------------------------
     *  Helper to convert a textual class name (as appears in the JSON)
     *  into a real {@link Class} instance.
     * ------------------------------------------------------------- */
    private static Class<?> resolveClass(String name) {
        if (name == null) {
            return null;
        }
        switch(name) {
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "boolean":
                return boolean.class;
            case "void":
                return void.class;
            default:
                // Handle array notation like "byte[]"
                if (name.endsWith("[]")) {
                    String component = name.substring(0, name.length() - 2);
                    return java.lang.reflect.Array.newInstance(resolveClass(component), 0).getClass();
                }
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Unable to resolve class name: " + name, e);
                }
        }
    }

    private static Stream<Arguments> supportedConversions() {
        return Stream.of(// String → Integer
        Arguments.of(resolveClass("java.lang.String"), resolveClass("java.lang.Integer"), true, "String can be converted to Integer (e.g., \"123\" → 123)."), // Integer → String
        Arguments.of(resolveClass("java.lang.Integer"), resolveClass("java.lang.String"), true, "Integer can be converted to String."), // LocalDate → LocalDateTime
        Arguments.of(resolveClass("java.time.LocalDate"), resolveClass("java.time.LocalDateTime"), true, "LocalDate can be converted to LocalDateTime (midnight time)."), // Date → Timestamp
        Arguments.of(resolveClass("java.util.Date"), resolveClass("java.sql.Timestamp"), true, "java.util.Date is supported for conversion to java.sql.Timestamp."), // Boolean → AtomicBoolean
        Arguments.of(resolveClass("java.lang.Boolean"), resolveClass("java.util.concurrent.atomic.AtomicBoolean"), true, "Boolean can be converted to AtomicBoolean."), // Same type (String → String)
        Arguments.of(resolveClass("java.lang.String"), resolveClass("java.lang.String"), true, "Conversion from a type to the same type is always supported."));
    }

    private static Stream<Arguments> unsupportedConversions() {
        return Stream.of(// String → LocalDate (no format parser)
        Arguments.of(resolveClass("java.lang.String"), resolveClass("java.time.LocalDate"), false, "String cannot be directly converted to LocalDate without a format parser."), // Date → LocalDate (unsupported)
        Arguments.of(resolveClass("java.util.Date"), resolveClass("java.time.LocalDate"), false, "java.util.Date is not supported for conversion to java.time.LocalDate."), // List → Set (not part of built‑in map according to JSON)
        Arguments.of(resolveClass("java.util.List"), resolveClass("java.util.Set"), false, "Collection type conversions (List → Set) are not part of the built‑in conversion map."), // Map → String (no default string representation)
        Arguments.of(resolveClass("java.util.Map"), resolveClass("java.lang.String"), false, "Map cannot be converted to a String representation by default."), // ByteBuffer → byte[] (unsupported out‑of‑the‑box)
        Arguments.of(resolveClass("java.nio.ByteBuffer"), resolveClass("byte[]"), false, "ByteBuffer to byte[] conversion is not supported out‑of‑the‑box."), // Integer → ZonedDateTime (numeric to temporal unsupported)
        Arguments.of(resolveClass("java.lang.Integer"), resolveClass("java.time.ZonedDateTime"), false, "Numeric types cannot be converted to temporal types without explicit mapping."));
    }

    private static Stream<Arguments> nullParameterConversions() {
        return Stream.of(Arguments.of(null, resolveClass("java.lang.String"), false, "Null source class should result in unsupported conversion."), Arguments.of(resolveClass("java.lang.String"), null, false, "Null target class should result in unsupported conversion."), Arguments.of(null, null, false, "Both source and target being null must be unsupported."));
    }

    private static Stream<Arguments> primitiveWrapperConversions() {
        return Stream.of(// int → Integer
        Arguments.of(resolveClass("int"), resolveClass("java.lang.Integer"), true, "Primitive int can be converted to its wrapper Integer."), // Integer → int
        Arguments.of(resolveClass("java.lang.Integer"), resolveClass("int"), true, "Wrapper Integer can be converted to primitive int."), // int → String
        Arguments.of(resolveClass("int"), resolveClass("java.lang.String"), true, "Primitive int can be converted to String representation."), // boolean → Boolean
        Arguments.of(resolveClass("boolean"), resolveClass("java.lang.Boolean"), true, "Primitive boolean to Boolean wrapper conversion is supported."), // Boolean → boolean
        Arguments.of(resolveClass("java.lang.Boolean"), resolveClass("boolean"), true, "Boolean wrapper to primitive boolean conversion is supported."), // int → long (widening primitive)
        Arguments.of(resolveClass("int"), resolveClass("long"), true, "Widening primitive conversion from int to long is supported."), // int → LocalDate (unsupported)
        Arguments.of(resolveClass("int"), resolveClass("java.time.LocalDate"), false, "Primitive numeric types cannot be converted to unrelated temporal types."));
    }

    private static Stream<Arguments> additionalSupportedConversions() {
        return Stream.of(// Boolean → String
        Arguments.of(resolveClass("java.lang.Boolean"), resolveClass("java.lang.String"), true, "Boolean can be converted to its textual representation \"true\"/\"false\"."), // String → Boolean
        Arguments.of(resolveClass("java.lang.String"), resolveClass("java.lang.Boolean"), true, "String values \"true\"/\"false\" can be parsed into Boolean."), // String → UUID
        Arguments.of(resolveClass("java.lang.String"), resolveClass("java.util.UUID"), true, "String containing a UUID literal can be converted to java.util.UUID."), // UUID → String
        Arguments.of(resolveClass("java.util.UUID"), resolveClass("java.lang.String"), true, "UUID can be converted to its canonical String form."), // Integer → AtomicInteger
        Arguments.of(resolveClass("java.lang.Integer"), resolveClass("java.util.concurrent.atomic.AtomicInteger"), true, "Integer can be wrapped into AtomicInteger."), // AtomicInteger → Integer
        Arguments.of(resolveClass("java.util.concurrent.atomic.AtomicInteger"), resolveClass("java.lang.Integer"), true, "AtomicInteger can be unwrapped to Integer."), // String → BigDecimal
        Arguments.of(resolveClass("java.lang.String"), resolveClass("java.math.BigDecimal"), true, "Numeric String can be parsed into BigDecimal."), // BigDecimal → String
        Arguments.of(resolveClass("java.math.BigDecimal"), resolveClass("java.lang.String"), true, "BigDecimal can be converted to its plain String representation."), // LocalDateTime → LocalDate
        Arguments.of(resolveClass("java.time.LocalDateTime"), resolveClass("java.time.LocalDate"), true, "LocalDateTime can be truncated to LocalDate (date part only)."), // ZonedDateTime → LocalDateTime
        Arguments.of(resolveClass("java.time.ZonedDateTime"), resolveClass("java.time.LocalDateTime"), true, "ZonedDateTime can be converted to LocalDateTime by dropping zone information."), // Timestamp → Date
        Arguments.of(resolveClass("java.sql.Timestamp"), resolveClass("java.util.Date"), true, "Timestamp is a subclass of java.util.Date, conversion is supported."), // CharBuffer → String
        Arguments.of(resolveClass("java.nio.CharBuffer"), resolveClass("java.lang.String"), true, "CharBuffer can be converted to a String containing its characters."));
    }

    private static Stream<Arguments> sameTypePrimitiveConversions() {
        return Stream.of(// int → int
        Arguments.of(resolveClass("int"), resolveClass("int"), true, "Conversion from a primitive type to the same primitive type is always supported."), // boolean → boolean
        Arguments.of(resolveClass("boolean"), resolveClass("boolean"), true, "Conversion from boolean to boolean (same primitive) is trivially supported."));
    }

    @Test
    void whenSourceAndTargetAreSame_thenSupported() {
        // GIVEN
        Class<?> source = Integer.class;
        Class<?> target = Integer.class;
        // WHEN
        boolean supported = Converter.isConversionSupportedFor(source, target);
        // THEN
        assertTrue(supported, "Conversion from a type to the same type should be supported");
    }

    @Test
    void whenSupportedPrimitiveToWrapperConversion_thenSupported() {
        // GIVEN
        // primitive int
        Class<?> source = int.class;
        // wrapper Integer
        Class<?> target = Integer.class;
        // WHEN
        boolean supported = Converter.isConversionSupportedFor(source, target);
        // THEN
        assertTrue(supported, "Primitive int → Integer conversion should be supported");
    }

    @Test
    void whenSupportedStringToIntegerConversion_thenSupported() {
        // GIVEN
        Class<?> source = String.class;
        Class<?> target = Integer.class;
        // WHEN
        boolean supported = Converter.isConversionSupportedFor(source, target);
        // THEN
        assertTrue(supported, "String → Integer conversion should be supported");
    }

    @Test
    void whenSupportedDateToLocalDateTimeConversion_thenSupported() {
        // GIVEN
        Class<?> source = java.util.Date.class;
        Class<?> target = java.time.LocalDateTime.class;
        // WHEN
        boolean supported = Converter.isConversionSupportedFor(source, target);
        // THEN
        assertTrue(supported, "Date → LocalDateTime conversion should be supported");
    }

    @Test
    void whenSupportedCollectionToSetConversion_thenSupported() {
        // GIVEN
        Class<?> source = java.util.List.class;
        Class<?> target = java.util.Set.class;
        // WHEN
        boolean supported = Converter.isConversionSupportedFor(source, target);
        // THEN
        assertTrue(supported, "List → Set conversion should be supported");
    }

    @Test
    void whenUnsupportedConversion_thenNotSupported() {
        // GIVEN
        Class<?> source = String.class;
        // No built‑in conversion for Thread
        Class<?> target = Thread.class;
        // WHEN
        boolean supported = Converter.isConversionSupportedFor(source, target);
        // THEN
        assertFalse(supported, "String → Thread conversion should NOT be supported");
    }

    @Test
    void whenSourceIsNull_thenNotSupported() {
        // GIVEN
        Class<?> source = null;
        Class<?> target = Integer.class;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> Converter.isConversionSupportedFor(source, target), "Passing null as source should throw NPE (or be handled as unsupported)");
    }

    @Test
    void whenTargetIsNull_thenNotSupported() {
        // GIVEN
        Class<?> source = String.class;
        Class<?> target = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> Converter.isConversionSupportedFor(source, target), "Passing null as target should throw NPE (or be handled as unsupported)");
    }

    /* -------------------------------------------------------------
     *  Supported Conversions (group: isConversionSupportedFor - Supported Conversions)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("supportedConversions")
    void testSupportedConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        boolean actual = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, actual, description);
    }

    /* -------------------------------------------------------------
     *  Unsupported Conversions (group: isConversionSupportedFor - Unsupported Conversions)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("unsupportedConversions")
    void testUnsupportedConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        boolean actual = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, actual, description);
    }

    /* -------------------------------------------------------------
     *  Null Parameters (group: isConversionSupportedFor - Null Parameters)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("nullParameterConversions")
    void testNullParameters(Class<?> source, Class<?> target, boolean expected, String description) {
        boolean actual = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, actual, description);
    }

    /* -------------------------------------------------------------
     *  Primitive / Wrapper Conversions (group: isConversionSupportedFor - Primitive and Wrapper Conversions)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("primitiveWrapperConversions")
    void testPrimitiveWrapperConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        boolean actual = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, actual, description);
    }

    /* -------------------------------------------------------------
     *  Additional Supported Conversions (group: isConversionSupportedFor - Additional Supported Conversions)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("additionalSupportedConversions")
    void testAdditionalSupportedConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        boolean actual = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, actual, description);
    }

    /* -------------------------------------------------------------
     *  Same Type Primitive Conversions (group: isConversionSupportedFor - Same Type Primitive)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("sameTypePrimitiveConversions")
    void testSameTypePrimitiveConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        boolean actual = Converter.isConversionSupportedFor(source, target);
        assertEquals(expected, actual, description);
    }
}
