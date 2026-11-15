package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class SealableNavigableSet_containsAll_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    /**
     * A collection that throws a {@link RuntimeException} when its iterator
     * is obtained.  Used to verify exception propagation.
     */
    private static class ThrowingCollection<E> implements Collection<E> {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Iterator<E> iterator() {
            throw new RuntimeException("iteration failure");
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return a;
        }

        @Override
        public boolean add(E e) {
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default set: a mutable TreeSet that permits null (nullsFirst comparator)
        set = new SealableNavigableSet<>(sealedSupplier);
        // Populate with a null and three integers – this matches the original framework
        set.add(null);
        set.add(10);
        set.add(20);
        set.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references for GC
        set = null;
    }

    /**
     * Replaces the private {@code navSet} field of a {@link SealableNavigableSet}
     * with the supplied {@link NavigableSet}.  Used to inject custom backing
     * sets (e.g. a TreeSet with natural ordering, a sub‑set view, etc.).
     */
    private static <E> void setBackingNavSet(SealableNavigableSet<E> sealable, NavigableSet<E> backing) {
        try {
            Field f = SealableNavigableSet.class.getDeclaredField("navSet");
            f.setAccessible(true);
            f.set(sealable, backing);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Supplies arguments for the standard and edge‑case groups that can be
     * exercised with the default {@code set} initialised in {@link #setupBeforeEach()}.
     */
    private static Stream<Arguments> standardAndEdgeCases() {
        return Stream.of(// Standard behavior – all elements present
        Arguments.of("Standard behavior - all elements present", List.of(10, 20, 30), true, "All elements are present in the set"), // Standard behavior – missing element
        Arguments.of("Standard behavior - missing element", List.of(10, 99), false, "One element (99) is not present"), // Edge case – empty collection
        Arguments.of("Edge cases - empty collection", Collections.emptyList(), true, "Empty collection is trivially contained"), // Edge case – collection with null element (set permits null)
        Arguments.of("Edge cases - collection with null element (set permits null)", Collections.singletonList(null), true, "Null is present in the backing set"), // Edge case – duplicate elements in collection
        Arguments.of("Duplicate elements in collection", List.of(10, 10, 20), true, "Duplicates do not affect the result"), // Edge case – duplicate null elements (set permits null)
        Arguments.of("Duplicate null elements (set permits null)", List.of(null, null), true, "Presence of null once is sufficient"), // Edge case – self collection (same reference) returns true
        Arguments.of("Self collection (same reference) returns true", // placeholder, will be replaced in test body
        null, true, "A copy of the set should be fully contained"));
    }

    @Test
    void containsAll_allElementsPresent_returnsTrue() {
        Collection<Integer> col = Arrays.asList(10, 20);
        assertTrue(set.containsAll(col));
    }

    @Test
    void containsAll_someElementsMissing_returnsFalse() {
        Collection<Integer> col = Arrays.asList(10, 40);
        assertFalse(set.containsAll(col));
    }

    @Test
    void containsAll_emptyCollection_returnsTrue() {
        Collection<Integer> col = Collections.emptyList();
        assertTrue(set.containsAll(col));
    }

    @Test
    void containsAll_collectionContainsNull_elementPresent_returnsTrue() {
        Collection<Integer> col = Collections.singletonList(null);
        assertTrue(set.containsAll(col));
    }

    @Test
    void containsAll_collectionContainsNull_elementAbsent_returnsFalse() {
        // Create a set without the null element
        SealableNavigableSet<Integer> noNullSet = new SealableNavigableSet<>(sealedSupplier);
        noNullSet.add(10);
        noNullSet.add(20);
        noNullSet.add(30);
        Collection<Integer> col = Collections.singletonList(null);
        assertFalse(noNullSet.containsAll(col));
    }

    @Test
    void containsAll_whenSealedStateTrue_behavesSameAsWhenUnsealed() {
        // simulate sealed (read‑only) state
        sealedState = true;
        Collection<Integer> col = List.of(10, 30);
        assertTrue(set.containsAll(col), "Sealed state must not affect read‑only operation");
        Collection<Integer> missing = List.of(10, 99);
        assertFalse(set.containsAll(missing), "Sealed state must not affect read‑only operation");
    }

    @Test
    void containsAll_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> set.containsAll(null), "Passing null collection must throw NPE");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("standardAndEdgeCases")
    void containsAll_parameterised(String group, List<Integer> input, boolean expected, String description) {
        // For the "self collection" case we need a collection that is a copy of the set
        Collection<Integer> col;
        if (input == null) {
            col = new ArrayList<>(set);
        } else {
            col = input;
        }
        assertEquals(expected, set.containsAll(col), description);
    }

    @Test
    void containsAll_collectionWithNull_elementNotPermitted_throwsNPE() {
        // Backing set: natural ordering TreeSet (does NOT allow null)
        NavigableSet<Integer> natural = new TreeSet<>();
        natural.addAll(List.of(1, 2, 3));
        SealableNavigableSet<Integer> naturalSet = new SealableNavigableSet<>(sealedSupplier);
        setBackingNavSet(naturalSet, natural);
        Collection<Integer> col = List.of((Integer) null);
        assertThrows(NullPointerException.class, () -> naturalSet.containsAll(col), "TreeSet with natural ordering should throw NPE for null");
    }

    @Test
    void containsAll_collectionThrowsDuringIteration_propagatesException() {
        Collection<Integer> throwing = new ThrowingCollection<>();
        assertThrows(RuntimeException.class, () -> set.containsAll(throwing), "Exception from collection iteration must propagate");
    }

    @Test
    void containsAll_incompatibleType_throwsClassCastException() {
        // Backing set with natural ordering (Integer)
        NavigableSet<Integer> natural = new TreeSet<>(List.of(1, 2, 3));
        SealableNavigableSet<Integer> naturalSet = new SealableNavigableSet<>(sealedSupplier);
        setBackingNavSet(naturalSet, natural);
        Collection<String> col = List.of("string");
        assertThrows(ClassCastException.class, () -> naturalSet.containsAll(col), "Incompatible element type should cause ClassCastException");
    }

    @Test
    void containsAll_rangeViolation_outOfSubsetBounds_throwsIllegalArgumentException() {
        // Create a TreeSet with values 1‑4 and then a subSet view 1..4 (inclusive)
        NavigableSet<Integer> base = new TreeSet<>(IntStream.rangeClosed(1, 4).boxed().collect(Collectors.toList()));
        NavigableSet<Integer> sub = base.subSet(1, true, 4, true);
        SealableNavigableSet<Integer> subSetWrapper = new SealableNavigableSet<>(sealedSupplier);
        setBackingNavSet(subSetWrapper, sub);
        // 5 is outside the sub‑set range
        Collection<Integer> col = List.of(5);
        assertThrows(IllegalArgumentException.class, () -> subSetWrapper.containsAll(col), "containsAll should propagate IllegalArgumentException for out‑of‑range element");
    }

    @Test
    void containsAll_largeCollection_allElementsPresent_returnsTrue() {
        // Populate set with 1‑1000
        SealableNavigableSet<Integer> largeSet = new SealableNavigableSet<>(sealedSupplier);
        largeSet.addAll(IntStream.rangeClosed(1, 1000).boxed().collect(Collectors.toList()));
        List<Integer> col = IntStream.rangeClosed(1, 1000).boxed().collect(Collectors.toList());
        assertTrue(largeSet.containsAll(col), "Large collection that matches the set should return true");
    }

    @Test
    void containsAll_emptySet_nonEmptyCollection_returnsFalse() {
        SealableNavigableSet<Integer> emptySet = new SealableNavigableSet<>(sealedSupplier);
        // Ensure underlying navSet is an empty TreeSet (natural ordering)
        setBackingNavSet(emptySet, new TreeSet<>());
        Collection<Integer> col = List.of(5, 6, 7);
        assertFalse(emptySet.containsAll(col), "Non‑empty collection cannot be contained in an empty set");
    }

    @Test
    void containsAll_emptySet_emptyCollection_returnsTrue() {
        SealableNavigableSet<Integer> emptySet = new SealableNavigableSet<>(sealedSupplier);
        setBackingNavSet(emptySet, new TreeSet<>());
        Collection<Integer> col = Collections.emptyList();
        assertTrue(emptySet.containsAll(col), "Empty collection is trivially contained in an empty set");
    }

    @Test
    void containsAll_mixedNullAndNonNull_setPermitsNull_returnsTrue() {
        // Backing set that permits null via nullsFirst comparator
        NavigableSet<Integer> withNull = new TreeSet<>(Comparator.nullsFirst(Comparator.naturalOrder()));
        withNull.addAll(List.of(null, 2, 3));
        SealableNavigableSet<Integer> mixedSet = new SealableNavigableSet<>(sealedSupplier);
        setBackingNavSet(mixedSet, withNull);
        Collection<Integer> col = List.of(null, 2);
        assertTrue(mixedSet.containsAll(col), "Collection with null and present element should return true");
    }
}
