package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;
import java.util.stream.Stream;
// import org.junit.After;
// import org.junit.AfterClass;
// import org.junit.Before;
// import org.junit.BeforeClass;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.junit.runners.JUnit4;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
// @RunWith(JUnit4.class)
class ClassUtilities_doesOneWrapTheOther_Test {

    /**
     * Holds a reference to the private static {@code wrapperMap} inside {@link ClassUtilities}.
     */
    private static Map<Class<?>, Class<?>> wrapperMap;

//     @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Obtain the private static field via reflection once for the whole test suite.
        Field field = ClassUtilities.class.getDeclaredField("wrapperMap");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Class<?>, Class<?>> map = (Map<Class<?>, Class<?>>) field.get(null);
        wrapperMap = map;
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean map for each test case.
        wrapperMap.clear();
        // Populate with a few known primitive‑wrapper pairs that we will use in the tests.
        // The direction of the mapping matters because {@code doesOneWrapTheOther}
        // checks {@code wrapperMap.get(x) == y}.
        // wrapper → primitive
        wrapperMap.put(Integer.class, int.class);
        // primitive → wrapper
        wrapperMap.put(int.class, Integer.class);
        wrapperMap.put(Boolean.class, boolean.class);
        wrapperMap.put(boolean.class, Boolean.class);
    }

//     @AfterEach
    void teardownAfterEach() {
        // Clean up to avoid cross‑test contamination.
        wrapperMap.clear();
    }

    // -----------------------------------------------------------------
//     private static Stream<Arguments> positivePrimitiveToWrapperProvider() {
//         return Stream.of(Arguments.of(int.class, Integer.class, true, "int.class → Integer.class"), Arguments.of(boolean.class, Boolean.class, true, "boolean.class → Boolean.class"));
//     }

    // -----------------------------------------------------------------
//     private static Stream<Arguments> negativeWrapperToPrimitiveProvider() {
//         return Stream.of(Arguments.of(Integer.class, int.class, false, "Integer.class → int.class (wrapper key missing)"), Arguments.of(Boolean.class, boolean.class, false, "Boolean.class → boolean.class (wrapper key missing)"));
//     }

    // -----------------------------------------------------------------
//     private static Stream<Arguments> unrelatedClassesProvider() {
//         return Stream.of(Arguments.of(String.class, Integer.class, false, "String.class ↔ Integer.class"), Arguments.of(java.util.Date.class, Long.class, false, "Date.class ↔ Long.class"));
//     }

    // -----------------------------------------------------------------
//     private static Stream<Arguments> nullParametersProvider() {
//         return Stream.of(Arguments.of(null, null, false, "Both arguments null – should return false"), Arguments.of(null, Integer.class, false, "First argument null – should return false"), Arguments.of(int.class, null, false, "Second argument null – should return false"));
//     }

    // -----------------------------------------------------------------
//     private static Stream<Arguments> samePrimitiveProvider() {
//         return Stream.of(Arguments.of(int.class, int.class, false, "int.class ↔ int.class"), Arguments.of(boolean.class, boolean.class, false, "boolean.class ↔ boolean.class"));
//     }

    // -----------------------------------------------------------------
//     private static Stream<Arguments> sameWrapperProvider() {
//         return Stream.of(Arguments.of(Integer.class, Integer.class, false, "Integer.class ↔ Integer.class"), Arguments.of(Boolean.class, Boolean.class, false, "Boolean.class ↔ Boolean.class"));
//     }

    // -----------------------------------------------------------------
//     @Test
//     @DisplayName("doesOneWrapTheOther returns true for wrapper → primitive mapping")
//     void testWrapperToPrimitiveTrue() {
//         assertTrue("Integer.class should wrap int.class", ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
//     }

//     @Test
//     @DisplayName("doesOneWrapTheOther returns true for primitive → wrapper mapping")
//     void testPrimitiveToWrapperTrue() {
//         assertTrue("int.class should wrap Integer.class", ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
//     }

//     @Test
//     @DisplayName("doesOneWrapTheOther returns true for Boolean ↔ boolean mapping")
//     void testBooleanMappingTrue() {
//         assertTrue(ClassUtilities.doesOneWrapTheOther(Boolean.class, boolean.class));
//         assertTrue(ClassUtilities.doesOneWrapTheOther(boolean.class, Boolean.class));
//     }

    // -----------------------------------------------------------------
//     @Test
//     @DisplayName("doesOneWrapTheOther returns false when no mapping exists")
//     void testNoMappingFalse() {
//         assertFalse("Unrelated classes should not be considered wrappers of each other", ClassUtilities.doesOneWrapTheOther(String.class, Integer.class));
//     }

//     @Test
//     @DisplayName("doesOneWrapTheOther returns false when mapping is one‑way only")
//     void testOneWayMappingFalse() {
//         // Remove the primitive → wrapper entry for Double to test one‑way behaviour.
//         wrapperMap.remove(double.class);
//         // only wrapper → primitive
//         wrapperMap.put(Double.class, double.class);
//         assertTrue("Double.class → double.class should be true", ClassUtilities.doesOneWrapTheOther(Double.class, double.class));
//         assertFalse("double.class → Double.class should be false because the map entry is missing", ClassUtilities.doesOneWrapTheOther(double.class, Double.class));
//     }

    // -----------------------------------------------------------------
//     @Test
//     @DisplayName("doesOneWrapTheOther returns false when first argument is null")
//     void testFirstArgNull() {
//         assertFalse("Null as first argument must yield false", ClassUtilities.doesOneWrapTheOther(null, Integer.class));
//     }

//     @Test
//     @DisplayName("doesOneWrapTheOther returns false when second argument is null")
//     void testSecondArgNull() {
//         assertFalse("Null as second argument must yield false", ClassUtilities.doesOneWrapTheOther(Integer.class, null));
//     }

