package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.*;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Comprehensive JUnit‑4 test suite for {@link DeepEquals}.
 *
 * <p>This test class exercises every public entry point of {@code DeepEquals}
 * as well as the most important internal behaviours that are observable
 * through the public API (e.g. diff messages, option handling, custom
 * {@code equals()} handling, circular reference detection, and deep hash
 * code stability).</p>
 *
 * <p>Only standard Java types and the classes defined inside this file are
 * used – no external mocking frameworks or subclassing of library internals.</p>
 */
public class DeepEqualsTest {

    /** Helper class with a custom {@code equals()} implementation (only {@code name} matters). */
    static class Person {
        String name;
        int age;
        String city;

        Person(String name, int age, String city) {
            this.name = name;
            this.age = age;
            this.city = city;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Person)) return false;
            Person p = (Person) o;
            // custom equality: only name is considered
            return Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name); // consistent with custom equals
        }
    }

    /** Simple POJO without a custom equals – used for field‑by‑field comparison. */
    static class Container {
        int id;
        String label;
        List<Integer> numbers;

        Container(int id, String label, List<Integer> numbers) {
            this.id = id;
            this.label = label;
            this.numbers = numbers;
        }
    }

    /** Class that references itself – used to test circular reference handling. */
    static class Node {
        String name;
        Node next;

        Node(String name) {
            this.name = name;
        }
    }

    private Map<String, Object> options;

    @Before
    public void setUp() {
        options = new HashMap<>();
    }

    /*--------------------------------------------------------------
     *  Simple primitive / null handling
     *--------------------------------------------------------------*/

    @Test
    public void testSimpleEqualityAndNulls() {
        assertTrue(DeepEquals.deepEquals(5, 5));
        assertTrue(DeepEquals.deepEquals("hello", "hello"));
        assertTrue(DeepEquals.deepEquals(null, null));

        assertFalse(DeepEquals.deepEquals(5, null));
        assertFalse(DeepEquals.deepEquals(null, "x"));
        assertFalse(DeepEquals.deepEquals("a", "b"));
    }

    /*--------------------------------------------------------------
     *  Number comparison (including floating point tolerance)
     *--------------------------------------------------------------*/

    @Test
    public void testFloatingPointPrecision() {
        // Values differ by less than the epsilon (1e‑15) – should be equal
        double a = 1.000000000000001;
        double b = 1.000000000000002;
        assertTrue(DeepEquals.deepEquals(a, b));

        // Values differ beyond epsilon – should be unequal and produce a diff
        double c = 1.0;
        double d = 1.0000000001;
        assertFalse(DeepEquals.deepEquals(c, d, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("value mismatch"));
    }

    /*--------------------------------------------------------------
     *  String ↔ Number matching (option controlled)
     *--------------------------------------------------------------*/

    @Test
    public void testStringNumberMatchingEnabled() {
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("42", 42, options));
        assertTrue(DeepEquals.deepEquals(3.14, "3.14", options));
    }

    @Test
    public void testStringNumberMatchingDisabled() {
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);
        assertFalse(DeepEquals.deepEquals("42", 42, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("value mismatch"));
    }

    /*--------------------------------------------------------------
     *  Custom equals handling and ignore‑custom‑equals option
     *--------------------------------------------------------------*/

    @Test
    public void testCustomEqualsIsUsedByDefault() {
        Person p1 = new Person("John", 30, "NY");
        Person p2 = new Person("John", 99, "LA"); // age and city differ, but custom equals ignores them
        assertTrue(DeepEquals.deepEquals(p1, p2));
    }

    @Test
    public void testIgnoreCustomEqualsForSpecificClass() {
        Person p1 = new Person("John", 30, "NY");
        Person p2 = new Person("John", 99, "LA");

        // Ignore custom equals for Person – forces field‑by‑field comparison
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(Person.class));
        assertFalse(DeepEquals.deepEquals(p1, p2, options));

        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        // The first differing field should be "age"
        assertTrue(diff.contains(".age"));
    }

    @Test
    public void testIgnoreAllCustomEquals() {
        Person p1 = new Person("John", 30, "NY");
        Person p2 = new Person("John", 99, "LA");

        // Empty set → ignore *all* custom equals
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        assertFalse(DeepEquals.deepEquals(p1, p2, options));

        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains(".age"));
    }

    @Test
    public void testHasCustomEqualsAndHashCodeDetection() {
        assertTrue(DeepEquals.hasCustomEquals(Person.class));
        assertTrue(DeepEquals.hasCustomHashCode(Person.class));

        assertFalse(DeepEquals.hasCustomEquals(Container.class));
        assertFalse(DeepEquals.hasCustomHashCode(Container.class));
    }

    /*--------------------------------------------------------------
     *  Ordered collections (List) – order matters
     *--------------------------------------------------------------*/

    @Test
    public void testOrderedListEquality() {
        List<String> a = Arrays.asList("one", "two", "three");
        List<String> b = Arrays.asList("one", "two", "three");
        assertTrue(DeepEquals.deepEquals(a, b));

        List<String> c = Arrays.asList("one", "three", "two");
        assertFalse(DeepEquals.deepEquals(a, c, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        // The first mismatch is at index 1
        assertTrue(diff.contains("[1]"));
    }

    /*--------------------------------------------------------------
     *  Unordered collections (Set) – order ignored
     *--------------------------------------------------------------*/

    @Test
    public void testUnorderedSetEquality() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 2, 1));
        assertTrue(DeepEquals.deepEquals(s1, s2));
    }

    @Test
    public void testUnorderedSetMissingElement() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(1, 2));
        assertFalse(DeepEquals.deepEquals(s1, s2, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("missing collection element"));
    }

    /*--------------------------------------------------------------
     *  Map handling
     *--------------------------------------------------------------*/

    @Test
    public void testMapEquality() {
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("a", 1);
        m2.put("b", 2);
        assertTrue(DeepEquals.deepEquals(m1, m2));
    }

    @Test
    public void testMapMissingKey() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("x", 10);
        m1.put("y", 20);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("x", 10);
        // "y" missing
        assertFalse(DeepEquals.deepEquals(m1, m2, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("missing map key"));
    }

    @Test
    public void testMapValueMismatch() {
        Map<String, String> m1 = new HashMap<>();
        m1.put("k", "v1");
        Map<String, String> m2 = new HashMap<>();
        m2.put("k", "v2");
        assertFalse(DeepEquals.deepEquals(m1, m2, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("map value mismatch"));
    }

    /*--------------------------------------------------------------
     *  Array handling – dimensions, component type, length, elements
     *--------------------------------------------------------------*/

    @Test
    public void testArrayEquality() {
        int[] a = {1, 2, 3};
        int[] b = {1, 2, 3};
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testArrayLengthMismatch() {
        int[] a = {1, 2, 3};
        int[] b = {1, 2};
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array length mismatch"));
    }

    @Test
    public void testArrayComponentTypeMismatch() {
        int[] a = {1, 2};
        long[] b = {1L, 2L};
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array component type mismatch"));
    }

    @Test
    public void testArrayElementMismatch() {
        String[] a = {"foo", "bar"};
        String[] b = {"foo", "baz"};
        assertFalse(DeepEquals.deepEquals(a, b, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        // Index 1 should be highlighted
        assertTrue(diff.contains("[1]"));
        assertTrue(diff.contains("Expected: \"bar\""));
        assertTrue(diff.contains("Found: \"baz\""));
    }

    @Test
    public void testMultidimensionalArrayEquality() {
        int[][] a = {{1, 2}, {3, 4}};
        int[][] b = {{1, 2}, {3, 4}};
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    /*--------------------------------------------------------------
     *  Object field comparison (when custom equals is ignored)
     *--------------------------------------------------------------*/

    @Test
    public void testObjectFieldDifference() {
        Container c1 = new Container(1, "alpha", Arrays.asList(10, 20));
        Container c2 = new Container(2, "alpha", Arrays.asList(10, 20)); // id differs

        // No ignore set → DeepEquals will compare fields (Container has no custom equals)
        assertFalse(DeepEquals.deepEquals(c1, c2, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        // The path should reference the field "id"
        assertTrue(diff.contains(".id"));
        assertTrue(diff.contains("Expected: 1"));
        assertTrue(diff.contains("Found: 2"));
    }

    /*--------------------------------------------------------------
     *  Circular reference handling – should not loop forever
     *--------------------------------------------------------------*/

    @Test
    public void testCircularReferenceEquality() {
        Node n1 = new Node("first");
        Node n2 = new Node("first");
        n1.next = n2;
        n2.next = n1; // creates a cycle

        Node m1 = new Node("first");
        Node m2 = new Node("first");
        m1.next = m2;
        m2.next = m1; // identical structure

        assertTrue(DeepEquals.deepEquals(n1, m1));
    }

    @Test
    public void testCircularReferenceDifference() {
        Node n1 = new Node("first");
        Node n2 = new Node("first");
        n1.next = n2;
        n2.next = n1; // cycle

        Node m1 = new Node("first");
        Node m2 = new Node("second"); // different name
        m1.next = m2;
        m2.next = m1; // cycle

        assertFalse(DeepEquals.deepEquals(n1, m1, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        // The difference should be on the field "name" of the second node
        assertTrue(diff.contains(".name"));
        assertTrue(diff.contains("Expected: \"first\""));
        assertTrue(diff.contains("Found: \"second\""));
    }

    /*--------------------------------------------------------------
     *  deepHashCode consistency – equal objects must have same hash
     *--------------------------------------------------------------*/

    @Test
    public void testDeepHashCodeEquality() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        assertEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));

        // Different content → hash likely differs (not required to be different,
        // but a mismatch would be a sign the algorithm is broken)
        List<Integer> list3 = Arrays.asList(1, 2, 4);
        assertNotEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list3));
    }

    /*--------------------------------------------------------------
     *  Additional sanity checks for diff generation format
     *--------------------------------------------------------------*/

    @Test
    public void testDiffMessageContainsRootContext() {
        Container c1 = new Container(1, "alpha", Arrays.asList(10, 20));
        Container c2 = new Container(1, "beta", Arrays.asList(10, 20)); // label differs

        assertFalse(DeepEquals.deepEquals(c1, c2, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        // Root context should be the concise representation of the Container
        assertTrue(diff.startsWith("["));
        assertTrue(diff.contains("Container"));
        // Path should include ".label"
        assertTrue(diff.contains(".label"));
    }

    @Test
    public void testAtomicBooleanComparison() {
        AtomicBoolean a = new AtomicBoolean(true);
        AtomicBoolean b = new AtomicBoolean(true);
        AtomicBoolean c = new AtomicBoolean(false);
        assertTrue(DeepEquals.deepEquals(a, b));
        assertFalse(DeepEquals.deepEquals(a, c, options));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("value mismatch"));
    }
}
