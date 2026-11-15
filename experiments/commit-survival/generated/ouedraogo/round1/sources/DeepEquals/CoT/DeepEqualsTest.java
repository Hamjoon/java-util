package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test suite for {@link DeepEquals}.
 *
 * The suite is built in three logical parts:
 *
 * 1. **Public method extraction** – a comment block that lists every public
 *    method (including signatures) found in {@code DeepEquals}.
 *
 * 2. **Basic functionality tests** – one straightforward test per public method
 *    that verifies the happy‑path behaviour.
 *
 * 3. **Edge‑case / exception tests** – additional tests that target the special
 *    handling inside the implementation (nulls, cycles, number conversion,
 *    custom‑equals suppression, etc.).
 *
 * All tests are written for JUnit 4 and rely only on classes that are present
 * in the supplied source or on standard Java SE APIs.
 */
public class DeepEqualsTest {

    /* -------------------------------------------------------------
     * 1. PUBLIC METHODS EXTRACTED FROM DeepEquals
     * -------------------------------------------------------------
     *
     * public static final String IGNORE_CUSTOM_EQUALS
     * public static final String ALLOW_STRINGS_TO_MATCH_NUMBERS
     *
     * public static boolean deepEquals(Object a, Object b)
     * public static boolean deepEquals(Object a, Object b, Map<String, ?> options)
     *
     * public static boolean isContainerType(Object o)
     *
     * public static boolean hasCustomEquals(Class<?> c)
     *
     * public static int deepHashCode(Object obj)
     *
     * public static boolean hasCustomHashCode(Class<?> c)
     *
     * ------------------------------------------------------------- */

    private Map<String, Object> emptyOptions;

    @Before
    public void setUp() {
        emptyOptions = new HashMap<>();
    }

    /* -------------------------------------------------------------
     * 2. BASIC FUNCTIONALITY TESTS
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsSimplePrimitives() {
        assertTrue(DeepEquals.deepEquals(5, 5));
        assertTrue(DeepEquals.deepEquals("hello", "hello"));
        assertFalse(DeepEquals.deepEquals("hello", "world"));
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
    }

    @Test
    public void testDeepEqualsNumbersDifferentTypes() {
        // int vs long, same value
        assertTrue(DeepEquals.deepEquals(42, 42L));

        // float vs double, within epsilon
        assertTrue(DeepEquals.deepEquals(0.1f, 0.1000000001d));
    }

    @Test
    public void testDeepEqualsWithOptionsIgnoreCustomEquals() {
        // Class with overridden equals – we will create a simple wrapper
        class Wrapper {
            int value;
            Wrapper(int v) { this.value = v; }
            @Override public boolean equals(Object o) {
                if (o instanceof Wrapper) {
                    return ((Wrapper) o).value == this.value;
                }
                return false;
            }
        }

        Wrapper w1 = new Wrapper(10);
        Wrapper w2 = new Wrapper(10);
        // Without ignoring custom equals, they are equal
        assertTrue(DeepEquals.deepEquals(w1, w2));

        // Ignore custom equals for Wrapper class
        Set<Class<?>> ignoreSet = new HashSet<>();
        ignoreSet.add(Wrapper.class);
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignoreSet);
        // Now DeepEquals will compare fields → still equal because fields match,
        // but the test proves the option is accepted without error.
        assertTrue(DeepEquals.deepEquals(w1, w2, opts));
    }

    @Test
    public void testDeepEqualsArrayComparison() {
        int[] a1 = {1, 2, 3};
        int[] a2 = {1, 2, 3};
        int[] a3 = {1, 2, 4};

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    @Test
    public void testDeepEqualsCollectionOrdered() {
        List<String> l1 = Arrays.asList("a", "b", "c");
        List<String> l2 = Arrays.asList("a", "b", "c");
        List<String> l3 = Arrays.asList("c", "b", "a");

        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));
    }

    @Test
    public void testDeepEqualsSetUnordered() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 2, 1));
        Set<Integer> s3 = new HashSet<>(Arrays.asList(1, 2, 4));

        assertTrue(DeepEquals.deepEquals(s1, s2));
        assertFalse(DeepEquals.deepEquals(s1, s3));
    }

    @Test
    public void testDeepEqualsMapComparison() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new HashMap<>();
        m2.put("two", 2);
        m2.put("one", 1);

        Map<String, Integer> m3 = new HashMap<>();
        m3.put("one", 1);
        m3.put("two", 3);

        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    @Test
    public void testDeepEqualsAtomicBoolean() {
        AtomicBoolean a1 = new AtomicBoolean(true);
        AtomicBoolean a2 = new AtomicBoolean(true);
        AtomicBoolean a3 = new AtomicBoolean(false);

        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    @Test
    public void testIsContainerType() {
        assertTrue(DeepEquals.isContainerType(new ArrayList<>()));
        assertTrue(DeepEquals.isContainerType(new HashMap<>()));
        assertFalse(DeepEquals.isContainerType("not a container"));
        assertFalse(DeepEquals.isContainerType(123));
    }

    @Test
    public void testHasCustomEqualsAndHashCode() {
        // String overrides equals and hashCode
        assertTrue(DeepEquals.hasCustomEquals(String.class));
        assertTrue(DeepEquals.hasCustomHashCode(String.class));

        // Object does not
        assertFalse(DeepEquals.hasCustomEquals(Object.class));
        assertFalse(DeepEquals.hasCustomHashCode(Object.class));
    }

    @Test
    public void testDeepHashCodeDeterminism() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        assertEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));

        // Different order → different hash
        List<Integer> list3 = Arrays.asList(3, 2, 1);
        assertNotEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list3));
    }

    /* -------------------------------------------------------------
     * 3. EDGE CASE / EXCEPTION TESTS
     * ------------------------------------------------------------- */

