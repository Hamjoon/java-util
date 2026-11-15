package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.*;
import java.security.Permission;
import java.security.ReflectPermission;
import java.util.NoSuchElementException;

class CompactMap_compactSize_Test {

    /**
     * Instance used for the default‑behavior tests.
     */
    private CompactMap<String, Integer> defaultMap;

    class BadCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            // illegal, must be >= 2
            return 1;
        }
    }

    class LargeCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            // valid, >= 2
            return 200;
        }
    }

    class OverriddenMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            return 100;
        }
    }

    class OverriddenMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            return 100;
        }
    }

    class FirstLevelMap<K, V> extends CompactMap<K, V> {
        // No override
    }

    class SecondLevelMap<K, V> extends FirstLevelMap<K, V> {
        // No override
    }

    class BadMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            throw new RuntimeException("boom");
        }
    }

    class OverriddenMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            return 100;
        }
    }

    class OverriddenMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            return 100;
        }
    }

    class FirstLevelMap<K, V> extends CompactMap<K, V> {
        // No override
    }

    class SecondLevelMap<K, V> extends FirstLevelMap<K, V> {
        // No override
    }

    class BadMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            throw new RuntimeException("boom");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        defaultMap = new CompactMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid side‑effects between tests.
        defaultMap.clear();
    }

    /**
     * Verify that the default implementation of {@code compactSize()}
     * returns the constant defined in the source (80).
     */
    @Test
    void defaultCompactSizeIsEighty() {
        assertEquals(80, defaultMap.compactSize(), "CompactMap should report a default compactSize of 80");
    }

    /**
     * Verify that the {@link CompactMap} constructor validates the value
     * returned by {@code compactSize()}. When a subclass overrides the method
     * to return a value less than 2, the constructor must throw an
     * {@link IllegalStateException}.
     */
    @Test
    void constructorThrowsWhenCompactSizeTooSmall() {
        // Create an anonymous subclass that forces a compactSize of 1.
        class BadCompactMap<K, V> extends CompactMap<K, V> {

            @Override
            protected int compactSize() {
                // illegal, must be >= 2
                return 1;
            }
        }
        IllegalStateException ex = assertThrows(IllegalStateException.class, BadCompactMap::new, "Constructor should reject compactSize() < 2");
        assertEquals("compactSize() must be >= 2", ex.getMessage());
    }

    /**
     * Verify that a subclass can safely increase the compact size and that the
     * constructor accepts the overridden value.
     */
    @Test
    void constructorAcceptsLargerCompactSize() {
        class LargeCompactMap<K, V> extends CompactMap<K, V> {

            @Override
            protected int compactSize() {
                // valid, >= 2
                return 200;
            }
        }
        // Should not throw any exception.
        assertDoesNotThrow(LargeCompactMap::new, "Constructor should accept a compactSize >= 2");
    }

    /**
     * Direct call on an anonymous subclass – should still return 80.
     */
    @Test
    void compactSizeBasicBehavior() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertEquals(80, map.compactSize(), "compactSize() should return the constant 80");
    }

    /**
     * Invoke {@code compactSize()} via reflection with accessibility set
     * to true – expect the constant 80.
     */
    @Test
    void compactSizeViaReflectionAccessible() throws Exception {
        CompactMap<String, Integer> map = new CompactMap<>();
        Method m = CompactMap.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        int result = (int) m.invoke(map);
        assertEquals(80, result, "Reflective call should return 80");
    }

    /**
     * Attempt to invoke {@code compactSize()} via reflection without
     * setting the method accessible. The call should throw an
     * {@link IllegalAccessException} because the method is protected.
     */
    @Test
    void compactSizeViaReflectionInaccessible() throws Exception {
        CompactMap<String, Integer> map = new CompactMap<>();
        Method m = CompactMap.class.getDeclaredMethod("compactSize");
        // Do NOT call setAccessible(true)
        assertThrows(IllegalAccessException.class, () -> m.invoke(map), "Invoking a protected method without setAccessible should fail");
    }

    /**
     * Subclass overrides {@code compactSize()} to return 100 – direct call.
     */
    @Test
    void overriddenCompactSizeDirectCall() {
        class OverriddenMap<K, V> extends CompactMap<K, V> {

            @Override
            protected int compactSize() {
                return 100;
            }
        }
        CompactMap<String, Integer> map = new OverriddenMap<>();
        assertEquals(100, map.compactSize(), "Overridden compactSize() should return 100");
    }

    /**
     * Subclass overrides {@code compactSize()} to return 100 – reflective call.
     */
    @Test
    void overriddenCompactSizeViaReflection() throws Exception {
        class OverriddenMap<K, V> extends CompactMap<K, V> {

            @Override
            protected int compactSize() {
                return 100;
            }
        }
        CompactMap<String, Integer> map = new OverriddenMap<>();
        Method m = OverriddenMap.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        int result = (int) m.invoke(map);
        assertEquals(100, result, "Reflective call should respect the overridden implementation");
    }

    @Test
    void compactSizeUnaffectedByEntries() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(80, map.compactSize(), "compactSize() must stay constant after adding entries");
    }

    @Test
    void compactSizeUnaffectedByNullKeyAndValue() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put(null, 42);
        map.put("key", null);
        assertEquals(80, map.compactSize(), "compactSize() must stay constant after inserting null key/value");
    }

    @Test
    void secondLevelSubclassInheritsCompactSize() {
        class FirstLevelMap<K, V> extends CompactMap<K, V> {
            // No override
        }
        class SecondLevelMap<K, V> extends FirstLevelMap<K, V> {
            // No override
        }
        CompactMap<String, Integer> map = new SecondLevelMap<>();
        assertEquals(80, map.compactSize(), "Second‑level subclass should inherit compactSize() = 80");
    }

    @Test
    void reflectionInvokeWithNullTargetThrowsNPE() throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        assertThrows(NullPointerException.class, () -> m.invoke(null), "Invoking an instance method with a null target should throw NPE");
    }

    @Test
    void reflectionInvocationTargetExceptionWrapping() throws Exception {
        class BadMap<K, V> extends CompactMap<K, V> {

            @Override
            protected int compactSize() {
                throw new RuntimeException("boom");
            }
        }
        CompactMap<String, Integer> map = new BadMap<>();
        Method m = BadMap.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        InvocationTargetException ite = assertThrows(InvocationTargetException.class, () -> m.invoke(map), "RuntimeException should be wrapped in InvocationTargetException");
        assertTrue(ite.getCause() instanceof RuntimeException);
        assertEquals("boom", ite.getCause().getMessage());
    }

    @Test
    void reflectionIllegalArgumentExceptionForExtraArgs() throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        assertThrows(IllegalArgumentException.class, () -> m.invoke(defaultMap, 123), "Providing arguments to a no‑arg method should throw IllegalArgumentException");
    }

    @Test
    void reflectionNoSuchMethodExceptionForMisspelledName() {
        assertThrows(NoSuchMethodException.class, () -> CompactMap.class.getDeclaredMethod("compactSizeX"), "Looking up a non‑existent method should throw NoSuchMethodException");
    }

    @Test
    void reflectionSecurityExceptionWhenAccessDenied() throws Exception {
        // Save the original SecurityManager so we can restore it later.
        SecurityManager original = System.getSecurityManager();
        // Install a restrictive SecurityManager that denies reflective access.
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything except ReflectPermission "suppressAccessChecks".
                if (perm instanceof ReflectPermission) {
                    throw new SecurityException("Reflective access denied");
                }
            }
        });
        try {
            Method m = CompactMap.class.getDeclaredMethod("compactSize");
            // The following line should trigger the SecurityException.
            assertThrows(SecurityException.class, () -> m.setAccessible(true), "Setting accessible should be blocked by SecurityManager");
        } finally {
            // Restore the original SecurityManager to avoid side effects on other tests.
            System.setSecurityManager(original);
        }
    }
}
