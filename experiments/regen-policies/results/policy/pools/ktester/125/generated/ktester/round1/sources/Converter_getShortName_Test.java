package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.sql.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Converter_getShortName_Test {

    class MyClass {
    }

    private static Stream<Arguments> provideNonArrayTypes() {
        return Stream.of(Arguments.of(String.class, "String"), Arguments.of(Integer.class, "Integer"), Arguments.of(int.class, "int"), Arguments.of(java.util.List.class, "List"), Arguments.of(java.time.DayOfWeek.class, "DayOfWeek"));
    }

    private static Stream<Arguments> provideArrayTypes() {
        return Stream.of(Arguments.of(int[].class, "int[]"), Arguments.of(double[].class, "double[]"), Arguments.of(byte[].class, "byte[]"), Arguments.of(String[].class, "String[]"), Arguments.of(Integer[].class, "Integer[]"));
    }

    private static Stream<Arguments> provideMultiDimensionalArrayTypes() {
        return Stream.of(Arguments.of(int[][].class, "int[][]"), Arguments.of(String[][].class, "String[][]"));
    }

    /**
     * Simple (non‑array, non‑Date) class should return its simple Converter_getShortName_Test.
     */
    @Test
    void getShortName_returnsSimpleName_forRegularClass() {
        assertEquals("String", Converter.getShortName(String.class));
        assertEquals("Integer", Converter.getShortName(Integer.class));
    }

    /**
     * Primitive array should return primitive simple Converter_getShortName_Test followed by "[]".
     */
    @Test
    void getShortName_returnsComponentSimpleNamePlusBrackets_forPrimitiveArray() {
        assertEquals("int[]", Converter.getShortName(int[].class));
        assertEquals("double[]", Converter.getShortName(double[].class));
        assertEquals("byte[]", Converter.getShortName(byte[].class));
    }

    /**
     * Object array should return component simple Converter_getShortName_Test followed by "[]".
     */
    @Test
    void getShortName_returnsComponentSimpleNamePlusBrackets_forObjectArray() {
        assertEquals("String[]", Converter.getShortName(String[].class));
        assertEquals("Integer[]", Converter.getShortName(Integer[].class));
        // Custom class to verify generic handling
        class MyClass {
        }
        assertEquals("MyClass[]", Converter.getShortName(MyClass[].class));
    }

    /**
     * java.sql.Date is treated specially – the fully‑qualified Converter_getShortName_Test is returned.
     */
    @Test
    void getShortName_returnsFullName_forSqlDate() {
        assertEquals(Date.class.getName(), Converter.getShortName(Date.class));
    }

    /**
     * Passing {@code null} should throw a {@link NullPointerException}.
     */
    @Test
    void getShortName_throwsNPE_whenNullArgument() {
        Assertions.assertThrows(NullPointerException.class, () -> Converter.getShortName(null));
    }

    @ParameterizedTest(name = "{index}: type={0} → expected={1}")
    @MethodSource("provideNonArrayTypes")
    void getShortName_returnsSimpleName_forNonArrayTypes(Class<?> type, String expected) {
        assertEquals(expected, Converter.getShortName(type));
    }

    @ParameterizedTest(name = "{index}: type={0} → expected={1}")
    @MethodSource("provideArrayTypes")
    void getShortName_returnsComponentSimpleNamePlusBrackets_forArrayTypes(Class<?> type, String expected) {
        assertEquals(expected, Converter.getShortName(type));
    }

    @ParameterizedTest(name = "{index}: type={0} → expected={1}")
    @MethodSource("provideMultiDimensionalArrayTypes")
    void getShortName_handlesMultiDimensionalArrays(Class<?> type, String expected) {
        assertEquals(expected, Converter.getShortName(type));
    }

    @Test
    void getShortName_returnsCustomName_whenDefinedInCustomArrayNames() throws Exception {
        // Obtain the private static map via reflection
        Field field = Converter.class.getDeclaredField("CUSTOM_ARRAY_NAMES");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Class<?>, String> map = (Map<Class<?>, String>) field.get(null);
        // Insert a custom entry and ensure it is removed afterwards
        try {
            map.put(int[].class, "intArray");
            assertEquals("intArray", Converter.getShortName(int[].class));
        } finally {
            map.remove(int[].class);
        }
    }

    @Test
    void getShortName_returnsFullyQualifiedName_forJavaUtilDate() {
        assertEquals(java.util.Date.class.getName(), Converter.getShortName(java.util.Date.class));
    }

    @Test
    void getShortName_returnsSimpleName_forJavaSqlDate() {
        // java.sql.Date is NOT the special case; it falls back to default handling
        assertEquals("Date", Converter.getShortName(Date.class));
    }
}
