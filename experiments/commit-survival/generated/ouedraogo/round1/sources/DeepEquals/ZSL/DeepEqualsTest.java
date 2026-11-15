package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 tests for {@link DeepEquals}.
 *
 * The tests cover:
 * <ul>
 *   <li>Primitive and wrapper number comparison (including floating point epsilon)</li>
 *   <li>String‑to‑Number matching via options</li>
 *   <li>Custom {@code equals()} handling and the IGNORE_CUSTOM_EQUALS option</li>
 *   <li>Ordered collections, unordered sets, maps and arrays</li>
 *   <li>Cycle detection</li>
 *   <li>Container type detection</li>
 *   <li>Deep hash code generation and its stability</li>
 *   <li>Detection of custom {@code equals()} and {@code hashCode()} methods</li>
 * </ul>
 */
public class DeepEqualsTest {

    /** Simple POJO with a custom equals / hashCode implementation. */
    static class WithCustomEquals {
        int id;
        String name;

        WithCustomEquals(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof WithCustomEquals)) return false;
            WithCustomEquals that = (WithCustomEquals) o;
            // custom logic: equality based ONLY on id, ignore name
            return this.id == that.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    /** Simple POJO without any custom equals / hashCode. */
    static class PlainPojo {
        int a;
        String b;

        PlainPojo(int a, String b) {
            this.a = a;
            this.b = b;
        }
    }

    /** Node class used to build cyclic object graphs. */
    static class Node {
        String value;
        Node next;

        Node(String value) {
            this.value = value;
        }
    }

    private Map<String, Object> emptyOptions;

    @Before
    public void setUp() {
        emptyOptions = new HashMap<>();
    }

