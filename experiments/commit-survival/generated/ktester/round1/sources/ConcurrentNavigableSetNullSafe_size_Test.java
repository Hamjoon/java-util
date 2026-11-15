package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_size_Test {

    /**
     * Set used in most tests – created fresh before each test method.
     */
    private NavigableSet<String> set;

    /**
     * Comparator that orders strings by length (null is treated as length 0).
     */
    private static final Comparator<String> LENGTH_COMPARATOR = Comparator.comparingInt(s -> s == null ? 0 : s.length());

    @BeforeEach
    void setupBeforeEach() {
        // Default constructor gives a natural‑ordering set that can hold nulls.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared so that no stray state leaks between tests.
        set.clear();
    }

    @Test
    void sizeEmptySet() {
        assertEquals(0, set.size(), "A newly created set must report size 0");
        assertTrue(set.isEmpty(), "A newly created set must be empty");
    }

    @Test
    void sizeAfterAddingElementsIncludingNull() {
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add(null));
        assertTrue(set.add("cherry"));
        // Four distinct elements have been added (null counts as an element).
        assertEquals(4, set.size(), "Size should reflect all added elements, including null");
        assertTrue(set.containsAll(Arrays.asList("apple", "banana", "cherry", null)));
    }

    @Test
    void sizeAfterRemovingElements() {
        set.addAll(Arrays.asList("one", "two", "three", null));
        assertEquals(4, set.size());
        // Remove a non‑null element
        assertTrue(set.remove("two"));
        // Remove null
        assertTrue(set.remove(null));
        assertEquals(2, set.size(), "Size must decrease after removals");
        assertFalse(set.contains("two"));
        assertFalse(set.contains(null));
    }

    @Test
    void sizeAfterClear() {
        set.addAll(Arrays.asList("x", "y", null));
        assertEquals(3, set.size());
        set.clear();
        assertEquals(0, set.size(), "Size should be 0 after clear()");
        assertTrue(set.isEmpty());
    }

    @Test
    void sizeUsingCollectionConstructor() {
        Collection<String> source = Arrays.asList("alpha", "beta", "gamma", null, "beta");
        // Duplicate "beta" should be ignored by the set semantics.
        NavigableSet<String> collSet = new ConcurrentNavigableSetNullSafe<>(source);
        // Expected distinct elements: alpha, beta, gamma, null
        assertEquals(4, collSet.size());
        assertTrue(collSet.containsAll(Arrays.asList("alpha", "beta", "gamma", null)));
    }

    @Test
    void sizeUsingComparatorConstructor() {
        NavigableSet<String> compSet = new ConcurrentNavigableSetNullSafe<>(LENGTH_COMPARATOR);
        compSet.addAll(Arrays.asList("a", "bb", "ccc", null));
        // Four distinct elements, null counts as an element.
        assertEquals(4, compSet.size());
        // Verify ordering does not affect size.
        Iterator<String> it = compSet.iterator();
        // length 0
        assertEquals(null, it.next());
        // length 1
        assertEquals("a", it.next());
        // length 2
        assertEquals("bb", it.next());
        // length 3
        assertEquals("ccc", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void sizeUnderConcurrentAdds() throws InterruptedException, ExecutionException {
        // Use the default constructor – it is thread‑safe.
        NavigableSet<Integer> concurrentSet = new ConcurrentNavigableSetNullSafe<>();
        int threadCount = 8;
        // 8 * 250 = 2000 total distinct values
        int perThread = 250;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int base = i * perThread;
            tasks.add(() -> {
                for (int j = 0; j < perThread; j++) {
                    concurrentSet.add(base + j);
                }
                return null;
            });
        }
        // Submit and wait for all tasks to finish.
        List<Future<Void>> futures = exec.invokeAll(tasks);
        for (Future<Void> f : futures) {
            // propagate any exception
            f.get();
        }
        exec.shutdownNow();
        assertEquals(threadCount * perThread, concurrentSet.size(), "Size must equal the total number of distinct elements added concurrently");
        // Spot‑check a few values.
        assertTrue(concurrentSet.contains(0));
        assertTrue(concurrentSet.contains(threadCount * perThread - 1));
    }

    @Test
    void sizeAfterAddingDuplicateElements() {
        assertTrue(set.add("dup"));
        assertTrue(set.add(null));
        // Adding duplicates should not increase size.
        assertFalse(set.add("dup"));
        assertFalse(set.add(null));
        assertEquals(2, set.size(), "Size must not increase when duplicate elements are added");
    }

    @Test
    @DisplayName("size() after adding a single element")
    void sizeSingleElement() {
        assertTrue(set.add("A"));
        assertEquals(1, set.size(), "A set containing a single element must report size 1");
    }

    @Test
    @DisplayName("size() with multiple distinct integer elements")
    void sizeMultipleDistinctIntegers() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(5, intSet.size(), "Size should equal the number of distinct elements");
    }

    @Test
    @DisplayName("size() after adding three elements via add(E)")
    void sizeAfterThreeAdds() {
        assertTrue(set.add("first"));
        assertTrue(set.add("second"));
        assertTrue(set.add("third"));
        assertEquals(3, set.size(), "Size must reflect three added elements");
    }

    @Test
    @DisplayName("size() after clear() on a populated set")
    void sizeAfterClearPopulatedSet() {
        set.addAll(Arrays.asList("a", "b", "c"));
        assertEquals(3, set.size());
        set.clear();
        assertEquals(0, set.size(), "Size must be 0 after clear()");
    }

    @Test
    @DisplayName("size() when internalSet is null – expect NullPointerException")
    void sizeWithNullInternalSet_throwsNPE() throws Exception {
        // Create a normal instance then replace the internalSet with null via reflection.
        ConcurrentNavigableSetNullSafe<String> unsafeSet = new ConcurrentNavigableSetNullSafe<>();
        Field internalField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalField.setAccessible(true);
        internalField.set(unsafeSet, null);
        assertThrows(NullPointerException.class, unsafeSet::size, "Calling size() when internalSet is null should throw NPE");
    }

    @Test
    @DisplayName("size() when internalSet throws UnsupportedOperationException")
    void sizeWhenInternalSetThrowsUnsupportedOperationException() throws Exception {
        // Mock a NavigableSet that throws on size()
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockSet = Mockito.mock(NavigableSet.class);
        Mockito.when(mockSet.size()).thenThrow(new UnsupportedOperationException("size not supported"));
        // Inject the mock into a new wrapper instance via reflection.
        ConcurrentNavigableSetNullSafe<String> wrapper = new ConcurrentNavigableSetNullSafe<>();
        Field internalField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalField.setAccessible(true);
        internalField.set(wrapper, mockSet);
        assertThrows(UnsupportedOperationException.class, wrapper::size, "size() should propagate UnsupportedOperationException from internalSet");
    }

    @Test
    @DisplayName("size() after adding duplicate elements and a distinct element")
    void sizeAfterDuplicatesAndDistinct() {
        assertTrue(set.add("X"));
        // duplicate – size unchanged
        assertFalse(set.add("X"));
        assertTrue(set.add("Y"));
        assertEquals(2, set.size(), "Size should be 2 after adding duplicate X and distinct Y");
    }

    @Test
    @DisplayName("size() after adding and then removing a null element")
    void sizeAddRemoveNullElement() {
        assertTrue(set.add(null));
        assertEquals(1, set.size(), "Null counts as an element");
        assertTrue(set.remove(null));
        assertEquals(0, set.size(), "Size should be 0 after removing the null element");
    }

    @Test
    @DisplayName("size() with custom comparator (string length) – already covered but re‑asserted")
    void sizeWithLengthComparator() {
        NavigableSet<String> lengthSet = new ConcurrentNavigableSetNullSafe<>(LENGTH_COMPARATOR);
        lengthSet.addAll(Arrays.asList("a", "bb", "ccc", "dddd"));
        assertEquals(4, lengthSet.size(), "All distinct length strings should be counted");
    }

    @Test
    @DisplayName("size() after concurrent removals")
    void sizeAfterConcurrentRemovals() throws InterruptedException, ExecutionException {
        NavigableSet<Integer> concurrentSet = new ConcurrentNavigableSetNullSafe<>();
        // Populate with 5 distinct elements.
        concurrentSet.addAll(Arrays.asList(10, 20, 30, 40, 50));
        ExecutorService exec = Executors.newFixedThreadPool(3);
        List<Callable<Void>> tasks = Arrays.asList(() -> {
            concurrentSet.remove(10);
            return null;
        }, () -> {
            concurrentSet.remove(20);
            return null;
        }, () -> {
            concurrentSet.remove(30);
            return null;
        });
        // Execute removals concurrently.
        List<Future<Void>> futures = exec.invokeAll(tasks);
        for (Future<Void> f : futures) {
            // re‑throw any exception
            f.get();
        }
        exec.shutdownNow();
        assertEquals(2, concurrentSet.size(), "After removing three of five elements concurrently, size must be 2");
        assertTrue(concurrentSet.containsAll(Arrays.asList(40, 50)));
    }
}
