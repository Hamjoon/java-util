package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.*;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link DeepEquals}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic primitive / simple type equality</li>
 *   <li>type mismatches</li>
 *   <li>ordered and unordered collections</li>
 *   <li>maps (size, missing key, value mismatch)</li>
 *   <li>arrays (dimension, component type, length, element mismatch)</li>
 *   <li>floating‑point tolerance</li>
 *   <li>string‑to‑number matching (option enabled / disabled)</li>
 *   <li>custom {@code equals()} handling and the {@code IGNORE_CUSTOM_EQUALS} option</li>
 *   <li>custom {@code hashCode()} detection</li>
 *   <li>deep hash‑code consistency</li>
 *   <li>circular reference safety</li>
 *   <li>difference description generation (the {@code DIFF} entry)</li>
 * </ul>
 */
public class DeepEqualsTest {

    /* --------------------------------------------------------------------- */
    /*  Helper classes used by the tests                                      */
    /* --------------------------------------------------------------------- */

    /** Simple POJO without a custom equals() implementation. */
    static class Plain {
        int id;
        String name;

        Plain(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /** POJO that overrides {@code equals()} but does *not* compare the field {@code extra}. */
    static class WithCustomEquals {
        int id;
        String name;
        String extra;   // not considered by equals()

        WithCustomEquals(int id, String name, String extra) {
            this.id = id;
            this.name = name;
            this.extra = extra;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof WithCustomEquals)) return false;
            WithCustomEquals w = (WithCustomEquals) o;
            return id == w.id && Objects.equals(name, w.name);
        }
    }

    /** POJO that overrides {@code hashCode()}. */
    static class WithCustomHashCode {
        int id;
        String name;

