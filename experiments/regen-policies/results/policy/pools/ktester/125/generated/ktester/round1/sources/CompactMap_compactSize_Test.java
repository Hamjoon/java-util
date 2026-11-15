package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class CompactMap_compactSize_Test {

    /**
     * Helper subclass that simply exposes the protected {@code compactSize()}
     * method via a public wrapper. Used for tests that need to call the method
     * directly.
     */
    private static class ExposedCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected Map<K, V> getNewMap() {
            return new HashMap<>();
        }

        public int exposeCompactSize() {
            return compactSize();
        }
    }

    /**
     * Helper subclass that overrides {@code compactSize()} to return a custom
     * value and also exposes it via a public wrapper.
     */
    private static class OverriddenCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected Map<K, V> getNewMap() {
            return new HashMap<>();
        }

        @Override
        protected int compactSize() {
            return 5;
        }

        public int exposeCompactSize() {
            return compactSize();
        }
    }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    /**
     * Returns {@code true} if the supplied method is {@code public}.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method is {@code public}
     */
    public static boolean isPublic(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }

    /**
     * Returns {@code true} if the supplied method is {@code static}.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method is {@code static}
     */
    public static boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    /**
     * Returns {@code true} if the supplied method is {@code abstract}.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method is {@code abstract}
     */
    public static boolean isAbstract(Method method) {
        return Modifier.isAbstract(method.getModifiers());
    }

    /**
     * Finds a declared method with the given name and parameter types on the
     * supplied class. Returns {@code null} if no such method exists.
     *
     * @param clazz the class to search; never {@code null}
     * @param name the method name; never {@code null}
     * @param parameterTypes the method parameter types; may be empty
     * @return the {@link Method} if found, otherwise {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * Returns {@code true} if a method with the given name and parameter types
     * is present on the supplied class.
     *
     * @param clazz the class to search; never {@code null}
     * @param name the method name; never {@code null}
     * @param parameterTypes the method parameter types; may be empty
     * @return {@code true} if such a method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes) != null;
    }

    /**
     * Supplies the boolean flags for {@link #compactSizeRemainsConstantAfterModifications(boolean)}.
     *
     * @return a stream of booleans: {@code false} (only insertions) and {@code true}
     *         (insertions then clear).
     */
    private static Stream<Boolean> provideClearFlag() {
        return Stream.of(false, true);
    }

    /**
     * Verify that the base implementation of {@code compactSize()} returns the
     * constant {@code DEFAULT_COMPACT_SIZE} (70).
     */
    @Test
    void defaultCompactSize_is70() {
        ExposedCompactMap<String, Integer> map = new ExposedCompactMap<>();
        assertEquals(70, map.exposeCompactSize(), "Default compactSize should be 70");
    }

    /**
     * Verify that a subclass can override {@code compactSize()} and that the
     * overridden value is respected by the map implementation.
     */
    @Test
    void overriddenCompactSize_isRespected() {
        OverriddenCompactMap<String, Integer> map = new OverriddenCompactMap<>();
        assertEquals(5, map.exposeCompactSize(), "Overridden compactSize should be 5");
    }

    /**
     * The {@code CompactMap} constructor must reject a compact size smaller than 2.
     * This test creates a subclass that returns {@code 1} and expects an
     * {@link IllegalArgumentException} during construction.
     */
    @Test
    void constructorThrowsWhenCompactSizeTooSmall() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            new CompactMap<String, Integer>() {

                @Override
                protected int compactSize() {
                    // illegal, must be >= 2
                    return 1;
                }

                @Override
                protected Map<String, Integer> getNewMap() {
                    return new HashMap<>();
                }
            };
        });
        assertTrue(ex.getMessage().contains("compactSize() must be >= 2"), "Exception message should mention the compact size constraint");
    }

    /**
     * Demonstrates invoking the protected {@code compactSize()} via reflection,
     * ensuring that the method can be accessed without altering visibility.
     */
    @Test
    void reflectivelyInvokeCompactSize() throws Exception {
        CompactMap<String, String> map = new CompactMap<>();
        Method method = CompactMap.class.getDeclaredMethod("compactSize");
        method.setAccessible(true);
        int size = (int) method.invoke(map);
        assertEquals(70, size, "Reflection should return the default compact size of 70");
    }

    /**
     * Parameterized test that verifies {@code compactSize()} remains constant
     * after map modifications:
     * <ul>
     *   <li>After inserting several entries.</li>
     *   <li>After clearing the map that previously contained entries.</li>
     * </ul>
     *
     * @param clearAfterInsert if {@code true}, the map is cleared after insertion before checking compactSize.
     */
    @ParameterizedTest(name = "compactSize after modifications (clearAfterInsert={0})")
    @MethodSource("provideClearFlag")
    void compactSizeRemainsConstantAfterModifications(boolean clearAfterInsert) {
        ExposedCompactMap<String, Integer> map = new ExposedCompactMap<>();
        // Insert several key/value pairs (more than 2 to ensure we are in the "compact array" state)
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", 4);
        if (clearAfterInsert) {
            map.clear();
        }
        // The compact size must still be the default constant (70)
        assertEquals(70, map.exposeCompactSize(), "compactSize should remain 70 after " + (clearAfterInsert ? "clear()" : "insertions"));
    }
}
