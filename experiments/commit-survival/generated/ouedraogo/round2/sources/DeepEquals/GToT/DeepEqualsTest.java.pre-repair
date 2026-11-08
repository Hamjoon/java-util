package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Comprehensive JUnit‑4 test suite for {@link DeepEquals}.
 * Covers basic functionality, edge‑cases, options handling and
 * exception scenarios as required.
 */
public class DeepEqualsTest {

    /* ----------------------------------------------------------------------
     * Helper classes used in the tests
     * ---------------------------------------------------------------------- */

    /** Simple POJO without a custom equals/hashCode implementation. */
    public static class SimplePojo {
        int intValue;
        String text;
        SimplePojo child; // for nested / cyclic tests

        public SimplePojo(int intValue, String text) {
            this.intValue = intValue;
            this.text = text;
        }
    }

    /** POJO that overrides equals (and hashCode) in a non‑standard way. */
    public static class CustomEquals {
        int id;

        public CustomEquals(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            // deliberately return true for any non‑null object of the same class
            return o instanceof CustomEquals;
        }

        @Override
        public int hashCode() {
            return 42;
        }
    }

    /** Simple linked node to test cyclic graphs. */
    public static class Node {
        int value;
        Node next;

        public Node(int value) {
            this.value = value;
        }
    }

    /* ----------------------------------------------------------------------
     * 1. Basic public method tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsPrimitivesAndWrappers() {
        assertTrue(DeepEquals.deepEquals(5, 5L));                // int vs long
        assertTrue(DeepEquals.deepEquals(3.14f, 3.14d));        // float vs double
        assertTrue(DeepEquals.deepEquals('a', 'a'));           // char equality
        assertFalse(DeepEquals.deepEquals(5, 6L));
    }

    @Test
    public void testDeepEqualsStringsAndNulls() {
        assertTrue(DeepEquals.deepEquals("hello", "hello"));
        assertFalse(DeepEquals.deepEquals("hello", "world"));
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
    }

    @Test
    public void testDeepEqualsCollectionsOrdered() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        List<Integer> l3 = Arrays.asList(3, 2, 1);
        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));
    }

    @Test
    public void testDeepEqualsUnorderedSet() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("c", "b", "a"));
        Set<String> s3 = new HashSet<>(Arrays.asList("a", "b"));
        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    @Test
    public void testDeepEqualsMap() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new HashMap<>();
        m2.put("two", 2);
        m2.put("one", 1);

        Map<String, Integer> m3 = new HashMap<>();
        m3.put("one", 1);
        m3.put("two", 3);

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    @Test
    public void testDeepEqualsPojoFieldByField() {
        SimplePojo a = new SimplePojo(10, "test");
        SimplePojo b = new SimplePojo(10, "test");
        SimplePojo c = new SimplePojo(20, "test");

        assertTrue(DeepEquals.deepEquals(a, b));
        assertFalse(DeepEquals.deepEquals(a, c));
    }

    @Test
    public void testDeepEqualsCyclicGraph() {
        Node a1 = new Node(1);
        Node a2 = new Node(2);
        a1.next = a2;
        a2.next = a1; // cycle

        Node b1 = new Node(1);
        Node b2 = new Node(2);
        b1.next = b2;
        b2.next = b1; // same shape

        assertTrue(DeepEquals.deepEquals(a1, b1));
    }

    @Test
    public void testDeepEqualsArrayComparison() {
        int[] arr1 = {1, 2, 3};
        int[] arr2 = {1, 2, 3};
        int[] arr3 = {3, 2, 1};
        assertTrue(DeepEquals.deepEquals(arr1, arr2));
        assertFalse(DeepEquals.deepEquals(arr1, arr3));
    }

    @Test
    public void testDeepEqualsAtomicBoolean() {
        AtomicBoolean ab1 = new AtomicBoolean(true);
        AtomicBoolean ab2 = new AtomicBoolean(true);
        AtomicBoolean ab3 = new AtomicBoolean(false);
        assertTrue(DeepEquals.deepEquals(ab1, ab2));
        assertFalse(DeepEquals.deepEquals(ab1, ab3));
    }

    @Test
    public void testIsContainerType() {
        assertTrue(DeepEquals.isContainerType(new ArrayList<>()));
        assertTrue(DeepEquals.isContainerType(new HashMap<>()));
        assertFalse(DeepEquals.isContainerType("not a container"));
        assertFalse(DeepEquals.isContainerType(42));
    }

    @Test
    public void testHasCustomEquals() {
        assertFalse(DeepEquals.hasCustomEquals(SimplePojo.class));
        assertTrue(DeepEquals.hasCustomEquals(CustomEquals.class));
        assertTrue(DeepEquals.hasCustomEquals(String.class)); // String overrides equals
    }

    @Test
    public void testHasCustomHashCode() {
        assertFalse(DeepEquals.hasCustomHashCode(SimplePojo.class));
        assertTrue(DeepEquals.hasCustomHashCode(CustomEquals.class));
        assertTrue(DeepEquals.hasCustomHashCode(String.class));
    }

    @Test
    public void testDeepHashCodeConsistency() {
        SimplePojo a = new SimplePojo(5, "x");
        SimplePojo b = new SimplePojo(5, "x");
        assertEquals(DeepEquals.deepHashCode(a), DeepEquals.deepHashCode(b));

        SimplePojo c = new SimplePojo(6, "x");
        assertNotEquals(DeepEquals.deepHashCode(a), DeepEquals.deepHashCode(c));
    }

    /* ----------------------------------------------------------------------
     * 2. Edge‑case tests (options, floating point epsilon, empty containers, etc.)
     * ---------------------------------------------------------------------- */

