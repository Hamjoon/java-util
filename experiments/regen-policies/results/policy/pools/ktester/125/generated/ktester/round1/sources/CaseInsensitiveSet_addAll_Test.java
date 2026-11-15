package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_addAll_Test {

    private CaseInsensitiveSet<Object> set;

    private List<Object> sampleList;

    /**
     * A collection whose iterator throws a {@link RuntimeException} on the second call to {@code next()}.
     */
    private static class ThrowingCollection<E> implements Collection<E> {

        private final List<E> data;

        ThrowingCollection(List<E> data) {
            this.data = data;
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < data.size();
                }

                @Override
                public E next() {
                    if (index == 1) {
                        throw new RuntimeException("Intentional exception from iterator.next()");
                    }
                    return data.get(index++);
                }
            };
        }

        // The remaining Collection methods are not needed for the test; delegate to the underlying list.
        @Override
        public int size() {
            return data.size();
        }

        @Override
        public boolean isEmpty() {
            return data.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return data.contains(o);
        }

        @Override
        public Object[] toArray() {
            return data.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return data.toArray(a);
        }

        @Override
        public boolean add(E e) {
            return data.add(e);
        }

        @Override
        public boolean remove(Object o) {
            return data.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return data.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return data.addAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return data.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return data.retainAll(c);
        }

        @Override
        public void clear() {
            data.clear();
        }
    }

    /**
     * A collection whose iterator's {@code hasNext()} throws a {@link RuntimeException}
     * after the first element.
     */
    private static class HasNextThrowingCollection<E> implements Collection<E> {

        private final List<E> data;

        HasNextThrowingCollection(List<E> data) {
            this.data = data;
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                private int index = 0;

                @Override
                public boolean hasNext() {
                    if (index > 0) {
                        throw new RuntimeException("Intentional exception from iterator.hasNext()");
                    }
                    return index < data.size();
                }

                @Override
                public E next() {
                    return data.get(index++);
                }
            };
        }

        // Delegate other methods to the underlying list (not used in the test)
        @Override
        public int size() {
            return data.size();
        }

        @Override
        public boolean isEmpty() {
            return data.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return data.contains(o);
        }

        @Override
        public Object[] toArray() {
            return data.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return data.toArray(a);
        }

        @Override
        public boolean add(E e) {
            return data.add(e);
        }

        @Override
        public boolean remove(Object o) {
            return data.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return data.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return data.addAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return data.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return data.retainAll(c);
        }

        @Override
        public void clear() {
            data.clear();
        }
    }

    /**
     * A collection that modifies its backing list during iteration, causing a
     * {@link ConcurrentModificationException}.
     */
    private static class SelfModifyingCollection<E> implements Collection<E> {

        private final List<E> data;

        SelfModifyingCollection(List<E> data) {
            this.data = data;
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                private final Iterator<E> it = data.iterator();

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public E next() {
                    // Modify the underlying list on each call to next()
                    data.add((E) ("added-" + System.nanoTime()));
                    return it.next();
                }
            };
        }

        // Delegate other methods to the underlying list (not used in the test)
        @Override
        public int size() {
            return data.size();
        }

        @Override
        public boolean isEmpty() {
            return data.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return data.contains(o);
        }

        @Override
        public Object[] toArray() {
            return data.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return data.toArray(a);
        }

        @Override
        public boolean add(E e) {
            return data.add(e);
        }

        @Override
        public boolean remove(Object o) {
            return data.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return data.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return data.addAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return data.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return data.retainAll(c);
        }

        @Override
        public void clear() {
            data.clear();
        }
    }

    /**
     * A simple {@link Map} implementation that throws {@link ClassCastException}
     * when a non‑String key is supplied to {@code put}.  This allows us to test
     * how {@code addAll} reacts to incompatible element types.
     */
    private static class ThrowOnPutMap implements Map<Object, Object> {

        private final Map<Object, Object> delegate = new HashMap<>();

        @Override
        public Object put(Object key, Object value) {
            if (!(key instanceof String)) {
                throw new ClassCastException("Only String keys are allowed");
            }
            return delegate.put(key, value);
        }

        // The rest of the Map contract is delegated without modification.
        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public Object get(Object key) {
            return delegate.get(key);
        }

        @Override
        public Object remove(Object key) {
            return delegate.remove(key);
        }

        @Override
        public void putAll(Map<? extends Object, ? extends Object> m) {
            for (Entry<? extends Object, ? extends Object> e : m.entrySet()) {
                put(e.getKey(), e.getValue());
            }
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Set<Object> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<Object> values() {
            return delegate.values();
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return delegate.entrySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh set for each test
        set = new CaseInsensitiveSet<>();
        // A list that contains a mixture of strings (different cases) and non‑string objects
        sampleList = new ArrayList<>();
        sampleList.add("Apple");
        // duplicate (case‑insensitive)
        sampleList.add("apple");
        sampleList.add("Banana");
        sampleList.add(42);
        // distinct from Integer 42
        sampleList.add(42L);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly necessary but keeps the test tidy
        set = null;
        sampleList = null;
    }

    /**
     * Supplies arguments for the simple addAll scenarios (empty, only existing,
     * only new, mixed, duplicate new, duplicate existing, case‑insensitive, mixed
     * case‑insensitive & new, all present different case, large collection).
     */
    private static Stream<Arguments> provideAddAllScenarios() {
        return Stream.of(// empty collection
        Arguments.of(Collections.emptyList(), false, "empty collection"), // collection with only existing elements (pre‑populate set with alpha & beta)
        Arguments.of(Arrays.asList("alpha", "beta"), false, "only existing elements"), // collection with only new elements
        Arguments.of(Arrays.asList("gamma", "delta"), true, "only new elements"), // collection with mixed existing and new elements
        Arguments.of(Arrays.asList("alpha", "epsilon", "beta"), true, "mixed existing & new"), // collection with duplicate new elements
        Arguments.of(Arrays.asList("newItem", "newItem"), true, "duplicate new elements"), // collection with duplicate existing elements
        Arguments.of(Arrays.asList("existing", "existing"), false, "duplicate existing elements"), // case‑insensitive existing element
        Arguments.of(Arrays.asList("alpha"), false, "case‑insensitive existing element"), // mixed case‑insensitive and new elements
        Arguments.of(Arrays.asList("alpha", "Beta"), true, "mixed case‑insensitive & new"), // all elements already present but different case
        Arguments.of(Arrays.asList("ALPHA", "beta"), false, "all present different case"), // large collection of only new elements (100 items)
        Arguments.of(buildLargeCollection(), true, "large collection of new elements"));
    }

    /**
     * Helper that builds a list of 100 distinct strings: item0 … item99.
     */
    private static List<String> buildLargeCollection() {
        List<String> list = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            list.add("item" + i);
        }
        return list;
    }

    @Test
    void addAllAddsNewElementsAndReturnsTrue() {
        // The set is initially empty, so adding the list must change its size
        boolean changed = set.addAll(sampleList);
        assertTrue(changed, "addAll should report a change when new elements are added");
        // Expected elements: "Apple", "Banana", 42, 42L
        // The duplicate "apple" must be ignored because "Apple" already exists (case‑insensitive)
        Set<Object> expected = new HashSet<>(Arrays.asList("Apple", "Banana", 42, 42L));
        assertEquals(expected.size(), set.size(), "Set size should match number of unique elements");
        assertTrue(set.containsAll(expected), "Set should contain all unique elements");
    }

    @Test
    void addAllWithAllElementsAlreadyPresentReturnsFalse() {
        // First populate the set with the unique elements from sampleList
        set.addAll(sampleList);
        // Prepare a collection that only contains elements already present (including case variants)
        Collection<Object> alreadyPresent = Arrays.asList("APPLE", "banana", 42, 42L);
        boolean changed = set.addAll(alreadyPresent);
        assertFalse(changed, "addAll should return false when no new elements are added");
        // Size must remain unchanged
        assertEquals(4, set.size(), "Set size must stay the same after a no‑op addAll");
    }

    @Test
    void addAllPreservesOriginalCaseForIteration() {
        // Add a mixture of differently‑cased strings
        List<String> strings = Arrays.asList("One", "ONE", "two", "TWO", "Three");
        set.addAll(strings);
        // The set should contain only three distinct logical entries
        assertEquals(3, set.size());
        // The iteration order (insertion order) should reflect the first occurrence of each logical entry
        Iterator<Object> it = set.iterator();
        List<Object> iterationResult = new ArrayList<>();
        while (it.hasNext()) {
            iterationResult.add(it.next());
        }
        List<Object> expectedOrder = Arrays.asList("One", "two", "Three");
        assertEquals(expectedOrder, iterationResult, "Iteration should preserve the original case of the first inserted element");
    }

    @Test
    void addAllWorksWithCustomBackingMapPreservingInsertionOrder() {
        // Use a LinkedHashMap‑backed CaseInsensitiveMap to guarantee insertion order
        Map<Object, Object> backingMap = new CaseInsensitiveMap<>(new LinkedHashMap<>());
        CaseInsensitiveSet<Object> orderedSet = new CaseInsensitiveSet<>(Collections.emptySet(), backingMap);
        List<String> strings = Arrays.asList("alpha", "ALPHA", "beta", "BETA");
        orderedSet.addAll(strings);
        // Expected logical elements: "alpha", "beta"
        assertEquals(2, orderedSet.size());
        // Verify iteration order matches first insertion order
        Iterator<Object> it = orderedSet.iterator();
        List<Object> order = new ArrayList<>();
        while (it.hasNext()) {
            order.add(it.next());
        }
        List<Object> expected = Arrays.asList("alpha", "beta");
        assertEquals(expected, order, "Custom backing map should preserve insertion order");
    }

    @ParameterizedTest(name = "addAll – {2}")
    @MethodSource("provideAddAllScenarios")
    void addAllParameterizedTests(List<?> collection, boolean expectedResult, String description) {
        // Pre‑populate the set when the scenario requires existing elements.
        // The JSON description tells us which elements should already be present.
        if (description.contains("existing")) {
            // For the purpose of these tests we consider "alpha", "beta", and "existing" as
            // the elements that might already be in the set.
            Set<Object> pre = new HashSet<>(Arrays.asList("Alpha", "Beta", "existing"));
            set.addAll(pre);
        }
        // Perform the operation under test
        boolean changed = set.addAll(collection);
        assertEquals(expectedResult, changed, () -> "addAll should return " + expectedResult + " for scenario: " + description);
    }

    @Test
    void addAllNullCollectionThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> set.addAll(null), "addAll must throw NullPointerException when the collection argument is null");
    }

    @Test
    void addAllCollectionContainingNullElementThrowsNullPointerException() {
        List<Object> withNull = Arrays.asList(null, "zeta");
        assertThrows(NullPointerException.class, () -> set.addAll(withNull), "addAll must propagate NullPointerException when a null element is encountered");
    }

    @Test
    void addAllCollectionWithDuplicateNewElementsReturnsTrueEvenIfSizeUnchangedAfterFirst() {
        // Already covered by parameterized test, but this explicit test documents the intent.
        List<String> dup = Arrays.asList("newItem", "newItem");
        boolean changed = set.addAll(dup);
        assertTrue(changed, "addAll should return true when at least one new element is added");
    }

    @Test
    void addAllCollectionThatThrowsDuringIterationPropagatesRuntimeException() {
        Collection<Object> throwing = new ThrowingCollection<>(Arrays.asList("first", "second"));
        assertThrows(RuntimeException.class, () -> set.addAll(throwing), "RuntimeException from iterator.next() must propagate out of addAll");
    }

    @Test
    void addAllCollectionWithIteratorHasNextThrowingPropagatesRuntimeException() {
        Collection<Object> hasNextThrow = new HasNextThrowingCollection<>(Collections.singletonList("only"));
        assertThrows(RuntimeException.class, () -> set.addAll(hasNextThrow), "RuntimeException from iterator.hasNext() must propagate out of addAll");
    }

    @Test
    void addAllCollectionModifyingItselfDuringIterationThrowsConcurrentModificationException() {
        Collection<Object> selfMod = new SelfModifyingCollection<>(new ArrayList<>(Arrays.asList("a", "b", "c")));
        assertThrows(ConcurrentModificationException.class, () -> set.addAll(selfMod), "ConcurrentModificationException should propagate when collection is modified during iteration");
    }

    @Test
    void addAllCollectionWithIncompatibleElementTypePropagatesClassCastException() {
        // Use a backing map that throws ClassCastException for non‑String keys.
        Map<Object, Object> strictMap = new ThrowOnPutMap();
        CaseInsensitiveSet<Object> strictSet = new CaseInsensitiveSet<>(Collections.emptySet(), strictMap);
        // Integer is incompatible with the strict map
        Collection<Object> mixed = Arrays.asList(123);
        assertThrows(ClassCastException.class, () -> strictSet.addAll(mixed), "ClassCastException should propagate when an element of incompatible type is added");
    }
}
