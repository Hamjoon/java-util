package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.ReflectPermission;
// No external resources or mocks are required for these tests.
import java.security.Permission;
import java.util.stream.Stream;

class DeepEquals_deepHashCode_Test {

    // Simple helper class to create a cyclic reference.
    private static class Node {

        String name;

        Node next;

        Node(String name) {
            this.name = name;
        }
    }

    /**
     * Helper that overrides hashCode to return a supplied value.
     */
    private static class CustomHash {

        private final int value;

        CustomHash(int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    /**
     * Helper utilities mirroring the ones referenced in the JSON test cases.
     */
    private static class DeepEqualsTestHelper {

        /**
         * Returns a simple two‑node cyclic structure.
         */
        static Object cyclicNode() {
            class CyNode {

                CyNode next;
            }
            CyNode a = new CyNode();
            CyNode b = new CyNode();
            a.next = b;
            b.next = a;
            return a;
        }

        /**
         * Builds a deeply nested Object[] array of the specified depth.
         */
        static Object deeplyNestedArray(int depth) {
            Object current = null;
            for (int i = 0; i < depth; i++) {
                current = new Object[] { current };
            }
            return current;
        }
    }

    class CyNode {

        CyNode next;
    }

    class CyNode {

        CyNode next;
    }

    /**
     * Class with a private field that will trigger a SecurityException under a restrictive SecurityManager.
     */
    private static class InaccessibleFieldHolder {

        private final String secret = "top-secret";
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> primitiveWrapperProvider() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of(Integer.valueOf(42), 42), org.junit.jupiter.params.provider.Arguments.of(Long.valueOf(123456789L), 123456789), org.junit.jupiter.params.provider.Arguments.of(Boolean.TRUE, 1231), org.junit.jupiter.params.provider.Arguments.of(Double.valueOf(3.14), Double.hashCode(3.14)));
    }

    /**
     * Verify that deepHashCode returns the same value for objects that are
     * considered deeply equal. Primitive wrapper objects with the same value
     * should produce identical hash codes.
     */
    @Test
    void testDeepHashCode_PrimitiveWrappers() {
        Integer i1 = Integer.valueOf(42);
        Integer i2 = Integer.valueOf(42);
        Long l1 = Long.valueOf(42L);
        int hashI1 = DeepEquals.deepHashCode(i1);
        int hashI2 = DeepEquals.deepHashCode(i2);
        int hashL1 = DeepEquals.deepHashCode(l1);
        // Same wrapper value → same hash
        assertEquals(hashI1, hashI2, "Two Integer objects with the same value should have identical deep hash codes");
        // Different wrapper types (Integer vs Long) may have different hash codes;
        // however, the deepEquals contract treats numeric values as equal, so we
        // only assert that the method does not throw and returns a deterministic value.
        assertNotEquals(0, hashL1, "Hash code for a Long should be non‑zero");
    }

    /**
     * Verify that unordered collections (e.g., HashSet) produce the same deep hash
     * code regardless of iteration order.
     */
    @Test
    void testDeepHashCode_UnorderedCollection() {
        Set<String> setA = new HashSet<>(Arrays.asList("alpha", "beta", "gamma"));
        // different insertion order
        Set<String> setB = new HashSet<>(Arrays.asList("gamma", "alpha", "beta"));
        int hashA = DeepEquals.deepHashCode(setA);
        int hashB = DeepEquals.deepHashCode(setB);
        assertEquals(hashA, hashB, "Two HashSet instances with identical contents should have the same deep hash code");
    }

    /**
     * Verify that maps with identical entries generate the same deep hash code,
     * regardless of the order in which entries were added.
     */
    @Test
    void testDeepHashCode_Map() {
        Map<String, Integer> mapA = new LinkedHashMap<>();
        mapA.put("one", 1);
        mapA.put("two", 2);
        mapA.put("three", 3);
        Map<String, Integer> mapB = new LinkedHashMap<>();
        mapB.put("three", 3);
        mapB.put("one", 1);
        mapB.put("two", 2);
        int hashA = DeepEquals.deepHashCode(mapA);
        int hashB = DeepEquals.deepHashCode(mapB);
        assertEquals(hashA, hashB, "Two maps with identical key/value pairs should have the same deep hash code");
    }

