package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JUnit‑4 test suite for {@link DeepEquals}.
 *
 * The suite is organized as follows:
 *
 * <ul>
 *   <li>Public API extraction – listed in the Javadoc of this class.</li>
 *   <li>Basic sanity tests – null handling, primitive wrappers, simple objects.</li>
 *   <li>Edge‑case tests – floating‑point tolerance, BigDecimal overflow, string‑to‑number
 *        matching, custom {@code equals()} handling, circular references, array
 *        dimension/component/length mismatches, collection/map mismatches.</li>
 *   <li>Utility / helper inner classes used only by the tests.</li>
 * </ul>
 *
 * All tests use only the public API of {@link DeepEquals} and standard JDK classes,
 * as required by the task description.
 */
public class DeepEqualsTest {

    /* -------------------------------------------------
     *  Public methods of DeepEquals (extracted)
     * -------------------------------------------------
     *
     *   public static boolean deepEquals(Object a, Object b)
     *   public static boolean deepEquals(Object a, Object b, Map<String, ?> options)
     *   public static boolean hasCustomEquals(Class<?> c)
     *   public static boolean hasCustomHashCode(Class<?> c)
     *   public static int deepHashCode(Object obj)
     *
     */

    /* -------------------------------------------------
     *  Helper classes for testing
     * ------------------------------------------------- */

    /** Simple POJO with two fields – no custom equals/hashCode. */
    private static class Person {
        String name;
        int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    /** POJO that implements its own equals/hashCode based on an id field. */
    private static class CustomEquals {
        int id;

        CustomEquals(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CustomEquals)) return false;
            return id == ((CustomEquals) o).id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    /** Two-node circular structure. */
    private static class Node {
        Node next;
        String label;

        Node(String label) {
            this.label = label;
        }
    }

    /* -------------------------------------------------
     *  Basic functionality tests
     * ------------------------------------------------- */

    @Test
    public void testDeepEqualsBothNull() {
        assertTrue(DeepEquals.deepEquals(null, null));
    }

    @Test
    public void testDeepEqualsOneNull() {
        assertFalse(DeepEquals.deepEquals(null, new Object()));
        assertFalse(DeepEquals.deepEquals(new Object(), null));
    }

    @Test
    public void testDeepEqualsSimpleWrappersEqual() {
        assertTrue(DeepEquals.deepEquals(Integer.valueOf(5), Integer.valueOf(5)));
        assertTrue(DeepEquals.deepEquals(Boolean.TRUE, Boolean.TRUE));
    }

    @Test
    public void testDeepEqualsSimpleWrappersNotEqual() {
        assertFalse(DeepEquals.deepEquals(Integer.valueOf(5), Integer.valueOf(6)));
        assertFalse(DeepEquals.deepEquals(Boolean.TRUE, Boolean.FALSE));
    }

    @Test
    public void testDeepEqualsSimpleObjects() {
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 30);
        assertTrue(DeepEquals.deepEquals(p1, p2));
    }

