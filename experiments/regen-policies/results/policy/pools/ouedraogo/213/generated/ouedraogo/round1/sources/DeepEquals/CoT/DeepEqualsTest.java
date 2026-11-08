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
 * <p>This file is generated automatically according to the specification:
 * <ul>
 *   <li>List all public methods of {@code DeepEquals}.</li>
 *   <li>Provide a basic test for each method.</li>
 *   <li>Identify edge‑case / exception scenarios and test them.</li>
 *   <li>All tests are compiled into a single class {@code DeepEqualsTest}.</li>
 * </ul>
 * </p>
 */
public class DeepEqualsTest {

    /** Simple POJO with a custom {@code equals} and {@code hashCode}. */
    static class CustomEquals {
        int id;
        String name;

        CustomEquals(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CustomEquals)) return false;
            CustomEquals other = (CustomEquals) o;
            return id == other.id && Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

    /** POJO without a custom {@code equals} (relies on {@code Object.equals}). */
    static class NoCustomEquals {
        int value;

        NoCustomEquals(int v) { this.value = v; }
    }

    /** POJO that references itself to create a circular graph. */
    static class Node {
        String label;
        Node next;

        Node(String label) { this.label = label; }
    }

    private Map<String, Object> options;

    @Before
    public void setUp() {
        options = new HashMap<>();
    }

    @After
    public void tearDown() {
        options.clear();
    }

    // ------------------------------------------------------------------------
    //  List of public methods in DeepEquals
    // ------------------------------------------------------------------------
    // 1. public static boolean deepEquals(Object a, Object b)
    // 2. public static boolean deepEquals(Object a, Object b, Map<String, ?> options)
    // 3. public static boolean hasCustomEquals(Class<?> c)
    // 4. public static boolean hasCustomHashCode(Class<?> c)
    // 5. public static int deepHashCode(Object obj)

    // ------------------------------------------------------------------------
    //  Basic test cases for each public method
    // ------------------------------------------------------------------------

    /** 1. deepEquals(Object, Object) – simple equal primitives. */
    @Test
    public void testDeepEqualsPrimitivesEqual() {
        assertTrue(DeepEquals.deepEquals(42, 42));
        assertTrue(DeepEquals.deepEquals("foo", "foo"));
        assertTrue(DeepEquals.deepEquals(true, true));
        assertTrue(DeepEquals.deepEquals(null, null));
    }

    /** 1. deepEquals(Object, Object) – simple unequal primitives. */
    @Test
    public void testDeepEqualsPrimitivesUnequal() {
        assertFalse(DeepEquals.deepEquals(42, 43));
        assertFalse(DeepEquals.deepEquals("foo", "bar"));
        assertFalse(DeepEquals.deepEquals(true, false));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
    }

    /** 2. deepEquals(Object, Object, Map) – basic map usage. */
    @Test
    public void testDeepEqualsWithOptionsSimple() {
        Map<String, Object> opt = new HashMap<>();
        assertTrue(DeepEquals.deepEquals("a", "a", opt));
        assertTrue(opt.isEmpty()); // no diff key when equal
    }

    /** 2. deepEquals – verifies diff is populated on mismatch. */
    @Test
    public void testDeepEqualsDiffPopulation() {
        Map<String, Object> opt = new HashMap<>();
        boolean result = DeepEquals.deepEquals(1, 2, opt);
        assertFalse(result);
        assertNotNull(opt.get(DeepEquals.DIFF));
        assertTrue(opt.get(DeepEquals.DIFF) instanceof String);
    }

    /** 2. deepEquals – test IGNORE_CUSTOM_EQUALS option (ignore custom equals). */
    @Test
    public void testIgnoreCustomEqualsOption() {
        CustomEquals a = new CustomEquals(1, "Alice");
        CustomEquals b = new CustomEquals(2, "Bob"); // different fields

        // Without ignoring, deepEquals falls back to field‑by‑field comparison and should be false
        assertFalse(DeepEquals.deepEquals(a, b));

        // Ignore all custom equals (empty set) – should compare fields recursively and be false as well,
        // but we verify that the option is accepted without throwing.
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertFalse(DeepEquals.deepEquals(a, b, options));
    }

    /** 2. deepEquals – test ALLOW_STRINGS_TO_MATCH_NUMBERS option. */
    @Test
    public void testAllowStringNumberMatching() {
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("10", 10, options));
        assertTrue(DeepEquals.deepEquals(3.14, "3.14", options));

