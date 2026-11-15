package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Comparator;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_isEmpty_Test {

    /**
     * Instance under test for the default‑constructor scenarios.
     */
    private ConcurrentNavigableSetNullSafe<String> defaultSet;

    /**
     * Instance under test for the comparator‑constructor scenarios.
     */
    private ConcurrentNavigableSetNullSafe<String> comparatorSet;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh sets for each test to guarantee isolation.
        defaultSet = new ConcurrentNavigableSetNullSafe<>();
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        comparatorSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no stray references survive between tests.
        defaultSet = null;
        comparatorSet = null;
    }

    @Test
    void isEmpty_ShouldReturnTrue_ForNewDefaultSet() {
        assertTrue(defaultSet.isEmpty(), "A newly created set must be empty");
    }

    @Test
    void isEmpty_ShouldReturnFalse_AfterAddingNonNullElement() {
        defaultSet.add("apple");
        assertFalse(defaultSet.isEmpty(), "Set should not be empty after a non‑null element is added");
    }

    @Test
    void isEmpty_ShouldReturnFalse_AfterAddingNullElement() {
        defaultSet.add(null);
        assertFalse(defaultSet.isEmpty(), "Set should not be empty after a null element is added");
    }

    @Test
    void isEmpty_ShouldReturnTrue_AfterClear() {
        defaultSet.add("banana");
        defaultSet.add(null);
        defaultSet.clear();
        assertTrue(defaultSet.isEmpty(), "Set should be empty after clear() is invoked");
    }

    @Test
    void isEmpty_ShouldReturnTrue_ForNewComparatorSet() {
        assertTrue(comparatorSet.isEmpty(), "A newly created comparator‑based set must be empty");
    }

    @Test
    void isEmpty_ShouldReturnFalse_AfterAddingElements_WithComparator() {
        comparatorSet.add("kiwi");
        comparatorSet.add(null);
        assertFalse(comparatorSet.isEmpty(), "Set should not be empty after adding elements (including null)");
    }

    @Test
    void isEmpty_ShouldReturnTrue_AfterAddingAndClearing_WithComparator() {
        comparatorSet.add("apple");
        comparatorSet.add("banana");
        comparatorSet.clear();
        assertTrue(comparatorSet.isEmpty(), "Set should be empty after clear() when a comparator is used");
    }

    /**
     * isEmpty should return true when the underlying set is empty.
     */
    @Test
    void isEmpty_WhenUnderlyingSetIsEmpty_ShouldReturnTrue() {
        // defaultSet is freshly created and therefore empty.
        assertTrue(defaultSet.isEmpty(), "When the underlying internalSet is empty, isEmpty() should return true.");
    }

    /**
     * isEmpty should return false when the underlying set contains an element.
     */
    @Test
    void isEmpty_WhenUnderlyingSetHasElement_ShouldReturnFalse() {
        defaultSet.add("element");
        assertFalse(defaultSet.isEmpty(), "When the underlying internalSet contains at least one element, isEmpty() should return false.");
    }

    /**
     * After clear() the set must be empty.
     */
    @Test
    void isEmptyAfterClear_ShouldReturnTrue() {
        defaultSet.add("one");
        defaultSet.add("two");
        defaultSet.clear();
        assertTrue(defaultSet.isEmpty(), "After clear() the set should be empty.");
    }

    /**
     * Add then remove the same element – the set should be empty again.
     */
    @Test
    void isEmptyAfterAddRemove_ShouldReturnTrue() {
        defaultSet.add("temp");
        defaultSet.remove("temp");
        assertTrue(defaultSet.isEmpty(), "After adding and then removing the same element, the set should be empty.");
    }

    /**
     * Remove only one of three elements – set should still be non‑empty.
     */
    @Test
    void isEmptyAfterPartialRemoval_ShouldReturnFalse() {
        defaultSet.add("a");
        defaultSet.add("b");
        defaultSet.add("c");
        defaultSet.remove("b");
        assertFalse(defaultSet.isEmpty(), "After removing only one element from three, the set should not be empty.");
    }

    /**
     * Remove all three elements – set must be empty.
     */
    @Test
    void isEmptyAfterFullRemoval_ShouldReturnTrue() {
        defaultSet.add("x");
        defaultSet.add("y");
        defaultSet.add("z");
        defaultSet.remove("x");
        defaultSet.remove("y");
        defaultSet.remove("z");
        assertTrue(defaultSet.isEmpty(), "After removing all elements, the set should be empty.");
    }

    /**
     * Concurrent add and remove of the same element – final state should be empty.
     */
    @Test
    void isEmptyConcurrentAddRemove_ShouldReturnTrue() throws InterruptedException {
        final String element = "concurrent";
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        // Thread 1: add element
        executor.submit(() -> {
            try {
                // wait for both threads to be ready
                startLatch.await();
                defaultSet.add(element);
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        // Thread 2: remove element
        executor.submit(() -> {
            try {
                startLatch.await();
                // Ensure remove runs after add – a tiny sleep helps in most environments
                Thread.sleep(10);
                defaultSet.remove(element);
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        // Release both threads
        startLatch.countDown();
        // Wait for both to finish
        doneLatch.await();
        executor.shutdownNow();
        assertTrue(defaultSet.isEmpty(), "After concurrent add and remove of the same element, the set should be empty.");
    }

    /**
     * Repeated clear → add → clear → add sequence leaves the set non‑empty.
     */
    @Test
    void isEmptyAfterRepeatedClearAndAdd_ShouldReturnFalse() {
        // initial clear (no‑op)
        defaultSet.clear();
        defaultSet.add("first");
        // clear again
        defaultSet.clear();
        // final add
        defaultSet.add("second");
        assertFalse(defaultSet.isEmpty(), "After the sequence clear → add → clear → add, the set should not be empty.");
    }
}
