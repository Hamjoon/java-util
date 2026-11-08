package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JUnit‑4 test suite for {@link DeepEquals}.
 *
 * The tests are organised in three sections:
 * <ol>
 *   <li>Listing of the public API.</li>
 *   <li>Basic sanity tests that verify the normal behaviour of each public method.</li>
 *   <li>Edge‑case / exception tests that exercise the more subtle parts of the algorithm
 *       (null handling, number conversion, custom equals/hashCode, cycles, collections, etc.).</li>
 * </ol>
 *
 * All tests use only the classes, constructors and methods that are present in the
 * source code supplied for {@code DeepEquals}.  No mocking frameworks are used.
 */
public class DeepEqualsTest {

    /* -------------------------------------------------------------
     *  1. PUBLIC API OF DeepEquals (extracted from the source file)
     * -------------------------------------------------------------
     *
     * public static boolean deepEquals(Object a, Object b)
     * public static boolean deepEquals(Object a, Object b, Map<String, ?> options)
     * public static boolean isContainerType(Object o)
     * public static boolean hasCustomEquals(Class<?> c)
     * public static int deepHashCode(Object obj)
     * public static boolean hasCustomHashCode(Class<?> c)
     */

    /* -------------------------------------------------------------
     *  2. BASIC FUNCTIONALITY TESTS
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsSimpleObjects() {
        String s1 = "hello";
        String s2 = new String("hello");
        assertTrue(DeepEquals.deepEquals(s1, s2));
    }

    @Test
    public void testDeepEqualsNulls() {
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
        assertFalse(DeepEquals.deepEquals("not null", null));
    }

    @Test
    public void testDeepEqualsNumberCrossTypes() {
        Integer i = 42;
        Long l = 42L;
        assertTrue(DeepEquals.deepEquals(i, l));

        Double d = 42.0;
        assertTrue(DeepEquals.deepEquals(i, d));
    }

    @Test
    public void testDeepEqualsFloatingPointEpsilon() {
        Float f = 0.123456f;
        Double d = 0.123456d;
        // The difference is below the epsilon used for Float‑vs‑Double comparison.
        assertTrue(DeepEquals.deepEquals(f, d));
    }

    @Test
    public void testDeepEqualsAtomicBoolean() {
        AtomicBoolean a1 = new AtomicBoolean(true);
        AtomicBoolean a2 = new AtomicBoolean(true);
        AtomicBoolean a3 = new AtomicBoolean(false);
        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    @Test
    public void testIsContainerType() {
        assertTrue(DeepEquals.isContainerType(new ArrayList<>()));
        assertTrue(DeepEquals.isContainerType(new HashMap<>()));
        assertFalse(DeepEquals.isContainerType("not a container"));
        assertFalse(DeepEquals.isContainerType(123));
    }

    @Test
    public void testHasCustomEqualsAndHashCode() {
        assertTrue(DeepEquals.hasCustomEquals(CustomEquals.class));
        assertFalse(DeepEquals.hasCustomEquals(NoCustomEquals.class));

        assertTrue(DeepEquals.hasCustomHashCode(CustomHash.class));
        assertFalse(DeepEquals.hasCustomHashCode(NoCustomHash.class));
    }

    @Test
    public void testDeepHashCodeDeterministic() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        assertEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));

        // Different order -> different hash (lists are ordered)
        List<Integer> list3 = Arrays.asList(3, 2, 1);
        assertNotEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list3));
    }

    /* -------------------------------------------------------------
     *  3. EDGE‑CASE / EXCEPTION TESTS
     * ------------------------------------------------------------- */

    @Test
    public void testStringNumberConversionEnabled() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        assertTrue(DeepEquals.deepEquals("123", 123, options));
        assertTrue(DeepEquals.deepEquals(456, "456", options));
    }

    @Test
    public void testStringNumberConversionDisabled() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);

        assertFalse(DeepEquals.deepEquals("123", 123, options));
        assertFalse(DeepEquals.deepEquals(456, "456", options));
    }

    @Test
    public void testIgnoreCustomEqualsOption() {
        Map<String, Object> options = new HashMap<>();
        Set<Class<?>> ignore = new HashSet<>();
        ignore.add(CustomEquals.class);
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignore);

        CustomEquals a = new CustomEquals(5);
        CustomEquals b = new CustomEquals(5); // same logical value but equals() is overridden to compare by identity

        // With ignore option, deepEquals falls back to field‑by‑field comparison and succeeds
        assertTrue(DeepEquals.deepEquals(a, b, options));

        // Without the ignore option, the overridden equals() (which checks identity) makes them unequal
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testUnorderedSetEquality() {
        Set<String> s1 = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("c", "b", "a"));
        assertTrue(DeepEquals.deepEquals(s1, s2));
    }

    @Test
    public void testOrderedListInequality() {
        List<String> l1 = Arrays.asList("a", "b", "c");
        List<String> l2 = Arrays.asList("c", "b", "a");
        assertFalse(DeepEquals.deepEquals(l1, l2));
    }

    @Test
    public void testMapEquality() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("two", 2);
        m2.put("one", 1);

        assertTrue(DeepEquals.deepEquals(m1, m2));
    }

    @Test
    public void testArrayEquality() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        assertTrue(DeepEquals.deepEquals(a1, a2));

        int[] a3 = {3, 2, 1};
        assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    @Test
    public void testObjectGraphWithCycle() {
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        n1.next = n2;
        n2.next = n1; // cycle

        Node m1 = new Node(1);
        Node m2 = new Node(2);
        m1.next = m2;
        m2.next = m1; // identical structure, different instances

        assertTrue(DeepEquals.deepEquals(n1, m1));
    }

    @Test
    public void testDeepEqualsWithDifferentClasses() {
        assertFalse(DeepEquals.deepEquals("string", 123));
    }

    @Test
    public void testDeepEqualsWithCustomHashCodeClass() {
        CustomHash ch1 = new CustomHash(10);
        CustomHash ch2 = new CustomHash(10);
        // deepEquals uses field comparison because the class does not override equals()
        assertTrue(DeepEquals.deepEquals(ch1, ch2));
        // deepHashCode should use the overridden hashCode()
        assertEquals(ch1.hashCode(), DeepEquals.deepHashCode(ch1));
    }

    /* -------------------------------------------------------------
     *  Helper classes used in the tests
     * ------------------------------------------------------------- */

    /** A class that overrides {@code equals(Object)} but compares by identity. */
    private static class CustomEquals {
        private final int value;

        CustomEquals(int v) { this.value = v; }

        @Override
        public boolean equals(Object o) {
            // Identity‑based equality (different from field comparison)
            return this == o;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    /** A class without a custom equals method (inherits Object.equals). */
    private static class NoCustomEquals {
        private final int value;

        NoCustomEquals(int v) { this.value = v; }
    }

    /** A class that overrides {@code hashCode()} but not {@code equals()}. */
    private static class CustomHash {
        private final int id;

        CustomHash(int id) { this.id = id; }

        @Override
        public int hashCode() {
            // deterministic custom hash
            return id * 31;
        }

        // No custom equals – default identity semantics
    }

    /** A class without a custom hashCode method. */
    private static class NoCustomHash {
        private final int id;

        NoCustomHash(int id) { this.id = id; }
    }

    /** Simple node class to create cyclic object graphs. */
    private static class Node {
        int value;
        Node next;

        Node(int v) { this.value = v; }
    }
}