        // When disabled (default) the same values are not equal
        assertFalse(DeepEquals.deepEquals("10", 10));
    }

    /** 3. hasCustomEquals – class with custom equals. */
    @Test
    public void testHasCustomEqualsPositive() {
        assertTrue(DeepEquals.hasCustomEquals(CustomEquals.class));
    }

    /** 3. hasCustomEquals – class without custom equals. */
    @Test
    public void testHasCustomEqualsNegative() {
        assertFalse(DeepEquals.hasCustomEquals(NoCustomEquals.class));
        assertFalse(DeepEquals.hasCustomEquals(String.class)); // String overrides equals but is a final JDK class; still counts as custom
    }

    /** 4. hasCustomHashCode – class with custom hashCode. */
    @Test
    public void testHasCustomHashCodePositive() {
        assertTrue(DeepEquals.hasCustomHashCode(CustomEquals.class));
    }

    /** 4. hasCustomHashCode – class without custom hashCode. */
    @Test
    public void testHasCustomHashCodeNegative() {
        assertFalse(DeepEquals.hasCustomHashCode(NoCustomEquals.class));
        assertFalse(DeepEquals.hasCustomHashCode(Integer.class)); // Integer overrides hashCode but is a JDK class; still considered custom
    }

    /** 5. deepHashCode – simple primitives. */
    @Test
    public void testDeepHashCodePrimitives() {
        int h1 = DeepEquals.deepHashCode(123);
        int h2 = DeepEquals.deepHashCode(123);
        assertEquals(h1, h2);
    }

    /** 5. deepHashCode – collections with same content. */
    @Test
    public void testDeepHashCodeCollections() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        assertEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));
    }

    /** 5. deepHashCode – verifies cycle handling does not cause StackOverflow. */
    @Test
    public void testDeepHashCodeCircularReference() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = b;
        b.next = a; // creates cycle

        // Should complete without throwing
        int hash = DeepEquals.deepHashCode(a);
        // Hash is deterministic for the same graph
        int hash2 = DeepEquals.deepHashCode(a);
        assertEquals(hash, hash2);
    }

    // ------------------------------------------------------------------------
    //  Edge‑case / exception tests
    // ------------------------------------------------------------------------

    /** Null vs non‑null should be unequal and produce a diff. */
    @Test
    public void testNullVsNonNull() {
        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(null, "nonNull", opt));
        assertNotNull(opt.get(DeepEquals.DIFF));
    }

    /** Different array dimensions (e.g., int[] vs int[][]) */
    @Test
    public void testArrayDimensionMismatch() {
        int[] oneD = {1, 2, 3};
        int[][] twoD = {{1, 2}, {3, 4}};
        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(oneD, twoD, opt));
        assertTrue(((String) opt.get(DeepEquals.DIFF)).contains("array dimensionality mismatch");
    }

    /** Same dimension but different component types (int[] vs long[]). */
    @Test
    public void testArrayComponentTypeMismatch() {
        int[] ints = {1, 2};
        long[] longs = {1L, 2L};
        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(ints, longs, opt));
        assertTrue(((String) opt.get(DeepEquals.DIFF)).contains("array component type mismatch"));
    }

    /** Array length mismatch */
    @Test
    public void testArrayLengthMismatch() {
        String[] a1 = {"a", "b"};
        String[] a2 = {"a", "b", "c"};
        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a1, a2, opt));
        assertTrue(((String) opt.get(DeepEquals.DIFF)).contains("array length mismatch"));
    }

    /** Array element mismatch */
    @Test
    public void testArrayElementMismatch() {
        String[] a1 = {"x", "y"};
        String[] a2 = {"x", "z"};
        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a1, a2, opt));
        assertTrue(((String) opt.get(DeepEquals.DIFF)).contains("array element mismatch"));
    }

    /** Unordered collection size mismatch */
    @Test
    public void testUnorderedCollectionSizeMismatch() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(1, 2));
        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(s1, s2, opt));
        assertTrue(((String) opt.get(DeepEquals.DIFF)).contains("collection size mismatch"));
    }

    /** Unordered collection missing element */
    @Test
    public void testUnorderedCollectionMissingElement() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> s2 = new HashSet<>(Collections.singletonList("a"));
        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(s1, s2, opt));
        assertTrue(((String) opt.get(DeepEquals.DIFF)).contains("missing collection element"));
    }

    /** Ordered collection element mismatch (List). */
    @Test
    public void testOrderedCollectionElementMismatch() {
        List<String> l1 = Arrays.asList("first", "second");
        List<String> l2 = Arrays.asList("first", "different");
        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(l1, l2, opt));
        assertTrue(((String) opt.get(DeepEquals.DIFF)).contains("collection element mismatch"));
    }

    /** Map size mismatch */
    @Test
    public void testMapSizeMismatch() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("a", 1);
        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m1, m2, opt));
        assertTrue(((String) opt.get(DeepEquals.DIFF)).contains("map size mismatch"));
    }

    /** Map missing key */
    @Test
    public void testMapMissingKey() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("a", 1);
        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m1, m2, opt));
        assertTrue(((String) opt.get(DeepEquals.DIFF)).contains("missing map key"));
    }

    /** Map value mismatch */
    @Test
    public void testMapValueMismatch() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("key", 10);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("key", 20);
        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m1, m2, opt));
        assertTrue(((String) opt.get(DeepEquals.DIFF)).contains("map value mismatch"));
    }

    /** Floating‑point tolerance (nearly equal). */
    @Test
    public void testFloatingPointTolerance() {
        double a = 0.1 + 0.2; // 0.30000000000000004
        double b = 0.3;
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    /** Floating‑point beyond tolerance should be unequal. */
    @Test
    public void testFloatingPointBeyondTolerance() {
        double a = 1.000000000000001;
        double b = 1.0;
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    /** String‑to‑Number conversion failure when option disabled. */
    @Test
    public void testStringNumberMismatchWhenOptionDisabled() {
        assertFalse(DeepEquals.deepEquals("123abc", 123));
    }

    /** Deep comparison of objects with custom equals but ignored via option. */
    @Test
    public void testCustomEqualsIgnoredViaOption() {
        CustomEquals a = new CustomEquals(1, "X");
        CustomEquals b = new CustomEquals(2, "Y"); // different fields

        // Ignore the custom equals for CustomEquals class only
        Set<Class<?>> ignoreSet = new HashSet<>();
        ignoreSet.add(CustomEquals.class);
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignoreSet);
        // With ignore, deepEquals falls back to field‑by‑field comparison and should detect mismatch
        assertFalse(DeepEquals.deepEquals(a, b, options));
    }

    /** Deep comparison of objects that have a circular reference. */
    @Test
    public void testCircularReferenceEquality() {
        Node n1 = new Node("root");
        Node n2 = new Node("root");
        n1.next = n1; // self‑cycle
        n2.next = n2; // self‑cycle
        assertTrue(DeepEquals.deepEquals(n1, n2));
    }

    /** Deep comparison where circular references differ. */
    @Test
    public void testCircularReferenceInequality() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = a; // self‑cycle
        b.next = new Node("B"); // different structure
        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, opt));
        assertTrue(((String) opt.get(DeepEquals.DIFF)).contains("value mismatch"));
    }

    /** Verify that deepHashCode works for maps with custom hashCode. */
    @Test
    public void testDeepHashCodeMapWithCustomObjects() {
        Map<String, CustomEquals> map1 = new HashMap<>();
        map1.put("one", new CustomEquals(1, "One"));
        Map<String, CustomEquals> map2 = new HashMap<>();
        map2.put("one", new CustomEquals(1, "One"));
        assertEquals(DeepEquals.deepHashCode(map1), DeepEquals.deepHashCode(map2));
    }

    /** Verify that deepHashCode treats unordered collections without order sensitivity. */
    @Test
    public void testDeepHashCodeUnorderedCollectionOrderIndependence() {
        Set<Integer> s1 = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 2, 1));
        assertEquals(DeepEquals.deepHashCode(s1), DeepEquals.deepHashCode(s2));
    }

    /** Verify that deepEquals works with URI, URL and UUID (simple types). */
    @Test
    public void testSimpleObjectTypes() throws Exception {
        URI uri1 = new URI("http://example.com");
        URI uri2 = new URI("http://example.com");
        assertTrue(DeepEquals.deepEquals(uri1, uri2));

        URL url1 = new URL("http://example.com");
        URL url2 = new URL("http://example.com");
        assertTrue(DeepEquals.deepEquals(url1, url2));

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.fromString(uuid1.toString());
        assertTrue(DeepEquals.deepEquals(uuid1, uuid2));
    }

    /** Verify that deepEquals handles Atomic* types correctly. */
    @Test
    public void testAtomicTypes() {
        AtomicBoolean ab1 = new AtomicBoolean(true);
        AtomicBoolean ab2 = new AtomicBoolean(true);
        assertTrue(DeepEquals.deepEquals(ab1, ab2));

        AtomicInteger ai1 = new AtomicInteger(5);
        AtomicInteger ai2 = new AtomicInteger(5);
        assertTrue(DeepEquals.deepEquals(ai1, ai2));

        AtomicLong al1 = new AtomicLong(123L);
        AtomicLong al2 = new AtomicLong(123L);
        assertTrue(DeepEquals.deepEquals(al1, al2));
    }

    // ------------------------------------------------------------------------
    //  Helper assertions for diff content (optional but useful)
    // ------------------------------------------------------------------------
    private static void assertDiffContains(Map<String, Object> opt, String fragment) {
        Object diff = opt.get(DeepEquals.DIFF);
        assertNotNull("Diff should be present", diff);
        assertTrue("Diff should be a String", diff instanceof String);
        assertTrue("Diff should contain expected fragment", ((String) diff).contains(fragment));
    }
}
