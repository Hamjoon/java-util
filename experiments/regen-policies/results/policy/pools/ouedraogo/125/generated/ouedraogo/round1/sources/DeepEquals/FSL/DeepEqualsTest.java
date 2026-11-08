package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeepEqualsTest {

    /* --------------------------------------------------------------
     * Simple primitive and wrapper equality
     * ------------------------------------------------------------ */
    @Test
    public void testPrimitiveAndWrapperEquality() {
        assertTrue(DeepEquals.deepEquals(5, 5));
        assertTrue(DeepEquals.deepEquals("hello", "hello"));
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(5, null));
        assertFalse(DeepEquals.deepEquals("a", "b"));
    }

    /* --------------------------------------------------------------
     * Numeric comparison with tolerance (double/float)
     * ------------------------------------------------------------ */
    @Test
    public void testNumericTolerance() {
        double a = 0.123456789012345;
        double b = 0.123456789012346; // differ by 1e-15
        assertTrue(DeepEquals.deepEquals(a, b));

        float f1 = 1.000001f;
        float f2 = 1.000002f; // within float epsilon
        assertTrue(DeepEquals.deepEquals(f1, f2));

        // Different beyond epsilon
        double c = 0.1234567890123;
        double d = 0.1234567890120;
        assertFalse(DeepEquals.deepEquals(c, d));
    }

    /* --------------------------------------------------------------
     * String‑to‑Number matching option
     * ------------------------------------------------------------ */
    @Test
    public void testStringNumberMatchingOption() {
        Map<String, Object> opts = new HashMap<>();
        // option disabled → mismatch
        assertFalse(DeepEquals.deepEquals("10", 10, opts));
        assertNull(opts.get(DeepEquals.DIFF)); // no diff stored because comparison short‑circuits

        // enable option
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("10", 10, opts));
        assertTrue(DeepEquals.deepEquals(15.0, "15.0", opts));

        // mismatch when numbers differ
        assertFalse(DeepEquals.deepEquals("11", 10, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[value mismatch]"));
    }

    /* --------------------------------------------------------------
     * Array handling (1‑D and multi‑dimensional)
     * ------------------------------------------------------------ */
    @Test
    public void testArrayEquality() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {1, 2, 4};

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));

        // length mismatch
        int[] a4 = {1, 2};
        assertFalse(DeepEquals.deepEquals(a1, a4));

        // multidimensional
        int[][] m1 = {{1, 2}, {3, 4}};
        int[][] m2 = {{1, 2}, {3, 4}};
        int[][] m3 = {{1, 2}, {3, 5}};
        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));

        // dimension mismatch
        int[][][] m4 = new int[2][2][2];
        assertFalse(DeepEquals.deepEquals(m1, m4));
    }

    /* --------------------------------------------------------------
     * Ordered collection (List) handling
     * ------------------------------------------------------------ */
    @Test
    public void testOrderedCollectionEquality() {
        List<String> l1 = Arrays.asList("a", "b", "c");
        List<String> l2 = Arrays.asList("a", "b", "c");
        List<String> l3 = Arrays.asList("c", "b", "a");
        List<String> l4 = Arrays.asList("a", "b");

        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3)); // order matters
        assertFalse(DeepEquals.deepEquals(l1, l4)); // size mismatch
    }

    /* --------------------------------------------------------------
     * Unordered collection (Set) handling
     * ------------------------------------------------------------ */
    @Test
    public void testUnorderedCollectionEquality() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 2, 1));
        Set<Integer> s3 = new HashSet<>(Arrays.asList(1, 2));
        Set<Integer> s4 = new HashSet<>(Arrays.asList(1, 2, 4));

        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3)); // missing element
        assertFalse(DeepEquals.deepEquals(s1, s4)); // different element
    }

    /* --------------------------------------------------------------
     * Map handling (key/value, size, missing key/value)
     * ------------------------------------------------------------ */
    @Test
    public void testMapEquality() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new HashMap<>();
        m2.put("two", 2);
        m2.put("one", 1);

        Map<String, Integer> m3 = new HashMap<>();
        m3.put("one", 1);
        m3.put("two", 3); // value differs

        Map<String, Integer> m4 = new HashMap<>();
        m4.put("one", 1); // missing key

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
        assertFalse(DeepEquals.deepEquals(m1, m4));

        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(m1, m3, opts);
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[map value mismatch]"));
    }

    /* --------------------------------------------------------------
     * Custom equals handling – ignored vs used
     * ------------------------------------------------------------ */
    static class PartialEquals {
        public int id;
        public String name;

        PartialEquals(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PartialEquals)) return false;
            PartialEquals p = (PartialEquals) o;
            // deliberately ignore 'name'
            return this.id == p.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    @Test
    public void testCustomEqualsUsedWhenNotIgnored() {
        PartialEquals a = new PartialEquals(1, "Alice");
        PartialEquals b = new PartialEquals(1, "Bob");

        // default → custom equals is used → objects considered equal
        assertTrue(DeepEquals.deepEquals(a, b));

        // ignore ALL custom equals (empty set) → field‑by‑field comparison → mismatch
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertFalse(DeepEquals.deepEquals(a, b, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[field value mismatch]"));
        assertTrue(diff.contains(".name"));
    }

    @Test
    public void testCustomEqualsIgnoredForSpecificClass() {
        PartialEquals a = new PartialEquals(2, "X");
        PartialEquals b = new PartialEquals(2, "Y");

        // ignore only PartialEquals → deep field comparison
        Map<String, Object> opts = new HashMap<>();
        Set<Class<?>> ignore = new HashSet<>();
        ignore.add(PartialEquals.class);
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignore);
        assertFalse(DeepEquals.deepEquals(a, b, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains(".name"));
    }

    /* --------------------------------------------------------------
     * hasCustomEquals / hasCustomHashCode detection
     * ------------------------------------------------------------ */
    static class NoCustom { public int x = 5; }

    static class WithCustomEquals {
        @Override public boolean equals(Object o) { return this == o; }
    }

    static class WithCustomHashCode {
        @Override public int hashCode() { return 42; }
    }

    @Test
    public void testHasCustomEqualsAndHashCode() {
        assertFalse(DeepEquals.hasCustomEquals(NoCustom.class));
        assertTrue(DeepEquals.hasCustomEquals(WithCustomEquals.class));

        assertFalse(DeepEquals.hasCustomHashCode(NoCustom.class));
        assertTrue(DeepEquals.hasCustomHashCode(WithCustomHashCode.class));
    }

    /* --------------------------------------------------------------
     * deepHashCode consistency with deepEquals
     * ------------------------------------------------------------ */
    @Test
    public void testDeepHashCodeConsistency() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        List<String> list3 = Arrays.asList("c", "b", "a");

        assertEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));
        assertNotEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list3));

        Map<String, Integer> map1 = new HashMap<>();
        map1.put("one", 1);
        map1.put("two", 2);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("two", 2);
        map2.put("one", 1);
        assertEquals(DeepEquals.deepHashCode(map1), DeepEquals.deepHashCode(map2));
    }

    /* --------------------------------------------------------------
     * Circular reference detection – should not stack overflow
     * ------------------------------------------------------------ */
    static class Node {
        public String name;
        public Node next;

        Node(String name) { this.name = name; }
    }

    @Test
    public void testCircularReferenceEquality() {
        Node a1 = new Node("A");
        Node a2 = new Node("B");
        a1.next = a2;
        a2.next = a1; // cycle

        Node b1 = new Node("A");
        Node b2 = new Node("B");
        b1.next = b2;
        b2.next = b1; // cycle

        assertTrue(DeepEquals.deepEquals(a1, b1));
        assertTrue(DeepEquals.deepEquals(a2, b2));
    }

    /* --------------------------------------------------------------
     * Complex mixed structure (object with fields, collections, map)
     * ------------------------------------------------------------ */
    static class Person {
        public String name;
        public int age;
        public List<String> nicknames;
        public Map<String, String> attributes;

        Person(String name, int age, List<String> nicknames, Map<String, String> attributes) {
            this.name = name;
            this.age = age;
            this.nicknames = nicknames;
            this.attributes = attributes;
        }
    }

    @Test
    public void testComplexObjectEquality() {
        Person p1 = new Person(
                "John",
                30,
                Arrays.asList("Johnny", "J"),
                new HashMap<String, String>() {{
                    put("eye", "blue");
                    put("hair", "brown");
                }});

        Person p2 = new Person(
                "John",
                30,
                Arrays.asList("Johnny", "J"),
                new HashMap<String, String>() {{
                    put("hair", "brown");
                    put("eye", "blue");
                }});

        assertTrue(DeepEquals.deepEquals(p1, p2));

        // modify a nested field
        p2.attributes.put("eye", "green");
        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(p1, p2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[map value mismatch]"));
        assertTrue(diff.contains("eye"));
    }

    /* --------------------------------------------------------------
     * AtomicBoolean handling (special case)
     * ------------------------------------------------------------ */
    @Test
    public void testAtomicBooleanEquality() {
        AtomicBoolean ab1 = new AtomicBoolean(true);
        AtomicBoolean ab2 = new AtomicBoolean(true);
        AtomicBoolean ab3 = new AtomicBoolean(false);
        assertTrue(DeepEquals.deepEquals(ab1, ab2));
        assertFalse(DeepEquals.deepEquals(ab1, ab3));
    }

    /* --------------------------------------------------------------
     * Simple type conversion support (URI, URL, UUID, TimeZone)
     * ------------------------------------------------------------ */
    @Test
    public void testSimpleTypesEquality() throws Exception {
        URI uri1 = new URI("http://example.com");
        URI uri2 = new URI("http://example.com");
        assertTrue(DeepEquals.deepEquals(uri1, uri2));

        URL url1 = new URL("http://example.com");
        URL url2 = new URL("http://example.com");
        assertTrue(DeepEquals.deepEquals(url1, url2));

        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.fromString(u1.toString());
        assertTrue(DeepEquals.deepEquals(u1, u2));

        TimeZone tz1 = TimeZone.getTimeZone("UTC");
        TimeZone tz2 = TimeZone.getTimeZone("UTC");
        assertTrue(DeepEquals.deepEquals(tz1, tz2));
    }
}
