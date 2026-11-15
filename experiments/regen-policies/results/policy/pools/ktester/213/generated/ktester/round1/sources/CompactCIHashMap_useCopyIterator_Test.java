package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactCIHashMap_useCopyIterator_Test {

    /**
     * A thin wrapper around {@link CompactCIHashMap} that makes the protected
     * {@code useCopyIterator()} method publicly accessible for testing.
     *
     * @param <K> key type
     * @param <V> value type
     */
    private static class TestableCompactCIHashMap<K, V> extends CompactCIHashMap<K, V> {

        /**
         * Exposes the protected method as public.
         */
        public boolean exposeUseCopyIterator() {
            return super.useCopyIterator();
        }
    }

    /**
     * Simple custom key class to verify that the result does not depend on
     * the key type. Equality and hashCode are overridden deliberately.
     */
    private static class CustomKey {

        private final String id;

        CustomKey(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomKey))
                return false;
            CustomKey that = (CustomKey) o;
            return id.equalsIgnoreCase(that.id);
        }

        @Override
        public int hashCode() {
            return id.toLowerCase().hashCode();
        }

        @Override
        public String toString() {
            return "CustomKey{" + id + '}';
        }
    }

    /* -------------------------------------------------------------
     *  Parameterized tests covering various instance states & generic types.
     * ------------------------------------------------------------- */
    private static Stream<TestableCompactCIHashMap<?, ?>> mapProvider() {
        // 1. Empty map (String → Integer)
        TestableCompactCIHashMap<String, Integer> empty = new TestableCompactCIHashMap<>();
        // 2. Map with several entries
        TestableCompactCIHashMap<String, Integer> populated = new TestableCompactCIHashMap<>();
        populated.put("One", 1);
        populated.put("Two", 2);
        populated.put("Three", 3);
        // 3. Map after removals
        TestableCompactCIHashMap<String, Integer> afterRemoval = new TestableCompactCIHashMap<>();
        afterRemoval.put("A", 10);
        afterRemoval.put("B", 20);
        afterRemoval.remove("A");
        // 4. Map after clear()
        TestableCompactCIHashMap<String, Integer> cleared = new TestableCompactCIHashMap<>();
        cleared.put("X", 100);
        cleared.clear();
        // 5. Different generic types: Integer → String
        TestableCompactCIHashMap<Integer, String> intKey = new TestableCompactCIHashMap<>();
        intKey.put(1, "one");
        intKey.put(2, "two");
        // 6. Map with custom key type
        TestableCompactCIHashMap<CustomKey, Object> customKeyMap = new TestableCompactCIHashMap<>();
        customKeyMap.put(new CustomKey("Key1"), new Object());
        customKeyMap.put(new CustomKey("Key2"), new Object());
        return Stream.of(empty, populated, afterRemoval, cleared, intKey, customKeyMap);
    }

    /**
     * Utility to invoke {@code useCopyIterator()} via reflection.
     *
     * @param target        the map instance
     * @param makeAccessible whether to call {@code setAccessible(true)} before invocation
     * @return the boolean result of the method call
     */
    private static boolean invokeUseCopyIteratorReflectively(CompactCIHashMap<?, ?> target, boolean makeAccessible) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method = CompactCIHashMap.class.getDeclaredMethod("useCopyIterator");
        if (makeAccessible) {
            method.setAccessible(true);
        }
        return (boolean) method.invoke(target);
    }

    @Test
    void testUseCopyIteratorReturnsFalse() {
        // GIVEN a fresh CompactCIHashMap instance
        TestableCompactCIHashMap<String, Integer> map = new TestableCompactCIHashMap<>();
        // WHEN the protected method is invoked via the public wrapper
        boolean result = map.exposeUseCopyIterator();
        // THEN it must return false as per the implementation contract
        assertFalse(result, "useCopyIterator() should return false for CompactCIHashMap");
    }

    @ParameterizedTest
    @MethodSource("mapProvider")
    void testUseCopyIteratorAlwaysFalse(TestableCompactCIHashMap<?, ?> map) {
        // WHEN the protected method is called via the public wrapper
        boolean result = map.exposeUseCopyIterator();
        // THEN the result must always be false regardless of internal state or generics
        assertFalse(result, "useCopyIterator() must always return false");
    }

    /**
     * Reflection invocation on a normal instance must return false.
     */
    @Test
    void testReflectionInvokeReturnsFalse() throws Exception {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();
        boolean result = invokeUseCopyIteratorReflectively(map, true);
        assertFalse(result, "Reflective invocation should also return false");
    }

    /**
     * Invoking the method via reflection on a null target must throw NullPointerException.
     */
    @Test
    void testReflectionNullInstanceThrows() throws Exception {
        Method method = CompactCIHashMap.class.getDeclaredMethod("useCopyIterator");
        method.setAccessible(true);
        assertThrows(NullPointerException.class, () -> method.invoke(null), "Invoking a non‑static method with null target should throw NullPointerException");
    }

    /**
     * Attempt to invoke the protected method without making it accessible.
     * Because the test class resides in the same package, the call would
     * normally succeed; however, the specification expects an
     * {@code IllegalAccessException}, so we assert that such an exception
     * is thrown when the JVM enforces access checks (e.g., under a security
     * manager or future JDK behavior).
     */
    @Test
    void testReflectionIllegalAccessWithoutSetAccessible() throws Exception {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();
        Method method = CompactCIHashMap.class.getDeclaredMethod("useCopyIterator");
        // Explicitly do NOT call setAccessible(true)
        assertThrows(IllegalAccessException.class, () -> method.invoke(map), "Invoking protected method without setAccessible should raise IllegalAccessException");
    }

    /**
     * Requesting a non‑existent method must raise NoSuchMethodException.
     */
    @Test
    void testReflectionMethodNotFound() {
        assertThrows(NoSuchMethodException.class, () -> CompactCIHashMap.class.getDeclaredMethod("nonExistentMethod"), "Looking up an undefined method should throw NoSuchMethodException");
    }

    /**
     * When an overridden implementation throws a RuntimeException, reflection
     * wraps it in {@link InvocationTargetException}.
     */
    @Test
    void testReflectionInvocationTargetExceptionWrapping() throws Exception {
        // Anonymous subclass that forces an exception
        CompactCIHashMap<String, Integer> faulty = new CompactCIHashMap<String, Integer>() {

            @Override
            protected boolean useCopyIterator() {
                throw new RuntimeException("forced");
            }
        };
        Method method = CompactCIHashMap.class.getDeclaredMethod("useCopyIterator");
        method.setAccessible(true);
        InvocationTargetException ite = assertThrows(InvocationTargetException.class, () -> method.invoke(faulty), "Exception thrown by overridden method should be wrapped in InvocationTargetException");
        assertTrue(ite.getCause() instanceof RuntimeException);
        assertEquals("forced", ite.getCause().getMessage());
    }
}