    /* ---------------------------------------------------------------------- */
    /* 1. Primitive / Wrapper number comparison                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsNumberValueEquality() {
        Integer i = 42;
        Long l = 42L;
        assertTrue(DeepEquals.deepEquals(i, l));
        assertTrue(DeepEquals.deepEquals(l, i));

        Integer i2 = 43;
        assertFalse(DeepEquals.deepEquals(i, i2));
    }

    @Test
    public void testDeepEqualsFloatingPointEpsilon() {
        Float f = 0.123456f;               // 6 decimal places
        Double d = 0.123456001;            // within float epsilon (1e-6)
        assertTrue(DeepEquals.deepEquals(f, d));

        Double d2 = 0.1235;               // outside epsilon
        assertFalse(DeepEquals.deepEquals(f, d2));
    }

    /* ---------------------------------------------------------------------- */
    /* 2. String ↔ Number matching via options                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testStringNumberMatchingDisabledByDefault() {
        String s = "100";
        Integer i = 100;
        assertFalse(DeepEquals.deepEquals(s, i));
    }

    @Test
    public void testStringNumberMatchingEnabled() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        String s = "12345";
        Long l = 12345L;
        assertTrue(DeepEquals.deepEquals(s, l, opts));
        assertTrue(DeepEquals.deepEquals(l, s, opts));

        // Non‑numeric string still fails
        assertFalse(DeepEquals.deepEquals("abc", 0, opts));
    }

    /* ---------------------------------------------------------------------- */
    /* 3. Custom equals handling                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsUsesCustomEqualsByDefault() {
        WithCustomEquals a = new WithCustomEquals(1, "Alice");
        WithCustomEquals b = new WithCustomEquals(1, "Bob"); // different name, same id
        // custom equals only checks id → should be true
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEqualsIgnoreCustomEqualsOption() {
        WithCustomEquals a = new WithCustomEquals(2, "Alice");
        WithCustomEquals b = new WithCustomEquals(2, "Bob"); // same id, different name

        // Custom equals would say true, but we want field‑by‑field comparison
        Set<Class<?>> ignore = new HashSet<>();
        ignore.add(WithCustomEquals.class);
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignore);

        // Field comparison finds different name → false
        assertFalse(DeepEquals.deepEquals(a, b, opts));

        // When fields match, result is true
        WithCustomEquals c = new WithCustomEquals(2, "Alice");
        assertTrue(DeepEquals.deepEquals(a, c, opts));
    }

    /* ---------------------------------------------------------------------- */
    /* 4. Ordered collections (Lists)                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsOrderedCollections() {
        List<Integer> l1 = Arrays.asList(1, 2, 3);
        List<Integer> l2 = Arrays.asList(1, 2, 3);
        List<Integer> l3 = Arrays.asList(3, 2, 1);

        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));
    }

    /* ---------------------------------------------------------------------- */
    /* 5. Unordered collections (Sets)                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsUnorderedSets() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new HashSet<>(Arrays.asList("c", "b", "a"));
        Set<String> s3 = new HashSet<>(Arrays.asList("a", "b"));

        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    /* ---------------------------------------------------------------------- */
    /* 6. Maps                                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsMaps() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new HashMap<>();
        m2.put("two", 2);
        m2.put("one", 1);

        Map<String, Integer> m3 = new HashMap<>();
        m3.put("one", 1);
        m3.put("two", 3); // different value

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    /* ---------------------------------------------------------------------- */
    /* 7. Arrays                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsPrimitiveArray() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {3, 2, 1};

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    @Test
    public void testDeepEqualsObjectArray() {
        String[] s1 = {"x", "y"};
        String[] s2 = {"x", "y"};
        String[] s3 = {"y", "x"};

        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    /* ---------------------------------------------------------------------- */
    /* 8. Cycle detection                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsWithCycles() {
        Node a1 = new Node("first");
        Node a2 = new Node("second");
        a1.next = a2;
        a2.next = a1; // cycle

        Node b1 = new Node("first");
        Node b2 = new Node("second");
        b1.next = b2;
        b2.next = b1; // identical cycle

        assertTrue(DeepEquals.deepEquals(a1, b1));
    }

    @Test
    public void testDeepEqualsWithDifferentCycles() {
        Node a1 = new Node("first");
        Node a2 = new Node("second");
        a1.next = a2;
        a2.next = a1; // cycle

        Node b1 = new Node("first");
        Node b2 = new Node("different");
        b1.next = b2;
        b2.next = b1; // different value in second node

        assertFalse(DeepEquals.deepEquals(a1, b1));
    }

    /* ---------------------------------------------------------------------- */
    /* 9. AtomicBoolean comparison                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsAtomicBoolean() {
        AtomicBoolean ab1 = new AtomicBoolean(true);
        AtomicBoolean ab2 = new AtomicBoolean(true);
        AtomicBoolean ab3 = new AtomicBoolean(false);

        assertTrue(DeepEquals.deepEquals(ab1, ab2));
        assertFalse(DeepEquals.deepEquals(ab1, ab3));
    }

    /* ---------------------------------------------------------------------- */
    /* 10. isContainerType                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsContainerType() {
        assertTrue(DeepEquals.isContainerType(new ArrayList<>()));
        assertTrue(DeepEquals.isContainerType(new HashSet<>()));
        assertTrue(DeepEquals.isContainerType(new HashMap<>()));
        assertFalse(DeepEquals.isContainerType("NotAContainer"));
        assertFalse(DeepEquals.isContainerType(123));
    }

    /* ---------------------------------------------------------------------- */
    /* 11. deepHashCode stability                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeDeterministic() {
        List<Object> list1 = new ArrayList<>();
        list1.add("a");
        list1.add(1);
        list1.add(new int[]{2, 3});

        List<Object> list2 = new ArrayList<>();
        list2.add("a");
        list2.add(1);
        list2.add(new int[]{2, 3});

        int h1 = DeepEquals.deepHashCode(list1);
        int h2 = DeepEquals.deepHashCode(list2);
        assertEquals(h1, h2);
    }

    @Test
    public void testDeepHashCodeDetectsDifference() {
        List<Object> list1 = Arrays.asList("a", 1);
        List<Object> list2 = Arrays.asList("a", 2);
        assertNotEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));
    }

    /* ---------------------------------------------------------------------- */
    /* 12. hasCustomEquals detection                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHasCustomEqualsDetection() {
        assertTrue(DeepEquals.hasCustomEquals(WithCustomEquals.class));
        assertFalse(DeepEquals.hasCustomEquals(PlainPojo.class));
        // Primitive wrapper classes have Object.equals overridden, but they are
        // considered "custom" by the implementation because they declare equals.
        assertTrue(DeepEquals.hasCustomEquals(Integer.class));
    }

    /* ---------------------------------------------------------------------- */
    /* 13. hasCustomHashCode detection                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHasCustomHashCodeDetection() {
        assertTrue(DeepEquals.hasCustomHashCode(WithCustomEquals.class));
        assertFalse(DeepEquals.hasCustomHashCode(PlainPojo.class));
        // Most JDK wrapper classes override hashCode()
        assertTrue(DeepEquals.hasCustomHashCode(Integer.class));
    }

    /* ---------------------------------------------------------------------- */
    /* 14. Deep equals with nulls and identity                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeepEqualsNullHandling() {
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
        assertFalse(DeepEquals.deepEquals("not null", null));
    }

    @Test
    public void testDeepEqualsIdentity() {
        Object obj = new Object();
        assertTrue(DeepEquals.deepEquals(obj, obj));
    }
}
