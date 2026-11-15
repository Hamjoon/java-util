package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_getCheckedCollection_Test {

    /**
     * Simple custom collection that delegates to an internal {@link ArrayList}.
     * It implements {@link Collection} only (not List/Set) to hit the
     * generic {@code checkedCollection} branch.
     */
    private static class MyCollection<E> extends AbstractCollection<E> {

        private final List<E> delegate;

        MyCollection(Collection<E> source) {
            this.delegate = new ArrayList<>(source);
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
        public boolean add(E e) {
            return delegate.add(e);
        }
    }

    /**
     * Collection that deliberately fails on {@code add}.
     */
    private static class MyFailingCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean add(E e) {
            throw new IllegalStateException("Deliberate failure");
        }
    }

    /**
     * A collection that implements both {@link List} and {@link Set}.
     * Internally it stores elements in a {@link LinkedHashSet} to preserve insertion order.
     */
    private static class ListSet<E> extends AbstractList<E> implements Set<E> {

        private final LinkedHashSet<E> data = new LinkedHashSet<>();

        @Override
        public E get(int index) {
            // LinkedHashSet does not support random access; we convert to array for simplicity.
            return new ArrayList<>(data).get(index);
        }

        @Override
        public int size() {
            return data.size();
        }

        @Override
        public boolean add(E e) {
            return data.add(e);
        }

        @Override
        public Iterator<E> iterator() {
            return data.iterator();
        }

        // Set contract requires equals/hashCode; delegate to underlying set.
        @Override
        public boolean equals(Object o) {
            return data.equals(o);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }
    }

    @Test
    void getCheckedCollection_NavigableSet_ReturnsCheckedNavigableSet() {
        NavigableSet<String> original = new TreeSet<>();
        original.add("valid");
        Collection<String> checked = CollectionUtilities.getCheckedCollection(original, String.class);
        assertTrue(checked instanceof NavigableSet, "Should be a NavigableSet view");
        // Adding a correct type works
        assertDoesNotThrow(() -> checked.add("another"));
        // Adding a wrong type throws ClassCastException
        assertThrows(ClassCastException.class, () -> ((Collection) checked).add(123));
    }

    @Test
    void getCheckedCollection_SortedSet_ReturnsCheckedSortedSet() {
        // An unmodifiable SortedSet is *not* a NavigableSet, so the SortedSet branch is exercised
        SortedSet<Integer> original = Collections.unmodifiableSortedSet(new TreeSet<>(Set.of(1, 2, 3)));
        Collection<Integer> checked = CollectionUtilities.getCheckedCollection(original, Integer.class);
        assertTrue(checked instanceof SortedSet, "Should be a SortedSet view");
        assertFalse(checked instanceof NavigableSet, "Should not be a NavigableSet view");
        // Correct type succeeds
        assertDoesNotThrow(() -> ((Collection<Integer>) checked).add(4));
        // Wrong type fails
        assertThrows(ClassCastException.class, () -> ((Collection) checked).add("string"));
    }

    @Test
    void getCheckedCollection_Set_ReturnsCheckedSet() {
        Set<Double> original = new HashSet<>(Set.of(1.1, 2.2));
        Collection<Double> checked = CollectionUtilities.getCheckedCollection(original, Double.class);
        assertTrue(checked instanceof Set, "Should be a Set view");
        assertFalse(checked instanceof List, "Should not be a List view");
        // Correct type succeeds
        assertDoesNotThrow(() -> checked.add(3.3));
        // Wrong type fails
        assertThrows(ClassCastException.class, () -> ((Collection) checked).add(true));
    }

    @Test
    void getCheckedCollection_List_ReturnsCheckedList() {
        List<String> original = new ArrayList<>(List.of("a", "b"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(original, String.class);
        assertTrue(checked instanceof List, "Should be a List view");
        assertFalse(checked instanceof Set, "Should not be a Set view");
        // Correct type succeeds
        assertDoesNotThrow(() -> ((List<String>) checked).add("c"));
        // Wrong type fails
        assertThrows(ClassCastException.class, () -> ((Collection) checked).add(42));
    }

    @Test
    void getCheckedCollection_OtherCollection_ReturnsCheckedCollection() {
        // ArrayDeque implements Collection but not List, Set, SortedSet or NavigableSet
        Collection<Long> original = new ArrayDeque<>(List.of(10L, 20L));
        Collection<Long> checked = CollectionUtilities.getCheckedCollection(original, Long.class);
        assertTrue(checked instanceof Collection, "Should be a Collection view");
        assertFalse(checked instanceof List, "Should not be a List view");
        assertFalse(checked instanceof Set, "Should not be a Set view");
        // Correct type succeeds
        assertDoesNotThrow(() -> checked.add(30L));
        // Wrong type fails
        assertThrows(ClassCastException.class, () -> ((Collection) checked).add('x'));
    }

    @Test
    void getCheckedCollection_NullCollection_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> CollectionUtilities.getCheckedCollection(null, String.class));
    }

    @Test
    void getCheckedCollection_NullType_ThrowsNPE() {
        List<String> list = new ArrayList<>();
        assertThrows(NullPointerException.class, () -> CollectionUtilities.getCheckedCollection(list, null));
    }

    @Test
    void getCheckedCollection_UnmodifiableList_ThrowsUnsupportedOperationException() {
        // Underlying list is unmodifiable; the checked wrapper should forward the UOE.
        List<String> unmodifiable = Collections.unmodifiableList(new ArrayList<>(List.of("x")));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(unmodifiable, String.class);
        // The wrapper type is still a CheckedList
        assertTrue(checked instanceof List, "Wrapper should be a List");
        // Adding any element (even of correct type) fails with UOE before type checking.
        assertThrows(UnsupportedOperationException.class, () -> checked.add("new"));
        // Adding a wrong type also results in UOE (the underlying collection blocks first)
        assertThrows(UnsupportedOperationException.class, () -> ((Collection) checked).add(123));
    }

    @Test
    void getCheckedCollection_SupertypeType_AcceptsSubtypes() {
        // List<Integer> but we check against Number.class (super‑type)
        List<Integer> original = new ArrayList<>(List.of(1, 2));
        Collection<Number> checked = CollectionUtilities.getCheckedCollection(original, Number.class);
        // Adding an Integer (subtype) succeeds
        assertDoesNotThrow(() -> checked.add(3));
        // Adding a Double (another subtype) also succeeds
        assertDoesNotThrow(() -> checked.add(4.5));
        // Adding a String (unrelated) fails
        assertThrows(ClassCastException.class, () -> ((Collection) checked).add("bad"));
    }

    @Test
    void getCheckedCollection_PrimitiveType_RejectsAllAdditions() {
        // Primitive class cannot be used for instanceof checks – every add should fail.
        List<Integer> original = new ArrayList<>(List.of(1, 2));
        Collection<Integer> checked = CollectionUtilities.getCheckedCollection(original, int.class);
        // Even adding an Integer (which would normally be allowed) throws ClassCastException
        assertThrows(ClassCastException.class, () -> checked.add(3));
    }

    @Test
    void getCheckedCollection_AlreadyCheckedCollection_BehavesCorrectly() {
        // Create a checked set for String, then wrap it again with the same type.
        Set<String> alreadyChecked = Collections.checkedSet(new HashSet<>(), String.class);
        Collection<String> checked = CollectionUtilities.getCheckedCollection(alreadyChecked, String.class);
        // Adding a correct type works
        assertDoesNotThrow(() -> checked.add("hello"));
        // Adding a wrong type throws ClassCastException
        assertThrows(ClassCastException.class, () -> ((Collection) checked).add(123));
    }

    @Test
    void getCheckedCollection_MismatchedExistingCheckedCollection_ThrowsFromUnderlyingWrapper() {
        // Underlying collection only accepts String, but we ask for Integer.
        Set<String> stringChecked = Collections.checkedSet(new HashSet<>(), String.class);
        Collection<Integer> checked = CollectionUtilities.getCheckedCollection(stringChecked, Integer.class);
        // The new wrapper would allow Integer, but the underlying wrapper rejects it.
        assertThrows(ClassCastException.class, () -> checked.add(42));
    }

    @Test
    void getCheckedCollection_BothParametersNull_CollectionCheckFirst() {
        // Both arguments null – NPE should be for the collection (first check).
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CollectionUtilities.getCheckedCollection(null, null));
        assertTrue(ex.getMessage().contains("Collection must not be null"));
    }

    @Test
    void getCheckedCollection_CustomCollection_UsesCheckedCollection() {
        // MyCollection implements Collection only.
        MyCollection<String> custom = new MyCollection<>(List.of("a", "b"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(custom, String.class);
        // The returned view is a generic checked collection (not List/Set/etc.)
        assertTrue(checked instanceof Collection, "Should be a Collection view");
        assertFalse(checked instanceof List, "Should not be a List view");
        assertFalse(checked instanceof Set, "Should not be a Set view");
        // Correct type succeeds
        assertDoesNotThrow(() -> checked.add("c"));
        // Wrong type fails
        assertThrows(ClassCastException.class, () -> ((Collection) checked).add(99));
    }

    @Test
    void getCheckedCollection_UnderlyingCollectionThrowsCustomException() {
        // MyFailingCollection throws IllegalStateException on add.
        MyFailingCollection<String> failing = new MyFailingCollection<>();
        Collection<String> checked = CollectionUtilities.getCheckedCollection(failing, String.class);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> checked.add("any"));
        assertEquals("Deliberate failure", ex.getMessage());
    }

    @Test
    void getCheckedCollection_MultipleInterfaceImplementation_BranchOrderSetBeforeList() {
        // ListSet implements both List and Set.
        ListSet<String> listSet = new ListSet<>();
        Collection<String> checked = CollectionUtilities.getCheckedCollection(listSet, String.class);
        // According to the implementation order, Set branch is taken → CheckedSet.
        assertTrue(checked instanceof Set, "Wrapper should be a Set");
        assertFalse(checked instanceof List, "Wrapper should not be a List");
        // Adding a correct element works
        assertDoesNotThrow(() -> checked.add("value"));
        // Adding a wrong element throws ClassCastException
        assertThrows(ClassCastException.class, () -> ((Collection) checked).add(123));
    }

    @Test
    void getCheckedCollection_UnmodifiableNavigableSet_ThrowsUnsupportedOperationException() {
        NavigableSet<Integer> unmodifiable = Collections.unmodifiableNavigableSet(new TreeSet<>(Set.of(1, 2)));
        Collection<Integer> checked = CollectionUtilities.getCheckedCollection(unmodifiable, Integer.class);
        // Wrapper is a CheckedNavigableSet
        assertTrue(checked instanceof NavigableSet, "Should be a NavigableSet view");
        // Any modification attempt results in UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> checked.add(3));
        // Even a wrong type triggers the same exception (underlying collection blocks first)
        assertThrows(UnsupportedOperationException.class, () -> ((Collection) checked).add("bad"));
    }
}
