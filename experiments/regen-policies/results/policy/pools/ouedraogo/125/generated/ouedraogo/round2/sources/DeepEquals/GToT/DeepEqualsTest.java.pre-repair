package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link DeepEquals}.
 *
 * The suite is organised in three logical sections that correspond to the
 * steps described in the exercise:
 *
 * 1. **Public method extraction** – a comment block lists every public method
 *    together with its signature.
 *
 * 2. **Basic functionality tests** – one simple test per public method that
 *    validates the expected “happy‑path” behaviour.
 *
 * 3. **Edge‑case / exception tests** – tests that target the special
 *    situations identified by analysing the source code (nulls, circular
 *    references, custom equals handling, numeric tolerance, string‑to‑number
 *    matching, etc.).
 *
 * All tests are written using only the public API of {@link DeepEquals} and
 * standard JDK classes – no mocking frameworks or external libraries are
 * required.
 */
public class DeepEqualsTest {

    /* -------------------------------------------------------------
     * 1.  PUBLIC METHOD LIST (extracted from DeepEquals source)
     * -------------------------------------------------------------
     *
     * public static boolean deepEquals(Object a, Object b)
     * public static boolean deepEquals(Object a, Object b, Map<String, ?> options)
     *
     * public static boolean hasCustomEquals(Class<?> c)
     * public static boolean hasCustomHashCode(Class<?> c)
     *
     * public static int deepHashCode(Object obj)
     *
     * // The following inner classes / enums are package‑private, therefore
     * // they are *not* part of the public API and are not listed here.
     */

