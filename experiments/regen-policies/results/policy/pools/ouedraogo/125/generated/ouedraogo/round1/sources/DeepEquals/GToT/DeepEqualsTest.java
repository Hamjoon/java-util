package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Comprehensive JUnit‑4 test suite for {@link DeepEquals}.
 *
 * <p>This file is organized in four logical sections that mirror the
 * collaborative steps described in the prompt:</p>
 *
 * <ol>
 *   <li><strong>Public API extraction</strong> – a comment block that lists
 *       every public method (including signatures) of {@code DeepEquals}.</li>
 *   <li><strong>Basic sanity tests</strong> – one straightforward test per
 *       public method that checks the most common / happy‑path behaviour.</li>
 *   <li><strong>Edge‑case & exception identification</strong> – a comment
 *       summarising the important corner cases that the class can encounter.</li>
 *   <li><strong>Edge‑case tests</strong> – dedicated tests that exercise
 *       each identified corner case, including error handling (e.g. {@code null}
 *       arguments, circular references, option handling, floating‑point
 *       tolerance, etc.).</li>
 * </ol>
 *
 * <p>The test class uses only the public API of {@code DeepEquals} and of the
 * Java standard library – no mocking frameworks or reflection hacks are
 * employed, in accordance with the constraints of the exercise.</p>
 */
public class DeepEqualsTest {

    /* --------------------------------------------------------------
     * 1. PUBLIC API EXTRACTION
     * --------------------------------------------------------------
     *
     * The following public static methods are defined in {@link DeepEquals}:
     *
     * <ul>
     *   <li>{@code public static boolean deepEquals(Object a, Object b)}</li>
     *   <li>{@code public static boolean deepEquals(Object a, Object b, Map<String, ?> options)}</li>
     *   <li>{@code public static boolean hasCustomEquals(Class<?> c)}</li>
     *   <li>{@code public static boolean hasCustomHashCode(Class<?> c)}</li>
     *   <li>{@code public static int deepHashCode(Object obj)}</li>
     * </ul>
     *
     * The class also defines the public constant option keys:
     *
     * <ul>
     *   <li>{@code public static final String IGNORE_CUSTOM_EQUALS}</li>
     *   <li>{@code public static final String ALLOW_STRINGS_TO_MATCH_NUMBERS}</li>
     *   <li>{@code public static final String DIFF}</li>
     * </ul>
     */

    /* --------------------------------------------------------------
     * 2. BASIC SANITY TESTS (ONE PER PUBLIC METHOD)
     * --------------------------------------------------------------
     */

    @Test
    public void testDeepEqualsSimpleValues() {
        // Primitive wrappers and strings – happy path
        Assert.assertTrue(DeepEquals.deepEquals(42, 42));
        Assert.assertTrue(DeepEquals.deepEquals("hello", "hello"));
        Assert.assertTrue(DeepEquals.deepEquals(null, null));

        // Different values must be false
        Assert.assertFalse(DeepEquals.deepEquals(42, 43));
        Assert.assertFalse(DeepEquals.deepEquals("hello", "world"));
        Assert.assertFalse(DeepEquals.deepEquals(null, "not null"));
    }

    @Test
    public void testDeepEqualsWithOptionsMap() {
        Map<String, Object> opts = new HashMap<>();
        // No special options – behaviour must be identical to the 2‑arg overload
        Assert.assertTrue(DeepEquals.deepEquals("abc", "abc", opts));
        Assert.assertFalse(DeepEquals.deepEquals("abc", "def", opts));
        // Verify that the diff entry is populated on failure
        Assert.assertFalse(DeepEquals.deepEquals("x", "y", opts));
        Assert.assertTrue(opts.containsKey(DeepEquals.DIFF));
        Assert.assertTrue(((String) opts.get(DeepEquals.DIFF)).contains("value mismatch"));
    }

    @Test
    public void testHasCustomEquals() {
        // Class with overridden equals
        Assert.assertTrue(DeepEquals.hasCustomEquals(WithCustomEquals.class));
        // Class that inherits Object.equals()
        Assert.assertFalse(DeepEquals.hasCustomEquals(PlainClass.class));
    }

    @Test
    public void testHasCustomHashCode() {
        // Class with overridden hashCode
        Assert.assertTrue(DeepEquals.hasCustomHashCode(WithCustomHashCode.class));
        // Class that inherits Object.hashCode()
        Assert.assertFalse(DeepEquals.hasCustomHashCode(PlainClass.class));
    }

    @Test
    public void testDeepHashCodeConsistency() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        int h1 = DeepEquals.deepHashCode(list1);
        int h2 = DeepEquals.deepHashCode(list2);
        Assert.assertEquals(h1, h2);

