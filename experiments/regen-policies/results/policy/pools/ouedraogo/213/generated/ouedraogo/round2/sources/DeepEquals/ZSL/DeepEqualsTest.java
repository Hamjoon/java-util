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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive JUnit4 test suite for {@link DeepEquals}.
 * Covers:
 * <ul>
 *   <li>Simple value comparisons</li>
 *   <li>Number tolerance (float/double)</li>
 *   <li>String‑to‑Number matching option</li>
 *   <li>Arrays (including multi‑dimensional)</li>
 *   <li>Ordered vs unordered collections</li>
 *   <li>Maps</li>
 *   <li>Custom {@code equals}/{@code hashCode} handling and ignore options</li>
 *   <li>Atomic types</li>
 *   <li>Circular references</li>
 *   <li>deepHashCode consistency</li>
 * </ul>
 */
public class DeepEqualsTest {

    private Map<String, Object> options;

    @Before
    public void setUp() {
        options = new HashMap<>();
    }

    // ----------------------------------------------------------------------
    // Simple scalar comparisons
    // ----------------------------------------------------------------------
    @Test
    public void testSimpleScalars() {
        assertTrue(DeepEquals.deepEquals(null, null));
        assertTrue(DeepEquals.deepEquals("abc", "abc"));
        assertTrue(DeepEquals.deepEquals(42, 42));
        assertFalse(DeepEquals.deepEquals("abc", "def"));
        assertFalse(DeepEquals.deepEquals(42, 43));
        assertTrue(DeepEquals.deepEquals(true, true));
        assertFalse(DeepEquals.deepEquals(true, false));
    }

    // ----------------------------------------------------------------------
    // Number tolerance (floating point)
    // ----------------------------------------------------------------------
    @Test
    public void testFloatingPointTolerance() {
        // 0.1 + 0.2 is not exactly 0.3, but DeepEquals should treat them as equal
        double a = 0.1 + 0.2;
        double b = 0.3;
        assertTrue(DeepEquals.deepEquals(a, b));

        // Very small difference beyond epsilon should be false
        double c = 1.000000000000001;
        double d = 1.0;
        assertFalse(DeepEquals.deepEquals(c, d));
    }

    // ----------------------------------------------------------------------
    // String‑to‑Number matching option
    // ----------------------------------------------------------------------
    @Test
    public void testStringNumberMatchingOption() {
        // Without option – should be false
        assertFalse(DeepEquals.deepEquals("10", 10));

        // Enable option – should be true
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.TRUE);
        assertTrue(DeepEquals.deepEquals("10", 10, options));

