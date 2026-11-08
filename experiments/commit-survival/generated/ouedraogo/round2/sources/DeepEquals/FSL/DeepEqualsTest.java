package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Comprehensive JUnit‑4 test suite for {@link DeepEquals}.
 * All public behaviours are exercised, including the various
 * option flags, container handling, number comparison, custom
 * equals/hashCode detection and cycle handling.
 */
public class DeepEqualsTest {

    /* --------------------------------------------------------------------- */
    /* Helper classes used in several tests                                 */
    /* --------------------------------------------------------------------- */

    /** Simple POJO with a custom {@code equals} that deliberately fails. */
    static class CustomEquals {
        int value;

        CustomEquals(int v) { this.value = v; }

        @Override
        public boolean equals(Object o) {
            // Always return false – we want DeepEquals to be able to ignore it.
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    /** Simple POJO with a custom {@code hashCode} implementation. */
    static class CustomHash {
        int value;

        CustomHash(int v) { this.value = v; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CustomHash)) return false;
            return value == ((CustomHash) o).value;
        }

        @Override
        public int hashCode() {
            // non‑standard hash (just the value shifted)
            return value << 2;
        }
    }

    /** Node class that can form a cycle (next -> previous). */
    static class Node {
        String name;
        Node next;

        Node(String name) { this.name = name; }
    }