    /**
     * Verify that the algorithm can handle cyclic object graphs without entering
     * an infinite recursion and that the hash code is deterministic.
     */
    @Test
    void testDeepHashCode_CyclicGraph() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = b;
        // creates a cycle A -> B -> A
        b.next = a;
        // The method should complete and return a consistent hash code.
        int hash1 = DeepEquals.deepHashCode(a);
        // second call to verify determinism
        int hash2 = DeepEquals.deepHashCode(a);
        assertEquals(hash1, hash2, "deepHashCode should be deterministic even for cyclic object graphs");
    }

    /**
     * Null handling – the method should return 0 for a null argument.
     */
    @Test
    void testDeepHashCode_NullHandling() {
        assertEquals(0, DeepEquals.deepHashCode(null), "deepHashCode should return 0 when the input object is null");
    }

    /**
     * Primitive wrappers – verify that the deep hash matches the standard wrapper hashCode.
     */
    @ParameterizedTest
    @MethodSource("primitiveWrapperProvider")
    void testDeepHashCode_PrimitiveWrappers_Values(Object input, int expected) {
        assertEquals(expected, DeepEquals.deepHashCode(input), "Unexpected deep hash for wrapper " + input);
    }

    /**
     * String handling – verify hash codes match String.hashCode().
     */
    @Test
    void testDeepHashCode_StringHandling() {
        assertEquals("abc".hashCode(), DeepEquals.deepHashCode("abc"), "String \"abc\" should produce the same deep hash as String.hashCode()");
        assertEquals("".hashCode(), DeepEquals.deepHashCode(""), "Empty string should produce hash 0");
    }

    /**
     * Primitive arrays – ensure they are hashed like java.util.Arrays.hashCode().
     */
    @Test
    void testDeepHashCode_PrimitiveArrays() {
        int[] intArray = new int[] { 1, 2, 3 };
        byte[] byteArray = new byte[] { 10, 20 };
        int[] emptyIntArray = new int[] {};
        assertEquals(Arrays.hashCode(intArray), DeepEquals.deepHashCode(intArray), "int[] {1,2,3} hash mismatch");
        assertEquals(Arrays.hashCode(byteArray), DeepEquals.deepHashCode(byteArray), "byte[] {10,20} hash mismatch");
        assertEquals(Arrays.hashCode(emptyIntArray), DeepEquals.deepHashCode(emptyIntArray), "Empty int[] hash mismatch");
    }

    /**
     * Object arrays – verify recursive hashing of nested structures.
     */
    @Test
    void testDeepHashCode_ObjectArrays() {
        Object[] mixed = new Object[] { "a", Integer.valueOf(2), null };
        Object[] nested = new Object[] { new int[] { 4, 5 }, new String[] { "x", "y" } };
        // Expected values are taken from the supplied test data.
        assertEquals(123070, DeepEquals.deepHashCode(mixed), "Object array with mixed elements produced unexpected hash");
        assertEquals(1111910, DeepEquals.deepHashCode(nested), "Nested object array produced unexpected hash");
    }

    /**
     * Collections – List handling (order matters).
     */
    @Test
    void testDeepHashCode_ListCollections() {
        List<Integer> list = Arrays.asList(Integer.valueOf(5), Integer.valueOf(6));
        List<Integer> empty = new ArrayList<>();
        assertEquals(1122, DeepEquals.deepHashCode(list), "List hash does not follow expected 31*prev+element formula");
        assertEquals(1, DeepEquals.deepHashCode(empty), "Empty list should return seed value 1");
    }

