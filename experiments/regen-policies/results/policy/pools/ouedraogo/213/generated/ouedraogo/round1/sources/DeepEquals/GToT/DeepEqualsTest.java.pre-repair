package com.cedarsoftware.util;

import org.junit.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link DeepEquals}.
 *
 * <p>The suite covers:
 * <ul>
 *   <li>All public API methods (deepEquals overloads, hasCustomEquals, hasCustomHashCode, deepHashCode)</li>
 *   <li>Typical use‑cases (simple values, collections, maps, arrays, nested objects)</li>
 *   <li>Edge‑cases (nulls, empty containers, mismatched dimensions, circular references)</li>
 *   <li>Option handling (ignore custom equals, allow string‑to‑number matching)</li>
 *   <li>Verification of the diff description stored under {@link DeepEquals#DIFF}</li>
 * </ul>
 * </p>
 */
public class DeepEqualsTest {

    /* ---------------------------------------------------------------------- */
    /* Helper types used in the tests                                          */
    /* ---------------------------------------------------------------------- */

    /** Simple POJO with default Object.equals(). */
    static class SimpleBean {
        int id;
        String name;
        List<String> tags;

        SimpleBean(int id, String name, List<String> tags) {
            this.id = id;
            this.name = name;
            this.tags = tags;
        }
    }

    /** POJO that overrides equals() – used to test IGNORE_CUSTOM_EQUALS option. */
    static class CustomEqualsBean {
        int id;
        String data;

        CustomEqualsBean(int id, String data) {
            this.id = id;
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            // deliberately wrong: consider all instances equal regardless of fields
            return o instanceof CustomEqualsBean;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, data);
        }
    }

    /** POJO that overrides hashCode() – used to test hasCustomHashCode(). */
    static class CustomHashCodeBean {
        int value;

        CustomHashCodeBean(int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value * 31;
        }
    }

    /** POJO that references itself – used to test circular‑reference handling. */
    static class CircularBean {
        String name;
        CircularBean partner;

        CircularBean(String name) {
            this.name = name;
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 1. Tests for deepEquals(Object, Object)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsSimplePrimitives() {
        assertTrue(DeepEquals.deepEquals(null, null));
        assertTrue(DeepEquals.deepEquals(5, 5));
        assertTrue(DeepEquals.deepEquals("abc", "abc"));
        assertTrue(DeepEquals.deepEquals(true, true));

        assertFalse(DeepEquals.deepEquals(5, null));
        assertFalse(DeepEquals.deepEquals("a", "b"));
        assertFalse(DeepEquals.deepEquals(Boolean.TRUE, Boolean.FALSE));
    }

    @Test
    public void testDeepEqualsNumbersWithFloatingPointTolerance() {
        // Values that differ well within doubleEpsilon should be considered equal
        double a = 0.1 + 0.2; // 0.30000000000000004
        double b = 0.3;
        assertTrue(DeepEquals.deepEquals(a, b));

        // Large difference must be reported
        assertFalse(DeepEquals.deepEquals(1.0, 1.00000000000001));
    }

    @Test
    public void testDeepEqualsArrays() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {1, 2, 4};

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));

        // Multi‑dimensional arrays
        int[][] m1 = {{1, 2}, {3, 4}};
        int[][] m2 = {{1, 2}, {3, 4}};
        int[][] m3 = {{1, 2}, {4, 3}};
        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    @Test
    public void testDeepEqualsCollections() {
        List<String> l1 = Arrays.asList("a", "b", "c");
        List<String> l2 = Arrays.asList("a", "b", "c");
        List<String> l3 = Arrays.asList("a", "c", "b"); // order matters for List

        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));

        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 2, 1));
        Set<Integer> s3 = new HashSet<>(Arrays.asList(1, 2, 4));

        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    @Test
    public void testDeepEqualsMaps() {
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("one", 1);
        m2.put("two", 2);

        Map<String, Integer> m3 = new LinkedHashMap<>();
        m3.put("one", 1);
        m3.put("two", 3); // different value

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    @Test
    public void testDeepEqualsComplexObjects() {
        SimpleBean b1 = new SimpleBean(10, "Bob", Arrays.asList("x", "y"));
        SimpleBean b2 = new SimpleBean(10, "Bob", Arrays.asList("x", "y"));
        SimpleBean b3 = new SimpleBean(11, "Bob", Arrays.asList("x", "y"));
        SimpleBean b4 = new SimpleBean(10, "Bob", Arrays.asList("x", "z")); // different list element

        assertTrue(DeepEquals.deepEquals(b1, b2));
        assertFalse(DeepEquals.deepEquals(b1, b3));
        assertFalse(DeepEquals.deepEquals(b1, b4));
    }

    @Test
    public void testDeepEqualsCircularReferences() {
        CircularBean a = new CircularBean("A");
        CircularBean b = new CircularBean("B");
        a.partner = b;
        b.partner = a;

        CircularBean aCopy = new CircularBean("A");
        CircularBean bCopy = new CircularBean("B");
        aCopy.partner = bCopy;
        bCopy.partner = aCopy;

        assertTrue(DeepEquals.deepEquals(a, aCopy));

        // Break the cycle on the copy side – should be detected as mismatch
        bCopy.partner = null;
        assertFalse(DeepEquals.deepEquals(a, aCopy));
    }

    /* ---------------------------------------------------------------------- */
    /* 2. Tests for deepEquals(Object, Object, Map) – options handling          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsIgnoreCustomEquals() {
        CustomEqualsBean c1 = new CustomEqualsBean(1, "alpha");
        CustomEqualsBean c2 = new CustomEqualsBean(2, "beta"); // different fields

        // Without ignoring, custom equals says they are equal → deepEquals returns true
        assertTrue(DeepEquals.deepEquals(c1, c2));

        // With IGNORE_CUSTOM_EQUALS set to empty set → ignore ALL custom equals,
        // deepEquals must fall back to field‑by‑field comparison → false
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertFalse(DeepEquals.deepEquals(c1, c2, opts));

        // Ignore only this specific class → still ignored, result false
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.singleton(CustomEqualsBean.class));
        assertFalse(DeepEquals.deepEquals(c1, c2, opts));

        // Ignore a different class → custom equals is used → true
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.singleton(String.class));
        assertTrue(DeepEquals.deepEquals(c1, c2, opts));
    }

    @Test
    public void testDeepEqualsAllowStringNumberMatching() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        assertTrue(DeepEquals.deepEquals("123", 123, opts));
        assertTrue(DeepEquals.deepEquals(45.6, "45.6", opts));
        assertFalse(DeepEquals.deepEquals("12a", 12, opts)); // invalid numeric string

        // With option disabled the same comparisons must fail
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);
        assertFalse(DeepEquals.deepEquals("123", 123, opts));
    }

    @Test
    public void testDeepEqualsDiffMessage() {
        Map<String, Object> opts = new HashMap<>();
        SimpleBean b1 = new SimpleBean(1, "Bob", Arrays.asList("a"));
        SimpleBean b2 = new SimpleBean(1, "Bob", Arrays.asList("b")); // list element differs

        assertFalse(DeepEquals.deepEquals(b1, b2, opts));
        Object diff = opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        String diffStr = diff.toString();

        // The diff should mention the field that differs (tags) and show expected/found values
        assertTrue(diffStr.contains("field value mismatch"));
        assertTrue(diffStr.contains(".tags"));
        assertTrue(diffStr.contains("Expected: [\"a\"]"));
        assertTrue(diffStr.contains("Found: [\"b\"]"));
    }

    /* ---------------------------------------------------------------------- */
    /* 3. Tests for hasCustomEquals(Class<?>)                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHasCustomEquals() {
        // Class that overrides equals()
        assertTrue(DeepEquals.hasCustomEquals(CustomEqualsBean.class));

        // Class that does NOT override equals()
        assertFalse(DeepEquals.hasCustomEquals(SimpleBean.class));

        // Primitive wrapper – inherits Object.equals(), but wrapper overrides it (so it's custom)
        assertTrue(DeepEquals.hasCustomEquals(Integer.class));

        // Interface / abstract class (no concrete equals implementation)
        assertFalse(DeepEquals.hasCustomEquals(List.class));
    }

    /* ---------------------------------------------------------------------- */
    /* 4. Tests for hasCustomHashCode(Class<?>)                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHasCustomHashCode() {
        // Class with overridden hashCode()
        assertTrue(DeepEquals.hasCustomHashCode(CustomHashCodeBean.class));

        // Class without overridden hashCode()
        assertFalse(DeepEquals.hasCustomHashCode(SimpleBean.class));

        // Primitive wrapper overrides hashCode()
        assertTrue(DeepEquals.hasCustomHashCode(Long.class));

        // Abstract collection – does not override hashCode()
        assertFalse(DeepEquals.hasCustomHashCode(AbstractMap.class));
    }

    /* ---------------------------------------------------------------------- */
    /* 5. Tests for deepHashCode(Object)                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeSimpleValues() {
        assertEquals(DeepEquals.deepHashCode(null), 0);
        assertEquals(DeepEquals.deepHashCode(123), Integer.valueOf(123).hashCode());
        assertEquals(DeepEquals.deepHashCode("test"), "test".hashCode());
    }

    @Test
    public void testDeepHashCodeArraysAndCollections() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {3, 2, 1};

        assertEquals(DeepEquals.deepHashCode(a1), DeepEquals.deepHashCode(a2));
        assertNotEquals(DeepEquals.deepHashCode(a1), DeepEquals.deepHashCode(a3));

        List<String> l1 = Arrays.asList("x", "y");
        List<String> l2 = Arrays.asList("x", "y");
        List<String> l3 = Arrays.asList("y", "x"); // order matters for List

        assertEquals(DeepEquals.deepHashCode(l1), DeepEquals.deepHashCode(l2));
        assertNotEquals(DeepEquals.deepHashCode(l1), DeepEquals.deepHashCode(l3));

        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 2, 1));
        assertEquals(DeepEquals.deepHashCode(s1), DeepEquals.deepHashCode(s2));
    }

    @Test
    public void testDeepHashCodeMapsAndObjects() {
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);

        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("a", 1);
        m2.put("b", 2);

        Map<String, Integer> m3 = new LinkedHashMap<>();
        m3.put("a", 1);
        m3.put("b", 3); // different value

        assertEquals(DeepEquals.deepHashCode(m1), DeepEquals.deepHashCode(m2));
        assertNotEquals(DeepEquals.deepHashCode(m1), DeepEquals.deepHashCode(m3));

        SimpleBean sb1 = new SimpleBean(5, "Joe", Collections.singletonList("tag"));
        SimpleBean sb2 = new SimpleBean(5, "Joe", Collections.singletonList("tag"));
        SimpleBean sb3 = new SimpleBean(6, "Joe", Collections.singletonList("tag"));

        assertEquals(DeepEquals.deepHashCode(sb1), DeepEquals.deepHashCode(sb2));
        assertNotEquals(DeepEquals.deepHashCode(sb1), DeepEquals.deepHashCode(sb3));
    }

    @Test
    public void testDeepHashCodeCircularReferenceStability() {
        CircularBean a = new CircularBean("A");
        CircularBean b = new CircularBean("B");
        a.partner = b;
        b.partner = a;

        // The hash code calculation must terminate and be repeatable
        int h1 = DeepEquals.deepHashCode(a);
        int h2 = DeepEquals.deepHashCode(a);
        assertEquals(h1, h2);
    }

    /* ---------------------------------------------------------------------- */
    /* 6. Miscellaneous edge‑case tests                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsEmptyContainers() {
        assertTrue(DeepEquals.deepEquals(new int[0], new int[0]));
        assertTrue(DeepEquals.deepEquals(Collections.emptyList(), Collections.emptyList()));
        assertTrue(DeepEquals.deepEquals(Collections.emptySet(), Collections.emptySet()));
        assertTrue(DeepEquals.deepEquals(Collections.emptyMap(), Collections.emptyMap()));
    }

    @Test
    public void testDeepEqualsMismatchedTypes() {
        assertFalse(DeepEquals.deepEquals("string", 123));
        assertFalse(DeepEquals.deepEquals(new int[]{1, 2}, new long[]{1L, 2L}));
        assertFalse(DeepEquals.deepEquals(Arrays.asList(1, 2), new int[]{1, 2}));
    }

    @Test
    public void testDeepEqualsWithUriAndUrl() throws Exception {
        URI u1 = new URI("http://example.com/path");
        URI u2 = new URI("http://example.com/path");
        URL url1 = u1.toURL();
        URL url2 = u2.toURL();

        assertTrue(DeepEquals.deepEquals(u1, u2));
        assertTrue(DeepEquals.deepEquals(url1, url2));
        assertFalse(DeepEquals.deepEquals(u1, url1)); // different classes
    }

    @Test
    public void testDeepEqualsBigDecimalPrecision() {
        BigDecimal bd1 = new BigDecimal("1.0000000000000001");
        BigDecimal bd2 = new BigDecimal("1.0000000000000002"); // difference beyond double epsilon
        assertFalse(DeepEquals.deepEquals(bd1, bd2));

        BigDecimal bd3 = new BigDecimal("1.0");
        BigDecimal bd4 = new BigDecimal("1.00");
        assertTrue(DeepEquals.deepEquals(bd3, bd4));
    }
}
