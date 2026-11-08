package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;

class DeepEquals_deepHashCode_Test {

    /**
     * Simple mutable node used to create circular object graphs.
     */
    private static class Node {

        final String name;

        Node next;

        Node(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Node))
                return false;
            Node node = (Node) o;
            return Objects.equals(name, node.name) && Objects.equals(next, node.next);
        }

        @Override
        public int hashCode() {
            // Delegates to DeepEquals to avoid using the default Object hashCode.
            return DeepEquals.deepHashCode(this);
        }
    }

    /**
     * Supplies the null‑handling test data.
     *
     * <p>Two cases are covered (mirroring the JSON test description):
     * <ul>
     *   <li>Direct {@code null} argument – expected hash 0.</li>
     *   <li>A collection that contains a single {@code null} element – the element's hash is 0,
     *       and because the collection has no other elements the overall hash should also be 0.</li>
     * </ul>
     */
    private static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(Arguments.of("Direct null argument", null, 0), Arguments.of("Collection with a single null element", Collections.singletonList(null), 0));
    }

    @Test
    @DisplayName("deepHashCode returns identical values for equal primitive wrappers")
    void testPrimitiveWrapperHashConsistency() {
        Integer a = 42;
        // equal but distinct instance
        Integer b = Integer.valueOf(42);
        int hashA = DeepEquals.deepHashCode(a);
        int hashB = DeepEquals.deepHashCode(b);
        assertEquals(hashA, hashB, "Hash codes for equal Integer objects should match");
    }

    @Test
    @DisplayName("deepHashCode returns identical values for equal collections")
    void testCollectionHashConsistency() {
        List<String> list1 = Arrays.asList("alpha", "bravo", "charlie");
        // same content, different implementation
        List<String> list2 = new ArrayList<>(list1);
        int hash1 = DeepEquals.deepHashCode(list1);
        int hash2 = DeepEquals.deepHashCode(list2);
        assertEquals(hash1, hash2, "Hash codes for collections with identical contents should match");
    }

    @Test
    @DisplayName("deepHashCode returns identical values for equal maps")
    void testMapHashConsistency() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("one", 1);
        map1.put("two", 2);
        map1.put("three", 3);
        Map<String, Integer> map2 = new HashMap<>();
        // same entries, different concrete class
        map2.putAll(map1);
        int hash1 = DeepEquals.deepHashCode(map1);
        int hash2 = DeepEquals.deepHashCode(map2);
        assertEquals(hash1, hash2, "Hash codes for maps with identical entries should match");
    }

    @Test
    @DisplayName("deepHashCode handles null safely")
    void testNullHash() {
        int hash = DeepEquals.deepHashCode(null);
        // The implementation returns 0 for a null argument (checked via contract of deepHashCode)
        assertEquals(0, hash, "Hash code for null should be 0");
    }

    @Test
    @DisplayName("deepHashCode processes circular references without StackOverflow")
    void testCircularReference() {
        // Build two distinct but structurally identical circular graphs.
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = b;
        // creates a cycle A -> B -> A
        b.next = a;
        Node aCopy = new Node("A");
        Node bCopy = new Node("B");
        aCopy.next = bCopy;
        // identical cycle
        bCopy.next = aCopy;
        int hashOriginal = DeepEquals.deepHashCode(a);
        int hashCopy = DeepEquals.deepHashCode(aCopy);
        assertEquals(hashOriginal, hashCopy, "Hash codes for two identical circular structures should be equal");
    }

    @Test
    @DisplayName("different objects normally yield different hash codes")
    void testDifferentObjectsDifferentHash() {
        List<String> list = List.of("same", "content");
        // same elements but different type
        Set<String> set = new HashSet<>(list);
        int hashList = DeepEquals.deepHashCode(list);
        int hashSet = DeepEquals.deepHashCode(set);
        // It's possible (though unlikely) for two different structures to collide.
        // The assertion is written to surface a collision if it occurs.
        assertNotEquals(hashList, hashSet, "Hash codes for a List and a Set with the same elements should differ");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullHandlingProvider")
    @DisplayName("deepHashCode null‑handling scenarios")
    void testNullHandlingScenarios(String description, Object input, int expected) {
        int actual = DeepEquals.deepHashCode(input);
        assertEquals(expected, actual, () -> "Failed null‑handling scenario: " + description);
    }
}
