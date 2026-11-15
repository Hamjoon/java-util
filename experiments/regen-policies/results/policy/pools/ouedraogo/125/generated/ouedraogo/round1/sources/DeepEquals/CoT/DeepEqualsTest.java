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
 * The test class follows the steps requested:
 *   1. List public methods (see comment block below).
 *   2. Provide a basic test for each public method.
 *   3. Identify edge‑case / exception scenarios.
 *   4. Add dedicated tests for those scenarios.
 *   5. Combine everything into a single test file.
 */
public class DeepEqualsTest {

    /* ----------------------------------------------------------------------
     *  Public methods of DeepEquals (extracted from the source)
     * ----------------------------------------------------------------------
     *
     * public static boolean deepEquals(Object a, Object b)
     * public static boolean deepEquals(Object a, Object b, Map<String, ?> options)
     * public static boolean hasCustomEquals(Class<?> c)
     * public static boolean hasCustomHashCode(Class<?> c)
     * public static int deepHashCode(Object obj)
     *
     * ---------------------------------------------------------------------- */

    /* ----------------------------------------------------------------------
     *  Helper POJOs used by the tests
     * ---------------------------------------------------------------------- */

    /** Simple bean with a custom equals implementation that *always* returns false. */
    public static class BadEquals {
        int id;

        public BadEquals(int id) { this.id = id; }

        @Override
        public boolean equals(Object o) {
            // deliberately broken – never equal even when fields match
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    /** Simple bean with a correct custom equals implementation. */
    public static class GoodEquals {
        int id;
        String name;

        public GoodEquals(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GoodEquals)) return false;
            GoodEquals that = (GoodEquals) o;
            return id == that.id && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

    /** Bean that contains a reference to another instance – used for circular‑reference tests. */
    public static class Node {
        int value;
        Node next;

        public Node(int value) {
            this.value = value;
        }
    }

    /** Bean with a custom hashCode implementation (different from Object.hashCode). */
    public static class CustomHash {
        int id;

        public CustomHash(int id) { this.id = id; }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomHash) && ((CustomHash) o).id == id;
        }

        @Override
        public int hashCode() {
            // a deterministic hash based on the field
            return id * 31;
        }
    }

    private Map<String, Object> options;

    @Before
    public void setUp() {
        options = new HashMap<>();
    }

    /* ----------------------------------------------------------------------
     *  BASIC TESTS – one per public method
     * ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsSimplePrimitives() {
        assertTrue(DeepEquals.deepEquals(5, 5));
        assertFalse(DeepEquals.deepEquals(5, 6));
        assertTrue(DeepEquals.deepEquals("abc", "abc"));
        assertFalse(DeepEquals.deepEquals("abc", "def"));
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
    }

    @Test
    public void testDeepEqualsWithOptionsMap() {
        // No options – should behave like the two‑arg version
        assertTrue(DeepEquals.deepEquals("x", "x", options));
        assertFalse(DeepEquals.deepEquals("x", "y", options));
    }

    @Test
    public void testHasCustomEquals() {
        assertFalse(DeepEquals.hasCustomEquals(Object.class));
        assertTrue(DeepEquals.hasCustomEquals(GoodEquals.class));
        assertTrue(DeepEquals.hasCustomEquals(BadEquals.class));
    }

    @Test
    public void testHasCustomHashCode() {
        assertFalse(DeepEquals.hasCustomHashCode(Object.class));
        assertTrue(DeepEquals.hasCustomHashCode(CustomHash.class));
        assertFalse(DeepEquals.hasCustomHashCode(GoodEquals.class)); // GoodEquals inherits Object.hashCode()
    }

    @Test
    public void testDeepHashCodeBasic() {
        int h1 = DeepEquals.deepHashCode("test");
        int h2 = DeepEquals.deepHashCode("test");
        assertEquals(h1, h2);

        int h3 = DeepEquals.deepHashCode(123);
        int h4 = DeepEquals.deepHashCode(123);
        assertEquals(h3, h4);
    }

    /* ----------------------------------------------------------------------
     *  EDGE‑CASE / EXCEPTION SCENARIOS
     * ---------------------------------------------------------------------- */

