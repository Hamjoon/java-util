package com.cedarsoftware.util;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_toPrimitiveWrapperClass_Test {

    // Simple class used only for the returnsVoid test above.
    private static class Sample {

        public void doSomething() {
        }

        public int doNothing() {
            return 0;
        }
    }

    /**
     * Returns {@code true} if the supplied {@link java.lang.reflect.Method}
     * has a {@code void} return type.
     *
     * @param method the method to inspect; must not be {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(java.lang.reflect.Method method) {
        if (method == null) {
            throw new NullPointerException("method must not be null");
        }
        return void.class.equals(method.getReturnType());
    }

    private static Stream<Arguments> primitiveToWrapperProvider() {
        return Stream.of(Arguments.of(boolean.class, Boolean.class), Arguments.of(byte.class, Byte.class), Arguments.of(char.class, Character.class), Arguments.of(short.class, Short.class), Arguments.of(int.class, Integer.class), Arguments.of(long.class, Long.class), Arguments.of(float.class, Float.class), Arguments.of(double.class, Double.class), Arguments.of(void.class, Void.class));
    }

    private static Stream<Arguments> primitiveWithMappingProvider() {
        // same data as primitiveToWrapperProvider – the map will be overridden in the test
        return primitiveToWrapperProvider();
    }

    private static Stream<Arguments> primitiveWithoutMappingProvider() {
        // void is a primitive but is *not* expected to be present in the map for this test
        return Stream.of(Arguments.of(boolean.class), Arguments.of(char.class), Arguments.of(void.class));
    }

    private static Stream<Arguments> arrayTypeProvider() {
        return Stream.of(Arguments.of(int[].class), Arguments.of(String[].class));
    }

    private static void withMapSnapshot(Runnable testBody) throws ReflectiveOperationException {
        Map<Class<?>, Class<?>> map = getPrimitiveToWrapperMap();
        Map<Class<?>, Class<?>> backup = new HashMap<>(map);
        try {
            testBody.run();
        } finally {
            map.clear();
            map.putAll(backup);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Class<?>, Class<?>> getPrimitiveToWrapperMap() throws ReflectiveOperationException {
        Field f = ClassUtilities.class.getDeclaredField("primitiveToWrapper");
        f.setAccessible(true);
        return (Map<Class<?>, Class<?>>) f.get(null);
    }

    @ParameterizedTest(name = "{index}: {0} → {1}")
    @MethodSource("primitiveToWrapperProvider")
    void toPrimitiveWrapperClass_convertsPrimitiveToWrapper(Class<?> primitive, Class<?> expectedWrapper) {
        Class<?> actual = ClassUtilities.toPrimitiveWrapperClass(primitive);
        assertEquals(expectedWrapper, actual, () -> "Expected wrapper for " + primitive.getName() + " to be " + expectedWrapper.getName());
    }

    @Test
    void toPrimitiveWrapperClass_returnsSameForNonPrimitive() {
        Class<?> nonPrimitive = String.class;
        Class<?> result = ClassUtilities.toPrimitiveWrapperClass(nonPrimitive);
        assertSame(nonPrimitive, result, "Non‑primitive class should be returned unchanged");
    }

    @Test
    void toPrimitiveWrapperClass_returnsSameForWrapperClass() {
        Class<?> wrapper = Integer.class;
        Class<?> result = ClassUtilities.toPrimitiveWrapperClass(wrapper);
        assertSame(wrapper, result, "Wrapper class should be returned unchanged");
    }

    @Test
    void toPrimitiveWrapperClass_nullArgument_throwsNPE() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.toPrimitiveWrapperClass(null), "Passing null should result in NullPointerException");
    }

    @Test
    void toPrimitiveWrapperClass_validPrimitiveDoesNotThrow() {
        assertDoesNotThrow(() -> ClassUtilities.toPrimitiveWrapperClass(int.class), "Valid primitive should not cause an exception");
    }

    @ParameterizedTest(name = "{index}: {0} → {1}")
    @MethodSource("primitiveWithMappingProvider")
    void toPrimitiveWrapperClass_convertsPrimitiveToWrapper_whenMappingPresent(Class<?> primitive, Class<?> expectedWrapper) throws Exception {
        withMapSnapshot(() -> {
            try {
                Map<Class<?>, Class<?>> map = getPrimitiveToWrapperMap();
                map.clear();
                map.put(primitive, expectedWrapper);
                Class<?> actual = ClassUtilities.toPrimitiveWrapperClass(primitive);
                assertEquals(expectedWrapper, actual, () -> "Expected wrapper for " + primitive.getName() + " to be " + expectedWrapper.getName());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @ParameterizedTest(name = "{index}: {0} unmapped → IAE")
    @MethodSource("primitiveWithoutMappingProvider")
    void toPrimitiveWrapperClass_unmappedPrimitive_throws(Class<?> primitive) throws Exception {
        withMapSnapshot(() -> {
            try {
                Map<Class<?>, Class<?>> map = getPrimitiveToWrapperMap();
                // ensure the primitive is not present
                map.clear();
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.toPrimitiveWrapperClass(primitive), "Expected IllegalArgumentException for unmapped primitive " + primitive.getName());
                assertTrue(ex.getMessage().contains(primitive.getName()), "Exception message should mention the primitive class");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void toPrimitiveWrapperClass_mappingWithNullValue_throws() throws Exception {
        withMapSnapshot(() -> {
            try {
                Map<Class<?>, Class<?>> map = getPrimitiveToWrapperMap();
                map.clear();
                // deliberately insert null
                map.put(short.class, null);
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.toPrimitiveWrapperClass(short.class), "Expected IllegalArgumentException when map contains null value");
                assertTrue(ex.getMessage().contains("short"), "Exception message should contain the primitive name");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @ParameterizedTest(name = "{index}: {0} unchanged")
    @MethodSource("arrayTypeProvider")
    void toPrimitiveWrapperClass_arrayTypesReturnedUnchanged(Class<?> arrayClass) {
        Class<?> result = ClassUtilities.toPrimitiveWrapperClass(arrayClass);
        assertSame(arrayClass, result, "Array class should be returned unchanged");
    }

    @Test
    void toPrimitiveWrapperClass_repeatedCallsReturnConsistentResult() throws Exception {
        withMapSnapshot(() -> {
            try {
                Map<Class<?>, Class<?>> map = getPrimitiveToWrapperMap();
                map.clear();
                map.put(int.class, Integer.class);
                Class<?> first = ClassUtilities.toPrimitiveWrapperClass(int.class);
                Class<?> second = ClassUtilities.toPrimitiveWrapperClass(int.class);
                assertEquals(Integer.class, first);
                assertSame(first, second, "Subsequent calls should return the same wrapper class instance");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void returnsVoid_worksCorrectly() throws NoSuchMethodException {
        Method m1 = Sample.class.getMethod("doSomething");
        Method m2 = Sample.class.getMethod("doNothing");
        assertTrue(ClassUtilities_toPrimitiveWrapperClass_Test.returnsVoid(m1));
        assertFalse(ClassUtilities_toPrimitiveWrapperClass_Test.returnsVoid(m2));
    }
}
