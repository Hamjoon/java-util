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
class CompactSet_containsAll_Test {

    private CompactSet<String> setUnderTest;

    /**
     * A collection whose iterator's {@code next()} method always throws a
     * {@link RuntimeException}. Used to verify that {@code containsAll}
     * propagates iterator exceptions.
     */
    private static class FaultyCollection<E> extends AbstractCollection<E> {

        private final Collection<E> delegate;

        FaultyCollection(Collection<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    return delegate.iterator().hasNext();
                }

                @Override
                public E next() {
                    throw new RuntimeException("iterator failure");
                }
            };
        }

        @Override
        public int size() {
            return delegate.size();
        }
    }

    /**
     * A collection that modifies itself after an iterator is created,
     * causing a {@link ConcurrentModificationException} when {@code next()}
     * is called.
     */
    private static class ConcurrentModificationCollection<E> extends AbstractCollection<E> {

        private final List<E> backing;

        ConcurrentModificationCollection(Collection<E> source) {
            this.backing = new ArrayList<>(source);
        }

        @Override
        public Iterator<E> iterator() {
            Iterator<E> it = backing.iterator();
            // modify the backing list after iterator creation
            backing.add(null);
            return it;
        }

        @Override
        public int size() {
            return backing.size();
        }
    }

    /**
     * An object whose {@code equals(Object)} method always throws an
     * {@link IllegalStateException}. Used to test exception propagation from
     * element comparison.
     */
    private static class BadEqualsObject {

        @Override
        public boolean equals(Object obj) {
            throw new IllegalStateException("bad equals");
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a CompactSet with a known content for each test.
        setUnderTest = new CompactSet<>();
        setUnderTest.add("apple");
        setUnderTest.add("banana");
        setUnderTest.add("cherry");
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared after each test to avoid cross‑test contamination.
        setUnderTest.clear();
    }

    // ---------- Helper methods for building CompactSet instances ----------
    private static CompactSet<Integer> buildIntegerSet(Integer... elements) {
        CompactSet<Integer> set = new CompactSet<>();
        for (Integer e : elements) {
            set.add(e);
        }
        return set;
    }

    private static CompactSet<Integer> buildIntegerSetWithNull(Integer... elements) {
        CompactSet<Integer> set = new CompactSet<>();
        for (Integer e : elements) {
            set.add(e);
        }
        set.add(null);
        return set;
    }

    static Stream<Arguments> basicFunctionalityEmptyAndFullMatches() {
        // Empty set, empty collection → true
        CompactSet<Integer> emptySet = new CompactSet<>();
        return Stream.of(Arguments.of("Empty set, empty collection → true", emptySet, Collections.emptyList(), true), // Set {1,2,3}, collection {1,2,3} → true
        Arguments.of("Exact match of all elements → true", buildIntegerSet(1, 2, 3), Arrays.asList(1, 2, 3), true), // Set {1,2,3}, collection {1,2} → true
        Arguments.of("Subset collection → true", buildIntegerSet(1, 2, 3), Arrays.asList(1, 2), true));
    }

    static Stream<Arguments> basicFunctionalityMissingElements() {
        CompactSet<Integer> base = buildIntegerSet(1, 2, 3);
        return Stream.of(Arguments.of("Missing element 4 → false", base, Arrays.asList(1, 4), false), Arguments.of("All elements missing → false", base, Arrays.asList(4, 5), false));
    }

    static Stream<Arguments> edgeCasesNullElements() {
        // Set containing null
        CompactSet<Integer> setWithNull = buildIntegerSetWithNull(1, 3);
        // Set without null
        CompactSet<Integer> setWithoutNull = buildIntegerSet(1, 3);
        return Stream.of(Arguments.of("Set contains null, query [null] → true", setWithNull, Collections.singletonList(null), true), Arguments.of("Set contains null, query [null,2] → false", setWithNull, Arrays.asList(null, 2), false), Arguments.of("Set without null, query [null] → false", setWithoutNull, Collections.singletonList(null), false), Arguments.of("Set without null, query [null,2] → false", setWithoutNull, Arrays.asList(null, 2), false));
    }

    static Stream<Arguments> duplicateElementsInArgument() {
        CompactSet<Integer> base = buildIntegerSet(1, 2, 3);
        return Stream.of(Arguments.of("Duplicates present but all distinct values exist → true", base, Arrays.asList(1, 1, 2), true), Arguments.of("Duplicate 1 and missing 4 → false", base, Arrays.asList(1, 1, 4), false));
    }

    static Stream<Arguments> emptySetNonEmptyCollection() {
        CompactSet<Integer> emptySet = new CompactSet<>();
        return Stream.of(Arguments.of("Empty set, query [1] → false", emptySet, Collections.singletonList(1), false), Arguments.of("Empty set, query ['a','b'] → false", emptySet, // using Character literals to stay generic
        Arrays.asList('a', 'b'), false));
    }

    static Stream<Arguments> mixedTypeElementsEqualsMismatch() {
        CompactSet<Integer> set = buildIntegerSet(1, 2);
        return Stream.of(Arguments.of("Integer 1 vs Double 1.0 → false", set, Arrays.asList(1, 1.0), false), Arguments.of("Mix of Double 1.0 and present 2 → false", set, Arrays.asList(1, 1.0, 2), false));
    }

    static Stream<Arguments> collectionImplementationVariations() {
        CompactSet<Integer> set = buildIntegerSet(1, 2);
        return Stream.of(Arguments.of("HashSet implementation, all present → true", set, new HashSet<>(Arrays.asList(1, 2)), true), Arguments.of("TreeSet implementation, element missing → false", set, new TreeSet<>(Collections.singletonList(4)), false));
    }

    @Test
    void containsAll_AllElementsPresent_ReturnsTrue() {
        // given
        Collection<String> query = Arrays.asList("apple", "banana");
        // when
        boolean result = setUnderTest.containsAll(query);
        // then
        assertTrue(result, "containsAll should return true when all queried elements are present");
    }

    @Test
    void containsAll_MissingElement_ReturnsFalse() {
        // given
        // "durian" is not in the set
        Collection<String> query = Arrays.asList("apple", "durian");
        // when
        boolean result = setUnderTest.containsAll(query);
        // then
        assertFalse(result, "containsAll should return false when at least one queried element is missing");
    }

    @Test
    void containsAll_EmptyCollection_ReturnsTrue() {
        // given
        Collection<String> empty = Collections.emptyList();
        // when
        boolean result = setUnderTest.containsAll(empty);
        // then
        assertTrue(result, "containsAll should return true for an empty collection (vacuous truth)");
    }

    @Test
    void containsAll_CollectionContainsNull_ReturnsFalse() {
        // given
        // The default CompactSet does not allow null elements (see existing tests).
        Collection<String> withNull = Arrays.asList("apple", null);
        // when
        boolean result = setUnderTest.containsAll(withNull);
        // then
        assertFalse(result, "containsAll should return false when the collection contains null and the set does not");
    }

    @Test
    void containsAll_DifferentCollectionImplementation_ReturnsTrue() {
        // given
        Set<String> hashSet = new HashSet<>(Arrays.asList("banana", "cherry"));
        // Using a HashSet instead of a List to verify that the method works with any Collection.
        // when
        boolean result = setUnderTest.containsAll(hashSet);
        // then
        assertTrue(result, "containsAll should work with any Collection implementation");
    }

    @Test
    void containsAll_IncompatibleElementType_ReturnsFalse() {
        // given
        // A collection that mixes correct type and an incompatible type.
        Collection<Object> mixed = Arrays.asList("apple", 42);
        // when
        boolean result = setUnderTest.containsAll(mixed);
        // then
        assertFalse(result, "containsAll should return false when the collection contains an element of an incompatible type");
    }

    @Test
    void containsAll_SupersetQuery_ReturnsFalse() {
        // given
        // Query collection larger than the set (contains an extra element).
        Collection<String> superset = Arrays.asList("apple", "banana", "cherry", "date");
        // when
        boolean result = setUnderTest.containsAll(superset);
        // then
        assertFalse(result, "containsAll should return false when the query collection is a superset of the set");
    }

    @Test
    void containsAll_SelfReference_ReturnsTrue() {
        // given
        // The set itself is a Collection.
        Collection<String> self = setUnderTest;
        // when
        boolean result = setUnderTest.containsAll(self);
        // then
        assertTrue(result, "containsAll should return true when the collection argument is the set itself");
    }

    // ---------- 1. Basic functionality – empty and full matches ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("basicFunctionalityEmptyAndFullMatches")
    void containsAll_BasicFunctionality_EmptyAndFullMatches(String description, CompactSet<Integer> set, Collection<?> query, boolean expected) {
        assertEquals(expected, set.containsAll(query), description);
    }

    // ---------- 2. Basic functionality – missing elements ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("basicFunctionalityMissingElements")
    void containsAll_BasicFunctionality_MissingElements(String description, CompactSet<Integer> set, Collection<?> query, boolean expected) {
        assertEquals(expected, set.containsAll(query), description);
    }

    // ---------- 3. Edge cases – null elements ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("edgeCasesNullElements")
    void containsAll_EdgeCases_NullElements(String description, CompactSet<Integer> set, Collection<?> query, boolean expected) {
        assertEquals(expected, set.containsAll(query), description);
    }

    // ---------- 4. Edge cases – duplicate elements in argument collection ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("duplicateElementsInArgument")
    void containsAll_DuplicateElementsInArgument(String description, CompactSet<Integer> set, Collection<?> query, boolean expected) {
        assertEquals(expected, set.containsAll(query), description);
    }

    // ---------- 5. Exception handling – null collection argument ----------
    @Test
    void containsAll_NullCollectionArgument_ThrowsNullPointerException() {
        CompactSet<Integer> set = buildIntegerSet(1, 2, 3);
        assertThrows(NullPointerException.class, () -> set.containsAll(null), "Passing null as collection argument must throw NullPointerException");
    }

    // ---------- 6. Empty set vs non‑empty collection ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("emptySetNonEmptyCollection")
    void containsAll_EmptySet_NonEmptyCollection(String description, CompactSet<Integer> emptySet, Collection<?> query, boolean expected) {
        assertEquals(expected, emptySet.containsAll(query), description);
    }

    // ---------- 7. Non‑empty set vs empty collection ----------
    @Test
    void containsAll_NonEmptySet_EmptyCollection_ReturnsTrue() {
        CompactSet<Integer> set = buildIntegerSet(1, 2, 3);
        assertTrue(set.containsAll(Collections.emptyList()), "Every set must contain all elements of an empty collection");
    }

    // ---------- 8. Mixed type elements – equals mismatch ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mixedTypeElementsEqualsMismatch")
    void containsAll_MixedTypeElementsEqualsMismatch(String description, CompactSet<Integer> set, Collection<?> query, boolean expected) {
        assertEquals(expected, set.containsAll(query), description);
    }

    // ---------- 9. Collection implementation variations ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("collectionImplementationVariations")
    void containsAll_CollectionImplementationVariations(String description, CompactSet<Integer> set, Collection<?> query, boolean expected) {
        assertEquals(expected, set.containsAll(query), description);
    }

    // ---------- 10. Large collection – all present ----------
    @Test
    void containsAll_LargeCollection_AllPresent_ReturnsTrue() {
        CompactSet<Integer> set = buildIntegerSet(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> query = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertTrue(set.containsAll(query), "All elements present in large collection should return true");
    }

    // ---------- 11. Large collection – one missing element ----------
    @Test
    void containsAll_LargeCollection_OneMissing_ReturnsFalse() {
        CompactSet<Integer> set = buildIntegerSet(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> query = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 11);
        assertFalse(set.containsAll(query), "Missing element in large collection should return false");
    }

    // ---------- 12. Duplicate nulls when null is present ----------
    @Test
    void containsAll_DuplicateNullsWhenNullPresent_ReturnsTrue() {
        CompactSet<Integer> set = buildIntegerSetWithNull(1);
        List<Integer> query = Arrays.asList(null, null, 1);
        assertTrue(set.containsAll(query), "Duplicate nulls should not affect result when null is present");
    }

    // ---------- 13. Exception handling – iterator throws RuntimeException ----------
    @Test
    void containsAll_FaultyCollection_IteratorThrowsRuntimeException_Propagates() {
        CompactSet<Integer> set = buildIntegerSet(1, 2, 3);
        Collection<Integer> faulty = new FaultyCollection<>(Arrays.asList(1, 2));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> set.containsAll(faulty), "RuntimeException from iterator should be propagated");
        assertEquals("iterator failure", ex.getMessage());
    }

    // ---------- 14. Exception handling – iterator throws ConcurrentModificationException ----------
    @Test
    void containsAll_ConcurrentModificationCollection_PropagatesException() {
        CompactSet<Integer> set = buildIntegerSet(1, 2, 3);
        Collection<Integer> concurrent = new ConcurrentModificationCollection<>(Arrays.asList(1, 2, 3));
        assertThrows(ConcurrentModificationException.class, () -> set.containsAll(concurrent), "ConcurrentModificationException should be propagated");
    }

    // ---------- 15. Exception handling – element equals throws exception ----------
    @Test
    void containsAll_ElementEqualsThrowsException_Propagates() {
        CompactSet<Integer> set = buildIntegerSet(1, 2);
        List<Object> list = new ArrayList<>();
        // this object's equals will throw
        list.add(new BadEqualsObject());
        list.add(1);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> set.containsAll(list), "Exception from element.equals should be propagated");
        assertEquals("bad equals", ex.getMessage());
    }
}
