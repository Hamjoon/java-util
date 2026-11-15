package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_doesOneWrapTheOther_Test {

    /**
     * Holds a reference to the private static {@code wrapperMap}.
     */
    private static Map<Class<?>, Class<?>> wrapperMapRef;

    @BeforeAll
    static void setupBeforeAll() {
        // Obtain the private static field once for the whole test class.
        Field field = ClassUtilities.class.getDeclaredField("wrapperMap");
        field.setAccessible(true);
        // The field is declared as Map<Class<?>, Class<?>>, we cast safely.
        @SuppressWarnings("unchecked")
        Map<Class<?>, Class<?>> map = (Map<Class<?>, Class<?>>) field.get(null);
        wrapperMapRef = map;
    }

    @AfterEach
    void clearWrapperMap() {
        // Ensure a clean state for the next test.
        wrapperMapRef.clear();
    }

    /**
     * Supplies the test data extracted from the provided JSON.  Each entry
     * contains a human‑readable description, the two parameters, and the expected result.
     */
    private static Stream<Arguments> wrapperCases() {
        return Stream.of(// Mapping exists in wrapperMap (Integer -> int) and matches y
        Arguments.of("Mapping exists (Integer -> int) matches y", Integer.class, int.class, true), // Mapping exists but y does not match
        Arguments.of("Mapping exists (Integer -> int) but y is long", Integer.class, long.class, false), // No entry for x
        Arguments.of("No entry for String, y=Object", String.class, Object.class, false), // First argument null
        Arguments.of("First argument null, y=Integer", null, Integer.class, false), // Second argument null
        Arguments.of("First argument Integer, second null", Integer.class, null, false), // Both arguments null – HashMap returns null for null key, null == null => true
        Arguments.of("Both arguments null", null, null, true), // Boolean wrapper
        Arguments.of("Mapping exists (Boolean -> boolean)", Boolean.class, boolean.class, true), // Character wrapper
        Arguments.of("Mapping exists (Character -> char)", Character.class, char.class, true), // Double wrapper
        Arguments.of("Mapping exists (Double -> double)", Double.class, double.class, true), // Float wrapper
        Arguments.of("Mapping exists (Float -> float)", Float.class, float.class, true), // Long wrapper
        Arguments.of("Mapping exists (Long -> long)", Long.class, long.class, true), // Short wrapper
        Arguments.of("Mapping exists (Short -> short)", Short.class, short.class, true), // Byte wrapper
        Arguments.of("Mapping exists (Byte -> byte)", Byte.class, byte.class, true), // Same wrapper class for both args – should be false
        Arguments.of("Same wrapper class for both args (Integer, Integer)", Integer.class, Integer.class, false), // Primitive as key – not present
        Arguments.of("Primitive key (int) with wrapper value", int.class, Integer.class, false), // Object with itself – no mapping
        Arguments.of("Object with itself", Object.class, Object.class, false), // Date with itself – no mapping
        Arguments.of("Date with itself", java.util.Date.class, java.util.Date.class, false), // Array types – not stored in map
        Arguments.of("Array types (Integer[] , int[])", Integer[].class, int[].class, false), // Void wrapper – not stored
        Arguments.of("Void wrapper (Void , void)", Void.class, void.class, false), // String with null second argument
        Arguments.of("String with null y", String.class, null, false), // Null first argument, primitive y
        Arguments.of("Null x with primitive y (int)", null, int.class, false));
    }

    /**
     * The internal wrapper map is empty by default, therefore the method should
     * return {@code false} for any pair of classes.
     */
    @Test
    void doesOneWrapTheOther_ReturnsFalse_WhenMapIsEmpty() {
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, int.class), "Without any wrapper mapping the method must return false");
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, Object.class), "Unrelated classes must also yield false");
        assertFalse(ClassUtilities.doesOneWrapTheOther(void.class, Void.class), "Even primitive‑wrapper pairs are false when the map is empty");
    }

    /**
     * Verify that passing the same class for both arguments never yields true,
     * because the map stores a *different* class as the value.
     */
    @Test
    void doesOneWrapTheOther_ReturnsFalse_WhenSameClassProvided() {
        assertFalse(ClassUtilities.doesOneWrapTheOther(Object.class, Object.class), "Same class for both parameters should not be considered a wrapper relationship");
    }

    /**
     * Ensure that a {@code null} argument is safely handled and results in {@code false}.
     */
    @Test
    void doesOneWrapTheOther_ReturnsFalse_WhenNullArguments() {
        assertFalse(ClassUtilities.doesOneWrapTheOther(null, String.class), "Null as first argument must return false");
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, null), "Null as second argument must return false");
        assertFalse(ClassUtilities.doesOneWrapTheOther(null, null), "Both arguments null must return false");
    }

    /**
     * Parameterized test that covers the exhaustive set of cases supplied in the
     * JSON description.  The map is populated according to the primitive‑wrapper
     * pairs that are expected to exist.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("wrapperCases")
    void doesOneWrapTheOther_VariousScenarios(String description, Class<?> x, Class<?> y, boolean expected) {
        // Populate the map with the standard primitive‑wrapper relationships.
        // This mirrors the real ClassUtilities static initializer (not shown here).
        wrapperMapRef.put(Integer.class, int.class);
        wrapperMapRef.put(Boolean.class, boolean.class);
        wrapperMapRef.put(Character.class, char.class);
        wrapperMapRef.put(Double.class, double.class);
        wrapperMapRef.put(Float.class, float.class);
        wrapperMapRef.put(Long.class, long.class);
        wrapperMapRef.put(Short.class, short.class);
        wrapperMapRef.put(Byte.class, byte.class);
        // Note: Void is intentionally *not* added because the original map does not contain it.
        boolean actual = ClassUtilities.doesOneWrapTheOther(x, y);
        assertEquals(expected, actual, () -> String.format("Case \"%s\" expected %s but got %s", description, expected, actual));
    }
}
