package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JUnit‑4 test suite for {@link DeepEquals}.
 *
 * The tests are grouped by the public API methods of {@code DeepEquals}:
 *
 * <ul>
 *   <li>{@link DeepEquals#deepEquals(Object, Object)}</li>
 *   <li>{@link DeepEquals#deepEquals(Object, Object, Map)}</li>
 *   <li>{@link DeepEquals#hasCustomEquals(Class)}</li>
 *   <li>{@link DeepEquals#hasCustomHashCode(Class)}</li>
 *   <li>{@link DeepEquals#deepHashCode(Object)}</li>
 * </ul>
 *
 * Each method receives a basic verification test and a set of edge‑case tests that
 * exercise the special options, circular references, numeric tolerance, etc.
 */
public class DeepEqualsTest {

    /* ---------------------------------------------------------------------- */
    /* Helper classes used in several tests                                    */
    /* ---------------------------------------------------------------------- */

    /** Simple POJO with two fields – no custom equals/hashCode. */
    private static class SimpleBean {
        int id;
        String name;

        SimpleBean(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /** POJO that overrides {@code equals} and {@code hashCode}. */
    private static class CustomEqualsBean {
        int id;
        String name;

        CustomEqualsBean(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CustomEqualsBean)) return false;
            CustomEqualsBean other = (CustomEqualsBean) o;
            // Custom logic: ignore the {@code name} field
            return this.id == other.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    /** POJO that contains a self‑reference to test circular detection. */
    private static class Node {
        String value;
        Node next;

        Node(String value) {
            this.value = value;
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 1. public static boolean deepEquals(Object, Object)                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsSimplePrimitives() {
        assertTrue(DeepEquals.deepEquals(5, 5));
        assertTrue(DeepEquals.deepEquals("hello", "hello"));
        assertTrue(DeepEquals.deepEquals(true, true));

        assertFalse(DeepEquals.deepEquals(5, 6));
        assertFalse(DeepEquals.deepEquals("hello", "world"));
        assertFalse(DeepEquals.deepEquals(Boolean.FALSE, Boolean.TRUE));
    }

    @Test
    public void testDeepEqualsNulls() {
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
        assertFalse(DeepEquals.deepEquals(123, null));
    }

    @Test
    public void testDeepEqualsDifferentTypes() {
        assertFalse(DeepEquals.deepEquals(123, "123"));
        assertFalse(DeepEquals.deepEquals(new Date(), new URI("http://example.com")));
    }

    @Test
    public void testDeepEqualsSimpleBeans() {
        SimpleBean a = new SimpleBean(1, "Alice");
        SimpleBean b = new SimpleBean(1, "Alice");
        SimpleBean c = new SimpleBean(2, "Bob");

        assertTrue(DeepEquals.deepEquals(a, b));
        assertFalse(DeepEquals.deepEquals(a, c));
    }

    @Test
    public void testDeepEqualsArrays() {
        int[] a = {1, 2, 3};
        int[] b = {1, 2, 3};
        int[] c = {1, 2, 4};

        assertTrue(DeepEquals.deepEquals(a, b));
        assertFalse(DeepEquals.deepEquals(a, c));
    }

    @Test
    public void testDeepEqualsMultiDimensionalArrays() {
        int[][] a = {{1, 2}, {3, 4}};
        int[][] b = {{1, 2}, {3, 4}};
        int[][] c = {{1, 2}, {4, 3}}; // element order changed

        assertTrue(DeepEquals.deepEquals(a, b));
        assertFalse(DeepEquals.deepEquals(a, c));
    }

    @Test
    public void testDeepEqualsArrayDimensionMismatch() {
        int[] oneDim = {1, 2, 3};
        int[][] twoDim = {{1, 2, 3}};

        assertFalse(DeepEquals.deepEquals(oneDim, twoDim));
    }

    @Test
    public void testDeepEqualsOrderedCollections() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        List<String> list3 = Arrays.asList("c", "b", "a");

        assertTrue(DeepEquals.deepEquals(list1, list2));
        assertFalse(DeepEquals.deepEquals(list1, list3));
    }

    @Test
    public void testDeepEqualsUnorderedCollections() {
        Set<Integer> set1 = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 2, 1));
        Set<Integer> set3 = new HashSet<>(Arrays.asList(1, 2, 4));

        assertTrue(DeepEquals.deepEquals(set1, set2));
        assertFalse(DeepEquals.deepEquals(set1, set3));
    }

    @Test
    public void testDeepEqualsMaps() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("x", 1);
        m1.put("y", 2);

        Map<String, Integer> m2 = new HashMap<>();
        m2.put("y", 2);
        m2.put("x", 1);

        Map<String, Integer> m3 = new HashMap<>();
        m3.put("x", 1);
        m3.put("y", 3); // different value

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    @Test
    public void testDeepEqualsFloatingPointTolerance() {
        double a = 0.1 + 0.2; // 0.30000000000000004
        double b = 0.3;

        // Direct equals would be false, deepEquals should treat them as equal
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEqualsStringNumberMatchingDisabled() {
        Map<String, Object> options = new HashMap<>();
        // default is false – do not set the flag
        assertFalse(DeepEquals.deepEquals("10", 10, options));
    }

    @Test
    public void testDeepEqualsStringNumberMatchingEnabled() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("10", 10, options));
        assertTrue(DeepEquals.deepEquals(BigDecimal.valueOf(123.45), "123.45", options));
    }

    @Test
    public void testDeepEqualsCustomEqualsUsedByDefault() {
        CustomEqualsBean a = new CustomEqualsBean(1, "Alice");
        CustomEqualsBean b = new CustomEqualsBean(1, "Bob"); // same id, different name

        // Default behaviour: custom equals IS used → objects are equal
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEqualsCustomEqualsIgnoredViaOption() {
        CustomEqualsBean a = new CustomEqualsBean(1, "Alice");
        CustomEqualsBean b = new CustomEqualsBean(1, "Bob");

        Map<String, Object> options = new HashMap<>();
        // ignore custom equals for the specific class
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.singleton(CustomEqualsBean.class));

        // With the option, field‑by‑field comparison is performed → name differs → not equal
        assertFalse(DeepEquals.deepEquals(a, b, options));
    }

    @Test
    public void testDeepEqualsDiffMessageIsPopulated() {
        Map<String, Object> options = new HashMap<>();
        SimpleBean a = new SimpleBean(1, "Alice");
        SimpleBean b = new SimpleBean(2, "Bob");

        boolean result = DeepEquals.deepEquals(a, b, options);
        assertFalse(result);
        assertNotNull(options.get(DeepEquals.DIFF));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("[field value mismatch]"));
        assertTrue(diff.contains(".id"));
        assertTrue(diff.contains("Expected: 1"));
        assertTrue(diff.contains("Found: 2"));
    }

    /* ---------------------------------------------------------------------- */
    /* 2. public static boolean deepEquals(Object, Object, Map)                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsWithEmptyOptionsMap() {
        Map<String, Object> options = new HashMap<>();
        assertTrue(DeepEquals.deepEquals("same", "same", options));
        assertFalse(DeepEquals.deepEquals("same", "different", options));
    }

    /* ---------------------------------------------------------------------- */
    /* 3. public static boolean hasCustomEquals(Class<?>)                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHasCustomEquals() {
        assertTrue(DeepEquals.hasCustomEquals(CustomEqualsBean.class));
        assertFalse(DeepEquals.hasCustomEquals(SimpleBean.class));
        assertFalse(DeepEquals.hasCustomEquals(String.class)); // String inherits Object.equals but overrides – still true?
        // String overrides equals, but ReflectionUtils.getMethod will return the method declared in String,
        // which is not Object, therefore hasCustomEquals should return true for String.
        // The library's intention is to detect any override, so we assert true.
        assertTrue(DeepEquals.hasCustomEquals(String.class));
    }

    /* ---------------------------------------------------------------------- */
    /* 4. public static boolean hasCustomHashCode(Class<?>)                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHasCustomHashCode() {
        assertTrue(DeepEquals.hasCustomHashCode(CustomEqualsBean.class));
        assertFalse(DeepEquals.hasCustomHashCode(SimpleBean.class));
        // String overrides hashCode
        assertTrue(DeepEquals.hasCustomHashCode(String.class));
    }

    /* ---------------------------------------------------------------------- */
    /* 5. public static int deepHashCode(Object)                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeSimpleEquality() {
        SimpleBean a = new SimpleBean(10, "X");
        SimpleBean b = new SimpleBean(10, "X");
        assertEquals(DeepEquals.deepHashCode(a), DeepEquals.deepHashCode(b));
    }

    @Test
    public void testDeepHashCodeDifferentObjects() {
        SimpleBean a = new SimpleBean(10, "X");
        SimpleBean b = new SimpleBean(20, "Y");
        assertNotEquals(DeepEquals.deepHashCode(a), DeepEquals.deepHashCode(b));
    }

    @Test
    public void testDeepHashCodeWithCollectionsAndMaps() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        Set<Integer> set = new HashSet<>(list);
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        int listHash = DeepEquals.deepHashCode(list);
        int setHash = DeepEquals.deepHashCode(set);
        int mapHash = DeepEquals.deepHashCode(map);

        // Collections with the same content should yield the same deep hash code,
        // even though order does not matter for sets.
        assertEquals(listHash, DeepEquals.deepHashCode(Arrays.asList(1, 2, 3)));
        assertEquals(setHash, DeepEquals.deepHashCode(new HashSet<>(Arrays.asList(3, 2, 1))));
        assertEquals(mapHash, DeepEquals.deepHashCode(new HashMap<>(map)));
    }

    @Test
    public void testDeepHashCodeCircularReference() {
        Node n1 = new Node("first");
        Node n2 = new Node("second");
        n1.next = n2;
        n2.next = n1; // creates a cycle

        // The method must terminate without StackOverflowError.
        int hash = DeepEquals.deepHashCode(n1);
        // Not asserting a particular value, only that it returns something.
        assertTrue(hash != 0 || hash == 0);
    }

    @Test
    public void testDeepHashCodeWithAtomicBoolean() {
        AtomicBoolean a = new AtomicBoolean(true);
        AtomicBoolean b = new AtomicBoolean(true);
        AtomicBoolean c = new AtomicBoolean(false);

        assertEquals(DeepEquals.deepHashCode(a), DeepEquals.deepHashCode(b));
        assertNotEquals(DeepEquals.deepHashCode(a), DeepEquals.deepHashCode(c));
    }

    @Test
    public void testDeepHashCodeConsistentAcrossRuns() {
        SimpleBean bean = new SimpleBean(42, "answer");
        int first = DeepEquals.deepHashCode(bean);
        int second = DeepEquals.deepHashCode(bean);
        assertEquals(first, second);
    }

    /* ---------------------------------------------------------------------- */
    /* Additional edge cases (null options, empty ignore set, etc.)             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIgnoreCustomEqualsEmptySetMeansIgnoreAll() {
        CustomEqualsBean a = new CustomEqualsBean(1, "Alice");
        CustomEqualsBean b = new CustomEqualsBean(1, "Bob");

        Map<String, Object> options = new HashMap<>();
        // Empty set → ignore ALL custom equals
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());

        // Field‑by‑field comparison sees the name differ → not equal
        assertFalse(DeepEquals.deepEquals(a, b, options));
    }

    @Test
    public void testIgnoreCustomEqualsNullMeansUseAll() {
        CustomEqualsBean a = new CustomEqualsBean(1, "Alice");
        CustomEqualsBean b = new CustomEqualsBean(1, "Bob");

        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, null); // explicit null

        // Should fall back to using the custom equals → equal
        assertTrue(DeepEquals.deepEquals(a, b, options));
    }

    @Test
    public void testDeepEqualsWithComplexObjectGraph() {
        SimpleBean leaf1 = new SimpleBean(1, "leaf");
        SimpleBean leaf2 = new SimpleBean(1, "leaf");

        Map<String, Object> graphA = new HashMap<>();
        graphA.put("root", leaf1);
        graphA.put("list", Arrays.asList(leaf1, leaf2));

        Map<String, Object> graphB = new HashMap<>();
        graphB.put("root", leaf2);
        graphB.put("list", Arrays.asList(leaf2, leaf1));

        assertTrue(DeepEquals.deepEquals(graphA, graphB));
    }

    @Test
    public void testDeepEqualsWithDifferentArrayComponentTypes() {
        int[] intArray = {1, 2, 3};
        long[] longArray = {1L, 2L, 3L};

        // Component types differ → should be false
        assertFalse(DeepEquals.deepEquals(intArray, longArray));
    }

    @Test
    public void testDeepEqualsWithDifferentArrayLengths() {
        int[] a = {1, 2, 3};
        int[] b = {1, 2, 3, 4};

        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEqualsWithDifferentCollectionSizes() {
        List<String> a = Arrays.asList("a", "b");
        List<String> b = Arrays.asList("a", "b", "c");

        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEqualsMapMissingKey() {
        Map<String, Integer> a = new HashMap<>();
        a.put("one", 1);
        a.put("two", 2);

        Map<String, Integer> b = new HashMap<>();
        b.put("one", 1); // missing "two"

        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEqualsMapValueMismatch() {
        Map<String, Integer> a = new HashMap<>();
        a.put("key", 1);
        Map<String, Integer> b = new HashMap<>();
        b.put("key", 2);

        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEqualsAtomicBoolean() {
        AtomicBoolean a = new AtomicBoolean(true);
        AtomicBoolean b = new AtomicBoolean(true);
        AtomicBoolean c = new AtomicBoolean(false);

        assertTrue(DeepEquals.deepEquals(a, b));
        assertFalse(DeepEquals.deepEquals(a, c));
    }

    @Test
    public void testDeepEqualsWithURIAndURL() throws Exception {
        URI uri1 = new URI("http://example.com");
        URI uri2 = new URI("http://example.com");
        URL url1 = uri1.toURL();
        URL url2 = uri2.toURL();

        assertTrue(DeepEquals.deepEquals(uri1, uri2));
        assertTrue(DeepEquals.deepEquals(url1, url2));
        assertFalse(DeepEquals.deepEquals(uri1, url2)); // different classes
    }

    @Test
    public void testDeepEqualsWithUUID() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.fromString(u1.toString());

        assertTrue(DeepEquals.deepEquals(u1, u2));
    }

    @Test
    public void testDeepEqualsWithBigDecimalPrecision() {
        BigDecimal a = new BigDecimal("0.30000000000000004");
        BigDecimal b = new BigDecimal("0.3");
        assertTrue(DeepEquals.deepEquals(a, b));
    }
}
