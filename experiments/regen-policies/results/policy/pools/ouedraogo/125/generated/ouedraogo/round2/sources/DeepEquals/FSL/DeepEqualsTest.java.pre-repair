package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Comprehensive JUnit‑4 test suite for {@link DeepEquals}.
 *
 * The tests cover:
 * <ul>
 *   <li>Simple primitive / wrapper / String comparisons</li>
 *   <li>Floating‑point tolerance</li>
 *   <li>String‑to‑Number matching (option enabled / disabled)</li>
 *   <li>Custom {@code equals()} handling and the {@code IGNORE_CUSTOM_EQUALS} option</li>
 *   <li>Map, List, Set and array differences – including diff‑description validation</li>
 *   <li>Circular reference detection</li>
 *   <li>Deep hash‑code consistency</li>
 *   <li>Utility methods {@code hasCustomEquals} and {@code hasCustomHashCode}</li>
 * </ul>
 *
 * All objects are created using only the public API present in the source file
 * (or standard JDK classes). No mocking framework is used.
 */
public class DeepEqualsTest {

    /* ---------------------------------------------------------------------- */
    /* Helper POJOs used by several tests                                      */
    /* ---------------------------------------------------------------------- */

    /** Simple POJO without a custom {@code equals()} implementation. */
    public static class SimplePojo {
        public int id;
        public String name;

