package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

// ensure deterministic ordering for concurrency test
@Execution(ExecutionMode.SAME_THREAD)
class ConcurrentSet_removeAll_Test {

    private ConcurrentSet<Integer> intSet;

    private ConcurrentSet<String> stringSet;

    @BeforeEach
    void setupBeforeEach() {
        intSet = new ConcurrentSet<>();
        stringSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        intSet = null;
        stringSet = null;
    }

    @Test
    void removeAllFromEmptySetReturnsFalse() {
        // Given
        Collection<Integer> toRemove = Arrays.asList(1, 2, 3);
        // When
        boolean modified = intSet.removeAll(toRemove);
        // Then
        assertFalse(modified, "removeAll should return false when the set is empty");
        assertTrue(intSet.isEmpty(), "Set should remain empty");
    }

    @Test
    void removeAllAllElementsEmptiesSet() {
        // Given
        intSet.addAll(Arrays.asList(1, 2, 3));
        assertEquals(3, intSet.size(), "Pre‑condition: set should contain three elements");
        // When
        boolean modified = intSet.removeAll(Arrays.asList(1, 2, 3));
        // Then
        assertTrue(modified, "removeAll should return true when elements are removed");
        assertTrue(intSet.isEmpty(), "Set should be empty after removing all elements");
    }

    @Test
    void removeAllPartialRemovalReturnsTrue() {
        // Given
        intSet.addAll(Arrays.asList(1, 2, 3));
        // 4 is not present
        Collection<Integer> toRemove = Arrays.asList(2, 4);
        // When
        boolean modified = intSet.removeAll(toRemove);
        // Then
        assertTrue(modified, "removeAll should return true when at least one element is removed");
        assertEquals(2, intSet.size(), "Set should contain two remaining elements");
        assertTrue(intSet.containsAll(Arrays.asList(1, 3)), "Remaining elements should be 1 and 3");
        assertFalse(intSet.contains(2), "Element 2 should have been removed");
    }

    @Test
    void removeAllWithNullElement() {
        // Given
        stringSet.addAll(Arrays.asList(null, "a", "b"));
        Collection<String> toRemove = Arrays.asList(null, "c");
        // When
        boolean modified = stringSet.removeAll(toRemove);
        // Then
        assertTrue(modified, "removeAll should return true when null is removed");
        assertEquals(2, stringSet.size(), "Set should contain two elements after removal");
        assertTrue(stringSet.containsAll(Arrays.asList("a", "b")), "Set should still contain 'a' and 'b'");
        assertFalse(stringSet.contains(null), "Null should have been removed");
    }

    @Test
    void removeAllWhenNullNotPresentReturnsFalse() {
        // Given
        stringSet.addAll(Arrays.asList("a", "b"));
        Collection<String> toRemove = Collections.singletonList(null);
        // When
        boolean modified = stringSet.removeAll(toRemove);
        // Then
        assertFalse(modified, "removeAll should return false when null is not present");
        assertEquals(2, stringSet.size(), "Set size should be unchanged");
    }

    @Test
    void removeAllWithEmptyCollectionReturnsFalse() {
        // Given
        intSet.addAll(Arrays.asList(1, 2));
        Collection<Integer> empty = Collections.emptyList();
        // When
        boolean modified = intSet.removeAll(empty);
        // Then
        assertFalse(modified, "removeAll should return false when the collection to remove is empty");
        assertEquals(2, intSet.size(), "Set size should remain unchanged");
    }

    @Test
    void removeAllIdempotentBehavior() {
        // Given
        intSet.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> toRemove = Arrays.asList(1, 2);
        // When - first call
        boolean first = intSet.removeAll(toRemove);
        // When - second call with same collection
        boolean second = intSet.removeAll(toRemove);
        // Then
        assertTrue(first, "First call should remove elements and return true");
        assertFalse(second, "Second call should return false because nothing new was removed");
        assertEquals(1, intSet.size(), "Only element 3 should remain");
        assertTrue(intSet.contains(3));
    }

    @Test
    void removeAllWithDuplicateElementsInCollection() {
        // Given
        intSet.addAll(Arrays.asList(1, 2, 3));
        // duplicate 2, 4 not present
        Collection<Integer> toRemove = Arrays.asList(2, 2, 4);
        // When
        boolean modified = intSet.removeAll(toRemove);
        // Then
        assertTrue(modified, "removeAll should return true when at least one element is removed");
        assertEquals(2, intSet.size(), "Set should contain two elements after removal");
        assertTrue(intSet.containsAll(Arrays.asList(1, 3)), "Elements 1 and 3 should remain");
        assertFalse(intSet.contains(2), "Element 2 should have been removed");
    }

    @Test
    void removeAllWithNullCollectionThrowsNPE() {
        // Given
        intSet.addAll(Arrays.asList(1, 2, 3));
        // When / Then
        assertThrows(NullPointerException.class, () -> intSet.removeAll(null), "Passing a null collection should throw NullPointerException");
    }

    @Test
    void removeAllWithCollectionEqualToSelfClearsSet() {
        // Given
        intSet.addAll(Arrays.asList(10, 20, 30));
        assertEquals(3, intSet.size());
        // When: the collection argument is the set itself
        boolean modified = intSet.removeAll(intSet);
        // Then
        assertTrue(modified, "removeAll should return true when removing its own elements");
        assertTrue(intSet.isEmpty(), "Set should be empty after removing itself");
    }