    /* --------------------------------------------------------------------- */
    /* Basic null handling                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsNulls() {
        Assert.assertTrue(DeepEquals.deepEquals(null, null));
        Assert.assertFalse(DeepEquals.deepEquals(null, "not null"));
        Assert.assertFalse(DeepEquals.deepEquals("not null", null));
    }

    /* --------------------------------------------------------------------- */
    /* Number comparison (including cross‑type)                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsNumbersCrossType() {
        // int vs long
        Assert.assertTrue(DeepEquals.deepEquals(42, 42L));
        // short vs Byte
        Assert.assertTrue(DeepEquals.deepEquals((short) 7, (byte) 7));
        // double vs Float (within epsilon)
        Assert.assertTrue(DeepEquals.deepEquals(0.123456789d, 0.1234567f));
        // double vs Float (outside epsilon)
        Assert.assertFalse(DeepEquals.deepEquals(0.1234d, 0.12f));
    }

    @Test
    public void testDeepEqualsStringNumberOption() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        // String "5" vs Integer 5 – should be equal when option enabled
        Assert.assertTrue(DeepEquals.deepEquals("5", 5, opts));

        // String "5.0" vs Double 5.0 – also equal
        Assert.assertTrue(DeepEquals.deepEquals("5.0", 5.0d, opts));

        // When the option is *not* set, they are not equal
        Assert.assertFalse(DeepEquals.deepEquals("5", 5));
    }

    @Test
    public void testDeepEqualsAtomicBoolean() {
        AtomicBoolean a1 = new AtomicBoolean(true);
        AtomicBoolean a2 = new AtomicBoolean(true);
        AtomicBoolean a3 = new AtomicBoolean(false);

        Assert.assertTrue(DeepEquals.deepEquals(a1, a2));
        Assert.assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    /* --------------------------------------------------------------------- */
    /* Array handling (primitive and wrapper)                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsArrays() {
        int[] intArray = {1, 2, 3};
        Integer[] integerArray = {1, 2, 3};
        int[] intArrayDiff = {1, 2, 4};

        // Primitive vs wrapper – deepEquals should succeed
        Assert.assertTrue(DeepEquals.deepEquals(intArray, integerArray));

        // Different content
        Assert.assertFalse(DeepEquals.deepEquals(intArray, intArrayDiff));

        // Different length
        int[] shortArray = {1, 2};
        Assert.assertFalse(DeepEquals.deepEquals(intArray, shortArray));
    }

    /* --------------------------------------------------------------------- */
    /* Ordered collections (List)                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsOrderedCollections() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = new ArrayList<>(list1);
        List<String> listDiffOrder = Arrays.asList("c", "b", "a");
        List<String> listDiffSize = Arrays.asList("a", "b");

        Assert.assertTrue(DeepEquals.deepEquals(list1, list2));
        Assert.assertFalse(DeepEquals.deepEquals(list1, listDiffOrder));
        Assert.assertFalse(DeepEquals.deepEquals(list1, listDiffSize));
    }

    /* --------------------------------------------------------------------- */
    /* Unordered collections (Set)                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsUnorderedSets() {
        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 2, 1));
        Set<Integer> setDiff = new HashSet<>(Arrays.asList(1, 2, 4));

        Assert.assertTrue(DeepEquals.deepEquals(set1, set2));
        Assert.assertFalse(DeepEquals.deepEquals(set1, setDiff));
    }

    /* --------------------------------------------------------------------- */
    /* Map handling                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsMaps() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("one", 1);
        map1.put("two", 2);

        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("two", 2);
        map2.put("one", 1); // order different – should still be equal

        Map<String, Integer> mapDiffKey = new HashMap<>();
        mapDiffKey.put("one", 1);
        mapDiffKey.put("three", 3);

        Map<String, Integer> mapDiffValue = new HashMap<>();
        mapDiffValue.put("one", 1);
        mapDiffValue.put("two", 22);

        Assert.assertTrue(DeepEquals.deepEquals(map1, map2));
        Assert.assertFalse(DeepEquals.deepEquals(map1, mapDiffKey));
        Assert.assertFalse(DeepEquals.deepEquals(map1, mapDiffValue));
    }

    /* --------------------------------------------------------------------- */
    /* Cycle detection                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsCycles() {
        Node a1 = new Node("A");
        Node b1 = new Node("B");
        a1.next = b1;
        b1.next = a1; // cycle

        Node a2 = new Node("A");
        Node b2 = new Node("B");
        a2.next = b2;
        b2.next = a2; // second cycle, same structure

        Assert.assertTrue(DeepEquals.deepEquals(a1, a2));
    }

    /* --------------------------------------------------------------------- */
    /* Custom equals handling (ignoreCustomEquals option)                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIgnoreCustomEqualsOption() {
        CustomEquals c1 = new CustomEquals(10);
        CustomEquals c2 = new CustomEquals(10);

        // Without ignoring, DeepEquals will invoke the broken equals -> false
        Assert.assertFalse(DeepEquals.deepEquals(c1, c2));

        // With ignoreCustomEquals set to contain CustomEquals, field‑by‑field compare succeeds
        Set<Class<?>> ignoreSet = new HashSet<>();
        ignoreSet.add(CustomEquals.class);
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignoreSet);

        Assert.assertTrue(DeepEquals.deepEquals(c1, c2, opts));
    }

    /* --------------------------------------------------------------------- */
    /* Detection of custom equals / hashCode methods                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testHasCustomEqualsAndHashCodeDetection() {
        // CustomEquals overrides equals, should be detected
        Assert.assertTrue(DeepEquals.hasCustomEquals(CustomEquals.class));

        // CustomHash overrides both equals and hashCode
        Assert.assertTrue(DeepEquals.hasCustomEquals(CustomHash.class));
        Assert.assertTrue(DeepEquals.hasCustomHashCode(CustomHash.class));

        // Plain java.lang.String does NOT have a custom equals (it overrides Object.equals,
        // but DeepEquals treats it as a primitive‑like type, still hasCustomEquals returns true)
        // The implementation checks for any declared equals up the hierarchy, so String returns true.
        Assert.assertTrue(DeepEquals.hasCustomEquals(String.class));

        // java.util.ArrayList overrides equals, should be true
        Assert.assertTrue(DeepEquals.hasCustomEquals(ArrayList.class));

        // A class without overrides
        class NoOverrides { int x; }
        Assert.assertFalse(DeepEquals.hasCustomEquals(NoOverrides.class));
        Assert.assertFalse(DeepEquals.hasCustomHashCode(NoOverrides.class));
    }

    /* --------------------------------------------------------------------- */
    /* isContainerType utility                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsContainerType() {
        List<Integer> list = Arrays.asList(1, 2);
        Set<String> set = new HashSet<>(Arrays.asList("a"));
        Map<String, Integer> map = new HashMap<>();
        String notContainer = "hello";

        Assert.assertTrue(DeepEquals.isContainerType(list));
        Assert.assertTrue(DeepEquals.isContainerType(set));
        Assert.assertTrue(DeepEquals.isContainerType(map));
        Assert.assertFalse(DeepEquals.isContainerType(notContainer));
    }

    /* --------------------------------------------------------------------- */
    /* deepHashCode consistency checks                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeBasicEquality() {
        // Primitive wrappers
        Integer i1 = 42;
        Integer i2 = 42;
        Assert.assertEquals(DeepEquals.deepHashCode(i1), DeepEquals.deepHashCode(i2));

        // Different objects but same content (list)
        List<String> l1 = Arrays.asList("x", "y");
        List<String> l2 = new ArrayList<>(l1);
        Assert.assertEquals(DeepEquals.deepHashCode(l1), DeepEquals.deepHashCode(l2));

        // Sets – order independent
        Set<Integer> s1 = new HashSet<>(Arrays.asList(3, 1, 2));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(2, 3, 1));
        Assert.assertEquals(DeepEquals.deepHashCode(s1), DeepEquals.deepHashCode(s2));

        // Maps – order independent
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("b", 2);
        m2.put("a", 1);
        Assert.assertEquals(DeepEquals.deepHashCode(m1), DeepEquals.deepHashCode(m2));
    }

    @Test
    public void testDeepHashCodeFloatAndDoubleRounding() {
        // Values that differ only beyond the epsilon should produce the same hash
        Float f1 = 1.234567f;
        Float f2 = 1.234568f; // difference < float epsilon (1e-6)
        Assert.assertEquals(DeepEquals.deepHashCode(f1), DeepEquals.deepHashCode(f2));

        // Values that differ beyond epsilon must produce different hashes
        Float f3 = 1.2345f;
        Float f4 = 1.2355f; // difference > epsilon
        Assert.assertNotEquals(DeepEquals.deepHashCode(f3), DeepEquals.deepHashCode(f4));

        // Double rounding
        Double d1 = 12345.678901234;
        Double d2 = 12345.678901235; // within double epsilon (1e-15)
        Assert.assertEquals(DeepEquals.deepHashCode(d1), DeepEquals.deepHashCode(d2));

        Double d3 = 12345.6789;
        Double d4 = 12345.6790; // beyond epsilon
        Assert.assertNotEquals(DeepEquals.deepHashCode(d3), DeepEquals.deepHashCode(d4));
    }

    @Test
    public void testDeepHashCodeWithCustomHash() {
        CustomHash ch1 = new CustomHash(7);
        CustomHash ch2 = new CustomHash(7);
        // Since CustomHash provides its own hashCode, deepHashCode should incorporate it.
        Assert.assertEquals(DeepEquals.deepHashCode(ch1), DeepEquals.deepHashCode(ch2));

        CustomHash ch3 = new CustomHash(8);
        Assert.assertNotEquals(DeepEquals.deepHashCode(ch1), DeepEquals.deepHashCode(ch3));
    }

    @Test
    public void testDeepHashCodeWithCycles() {
        Node n1 = new Node("cycle");
        Node n2 = new Node("cycle");
        n1.next = n1; // self‑cycle
        n2.next = n2;

        // Even with cycles the hash code computation must terminate and be consistent
        Assert.assertEquals(DeepEquals.deepHashCode(n1), DeepEquals.deepHashCode(n2));
    }
}
