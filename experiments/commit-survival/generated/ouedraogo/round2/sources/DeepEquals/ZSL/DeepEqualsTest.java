package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.math.BigDecimal;

/**
 * Comprehensive JUnit 4 tests for {@link DeepEquals}.
 */
public class DeepEqualsTest {

    /* ---------------------------------------------------------------------- */
    /* Helper classes used in the tests                                         */
    /* ---------------------------------------------------------------------- */

    /** Simple class without custom equals / hashCode */
    private static class SimpleNoEquals {
        int x;
        SimpleNoEquals(int x) { this.x = x; }
    }

    /** Class with a custom equals that only looks at field 'x' (ignores 'y') */
    private static class WithCustomEqualsIgnore {
        int x;
        int y;
        WithCustomEqualsIgnore(int x, int y) { this.x = x; this.y = y; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof WithCustomEqualsIgnore)) return false;
            return this.x == ((WithCustomEqualsIgnore) o).x; // deliberately ignore y
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(x);
        }
    }

    /** Class that overrides both equals and hashCode (full field comparison) */
    private static class WithCustomEquals {
        int x;
        WithCustomEquals(int x) { this.x = x; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof WithCustomEquals)) return false;
            return this.x == ((WithCustomEquals) o).x;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(x);
        }
    }

    /** Class that only overrides hashCode */
    private static class WithCustomHashOnly {
        int x;
        WithCustomHashOnly(int x) { this.x = x; }

        @Override
        public int hashCode() {
            return Integer.hashCode(x);
        }
    }

    /** Node class used to create cyclic object graphs */
    private static class Node {
        int value;
        Node next;
        Node(int value) { this.value = value; }
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for primitive / number handling                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNumberCrossTypeEquality() {
        assertTrue(DeepEquals.deepEquals(5, 5L));
        assertTrue(DeepEquals.deepEquals(5.0f, 5.0d));
        assertTrue(DeepEquals.deepEquals((short)2, (byte)2));
    }

    @Test
    public void testFloatingPointEpsilon() {
        // Values within the defined epsilon should be considered equal
        assertTrue(DeepEquals.deepEquals(0.1f, 0.1000001f)); // within floatEpsilon
        assertTrue(DeepEquals.deepEquals(0.1d, 0.100000000000001d)); // within doubleEpsilon

        // Values outside epsilon should be unequal
        assertFalse(DeepEquals.deepEquals(0.1f, 0.2f));
        assertFalse(DeepEquals.deepEquals(0.1d, 0.2d));
    }

    @Test
    public void testStringNumberMatchingOption() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("123", 123L, opts));
        assertTrue(DeepEquals.deepEquals(45, "45", opts));

        // Without the option, the comparison should fail
        assertFalse(DeepEquals.deepEquals("123", 123L));
    }

    @Test
    public void testAtomicBooleanEquality() {
        AtomicBoolean a = new AtomicBoolean(true);
        AtomicBoolean b = new AtomicBoolean(true);
        AtomicBoolean c = new AtomicBoolean(false);
        assertTrue(DeepEquals.deepEquals(a, b));
        assertFalse(DeepEquals.deepEquals(a, c));
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for collections (ordered & unordered)                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testOrderedCollectionEquality() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        List<Integer> l3 = Arrays.asList(3, 2, 1);
        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));
    }

    @Test
    public void testUnorderedSetEquality() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("c", "b", "a"));
        Set<String> s3 = new HashSet<>(Arrays.asList("a", "b"));
        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    @Test
    public void testSetWithCustomEqualsObjects() {
        WithCustomEqualsIgnore o1 = new WithCustomEqualsIgnore(10, 100);
        WithCustomEqualsIgnore o2 = new WithCustomEqualsIgnore(10, 200); // y differs
        Set<WithCustomEqualsIgnore> set1 = new HashSet<>(Collections.singletonList(o1));
        Set<WithCustomEqualsIgnore> set2 = new HashSet<>(Collections.singletonList(o2));
        // Custom equals says they are equal (ignores y)
        assertTrue(DeepEquals.deepEquals(set1, set2));

        // Now ignore custom equals – fields will be compared, y differs => false
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.singleton(WithCustomEqualsIgnore.class));
        assertFalse(DeepEquals.deepEquals(set1, set2, opts));
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for maps                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMapEquality() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);
        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("two", 2);
        m2.put("one", 1);
        assertTrue(DeepEquals.deepEquals(m1, m2));

        m2.put("three", 3);
        assertFalse(DeepEquals.deepEquals(m1, m2));
    }

    @Test
    public void testMapWithCustomEqualsKey() {
        WithCustomEqualsIgnore key1 = new WithCustomEqualsIgnore(5, 10);
        WithCustomEqualsIgnore key2 = new WithCustomEqualsIgnore(5, 20); // y differs, equals true
        Map<WithCustomEqualsIgnore, String> map1 = new HashMap<>();
        map1.put(key1, "value");
        Map<WithCustomEqualsIgnore, String> map2 = new HashMap<>();
        map2.put(key2, "value");
        assertTrue(DeepEquals.deepEquals(map1, map2));
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for arrays                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testArrayEquality() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {3, 2, 1};
        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    @Test
    public void testMultiDimensionalArrayEquality() {
        String[][] arr1 = { {"a", "b"}, {"c"} };
        String[][] arr2 = { {"a", "b"}, {"c"} };
        String[][] arr3 = { {"a", "b"}, {"d"} };
        assertTrue(DeepEquals.deepEquals(arr1, arr2));
        assertFalse(DeepEquals.deepEquals(arr1, arr3));
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for custom equals / hashCode detection                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHasCustomEqualsDetection() {
        assertTrue(DeepEquals.hasCustomEquals(WithCustomEquals.class));
        assertTrue(DeepEquals.hasCustomEquals(WithCustomEqualsIgnore.class));
        assertFalse(DeepEquals.hasCustomEquals(SimpleNoEquals.class));
    }

    @Test
    public void testHasCustomHashCodeDetection() {
        assertTrue(DeepEquals.hasCustomHashCode(WithCustomHashOnly.class));
        assertTrue(DeepEquals.hasCustomHashCode(WithCustomEquals.class)); // overrides both
        assertFalse(DeepEquals.hasCustomHashCode(SimpleNoEquals.class));
    }

    @Test
    public void testDeepEqualsWithCustomEqualsVsIgnoreOption() {
        WithCustomEqualsIgnore a = new WithCustomEqualsIgnore(1, 100);
        WithCustomEqualsIgnore b = new WithCustomEqualsIgnore(1, 200); // y differs

        // Default: custom equals is used, objects are considered equal
        assertTrue(DeepEquals.deepEquals(a, b));

        // Ignore custom equals – now field-by-field comparison sees y differs
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.singleton(WithCustomEqualsIgnore.class));
        assertFalse(DeepEquals.deepEquals(a, b, opts));
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for cycle detection                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCyclicGraphEquality() {
        Node a1 = new Node(1);
        Node b1 = new Node(2);
        a1.next = b1;
        b1.next = a1; // cycle

        Node a2 = new Node(1);
        Node b2 = new Node(2);
        a2.next = b2;
        b2.next = a2; // parallel cycle

        assertTrue(DeepEquals.deepEquals(a1, a2));
    }

    @Test
    public void testCyclicGraphInequality() {
        Node a1 = new Node(1);
        Node b1 = new Node(2);
        a1.next = b1;
        b1.next = a1; // cycle

        Node a2 = new Node(1);
        Node b2 = new Node(3); // different value
        a2.next = b2;
        b2.next = a2;

        assertFalse(DeepEquals.deepEquals(a1, a2));
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for isContainerType                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsContainerType() {
        assertTrue(DeepEquals.isContainerType(new ArrayList<>()));
        assertTrue(DeepEquals.isContainerType(new HashSet<>()));
        assertTrue(DeepEquals.isContainerType(new HashMap<>()));
        assertFalse(DeepEquals.isContainerType("string"));
        assertFalse(DeepEquals.isContainerType(42));
    }

    /* ---------------------------------------------------------------------- */
    /* Tests for deepHashCode consistency with deepEquals                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeConsistency() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        assertTrue(DeepEquals.deepEquals(list1, list2));
        assertEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));

        // Different content -> different hash (not guaranteed but highly likely)
        List<Integer> list3 = Arrays.asList(3, 2, 1);
        assertFalse(DeepEquals.deepEquals(list1, list3));
        // We allow hash collision but we assert that at least one of them is different
        // to avoid flaky test we only check inequality when objects are not equal
        if (!list1.equals(list3)) {
            assertNotEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list3));
        }
    }

    @Test
    public void testDeepHashCodeWithFloatAndDoublePrecision() {
        Float f1 = 0.123456f;
        Float f2 = 0.123457f; // within float epsilon
        Double d1 = 0.12345678901;
        Double d2 = 0.12345678902; // within double epsilon

        assertEquals(DeepEquals.deepHashCode(f1), DeepEquals.deepHashCode(f2));
        assertEquals(DeepEquals.deepHashCode(d1), DeepEquals.deepHashCode(d2));
    }

    @Test
    public void testDeepHashCodeWithCustomHashMethod() {
        WithCustomHashOnly obj1 = new WithCustomHashOnly(123);
        WithCustomHashOnly obj2 = new WithCustomHashOnly(123);
        assertEquals(DeepEquals.deepHashCode(obj1), DeepEquals.deepHashCode(obj2));
    }

    /* ---------------------------------------------------------------------- */
    /* Additional regression tests for edge cases                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNullHandling() {
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
        assertFalse(DeepEquals.deepEquals("not null", null));
    }

    @Test
    public void testSameInstanceShortCircuit() {
        Object o = new Object();
        assertTrue(DeepEquals.deepEquals(o, o));
    }

    @Test
    public void testClassObjectsEquality() {
        assertTrue(DeepEquals.deepEquals(String.class, String.class));
        assertFalse(DeepEquals.deepEquals(String.class, Integer.class));
    }
}
