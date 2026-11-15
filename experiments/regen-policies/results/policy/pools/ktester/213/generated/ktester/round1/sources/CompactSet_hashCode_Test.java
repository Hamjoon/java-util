package com.cedarsoftware.util;

import java.util.*;
// import org.junit.runner.RunWith;
// import org.junit.runners.JUnit4;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
// import org.junit.Test;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

class CompactSet_hashCode_Test {

    /**
     * Supplies the arguments for {@link #hashCodeParameterized(String, java.util.function.Function, Set)}.
     */
    private static Stream<Arguments> hashCodeScenarioProvider() {
        return Stream.of(// Empty set (already covered, but kept for completeness)
        Arguments.of("Empty set", (Function<CompactSet<String>, CompactSet<String>>) s -> s, Collections.emptySet()), // Single element
        Arguments.of("Single element \"foo\"", (Function<CompactSet<String>, CompactSet<String>>) s -> {
            s.add("foo");
            return s;
        }, new HashSet<>(Collections.singleton("foo"))), // Multiple elements, order independent
        Arguments.of("Multiple elements \"alpha\",\"beta\",\"gamma\"", (Function<CompactSet<String>, CompactSet<String>>) s -> {
            s.add("beta");
            s.add("gamma");
            s.add("alpha");
            return s;
        }, new HashSet<>(Arrays.asList("alpha", "beta", "gamma"))), // Duplicates ignored
        Arguments.of("Duplicates \"x\",\"y\",\"x\",\"y\"", (Function<CompactSet<String>, CompactSet<String>>) s -> {
            s.add("x");
            s.add("y");
            s.add("x");
            s.add("y");
            return s;
        }, new HashSet<>(Arrays.asList("x", "y"))), // Case‑insensitive configuration
        Arguments.of("Case‑insensitive \"Test\",\"test\"", (Function<CompactSet<String>, CompactSet<String>>) s -> {
            CompactSet<String> ci = CompactSet.<String>builder().caseSensitive(false).build();
            ci.add("Test");
            ci.add("test");
            return ci;
        }, new HashSet<>(Collections.singleton("test"))), // After removal
        Arguments.of("After removing \"remove\"", (Function<CompactSet<String>, CompactSet<String>>) s -> {
            s.add("keep");
            s.add("remove");
            s.remove("remove");
            return s;
        }, new HashSet<>(Collections.singleton("keep"))));
    }

//     @Test
//     void hashCodeEmptySet() {
//         Set<String> compactSet = new CompactSet<>();
//         Set<String> reference = new HashSet<>();
//         assertTrue(compactSet.isEmpty(), "CompactSet should be empty");
//         assertEquals(reference.hashCode(), compactSet.hashCode(), "hashCode of an empty CompactSet must match that of an empty HashSet (0)");
//     }

//     @Test
//     void hashCodeWithElements() {
//         Set<String> compactSet = new CompactSet<>();
//         compactSet.add("foo");
//         compactSet.add("bar");
//         compactSet.add("baz");
//         Set<String> reference = new HashSet<>(Arrays.asList("foo", "bar", "baz"));
//         assertEquals(reference.hashCode(), compactSet.hashCode(), "hashCode should be based on the set of elements");
//     }

//     @Test
//     void hashCodeConsistencyAcrossEqualSets() {
//         CompactSet<String> setA = new CompactSet<>();
//         setA.add("alpha");
//         setA.add("beta");
//         setA.add("gamma");
//         CompactSet<String> setB = new CompactSet<>();
//         // Add in a different order
//         setB.add("gamma");
//         setB.add("alpha");
//         setB.add("beta");
//         assertEquals(setA, setB, "Two CompactSets with the same elements must be equal");
//         assertEquals(setA.hashCode(), setB.hashCode(), "Equal CompactSets must have identical hashCode values");
//     }

//     @Test
//     void hashCodeChangesWhenSetMutates() {
//         CompactSet<Integer> set = new CompactSet<>();
//         set.add(1);
//         set.add(2);
//         set.add(3);
//         int hashBefore = set.hashCode();
//         // Remove an element and verify the hashCode changes
//         set.remove(2);
//         int hashAfterRemoval = set.hashCode();
//         assertNotEquals(hashBefore, hashAfterRemoval, "hashCode must change after the contents of the set change");
//         // Add the element back – hash should return to original value
//         set.add(2);
//         int hashAfterReaddition = set.hashCode();
//         assertEquals(hashBefore, hashAfterReaddition, "hashCode should be restored after the set returns to its original contents");
//     }

//     @Test
//     void hashCodeMatchesUnderlyingKeySetHashCode() {
//         // This test mirrors the implementation detail: CompactSet.hashCode()
//         // delegates to map.keySet().hashCode()
//         CompactSet<String> compactSet = new CompactSet<>();
//         compactSet.add("one");
//         compactSet.add("two");
//         Set<String> expected = new HashSet<>(Arrays.asList("one", "two"));
//         assertEquals(expected.hashCode(), compactSet.hashCode(), "hashCode must equal the hashCode of the underlying keySet (represented by a normal Set)");
//     }

//     @Test
//     void hashCodeSingleElement() {
//         CompactSet<String> compactSet = new CompactSet<>();
//         compactSet.add("foo");
//         Set<String> reference = new HashSet<>(Collections.singleton("foo"));
//         assertEquals(reference.hashCode(), compactSet.hashCode(), "CompactSet with one element should have the same hashCode as a HashSet containing that element");
//     }

//     @Test
//     void hashCodeMultipleElementsOrderIndependent() {
//         CompactSet<String> setA = new CompactSet<>();
//         setA.add("alpha");
//         setA.add("beta");
//         setA.add("gamma");
//         CompactSet<String> setB = new CompactSet<>();
//         // Insert in a different order
//         setB.add("gamma");
//         setB.add("alpha");
//         setB.add("beta");
//         assertEquals(setA.hashCode(), setB.hashCode(), "Hash codes must be order‑independent for sets with the same elements");
//     }

//     @Test
//     void hashCodeDuplicateElementsIgnored() {
//         CompactSet<String> compactSet = new CompactSet<>();
//         compactSet.add("x");
//         compactSet.add("y");
//         // Add duplicates
//         compactSet.add("x");
//         compactSet.add("y");
//         Set<String> reference = new HashSet<>(Arrays.asList("x", "y"));
//         assertEquals(reference.hashCode(), compactSet.hashCode(), "Duplicate additions should not affect the hashCode");
//     }

//     @Test
//     void hashCodeCaseInsensitiveConfiguration() {
//         // Build a case‑insensitive CompactSet
//         CompactSet<String> ciSet = CompactSet.<String>builder().caseSensitive(false).build();
//         ciSet.add("Test");
//         // duplicate under case‑insensitive semantics
//         ciSet.add("test");
//         Set<String> reference = new HashSet<>(Collections.singleton("test"));
//         assertEquals(reference.hashCode(), ciSet.hashCode(), "Case‑insensitive CompactSet should treat \"Test\" and \"test\" as the same element");
//     }

//     @Test
//     void hashCodeAfterRemovalReflectsCurrentContents() {
//         CompactSet<String> compactSet = new CompactSet<>();
//         compactSet.add("keep");
//         compactSet.add("remove");
//         // Remove one element
//         compactSet.remove("remove");
//         Set<String> reference = new HashSet<>(Collections.singleton("keep"));
//         assertEquals(reference.hashCode(), compactSet.hashCode(), "hashCode after removal must match a HashSet with the remaining elements");
//     }

//     @Test
//     void hashCodeIdempotentWhenUnchanged() {
//         CompactSet<String> compactSet = new CompactSet<>();
//         compactSet.add("a");
//         compactSet.add("b");
//         compactSet.add("c");
//         int first = compactSet.hashCode();
//         int second = compactSet.hashCode();
//         int third = compactSet.hashCode();
//         assertEquals(first, second, "Second call should return the same hashCode as the first");
//         assertEquals(first, third, "Third call should also return the same hashCode");
//     }

    @ParameterizedTest(name = "{0}")
    @MethodSource("hashCodeScenarioProvider")
    void hashCodeParameterized(String description, Function<CompactSet<String>, CompactSet<String>> setBuilder, Set<String> expected) {
        CompactSet<String> compactSet = setBuilder.apply(new CompactSet<>());
        assertEquals(expected.hashCode(), compactSet.hashCode(), "hashCode mismatch for scenario: " + description);
    }
}