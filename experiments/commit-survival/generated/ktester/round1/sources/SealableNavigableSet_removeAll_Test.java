package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_removeAll_Test {

    /**
     * Instance under test
     */
    private SealableNavigableSet<Integer> set;

    /**
     * Controls the sealed state supplied to the set
     */
    private volatile boolean sealedState = false;

    /**
     * Supplier used by the set to query its sealed state
     */
    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    /**
     * Common test data
     */
    private static final List<Integer> INITIAL_ELEMENTS = Arrays.asList(10, 20, 30, 40, 50);

    /**
     * Instance under test – integer set for most scenarios
     */
    private SealableNavigableSet<Integer> intSet;

    /**
     * Common integer elements used for the default set
     */
    private static final List<Integer> INITIAL_INT_ELEMENTS = Arrays.asList(10, 20, 30, 40, 50);

    /**
     * A collection whose iterator throws a {@link RuntimeException} as soon as
     * {@code next()} is called.  Used to verify that such exceptions propagate
     * out of {@code removeAll}.
     */
    private static class ExceptionThrowingCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public E next() {
                    throw new RuntimeException("Iterator failure");
                }
            };
        }

        @Override
        public int size() {
            return 1;
        }
    }

    /**
     * A collection that modifies itself while its iterator is being consumed,
     * triggering a {@link ConcurrentModificationException} from the underlying
     * iterator implementation.
     */
    private static class SelfModifyingCollection<E> extends AbstractCollection<E> {

        private final List<E> backing = new ArrayList<>(Collections.singletonList(null));

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                private final Iterator<E> it = backing.iterator();

                private boolean first = true;

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public E next() {
                    if (first) {
                        // Modify the collection during iteration.
                        backing.add(null);
                        first = false;
                    }
                    return it.next();
                }
            };
        }

        @Override
        public int size() {
            return backing.size();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh, un‑sealed integer set before each test.
        intSet = new SealableNavigableSet<>(sealedSupplier);
        INITIAL_INT_ELEMENTS.forEach(intSet::add);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helps the garbage collector in long test suites.
        set = null;
    }

    /**
     * Creates a fresh {@code SealableNavigableSet<String>} containing "a","b","c".
     */
    private static SealableNavigableSet<String> createStringSet(Supplier<Boolean> supplier) {
        SealableNavigableSet<String> set = new SealableNavigableSet<>(supplier);
        set.add("a");
        set.add("b");
        set.add("c");
        return set;
    }

    /**
     * Test data for unsealed integer set scenarios.
     */
    private static Stream<Arguments> intRemoveAllScenarios() {
        return Stream.of(// null collection – expect NPE from underlying set
        Arguments.of(null, NullPointerException.class, false, "null collection should cause NPE"), // empty collection – no change, false
        Arguments.of(Collections.emptyList(), false, false, "empty collection returns false"), // collection with present elements – true
        Arguments.of(Arrays.asList(20, 99), true, true, "mix of present and absent elements returns true"), // collection with no matching elements – false
        Arguments.of(Arrays.asList(99, 100), false, false, "no matching elements returns false"), // collection with null (set does not contain null) – false
        Arguments.of(Collections.singletonList(null), false, false, "collection with only null returns false"), // collection with present element and null – true (null ignored)
        Arguments.of(Arrays.asList(30, null), true, true, "present element + null returns true"));
    }

    /**
     * Test data for unsealed string set scenarios (including self‑collection).
     */
    private static Stream<Arguments> stringRemoveAllScenarios() {
        return Stream.of(// collection = self (wrapped NavigableSet) – all elements removed, true
        Arguments.of("self", true, true, "self collection removes all elements"), // collection with all elements – true, set becomes empty
        Arguments.of(Arrays.asList("a", "b", "c"), true, true, "full match removes all"), // collection with duplicate entries – true
        Arguments.of(Arrays.asList("a", "a", "b"), true, true, "duplicates are ignored, still true"), // collection with null and present element – true
        Arguments.of(Arrays.asList("a", null), true, true, "present + null returns true"), // collection with incompatible type (Integer) – false
        Arguments.of(Arrays.asList(1, 2, 3), false, false, "incompatible type yields false"));
    }

    /**
     * Test data for exceptional collections.
     */
    private static Stream<Arguments> exceptionalCollections() {
        return Stream.of(// iterator throws RuntimeException
        Arguments.of(new ExceptionThrowingCollection<>(), RuntimeException.class, "iterator throws RuntimeException"), // self‑modifying collection triggers ConcurrentModificationException
        Arguments.of(new SelfModifyingCollection<>(), ConcurrentModificationException.class, "collection modifies itself during iteration"));
    }

    @Test
    void removeAll_Unsealed_RemovesExistingElements() {
        // GIVEN
        // 99 is not present
        Collection<Integer> toRemove = Arrays.asList(20, 40, 99);
        // WHEN
        boolean changed = set.removeAll(toRemove);
        // THEN
        assertTrue(changed, "removeAll should report a change when at least one element is removed");
        // The set must no longer contain the removed elements.
        assertFalse(set.contains(20));
        assertFalse(set.contains(40));
        // Elements that were not in the collection must remain.
        assertTrue(set.contains(10));
        assertTrue(set.contains(30));
        assertTrue(set.contains(50));
        // Size reflects only the actually removed elements.
        assertEquals(INITIAL_ELEMENTS.size() - 2, set.size());
    }

    @Test
    void removeAll_Unsealed_NoChangeWhenCollectionEmpty() {
        // GIVEN
        Collection<Integer> empty = Collections.emptyList();
        // WHEN
        boolean changed = set.removeAll(empty);
        // THEN
        assertFalse(changed, "removeAll should return false when the collection is empty");
        assertEquals(INITIAL_ELEMENTS.size(), set.size());
    }

    @Test
    void removeAll_Unsealed_NoChangeWhenNoMatchingElements() {
        // GIVEN
        Collection<Integer> nonMatching = Arrays.asList(99, 100);
        // WHEN
        boolean changed = set.removeAll(nonMatching);
        // THEN
        assertFalse(changed, "removeAll should return false when nothing matches");
        assertEquals(INITIAL_ELEMENTS.size(), set.size());
    }

    @Test
    void removeAll_Sealed_ThrowsUnsupportedOperationException() {
        // GIVEN
        // seal the set
        sealedState = true;
        Collection<Integer> toRemove = Arrays.asList(10, 20);
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> set.removeAll(toRemove), "removeAll must reject modifications when the set is sealed");
    }

    @Test
    void removeAll_Sealed_ThrowsEvenWhenCollectionIsEmpty() {
        // GIVEN
        sealedState = true;
        Collection<Integer> empty = Collections.emptyList();
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> set.removeAll(empty), "removeAll must still enforce the sealed contract for an empty collection");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("intRemoveAllScenarios")
    void removeAll_IntegerSet_VariousScenarios(Object col, Class<? extends Throwable> expectedException, boolean expectedResult, String description) {
        // GIVEN
        // ensure un‑sealed
        sealedState = false;
        // WHEN / THEN
        if (expectedException != null) {
            assertThrows(expectedException, () -> intSet.removeAll((Collection<?>) col), "Expected exception for: " + description);
        } else {
            boolean changed = intSet.removeAll((Collection<?>) col);
            assertEquals(expectedResult, changed, "Unexpected return value for: " + description);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("stringRemoveAllScenarios")
    void removeAll_StringSet_VariousScenarios(Object col, boolean expectedResult, boolean expectEmptyAfter, String description) throws Exception {
        // GIVEN
        sealedState = false;
        SealableNavigableSet<String> stringSet = createStringSet(sealedSupplier);
        Collection<?> collection;
        if ("self".equals(col)) {
            // Use reflection to obtain the private wrapped NavigableSet.
            Field f = SealableNavigableSet.class.getDeclaredField("navSet");
            f.setAccessible(true);
            collection = (Collection<?>) f.get(stringSet);
        } else {
            collection = (Collection<?>) col;
        }
        // WHEN
        boolean changed = stringSet.removeAll(collection);
        // THEN
        assertEquals(expectedResult, changed, "Unexpected return value for: " + description);
        if (expectEmptyAfter) {
            assertTrue(stringSet.isEmpty(), "Set should be empty after removal for: " + description);
        } else {
            // Verify that elements not targeted remain.
            if (collection != null && collection.contains("a")) {
                assertFalse(stringSet.contains("a"));
            } else {
                assertTrue(stringSet.contains("a"));
            }
        }
    }

    @Test
    void removeAll_Sealed_ThrowsIllegalStateException() {
        // GIVEN
        // seal the set
        sealedState = true;
        Collection<Integer> any = Arrays.asList(10, 20);
        // WHEN / THEN
        assertThrows(IllegalStateException.class, () -> intSet.removeAll(any), "removeAll must reject modifications when the set is sealed");
    }

    @Test
    void removeAll_Sealed_EmptyCollection_StillThrowsIllegalStateException() {
        // GIVEN
        sealedState = true;
        Collection<Integer> empty = Collections.emptyList();
        // WHEN / THEN
        assertThrows(IllegalStateException.class, () -> intSet.removeAll(empty), "removeAll must still enforce sealed contract even for empty collection");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionalCollections")
    void removeAll_ExceptionalCollectionsPropagate(Collection<?> col, Class<? extends Throwable> expectedException, String description) {
        // GIVEN
        sealedState = false;
        // WHEN / THEN
        assertThrows(expectedException, () -> intSet.removeAll(col), "Expected exception for: " + description);
    }
}
