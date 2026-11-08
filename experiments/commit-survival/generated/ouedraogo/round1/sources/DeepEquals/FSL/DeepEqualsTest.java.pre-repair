package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

/**
 * Unit tests for {@link DeepEquals}.  The tests cover:
 * <ul>
 *   <li>primitive and wrapper number comparisons (including cross‑type)</li>
 *   <li>floating‑point epsilon handling</li>
 *   <li>String ↔ Number matching when the option is enabled</li>
 *   <li>custom {@code equals()} handling and the {@code IGNORE_CUSTOM_EQUALS} option</li>
 *   <li>cycle detection in object graphs</li>
 *   <li>array, ordered collection, unordered collection and map comparisons</li>
 *   <li>AtomicBoolean handling</li>
 *   <li>deep hash code generation and consistency</li>
 *   <li>utility methods {@code hasCustomEquals}, {@code hasCustomHashCode} and {@code isContainerType}</li>
 * </ul>
 */
public class DeepEqualsTest {

    /* --------------------------------------------------------------
     * Helper classes used in the tests
     * -------------------------------------------------------------- */

    /** Simple POJO with a custom equals that deliberately returns false. */
    private static class BadEquals {
        int value;

        BadEquals(int v) { this.value = v; }

        @Override
        public boolean equals(Object o) {
            // Wrong implementation – always false
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    /** Simple POJO with a proper equals based on its fields. */
    private static class GoodEquals {
        int value;

        GoodEquals(int v) { this.value = v; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GoodEquals)) return false;
            GoodEquals that = (GoodEquals) o;
            return this.value == that.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    /** Node class to create cyclic object graphs. */
    private static class Node {
        String name;
        Node next;

        Node(String name) { this.name = name; }
    }

    /* --------------------------------------------------------------
     * Tests for primitive / wrapper number handling
     * -------------------------------------------------------------- */

    @Test
    public void testNumberCrossTypeEquality() {
        assertTrue(DeepEquals.deepEquals(5, 5L));                 // int vs long
        assertTrue(DeepEquals.deepEquals(10, new BigDecimal("10"))); // int vs BigDecimal
        assertFalse(DeepEquals.deepEquals(5, 6L));               // different values
    }

    @Test
    public void testFloatingPointEpsilon() {
        // 0.1 + 0.2 is not exactly 0.3 in binary, but DeepEquals should treat them as equal
        double a = 0.1 + 0.2;
        double b = 0.3;
        assertTrue(DeepEquals.deepEquals(a, b));

        // Float epsilon test – values within 1e‑6 should be equal
        float f1 = 1.000001f;
        float f2 = 1.000002f;
        assertTrue(DeepEquals.deepEquals(f1, f2));
    }

    /* --------------------------------------------------------------
     * Tests for String ↔ Number matching option
     * -------------------------------------------------------------- */

    @Test
    public void testStringNumberMatchingDisabled() {
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals("5", 5, options));
    }

    @Test
    public void testStringNumberMatchingEnabled() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("5", 5, options));
        assertTrue(DeepEquals.deepEquals(10, "10", options));

