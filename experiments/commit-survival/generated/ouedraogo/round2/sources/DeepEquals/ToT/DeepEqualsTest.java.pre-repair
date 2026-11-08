package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Comprehensive JUnit‑4 test suite for {@link DeepEquals}.
 *
 * The tests are grouped by the method they exercise and cover:
 * <ul>
 *   <li>basic primitive / wrapper equality</li>
 *   <li>numeric comparisons across different types and with epsilon handling</li>
 *   <li>String‑to‑Number matching via options</li>
 *   <li>Collections (ordered List, unordered Set) and Maps</li>
 *   <li>Array handling (primitive and object arrays)</li>
 *   <li>AtomicBoolean handling</li>
 *   <li>Custom {@code equals} / {@code hashCode} behaviour and the IGNORE_CUSTOM_EQUALS option</li>
 *   <li>Cycle detection</li>
 *   <li>deepHashCode consistency with deepEquals</li>
 *   <li>ItemsToCompare contract (equals, hashCode, toString)</li>
 *   <li>hasCustomEquals / hasCustomHashCode detection</li>
 * </ul>
 */
public class DeepEqualsTest {

    /* -------------------------------------------------------------
     *  Helper classes used in several tests
     * ------------------------------------------------------------- */

    /** Simple POJO with no custom equals / hashCode. */
    static class Plain {
        int i;
        String s;
        Plain(int i, String s) { this.i = i; this.s = s; }
    }

