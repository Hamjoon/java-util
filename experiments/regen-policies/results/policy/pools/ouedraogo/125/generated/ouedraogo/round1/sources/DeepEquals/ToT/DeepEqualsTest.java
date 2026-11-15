package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link DeepEquals}.
 *
 * <p>This file demonstrates the collaborative “three‑expert” approach
 * described in the prompt – each public method of {@code DeepEquals}
 * receives at least one dedicated test case that covers typical usage,
 * edge‑cases and error‑scenarios.</p>
 *
 * <p>All tests rely only on classes, constructors and methods that are
 * visible in the source code of {@code DeepEquals} (or the Java SE
 * standard library). No mocking frameworks or external libraries are
 * used.</p>
 */
public class DeepEqualsTest {

    /** Keys used in the {@code options} map – copied from DeepEquals for readability. */
    private static final String IGNORE_CUSTOM_EQUALS = DeepEquals.IGNORE_CUSTOM_EQUALS;
    private static final String ALLOW_STRINGS_TO_MATCH_NUMBERS = DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS;
    private static final String DIFF = DeepEquals.DIFF;

    /** Helper to create a fresh options map for each test. */
    private Map<String, Object> newOptions() {
        return new HashMap<>();
    }

    /* --------------------------------------------------------------------- */
    /*  Helper classes used by several tests                                 */
    /* --------------------------------------------------------------------- */

