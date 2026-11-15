package com.cedarsoftware.util.convert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.Date;
import java.sql.Timestamp;
import java.time.Month;
import java.util.stream.Stream;

import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.*;   // <-- added for assertEquals, assertThrows

@ExtendWith(MockitoExtension.class)
class Converter_getShortName_Test {

    /**
     * Holds a reference to the private CUSTOM_ARRAY_NAMES map via reflection.
     */
    private Map<Class<?>, String> customArrayNamesMap;

    /**
     * Map that allows registration of custom short names for specific array
     * types.  The key is the array {@link Class} (e.g. {@code Set[].class})
     * and the value is the short name that {@link #getShortName(Class)} should
     * return for that type.
     */
    static final Map<Class<?>, String> CUSTOM_ARRAY_NAMES = new ConcurrentHashMap<>();

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Access the private static field CUSTOM_ARRAY_NAMES in Converter using reflection.
        Field field = Converter.class.getDeclaredField("CUSTOM_ARRAY_NAMES");
        field.setAccessible(true);
        // The field is static, so pass null as the target object.
        @SuppressWarnings("unchecked")
        Map<Class<?>, String> map = (Map<Class<?>, String>) field.get(null);
        this.customArrayNamesMap = map;
        // Register a custom short name for Set[] to test the CUSTOM_ARRAY_NAMES map.
        this.customArrayNamesMap.put(Set[].class, "CustomSetArray");
    }

    @AfterEach
    void teardownAfterEach() {
        // Remove the custom entry to avoid side‑effects on other tests.
        if (this.customArrayNamesMap != null) {
            this.customArrayNamesMap.remove(Set[].class);
        }
    }

    private static Stream<Arguments> arrayHandlingProvider() {
        return // Simple one‑dimensional arrays
                Stream. // Simple one‑dimensional arrays
                of( // Custom name registered in CUSTOM_ARRAY_NAMES
                        Arguments.of(int[].class, "int[]",
                                "Array type without a custom short name should return component simple name plus \"[]\"."),
                        Arguments.of(String[].class, "String[]",
                                "Array type without a custom short name should return component simple name plus \"[]\"."),
                        Arguments.of(java.util.List[].class, "List[]",
                                "Array of a non‑primitive type without a custom short name should return the component's simple name followed by \"[]\"."),
                        Arguments.of(java.time.LocalDate[].class, "LocalDate[]",
                                "Array of a class from java.time package without a custom short name should return the component's simple name plus \"[]\"."),
                        Arguments.of(java.util.Map[].class, "Map[]",
                                "Array of a class from java.util package without a custom short name should return the component's simple name plus \"[]\"."),
                        // Multi‑dimensional arrays
                        Arguments.of(Set[].class, "CustomSetArray",
                                "Array type with a custom short name registered in CUSTOM_ARRAY_NAMES should return that custom name."),
                        Arguments.of(int[][].class, "int[][]",
                                "int[][] → component type is int[] (simple name \"int[]\") → result \"int[][]\""),
                        Arguments.of(String[][].class, "String[][]",
                                "String[][] → component type is String[] (simple name \"String[]\") → result \"String[][]\"")
                );
    }

    private static Stream<Arguments> nonArrayHandlingProvider() {
        // Anonymous class instance – its simple name is an empty string.
        Class<?> anonymousClass = new Runnable() {
            public void run() {
            }
        }.getClass();

        return Stream.of(
                Arguments.of(Date.class, "java.util.Date",
                        "java.util.Date matches the special Date case; method should return the fully‑qualified class name, not the simple name."),
                Arguments.of(Timestamp.class, "Timestamp",
                        "A Date subclass that is *not* java.util.Date should fall back to the default path and return the simple name."),
                Arguments.of(int.class, "int",
                        "Primitive type (int) is not an array and not Date; method should return its simple name."),
                Arguments.of(Integer.class, "Integer",
                        "Wrapper class should return its simple name."),
                Arguments.of(Month.class, "Month",
                        "Enum type should be treated like any other class and return the simple name."),
                Arguments.of(java.util.List.class, "List",
                        "Interface type should return its simple name."),
                Arguments.of(java.util.AbstractList.class, "AbstractList",
                        "Abstract class should return its simple name."),
                Arguments.of(Map.Entry.class, "Entry",
                        "Inner class (static member) should return the inner class's simple name."),
                Arguments.of(anonymousClass, "",
                        "Anonymous class has an empty simple name; method should return an empty string.")
        );
    }

    /**
     * Returns a short name for the given class.
     *
     * <ul>
     *   <li>For specific array types, returns the custom name from
     *   {@link #CUSTOM_ARRAY_NAMES} if present.</li>
     *   <li>For other array types, returns the component's simple name plus
     *   {@code "[]"}.</li>
     *   <li>For {@code java.util.Date}, returns the fully‑qualified name
     *   {@code "java.util.Date"}.</li>
     *   <li>For all other classes, returns the simple name.</li>
     * </ul>
     *
     * @param type the class to get the short name for; must not be {@code null}
     * @return the short name of the class
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public static String getShortName(Class<?> type) {
        if (type == null) {
            throw new NullPointerException("Class argument must not be null");
        }
        // Array handling – first look for a custom registration
        if (type.isArray()) {
            String custom = CUSTOM_ARRAY_NAMES.get(type);
            if (custom != null) {
                return custom;
            }
            // No custom name – use component simple name + "[]"
            Class<?> component = type.getComponentType();
            // component may itself be an array; its simple name already contains brackets
            return component.getSimpleName() + "[]";
        }
        // Special case for java.util.Date – full name required by the spec
        if (type == java.util.Date.class) {
            return "java.util.Date";
        }
        // All other classes – simple name (works for primitives, wrappers,
        // enums, interfaces, inner classes, and anonymous classes)
        return type.getSimpleName();
    }

    @Test
    void getShortName_returnsSimpleNameForRegularClasses() {
        assertEquals("String", Converter.getShortName(String.class));
        assertEquals("Integer", Converter.getShortName(Integer.class));
        assertEquals("BigDecimal", Converter.getShortName(java.math.BigDecimal.class));
    }

    @Test
    void getShortName_returnsFullNameForUtilDate() {
        // Special case – java.util.Date returns its fully‑qualified name.
        assertEquals("java.util.Date", Converter.getShortName(Date.class));
    }

    @Test
    void getShortName_returnsComponentSimpleNameWithBrackets_forObjectArray() {
        assertEquals("String[]", Converter.getShortName(String[].class));
        assertEquals("Object[]", Converter.getShortName(Object[].class));
    }

    @Test
    void getShortName_returnsComponentSimpleNameWithBrackets_forPrimitiveArray() {
        assertEquals("int[]", Converter.getShortName(int[].class));
        assertEquals("byte[]", Converter.getShortName(byte[].class));
        assertEquals("double[]", Converter.getShortName(double[].class));
    }

    @Test
    void getShortName_handlesMultidimensionalArrays() {
        // int[][] → component type is int[] (simple name "int[]") → result "int[][]"
        assertEquals("int[][]", Converter.getShortName(int[][].class));
        // String[][] → component type is String[] (simple name "String[]") → result "String[][]"
        assertEquals("String[][]", Converter.getShortName(String[][].class));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("arrayHandlingProvider")
    void getShortName_arrayHandling(Class<?> type, String expected, String description) {
        assertEquals(expected, Converter.getShortName(type), description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nonArrayHandlingProvider")
    void getShortName_nonArrayHandling(Class<?> type, String expected, String description) {
        assertEquals(expected, Converter.getShortName(type), description);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getShortName_nullArgument_throwsNullPointerException() {
        Executable call = () -> Converter.getShortName(null);
        assertThrows(NullPointerException.class, call,
                "Passing a null Class argument should cause a NullPointerException because the method dereferences the argument (type.isArray()).");
    }
}