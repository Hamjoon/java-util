package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.*;

public class DeepEqualsTest {

    /* --------------------------------------------------------------
     * Helper classes used in the tests
     * -------------------------------------------------------------- */

    /** Simple POJO with no custom equals / hashCode */
    static class Person {
        String name;
        int age;
        Person friend; // to test nested objects & circular refs

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    /** POJO that overrides equals but only checks the name field */
    static class PersonCustomEquals {
        String name;
        int age;

        PersonCustomEquals(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PersonCustomEquals)) return false;
            PersonCustomEquals other = (PersonCustomEquals) o;
            return Objects.equals(name, other.name); // ignores age!
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    /** POJO that overrides both equals and hashCode */
    static class Simple {
        int id;

        Simple(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Simple)) return false;
            return id == ((Simple) o).id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    /* --------------------------------------------------------------
     * Tests for hasCustomEquals / hasCustomHashCode
     * -------------------------------------------------------------- */

    @Test
    public void testHasCustomEqualsDetection() {
        // Person does NOT override equals
        assertFalse(DeepEquals.hasCustomEquals(Person.class));

        // PersonCustomEquals DOES override equals
        assertTrue(DeepEquals.hasCustomEquals(PersonCustomEquals.class));

        // Simple overrides both equals and hashCode
        assertTrue(DeepEquals.hasCustomEquals(Simple.class));
    }

    @Test
    public void testHasCustomHashCodeDetection() {
        // Person does NOT override hashCode
        assertFalse(DeepEquals.hasCustomHashCode(Person.class));

        // PersonCustomEquals overrides hashCode (via equals implementation)
        assertTrue(DeepEquals.hasCustomHashCode(PersonCustomEquals.class));

        // Simple overrides hashCode
        assertTrue(DeepEquals.hasCustomHashCode(Simple.class));
    }

    /* --------------------------------------------------------------
     * Basic deepEquals without options
     * -------------------------------------------------------------- */

    @Test
    public void testDeepEqualsPrimitivesAndSimpleObjects() {
        // null vs null
        assertTrue(DeepEquals.deepEquals(null, null));

        // null vs non‑null
        assertFalse(DeepEquals.deepEquals(null, "abc"));

        // Simple numbers
        assertTrue(DeepEquals.deepEquals(42, 42));
        assertFalse(DeepEquals.deepEquals(42, 43));

        // Floating point tolerance
        double a = 0.1 + 0.2; // 0.30000000000000004
        double b = 0.3;
        assertTrue(DeepEquals.deepEquals(a, b));

        // Different floating point values beyond epsilon
        assertFalse(DeepEquals.deepEquals(0.1, 0.1000000001));
    }

    @Test
    public void testDeepEqualsSimplePojoFields() {
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 30);
        Person p3 = new Person("Alice", 31);