    /** Simple POJO with a proper {@code equals} and {@code hashCode}. */
    private static class Point {
        final int x;
        final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Point)) return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /**
     * POJO that deliberately returns {@code false} from {@code equals}
     * – useful for testing {@code IGNORE_CUSTOM_EQUALS}.
     */
    private static class IgnoredEquals {
        final int value;

        IgnoredEquals(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            // Always false – forces DeepEquals to fall back to field comparison
            return false;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    /**
     * POJO that contains a self‑reference, creating a circular graph.
     * Used to verify that DeepEquals does not overflow the stack.
     */
    private static class Node {
        String name;
        Node next;

        Node(String name) {
            this.name = name;
        }
    }

    /* --------------------------------------------------------------------- */
    /*  1. deepEquals(Object, Object) – basic contract tests                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsSimplePrimitives() {
        // null vs null → true
        assertTrue(DeepEquals.deepEquals(null, null));

        // same primitive wrapper instance → true
        Integer i = 42;
        assertTrue(DeepEquals.deepEquals(i, i));

        // equal values, different instances → true
        assertTrue(DeepEquals.deepEquals(Integer.valueOf(5), Integer.valueOf(5)));

        // different values → false
        assertFalse(DeepEquals.deepEquals(Integer.valueOf(5), Integer.valueOf(6)));

        // primitive vs wrapper (auto‑boxing) → true because values match
        assertTrue(DeepEquals.deepEquals(7, Integer.valueOf(7)));
    }

    @Test
    public void testDeepEqualsFloatingPointTolerance() {
        // 0.1 + 0.2 is not exactly 0.3, but DeepEquals uses epsilon tolerance
        double sum = 0.1 + 0.2;
        assertTrue(DeepEquals.deepEquals(sum, 0.3));

        // Very small differences beyond epsilon should be reported
        double a = 1.000000000000001;
        double b = 1.0;
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEqualsStringNumberOption() {
        Map<String, Object> opts = newOptions();
        opts.put(ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        // String "123" matches Integer 123 when option is true
        assertTrue(DeepEquals.deepEquals("123", 123, opts));

        // Mismatch when option is false (default)
        assertFalse(DeepEquals.deepEquals("123", 123));
    }

    @Test
    public void testDeepEqualsCustomEqualsIgnored() {
        IgnoredEquals a = new IgnoredEquals(10);
        IgnoredEquals b = new IgnoredEquals(10);

        // Without ignoring, custom equals returns false → overall false
        assertFalse(DeepEquals.deepEquals(a, b));

        // Ignore the class → DeepEquals falls back to field comparison → true
        Map<String, Object> opts = newOptions();
        opts.put(IGNORE_CUSTOM_EQUALS, Collections.singleton(IgnoredEquals.class));
        assertTrue(DeepEquals.deepEquals(a, b, opts));
    }

    @Test
    public void testDeepEqualsCustomEqualsNotIgnored() {
        // Even when we provide a non‑empty ignore set that does NOT contain the class,
        // DeepEquals must still honour the class's custom equals.
        IgnoredEquals a = new IgnoredEquals(10);
        IgnoredEquals b = new IgnoredEquals(10);

        Map<String, Object> opts = newOptions();
        opts.put(IGNORE_CUSTOM_EQUALS, Collections.emptySet()); // ignore ALL custom equals
        // empty set means *ignore all* – should succeed
        assertTrue(DeepEquals.deepEquals(a, b, opts));

        // Non‑empty set that does NOT contain IgnoredEquals → custom equals used → false
        opts.put(IGNORE_CUSTOM_EQUALS, Collections.singleton(Point.class));
        assertFalse(DeepEquals.deepEquals(a, b, opts));
    }

    @Test
    public void testDeepEqualsCollectionsAndMaps() {
        // Ordered List – order matters
        List<String> l1 = Arrays.asList("a", "b", "c");
        List<String> l2 = Arrays.asList("a", "b", "c");
        List<String> l3 = Arrays.asList("c", "b", "a");

        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));

        // Unordered Set – order ignored
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 2, 1));
        assertTrue(DeepEquals.deepEquals(s1, s2));

        // Map – key/value mismatch
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("one", 1);
        m2.put("two", 3); // different value

        assertFalse(DeepEquals.deepEquals(m1, m2));

        // Verify that the diff description is populated
        Map<String, Object> opts = newOptions();
        DeepEquals.deepEquals(m1, m2, opts);
        assertNotNull(opts.get(DIFF));
        String diff = (String) opts.get(DIFF);
        assertTrue(diff.contains("map value mismatch"));
    }

    @Test
    public void testDeepEqualsArraysAndMultidimensionalArrays() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {1, 2, 4};

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));

        // Multi‑dimensional array
        int[][] m1 = {{1, 2}, {3, 4}};
        int[][] m2 = {{1, 2}, {3, 4}};
        int[][] m3 = {{1, 2}, {4, 3}}; // swapped inner elements

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    @Test
    public void testDeepEqualsCircularReference() {
        Node n1 = new Node("A");
        Node n2 = new Node("A");
        n1.next = n1; // self‑reference
        n2.next = n2;

        // Should not overflow stack and should be equal
        assertTrue(DeepEquals.deepEquals(n1, n2));
    }

    /* --------------------------------------------------------------------- */
    /*  2. hasCustomEquals(Class<?>) – detection tests                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testHasCustomEqualsDetection() {
        // Point overrides equals → true
        assertTrue(DeepEquals.hasCustomEquals(Point.class));

        // String does NOT override equals (inherits from Object) → false
        assertFalse(DeepEquals.hasCustomEquals(String.class));

        // IgnoredEquals overrides equals (returns false) → true
        assertTrue(DeepEquals.hasCustomEquals(IgnoredEquals.class));
    }

    /* --------------------------------------------------------------------- */
    /*  3. hasCustomHashCode(Class<?>) – detection tests                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testHasCustomHashCodeDetection() {
        // Point overrides hashCode → true
        assertTrue(DeepEquals.hasCustomHashCode(Point.class));

        // java.util.Date overrides hashCode → true
        assertTrue(DeepEquals.hasCustomHashCode(Date.class));

        // Simple array class does NOT override hashCode (Object) → false
        assertFalse(DeepEquals.hasCustomHashCode(int[].class));
    }

    /* --------------------------------------------------------------------- */
    /*  4. deepHashCode(Object) – consistency and collision tests             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeBasicConsistency() {
        Point p1 = new Point(5, 7);
        Point p2 = new Point(5, 7);
        Point p3 = new Point(7, 5);

        int h1 = DeepEquals.deepHashCode(p1);
        int h2 = DeepEquals.deepHashCode(p2);
        int h3 = DeepEquals.deepHashCode(p3);

        // Same logical state → same hash
        assertEquals(h1, h2);
        // Different state → likely different hash (not a guarantee, but very probable)
        assertNotEquals(h1, h3);
    }

    @Test
    public void testDeepHashCodeComplexStructures() {
        List<Object> list1 = new ArrayList<>();
        list1.add("hello");
        list1.add(123);
        list1.add(new Point(1, 2));

        List<Object> list2 = new ArrayList<>();
        list2.add("hello");
        list2.add(123);
        list2.add(new Point(1, 2));

        // Deep hash must be identical for deeply equal structures
        assertEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));

        // Change one element → hash changes
        list2.set(1, 124);
        assertNotEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));
    }

    @Test
    public void testDeepHashCodeWithCircularReference() {
        Node a = new Node("root");
        Node b = new Node("root");
        a.next = a; // circular
        b.next = b;

        // Should compute without infinite recursion
        int hA = DeepEquals.deepHashCode(a);
        int hB = DeepEquals.deepHashCode(b);
        assertEquals(hA, hB);
    }

    /* --------------------------------------------------------------------- */
    /*  5. Additional regression tests for edge cases                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsWithURIAndURL() throws Exception {
        URI uri1 = new URI("http://example.com/path");
        URI uri2 = new URI("http://example.com/path");
        assertTrue(DeepEquals.deepEquals(uri1, uri2));

        URL url1 = new URL("http://example.com/path");
        URL url2 = new URL("http://example.com/path");
        assertTrue(DeepEquals.deepEquals(url1, url2));
    }

    @Test
    public void testDeepEqualsBigDecimalPrecision() {
        // BigDecimal values that are numerically equal but have different scales
        BigDecimal bd1 = new BigDecimal("1.00");
        BigDecimal bd2 = new BigDecimal("1.0");
        assertTrue(DeepEquals.deepEquals(bd1, bd2));
    }

    @Test
    public void testDeepEqualsAtomicTypes() {
        AtomicBoolean ab1 = new AtomicBoolean(true);
        AtomicBoolean ab2 = new AtomicBoolean(true);
        AtomicBoolean ab3 = new AtomicBoolean(false);

        assertTrue(DeepEquals.deepEquals(ab1, ab2));
        assertFalse(DeepEquals.deepEquals(ab1, ab3));
    }

    @Test
    public void testDeepEqualsDifferentRootTypes() {
        // Different root types should immediately report false
        assertFalse(DeepEquals.deepEquals("string", 123));
        assertFalse(DeepEquals.deepEquals(new int[]{1, 2}, new ArrayList<>(Arrays.asList(1, 2))));
    }

    @Test
    public void testDeepEqualsNullAndNonNullRoot() {
        assertFalse(DeepEquals.deepEquals(null, "non-null"));
        assertFalse(DeepEquals.deepEquals(42, null));
    }
}
