package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.ReflectPermission;
import java.security.Permission;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllConstructors_Test {

    /**
     * -----------------------------------------------------------------------
     *  Test fixtures – classes with various constructor configurations
     * -----------------------------------------------------------------------
     */
    static class NoCtor {
        // implicit public no‑arg constructor
    }

    static class MultipleCtors {

        public MultipleCtors() {
        }

        public MultipleCtors(int a) {
        }

        private MultipleCtors(String s, double d) {
        }
    }

    static class InnerClass {

        // non‑static inner class – has an implicit synthetic constructor
        class NonStatic {

            NonStatic() {
            }
        }
    }

    // non‑static inner class – has an implicit synthetic constructor
    class NonStatic {

        NonStatic() {
        }
    }

    /* -----------------------------------------------------------------------
     *  Test fixtures – classes with various constructor configurations
     * ----------------------------------------------------------------------- */
    static class SimpleClass {
        // implicit public no‑arg constructor
    }

    static class MultiConstructorClass {

        public MultiConstructorClass() {
        }

        public MultiConstructorClass(int a) {
        }

        private MultiConstructorClass(String s, double d) {
        }
    }

    static class PrivateConstructorClass {

        private PrivateConstructorClass() {
        }
    }

    static class SecureClass {

        public SecureClass() {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the sorted‑constructors cache to an empty map before each test
        ReflectionUtils.setSortedConstructorsCache(new HashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared after the test as a safety net
        ReflectionUtils.setSortedConstructorsCache(new HashMap<>());
        // Reset any custom SecurityManager that a test may have installed
        System.setSecurityManager(null);
    }

    private static Stream<Arguments> primitiveAndArrayProvider() {
        return Stream.of(Arguments.of(int.class), Arguments.of(boolean.class), Arguments.of(double.class), Arguments.of(void.class), Arguments.of(String[].class), Arguments.of(int[][].class));
    }

    @Test
    void testNullClassReturnsEmptyArray() {
        Constructor<?>[] result = ReflectionUtils.getAllConstructors(null);
        assertNotNull(result, "Result must never be null");
        assertEquals(0, result.length, "Null input should yield an empty array");
    }

    @Test
    void testAllConstructorsReturned() {
        // Expected constructors are those declared directly on the class
        Constructor<?>[] expected = MultipleCtors.class.getDeclaredConstructors();
        Constructor<?>[] actual = ReflectionUtils.getAllConstructors(MultipleCtors.class);
        // The utility returns *all* constructors (including private ones)
        assertEquals(expected.length, actual.length, "Number of constructors returned should match declared constructors");
        // Verify that each expected constructor appears in the result (order is not required)
        for (Constructor<?> exp : expected) {
            boolean found = false;
            for (Constructor<?> act : actual) {
                if (act.equals(exp)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Expected constructor not found: " + exp);
        }
    }

    @Test
    void testCacheReturnsSameInstance() {
        // First call – populates the cache
        Constructor<?>[] firstCall = ReflectionUtils.getAllConstructors(NoCtor.class);
        // Second call – should hit the cache and return the *same* array instance
        Constructor<?>[] secondCall = ReflectionUtils.getAllConstructors(NoCtor.class);
        assertSame(firstCall, secondCall, "Subsequent calls for the same class must return the cached array instance");
    }

    @Test
    void testCacheIsClearedWhenReset() {
        // Populate cache
        Constructor<?>[] beforeReset = ReflectionUtils.getAllConstructors(NoCtor.class);
        // Reset the cache via the public API
        ReflectionUtils.setSortedConstructorsCache(new HashMap<>());
        // New call should produce a *different* array instance
        Constructor<?>[] afterReset = ReflectionUtils.getAllConstructors(NoCtor.class);
        assertNotSame(beforeReset, afterReset, "After resetting the cache a new array instance should be created");
        // The contents must still be correct
        assertArrayEquals(beforeReset, afterReset, "Even after cache reset the constructor contents must be identical");
    }

    @Test
    void testInnerClassConstructorsHandled() {
        // Non‑static inner class has an additional synthetic constructor that takes the
        // enclosing instance as its first argument.
        Class<?> innerClass = InnerClass.class.getDeclaredClasses()[0];
        Constructor<?>[] constructors = ReflectionUtils.getAllConstructors(innerClass);
        // At least one constructor must exist (the synthetic one)
        assertTrue(constructors.length >= 1, "Inner class should have at least one constructor (the synthetic one)");
        // Verify that one of the constructors has a first parameter of type InnerClass
        boolean foundSynthetic = false;
        for (Constructor<?> ctor : constructors) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length > 0 && params[0] == InnerClass.class) {
                foundSynthetic = true;
                break;
            }
        }
        assertTrue(foundSynthetic, "Expected synthetic constructor that receives the enclosing instance");
    }

    /* -----------------------------------------------------------------------
     *  Primitive and array types – they have no constructors
     * ----------------------------------------------------------------------- */
    @ParameterizedTest(name = "Class {0} should yield empty constructor array")
    @MethodSource("primitiveAndArrayProvider")
    void testPrimitiveAndArrayTypesReturnEmptyArray(Class<?> clazz) {
        Constructor<?>[] result = ReflectionUtils.getAllConstructors(clazz);
        assertNotNull(result, "Result must never be null");
        assertEquals(0, result.length, "Primitive or array type should have no constructors");
    }

    /* -----------------------------------------------------------------------
     *  Private constructor handling – should be returned as well
     * ----------------------------------------------------------------------- */
    @Test
    void testPrivateConstructorIsReturned() {
        Constructor<?>[] constructors = ReflectionUtils.getAllConstructors(PrivateConstructorClass.class);
        assertEquals(1, constructors.length, "PrivateConstructorClass defines exactly one private constructor");
        assertTrue(constructors[0].canAccess(null) == false, "The returned constructor should be the private one (accessibility unchanged)");
    }

    /* -----------------------------------------------------------------------
     *  SecurityManager restriction – the method must propagate the exception
     * ----------------------------------------------------------------------- */
    @Test
    void testSecurityManagerDeniesReflectiveAccess() {
        // Install a SecurityManager that denies all reflective access
        SecurityManager denyAll = new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Throw only for reflective access checks
                if (perm instanceof ReflectPermission) {
                    throw new SecurityException("Reflective access denied by test SecurityManager");
                }
            }
        };
        System.setSecurityManager(denyAll);
        // The call should propagate the SecurityException thrown by Class.getDeclaredConstructors()
        assertThrows(SecurityException.class, () -> ReflectionUtils.getAllConstructors(SecureClass.class), "Expected SecurityException when reflective access is denied");
    }

    /* -----------------------------------------------------------------------
     *  Null cache handling – setting the internal cache to null should cause NPE
     * ----------------------------------------------------------------------- */
    @Test
    void testNullSortedConstructorsCacheThrowsNPE() {
        // Explicitly set the internal cache to null
        ReflectionUtils.setSortedConstructorsCache(null);
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getAllConstructors(SimpleClass.class), "When the internal cache map is null, a NullPointerException is expected");
    }
}
