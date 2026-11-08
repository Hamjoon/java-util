package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

// No mocks are required, but the annotation is kept as in the template
@ExtendWith(MockitoExtension.class)
class ClassUtilities_doesOneWrapTheOther_Test {

    /**
     * Reference to the private static {@code wrapperMap} field inside {@link ClassUtilities}.
     */
    private static Map<Class<?>, Class<?>> wrapperMap;

    /* ----------------------------------------------------------------------
     *  Lifecycle hooks – the current implementation of doesOneWrapTheOther
     *  does not need any explicit setup or teardown, but the methods are
     *  retained to satisfy the supplied template.
     * ---------------------------------------------------------------------- */
    @BeforeAll
    static void setupBeforeAll() {
        Field f = ClassUtilities.class.getDeclaredField("wrapperMap");
        f.setAccessible(true);
        // The field is static, so the target object is null.
        wrapperMap = (Map<Class<?>, Class<?>>) f.get(null);
    }

    @BeforeEach
    void setupBeforeEach() {
        wrapperMap.clear();
        // Populate with the standard wrapper → primitive pairs.
        wrapperMap.put(Integer.class, int.class);
        wrapperMap.put(Boolean.class, boolean.class);
        wrapperMap.put(Long.class, long.class);
        wrapperMap.put(Double.class, double.class);
        wrapperMap.put(Character.class, char.class);
        wrapperMap.put(Short.class, short.class);
        wrapperMap.put(Byte.class, byte.class);
        wrapperMap.put(Float.class, float.class);
    }

    /**
     * Supplies the test data extracted from the JSON description.
     */
    private static Stream<Arguments> provideTestCases() {
        return Stream.of(// Mapping exists and matches
        Arguments.of("java.lang.Integer", "int", true, "Wrapper map contains Integer → int; should return true.", "Mapping exists and matches"), // No mapping for x
        Arguments.of("java.lang.String", "int", false, "String is not a key; should return false.", "No mapping for x – String"), Arguments.of("java.util.Date", "java.lang.Integer", false, "Date is not a key; should return false.", "No mapping for x – Date"), // Mapping exists but different y
        Arguments.of("java.lang.Integer", "long", false, "Integer maps to int, not long; should return false.", "Mapping exists but different y – long"), Arguments.of("java.lang.Boolean", "byte", false, "Boolean maps to boolean, not byte; should return false.", "Mapping exists but different y – byte"), // Both parameters null
        Arguments.of(null, null, true, "Both arguments null – map lookup yields null == null.", "Both parameters null"), // x null, y non‑null
        Arguments.of(null, "int", false, "x is null, map returns null which does not equal int.", "x null, y non‑null"), // y null, x non‑null
        Arguments.of("java.lang.Integer", null, false, "Integer maps to int; comparing int to null yields false.", "y null, x non‑null"), // Reverse direction (primitive as x, wrapper as y)
        Arguments.of("int", "java.lang.Integer", false, "Map stores wrapper → primitive, not primitive → wrapper.", "Reverse direction – primitive as x"), Arguments.of("boolean", "java.lang.Boolean", false, "Same reasoning for boolean.", "Reverse direction – primitive as x (boolean)"), // Unrelated classes
        Arguments.of("java.lang.String", "java.lang.Object", false, "Neither class participates in wrapperMap.", "Unrelated classes – String/Object"), Arguments.of("java.lang.Long", "java.lang.Double", false, "Both are wrappers but map only relates each wrapper to its own primitive.", "Unrelated wrappers"));
    }

    /**
     * Resolves a class name to a {@link Class} instance.
     *
     * <p>The method understands primitive names such as {@code "int"} and returns
     * the corresponding primitive {@code Class}.  For fully‑qualified names it
     * delegates to {@link Class#forName(String)}.  A {@code null} argument yields
     * {@code null}.</p>
     *
     * @param name the class name or primitive keyword
     * @return the {@code Class} object or {@code null}
     */
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
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Unable to resolve class name: " + name, e);
                }
        }
    }

    @Test
    void primitiveToWrapperIsRecognised() {
        assertTrue(ClassUtilities.doesOneWrapTheOther(int.class, Integer.class), "int should wrap to Integer");
        assertTrue(ClassUtilities.doesOneWrapTheOther(boolean.class, Boolean.class), "boolean should wrap to Boolean");
        assertTrue(ClassUtilities.doesOneWrapTheOther(double.class, Double.class), "double should wrap to Double");
        assertTrue(ClassUtilities.doesOneWrapTheOther(char.class, Character.class), "char should wrap to Character");
    }

    @Test
    void wrapperToPrimitiveIsNotRecognised() {
        // The map is one‑way (primitive → wrapper). The reverse direction must be false.
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class), "Integer should not wrap to int");
        assertFalse(ClassUtilities.doesOneWrapTheOther(Boolean.class, boolean.class), "Boolean should not wrap to boolean");
    }

    @Test
    void unrelatedClassesReturnFalse() {
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, Integer.class), "Unrelated classes must not be considered a wrapper relationship");
        assertFalse(ClassUtilities.doesOneWrapTheOther(Object.class, String.class), "Unrelated classes must not be considered a wrapper relationship");
    }

    @Test
    void nullArgumentsHandledGracefully() {
        // wrapperMap.get(null) returns null, so the comparison yields true only when y is also null.
        // The contract does not define a special case for null, but the method must not throw.
        assertFalse(ClassUtilities.doesOneWrapTheOther(null, Integer.class), "null as first argument should result in false");
        assertFalse(ClassUtilities.doesOneWrapTheOther(int.class, null), "null as second argument should result in false");
        // Both null – the map returns null, and null == null evaluates to true.
        // This behaviour is documented by the implementation, not by the API, so we assert the actual result.
        assertTrue(ClassUtilities.doesOneWrapTheOther(null, null), "Both arguments null should return true because map lookup yields null");
    }

    @Test
    void sameClassNeverWrapsEachOther() {
        // Even if a class maps to itself (which never happens in the current implementation),
        // the method checks for a *different* entry in the map, therefore the result must be false.
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, Integer.class), "A class should not be considered to wrap itself");
        assertFalse(ClassUtilities.doesOneWrapTheOther(int.class, int.class), "A primitive should not be considered to wrap itself");
    }

    /* ----------------------------------------------------------------------
     *  Parameterised test covering all JSON‑specified groups.
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("provideTestCases")
    void doesOneWrapTheOther_variousScenarios(String xName, String yName, boolean expected, String description, String testName) {
        Class<?> x = resolveClass(xName);
        Class<?> y = resolveClass(yName);
        boolean actual = ClassUtilities.doesOneWrapTheOther(x, y);
        assertEquals(expected, actual, description);
    }
}
