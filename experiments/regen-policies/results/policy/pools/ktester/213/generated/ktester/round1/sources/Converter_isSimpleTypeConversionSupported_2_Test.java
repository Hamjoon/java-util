package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_isSimpleTypeConversionSupported_2_Test {

    class Custom {
    }

    private static Class<?> toClass(String name) {
        if (name == null) {
            return null;
        }
        return switch(name) {
            case "int" ->
                int.class;
            case "boolean" ->
                boolean.class;
            case "char" ->
                char.class;
            case "byte" ->
                byte.class;
            case "short" ->
                short.class;
            case "long" ->
                long.class;
            case "float" ->
                float.class;
            case "double" ->
                double.class;
            default ->
                {
                    try {
                        ???;
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Unable to resolve class name: " + name, e);
                    }
                }
        };
    }

    static Stream<Arguments> supportedSimpleConversions() {
        return Stream.of(Arguments.of(toClass("java.lang.Integer"), toClass("java.lang.String"), true, "Conversion from Integer to String should be supported."), Arguments.of(toClass("java.lang.String"), toClass("java.lang.Integer"), true, "Conversion from String to Integer should be supported."), Arguments.of(toClass("int"), toClass("java.lang.Integer"), true, "Primitive int to wrapper Integer conversion is supported."), Arguments.of(toClass("java.lang.Integer"), toClass("int"), true, "Wrapper Integer to primitive int conversion is supported."), Arguments.of(toClass("java.lang.Boolean"), toClass("java.lang.String"), true, "Conversion from Boolean to String should be supported."), Arguments.of(toClass("java.lang.String"), toClass("java.lang.Boolean"), true, "Conversion from String to Boolean should be supported."), Arguments.of(toClass("java.util.Date"), toClass("java.sql.Timestamp"), true, "Conversion from java.util.Date to java.sql.Timestamp is a simple supported conversion."), Arguments.of(toClass("java.sql.Timestamp"), toClass("java.util.Date"), true, "Conversion from java.sql.Timestamp to java.util.Date is a simple supported conversion."), Arguments.of(toClass("java.lang.String"), toClass("java.lang.String"), true, "Identical source and target types should be considered supported."));
    }

    static Stream<Arguments> unsupportedOrEdgeCaseConversions() {
        return Stream.of(Arguments.of(toClass("java.time.LocalDate"), toClass("java.util.Date"), false, "Conversion between LocalDate and java.util.Date is not a simple conversion."), Arguments.of(toClass("java.lang.Object"), toClass("java.lang.String"), false, "Generic Object to String is not considered a simple conversion."), Arguments.of(null, toClass("java.lang.String"), false, "Null source class should result in unsupported conversion."), Arguments.of(toClass("java.lang.String"), null, false, "Null target class should result in unsupported conversion."), Arguments.of(null, null, false, "Both source and target being null should be unsupported."));
    }

    static Stream<Arguments> additionalSupportedSimpleConversions() {
        return Stream.of(Arguments.of(toClass("int"), toClass("int"), true, "Identical primitive types (int → int) are trivially supported."), Arguments.of(toClass("boolean"), toClass("java.lang.Boolean"), true, "Primitive boolean can be converted to its wrapper Boolean."), Arguments.of(toClass("java.lang.Boolean"), toClass("boolean"), true, "Wrapper Boolean can be converted back to primitive boolean."), Arguments.of(toClass("char"), toClass("java.lang.Character"), true, "Primitive char to wrapper Character conversion is supported."), Arguments.of(toClass("java.lang.Character"), toClass("char"), true, "Wrapper Character to primitive char conversion is supported."), Arguments.of(toClass("double"), toClass("java.lang.Double"), true, "Primitive double to wrapper Double conversion is supported."), Arguments.of(toClass("java.lang.Float"), toClass("float"), true, "Wrapper Float to primitive float conversion is supported."), Arguments.of(toClass("java.lang.String"), toClass("char"), true, "String to primitive char (first character) is treated as a simple conversion."), Arguments.of(toClass("java.lang.Character"), toClass("java.lang.String"), true, "Wrapper Character to String conversion is supported."), Arguments.of(toClass("java.lang.String"), toClass("java.math.BigDecimal"), true, "String representation of a number can be converted to BigDecimal."), Arguments.of(toClass("java.math.BigDecimal"), toClass("java.lang.String"), true, "BigDecimal to its String representation is a simple conversion."), Arguments.of(toClass("java.util.UUID"), toClass("java.lang.String"), true, "UUID to String (via toString) is supported as a simple conversion."), Arguments.of(toClass("java.lang.String"), toClass("java.util.UUID"), true, "String containing a UUID can be converted to a UUID instance."), Arguments.of(toClass("java.util.concurrent.atomic.AtomicInteger"), toClass("java.lang.Integer"), true, "AtomicInteger can be converted to its underlying Integer value."), Arguments.of(toClass("java.lang.Integer"), toClass("java.util.concurrent.atomic.AtomicInteger"), true, "Integer can be wrapped into an AtomicInteger via a simple conversion."), Arguments.of(toClass("java.util.Date"), toClass("java.util.Calendar"), true, "Conversion from java.util.Date to java.util.Calendar is considered a simple conversion."), Arguments.of(toClass("java.util.Calendar"), toClass("java.util.Date"), true, "Conversion from java.util.Calendar to java.util.Date is a simple supported conversion."));
    }

    static Stream<Arguments> additionalUnsupportedConversions() {
        return Stream.of(Arguments.of(toClass("java.util.List"), toClass("java.lang.String"), false, "Converting a collection (List) directly to a String is not a simple conversion."), Arguments.of(toClass("java.util.Map"), toClass("java.lang.String"), false, "Converting a Map directly to a String is not a simple conversion."), Arguments.of(toClass("java.time.LocalDateTime"), toClass("java.util.Date"), false, "LocalDateTime ↔ java.util.Date requires complex handling; not a simple conversion."), Arguments.of(toClass("java.util.Date"), toClass("java.time.LocalDateTime"), false, "java.util.Date ↔ LocalDateTime conversion is not considered simple."), Arguments.of(toClass("java.time.Instant"), toClass("java.util.Date"), false, "Instant to Date conversion is not a simple conversion in this utility."), Arguments.of(toClass("java.util.Date"), toClass("java.time.Instant"), false, "Date to Instant conversion is not a simple conversion."), Arguments.of(toClass("java.lang.String"), toClass("java.lang.Object"), false, "String → Object is not treated as a simple conversion (no special handling)."), Arguments.of(toClass("java.lang.Object"), toClass("java.lang.Integer"), false, "Generic Object → Integer requires explicit conversion logic; not simple."), Arguments.of(toClass("java.lang.Enum"), toClass("java.lang.String"), false, "Enum → String is not listed as a simple conversion in this context."), Arguments.of(toClass("int[]"), toClass("java.lang.String"), false, "Array of primitives to String is not a simple conversion."), Arguments.of(toClass("java.lang.Class"), toClass("java.lang.String"), false, "Class objects cannot be directly converted to String via a simple conversion."));
    }

    /**
     * Verify that the method returns {@code true} for a variety of
     * simple, natively‑supported source/target pairs.  The method is
     * typically used with the same source and target class, but the
     * contract also allows simple conversions between compatible
     * primitive/wrapper types.
     */
    @Test
    void testSupportedSimpleTypes() {
        // Primitive wrappers and primitives
        assertTrue(Converter.isSimpleTypeConversionSupported(Integer.class, Integer.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(int.class, int.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Boolean.class, Boolean.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(boolean.class, boolean.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Character.class, Character.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(char.class, char.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Byte.class, Byte.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(byte.class, byte.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Short.class, Short.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(short.class, short.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Long.class, Long.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(long.class, long.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Float.class, Float.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(float.class, float.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Double.class, Double.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(double.class, double.class));
        // Common object types
        assertTrue(Converter.isSimpleTypeConversionSupported(String.class, String.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Date.class, Date.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(java.sql.Date.class, java.sql.Date.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Timestamp.class, Timestamp.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(LocalDate.class, LocalDate.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(LocalDateTime.class, LocalDateTime.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(ZonedDateTime.class, ZonedDateTime.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Instant.class, Instant.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(Calendar.class, Calendar.class));
        // Numeric high‑precision types
        assertTrue(Converter.isSimpleTypeConversionSupported(BigDecimal.class, BigDecimal.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(BigInteger.class, BigInteger.class));
        // Atomic types
        assertTrue(Converter.isSimpleTypeConversionSupported(AtomicBoolean.class, AtomicBoolean.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(AtomicInteger.class, AtomicInteger.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(AtomicLong.class, AtomicLong.class));
        // Buffer types
        assertTrue(Converter.isSimpleTypeConversionSupported(ByteBuffer.class, ByteBuffer.class));
        assertTrue(Converter.isSimpleTypeConversionSupported(CharBuffer.class, CharBuffer.class));
        // UUID
        assertTrue(Converter.isSimpleTypeConversionSupported(UUID.class, UUID.class));
    }

    /**
     * Verify that the method returns {@code false} for types that are
     * not considered “simple” by the Converter – for example collections,
     * maps, arrays and user‑defined classes.
     */
    @Test
    void testUnsupportedSimpleTypes() {
        // Collections and maps
        assertFalse(Converter.isSimpleTypeConversionSupported(List.class, List.class));
        assertFalse(Converter.isSimpleTypeConversionSupported(Set.class, Set.class));
        assertFalse(Converter.isSimpleTypeConversionSupported(Map.class, Map.class));
        assertFalse(Converter.isSimpleTypeConversionSupported(Collection.class, Collection.class));
        // Arrays
        assertFalse(Converter.isSimpleTypeConversionSupported(int[].class, int[].class));
        assertFalse(Converter.isSimpleTypeConversionSupported(Object[].class, Object[].class));
        // User‑defined class
        class Custom {
        }
        assertFalse(Converter.isSimpleTypeConversionSupported(Custom.class, Custom.class));
        // Mixed source/target where at least one side is non‑simple
        assertFalse(Converter.isSimpleTypeConversionSupported(String.class, List.class));
        assertFalse(Converter.isSimpleTypeConversionSupported(Map.class, String.class));
    }

    /**
     * The method should also handle {@code null} arguments gracefully.
     * According to the public contract, a {@code null} source or target
     * is not a simple conversion, so the result must be {@code false}.
     */
    @Test
    void testNullArguments() {
        assertFalse(Converter.isSimpleTypeConversionSupported(null, String.class));
        assertFalse(Converter.isSimpleTypeConversionSupported(String.class, null));
        assertFalse(Converter.isSimpleTypeConversionSupported(null, null));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("supportedSimpleConversions")
    void testSupportedSimpleConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        boolean actual = Converter.isSimpleTypeConversionSupported(source, target);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("unsupportedOrEdgeCaseConversions")
    void testUnsupportedOrEdgeCaseConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        boolean actual = Converter.isSimpleTypeConversionSupported(source, target);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("additionalSupportedSimpleConversions")
    void testAdditionalSupportedSimpleConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        boolean actual = Converter.isSimpleTypeConversionSupported(source, target);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("additionalUnsupportedConversions")
    void testAdditionalUnsupportedConversions(Class<?> source, Class<?> target, boolean expected, String description) {
        boolean actual = Converter.isSimpleTypeConversionSupported(source, target);
        assertEquals(expected, actual, description);
    }
}