//     @Test
//     @DisplayName("doesOneWrapTheOther returns false when both arguments are null")
//     void testBothArgsNull() {
//         assertFalse("Both arguments null must yield false", ClassUtilities.doesOneWrapTheOther(null, null));
//     }

    // -----------------------------------------------------------------
//     @Test
//     @DisplayName("doesOneWrapTheOther handles null entries in the map gracefully")
//     void testMapContainsNullEntry() {
//         wrapperMap.put(null, null);
//         assertFalse("null key with non‑null value should yield false", ClassUtilities.doesOneWrapTheOther(null, String.class));
//         assertFalse("non‑null key with null value should yield false", ClassUtilities.doesOneWrapTheOther(String.class, null));
//     }

//     @ParameterizedTest(name = "[{index}] {3}")
//     @MethodSource("positivePrimitiveToWrapperProvider")
//     @DisplayName("Positive mapping – primitive to its wrapper should return true")
//     void testPositivePrimitiveToWrapper(Class<?> x, Class<?> y, boolean expected, String description) {
//         assertEquals(expected, ClassUtilities.doesOneWrapTheOther(x, y), description);
//     }

//     @ParameterizedTest(name = "[{index}] {3}")
//     @MethodSource("negativeWrapperToPrimitiveProvider")
//     @DisplayName("Negative mapping – wrapper as key should return false when entry is missing")
//     void testNegativeWrapperToPrimitive(Class<?> x, Class<?> y, boolean expected, String description) {
//         // Ensure the map does NOT contain wrapper → primitive entries for this test.
//         wrapperMap.remove(Integer.class);
//         wrapperMap.remove(Boolean.class);
//         assertEquals(expected, ClassUtilities.doesOneWrapTheOther(x, y), description);
//     }

//     @ParameterizedTest(name = "[{index}] {3}")
//     @MethodSource("unrelatedClassesProvider")
//     @DisplayName("Negative mapping – unrelated classes should return false")
//     void testUnrelatedClasses(Class<?> x, Class<?> y, boolean expected, String description) {
//         assertEquals(expected, ClassUtilities.doesOneWrapTheOther(x, y), description);
//     }

//     @ParameterizedTest(name = "[{index}] {3}")
//     @MethodSource("nullParametersProvider")
//     @DisplayName("Null handling – method should compare nulls without throwing")
//     void testNullParameters(Class<?> x, Class<?> y, boolean expected, String description) {
//         assertEquals(expected, ClassUtilities.doesOneWrapTheOther(x, y), description);
//     }

//     @ParameterizedTest(name = "[{index}] {3}")
//     @MethodSource("samePrimitiveProvider")
//     @DisplayName("Same class (primitive) – should return false")
//     void testSamePrimitive(Class<?> x, Class<?> y, boolean expected, String description) {
//         assertEquals(expected, ClassUtilities.doesOneWrapTheOther(x, y), description);
//     }

//     @ParameterizedTest(name = "[{index}] {3}")
//     @MethodSource("sameWrapperProvider")
//     @DisplayName("Same class (wrapper) – should return false")
//     void testSameWrapper(Class<?> x, Class<?> y, boolean expected, String description) {
//         assertEquals(expected, ClassUtilities.doesOneWrapTheOther(x, y), description);
//     }

//     @AfterClass
//     public static void teardownAfterAll() {
//         // No global state to clean up beyond what the individual tests do.
//     }

//     @Test
//     public void positivePrimitiveToWrapper() {
//         assertTrue("int.class → Integer.class", ClassUtilities.doesOneWrapTheOther(int.class, Integer.class));
//         assertTrue("boolean.class → Boolean.class", ClassUtilities.doesOneWrapTheOther(boolean.class, Boolean.class));
//     }

//     @Test
//     public void negativeWrapperToPrimitive() {
//         // Ensure the map does NOT contain wrapper → primitive entries for this test.
//         wrapperMap.remove(Integer.class);
//         wrapperMap.remove(Boolean.class);
//         assertFalse("Integer.class → int.class (wrapper key missing)", ClassUtilities.doesOneWrapTheOther(Integer.class, int.class));
//         assertFalse("Boolean.class → boolean.class (wrapper key missing)", ClassUtilities.doesOneWrapTheOther(Boolean.class, boolean.class));
//     }

//     @Test
//     public void unrelatedClasses() {
//         assertFalse("String.class ↔ Integer.class", ClassUtilities.doesOneWrapTheOther(String.class, Integer.class));
//         assertFalse("Date.class ↔ Long.class", ClassUtilities.doesOneWrapTheOther(java.util.Date.class, Long.class));
//     }

//     @Test
//     public void nullParameters() {
//         assertFalse("Both arguments null – should return false", ClassUtilities.doesOneWrapTheOther(null, null));
//         assertFalse("First argument null – should return false", ClassUtilities.doesOneWrapTheOther(null, Integer.class));
//         assertFalse("Second argument null – should return false", ClassUtilities.doesOneWrapTheOther(int.class, null));
//     }

//     @Test
//     public void samePrimitive() {
//         assertFalse("int.class ↔ int.class", ClassUtilities.doesOneWrapTheOther(int.class, int.class));
//         assertFalse("boolean.class ↔ boolean.class", ClassUtilities.doesOneWrapTheOther(boolean.class, boolean.class));
//     }

//     @Test
//     public void sameWrapper() {
//         assertFalse("Integer.class ↔ Integer.class", ClassUtilities.doesOneWrapTheOther(Integer.class, Integer.class));
//         assertFalse("Boolean.class ↔ Boolean.class", ClassUtilities.doesOneWrapTheOther(Boolean.class, Boolean.class));
//     }
}