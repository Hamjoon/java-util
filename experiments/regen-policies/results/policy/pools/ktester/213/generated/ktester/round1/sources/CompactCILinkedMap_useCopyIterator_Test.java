package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class CompactCILinkedMap_useCopyIterator_Test {

    private TestableCompactCILinkedMap<String, Integer> mapUnderTest;

    /**
     * Concrete subclass that makes the protected {@code useCopyIterator()} method
     * publicly accessible for testing purposes.
     */
    private static class TestableCompactCILinkedMap<K, V> extends CompactCILinkedMap<K, V> {

        /**
         * Calls the superclass implementation of {@code useCopyIterator()}.
         */
        public boolean callUseCopyIterator() {
            return super.useCopyIterator();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a new map before each test to avoid cross‑test state leakage.
        mapUnderTest = new TestableCompactCILinkedMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the reference; helps GC in larger test suites.
        mapUnderTest = null;
    }

    @Test
    @DisplayName("useCopyIterator() should always return false")
    void testUseCopyIteratorReturnsFalse() {
        // Given: a newly constructed CompactCILinkedMap (via the testable subclass)
        // When: invoking the protected method through the public wrapper
        boolean result = mapUnderTest.callUseCopyIterator();
        // Then: the method must return false as per its contract
        assertFalse(result, "useCopyIterator() should return false");
    }
}
