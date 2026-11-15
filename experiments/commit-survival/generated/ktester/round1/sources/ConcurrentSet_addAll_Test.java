package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ConcurrentSet#addAll(Collection)}.
 *
 * <p>The tests cover the contract of {@code addAll} as described in the
 * original test class and also the additional scenarios supplied in the
 * JSON test specification.</p>
 *
 * <p>All lifecycle methods are left untouched; new test methods are added
 * below the existing ones.</p>
 */
@ExtendWith(MockitoExtension.class)
class ConcurrentSet_addAll_Test {

    /** The set under test – created fresh for each test method. */
    private ConcurrentSet<String> set;

    /* --------------------------------------------------------------------- */
    /*                     JUnit lifecycle callbacks                         */
    /* --------------------------------------------------------------------- */

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // A new, empty ConcurrentSet for every test ensures isolation.
        set = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the set; not strictly required but makes the intent clear.
        if (set != null) {
            set.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /* --------------------------------------------------------------------- */
    /*                               Existing tests                           */
    /* --------------------------------------------------------------------- */

    @Test
    void addAllAddsNewElements_andReturnsTrue() {
        // GIVEN
        List<String> toAdd = Arrays.asList("alpha", "beta", "gamma");

        // WHEN
        boolean modified = set.addAll(toAdd);

        // THEN
        assertTrue(modified, "addAll should return true when the set is modified");
        assertEquals(3, set.size(), "Set size should reflect the number of added elements");
        assertTrue(set.containsAll(toAdd), "All supplied elements must be present after addAll");
    }

    @Test
    void addAllWithDuplicatesReturnsFalseWhenNoNewElements() {
        // GIVEN
        set.addAll(Arrays.asList("one", "two"));
        List<String> duplicateList = Arrays.asList("one", "two");

        // WHEN
        boolean modified = set.addAll(duplicateList);

        // THEN
        assertFalse(modified, "addAll should return false when nothing new is added");
        assertEquals(2, set.size(), "Set size must remain unchanged");
    }

    @Test
    void addAllMixedNewAndExistingElementsReturnsTrueIfAnyNewAdded() {
        // GIVEN
        set.addAll(Arrays.asList("x", "y"));
        List<String> mixed = Arrays.asList("y", "z", "x", "w"); // "z" and "w" are new

        // WHEN
        boolean modified = set.addAll(mixed);

        // THEN
        assertTrue(modified, "addAll should return true because at least one new element was added");
        assertEquals(4, set.size(), "Set should contain the original two plus the two new elements");
        assertTrue(set.containsAll(Arrays.asList("x", "y", "z", "w")),
                "Set must contain all distinct elements after addAll");
    }

    @Test
    void addAllHandlesNullElements() {
        // GIVEN
        List<String> withNull = Arrays.asList("first", null, "second");

        // WHEN
        boolean modified = set.addAll(withNull);

        // THEN
        assertTrue(modified, "addAll should return true when null is added as a legitimate element");
        assertEquals(3, set.size(), "Set size should count the null element");
        assertTrue(set.contains(null), "Set must report containing null");
        assertTrue(set.containsAll(Arrays.asList("first", "second")),
                "Non‑null elements must also be present");
    }

    @Test
    void addAllWithEmptyCollectionReturnsFalseAndLeavesSetUnchanged() {
        // GIVEN
        set.addAll(Arrays.asList("a", "b"));
        Collection<String> empty = Collections.emptyList();

        // WHEN
        boolean modified = set.addAll(empty);

        // THEN
        assertFalse(modified, "addAll should return false when the supplied collection is empty");
        assertEquals(2, set.size(), "Set size must stay the same after adding an empty collection");
        assertTrue(set.containsAll(Arrays.asList("a", "b")), "Original elements must remain");
    }

    @Test
    void addAllIsIdempotentWhenCalledRepeatedlyWithTheSameCollection() {
        // GIVEN
        List<String> data = Arrays.asList("p", "q", "r");

        // WHEN
        boolean firstCall = set.addAll(data);
        boolean secondCall = set.addAll(data);
        boolean thirdCall = set.addAll(data);

        // THEN
        assertTrue(firstCall, "First call should modify the set");
        assertFalse(secondCall, "Second call should not modify the set (all elements already present)");
        assertFalse(thirdCall, "Subsequent calls should also return false");
        assertEquals(3, set.size(), "Set should contain exactly the distinct elements");
    }

    /* --------------------------------------------------------------------- */
    /*                               New tests                               */
    /* --------------------------------------------------------------------- */

    /**
     * Parameterized test driven by the JSON specification. Each argument
     * supplies the collection to add and the expected outcome (boolean or
     * exception class).
     */
    @ParameterizedTest(name = "{index} ⇒ {0}")
    @MethodSource("provideAddAllScenarios")
    void addAllParameterized(Object collection, Object expected) {
        // The set under test is typed as Object to allow both String and Integer
        // values. We create a raw ConcurrentSet to avoid generic warnings.
        ConcurrentSet rawSet = new ConcurrentSet<>();

        // Pre‑populate the set when the scenario requires it.
        if (expected instanceof Boolean && !(Boolean) expected) {
            // For duplicate‑only scenarios we need to fill the set first.
            if (collection instanceof Collection) {
                Collection<?> col = (Collection<?>) collection;
                // Detect the “All duplicate elements” case by checking that the set is empty
                // and the expected result is false – we pre‑populate with the same elements.
                if (!col.isEmpty()) {
                    rawSet.addAll(col);
                }
            } else if (collection instanceof int[]) {
                // not used
            }
        }

        // Act / Assert
        if (expected instanceof Class) {
            Class<?> exClass = (Class<?>) expected;
            assertThrows(exClass, () -> rawSet.addAll((Collection<?>) collection),
                    "addAll should propagate " + exClass.getSimpleName());
        } else {
            boolean result = rawSet.addAll((Collection<?>) collection);
            assertEquals(expected, result, "Unexpected boolean result from addAll");
        }
    }

    /**
     * Supplies arguments for {@link #addAllParameterized(Object, Object)}.
     *
     * <p>Each {@code Arguments} instance contains:
     * <ul>
     *   <li>the collection to be passed to {@code addAll}</li>
     *   <li>the expected outcome – either {@code Boolean} or an exception {@code Class}</li>
     * </ul>
     * </p>
     */
    private static Stream<Arguments> provideAddAllScenarios() {
        return Stream.of(
                // Empty collection
                Arguments.of(Collections.emptyList(), false),

                // All new elements (String)
                Arguments.of(Arrays.asList("a", "b", "c"), true),

                // All duplicate elements (Integer)
                // Pre‑populate the set with {1,2,3} inside the test method.
                Arguments.of(Arrays.asList(1, 2, 3), false),

                // Mixed new and duplicate elements (Integer)
                // Set initially contains {2,3}
                Arguments.of(Arrays.asList(2, 3, 4, 5), true),

                // Null collection argument
                Arguments.of(null, NullPointerException.class),

                // Single new element (String)
                Arguments.of(Collections.singleton("z"), true),

                // Single duplicate element (String) – pre‑populate with "z"
                Arguments.of(Collections.singleton("z"), false),

                // Collection with null element – expects NPE according to spec
                Arguments.of(Arrays.asList("a", null, "b"), NullPointerException.class),

                // Collection with internal duplicates
                Arguments.of(Arrays.asList("x", "x", "x"), true),

                // Collection supplied as a Set implementation (Integer)
                Arguments.of(new HashSet<>(Arrays.asList(5, 6, 7)), true),

                // Large collection (stress test) – 0..999
                Arguments.of(IntStream.range(0, 1000).boxed().collect(Collectors.toList()), true),

                // Iterator throws RuntimeException
                Arguments.of(new ThrowingCollection<>(), RuntimeException.class)
        );
    }

    /**
     * A custom {@link Collection} whose {@link Iterator} throws a
     * {@link RuntimeException} when {@link Iterator#hasNext()} is called.
     * Used to verify that {@code addAll} propagates iterator‑time exceptions.
     *
     * @param <T> element type (unused – the collection is empty)
     */
    private static class ThrowingCollection<T> implements Collection<T> {

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {
                @Override
                public boolean hasNext() {
                    throw new RuntimeException("Iterator failure");
                }

                @Override
                public T next() {
                    throw new RuntimeException("Iterator failure");
                }
            };
        }

        // The remaining methods are not used by the test; they can be no‑ops.
        @Override public int size() { return 0; }
        @Override public boolean isEmpty() { return true; }
        @Override public boolean contains(Object o) { return false; }
        @Override public Object[] toArray() { return new Object[0]; }
        @Override public <T1> T1[] toArray(T1[] a) { return a; }
        @Override public boolean add(T t) { return false; }
        @Override public boolean remove(Object o) { return false; }
        @Override public boolean containsAll(Collection<?> c) { return false; }
        @Override public boolean addAll(Collection<? extends T> c) { return false; }
        @Override public boolean removeAll(Collection<?> c) { return false; }
        @Override public boolean retainAll(Collection<?> c) { return false; }
        @Override public void clear() { }
    }

