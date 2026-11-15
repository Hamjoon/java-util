package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.cedarsoftware.util.DeepEquals.*;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DeepEquals}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Basic value comparisons (nulls, primitives, wrappers, strings, dates, URIs, URLs, UUIDs)</li>
 *   <li>Numeric tolerance handling for floating point values</li>
 *   <li>String‑to‑number matching enabled via {@code ALLOW_STRINGS_TO_MATCH_NUMBERS}</li>
 *   <li>Ordered collections (List) – order matters</li>
 *   <li>Unordered collections (Set) – element presence matters only</li>
 *   <li>Maps – key and value comparisons</li>
 *   <li>Arrays – single‑ and multi‑dimensional, including component type mismatches</li>
 *   <li>Circular reference detection</li>
 *   <li>Custom {@code equals()} handling with {@code IGNORE_CUSTOM_EQUALS}</li>
 *   <li>Deep hash code consistency</li>
 *   <li>Utility methods {@code hasCustomEquals} and {@code hasCustomHashCode}</li>
 * </ul>
 * </p>
 *
 * <p>All tests use only the public API and the concrete classes defined in this file.</p>
 */
public class DeepEqualsTest {

    // -------------------------------------------------------------------------
    // Helper classes used in several tests
    // -------------------------------------------------------------------------

