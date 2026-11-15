package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_hashCode_Test {

    static Stream<Arguments> hashCodeEqualityProvider() {
        return Stream.of(Arguments.of("single element", Collections.singleton("hello"), new HashSet<>(Collections.singleton("hello"))), Arguments.of("multiple distinct elements", new HashSet<>(Arrays.asList("alpha", "beta", "gamma")), new HashSet<>(Arrays.asList("alpha", "beta", "gamma"))), Arguments.of("case‑insensitive duplicate", new HashSet<>(Arrays.asList("Test", "test")), new HashSet<>(Collections.singleton("Test"))));
    }

    /**
     * Two sets that are logically equal (including case‑insensitive handling) must have the same hashCode.
     */
    @Test
    void hashCodeEqualsWhenSetsAreEqual() {
        CaseInsensitiveSet<String> setA = new CaseInsensitiveSet<>();
        setA.add("One");
        setA.add("Two");
        setA.add("Three");
        CaseInsensitiveSet<String> setB = new CaseInsensitiveSet<>();
        // use different cases to verify case‑insensitivity
        setB.add("ONE");
        setB.add("two");
        setB.add("tHrEe");
        assertEquals(setA, setB, "Sets should be equal (case‑insensitive)");
        assertEquals(setA.hashCode(), setB.hashCode(), "Equal sets must produce identical hashCode values");
    }

    /**
     * Sets that differ in at least one logical element must produce different hashCodes.
     */
    @Test
    void hashCodeDiffersWhenSetsDiffer() {
        CaseInsensitiveSet<String> setA = new CaseInsensitiveSet<>();
        setA.add("Apple");
        setA.add("Banana");
        CaseInsensitiveSet<String> setB = new CaseInsensitiveSet<>();
        setB.add("Apple");
        setB.add("Banana");
        // extra element
        setB.add("Cherry");
        assertNotEquals(setA, setB, "Sets are not equal");
        assertNotEquals(setA.hashCode(), setB.hashCode(), "Unequal sets should have different hashCode values");
    }

    /**
     * The hashCode of an empty CaseInsensitiveSet must match the hashCode of an empty standard Set.
     */
    @Test
    void hashCodeOfEmptySet() {
        CaseInsensitiveSet<Object> ciSet = new CaseInsensitiveSet<>();
        Set<Object> emptyHashSet = new HashSet<>();
        assertTrue(ciSet.isEmpty(), "CaseInsensitiveSet should be empty");
        assertEquals(emptyHashSet.hashCode(), ciSet.hashCode(), "Empty sets must have identical hashCode values");
    }

    /**
     * Different backing map implementations should not affect the logical hashCode result.
     */
    @Test
    void hashCodeIndependentOfBackingMap() {
        // Default backing map (CaseInsensitiveMap with LinkedHashMap)
        CaseInsensitiveSet<String> defaultMapSet = new CaseInsensitiveSet<>();
        defaultMapSet.addAll(Arrays.asList("Alpha", "Beta", "Gamma"));
        // Backing map supplied explicitly (TreeMap)
        Map<String, Object> treeMapBacking = new CaseInsensitiveMap<>(new TreeMap<>());
        CaseInsensitiveSet<String> treeMapSet = new CaseInsensitiveSet<>(Collections.emptySet(), treeMapBacking);
        treeMapSet.addAll(Arrays.asList("Alpha", "Beta", "Gamma"));
        assertEquals(defaultMapSet, treeMapSet, "Sets with different backing maps should be equal");
        assertEquals(defaultMapSet.hashCode(), treeMapSet.hashCode(), "Hash codes must be identical regardless of backing map implementation");
    }

    /**
     * Verify that hashCode is consistent across multiple invocations (no mutation between calls).
     */
    @Test
    void hashCodeIsStableAcrossCalls() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("X", "Y", "Z"));
        int first = set.hashCode();
        int second = set.hashCode();
        int third = set.hashCode();
        assertEquals(first, second, "hashCode should be stable across calls");
        assertEquals(second, third, "hashCode should be stable across calls");
    }

    /**
     * Empty set hashCode should be 0 (the hash code of an empty keySet).
     */
    @Test
    void hashCodeEmptySetIsZero() {
        CaseInsensitiveSet<String> empty = new CaseInsensitiveSet<>();
        assertEquals(0, empty.hashCode(), "A newly created empty CaseInsensitiveSet must have hashCode 0");
    }

    /**
     * Single element set hashCode must match a regular HashSet containing the same element.
     */
    @Test
    void hashCodeSingleElement() {
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.add("hello");
        Set<String> reference = new HashSet<>(Collections.singleton("hello"));
        assertEquals(reference.hashCode(), ciSet.hashCode(), "Hash code of single‑element set should equal that of a standard HashSet");
    }

    /**
     * Multiple distinct elements – hashCode must equal that of a regular HashSet with same elements.
     */
    @Test
    void hashCodeMultipleDistinctElements() {
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.addAll(Arrays.asList("alpha", "beta", "gamma"));
        Set<String> reference = new HashSet<>(Arrays.asList("alpha", "beta", "gamma"));
        assertEquals(reference.hashCode(), ciSet.hashCode(), "Hash code of multi‑element set should match standard HashSet hashCode");
    }

    /**
     * Adding case‑insensitive duplicates should result in a single logical element,
     * therefore hashCode equals that of a singleton set.
     */
    @Test
    void hashCodeCaseInsensitiveDuplicates() {
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.add("Test");
        // duplicate, different case
        ciSet.add("test");
        Set<String> reference = new HashSet<>(Collections.singleton("Test"));
        assertEquals(reference.hashCode(), ciSet.hashCode(), "Duplicate case‑insensitive entries must not affect hashCode");
    }

    /**
     * After removing elements, hashCode must reflect the remaining logical contents.
     */
    @Test
    void hashCodeAfterRemovals() {
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.addAll(Arrays.asList("keep", "remove1", "remove2"));
        ciSet.remove("remove1");
        ciSet.remove("remove2");
        Set<String> reference = new HashSet<>(Collections.singleton("keep"));
        assertEquals(reference.hashCode(), ciSet.hashCode(), "Hash code after removals should match hash code of remaining elements");
    }

    /**
     * Large set stability – hashCode must equal that of a standard HashSet built from the same keys.
     */
    @Test
    void hashCodeLargeSetStability() {
        // Build 1000 unique keys
        List<String> keys = IntStream.range(0, 1000).mapToObj(i -> "key" + i).collect(Collectors.toList());
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.addAll(keys);
        Set<String> reference = new HashSet<>(keys);
        assertEquals(reference.hashCode(), ciSet.hashCode(), "Hash code of large set must match hash code of equivalent standard HashSet");
    }

    /**
     * Adding a null element and invoking hashCode should throw NullPointerException.
     */
    @Test
    void hashCodeNullElementThrowsNPE() {
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        // underlying map now contains a null key
        ciSet.add(null);
        assertThrows(NullPointerException.class, ciSet::hashCode, "hashCode() should throw NPE when the set contains a null key");
    }

    /**
     * Parameterized test that checks hashCode equality against a reference HashSet
     * for several collections (single, multiple, case‑insensitive duplicate).
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("hashCodeEqualityProvider")
    void hashCodeMatchesReferenceSet(String description, Set<String> elements, Set<String> reference) {
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.addAll(elements);
        assertEquals(reference.hashCode(), ciSet.hashCode(), "hashCode should match reference HashSet for scenario: " + description);
    }
}