        public SimplePojo(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /** POJO that deliberately implements a *bad* {@code equals()} (always false). */
    public static class BadEqualsPojo {
        public int id;
        public String label;

        public BadEqualsPojo(int id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public boolean equals(Object o) {
            // Intentionally always return false to force DeepEquals to fall back to field‑by‑field comparison
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, label);
        }
    }

    /** Node class for circular‑reference testing. */
    public static class Node {
        public Node next;
        public String value;

        public Node(String value) {
            this.value = value;
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 1. Simple primitive / wrapper / String equality                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSimpleEquality() {
        assertTrue(DeepEquals.deepEquals(null, null));
        assertTrue(DeepEquals.deepEquals("abc", "abc"));
        assertTrue(DeepEquals.deepEquals(42, 42));
        assertTrue(DeepEquals.deepEquals(Integer.valueOf(7), Integer.valueOf(7)));
        assertTrue(DeepEquals.deepEquals(Boolean.TRUE, Boolean.TRUE));

        assertFalse(DeepEquals.deepEquals("abc", "def"));
        assertFalse(DeepEquals.deepEquals(42, 43));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
    }

    /* ---------------------------------------------------------------------- */
    /* 2. Floating‑point tolerance (nearly‑equal)                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFloatingPointTolerance() {
        double a = 0.1 + 0.2;          // 0.30000000000000004
        double b = 0.3;
        assertTrue(DeepEquals.deepEquals(a, b)); // within epsilon

        double c = 1.000000000000001;
        double d = 1.0;
        assertTrue(DeepEquals.deepEquals(c, d)); // within epsilon

        double e = 1.0000000000001;
        double f = 1.0;
        assertFalse(DeepEquals.deepEquals(e, f)); // beyond epsilon
    }

    /* ---------------------------------------------------------------------- */
    /* 3. String‑to‑Number matching (option)                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testStringNumberMatchingDisabled() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);
        assertFalse(DeepEquals.deepEquals("10", 10, options));
    }

    @Test
    public void testStringNumberMatchingEnabled() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("10", 10, options));

        // also test reverse direction
        assertTrue(DeepEquals.deepEquals(3.14, "3.14", options));
    }

    /* ---------------------------------------------------------------------- */
    /* 4. Custom equals handling                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCustomEqualsUsedByDefault() {
        BadEqualsPojo a = new BadEqualsPojo(1, "alpha");
        BadEqualsPojo b = new BadEqualsPojo(1, "alpha");

        // By default DeepEquals respects the custom equals (which always fails)
        assertFalse(DeepEquals.deepEquals(a, b));

        // Verify that a diff description is stored
        Map<String, Object> opt = new HashMap<>();
        DeepEquals.deepEquals(a, b, opt);
        assertNotNull(opt.get(DeepEquals.DIFF));
        String diff = (String) opt.get(DeepEquals.DIFF);
        assertTrue(diff.contains("value mismatch") || diff.contains("type mismatch"));
    }

    @Test
    public void testIgnoreAllCustomEquals() {
        BadEqualsPojo a = new BadEqualsPojo(2, "beta");
        BadEqualsPojo b = new BadEqualsPojo(2, "beta");

        Map<String, Object> options = new HashMap<>();
        // Empty set => ignore *all* custom equals implementations
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());

        assertTrue(DeepEquals.deepEquals(a, b, options));
    }

    @Test
    public void testIgnoreSpecificCustomEquals() {
        BadEqualsPojo a = new BadEqualsPojo(3, "gamma");
        BadEqualsPojo b = new BadEqualsPojo(3, "gamma");

        Map<String, Object> options = new HashMap<>();
        // Ignore only BadEqualsPojo – other custom equals would still be used
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(BadEqualsPojo.class));

        assertTrue(DeepEquals.deepEquals(a, b, options));
    }

    @Test
    public void testHasCustomEqualsAndHashCode() {
        assertTrue(DeepEquals.hasCustomEquals(BadEqualsPojo.class));
        assertTrue(DeepEquals.hasCustomHashCode(BadEqualsPojo.class));

        assertFalse(DeepEquals.hasCustomEquals(SimplePojo.class));
        assertFalse(DeepEquals.hasCustomHashCode(SimplePojo.class));
    }

    /* ---------------------------------------------------------------------- */
    /* 5. Collection / Map / Array differences – diff description validation    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testListSizeMismatchDiff() {
        List<String> l1 = Arrays.asList("a", "b", "c");
        List<String> l2 = Arrays.asList("a", "b");

        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(l1, l2, opt));

        String diff = (String) opt.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[collection size mismatch]"));
        assertTrue(diff.contains("Expected size: 3"));
        assertTrue(diff.contains("Found size: 2"));
    }

    @Test
    public void testMapValueMismatchDiff() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("key", 1);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("key", 2);

        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m1, m2, opt));

        String diff = (String) opt.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[map value mismatch]"));
        assertTrue(diff.contains("Expected: 1"));
        assertTrue(diff.contains("Found: 2"));
        // Path should contain the map key
        assertTrue(diff.contains("《\"key\" ⇨"));
    }

    @Test
    public void testArrayElementMismatchDiff() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 9, 3};

        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a1, a2, opt));

        String diff = (String) opt.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[array element mismatch]"));
        assertTrue(diff.contains("Expected: 2"));
        assertTrue(diff.contains("Found: 9"));
        // Path should show the index
        assertTrue(diff.contains("[2]") || diff.contains("[1]"));
    }

    @Test
    public void testArrayDimensionMismatchDiff() {
        int[][] a1 = {{1, 2}, {3, 4}};
        int[] a2 = {1, 2, 3};

        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a1, a2, opt));

        String diff = (String) opt.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[array dimensionality mismatch]"));
    }

    /* ---------------------------------------------------------------------- */
    /* 6. Circular reference handling                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCircularReferenceEquality() {
        Node n1 = new Node("root");
        Node n2 = new Node("root");
        n1.next = n2;
        n2.next = n1; // creates a cycle

        Node m1 = new Node("root");
        Node m2 = new Node("root");
        m1.next = m2;
        m2.next = m1; // identical structure

        assertTrue(DeepEquals.deepEquals(n1, m1));
    }

    @Test
    public void testCircularReferenceInequality() {
        Node n1 = new Node("root");
        Node n2 = new Node("root");
        n1.next = n2;
        n2.next = n1; // cycle

        Node m1 = new Node("different");
        Node m2 = new Node("different");
        m1.next = m2;
        m2.next = m1;

        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(n1, m1, opt));
        String diff = (String) opt.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[field value mismatch]"));
    }

    /* ---------------------------------------------------------------------- */
    /* 7. Deep hash‑code consistency                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeConsistency() {
        SimplePojo p1 = new SimplePojo(10, "alpha");
        SimplePojo p2 = new SimplePojo(10, "alpha");
        SimplePojo p3 = new SimplePojo(11, "beta");

        int h1 = DeepEquals.deepHashCode(p1);
        int h2 = DeepEquals.deepHashCode(p2);
        int h3 = DeepEquals.deepHashCode(p3);

        assertEquals(h1, h2);
        assertNotEquals(h1, h3);
    }

    @Test
    public void testDeepHashCodeWithCollections() {
        List<String> list1 = Arrays.asList("x", "y", "z");
        List<String> list2 = Arrays.asList("x", "y", "z");
        List<String> list3 = Arrays.asList("x", "y", "different");

        int h1 = DeepEquals.deepHashCode(list1);
        int h2 = DeepEquals.deepHashCode(list2);
        int h3 = DeepEquals.deepHashCode(list3);

        assertEquals(h1, h2);
        assertNotEquals(h1, h3);
    }

    /* ---------------------------------------------------------------------- */
    /* 8. Deep equals with nested objects (field decomposition)                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNestedObjectEquality() {
        SimplePojo inner1 = new SimplePojo(5, "inner");
        SimplePojo outer1 = new SimplePojo(1, "outer");
        SimplePojo inner2 = new SimplePojo(5, "inner");
        SimplePojo outer2 = new SimplePojo(1, "outer");

        // embed inner objects via a map
        Map<String, SimplePojo> map1 = new HashMap<>();
        map1.put("inner", inner1);
        Map<String, SimplePojo> map2 = new HashMap<>();
        map2.put("inner", inner2);

        Map<String, Object> container1 = new HashMap<>();
        container1.put("outer", outer1);
        container1.put("innerMap", map1);

        Map<String, Object> container2 = new HashMap<>();
        container2.put("outer", outer2);
        container2.put("innerMap", map2);

        assertTrue(DeepEquals.deepEquals(container1, container2));
    }

    @Test
    public void testNestedObjectDifferenceDiff() {
        SimplePojo inner1 = new SimplePojo(5, "inner");
        SimplePojo inner2 = new SimplePojo(6, "inner"); // id differs

        Map<String, SimplePojo> map1 = new HashMap<>();
        map1.put("i", inner1);
        Map<String, SimplePojo> map2 = new HashMap<>();
        map2.put("i", inner2);

        Map<String, Object> opt = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(map1, map2, opt));

        String diff = (String) opt.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[field value mismatch]"));
        // Path should contain the field name 'id' inside the inner POJO
        assertTrue(diff.contains(".id"));
    }

    /* ---------------------------------------------------------------------- */
    /* 9. Verify that options map is not polluted after successful compare      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testOptionsMapUnchangedOnSuccess() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("123", 123, options));

        // No diff entry should be added on success
        assertNull(options.get(DeepEquals.DIFF));
    }
}