    /** POJO that overrides equals / hashCode based on a single field. */
    static class WithCustomEquals {
        final int id;
        WithCustomEquals(int id) { this.id = id; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WithCustomEquals)) return false;
            return id == ((WithCustomEquals) o).id;
        }
        @Override public int hashCode() { return id; }
    }

    /** POJO that overrides only hashCode (no custom equals). */
    static class WithCustomHash {
        final String value;
        WithCustomHash(String value) { this.value = value; }
        @Override public int hashCode() { return value.hashCode(); }
    }

    /** POJO that contains a self‑reference to create a cycle. */
    static class Cyclic {
        Cyclic next;
    }

    /* -------------------------------------------------------------
     *  Tests for ItemsToCompare (equals, hashCode, toString)
     * ------------------------------------------------------------- */

    @Test
    public void testItemsToCompareEqualsAndHashCode() {
        DeepEquals.ItemsToCompare a1 = new DeepEquals.ItemsToCompare("x", 1);
        DeepEquals.ItemsToCompare a2 = new DeepEquals.ItemsToCompare("x", 1);
        DeepEquals.ItemsToCompare b  = new DeepEquals.ItemsToCompare("x", 2);

        // Equality is based on identity (==) of the wrapped objects
        Assert.assertTrue(a1.equals(a2));
        Assert.assertEquals(a1.hashCode(), a2.hashCode());

        // Different second element -> not equal
        Assert.assertFalse(a1.equals(b));

        // Comparison with other type must return false
        Assert.assertFalse(a1.equals("not a ItemsToCompare"));
    }

    @Test
    public void testItemsToCompareToString() {
        DeepEquals.ItemsToCompare pair = new DeepEquals.ItemsToCompare(5, "hello");
        // Both objects are not primitive, so class names are shown
        String expected = Integer.class.getName() + " | " + String.class.getName();
        Assert.assertEquals(expected, pair.toString());
    }

    /* -------------------------------------------------------------
     *  DeepEquals – basic primitive / wrapper handling
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsPrimitivesAndWrappers() {
        Assert.assertTrue(DeepEquals.deepEquals(5, 5));
        Assert.assertTrue(DeepEquals.deepEquals(Integer.valueOf(5), Integer.valueOf(5)));
        Assert.assertTrue(DeepEquals.deepEquals("abc", "abc"));
        Assert.assertFalse(DeepEquals.deepEquals("abc", "def"));
        Assert.assertFalse(DeepEquals.deepEquals(5, null));
    }

    /* -------------------------------------------------------------
     *  DeepEquals – Number comparisons (including epsilon)
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsNumberEqualityAcrossTypes() {
        // int vs long
        Assert.assertTrue(DeepEquals.deepEquals(42, 42L));

        // float vs double within epsilon
        Assert.assertTrue(DeepEquals.deepEquals(0.1f, 0.10000000001d));

        // float vs double outside epsilon
        Assert.assertFalse(DeepEquals.deepEquals(0.1f, 0.1001d));
    }

    @Test
    public void testDeepEqualsStringNumberMatchingOption() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        // String "123" vs Integer 123
        Assert.assertTrue(DeepEquals.deepEquals("123", 123, options));

        // String "12.34" vs Double 12.34
        Assert.assertTrue(DeepEquals.deepEquals("12.34", 12.34, options));

        // Invalid numeric string should fall back to false
        Assert.assertFalse(DeepEquals.deepEquals("abc", 123, options));

        // Option disabled – should be false
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);
        Assert.assertFalse(DeepEquals.deepEquals("123", 123, options));
    }

    /* -------------------------------------------------------------
     *  DeepEquals – AtomicBoolean handling
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsAtomicBoolean() {
        Assert.assertTrue(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(true)));
        Assert.assertFalse(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(false)));
    }

    /* -------------------------------------------------------------
     *  DeepEquals – Collections (ordered List)
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsOrderedCollections() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        List<Integer> l3 = Arrays.asList(3, 2, 1);

        Assert.assertTrue(DeepEquals.deepEquals(l1, l2));
        Assert.assertFalse(DeepEquals.deepEquals(l1, l3));
    }

    /* -------------------------------------------------------------
     *  DeepEquals – Sets (unordered)
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsUnorderedSet() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("c", "b", "a"));
        Set<String> s3 = new HashSet<>(Arrays.asList("a", "b"));

        Assert.assertTrue(DeepEquals.deepEquals(s1, s2));
        Assert.assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    @Test
    public void testDeepEqualsSetWithCollision() {
        // Force a hash collision by using objects that have the same deepHashCode
        // but are not equal.  We use WithCustomHash which overrides hashCode only.
        WithCustomHash a = new WithCustomHash("X");
        WithCustomHash b = new WithCustomHash("X"); // same hashCode, different instance

        Set<WithCustomHash> set1 = new HashSet<>(Arrays.asList(a));
        Set<WithCustomHash> set2 = new HashSet<>(Arrays.asList(b));

        // DeepEquals must compare contents deeply, not just hashCode
        Assert.assertTrue(DeepEquals.deepEquals(set1, set2));
    }

    /* -------------------------------------------------------------
     *  DeepEquals – Maps
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsMaps() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("two", 2);
        m2.put("one", 1); // insertion order differs

        Map<String, Integer> m3 = new HashMap<>();
        m3.put("one", 1);
        m3.put("two", 3); // different value

        Assert.assertTrue(DeepEquals.deepEquals(m1, m2));
        Assert.assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    @Test
    public void testDeepEqualsMapWithCustomKey() {
        WithCustomEquals key1 = new WithCustomEquals(5);
        WithCustomEquals key2 = new WithCustomEquals(5); // equal via custom equals
        Map<WithCustomEquals, String> map1 = new HashMap<>();
        map1.put(key1, "value");

        Map<WithCustomEquals, String> map2 = new HashMap<>();
        map2.put(key2, "value");

        Assert.assertTrue(DeepEquals.deepEquals(map1, map2));
    }

    /* -------------------------------------------------------------
     *  DeepEquals – Arrays (primitive and object)
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsPrimitiveArrays() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {3, 2, 1};

        Assert.assertTrue(DeepEquals.deepEquals(a1, a2));
        Assert.assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    @Test
    public void testDeepEqualsObjectArrays() {
        String[] s1 = {"x", "y"};
        String[] s2 = {"x", "y"};
        String[] s3 = {"y", "x"};

        Assert.assertTrue(DeepEquals.deepEquals(s1, s2));
        Assert.assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    /* -------------------------------------------------------------
     *  DeepEquals – Custom equals handling and IGNORE_CUSTOM_EQUALS option
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsUsesCustomEqualsByDefault() {
        WithCustomEquals a = new WithCustomEquals(10);
        WithCustomEquals b = new WithCustomEquals(10);
        Assert.assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEqualsIgnoreCustomEqualsOption() {
        WithCustomEquals a = new WithCustomEquals(10);
        WithCustomEquals b = new WithCustomEquals(10);

        // Force field‑by‑field comparison by ignoring the custom equals for this class
        Set<Class<?>> ignoreSet = new HashSet<>();
        ignoreSet.add(WithCustomEquals.class);
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignoreSet);

        // Both objects have same field value, so field comparison succeeds
        Assert.assertTrue(DeepEquals.deepEquals(a, b, options));

        // Change a private field via reflection to make them differ
        try {
            java.lang.reflect.Field f = WithCustomEquals.class.getDeclaredField("id");
            f.setAccessible(true);
            f.setInt(a, 99);
        } catch (Exception e) {
            Assert.fail("Reflection failed: " + e);
        }

        // Now deepEquals should detect the difference when custom equals are ignored
        Assert.assertFalse(DeepEquals.deepEquals(a, b, options));
    }

    /* -------------------------------------------------------------
     *  DeepEquals – Cycle detection
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsHandlesCycles() {
        Cyclic a = new Cyclic();
        Cyclic b = new Cyclic();
        a.next = a; // self‑cycle
        b.next = b; // self‑cycle
        Assert.assertTrue(DeepEquals.deepEquals(a, b));

        // Different cycle shape should be false
        Cyclic c = new Cyclic();
        c.next = new Cyclic(); // a → new object
        Assert.assertFalse(DeepEquals.deepEquals(a, c));
    }

    /* -------------------------------------------------------------
     *  isContainerType
     * ------------------------------------------------------------- */

    @Test
    public void testIsContainerType() {
        Assert.assertTrue(DeepEquals.isContainerType(new ArrayList<>()));
        Assert.assertTrue(DeepEquals.isContainerType(new HashMap<>()));
        Assert.assertFalse(DeepEquals.isContainerType("string"));
        Assert.assertFalse(DeepEquals.isContainerType(123));
    }

    /* -------------------------------------------------------------
     *  hasCustomEquals / hasCustomHashCode detection
     * ------------------------------------------------------------- */

    @Test
    public void testHasCustomEqualsDetection() {
        Assert.assertTrue(DeepEquals.hasCustomEquals(WithCustomEquals.class));
        Assert.assertFalse(DeepEquals.hasCustomEquals(Plain.class));
        // Object class itself has no custom equals
        Assert.assertFalse(DeepEquals.hasCustomEquals(Object.class));
    }

    @Test
    public void testHasCustomHashCodeDetection() {
        Assert.assertTrue(DeepEquals.hasCustomHashCode(WithCustomHash.class));
        Assert.assertFalse(DeepEquals.hasCustomHashCode(Plain.class));
        Assert.assertFalse(DeepEquals.hasCustomHashCode(Object.class));
    }

    /* -------------------------------------------------------------
     *  deepHashCode consistency checks
     * ------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeConsistentWithDeepEquals() {
        // Two equal object graphs must have the same deepHashCode
        List<Object> list1 = new ArrayList<>();
        list1.add(new Plain(1, "a"));
        list1.add(Arrays.asList(2, 3));

        List<Object> list2 = new ArrayList<>();
        list2.add(new Plain(1, "a"));
        list2.add(Arrays.asList(2, 3));

        Assert.assertTrue(DeepEquals.deepEquals(list1, list2));
        Assert.assertEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));
    }

    @Test
    public void testDeepHashCodeDifferentObjectsYieldDifferentHashes() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 4);
        Assert.assertFalse(DeepEquals.deepEquals(l1, l2));
        Assert.assertNotEquals(DeepEquals.deepHashCode(l1), DeepEquals.deepHashCode(l2));
    }

    @Test
    public void testDeepHashCodeHandlesFloatEpsilon() {
        Float f1 = 0.123456f;
        Float f2 = 0.1234567f; // differs but within the defined epsilon for float
        Assert.assertTrue(DeepEquals.deepEquals(f1, f2));
        Assert.assertEquals(DeepEquals.deepHashCode(f1), DeepEquals.deepHashCode(f2));
    }

    @Test
    public void testDeepHashCodeHandlesDoubleEpsilon() {
        Double d1 = 0.1234567890123;
        Double d2 = 0.1234567890124; // within epsilon for double
        Assert.assertTrue(DeepEquals.deepEquals(d1, d2));
        Assert.assertEquals(DeepEquals.deepHashCode(d1), DeepEquals.deepHashCode(d2));
    }

    /* -------------------------------------------------------------
     *  compareNumbers private method exercised via deepEquals
     * ------------------------------------------------------------- */

    @Test
    public void testCompareNumbersViaDeepEquals() {
        // BigDecimal comparison through Number conversion
        BigDecimal bd1 = new BigDecimal("123.4500");
        BigDecimal bd2 = new BigDecimal("123.45");
        Assert.assertTrue(DeepEquals.deepEquals(bd1, bd2));

        // Different scale but same value should be equal
        Assert.assertTrue(DeepEquals.deepEquals(new BigDecimal("1.0"), new BigDecimal("1"));
    }

    /* -------------------------------------------------------------
     *  Additional edge cases
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsWithNulls() {
        Assert.assertTrue(DeepEquals.deepEquals(null, null));
        Assert.assertFalse(DeepEquals.deepEquals(null, "not null"));
        Assert.assertFalse(DeepEquals.deepEquals("not null", null));
    }

    @Test
    public void testDeepEqualsClassObjects() {
        // Class objects are compared via equals()
        Assert.assertTrue(DeepEquals.deepEquals(String.class, String.class));
        Assert.assertFalse(DeepEquals.deepEquals(String.class, Integer.class));
    }

    @Test
    public void testDeepEqualsDateObjects() {
        Date d1 = new Date(123456789L);
        Date d2 = new Date(123456789L);
        Date d3 = new Date(987654321L);
        Assert.assertTrue(DeepEquals.deepEquals(d1, d2));
        Assert.assertFalse(DeepEquals.deepEquals(d1, d3));
    }
}
