package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_clear_Test {

    private CaseInsensitiveSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test.
        set = new CaseInsensitiveSet<>();
        // Populate it with a few values to exercise the clear operation.
        set.addAll(Arrays.asList("Alpha", "Bravo", "charlie"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no cross‑test contamination.
        set = null;
    }

    @Test
    @DisplayName("clear() on a non‑empty set empties the set")
    void clearNonEmptySet() {
        // Given: set is populated in @BeforeEach (size = 3)
        assertFalse(set.isEmpty(), "Precondition: set should not be empty before clear");
        assertEquals(3, set.size(), "Precondition: set should contain three elements");
        // When
        set.clear();
        // Then
        assertTrue(set.isEmpty(), "Set should be empty after clear()");
        assertEquals(0, set.size(), "Size should be zero after clear()");
        // Verify that previously added elements are no longer present.
        assertFalse(set.contains("Alpha"));
        assertFalse(set.contains("BRAVO"));
        assertFalse(set.contains("charlie"));
    }

    @Test
    @DisplayName("clear() on an already empty set remains empty")
    void clearAlreadyEmptySet() {
        // Given: start with an empty set
        CaseInsensitiveSet<String> emptySet = new CaseInsensitiveSet<>();
        assertTrue(emptySet.isEmpty(), "Precondition: set should be empty");
        // When
        emptySet.clear();
        // Then
        assertTrue(emptySet.isEmpty(), "Set should remain empty after clear()");
        assertEquals(0, emptySet.size(), "Size should stay zero after clear()");
    }

    @Test
    @DisplayName("set continues to be case‑insensitive after clear()")
    void caseInsensitiveAfterClear() {
        // When
        set.clear();
        // Then: adding strings with different cases should be considered equal.
        assertTrue(set.add("Delta"));
        // Adding the same word with different case should not increase size.
        assertFalse(set.add("delta"));
        assertEquals(1, set.size());
        assertTrue(set.contains("DELTA"));
    }

    @Test
    @DisplayName("size is 0 after clear() on set populated via addAll()")
    void sizeZeroAfterClearWithAddAll() {
        // set already populated via @BeforeEach using addAll
        assertEquals(3, set.size());
        set.clear();
        assertEquals(0, set.size(), "Size must be zero after clear()");
        assertTrue(set.isEmpty(), "Set must be empty after clear()");
    }

    @Test
    @DisplayName("iterator obtained before clear() reports no elements after clear()")
    void iteratorBeforeClearSeesEmpty() {
        Iterator<String> it = set.iterator();
        // Ensure iterator initially has elements
        assertTrue(it.hasNext());
        set.clear();
        // After clear, the iterator should not have any remaining elements.
        assertFalse(it.hasNext(), "Iterator should report no elements after the underlying set is cleared");
    }

    @Test
    @DisplayName("set does not contain null after clear() when null was present")
    void clearRemovesNullElement() {
        // The underlying implementation may or may not allow null; we test the contract.
        set.add(null);
        assertTrue(set.contains(null), "Set should contain null after adding it");
        set.clear();
        assertEquals(0, set.size(), "Size should be zero after clear()");
        assertFalse(set.contains(null), "Set should not contain null after clear()");
    }

    @Test
    @DisplayName("no ConcurrentModificationException when iterating after clear()")
    void noConcurrentModificationExceptionAfterClear() {
        // Obtain iterator after clear to ensure safe iteration.
        set.clear();
        // Adding elements after clear and iterating should not throw CME.
        set.addAll(Arrays.asList("One", "Two", "Three"));
        assertDoesNotThrow(() -> {
            for (String s : set) {
                // No operation, just iterate.
            }
        }, "Iterating after clear() should not throw ConcurrentModificationException");
    }

    @Test
    @DisplayName("set remains empty after concurrent clear() calls from multiple threads")
    void concurrentClearCalls() throws InterruptedException, ExecutionException {
        // Populate with many elements to make the operation visible.
        set.clear();
        set.addAll(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"));
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                set.clear();
                return null;
            });
        }
        // Execute all clear tasks concurrently.
        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> f : futures) {
            // rethrow any exception
            f.get();
        }
        executor.shutdownNow();
        assertTrue(set.isEmpty(), "Set should be empty after concurrent clear() calls");
        assertEquals(0, set.size(), "Size must be zero after concurrent clear()");
    }

    @Test
    @DisplayName("clear() throws NullPointerException when internal map is null")
    void clearThrowsNPEWhenMapIsNull() throws Exception {
        CaseInsensitiveSet<String> faultySet = new CaseInsensitiveSet<>();
        // Use reflection to set the private 'map' field to null.
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(faultySet, null);
        // Expect NPE because clear() will dereference the null map.
        assertThrows(NullPointerException.class, faultySet::clear, "clear() should throw NullPointerException when internal map is null");
    }

    @Test
    @DisplayName("clear() propagates UnsupportedOperationException from an unmodifiable map")
    void clearPropagatesUnsupportedOperationException() throws Exception {
        CaseInsensitiveSet<String> customSet = new CaseInsensitiveSet<>();
        // Replace internal map with an unmodifiable map.
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        Map<String, Object> unmodifiable = Collections.unmodifiableMap(new ConcurrentSkipListMap<>());
        mapField.set(customSet, unmodifiable);
        // clear() should attempt to clear the unmodifiable map and thus throw.
        assertThrows(UnsupportedOperationException.class, customSet::clear, "clear() should propagate UnsupportedOperationException from the underlying map");
    }
}