        assertTrue(DeepEquals.deepEquals(p1, p2));
        assertFalse(DeepEquals.deepEquals(p1, p3));

        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(p1, p3, opts);
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains(".age"));
        assertTrue(diff.contains("Expected: 30"));
        assertTrue(diff.contains("Found: 31"));
    }

    /* --------------------------------------------------------------
     * Collections – ordered vs unordered
     * -------------------------------------------------------------- */

    @Test
    public void testOrderedCollectionEquality() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        List<Integer> l3 = Arrays.asList(3, 2, 1);

        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));
    }

    @Test
    public void testUnorderedCollectionEquality() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("c", "b", "a"));
        Set<String> s3 = new HashSet<>(Arrays.asList("a", "b"));

        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));

        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(s1, s3, opts);
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("collection size mismatch"));
    }

    /* --------------------------------------------------------------
     * Map comparison
     * -------------------------------------------------------------- */

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
        m3.put("two", 3); // value differs

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));

        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(m1, m3, opts);
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("map value mismatch"));
        assertTrue(diff.contains("Expected: 2"));
        assertTrue(diff.contains("Found: 3"));
    }

    @Test
    public void testMapMissingKey() {
        Map<String, String> a = new HashMap<>();
        a.put("k1", "v1");
        a.put("k2", "v2");

        Map<String, String> b = new HashMap<>();
        b.put("k1", "v1"); // missing k2

        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(a, b, opts);
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("missing map key"));
        assertTrue(diff.contains("k2"));
    }

    /* --------------------------------------------------------------
     * Array handling – dimensions, component type, length, elements
     * -------------------------------------------------------------- */

    @Test
    public void testArrayEquality() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {1, 2, 4};

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));

        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(a1, a3, opts);
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array element mismatch"));
        assertTrue(diff.contains("Expected: 3"));
        assertTrue(diff.contains("Found: 4"));
    }

    @Test
    public void testMultiDimensionalArrayMismatch() {
        int[][] a1 = {{1, 2}, {3, 4}};
        int[][] a2 = {{1, 2}, {3, 5}}; // inner element differs

        assertFalse(DeepEquals.deepEquals(a1, a2));

        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(a1, a2, opts);
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array element mismatch"));
        assertTrue(diff.contains("[1][1]")); // second row, second column
    }

    @Test
    public void testArrayDimensionMismatch() {
        int[] oneD = {1, 2, 3};
        int[][] twoD = {{1, 2, 3}};

        assertFalse(DeepEquals.deepEquals(oneD, twoD));

        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(oneD, twoD, opts);
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array dimensionality mismatch"));
    }

    /* --------------------------------------------------------------
     * Options – ignore custom equals
     * -------------------------------------------------------------- */

    @Test
    public void testCustomEqualsUsedByDefault() {
        PersonCustomEquals p1 = new PersonCustomEquals("Bob", 25);
        PersonCustomEquals p2 = new PersonCustomEquals("Bob", 30); // age differs but equals ignores it

        // Default behavior uses the custom equals, so they are considered equal
        assertTrue(DeepEquals.deepEquals(p1, p2));

        // When we ignore custom equals (empty set), fields are compared and age mismatch is detected
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet()); // ignore ALL custom equals
        assertFalse(DeepEquals.deepEquals(p1, p2, opts));

        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains(".age"));
        assertTrue(diff.contains("Expected: 25"));
        assertTrue(diff.contains("Found: 30"));
    }

    @Test
    public void testIgnoreSpecificCustomEqualsClass() {
        PersonCustomEquals p1 = new PersonCustomEquals("Carol", 40);
        PersonCustomEquals p2 = new PersonCustomEquals("Carol", 41);

        // Ignore only PersonCustomEquals – should behave like ignoring all for this class
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(PersonCustomEquals.class));
        assertFalse(DeepEquals.deepEquals(p1, p2, opts));

        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains(".age"));
    }

    /* --------------------------------------------------------------
     * Options – allow strings to match numbers
     * -------------------------------------------------------------- */

    @Test
    public void testStringNumberMatchingOption() {
        // Without option – should be false
        assertFalse(DeepEquals.deepEquals("10", 10));

        // With option enabled – should be true
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("10", 10, opts));

        // Mismatch still reported correctly when numbers differ
        Map<String, Object> opts2 = new HashMap<>();
        opts2.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertFalse(DeepEquals.deepEquals("10", 11, opts2));
        String diff = (String) opts2.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("value mismatch"));
    }

    /* --------------------------------------------------------------
     * deepHashCode correctness – basic sanity checks
     * -------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeConsistency() {
        // Same object graph → same hash
        Person p1 = new Person("Dave", 45);
        Person p2 = new Person("Dave", 45);
        int h1 = DeepEquals.deepHashCode(p1);
        int h2 = DeepEquals.deepHashCode(p2);
        assertEquals(h1, h2);

        // Different field values → likely different hash (not guaranteed but highly probable)
        Person p3 = new Person("Dave", 46);
        int h3 = DeepEquals.deepHashCode(p3);
        assertNotEquals(h1, h3);
    }

    @Test
    public void testDeepHashCodeOnCollectionsAndMaps() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        assertEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));

        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 2, 1));
        assertEquals(DeepEquals.deepHashCode(s1), DeepEquals.deepHashCode(s2));

        Map<String, Integer> m1 = new HashMap<>();
        m1.put("x", 1);
        m1.put("y", 2);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("y", 2);
        m2.put("x", 1);
        assertEquals(DeepEquals.deepHashCode(m1), DeepEquals.deepHashCode(m2));
    }

    /* --------------------------------------------------------------
     * Circular reference handling
     * -------------------------------------------------------------- */

    @Test
    public void testCircularReferenceDoesNotStackOverflow() {
        Person a = new Person("Eve", 20);
        Person b = new Person("Eve", 20);
        a.friend = b;
        b.friend = a; // circular link

        // Should complete without infinite recursion
        assertTrue(DeepEquals.deepEquals(a, b));

        // Change a nested field to cause a diff
        b.age = 21;
        assertFalse(DeepEquals.deepEquals(a, b));

        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals(a, b, opts);
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains(".age"));
    }

    /* --------------------------------------------------------------
     * Miscellaneous – URI, URL, UUID handling (simple type conversion)
     * -------------------------------------------------------------- */

    @Test
    public void testSimpleTypesEquality() throws Exception {
        URI u1 = new URI("http://example.com");
        URI u2 = new URI("http://example.com");
        assertTrue(DeepEquals.deepEquals(u1, u2));

        URL url1 = new URL("http://example.com");
        URL url2 = new URL("http://example.com");
        assertTrue(DeepEquals.deepEquals(url1, url2));

        UUID id1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        UUID id2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        assertTrue(DeepEquals.deepEquals(id1, id2));
    }

    /* --------------------------------------------------------------
     * Verify that deepEquals(Object, Object) delegates to the overload
     * -------------------------------------------------------------- */

    @Test
    public void testDeepEqualsNoOptionsDelegatesCorrectly() {
        // Use a structure that would produce a diff if options were mishandled
        List<Integer> l1 = Arrays.asList(1, 2);
        List<Integer> l2 = Arrays.asList(1, 3);

        // The two‑argument version should return false and not throw
        assertFalse(DeepEquals.deepEquals(l1, l2));
    }
}
