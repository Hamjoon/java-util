package com.cedarsoftware.util;

import org.junit.Test;
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
 * <p>The tests are written from the perspective of three “experts”.<br/>
 * Each expert contributes one test per public method, validates the behaviour,
 * and then the suite is assembled as a single file.</p>
 *
 * <p>Only the public API of {@code DeepEquals} and the classes that appear
 * in the source file are used – no mocking, no subclassing of library classes.</p>
 */
public class DeepEqualsTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper classes used by several tests                                 */
    /* ---------------------------------------------------------------------- */

    /** Simple POJO with a custom {@code equals()} that deliberately returns {@code false}. */
    static class CustomEquals {
        int id;
        String name;

        CustomEquals(int id, String name) {
            this.id = id;
            this.name = name;
        }

        // Custom equals that *always* returns false – forces DeepEquals to fall back to field comparison.
        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

    /** POJO with a proper equals implementation (used to verify that DeepEquals respects it). */
    static class ProperEquals {
        int value;

        ProperEquals(int v) { this.value = v; }

        @Override
        public boolean equals(Object o) {
            return (o instanceof ProperEquals) && ((ProperEquals) o).value == this.value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }
    }

    /** POJO that contains a circular reference back to itself. */
    static class Node {
        String name;
        Node next;

        Node(String name) { this.name = name; }
    }

    /* ---------------------------------------------------------------------- */
    /*  1. deepEquals(Object, Object)                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void expert1_deepEqualsSimpleScalars() {
        // equal primitives wrapped as objects
        assertTrue(DeepEquals.deepEquals(5, Integer.valueOf(5)));
        assertTrue(DeepEquals.deepEquals('a', Character.valueOf('a')));
        assertTrue(DeepEquals.deepEquals(true, Boolean.TRUE));

        // different values must be false
        assertFalse(DeepEquals.deepEquals(5, 6));
        assertFalse(DeepEquals.deepEquals("foo", "bar"));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
        assertTrue(DeepEquals.deepEquals(null, null));
    }

    @Test
    public void expert1_deepEqualsFloatingPointTolerance() {
        // Values that differ only beyond the epsilon should be considered equal
        double a = 0.1 + 0.2; // 0.30000000000000004
        double b = 0.3;
        assertTrue(DeepEquals.deepEquals(a, b));

        // Very small difference beyond epsilon should be false
        assertFalse(DeepEquals.deepEquals(1.000000000000001, 1.0));
    }

    @Test
    public void expert1_deepEqualsArraysAndDimensions() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {1, 2};

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));

        // multidimensional arrays
        int[][] m1 = {{1, 2}, {3, 4}};
        int[][] m2 = {{1, 2}, {3, 4}};
        int[][] m3 = {{1, 2}, {4, 3}}; // element order differs
        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));

        // Different dimensions
        int[][][] d1 = new int[2][2][2];
        int[][] d2 = new int[2][2];
        assertFalse(DeepEquals.deepEquals(d1, d2));
    }

    /* ---------------------------------------------------------------------- */
    /*  2. deepEquals(Object, Object, Map) – options handling                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void expert2_deepEqualsIgnoreCustomEquals() {
        CustomEquals c1 = new CustomEquals(1, "alpha");
        CustomEquals c2 = new CustomEquals(1, "alpha");

        // By default, custom equals is used (which always fails)
        assertFalse(DeepEquals.deepEquals(c1, c2));

        // Ignore the custom equals – field‑by‑field comparison should succeed
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(CustomEquals.class));
        assertTrue(DeepEquals.deepEquals(c1, c2, opts));
    }

    @Test
    public void expert2_deepEqualsStringNumberMatchingOption() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        // String "10" should match Integer 10 when the option is true
        assertTrue(DeepEquals.deepEquals("10", 10, opts));

        // When the option is false (default) they are not equal
        assertFalse(DeepEquals.deepEquals("10", 10));

        // Mixed numeric types (BigDecimal vs Double) are handled without the option
        BigDecimal bd = new BigDecimal("3.14");
        Double d = 3.14;
        assertTrue(DeepEquals.deepEquals(bd, d));
    }

    @Test
    public void expert2_deepEqualsCollectionsAndMaps() {
        // Ordered List – order matters
        List<String> list1 = List.of("a", "b", "c");
        List<String> list2 = List.of("a", "b", "c");
        List<String> list3 = List.of("c", "b", "a");
        assertTrue(DeepEquals.deepEquals(list1, list2));
        assertFalse(DeepEquals.deepEquals(list1, list3));

        // Unordered Set – order ignored
        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 2, 1));
        assertTrue(DeepEquals.deepEquals(set1, set2));

        // Map with same entries
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("x", 1);
        map1.put("y", 2);
        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("x", 1);
        map2.put("y", 2);
        assertTrue(DeepEquals.deepEquals(map1, map2));

        // Map value mismatch – diff should be stored
        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(map1, map2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("map value mismatch"));
    }

    @Test
    public void expert2_deepEqualsCircularReference() {
        Node n1 = new Node("first");
        Node n2 = new Node("first");
        n1.next = n1; // self‑reference
        n2.next = n2;

        assertTrue(DeepEquals.deepEquals(n1, n2));

        // Slight difference in the circular chain
        Node n3 = new Node("first");
        Node n4 = new Node("first");
        n3.next = new Node("second"); // break the cycle
        n4.next = n4; // proper cycle
        assertFalse(DeepEquals.deepEquals(n3, n4));
    }

    /* ---------------------------------------------------------------------- */
    /*  3. hasCustomEquals(Class<?>)                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void expert3_hasCustomEqualsDetection() {
        assertTrue(DeepEquals.hasCustomEquals(CustomEquals.class));
        assertTrue(DeepEquals.hasCustomEquals(ProperEquals.class));
        // java.lang.Object does not declare its own equals
        assertFalse(DeepEquals.hasCustomEquals(Object.class));
        // Primitive wrapper classes use Object's equals (they override, actually they do)
        assertTrue(DeepEquals.hasCustomEquals(Integer.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  4. hasCustomHashCode(Class<?>)                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void expert3_hasCustomHashCodeDetection() {
        assertTrue(DeepEquals.hasCustomHashCode(CustomEquals.class));
        // java.lang.Object has no custom hashCode
        assertFalse(DeepEquals.hasCustomHashCode(Object.class));
        // Integer overrides hashCode
        assertTrue(DeepEquals.hasCustomHashCode(Integer.class));
    }

    /* ---------------------------------------------------------------------- */
    /*  5. deepHashCode(Object) – consistency with deepEquals                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void expert3_deepHashCodeConsistency() {
        // Simple scalar
        Integer i1 = 42;
        Integer i2 = 42;
        assertEquals(DeepEquals.deepHashCode(i1), DeepEquals.deepHashCode(i2));

        // Complex object with fields
        ProperEquals p1 = new ProperEquals(99);
        ProperEquals p2 = new ProperEquals(99);
        assertEquals(DeepEquals.deepHashCode(p1), DeepEquals.deepHashCode(p2));

        // Different objects must (usually) have different hash codes
        ProperEquals p3 = new ProperEquals(100);
        assertNotEquals(DeepEquals.deepHashCode(p1), DeepEquals.deepHashCode(p3));

        // Collections – order matters for List, not for Set
        List<String> l1 = List.of("a", "b", "c");
        List<String> l2 = List.of("a", "b", "c");
        List<String> l3 = List.of("c", "b", "a");
        assertEquals(DeepEquals.deepHashCode(l1), DeepEquals.deepHashCode(l2));
        assertNotEquals(DeepEquals.deepHashCode(l1), DeepEquals.deepHashCode(l3));

        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 2, 1));
        assertEquals(DeepEquals.deepHashCode(s1), DeepEquals.deepHashCode(s2));

        // Maps – order of insertion does not affect deepHashCode
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("b", 2);
        m2.put("a", 1);
        assertEquals(DeepEquals.deepHashCode(m1), DeepEquals.deepHashCode(m2));
    }

    /* ---------------------------------------------------------------------- */
    /*  6. DeepEquals with a mix of all supported types (integration test)     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void expert3_complexObjectGraphEquality() throws Exception {
        // Build a complex graph containing primitives, collections, maps, arrays,
        // custom‑equals objects, and a circular reference.
        Map<String, Object> mapA = new LinkedHashMap<>();
        mapA.put("int", 100);
        mapA.put("str", "hello");
        mapA.put("list", List.of(1, 2, 3));
        mapA.put("custom", new CustomEquals(7, "seven"));

        // Circular structure
        Node nA = new Node("root");
        nA.next = nA; // self‑cycle
        mapA.put("node", nA);

        // Duplicate the structure
        Map<String, Object> mapB = new LinkedHashMap<>();
        mapB.put("int", 100);
        mapB.put("str", "hello");
        mapB.put("list", List.of(1, 2, 3));
        mapB.put("custom", new CustomEquals(7, "seven"));
        Node nB = new Node("root");
        nB.next = nB;
        mapB.put("node", nB);

        // Without ignoring custom equals the comparison fails
        assertFalse(DeepEquals.deepEquals(mapA, mapB));

        // Ignore the custom equals class – now the structures are equal
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(CustomEquals.class));
        assertTrue(DeepEquals.deepEquals(mapA, mapB, opts));

        // Verify that the diff string for the failing case points to the custom field
        Map<String, Object> failOpts = new HashMap<>();
        DeepEquals.deepEquals(mapA, mapB, failOpts);
        String diff = (String) failOpts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("field value mismatch"));
        assertTrue(diff.contains(".custom"));
    }

    /* ---------------------------------------------------------------------- */
    /*  7. Edge cases – null handling and empty containers                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void expert3_nullAndEmptyContainerHandling() {
        // Both null
        assertTrue(DeepEquals.deepEquals(null, null));

        // One null, one non‑null
        assertFalse(DeepEquals.deepEquals(null, "not null"));

        // Empty collections
        List<String> emptyList1 = Collections.emptyList();
        List<String> emptyList2 = Collections.emptyList();
        assertTrue(DeepEquals.deepEquals(emptyList1, emptyList2));

        // Empty maps
        Map<String, Integer> emptyMap1 = Collections.emptyMap();
        Map<String, Integer> emptyMap2 = Collections.emptyMap();
        assertTrue(DeepEquals.deepEquals(emptyMap1, emptyMap2));

        // Empty arrays
        int[] emptyArr1 = new int[0];
        int[] emptyArr2 = new int[0];
        assertTrue(DeepEquals.deepEquals(emptyArr1, emptyArr2));
    }

    /* ---------------------------------------------------------------------- */
    /*  8. Verify that the breadcrumb diff string is correctly formatted       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void expert3_diffStringFormatting() {
        Map<String, Object> opts = new HashMap<>();

        List<Integer> a = List.of(1, 2, 3);
        List<Integer> b = List.of(1, 9, 3); // element 1 differs

        assertFalse(DeepEquals.deepEquals(a, b, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        // Expected pattern: "[collection element mismatch] ▶ List(0..2) ▶ .[1]"
        assertTrue(diff.contains("[collection element mismatch]"));
        assertTrue(diff.contains("List(0..2)"));
        assertTrue(diff.contains("[1]"));
        // Expected/Found values should appear
        assertTrue(diff.contains("Expected: 2"));
        assertTrue(diff.contains("Found: 9"));
    }

    /* ---------------------------------------------------------------------- */
    /*  9. Verify that deepEquals correctly handles Atomic types                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void expert3_atomicTypesEquality() {
        AtomicBoolean ab1 = new AtomicBoolean(true);
        AtomicBoolean ab2 = new AtomicBoolean(true);
        AtomicBoolean ab3 = new AtomicBoolean(false);
        assertTrue(DeepEquals.deepEquals(ab1, ab2));
        assertFalse(DeepEquals.deepEquals(ab1, ab3));

        AtomicInteger ai1 = new AtomicInteger(42);
        AtomicInteger ai2 = new AtomicInteger(42);
        AtomicInteger ai3 = new AtomicInteger(43);
        assertTrue(DeepEquals.deepEquals(ai1, ai2));
        assertFalse(DeepEquals.deepEquals(ai1, ai3));
    }

    /* ---------------------------------------------------------------------- */
    /*  10. Verify handling of URI, URL, UUID, TimeZone, Date                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void expert3_miscSimpleTypes() throws Exception {
        URI u1 = new URI("http://example.com");
        URI u2 = new URI("http://example.com");
        assertTrue(DeepEquals.deepEquals(u1, u2));

        URL url1 = new URL("http://example.com");
        URL url2 = new URL("http://example.com");
        assertTrue(DeepEquals.deepEquals(url1, url2));

        UUID id1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID id2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertTrue(DeepEquals.deepEquals(id1, id2));

        TimeZone tz1 = TimeZone.getTimeZone("UTC");
        TimeZone tz2 = TimeZone.getTimeZone("UTC");
        assertTrue(DeepEquals.deepEquals(tz1, tz2));

        Date d1 = new Date(1633024800000L);
        Date d2 = new Date(1633024800000L);
        assertTrue(DeepEquals.deepEquals(d1, d2));

        // Different values
        assertFalse(DeepEquals.deepEquals(tz1, TimeZone.getTimeZone("GMT")));
        assertFalse(DeepEquals.deepEquals(d1, new Date(0L)));
    }
}
