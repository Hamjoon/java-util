package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_clear_Test {

    /**
     * The set under test
     */
    private SealableNavigableSet<Integer> set;

    /**
     * Mutable flag used by the supplier to indicate the sealed state
     */
    private volatile boolean sealedState = false;

    /**
     * Supplier that simply returns the current value of {@code sealedState}
     */
    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    /**
     * A minimal {@link NavigableSet} implementation that delegates to a TreeSet
     * but deliberately throws a {@link RuntimeException} from {@code clear()}.
     */
    private static class ThrowingNavigableSet<E> extends AbstractSet<E> implements NavigableSet<E> {

        private final NavigableSet<E> delegate = new TreeSet<>();

        @Override
        public void clear() {
            throw new RuntimeException("clear failed deliberately");
        }

        // -----------------------------------------------------------------
        // Boiler‑plate delegation for the remaining NavigableSet methods.
        // Only the methods required for the tests are fully implemented.
        // -----------------------------------------------------------------
        @Override
        public boolean add(E e) {
            return delegate.add(e);
        }

        @Override
        public Iterator<E> iterator() {
            return delegate.iterator();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public Comparator<? super E> comparator() {
            return delegate.comparator();
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

        @Override
        public E first() {
            return delegate.first();
        }

        @Override
        public E last() {
            return delegate.last();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test and populate it with a few values
        set = new SealableNavigableSet<>(new TreeSet<>(), sealedSupplier);
        set.add(10);
        set.add(20);
        set.add(30);
        // Ensure the set starts in an un‑sealed state
        sealedState = false;
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the sealed flag so that a failure in one test does not affect the next
        sealedState = false;
        set = null;
    }

    @Test
    @DisplayName("clear() succeeds when the set is not sealed")
    void clearWhenUnsealed() {
        // pre‑condition: the set contains three elements
        assertEquals(3, set.size());
        // Act – clear the set while it is un‑sealed
        assertDoesNotThrow(set::clear);
        // post‑condition: the set is empty
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    @DisplayName("clear() throws when the set is sealed")
    void clearWhenSealedThrows() {
        // Seal the set
        sealedState = true;
        // Act & Assert – clear should throw an UnsupportedOperationException
        // (original test expectation – retained for backward compatibility)
        assertThrows(UnsupportedOperationException.class, set::clear);
    }

    @Test
    @DisplayName("clear() on a view (tailSet) respects sealed state")
    void clearOnTailSetRespectsSealedState() {
        // Obtain a view that starts at 20 (inclusive)
        NavigableSet<Integer> tail = set.tailSet(20, true);
        // ---- un‑sealed case -------------------------------------------------
        assertDoesNotThrow(tail::clear);
        // After clearing the tail view, only the element < 20 should remain
        assertEquals(1, set.size());
        assertTrue(set.contains(10));
        // Re‑populate the set for the sealed scenario
        set.add(20);
        set.add(30);
        sealedState = true;
        // ---- sealed case ----------------------------------------------------
        assertThrows(UnsupportedOperationException.class, tail::clear);
        // The underlying set must remain unchanged
        assertEquals(3, set.size());
    }

    @Test
    @DisplayName("clear() on a view (subSet) removes only the subset elements")
    void clearOnSubSetRemovesSubsetElements() {
        // Create a subset that includes 10 and 20
        NavigableSet<Integer> sub = set.subSet(10, true, 20, true);
        assertEquals(2, sub.size());
        // Clear the subset while un‑sealed
        assertDoesNotThrow(sub::clear);
        // The original set should now contain only the element that was not in the subset
        assertEquals(1, set.size());
        assertTrue(set.contains(30));
    }

    @Test
    @DisplayName("clear() when the set is sealed should throw IllegalStateException")
    void clearWhenSealedThrowsIllegalStateException() {
        // Seal the set using the supplied flag
        sealedState = true;
        // The contract (JSON) expects IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, set::clear);
        assertEquals("Set is sealed", ex.getMessage());
        // Verify that the underlying set is unchanged
        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList(10, 20, 30)));
    }

    @Test
    @DisplayName("clear() on an unmodifiable underlying NavigableSet propagates UnsupportedOperationException")
    void clearOnUnmodifiableUnderlyingSet() {
        // Create an unmodifiable NavigableSet with some elements
        NavigableSet<Integer> mutable = new TreeSet<>(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> unmodifiable = Collections.unmodifiableNavigableSet(mutable);
        // Wrap it with SealableNavigableSet (still un‑sealed)
        SealableNavigableSet<Integer> sealedSet = new SealableNavigableSet<>(unmodifiable, sealedSupplier);
        // clear() should propagate the UnsupportedOperationException from the delegate
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, sealedSet::clear);
        assertNotNull(ex.getMessage());
        // The underlying mutable set must still contain its elements (the unmodifiable wrapper
        // prevents modification, so size remains the same)
        assertEquals(3, mutable.size());
    }

    @Test
    @DisplayName("clear() propagates custom RuntimeException thrown by the underlying set")
    void clearPropagatesCustomRuntimeException() {
        // Custom NavigableSet that throws a RuntimeException from clear()
        NavigableSet<Integer> throwingSet = new ThrowingNavigableSet<>();
        // Populate it before wrapping (the custom implementation stores elements)
        throwingSet.addAll(Arrays.asList(5, 6, 7));
        // Wrap with SealableNavigableSet (un‑sealed)
        SealableNavigableSet<Integer> sealedSet = new SealableNavigableSet<>(throwingSet, sealedSupplier);
        // Expect the same RuntimeException to be propagated
        RuntimeException ex = assertThrows(RuntimeException.class, sealedSet::clear);
        assertEquals("clear failed deliberately", ex.getMessage());
        // The underlying set should still contain its elements because clear() never succeeded
        assertEquals(3, throwingSet.size());
    }
}
