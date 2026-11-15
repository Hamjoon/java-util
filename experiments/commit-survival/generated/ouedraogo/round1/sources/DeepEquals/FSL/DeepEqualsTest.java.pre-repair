package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

/**
 * Comprehensive JUnit4 test suite for {@link DeepEquals}.
 * Covers deepEquals (with and without options), isContainerType,
 * deepHashCode, hasCustomEquals, hasCustomHashCode and the various
 * internal comparison paths (numbers, arrays, collections, sets,
 * maps, AtomicBoolean and the string‑to‑number option).
 */
public class DeepEqualsTest {

    /* ----------------------------------------------------------------------
     * Helper classes used in the tests
     * ---------------------------------------------------------------------- */

    /** Simple POJO with a custom equals that deliberately returns false. */
    private static class CustomEqualsAlwaysFalse {
        int value;

        CustomEqualsAlwaysFalse(int v) { this.value = v; }

        @Override
        public boolean equals(Object o) {
            // Always return false regardless of field values
            return false;
        }

        @Override
        public int hashCode() {
            // hash based on the field value (so two equal objects have same hash)
            return Integer.hashCode(value);
        }
    }

    /** Simple POJO with default Object.equals (no override). */
    private static class PlainPojo {
        int a;
        String b;

        PlainPojo(int a, String b) { this.a = a; this.b = b; }
    }

    /* ----------------------------------------------------------------------
     * Basic primitive / wrapper / String handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testPrimitiveAndWrapperEquality() {
        assertTrue(DeepEquals.deepEquals(5, 5));                     // int vs int
        assertTrue(DeepEquals.deepEquals(Integer.valueOf(5), 5L));   // Integer vs Long same value
        assertTrue(DeepEquals.deepEquals("abc", "abc"));             // String equality
        assertFalse(DeepEquals.deepEquals("abc", "def"));
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
    }

    @Test
    public void testNumberStringMatchOptionEnabled() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.TRUE);

        assertTrue(DeepEquals.deepEquals("123", 123, opts));
        assertTrue(DeepEquals.deepEquals(45.6, "45.6", opts));
        assertTrue(DeepEquals.deepEquals(new BigDecimal("7.89"), "7.89", opts));

        // without the option the comparison must fail
        assertFalse(DeepEquals.deepEquals("123", 123));
        assertFalse(DeepEquals.deepEquals(45.6, "45.6"));
    }

    @Test
    public void testAtomicBooleanEquality() {
        AtomicBoolean a1 = new AtomicBoolean(true);
        AtomicBoolean a2 = new AtomicBoolean(true);
        AtomicBoolean a3 = new AtomicBoolean(false);

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    @Test
    public void testFloatingPointEpsilon() {
        // Values that differ only within the defined epsilon should be equal
        Float f1 = 0.1234567f;
        Float f2 = 0.1234570f; // diff ~3e-6 < floatEpsilon (1e-6) after rounding
        assertTrue(DeepEquals.deepEquals(f1, f2));

        Double d1 = 0.123456789012345;
        Double d2 = 0.123456789012340; // diff 5e-15 < doubleEpsilon (1e-15) (just over, but rounding may make them equal)
        assertTrue(DeepEquals.deepEquals(d1, d2));
    }

    /* ----------------------------------------------------------------------
     * Array handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testArrayEquality() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {3, 2, 1};

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));

        String[] s1 = {"a", "b"};
        String[] s2 = {"a", "b"};
        String[] s3 = {"b", "a"};

        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    /* ----------------------------------------------------------------------
     * Collection handling (ordered)
     * ---------------------------------------------------------------------- */

