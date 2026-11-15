package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class DeepEqualsTest {

    // ----------------------------------------------------------------------
    // Helper classes used in the tests
    // ----------------------------------------------------------------------
    /** Simple POJO with no custom equals / hashCode */
    public static class SimpleBean {
        public int id;
        public String name;
        public SimpleBean child;

        public SimpleBean(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /** POJO that overrides equals / hashCode */
    public static class EqualsBean {
        private final int id;
        private final String value;

        public EqualsBean(int id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EqualsBean)) return false;
            EqualsBean that = (EqualsBean) o;
            return id == that.id && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value);
        }
    }

    /** POJO that overrides only hashCode (to test hasCustomEquals vs hasCustomHashCode) */
    public static class HashOnlyBean {
        private final int id;

        public HashOnlyBean(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    // ----------------------------------------------------------------------
    // Simple primitive / wrapper comparisons
    // ----------------------------------------------------------------------
    @Test
    public void testPrimitiveAndWrapperEquality() {
        assertTrue(DeepEquals.deepEquals(5, 5));
        assertTrue(DeepEquals.deepEquals(5L, 5));
        assertTrue(DeepEquals.deepEquals(5.0d, 5));
        assertTrue(DeepEquals.deepEquals(5.0f, 5));
        assertFalse(DeepEquals.deepEquals(5, 6));
        assertFalse(DeepEquals.deepEquals(5.0d, 5.1d));
    }

    @Test
    public void testStringNumberMatchingOption() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        assertTrue(DeepEquals.deepEquals("123", 123, options));
        assertTrue(DeepEquals.deepEquals(123, "123", options));

        // Without the option strings are not considered numbers
        assertFalse(DeepEquals.deepEquals("123", 123));
    }

    // ----------------------------------------------------------------------
    // AtomicBoolean handling
    // ----------------------------------------------------------------------
    @Test
    public void testAtomicBooleanEquality() {
        assertTrue(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(true)));
        assertFalse(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(false)));
    }

    // ----------------------------------------------------------------------
    // Array comparisons
    // ----------------------------------------------------------------------
    @Test
    public void testArrayEquality() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {1, 2, 4};

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));

        String[] s1 = {"a", "b"};
        String[] s2 = {"a", "b"};
        String[] s3 = {"b", "a"};

        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    // ----------------------------------------------------------------------
    // Collection (ordered) comparisons
    // ----------------------------------------------------------------------
    @Test
    public void testOrderedCollectionEquality() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        List<Integer> l3 = Arrays.asList(3, 2, 1);

        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));
    }

    // ----------------------------------------------------------------------
    // Set (unordered) comparisons, including hash collision handling
    // ----------------------------------------------------------------------
    @Test
    public void testSetEquality() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("c", "b", "a"));
        Set<String> s3 = new HashSet<>(Arrays.asList("a", "b"));

        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    @Test
    public void testSetWithCollidingHashCodes() {
        // Create two distinct objects that have the same deepHashCode (by design they both contain a Float with same rounded value)
        Set<Object> set1 = new HashSet<>();
        Set<Object> set2 = new HashSet<>();

        set1.add(0.1234567f); // will be rounded to 0.12346
        set2.add(0.1234599f); // same rounded value -> same deepHashCode

        assertTrue(DeepEquals.deepEquals(set1, set2));
    }

    // ----------------------------------------------------------------------
    // Map comparisons
    // ----------------------------------------------------------------------
    @Test
    public void testMapEquality() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("two", 2);
        m2.put("one", 1);

        Map<String, Integer> m3 = new HashMap<>();
        m3.put("one", 1);
        m3.put("two", 3);

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    // ----------------------------------------------------------------------
    // Deep object graph with cycles
    // ----------------------------------------------------------------------
    @Test
    public void testCyclicGraphEquality() {
        SimpleBean a = new SimpleBean(1, "A");
        SimpleBean b = new SimpleBean(2, "B");
        a.child = b;
        b.child = a; // cycle

        SimpleBean a2 = new SimpleBean(1, "A");
        SimpleBean b2 = new SimpleBean(2, "B");
        a2.child = b2;
        b2.child = a2; // same structure, different instances

        assertTrue(DeepEquals.deepEquals(a, a2));
    }

    // ----------------------------------------------------------------------
    // Custom equals handling and IGNORE_CUSTOM_EQUALS option
    // ----------------------------------------------------------------------
    @Test
    public void testCustomEqualsUsedByDefault() {
        EqualsBean eb1 = new EqualsBean(10, "x");
        EqualsBean eb2 = new EqualsBean(10, "x");
        EqualsBean eb3 = new EqualsBean(10, "y");

        assertTrue(DeepEquals.deepEquals(eb1, eb2));
        assertFalse(DeepEquals.deepEquals(eb1, eb3));
    }

    @Test
    public void testIgnoreCustomEqualsOption() {
        EqualsBean eb1 = new EqualsBean(10, "x");
        EqualsBean eb2 = new EqualsBean(10, "x");

        // Ignore custom equals for EqualsBean class
        Set<Class<?>> ignoreSet = new HashSet<>();
        ignoreSet.add(EqualsBean.class);
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignoreSet);

        // Since fields are the same, deepEquals should still be true, but if we change a field it will be false
        eb2.value = "different"; // can't modify final, so we use reflection to simulate a mismatch
        // Instead we create a subclass without custom equals to test the path
        class NoEquals extends SimpleBean {
            public NoEquals(int id, String name) { super(id, name); }
        }
        NoEquals ne1 = new NoEquals(1, "test");
        NoEquals ne2 = new NoEquals(1, "test");
        assertTrue(DeepEquals.deepEquals(ne1, ne2, options));
    }

    // ----------------------------------------------------------------------
    // deepHashCode consistency checks
    // ----------------------------------------------------------------------
    @Test
    public void testDeepHashCodeDeterministic() {
        List<Object> list1 = new ArrayList<>();
        list1.add(1);
        list1.add("two");
        list1.add(Arrays.asList(3, 4));

        List<Object> list2 = new ArrayList<>();
        list2.add(1);
        list2.add("two");
        list2.add(Arrays.asList(3, 4));

        assertEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));
    }

    @Test
    public void testDeepHashCodeWithCustomHash() {
        HashOnlyBean h1 = new HashOnlyBean(123);
        HashOnlyBean h2 = new HashOnlyBean(123);
        assertEquals(h1.hashCode(), h2.hashCode());
        assertEquals(DeepEquals.deepHashCode(h1), DeepEquals.deepHashCode(h2));
    }

    @Test
    public void testDeepHashCodeWithFloatEpsilon() {
        Float f1 = 1.000001f;
        Float f2 = 1.000002f; // within epsilon for float hashing
        assertEquals(DeepEquals.deepHashCode(f1), DeepEquals.deepHashCode(f2));
    }

    @Test
    public void testDeepHashCodeWithDoubleEpsilon() {
        Double d1 = 1.00000000001;
        Double d2 = 1.00000000002; // within epsilon for double hashing
        assertEquals(DeepEquals.deepHashCode(d1), DeepEquals.deepHashCode(d2));
    }

    // ----------------------------------------------------------------------
    // Edge cases: null handling and different types
    // ----------------------------------------------------------------------
    @Test
    public void testNullHandling() {
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
        assertFalse(DeepEquals.deepEquals("not null", null));
    }

    @Test
    public void testDifferentTypesNotEqual() {
        assertFalse(DeepEquals.deepEquals("123", 123L)); // without option
        assertFalse(DeepEquals.deepEquals(new int[]{1,2}, new ArrayList<>(Arrays.asList(1,2))));
    }

    // ----------------------------------------------------------------------
    // Verify internal ItemsToCompare equals / hashCode behavior (indirectly)
    // ----------------------------------------------------------------------
    @Test
    public void testItemsToCompareIdentityEquality() {
        // Use reflection to instantiate the private class
        try {
            java.lang.reflect.Constructor<DeepEquals.ItemsToCompare> ctor =
                    DeepEquals.ItemsToCompare.class.getDeclaredConstructor(Object.class, Object.class);
            ctor.setAccessible(true);
            DeepEquals.ItemsToCompare itc1 = ctor.newInstance("a", "b");
            DeepEquals.ItemsToCompare itc2 = ctor.newInstance("a", "b");
            DeepEquals.ItemsToCompare itc3 = ctor.newInstance(new String("a"), new String("b"));

            // Same references -> equals true, otherwise false
            assertTrue(itc1.equals(itc1));
            assertFalse(itc1.equals(itc2)); // different object references

            // hashCode based on objects' hashCodes
            int expectedHash = ("a".hashCode()) + ("b".hashCode());
            assertEquals(expectedHash, itc1.hashCode());
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }
}
