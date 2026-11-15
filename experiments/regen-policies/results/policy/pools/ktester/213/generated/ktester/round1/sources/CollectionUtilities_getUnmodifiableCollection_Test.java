package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_getUnmodifiableCollection_Test {

    /**
     * Minimal {@link SortedSet} implementation that delegates to an internal {@link TreeSet}
     * but does **not** implement {@link NavigableSet}.
     */
    private static class SimpleSortedSet<E> implements SortedSet<E> {

        private final TreeSet<E> delegate = new TreeSet<>();

        @Override
        public Comparator<? super E> comparator() {
            return delegate.comparator();
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

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        public Iterator<E> iterator() {
            return delegate.iterator();
        }

        @Override
        public Object[] toArray() {
            return delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return delegate.toArray(a);
        }

        @Override
        public boolean add(E e) {
            return delegate.add(e);
        }

        @Override
        public boolean remove(Object o) {
            return delegate.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return delegate.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return delegate.addAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return delegate.retainAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return delegate.removeAll(c);
        }

        @Override
        public void clear() {
            delegate.clear();
        }
    }

    /**
     * Data for the null‑collection group.
     */
    static Stream<Arguments> nullCollectionProvider() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "Collection must not be null"));
    }

    /**
     * Data for the NavigableSet group.
     */
    static Stream<Arguments> navigableSetProvider() {
        return Stream.of(// non‑empty set
        Arguments.of(new TreeSet<>(Arrays.asList(1, 2, 3)), NavigableSet.class, 3, List.of(1, 2, 3), false, false), // empty set
        Arguments.of(new TreeSet<Integer>(), NavigableSet.class, 0, List.of(), false, false), // single element – test mutating add
        Arguments.of(new TreeSet<>(Collections.singletonList(5)), NavigableSet.class, 1, List.of(5), true, false), // reflect underlying changes
        Arguments.of(new TreeSet<>(Arrays.asList(7, 8)), NavigableSet.class, 2, List.of(7, 8), false, true));
    }

    /**
     * Data for the SortedSet‑only (non‑Navigable) group.
     */
    static Stream<Arguments> sortedSetProvider() {
        return Stream.of(// pre‑wrapped unmodifiable sorted set with two elements
        Arguments.of(Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(4, 6))), SortedSet.class, 2, List.of(4, 6), false, false), // empty pre‑wrapped sorted set
        Arguments.of(Collections.unmodifiableSortedSet(new TreeSet<Integer>()), SortedSet.class, 0, List.of(), false, false), // single element – test mutating remove
        Arguments.of(Collections.unmodifiableSortedSet(new TreeSet<>(Collections.singleton(11))), SortedSet.class, 1, List.of(11), true, false), // plain TreeSet (NavigableSet) but we treat as SortedSet after wrapping
        Arguments.of(new TreeSet<>(Arrays.asList(12, 13)), SortedSet.class, 2, List.of(12, 13), false, true));
    }

    /**
     * Data for the Set group.
     */
    static Stream<Arguments> setProvider() {
        return Stream.of(Arguments.of(new HashSet<>(Arrays.asList("a", "b", "c")), Set.class, 3, List.of("a", "b", "c"), false, false), Arguments.of(new HashSet<String>(), Set.class, 0, List.of(), false, false), Arguments.of(new HashSet<>(Collections.singleton("x")), Set.class, 1, List.of("x"), true, false), Arguments.of(new HashSet<>(Collections.singleton("y")), Set.class, 1, List.of("y"), false, true));
    }

    /**
     * Data for the List group.
     */
    static Stream<Arguments> listProvider() {
        return Stream.of(Arguments.of(Arrays.asList(10, 20, 30), List.class, 3, List.of(10, 20, 30), false, false), Arguments.of(new ArrayList<Integer>(), List.class, 0, List.of(), false, false), Arguments.of(new ArrayList<>(Collections.singletonList("elem")), List.class, 1, List.of("elem"), true, false), Arguments.of(new ArrayList<>(Arrays.asList("a", "b")), List.class, 2, List.of("a", "b"), false, true));
    }

    /**
     * Data for the generic Collection (non‑List/Set) group.
     */
    static Stream<Arguments> otherCollectionProvider() {
        return Stream.of(Arguments.of(new ArrayDeque<>(Arrays.asList(100, 200)), Collection.class, 2, List.of(100, 200), false, false), Arguments.of(new ArrayDeque<Integer>(), Collection.class, 0, List.of(), false, false), Arguments.of(new ArrayDeque<>(Collections.singleton(999)), Collection.class, 1, List.of(999), true, false), Arguments.of(new ArrayDeque<>(Arrays.asList(1, 2, 3)), Collection.class, 3, List.of(1, 2, 3), false, true));
    }

    /**
     * Data for iterator‑remove tests (one entry per collection type).
     */
    static Stream<Arguments> iteratorRemoveProvider() {
        return Stream.of(Arguments.of(new TreeSet<>(Arrays.asList(1, 2, 3))), Arguments.of(Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(4, 5)))), Arguments.of(new HashSet<>(Collections.singleton("x"))), Arguments.of(Arrays.asList(10, 20, 30)), Arguments.of(new ArrayDeque<>(Arrays.asList(100, 200))));
    }

    /**
     * Data for direct mutating add tests on the unmodifiable view.
     */
    static Stream<Arguments> directAddProvider() {
        return Stream.of(Arguments.of(new HashSet<>(Collections.singleton("a")), "b"), Arguments.of(new ArrayList<>(Collections.singletonList(1)), 2), Arguments.of(new ArrayDeque<>(Collections.singleton(999)), 1000));
    }

    @Test
    void shouldThrowNPEWhenCollectionIsNull() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CollectionUtilities.getUnmodifiableCollection(null));
        assertEquals("Collection must not be null", ex.getMessage());
    }

    @Test
    void shouldReturnUnmodifiableNavigableSet() {
        NavigableSet<String> original = new TreeSet<>(List.of("a", "b", "c"));
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(original);
        // Verify type
        assertTrue(result instanceof NavigableSet, "Result should be a NavigableSet");
        // Verify unmodifiable behavior
        assertThrows(UnsupportedOperationException.class, () -> ((NavigableSet<String>) result).add("d"));
    }

    @Test
    void shouldReturnUnmodifiableSortedSetWhenNotNavigableSet() {
        SortedSet<Integer> original = new SimpleSortedSet<>();
        original.addAll(List.of(3, 1, 2));
        Collection<Integer> result = CollectionUtilities.getUnmodifiableCollection(original);
        assertTrue(result instanceof SortedSet, "Result should be a SortedSet");
        assertFalse(result instanceof NavigableSet, "Result must not be a NavigableSet");
        assertThrows(UnsupportedOperationException.class, () -> ((SortedSet<Integer>) result).add(4));
    }

    @Test
    void shouldReturnUnmodifiableSet() {
        Set<Double> original = new HashSet<>(Set.of(1.1, 2.2));
        Collection<Double> result = CollectionUtilities.getUnmodifiableCollection(original);
        assertTrue(result instanceof Set, "Result should be a Set");
        assertThrows(UnsupportedOperationException.class, () -> ((Set<Double>) result).add(3.3));
    }

    @Test
    void shouldReturnUnmodifiableList() {
        List<String> original = new ArrayList<>(List.of("x", "y"));
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(original);
        assertTrue(result instanceof List, "Result should be a List");
        assertThrows(UnsupportedOperationException.class, () -> ((List<String>) result).add("z"));
    }

    @Test
    void shouldReturnUnmodifiableCollectionForOtherTypes() {
        // ArrayDeque implements Collection but is neither Set nor List.
        Collection<String> original = new ArrayDeque<>(List.of("one", "two"));
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(original);
        // The exact runtime type is not important; we just need it to be unmodifiable.
        assertThrows(UnsupportedOperationException.class, () -> result.add("three"));
    }

    @Test
    void returnedCollectionShouldReflectOriginalElements() {
        List<Integer> original = List.of(10, 20, 30);
        Collection<Integer> result = CollectionUtilities.getUnmodifiableCollection(original);
        assertEquals(original, new ArrayList<>(result), "Elements must be identical and in order");
    }

    @ParameterizedTest(name = "[{index}] NavigableSet test – size={2}, mutable={4}")
    @MethodSource("navigableSetProvider")
    void testNavigableSetBranch(NavigableSet<Integer> original, Class<?> expectedInterface, int expectedSize, List<Integer> expectedElements, boolean triggerMutatingOperation, boolean testUnderlyingReflection) {
        Collection<Integer> result = CollectionUtilities.getUnmodifiableCollection(original);
        // verify most specific type
        assertTrue(expectedInterface.isInstance(result), "Result should be instance of " + expectedInterface.getSimpleName());
        // verify size and content
        assertEquals(expectedSize, result.size());
        assertEquals(expectedElements, new ArrayList<>(result));
        // mutating operation test
        if (triggerMutatingOperation) {
            assertThrows(UnsupportedOperationException.class, () -> ((NavigableSet<Integer>) result).add(999));
        }
        // underlying‑collection reflection test
        if (testUnderlyingReflection) {
            original.add(9);
            assertTrue(result.contains(9), "Unmodifiable view must reflect additions to the underlying collection");
            assertEquals(expectedSize + 1, result.size());
        }
    }

    @ParameterizedTest(name = "[{index}] SortedSet (non‑Navigable) test")
    @MethodSource("sortedSetProvider")
    void testSortedSetBranch(SortedSet<Integer> original, Class<?> expectedInterface, int expectedSize, List<Integer> expectedElements, boolean triggerMutatingOperation, boolean testUnderlyingReflection) {
        Collection<Integer> result = CollectionUtilities.getUnmodifiableCollection(original);
        // most specific type
        assertTrue(expectedInterface.isInstance(result));
        assertFalse(result instanceof NavigableSet, "Result must not be a NavigableSet");
        assertEquals(expectedSize, result.size());
        assertEquals(expectedElements, new ArrayList<>(result));
        if (triggerMutatingOperation) {
            assertThrows(UnsupportedOperationException.class, () -> ((SortedSet<Integer>) result).remove(11));
        }
        if (testUnderlyingReflection) {
            // original may be a NavigableSet (TreeSet) – still reflects changes
            original.add(14);
            assertTrue(result.contains(14));
            assertEquals(expectedSize + 1, result.size());
        }
    }

    @ParameterizedTest(name = "[{index}] Set branch")
    @MethodSource("setProvider")
    void testSetBranch(Set<String> original, Class<?> expectedInterface, int expectedSize, List<String> expectedElements, boolean triggerMutatingOperation, boolean testUnderlyingReflection) {
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(original);
        assertTrue(expectedInterface.isInstance(result));
        assertEquals(expectedSize, result.size());
        // order is not guaranteed for Set – compare as a Set
        assertEquals(new HashSet<>(expectedElements), new HashSet<>(result));
        if (triggerMutatingOperation) {
            assertThrows(UnsupportedOperationException.class, () -> ((Set<String>) result).clear());
        }
        if (testUnderlyingReflection) {
            original.add("z");
            assertTrue(result.contains("z"));
            assertEquals(expectedSize + 1, result.size());
        }
    }

    @ParameterizedTest(name = "[{index}] List branch")
    @MethodSource("listProvider")
    void testListBranch(List<?> original, Class<?> expectedInterface, int expectedSize, List<?> expectedElements, boolean triggerMutatingOperation, boolean testUnderlyingReflection) {
        Collection<?> result = CollectionUtilities.getUnmodifiableCollection(original);
        assertTrue(expectedInterface.isInstance(result));
        assertEquals(expectedSize, result.size());
        assertEquals(expectedElements, new ArrayList<>(result));
        if (triggerMutatingOperation) {
            assertThrows(UnsupportedOperationException.class, () -> ((List<?>) result).set(0, "new"));
        }
        if (testUnderlyingReflection) {
            // original is mutable – add an element and verify view updates
            if (original instanceof List) {
                ((List<Object>) original).add("c");
            }
            assertTrue(result.contains("c"));
            assertEquals(expectedSize + 1, result.size());
        }
    }

    @ParameterizedTest(name = "[{index}] Other Collection branch")
    @MethodSource("otherCollectionProvider")
    void testOtherCollectionBranch(Collection<Integer> original, Class<?> expectedInterface, int expectedSize, List<Integer> expectedElements, boolean triggerMutatingOperation, boolean testUnderlyingReflection) {
        Collection<Integer> result = CollectionUtilities.getUnmodifiableCollection(original);
        assertTrue(expectedInterface.isInstance(result));
        assertEquals(expectedSize, result.size());
        assertEquals(expectedElements, new ArrayList<>(result));
        if (triggerMutatingOperation) {
            assertThrows(UnsupportedOperationException.class, () -> result.remove(999));
        }
        if (testUnderlyingReflection) {
            original.add(4);
            assertTrue(result.contains(4));
            assertEquals(expectedSize + 1, result.size());
        }
    }

    @ParameterizedTest(name = "[{index}] iterator().remove() must throw UOE")
    @MethodSource("iteratorRemoveProvider")
    void iteratorRemoveShouldThrowUnsupportedOperation(Collection<?> original) {
        Collection<?> result = CollectionUtilities.getUnmodifiableCollection(original);
        Iterator<?> it = result.iterator();
        assertTrue(it.hasNext());
        // advance to first element
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @ParameterizedTest(name = "[{index}] direct add() must throw UOE")
    @MethodSource("directAddProvider")
    void directAddShouldThrowUnsupportedOperation(Object original, Object elementToAdd) {
        Collection<?> result = CollectionUtilities.getUnmodifiableCollection((Collection<?>) original);
        assertThrows(UnsupportedOperationException.class, () -> result.add(elementToAdd));
    }
}
