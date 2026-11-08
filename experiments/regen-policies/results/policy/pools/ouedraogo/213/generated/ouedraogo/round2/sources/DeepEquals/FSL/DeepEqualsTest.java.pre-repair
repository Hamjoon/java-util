package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive JUnit‑4 test suite for {@link DeepEquals}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic primitive and object equality.</li>
 *   <li>Numeric comparison (including floating‑point tolerance).</li>
 *   <li>String‑to‑Number matching (option enabled / disabled).</li>
 *   <li>Ordered and unordered collections.</li>
 *   <li>Maps (size, missing key, value mismatch).</li>
 *   <li>Arrays (dimensionality, component type, length, element).</li>
 *   <li>Custom {@code equals()} handling with {@code IGNORE_CUSTOM_EQUALS} option.</li>
 *   <li>Circular references.</li>
 *   <li>deepHashCode consistency.</li>
 *   <li>hasCustomEquals / hasCustomHashCode detection.</li>
 * </ul>
 */
public class DeepEqualsTest {

    /* ---------------------------------------------------------------------- */
    /* Helper classes for custom‑equals / custom‑hashCode scenarios           */
    /* ---------------------------------------------------------------------- */

    /** Simple POJO with a custom {@code equals()} that only checks {@code id}. */
    static final class CustomEquals {
        int id;
        String data;

