package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_retainAll_Test {

    private ConcurrentSet<Integer> intSet;

    private ConcurrentSet<String> stringSet;

    /**
     * A collection whose iterator throws a {@link RuntimeException} when iterated.
     */
    private static class BadCollection<E> implements Collection<E> {

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
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    throw new RuntimeException("iterator failure");
                }

                @Override
                public E next() {
                    throw new RuntimeException("iterator failure");
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
        intSet = new ConcurrentSet<>();
        stringSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required.
        intSet = null;
        stringSet = null;
    }

    /**
     * Provides the matrix of inputs/expected results taken from the supplied
     * JSON test cases.  All cases start with the set containing {@code "a","b","c"}.
     */
    private static Stream<Arguments> retainAllNormalCases() {
        return Stream.of(// 1. Empty collection – set is non‑empty, should be cleared and return true
        Arguments.of(Collections.emptyList(), true, Collections.emptySet(), "Empty collection input – clears the set and returns true"), // 2. Identical collection – no change, return false
        Arguments.of(Arrays.asList("a", "b", "c"), false, Set.of("a", "b", "c"), "All matching elements – set already equals collection, no modification"), // 3. Subset – element \"a\" removed, return true
        Arguments.of(Arrays.asList("b", "c"), true, Set.of("b", "c"), "Partial overlap – retains only the subset"), // 4. No overlap – set becomes empty, return true
        Arguments.of(Arrays.asList("x", "y"), true, Collections.emptySet(), "No overlap – all original elements removed"), // 5. Collection containing null – null is wrapped, no null in original set,
        //    therefore set becomes empty, return true
        Arguments.of(Collections.singletonList(null), true, Collections.emptySet(), "Collection containing null – results in empty set"), // 6. Duplicate elements – duplicates are ignored, element \"c\" removed
        Arguments.of(Arrays.asList("a", "a", "b"), true, Set.of("a", "b"), "Duplicates in collection – set ends with unique retained elements"), // 7. Incompatible type (Integer) – nothing matches, set cleared
        Arguments.of(Arrays.asList(1, 2), true, Collections.emptySet(), "Incompatible type – all original elements removed"), // 8. Collection with only duplicates of an existing element – retain only \"b\"
        Arguments.of(Arrays.asList("b", "b", "b"), true, Set.of("b"), "Duplicates of a single existing element – other elements removed"), // 9. Passing the same Set instance as the collection – no change, return false
        Arguments.of(null, false, Set.of("a", "b", "c"), "Self collection – no modification when collection is the set itself"));
    }

    /* -------------------------------------------------------------
     * retainAll – basic behaviour
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("retainAll removes elements not present in the supplied collection and returns true")
    void testRetainAllRemovesElements() {
        // given
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        // when
        boolean changed = intSet.retainAll(Arrays.asList(2, 3, 5));
        // then
        assertTrue(changed, "retainAll should report that the set was modified");
        assertTrue(intSet.containsAll(Arrays.asList(2, 3, 5)), "Set should contain only the retained elements");
        assertFalse(intSet.contains(1) || intSet.contains(4), "Elements not in the retain collection must be removed");
        assertEquals(3, intSet.size(), "Set size must reflect retained elements");
    }

    @Test
    @DisplayName("retainAll with identical collection does not modify the set and returns false")
    void testRetainAllNoChange() {
        // given
        intSet.addAll(Arrays.asList(10, 20, 30));
        // when
        boolean changed = intSet.retainAll(Arrays.asList(10, 20, 30));
        // then
        assertFalse(changed, "retainAll should report no modification when collections are equal");
        assertEquals(3, intSet.size(), "Size must stay unchanged");
        assertTrue(intSet.containsAll(Arrays.asList(10, 20, 30)));
    }

    @Test
    @DisplayName("retainAll with an empty collection clears the set")
    void testRetainAllEmptyCollection() {
        // given
        intSet.addAll(Arrays.asList(7, 8, 9));
        // when
        boolean changed = intSet.retainAll(Collections.emptyList());
        // then
        assertTrue(changed, "retainAll should report a change when clearing the set");
        assertTrue(intSet.isEmpty(), "Set must be empty after retaining an empty collection");
        assertEquals(0, intSet.size());
    }

    /* -------------------------------------------------------------
     * retainAll – handling of null elements
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("retainAll correctly retains null elements")
    void testRetainAllWithNullElements() {
        // given
        stringSet.addAll(Arrays.asList("a", null, "b", "c"));
        // when
        boolean changed = stringSet.retainAll(Arrays.asList(null, "b"));
        // then
        assertTrue(changed, "retainAll should report a modification when discarding elements");
        assertEquals(2, stringSet.size(), "Only two elements (null and \"b\") should remain");
        assertTrue(stringSet.contains(null), "Set must still contain null");
        assertTrue(stringSet.contains("b"), "Set must still contain \"b\"");
        assertFalse(stringSet.contains("a"));
        assertFalse(stringSet.contains("c"));
    }

    @Test
    @DisplayName("retainAll returns false when the set already contains only the null element")
    void testRetainAllOnlyNullNoChange() {
        // given
        stringSet.add(null);
        // when
        boolean changed = stringSet.retainAll(Collections.singletonList(null));
        // then
        assertFalse(changed, "No change should be reported when the set already matches the retain collection");
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains(null));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("retainAllNormalCases")
    void testRetainAll_NormalBehaviour(List<?> collectionInput, boolean expectedReturn, Set<String> expectedAfter, String description) {
        // -----------------------------------------------------------------
        // GIVEN – a set pre‑populated with {"a","b","c"}
        // -----------------------------------------------------------------
        stringSet.addAll(Arrays.asList("a", "b", "c"));
        // -----------------------------------------------------------------
        // WHEN – invoke retainAll.  For the special “self collection” case
        // the collection argument is the set itself.
        // -----------------------------------------------------------------
        Collection<?> collection = (collectionInput == null) ? stringSet : collectionInput;
        boolean actualReturn = stringSet.retainAll(collection);
        // -----------------------------------------------------------------
        // THEN – verify the boolean result and the final contents of the set
        // -----------------------------------------------------------------
        assertEquals(expectedReturn, actualReturn, "retainAll should return the expected modification flag");
        assertEquals(expectedAfter, new HashSet<>(stringSet), "Set contents after retainAll do not match expected");
    }

    /* -------------------------------------------------------------
     * Edge case – retainAll on an already empty set with an empty collection
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("retainAll on an empty set with empty collection returns false")
    void testRetainAll_EmptySetEmptyCollection() {
        // GIVEN – both set and collection are empty
        // (stringSet is already empty from @BeforeEach)
        // WHEN
        boolean changed = stringSet.retainAll(Collections.emptyList());
        // THEN
        assertFalse(changed, "When both set and collection are empty, retainAll should report no change");
        assertTrue(stringSet.isEmpty(), "Set must remain empty");
    }

    @Test
    @DisplayName("retainAll throws NullPointerException when the collection argument is null")
    void testRetainAll_NullCollection_NPE() {
        // GIVEN
        stringSet.addAll(Arrays.asList("a", "b"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> stringSet.retainAll(null), "Passing a null collection should result in NullPointerException");
    }

    @Test
    @DisplayName("retainAll propagates RuntimeException thrown by collection iterator")
    void testRetainAll_BadCollection_RuntimeException() {
        // GIVEN
        stringSet.addAll(Arrays.asList("a", "b", "c"));
        Collection<String> bad = new BadCollection<>();
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> stringSet.retainAll(bad), "Exception from collection iterator should propagate");
        assertEquals("iterator failure", ex.getMessage());
    }

    /* -------------------------------------------------------------
     * Additional explicit test for null element handling (sentinel behaviour)
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("retainAll correctly retains a null element when it is present in the set")
    void testRetainAll_RetainsExistingNull() {
        // GIVEN – set contains a null sentinel and a regular element
        stringSet.addAll(Arrays.asList(null, "x", "y"));
        // WHEN – retain only null
        boolean changed = stringSet.retainAll(Collections.singletonList(null));
        // THEN
        assertTrue(changed, "Set should be modified (non‑null elements removed)");
        assertEquals(1, stringSet.size(), "Only the null element should remain");
        assertTrue(stringSet.contains(null), "Null sentinel must still be present");
    }
}
