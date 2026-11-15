package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_toPrimitiveWrapperClass_Test {

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the map does NOT contain a mapping for void.class.
        // The original test suite expects an IllegalArgumentException for void.
        removeMapEntry(void.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the void entry after each test to keep the class in a clean state.
        // (If the test itself added the entry back, this is a no‑op.)
        addMapEntry(void.class, Void.class);
    }

    private static void removeMapEntry(Class<?> key) throws Exception {
        Map<Class<?>, Class<?>> map = getPrimitiveToWrapperMap();
        map.remove(key);
    }

    private static void addMapEntry(Class<?> key, Class<?> value) throws Exception {
        Map<Class<?>, Class<?>> map = getPrimitiveToWrapperMap();
        map.put(key, value);
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> primitiveWrapperMappings() {
        return Stream.of(Arguments.of(boolean.class, Boolean.class), Arguments.of(byte.class, Byte.class), Arguments.of(char.class, Character.class), Arguments.of(short.class, Short.class), Arguments.of(int.class, Integer.class), Arguments.of(long.class, Long.class), Arguments.of(float.class, Float.class), Arguments.of(double.class, Double.class), // The JSON also contains a mapping for void → Void.
        // We add the entry temporarily inside the test.
        Arguments.of(void.class, Void.class));
    }

    /**
     * Verify that a non‑primitive class is returned unchanged.
     */
    @Test
    void nonPrimitiveClassIsReturnedUnchanged() {
        Class<?> result = ClassUtilities.toPrimitiveWrapperClass(String.class);
        assertSame(String.class, result, "Non‑primitive class should be returned unchanged");
    }

    /**
     * Verify that primitive types are converted to their wrapper equivalents.
     */
    @Test
    void primitiveTypesAreConvertedToWrappers() {
        assertAll(() -> assertEquals(Integer.class, ClassUtilities.toPrimitiveWrapperClass(int.class), "int should map to Integer"), () -> assertEquals(Long.class, ClassUtilities.toPrimitiveWrapperClass(long.class), "long should map to Long"), () -> assertEquals(Boolean.class, ClassUtilities.toPrimitiveWrapperClass(boolean.class), "boolean should map to Boolean"), () -> assertEquals(Character.class, ClassUtilities.toPrimitiveWrapperClass(char.class), "char should map to Character"), () -> assertEquals(Byte.class, ClassUtilities.toPrimitiveWrapperClass(byte.class), "byte should map to Byte"), () -> assertEquals(Short.class, ClassUtilities.toPrimitiveWrapperClass(short.class), "short should map to Short"), () -> assertEquals(Float.class, ClassUtilities.toPrimitiveWrapperClass(float.class), "float should map to Float"), () -> assertEquals(Double.class, ClassUtilities.toPrimitiveWrapperClass(double.class), "double should map to Double"));
    }

    /**
     * Verify that calling the method with a primitive that has no wrapper entry
     * (e.g., void) results in an IllegalArgumentException.
     */
    @Test
    void primitiveWithoutWrapperThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.toPrimitiveWrapperClass(void.class), "void.class should trigger IllegalArgumentException");
        assertTrue(ex.getMessage().contains("is not a primitive class"));
    }

    /**
     * Verify that a null argument causes a NullPointerException.
     * The method calls {@code primitiveClass.isPrimitive()}, which throws NPE for null.
     */
    @Test
    void nullArgumentThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.toPrimitiveWrapperClass(null), "Passing null should throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static Map<Class<?>, Class<?>> getPrimitiveToWrapperMap() throws Exception {
        Field f = ClassUtilities.class.getDeclaredField("primitiveToWrapper");
        f.setAccessible(true);
        return (Map<Class<?>, Class<?>>) f.get(null);
    }

    @ParameterizedTest(name = "primitive {0} should map to wrapper {1}")
    @MethodSource("primitiveWrapperMappings")
    void primitiveToWrapperMapping(Class<?> primitive, Class<?> expectedWrapper) throws Exception {
        // For void we need to ensure the map contains the entry,
        // because the @BeforeEach method removes it.
        if (primitive == void.class) {
            addMapEntry(void.class, Void.class);
        }
        Class<?> actual = ClassUtilities.toPrimitiveWrapperClass(primitive);
        assertEquals(expectedWrapper, actual, () -> primitive.getName() + " should map to " + expectedWrapper.getName());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void missingMappingThrowsIllegalArgumentException() throws Exception {
        // Remove the int → Integer mapping
        removeMapEntry(int.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.toPrimitiveWrapperClass(int.class), "After removing the mapping, int.class should trigger IllegalArgumentException");
        assertTrue(ex.getMessage().contains("is not a primitive class"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nonPrimitiveDoesNotInteractWithMap() throws Exception {
        // Capture the map size before the call
        int sizeBefore = getPrimitiveToWrapperMap().size();
        Class<?> result = ClassUtilities.toPrimitiveWrapperClass(Object.class);
        assertSame(Object.class, result);
        // Ensure the map was not modified
        assertEquals(sizeBefore, getPrimitiveToWrapperMap().size(), "Calling with a non‑primitive class must not alter the internal map");
    }
}
