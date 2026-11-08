package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlException;

@ExtendWith(MockitoExtension.class)
class CompactMap_compactSize_Test {

    /**
     * A concrete subclass that overrides {@code compactSize()} to a custom value.
     */
    private static class CustomCompactMap<K, V> extends CompactMap<K, V> {

        private final int customSize;

        CustomCompactMap(int customSize) {
            this.customSize = customSize;
        }

        @Override
        protected int compactSize() {
            return customSize;
        }

        int exposeCompactSize() {
            return compactSize();
        }
    }

    class TooSmallCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            // violates the contract (must be >= 2)
            return 1;
        }
    }

    /**
     * Simple concrete subclass that does not override {@code compactSize()}.
     * Provides a public bridge to the protected method.
     */
    private static class DefaultCompactMap<K, V> extends CompactMap<K, V> {

        int exposeCompactSize() {
            return compactSize();
        }
    }

    /**
     * Subclass that overrides {@code compactSize()} to throw an exception.
     */
    private static class ExceptionCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            throw new IllegalStateException("forced exception");
        }

        int exposeCompactSize() {
            return compactSize();
        }
    }

    @Test
    @DisplayName("Default CompactMap returns the documented default compact size (80)")
    void defaultCompactSize_is80() {
        // given
        DefaultCompactMap<String, Integer> map = new DefaultCompactMap<>();
        // when
        int size = map.exposeCompactSize();
        // then
        assertEquals(80, size, "The default implementation of compactSize() should return 80.");
    }

    @Test
    @DisplayName("Custom CompactMap returns the overridden compact size")
    void overriddenCompactSize_returnsCustomValue() {
        // given
        int expected = 5;
        CustomCompactMap<String, Integer> map = new CustomCompactMap<>(expected);
        // when
        int actual = map.exposeCompactSize();
        // then
        assertEquals(expected, actual, "The overridden compactSize() should return the value supplied to the subclass.");
    }

    @Test
    @DisplayName("CompactMap constructor throws IllegalStateException when compactSize() < 2")
    void constructorThrowsWhenCompactSizeTooSmall() {
        // given
        class TooSmallCompactMap<K, V> extends CompactMap<K, V> {

            @Override
            protected int compactSize() {
                // violates the contract (must be >= 2)
                return 1;
            }
        }
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, TooSmallCompactMap::new, "Constructor should reject a compactSize() less than 2.");
        assertTrue(ex.getMessage().contains("compactSize() must be >= 2"));
    }

    @Test
    @DisplayName("compactSize accessible via reflection")
    void compactSize_reflectionAccessible() throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        DefaultCompactMap<String, Integer> map = new DefaultCompactMap<>();
        int result = (int) m.invoke(map);
        assertEquals(80, result, "Reflection should return the default compact size (80).");
    }

    @Test
    @DisplayName("compactSize not callable from unrelated class without setAccessible")
    void compactSize_illegalAccessWithoutSetAccessible() throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("compactSize");
        DefaultCompactMap<String, Integer> map = new DefaultCompactMap<>();
        // Do NOT call setAccessible(true)
        assertThrows(IllegalAccessException.class, () -> m.invoke(map), "Invoking a protected method without setAccessible should throw IllegalAccessException.");
    }

    @Test
    @DisplayName("compactSize via reflection on subclass instance")
    void compactSize_reflectionOnSubclass() throws Exception {
        Method m = DefaultCompactMap.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        DefaultCompactMap<String, Integer> map = new DefaultCompactMap<>();
        // Populate the map to ensure internal state changes do not affect the result
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        int result = (int) m.invoke(map);
        assertEquals(80, result, "Even after mutations, reflective call should return 80.");
    }

    @Test
    @DisplayName("compactSize reflection with wrong argument count")
    void compactSize_reflectionWrongArgCount() throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        DefaultCompactMap<String, Integer> map = new DefaultCompactMap<>();
        assertThrows(IllegalArgumentException.class, // extra argument
        () -> m.invoke(map, new Object()), "Invoking with a wrong number of arguments must throw IllegalArgumentException.");
    }

    @Test
    @DisplayName("compactSize reflection with null target")
    void compactSize_reflectionNullTarget() throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        assertThrows(NullPointerException.class, () -> m.invoke(null), "Invoking an instance method with a null target must throw NullPointerException.");
    }

    @Test
    @DisplayName("compactSize reflection with incompatible target")
    void compactSize_reflectionIncompatibleTarget() throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        String notACompactMap = "I am not a map";
        assertThrows(IllegalArgumentException.class, () -> m.invoke(notACompactMap), "Invoking compactSize() on an object that does not extend CompactMap must throw IllegalArgumentException.");
    }

    @Test
    @DisplayName("compactSize reflection blocked by SecurityManager")
    @Disabled("SecurityManager is deprecated and often disabled in modern JVMs; kept for completeness.")
    void compactSize_reflectionSecurityManagerBlocks() throws Exception {
        // Install a SecurityManager that denies setAccessible.
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(java.security.Permission perm) {
                // Allow everything except reflectively setting accessible
                if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                    throw new AccessControlException("Reflective access denied");
                }
            }
        });
        try {
            Method m = CompactMap.class.getDeclaredMethod("compactSize");
            assertThrows(AccessControlException.class, m::setAccessible, "SecurityManager should prevent setAccessible(true) and raise AccessControlException.");
        } finally {
            // clean up
            System.setSecurityManager(null);
        }
    }

    @Test
    @DisplayName("compactSize returns constant after map mutations")
    void compactSize_constantAfterMutations() {
        DefaultCompactMap<String, Integer> map = new DefaultCompactMap<>();
        // Populate
        for (int i = 0; i < 10; i++) {
            map.put("k" + i, i);
        }
        assertEquals(80, map.exposeCompactSize(), "compactSize() must stay constant (80) after putting entries.");
        // Clear and test again
        map.clear();
        assertEquals(80, map.exposeCompactSize(), "compactSize() must stay constant (80) after clearing the map.");
    }

    @Test
    @DisplayName("Overridden compactSize remains constant after mutations")
    void overriddenCompactSize_constantAfterMutations() {
        CustomCompactMap<String, Integer> map = new CustomCompactMap<>(42);
        for (int i = 0; i < 5; i++) {
            map.put("k" + i, i);
        }
        assertEquals(42, map.exposeCompactSize(), "Overridden compactSize() should still return 42 after puts.");
        map.clear();
        assertEquals(42, map.exposeCompactSize(), "Overridden compactSize() should still return 42 after clear.");
    }

    @Test
    @DisplayName("compactSize on anonymous subclass instance")
    void compactSize_anonymousSubclass() {
        CompactMap<String, Integer> anon = new CompactMap<>() {

            int expose() {
                return compactSize();
            }
        };
        // The anonymous class does not override compactSize(), so default 80 is expected
        assertEquals(80, ((CompactMap<String, Integer>) anon).expose(), "Anonymous subclass should inherit default compactSize() of 80.");
    }

    @Test
    @DisplayName("compactSize consistency across generic type parameters (String,Integer)")
    void compactSize_genericStringInteger() {
        DefaultCompactMap<String, Integer> map = new DefaultCompactMap<>();
        assertEquals(80, map.exposeCompactSize(), "Generic parameters must not affect compactSize() result.");
    }

    @Test
    @DisplayName("compactSize consistency across generic type parameters (Object,Object)")
    void compactSize_genericObjectObject() {
        DefaultCompactMap<Object, Object> map = new DefaultCompactMap<>();
        assertEquals(80, map.exposeCompactSize(), "Generic parameters must not affect compactSize() result.");
    }

    @Test
    @DisplayName("compactSize overridden to throw exception propagates")
    void overriddenCompactSize_throwsException() {
        ExceptionCompactMap<String, Integer> map = new ExceptionCompactMap<>();
        IllegalStateException ex = assertThrows(IllegalStateException.class, map::exposeCompactSize, "The overridden compactSize() should propagate its IllegalStateException.");
        assertEquals("forced exception", ex.getMessage());
    }
}
