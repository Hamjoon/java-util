package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.Point;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class SealableList_containsAll_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    /**
     * A class with a broken hashCode implementation (different from equals).
     */
    private static final class BadHash {

        private final int value;

        BadHash(int v) {
            this.value = v;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof BadHash && ((BadHash) o).value == value;
        }

        // Deliberately return a constant that does NOT match the value hash.
        @Override
        public int hashCode() {
            return 42;
        }
    }

    /**
     * A class whose hashCode throws a RuntimeException.
     */
    private static final class BadHashCode {

        @Override
        public int hashCode() {
            throw new RuntimeException("hashCode explosion");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    /**
     * A class whose equals throws a RuntimeException.
     */
    private static final class ExplodingEquals {

        private final int v;

        ExplodingEquals(int v) {
            this.v = v;
        }

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("equals explosion");
        }

        @Override
        public int hashCode() {
            return v;
        }
    }

    /**
     * Collection whose iterator.next() throws a RuntimeException.
     */
    private static final class BadIteratorCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public E next() {
                    throw new RuntimeException("iterator next explosion");
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public int size() {
            return 1;
        }
    }

    /**
     * Collection that mutates itself during iteration, causing CME.
     */
    private static final class ConcurrentModifyingCollection<E> extends AbstractCollection<E> {

        private final List<E> internal = new ArrayList<>();

        ConcurrentModifyingCollection(E... elements) {
            Collections.addAll(internal, elements);
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                private final Iterator<E> it = internal.iterator();

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public E next() {
                    // Mutate the list on the first call to provoke CME on the next call.
                    if (!internal.isEmpty()) {
                        internal.remove(0);
                    }
                    return it.next();
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        @Override
        public int size() {
            return internal.size();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the list starts in an un‑sealed, known state before each test.
        sealedState = false;
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        list.add(10);
        list.add(20);
        list.add(30);
        // add a null element to verify null handling
        list.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and guarantee a clean slate for the next test.
        list = null;
    }

    @Test
    void containsAll_AllElementsPresent_ReturnsTrue() {
        Collection<Integer> col = Arrays.asList(10, 20, 30);
        assertTrue(list.containsAll(col), "Should return true when all elements are present");
    }

    @Test
    void containsAll_MissingElement_ReturnsFalse() {
        Collection<Integer> col = Arrays.asList(10, 99);
        assertFalse(list.containsAll(col), "Should return false when a collection element is missing");
    }

    @Test
    void containsAll_EmptyCollection_ReturnsTrue() {
        Collection<Integer> empty = Collections.emptyList();
        assertTrue(list.containsAll(empty), "An empty collection is always contained");
    }

    @Test
    void containsAll_CollectionWithNull_ElementPresent_ReturnsTrue() {
        Collection<Integer> col = Arrays.asList(null, 20);
        assertTrue(list.containsAll(col), "Should handle null elements correctly");
    }

    @Test
    void containsAll_CollectionWithNull_ElementAbsent_ReturnsFalse() {
        // Remove null from the list first
        list.remove(null);
        Collection<Integer> col = Collections.singletonList(null);
        assertFalse(list.containsAll(col), "Should return false when null is not present");
    }

    @Test
    void containsAll_DifferentTypeElements_ReturnsFalse() {
        // The list holds Integer, but we pass a collection of Strings.
        Collection<String> col = Arrays.asList("10", "20");
        // The raw type of containsAll accepts Collection<?>, so this compiles.
        @SuppressWarnings("unchecked")
        boolean result = list.containsAll((Collection<?>) col);
        assertFalse(result, "Should return false when collection elements are of a different type");
    }

    @Test
    void containsAll_WhenSealed_StillWorks() {
        // make the list immutable – this should not affect containsAll
        sealedState = true;
        Collection<Integer> col = Arrays.asList(10, 20);
        assertTrue(list.containsAll(col), "containsAll must work regardless of sealed state");
    }

    @Test
    void containsAll_DoesNotModifyList() {
        List<Integer> before = new ArrayList<>(list);
        Collection<Integer> col = Arrays.asList(10, 30);
        assertTrue(list.containsAll(col));
        assertEquals(before, list, "The list content must remain unchanged after containsAll");
    }

    @Test
    void containsAll_BasicAllPresent() {
        SealableList<String> l = new SealableList<>(new ArrayList<>(Arrays.asList("a", "b", "c")), sealedSupplier);
        assertTrue(l.containsAll(Arrays.asList("b", "c")), "The underlying list contains all elements of the collection; should return true.");
    }

    @Test
    void containsAll_BasicMissingElement() {
        SealableList<Integer> l = new SealableList<>(new ArrayList<>(Arrays.asList(1, 2, 3, 4)), sealedSupplier);
        assertFalse(l.containsAll(Arrays.asList(2, 5)), "Collection contains an element not present in the list; should return false.");
    }

    @Test
    void containsAll_BothEmpty_ReturnsTrue() {
        SealableList<Object> l = new SealableList<>(new ArrayList<>(), sealedSupplier);
        assertTrue(l.containsAll(Collections.emptyList()), "Both list and collection are empty; containsAll must return true.");
    }

    @Test
    void containsAll_ListEmpty_CollectionNotEmpty_ReturnsFalse() {
        SealableList<Object> l = new SealableList<>(new ArrayList<>(), sealedSupplier);
        assertFalse(l.containsAll(Arrays.asList("x")), "List empty but collection not; should return false.");
    }

    @Test
    void containsAll_CollectionEmpty_ReturnsTrue() {
        SealableList<String> l = new SealableList<>(new ArrayList<>(Arrays.asList("x", "y")), sealedSupplier);
        assertTrue(l.containsAll(Collections.emptyList()), "Empty collection should always be considered contained.");
    }

    @Test
    void containsAll_NullInBothListAndCollection_ReturnsTrue() {
        SealableList<String> l = new SealableList<>(new ArrayList<>(Arrays.asList("a", null, "c")), sealedSupplier);
        assertTrue(l.containsAll(Collections.singletonList(null)), "Both list and collection contain null; HashSet handles null correctly.");
    }

    @Test
    void containsAll_NullInCollectionOnly_ReturnsFalse() {
        SealableList<String> l = new SealableList<>(new ArrayList<>(Arrays.asList("a", "b")), sealedSupplier);
        assertFalse(l.containsAll(Collections.singletonList(null)), "Collection contains null but list does not; should return false.");
    }

    @Test
    void containsAll_NullCollection_ThrowsNPE() {
        SealableList<String> l = new SealableList<>(new ArrayList<>(Arrays.asList("a", "b")), sealedSupplier);
        assertThrows(NullPointerException.class, () -> l.containsAll(null), "Passing a null collection must throw NullPointerException.");
    }

    @Test
    void containsAll_DuplicateElements_AllPresent_ReturnsTrue() {
        SealableList<String> l = new SealableList<>(new ArrayList<>(Arrays.asList("x", "y", "z")), sealedSupplier);
        assertTrue(l.containsAll(Arrays.asList("y", "y", "z")), "Duplicates in collection are ignored by HashSet.containsAll.");
    }

    @Test
    void containsAll_DuplicateElements_MissingOne_ReturnsFalse() {
        SealableList<String> l = new SealableList<>(new ArrayList<>(Arrays.asList("x", "y")), sealedSupplier);
        assertFalse(l.containsAll(Arrays.asList("y", "y", "w")), "Missing element (w) makes the result false even with duplicates.");
    }

    @Test
    void containsAll_PointEquals_True() {
        List<Point> backing = new ArrayList<>(Arrays.asList(new Point(1, 2), new Point(3, 4)));
        SealableList<Point> l = new SealableList<>(backing, sealedSupplier);
        assertTrue(l.containsAll(Collections.singletonList(new Point(1, 2))), "Points implement proper equals/hashCode; should be found.");
    }

    @Test
    void containsAll_PointEquals_False() {
        List<Point> backing = new ArrayList<>(Arrays.asList(new Point(1, 2), new Point(3, 4)));
        SealableList<Point> l = new SealableList<>(backing, sealedSupplier);
        assertFalse(l.containsAll(Collections.singletonList(new Point(5, 6))), "Point not present; should return false.");
    }

    @Test
    void containsAll_ListDuplicates_Ignored_ReturnsTrue() {
        SealableList<String> l = new SealableList<>(new ArrayList<>(Arrays.asList("a", "a", "b")), sealedSupplier);
        assertTrue(l.containsAll(Arrays.asList("a", "b")), "Presence matters, not multiplicity.");
    }

    @Test
    void containsAll_ListDuplicates_CollectionMoreOccurrences_ReturnsTrue() {
        SealableList<String> l = new SealableList<>(new ArrayList<>(Arrays.asList("a", "a", "b")), sealedSupplier);
        assertTrue(l.containsAll(Arrays.asList("a", "a", "b", "b")), "Even though collection repeats 'b', only presence is checked.");
    }

    @Test
    void containsAll_HashSetArgument_ReturnsTrue() {
        SealableList<String> l = new SealableList<>(new ArrayList<>(Arrays.asList("x", "y")), sealedSupplier);
        Set<String> set = new HashSet<>(Collections.singletonList("x"));
        assertTrue(l.containsAll(set), "HashSet argument with present element should return true.");
    }

    @Test
    void containsAll_TreeSetArgument_ReturnsFalse() {
        SealableList<String> l = new SealableList<>(new ArrayList<>(Arrays.asList("x", "y")), sealedSupplier);
        Set<String> set = new TreeSet<>(Collections.singletonList("z"));
        assertFalse(l.containsAll(set), "TreeSet argument with missing element should return false.");
    }

    @Test
    void containsAll_SameInstance_ReturnsTrue() {
        SealableList<String> l = new SealableList<>(new ArrayList<>(Arrays.asList("p", "q")), sealedSupplier);
        // Pass the underlying list itself (as a Collection)
        assertTrue(l.containsAll(l), "A collection is always a superset of itself.");
    }

    @Test
    void containsAll_BadHash_Mismatch_ReturnsFalse() {
        SealableList<BadHash> l = new SealableList<>(new ArrayList<>(Collections.singletonList(new BadHash(1))), sealedSupplier);
        assertFalse(l.containsAll(Collections.singletonList(new BadHash(1))), "Different hashCode causes HashSet lookup to fail, yielding false.");
    }

    @Test
    void containsAll_TypeMismatch_NoEquals_ReturnsFalse() {
        SealableList<Integer> l = new SealableList<>(new ArrayList<>(Arrays.asList(1, 2, 3)), sealedSupplier);
        assertFalse(l.containsAll(Collections.singletonList(1L)), "Long is not equal to Integer; containsAll must return false.");
    }

    @Test
    void containsAll_ExplodingEquals_PropagatesRuntimeException() {
        SealableList<ExplodingEquals> l = new SealableList<>(new ArrayList<>(Collections.singletonList(new ExplodingEquals(5))), sealedSupplier);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> l.containsAll(Collections.singletonList(new ExplodingEquals(5))), "equals() throwing should propagate as RuntimeException.");
        assertEquals("equals explosion", ex.getMessage());
    }

    @Test
    void containsAll_UnderlyingListNull_ThrowsNPE() {
        SealableList<Object> l = new SealableList<>(null, sealedSupplier);
        assertThrows(NullPointerException.class, () -> l.containsAll(Collections.emptyList()), "Constructing with null backing list should cause NPE when containsAll builds the HashSet.");
    }

    @Test
    void containsAll_ListElementBadHashCode_PropagatesRuntimeException() {
        SealableList<BadHashCode> l = new SealableList<>(new ArrayList<>(Collections.singletonList(new BadHashCode())), sealedSupplier);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> l.containsAll(Collections.emptyList()), "hashCode() throwing while building the HashSet must propagate.");
        assertEquals("hashCode explosion", ex.getMessage());
    }

    @Test
    void containsAll_CollectionElementBadHashCode_PropagatesRuntimeException() {
        SealableList<Object> l = new SealableList<>(new ArrayList<>(), sealedSupplier);
        Collection<BadHashCode> badCol = Collections.singletonList(new BadHashCode());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> l.containsAll(badCol), "hashCode() throwing during HashSet.containsAll must propagate.");
        assertEquals("hashCode explosion", ex.getMessage());
    }

    @Test
    void containsAll_BadIteratorCollection_PropagatesRuntimeException() {
        SealableList<Object> l = new SealableList<>(new ArrayList<>(), sealedSupplier);
        BadIteratorCollection<Object> badIter = new BadIteratorCollection<>();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> l.containsAll(badIter), "Exception from iterator.next() must propagate.");
        assertEquals("iterator next explosion", ex.getMessage());
    }

    @Test
    void containsAll_ConcurrentModifyingCollection_ThrowsCME() {
        SealableList<Object> l = new SealableList<>(new ArrayList<>(), sealedSupplier);
        ConcurrentModifyingCollection<Object> concurrentCol = new ConcurrentModifyingCollection<>("a", "b");
        assertThrows(ConcurrentModificationException.class, () -> l.containsAll(concurrentCol), "Concurrent modification during iteration should result in CME.");
    }
}
