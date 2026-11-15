package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.lang.reflect.*;
import java.security.Permission;
import java.util.*;
import java.util.stream.Stream;

class CompactLinkedMap_useCopyIterator_Test {

    private TestableCompactLinkedMap<String, Integer> map;

    /**
     * Simple subclass that exposes the protected {@code useCopyIterator()} method as public.
     */
    private static class TestableCompactLinkedMap<K, V> extends CompactLinkedMap<K, V> {

        public boolean callUseCopyIterator() {
            return super.useCopyIterator();
        }
    }

    /* --------------------------------------------------------------
     *  Helper data class for parameterised reflection exception tests
     * -------------------------------------------------------------- */
    private static class ReflectionExceptionCase {

        // target object for Method.invoke
        final Object instance;

        // whether to call setAccessible(true)
        final boolean setAccessible;

        // arguments to pass (null for none)
        final Object[] arguments;

        final Class<? extends Exception> expectedException;

        final String description;

        ReflectionExceptionCase(Object instance, boolean setAccessible, Object[] arguments, Class<? extends Exception> expectedException, String description) {
            this.instance = instance;
            this.setAccessible = setAccessible;
            this.arguments = arguments;
            this.expectedException = expectedException;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test.
        map = new TestableCompactLinkedMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC (not strictly required).
        map = null;
    }

    /**
     * Helper to obtain the reflected Method for {@code useCopyIterator}.
     */
    private static Method getUseCopyIteratorMethod() throws NoSuchMethodException {
        Method m = CompactLinkedMap.class.getDeclaredMethod("useCopyIterator");
        // Do NOT set accessible here – each test decides whether to.
        return m;
    }

    /**
     * Parameter source for exception‑driven reflection tests.
     */
    private static Stream<ReflectionExceptionCase> reflectionExceptionProvider() {
        return Stream.of(// 1. IllegalAccessException – no setAccessible(true)
        new ReflectionExceptionCase(new CompactLinkedMap<>(), false, null, IllegalAccessException.class, "IllegalAccessException when setAccessible(false)"), // 2. NullPointerException – null target
        new ReflectionExceptionCase(null, true, null, NullPointerException.class, "NullPointerException when target instance is null"), // 3. IllegalArgumentException – unrelated target object
        new ReflectionExceptionCase(new Object(), true, null, IllegalArgumentException.class, "IllegalArgumentException when target is not a CompactLinkedMap"), // 4. IllegalArgumentException – wrong argument count
        new ReflectionExceptionCase(new CompactLinkedMap<>(), true, new Object[] { true }, IllegalArgumentException.class, "IllegalArgumentException when passing arguments to a zero‑arg method"));
    }

    @Test
    void useCopyIteratorReturnsFalseByDefault() {
        // GIVEN a freshly constructed CompactLinkedMap (via the testable subclass)
        // WHEN the protected method is invoked through the public wrapper
        boolean result = map.callUseCopyIterator();
        // THEN the result must be false as per the default implementation
        assertFalse(result, "useCopyIterator() should return false for the default CompactLinkedMap");
    }

    @Test
    void useCopyIteratorViaReflectionReturnsFalse() throws Exception {
        // GIVEN a fresh instance
        CompactLinkedMap<String, Integer> instance = new CompactLinkedMap<>();
        // AND a reflected Method object for the protected method
        Method m = CompactLinkedMap.class.getDeclaredMethod("useCopyIterator");
        // allow access for the test
        m.setAccessible(true);
        // WHEN invoking the method via reflection
        Object result = m.invoke(instance);
        // THEN the result is false
        assertTrue(result instanceof Boolean);
        assertFalse((Boolean) result, "Reflection call should also return false");
    }

    @Test
    void useCopyIteratorRemainsFalseWhenMapIsPopulated() {
        // Populate the map with several entries
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        // Verify that the wrapper still reports false
        assertFalse(map.callUseCopyIterator(), "useCopyIterator() must stay false regardless of map contents");
    }

    @Test
    void useCopyIteratorRemainsFalseWhenMapIsCleared() {
        // Populate then clear
        map.put("a", 10);
        map.clear();
        // Verify false after clear
        assertFalse(map.callUseCopyIterator(), "useCopyIterator() must stay false after clearing the map");
    }

    @Test
    void useCopyIteratorRemainsFalseAfterSerialization() throws Exception {
        // Populate map
        map.put("x", 100);
        map.put("y", 200);
        // Serialize
        byte[] serialized;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(map);
            serialized = bos.toByteArray();
        }
        // Deserialize
        TestableCompactLinkedMap<String, Integer> deserialized;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
            ObjectInputStream in = new ObjectInputStream(bis)) {
            @SuppressWarnings("unchecked")
            TestableCompactLinkedMap<String, Integer> tmp = (TestableCompactLinkedMap<String, Integer>) in.readObject();
            deserialized = tmp;
        }
        // Verify false on the deserialized instance
        assertFalse(deserialized.callUseCopyIterator(), "useCopyIterator() must remain false after serialization round‑trip");
    }

    @Test
    void useCopyIteratorFalseOnAnonymousSubclassViaReflection() throws Exception {
        // Create an anonymous subclass instance
        CompactLinkedMap<String, Integer> anon = new CompactLinkedMap<String, Integer>() {
        };
        // Access protected method via reflection
        Method m = CompactLinkedMap.class.getDeclaredMethod("useCopyIterator");
        m.setAccessible(true);
        Object result = m.invoke(anon);
        assertTrue(result instanceof Boolean);
        assertFalse((Boolean) result, "Anonymous subclass should inherit false from the protected method");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("reflectionExceptionProvider")
    void reflectionInvocationThrowsExpectedException(ReflectionExceptionCase tc) throws Exception {
        Method m = getUseCopyIteratorMethod();
        if (tc.setAccessible) {
            m.setAccessible(true);
        } else {
            // Explicitly ensure it is not accessible
            m.setAccessible(false);
        }
        // Act & Assert
        Exception thrown = assertThrows(tc.expectedException, () -> {
            if (tc.arguments == null) {
                m.invoke(tc.instance);
            } else {
                m.invoke(tc.instance, tc.arguments);
            }
        }, tc.description);
        assertNotNull(thrown);
    }

    /**
     * Test that a SecurityManager denying ReflectPermission results in a SecurityException.
     * The test installs a custom SecurityManager, attempts reflective access, and finally
     * restores the original manager.
     */
    @Test
    void reflectionBlockedBySecurityManagerThrowsSecurityException() throws Exception {
        // Save the original manager to restore later
        SecurityManager original = System.getSecurityManager();
        // Install a restrictive manager that throws on ReflectPermission checks
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything except ReflectPermission
                if (perm instanceof java.lang.reflect.ReflectPermission) {
                    throw new SecurityException("ReflectPermission denied by test SecurityManager");
                }
            }
        });
        try {
            Method m = getUseCopyIteratorMethod();
            // Even though we set it accessible, the SecurityManager should block it
            m.setAccessible(true);
            CompactLinkedMap<String, Integer> instance = new CompactLinkedMap<>();
            // Expect SecurityException when invoking
            assertThrows(SecurityException.class, () -> m.invoke(instance), "SecurityException expected when ReflectPermission is denied");
        } finally {
            // Restore the original SecurityManager to avoid side‑effects on other tests
            System.setSecurityManager(original);
        }
    }
}
