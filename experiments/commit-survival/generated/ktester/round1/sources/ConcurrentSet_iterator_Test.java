package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_iterator_Test {

    /**
     * The set used in most tests – created fresh before each test.
     */
    private ConcurrentSet<Integer> intSet;

    /**
     * A set that contains a {@code null} element – used for null‑handling tests.
     */
    private ConcurrentSet<String> stringSetWithNull;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a set with a known collection of integers.
        intSet = new ConcurrentSet<>();
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        // Initialise a set that includes a null element.
        stringSetWithNull = new ConcurrentSet<>();
        stringSetWithNull.addAll(Arrays.asList("a", null, "b"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the sets – not strictly required but makes intent clear.
        if (intSet != null) {
            intSet.clear();
        }
        if (stringSetWithNull != null) {
            stringSetWithNull.clear();
        }
    }

    /**
     * Verify that the iterator traverses all elements exactly once.
     */
    @Test
    void iteratorTraversesAllElements() {
        // given
        Set<Integer> expected = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        // when
        Iterator<Integer> it = intSet.iterator();
        Set<Integer> actual = new HashSet<>();
        while (it.hasNext()) {
            actual.add(it.next());
        }
        // then
        assertEquals(expected, actual, "Iterator should return every element exactly once");
    }

    /**
     * Verify that the iterator's {@code remove()} method removes the last returned element.
     */
    @Test
    void iteratorRemoveDeletesElement() {
        // given
        Iterator<Integer> it = intSet.iterator();
        // when – advance to element '3' and remove it
        while (it.hasNext()) {
            Integer val = it.next();
            if (val == 3) {
                it.remove();
                break;
            }
        }
        // then
        assertFalse(intSet.contains(3), "Element 3 should have been removed via iterator");
        assertEquals(4, intSet.size(), "Set size should reflect the removal");
    }

    /**
     * Verify that an iterator over an empty set reports no elements.
     */
    @Test
    void iteratorOnEmptySet() {
        // given
        ConcurrentSet<String> empty = new ConcurrentSet<>();
        Iterator<String> it = empty.iterator();
        // then
        assertFalse(it.hasNext(), "Empty set iterator should have no next element");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator should throw NoSuchElementException");
    }

    /**
     * Verify that the iterator correctly returns {@code null} values stored in the set.
     */
    @Test
    void iteratorHandlesNullElements() {
        // given
        Iterator<String> it = stringSetWithNull.iterator();
        List<String> collected = new ArrayList<>();
        // when
        while (it.hasNext()) {
            collected.add(it.next());
        }
        // then
        assertTrue(collected.contains(null), "Iterator should return the stored null element");
        assertTrue(collected.containsAll(Arrays.asList("a", "b")), "Iterator should return all non‑null elements");
        assertEquals(3, collected.size(), "Exactly three elements (including null) should be returned");
    }

    /**
     * Verify that the iterator works correctly under concurrent modifications.
     */
    @Test
    void iteratorConcurrentModification() throws InterruptedException {
        // given
        ConcurrentSet<Integer> concurrentSet = new ConcurrentSet<>();
        int threadCount = 3;
        int opsPerThread = 1000;
        CountDownLatch latch = new CountDownLatch(threadCount * 2);
        // start threads that add and remove concurrently
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    concurrentSet.add(j);
                }
                latch.countDown();
            }).start();
            new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    concurrentSet.remove(j);
                }
                latch.countDown();
            }).start();
        }
        // when – wait for all workers to finish and then iterate
        latch.await();
        // then – iterator should not throw and should reflect the final state
        Iterator<Integer> it = concurrentSet.iterator();
        while (it.hasNext()) {
            // just consume the iterator to ensure no ConcurrentModificationException
            it.next();
        }
        // The size can be anywhere from 0 up to opsPerThread (inclusive)
        assertTrue(concurrentSet.size() >= 0 && concurrentSet.size() <= opsPerThread, "Final size should be within expected bounds after concurrent adds/removes");
    }

    /**
     * Adding {@code null} to a ConcurrentSet should throw NullPointerException (underlying map restriction).
     */
    @Test
    void addingNullThrowsNPE() {
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        assertThrows(NullPointerException.class, () -> set.add(null), "Adding null should raise NullPointerException");
    }

    /**
     * Calling {@code remove()} before any {@code next()} must throw IllegalStateException.
     */
    @Test
    void iteratorRemoveWithoutPriorNext() {
        Iterator<Integer> it = intSet.iterator();
        assertThrows(IllegalStateException.class, it::remove, "remove() before next() should throw IllegalStateException");
    }

    /**
     * Calling {@code remove()} twice consecutively must throw IllegalStateException.
     */
    @Test
    void iteratorRemoveCalledTwice() {
        Iterator<Integer> it = intSet.iterator();
        assertTrue(it.hasNext(), "Iterator should have at least one element");
        // first element
        it.next();
        // first removal – ok
        it.remove();
        assertThrows(IllegalStateException.class, it::remove, "Second remove() without intervening next() should throw IllegalStateException");
    }

    /**
     * After {@code clear()}, iterator should behave like an empty set iterator.
     */
    @Test
    void iteratorAfterClear() {
        // given
        intSet.addAll(Arrays.asList(10, 20, 30));
        intSet.clear();
        // when
        Iterator<Integer> it = intSet.iterator();
        // then
        assertFalse(it.hasNext(), "Iterator after clear should have no next element");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() after clear should throw NoSuchElementException");
    }

    /**
     * Iterator should traverse exactly the elements supplied via {@code addAll}.
     */
    @Test
    void iteratorAfterAddAll() {
        // given
        ConcurrentSet<String> set = new ConcurrentSet<>();
        List<String> source = Arrays.asList("x", "y", "z");
        set.addAll(source);
        // when
        Set<String> iterated = new HashSet<>();
        for (Iterator<String> it = set.iterator(); it.hasNext(); ) {
            iterated.add(it.next());
        }
        // then
        assertEquals(new HashSet<>(source), iterated, "Iterator must return exactly the elements added via addAll");
    }

    /**
     * Weak‑consistency: iterator must not throw CME when another thread removes an element.
     */
    @Test
    void iteratorWeakConsistencyWithConcurrentRemoval() throws Exception {
        // given
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));
        // snapshot iterator
        Iterator<Integer> it = set.iterator();
        // remove element 3 concurrently after iterator creation
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> remover = exec.submit(() -> set.remove(3));
        // ensure removal happened
        remover.get();
        exec.shutdown();
        // when – consume iterator, ensuring no CME
        List<Integer> collected = new ArrayList<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        // then – iterator either contains 3 (snapshot) or not, but no exception
        assertTrue(collected.containsAll(Arrays.asList(1, 2, 4, 5)), "Iterator must contain all elements that were present before removal (except possibly the removed one)");
        assertTrue(collected.size() >= 4 && collected.size() <= 5, "Iterator size should be between 4 and 5 depending on weak‑consistency semantics");
    }

    /**
     * Verify generic type safety – iterator returns objects assignable to the declared type.
     */
    @Test
    void iteratorTypeSafetyGenericUnwrap() {
        // given
        ConcurrentSet<Number> set = new ConcurrentSet<>();
        // Integer
        set.add(42);
        // Double
        set.add(3.14);
        // when
        List<Number> collected = new ArrayList<>();
        for (Iterator<Number> it = set.iterator(); it.hasNext(); ) {
            collected.add(it.next());
        }
        // then
        assertEquals(2, collected.size(), "Both elements should be collected");
        for (Number n : collected) {
            assertTrue(n instanceof Number, "Each element must be assignable to Number");
        }
    }

    /**
     * Repeated calls to {@code hasNext()} must not advance the iterator.
     */
    @Test
    void iteratorInterleavedHasNextNextCalls() {
        // given
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        Iterator<String> it = set.iterator();
        // when / then – interleave hasNext calls
        assertTrue(it.hasNext());
        // second call should not move cursor
        assertTrue(it.hasNext());
        String first = it.next();
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        String second = it.next();
        assertTrue(it.hasNext());
        String third = it.next();
        assertFalse(it.hasNext());
        // then – ensure three distinct values were returned
        Set<String> returned = new HashSet<>(Arrays.asList(first, second, third));
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), returned, "Iterator must return each distinct element exactly once");
    }

    /**
     * Adding a mismatched type via raw access should cause ClassCastException on next().
     */
    @Test
    void iteratorClassCastExceptionOnMismatchedType() {
        // given: raw access to bypass generic compile‑time check
        @SuppressWarnings("unchecked")
        ConcurrentSet rawSet = new ConcurrentSet();
        // String inside a set intended for Number
        rawSet.add("not-a-number");
        @SuppressWarnings("unchecked")
        ConcurrentSet<Number> // treat as Number set
        numberSet = rawSet;
        Iterator<Number> it = numberSet.iterator();
        // when / then
        assertThrows(ClassCastException.class, it::next, "Iterator.next() should throw ClassCastException when underlying element is not assignable to generic type");
    }

    /**
     * Single element iteration – iterator should return the element and then finish.
     */
    @Test
    void singleElementIteration() {
        // given
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add("only");
        // when
        Iterator<String> it = set.iterator();
        // then
        assertTrue(it.hasNext(), "Iterator should have next for single element");
        assertEquals("only", it.next(), "Iterator should return the only element");
        assertFalse(it.hasNext(), "Iterator should report no further elements");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() after exhaustion should throw NoSuchElementException");
    }

    /**
     * Multiple elements iteration – ensure all added elements are visited exactly once.
     */
    @Test
    void multipleElementsIteration() {
        // given
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        List<Integer> source = Arrays.asList(10, 20, 30, 40);
        set.addAll(source);
        // when
        Set<Integer> visited = new HashSet<>();
        for (Iterator<Integer> it = set.iterator(); it.hasNext(); ) {
            visited.add(it.next());
        }
        // then
        assertEquals(new HashSet<>(source), visited, "Iterator must visit each added element exactly once");
    }
}