        // non‑numeric string should still fail
        assertFalse(DeepEquals.deepEquals("abc", 1, options));
    }

    /* --------------------------------------------------------------
     * Tests for custom equals handling and IGNORE_CUSTOM_EQUALS option
     * -------------------------------------------------------------- */

    @Test
    public void testCustomEqualsUsedByDefault() {
        BadEquals a = new BadEquals(1);
        BadEquals b = new BadEquals(1);
        // BadEquals.equals() always returns false → deepEquals must return false
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testIgnoreCustomEqualsOption() {
        BadEquals a = new BadEquals(1);
        BadEquals b = new BadEquals(1);
        Set<Class<?>> ignore = new HashSet<>();
        ignore.add(BadEquals.class);
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignore);
        // With the class ignored, DeepEquals falls back to field‑by‑field comparison,
        // which sees the identical 'value' field → true.
        assertTrue(DeepEquals.deepEquals(a, b, options));
    }

    /* --------------------------------------------------------------
     * Tests for cycle detection
     * -------------------------------------------------------------- */

    @Test
    public void testCyclicObjectGraphsAreEqual() {
        Node a1 = new Node("A");
        Node a2 = new Node("B");
        Node a3 = new Node("C");
        a1.next = a2;
        a2.next = a3;
        a3.next = a1; // cycle

        Node b1 = new Node("A");
        Node b2 = new Node("B");
        Node b3 = new Node("C");
        b1.next = b2;
        b2.next = b3;
        b3.next = b1; // same shape, different instances

        assertTrue(DeepEquals.deepEquals(a1, b1));
    }

    @Test
    public void testCyclicObjectGraphsDifferentValues() {
        Node a1 = new Node("A");
        Node a2 = new Node("B");
        a1.next = a2;
        a2.next = a1; // cycle

        Node b1 = new Node("A");
        Node b2 = new Node("X"); // different name
        b1.next = b2;
        b2.next = b1; // cycle

        assertFalse(DeepEquals.deepEquals(a1, b1));
    }

    /* --------------------------------------------------------------
     * Tests for array handling
     * -------------------------------------------------------------- */

    @Test
    public void testArrayEquality() {
        int[] arr1 = {1, 2, 3};
        int[] arr2 = {1, 2, 3};
        int[] arr3 = {3, 2, 1};

        assertTrue(DeepEquals.deepEquals(arr1, arr2));
        assertFalse(DeepEquals.deepEquals(arr1, arr3));
    }

    @Test
    public void testMultiDimensionalArrayEquality() {
        String[][] a1 = { {"a", "b"}, {"c"} };
        String[][] a2 = { {"a", "b"}, {"c"} };
        assertTrue(DeepEquals.deepEquals(a1, a2));
    }

    /* --------------------------------------------------------------
     * Tests for ordered collections (List)
     * -------------------------------------------------------------- */

    @Test
    public void testOrderedCollectionEquality() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        List<Integer> l3 = Arrays.asList(3, 2, 1);

        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));
    }

    /* --------------------------------------------------------------
     * Tests for unordered collections (Set)
     * -------------------------------------------------------------- */

    @Test
    public void testUnorderedSetEquality() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("c", "b", "a"));
        Set<String> s3 = new HashSet<>(Arrays.asList("a", "b"));

        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    /* --------------------------------------------------------------
     * Tests for map handling
     * -------------------------------------------------------------- */

    @Test
    public void testMapEquality() {
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("x", 1);
        m1.put("y", 2);
        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("y", 2);
        m2.put("x", 1);
        Map<String, Integer> m3 = new LinkedHashMap<>();
        m3.put("x", 1);
        m3.put("y", 3);

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    /* --------------------------------------------------------------
     * Tests for AtomicBoolean handling
     * -------------------------------------------------------------- */

    @Test
    public void testAtomicBooleanEquality() {
        AtomicBoolean ab1 = new AtomicBoolean(true);
        AtomicBoolean ab2 = new AtomicBoolean(true);
        AtomicBoolean ab3 = new AtomicBoolean(false);

        assertTrue(DeepEquals.deepEquals(ab1, ab2));
        assertFalse(DeepEquals.deepEquals(ab1, ab3));
    }

    /* --------------------------------------------------------------
     * Tests for deepHashCode consistency
     * -------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeConsistencyForEqualObjects() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        assertEquals(DeepEquals.deepHashCode(l1), DeepEquals.deepHashCode(l2));
    }

    @Test
    public void testDeepHashCodeDifferentForDifferentObjects() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(3, 2, 1);
        assertNotEquals(DeepEquals.deepHashCode(l1), DeepEquals.deepHashCode(l2));
    }

    @Test
    public void testDeepHashCodeWithFloatingPointNormalization() {
        double d1 = 0.1234567890123;
        double d2 = 0.1234567890124; // difference smaller than epsilon
        assertEquals(DeepEquals.deepHashCode(d1), DeepEquals.deepHashCode(d2));

        float f1 = 1.23456f;
        float f2 = 1.23457f; // within float epsilon
        assertEquals(DeepEquals.deepHashCode(f1), DeepEquals.deepHashCode(f2));
    }

    /* --------------------------------------------------------------
     * Tests for utility methods
     * -------------------------------------------------------------- */

    @Test
    public void testHasCustomEquals() {
        // String overrides equals
        assertTrue(DeepEquals.hasCustomEquals(String.class));

        // Integer also overrides equals
        assertTrue(DeepEquals.hasCustomEquals(Integer.class));

        // A class without an override (e.g., Object itself) returns false
        assertFalse(DeepEquals.hasCustomEquals(Object.class));
    }

    @Test
    public void testHasCustomHashCode() {
        // String overrides hashCode
        assertTrue(DeepEquals.hasCustomHashCode(String.class));

        // Integer overrides hashCode
        assertTrue(DeepEquals.hasCustomHashCode(Integer.class));

        // A class without an override (e.g., Object) returns false
        assertFalse(DeepEquals.hasCustomHashCode(Object.class));
    }

    @Test
    public void testIsContainerType() {
        assertTrue(DeepEquals.isContainerType(new ArrayList<>()));
        assertTrue(DeepEquals.isContainerType(new HashMap<>()));
        assertFalse(DeepEquals.isContainerType("not a container"));
        assertFalse(DeepEquals.isContainerType(42));
    }

    /* --------------------------------------------------------------
     * Tests for deepEquals with null handling
     * -------------------------------------------------------------- */

    @Test
    public void testNullHandling() {
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
        assertFalse(DeepEquals.deepEquals("not null", null));
    }

    /* --------------------------------------------------------------
     * Tests for Date and Class objects (they rely on .equals())
     * -------------------------------------------------------------- */

    @Test
    public void testDateEquality() {
        Date d1 = new Date(123456789L);
        Date d2 = new Date(123456789L);
        Date d3 = new Date(987654321L);
        assertTrue(DeepEquals.deepEquals(d1, d2));
        assertFalse(DeepEquals.deepEquals(d1, d3));
    }

    @Test
    public void testClassEquality() {
        assertTrue(DeepEquals.deepEquals(String.class, String.class));
        assertFalse(DeepEquals.deepEquals(String.class, Integer.class));
    }
}
