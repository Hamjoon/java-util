package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DeepEquals}.
 *
 * The tests are grouped into three sections:
 *   1.  Public API sanity checks (basic functionality).
 *   2.  Edge‑case / option handling (nulls, numbers, collections, maps, arrays,
 *       circular references, custom equals handling, etc.).
 *   3.  {@code deepHashCode} consistency checks.
 *
 * Every test method follows the *given‑when‑then* pattern and uses only the
 * classes, constructors and methods that appear in the supplied source code.
 */
public class DeepEqualsTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper classes used only by the tests                                 */
    /* ---------------------------------------------------------------------- */

    /** Simple POJO – no custom {@code equals} / {@code hashCode}. */
    private static class Simple {
        int i;
        String s;

        Simple(int i, String s) {
            this.i = i;
            this.s = s;
        }
    }

    /** POJO with a custom {@code equals} that only checks {@code name}. */
    private static class Person {
        int id;
        String name;
        int age;          // extra field not used by {@code equals}

        Person(int id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Person)) return false;
            Person p = (Person) o;
            // custom equals deliberately ignores {@code age}
            return Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    /** Simple node that can form a circular reference. */
    private static class Node {
        String value;
        Node next;

        Node(String value) {
            this.value = value;
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  1.  Public API basic tests                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsSimplePrimitives() {
        assertTrue(DeepEquals.deepEquals(5, 5));
        assertFalse(DeepEquals.deepEquals(5, 6));
    }

    @Test
    public void testDeepEqualsNulls() {
        assertTrue(DeepEquals.deepEquals(null, null));

        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(null, "not null", opts));
        assertNotNull(opts.get(DeepEquals.DIFF));
    }

    @Test
    public void testDeepEqualsWithOptionsMapIsReturned() {
        Map<String, Object> options = new HashMap<>();
        boolean result = DeepEquals.deepEquals("a", "b", options);
        assertFalse(result);
        assertTrue(options.containsKey(DeepEquals.DIFF));
        assertTrue(((String) options.get(DeepEquals.DIFF)).contains("value mismatch"));
    }

    @Test
    public void testHasCustomEqualsAndHashCode() {
        assertTrue(DeepEquals.hasCustomEquals(Person.class));
        assertTrue(DeepEquals.hasCustomHashCode(Person.class));

        assertFalse(DeepEquals.hasCustomEquals(Simple.class));
        assertFalse(DeepEquals.hasCustomHashCode(Simple.class));
    }

    @Test
    public void testDeepHashCodeConsistentForEqualObjects() {
        Simple a = new Simple(10, "x");
        Simple b = new Simple(10, "x");
        assertTrue(DeepEquals.deepEquals(a, b));
        assertEquals(DeepEquals.deepHashCode(a), DeepEquals.deepHashCode(b));
    }

    /* ---------------------------------------------------------------------- */
    /*  2.  Edge‑case / option handling tests                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNumberComparisonExact() {
        assertTrue(DeepEquals.deepEquals(10, 10L));               // int vs long
        assertTrue(DeepEquals.deepEquals(new BigDecimal("10"), 10)); // BigDecimal vs int
    }

    @Test
    public void testFloatingPointTolerance() {
        double a = 1.0;
        double b = 1.0 + 1e-16;   // within epsilon (1e-15)
        double c = 1.0 + 1e-14;   // outside epsilon

        assertTrue(DeepEquals.deepEquals(a, b));
        assertFalse(DeepEquals.deepEquals(a, c));
    }

    @Test
    public void testStringNumberMatchingOption() {
        Map<String, Object> opts = new HashMap<>();
        // default = false
        assertFalse(DeepEquals.deepEquals("10", 10, opts));

        // enable option
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("10", 10, opts));

        // mismatched string should still fail
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertFalse(DeepEquals.deepEquals("10a", 10, opts));
    }

    @Test
    public void testOrderedCollectionMismatch() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 3, 2);
        assertFalse(DeepEquals.deepEquals(l1, l2));
    }

    @Test
    public void testUnorderedCollectionEquality() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> s2 = new HashSet<>(Arrays.asList("b", "a"));
        assertTrue(DeepEquals.deepEquals(s1, s2));
    }

    @Test
    public void testCollectionSizeMismatch() {
        List<Integer> l1 = Arrays.asList(1, 2);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(l1, l2, opts));
        assertTrue(((String) opts.get(DeepEquals.DIFF)).contains("size mismatch"));
    }

    @Test
    public void testMapValueMismatch() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("k", 1);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("k", 2);
        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m1, m2, opts));
        assertTrue(((String) opts.get(DeepEquals.DIFF)).contains("map value mismatch"));
    }

    @Test
    public void testMapMissingKey() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("a", 1);
        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m1, m2, opts));
        assertTrue(((String) opts.get(DeepEquals.DIFF)).contains("missing map key"));
    }

    @Test
    public void testArrayDimensionMismatch() {
        int[] a1 = {1, 2};
        int[][] a2 = {{1, 2}};
        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a1, a2, opts));
        assertTrue(((String) opts.get(DeepEquals.DIFF)).contains("array dimensionality mismatch"));
    }

    @Test
    public void testArrayComponentTypeMismatch() {
        int[] i = {1, 2};
        long[] l = {1L, 2L};
        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(i, l, opts));
        assertTrue(((String) opts.get(DeepEquals.DIFF)).contains("array component type mismatch"));
    }

    @Test
    public void testArrayLengthMismatch() {
        int[] a = {1, 2, 3};
        int[] b = {1, 2};
        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, opts));
        assertTrue(((String) opts.get(DeepEquals.DIFF)).contains("array length mismatch"));
    }

    @Test
    public void testCircularReferenceEquality() {
        Node n1 = new Node("root");
        Node n2 = new Node("root");
        n1.next = n1;  // self‑reference
        n2.next = n2;  // self‑reference

        assertTrue(DeepEquals.deepEquals(n1, n2));
    }

    @Test
    public void testCircularReferenceInequality() {
        Node n1 = new Node("root1");
        Node n2 = new Node("root2");
        n1.next = n1;
        n2.next = n2;
        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(n1, n2, opts));
        assertTrue(((String) opts.get(DeepEquals.DIFF)).contains("value mismatch"));
    }

    @Test
    public void testCustomEqualsUsedByDefault() {
        Person p1 = new Person(1, "John", 30);
        Person p2 = new Person(2, "John", 40); // same name, different id/age
        // default behaviour -> custom equals (only name) -> should be equal
        assertTrue(DeepEquals.deepEquals(p1, p2));
    }

    @Test
    public void testIgnoreAllCustomEquals() {
        // Empty set => ignore ALL custom equals implementations
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());

        Person p1 = new Person(1, "John", 30);
        Person p2 = new Person(2, "John", 40);
        // field‑by‑field comparison will see id/age differ -> not equal
        assertFalse(DeepEquals.deepEquals(p1, p2, opts));
        assertTrue(((String) opts.get(DeepEquals.DIFF)).contains("field value mismatch"));
    }

    @Test
    public void testIgnoreSpecificCustomEquals() {
        // Ignore only Person.class custom equals
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(Person.class));

        Person p1 = new Person(1, "John", 30);
        Person p2 = new Person(2, "John", 40);
        // Since Person is ignored, DeepEquals will compare fields -> mismatch
        assertFalse(DeepEquals.deepEquals(p1, p2, opts));
        assertTrue(((String) opts.get(DeepEquals.DIFF)).contains("field value mismatch"));
    }

    @Test
    public void testAtomicBooleanComparison() {
        AtomicBoolean a1 = new AtomicBoolean(true);
        AtomicBoolean a2 = new AtomicBoolean(true);
        AtomicBoolean a3 = new AtomicBoolean(false);
        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    @Test
    public void testSimpleTypesAreComparedCorrectly() {
        assertTrue(DeepEquals.deepEquals(new Date(0L), new Date(0L)));
        assertFalse(DeepEquals.deepEquals(new Date(0L), new Date(1L)));

        URI u1 = URI.create("http://example.com");
        URI u2 = URI.create("http://example.com");
        URI u3 = URI.create("http://example.org");
        assertTrue(DeepEquals.deepEquals(u1, u2));
        assertFalse(DeepEquals.deepEquals(u1, u3));
    }

    /* ---------------------------------------------------------------------- */
    /*  3.  deepHashCode consistency tests                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeForCollections() {
        List<String> l1 = Arrays.asList("a", "b", "c");
        List<String> l2 = Arrays.asList("a", "b", "c");
        assertEquals(DeepEquals.deepHashCode(l1), DeepEquals.deepHashCode(l2));
    }

    @Test
    public void testDeepHashCodeDiffersWhenObjectsDiffer() {
        List<String> l1 = Arrays.asList("a", "b");
        List<String> l2 = Arrays.asList("a", "c");
        assertNotEquals(DeepEquals.deepHashCode(l1), DeepEquals.deepHashCode(l2));
    }

    @Test
    public void testDeepHashCodeHandlesCircularReferences() {
        Node n1 = new Node("c1");
        Node n2 = new Node("c1");
        n1.next = n1;
        n2.next = n2;
        // Should not throw StackOverflowError and should give deterministic result
        int h1 = DeepEquals.deepHashCode(n1);
        int h2 = DeepEquals.deepHashCode(n2);
        assertEquals(h1, h2);
    }

    @Test
    public void testDeepHashCodeConsidersCustomEqualsImplementation() {
        Person p1 = new Person(1, "Alice", 25);
        Person p2 = new Person(2, "Alice", 30);
        // Since Person defines a custom hashCode, deepHashCode will use it
        assertEquals(p1.hashCode(), p2.hashCode()); // sanity
        assertEquals(DeepEquals.deepHashCode(p1), DeepEquals.deepHashCode(p2));
    }
}