    /* -------------------------------------------------------------
     * 2.  BASIC FUNCTIONALITY TESTS
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsSimplePrimitives() {
        assertTrue(DeepEquals.deepEquals(5, 5));
        assertTrue(DeepEquals.deepEquals("abc", "abc"));
        assertTrue(DeepEquals.deepEquals(true, true));
        assertTrue(DeepEquals.deepEquals(null, null));

        assertFalse(DeepEquals.deepEquals(5, 6));
        assertFalse(DeepEquals.deepEquals("abc", "def"));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
    }

    @Test
    public void testDeepEqualsArrayEquality() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {1, 2, 4};

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    @Test
    public void testDeepEqualsMultiDimensionalArray() {
        int[][] m1 = {{1, 2}, {3, 4}};
        int[][] m2 = {{1, 2}, {3, 4}};
        int[][] m3 = {{1, 2}, {4, 3}};

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    @Test
    public void testDeepEqualsCollections() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        List<String> list3 = Arrays.asList("a", "c", "b"); // order differs

        assertTrue(DeepEquals.deepEquals(list1, list2));
        assertFalse(DeepEquals.deepEquals(list1, list3));

        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 2, 1));
        Set<Integer> set3 = new HashSet<>(Arrays.asList(1, 2));

        assertTrue(DeepEquals.deepEquals(set1, set2));
        assertFalse(DeepEquals.deepEquals(set1, set3));
    }

    @Test
    public void testDeepEqualsMaps() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new HashMap<>();
        m2.put("two", 2);
        m2.put("one", 1);

        Map<String, Integer> m3 = new HashMap<>();
        m3.put("one", 1);
        m3.put("two", 3); // different value

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    @Test
    public void testDeepEqualsObjectFields() {
        Person p1 = new Person("John", 30);
        Person p2 = new Person("John", 30);
        Person p3 = new Person("John", 31);

        assertTrue(DeepEquals.deepEquals(p1, p2));
        assertFalse(DeepEquals.deepEquals(p1, p3));
    }

    @Test
    public void testHasCustomEquals() {
        // java.lang.String overrides equals()
        assertTrue(DeepEquals.hasCustomEquals(String.class));

        // java.lang.Integer overrides equals()
        assertTrue(DeepEquals.hasCustomEquals(Integer.class));

        // A simple POJO without overriding equals()
        assertFalse(DeepEquals.hasCustomEquals(Person.class));
    }

    @Test
    public void testHasCustomHashCode() {
        // java.lang.String overrides hashCode()
        assertTrue(DeepEquals.hasCustomHashCode(String.class));

        // java.lang.Integer overrides hashCode()
        assertTrue(DeepEquals.hasCustomHashCode(Integer.class));

        // POJO without overriding hashCode()
        assertFalse(DeepEquals.hasCustomHashCode(Person.class));
    }

    @Test
    public void testDeepHashCodeConsistency() {
        Person p1 = new Person("Alice", 25);
        Person p2 = new Person("Alice", 25);
        Person p3 = new Person("Alice", 26);

        int h1 = DeepEquals.deepHashCode(p1);
        int h2 = DeepEquals.deepHashCode(p2);
        int h3 = DeepEquals.deepHashCode(p3);

        // Equal objects must have the same deep hash code
        assertEquals(h1, h2);
        // Different objects should (very likely) have different hash codes
        assertNotEquals(h1, h3);
    }

    /* -------------------------------------------------------------
     * 3.  EDGE CASE / EXCEPTION TESTS
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsWithNullsAndDifferentTypes() {
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
        assertFalse(DeepEquals.deepEquals("not null", null));
        assertFalse(DeepEquals.deepEquals(5, "5")); // without options
    }

    @Test
    public void testStringToNumberMatchingEnabled() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        assertTrue(DeepEquals.deepEquals("10", 10, options));
        assertTrue(DeepEquals.deepEquals(10, "10", options));
        assertFalse(DeepEquals.deepEquals("10.5", 10, options));
    }

    @Test
    public void testStringToNumberMatchingDisabled() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);

        assertFalse(DeepEquals.deepEquals("10", 10, options));
    }

    @Test
    public void testFloatingPointTolerance() {
        double a = 0.1 + 0.2; // 0.30000000000000004
        double b = 0.3;

        assertTrue(DeepEquals.deepEquals(a, b)); // uses epsilon tolerance
    }

    @Test
    public void testCircularReferenceDetection() {
        Node n1 = new Node("root");
        Node n2 = new Node("child");
        n1.next = n2;
        n2.next = n1; // circular

        Node m1 = new Node("root");
        Node m2 = new Node("child");
        m1.next = m2;
        m2.next = m1; // circular

        assertTrue(DeepEquals.deepEquals(n1, m1));
    }

    @Test
    public void testIgnoreCustomEqualsOption() {
        // Class with a custom equals that deliberately returns false
        BadEquals be1 = new BadEquals(5);
        BadEquals be2 = new BadEquals(5);

        // Default behaviour – custom equals is used → false
        assertFalse(DeepEquals.deepEquals(be1, be2));

        // Ignore the custom equals for BadEquals class
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(BadEquals.class));
        assertTrue(DeepEquals.deepEquals(be1, be2, options));
    }

    @Test
    public void testDifferenceMessageIsPopulated() {
        Map<String, Object> options = new HashMap<>();
        Person p1 = new Person("Bob", 40);
        Person p2 = new Person("Bob", 41);

        boolean result = DeepEquals.deepEquals(p1, p2, options);
        assertFalse(result);
        assertNotNull(options.get(DeepEquals.DIFF));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("[field value mismatch]"));
        assertTrue(diff.contains(".age"));
        assertTrue(diff.contains("Expected: 40"));
        assertTrue(diff.contains("Found: 41"));
    }

    @Test
    public void testArrayDimensionMismatch() {
        int[] a1 = {1, 2, 3};
        int[][] a2 = {{1, 2, 3}};

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a1, a2, options));
        assertNotNull(options.get(DeepEquals.DIFF));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("array dimensionality mismatch"));
    }

    @Test
    public void testArrayComponentTypeMismatch() {
        int[] ints = {1, 2, 3};
        long[] longs = {1L, 2L, 3L};

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(ints, longs, options));
        assertNotNull(options.get(DeepEquals.DIFF));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("array component type mismatch"));
    }

    @Test
    public void testArrayLengthMismatch() {
        String[] a1 = {"a", "b"};
        String[] a2 = {"a", "b", "c"};

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a1, a2, options));
        assertNotNull(options.get(DeepEquals.DIFF));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("array length mismatch"));
    }

    @Test
    public void testCollectionSizeMismatch() {
        List<Integer> l1 = Arrays.asList(1, 2);
        List<Integer> l2 = Arrays.asList(1, 2, 3);

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(l1, l2, options));
        assertNotNull(options.get(DeepEquals.DIFF));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("collection size mismatch"));
    }

    @Test
    public void testMapSizeMismatch() {
        Map<String, String> m1 = Map.of("a", "1");
        Map<String, String> m2 = Map.of("a", "1", "b", "2");

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m1, m2, options));
        assertNotNull(options.get(DeepEquals.DIFF));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("map size mismatch"));
    }

    @Test
    public void testMapMissingKey() {
        Map<String, Integer> m1 = Map.of("one", 1);
        Map<String, Integer> m2 = Map.of("one", 1, "two", 2);

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m2, m1, options));
        assertNotNull(options.get(DeepEquals.DIFF));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("missing map key"));
    }

    @Test
    public void testMapValueMismatch() {
        Map<String, Integer> m1 = Map.of("one", 1);
        Map<String, Integer> m2 = Map.of("one", 2);

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m1, m2, options));
        assertNotNull(options.get(DeepEquals.DIFF));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("map value mismatch"));
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
    public void testDeepEqualsWithComplexNestedStructure() {
        Person child1 = new Person("Kid", 5);
        Person child2 = new Person("Kid", 5);
        Person parent1 = new Person("Parent", 30, List.of(child1));
        Person parent2 = new Person("Parent", 30, List.of(child2));

        assertTrue(DeepEquals.deepEquals(parent1, parent2));
    }

    @Test
    public void testDeepEqualsWithURIAndURL() throws Exception {
        URI uri1 = new URI("http://example.com");
        URI uri2 = new URI("http://example.com");
        URL url1 = uri1.toURL();
        URL url2 = uri2.toURL();

        assertTrue(DeepEquals.deepEquals(uri1, uri2));
        assertTrue(DeepEquals.deepEquals(url1, url2));
    }

    @Test
    public void testDeepEqualsWithUUID() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.fromString(id1.toString());

        assertTrue(DeepEquals.deepEquals(id1, id2));
    }

    /* -------------------------------------------------------------
     * Helper classes used by the tests
     * ------------------------------------------------------------- */

    /** Simple POJO without custom equals / hashCode */
    static class Person {
        String name;
        int age;
        List<Person> children;

        Person(String name, int age) {
            this(name, age, Collections.emptyList());
        }

        Person(String name, int age, List<Person> children) {
            this.name = name;
            this.age = age;
            this.children = children;
        }
    }

    /** Class that deliberately implements a bad equals() */
    static class BadEquals {
        int value;

        BadEquals(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            // always return false to simulate a “bad” custom equals
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    /** Node class used to test circular reference handling */
    static class Node {
        String id;
        Node next;

        Node(String id) {
            this.id = id;
        }
    }
}
