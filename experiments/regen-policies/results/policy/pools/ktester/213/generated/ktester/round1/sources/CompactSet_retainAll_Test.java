package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactSet_retainAll_Test {

    /**
     * A regular (case‑sensitive) CompactSet used by most tests.
     */
    private CompactSet<String> caseSensitiveSet;

    /**
     * A case‑insensitive CompactSet built via the builder.
     */
    private CompactSet<String> caseInsensitiveSet;

    /* --------------------------------------------------------------------- *
     * Helper class used by the parameterized tests.
     * --------------------------------------------------------------------- */
    private static class BasicCase<T> {

        private final List<T> initial;

        private final List<T> retain;

        private final boolean expectedChanged;

        private final List<T> expectedResult;

        private final String description;

        BasicCase(List<T> initial, List<T> retain, boolean expectedChanged, List<T> expectedResult, String description) {
            this.initial = initial;
            this.retain = retain;
            this.expectedChanged = expectedChanged;
            this.expectedResult = expectedResult;
            this.description = description;
        }

        List<T> initial() {
            return initial;
        }

        List<T> retain() {
            return retain;
        }

        boolean expectedChanged() {
            return expectedChanged;
        }

        List<T> expectedResult() {
            return expectedResult;
        }

        String description() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a case‑sensitive set with three distinct elements.
        caseSensitiveSet = new CompactSet<>();
        caseSensitiveSet.add("Apple");
        caseSensitiveSet.add("Banana");
        caseSensitiveSet.add("Cherry");
        // Initialise a case‑insensitive set (uses the builder API).
        caseInsensitiveSet = CompactSet.<String>builder().caseSensitive(false).build();
        caseInsensitiveSet.add("Apple");
        caseInsensitiveSet.add("Banana");
        caseInsensitiveSet.add("Cherry");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to avoid cross‑test contamination.
        caseSensitiveSet.clear();
        caseInsensitiveSet.clear();
    }

    static Stream<BasicCase<Integer>> basicIntegerProvider() {
        return Stream.of(new BasicCase<>(List.of(1, 2, 3), List.of(2, 3, 4), true, List.of(2, 3), "partial overlap – element 1 removed"), new BasicCase<>(List.of(1, 2), List.of(), true, List.of(), "empty retain collection clears the set"));
    }

    static Stream<BasicCase<String>> basicStringProvider() {
        return Stream.of(new BasicCase<>(List.of("a", "b"), List.of("a", "b"), false, List.of("a", "b"), "exact match – no change"));
    }

    @Test
    void retainAllSubsetChangesSet() {
        // Keep only "Apple" and "Banana"
        Collection<String> keep = Arrays.asList("Apple", "Banana");
        boolean changed = caseSensitiveSet.retainAll(keep);
        assertTrue(changed, "retainAll should report modification when elements are removed");
        assertEquals(2, caseSensitiveSet.size());
        assertTrue(caseSensitiveSet.containsAll(keep));
        assertFalse(caseSensitiveSet.contains("Cherry"));
    }

    @Test
    void retainAllEqualCollectionNoChange() {
        // Collection exactly matches the set content
        Collection<String> same = Arrays.asList("Apple", "Banana", "Cherry");
        boolean changed = caseSensitiveSet.retainAll(same);
        assertFalse(changed, "retainAll should report no modification when collection equals the set");
        assertEquals(3, caseSensitiveSet.size());
        assertTrue(caseSensitiveSet.containsAll(same));
    }

    @Test
    void retainAllEmptyCollectionClearsSet() {
        Collection<String> empty = Collections.emptyList();
        boolean changed = caseSensitiveSet.retainAll(empty);
        assertTrue(changed, "retainAll should report modification when clearing the set");
        assertTrue(caseSensitiveSet.isEmpty(), "Set should be empty after retaining an empty collection");
    }

    @Test
    void retainAllWithUnrelatedTypesEmptiesSet() {
        // Collection contains elements of a different type (Integer)
        Collection<Integer> numbers = Arrays.asList(1, 2, 3);
        @SuppressWarnings("unchecked")
        Collection<?> mixed = (Collection<?>) numbers;
        boolean changed = caseSensitiveSet.retainAll(mixed);
        assertTrue(changed, "retainAll should report modification when no element matches");
        assertTrue(caseSensitiveSet.isEmpty(), "Set should be empty when collection has no matching elements");
    }

    @Test
    void retainAllWithNullElementRemovesNullIfPresent() {
        // CompactSet permits null values (the default implementation of Set does).
        caseSensitiveSet.add(null);
        assertTrue(caseSensitiveSet.contains(null));
        Collection<String> keep = Arrays.asList("Apple", "Banana", "Cherry");
        boolean changed = caseSensitiveSet.retainAll(keep);
        assertTrue(changed, "retainAll should report modification when null is removed");
        assertFalse(caseSensitiveSet.contains(null), "Null should have been removed");
        assertEquals(3, caseSensitiveSet.size());
    }

    @Test
    void retainAllCaseInsensitiveSetRespectsCase() {
        // The case‑insensitive set contains "Apple", "Banana", "Cherry"
        // Retain only "apple" (lower‑case) – it should keep the original "Apple".
        Collection<String> keep = Collections.singletonList("apple");
        boolean changed = caseInsensitiveSet.retainAll(keep);
        assertTrue(changed, "retainAll should report modification when elements are removed");
        assertEquals(1, caseInsensitiveSet.size());
        assertTrue(caseInsensitiveSet.contains("Apple"));
        assertTrue(caseInsensitiveSet.contains("apple"));
    }

    @Test
    void retainAllCaseInsensitiveSetNoChangeWhenEqualIgnoringCase() {
        // Collection uses different case but should be considered equal.
        Collection<String> keep = Arrays.asList("apple", "BANANA", "cHeRrY");
        boolean changed = caseInsensitiveSet.retainAll(keep);
        assertFalse(changed, "retainAll should report no modification when collection equals set ignoring case");
        assertEquals(3, caseInsensitiveSet.size());
        assertTrue(caseInsensitiveSet.containsAll(Arrays.asList("Apple", "Banana", "Cherry")));
    }

    @Test
    void retainAllReturnsFalseWhenSetAlreadyEmpty() {
        CompactSet<String> emptySet = new CompactSet<>();
        Collection<String> any = Arrays.asList("x", "y");
        boolean changed = emptySet.retainAll(any);
        assertFalse(changed, "retainAll on an already empty set should return false");
        assertTrue(emptySet.isEmpty());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource({ "basicIntegerProvider", "basicStringProvider" })
    <T> void retainAllBasicFunctionality(BasicCase<T> tc) {
        // Build a fresh CompactSet with the supplied initial elements.
        CompactSet<T> set = new CompactSet<>();
        tc.initial().forEach(set::add);
        boolean changed = set.retainAll(tc.retain());
        assertEquals(tc.expectedChanged(), changed, "Unexpected modification flag for case: " + tc.description());
        assertEquals(tc.expectedResult().size(), set.size(), "Unexpected size after retainAll for case: " + tc.description());
        assertTrue(set.containsAll(tc.expectedResult()), "Set does not contain the expected elements after retainAll for case: " + tc.description());
    }

    @Test
    void retainAllOnEmptySetWithNonEmptyCollectionReturnsFalse() {
        // The JSON case: set empty, collection ["x"] → false, set stays empty.
        CompactSet<Integer> emptySet = new CompactSet<>();
        Collection<Integer> collection = List.of(9);
        boolean changed = emptySet.retainAll(collection);
        assertFalse(changed, "retainAll on an empty set should return false even if collection is non‑empty");
        assertTrue(emptySet.isEmpty(), "Set must remain empty");
    }

    @Test
    void retainAllSupersetCollectionNoChange() {
        // Set [1,2]; collection [0,1,2,3] → false, unchanged.
        CompactSet<Integer> set = new CompactSet<>();
        set.add(1);
        set.add(2);
        Collection<Integer> superset = List.of(0, 1, 2, 3);
        boolean changed = set.retainAll(superset);
        assertFalse(changed, "Superset collection should not modify the set");
        assertEquals(2, set.size());
        assertTrue(set.containsAll(List.of(1, 2)));
    }

    @Test
    void retainAllEmptySetAndEmptyCollectionNoChange() {
        CompactSet<String> set = new CompactSet<>();
        boolean changed = set.retainAll(Collections.emptyList());
        assertFalse(changed, "Both set and collection empty → no modification");
        assertTrue(set.isEmpty());
    }

    @Test
    void retainAllCollectionOnlyNullSetWithoutNull() {
        // Set [1,2]; collection [null] → true, set becomes empty.
        CompactSet<Integer> set = new CompactSet<>();
        set.add(1);
        set.add(2);
        Collection<Integer> onlyNull = Collections.singletonList(null);
        boolean changed = set.retainAll(onlyNull);
        assertTrue(changed, "Null collection element not present should clear the set");
        assertTrue(set.isEmpty(), "Set should be empty after retaining only null");
    }

    @Test
    void retainAllCollectionOnlyNullSetWithNull() {
        // Set [null]; collection [null] → false, set remains [null].
        CompactSet<Integer> set = new CompactSet<>();
        set.add(null);
        Collection<Integer> onlyNull = Collections.singletonList(null);
        boolean changed = set.retainAll(onlyNull);
        assertFalse(changed, "Retaining identical null element should not modify the set");
        assertEquals(1, set.size());
        assertTrue(set.contains(null));
    }

    @Test
    void retainAllCollectionWithDuplicateElements() {
        // Set [1,2,3]; collection [2,2,3,3] → true, result [2,3].
        CompactSet<Integer> set = new CompactSet<>();
        set.add(1);
        set.add(2);
        set.add(3);
        Collection<Integer> duplicates = List.of(2, 2, 3, 3);
        boolean changed = set.retainAll(duplicates);
        assertTrue(changed, "Duplicates in collection should still cause removal of non‑present elements");
        assertEquals(2, set.size());
        assertTrue(set.containsAll(List.of(2, 3)));
        assertFalse(set.contains(1));
    }

    @Test
    void retainAllWithSelfReferenceNoChange() {
        // Set [1,2,3]; retainAll(self) → false, unchanged.
        CompactSet<Integer> set = new CompactSet<>();
        set.add(1);
        set.add(2);
        set.add(3);
        boolean changed = set.retainAll(set);
        assertFalse(changed, "Calling retainAll with the set itself must not modify it");
        assertEquals(3, set.size());
        assertTrue(set.containsAll(List.of(1, 2, 3)));
    }

    @Test
    void retainAllNullArgumentThrowsNPE() {
        CompactSet<String> set = new CompactSet<>();
        assertThrows(NullPointerException.class, () -> set.retainAll(null), "retainAll(null) must throw NullPointerException as per Set contract");
    }

    @Test
    void retainAllClassCastExceptionDueToOrdering_IntegerSetWithString() {
        // Build a sorted (TreeMap‑backed) CompactSet of Integers.
        CompactSet<Integer> set = CompactSet.<Integer>builder().sortedOrder().build();
        set.add(1);
        set.add(2);
        // Attempt to retain a collection containing a String – should trigger CCE.
        Collection<?> mixed = List.of("string");
        assertThrows(ClassCastException.class, () -> set.retainAll(mixed), "Incompatible element type should cause ClassCastException for sorted sets");
    }

    @Test
    void retainAllClassCastExceptionDueToOrdering_IntegerSetWithObject() {
        CompactSet<Integer> set = CompactSet.<Integer>builder().sortedOrder().build();
        set.add(10);
        set.add(20);
        // An Object that does not implement Comparable<Integer>
        Object nonComparable = new Object();
        Collection<?> mixed = List.of(nonComparable);
        assertThrows(ClassCastException.class, () -> set.retainAll(mixed), "Non‑Comparable element in collection should cause ClassCastException for sorted sets");
    }
}