        // Ensure mismatch still reported correctly
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.TRUE);
        assertFalse(DeepEquals.deepEquals("10", 11, options));
        assertEquals("[value mismatch] ▶ null", ((Map<String, Object>)options).get(DeepEquals.DIFF).toString().substring(0, 27));
    }

    // ----------------------------------------------------------------------
    // Atomic types
    // ----------------------------------------------------------------------
    @Test
    public void testAtomicBooleanComparison() {
        AtomicBoolean a = new AtomicBoolean(true);
        AtomicBoolean b = new AtomicBoolean(true);
        AtomicBoolean c = new AtomicBoolean(false);
        assertTrue(DeepEquals.deepEquals(a, b));
        assertFalse(DeepEquals.deepEquals(a, c));
    }

    // ----------------------------------------------------------------------
    // Array comparisons
    // ----------------------------------------------------------------------
    @Test
    public void testPrimitiveArrayEquality() {
        int[] arr1 = {1, 2, 3};
        int[] arr2 = {1, 2, 3};
        int[] arr3 = {1, 2, 4};

        assertTrue(DeepEquals.deepEquals(arr1, arr2));
        assertFalse(DeepEquals.deepEquals(arr1, arr3));

        // Verify diff contains array element mismatch description
        assertFalse(DeepEquals.deepEquals(arr1, arr3, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("array element mismatch"));
    }

    @Test
    public void testMultiDimensionalArrayDimensionMismatch() {
        int[][] a = {{1, 2}, {3, 4}};
        int[][][] b = {{{1, 2}, {3, 4}}};

        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("array dimensionality mismatch"));
    }

    // ----------------------------------------------------------------------
    // Ordered collections (List)
    // ----------------------------------------------------------------------
    @Test
    public void testListOrderMatters() {
        List<String> l1 = Arrays.asList("a", "b", "c");
        List<String> l2 = Arrays.asList("a", "b", "c");
        List<String> l3 = Arrays.asList("c", "b", "a");

        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));

        // Verify diff mentions collection element mismatch
        assertFalse(DeepEquals.deepEquals(l1, l3, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("collection element mismatch"));
    }

    // ----------------------------------------------------------------------
    // Unordered collections (Set)
    // ----------------------------------------------------------------------
    @Test
    public void testSetEquality() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 2, 1));
        Set<Integer> s3 = new HashSet<>(Arrays.asList(1, 2, 4));

        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));

        // Diff should reference missing element
        assertFalse(DeepEquals.deepEquals(s1, s3, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("missing collection element"));
    }

    // ----------------------------------------------------------------------
    // Map comparisons
    // ----------------------------------------------------------------------
    @Test
    public void testMapEqualityAndDiff() {
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("one", 1);
        m2.put("two", 2);

        Map<String, Integer> m3 = new LinkedHashMap<>();
        m3.put("one", 1);
        m3.put("two", 22); // different value

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));

        // Capture diff for value mismatch
        assertFalse(DeepEquals.deepEquals(m1, m3, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("map value mismatch"));
    }

    // ----------------------------------------------------------------------
    // Custom equals handling
    // ----------------------------------------------------------------------
    static class CustomEquals {
        int id;
        String name;

        CustomEquals(int id, String name) {
            this.id = id;
            this.name = name;
        }

        // Custom equals only checks id
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CustomEquals)) return false;
            return this.id == ((CustomEquals) o).id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    @Test
    public void testCustomEqualsDefaultBehaviour() {
        CustomEquals a = new CustomEquals(1, "Alice");
        CustomEquals b = new CustomEquals(1, "Bob"); // same id, different name

        // Default uses custom equals -> should be true
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testIgnoreAllCustomEquals() {
        CustomEquals a = new CustomEquals(1, "Alice");
        CustomEquals b = new CustomEquals(1, "Bob"); // name differs

        // Empty set -> ignore ALL custom equals, compare fields
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());

        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("field value mismatch"));
    }

    @Test
    public void testIgnoreSpecificCustomEquals() {
        CustomEquals a = new CustomEquals(1, "Alice");
        CustomEquals b = new CustomEquals(1, "Bob");

        // Ignore only CustomEquals class
        Set<Class<?>> ignoreSet = new HashSet<>();
        ignoreSet.add(CustomEquals.class);
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignoreSet);

        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("field value mismatch"));
    }

    // ----------------------------------------------------------------------
    // hasCustomEquals / hasCustomHashCode
    // ----------------------------------------------------------------------
    @Test
    public void testHasCustomEqualsAndHashCodeDetection() {
        assertTrue(DeepEquals.hasCustomEquals(CustomEquals.class));
        assertTrue(DeepEquals.hasCustomHashCode(CustomEquals.class));

        // A plain class without overrides
        class Plain { }
        assertFalse(DeepEquals.hasCustomEquals(Plain.class));
        assertFalse(DeepEquals.hasCustomHashCode(Plain.class));
    }

    // ----------------------------------------------------------------------
    // Circular reference handling
    // ----------------------------------------------------------------------
    static class Node {
        String name;
        Node next;

        Node(String name) { this.name = name; }
    }

    @Test
    public void testCircularReferenceEquality() {
        Node n1 = new Node("A");
        Node n2 = new Node("B");
        n1.next = n2;
        n2.next = n1; // cycle

        Node m1 = new Node("A");
        Node m2 = new Node("B");
        m1.next = m2;
        m2.next = m1; // identical structure

        assertTrue(DeepEquals.deepEquals(n1, m1));
    }

    @Test
    public void testCircularReferenceDifference() {
        Node n1 = new Node("A");
        Node n2 = new Node("B");
        n1.next = n2;
        n2.next = n1;

        Node m1 = new Node("A");
        Node m2 = new Node("C"); // different name
        m1.next = m2;
        m2.next = m1;

        assertFalse(DeepEquals.deepEquals(n1, m1, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("field value mismatch"));
    }

    // ----------------------------------------------------------------------
    // deepHashCode consistency
    // ----------------------------------------------------------------------
    @Test
    public void testDeepHashCodeConsistentForEqualObjects() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        assertTrue(DeepEquals.deepEquals(list1, list2));

        int h1 = DeepEquals.deepHashCode(list1);
        int h2 = DeepEquals.deepHashCode(list2);
        assertEquals(h1, h2);
    }

    @Test
    public void testDeepHashCodeDifferentForDifferentObjects() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 4);
        assertFalse(DeepEquals.deepEquals(list1, list2));

        int h1 = DeepEquals.deepHashCode(list1);
        int h2 = DeepEquals.deepHashCode(list2);
        // Not guaranteed to be different, but very likely for these small lists
        assertNotEquals(h1, h2);
    }

    // ----------------------------------------------------------------------
    // Additional type handling (Date, URI, URL, UUID, BigDecimal)
    // ----------------------------------------------------------------------
    @Test
    public void testVariousSimpleTypes() throws Exception {
        Date d1 = new SimpleDateFormat("yyyy-MM-dd").parse("2020-01-01");
        Date d2 = new SimpleDateFormat("yyyy-MM-dd").parse("2020-01-01");
        assertTrue(DeepEquals.deepEquals(d1, d2));

        URI uri1 = new URI("http://example.com");
        URI uri2 = new URI("http://example.com");
        assertTrue(DeepEquals.deepEquals(uri1, uri2));

        URL url1 = new URL("http://example.com");
        URL url2 = new URL("http://example.com");
        assertTrue(DeepEquals.deepEquals(url1, url2));

        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.fromString(u1.toString());
        assertTrue(DeepEquals.deepEquals(u1, u2));

        BigDecimal bd1 = new BigDecimal("123.4500");
        BigDecimal bd2 = new BigDecimal("123.45");
        assertTrue(DeepEquals.deepEquals(bd1, bd2));
    }

    // ----------------------------------------------------------------------
    // Verify that deepEquals with empty options behaves like the no‑arg version
    // ----------------------------------------------------------------------
    @Test
    public void testDeepEqualsNoOptionsEquivalentToEmptyMap() {
        String s1 = "test";
        String s2 = new String("test");
        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertTrue(DeepEquals.deepEquals(s1, s2, new HashMap<>()));
    }

    // ----------------------------------------------------------------------
    // Verify that deepEquals correctly reports container size mismatches
    // ----------------------------------------------------------------------
    @Test
    public void testCollectionSizeMismatchDiff() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(1, 2);
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("collection size mismatch"));
    }

    @Test
    public void testMapSizeMismatchDiff() {
        Map<String, Integer> a = new HashMap<>();
        a.put("x", 1);
        a.put("y", 2);
        Map<String, Integer> b = new HashMap<>();
        b.put("x", 1);
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("map size mismatch"));
    }
}