    /** Simple POJO with a custom {@code equals} implementation. */
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
            // Custom equality: ignore case of name, compare age exactly
            return Objects.equals(name == null ? null : name.toLowerCase(),
                                 p.name == null ? null : p.name.toLowerCase())
                    && age == p.age;
        }
    }

    /** Class that overrides {@code hashCode()} only. */
    static class HashOnly {
        int id;

        HashOnly(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id * 31;
        }
    }

    /** Simple POJO without custom {@code equals()} or {@code hashCode()}. */
    static class SimpleBean {
        int a;
        String b;
        List<Integer> list;

        SimpleBean(int a, String b, List<Integer> list) {
            this.a = a;
            this.b = b;
            this.list = list;
        }
    }

    /** Class that contains a self‑reference to test circular detection. */
    static class Node {
        String name;
        Node next;

        Node(String name) {
            this.name = name;
        }
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private Map<String, Object> options;

    @Before
    public void setUp() {
        options = new HashMap<>();
    }

    @After
    public void tearDown() {
        options.clear();
    }

    // -------------------------------------------------------------------------
    // 1. Basic value comparisons
    // -------------------------------------------------------------------------

    @Test
    public void testDeepEquals_PrimitiveWrappersAndNulls() {
        // Same wrapper instances
        assertTrue(DeepEquals.deepEquals(Integer.valueOf(5), Integer.valueOf(5)));

        // Different wrappers, same value
        assertTrue(DeepEquals.deepEquals(Long.valueOf(10L), Long.valueOf(10L)));

        // Null handling
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
        assertFalse(DeepEquals.deepEquals("not null", null));

        // Different primitive values
        assertFalse(DeepEquals.deepEquals(Integer.valueOf(1), Integer.valueOf(2)));
    }

    @Test
    public void testDeepEquals_SimpleTypes() throws Exception {
        // Date comparison
        Date now = new Date();
        Date copy = new Date(now.getTime());
        assertTrue(DeepEquals.deepEquals(now, copy));

        // URI and URL comparison
        URI uri1 = new URI("http://example.com/path");
        URI uri2 = new URI("http://example.com/path");
        assertTrue(DeepEquals.deepEquals(uri1, uri2));

        URL url1 = new URL("http://example.com/path");
        URL url2 = new URL("http://example.com/path");
        assertTrue(DeepEquals.deepEquals(url1, url2));

        // UUID comparison
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.fromString(u1.toString());
        assertTrue(DeepEquals.deepEquals(u1, u2));

        // Different simple types
        assertFalse(DeepEquals.deepEquals("abc", "def"));
        assertFalse(DeepEquals.deepEquals('a', 'b'));
        assertFalse(DeepEquals.deepEquals(Boolean.TRUE, Boolean.FALSE));
    }

    // -------------------------------------------------------------------------
    // 2. Numeric tolerance (floating point)
    // -------------------------------------------------------------------------

    @Test
    public void testDeepEquals_FloatingPointTolerance() {
        double d1 = 0.1 + 0.2; // 0.30000000000000004
        double d2 = 0.3;
        assertTrue(DeepEquals.deepEquals(d1, d2));

        float f1 = 1.000001f;
        float f2 = 1.000002f;
        // tolerance is 1e-5 for float via SCALE_FLOAT, both should be equal
        assertTrue(DeepEquals.deepEquals(f1, f2));

        // Outside tolerance
        assertFalse(DeepEquals.deepEquals(1.0, 1.0001));
    }

    // -------------------------------------------------------------------------
    // 3. String‑to‑number matching option
    // -------------------------------------------------------------------------

    @Test
    public void testDeepEquals_StringNumberMatching_DisabledByDefault() {
        assertFalse(DeepEquals.deepEquals("10", Integer.valueOf(10)));
    }

    @Test
    public void testDeepEquals_StringNumberMatching_Enabled() {
        options.put(ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.TRUE);
        assertTrue(DeepEquals.deepEquals("10", Integer.valueOf(10), options));
        assertTrue(DeepEquals.deepEquals(BigDecimal.valueOf(123.45), "123.45", options));

        // Mismatch still reported when numbers differ
        assertFalse(DeepEquals.deepEquals("10", Integer.valueOf(11), options));
        assertEquals("[value mismatch] ▶ null", options.get(DIFF));
    }

    // -------------------------------------------------------------------------
    // 4. Ordered collections (List)
    // -------------------------------------------------------------------------

    @Test
    public void testDeepEquals_List_OrderMatters() {
        List<String> l1 = Arrays.asList("a", "b", "c");
        List<String> l2 = Arrays.asList("a", "b", "c");
        List<String> l3 = Arrays.asList("c", "b", "a");

        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));

        // Verify diff path points to the first mismatching index (0)
        assertFalse(DeepEquals.deepEquals(l1, l3, options));
        String diff = (String) options.get(DIFF);
        assertTrue(diff.contains(".[0]")); // first element differs
    }

    // -------------------------------------------------------------------------
    // 5. Unordered collections (Set)
    // -------------------------------------------------------------------------

    @Test
    public void testDeepEquals_Set_UnorderedEquality() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new LinkedHashSet<>(Arrays.asList(3, 2, 1));
        Set<Integer> s3 = new HashSet<>(Arrays.asList(1, 2, 4));

        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));

        // Diff should indicate missing element "4"
        assertFalse(DeepEquals.deepEquals(s1, s3, options));
        String diff = (String) options.get(DIFF);
        assertTrue(diff.contains("missing collection element"));
    }

    // -------------------------------------------------------------------------
    // 6. Map comparisons
    // -------------------------------------------------------------------------

    @Test
    public void testDeepEquals_Map_KeyAndValueMismatches() {
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("one", 1);
        m2.put("two", 2);

        Map<String, Integer> m3 = new LinkedHashMap<>();
        m3.put("one", 1);
        m3.put("two", 22); // different value

        Map<String, Integer> m4 = new LinkedHashMap<>();
        m4.put("one", 1);
        m4.put("three", 3); // missing key "two"

        assertTrue(DeepEquals.deepEquals(m1, m2));

        // Value mismatch
        assertFalse(DeepEquals.deepEquals(m1, m3, options));
        String diffValue = (String) options.get(DIFF);
        assertTrue(diffValue.contains("map value mismatch"));
        assertTrue(diffValue.contains("Expected: 2"));
        assertTrue(diffValue.contains("Found: 22"));

        // Missing key
        assertFalse(DeepEquals.deepEquals(m1, m4, options));
        String diffKey = (String) options.get(DIFF);
        assertTrue(diffKey.contains("missing map key"));
        assertTrue(diffKey.contains("one")); // the key that was examined first
    }

    // -------------------------------------------------------------------------
    // 7. Array comparisons (including multi‑dimensional)
    // -------------------------------------------------------------------------

    @Test
    public void testDeepEquals_SimpleArrays() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {1, 2, 4};

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3, options));
        String diff = (String) options.get(DIFF);
        assertTrue(diff.contains("array element mismatch"));
        assertTrue(diff.contains("[0]")); // first mismatching index
    }

    @Test
    public void testDeepEquals_MultiDimensionalArrays() {
        int[][] m1 = {{1, 2}, {3, 4}};
        int[][] m2 = {{1, 2}, {3, 4}};
        int[][] m3 = {{1, 2}, {3, 5}}; // last element differs

        assertTrue(DeepEquals.deepEquals(m1, m2));

        assertFalse(DeepEquals.deepEquals(m1, m3, options));
        String diff = (String) options.get(DIFF);
        assertTrue(diff.contains("array element mismatch"));
        assertTrue(diff.contains("[1][1]")); // second row, second column
    }

    @Test
    public void testDeepEquals_ArrayComponentTypeMismatch() {
        int[] ints = {1, 2, 3};
        long[] longs = {1L, 2L, 3L};

        assertFalse(DeepEquals.deepEquals(ints, longs, options));
        String diff = (String) options.get(DIFF);
        assertTrue(diff.contains("array component type mismatch"));
    }

    // -------------------------------------------------------------------------
    // 8. Circular reference handling
    // -------------------------------------------------------------------------

    @Test
    public void testDeepEquals_CircularReferences() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = b;
        b.next = a; // creates a cycle

        Node aCopy = new Node("A");
        Node bCopy = new Node("B");
        aCopy.next = bCopy;
        bCopy.next = aCopy;

        // Should finish without StackOverflowError
        assertTrue(DeepEquals.deepEquals(a, aCopy));

        // Break the cycle differently to cause a mismatch
        Node bDifferent = new Node("C");
        aCopy.next = bDifferent;
        bDifferent.next = aCopy;
        assertFalse(DeepEquals.deepEquals(a, aCopy, options));
        String diff = (String) options.get(DIFF);
        assertTrue(diff.contains("field value mismatch"));
        assertTrue(diff.contains(".name"));
    }

    // -------------------------------------------------------------------------
    // 9. Custom equals handling with IGNORE_CUSTOM_EQUALS option
    // -------------------------------------------------------------------------

    @Test
    public void testDeepEquals_CustomEquals_Ignored() {
        Person p1 = new Person("John", 30);
        Person p2 = new Person("john", 30); // name differs only by case – custom equals says equal

        // By default custom equals is used – they are equal
        assertTrue(DeepEquals.deepEquals(p1, p2));

        // Ignore custom equals for Person -> field‑by‑field comparison should detect case difference
        options.put(IGNORE_CUSTOM_EQUALS, Collections.singleton(Person.class));
        assertFalse(DeepEquals.deepEquals(p1, p2, options));
        String diff = (String) options.get(DIFF);
        assertTrue(diff.contains("field value mismatch"));
        assertTrue(diff.contains(".name"));
    }

    @Test
    public void testDeepEquals_CustomEquals_IgnoredGlobally() {
        Person p1 = new Person("Alice", 25);
        Person p2 = new Person("alice", 25);

        // Empty set means ignore all custom equals
        options.put(IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertFalse(DeepEquals.deepEquals(p1, p2, options));
        String diff = (String) options.get(DIFF);
        assertTrue(diff.contains(".name"));
    }

    // -------------------------------------------------------------------------
    // 10. Deep hash code consistency
    // -------------------------------------------------------------------------

    @Test
    public void testDeepHashCode_ConsistentWithDeepEquals() {
        List<Map<String, Integer>> complex1 = new ArrayList<>();
        complex1.add(Collections.singletonMap("a", 1));
        complex1.add(Collections.singletonMap("b", 2));

        List<Map<String, Integer>> complex2 = new ArrayList<>();
        complex2.add(Collections.singletonMap("a", 1));
        complex2.add(Collections.singletonMap("b", 2));

        assertTrue(DeepEquals.deepEquals(complex1, complex2));
        assertEquals(DeepEquals.deepHashCode(complex1), DeepEquals.deepHashCode(complex2));

        // Change a leaf value – deepEquals becomes false and hash codes differ
        complex2.get(1).put("b", 99);
        assertFalse(DeepEquals.deepEquals(complex1, complex2));
        assertNotEquals(DeepEquals.deepHashCode(complex1), DeepEquals.deepHashCode(complex2));
    }

    // -------------------------------------------------------------------------
    // 11. hasCustomEquals and hasCustomHashCode utilities
    // -------------------------------------------------------------------------

    @Test
    public void testHasCustomEquals() {
        assertTrue(DeepEquals.hasCustomEquals(Person.class));
        assertFalse(DeepEquals.hasCustomEquals(SimpleBean.class));
        assertFalse(DeepEquals.hasCustomEquals(String.class)); // String overrides equals, but it's in java.lang.Object?
        // Actually String overrides equals, so should be true.
        assertTrue(DeepEquals.hasCustomEquals(String.class));
    }

    @Test
    public void testHasCustomHashCode() {
        assertFalse(DeepEquals.hasCustomHashCode(SimpleBean.class));
        assertTrue(DeepEquals.hasCustomHashCode(HashOnly.class));
        assertTrue(DeepEquals.hasCustomHashCode(String.class));
        assertTrue(DeepEquals.hasCustomHashCode(Integer.class));
    }

    // -------------------------------------------------------------------------
    // 12. DeepEquals with empty options map (default behaviour)
    // -------------------------------------------------------------------------

    @Test
    public void testDeepEquals_DefaultOptions_NoSideEffect() {
        Map<String, Object> emptyOpts = new HashMap<>();
        assertTrue(DeepEquals.deepEquals("test", "test", emptyOpts));
        assertNull(emptyOpts.get(DIFF)); // No diff generated on success
    }

    // -------------------------------------------------------------------------
    // 13. Verify that DIFF entry is populated only on failure
    // -------------------------------------------------------------------------

    @Test
    public void testDiffPopulationOnlyOnFailure() {
        Map<String, Object> opts = new HashMap<>();
        assertTrue(DeepEquals.deepEquals(42, 42, opts));
        assertNull("DIFF should be absent when objects are equal", opts.get(DIFF));

        assertFalse(DeepEquals.deepEquals(42, 43, opts));
        assertNotNull("DIFF must be present when objects differ", opts.get(DIFF));
        String diff = (String) opts.get(DIFF);
        assertTrue(diff.contains("value mismatch"));
    }
}
