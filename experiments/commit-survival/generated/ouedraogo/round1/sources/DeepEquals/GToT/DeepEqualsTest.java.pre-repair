package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class DeepEqualsTest {

    /* ----------------------------------------------------------------------
     * Helper classes used in several tests
     * ---------------------------------------------------------------------- */
    private static class NoEquals {
        int x;

        NoEquals(int x) { this.x = x; }
    }

    private static class CustomEquals {
        int x;

        CustomEquals(int x) { this.x = x; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CustomEquals)) return false;
            return this.x == ((CustomEquals) o).x;
        }
    }

    private static class CustomHash {
        int x;

        CustomHash(int x) { this.x = x; }

        @Override
        public int hashCode() {
            return x;
        }

        @Override
        public boolean equals(Object o) {
            // intentional identity‑only equality – deepEquals should fall back to field comparison
            return this == o;
        }
    }

    private static class Node {
        Node next;
        int value;

        Node(int value) { this.value = value; }
    }

    /* ----------------------------------------------------------------------
     * Public method: deepEquals(Object, Object)
     * ---------------------------------------------------------------------- */
    @Test
    public void testDeepEqualsSimplePrimitives() {
        assertTrue(DeepEquals.deepEquals(5, 5));
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, 5));
        assertFalse(DeepEquals.deepEquals(5, null));
    }

    @Test
    public void testDeepEqualsNumberTypes() {
        // Integer vs Long – same numeric value should be equal
        assertTrue(DeepEquals.deepEquals(Integer.valueOf(42), Long.valueOf(42L)));
        // Float vs Double – within epsilon should be equal
        assertTrue(DeepEquals.deepEquals(Float.valueOf(0.123456f), Double.valueOf(0.123456001));
    }

    @Test
    public void testDeepEqualsStringNumberOption() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("123", Integer.valueOf(123), opts));
        assertTrue(DeepEquals.deepEquals(Long.valueOf(999L), "999", opts));

        // without the option the comparison must fail
        assertFalse(DeepEquals.deepEquals("123", Integer.valueOf(123)));
    }

    @Test
    public void testDeepEqualsAtomicBoolean() {
        assertTrue(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(true)));
        assertFalse(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(false)));
    }

    @Test
    public void testDeepEqualsArrays() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {1, 2, 4};
        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));

        String[] s1 = {"a", "b"};
        String[] s2 = {"a", "b"};
        assertTrue(DeepEquals.deepEquals(s1, s2));
    }

    @Test
    public void testDeepEqualsCollectionsOrdered() {
        List<String> l1 = Arrays.asList("x", "y", "z");
        List<String> l2 = Arrays.asList("x", "y", "z");
        List<String> l3 = Arrays.asList("z", "y", "x");
        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));
    }

    @Test
    public void testDeepEqualsUnorderedSet() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 2, 1));
        Set<Integer> s3 = new HashSet<>(Arrays.asList(1, 2, 4));
        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    @Test
    public void testDeepEqualsMap() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);

        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("b", 2);
        m2.put("a", 1);

        Map<String, Integer> m3 = new HashMap<>();
        m3.put("a", 1);
        m3.put("b", 3);

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    @Test
    public void testDeepEqualsCycles() {
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        n1.next = n2;
        n2.next = n1; // cycle

        Node m1 = new Node(1);
        Node m2 = new Node(2);
        m1.next = m2;
        m2.next = m1; // identical structure

        assertTrue(DeepEquals.deepEquals(n1, m1));
    }

    @Test
    public void testDeepEqualsCustomEqualsAndIgnoreOption() {
        CustomEquals c1 = new CustomEquals(10);
        CustomEquals c2 = new CustomEquals(20);

        // default – uses overridden equals, should be false
        assertFalse(DeepEquals.deepEquals(c1, c2));

        // ignore custom equals via options
        Set<Class<?>> ignore = new HashSet<>();
        ignore.add(CustomEquals.class);
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignore);
        assertTrue(DeepEquals.deepEquals(c1, c2, opts));
    }

    @Test
    public void testDeepEqualsCustomHashCode() {
        CustomHash h1 = new CustomHash(100);
        CustomHash h2 = new CustomHash(100);
        // Objects are not the same instance, but have same custom hashCode.
        // deepEquals should compare fields (none) and deem them equal.
        assertTrue(DeepEquals.deepEquals(h1, h2));
    }

    /* ----------------------------------------------------------------------
     * Public method: deepEquals(Object, Object, Map<String,?>)
     * ---------------------------------------------------------------------- */
    @Test
    public void testDeepEqualsWithEmptyOptionsMap() {
        Map<String, Object> empty = new HashMap<>();
        assertTrue(DeepEquals.deepEquals("test", "test", empty));
        assertFalse(DeepEquals.deepEquals("test", "different", empty));
    }

    /* ----------------------------------------------------------------------
     * Public method: isContainerType(Object)
     * ---------------------------------------------------------------------- */
    @Test
    public void testIsContainerType() {
        assertTrue(DeepEquals.isContainerType(new ArrayList<>()));
        assertTrue(DeepEquals.isContainerType(new HashMap<>()));
        assertFalse(DeepEquals.isContainerType("string"));
        assertFalse(DeepEquals.isContainerType(123));
        assertFalse(DeepEquals.isContainerType(null));
    }

    /* ----------------------------------------------------------------------
     * Public method: hasCustomEquals(Class<?>)
     * ---------------------------------------------------------------------- */
    @Test
    public void testHasCustomEquals() {
        assertTrue(DeepEquals.hasCustomEquals(CustomEquals.class));
        assertFalse(DeepEquals.hasCustomEquals(NoEquals.class));
        assertFalse(DeepEquals.hasCustomEquals(String.class)); // String overrides equals but it is final in java.lang – still true
        // Ensure caching does not affect result
        assertTrue(DeepEquals.hasCustomEquals(CustomEquals.class));
    }

    /* ----------------------------------------------------------------------
     * Public method: deepHashCode(Object)
     * ---------------------------------------------------------------------- */
    @Test
    public void testDeepHashCodePrimitivesAndArrays() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {3, 2, 1};

        assertEquals(DeepEquals.deepHashCode(a1), DeepEquals.deepHashCode(a2));
        assertNotEquals(DeepEquals.deepHashCode(a1), DeepEquals.deepHashCode(a3));
    }

    @Test
    public void testDeepHashCodeCollectionsOrderMatters() {
        List<String> l1 = Arrays.asList("a", "b", "c");
        List<String> l2 = Arrays.asList("c", "b", "a");
        assertNotEquals(DeepEquals.deepHashCode(l1), DeepEquals.deepHashCode(l2));
    }

    @Test
    public void testDeepHashCodeUnorderedSet() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 2, 1));
        // Order does not affect hash of a Set because internal algorithm is order‑independent
        assertEquals(DeepEquals.deepHashCode(s1), DeepEquals.deepHashCode(s2));
    }

    @Test
    public void testDeepHashCodeMap() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("x", 1);
        m1.put("y", 2);

        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("y", 2);
        m2.put("x", 1);

        assertEquals(DeepEquals.deepHashCode(m1), DeepEquals.deepHashCode(m2));
    }

    @Test
    public void testDeepHashCodeCycles() {
        Node n1 = new Node(5);
        Node n2 = new Node(6);
        n1.next = n2;
        n2.next = n1; // cycle

        Node m1 = new Node(5);
        Node m2 = new Node(6);
        m1.next = m2;
        m2.next = m1;

        assertEquals(DeepEquals.deepHashCode(n1), DeepEquals.deepHashCode(m1));
    }

    @Test
    public void testDeepHashCodeFloatingPointNormalization() {
        // Values that differ only beyond the epsilon should hash to the same value
        double d1 = 0.12345678901;
        double d2 = 0.12345678902; // difference < 1e-15
        assertEquals(DeepEquals.deepHashCode(d1), DeepEquals.deepHashCode(d2));

        float f1 = 0.12345f;
        float f2 = 0.12346f; // difference < 1e-6
        assertEquals(DeepEquals.deepHashCode(f1), DeepEquals.deepHashCode(f2));
    }

    /* ----------------------------------------------------------------------
     * Public method: hasCustomHashCode(Class<?>)
     * ---------------------------------------------------------------------- */
    @Test
    public void testHasCustomHashCode() {
        assertTrue(DeepEquals.hasCustomHashCode(CustomHash.class));
        assertFalse(DeepEquals.hasCustomHashCode(NoEquals.class));
        // String overrides hashCode – should be true
        assertTrue(DeepEquals.hasCustomHashCode(String.class));
    }

    /* ----------------------------------------------------------------------
     * Edge‑case: deepEquals with mismatched container types
     * ---------------------------------------------------------------------- */
    @Test
    public void testDeepEqualsMismatchedContainerTypes() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
        assertFalse(DeepEquals.deepEquals(list, set));
    }

    /* ----------------------------------------------------------------------
     * Edge‑case: deepEquals with different classes but same toString()
     * ---------------------------------------------------------------------- */
    @Test
    public void testDeepEqualsDifferentClassesSameToString() {
        Integer i = 42;
        Long l = 42L;
        assertTrue(DeepEquals.deepEquals(i, l)); // numeric conversion handles this
    }

    /* ----------------------------------------------------------------------
     * Edge‑case: deepEquals where one side is a primitive wrapper and the other is a String,
     *          with option disabled – should be false.
     * ---------------------------------------------------------------------- */
    @Test
    public void testDeepEqualsStringNumberOptionDisabled() {
        assertFalse(DeepEquals.deepEquals("100", Integer.valueOf(100)));
    }

    /* ----------------------------------------------------------------------
     * Edge‑case: deepEquals with null inside collections
     * ---------------------------------------------------------------------- */
    @Test
    public void testDeepEqualsCollectionsContainingNulls() {
        List<String> l1 = Arrays.asList("a", null, "c");
        List<String> l2 = Arrays.asList("a", null, "c");
        List<String> l3 = Arrays.asList("a", "b", "c");
        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));
    }

    /* ----------------------------------------------------------------------
     * Edge‑case: deepEquals with empty collections and maps
     * ---------------------------------------------------------------------- */
    @Test
    public void testDeepEqualsEmptyStructures() {
        assertTrue(DeepEquals.deepEquals(new ArrayList<>(), new LinkedList<>()));
        assertTrue(DeepEquals.deepEquals(new HashSet<>(), new HashSet<>()));
        assertTrue(DeepEquals.deepEquals(new HashMap<>(), new Hashtable<>()));
    }

    /* ----------------------------------------------------------------------
     * Edge‑case: deepEquals with differing array component types
     * ---------------------------------------------------------------------- */
    @Test
    public void testDeepEqualsArrayComponentTypeMismatch() {
        int[] ints = {1, 2, 3};
        long[] longs = {1L, 2L, 3L};
        assertFalse(DeepEquals.deepEquals(ints, longs));
    }
}