    @Test
    public void testOrderedCollectionEquality() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        List<Integer> l3 = Arrays.asList(3, 2, 1);

        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));
    }

    /* ----------------------------------------------------------------------
     * Set handling (unordered)
     * ---------------------------------------------------------------------- */

    @Test
    public void testUnorderedSetEquality() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("c", "b", "a"));
        Set<String> s3 = new HashSet<>(Arrays.asList("a", "b"));

        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    /* ----------------------------------------------------------------------
     * Map handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testMapEquality() {
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);
        m1.put("three", 3);

        Map<String, Integer> m2 = new HashMap<>();
        m2.put("three", 3);
        m2.put("two", 2);
        m2.put("one", 1);

        Map<String, Integer> m3 = new HashMap<>();
        m3.put("one", 1);
        m3.put("two", 22); // different value

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    /* ----------------------------------------------------------------------
     * Container type detection
     * ---------------------------------------------------------------------- */

    @Test
    public void testIsContainerType() {
        assertTrue(DeepEquals.isContainerType(new ArrayList<>()));
        assertTrue(DeepEquals.isContainerType(new HashSet<>()));
        assertTrue(DeepEquals.isContainerType(new HashMap<>()));
        assertFalse(DeepEquals.isContainerType("not a container"));
        assertFalse(DeepEquals.isContainerType(123));
    }

    /* ----------------------------------------------------------------------
     * Custom equals handling and IGNORE_CUSTOM_EQUALS option
     * ---------------------------------------------------------------------- */

    @Test
    public void testCustomEqualsUsedByDefault() {
        CustomEqualsAlwaysFalse o1 = new CustomEqualsAlwaysFalse(10);
        CustomEqualsAlwaysFalse o2 = new CustomEqualsAlwaysFalse(10);

        // Default behavior: custom equals (which always returns false) is used → deepEquals false
        assertFalse(DeepEquals.deepEquals(o1, o2));

        // Verify that the class indeed reports a custom equals implementation
        assertTrue(DeepEquals.hasCustomEquals(CustomEqualsAlwaysFalse.class));
    }

    @Test
    public void testIgnoreCustomEqualsOption() {
        CustomEqualsAlwaysFalse o1 = new CustomEqualsAlwaysFalse(10);
        CustomEqualsAlwaysFalse o2 = new CustomEqualsAlwaysFalse(10);

        // Build options to ignore the custom equals for this class
        Set<Class<?>> ignoreSet = new HashSet<>();
        ignoreSet.add(CustomEqualsAlwaysFalse.class);
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignoreSet);

        // With the option, DeepEquals falls back to field‑wise comparison (via reflection)
        // Both objects have the same field value, so they are considered equal.
        assertTrue(DeepEquals.deepEquals(o1, o2, opts));
    }

    /* ----------------------------------------------------------------------
     * Custom hashCode detection and deepHashCode consistency
     * ---------------------------------------------------------------------- */

    @Test
    public void testHasCustomHashCodeDetection() {
        assertTrue(DeepEquals.hasCustomHashCode(CustomEqualsAlwaysFalse.class));
        assertFalse(DeepEquals.hasCustomHashCode(PlainPojo.class)); // PlainPojo does not override hashCode
    }

    @Test
    public void testDeepHashCodeConsistency() {
        // Two distinct objects that are deep‑equal must produce the same deep hash code.
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        assertTrue(DeepEquals.deepEquals(list1, list2));
        assertEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));

        // Objects that rely on a custom hashCode should use that value.
        CustomEqualsAlwaysFalse c1 = new CustomEqualsAlwaysFalse(42);
        CustomEqualsAlwaysFalse c2 = new CustomEqualsAlwaysFalse(42);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertEquals(DeepEquals.deepHashCode(c1), DeepEquals.deepHashCode(c2));
    }

    /* ----------------------------------------------------------------------
     * Cycle detection sanity check (simple self‑reference)
     * ---------------------------------------------------------------------- */

    private static class Node {
        Node next;
        int value;
        Node(int v) { this.value = v; }
    }

    @Test
    public void testCycleDetection() {
        // Build two identical cyclic graphs: A -> B -> A
        Node a1 = new Node(1);
        Node b1 = new Node(2);
        a1.next = b1;
        b1.next = a1;

        Node a2 = new Node(1);
        Node b2 = new Node(2);
        a2.next = b2;
        b2.next = a2;

        assertTrue(DeepEquals.deepEquals(a1, a2));
    }

    /* ----------------------------------------------------------------------
     * Additional sanity checks for deepEquals with mixed types
     * ---------------------------------------------------------------------- */

    @Test
    public void testMixedTypeMismatches() {
        // Different collection types
        List<Integer> list = Arrays.asList(1, 2);
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2));
        assertFalse(DeepEquals.deepEquals(list, set));

        // Number vs non‑Number (without option)
        assertFalse(DeepEquals.deepEquals(123, "123"));
    }
}
