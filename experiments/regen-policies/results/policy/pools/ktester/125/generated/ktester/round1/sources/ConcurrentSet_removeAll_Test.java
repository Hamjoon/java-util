package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_removeAll_Test {

    /**
     * The set used in most tests – a fresh instance for each test method.
     */
    private ConcurrentSet<Integer> intSet;

    /**
     * A spy of ConcurrentSet used to verify internal calls to {@code remove(Object)}.
     */
    @Mock
    private ConcurrentSet<String> // will be replaced by a spy in setup
    stringSetMock;

    /**
     * A minimal collection whose {@code iterator()} throws a {@link RuntimeException}
     * when any iteration method is invoked. Used to verify exception propagation.
     */
    private static class ThrowingCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    throw new RuntimeException("Iterator failure");
                }

                @Override
                public E next() {
                    throw new RuntimeException("Iterator failure");
                }
            };
        }

        @Override
        public int size() {
            // Size is irrelevant for the test; returning 1 satisfies the contract.
            return 1;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        intSet = new ConcurrentSet<>();
        // Create a real instance and then spy on it so that real behavior is preserved
        ConcurrentSet<String> realStringSet = new ConcurrentSet<>();
        stringSetMock = spy(realStringSet);
    }

    @AfterEach
    void teardownAfterEach() {
        intSet = null;
        stringSetMock = null;
    }

    /**
     * Provides a stream of arguments for the {@link #removeAllParameterized} test.
     * Each argument contains:
     *   - description (used as the test name)
     *   - initial content of the set
     *   - collection to pass to {@code removeAll}
     *   - expected boolean result
     *   - expected remaining elements after the call
     */
    private static Stream<Arguments> provideRemoveAllCases() {
        return Stream.of(// 1. empty collection, empty set
        Arguments.of("Empty collection on empty set", Collections.emptySet(), Collections.emptyList(), false, Collections.emptySet()), // 2. no matching elements
        Arguments.of("No matching elements", new HashSet<>(Arrays.asList("a", "b")), Arrays.asList("x", "y"), false, new HashSet<>(Arrays.asList("a", "b"))), // 3. one matching element
        Arguments.of("One matching element", new HashSet<>(Arrays.asList("a", "b")), Arrays.asList("a", "c"), true, new HashSet<>(Collections.singletonList("b"))), // 4. all elements match
        Arguments.of("All elements match", new HashSet<>(Arrays.asList("a", "b")), Arrays.asList("a", "b"), true, Collections.emptySet()), // 5. duplicate entries in collection
        Arguments.of("Duplicate entries in collection", new HashSet<>(Arrays.asList("a", "b")), Arrays.asList("a", "a", "b"), true, Collections.emptySet()), // 6. null element present
        Arguments.of("Null element present", new HashSet<>(Arrays.asList(null, "a")), Arrays.asList(null, "a"), true, Collections.emptySet()), // 7. collection contains null but set does not
        Arguments.of("Collection contains null but set does not", new HashSet<>(Arrays.asList("a", "b")), Arrays.asList(null, "x"), false, new HashSet<>(Arrays.asList("a", "b"))), // 8. one matching, others non‑matching
        Arguments.of("One matching, others non‑matching", new HashSet<>(Arrays.asList("a", "b")), Arrays.asList("b", "c", "d"), true, new HashSet<>(Collections.singletonList("a"))), // 9. all set elements plus extra non‑matching
        Arguments.of("All set elements plus extra non‑matching", new HashSet<>(Arrays.asList("a", "b")), Arrays.asList("a", "b", "c", "d"), true, Collections.emptySet()), // 10. only null to remove
        Arguments.of("Only null to remove", new HashSet<>(Arrays.asList(null, "x")), Collections.singletonList(null), true, new HashSet<>(Collections.singletonList("x"))), // 11. empty var‑args collection (equivalent to empty list)
        Arguments.of("Empty var‑args collection", Collections.emptySet(), Collections.emptyList(), false, Collections.emptySet()), // 12. empty HashSet argument
        Arguments.of("Empty HashSet argument", Collections.emptySet(), new HashSet<>(), false, Collections.emptySet()), // 13. multiple null entries in collection
        Arguments.of("Multiple null entries in collection", new HashSet<>(Collections.singletonList(null)), Arrays.asList(null, null, null), true, Collections.emptySet()), // 14. duplicate matching elements
        Arguments.of("Duplicate matching elements", new HashSet<>(Collections.singletonList("a")), Arrays.asList("a", "a", "a"), true, Collections.emptySet()), // 15. unrelated object type in collection
        Arguments.of("Unrelated object type in collection", new HashSet<>(Arrays.asList("a", "b")), Collections.singletonList(new Object()), false, new HashSet<>(Arrays.asList("a", "b"))), // 16. mixed present elements and null duplicates
        Arguments.of("Mixed present elements and null duplicates", new HashSet<>(Arrays.asList("a", null, "b")), Arrays.asList("a", null, "b", null), true, Collections.emptySet()), // 17. large collection with only a subset matching
        Arguments.of("Large collection with subset matching", new HashSet<>(Arrays.asList("a", "b")), Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"), true, Collections.emptySet()));
    }

    @Test
    void removeAllRemovesExistingElementsAndReturnsTrue() {
        // given
        intSet.addAll(Arrays.asList(1, 2, 3, 4));
        // 99 is not present
        Collection<Integer> toRemove = Arrays.asList(2, 4, 99);
        // when
        boolean modified = intSet.removeAll(toRemove);
        // then
        assertTrue(modified, "removeAll should return true when at least one element is removed");
        assertFalse(intSet.contains(2), "Element 2 should have been removed");
        assertFalse(intSet.contains(4), "Element 4 should have been removed");
        assertTrue(intSet.contains(1), "Element 1 should remain");
        assertTrue(intSet.contains(3), "Element 3 should remain");
        assertEquals(2, intSet.size(), "Set should contain exactly two remaining elements");
    }

    @Test
    void removeAllWithNoMatchingElementsReturnsFalse() {
        // given
        intSet.addAll(Arrays.asList(10, 20, 30));
        // none are present
        Collection<Integer> toRemove = Arrays.asList(1, 2, 3);
        // when
        boolean modified = intSet.removeAll(toRemove);
        // then
        assertFalse(modified, "removeAll should return false when no elements are removed");
        assertEquals(3, intSet.size(), "Set size should be unchanged");
        assertTrue(intSet.containsAll(Arrays.asList(10, 20, 30)), "Original elements must still be present");
    }

    @Test
    void removeAllWithEmptyCollectionReturnsFalseAndLeavesSetUnchanged() {
        // given
        intSet.addAll(Arrays.asList(5, 6));
        Collection<Integer> empty = Collections.emptyList();
        // when
        boolean modified = intSet.removeAll(empty);
        // then
        assertFalse(modified, "removeAll on an empty collection should return false");
        assertEquals(2, intSet.size(), "Set size should remain the same");
        assertTrue(intSet.containsAll(Arrays.asList(5, 6)), "Elements should be unchanged");
    }

    @Test
    void removeAllHandlesNullElementCorrectly() {
        // given
        ConcurrentSet<String> setWithNull = new ConcurrentSet<>();
        setWithNull.addAll(Arrays.asList("a", null, "b"));
        // null is present, "c" is not
        Collection<String> toRemove = Arrays.asList(null, "c");
        // when
        boolean modified = setWithNull.removeAll(toRemove);
        // then
        assertTrue(modified, "removeAll should return true when null element is removed");
        assertFalse(setWithNull.contains(null), "Null element should have been removed");
        assertTrue(setWithNull.containsAll(Arrays.asList("a", "b")), "Other elements must remain");
        assertEquals(2, setWithNull.size(), "Set should contain exactly two elements after removal");
    }

    @Test
    void removeAllInvokesRemoveForEachElementInTheSuppliedCollection() {
        // given
        stringSetMock.addAll(Arrays.asList("x", "y", "z"));
        Collection<String> toRemove = Arrays.asList("x", "missing", "z");
        // when
        boolean modified = stringSetMock.removeAll(toRemove);
        // then
        assertTrue(modified, "removeAll should report modification because at least one element existed");
        // Verify that remove(Object) was called exactly once for each element in the collection
        verify(stringSetMock, times(toRemove.size())).remove(any());
        // Additionally verify the specific calls (order is not guaranteed)
        verify(stringSetMock).remove("x");
        verify(stringSetMock).remove("missing");
        verify(stringSetMock).remove("z");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideRemoveAllCases")
    void removeAllParameterized(String description, Set<Object> initial, Collection<?> col, boolean expectedResult, Set<Object> expectedRemaining) {
        // given
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        set.addAll(initial);
        // when
        boolean result = set.removeAll(col);
        // then
        assertEquals(expectedResult, result, () -> "removeAll returned unexpected result for case: " + description);
        // Verify the remaining elements exactly match the expectation
        assertEquals(expectedRemaining, new HashSet<>(set), () -> "Remaining elements differ for case: " + description);
    }

    @Test
    void removeAllNullCollectionThrowsNullPointerException() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        assertThrows(NullPointerException.class, () -> set.removeAll(null), "Calling removeAll with a null collection must throw NullPointerException");
    }

    @Test
    void removeAllPropagatesRuntimeExceptionFromCollectionIterator() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        ThrowingCollection<Integer> throwing = new ThrowingCollection<>();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> set.removeAll(throwing), "removeAll should propagate RuntimeException thrown by the collection's iterator");
        assertEquals("Iterator failure", ex.getMessage(), "Exception message should match the one thrown by the iterator");
    }
}