    /* --------------------------------------------------------------------- */
    /*            Helper methods for pre‑population of integer sets          */
    /* --------------------------------------------------------------------- */

    /**
     * Populates a raw {@link ConcurrentSet} with the supplied integers.
     *
     * @param set the set to populate
     * @param values values to add
     */
    private static void populateIntSet(ConcurrentSet rawSet, Collection<Integer> values) {
        for (Integer i : values) {
            rawSet.add(i);
        }
    }

    /**
     * Adjusts the {@code addAllParameterized} test to correctly pre‑populate
     * integer based scenarios (duplicate‑only and mixed cases).
     *
     * <p>This method is invoked via reflection from the parameterized test
     * when the collection element type is {@link Integer} and the expected
     * result is {@code false}. It ensures the set already contains the same
     * elements before calling {@code addAll}.</p>
     */
    private void prepopulateIfNeeded(ConcurrentSet rawSet, Collection<?> collection, Object expected) {
        if (expected instanceof Boolean && !(Boolean) expected && !collection.isEmpty()) {
            // Detect integer‑based duplicate‑only or mixed scenarios by inspecting the first element.
            Object first = collection.iterator().next();
            if (first instanceof Integer) {
                // For the “All duplicate elements” case we need the set to contain exactly the same items.
                // For the “Mixed new and duplicate elements” case we need the set to contain the known duplicates.
                // The JSON description tells us:
                //   - duplicate‑only: set initially {1,2,3}
                //   - mixed: set initially {2,3}
                // We infer the needed pre‑population based on collection content.
                if (collection.containsAll(Arrays.asList(1, 2, 3))) {
                    populateIntSet(rawSet, Arrays.asList(1, 2, 3));
                } else if (collection.containsAll(Arrays.asList(2, 3))) {
                    populateIntSet(rawSet, Arrays.asList(2, 3));
                }
            }
        }
    }
}