    /**
     * Collections – Set handling (order independent, sum of element hashes).
     */
    @Test
    void testDeepHashCode_SetCollections() {
        Set<Integer> set = new HashSet<>(Arrays.asList(Integer.valueOf(1), Integer.valueOf(2)));
        Set<Integer> empty = Collections.emptySet();
        assertEquals(3, DeepEquals.deepHashCode(set), "HashSet hash should be sum of element hashCodes");
        assertEquals(0, DeepEquals.deepHashCode(empty), "Empty Set hash should be 0");
    }

    /**
     * Maps – verify sum of entry hashCodes (key.hashCode() ^ value.hashCode()).
     */
    @Test
    void testDeepHashCode_Maps() {
        Map<String, Integer> map = Map.of("x", Integer.valueOf(10), "y", Integer.valueOf(20));
        Map<String, Integer> empty = new HashMap<>();
        assertEquals(235, DeepEquals.deepHashCode(map), "Map hash does not match expected sum of entry hashes");
        assertEquals(0, DeepEquals.deepHashCode(empty), "Empty Map hash should be 0");
    }

    /**
     * Custom objects that override hashCode – the overridden value should be used.
     */
    @Test
    void testDeepHashCode_CustomHashCode() {
        CustomHash custom = new CustomHash(12345);
        assertEquals(12345, DeepEquals.deepHashCode(custom), "CustomHash should return its overridden hashCode value");
    }

    /**
     * Cycle detection – method must terminate without throwing.
     */
    @Test
    void testDeepHashCode_CycleDetection() {
        Object cyclic = DeepEqualsTestHelper.cyclicNode();
        assertDoesNotThrow(() -> DeepEquals.deepHashCode(cyclic), "deepHashCode should handle cyclic object graphs without StackOverflowError");
    }

    /**
     * Unsupported type – passing a Thread should result in IllegalArgumentException.
     */
    @Test
    void testDeepHashCode_UnsupportedType() {
        Thread t = new Thread();
        assertThrows(IllegalArgumentException.class, () -> DeepEquals.deepHashCode(t), "Unsupported type should cause IllegalArgumentException");
    }

    /**
     * Concurrent modification – if the collection is changed during traversal, CME should propagate.
     */
    @Test
    void testDeepHashCode_ConcurrentModification() {
        List<Integer> list = new ArrayList<Integer>() {

            {
                add(1);
                add(2);
                add(3);
                Iterator<Integer> it = iterator();
                // advance iterator
                it.next();
                // modify the list while iterator exists
                it.remove();
            }
        };
        assertThrows(ConcurrentModificationException.class, () -> DeepEquals.deepHashCode(list), "Modifying a collection during deepHashCode should throw ConcurrentModificationException");
    }

    /**
     * Security exception – simulate a restrictive SecurityManager that denies reflective access.
     */
    @Test
    void testDeepHashCode_SecurityExceptionOnInaccessibleField() {
        // Install a SecurityManager that forbids suppressAccessChecks permission.
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                    throw new SecurityException("Reflective access denied by test SecurityManager");
                }
                // allow everything else
            }
        });
        try {
            InaccessibleFieldHolder holder = new InaccessibleFieldHolder();
            assertThrows(SecurityException.class, () -> DeepEquals.deepHashCode(holder), "Attempting to access a private field should result in SecurityException");
        } finally {
            // Restore original SecurityManager to avoid side‑effects on other tests.
            System.setSecurityManager(original);
        }
    }

    /**
     * Deep recursion – a very deep nested array should cause StackOverflowError if not guarded.
     */
    @Test
    void testDeepHashCode_DeepRecursionStackOverflow() {
        Object deepArray = DeepEqualsTestHelper.deeplyNestedArray(20_000);
        assertThrows(StackOverflowError.class, () -> DeepEquals.deepHashCode(deepArray), "Extremely deep nesting should trigger StackOverflowError");
    }
}
