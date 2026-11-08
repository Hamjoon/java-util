package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Comprehensive JUnit 4 tests for {@link DeepEquals}.
 */
public class DeepEqualsTest {

    /*** Helper classes ***/

    // Class with a correct custom equals implementation (but no hashCode)
    private static class WithCustomEquals {
        int x;

        WithCustomEquals(int x) {
            this.x = x;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof WithCustomEquals)) return false;
            return ((WithCustomEquals) o).x == this.x;
        }
    }

    // Class whose custom equals always returns false (to test ignore option)
    private static class BadEquals {
        int x;

        BadEquals(int x) {
            this.x = x;
        }

        @Override
        public boolean equals(Object o) {
            return false; // deliberately broken
        }
    }

    // Class with a custom hashCode implementation (but default equals)
    private static class WithCustomHash {
        int x;

        WithCustomHash(int x) {
            this.x = x;
        }

        @Override
        public int hashCode() {
            return x * 31;
        }
    }

    // Simple cyclic node class
    private static class Node {
        int value;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    /*** Tests for primitive and number handling ***/

    @Test
    public void testNumberEqualityAcrossTypes() {
        // int vs long, same value
        Assert.assertTrue(DeepEquals.deepEquals(42, 42L));

        // double vs int, same numeric value
        Assert.assertTrue(DeepEquals.deepEquals(100.0, 100));

        // float vs double within epsilon
        Assert.assertTrue(DeepEquals.deepEquals(0.1f, 0.100000001f));
        // outside epsilon should be false
        Assert.assertFalse(DeepEquals.deepEquals(0.1f, 0.2f));
    }

    @Test
    public void testStringNumberMatchingOption() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        // String "123" matches Integer 123 when option enabled
        Assert.assertTrue(DeepEquals.deepEquals("123", 123, options));

        // String "123.45" matches Double 123.45
        Assert.assertTrue(DeepEquals.deepEquals("123.45", 123.45, options));

        // Without the option the comparison must be false
        Assert.assertFalse(DeepEquals.deepEquals("123", 123));
    }

    @Test
    public void testAtomicBooleanComparison() {
        AtomicBoolean a1 = new AtomicBoolean(true);
        AtomicBoolean a2 = new AtomicBoolean(true);
        AtomicBoolean a3 = new AtomicBoolean(false);

        Assert.assertTrue(DeepEquals.deepEquals(a1, a2));
        Assert.assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    /*** Tests for custom equals handling ***/

    @Test
    public void testCustomEqualsIsUsedByDefault() {
        WithCustomEquals o1 = new WithCustomEquals(5);
        WithCustomEquals o2 = new WithCustomEquals(5);
        WithCustomEquals o3 = new WithCustomEquals(6);

        Assert.assertTrue(DeepEquals.deepEquals(o1, o2));
        Assert.assertFalse(DeepEquals.deepEquals(o1, o3));
    }

    @Test
    public void testIgnoreCustomEqualsOption() {
        BadEquals b1 = new BadEquals(10);
        BadEquals b2 = new BadEquals(10);

        // By default the broken equals causes deepEquals to be false
        Assert.assertFalse(DeepEquals.deepEquals(b1, b2));

        // When we tell DeepEquals to ignore custom equals, field comparison succeeds
        Set<Class<?>> ignoreSet = new HashSet<>();
        ignoreSet.add(BadEquals.class);
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignoreSet);

        Assert.assertTrue(DeepEquals.deepEquals(b1, b2, options));
    }

    @Test
    public void testHasCustomEqualsAndHashCodeDetection() {
        // Custom equals detection
        Assert.assertTrue(DeepEquals.hasCustomEquals(WithCustomEquals.class));
        Assert.assertFalse(DeepEquals.hasCustomEquals(String.class));

        // Custom hashCode detection
        Assert.assertTrue(DeepEquals.hasCustomHashCode(WithCustomHash.class));
        Assert.assertFalse(DeepEquals.hasCustomHashCode(String.class));
    }

    /*** Collection and Map tests ***/

    @Test
    public void testOrderedCollectionEquality() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        List<String> list3 = Arrays.asList("c", "b", "a");

        Assert.assertTrue(DeepEquals.deepEquals(list1, list2));
        Assert.assertFalse(DeepEquals.deepEquals(list1, list3));
    }

    @Test
    public void testUnorderedSetEquality() {
        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 2, 1));
        Set<Integer> set3 = new HashSet<>(Arrays.asList(1, 2));

        Assert.assertTrue(DeepEquals.deepEquals(set1, set2));
        Assert.assertFalse(DeepEquals.deepEquals(set1, set3));
    }

    @Test
    public void testMapEqualityRegardlessOfEntryOrder() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("x", 1);
        map1.put("y", 2);

        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("y", 2);
        map2.put("x", 1);

        Map<String, Integer> map3 = new HashMap<>();
        map3.put("x", 1);
        map3.put("y", 3); // different value

        Assert.assertTrue(DeepEquals.deepEquals(map1, map2));
        Assert.assertFalse(DeepEquals.deepEquals(map1, map3));
    }

    /*** Array handling tests ***/

    @Test
    public void testPrimitiveArrayEquality() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {1, 2, 4};

        Assert.assertTrue(DeepEquals.deepEquals(a1, a2));
        Assert.assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    @Test
    public void testObjectArrayEquality() {
        String[] s1 = {"foo", "bar"};
        String[] s2 = {"foo", "bar"};
        String[] s3 = {"bar", "foo"};

        Assert.assertTrue(DeepEquals.deepEquals(s1, s2));
        Assert.assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    /*** Container type detection ***/

    @Test
    public void testIsContainerType() {
        List<Integer> list = Arrays.asList(1, 2);
        Map<String, String> map = new HashMap<>();
        Set<String> set = new HashSet<>();
        String str = "not a container";

        Assert.assertTrue(DeepEquals.isContainerType(list));
        Assert.assertTrue(DeepEquals.isContainerType(map));
        Assert.assertTrue(DeepEquals.isContainerType(set));
        Assert.assertFalse(DeepEquals.isContainerType(str));
    }

    /*** Cyclic graph handling ***/

    @Test
    public void testCyclicGraphEquality() {
        Node a1 = new Node(1);
        Node a2 = new Node(2);
        a1.next = a2;
        a2.next = a1; // cycle

        Node b1 = new Node(1);
        Node b2 = new Node(2);
        b1.next = b2;
        b2.next = b1; // same shape, different instances

        Assert.assertTrue(DeepEquals.deepEquals(a1, b1));
    }

    /*** deepHashCode consistency tests ***/

    @Test
    public void testDeepHashCodeConsistencyForEqualObjects() {
        // Two distinct but equal lists
        List<String> list1 = Arrays.asList("alpha", "beta");
        List<String> list2 = Arrays.asList("alpha", "beta");

        int hash1 = DeepEquals.deepHashCode(list1);
        int hash2 = DeepEquals.deepHashCode(list2);
        Assert.assertEquals(hash1, hash2);
    }

    @Test
    public void testDeepHashCodeRespectsCustomHashCode() {
        WithCustomHash obj1 = new WithCustomHash(7);
        WithCustomHash obj2 = new WithCustomHash(7);
        WithCustomHash obj3 = new WithCustomHash(8);

        int h1 = DeepEquals.deepHashCode(obj1);
        int h2 = DeepEquals.deepHashCode(obj2);
        int h3 = DeepEquals.deepHashCode(obj3);

        Assert.assertEquals(h1, h2);
        Assert.assertNotEquals(h1, h3);
    }

    /*** Edge cases: null handling ***/

    @Test
    public void testNullComparisons() {
        Assert.assertTrue(DeepEquals.deepEquals(null, null));
        Assert.assertFalse(DeepEquals.deepEquals(null, "not null"));
        Assert.assertFalse(DeepEquals.deepEquals("not null", null));
    }

    /*** Edge cases: different classes ***/

    @Test
    public void testDifferentClassTypesAreNotEqual() {
        Assert.assertFalse(DeepEquals.deepEquals("string", 123));
        Assert.assertFalse(DeepEquals.deepEquals(new ArrayList<>(), new HashSet<>()));
    }
}