        WithCustomHashCode(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public int hashCode() {
            return id * 31 + (name == null ? 0 : name.hashCode());
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    /** POJO that references another instance, used to test circular references. */
    static class Node {
        String value;
        Node next;

        Node(String value) {
            this.value = value;
        }
    }

    /* --------------------------------------------------------------------- */
    /*  1. Simple type equality                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSimpleEquality() {
        assertTrue(DeepEquals.deepEquals(null, null));
        assertTrue(DeepEquals.deepEquals("hello", "hello"));
        assertTrue(DeepEquals.deepEquals(42, 42));
        assertTrue(DeepEquals.deepEquals(BigDecimal.valueOf(12345), new BigDecimal("12345")));
        assertTrue(DeepEquals.deepEquals(true, true));
        assertTrue(DeepEquals.deepEquals(new Date(0L), new Date(0L)));
        assertTrue(DeepEquals.deepEquals(URI.create("http://example.com"), URI.create("http://example.com")));
        assertTrue(DeepEquals.deepEquals(URL.class, URL.class)); // Class objects are simple types
    }

    @Test
    public void testSimpleValueMismatch() {
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(5, 6, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("value mismatch"));
        assertTrue(diff.contains("Expected: 5"));
        assertTrue(diff.contains("Found: 6"));
    }

    @Test
    public void testSimpleTypeMismatch() {
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(5, "5", options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("type mismatch"));
    }

    /* --------------------------------------------------------------------- */
    /*  2. Ordered collection (List)                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testOrderedCollectionSizeMismatch() {
        List<Integer> a = Arrays.asList(1, 2);
        List<Integer> b = Arrays.asList(1, 2, 3);
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("collection size mismatch"));
    }

    @Test
    public void testOrderedCollectionElementMismatch() {
        List<String> a = Arrays.asList("alpha", "beta");
        List<String> b = Arrays.asList("alpha", "gamma");
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("collection element mismatch"));
        assertTrue(diff.contains("Expected: \"beta\""));
        assertTrue(diff.contains("Found: \"gamma\""));
    }

    /* --------------------------------------------------------------------- */
    /*  3. Unordered collection (Set)                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testUnorderedCollectionMissingElement() {
        Set<String> a = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> b = new HashSet<>(Arrays.asList("a", "c"));
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("missing collection element"));
        assertTrue(diff.contains("Expected: \"b\""));
    }

    /* --------------------------------------------------------------------- */
    /*  4. Map handling                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testMapSizeMismatch() {
        Map<String, Integer> a = new HashMap<>();
        a.put("x", 1);
        Map<String, Integer> b = new HashMap<>();
        b.put("x", 1);
        b.put("y", 2);
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("map size mismatch"));
    }

    @Test
    public void testMapMissingKey() {
        Map<String, String> a = new HashMap<>();
        a.put("k1", "v1");
        a.put("k2", "v2");
        Map<String, String> b = new HashMap<>();
        b.put("k1", "v1");
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("missing map key"));
        assertTrue(diff.contains("k2"));
    }

    @Test
    public void testMapValueMismatch() {
        Map<String, Integer> a = new HashMap<>();
        a.put("k", 10);
        Map<String, Integer> b = new HashMap<>();
        b.put("k", 20);
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("map value mismatch"));
        assertTrue(diff.contains("Expected: 10"));
        assertTrue(diff.contains("Found: 20"));
    }

    /* --------------------------------------------------------------------- */
    /*  5. Array handling                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testArrayDimensionMismatch() {
        int[][] a = new int[2][3];
        int[] b = new int[5];
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("array dimensionality mismatch"));
    }

    @Test
    public void testArrayComponentTypeMismatch() {
        int[] a = new int[]{1, 2};
        long[] b = new long[]{1L, 2L};
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("array component type mismatch"));
    }

    @Test
    public void testArrayLengthMismatch() {
        String[] a = {"a", "b"};
        String[] b = {"a", "b", "c"};
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("array length mismatch"));
    }

    @Test
    public void testArrayElementMismatch() {
        Double[] a = {1.0, 2.0, 3.0};
        Double[] b = {1.0, 9.0, 3.0};
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("array element mismatch"));
        assertTrue(diff.contains("Expected: 2.0"));
        assertTrue(diff.contains("Found: 9.0"));
    }

    @Test
    public void testMultiDimensionalArrayElementMismatch() {
        int[][] a = {{1, 2}, {3, 4}};
        int[][] b = {{1, 2}, {3, 9}};
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("array element mismatch"));
        assertTrue(diff.contains("Expected: 4"));
        assertTrue(diff.contains("Found: 9"));
    }

    /* --------------------------------------------------------------------- */
    /*  6. Floating‑point tolerance                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFloatingPointTolerance() {
        double a = 0.1 + 0.2;   // 0.30000000000000004
        double b = 0.3;
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    /* --------------------------------------------------------------------- */
    /*  7. String‑to‑Number matching                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testStringNumberMatchWhenEnabled() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("10", 10, options));
        assertTrue(DeepEquals.deepEquals(10, "10", options));
        assertTrue(DeepEquals.deepEquals("10.5", 10.5, options));
    }

    @Test
    public void testStringNumberMismatchWhenDisabled() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);
        assertFalse(DeepEquals.deepEquals("10", 10, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("value mismatch"));
    }

    /* --------------------------------------------------------------------- */
    /*  8. Custom equals handling                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCustomEqualsUsedByDefault() {
        WithCustomEquals a = new WithCustomEquals(1, "Alice", "extraA");
        WithCustomEquals b = new WithCustomEquals(1, "Alice", "extraB"); // extra differs
        // default behaviour uses the overridden equals() -> should be equal
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testIgnoreCustomEqualsOption() {
        WithCustomEquals a = new WithCustomEquals(1, "Alice", "extraA");
        WithCustomEquals b = new WithCustomEquals(1, "Alice", "extraB");
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.singleton(WithCustomEquals.class));
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("field value mismatch"));
        assertTrue(diff.contains("extra"));
    }

    @Test
    public void testHasCustomEqualsDetection() {
        assertTrue(DeepEquals.hasCustomEquals(WithCustomEquals.class));
        assertFalse(DeepEquals.hasCustomEquals(Plain.class));
    }

    /* --------------------------------------------------------------------- */
    /*  9. Custom hashCode detection                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testHasCustomHashCodeDetection() {
        assertTrue(DeepEquals.hasCustomHashCode(WithCustomHashCode.class));
        assertFalse(DeepEquals.hasCustomHashCode(Plain.class));
    }

    /* --------------------------------------------------------------------- */
    /* 10. deepHashCode consistency                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeEqualityForEqualObjects() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(1, 2, 3);
        assertTrue(DeepEquals.deepEquals(a, b));
        assertEquals(DeepEquals.deepHashCode(a), DeepEquals.deepHashCode(b));
    }

    @Test
    public void testDeepHashCodeDifferenceForUnequalObjects() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(1, 2, 4);
        assertFalse(DeepEquals.deepEquals(a, b));
        // Hash codes are *not* required to be identical for unequal objects,
        // but they are very likely different – we assert they are not equal.
        assertNotEquals(DeepEquals.deepHashCode(a), DeepEquals.deepHashCode(b));
    }

    /* --------------------------------------------------------------------- */
    /* 11. Circular reference handling                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCircularReferenceEquality() {
        Node n1 = new Node("first");
        Node n2 = new Node("second");
        n1.next = n2;
        n2.next = n1; // cycle

        Node m1 = new Node("first");
        Node m2 = new Node("second");
        m1.next = m2;
        m2.next = m1; // same shape

        assertTrue(DeepEquals.deepEquals(n1, m1));
    }

    @Test
    public void testCircularReferenceInequality() {
        Node n1 = new Node("first");
        Node n2 = new Node("second");
        n1.next = n2;
        n2.next = n1; // cycle

        Node m1 = new Node("first");
        Node m2 = new Node("different");
        m1.next = m2;
        m2.next = m1; // same shape but different leaf value

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(n1, m1, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("field value mismatch"));
        assertTrue(diff.contains("value"));
    }

    /* --------------------------------------------------------------------- */
    /* 12. Deep equals with nulls and mixed containers                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNullsInContainers() {
        List<String> a = Arrays.asList("x", null, "z");
        List<String> b = Arrays.asList("x", null, "z");
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testMixedContainerInequality() {
        List<Integer> list = Arrays.asList(1, 2);
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2));
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(list, set, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("type mismatch"));
    }

    /* --------------------------------------------------------------------- */
    /* 13. Verify that the DIFF entry is populated only on failure            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDiffOnlyOnFailure() {
        Map<String, Object> options = new HashMap<>();
        assertTrue(DeepEquals.deepEquals(123, 123, options));
        assertNull(options.get(DeepEquals.DIFF));

        assertFalse(DeepEquals.deepEquals(123, 124, options));
        assertNotNull(options.get(DeepEquals.DIFF));
    }
}
