package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.concurrent.locks.ReentrantLock;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_size_Test {

    /**
     * Set used by most tests – created fresh before each test method.
     */
    private NavigableSet<String> set;

    /**
     * A set that will be shared by the concurrent test.
     */
    private NavigableSet<Integer> concurrentSet;

    /**
     * Simple delegating NavigableSet that throws a supplied exception from {@code size()}.
     */
    private static class ExceptionThrowingSet<E> extends AbstractSet<E> implements NavigableSet<E> {

        private final RuntimeException toThrow;

        ExceptionThrowingSet(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public int size() {
            throw toThrow;
        }

        // All other methods delegate to an empty set – they are never called in these tests.
        private final NavigableSet<E> delegate = new ConcurrentSkipListSet<>();

        @Override
        public Iterator<E> iterator() {
            return delegate.iterator();
        }

        @Override
        public boolean add(E e) {
            return delegate.add(e);
        }

        @Override
        public E lower(E e) {
            return delegate.lower(e);
        }

        @Override
        public E floor(E e) {
            return delegate.floor(e);
        }

        @Override
        public E ceiling(E e) {
            return delegate.ceiling(e);
        }

        @Override
        public E higher(E e) {
            return delegate.higher(e);
        }

        @Override
        public E pollFirst() {
            return delegate.pollFirst();
        }

        @Override
        public E pollLast() {
            return delegate.pollLast();
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return delegate.descendingSet();
        }

        @Override
        public Iterator<E> descendingIterator() {
            return delegate.descendingIterator();
        }

        @Override
        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return delegate.subSet(fromElement, fromInclusive, toElement, toInclusive);
        }

        @Override
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return delegate.headSet(toElement, inclusive);
        }

        @Override
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return delegate.tailSet(fromElement, inclusive);
        }

        @Override
        public Comparator<? super E> comparator() {
            return delegate.comparator();
        }

        @Override
        public E first() {
            return delegate.first();
        }

        @Override
        public E last() {
            return delegate.last();
        }

        @Override
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return delegate.subSet(fromElement, toElement);
        }

        @Override
        public SortedSet<E> headSet(E toElement) {
            return delegate.headSet(toElement);
        }

        @Override
        public SortedSet<E> tailSet(E fromElement) {
            return delegate.tailSet(fromElement);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default constructor – natural ordering, null‑safe.
        set = new ConcurrentNavigableSetNullSafe<>();
        // Separate set for the concurrency test (integer elements).
        concurrentSet = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to avoid cross‑test contamination.
        if (set != null) {
            set.clear();
        }
        if (concurrentSet != null) {
            concurrentSet.clear();
        }
    }

    /**
     * Helper to replace the private {@code internalSet} field via reflection.
     */
    private static void setInternalSet(ConcurrentNavigableSetNullSafe<?> wrapper, NavigableSet<Object> newSet) throws Exception {
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        f.set(wrapper, newSet);
    }

    @Test
    void sizeIsZeroForNewDefaultConstructor() {
        assertEquals(0, set.size(), "A newly created set should have size 0");
    }

    @Test
    void sizeReflectsNumberOfAddedElementsIncludingNull() {
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add(null));
        assertTrue(set.add("cherry"));
        assertEquals(4, set.size(), "Size should count all distinct elements, null included");
    }

    @Test
    void sizeAfterRemovingElements() {
        set.addAll(Arrays.asList("a", "b", "c", null));
        assertEquals(4, set.size());
        assertTrue(set.remove("b"));
        assertTrue(set.remove(null));
        assertEquals(2, set.size(), "Size should decrease after removals");
        assertFalse(set.contains("b"));
        assertFalse(set.contains(null));
    }

    @Test
    void sizeWithCustomComparator() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> lengthSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        lengthSet.add("kiwi");
        // length 5
        lengthSet.add("apple");
        // length 0
        lengthSet.add(null);
        // length 6
        lengthSet.add("banana");
        assertEquals(4, lengthSet.size(), "Size should reflect all added elements irrespective of ordering");
        // Ensure ordering is as expected (null first, then by length)
        Iterator<String> it = lengthSet.iterator();
        assertEquals(null, it.next());
        assertEquals("kiwi", it.next());
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void sizeUsingCollectionConstructor() {
        Collection<String> initial = Arrays.asList("x", "y", null, "z");
        NavigableSet<String> collectionSet = new ConcurrentNavigableSetNullSafe<>(initial);
        assertEquals(4, collectionSet.size(), "Collection constructor should initialise set with the correct size");
        assertTrue(collectionSet.containsAll(initial));
    }

    @Test
    void sizeRemainsConsistentAfterRepeatedAddsAndRemoves() {
        // Add a sequence of numbers as strings, then remove half of them.
        for (int i = 0; i < 100; i++) {
            set.add("val" + i);
        }
        assertEquals(100, set.size());
        for (int i = 0; i < 50; i++) {
            assertTrue(set.remove("val" + i));
        }
        assertEquals(50, set.size());
        // Re‑add some of the removed elements and verify size again.
        for (int i = 0; i < 25; i++) {
            set.add("val" + i);
        }
        assertEquals(75, set.size());
    }

    @Test
    void sizeUnderConcurrentModifications() throws Exception {
        final int THREAD_COUNT = 8;
        final int ELEMENTS_PER_THREAD = 250;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger added = new AtomicInteger();
        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    // synchronize start
                    startLatch.await();
                    for (int i = 0; i < ELEMENTS_PER_THREAD; i++) {
                        int value = threadId * ELEMENTS_PER_THREAD + i;
                        if (concurrentSet.add(value)) {
                            added.incrementAndGet();
                        }
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // Start all threads
        startLatch.countDown();
        // Wait for completion
        doneLatch.await();
        executor.shutdownNow();
        assertEquals(added.get(), concurrentSet.size(), "Size after concurrent adds should equal the number of successful insertions");
        // Verify that all expected elements are present
        for (int i = 0; i < THREAD_COUNT * ELEMENTS_PER_THREAD; i++) {
            assertTrue(concurrentSet.contains(i), "Set should contain element " + i);
        }
    }

    /**
     * Helper to obtain the private {@code internalSet} field via reflection.
     */
    @SuppressWarnings("unchecked")
    private static NavigableSet<Object> getInternalSet(ConcurrentNavigableSetNullSafe<?> wrapper) throws Exception {
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        return (NavigableSet<Object>) f.get(wrapper);
    }

    @Test
    void sizeZeroWhenEmpty() {
        // Already covered by sizeIsZeroForNewDefaultConstructor, kept for explicit JSON mapping.
        assertEquals(0, set.size());
    }

    @Test
    void sizeAfterAddingThreeDistinctElements() {
        set.add("one");
        set.add("two");
        set.add("three");
        assertEquals(3, set.size());
    }

    @Test
    void sizeAfterRemovingOneFromThreeElements() {
        set.addAll(Arrays.asList("a", "b", "c"));
        assertEquals(3, set.size());
        set.remove("b");
        assertEquals(2, set.size());
    }

    @Test
    void sizeAfterClear() {
        set.addAll(Arrays.asList("x", "y"));
        set.clear();
        assertEquals(0, set.size());
    }

    @Test
    void sizeDoesNotIncreaseWhenAddingDuplicate() {
        assertTrue(set.add("dup"));
        // duplicate, should not be added
        assertFalse(set.add("dup"));
        assertEquals(1, set.size());
    }

    @Test
    void sizeReflectsDirectModificationOfInternalSet() throws Exception {
        // Access the real internal set and add elements directly.
        NavigableSet<Object> internal = getInternalSet((ConcurrentNavigableSetNullSafe<String>) set);
        internal.add("a");
        internal.add("b");
        internal.add("c");
        // null is allowed via the wrapper's sentinel
        internal.add(null);
        assertEquals(4, set.size());
    }

    @Test
    void sizeThrowsNPEWhenInternalSetIsNull() throws Exception {
        // Simulate a corrupted state where internalSet is null.
        setInternalSet((ConcurrentNavigableSetNullSafe<String>) set, null);
        assertThrows(NullPointerException.class, set::size);
    }

    @Test
    void sizeWithComparatorTreatingAllElementsEqual() {
        Comparator<String> allEqual = (a, b) -> 0;
        NavigableSet<String> eqSet = new ConcurrentNavigableSetNullSafe<>(allEqual);
        eqSet.add("first");
        eqSet.add("second");
        eqSet.add("third");
        // All elements are considered equal, so only one is kept.
        assertEquals(1, eqSet.size());
    }

    @Test
    void sizeCountsNullElementWhenAllowed() {
        // The wrapper is null‑safe, so null should be counted.
        assertTrue(set.add(null));
        assertEquals(1, set.size());
    }

    @Test
    void sizeAfterPollingFirstUntilEmpty() {
        set.addAll(Arrays.asList("a", "b", "c"));
        while (!set.isEmpty()) {
            set.pollFirst();
        }
        assertEquals(0, set.size());
    }

    @Test
    void sizeAfterPollingLastUntilEmpty() {
        set.addAll(Arrays.asList("a", "b", "c"));
        while (!set.isEmpty()) {
            set.pollLast();
        }
        assertEquals(0, set.size());
    }

    @Test
    void sizeWhenInternalSetIsEmptyHeadSetView() throws Exception {
        set.addAll(Arrays.asList("m", "n", "o"));
        NavigableSet<Object> internal = getInternalSet((ConcurrentNavigableSetNullSafe<String>) set);
        // empty view
        NavigableSet<Object> headView = internal.headSet("m");
        setInternalSet((ConcurrentNavigableSetNullSafe<String>) set, headView);
        assertEquals(0, set.size());
    }

    @Test
    void sizeWhenInternalSetIsTailSetViewContainingTwoElements() throws Exception {
        set.addAll(Arrays.asList("a", "b", "c", "d"));
        NavigableSet<Object> internal = getInternalSet((ConcurrentNavigableSetNullSafe<String>) set);
        // should contain "c" and "d"
        NavigableSet<Object> tailView = internal.tailSet("c");
        setInternalSet((ConcurrentNavigableSetNullSafe<String>) set, tailView);
        assertEquals(2, set.size());
    }

    @Test
    void sizeReflectsReplacementWithEmptyNavigableSet() throws Exception {
        set.addAll(Arrays.asList("x", "y"));
        assertEquals(2, set.size());
        // Replace internal set with a fresh empty ConcurrentSkipListSet
        NavigableSet<Object> empty = new ConcurrentSkipListSet<>();
        setInternalSet((ConcurrentNavigableSetNullSafe<String>) set, empty);
        assertEquals(0, set.size());
    }

    @Test
    void sizeThrowsClassCastExceptionWhenInternalSetHasIncompatibleElement() throws Exception {
        // Directly add an Integer to the raw internal set.
        NavigableSet<Object> internal = getInternalSet((ConcurrentNavigableSetNullSafe<String>) set);
        // bypass generic type safety
        internal.add(123);
        // The next operation that forces the set to compare elements will throw.
        assertThrows(ClassCastException.class, () -> {
            // size() itself may not trigger the cast, so we invoke a method that does.
            // For ConcurrentSkipListSet, size() is safe, therefore we call iterator().next()
            set.iterator().next();
        });
    }

    @Test
    void sizePropagatesUnsupportedOperationExceptionFromInternalSet() throws Exception {
        ExceptionThrowingSet<Object> throwing = new ExceptionThrowingSet<>(new UnsupportedOperationException("size not supported"));
        setInternalSet((ConcurrentNavigableSetNullSafe<String>) set, throwing);
        assertThrows(UnsupportedOperationException.class, set::size);
    }

    @Test
    void sizePropagatesIllegalStateExceptionFromInternalSet() throws Exception {
        ExceptionThrowingSet<Object> throwing = new ExceptionThrowingSet<>(new IllegalStateException("corrupted state"));
        setInternalSet((ConcurrentNavigableSetNullSafe<String>) set, throwing);
        assertThrows(IllegalStateException.class, set::size);
    }

    @Test
    void sizePropagatesConcurrentModificationExceptionFromInternalSet() throws Exception {
        ExceptionThrowingSet<Object> throwing = new ExceptionThrowingSet<>(new ConcurrentModificationException("concurrent mod"));
        setInternalSet((ConcurrentNavigableSetNullSafe<String>) set, throwing);
        assertThrows(ConcurrentModificationException.class, set::size);
    }
}