    @Test
    void removeAllWithIteratorThrowingRuntimeExceptionPropagates() {
        // Create a mock Collection whose iterator throws on next()
        @SuppressWarnings("unchecked")
        Collection<Integer> badCollection = mock(Collection.class);
        Iterator<Integer> badIterator = mock(Iterator.class);
        when(badCollection.iterator()).thenReturn(badIterator);
        when(badIterator.hasNext()).thenReturn(true);
        when(badIterator.next()).thenThrow(new RuntimeException("Iterator failure"));
        // When / Then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> intSet.removeAll(badCollection), "removeAll should propagate RuntimeException from iterator");
        assertEquals("Iterator failure", ex.getMessage());
    }

    @Test
    void removeAllLargeCollectionPerformanceAndCorrectness() {
        // Populate set with 0..999
        List<Integer> initial = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            initial.add(i);
        }
        intSet.addAll(initial);
        assertEquals(1000, intSet.size());
        // Collection to remove: 500..1499 (500 present, 500 absent)
        List<Integer> toRemove = new ArrayList<>();
        for (int i = 500; i < 1500; i++) {
            toRemove.add(i);
        }
        // When
        boolean modified = intSet.removeAll(toRemove);
        // Then
        assertTrue(modified, "At least one element (500‑999) should be removed");
        assertEquals(500, intSet.size(), "Remaining elements should be 0‑499");
        for (int i = 0; i < 500; i++) {
            assertTrue(intSet.contains(i), "Set should still contain " + i);
        }
        for (int i = 500; i < 1000; i++) {
            assertFalse(intSet.contains(i), "Set should no longer contain " + i);
        }
    }

    @Test
    // guard against deadlock or endless loops
    @Timeout(5)
    void removeAllConcurrentModificationSafety() throws Exception {
        // Prepare a set that initially contains "c"
        stringSet.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(stringSet.contains("c"));
        // Latch to synchronize the start of removal and the concurrent addition
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);
        AtomicBoolean removalResult = new AtomicBoolean(false);
        // Thread that adds "c" again (no effect because set) while removal is in progress
        Thread adder = new Thread(() -> {
            try {
                // wait until removal has started
                startLatch.await();
                // concurrent add
                stringSet.add("c");
                // signal that add is done
                doneLatch.countDown();
            } catch (InterruptedException ignored) {
            }
        });
        adder.start();
        // Perform removeAll in the main thread
        Collection<String> toRemove = Collections.singletonList("c");
        // Signal the adder thread that we are about to start iteration
        startLatch.countDown();
        removalResult.set(stringSet.removeAll(toRemove));
        // Wait for the adder to finish (or timeout)
        doneLatch.await(2, TimeUnit.SECONDS);
        // Verify outcome
        assertTrue(removalResult.get(), "removeAll should return true when element is removed");
        assertFalse(stringSet.contains("c"), "Element 'c' should no longer be present after removal");
        // Ensure no ConcurrentModificationException was thrown (test would have failed)
    }

    @Test
    void removeAllBasicFunctionalityEmptyCollection() {
        // Given a non‑empty set
        intSet.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> empty = Collections.emptyList();
        // When
        boolean result = intSet.removeAll(empty);
        // Then
        assertFalse(result, "Calling removeAll with an empty collection should not modify the set and return false.");
        assertEquals(3, intSet.size(), "Set size should remain unchanged");
    }

    @Test
    void removeAllBasicFunctionalityPartialMatch() {
        // Given
        stringSet.addAll(Arrays.asList("a", "b", "c"));
        Collection<String> toRemove = Arrays.asList("a", "b");
        // When
        boolean result = stringSet.removeAll(toRemove);
        // Then
        assertTrue(result, "When the set contains some of the elements in the supplied collection, removeAll should remove them, modify the set and return true.");
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("c"));
    }

    @Test
    void removeAllBasicFunctionalityNoMatch() {
        // Given
        stringSet.addAll(Arrays.asList("a", "b"));
        Collection<String> toRemove = Arrays.asList("x", "y");
        // When
        boolean result = stringSet.removeAll(toRemove);
        // Then
        assertFalse(result, "When none of the elements in the supplied collection are present in the set, removeAll should leave the set unchanged and return false.");
        assertEquals(2, stringSet.size());
        assertTrue(stringSet.containsAll(Arrays.asList("a", "b")));
    }

    @Test
    void removeAllCollectionWithNullElementWhenSetContainsNull() {
        // Given
        stringSet.addAll(Arrays.asList(null, "x"));
        Collection<String> col = Collections.singletonList(null);
        // When
        boolean result = stringSet.removeAll(col);
        // Then
        assertTrue(result, "If the set holds a null element, removeAll with a collection containing null should remove it and return true.");
        assertFalse(stringSet.contains(null));
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("x"));
    }

    @Test
    void removeAllCollectionWithNullElementWhenSetDoesNotContainNull() {
        // Given
        stringSet.addAll(Arrays.asList("x", "y"));
        Collection<String> col = Collections.singletonList(null);
        // When
        boolean result = stringSet.removeAll(col);
        // Then
        assertFalse(result, "If the set does not hold a null element, removeAll with a collection containing null should return false.");
        assertEquals(2, stringSet.size());
    }

    @Test
    void removeAllDuplicateElementsInCollection() {
        // Given
        stringSet.addAll(Arrays.asList("a", "b", "c"));
        Collection<String> col = Arrays.asList("a", "a", "b");
        // When
        boolean result = stringSet.removeAll(col);
        // Then
        assertTrue(result, "Collection with duplicate entries should still cause each distinct present element to be removed once, and the method should return true if any removal occurred.");
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("c"));
    }
}
