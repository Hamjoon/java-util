package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class SealableSet_retainAll_Test {

    private SealableSet<Integer> set;

    private volatile boolean sealed = false;

    private Supplier<Boolean> sealedSupplier = () -> sealed;

    /**
     * A Collection whose iterator() method throws a RuntimeException.
     */
    private static class ThrowingIteratorCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            throw new RuntimeException("Iterator failed");
        }

        @Override
        public int size() {
            return 0;
        }
    }

    /**
     * A Collection whose iterator throws ConcurrentModificationException
     * on the first call to next().
     */
    private static class ConcurrentModificationCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                private boolean first = true;

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public E next() {
                    if (first) {
                        first = false;
                        throw new ConcurrentModificationException("Concurrent modification");
                    }
                    // never reached
                    return null;
                }
            };
        }

        @Override
        public int size() {
            return 1;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh set for each test (unsealed by default)
        set = new SealableSet<>(sealedSupplier);
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset sealed flag for next test
        sealed = false;
    }

    // Helper matcher to avoid rawtype warnings
    private static Collection<?> anyCollection() {
        return any(Collection.class);
    }

    @Test
    void retainAll_Unsealed_ChangesSet_ReturnsTrue() {
        // GIVEN
        Collection<Integer> toRetain = Arrays.asList(1, null);
        // WHEN
        boolean changed = set.retainAll(toRetain);
        // THEN
        assertTrue(changed, "retainAll should return true when the set is modified");
        assertEquals(2, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(null));
        assertFalse(set.contains(2));
        assertFalse(set.contains(3));
    }

    @Test
    void retainAll_Unsealed_NoChange_ReturnsFalse() {
        // GIVEN
        Collection<Integer> toRetain = Arrays.asList(1, 2, 3, null);
        // WHEN
        boolean changed = set.retainAll(toRetain);
        // THEN
        assertFalse(changed, "retainAll should return false when the set is not modified");
        assertEquals(4, set.size());
    }

    @Test
    void retainAll_Unsealed_EmptyCollection_EmptiesSet() {
        // GIVEN
        Collection<Integer> empty = Collections.emptyList();
        // WHEN
        boolean changed = set.retainAll(empty);
        // THEN
        assertTrue(changed, "retainAll should return true when elements are removed");
        assertTrue(set.isEmpty(), "Set should be empty after retaining an empty collection");
    }

    @Test
    void retainAll_WhenSealed_ThrowsUnsupportedOperationException() {
        // GIVEN
        // make the set sealed
        sealed = true;
        Collection<Integer> any = Arrays.asList(1, 2);
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> set.retainAll(any), "retainAll should throw when the set is sealed");
    }

    @Test
    void retainAll_DelegatesToUnderlyingSet() {
        // GIVEN: a spy on the underlying Set so we can verify the delegation
        @SuppressWarnings("unchecked")
        Set<Integer> backingSet = mock(Set.class);
        // The spy must behave like a normal set for the retainAll call
        when(backingSet.retainAll(anyCollection())).thenAnswer(invocation -> {
            Collection<?> col = invocation.getArgument(0);
            // Simulate a change (return true)
            return true;
        });
        // Use the constructor that accepts a Set
        SealableSet<Integer> spySet = new SealableSet<>(backingSet, sealedSupplier);
        spySet.add(1);
        spySet.add(2);
        spySet.add(3);
        Collection<Integer> retain = Arrays.asList(1, 3);
        // WHEN
        boolean result = spySet.retainAll(retain);
        // THEN
        assertTrue(result, "Result should reflect the backing set's return value");
        verify(backingSet, times(1)).retainAll(retain);
    }

    @Test
    void retainAll_WhenSealed_ThrowsIllegalStateException() {
        // GIVEN
        // make the set sealed
        sealed = true;
        Collection<Integer> any = Arrays.asList(1, 2);
        // WHEN / THEN
        assertThrows(IllegalStateException.class, () -> set.retainAll(any), "retainAll should throw IllegalStateException when the set is sealed");
    }

    @Test
    void retainAll_NormalOperation_ChangesSet_ReturnsTrue() {
        // Set up a fresh SealableSet with {"A","C"}
        SealableSet<String> stringSet = new SealableSet<>(sealedSupplier);
        stringSet.add("A");
        stringSet.add("C");
        // Retain only "A" and "B"
        Collection<String> col = Arrays.asList("A", "B");
        boolean changed = stringSet.retainAll(col);
        assertTrue(changed, "retainAll should return true when the set is modified");
        assertEquals(Set.of("A"), new HashSet<>(stringSet));
    }

    @Test
    void retainAll_NormalOperation_NoChange_ReturnsFalse() {
        // Set up a fresh SealableSet with {"X","Y"}
        SealableSet<String> stringSet = new SealableSet<>(sealedSupplier);
        stringSet.add("X");
        stringSet.add("Y");
        // Retain collection that already matches the set
        Collection<String> col = Arrays.asList("X", "Y");
        boolean changed = stringSet.retainAll(col);
        assertFalse(changed, "retainAll should return false when the set is not modified");
        assertEquals(Set.of("X", "Y"), new HashSet<>(stringSet));
    }

    @Test
    void retainAll_NormalOperation_EmptyCollection_EmptiesSet_ReturnsTrue() {
        // Set up a fresh SealableSet with {"M","N"}
        SealableSet<String> stringSet = new SealableSet<>(sealedSupplier);
        stringSet.add("M");
        stringSet.add("N");
        boolean changed = stringSet.retainAll(Collections.emptyList());
        assertTrue(changed, "retainAll should return true when the set becomes empty");
        assertTrue(stringSet.isEmpty(), "Set should be empty after retaining an empty collection");
    }

    @Test
    void retainAll_NormalOperation_NullCollection_PropagatesNullPointerException() {
        // Set up a fresh SealableSet
        SealableSet<Integer> intSet = new SealableSet<>(sealedSupplier);
        intSet.add(1);
        intSet.add(2);
        assertThrows(NullPointerException.class, () -> intSet.retainAll(null), "Passing null collection should propagate NullPointerException");
    }

    @Test
    void retainAll_NullElementsInCollection_ChangesSet_ReturnsTrue() {
        // Underlying set: {null, "A", "B"}  (use String set for simplicity)
        SealableSet<String> stringSet = new SealableSet<>(sealedSupplier);
        stringSet.add(null);
        stringSet.add("A");
        stringSet.add("B");
        Collection<String> col = Arrays.asList(null, "A");
        boolean changed = stringSet.retainAll(col);
        assertTrue(changed, "retainAll should return true when an element is removed");
        assertEquals(Set.of(null, "A"), new HashSet<>(stringSet));
    }

    @Test
    void retainAll_NullElementsInCollection_NoChange_ReturnsFalse() {
        SealableSet<String> stringSet = new SealableSet<>(sealedSupplier);
        stringSet.add(null);
        Collection<String> col = Collections.singletonList(null);
        boolean changed = stringSet.retainAll(col);
        assertFalse(changed, "retainAll should return false when nothing changes");
        assertEquals(Set.of((String) null), new HashSet<>(stringSet));
    }

    @Test
    void retainAll_IncompatibleElementType_ThrowsClassCastException() {
        // SealableSet<Integer> but collection contains a String
        SealableSet<Integer> intSet = new SealableSet<>(sealedSupplier);
        intSet.add(1);
        intSet.add(2);
        Collection<String> badCol = Collections.singletonList("string");
        assertThrows(ClassCastException.class, () -> intSet.retainAll(badCol), "Incompatible element type should cause ClassCastException");
    }

    @Test
    void retainAll_UnderlyingSetUnsupported_ThrowsUnsupportedOperationException() {
        // Create an unmodifiable backing set
        Set<Integer> unmodifiable = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(1, 2, 3)));
        SealableSet<Integer> wrapped = new SealableSet<>(unmodifiable, sealedSupplier);
        Collection<Integer> col = Arrays.asList(1, 2);
        assertThrows(UnsupportedOperationException.class, () -> wrapped.retainAll(col), "Underlying unmodifiable set should throw UnsupportedOperationException");
    }

    @Test
    void retainAll_CollectionThrowsDuringIteration_PropagatesRuntimeException() {
        SealableSet<Integer> intSet = new SealableSet<>(sealedSupplier);
        intSet.add(1);
        intSet.add(2);
        Collection<Integer> throwing = new ThrowingIteratorCollection<>();
        assertThrows(RuntimeException.class, () -> intSet.retainAll(throwing), "Exception from collection iterator should propagate");
    }

    // ---- Collection that throws ConcurrentModificationException -----------
    @Test
    void retainAll_CollectionConcurrentModification_PropagatesException() {
        SealableSet<Integer> intSet = new SealableSet<>(sealedSupplier);
        intSet.add(1);
        intSet.add(2);
        Collection<Integer> concurrent = new ConcurrentModificationCollection<>();
        assertThrows(ConcurrentModificationException.class, () -> intSet.retainAll(concurrent), "ConcurrentModificationException should propagate");
    }
}
