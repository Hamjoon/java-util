package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Method;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class CompactCIHashSet_getNewSet_Test {

    private CompactCIHashSet<String> ciHashSet;

    /**
     * Simple concrete subclass used in all test scenarios.
     */
    private static class ConcreteCIHashSet extends CompactCIHashSet<String> {

        private final boolean caseInsensitive;

        ConcreteCIHashSet(boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
        }

        @Override
        protected boolean isCaseInsensitive() {
            return caseInsensitive;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default instance – case‑insensitivity does not affect getNewSet()
        ciHashSet = new ConcreteCIHashSet(true);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC (not strictly required)
        ciHashSet = null;
    }

    @Test
    @DisplayName("getNewSet should return null as documented")
    void testGetNewSetReturnsNull() {
        // When
        // The method is protected; we can invoke it via reflection because the test class is in the same package.
        // This respects the rule of not accessing private members; protected is accessible within the same package.
        java.lang.reflect.Method method;
        try {
            method = CompactCIHashSet.class.getDeclaredMethod("getNewSet");
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<String> result = (Set<String>) method.invoke(ciHashSet);
            // Then
            assertNull(result, "getNewSet should return null");
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    /**
     * Helper that invokes the protected {@code getNewSet()} method via reflection.
     */
    @SuppressWarnings("unchecked")
    private Set<String> invokeGetNewSet(CompactCIHashSet<String> set) {
        try {
            Method method = CompactCIHashSet.class.getDeclaredMethod("getNewSet");
            method.setAccessible(true);
            return (Set<String>) method.invoke(set);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
            // unreachable
            return null;
        }
    }

    @Test
    @DisplayName("Calling getNewSet does not throw any exception")
    void testGetNewSetDoesNotThrow() {
        assertDoesNotThrow(() -> invokeGetNewSet(ciHashSet), "getNewSet should complete without throwing");
    }

    @Test
    @DisplayName("State independence – non‑empty set")
    void testGetNewSetStateIndependenceNonEmpty() {
        // Populate the set
        ciHashSet.add("One");
        ciHashSet.add("Two");
        ciHashSet.add("Three");
        int sizeBefore = ciHashSet.size();
        Set<String> result = invokeGetNewSet(ciHashSet);
        assertNull(result, "getNewSet must still return null");
        assertEquals(sizeBefore, ciHashSet.size(), "Set size must remain unchanged after getNewSet call");
    }

    @Test
    @DisplayName("State independence – after removals")
    void testGetNewSetStateIndependenceAfterRemovals() {
        ciHashSet.add("Alpha");
        ciHashSet.add("Beta");
        ciHashSet.remove("Alpha");
        // could be 0 or 1
        int sizeBefore = ciHashSet.size();
        Set<String> result = invokeGetNewSet(ciHashSet);
        assertNull(result, "getNewSet must return null irrespective of current size");
        assertEquals(sizeBefore, ciHashSet.size(), "Set size must stay the same after getNewSet call");
    }

    @ParameterizedTest(name = "isCaseInsensitive = {0}")
    @ValueSource(booleans = { true, false })
    @DisplayName("getNewSet returns null regardless of case‑sensitivity flag")
    void testGetNewSetIgnoresCaseSensitivityFlag(boolean caseInsensitive) {
        CompactCIHashSet<String> customSet = new ConcreteCIHashSet(caseInsensitive);
        Set<String> result = invokeGetNewSet(customSet);
        assertNull(result, "getNewSet should return null for caseInsensitive=" + caseInsensitive);
    }
}
