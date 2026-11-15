package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_hashCode_Test {

    private static final class Dummy {

        private final String id;

        Dummy(String id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            // intentional collision
            return 42;
        }

        @Override
        public boolean equals(Object o) {
            // deliberately return false unless same instance
            return this == o;
        }

        @Override
        public String toString() {
            return "Dummy{" + id + '}';
        }
    }

    /* ----------------------------------------------------------------------
     *  Helper for building a reference Set that follows the standard Set.hashCode()
     *  contract.  This is used to verify that {@code CaseInsensitiveSet} delegates
     *  correctly to {@code map.keySet().hashCode()}.
     * ---------------------------------------------------------------------- */
    private static int referenceHash(Object... elements) {
        Set<Object> ref = new HashSet<>();
        Collections.addAll(ref, elements);
        return ref.hashCode();
    }

    private static Stream<Arguments> behaviorProvider() {
        return Stream.of(Arguments.of(new String[] {}, 0, "empty set"), Arguments.of(new String[] { "a" }, "a".hashCode(), "single element"), Arguments.of(new String[] { "a", "b" }, "a".hashCode() + "b".hashCode(), "two distinct elements"), Arguments.of(new String[] { "a", "A", "b", "B" }, "a".hashCode() + "b".hashCode(), "case‑insensitive duplicates ignored"));
    }

    /**
     * An empty {@link CaseInsensitiveSet} should produce the same hash code as an
     * empty {@link Set}. The implementation delegates to {@code map.keySet().hashCode()}.
     */
    @Test
    void hashCodeEmptySet() {
        Set<Object> cisSet = new CaseInsensitiveSet<>();
        Set<Object> reference = Collections.emptySet();
        assertEquals(reference.hashCode(), cisSet.hashCode(), "hashCode of an empty CaseInsensitiveSet must match that of an empty Set");
    }

    /**
     * Verify that the hash code reflects the case‑insensitive semantics.
     * Adding duplicate strings with different case should not affect the hash.
     */
    @Test
    void hashCodeWithCaseInsensitiveDuplicates() {
        CaseInsensitiveSet<String> cisSet = new CaseInsensitiveSet<>();
        cisSet.add("One");
        // duplicate, different case – should be ignored
        cisSet.add("ONE");
        cisSet.add("Two");
        // duplicate, different case – should be ignored
        cisSet.add("tWo");
        cisSet.add("Three");
        // Build a reference Set that contains only the first occurrence of each logical value
        Set<String> reference = new HashSet<>();
        reference.add("One");
        reference.add("Two");
        reference.add("Three");
        assertEquals(reference.hashCode(), cisSet.hashCode(), "hashCode must be based on the logical contents, ignoring case‑duplicates");
    }

    /**
     * Two equal {@link CaseInsensitiveSet} instances must have identical hash codes.
     */
    @Test
    void hashCodeConsistencyWithEquals() {
        CaseInsensitiveSet<String> setA = new CaseInsensitiveSet<>();
        setA.add("Apple");
        setA.add("banana");
        setA.add("Cherry");
        CaseInsensitiveSet<String> setB = new CaseInsensitiveSet<>();
        // different case, same logical element
        setB.add("apple");
        setB.add("BANANA");
        setB.add("cherry");
        assertTrue(setA.equals(setB), "Sets should be equal under case‑insensitive comparison");
        assertEquals(setA.hashCode(), setB.hashCode(), "Equal sets must return the same hashCode");
    }

    /**
     * Ensure that the hash code changes when the logical content of the set changes.
     */
    @Test
    void hashCodeChangesWhenContentChanges() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("Alpha");
        int hashBefore = set.hashCode();
        set.add("Beta");
        int hashAfterAdd = set.hashCode();
        assertNotEquals(hashBefore, hashAfterAdd, "hashCode should change after adding a new element");
        // removes "Alpha" (case‑insensitive)
        set.remove("alpha");
        int hashAfterRemove = set.hashCode();
        assertNotEquals(hashAfterAdd, hashAfterRemove, "hashCode should change after removing an element");
    }

    @Test
    void hashCodeSingleElement() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("a");
        assertEquals("a".hashCode(), set.hashCode(), "Hash code with a single element should equal that element's hashCode");
    }

    @Test
    void hashCodeTwoElements() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("a");
        set.add("b");
        assertEquals("a".hashCode() + "b".hashCode(), set.hashCode(), "Hash code of two distinct elements must be the sum of their hashCodes");
    }

    @Test
    void hashCodeIgnoresCaseInsensitiveDuplicates() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("a");
        // duplicate, different case
        set.add("A");
        set.add("b");
        // duplicate, different case
        set.add("B");
        assertEquals(referenceHash("a", "b"), set.hashCode(), "Case‑insensitive duplicates must not affect the hash code");
    }

    @Test
    void hashCodeAfterRemoval() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("a");
        set.add("b");
        assertEquals(referenceHash("a", "b"), set.hashCode());
        // case‑insensitive removal
        set.remove("a");
        assertEquals("b".hashCode(), set.hashCode(), "Hash code must reflect the removal of an element");
    }

    @Test
    void hashCodeConsistencyMultipleCalls() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("x");
        set.add("y");
        int first = set.hashCode();
        int second = set.hashCode();
        int third = set.hashCode();
        assertEquals(first, second, "Repeated hashCode calls must return the same value");
        assertEquals(second, third, "Repeated hashCode calls must return the same value");
    }

    @Test
    void hashCodeWithOnlyNull() {
        CaseInsensitiveSet<Object> set = new CaseInsensitiveSet<>();
        set.add(null);
        // Set.hashCode treats null as 0 contribution
        assertEquals(0, set.hashCode(), "A set containing only null must have hash code 0");
    }

    @Test
    void hashCodeWithNullAndNonNull() {
        CaseInsensitiveSet<Object> set = new CaseInsensitiveSet<>();
        set.add(null);
        set.add("a");
        assertEquals(referenceHash(null, "a"), set.hashCode(), "Hash code must combine null (0) and the non‑null element's hashCode");
    }

    @Test
    void hashCodeAfterClear() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("foo");
        set.add("bar");
        set.clear();
        assertEquals(0, set.hashCode(), "After clear() the hash code must be 0");
    }

    @Test
    void hashCodeWithIntegers() {
        CaseInsensitiveSet<Integer> set = new CaseInsensitiveSet<>();
        set.add(1);
        set.add(2);
        // Integer.hashCode() returns the int value itself
        assertEquals(1 + 2, set.hashCode(), "Hash code for Integer elements should be the sum of their values");
    }

    @Test
    void hashCodeOrderIndependenceDifferentInsertionOrders() {
        // Order 1: c, a, b
        CaseInsensitiveSet<String> set1 = new CaseInsensitiveSet<>();
        set1.add("c");
        set1.add("a");
        set1.add("b");
        int hash1 = set1.hashCode();
        // Order 2: a, b, c
        CaseInsensitiveSet<String> set2 = new CaseInsensitiveSet<>();
        set2.add("a");
        set2.add("b");
        set2.add("c");
        int hash2 = set2.hashCode();
        int expected = "a".hashCode() + "b".hashCode() + "c".hashCode();
        assertEquals(expected, hash1, "Hash must be order‑independent (c,a,b)");
        assertEquals(expected, hash2, "Hash must be order‑independent (a,b,c)");
        assertEquals(hash1, hash2, "Both insertion orders must yield the same hash");
    }

    @Test
    void hashCodeAfterAddAllWithDuplicates() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        List<String> source = Arrays.asList("a", "A", "b", "B", "b");
        set.addAll(source);
        assertEquals(referenceHash("a", "b"), set.hashCode(), "addAll must ignore case‑insensitive duplicates");
    }

    @Test
    void hashCodeWithCollidingCustomObjects() {
        CaseInsensitiveSet<Dummy> set = new CaseInsensitiveSet<>();
        Dummy d1 = new Dummy("first");
        Dummy d2 = new Dummy("second");
        set.add(d1);
        set.add(d2);
        // Both objects are kept because equals() is false for different instances
        assertEquals(42 + 42, set.hashCode(), "Hash code must sum the hashCodes of distinct objects even if they collide");
    }

    @Test
    void hashCodeAfterIteratorRemove() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("a");
        set.add("b");
        set.add("c");
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            String val = it.next();
            if ("b".equalsIgnoreCase(val)) {
                // remove via iterator
                it.remove();
                break;
            }
        }
        assertEquals(referenceHash("a", "c"), set.hashCode(), "After iterator.remove() the hash must reflect the remaining elements");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("behaviorProvider")
    void hashCodeBehaviorParameterized(String[] elements, int expected, String description) {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        for (String e : elements) {
            set.add(e);
        }
        assertEquals(expected, set.hashCode(), "Hash code for scenario: " + description);
    }

    @Test
    void hashCodeConsistencyWithoutMutation() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("alpha", "beta", "gamma"));
        int first = set.hashCode();
        for (int i = 0; i < 5; i++) {
            assertEquals(first, set.hashCode(), "hashCode must remain stable across multiple calls without mutation");
        }
    }
}
