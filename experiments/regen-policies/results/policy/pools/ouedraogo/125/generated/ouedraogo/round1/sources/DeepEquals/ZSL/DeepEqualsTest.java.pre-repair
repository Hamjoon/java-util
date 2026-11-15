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
 * JUnit‑4 test suite for {@link DeepEquals}. The tests exercise the public API
 * (deepEquals, deepHashCode, hasCustomEquals, hasCustomHashCode) and verify
 * that the diff description generated for failing comparisons is meaningful.
 *
 * The suite deliberately creates real objects (no mocks) and uses only the
 * classes, constructors and methods that appear in the source code supplied.
 */
public class DeepEqualsTest {

    private Map<String, Object> options;

    @Before
    public void setUp() {
        options = new HashMap<>();
    }

    /* -------------------------------------------------------------
     *  Simple primitive / wrapper / String equality
     * ------------------------------------------------------------- */
    @Test
    public void testSimpleEquality() {
        assertTrue(DeepEquals.deepEquals(5, 5));
        assertTrue(DeepEquals.deepEquals("hello", "hello"));
        assertTrue(DeepEquals.deepEquals(true, true));
        assertTrue(DeepEquals.deepEquals(null, null));

        assertFalse(DeepEquals.deepEquals(5, 6));
        assertFalse(DeepEquals.deepEquals("a", "b"));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
    }

    /* -------------------------------------------------------------
     *  Floating‑point comparison with epsilon tolerance
     * ------------------------------------------------------------- */
    @Test
    public void testFloatingPointTolerance() {
        double a = 0.123456789012345;
        double b = a + 1e-16; // within epsilon (1e-15)
        assertTrue(DeepEquals.deepEquals(a, b));

        double c = a + 1e-13; // outside epsilon
        assertFalse(DeepEquals.deepEquals(a, c));
    }

    /* -------------------------------------------------------------
     *  String‑to‑Number matching (option enabled / disabled)
     * ------------------------------------------------------------- */
    @Test
    public void testStringNumberMatchingEnabled() {
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("10", 10, options));
        assertTrue(DeepEquals.deepEquals(10, "10", options));

        // mismatch still reported when values differ
        assertFalse(DeepEquals.deepEquals("10", 11, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("value mismatch"));
    }

    @Test
    public void testStringNumberMatchingDisabled() {
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);
        assertFalse(DeepEquals.deepEquals("10", 10, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("value mismatch"));
    }

    /* -------------------------------------------------------------
     *  Ordered collection (List) equality and size mismatch
     * ------------------------------------------------------------- */
    @Test
    public void testOrderedCollectionEquality() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        assertTrue(DeepEquals.deepEquals(l1, l2));

        List<Integer> l3 = Arrays.asList(1, 2);
        assertFalse(DeepEquals.deepEquals(l1, l3, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("collection size mismatch"));
    }

