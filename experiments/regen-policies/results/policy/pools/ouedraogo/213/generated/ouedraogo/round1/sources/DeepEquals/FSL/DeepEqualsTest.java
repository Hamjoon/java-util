package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive JUnit‑4 test suite for {@link DeepEquals}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic value equality (including numbers, strings, booleans, dates, etc.)</li>
 *   <li>Array handling (dimension, component type, length, element)</li>
 *   <li>Ordered and unordered collections</li>
 *   <li>Map handling (size, missing key, value mismatch)</li>
 *   <li>Custom {@code equals()} / {@code hashCode()} detection and the {@code IGNORE_CUSTOM_EQUALS} option</li>
 *   <li>String‑to‑number matching via {@code ALLOW_STRINGS_TO_MATCH_NUMBERS}</li>
 *   <li>Floating‑point tolerance</li>
 *   <li>Circular reference detection</li>
 *   <li>Deep hash‑code generation and its relationship to deep equality</li>
 *   <li>Public helper methods {@code hasCustomEquals} and {@code hasCustomHashCode}</li>
 * </ul>
 *
 * All tests use only the public API of {@link DeepEquals} and standard JDK classes.
 */
public class DeepEqualsTest {

    // -------------------------------------------------------------------------
    // Helper classes used in several test cases
    // -------------------------------------------------------------------------

    /** Simple POJO with a custom {@code equals()} implementation. */
    private static class Person {
        String name;
        int age;

        Person(String n, int a) { name = n; age = a; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Person)) return false;
            Person p = (Person) o;
            // Custom logic: only compare name, ignore age
            return Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    /** POJO that does *not* override {@code equals()} or {@code hashCode()}. */
    private static class SimpleBean {
        String id;
        List<Integer> numbers;

        SimpleBean(String id, List<Integer> numbers) {
            this.id = id;
            this.numbers = numbers;
        }
    }

    /** POJO that deliberately creates a circular reference. */
    private static class Node {
        String value;
        Node next;

        Node(String v) { value = v; }
    }

    // -------------------------------------------------------------------------
    // Basic value tests
    // -------------------------------------------------------------------------

