package com.cedarsoftware.util;

import org.junit.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.cedarsoftware.util.DeepEquals.*;
import static org.junit.Assert.*;

public class DeepEqualsTest {

    /* -------------------------------------------------------------
     *  Helper classes used in several tests
     * ------------------------------------------------------------- */

    /** A simple POJO with a custom equals that only checks the name. */
    static class Person {
        String name;
        int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Person)) return false;
            Person p = (Person) o;
            // custom equality: only name matters
            return Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    /** A POJO that overrides both equals and hashCode (different from Object). */
    static class CustomHash {
        int id;

        CustomHash(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomHash) && ((CustomHash) o).id == id;
        }

        @Override
        public int hashCode() {
            return id * 31;
        }
    }

    /** Simple circular linked node */
    static class Node {
        String value;
        Node next;

        Node(String value) {
            this.value = value;
        }
    }

    /* -------------------------------------------------------------
     *  Public method signatures extracted from DeepEquals
     * -------------------------------------------------------------
     *
     * public static boolean deepEquals(Object a, Object b)
     * public static boolean deepEquals(Object a, Object b, Map<String, ?> options)
     * public static boolean hasCustomEquals(Class<?> c)
     * public static boolean hasCustomHashCode(Class<?> c)
     * public static int deepHashCode(Object obj)
     *
     */

    /* -------------------------------------------------------------
     *  Basic functional tests
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsSimpleEquality() {
        assertTrue(DeepEquals.deepEquals("hello", "hello"));
        assertTrue(DeepEquals.deepEquals(42, 42));
        assertTrue(DeepEquals.deepEquals(true, true));
    }

    @Test
    public void testDeepEqualsNulls() {
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
        assertFalse(DeepEquals.deepEquals("not null", null));
    }

    @Test
    public void testDeepEqualsNumberPrecision() {
        // double values that differ only beyond epsilon should be equal
        double a = 1.000000000000001;
        double b = 1.000000000000002;
        assertTrue(DeepEquals.deepEquals(a, b));

        // values that differ more than epsilon must be false
        double c = 1.0;
        double d = 1.0000001;
        assertFalse(DeepEquals.deepEquals(c, d));
    }

    @Test
    public void testDeepEqualsStringNumberOptionEnabled() {
        Map<String, Object> options = new HashMap<>();
        options.put(ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        assertTrue(DeepEquals.deepEquals("10", 10, options));
        assertTrue(DeepEquals.deepEquals(10L, "10", options));
        assertTrue(DeepEquals.deepEquals(new BigDecimal("3.14"), "3.14", options));

        // mismatch when option disabled
        Map<String, Object> optionsOff = new HashMap<>();
        optionsOff.put(ALLOW_STRINGS_TO_MATCH_NUMBERS, false);
        assertFalse(DeepEquals.deepEquals("10", 10, optionsOff));
    }

    @Test
    public void testDeepEqualsListOrderMatters() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        List<Integer> l3 = Arrays.asList(3, 2, 1);

        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));

        // verify diff description contains "order"
        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(l1, l3, opts);
        String diff = (String) opts.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.toLowerCase().contains("order"));
    }

    @Test
    public void testDeepEqualsSetEquality() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("c", "b", "a"));
        Set<String> s3 = new HashSet<>(Arrays.asList("a", "b"));

        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    @Test
    public void testDeepEqualsMapValueMismatch() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("x", 1);
        m1.put("y", 2);

        Map<String, Integer> m2 = new HashMap<>();
        m2.put("x", 1);
        m2.put("y", 3); // different value

        assertFalse(DeepEquals.deepEquals(m1, m2));

        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(m1, m2, opts);
        String diff = (String) opts.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("map value mismatch"));
    }

    @Test
    public void testDeepEqualsArrayDimensionMismatch() {
        int[][] a1 = new int[][]{{1, 2}, {3, 4}};
        int[] a2 = new int[]{1, 2, 3, 4};

        assertFalse(DeepEquals.deepEquals(a1, a2));

        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(a1, a2, opts);
        String diff = (String) opts.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array dimensionality mismatch"));
    }

    @Test
    public void testDeepEqualsArrayComponentTypeMismatch() {
        int[] ints = {1, 2, 3};
        long[] longs = {1L, 2L, 3L};

        assertFalse(DeepEquals.deepEquals(ints, longs));

        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(ints, longs, opts);
        String diff = (String) opts.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array component type mismatch"));
    }

    @Test
    public void testDeepEqualsArrayLengthMismatch() {
        String[] a1 = {"a", "b", "c"};
        String[] a2 = {"a", "b"};

        assertFalse(DeepEquals.deepEquals(a1, a2));

        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(a1, a2, opts);
        String diff = (String) opts.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array length mismatch"));
    }

    @Test
    public void testDeepEqualsArrayElementMismatch() {
        double[] a1 = {1.0, 2.0, 3.0};
        double[] a2 = {1.0, 2.1, 3.0};

        assertFalse(DeepEquals.deepEquals(a1, a2));

        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(a1, a2, opts);
        String diff = (String) opts.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array element mismatch"));
    }

    @Test
    public void testDeepEqualsCircularReference() {
        Node n1 = new Node("first");
        Node n2 = new Node("second");
        n1.next = n2;
        n2.next = n1; // creates a cycle

        Node m1 = new Node("first");
        Node m2 = new Node("second");
        m1.next = m2;
        m2.next = m1; // same structure

        assertTrue(DeepEquals.deepEquals(n1, m1));
    }

    @Test
    public void testDeepEqualsCustomEqualsIgnored() {
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 40); // same name, different age -> equals() would be true

        // By default custom equals is used -> should be true
        assertTrue(DeepEquals.deepEquals(p1, p2));

        // Ignore all custom equals (empty set) -> field‑by‑field comparison -> ages differ -> false
        Map<String, Object> opts = new HashMap<>();
        opts.put(IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertFalse(DeepEquals.deepEquals(p1, p2, opts));

        // Verify diff mentions field value mismatch
        String diff = (String) opts.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.toLowerCase().contains("field value mismatch"));
    }

    @Test
    public void testDeepEqualsCustomEqualsSpecificIgnore() {
        Person p1 = new Person("Bob", 25);
        Person p2 = new Person("Bob", 35);

        // Ignore only Person class custom equals
        Map<String, Object> opts = new HashMap<>();
        opts.put(IGNORE_CUSTOM_EQUALS, Set.of(Person.class));
        assertFalse(DeepEquals.deepEquals(p1, p2, opts));
    }

    @Test
    public void testDeepEqualsAtomicBoolean() {
        AtomicBoolean a1 = new AtomicBoolean(true);
        AtomicBoolean a2 = new AtomicBoolean(true);
        AtomicBoolean a3 = new AtomicBoolean(false);

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    @Test
    public void testDeepEqualsComplexObjectsWithURIAndURL() throws Exception {
        URI uri1 = new URI("http://example.com");
        URI uri2 = new URI("http://example.com");
        URL url1 = new URL("http://example.com");
        URL url2 = new URL("http://example.com");

        assertTrue(DeepEquals.deepEquals(uri1, uri2));
        assertTrue(DeepEquals.deepEquals(url1, url2));
    }

    /* -------------------------------------------------------------
     *  Tests for hasCustomEquals / hasCustomHashCode
     * ------------------------------------------------------------- */

    @Test
    public void testHasCustomEquals() {
        // java.util.Date overrides equals
        assertTrue(DeepEquals.hasCustomEquals(Date.class));

        // Simple class without overriding equals
        assertFalse(DeepEquals.hasCustomEquals(String.class)); // String overrides, but it's a final class with custom equals (so true)
        // Actually String does override equals, so we expect true:
        assertTrue(DeepEquals.hasCustomEquals(String.class));

        // Our Person class overrides equals
        assertTrue(DeepEquals.hasCustomEquals(Person.class));

        // Primitive wrapper (Integer) overrides equals
        assertTrue(DeepEquals.hasCustomEquals(Integer.class));
    }

    @Test
    public void testHasCustomHashCode() {
        // java.util.Date overrides hashCode
        assertTrue(DeepEquals.hasCustomHashCode(Date.class));

        // String overrides hashCode
        assertTrue(DeepEquals.hasCustomHashCode(String.class));

        // Our CustomHash class overrides hashCode
        assertTrue(DeepEquals.hasCustomHashCode(CustomHash.class));

        // A class without overriding hashCode (e.g., Object itself)
        assertFalse(DeepEquals.hasCustomHashCode(Object.class));
    }

    /* -------------------------------------------------------------
     *  Tests for deepHashCode
     * ------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeNull() {
        assertEquals(0, DeepEquals.deepHashCode(null));
    }

    @Test
    public void testDeepHashCodeSimpleValues() {
        int h1 = DeepEquals.deepHashCode("test");
        int h2 = DeepEquals.deepHashCode("test");
        assertEquals(h1, h2);
    }

    @Test
    public void testDeepHashCodeCollections() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        assertEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));

        Set<String> set1 = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> set2 = new HashSet<>(Arrays.asList("b", "a"));
        assertEquals(DeepEquals.deepHashCode(set1), DeepEquals.deepHashCode(set2));
    }

    @Test
    public void testDeepHashCodeMaps() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("x", 1);
        m1.put("y", 2);

        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("y", 2);
        m2.put("x", 1);

        assertEquals(DeepEquals.deepHashCode(m1), DeepEquals.deepHashCode(m2));
    }

    @Test
    public void testDeepHashCodeArrays() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        assertEquals(DeepEquals.deepHashCode(a1), DeepEquals.deepHashCode(a2));

        int[][] multi1 = {{1, 2}, {3, 4}};
        int[][] multi2 = {{1, 2}, {3, 4}};
        assertEquals(DeepEquals.deepHashCode(multi1), DeepEquals.deepHashCode(multi2));
    }

    @Test
    public void testDeepHashCodeCustomEqualsIgnored() {
        Person p1 = new Person("Charlie", 20);
        Person p2 = new Person("Charlie", 30); // same name, different age

        // Since custom equals is used, deepHashCode will use Person.hashCode() (based on name)
        assertEquals(DeepEquals.deepHashCode(p1), DeepEquals.deepHashCode(p2));

        // When we ignore custom equals (empty ignore set), deepHashCode still uses hashCode()
        // because hashCode is custom; however deepEquals will compare fields.
        // The hash values remain equal because hashCode() is based only on name.
        Map<String, Object> opts = new HashMap<>();
        opts.put(IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertTrue(DeepEquals.deepEquals(p1, p2, opts)); // will be false because fields differ
        // hash codes stay the same
        assertEquals(DeepEquals.deepHashCode(p1), DeepEquals.deepHashCode(p2));
    }

    /* -------------------------------------------------------------
     *  Edge‑case / exception handling tests
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsWithUnsupportedOptionKey() {
        Map<String, Object> opts = new HashMap<>();
        opts.put("nonexistent_option", true);
        // The method should ignore unknown keys and behave like default
        assertTrue(DeepEquals.deepEquals("foo", "foo", opts));
    }

    @Test
    public void testDeepEqualsWithEmptyOptionMap() {
        Map<String, Object> empty = new HashMap<>();
        assertTrue(DeepEquals.deepEquals(123, 123, empty));
        assertFalse(DeepEquals.deepEquals(123, 124, empty));
    }

    @Test
    public void testDeepEqualsWithSelfReferenceInObjectFields() throws Exception {
        // Object that contains a reference to itself (field "self")
        class SelfRef {
            SelfRef self;

            SelfRef() {
                this.self = this;
            }
        }

        SelfRef a = new SelfRef();
        SelfRef b = new SelfRef();

        // Should not cause StackOverflowError
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEqualsWithDifferentClasses() {
        assertFalse(DeepEquals.deepEquals("string", 123));
        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals("string", 123, opts);
        String diff = (String) opts.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.toLowerCase().contains("type mismatch"));
    }
}