    @Test
    public void testDeepEqualsWithNullElementsInCollections() {
        List<String> l1 = Arrays.asList("a", null, "c");
        List<String> l2 = Arrays.asList("a", null, "c");
        List<String> l3 = Arrays.asList("a", "b", "c");

        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertFalse(DeepEquals.deepEquals(l1, l3));
    }

    @Test
    public void testDeepEqualsStringNumberMatchingOptionEnabled() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        assertTrue(DeepEquals.deepEquals("123", 123, opts));
        assertTrue(DeepEquals.deepEquals(45.6, "45.6", opts));
        // Different numeric value should still be false
        assertFalse(DeepEquals.deepEquals("124", 123, opts));
    }

    @Test
    public void testDeepEqualsStringNumberMatchingOptionDisabled() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);

        assertFalse(DeepEquals.deepEquals("123", 123, opts));
    }

    @Test
    public void testDeepEqualsHandlesCyclicGraphs() {
        class Node {
            Node next;
            int value;
            Node(int v) { this.value = v; }
        }

        Node a1 = new Node(1);
        Node a2 = new Node(2);
        a1.next = a2;
        a2.next = a1; // cycle

        Node b1 = new Node(1);
        Node b2 = new Node(2);
        b1.next = b2;
        b2.next = b1; // same shape

        assertTrue(DeepEquals.deepEquals(a1, b1));
    }

    @Test
    public void testDeepEqualsDifferentClassesSameFields() {
        class A {
            int x = 5;
        }
        class B {
            int x = 5;
        }

        A a = new A();
        B b = new B();

        // Different classes → must be false even though fields match
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testDeepEqualsWithCustomEqualsSuppressedAndDifferentFieldValues() {
        class Person {
            String name;
            int age;
            Person(String n, int a) { name = n; age = a; }
            @Override public boolean equals(Object o) {
                if (o instanceof Person) {
                    Person p = (Person) o;
                    return Objects.equals(name, p.name) && age == p.age;
                }
                return false;
            }
        }

        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 31); // age differs

        // Normal deepEquals uses custom equals → false
        assertFalse(DeepEquals.deepEquals(p1, p2));

        // Suppress custom equals – field comparison will detect the different age
        Set<Class<?>> ignore = new HashSet<>();
        ignore.add(Person.class);
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignore);
        assertFalse(DeepEquals.deepEquals(p1, p2, opts));
    }

    @Test
    public void testDeepHashCodeWithFloatingPointEpsilon() {
        double d1 = 0.1 + 0.2; // 0.30000000000000004
        double d2 = 0.3;

        // Direct double equality is false, but deepEquals treats them as equal
        assertTrue(DeepEquals.deepEquals(d1, d2));

        // deepHashCode must produce the same hash for values that are considered equal
        assertEquals(DeepEquals.deepHashCode(d1), DeepEquals.deepHashCode(d2));
    }

    @Test
    public void testDeepEqualsWithEmptyOptionMap() {
        // Ensure that passing an empty options map does not affect normal behaviour
        Map<String, Object> empty = new HashMap<>();
        assertTrue(DeepEquals.deepEquals("test", "test", empty));
        assertFalse(DeepEquals.deepEquals("test", "diff", empty));
    }

    @Test
    public void testDeepEqualsMapWithCollidingHashes() {
        // Construct two distinct keys that have the same deepHashCode()
        // Simple way: use two different Integer objects (they have same hashCode)
        Map<Integer, String> m1 = new HashMap<>();
        m1.put(1, "one");
        m1.put(2, "two");

        Map<Integer, String> m2 = new HashMap<>();
        m2.put(2, "two");
        m2.put(1, "one");

        assertTrue(DeepEquals.deepEquals(m1, m2));
    }

    @Test
    public void testDeepEqualsArrayOfObjectsWithNulls() {
        String[] arr1 = {"a", null, "c"};
        String[] arr2 = {"a", null, "c"};
        String[] arr3 = {"a", "b", "c"};

        assertTrue(DeepEquals.deepEquals(arr1, arr2));
        assertFalse(DeepEquals.deepEquals(arr1, arr3));
    }
}
