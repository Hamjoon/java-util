package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableSet_containsAll_Test {

    // uses a real underlying ConcurrentSet
    private SealableSet<Integer> realSet;

    // wraps a Mockito mock Set
    private SealableSet<Integer> mockSetWrapper;

    // Mockito mock for delegation verification
    private Set<Integer> mockSet;

    // mutable flag used by the supplier
    private volatile boolean sealed;

    // supplies the current sealed state
    private Supplier<Boolean> sealedSupplier;

    /**
     * A minimal Collection implementation that throws a RuntimeException
     * when any method that iterates over its elements is invoked.
     */
    private static class ThrowingCollection<E> implements Collection<E> {

        @Override
        public int size() {
            throw new RuntimeException("size() called");
        }

        @Override
        public boolean isEmpty() {
            throw new RuntimeException("isEmpty() called");
        }

        @Override
        public boolean contains(Object o) {
            throw new RuntimeException("contains() called");
        }

        @Override
        public Iterator<E> iterator() {
            throw new RuntimeException("iterator() called");
        }

        @Override
        public Object[] toArray() {
            throw new RuntimeException("toArray() called");
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new RuntimeException("toArray(T[]) called");
        }

        @Override
        public boolean add(E e) {
            throw new RuntimeException("add() called");
        }

        @Override
        public boolean remove(Object o) {
            throw new RuntimeException("remove() called");
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new RuntimeException("containsAll() called");
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw new RuntimeException("addAll() called");
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new RuntimeException("removeAll() called");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new RuntimeException("retainAll() called");
        }

        @Override
        public void clear() {
            throw new RuntimeException("clear() called");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // reset sealed flag for each test
        sealed = false;
        sealedSupplier = () -> sealed;
        /* real set – used for functional behaviour tests */
        realSet = new SealableSet<>(sealedSupplier);
        realSet.add(10);
        realSet.add(20);
        realSet.add(null);
        /* mock set – used for delegation verification */
        mockSet = mock(Set.class);
        mockSetWrapper = new SealableSet<>(mockSet, sealedSupplier);
    }

    static Stream<Arguments> positiveScenarios() {
        return Stream.of(Arguments.of(new HashSet<>(Arrays.asList("a", "b", "c")), Arrays.asList("a", "b"), "The collection is a proper subset of the backing set; containsAll should return true."), Arguments.of(new HashSet<>(Arrays.asList(1, 2, 3, 4)), Arrays.asList(1, 2, 3, 4), "The collection contains exactly the same elements as the backing set; containsAll should return true."), Arguments.of(new HashSet<>(), Collections.emptyList(), "Both the backing set and the collection are empty; containsAll should return true (vacuously)."), Arguments.of(new HashSet<>(Arrays.asList(null, "x")), Arrays.asList((Object) null), "The backing set allows null and the collection contains only null; containsAll should return true."));
    }

    static Stream<Arguments> negativeScenarios() {
        return Stream.of(Arguments.of(new HashSet<>(Arrays.asList("a", "b")), Arrays.asList("a", "c"), "The collection contains an element ('c') that is not present in the backing set; containsAll should return false."), Arguments.of(new HashSet<>(Arrays.asList(10, 20)), Arrays.asList(10, 30, 40), "The collection has multiple elements missing from the backing set; containsAll should return false."), Arguments.of(new HashSet<>(Arrays.asList("x")), Arrays.asList((Object) null), "The backing set does not contain null while the collection does; containsAll should return false."));
    }

    static Stream<Arguments> exceptionalScenarios() {
        return Stream.of(Arguments.of(new HashSet<>(Arrays.asList(1, 2, 3)), null, NullPointerException.class, "Passing a null collection should cause Set.containsAll to throw NullPointerException."), Arguments.of(Collections.unmodifiableSet(new HashSet<>(Arrays.asList("a"))), Arrays.asList("a"), // no exception expected – we will test the result separately
        null, "Even when the backing set is unmodifiable, containsAll should still delegate correctly and return true."));
    }

    @Test
    void testContainsAllReturnsTrueWhenAllElementsPresent() {
        Collection<Integer> query = Arrays.asList(10, null);
        assertTrue(realSet.containsAll(query));
    }

    @Test
    void testContainsAllReturnsFalseWhenMissingElements() {
        // 30 is not in the set
        Collection<Integer> query = Arrays.asList(10, 30);
        assertFalse(realSet.containsAll(query));
    }

    @Test
    void testContainsAllWithEmptyCollectionIsTrue() {
        assertTrue(realSet.containsAll(Collections.emptyList()));
    }

    @Test
    void testContainsAllThrowsNPEWhenArgumentIsNull() {
        assertThrows(NullPointerException.class, () -> realSet.containsAll(null));
    }

    @Test
    void testContainsAllIsUnaffectedBySealedState() {
        // put the SealableSet into “sealed” mode
        sealed = true;
        Collection<Integer> query = Collections.singleton(10);
        // containsAll is a read‑only operation, so it must still work.
        assertTrue(realSet.containsAll(query));
    }

    @Test
    void testContainsAllDelegatesToUnderlyingSet() {
        Collection<Integer> query = Arrays.asList(1, 2, 3);
        // define the mock's behaviour
        when(mockSet.containsAll(query)).thenReturn(true);
        // call the method under test
        boolean result = mockSetWrapper.containsAll(query);
        // verify result and delegation
        assertTrue(result);
        verify(mockSet, times(1)).containsAll(query);
    }

    @Test
    void testContainsAllDelegatesResultWhenUnderlyingSetReturnsFalse() {
        Collection<Integer> query = Collections.singleton(99);
        when(mockSet.containsAll(query)).thenReturn(false);
        boolean result = mockSetWrapper.containsAll(query);
        assertFalse(result);
        verify(mockSet, times(1)).containsAll(query);
    }

    @Test
    void testContainsAllDelegatesEvenWhenSealed() {
        // sealed state should not change delegation behaviour
        sealed = true;
        Collection<Integer> query = Collections.singleton(5);
        when(mockSet.containsAll(query)).thenReturn(true);
        assertTrue(mockSetWrapper.containsAll(query));
        verify(mockSet, times(1)).containsAll(query);
    }

    // ----- positive scenarios -------------------------------------------------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("positiveScenarios")
    void containsAllPositiveScenarios(Set<Object> backingSet, Collection<?> col, String description) {
        // Build a SealableSet that wraps the supplied backing set.
        @SuppressWarnings("unchecked")
        SealableSet<Object> set = new SealableSet<>(backingSet, sealedSupplier);
        assertTrue(set.containsAll(col), description);
    }

    // ----- negative scenarios -------------------------------------------------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("negativeScenarios")
    void containsAllNegativeScenarios(Set<Object> backingSet, Collection<?> col, String description) {
        @SuppressWarnings("unchecked")
        SealableSet<Object> set = new SealableSet<>(backingSet, sealedSupplier);
        assertFalse(set.containsAll(col), description);
    }

    // ----- exceptional scenarios ---------------------------------------------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionalScenarios")
    void containsAllExceptionalScenarios(Set<Object> backingSet, Collection<?> col, Class<? extends Throwable> expectedException, String description) {
        @SuppressWarnings("unchecked")
        SealableSet<Object> set = new SealableSet<>(backingSet, sealedSupplier);
        assertThrows(expectedException, () -> set.containsAll(col), description);
    }

    // The unmodifiable‑set case is a normal positive test; we handle it with a dedicated test.
    @Test
    void containsAllWithUnmodifiableBackingSet() {
        Set<String> backing = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("a")));
        SealableSet<String> set = new SealableSet<>(backing, sealedSupplier);
        assertTrue(set.containsAll(Arrays.asList("a")));
    }

    // ----- duplicate elements in collection ----------------------------------
    @Test
    void containsAllDuplicateElementsInCollection() {
        Set<String> backing = new HashSet<>(Arrays.asList("a", "b"));
        Collection<String> col = Arrays.asList("a", "a", "b");
        SealableSet<String> set = new SealableSet<>(backing, sealedSupplier);
        assertTrue(set.containsAll(col), "Duplicate entries in the collection should be ignored.");
    }

    // ----- empty backing set with non‑empty collection ------------------------
    @Test
    void containsAllEmptyBackingSetNonEmptyCollection() {
        Set<String> backing = new HashSet<>();
        Collection<String> col = Arrays.asList("x");
        SealableSet<String> set = new SealableSet<>(backing, sealedSupplier);
        assertFalse(set.containsAll(col), "Empty backing set cannot contain any element of a non‑empty collection.");
    }

    // ----- collection same reference as backing set -------------------------
    @Test
    void containsAllCollectionSameReferenceAsBackingSet() {
        Set<Integer> backing = new HashSet<>(Arrays.asList(1, 2, 3));
        // The collection argument is the exact same object as the backing set.
        SealableSet<Integer> set = new SealableSet<>(backing, sealedSupplier);
        assertTrue(set.containsAll(backing), "When the argument collection is the exact same object as the backing set, containsAll should trivially return true.");
    }

    // ----- mixed type elements ------------------------------------------------
    @Test
    void containsAllMixedTypeElementsPositive() {
        Set<Object> backing = new HashSet<>(Arrays.asList(1, "two", null));
        Collection<Object> col = Arrays.asList(1, "two", null);
        SealableSet<Object> set = new SealableSet<>(backing, sealedSupplier);
        assertTrue(set.containsAll(col), "Mixed runtime types present in both backing set and collection should return true.");
    }

    @Test
    void containsAllMixedTypeElementsNegative() {
        Set<Object> backing = new HashSet<>(Arrays.asList(1, "two", null));
        Collection<Object> col = Arrays.asList(1, "two", 3);
        SealableSet<Object> set = new SealableSet<>(backing, sealedSupplier);
        assertFalse(set.containsAll(col), "A collection element of a type not present in the backing set should cause containsAll to return false.");
    }

    // ----- exception propagation from collection -------------------------------
    @Test
    void containsAllExceptionPropagationFromCollection() {
        Set<String> backing = new HashSet<>(Arrays.asList("a", "b"));
        ThrowingCollection<String> throwingCol = new ThrowingCollection<>();
        SealableSet<String> set = new SealableSet<>(backing, sealedSupplier);
        assertThrows(RuntimeException.class, () -> set.containsAll(throwingCol), "Exceptions thrown while iterating the supplied collection must be propagated unchanged.");
    }

    // ----- ClassCastException scenario ----------------------------------------
    @Test
    void containsAllClassCastExceptionScenario() {
        // TreeSet enforces natural ordering; inserting a String will cause a ClassCastException.
        Set<Integer> backing = new java.util.TreeSet<>(Arrays.asList(1, 2, 3));
        Collection<String> col = Arrays.asList("a");
        SealableSet<Integer> set = new SealableSet<>(backing, sealedSupplier);
        assertThrows(ClassCastException.class, () -> set.containsAll(col), "TreeSet with incompatible element types should throw ClassCastException during containsAll.");
    }
}