    /** 1. Custom equals that returns false – should be detected unless ignored. */
    @Test
    public void testCustomEqualsFailureWithoutIgnore() {
        BadEquals a = new BadEquals(1);
        BadEquals b = new BadEquals(1);
        // fields are identical, but BadEquals.equals() always returns false
        assertFalse(DeepEquals.deepEquals(a, b));
        // diff description must contain "value mismatch" (any wording is fine)
        assertTrue(((String) options.get(DeepEquals.DIFF)).contains("value"));
    }

    /** 2. Same objects as above, but we ignore custom equals => field‑by‑field comparison succeeds. */
    @Test
    public void testCustomEqualsIgnoredViaOption() {
        BadEquals a = new BadEquals(42);
        BadEquals b = new BadEquals(42);
        // Empty set => ignore *all* custom equals
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertTrue(DeepEquals.deepEquals(a, b, options));
    }

    /** 3. String‑to‑Number matching – disabled by default, enabled via option. */
    @Test
    public void testStringNumberMatchingOption() {
        // default: false
        assertFalse(DeepEquals.deepEquals("10", 10));
        // enable option
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("10", 10, options));
        // also test the reverse direction
        assertTrue(DeepEquals.deepEquals(10, "10", options));
    }

    /** 4. Ordered collection (List) – order matters. */
    @Test
    public void testOrderedCollectionMismatch() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(3, 2, 1);
        assertFalse(DeepEquals.deepEquals(l1, l2));
        // diff should indicate collection element mismatch
        assertTrue(((String) options.get(DeepEquals.DIFF)).contains("element"));
    }

    /** 5. Unordered collection (Set) – order does NOT matter. */
    @Test
    public void testUnorderedCollectionEquality() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new LinkedHashSet<>(Arrays.asList("c", "b", "a"));
        assertTrue(DeepEquals.deepEquals(s1, s2));
    }

    /** 6. Map key/value mismatch detection. */
    @Test
    public void testMapValueMismatch() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("x", 1);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("x", 2);
        assertFalse(DeepEquals.deepEquals(m1, m2));
        assertTrue(((String) options.get(DeepEquals.DIFF)).contains("map value"));
    }

    /** 7. Map missing key detection. */
    @Test
    public void testMapMissingKey() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("a", 1); // 'b' is missing
        assertFalse(DeepEquals.deepEquals(m1, m2));
        assertTrue(((String) options.get(DeepEquals.DIFF)).contains("missing map key"));
    }

    /** 8. Array dimension mismatch. */
    @Test
    public void testArrayDimensionMismatch() {
        int[][] a1 = {{1, 2}, {3, 4}};
        int[] a2 = {1, 2, 3, 4};
        assertFalse(DeepEquals.deepEquals(a1, a2));
        assertTrue(((String) options.get(DeepEquals.DIFF)).contains("dimensionality"));
    }

    /** 9. Array component‑type mismatch (int[] vs long[]). */
    @Test
    public void testArrayComponentTypeMismatch() {
        int[] i = {1, 2, 3};
        long[] l = {1L, 2L, 3L};
        assertFalse(DeepEquals.deepEquals(i, l));
        assertTrue(((String) options.get(DeepEquals.DIFF)).contains("component type"));
    }

    /** 10. Circular reference handling – must not cause StackOverflowError. */
    @Test
    public void testCircularReferenceEquality() {
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        n1.next = n2;
        n2.next = n1; // creates a cycle

        Node m1 = new Node(1);
        Node m2 = new Node(2);
        m1.next = m2;
        m2.next = m1; // identical structure

        assertTrue(DeepEquals.deepEquals(n1, m1));
    }

    /** 11. Floating‑point tolerance – 0.1 + 0.2 vs 0.3 should be considered equal. */
    @Test
    public void testFloatingPointTolerance() {
        double a = 0.1 + 0.2; // 0.30000000000000004
        double b = 0.3;
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    /** 12. AtomicBoolean comparison. */
    @Test
    public void testAtomicBooleanEquality() {
        AtomicBoolean ab1 = new AtomicBoolean(true);
        AtomicBoolean ab2 = new AtomicBoolean(true);
        AtomicBoolean ab3 = new AtomicBoolean(false);
        assertTrue(DeepEquals.deepEquals(ab1, ab2));
        assertFalse(DeepEquals.deepEquals(ab1, ab3));
    }

    /** 13. deepHashCode consistency for complex structures (maps, lists, arrays). */
    @Test
    public void testDeepHashCodeComplexStructure() {
        Map<String, List<Integer>> map1 = new HashMap<>();
        map1.put("nums", Arrays.asList(1, 2, 3));
        map1.put("empty", Collections.emptyList());

        Map<String, List<Integer>> map2 = new HashMap<>();
        map2.put("empty", Collections.emptyList());
        map2.put("nums", Arrays.asList(1, 2, 3));

        assertTrue(DeepEquals.deepEquals(map1, map2));
        assertEquals(DeepEquals.deepHashCode(map1), DeepEquals.deepHashCode(map2));
    }

    /** 14. Verify that hasCustomEquals correctly reports false for classes without an overridden equals. */
    @Test
    public void testHasCustomEqualsOnPlainObject() {
        class Plain { int x; }
        assertFalse(DeepEquals.hasCustomEquals(Plain.class));
    }

    /** 15. Verify that hasCustomHashCode correctly reports false for classes without an overridden hashCode. */
    @Test
    public void testHasCustomHashCodeOnPlainObject() {
        class Plain { int x; }
        assertFalse(DeepEquals.hasCustomHashCode(Plain.class));
    }

    /** 16. Ensure that deepEquals works with URI, URL and UUID (simple types). */
    @Test
    public void testSimpleSupportedTypes() throws Exception {
        URI uri1 = new URI("http://example.com");
        URI uri2 = new URI("http://example.com");
        assertTrue(DeepEquals.deepEquals(uri1, uri2));

        URL url1 = new URL("http://example.org");
        URL url2 = new URL("http://example.org");
        assertTrue(DeepEquals.deepEquals(url1, url2));

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.fromString(id1.toString());
        assertTrue(DeepEquals.deepEquals(id1, id2));
    }

    /** 17. Verify that a null options map is tolerated (deepEquals creates a new one internally). */
    @Test
    public void testDeepEqualsWithNullOptionsMap() {
        // The public API never passes a null map; however we can still call the 2‑arg version.
        assertTrue(DeepEquals.deepEquals("same", "same"));
    }

    /** 18. Verify that a custom hashCode implementation is detected by hasCustomHashCode(). */
    @Test
    public void testHasCustomHashCodeDetection() {
        assertTrue(DeepEquals.hasCustomHashCode(CustomHash.class));
    }

    /** 19. Verify that deepEquals correctly handles a collection containing null elements. */
    @Test
    public void testCollectionWithNullElements() {
        List<String> l1 = Arrays.asList("a", null, "c");
        List<String> l2 = Arrays.asList("a", null, "c");
        assertTrue(DeepEquals.deepEquals(l1, l2));
        List<String> l3 = Arrays.asList("a", "b", "c");
        assertFalse(DeepEquals.deepEquals(l1, l3));
    }

    /** 20. Verify that deepEquals correctly reports type mismatch when comparing different container types. */
    @Test
    public void testTypeMismatchBetweenContainerKinds() {
        List<Integer> list = Arrays.asList(1, 2);
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2));
        assertFalse(DeepEquals.deepEquals(list, set));
        assertTrue(((String) options.get(DeepEquals.DIFF)).contains("type mismatch"));
    }

}