        // Different content → different hash (not guaranteed but highly likely)
        List<Integer> list3 = Arrays.asList(3, 2, 1);
        int h3 = DeepEquals.deepHashCode(list3);
        Assert.assertNotEquals(h1, h3);
    }

    /* --------------------------------------------------------------
     * 3. EDGE‑CASE & EXCEPTION IDENTIFICATION
     * --------------------------------------------------------------
     *
     * The following scenarios are critical to test:
     *
     * <ul>
     *   <li><strong>Null handling</strong> – one or both arguments are {@code null}.</li>
     *   <li><strong>Collections</strong> – ordered (List) vs unordered (Set) equality,
     *       size mismatches, empty collections.</li>
     *   <li><strong>Maps</strong> – missing keys, value mismatches, empty maps.</li>
     *   <li><strong>Arrays</strong> – different lengths, different dimensions,
     *       component‑type mismatches, empty arrays.</li>
     *   <li><strong>Circular references</strong> – objects that reference each other
     *       directly or indirectly.</li>
     *   <li><strong>Custom {@code equals()}</strong> – ensure the {@code ignoreCustomEquals}
     *       option works for selected classes.</li>
     *   <li><strong>String‑to‑Number matching</strong> – enabled via the
     *       {@code ALLOW_STRINGS_TO_MATCH_NUMBERS} option.</li>
     *   <li><strong>Floating‑point tolerance</strong> – numbers that differ only
     *       within the epsilon used by {@code DeepEquals}.</li>
     *   <li><strong>Exception handling</strong> – {@code hasCustomEquals(null)} and
     *       {@code hasCustomHashCode(null)} must throw {@link IllegalArgumentException}.</li>
     * </ul>
     */

    /* --------------------------------------------------------------
     * 4. EDGE‑CASE TESTS
     * --------------------------------------------------------------
     */

    @Test
    public void testDeepEqualsNullCases() {
        // both null → true
        Assert.assertTrue(DeepEquals.deepEquals(null, null));

        // exactly one null → false, diff populated
        Map<String, Object> opts = new HashMap<>();
        Assert.assertFalse(DeepEquals.deepEquals(null, "not null", opts));
        Assert.assertTrue(opts.containsKey(DeepEquals.DIFF));
        Assert.assertTrue(((String) opts.get(DeepEquals.DIFF)).contains("value mismatch"));
    }

    @Test
    public void testOrderedCollectionEquality() {
        List<String> a = Arrays.asList("one", "two", "three");
        List<String> b = Arrays.asList("one", "two", "three");
        List<String> c = Arrays.asList("three", "two", "one");

        Assert.assertTrue(DeepEquals.deepEquals(a, b));
        Assert.assertFalse(DeepEquals.deepEquals(a, c));

        // Size mismatch
        List<String> d = Arrays.asList("one", "two");
        Assert.assertFalse(DeepEquals.deepEquals(a, d));
    }

    @Test
    public void testUnorderedCollectionEquality() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 2, 1));
        Set<Integer> s3 = new HashSet<>(Arrays.asList(1, 2));

        Assert.assertTrue(DeepEquals.deepEquals(s1, s2));
        Assert.assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    @Test
    public void testMapEqualityAndMissingKey() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);

        Map<String, Integer> m2 = new HashMap<>();
        m2.put("a", 1);
        m2.put("b", 2);

        Map<String, Integer> m3 = new HashMap<>();
        m3.put("a", 1);
        // missing key "b"

        Assert.assertTrue(DeepEquals.deepEquals(m1, m2));
        Assert.assertFalse(DeepEquals.deepEquals(m1, m3));

        // Verify diff contains missing‑key wording
        Map<String, Object> opts = new HashMap<>();
        Assert.assertFalse(DeepEquals.deepEquals(m1, m3, opts));
        Assert.assertTrue(((String) opts.get(DeepEquals.DIFF)).contains("missing map key"));
    }

    @Test
    public void testArrayEqualityAndDimensionMismatches() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {1, 2};

        Assert.assertTrue(DeepEquals.deepEquals(a1, a2));
        Assert.assertFalse(DeepEquals.deepEquals(a1, a3));

        // Multi‑dimensional array
        int[][] md1 = {{1, 2}, {3, 4}};
        int[][] md2 = {{1, 2}, {3, 4}};
        int[][] md3 = {{1, 2}, {4, 3}}; // element mismatch
        int[][] md4 = {{1, 2, 3}, {4, 5, 6}}; // dimension mismatch

        Assert.assertTrue(DeepEquals.deepEquals(md1, md2));
        Assert.assertFalse(DeepEquals.deepEquals(md1, md3));
        Assert.assertFalse(DeepEquals.deepEquals(md1, md4));

        // Verify diff for dimension mismatch
        Map<String, Object> opts = new HashMap<>();
        Assert.assertFalse(DeepEquals.deepEquals(md1, md4, opts));
        Assert.assertTrue(((String) opts.get(DeepEquals.DIFF)).contains("array dimensionality mismatch"));
    }

    @Test
    public void testCircularReferenceHandling() {
        // Simple node class that points to another node
        class Node {
            int value;
            Node next;
            Node(int v) { this.value = v; }
        }

        Node n1 = new Node(1);
        Node n2 = new Node(2);
        n1.next = n2;
        n2.next = n1; // circular back to n1

        Node m1 = new Node(1);
        Node m2 = new Node(2);
        m1.next = m2;
        m2.next = m1; // same shape, same values

        Assert.assertTrue(DeepEquals.deepEquals(n1, m1));

        // Break the circle on the second graph – should be false
        Node m3 = new Node(1);
        Node m4 = new Node(2);
        m3.next = m4;
        // m4.next remains null

        Assert.assertFalse(DeepEquals.deepEquals(n1, m3));
    }

    @Test
    public void testIgnoreCustomEqualsOption() {
        // Class with a custom equals that deliberately returns false for equal data
        WithCustomEquals a = new WithCustomEquals(10);
        WithCustomEquals b = new WithCustomEquals(10);

        // Without ignoring, deepEquals uses the custom equals → false
        Assert.assertFalse(DeepEquals.deepEquals(a, b));

        // Ignore the class via options → field‑by‑field comparison → true
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.singleton(WithCustomEquals.class));
        Assert.assertTrue(DeepEquals.deepEquals(a, b, opts));
    }

    @Test
    public void testStringNumberMatchingOption() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        // String "42" should match integer 42
        Assert.assertTrue(DeepEquals.deepEquals("42", 42, opts));
        Assert.assertTrue(DeepEquals.deepEquals(3.14, "3.14", opts));

        // Mismatch when the numeric value differs
        Assert.assertFalse(DeepEquals.deepEquals("43", 42, opts));

        // Disabled option → strict type check
        Map<String, Object> disabled = new HashMap<>();
        disabled.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);
        Assert.assertFalse(DeepEquals.deepEquals("42", 42, disabled));
    }

    @Test
    public void testFloatingPointTolerance() {
        double a = 0.1 + 0.2; // 0.30000000000000004
        double b = 0.3;

        // Direct equals would be false, but DeepEquals should consider them equal
        Assert.assertTrue(DeepEquals.deepEquals(a, b));

        // Very far apart values must be false
        Assert.assertFalse(DeepEquals.deepEquals(1.0e-10, 1.0e-5));
    }

    @Test
    public void testEmptyContainers() {
        // Empty collections
        Assert.assertTrue(DeepEquals.deepEquals(Collections.emptyList(), Collections.emptyList()));
        Assert.assertTrue(DeepEquals.deepEquals(new HashSet<>(), new HashSet<>()));

        // Empty maps
        Assert.assertTrue(DeepEquals.deepEquals(Collections.emptyMap(), Collections.emptyMap()));

        // Empty arrays
        int[] empty1 = new int[0];
        int[] empty2 = new int[0];
        Assert.assertTrue(DeepEquals.deepEquals(empty1, empty2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHasCustomEqualsNullArgument() {
        DeepEquals.hasCustomEquals(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHasCustomHashCodeNullArgument() {
        DeepEquals.hasCustomHashCode(null);
    }

    /* --------------------------------------------------------------
     * Helper classes used by the tests
     * --------------------------------------------------------------
     */

    /** Plain class – inherits {@code Object.equals()} and {@code Object.hashCode()}. */
    private static class PlainClass {
        int x = 5;
    }

    /** Class that overrides {@code equals()} but not {@code hashCode()}. */
    private static class WithCustomEquals {
        final int value;
        WithCustomEquals(int v) { this.value = v; }

        @Override
        public boolean equals(Object o) {
            // Deliberately incorrect implementation for testing purposes
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WithCustomEquals that = (WithCustomEquals) o;
            // Returns false even when values are equal – forces DeepEquals to fall back
            return this.value != that.value;
        }
    }

    /** Class that overrides {@code hashCode()} but not {@code equals()}. */
    private static class WithCustomHashCode {
        final String id;
        WithCustomHashCode(String id) { this.id = id; }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }
}
