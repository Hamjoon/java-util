package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DeepEquals}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic null / primitive / simple type handling</li>
 *   <li>Floating‑point tolerance</li>
 *   <li>String‑to‑Number matching (option enabled / disabled)</li>
 *   <li>Custom {@code equals()} handling (ignored vs used)</li>
 *   <li>Ordered vs unordered collections</li>
 *   <li>Map value / key mismatches</li>
 *   <li>Arrays (including multi‑dimensional)</li>
 *   <li>Circular reference detection</li>
 *   <li>Deep hash‑code consistency</li>
 *   <li>Detection of custom {@code equals()} and {@code hashCode()}</li>
 * </ul>
 */
public class DeepEqualsTest {

    private static final String OPTIONS_DIFF = DeepEquals.DIFF;
    private static final String OPTIONS_IGNORE = DeepEquals.IGNORE_CUSTOM_EQUALS;
    private static final String OPTIONS_STRINGS_TO_NUMBERS = DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS;

    /** Helper class that overrides {@code equals} and {@code hashCode}. */
    private static class WithCustomEquals {
        int id;
        String name;

        WithCustomEquals(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            // deliberately wrong implementation for testing purposes
            if (this == o) return true;
            if (!(o instanceof WithCustomEquals)) return false;
            WithCustomEquals that = (WithCustomEquals) o;
            return id == that.id && Objects.equals(name, that.name) && false; // always false when fields match
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

    /** Helper class with a proper {@code equals} implementation (used to verify that DeepEquals can ignore it). */
    private static class ProperEquals {
        int x;
        String y;

        ProperEquals(int x, String y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProperEquals)) return false;
            ProperEquals that = (ProperEquals) o;
            return x == that.x && Objects.equals(y, that.y);
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /** Simple node class to create circular graphs. */
    private static class Node {
        String value;
        Node next;

        Node(String value) {
            this.value = value;
        }
    }

    @Before
    public void setUp() {
        // Ensure that the thread‑local formatting stack is cleared before each test.
        // DeepEquals does this internally, but we call it explicitly to be safe.
        DeepEquals.deepEquals(null, null);
    }

    /* ---------------------------------------------------------------------- */
    /*  1. Simple null / primitive / simple type handling                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNullHandling() {
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, new Object()));

        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals("foo", null, opts));
        assertNotNull(opts.get(OPTIONS_DIFF));

        assertTrue(DeepEquals.deepEquals(42, 42));
        assertFalse(DeepEquals.deepEquals(42, 43));
    }

    @Test
    public void testSimpleObjectsEquality() {
        assertTrue(DeepEquals.deepEquals("hello", "hello"));
        assertFalse(DeepEquals.deepEquals("hello", "world"));

        assertTrue(DeepEquals.deepEquals(true, true));
        assertFalse(DeepEquals.deepEquals(true, false));

        Date d1 = new Date(1_600_000_000_000L);
        Date d2 = new Date(1_600_000_000_000L);
        Date d3 = new Date(1_600_000_000_001L);
        assertTrue(DeepEquals.deepEquals(d1, d2));
        assertFalse(DeepEquals.deepEquals(d1, d3));
    }

    /* ---------------------------------------------------------------------- */
    /*  2. Floating‑point tolerance                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFloatingPointTolerance() {
        // 0.1 + 0.2 is not exactly 0.3, but DeepEquals should treat them as equal.
        double a = 0.1 + 0.2;
        double b = 0.3;
        assertTrue(DeepEquals.deepEquals(a, b));

        // Float tolerance
        float f1 = 1.000001f;
        float f2 = 1.000002f;
        assertTrue(DeepEquals.deepEquals(f1, f2));
    }

    /* ---------------------------------------------------------------------- */
    /*  3. String‑to‑Number matching (option)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testStringNumberMatchingOptionDisabled() {
        Map<String, Object> opts = new HashMap<>();
        // option defaults to false; do not set it
        assertFalse(DeepEquals.deepEquals("123", 123, opts));
        assertNotNull(opts.get(OPTIONS_DIFF));
    }

    @Test
    public void testStringNumberMatchingOptionEnabled() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(OPTIONS_STRINGS_TO_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("123", 123, opts));

        // also works the other way round
        assertTrue(DeepEquals.deepEquals(123, "123", opts));
    }

    /* ---------------------------------------------------------------------- */
    /*  4. Custom equals handling (ignore / use)                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCustomEqualsUsedByDefault() {
        WithCustomEquals a = new WithCustomEquals(1, "one");
        WithCustomEquals b = new WithCustomEquals(1, "one");

        // By default DeepEquals respects custom equals, which deliberately returns false.
        assertFalse(DeepEquals.deepEquals(a, b));

        // When we tell DeepEquals to ignore custom equals for this class,
        // it falls back to field‑by‑field comparison and should succeed.
        Map<String, Object> opts = new HashMap<>();
        opts.put(OPTIONS_IGNORE, Collections.emptySet()); // empty set => ignore all custom equals
        assertTrue(DeepEquals.deepEquals(a, b, opts));
    }

    @Test
    public void testCustomEqualsSelectiveIgnore() {
        ProperEquals a = new ProperEquals(10, "ten");
        ProperEquals b = new ProperEquals(10, "ten");

        // Default: custom equals is used, objects are equal.
        assertTrue(DeepEquals.deepEquals(a, b));

        // Ignore this specific class – DeepEquals must compare fields.
        Map<String, Object> opts = new HashMap<>();
        opts.put(OPTIONS_IGNORE, Set.of(ProperEquals.class));
        assertTrue(DeepEquals.deepEquals(a, b, opts));
    }

    /* ---------------------------------------------------------------------- */
    /*  5. Ordered collections (List)                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testOrderedCollectionMatchAndMismatch() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        List<Integer> list3 = Arrays.asList(1, 3, 2);

        assertTrue(DeepEquals.deepEquals(list1, list2));

        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(list1, list3, opts));
        assertNotNull(opts.get(OPTIONS_DIFF));
        String diff = (String) opts.get(OPTIONS_DIFF);
        assertTrue(diff.contains("collection element mismatch"));
    }

    /* ---------------------------------------------------------------------- */
    /*  6. Unordered collections (Set)                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testUnorderedCollectionEquality() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("c", "b", "a"));
        assertTrue(DeepEquals.deepEquals(s1, s2));

        Set<String> s3 = new HashSet<>(Arrays.asList("a", "b"));
        assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    /* ---------------------------------------------------------------------- */
    /*  7. Map handling (key/value mismatch)                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMapValueMismatch() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new HashMap<>();
        m2.put("one", 1);
        m2.put("two", 22); // different value

        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m1, m2, opts));
        assertNotNull(opts.get(OPTIONS_DIFF));
        String diff = (String) opts.get(OPTIONS_DIFF);
        assertTrue(diff.contains("map value mismatch"));
    }

    @Test
    public void testMapMissingKey() {
        Map<String, Integer> m1 = Map.of("a", 1, "b", 2);
        Map<String, Integer> m2 = Map.of("a", 1);
        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m1, m2, opts));
        assertNotNull(opts.get(OPTIONS_DIFF));
        assertTrue(((String) opts.get(OPTIONS_DIFF)).contains("missing map key"));
    }

    /* ---------------------------------------------------------------------- */
    /*  8. Array handling (including multi‑dimensional)                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testOneDimensionalArrayEquality() {
        int[] a = {1, 2, 3};
        int[] b = {1, 2, 3};
        int[] c = {1, 2, 4};

        assertTrue(DeepEquals.deepEquals(a, b));
        assertFalse(DeepEquals.deepEquals(a, c));
    }

    @Test
    public void testMultiDimensionalArrayEquality() {
        int[][] a = {{1, 2}, {3, 4}};
        int[][] b = {{1, 2}, {3, 4}};
        int[][] c = {{1, 2}, {3, 5}};

        assertTrue(DeepEquals.deepEquals(a, b));
        assertFalse(DeepEquals.deepEquals(a, c));
    }

    @Test
    public void testArrayDimensionMismatch() {
        int[] a = {1, 2, 3};
        int[][] b = {{1, 2, 3}};
        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a, b, opts));
        assertNotNull(opts.get(OPTIONS_DIFF));
        assertTrue(((String) opts.get(OPTIONS_DIFF)).contains("array dimensionality mismatch"));
    }

    /* ---------------------------------------------------------------------- */
    /*  9. Circular reference detection                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCircularReferenceEquality() {
        Node n1 = new Node("root");
        Node n2 = new Node("root");
        n1.next = n1; // self‑reference
        n2.next = n2;

        assertTrue(DeepEquals.deepEquals(n1, n2));
    }

    @Test
    public void testCircularReferenceMismatch() {
        Node n1 = new Node("root");
        Node n2 = new Node("root");
        Node n3 = new Node("different");
        n1.next = n1;
        n2.next = n3; // different object in the cycle

        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(n1, n2, opts));
        assertNotNull(opts.get(OPTIONS_DIFF));
        assertTrue(((String) opts.get(OPTIONS_DIFF)).contains("field value mismatch"));
    }

    /* ---------------------------------------------------------------------- */
    /* 10. Deep hash‑code consistency                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeConsistency() {
        // Simple objects
        String s1 = "test";
        String s2 = new String("test");
        assertEquals(DeepEquals.deepHashCode(s1), DeepEquals.deepHashCode(s2));

        // Collections
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        assertEquals(DeepEquals.deepHashCode(l1), DeepEquals.deepHashCode(l2));

        // Maps with same content but different order
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("b", 2);
        m2.put("a", 1);
        assertEquals(DeepEquals.deepHashCode(m1), DeepEquals.deepHashCode(m2));

        // Objects with custom hashCode – deepHashCode should use that method
        WithCustomEquals obj1 = new WithCustomEquals(5, "five");
        WithCustomEquals obj2 = new WithCustomEquals(5, "five");
        assertEquals(obj1.hashCode(), obj2.hashCode());
        assertEquals(DeepEquals.deepHashCode(obj1), DeepEquals.deepHashCode(obj2));
    }

    /* ---------------------------------------------------------------------- */
    /* 11. Detection of custom equals / hashCode methods                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHasCustomEqualsDetection() {
        assertTrue(DeepEquals.hasCustomEquals(WithCustomEquals.class));
        assertTrue(DeepEquals.hasCustomEquals(ProperEquals.class));
        // java.lang.String does NOT override equals (it does, but from Object -> actually it does,
        // but the implementation is in java.lang.String, so it is considered custom)
        assertTrue(DeepEquals.hasCustomEquals(String.class));
        // A plain POJO without overridden equals
        class Plain { int x; }
        assertFalse(DeepEquals.hasCustomEquals(Plain.class));
    }

    @Test
    public void testHasCustomHashCodeDetection() {
        assertTrue(DeepEquals.hasCustomHashCode(WithCustomEquals.class));
        assertTrue(DeepEquals.hasCustomHashCode(ProperEquals.class));
        // java.util.ArrayList overrides hashCode
        assertTrue(DeepEquals.hasCustomHashCode(ArrayList.class));
        // Simple POJO without hashCode override
        class Plain { int x; }
        assertFalse(DeepEquals.hasCustomHashCode(Plain.class));
    }
}