        CustomEquals(int id, String data) {
            this.id = id;
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CustomEquals)) return false;
            CustomEquals that = (CustomEquals) o;
            return this.id == that.id;                 // deliberately ignore {@code data}
        }
    }

    /** Simple POJO that overrides {@code hashCode()} only. */
    static final class CustomHash {
        int id;
        String data;

        CustomHash(int id, String data) {
            this.id = id;
            this.data = data;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, data);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CustomHash)) return false;
            CustomHash that = (CustomHash) o;
            return this.id == that.id && Objects.equals(this.data, that.data);
        }
    }

    /** Node class used to build circular reference graphs. */
    static final class Node {
        int value;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 1. Primitive / simple object equality                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSimpleEqualityPrimitives() {
        Assert.assertTrue(DeepEquals.deepEquals(42, 42));
        Assert.assertTrue(DeepEquals.deepEquals("hello", "hello"));
        Assert.assertTrue(DeepEquals.deepEquals(true, true));
        Assert.assertTrue(DeepEquals.deepEquals(null, null));
    }

    @Test
    public void testSimpleInequalityProducesDiff() {
        Map<String, Object> opts = new HashMap<>();
        boolean eq = DeepEquals.deepEquals(4, 5, opts);
        Assert.assertFalse(eq);
        String diff = (String) opts.get(DeepEquals.DIFF);
        Assert.assertNotNull(diff);
        Assert.assertTrue(diff.contains("value mismatch"));
    }

    /* ---------------------------------------------------------------------- */
    /* 2. Numeric comparison (including floating‑point tolerance)            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFloatingPointTolerance() {
        double a = 0.1 + 0.2;      // ≈ 0.30000000000000004
        double b = 0.3;
        Assert.assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testBigDecimalComparison() {
        BigDecimal bd1 = new BigDecimal("123.4500");
        BigDecimal bd2 = new BigDecimal("123.45");
        Assert.assertTrue(DeepEquals.deepEquals(bd1, bd2));
    }

    /* ---------------------------------------------------------------------- */
    /* 3. String‑to‑Number matching                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testStringNumberMatchingEnabled() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        Assert.assertTrue(DeepEquals.deepEquals("10", 10, opts));
        Assert.assertTrue(DeepEquals.deepEquals(10L, "10", opts));
    }

    @Test
    public void testStringNumberMatchingDisabled() {
        Map<String, Object> opts = new HashMap<>();
        // option not set → defaults to false
        Assert.assertFalse(DeepEquals.deepEquals("10", 10, opts));
    }

    /* ---------------------------------------------------------------------- */
    /* 4. Ordered collection (List)                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testOrderedCollectionEquality() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        Assert.assertTrue(DeepEquals.deepEquals(l1, l2));
    }

    @Test
    public void testOrderedCollectionElementMismatch() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 4, 3);
        Map<String, Object> opts = new HashMap<>();
        Assert.assertFalse(DeepEquals.deepEquals(l1, l2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        Assert.assertNotNull(diff);
        Assert.assertTrue(diff.contains("collection element mismatch"));
    }

    @Test
    public void testOrderedCollectionSizeMismatch() {
        List<Integer> l1 = Arrays.asList(1, 2);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        Map<String, Object> opts = new HashMap<>();
        Assert.assertFalse(DeepEquals.deepEquals(l1, l2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        Assert.assertTrue(diff.contains("collection size mismatch"));
    }

    /* ---------------------------------------------------------------------- */
    /* 5. Unordered collection (Set)                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testUnorderedCollectionEquality() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("c", "b", "a"));
        Assert.assertTrue(DeepEquals.deepEquals(s1, s2));
    }

    @Test
    public void testUnorderedCollectionMissingElement() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> s2 = new HashSet<>(Collections.singleton("a"));
        Map<String, Object> opts = new HashMap<>();
        Assert.assertFalse(DeepEquals.deepEquals(s1, s2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        Assert.assertTrue(diff.contains("missing collection element"));
    }

    /* ---------------------------------------------------------------------- */
    /* 6. Map comparison                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMapEquality() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("x", 1);
        m1.put("y", 2);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("y", 2);
        m2.put("x", 1);
        Assert.assertTrue(DeepEquals.deepEquals(m1, m2));
    }

    @Test
    public void testMapMissingKey() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("a", 1);
        Map<String, Object> opts = new HashMap<>();
        Assert.assertFalse(DeepEquals.deepEquals(m1, m2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        Assert.assertTrue(diff.contains("missing map key"));
    }

    @Test
    public void testMapValueMismatch() {
        Map<String, String> m1 = new HashMap<>();
        m1.put("k", "v1");
        Map<String, String> m2 = new HashMap<>();
        m2.put("k", "v2");
        Map<String, Object> opts = new HashMap<>();
        Assert.assertFalse(DeepEquals.deepEquals(m1, m2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        Assert.assertTrue(diff.contains("map value mismatch"));
    }

    @Test
    public void testMapSizeMismatch() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("a", 1);
        m2.put("b", 2);
        Map<String, Object> opts = new HashMap<>();
        Assert.assertFalse(DeepEquals.deepEquals(m1, m2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        Assert.assertTrue(diff.contains("map size mismatch"));
    }

    /* ---------------------------------------------------------------------- */
    /* 7. Array comparison (including multi‑dimensional)                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testArrayEquality() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        Assert.assertTrue(DeepEquals.deepEquals(a1, a2));
    }

    @Test
    public void testArrayLengthMismatch() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2};
        Map<String, Object> opts = new HashMap<>();
        Assert.assertFalse(DeepEquals.deepEquals(a1, a2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        Assert.assertTrue(diff.contains("array length mismatch"));
    }

    @Test
    public void testArrayComponentTypeMismatch() {
        int[] a1 = {1, 2, 3};
        long[] a2 = {1L, 2L, 3L};
        Map<String, Object> opts = new HashMap<>();
        Assert.assertFalse(DeepEquals.deepEquals(a1, a2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        Assert.assertTrue(diff.contains("array component type mismatch"));
    }

    @Test
    public void testMultiDimensionalArrayEquality() {
        int[][] a1 = {{1, 2}, {3, 4}};
        int[][] a2 = {{1, 2}, {3, 4}};
        Assert.assertTrue(DeepEquals.deepEquals(a1, a2));
    }

    @Test
    public void testMultiDimensionalArrayElementMismatch() {
        int[][] a1 = {{1, 2}, {3, 4}};
        int[][] a2 = {{1, 2}, {3, 5}}; // last element differs
        Map<String, Object> opts = new HashMap<>();
        Assert.assertFalse(DeepEquals.deepEquals(a1, a2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        Assert.assertTrue(diff.contains("array element mismatch"));
    }

    /* ---------------------------------------------------------------------- */
    /* 8. Custom equals handling via IGNORE_CUSTOM_EQUALS option                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCustomEqualsUsedByDefault() {
        CustomEquals c1 = new CustomEquals(1, "foo");
        CustomEquals c2 = new CustomEquals(1, "bar"); // different {@code data}
        // default: custom equals is respected → objects are considered equal
        Assert.assertTrue(DeepEquals.deepEquals(c1, c2));
    }

    @Test
    public void testIgnoreAllCustomEquals() {
        CustomEquals c1 = new CustomEquals(1, "foo");
        CustomEquals c2 = new CustomEquals(1, "bar");
        Map<String, Object> opts = new HashMap<>();
        // Empty set → ignore **all** custom equals implementations
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        Assert.assertFalse(DeepEquals.deepEquals(c1, c2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        Assert.assertTrue(diff.contains("field value mismatch"));
    }

    @Test
    public void testIgnoreSpecificCustomEquals() {
        CustomEquals c1 = new CustomEquals(1, "foo");
        CustomEquals c2 = new CustomEquals(1, "bar");
        Map<String, Object> opts = new HashMap<>();
        // Ignore only CustomEquals → field‑by‑field comparison should run
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.singleton(CustomEquals.class));
        Assert.assertFalse(DeepEquals.deepEquals(c1, c2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        Assert.assertTrue(diff.contains("field value mismatch"));
    }

    /* ---------------------------------------------------------------------- */
    /* 9. Circular reference handling                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCircularReferenceEquality() {
        Node n1 = new Node(5);
        n1.next = n1; // self‑reference

        Node n2 = new Node(5);
        n2.next = n2; // self‑reference

        Assert.assertTrue(DeepEquals.deepEquals(n1, n2));
    }

    @Test
    public void testCircularReferenceInequality() {
        Node n1 = new Node(5);
        n1.next = n1;

        Node n2 = new Node(6); // different value
        n2.next = n2;

        Map<String, Object> opts = new HashMap<>();
        Assert.assertFalse(DeepEquals.deepEquals(n1, n2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        Assert.assertTrue(diff.contains("field value mismatch"));
    }

    /* ---------------------------------------------------------------------- */
    /* 10. deepHashCode consistency                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeConsistentForEqualObjects() {
        Map<String, List<Integer>> m1 = new HashMap<>();
        m1.put("numbers", Arrays.asList(1, 2, 3));

        Map<String, List<Integer>> m2 = new HashMap<>();
        m2.put("numbers", Arrays.asList(1, 2, 3));

        Assert.assertTrue(DeepEquals.deepEquals(m1, m2));
        int h1 = DeepEquals.deepHashCode(m1);
        int h2 = DeepEquals.deepHashCode(m2);
        Assert.assertEquals(h1, h2);
    }

    @Test
    public void testDeepHashCodeDiffersWhenObjectsDiffer() {
        Map<String, List<Integer>> m1 = new HashMap<>();
        m1.put("numbers", Arrays.asList(1, 2, 3));

        Map<String, List<Integer>> m2 = new HashMap<>();
        m2.put("numbers", Arrays.asList(1, 2, 4)); // last element differs

        Assert.assertFalse(DeepEquals.deepEquals(m1, m2));
        int h1 = DeepEquals.deepHashCode(m1);
        int h2 = DeepEquals.deepHashCode(m2);
        Assert.assertNotEquals(h1, h2);
    }

    /* ---------------------------------------------------------------------- */
    /* 11. hasCustomEquals / hasCustomHashCode detection                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHasCustomEqualsDetection() {
        // String overrides equals → should be true
        Assert.assertTrue(DeepEquals.hasCustomEquals(String.class));

        // Integer also overrides equals
        Assert.assertTrue(DeepEquals.hasCustomEquals(Integer.class));

        // Custom class without overriding equals → false
        class NoEquals { }
        Assert.assertFalse(DeepEquals.hasCustomEquals(NoEquals.class));

        // Our own class with custom equals → true
        Assert.assertTrue(DeepEquals.hasCustomEquals(CustomEquals.class));
    }

    @Test
    public void testHasCustomHashCodeDetection() {
        // String overrides hashCode → true
        Assert.assertTrue(DeepEquals.hasCustomHashCode(String.class));

        // Integer overrides hashCode → true
        Assert.assertTrue(DeepEquals.hasCustomHashCode(Integer.class));

        // Class without overriding hashCode → false
        class NoHash { }
        Assert.assertFalse(DeepEquals.hasCustomHashCode(NoHash.class));

        // Our own class with custom hashCode → true
        Assert.assertTrue(DeepEquals.hasCustomHashCode(CustomHash.class));
    }

    /* ---------------------------------------------------------------------- */
    /* 12. Additional edge cases (Atomic types, URI, URL, UUID)               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAtomicTypesEquality() {
        AtomicBoolean ab1 = new AtomicBoolean(true);
        AtomicBoolean ab2 = new AtomicBoolean(true);
        Assert.assertTrue(DeepEquals.deepEquals(ab1, ab2));

        AtomicInteger ai1 = new AtomicInteger(7);
        AtomicInteger ai2 = new AtomicInteger(7);
        Assert.assertTrue(DeepEquals.deepEquals(ai1, ai2));

        AtomicLong al1 = new AtomicLong(123L);
        AtomicLong al2 = new AtomicLong(123L);
        Assert.assertTrue(DeepEquals.deepEquals(al1, al2));
    }

    @Test
    public void testURIAndURLEquality() throws Exception {
        URI uri1 = new URI("http://example.com/path");
        URI uri2 = new URI("http://example.com/path");
        Assert.assertTrue(DeepEquals.deepEquals(uri1, uri2));

        URL url1 = new URL("http://example.com/");
        URL url2 = new URL("http://example.com/");
        Assert.assertTrue(DeepEquals.deepEquals(url1, url2));
    }

    @Test
    public void testUUIDEquality() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.fromString(u1.toString());
        Assert.assertTrue(DeepEquals.deepEquals(u1, u2));
    }

    /* ---------------------------------------------------------------------- */
    /* 13. Verify that the diff description contains the expected path      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDiffPathContainsFieldName() {
        class Person {
            String name;
            int age;

            Person(String name, int age) {
                this.name = name;
                this.age = age;
            }
        }

        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 31); // age differs

        Map<String, Object> opts = new HashMap<>();
        Assert.assertFalse(DeepEquals.deepEquals(p1, p2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        Assert.assertNotNull(diff);
        // The path should include ".age"
        Assert.assertTrue(diff.contains(".age"));
    }
}