    @Test
    public void testDeepEqualsSimpleObjectsFieldMismatch() {
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 31);
        assertFalse(DeepEquals.deepEquals(p1, p2));
        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(p1, p2, opts);
        assertNotNull(opts.get(DeepEquals.DIFF));
    }

    /* -------------------------------------------------
     *  Edge‑case tests – numbers, tolerance, overflow
     * ------------------------------------------------- */

    @Test
    public void testDeepEqualsFloatingPointTolerance() {
        double a = 0.1 + 0.2; // 0.30000000000000004
        double b = 0.3;
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEqualsBigDecimalOverflow() {
        // Very large BigDecimal that exceeds Double.MAX_VALUE
        BigDecimal huge = new BigDecimal(Double.MAX_VALUE).multiply(BigDecimal.TEN);
        double d = Double.MAX_VALUE;
        // Should be considered not equal because of overflow check
        assertFalse(DeepEquals.deepEquals(huge, d));
    }

    @Test
    public void testStringToNumberMatchingEnabled() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("123", Integer.valueOf(123), opts));
        assertTrue(DeepEquals.deepEquals(Long.valueOf(456L), "456", opts));
    }

    @Test
    public void testStringToNumberMatchingDisabled() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);
        assertFalse(DeepEquals.deepEquals("123", Integer.valueOf(123), opts));
    }

    /* -------------------------------------------------
     *  Custom equals handling (ignoreCustomEquals option)
     * ------------------------------------------------- */

    @Test
    public void testHasCustomEquals() {
        assertTrue(DeepEquals.hasCustomEquals(String.class)); // String overrides equals
        assertFalse(DeepEquals.hasCustomEquals(Person.class)); // Person has no custom equals
    }

    @Test
    public void testHasCustomHashCode() {
        assertTrue(DeepEquals.hasCustomHashCode(String.class)); // String overrides hashCode
        assertFalse(DeepEquals.hasCustomHashCode(Person.class)); // Person has no custom hashCode
    }

    @Test
    public void testDeepEqualsCustomEqualsUsedByDefault() {
        CustomEquals c1 = new CustomEquals(1);
        CustomEquals c2 = new CustomEquals(2);
        // Default options – custom equals is *used* → mismatch
        assertFalse(DeepEquals.deepEquals(c1, c2));
    }

    @Test
    public void testDeepEqualsIgnoreAllCustomEquals() {
        CustomEquals c1 = new CustomEquals(1);
        CustomEquals c2 = new CustomEquals(2);
        Map<String, Object> opts = new HashMap<>();
        // Empty set → ignore *all* custom equals
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertFalse(DeepEquals.deepEquals(c1, c2, opts)); // field mismatch (id)
    }

    @Test
    public void testDeepEqualsIgnoreSpecificCustomEquals() {
        CustomEquals c1 = new CustomEquals(1);
        CustomEquals c2 = new CustomEquals(1);
        Map<String, Object> opts = new HashMap<>();
        // Ignore only CustomEquals → field‑wise comparison
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(CustomEquals.class));
        assertTrue(DeepEquals.deepEquals(c1, c2, opts));
    }

    /* -------------------------------------------------
     *  Circular reference detection
     * ------------------------------------------------- */

    @Test
    public void testDeepEqualsCircularReference() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = b;
        b.next = a; // creates a cycle

        Node aCopy = new Node("A");
        Node bCopy = new Node("B");
        aCopy.next = bCopy;
        bCopy.next = aCopy;

        assertTrue(DeepEquals.deepEquals(a, aCopy));
    }

    /* -------------------------------------------------
     *  Array specific edge cases
     * ------------------------------------------------- */

    @Test
    public void testArrayDimensionMismatch() {
        int[] oneDim = new int[]{1, 2};
        int[][] twoDim = new int[][]{{1, 2}};
        assertFalse(DeepEquals.deepEquals(oneDim, twoDim));
    }

    @Test
    public void testArrayComponentTypeMismatch() {
        int[] ints = new int[]{1, 2};
        long[] longs = new long[]{1L, 2L};
        assertFalse(DeepEquals.deepEquals(ints, longs));
    }

    @Test
    public void testArrayLengthMismatch() {
        String[] a = new String[]{"x", "y"};
        String[] b = new String[]{"x", "y", "z"};
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testArrayElementMismatch() {
        String[] a = new String[]{"foo", "bar"};
        String[] b = new String[]{"foo", "baz"};
        assertFalse(DeepEquals.deepEquals(a, b));
        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(a, b, opts);
        assertNotNull(opts.get(DeepEquals.DIFF));
    }

    /* -------------------------------------------------
     *  Collection specific edge cases
     * ------------------------------------------------- */

    @Test
    public void testOrderedCollectionElementMismatch() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 4, 3);
        assertFalse(DeepEquals.deepEquals(l1, l2));
    }

    @Test
    public void testUnorderedCollectionMissingElement() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> s2 = new HashSet<>(Collections.singletonList("a"));
        assertFalse(DeepEquals.deepEquals(s1, s2));
    }

    @Test
    public void testOrderedCollectionSizeMismatch() {
        List<String> l1 = Arrays.asList("x", "y");
        List<String> l2 = Collections.singletonList("x");
        assertFalse(DeepEquals.deepEquals(l1, l2));
    }

    /* -------------------------------------------------
     *  Map specific edge cases
     * ------------------------------------------------- */

    @Test
    public void testMapMissingKey() {
        Map<String, Integer> m1 = Map.of("one", 1, "two", 2);
        Map<String, Integer> m2 = Map.of("one", 1);
        assertFalse(DeepEquals.deepEquals(m1, m2));
    }

    @Test
    public void testMapValueMismatch() {
        Map<String, String> m1 = Map.of("k", "v1");
        Map<String, String> m2 = Map.of("k", "v2");
        assertFalse(DeepEquals.deepEquals(m1, m2));
        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(m1, m2, opts);
        assertNotNull(opts.get(DeepEquals.DIFF));
    }

    @Test
    public void testMapTypeMismatch() {
        Map<String, Integer> m1 = Map.of("a", 1);
        List<Integer> notAMap = List.of(1);
        assertFalse(DeepEquals.deepEquals(m1, notAMap));
    }

    /* -------------------------------------------------
     *  deepHashCode verification
     * ------------------------------------------------- */

    @Test
    public void testDeepHashCodeNull() {
        assertEquals(0, DeepEquals.deepHashCode(null));
    }

    @Test
    public void testDeepHashCodeSimpleObject() {
        Person p1 = new Person("Bob", 40);
        Person p2 = new Person("Bob", 40);
        assertEquals(DeepEquals.deepHashCode(p1), DeepEquals.deepHashCode(p2));
    }

    @Test
    public void testDeepHashCodeArray() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        assertEquals(DeepEquals.deepHashCode(a1), DeepEquals.deepHashCode(a2));
    }

    @Test
    public void testDeepHashCodeCollection() {
        List<String> l1 = Arrays.asList("a", "b", "c");
        List<String> l2 = Arrays.asList("a", "b", "c");
        assertEquals(DeepEquals.deepHashCode(l1), DeepEquals.deepHashCode(l2));
    }

    @Test
    public void testDeepHashCodeMap() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("x", 10);
        m1.put("y", 20);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("x", 10);
        m2.put("y", 20);
        assertEquals(DeepEquals.deepHashCode(m1), DeepEquals.deepHashCode(m2));
    }

    @Test
    public void testDeepHashCodeWithCustomEqualsIgnored() {
        CustomEquals c1 = new CustomEquals(5);
        CustomEquals c2 = new CustomEquals(5);
        // The objects are logically equal; deepHashCode should be identical
        assertEquals(DeepEquals.deepHashCode(c1), DeepEquals.deepHashCode(c2));
    }

    /* -------------------------------------------------
     *  Additional type checks (URI, URL, AtomicBoolean, etc.)
     * ------------------------------------------------- */

    @Test
    public void testDeepEqualsUriAndUrl() throws Exception {
        URI uri1 = new URI("http://example.com");
        URI uri2 = new URI("http://example.com");
        assertTrue(DeepEquals.deepEquals(uri1, uri2));

        URL url1 = new URL("http://example.com");
        URL url2 = new URL("http://example.com");
        assertTrue(DeepEquals.deepEquals(url1, url2));
    }

    @Test
    public void testDeepEqualsAtomicBoolean() {
        AtomicBoolean a1 = new AtomicBoolean(true);
        AtomicBoolean a2 = new AtomicBoolean(true);
        AtomicBoolean a3 = new AtomicBoolean(false);
        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));
    }
}