    @Test
    public void testStringNumberMatchingOptionEnabled() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("123", 123, opts));
        assertTrue(DeepEquals.deepEquals(45.6, "45.6", opts));
    }

    @Test
    public void testStringNumberMatchingOptionDisabled() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);
        assertFalse(DeepEquals.deepEquals("123", 123, opts));
    }

    @Test
    public void testIgnoreCustomEqualsOption() {
        CustomEquals a = new CustomEquals(1);
        CustomEquals b = new CustomEquals(2); // custom equals would say true

        // Without ignore option => deepEquals uses custom equals => true
        assertTrue(DeepEquals.deepEquals(a, b));

        // With ignore option => field‑by‑field => false
        Map<String, Object> opts = new HashMap<>();
        Set<Class<?>> ignoreSet = new HashSet<>();
        ignoreSet.add(CustomEquals.class);
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignoreSet);
        assertFalse(DeepEquals.deepEquals(a, b, opts));
    }

    @Test
    public void testFloatingPointEpsilon() {
        Double d1 = 0.1 + 0.2; // 0.30000000000000004
        Double d2 = 0.3;
        assertTrue(DeepEquals.deepEquals(d1, d2)); // within double epsilon

        Float f1 = 0.1f + 0.2f; // approx 0.30000001
        Float f2 = 0.3f;
        assertTrue(DeepEquals.deepEquals(f1, f2)); // within float epsilon
    }

    @Test
    public void testEmptyCollectionsAndArrays() {
        List<Integer> emptyList1 = Collections.emptyList();
        List<Integer> emptyList2 = new ArrayList<>();
        assertTrue(DeepEquals.deepEquals(emptyList1, emptyList2));

        int[] emptyArr1 = new int[0];
        int[] emptyArr2 = new int[0];
        assertTrue(DeepEquals.deepEquals(emptyArr1, emptyArr2));
    }

    @Test
    public void testDeepHashCodeFloatDoubleNormalization() {
        Float f1 = 1.000001f;
        Float f2 = 1.000002f; // difference within epsilon for hashFloat
        assertEquals(DeepEquals.deepHashCode(f1), DeepEquals.deepHashCode(f2));

        Double d1 = 1.0000000001;
        Double d2 = 1.0000000002; // difference within epsilon for hashDouble
        assertEquals(DeepEquals.deepHashCode(d1), DeepEquals.deepHashCode(d2));
    }

    /* ----------------------------------------------------------------------
     * 3. Exception / illegal‑argument scenarios
     * ---------------------------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testIgnoreCustomEqualsOptionWrongType() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, "not a set"); // illegal type
        DeepEquals.deepEquals(new CustomEquals(1), new CustomEquals(2), opts);
    }

    @Test
    public void testOptionsMapWithNullValues() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, null);
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, null);
        // Should behave like default options – no exception thrown
        assertTrue(DeepEquals.deepEquals("same", "same", opts));
    }

    /* ----------------------------------------------------------------------
     * 4. Constants visibility sanity check (not functional but ensures
     *    they are accessible as public fields)
     * ---------------------------------------------------------------------- */

    @Test
    public void testPublicConstantValues() {
        assertEquals("ignoreCustomEquals", DeepEquals.IGNORE_CUSTOM_EQUALS);
        assertEquals("stringsCanMatchNumbers", DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS);
    }
}