    /* -------------------------------------------------------------
     *  Unordered collection (Set) equality and missing element
     * ------------------------------------------------------------- */
    @Test
    public void testUnorderedCollectionEquality() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("c", "b", "a"));
        assertTrue(DeepEquals.deepEquals(s1, s2));

        Set<String> s3 = new HashSet<>(Arrays.asList("a", "b"));
        assertFalse(DeepEquals.deepEquals(s1, s3, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("missing collection element"));
    }

    /* -------------------------------------------------------------
     *  Map equality and missing key/value mismatch
     * ------------------------------------------------------------- */
    @Test
    public void testMapEquality() {
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("x", 1);
        m1.put("y", 2);

        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("x", 1);
        m2.put("y", 2);

        assertTrue(DeepEquals.deepEquals(m1, m2));

        // missing key
        Map<String, Integer> m3 = new LinkedHashMap<>();
        m3.put("x", 1);
        assertFalse(DeepEquals.deepEquals(m1, m3, options));
        String diffMissingKey = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diffMissingKey);
        assertTrue(diffMissingKey.contains("missing map key"));

        // value mismatch
        Map<String, Integer> m4 = new LinkedHashMap<>();
        m4.put("x", 9);
        m4.put("y", 2);
        assertFalse(DeepEquals.deepEquals(m1, m4, options));
        String diffValue = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diffValue);
        assertTrue(diffValue.contains("map value mismatch"));
    }

    /* -------------------------------------------------------------
     *  Array equality and length mismatch
     * ------------------------------------------------------------- */
    @Test
    public void testArrayEquality() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        assertTrue(DeepEquals.deepEquals(a1, a2));

        int[] a3 = {1, 2};
        assertFalse(DeepEquals.deepEquals(a1, a3, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array length mismatch"));
    }

    /* -------------------------------------------------------------
     *  Custom equals handling (ignore vs. respect)
     * ------------------------------------------------------------- */
    static class Person {
        String name;
        int age;

        Person(String name, int age) { this.name = name; this.age = age; }

        // Custom equals that only checks name (deliberately incomplete)
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person p = (Person) o;
            return Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    @Test
    public void testCustomEqualsIgnored() {
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 40); // age differs, but custom equals ignores age

        // Without ignoring custom equals, deepEquals should report mismatch (field age)
        assertFalse(DeepEquals.deepEquals(p1, p2, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("field value mismatch"));

        // Ignore custom equals for Person – deepEquals should compare fields and succeed
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.singleton(Person.class));
        assertTrue(DeepEquals.deepEquals(p1, p2, options));
    }

    @Test
    public void testCustomEqualsRespected() {
        Person p1 = new Person("Bob", 25);
        Person p2 = new Person("Bob", 99); // different age, but equals() says true

        // No ignore set – custom equals is used, so objects are considered equal
        assertTrue(DeepEquals.deepEquals(p1, p2));

        // Empty ignore set – all custom equals are ignored, field comparison should fail
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertFalse(DeepEquals.deepEquals(p1, p2, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("field value mismatch"));
    }

    /* -------------------------------------------------------------
     *  Circular reference handling
     * ------------------------------------------------------------- */
    static class Node {
        String name;
        Node next;

        Node(String name) { this.name = name; }
    }

    @Test
    public void testCircularReferenceEquality() {
        Node a = new Node("first");
        Node b = new Node("first");
        a.next = a; // self‑reference
        b.next = b;

        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testCircularReferenceInequality() {
        Node a = new Node("first");
        Node b = new Node("first");
        Node c = new Node("second");
        a.next = a;
        b.next = b;
        c.next = c;

        assertFalse(DeepEquals.deepEquals(a, c, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("field value mismatch"));
    }

    /* -------------------------------------------------------------
     *  deepHashCode consistency with deepEquals
     * ------------------------------------------------------------- */
    @Test
    public void testDeepHashCodeConsistency() {
        List<String> list1 = Arrays.asList("x", "y", "z");
        List<String> list2 = Arrays.asList("x", "y", "z");
        assertTrue(DeepEquals.deepEquals(list1, list2));

        int h1 = DeepEquals.deepHashCode(list1);
        int h2 = DeepEquals.deepHashCode(list2);
        assertEquals(h1, h2);
    }

    @Test
    public void testDeepHashCodeDifferentObjects() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("a", "c");
        assertFalse(DeepEquals.deepEquals(list1, list2));

        int h1 = DeepEquals.deepHashCode(list1);
        int h2 = DeepEquals.deepHashCode(list2);
        assertNotEquals(h1, h2);
    }

    /* -------------------------------------------------------------
     *  hasCustomEquals / hasCustomHashCode public helpers
     * ------------------------------------------------------------- */
    @Test
    public void testHasCustomEqualsAndHashCode() {
        assertTrue(DeepEquals.hasCustomEquals(Person.class));
        assertTrue(DeepEquals.hasCustomHashCode(Person.class));

        // A simple POJO with no overrides
        class Simple { }
        assertFalse(DeepEquals.hasCustomEquals(Simple.class));
        assertFalse(DeepEquals.hasCustomHashCode(Simple.class));
    }

    /* -------------------------------------------------------------
     *  Additional type handling (Date, URI, URL, UUID, AtomicBoolean)
     * ------------------------------------------------------------- */
    @Test
    public void testVariousSimpleTypes() throws Exception {
        Date d1 = new SimpleDateFormat("yyyy-MM-dd").parse("2023-01-01");
        Date d2 = new SimpleDateFormat("yyyy-MM-dd").parse("2023-01-01");
        assertTrue(DeepEquals.deepEquals(d1, d2));

        URI uri1 = new URI("http://example.com");
        URI uri2 = new URI("http://example.com");
        assertTrue(DeepEquals.deepEquals(uri1, uri2));

        URL url1 = new URL("http://example.com");
        URL url2 = new URL("http://example.com");
        assertTrue(DeepEquals.deepEquals(url1, url2));

        UUID uuid = UUID.randomUUID();
        assertTrue(DeepEquals.deepEquals(uuid, uuid));

        AtomicBoolean ab1 = new AtomicBoolean(true);
        AtomicBoolean ab2 = new AtomicBoolean(true);
        assertTrue(DeepEquals.deepEquals(ab1, ab2));

        AtomicBoolean ab3 = new AtomicBoolean(false);
        assertFalse(DeepEquals.deepEquals(ab1, ab3));
    }

    /* -------------------------------------------------------------
     *  Verify diff description contains the correct root context
     * ------------------------------------------------------------- */
    @Test
    public void testDiffRootContextFormatting() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 9, 3);
        assertFalse(DeepEquals.deepEquals(l1, l2, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        // Root should be a List with its size range notation
        assertTrue(diff.startsWith("[collection element mismatch]"));
        assertTrue(diff.contains("List(0..2)"));
        assertTrue(diff.contains(".[1]"));
    }

    /* -------------------------------------------------------------
     *  Verify that deepEquals with an empty options map behaves like
     *  the no‑arg overload (no custom options).
     * ------------------------------------------------------------- */
    @Test
    public void testDeepEqualsNoOptionsSameAsEmptyMap() {
        List<String> a = Arrays.asList("a", "b");
        List<String> b = Arrays.asList("a", "c");
        assertFalse(DeepEquals.deepEquals(a, b));
        assertFalse(DeepEquals.deepEquals(a, b, new HashMap<>()));
    }
}
