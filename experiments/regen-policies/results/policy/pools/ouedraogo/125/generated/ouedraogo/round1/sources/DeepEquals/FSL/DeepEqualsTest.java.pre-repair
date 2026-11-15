package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.*;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link DeepEquals}.
 *
 * The tests cover:
 * <ul>
 *   <li>Simple primitive and object equality</li>
 *   <li>Floating‑point tolerance</li>
 *   <li>String‑to‑Number matching (option enabled / disabled)</li>
 *   <li>Ordered vs unordered collection handling</li>
 *   <li>Map value mismatches</li>
 *   <li>Array dimension, component‑type and element mismatches</li>
 *   <li>Custom {@code equals()} handling with the {@code ignoreCustomEquals} option</li>
 *   <li>Detection of custom {@code equals()} / {@code hashCode()}</li>
 *   <li>Deep hash‑code consistency</li>
 *   <li>Circular reference handling</li>
 * </ul>
 */
public class DeepEqualsTest {

    /* --------------------------------------------------------------------- */
    /* Helper classes used by the tests                                      */
    /* --------------------------------------------------------------------- */

    /** Simple POJO with a custom {@code equals()} that only checks {@code id}. */
    static final class Person {
        final int id;
        final String name;
        final int age;

        Person(int id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person p = (Person) o;
            // Custom equals only cares about id
            return this.id == p.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    /** POJO without any custom equals / hashCode implementation. */
    static final class SimpleBean {
        int x;
        String y;

        SimpleBean(int x, String y) {
            this.x = x;
            this.y = y;
        }
    }

    /** Node class used to create circular references. */
    static final class Node {
        int value;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    /* --------------------------------------------------------------------- */
    /* 1. Simple primitive / object equality and diff generation               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSimpleEqualityAndDiff() {
        // equal primitives
        assertTrue(DeepEquals.deepEquals(42, 42));

        // unequal primitives – diff must be recorded
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(42, 43, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("value mismatch"));
        assertTrue(diff.contains("Expected: 42"));
        assertTrue(diff.contains("Found: 43"));
    }

    /* --------------------------------------------------------------------- */
    /* 2. Floating‑point tolerance                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFloatingPointTolerance() {
        double a = 0.1 + 0.2; // 0.30000000000000004
        double b = 0.3;
        assertTrue("Floating point numbers should be considered equal within epsilon",
                DeepEquals.deepEquals(a, b));
    }

    /* --------------------------------------------------------------------- */
    /* 3. String‑to‑Number matching (option)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testStringNumberMatchingOption() {
        Map<String, Object> enabled = new HashMap<>();
        enabled.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("10", 10, enabled));

        Map<String, Object> disabled = new HashMap<>();
        disabled.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);
        assertFalse(DeepEquals.deepEquals("10", 10, disabled));
    }

    /* --------------------------------------------------------------------- */
    /* 4. Unordered collection (Set) handling                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testUnorderedCollectionMismatch() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(1, 2, 4));

        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(s1, s2, opts));

        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("missing collection element"));
    }

    /* --------------------------------------------------------------------- */
    /* 5. Ordered collection (List) handling                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testOrderedCollectionMismatch() {
        List<String> l1 = Arrays.asList("alpha", "beta");
        List<String> l2 = Arrays.asList("beta", "alpha");

        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(l1, l2, opts));

        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("collection element mismatch"));
    }

    /* --------------------------------------------------------------------- */
    /* 6. Map value mismatch                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testMapValueMismatch() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new HashMap<>();
        m2.put("one", 1);
        m2.put("two", 3); // different value

        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m1, m2, opts));

        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("map value mismatch"));
        assertTrue(diff.contains("\"two\""));
    }

    /* --------------------------------------------------------------------- */
    /* 7. Array mismatches (element, length, dimension, component type)      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testArrayElementMismatch() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 4};

        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a1, a2, opts));

        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array element mismatch"));
        assertTrue(diff.contains("Expected: 3"));
        assertTrue(diff.contains("Found: 4"));
    }

    @Test
    public void testArrayLengthMismatch() {
        String[] s1 = {"a", "b"};
        String[] s2 = {"a", "b", "c"};

        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(s1, s2, opts));

        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array length mismatch"));
    }

    @Test
    public void testArrayDimensionMismatch() {
        int[][] dim1 = {{1, 2}};
        int[] dim2 = {1, 2};

        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(dim1, dim2, opts));

        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array dimensionality mismatch"));
    }

    @Test
    public void testArrayComponentTypeMismatch() {
        int[] ints = {1, 2, 3};
        long[] longs = {1L, 2L, 3L};

        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(ints, longs, opts));

        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array component type mismatch"));
    }

    /* --------------------------------------------------------------------- */
    /* 8. Custom equals handling with ignoreCustomEquals option               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCustomEqualsDefaultUsesCustomImplementation() {
        Person p1 = new Person(1, "Alice", 30);
        Person p2 = new Person(1, "Bob", 40); // same id, different fields

        // Default options – custom equals is used → objects are considered equal
        assertTrue(DeepEquals.deepEquals(p1, p2));
    }

    @Test
    public void testCustomEqualsIgnoredViaEmptySet() {
        Person p1 = new Person(1, "Alice", 30);
        Person p2 = new Person(1, "Bob", 40); // same id, different fields

        Map<String, Object> opts = new HashMap<>();
        // Empty set → ignore ALL custom equals implementations
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());

        assertFalse(DeepEquals.deepEquals(p1, p2, opts));

        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        // The mismatch should be reported as a field value mismatch
        assertTrue(diff.contains("field value mismatch"));
    }

    @Test
    public void testCustomEqualsIgnoredOnlyForSpecificClass() {
        Person p1 = new Person(1, "Alice", 30);
        Person p2 = new Person(1, "Bob", 40); // same id, different fields

        Map<String, Object> opts = new HashMap<>();
        // Ignore custom equals only for Person.class
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.singleton(Person.class));

        // Since Person is in the ignore set, field‑by‑field comparison is performed → mismatch
        assertFalse(DeepEquals.deepEquals(p1, p2, opts));

        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("field value mismatch"));
    }

    /* --------------------------------------------------------------------- */
    /* 9. Detection of custom equals / hashCode methods                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testHasCustomEqualsAndHashCodeDetection() {
        assertTrue("Person overrides equals", DeepEquals.hasCustomEquals(Person.class));
        assertTrue("Person overrides hashCode", DeepEquals.hasCustomHashCode(Person.class));

        assertFalse("SimpleBean does not override equals", DeepEquals.hasCustomEquals(SimpleBean.class));
        assertFalse("SimpleBean does not override hashCode", DeepEquals.hasCustomHashCode(SimpleBean.class));
    }

    /* --------------------------------------------------------------------- */
    /* 10. deepHashCode consistency                                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeConsistency() {
        Person p1 = new Person(42, "Carol", 25);
        Person p2 = new Person(42, "Carol", 25);

        // Using default behaviour (custom equals is considered)
        assertTrue(DeepEquals.deepEquals(p1, p2));

        // deepHashCode should be identical for deeply equal objects
        int h1 = DeepEquals.deepHashCode(p1);
        int h2 = DeepEquals.deepHashCode(p2);
        assertEquals("Hash codes of equal objects must match", h1, h2);
    }

    /* --------------------------------------------------------------------- */
    /* 11. Circular reference handling                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCircularReferenceEquality() {
        Node a = new Node(1);
        Node b = new Node(2);
        a.next = b;
        b.next = a; // circular link

        Node aCopy = new Node(1);
        Node bCopy = new Node(2);
        aCopy.next = bCopy;
        bCopy.next = aCopy; // matching circular structure

        assertTrue("Objects with identical circular graphs must be equal",
                DeepEquals.deepEquals(a, aCopy));
    }

    @Test
    public void testCircularReferenceMismatchProducesDiff() {
        Node a = new Node(1);
        Node b = new Node(2);
        a.next = b;
        b.next = a; // circular

        Node a2 = new Node(1);
        Node b2 = new Node(3); // different value
        a2.next = b2;
        b2.next = a2; // circular

        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, a2, opts));

        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        // The first differing leaf is the mismatched field 'value' of the second node
        assertTrue(diff.contains("field value mismatch"));
        assertTrue(diff.contains("Expected: 2"));
        assertTrue(diff.contains("Found: 3"));
    }

    /* --------------------------------------------------------------------- */
    /* 12. Additional simple type handling (Atomic, URI, URL, UUID, Date)     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAtomicAndSimpleTypesEquality() {
        assertTrue(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(true)));
        assertFalse(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(false)));

        URI uri1 = URI.create("http://example.com");
        URI uri2 = URI.create("http://example.com");
        assertTrue(DeepEquals.deepEquals(uri1, uri2));

        URL url1 = null;
        URL url2 = null;
        assertTrue(DeepEquals.deepEquals(url1, url2));
    }

    @Test
    public void testBigDecimalComparisonWithPrecision() {
        BigDecimal bd1 = new BigDecimal("1.000000000000001");
        BigDecimal bd2 = new BigDecimal("1.000000000000002");
        // Difference is below double epsilon, therefore should be considered equal
        assertTrue(DeepEquals.deepEquals(bd1, bd2));
    }
}
