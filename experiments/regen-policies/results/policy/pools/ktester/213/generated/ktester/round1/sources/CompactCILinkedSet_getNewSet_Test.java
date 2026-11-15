package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

class CompactCILinkedSet_getNewSet_Test {

    /**
     * Instance used in the tests.
     */
    private CompactCILinkedSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor – it is the simplest way to obtain an instance.
        set = new CompactCILinkedSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly necessary but keeps the pattern consistent.
        set = null;
    }

    /**
     * Provides instances of {@link CompactCILinkedSet} with different generic types
     * for the reflection‑based test.
     */
    private static Stream<CompactCILinkedSet<?>> reflectionSuccessProvider() {
        return Stream.of(new CompactCILinkedSet<String>(), new CompactCILinkedSet<Integer>());
    }

    @Test
    @DisplayName("getNewSet() should return null")
    void testGetNewSetReturnsNull() {
        // When
        Set<String> result = set.getNewSet();
        // Then
        assertNull(result, "CompactCILinkedSet#getNewSet() is expected to return null");
    }

    @Test
    @DisplayName("getNewSet() does not throw any exception")
    void testGetNewSetDoesNotThrow() {
        assertDoesNotThrow(() -> set.getNewSet(), "Calling getNewSet() should never throw an exception");
    }

    @ParameterizedTest(name = "Reflection on {0} should return null")
    @MethodSource("reflectionSuccessProvider")
    @DisplayName("Protected getNewSet() via reflection returns null (generic‑type independent)")
    void testGetNewSetViaReflectionReturnsNull(CompactCILinkedSet<?> instance) throws Exception {
        // Obtain the protected method via reflection
        Method method = CompactCILinkedSet.class.getDeclaredMethod("getNewSet");
        // allow protected access
        method.setAccessible(true);
        // Invoke on the supplied instance
        Object result = method.invoke(instance);
        // Verify that the returned value is null
        assertNull(result, "Reflective call to getNewSet() must return null");
    }

    @Test
    @DisplayName("Reflective call without setAccessible(true) throws IllegalAccessException")
    void testGetNewSetReflectionIllegalAccess() throws Exception {
        Method method = CompactCILinkedSet.class.getDeclaredMethod("getNewSet");
        // Do NOT call method.setAccessible(true);
        CompactCILinkedSet<String> instance = new CompactCILinkedSet<>();
        // The reflective invocation should fail with IllegalAccessException wrapped in InvocationTargetException
        InvocationTargetException ite = assertThrows(InvocationTargetException.class, () -> method.invoke(instance), "Invocation should fail because the method is not accessible");
        assertTrue(ite.getCause() instanceof IllegalAccessException, "Root cause must be IllegalAccessException");
    }

    @Test
    @DisplayName("Reflective call with null target throws NullPointerException")
    void testGetNewSetReflectionNullTarget() throws Exception {
        Method method = CompactCILinkedSet.class.getDeclaredMethod("getNewSet");
        method.setAccessible(true);
        // Passing null as the target instance should result in NPE
        NullPointerException npe = assertThrows(NullPointerException.class, () -> method.invoke(null), "Invoking a non‑static method with a null target must throw NullPointerException");
        assertNotNull(npe);
    }

    @Test
    @DisplayName("Reflective call with unexpected arguments throws IllegalArgumentException")
    void testGetNewSetReflectionIllegalArgument() throws Exception {
        Method method = CompactCILinkedSet.class.getDeclaredMethod("getNewSet");
        method.setAccessible(true);
        CompactCILinkedSet<Integer> instance = new CompactCILinkedSet<>();
        // Supplying an argument array that does not match the method signature
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> method.invoke(instance, "unexpected"), "Providing arguments to a no‑arg method must throw IllegalArgumentException");
        assertNotNull(iae);
    }
}
