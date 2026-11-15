package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.CountDownLatch;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

class ConcurrentSet_remove_Test {

    private ConcurrentSet<Integer> intSet;

    private ConcurrentSet<String> stringSet;

    @BeforeEach
    void setupBeforeEach() {
        intSet = new ConcurrentSet<>();
        stringSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        intSet = null;
        stringSet = null;
    }

    /**
     * Data provider for basic functionality: present vs. absent element.
     */
    static Stream<Arguments> basicRemoveProvider() {
        return Stream.of(// element present – expect true
        Arguments.of("apple", true, "The set contains \"apple\"; removal should succeed."), // element absent – expect false
        Arguments.of("banana", false, "The set does not contain \"banana\"; removal should fail."));
    }

    /**
     * Data provider for null‑element handling.
     */
    static Stream<Arguments> nullRemoveProvider() {
        return Stream.of(// null present – expect true
        Arguments.of(true, "Set contains null; removal should succeed."), // null absent – expect false
        Arguments.of(false, "Set does not contain null; removal should fail."));
    }

    /**
     * Verify that removing an existing element returns true and actually removes it.
     */
    @Test
    void testRemoveExistingElement() {
        assertTrue(intSet.add(42), "Adding a new element should return true");
        assertTrue(intSet.contains(42), "Set must contain the added element");
        assertEquals(1, intSet.size(), "Size should be 1 after addition");
        assertTrue(intSet.remove(42), "Removing an existing element should return true");
        assertFalse(intSet.contains(42), "Element must no longer be present after removal");
        assertEquals(0, intSet.size(), "Set should be empty after removal");
    }

    /**
     * Removing an element that is not present must return false and leave the set unchanged.
     */
    @Test
    void testRemoveNonExistingElement() {
        assertFalse(intSet.remove(99), "Removing a non‑existent element should return false");
        assertTrue(intSet.isEmpty(), "Set should still be empty");
        intSet.addAll(Arrays.asList(1, 2, 3));
        assertEquals(3, intSet.size(), "Pre‑condition check");
        assertFalse(intSet.remove(4), "Removing a value that was never added should return false");
        assertEquals(3, intSet.size(), "Size must remain unchanged after failed removal");
    }

    /**
     * The set supports a null element via an internal sentinel – removal must work for null.
     */
    @Test
    void testRemoveNullElement() {
        assertTrue(stringSet.add(null), "Adding null should succeed");
        assertTrue(stringSet.contains(null), "Set must contain null after addition");
        assertEquals(1, stringSet.size(), "Size should be 1 after adding null");
        assertTrue(stringSet.remove(null), "Removing null should return true");
        assertFalse(stringSet.contains(null), "Null must no longer be present");
        assertTrue(stringSet.isEmpty(), "Set should be empty after null removal");
    }

    /**
     * Removing a collection element reuses the remove(Object) implementation – ensure correct result.
     */
    @Test
    void testRemoveAllUsesRemove() {
        List<Integer> initial = Arrays.asList(10, 20, 30, 40);
        intSet.addAll(initial);
        assertEquals(4, intSet.size());
        // removeAll internally calls remove(Object) for each element
        assertTrue(intSet.removeAll(Arrays.asList(20, 40)), "removeAll should report modification");
        assertFalse(intSet.contains(20));
        assertFalse(intSet.contains(40));
        assertTrue(intSet.containsAll(Arrays.asList(10, 30)));
        assertEquals(2, intSet.size());
    }

    /**
     * Concurrent scenario: many threads add then remove the same range of values.
     */
    @Test
    void testConcurrentRemove() throws InterruptedException {
        int threadCount = 4;
        int opsPerThread = 5000;
        CountDownLatch latch = new CountDownLatch(threadCount * 2);
        // Threads that add numbers 0..opsPerThread‑1
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    intSet.add(j);
                }
                latch.countDown();
            }).start();
            // Threads that remove the same numbers
            new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    intSet.remove(j);
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        // After all adds and removes, the size must be between 0 and opsPerThread
        int size = intSet.size();
        assertTrue(size >= 0 && size <= opsPerThread, "Final size should be in the range [0, " + opsPerThread + "], but was " + size);
    }

    /**
     * Verify that remove works correctly after a mixture of adds, removes and retains.
     */
    @Test
    void testRemoveAfterMixedOperations() {
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        assertTrue(intSet.remove(3), "Removing an existing element must return true");
        assertFalse(intSet.remove(3), "Removing the same element again must return false");
        assertTrue(intSet.retainAll(Arrays.asList(1, 2, 5, 6)), "retainAll should succeed");
        assertEquals(3, intSet.size());
        assertTrue(intSet.containsAll(Arrays.asList(1, 2, 5)));
        assertFalse(intSet.contains(4), "Element 4 should have been removed by retainAll");
    }

    /**
     * Ensure that remove works when the set was constructed from an existing collection.
     */
    @Test
    void testRemoveFromConstructedSet() {
        List<String> source = Arrays.asList("alpha", "beta", "gamma");
        ConcurrentSet<String> constructed = new ConcurrentSet<>(source);
        assertEquals(3, constructed.size());
        assertTrue(constructed.remove("beta"));
        assertFalse(constructed.contains("beta"));
        assertEquals(2, constructed.size());
        assertFalse(constructed.remove("delta"));
        assertEquals(2, constructed.size());
    }

    @ParameterizedTest(name = "[basic] remove({0}) → expected {1}")
    @MethodSource("basicRemoveProvider")
    void testRemoveBasicFunctionality(String element, boolean expectedResult, String description) {
        // Arrange – a set that contains only "apple"
        ConcurrentSet<String> set = new ConcurrentSet<>();
        // ensure the present case is covered
        set.add("apple");
        // Act
        boolean result = set.remove(element);
        // Assert
        assertEquals(expectedResult, result, description);
        // Additional sanity checks
        if (expectedResult) {
            assertFalse(set.contains(element), "Element should no longer be present after successful removal.");
        } else {
            assertTrue(set.contains("apple"), "Unrelated element must still be present.");
        }
    }

    @ParameterizedTest(name = "[null] remove(null) → expected {0}")
    @MethodSource("nullRemoveProvider")
    void testRemoveNullHandling(boolean setContainsNull, String description) {
        // Arrange
        ConcurrentSet<String> set = new ConcurrentSet<>();
        if (setContainsNull) {
            set.add(null);
        }
        // Act
        boolean result = set.remove(null);
        // Assert
        assertEquals(setContainsNull, result, description);
        assertFalse(set.contains(null), "After removal attempt, null must not be present.");
    }

    /**
     * Verify that passing an object of the wrong generic type throws ClassCastException.
     */
    @Test
    void testRemoveTypeMismatchThrowsClassCastException() {
        // Arrange: a set of Integer
        ConcurrentSet<Integer> integerSet = new ConcurrentSet<>();
        integerSet.add(1);
        integerSet.add(2);
        // Act & Assert
        assertThrows(ClassCastException.class, () -> integerSet.remove("someString"), "Removing a String from a ConcurrentSet<Integer> must throw ClassCastException.");
    }
}
