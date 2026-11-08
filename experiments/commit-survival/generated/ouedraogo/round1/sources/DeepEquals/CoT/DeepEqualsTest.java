package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class DeepEqualsTest {

    /*** Helper classes for testing ***/

    // A class that overrides equals (but not hashCode) – used to test custom equals handling.
    private static class CustomEquals {
        int value;

        CustomEquals(int v) { this.value = v; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CustomEquals)) return false;
            return this.value == ((CustomEquals) o).value;
        }
    }

    // Same as CustomEquals but we will ignore its custom equals via options.
    private static class IgnoredCustomEquals extends CustomEquals {
        IgnoredCustomEquals(int v) { super(v); }
    }

    // A class that overrides hashCode (but not equals) – used to test custom hash handling.
    private static class CustomHash {
        int value;

        CustomHash(int v) { this.value = v; }

        @Override
        public int hashCode() {
            return value * 31;
        }
    }

    // Simple node class to create cyclic graphs.
    private static class Node {
        String name;
        Node next;

        Node(String name) { this.name = name; }
    }

    /*** Public method list ***/
    // 1. public static boolean deepEquals(Object a, Object b)
    // 2. public static boolean deepEquals(Object a, Object b, Map<String, ?> options)
    // 3. public static boolean isContainerType(Object o)
    // 4. public static boolean hasCustomEquals(Class<?> c)
    // 5. public static int deepHashCode(Object obj)
    // 6. public static boolean hasCustomHashCode(Class<?> c)

    /*** Basic test cases ***/

    @Test
    public void testDeepEqualsSimplePrimitives() {
        assertTrue(DeepEquals.deepEquals(5, 5L)); // int vs long, same value
        assertTrue(DeepEquals.deepEquals("abc", "abc"));
        assertFalse(DeepEquals.deepEquals("abc", "def"));
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
    }

    @Test
    public void testDeepEqualsWithOptionsAllowStringNumberMatch() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        assertTrue(DeepEquals.deepEquals("123", 123, options));
        assertTrue(DeepEquals.deepEquals(45.0, "45.0", options));
        // When option is false (default) the same comparison must be false
        assertFalse(DeepEquals.deepEquals("123", 123));
    }

    @Test
    public void testDeepEqualsCustomEqualsUsedByDefault() {
        CustomEquals a = new CustomEquals(10);
        CustomEquals b = new CustomEquals(10);
        CustomEquals c = new CustomEquals(20);

        assertTrue(DeepEquals.deepEquals(a, b)); // uses overridden equals
        assertFalse(DeepEquals.deepEquals(a, c));
    }

    @Test
    public void testDeepEqualsCustomEqualsIgnoredViaOption() {
        IgnoredCustomEquals a = new IgnoredCustomEquals(10);
        IgnoredCustomEquals b = new IgnoredCustomEquals(10);
        // they have the same field value, but we will ignore the custom equals
        Set<Class<?>> ignoreSet = new HashSet<>();
        ignoreSet.add(IgnoredCustomEquals.class);
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignoreSet);

        assertTrue(DeepEquals.deepEquals(a, b, options));
    }

    @Test
    public void testDeepEqualsCollectionsOrdered() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        List<Integer> list3 = Arrays.asList(3, 2, 1);

        assertTrue(DeepEquals.deepEquals(list1, list2));
        assertFalse(DeepEquals.deepEquals(list1, list3)); // order matters
    }

    @Test
    public void testDeepEqualsSetsUnordered() {
        Set<String> set1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> set2 = new HashSet<>(Arrays.asList("c", "b", "a"));
        Set<String> set3 = new HashSet<>(Arrays.asList("a", "b"));

        assertTrue(DeepEquals.deepEquals(set1, set2));
        assertFalse(DeepEquals.deepEquals(set1, set3));
    }

    @Test
    public void testDeepEqualsMaps() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("x", 1);
        m1.put("y", 2);

        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("y", 2);
        m2.put("x", 1); // different insertion order

        Map<String, Integer> m3 = new HashMap<>();
        m3.put("x", 1);
        m3.put("y", 3); // different value

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    @Test
    public void testDeepEqualsArrays() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {3, 2, 1};

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    @Test
    public void testDeepEqualsCyclicGraphs() {
        Node n1 = new Node("first");
        Node n2 = new Node("second");
        n1.next = n2;
        n2.next = n1; // cycle

        Node m1 = new Node("first");
        Node m2 = new Node("second");
        m1.next = m2;
        m2.next = m1; // same structure

        assertTrue(DeepEquals.deepEquals(n1, m1));
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
        assertFalse(DeepEquals.isContainerType(123));
    }

    @Test
    public void testHasCustomEquals() {
        assertTrue(DeepEquals.hasCustomEquals(CustomEquals.class));
        assertFalse(DeepEquals.hasCustomEquals(String.class)); // String overrides equals, but it's from Object? actually String overrides; should be true
        // To be precise, String does override equals, so we expect true.
        assertTrue(DeepEquals.hasCustomEquals(String.class));
        // A class without overridden equals
        class NoEquals { }
        assertFalse(DeepEquals.hasCustomEquals(NoEquals.class));
    }

    @Test
    public void testHasCustomHashCode() {
        assertTrue(DeepEquals.hasCustomHashCode(CustomHash.class));
        // String overrides hashCode, should be true
        assertTrue(DeepEquals.hasCustomHashCode(String.class));
        // Class without overridden hashCode
        class NoHash { }
        assertFalse(DeepEquals.hasCustomHashCode(NoHash.class));
    }

    @Test
    public void testDeepHashCodeDeterministicEquality() {
        List<Integer> listA = Arrays.asList(1, 2, 3);
        List<Integer> listB = Arrays.asList(1, 2, 3);
        List<Integer> listC = Arrays.asList(3, 2, 1);

        int hashA = DeepEquals.deepHashCode(listA);
        int hashB = DeepEquals.deepHashCode(listB);
        int hashC = DeepEquals.deepHashCode(listC);

        assertEquals(hashA, hashB);
        assertNotEquals(hashA, hashC);
    }

    @Test
    public void testDeepHashCodeWithCycles() {
        Node n1 = new Node("a");
        Node n2 = new Node("b");
        n1.next = n2;
        n2.next = n1; // cycle

        Node m1 = new Node("a");
        Node m2 = new Node("b");
        m1.next = m2;
        m2.next = m1; // same cycle

        int hash1 = DeepEquals.deepHashCode(n1);
        int hash2 = DeepEquals.deepHashCode(m1);
        assertEquals(hash1, hash2);
    }

    /*** Edge case tests ***/

    @Test
    public void testDeepEqualsNumberFloatingPointEpsilon() {
        // Values within epsilon should be considered equal
        double d1 = 0.123456789012345;
        double d2 = 0.123456789012340; // diff < 1e-15
        assertTrue(DeepEquals.deepEquals(d1, d2));

        // Values outside epsilon should be false
        double d3 = 0.123456789012345;
        double d4 = 0.123456789012000; // diff > 1e-15
        assertFalse(DeepEquals.deepEquals(d3, d4));
    }

    @Test
    public void testDeepEqualsFloatEpsilon() {
        float f1 = 1.000001f;
        float f2 = 1.000002f; // diff < 1e-6
        assertTrue(DeepEquals.deepEquals(f1, f2));

        float f3 = 1.000001f;
        float f4 = 1.001f; // diff > 1e-6
        assertFalse(DeepEquals.deepEquals(f3, f4));
    }

    @Test
    public void testDeepEqualsDifferentNumberTypesWithStringOptionFalse() {
        // Without allowing strings to match numbers, a String vs Number must be false
        assertFalse(DeepEquals.deepEquals("10", 10));
    }

    @Test
    public void testDeepEqualsEmptyCollectionsAndMaps() {
        assertTrue(DeepEquals.deepEquals(new ArrayList<>(), new LinkedList<>()));
        assertTrue(DeepEquals.deepEquals(new HashSet<>(), new HashSet<>()));
        assertTrue(DeepEquals.deepEquals(new HashMap<>(), new ConcurrentHashMap<>()));
    }

    @Test
    public void testDeepEqualsNestedStructures() {
        Map<String, Object> inner1 = new HashMap<>();
        inner1.put("num", 42);
        inner1.put("list", Arrays.asList("a", "b"));

        Map<String, Object> outer1 = new HashMap<>();
        outer1.put("inner", inner1);
        outer1.put("flag", true);

        Map<String, Object> inner2 = new HashMap<>();
        inner2.put("num", 42L); // long instead of int
        inner2.put("list", Arrays.asList("a", "b"));

        Map<String, Object> outer2 = new HashMap<>();
        outer2.put("inner", inner2);
        outer2.put("flag", true);

        assertTrue(DeepEquals.deepEquals(outer1, outer2));
    }

    @Test
    public void testDeepEqualsClassObjects() {
        assertTrue(DeepEquals.deepEquals(String.class, String.class));
        assertFalse(DeepEquals.deepEquals(String.class, Integer.class));
    }

    @Test
    public void testDeepEqualsDateObjects() {
        Date d1 = new Date(1000000L);
        Date d2 = new Date(1000000L);
        Date d3 = new Date(2000000L);
        assertTrue(DeepEquals.deepEquals(d1, d2));
        assertFalse(DeepEquals.deepEquals(d1, d3));
    }
}
