package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.provider.Arguments;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_doesOneWrapTheOther_Test {

    /* ----------------------------------------------------------------------
     *  Helper state for the "wrapperMap is null" tests.
     * ---------------------------------------------------------------------- */
    private static Map<Class<?>, Class<?>> originalWrapperMap = null;

    private static boolean wrapperMapWasTampered = false;

    @AfterEach
    void teardownAfterEach() {
        // Ensure that the wrapperMap is restored after any test that
        // temporarily sets it to null.
        restoreWrapperMapIfNeeded();
    }

    /* ----------------------------------------------------------------------
     *  Parameter source for primitive‑wrapper pairs.
     * ---------------------------------------------------------------------- */
    private static Stream<Arguments> primitiveWrapperPairs() {
        return Stream.of(Arguments.of(void.class, Void.class), Arguments.of(boolean.class, Boolean.class), Arguments.of(byte.class, Byte.class), Arguments.of(char.class, Character.class), Arguments.of(short.class, Short.class), Arguments.of(int.class, Integer.class), Arguments.of(long.class, Long.class), Arguments.of(float.class, Float.class), Arguments.of(double.class, Double.class));
    }

    /**
     * Stores the current value of {@code ClassUtilities.wrapperMap} and then
     * replaces it with {@code null}.  The original map is saved so it can be
     * restored after the test.
     */
    private static void nullifyWrapperMap() throws Exception {
        Field mapField = ClassUtilities.class.getDeclaredField("wrapperMap");
        mapField.setAccessible(true);
        // Remove the {@code final} modifier so we can set a new value.
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(mapField, mapField.getModifiers() & ~Modifier.FINAL);
        // Save the original map (if we haven't already) and set the field to null.
        if (!wrapperMapWasTampered) {
            originalWrapperMap = (Map<Class<?>, Class<?>>) mapField.get(null);
            wrapperMapWasTampered = true;
        }
        mapField.set(null, null);
    }

    /**
     * Restores the original {@code wrapperMap} if it has been set to {@code null}
     * by a test.
     */
    private static void restoreWrapperMapIfNeeded() {
        if (!wrapperMapWasTampered) {
            return;
        }
        try {
            Field mapField = ClassUtilities.class.getDeclaredField("wrapperMap");
            mapField.setAccessible(true);
            // Remove the {@code final} modifier again.
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(mapField, mapField.getModifiers() & ~Modifier.FINAL);
            mapField.set(null, originalWrapperMap);
        } catch (Exception e) {
            // If restoration fails we want the test suite to fail loudly.
            throw new IllegalStateException("Failed to restore wrapperMap", e);
        } finally {
            wrapperMapWasTampered = false;
            originalWrapperMap = null;
        }
    }

    /**
     * Primitive → wrapper pairs that must evaluate to {@code true}.
     */
    private static Stream<Arguments> positivePrimitiveWrapperPairs() {
        return Stream.of(Arguments.of(void.class, Void.class), Arguments.of(boolean.class, Boolean.class), Arguments.of(byte.class, Byte.class), Arguments.of(char.class, Character.class), Arguments.of(short.class, Short.class), Arguments.of(int.class, Integer.class), Arguments.of(long.class, Long.class), Arguments.of(float.class, Float.class), Arguments.of(double.class, Double.class));
    }

    /**
     * Various negative scenarios – the expected result is {@code false}.
     */
    private static Stream<Arguments> negativeCases() {
        return Stream.of(// primitive does not wrap the given class
        Arguments.of(int.class, Long.class), // wrapper does not wrap primitive (direction reversed)
        Arguments.of(Integer.class, int.class), // same primitive as both arguments
        Arguments.of(int.class, int.class), // same wrapper as both arguments
        Arguments.of(Integer.class, Integer.class), // non‑primitive class as x
        Arguments.of(String.class, String.class), // array types
        Arguments.of(int[].class, Integer[].class), // wrapper as x, primitive as y
        Arguments.of(Integer.class, int.class), // primitive with unrelated wrapper
        Arguments.of(double.class, Integer.class), // primitive with null wrapper
        Arguments.of(int.class, null), // null primitive with non‑null wrapper
        Arguments.of(null, Integer.class), // missing mapping (Date → Date)
        Arguments.of(Date.class, Date.class), // missing mapping (Date → null)
        Arguments.of(Date.class, null));
    }

    /**
     * Null‑argument scenarios where the expected boolean result is defined
     *  by the JSON test data.
     */
    private static Stream<Arguments> nullArgumentCases() {
        return Stream.of(// both arguments null → true (wrapperMap.get(null) == null)
        Arguments.of(null, null, true), // first null, second non‑null → false
        Arguments.of(null, Integer.class, false), // first non‑null, second null → false (already covered in negativeCases,
        // but kept here for clarity)
        Arguments.of(int.class, null, false));
    }

    @ParameterizedTest(name = "doesOneWrapTheOther({0}, {1}) => true")
    @MethodSource("primitiveWrapperPairs")
    void testPrimitiveWrapsItsWrapper(Class<?> primitive, Class<?> wrapper) {
        assertTrue(ClassUtilities.doesOneWrapTheOther(primitive, wrapper), () -> primitive.getName() + " should wrap " + wrapper.getName());
    }

    @ParameterizedTest(name = "doesOneWrapTheOther({1}, {0}) => false")
    @MethodSource("primitiveWrapperPairs")
    void testWrapperDoesNotWrapPrimitive(Class<?> primitive, Class<?> wrapper) {
        assertFalse(ClassUtilities.doesOneWrapTheOther(wrapper, primitive), () -> wrapper.getName() + " should NOT wrap " + primitive.getName());
    }

    @Test
    @DisplayName("Unrelated classes must return false")
    void testUnrelatedClasses() {
        assertFalse(ClassUtilities.doesOneWrapTheOther(String.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(ArrayList.class, LinkedList.class));
    }

    @Test
    @DisplayName("Same class (wrapper‑to‑wrapper) must return false")
    void testSameClass() {
        assertFalse(ClassUtilities.doesOneWrapTheOther(Integer.class, Integer.class));
        assertFalse(ClassUtilities.doesOneWrapTheOther(int.class, int.class));
    }

    @Test
    @DisplayName("Null first argument should throw NullPointerException")
    void testNullFirstArgument() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.doesOneWrapTheOther(null, Integer.class));
    }

    @Test
    @DisplayName("Null second argument should return false")
    void testNullSecondArgument() {
        assertFalse(ClassUtilities.doesOneWrapTheOther(int.class, null));
    }

    /* ----------------------------------------------------------------------
     *  Positive match – primitive wraps its wrapper
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "doesOneWrapTheOther({0}, {1}) => true")
    @MethodSource("positivePrimitiveWrapperPairs")
    void testPositivePrimitiveWrapsWrapper(Class<?> primitive, Class<?> wrapper) {
        assertTrue(ClassUtilities.doesOneWrapTheOther(primitive, wrapper), () -> primitive.getName() + " should wrap " + wrapper.getName());
    }

    /* ----------------------------------------------------------------------
     *  Negative matches – all cases must return false
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "doesOneWrapTheOther({0}, {1}) => false")
    @MethodSource("negativeCases")
    void testNegativeScenarios(Class<?> x, Class<?> y) {
        assertFalse(ClassUtilities.doesOneWrapTheOther(x, y), () -> String.format("Expected false for doesOneWrapTheOther(%s, %s)", Objects.toString(x), Objects.toString(y)));
    }

    /* ----------------------------------------------------------------------
     *  Null‑argument handling
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "doesOneWrapTheOther({0}, {1}) => {2}")
    @MethodSource("nullArgumentCases")
    void testNullArguments(Class<?> x, Class<?> y, boolean expected) {
        assertEquals(expected, ClassUtilities.doesOneWrapTheOther(x, y), () -> String.format("Expected %s for doesOneWrapTheOther(%s, %s)", expected, Objects.toString(x), Objects.toString(y)));
    }

    /* ----------------------------------------------------------------------
     *  Exception – wrapperMap is null
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("When wrapperMap is null, doesOneWrapTheOther throws NullPointerException")
    void testWrapperMapNullThrowsNPE() throws Exception {
        // Arrange – replace the internal map with null
        nullifyWrapperMap();
        // Act & Assert – primitive‑wrapper pair
        assertThrows(NullPointerException.class, () -> ClassUtilities.doesOneWrapTheOther(int.class, Integer.class), "Expected NPE when wrapperMap is null");
        // Act & Assert – both arguments null (still NPE because map access is attempted)
        assertThrows(NullPointerException.class, () -> ClassUtilities.doesOneWrapTheOther(null, null), "Expected NPE when wrapperMap is null even with null arguments");
    }
}
