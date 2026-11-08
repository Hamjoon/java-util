package com.cedarsoftware.util;

import org.junit.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.DeepEquals.*;
import static org.junit.Assert.*;

public class DeepEqualsTest {

    /*--------------------------------------------------------------------
     * Helper classes used throughout the test suite
     *--------------------------------------------------------------------*/

    /** Simple POJO with no custom equals/hashCode */
    public static class Plain {
        public int id;
        public String name;
        public List<String> tags;

        public Plain(int id, String name, List<String> tags) {
            this.id = id;
            this.name = name;
            this.tags = tags;
        }
    }

    /** POJO that overrides equals() but not hashCode() */
    public static class EqualsOnly {
        public int value;

        public EqualsOnly(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof EqualsOnly)) return false;
            return this.value == ((EqualsOnly) o).value;
        }
    }

    /** POJO that overrides both equals() and hashCode() */
    public static class FullCustom {
        public String key;
        public double amount;

        public FullCustom(String key, double amount) {
            this.key = key;
            this.amount = amount;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FullCustom)) return false;
            FullCustom other = (FullCustom) o;
            return Objects.equals(this.key, other.key) &&
                    Double.compare(this.amount, other.amount) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, amount);
        }
    }

    /** Simple class that contains a circular reference to itself */
    public static class Node {
        public String name;
        public Node next;

        public Node(String name) {
            this.name = name;
        }
    }

    /*--------------------------------------------------------------------
     * Tests for deepEquals(Object, Object) – the no‑options overload
     *--------------------------------------------------------------------*/

    @Test
    public void testDeepEquals_PrimitiveWrappers() {
        assertTrue(deepEquals(Integer.valueOf(5), Integer.valueOf(5)));
        assertFalse(deepEquals(Integer.valueOf(5), Integer.valueOf(6)));
        assertTrue(deepEquals(Boolean.TRUE, Boolean.TRUE));
        assertFalse(deepEquals(Boolean.TRUE, Boolean.FALSE));
        assertTrue(deepEquals(Character.valueOf('a'), Character.valueOf('a')));
    }

    @Test
    public void testDeepEquals_NumbersWithFloatingPointTolerance() {
        // Values that differ only beyond the epsilon should be considered equal
        double a = 0.123456789012345;
        double b = 0.123456789012346;
        assertTrue(deepEquals(a, b));

        // Values that differ beyond epsilon must be reported as different
        double c = 0.1234567890123;
        double d = 0.1234567890129;
        assertFalse(deepEquals(c, d));
    }

    @Test
    public void testDeepEquals_StringNumberMatchingDisabledByDefault() {
        assertFalse(deepEquals("10", Integer.valueOf(10)));
        assertFalse(deepEquals(Integer.valueOf(10), "10"));
    }

    @Test
    public void testDeepEquals_NullHandling() {
        assertTrue(deepEquals(null, null));
        assertFalse(deepEquals(null, "not null"));
        assertFalse(deepEquals("not null", null));
    }

    @Test
    public void testDeepEquals_AtomicTypes() {
        assertTrue(deepEquals(new AtomicBoolean(true), new AtomicBoolean(true)));
        assertFalse(deepEquals(new AtomicBoolean(true), new AtomicBoolean(false)));

        assertTrue(deepEquals(new AtomicInteger(42), new AtomicInteger(42)));
        assertFalse(deepEquals(new AtomicInteger(42), new AtomicInteger(43)));

        assertTrue(deepEquals(new AtomicLong(123456789L), new AtomicLong(123456789L)));
        assertFalse(deepEquals(new AtomicLong(1L), new AtomicLong(2L)));
    }

    @Test
    public void testDeepEquals_SimpleCollectionsOrdered() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        List<Integer> l3 = Arrays.asList(3, 2, 1);

        assertTrue(deepEquals(l1, l2));
        assertFalse(deepEquals(l1, l3)); // order matters for List
    }

    @Test
    public void testDeepEquals_UnorderedCollections() {
        Set<String> s1 = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("c", "b", "a"));
        Set<String> s3 = new HashSet<>(Arrays.asList("a", "b", "d"));

        assertTrue(deepEquals(s1, s2));
        assertFalse(deepEquals(s1, s3));
    }

    @Test
    public void testDeepEquals_Maps() {
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new HashMap<>();
        m2.put("two", 2);
        m2.put("one", 1);

        Map<String, Integer> m3 = new HashMap<>();
        m3.put("one", 1);
        m3.put("two", 22); // different value

        assertTrue(deepEquals(m1, m2));
        assertFalse(deepEquals(m1, m3));
    }

    @Test
    public void testDeepEquals_Arrays_PrimitiveAndObject() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {3, 2, 1};

        assertTrue(deepEquals(a1, a2));
        assertFalse(deepEquals(a1, a3));

        String[] s1 = {"x", "y"};
        String[] s2 = {"x", "y"};
        String[] s3 = {"y", "x"};

        assertTrue(deepEquals(s1, s2));
        assertFalse(deepEquals(s1, s3));
    }

    @Test
    public void testDeepEquals_MultiDimensionalArrays() {
        int[][] m1 = {{1, 2}, {3, 4}};
        int[][] m2 = {{1, 2}, {3, 4}};
        int[][] m3 = {{1, 2}, {4, 3}}; // swapped inner element

        assertTrue(deepEquals(m1, m2));
        assertFalse(deepEquals(m1, m3));
    }

    @Test
    public void testDeepEquals_CircularReference() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = b;
        b.next = a; // creates a cycle

        Node aCopy = new Node("A");
        Node bCopy = new Node("B");
        aCopy.next = bCopy;
        bCopy.next = aCopy;

        assertTrue(deepEquals(a, aCopy));
    }

    @Test
    public void testDeepEquals_DiffMessageOnFailure() {
        Map<String, Object> opts = new HashMap<>();
        boolean result = deepEquals(5, 6, opts);
        assertFalse(result);
        assertNotNull(opts.get(DIFF));
        String diff = (String) opts.get(DIFF);
        assertTrue(diff.contains("[value mismatch]"));
        assertTrue(diff.contains("Expected: 5"));
        assertTrue(diff.contains("Found: 6"));
    }

    /*--------------------------------------------------------------------
     * Tests for deepEquals(Object, Object, Map) – option handling
     *--------------------------------------------------------------------*/

    @Test
    public void testDeepEquals_AllowStringNumberMatchingEnabled() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.TRUE);
        assertTrue(deepEquals("123", Integer.valueOf(123), opts));
        assertTrue(deepEquals(BigDecimal.valueOf(99.99), "99.99", opts));
    }

    @Test
    public void testDeepEquals_AllowStringNumberMatchingDisabled() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.FALSE);
        assertFalse(deepEquals("123", Integer.valueOf(123), opts));
    }

    @Test
    public void testDeepEquals_IgnoreCustomEquals_AllCustomIgnored() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(IGNORE_CUSTOM_EQUALS, Collections.emptySet()); // ignore *all* custom equals

        EqualsOnly eo1 = new EqualsOnly(10);
        EqualsOnly eo2 = new EqualsOnly(20); // different value, but custom equals ignored

        // Since custom equals are ignored, field‑by‑field comparison should see the same field values
        assertTrue(deepEquals(eo1, eo2, opts));
    }

    @Test
    public void testDeepEquals_IgnoreCustomEquals_SpecificClass() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(IGNORE_CUSTOM_EQUALS, Set.of(FullCustom.class));

        FullCustom c1 = new FullCustom("key", 1.23);
        FullCustom c2 = new FullCustom("key", 9.99); // same key, different amount

        // FullCustom's equals is ignored, so DeepEquals will compare fields individually
        assertFalse(deepEquals(c1, c2, opts)); // amount differs => mismatch
        Map<String, Object> innerOpts = new HashMap<>();
        innerOpts.put(IGNORE_CUSTOM_EQUALS, Set.of(FullCustom.class));
        assertTrue(deepEquals(c1, c2, innerOpts) == false);
    }

    @Test
    public void testDeepEquals_IgnoreCustomEquals_NullOptionMeansUseAllCustomEquals() {
        // No entry for IGNORE_CUSTOM_EQUALS => use all custom equals
        Map<String, Object> opts = new HashMap<>();
        FullCustom c1 = new FullCustom("x", 1.0);
        FullCustom c2 = new FullCustom("x", 1.0);
        assertTrue(deepEquals(c1, c2, opts));

        FullCustom c3 = new FullCustom("x", 2.0);
        assertFalse(deepEquals(c1, c3, opts));
    }

    /*--------------------------------------------------------------------
     * Tests for hasCustomEquals(Class) and hasCustomHashCode(Class)
     *--------------------------------------------------------------------*/

    @Test
    public void testHasCustomEquals() {
        assertFalse(hasCustomEquals(String.class)); // String inherits Object.equals (but overrides? actually overrides)
        // String does override equals, but the utility checks declaring class != Object.
        // Therefore it should return true.
        assertTrue(hasCustomEquals(String.class));

        assertFalse(hasCustomEquals(Plain.class));
        assertTrue(hasCustomEquals(EqualsOnly.class));
        assertTrue(hasCustomEquals(FullCustom.class));
    }

    @Test
    public void testHasCustomHashCode() {
        // String overrides hashCode
        assertTrue(hasCustomHashCode(String.class));

        // Plain does not override hashCode
        assertFalse(hasCustomHashCode(Plain.class));

        // FullCustom overrides both equals and hashCode
        assertTrue(hasCustomHashCode(FullCustom.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHasCustomHashCode_NullArgument() {
        hasCustomHashCode(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHasCustomEquals_NullArgument() {
        hasCustomEquals(null);
    }

    /*--------------------------------------------------------------------
     * Tests for deepHashCode(Object)
     *--------------------------------------------------------------------*/

    @Test
    public void testDeepHashCode_SimpleValues() {
        int h1 = deepHashCode(Integer.valueOf(42));
        int h2 = deepHashCode(Integer.valueOf(42));
        assertEquals(h1, h2);
    }

    @Test
    public void testDeepHashCode_ConsistentWithDeepEquals() {
        List<String> l1 = Arrays.asList("a", "b", "c");
        List<String> l2 = new ArrayList<>(l1);
        assertTrue(deepEquals(l1, l2));
        assertEquals(deepHashCode(l1), deepHashCode(l2));
    }

    @Test
    public void testDeepHashCode_UnorderedCollection_OrderIrrelevant() {
        Set<Integer> s1 = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 2, 1));

        // deepEquals should consider them equal
        assertTrue(deepEquals(s1, s2));

        // deepHashCode must also be equal despite order differences
        assertEquals(deepHashCode(s1), deepHashCode(s2));
    }

    @Test
    public void testDeepHashCode_Maps() {
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new HashMap<>();
        m2.put("two", 2);
        m2.put("one", 1);

        assertTrue(deepEquals(m1, m2));
        assertEquals(deepHashCode(m1), deepHashCode(m2));
    }

    @Test
    public void testDeepHashCode_ArraysAndMultiDimensionalArrays() {
        int[] a1 = {5, 6, 7};
        int[] a2 = {5, 6, 7};
        assertEquals(deepHashCode(a1), deepHashCode(a2));

        int[][] m1 = {{1, 2}, {3, 4}};
        int[][] m2 = {{1, 2}, {3, 4}};
        assertEquals(deepHashCode(m1), deepHashCode(m2));
    }

    @Test
    public void testDeepHashCode_CustomObjectWithCustomHashCode() {
        FullCustom fc1 = new FullCustom("abc", 12.34);
        FullCustom fc2 = new FullCustom("abc", 12.34);
        assertEquals(fc1.hashCode(), fc2.hashCode()); // sanity check
        assertEquals(deepHashCode(fc1), deepHashCode(fc2));
    }

    @Test
    public void testDeepHashCode_CircularReferenceDoesNotLoop() {
        Node n1 = new Node("root");
        Node n2 = new Node("child");
        n1.next = n2;
        n2.next = n1; // cycle

        // Should complete without StackOverflowError
        int hash = deepHashCode(n1);
        // Not much to assert about the exact value, just that we got a result
        assertNotEquals(0, hash);
    }

    /*--------------------------------------------------------------------
     * Miscellaneous edge‑case tests
     *--------------------------------------------------------------------*/

    @Test
    public void testDeepEquals_EmptyCollectionsAndMaps() {
        assertTrue(deepEquals(Collections.emptyList(), Collections.emptyList()));
        assertTrue(deepEquals(new HashSet<>(), new LinkedHashSet<>()));
        assertTrue(deepEquals(Collections.emptyMap(), new HashMap<>()));
    }

    @Test
    public void testDeepEquals_DifferentContainerTypes() {
        List<Integer> list = Arrays.asList(1, 2);
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2));
        assertFalse(deepEquals(list, set));
    }

    @Test
    public void testDeepEquals_DifferentArrayComponentTypes() {
        int[] ints = {1, 2, 3};
        long[] longs = {1L, 2L, 3L};
        assertFalse(deepEquals(ints, longs));
    }

    @Test
    public void testDeepEquals_DifferentArrayDimensions() {
        int[] oneDim = {1, 2, 3};
        int[][] twoDim = {{1, 2, 3}};
        assertFalse(deepEquals(oneDim, twoDim));
    }

    @Test
    public void testDeepEquals_ComplexObjectGraph() {
        Plain p1 = new Plain(1, "alpha", Arrays.asList("x", "y"));
        Plain p2 = new Plain(1, "alpha", Arrays.asList("x", "y"));
        assertTrue(deepEquals(p1, p2));

        // Change a deep field
        Plain p3 = new Plain(1, "alpha", Arrays.asList("x", "z"));
        assertFalse(deepEquals(p1, p3));
    }

    @Test
    public void testDeepEquals_WithURIAndURL() throws Exception {
        URI uri1 = new URI("http://example.com");
        URI uri2 = new URI("http://example.com");
        URL url1 = new URL("http://example.com");
        URL url2 = new URL("http://example.com");

        assertTrue(deepEquals(uri1, uri2));
        assertTrue(deepEquals(url1, url2));
    }

    @Test
    public void testDeepEquals_WithBigDecimalPrecision() {
        BigDecimal bd1 = new BigDecimal("1.0000000001");
        BigDecimal bd2 = new BigDecimal("1.0000000002");
        // Difference is within epsilon for double comparison (1e-15)
        assertTrue(deepEquals(bd1, bd2));

        BigDecimal bd3 = new BigDecimal("1.0000001");
        BigDecimal bd4 = new BigDecimal("1.0000002");
        // Difference is larger than epsilon => mismatch
        assertFalse(deepEquals(bd3, bd4));
    }

    @Test
    public void testDeepEquals_DiffContainsCorrectPathForNestedFailure() {
        Plain p1 = new Plain(1, "alpha", Arrays.asList("a", "b"));
        Plain p2 = new Plain(1, "alpha", Arrays.asList("a", "c")); // second element differs

        Map<String, Object> opts = new HashMap<>();
        assertFalse(deepEquals(p1, p2, opts));

        String diff = (String) opts.get(DIFF);
        assertNotNull(diff);
        // Expect path to include the list field and the index where the mismatch occurs
        assertTrue(diff.contains(".tags"));
        assertTrue(diff.contains("[1]")); // second element index
        assertTrue(diff.contains("Expected: \"b\""));
        assertTrue(diff.contains("Found: \"c\""));
    }
}
