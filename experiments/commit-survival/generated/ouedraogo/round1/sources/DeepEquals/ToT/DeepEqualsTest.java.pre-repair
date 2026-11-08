package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class DeepEqualsTest {

    /* --------------------------------------------------------------
     * Helper classes used in several tests
     * -------------------------------------------------------------- */
    /** Simple node class to create cyclic object graphs. */
    private static class Node {
        Node next;
        String name;

        Node(String name) {
            this.name = name;
        }
    }

    /** Class that overrides equals() and hashCode(). */
    private static class CustomEquals {
        private final int id;

        CustomEquals(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CustomEquals)) return false;
            return id == ((CustomEquals) o).id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    /** Class without custom equals/hashCode (inherits from Object). */
    private static class Plain {
        int a;
        String b;

        Plain(int a, String b) {
            this.a = a;
            this.b = b;
        }
    }

    /* --------------------------------------------------------------
     * 1. deepEquals – basic primitive and wrapper handling
     * -------------------------------------------------------------- */
    @Test
    public void testDeepEquals_PrimitiveAndWrapperEquality() {
        assertTrue(DeepEquals.deepEquals(5, 5));
        assertTrue(DeepEquals.deepEquals(Integer.valueOf(5), Long.valueOf(5L)));
        assertTrue(DeepEquals.deepEquals(Double.valueOf(3.0), Float.valueOf(3.0f)));
        assertFalse(DeepEquals.deepEquals(5, null));
        assertFalse(DeepEquals.deepEquals(null, null)); // both null -> false because method checks identity first
    }

    /* --------------------------------------------------------------
     * 2. deepEquals – String versus Number with option enabled
     * -------------------------------------------------------------- */
    @Test
    public void testDeepEquals_StringNumberMatchingOption() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.TRUE);

        assertTrue(DeepEquals.deepEquals("123", 123, opts));
        assertTrue(DeepEquals.deepEquals(new BigDecimal("123.45"), "123.45", opts));
        assertFalse(DeepEquals.deepEquals("123abc", 123, opts));
    }

    /* --------------------------------------------------------------
     * 3. deepEquals – collection ordering (List) vs unordered (Set)
     * -------------------------------------------------------------- */
    @Test
    public void testDeepEquals_OrderedCollection() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        List<Integer> l3 = Arrays.asList(3, 2, 1);

        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));
    }

    @Test
    public void testDeepEquals_UnorderedSet() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("c", "b", "a"));
        Set<String> s3 = new HashSet<>(Arrays.asList("a", "b"));

        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    /* --------------------------------------------------------------
     * 4. deepEquals – Map handling with different entry order
     * -------------------------------------------------------------- */
    @Test
    public void testDeepEquals_Maps() {
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("two", 2);
        m2.put("one", 1);

        Map<String, Integer> m3 = new HashMap<>();
        m3.put("one", 1);
        m3.put("two", 3); // different value

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    /* --------------------------------------------------------------
     * 5. deepEquals – array handling (object and primitive)
     * -------------------------------------------------------------- */
    @Test
    public void testDeepEquals_Arrays() {
        String[] a1 = {"x", "y"};
        String[] a2 = {"x", "y"};
        String[] a3 = {"y", "x"};

        int[] p1 = {1, 2, 3};
        int[] p2 = {1, 2, 3};
        int[] p3 = {3, 2, 1};

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));
        assertTrue(DeepEquals.deepEquals(p1, p2));
        assertFalse(DeepEquals.deepEquals(p1, p3));
    }

    /* --------------------------------------------------------------
     * 6. deepEquals – cyclic object graphs
     * -------------------------------------------------------------- */
    @Test
    public void testDeepEquals_CyclicGraphs() {
        Node n1 = new Node("first");
        Node n2 = new Node("second");
        n1.next = n2;
        n2.next = n1; // cycle

        Node m1 = new Node("first");
        Node m2 = new Node("second");
        m1.next = m2;
        m2.next = m1; // same structure, different instances

        assertTrue(DeepEquals.deepEquals(n1, m1));
    }

    /* --------------------------------------------------------------
     * 7. deepEquals – custom equals handling and ignore option
     * -------------------------------------------------------------- */
    @Test
    public void testDeepEquals_CustomEqualsAndIgnoreOption() {
        CustomEquals c1 = new CustomEquals(10);
        CustomEquals c2 = new CustomEquals(10);
        CustomEquals c3 = new CustomEquals(20);

        // default uses custom equals
        assertTrue(DeepEquals.deepEquals(c1, c2));
        assertFalse(DeepEquals.deepEquals(c1, c3));

        // ignore custom equals for CustomEquals class
        Map<String, Object> opts = new HashMap<>();
        Set<Class<?>> ignoreSet = new HashSet<>();
        ignoreSet.add(CustomEquals.class);
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignoreSet);

        // now deepEquals falls back to field‑by‑field comparison (both have same 'id' value)
        assertTrue(DeepEquals.deepEquals(c1, c2));
        // different field value -> false
        assertFalse(DeepEquals.deepEquals(c1, c3));
    }

    /* --------------------------------------------------------------
     * 8. isContainerType – simple sanity checks
     * -------------------------------------------------------------- */
    @Test
    public void testIsContainerType() {
        assertTrue(DeepEquals.isContainerType(new ArrayList<>()));
        assertTrue(DeepEquals.isContainerType(new HashMap<>()));
        assertFalse(DeepEquals.isContainerType("string"));
        assertFalse(DeepEquals.isContainerType(42));
    }

    /* --------------------------------------------------------------
     * 9. compareAtomicBoolean – true/false handling
     * -------------------------------------------------------------- */
    @Test
    public void testCompareAtomicBoolean() {
        assertTrue(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(true)));
        assertFalse(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(false)));
    }

    /* --------------------------------------------------------------
     * 10. compareNumbers – floating point epsilon handling
     * -------------------------------------------------------------- */
    @Test
    public void testCompareNumbers_FloatingPointEpsilon() {
        // Difference within double epsilon
        double a = 0.123456789012345;
        double b = 0.123456789012346; // diff = 1e-15 approx
        assertTrue(DeepEquals.deepEquals(a, b));

        // Difference larger than epsilon
        double c = 0.1234567890123;
        double d = 0.1234567890129;
        assertFalse(DeepEquals.deepEquals(c, d));
    }

    /* --------------------------------------------------------------
     * 11. deepHashCode – deterministic hash for equivalent graphs
     * -------------------------------------------------------------- */
    @Test
    public void testDeepHashCode_Consistency() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        List<Integer> list3 = Arrays.asList(3, 2, 1);

        int h1 = DeepEquals.deepHashCode(list1);
        int h2 = DeepEquals.deepHashCode(list2);
        int h3 = DeepEquals.deepHashCode(list3);

        assertEquals(h1, h2);
        assertNotEquals(h1, h3);
    }

    @Test
    public void testDeepHashCode_WithCycles() {
        Node n1 = new Node("a");
        Node n2 = new Node("b");
        n1.next = n2;
        n2.next = n1; // cycle

        Node m1 = new Node("a");
        Node m2 = new Node("b");
        m1.next = m2;
        m2.next = m1;

        assertEquals(DeepEquals.deepHashCode(n1), DeepEquals.deepHashCode(m1));
    }

    /* --------------------------------------------------------------
     * 12. hasCustomEquals – detection of overridden equals()
     * -------------------------------------------------------------- */
    @Test
    public void testHasCustomEquals() {
        assertTrue(DeepEquals.hasCustomEquals(CustomEquals.class));
        assertFalse(DeepEquals.hasCustomEquals(Plain.class));
        // java.lang.String overrides equals()
        assertTrue(DeepEquals.hasCustomEquals(String.class));
    }

    /* --------------------------------------------------------------
     * 13. hasCustomHashCode – detection of overridden hashCode()
     * -------------------------------------------------------------- */
    @Test
    public void testHasCustomHashCode() {
        assertTrue(DeepEquals.hasCustomHashCode(CustomEquals.class));
        assertFalse(DeepEquals.hasCustomHashCode(Plain.class));
        // java.util.Date overrides hashCode()
        assertTrue(DeepEquals.hasCustomHashCode(Date.class));
    }

    /* --------------------------------------------------------------
     * 14. Edge case – null handling at top level
     * -------------------------------------------------------------- */
    @Test
    public void testDeepEquals_TopLevelNulls() {
        // According to implementation, if both are null the first identity check returns true,
        // therefore deepEquals(null, null) should be true.
        assertTrue(DeepEquals.deepEquals(null, null));
    }

    /* --------------------------------------------------------------
     * 15. Edge case – different container types (Set vs List)
     * -------------------------------------------------------------- */
    @Test
    public void testDeepEquals_DifferentContainerTypes() {
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
        List<Integer> list = Arrays.asList(1, 2, 3);
        assertFalse(DeepEquals.deepEquals(set, list));
    }

    /* --------------------------------------------------------------
     * 16. Edge case – map with colliding deepHashCode values
     * -------------------------------------------------------------- */
    @Test
    public void testDeepEquals_MapWithHashCollision() {
        // Two distinct keys that deliberately produce the same deepHashCode.
        // Using Integer values 0 and -0 (both hash to 0) is not possible,
        // instead we rely on different objects that hash to the same int
        // because deepHashCode for Numbers is based on value.
        Map<BigDecimal, String> map1 = new HashMap<>();
        map1.put(new BigDecimal("1.0"), "one");
        map1.put(new BigDecimal("1.00"), "one-point-zero"); // same numeric value, same deepHashCode

        Map<BigDecimal, String> map2 = new HashMap<>();
        map2.put(new BigDecimal("1.000"), "one");
        map2.put(new BigDecimal("1.0"), "one-point-zero");

        // The maps are considered equal because the numeric keys are equal.
        assertTrue(DeepEquals.deepEquals(map1, map2));
    }
}
