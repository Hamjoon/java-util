package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_iterator_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState;

    private Supplier<Boolean> sealedSupplier;

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        sealedSupplier = () -> sealedState;
        // The default constructor used in the original test suite takes a Supplier<Boolean>.
        set = new SealableNavigableSet<>(sealedSupplier);
        // Populate with predictable ordering.
        set.add(10);
        set.add(20);
        set.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        set = null;
        sealedSupplier = null;
    }

    /**
     * Provides data for parameterized tests (currently unused but kept for future
     * extensions where a data‑driven approach is desired).
     */
    private static Stream<Arguments> dummyProvider() {
        return Stream.of();
    }

    @Test
    void iteratorReturnsElementsInSortedOrderWhenUnsealed() {
        Iterator<Integer> it = set.iterator();
        assertTrue(it.hasNext());
        assertEquals(10, it.next());
        assertTrue(it.hasNext());
        assertEquals(20, it.next());
        assertTrue(it.hasNext());
        assertEquals(30, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void iteratorRemoveWorksWhenUnsealed() {
        Iterator<Integer> it = set.iterator();
        // advance to first element
        assertEquals(10, it.next());
        // should remove 10
        it.remove();
        // The set should now contain only 20 and 30
        assertEquals(2, set.size());
        assertFalse(set.contains(10));
        assertTrue(set.contains(20));
        assertTrue(set.contains(30));
    }

    @Test
    void iteratorRemoveThrowsWhenSealedAfterIterationStarted() {
        Iterator<Integer> it = set.iterator();
        // Move to first element while still unsealed
        assertEquals(10, it.next());
        // Seal the set now
        sealedState = true;
        // The next call to remove must fail
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void iteratorNextDoesNotThrowWhenSealed() {
        Iterator<Integer> it = set.iterator();
        // seal before any traversal
        sealedState = true;
        // Even when sealed, next() should still be allowed (read‑only)
        assertDoesNotThrow(it::next);
        // The returned element should be the first (sorted) one
        assertEquals(10, it.next());
    }

    @Test
    void iteratorRemoveThrowsImmediatelyWhenSealedBeforeCreation() {
        // seal before iterator is created
        sealedState = true;
        Iterator<Integer> it = set.iterator();
        // next is allowed
        assertDoesNotThrow(it::next);
        // remove must fail
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    /**
     * Group: iterator returns elements in natural order
     */
    @Test
    void iteratorReturnsElementsInNaturalOrder() {
        // Backing TreeSet with 1,2,3
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        s.add(1);
        s.add(2);
        s.add(3);
        List<Integer> collected = new ArrayList<>();
        s.iterator().forEachRemaining(collected::add);
        assertEquals(List.of(1, 2, 3), collected);
    }

    @Test
    void iteratorOnEmptySetHasNoElements() {
        SealableNavigableSet<Integer> empty = new SealableNavigableSet<>(sealedSupplier);
        Iterator<Integer> it = empty.iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    /**
     * Group: iterator respects seal state (remove operation)
     */
    @Test
    void iteratorRemoveThrowsWhenSealedDuringIteration() {
        // Single element set
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        s.add(42);
        // Seal before removal
        sealedState = true;
        Iterator<Integer> it = s.iterator();
        assertEquals(42, it.next());
        // According to the spec, a sealed set should prohibit modification.
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void iteratorRemoveSucceedsWhenUnsealed() {
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        s.add(1);
        s.add(2);
        Iterator<Integer> it = s.iterator();
        assertEquals(1, it.next());
        it.remove();
        assertEquals(Set.of(2), new HashSet<>(s));
    }

    /**
     * Group: iterator remove invoked before next
     */
    @Test
    void iteratorRemoveBeforeNextThrowsIllegalStateException() {
        Iterator<Integer> it = set.iterator();
        assertThrows(IllegalStateException.class, it::remove);
    }

    /**
     * Group: iterator next after exhaustion
     */
    @Test
    void iteratorNextAfterExhaustionThrowsNoSuchElementException() {
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        s.add(42);
        Iterator<Integer> it = s.iterator();
        assertEquals(42, it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    /**
     * Group: iterator fail‑fast on concurrent modification
     */
    @Test
    void iteratorDetectsConcurrentModification() {
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        s.addAll(List.of(1, 2, 3));
        Iterator<Integer> it = s.iterator();
        // Modify the set after iterator creation
        s.add(4);
        assertThrows(ConcurrentModificationException.class, it::next);
    }

    /**
     * Group: iterator reflects pre‑iteration modifications
     */
    @Test
    void iteratorSeesElementsAddedBeforeCreation() {
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        s.addAll(List.of(1, 2, 3));
        // Add another element before obtaining iterator
        s.add(4);
        List<Integer> collected = new ArrayList<>();
        s.iterator().forEachRemaining(collected::add);
        assertEquals(List.of(1, 2, 3, 4), collected);
    }

    /**
     * Group: iterator respects custom comparator ordering
     */
    @Test
    void iteratorHonorsCustomComparator() throws Exception {
        // Backing TreeSet with reverse alphabetical order
        NavigableSet<String> backing = new TreeSet<>(Comparator.reverseOrder());
        backing.addAll(List.of("a", "b", "c"));
        // Use reflection to inject the custom backing set
        SealableNavigableSet<String> s = createWithBacking(backing, sealedSupplier);
        List<String> collected = new ArrayList<>();
        s.iterator().forEachRemaining(collected::add);
        assertEquals(List.of("c", "b", "a"), collected);
    }

    /**
     * Group: iterator is non‑null
     */
    @Test
    void iteratorIsNonNull() {
        SealableNavigableSet<Integer> empty = new SealableNavigableSet<>(sealedSupplier);
        assertNotNull(empty.iterator());
    }

    /**
     * Group: iterator remove works after multiple next calls (unsealed)
     */
    @Test
    void iteratorRemoveAfterMultipleNextCalls() {
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        s.addAll(List.of(1, 2, 3));
        Iterator<Integer> it = s.iterator();
        // first
        assertEquals(1, it.next());
        // second
        assertEquals(2, it.next());
        // remove element 2
        it.remove();
        assertEquals(Set.of(1, 3), new HashSet<>(s));
    }

    /**
     * Group: iterator remove after set sealed post‑creation
     */
    @Test
    void iteratorRemoveFailsIfSetSealedAfterIteratorCreation() {
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        s.addAll(List.of(1, 2));
        Iterator<Integer> it = s.iterator();
        assertEquals(1, it.next());
        // Seal after iterator has been created and moved
        sealedState = true;
        assertThrows(IllegalStateException.class, it::remove);
    }

    /**
     * Group: iterator remove unsupported by underlying iterator
     */
    @Test
    void iteratorRemovePropagatesUnsupportedOperationException() throws Exception {
        // Create an unmodifiable view of a TreeSet
        NavigableSet<Integer> unmodifiable = Collections.unmodifiableNavigableSet(new TreeSet<>(List.of(1, 2, 3)));
        SealableNavigableSet<Integer> s = createWithBacking(unmodifiable, sealedSupplier);
        Iterator<Integer> it = s.iterator();
        assertEquals(1, it.next());
        // Underlying iterator does not support remove()
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    /**
     * Group: iterator remove after external element removal (concurrent modification)
     */
    @Test
    void iteratorRemoveDetectsExternalConcurrentModification() {
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        s.addAll(List.of(1, 2, 3));
        Iterator<Integer> it = s.iterator();
        assertEquals(1, it.next());
        // External removal of a different element
        s.remove(2);
        // The iterator should now detect concurrent modification on remove()
        assertThrows(ConcurrentModificationException.class, it::remove);
    }

    /**
     * Creates a {@link SealableNavigableSet} that uses the supplied {@code backing}
     * as its internal {@code navSet}. This is achieved via reflection because the
     * constructor of {@code SealableNavigableSet} does not expose a way to inject a
     * custom backing set.
     *
     * @param backing        the NavigableSet to be used internally
     * @param sealedSupplier supplier that determines the sealed state
     * @param <E>            element type
     * @return a {@code SealableNavigableSet} wrapping {@code backing}
     * @throws Exception if reflection fails
     */
    @SuppressWarnings("unchecked")
    private static <E> SealableNavigableSet<E> createWithBacking(NavigableSet<E> backing, Supplier<Boolean> sealedSupplier) throws Exception {
        SealableNavigableSet<E> wrapper = new SealableNavigableSet<>(sealedSupplier);
        Field navSetField = SealableNavigableSet.class.getDeclaredField("navSet");
        navSetField.setAccessible(true);
        navSetField.set(wrapper, backing);
        return wrapper;
    }
}
