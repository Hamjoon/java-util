package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_retainAll_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh set for each test
        set = new SealableNavigableSet<>(sealedSupplier);
        // Populate with a known ordering
        set.add(10);
        set.add(20);
        set.add(30);
        set.add(40);
        // Ensure the set starts in an un‑sealed state
        sealedState = false;
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the sealed flag so that a failure in one test does not affect the next
        sealedState = false;
        set = null;
    }

    private static Stream<Arguments> provideNotPresentElements() {
        return Stream.of(Arguments.of("All existing elements are removed because none match the collection, so retainAll should clear the set and return true.", Arrays.asList(99, 100), true));
    }

    private static Stream<Arguments> provideSomeMatchingElements() {
        return Stream.of(Arguments.of("Original set contains [1,2,3,4]; retainAll with [2,4,5] removes 1 and 3, leaving [2,4]. The set changes, so retainAll returns true.", Arrays.asList(1, 2, 3, 4), Arrays.asList(2, 4, 5), true, Set.of(2, 4)));
    }

    private static Stream<Arguments> provideIdenticalCollection() {
        return Stream.of(Arguments.of("Set initially holds [1,2,3]; the collection passed to retainAll contains exactly the same elements, so no modification occurs and the method returns false.", Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 3), false));
    }

    private static Stream<Arguments> provideDuplicateElements() {
        return Stream.of(Arguments.of("Set contains [1,2,3]; the collection has duplicates of 2. retainAll keeps only the element 2, removing 1 and 3, thus the set changes and returns true.", Arrays.asList(1, 2, 3), Arrays.asList(2, 2, 2), true, Set.of(2)));
    }

    @Test
    void retainAll_Unsealed_ChangesSetAndReturnsTrue() {
        // GIVEN
        Collection<Integer> toRetain = Arrays.asList(20, 30);
        // WHEN
        boolean changed = set.retainAll(toRetain);
        // THEN
        assertTrue(changed, "retainAll should report that the set was modified");
        assertEquals(2, set.size(), "Set should contain only the retained elements");
        assertTrue(set.containsAll(toRetain), "Set should contain the retained elements");
        assertFalse(set.contains(10), "Element 10 should have been removed");
        assertFalse(set.contains(40), "Element 40 should have been removed");
    }

    @Test
    void retainAll_Unsealed_NoChangeReturnsFalse() {
        // GIVEN
        Collection<Integer> sameElements = Arrays.asList(10, 20, 30, 40);
        // WHEN
        boolean changed = set.retainAll(sameElements);
        // THEN
        assertFalse(changed, "retainAll should report no modification when the collection matches the set");
        assertEquals(4, set.size(), "Set size should remain unchanged");
    }

    @Test
    void retainAll_Unsealed_EmptyCollectionClearsSet() {
        // GIVEN
        Collection<Integer> empty = Collections.emptyList();
        // WHEN
        boolean changed = set.retainAll(empty);
        // THEN
        assertTrue(changed, "retainAll should report modification when clearing the set");
        assertTrue(set.isEmpty(), "Set should be empty after retaining an empty collection");
    }

    @Test
    void retainAll_Sealed_ThrowsUnsupportedOperationException() {
        // GIVEN
        // seal the set
        sealedState = true;
        Collection<Integer> toRetain = Arrays.asList(20, 30);
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> set.retainAll(toRetain), "retainAll must throw when the set is sealed");
    }

    @Test
    void retainAll_NullCollection_ThrowsNullPointerException() {
        // GIVEN
        Collection<Integer> nullCol = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> set.retainAll(nullCol), "retainAll should throw NPE when passed a null collection");
    }

    @Test
    void retainAll_Sealed_ThrowsIllegalStateException() {
        // GIVEN
        // seal the set
        sealedState = true;
        Collection<Integer> toRetain = Arrays.asList(20, 30);
        // WHEN / THEN
        assertThrows(IllegalStateException.class, () -> set.retainAll(toRetain), "retainAll must throw IllegalStateException when the set is sealed");
    }

    // ---------- 1. collection with elements not present (clears set) ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideNotPresentElements")
    void retainAll_CollectionWithNoMatchingElements(String description, Collection<Integer> col, boolean expectedResult) {
        // WHEN
        boolean changed = set.retainAll(col);
        // THEN
        assertEquals(expectedResult, changed, description);
        assertTrue(set.isEmpty(), "Set should be empty after retaining a collection with no matching elements");
    }

    // ---------- 2. collection with some matching elements ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideSomeMatchingElements")
    void retainAll_CollectionWithSomeMatchingElements(String description, List<Integer> initialElements, Collection<Integer> col, boolean expectedResult, Set<Integer> expectedRemaining) {
        // Re‑initialise the set with a specific initial content for this scenario
        set = new SealableNavigableSet<>(() -> false);
        initialElements.forEach(set::add);
        // WHEN
        boolean changed = set.retainAll(col);
        // THEN
        assertEquals(expectedResult, changed, description);
        assertEquals(expectedRemaining, new HashSet<>(set), "Remaining elements after retainAll do not match expectation");
    }

    // ---------- 3. collection identical to set returns false ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideIdenticalCollection")
    void retainAll_IdenticalCollection(String description, List<Integer> initialElements, Collection<Integer> col, boolean expectedResult) {
        set = new SealableNavigableSet<>(() -> false);
        initialElements.forEach(set::add);
        boolean changed = set.retainAll(col);
        assertEquals(expectedResult, changed, description);
        assertEquals(new HashSet<>(initialElements), new HashSet<>(set), "Set content must remain unchanged");
    }

    // ---------- 4. collection with duplicate elements ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideDuplicateElements")
    void retainAll_CollectionWithDuplicateElements(String description, List<Integer> initialElements, Collection<Integer> col, boolean expectedResult, Set<Integer> expectedRemaining) {
        set = new SealableNavigableSet<>(() -> false);
        initialElements.forEach(set::add);
        boolean changed = set.retainAll(col);
        assertEquals(expectedResult, changed, description);
        assertEquals(expectedRemaining, new HashSet<>(set), "Resulting set does not match expected after duplicates handling");
    }

    // ---------- 5. collection of different type but with matching element ----------
    @Test
    void retainAll_DifferentTypeCollectionWithMatchingElement() {
        // This test uses a String‑typed SealableNavigableSet
        SealableNavigableSet<String> stringSet = new SealableNavigableSet<>(() -> false);
        stringSet.add("a");
        stringSet.add("b");
        // contains a matching String and an unrelated Integer
        Collection<Object> mixed = Arrays.asList("b", 123);
        boolean changed = stringSet.retainAll(mixed);
        assertTrue(changed, "retainAll should return true when the collection causes a change");
        assertEquals(Set.of("b"), new HashSet<>(stringSet), "Only the matching element should remain");
    }

    // ---------- 6. collection contains null when set does not allow null ----------
    @Test
    void retainAll_CollectionContainsNull_ThrowsNullPointerException() {
        // TreeSet (used internally) does not allow null with natural ordering
        Collection<Object> colWithNull = Collections.singletonList((Object) null);
        assertThrows(NullPointerException.class, () -> set.retainAll(colWithNull), "retainAll should throw NullPointerException when collection contains null and the backing set disallows it");
    }

    // ---------- 7. class cast exception for incompatible element type ----------
    @Test
    void retainAll_IncompatibleElementType_ThrowsClassCastException() {
        // The backing set holds Integer, passing a String forces a ClassCastException
        Collection<String> wrongType = Collections.singletonList("foo");
        assertThrows(ClassCastException.class, () -> set.retainAll(wrongType), "retainAll should throw ClassCastException when collection contains an element of an incompatible type");
    }
}
