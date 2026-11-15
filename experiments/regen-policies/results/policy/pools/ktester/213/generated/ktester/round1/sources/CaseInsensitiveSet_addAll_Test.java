package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_addAll_Test {

    private static Stream<Arguments> provideAllNewElements() {
        return Stream.of(Arguments.of(Arrays.asList("Apple", "Banana", "Cherry")));
    }

    /**
     * Verify that addAll returns {@code true} when at least one new element is added
     * and that case‑insensitive handling works correctly.
     */
    @Test
    void testAddAllAddsNewElementsAndReturnsTrue() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        // First add a single element
        assertTrue(set.add("Hello"));
        assertEquals(1, set.size());
        // Now add a collection containing a new element and a duplicate (different case)
        List<String> toAdd = Arrays.asList("World", "HELLO");
        boolean modified = set.addAll(toAdd);
        assertTrue(modified, "addAll should report modification when new elements are added");
        assertEquals(2, set.size(), "Set should contain only the distinct case‑insensitive values");
        assertTrue(set.contains("hello"));
        assertTrue(set.contains("world"));
    }

    /**
     * Verify that addAll returns {@code false} when the collection does not introduce
     * any new elements (all are already present, possibly with different case).
     */
    @Test
    void testAddAllWithOnlyDuplicatesReturnsFalse() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("One", "Two", "Three"));
        // Attempt to add the same values with different capitalisation
        List<String> duplicates = Arrays.asList("ONE", "two", "THREE");
        boolean modified = set.addAll(duplicates);
        assertFalse(modified, "addAll should return false when nothing changes");
        assertEquals(3, set.size(), "Size must stay unchanged");
    }

    /**
     * Verify that addAll works correctly with a mixed collection of {@link String}
     * and non‑String elements.
     */
    @Test
    void testAddAllMixedTypes() {
        CaseInsensitiveSet<Object> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("Apple", 42, true));
        assertEquals(3, set.size());
        // case‑insensitive match
        assertTrue(set.contains("apple"));
        assertTrue(set.contains(42));
        assertTrue(set.contains(true));
        // Adding a duplicate string with different case should not change the set
        boolean modified = set.addAll(Collections.singletonList("APPLE"));
        assertFalse(modified);
        assertEquals(3, set.size());
    }

    /**
     * Adding an empty collection must not modify the set and must return {@code false}.
     */
    @Test
    void testAddAllEmptyCollection() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("initial");
        boolean modified = set.addAll(Collections.emptyList());
        assertFalse(modified, "addAll of an empty collection should return false");
        assertEquals(1, set.size(), "Set size must remain unchanged");
    }

    /**
     * Verify that the boolean result of addAll reflects the presence of at least one
     * new element even when the collection also contains duplicates.
     */
    @Test
    void testAddAllPartialDuplicates() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("Alpha", "Beta"));
        // Collection contains one new element ("Gamma") and one duplicate ("beta")
        List<String> mixed = Arrays.asList("beta", "Gamma");
        boolean modified = set.addAll(mixed);
        assertTrue(modified, "addAll should return true when at least one new element is added");
        assertEquals(3, set.size());
        assertTrue(set.contains("gamma"));
    }

    @Test
    void testAddAllAllNewElements() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        List<String> toAdd = Arrays.asList("Apple", "Banana", "Cherry");
        boolean modified = set.addAll(toAdd);
        assertTrue(modified, "addAll should return true when new elements are added");
        assertEquals(3, set.size());
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("banana"));
        assertTrue(set.contains("cherry"));
    }

    @Test
    void testAddAllMixNewAndDuplicateElements() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("apple", "banana"));
        List<String> mixed = Arrays.asList("apple", "Durian", "BANANA");
        boolean modified = set.addAll(mixed);
        assertTrue(modified, "addAll must return true because at least one new element is added");
        assertEquals(3, set.size());
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("banana"));
        assertTrue(set.contains("durian"));
    }

    @Test
    void testAddAllAllDuplicateElements() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("APPLE", "banana"));
        List<String> duplicates = Arrays.asList("apple", "BANANA");
        boolean modified = set.addAll(duplicates);
        assertFalse(modified, "addAll should return false when nothing changes");
        assertEquals(2, set.size());
    }

    @Test
    void testAddAllNullCollectionArgument() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        assertThrows(NullPointerException.class, () -> set.addAll(null), "Passing a null collection must throw NullPointerException");
    }

    @Test
    void testAddAllCollectionContainingNullElement() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        List<String> withNull = Collections.singletonList(null);
        // According to the API contract a null element causes NPE
        assertThrows(NullPointerException.class, () -> set.addAll(withNull), "Collection containing null should cause NullPointerException");
    }

    @Test
    void testAddAllMixedCaseDuplicatesAndNewElements() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        List<String> input = Arrays.asList("Orange", "orange", "ORANGE", "Grape");
        boolean modified = set.addAll(input);
        assertTrue(modified, "addAll should return true because a new element (Grape) is added");
        assertEquals(2, set.size());
        assertTrue(set.contains("orange"));
        assertTrue(set.contains("grape"));
    }

    @Test
    void testAddAllWhitespaceAndEmptyStrings() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        List<String> input = Arrays.asList(" ", "", "  ", "Apple");
        boolean modified = set.addAll(input);
        assertTrue(modified);
        assertEquals(4, set.size());
        assertTrue(set.contains(" "));
        assertTrue(set.contains(""));
        assertTrue(set.contains("  "));
        assertTrue(set.contains("apple"));
    }

    @Test
    void testAddAllNonStringElements() {
        CaseInsensitiveSet<Object> set = new CaseInsensitiveSet<>();
        List<Object> input = Arrays.asList(123, 456, "123");
        boolean modified = set.addAll(input);
        assertTrue(modified);
        assertEquals(3, set.size());
        assertTrue(set.contains(123));
        assertTrue(set.contains(456));
        assertTrue(set.contains("123"));
    }

    @Test
    void testAddAllSelfAddReturnsFalse() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("one", "two"));
        // Adding the set to itself – no new elements should be introduced
        boolean modified = set.addAll(set);
        assertFalse(modified, "Self‑add should not modify the set");
        assertEquals(2, set.size());
    }

    @Test
    void testAddAllLargeCollectionPerformanceBoundary() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        List<String> large = new ArrayList<>(10_000);
        for (int i = 0; i < 10_000; i++) {
            large.add("value" + i);
        }
        boolean modified = set.addAll(large);
        assertTrue(modified);
        assertEquals(10_000, set.size());
        // Spot‑check a few entries
        assertTrue(set.contains("value0"));
        assertTrue(set.contains("value9999"));
    }

    @Test
    void testAddAllDuplicateNullElements() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        List<String> withTwoNulls = Arrays.asList(null, null);
        // The first null triggers NPE according to the contract, so we expect NPE
        assertThrows(NullPointerException.class, () -> set.addAll(withTwoNulls));
    }

    /**
     * Collection whose iterator throws a {@link RuntimeException} when {@code next()}
     * is called.  The exception must propagate out of {@code addAll}.
     */
    @Test
    void testAddAllIteratorThrowsRuntimeException() {
        Collection<String> badCollection = new Collection<>() {

            @Override
            public int size() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<String> iterator() {
                return new Iterator<>() {

                    private boolean first = true;

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public String next() {
                        if (first) {
                            first = false;
                            throw new RuntimeException("iterator failure");
                        }
                        return "unused";
                    }
                };
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
            public boolean add(String s) {
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
            public boolean addAll(Collection<? extends String> c) {
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
        };
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> set.addAll(badCollection));
        assertEquals("iterator failure", ex.getMessage());
    }

    /**
     * Adding an element of an incompatible type should cause a {@link ClassCastException}
     * because the internal implementation attempts to treat the element as a {@link String}
     * for case‑insensitive handling.
     */
    @Test
    void testAddAllElementTypeIncompatibleCausingClassCastException() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        List<Object> mixed = new ArrayList<>();
        mixed.add("apple");
        // non‑String element
        mixed.add(new Object());
        assertThrows(ClassCastException.class, () -> set.addAll(mixed));
    }

    /**
     * A collection that is a view backed by the same {@link CaseInsensitiveSet}
     * and is modified during iteration.  The fail‑fast iterator of the backing
     * set should throw {@link ConcurrentModificationException}.
     */
    @Test
    void testAddAllConcurrentModificationException() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        // Use the set itself as the collection argument – this will modify the set
        // while its iterator (used internally by addAll) is traversing it.
        assertThrows(ConcurrentModificationException.class, () -> set.addAll(set));
    }

    @ParameterizedTest
    @MethodSource("provideAllNewElements")
    void parameterizedTestAddAllAllNewElements(List<String> input) {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        boolean modified = set.addAll(input);
        assertTrue(modified);
        assertEquals(input.size(), set.size());
        input.forEach(s -> assertTrue(set.contains(s.toLowerCase())));
    }
}