    @Test
    public void testSimpleValues() {
        // Numbers – exact match
        assertTrue(DeepEquals.deepEquals(42, 42));
        assertTrue(DeepEquals.deepEquals(3.141592653589793, 3.141592653589793));

        // Numbers – floating‑point tolerance (within epsilon)
        double a = 0.1 + 0.2;               // 0.30000000000000004
        double b = 0.3;
        assertTrue(DeepEquals.deepEquals(a, b));

        // Different numbers
        assertFalse(DeepEquals.deepEquals(10, 11));

        // Strings
        assertTrue(DeepEquals.deepEquals("hello", "hello"));
        assertFalse(DeepEquals.deepEquals("hello", "world"));

        // Booleans and atomic booleans
        assertTrue(DeepEquals.deepEquals(true, true));
        assertTrue(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(true)));
        assertFalse(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(false)));

        // Dates – same instant
        Date d1 = new Date(1_600_000_000_000L);
        Date d2 = new Date(1_600_000_000_000L);
        assertTrue(DeepEquals.deepEquals(d1, d2));

        // URI / URL
        assertTrue(DeepEquals.deepEquals(URI.create("http://example.com"), URI.create("http://example.com")));
        assertFalse(DeepEquals.deepEquals(URI.create("http://example.com"), URI.create("http://example.org")));
    }

    // -------------------------------------------------------------------------
    // Array handling
    // -------------------------------------------------------------------------

    @Test
    public void testArrayDimensionMismatch() {
        int[][] a = new int[][]{{1,2},{3,4}};
        int[] b = new int[]{1,2,3,4};
        Map<String,Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, opts));
        assertTrue(((String)opts.get(DeepEquals.DIFF)).contains("array dimensionality mismatch"));
    }

    @Test
    public void testArrayComponentTypeMismatch() {
        int[] ints = new int[]{1,2,3};
        long[] longs = new long[]{1L,2L,3L};
        Map<String,Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(ints, longs, opts));
        assertTrue(((String)opts.get(DeepEquals.DIFF)).contains("array component type mismatch"));
    }

    @Test
    public void testArrayLengthMismatch() {
        String[] a = new String[]{"a","b"};
        String[] b = new String[]{"a","b","c"};
        Map<String,Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, opts));
        assertTrue(((String)opts.get(DeepEquals.DIFF)).contains("array length mismatch"));
    }

    @Test
    public void testArrayElementMismatch() {
        Double[] a = new Double[]{1.0, 2.0, 3.0};
        Double[] b = new Double[]{1.0, 2.5, 3.0};
        Map<String,Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertTrue(diff.contains("array element mismatch"));
        assertTrue(diff.contains("Expected: 2.0"));
        assertTrue(diff.contains("Found: 2.5"));
    }

    // -------------------------------------------------------------------------
    // Collection handling
    // -------------------------------------------------------------------------

    @Test
    public void testOrderedCollectionSizeMismatch() {
        List<String> l1 = Arrays.asList("x","y");
        List<String> l2 = Collections.singletonList("x");
        Map<String,Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(l1, l2, opts));
        assertTrue(((String)opts.get(DeepEquals.DIFF)).contains("collection size mismatch"));
    }

    @Test
    public void testOrderedCollectionElementMismatch() {
        List<Integer> l1 = Arrays.asList(1,2,3);
        List<Integer> l2 = Arrays.asList(1,9,3);
        Map<String,Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(l1, l2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertTrue(diff.contains("collection element mismatch"));
        assertTrue(diff.contains("Expected: 2"));
        assertTrue(diff.contains("Found: 9"));
    }

    @Test
    public void testUnorderedCollectionMissingElement() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a","b","c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("a","b"));
        Map<String,Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(s1, s2, opts));
        assertTrue(((String)opts.get(DeepEquals.DIFF)).contains("missing collection element"));
    }

    // -------------------------------------------------------------------------
    // Map handling
    // -------------------------------------------------------------------------

    @Test
    public void testMapSizeMismatch() {
        Map<String,Integer> m1 = new HashMap<>();
        m1.put("one",1);
        m1.put("two",2);
        Map<String,Integer> m2 = new HashMap<>();
        m2.put("one",1);
        Map<String,Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m1, m2, opts));
        assertTrue(((String)opts.get(DeepEquals.DIFF)).contains("map size mismatch"));
    }

    @Test
    public void testMapMissingKey() {
        Map<String,Integer> m1 = new HashMap<>();
        m1.put("a",10);
        m1.put("b",20);
        Map<String,Integer> m2 = new HashMap<>();
        m2.put("a",10);
        Map<String,Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m1, m2, opts));
        assertTrue(((String)opts.get(DeepEquals.DIFF)).contains("missing map key"));
    }

    @Test
    public void testMapValueMismatch() {
        Map<String,Integer> m1 = new HashMap<>();
        m1.put("key", 100);
        Map<String,Integer> m2 = new HashMap<>();
        m2.put("key", 101);
        Map<String,Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m1, m2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertTrue(diff.contains("map value mismatch"));
        assertTrue(diff.contains("Expected: 100"));
        assertTrue(diff.contains("Found: 101"));
    }

    // -------------------------------------------------------------------------
    // Custom equals handling and IGNORE_CUSTOM_EQUALS option
    // -------------------------------------------------------------------------

    @Test
    public void testCustomEqualsUsedByDefault() {
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 99); // age differs but custom equals ignores it
        assertTrue(DeepEquals.deepEquals(p1, p2));
    }

    @Test
    public void testIgnoreAllCustomEquals() {
        Person p1 = new Person("Bob", 40);
        Person p2 = new Person("Bob", 41); // age differs
        Map<String,Object> opts = new HashMap<>();
        // Empty set → ignore *all* custom equals
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertFalse(DeepEquals.deepEquals(p1, p2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertTrue(diff.contains("field value mismatch"));
        assertTrue(diff.contains(".age"));
    }

    @Test
    public void testIgnoreSpecificCustomEquals() {
        Person p1 = new Person("Carol", 25);
        Person p2 = new Person("Carol", 26);
        Map<String,Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(Person.class));
        assertFalse(DeepEquals.deepEquals(p1, p2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertTrue(diff.contains(".age"));
    }

    // -------------------------------------------------------------------------
    // String‑to‑number matching
    // -------------------------------------------------------------------------

    @Test
    public void testStringNumberMatchingDisabled() {
        Map<String,Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals("10", 10, opts));
    }

    @Test
    public void testStringNumberMatchingEnabled() {
        Map<String,Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("10", 10, opts));
        assertTrue(DeepEquals.deepEquals(42, "42", opts));

        // Mismatch when numbers differ
        assertFalse(DeepEquals.deepEquals("10", 11, opts));
    }

    // -------------------------------------------------------------------------
    // Floating‑point tolerance (nearlyEqual)
    // -------------------------------------------------------------------------

    @Test
    public void testFloatingPointTolerance() {
        double v1 = 0.123456789012345;
        double v2 = 0.123456789012346; // difference 1e-15
        assertTrue(DeepEquals.deepEquals(v1, v2));
    }

    // -------------------------------------------------------------------------
    // Circular reference detection
    // -------------------------------------------------------------------------

    @Test
    public void testCircularReferenceEquality() {
        Node n1 = new Node("root");
        Node n2 = new Node("root");
        n1.next = n1; // self‑reference
        n2.next = n2;
        assertTrue(DeepEquals.deepEquals(n1, n2));
    }

    @Test
    public void testCircularReferenceInequality() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = a;
        b.next = b;
        Map<String,Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertTrue(diff.contains("field value mismatch"));
        assertTrue(diff.contains(".value"));
    }

    // -------------------------------------------------------------------------
    // deepHashCode consistency
    // -------------------------------------------------------------------------

    @Test
    public void testDeepHashCodeConsistencyWithEquality() {
        List<Integer> list1 = Arrays.asList(1,2,3);
        List<Integer> list2 = Arrays.asList(1,2,3);
        assertTrue(DeepEquals.deepEquals(list1, list2));
        assertEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));

        // Different content → hash codes may differ (not required, but likely)
        List<Integer> list3 = Arrays.asList(1,2,4);
        assertFalse(DeepEquals.deepEquals(list1, list3));
        // Ensure we *do not* get an exception when hashing different structures
        DeepEquals.deepHashCode(list3);
    }

    @Test
    public void testDeepHashCodeWithCustomEqualsIgnored() {
        Person p1 = new Person("Dave", 10);
        Person p2 = new Person("Dave", 20);
        // With custom equals ignored, deepEquals reports false, but hash codes must still be calculated
        Map<String,Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertFalse(DeepEquals.deepEquals(p1, p2, opts));
        // Hash codes are based on the custom hashCode implementation (which only uses name)
        assertEquals(p1.hashCode(), p2.hashCode());
        // deepHashCode must incorporate field values when custom equals is ignored
        int deepHash1 = DeepEquals.deepHashCode(p1);
        int deepHash2 = DeepEquals.deepHashCode(p2);
        assertNotEquals(deepHash1, deepHash2);
    }

    // -------------------------------------------------------------------------
    // Public helper methods hasCustomEquals / hasCustomHashCode
    // -------------------------------------------------------------------------

    @Test
    public void testHasCustomEqualsAndHashCode() {
        // Person overrides both
        assertTrue(DeepEquals.hasCustomEquals(Person.class));
        assertTrue(DeepEquals.hasCustomHashCode(Person.class));

        // SimpleBean does not override either
        assertFalse(DeepEquals.hasCustomEquals(SimpleBean.class));
        assertFalse(DeepEquals.hasCustomHashCode(SimpleBean.class));

        // Standard JDK class with overridden equals (e.g., String)
        assertTrue(DeepEquals.hasCustomEquals(String.class));
        assertTrue(DeepEquals.hasCustomHashCode(String.class));
    }

    // -------------------------------------------------------------------------
    // Additional edge cases
    // -------------------------------------------------------------------------

    @Test
    public void testNullHandling() {
        assertTrue(DeepEquals.deepEquals(null, null));
        Map<String,Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(null, "not null", opts));
        assertTrue(((String)opts.get(DeepEquals.DIFF)).contains("value mismatch"));
    }

    @Test
    public void testDifferentRootTypes() {
        Map<String,Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals("string", 123, opts));
        assertTrue(((String)opts.get(DeepEquals.DIFF)).contains("type mismatch"));
    }

    @Test
    public void testComplexObjectEquality() {
        SimpleBean b1 = new SimpleBean("id1", Arrays.asList(1,2,3));
        SimpleBean b2 = new SimpleBean("id1", Arrays.asList(1,2,3));
        assertTrue(DeepEquals.deepEquals(b1, b2));
    }

    @Test
    public void testComplexObjectInequality() {
        SimpleBean b1 = new SimpleBean("id1", Arrays.asList(1,2,3));
        SimpleBean b2 = new SimpleBean("id2", Arrays.asList(1,2,3));
        Map<String,Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(b1, b2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertTrue(diff.contains("field value mismatch"));
        assertTrue(diff.contains(".id"));
    }
}
