package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactCIHashMap_useCopyIterator_Test {

    private CompactCIHashMap<String, String> map;

    /**
     * Concrete subclass used only for reflection tests (keeps the generic type intact).
     */
    private static class ConcreteCompactCIHashMap extends CompactCIHashMap<String, String> {
        // No additional behavior – inherits everything.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test to guarantee isolation.
        map = new CompactCIHashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly necessary but keeps pattern consistent.
        map = null;
    }

    /**
     * Supplies all reflection edge‑case scenarios defined in the JSON test data.
     */
    private static Stream<ReflectionScenario> reflectionScenarios() {
        return Stream.of(// 1. Null instance → NullPointerException before method execution.
        new ReflectionScenario(null, new Object[] {}, NullPointerException.class, "Invoke useCopyIterator() via reflection on a null reference; expect NPE."), // 2. Illegal access (no setAccessible) → IllegalAccessException.
        new ReflectionScenario(new ConcreteCompactCIHashMap(), new Object[] {}, IllegalAccessException.class, "Attempt to invoke protected useCopyIterator() via reflection without setAccessible(true); expect IllegalAccessException."), // 3. Wrong argument count → IllegalArgumentException.
        new ReflectionScenario(new ConcreteCompactCIHashMap(), new Object[] { true }, IllegalArgumentException.class, "Call useCopyIterator() via reflection with an unexpected argument; expect IllegalArgumentException."));
    }

    @Test
    void useCopyIterator_ShouldReturnFalse() {
        // GIVEN: a newly created CompactCIHashMap (setup in @BeforeEach)
        // WHEN: the protected useCopyIterator method is invoked directly.
        // The test class resides in the same package, so protected visibility allows direct access.
        boolean result = map.useCopyIterator();
        // THEN: the method must return false as per its implementation.
        assertFalse(result, "useCopyIterator() should return false for CompactCIHashMap");
    }

    @Test
    void useCopyIterator_ReflectionDirectCall_ShouldReturnFalse() throws Exception {
        // GIVEN: a concrete instance and a Method object for the protected method.
        ConcreteCompactCIHashMap instance = new ConcreteCompactCIHashMap();
        Method method = CompactCIHashMap.class.getDeclaredMethod("useCopyIterator");
        // bypass Java access checks
        method.setAccessible(true);
        // WHEN: the method is invoked via reflection.
        Object result = method.invoke(instance);
        // THEN: the returned value must be Boolean.FALSE.
        assertTrue(result instanceof Boolean, "Result should be a Boolean");
        assertFalse((Boolean) result, "useCopyIterator() via reflection should return false");
    }

    @ParameterizedTest(name = "{index}: {0.description}")
    @MethodSource("reflectionScenarios")
    void useCopyIterator_ReflectionEdgeCases(ReflectionScenario scenario) throws Exception {
        // GIVEN: Method object for the protected method.
        Method method = CompactCIHashMap.class.getDeclaredMethod("useCopyIterator");
        // Do NOT call setAccessible(true) for the IllegalAccessException case.
        // For other cases the default accessibility is sufficient.
        // WHEN / THEN: invoking the method should throw the expected exception.
        Executable invocation = () -> {
            // Reflection API wraps checked exceptions in InvocationTargetException.
            // We unwrap to assert the underlying cause matches the expected type.
            try {
                method.invoke(scenario.instance, scenario.args);
            } catch (InvocationTargetException ite) {
                // Rethrow the target exception to be caught by assertThrows.
                throw ite.getTargetException();
            }
        };
        Throwable thrown = assertThrows(scenario.expectedException, invocation, "Expected " + scenario.expectedException.getSimpleName() + " for scenario: " + scenario.description);
        // Additional sanity check: ensure the thrown exception type matches exactly.
        assertEquals(scenario.expectedException, thrown.getClass(), "Thrown exception type should be exactly " + scenario.expectedException.getSimpleName());
    }
}
