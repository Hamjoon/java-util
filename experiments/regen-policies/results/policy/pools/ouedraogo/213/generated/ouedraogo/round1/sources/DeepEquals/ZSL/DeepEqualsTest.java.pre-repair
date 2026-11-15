package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.*;

public class DeepEqualsTest {

    /* --------------------------------------------------------------------- *
     *  Helper classes used by several tests
     * --------------------------------------------------------------------- */

    /** Simple class with a custom equals that only checks the `name` field. */
    static class Person {
        String name;
        int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person p = (Person) o;
            // custom equals deliberately ignores `age`
            return Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    /** Simple class with no custom equals / hashCode. */
    static class Plain {
        String s;
        int i;

        Plain(String s, int i) {
            this.s = s;
            this.i = i;
        }
    }

    /** Node class used to create circular references. */
    static class Node {
        String id;
        Node next;

        Node(String id) {
            this.id = id;
        }
    }

    /* --------------------------------------------------------------------- *
     *  1. Basic primitive / wrapper equality
     * --------------------------------------------------------------------- */
    @Test
    public void testSimpleEquality() {
        assertTrue(DeepEquals.deepEquals(5, 5));
        assertTrue(DeepEquals.deepEquals("hello", "hello"));
        assertTrue(DeepEquals.deepEquals(null, null));

        assertFalse(DeepEquals.deepEquals(5, 6));
        assertFalse(DeepEquals.deepEquals("hello", "world"));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
    }

    /* --------------------------------------------------------------------- *
     *  2. Floating‑point tolerance (nearly equal)
     * --------------------------------------------------------------------- */
    @Test
    public void testFloatingPointTolerance() {
        double a = 0.1 + 0.2; // not exactly 0.3 due to binary rounding
        double b = 0.3;
        assertTrue(DeepEquals.deepEquals(a, b));

        // Force a mismatch that is larger than epsilon
        double c = 0.3000000000001;
        assertFalse(DeepEquals.deepEquals(b, c));
    }

    /* --------------------------------------------------------------------- *
     *  3. String ↔ Number comparison when the option is enabled
     * --------------------------------------------------------------------- */
    @Test
    public void testStringNumberMatchingOption() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        assertTrue(DeepEquals.deepEquals("123", 123, opts));
        assertTrue(DeepEquals.deepEquals(45.6, "45.6", opts));

        // Without the option the same pairs should be unequal
        assertFalse(DeepEquals.deepEquals("123", 123));
        assertFalse(DeepEquals.deepEquals(45.6, "45.6"));
    }

    /* --------------------------------------------------------------------- *
     *  4. Ignoring custom equals (empty set → ignore all)
     * --------------------------------------------------------------------- */
    @Test
    public void testIgnoreAllCustomEquals() {
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 40); // same name, different age

        // By default custom equals is used → they are considered equal
        assertTrue(DeepEquals.deepEquals(p1, p2));

        // Empty ignore set → all custom equals are ignored → field‑by‑field compare
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());

        assertFalse(DeepEquals.deepEquals(p1, p2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[field value mismatch]"));
        assertTrue(diff.contains(".age"));
    }

    /* --------------------------------------------------------------------- *
     *  5. Using custom equals (default behaviour)
     * --------------------------------------------------------------------- */
    @Test
    public void testUseCustomEqualsByDefault() {
        Person p1 = new Person("Bob", 25);
        Person p2 = new Person("Bob", 99); // age differs but custom equals ignores it

        assertTrue(DeepEquals.deepEquals(p1, p2));
    }

    /* --------------------------------------------------------------------- *
     *  6. Circular reference handling
     * --------------------------------------------------------------------- */
    @Test
    public void testCircularReference() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = b;
        b.next = a; // creates a cycle

        Node aCopy = new Node("A");
        Node bCopy = new Node("B");
        aCopy.next = bCopy;
        bCopy.next = aCopy;

        assertTrue(DeepEquals.deepEquals(a, aCopy));
    }

    /* --------------------------------------------------------------------- *
     *  7. Unordered collection (Set) equality and mismatch diff
     * --------------------------------------------------------------------- */
    @Test
    public void testUnorderedCollection() {
        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 2, 1));
        assertTrue(DeepEquals.deepEquals(set1, set2));

        Set<Integer> set3 = new HashSet<>(Arrays.asList(1, 2, 4));
        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(set1, set3, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[missing collection element]"));
    }

    /* --------------------------------------------------------------------- *
     *  8. Ordered collection (List) equality and order‑sensitivity
     * --------------------------------------------------------------------- */
    @Test
    public void testOrderedCollection() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        assertTrue(DeepEquals.deepEquals(list1, list2));

        List<String> list3 = Arrays.asList("a", "c", "b");
        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(list1, list3, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[collection element mismatch]"));
        assertTrue(diff.contains("[0]")); // index 0 is same, mismatch appears at index 1 or 2
    }

    /* --------------------------------------------------------------------- *
     *  9. Map comparison and diff message
     * --------------------------------------------------------------------- */
    @Test
    public void testMapDifferenceAndDiffMessage() {
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);

        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("one", 1);
        m2.put("two", 3); // value differs

        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(m1, m2, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        // Expect a map‑value‑mismatch description
        assertTrue(diff.contains("[map value mismatch]"));
        assertTrue(diff.contains("《\"two\" ⇨"));
        assertTrue(diff.contains("Expected: 2"));
        assertTrue(diff.contains("Found: 3"));
    }

    /* --------------------------------------------------------------------- *
     * 10. Multi‑dimensional array equality
     * --------------------------------------------------------------------- */
    @Test
    public void testMultiDimensionalArrayEquality() {
        int[][] a1 = { {1, 2}, {3, 4} };
        int[][] a2 = { {1, 2}, {3, 4} };
        assertTrue(DeepEquals.deepEquals(a1, a2));

        int[][] a3 = { {1, 2}, {3, 5} };
        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(a1, a3, opts));
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[array element mismatch]"));
        assertTrue(diff.contains("[1][1]")); // second row, second column
    }

    /* --------------------------------------------------------------------- *
     * 11. deepHashCode consistency for equal objects
     * --------------------------------------------------------------------- */
    @Test
    public void testDeepHashCodeConsistency() {
        Plain p1 = new Plain("test", 42);
        Plain p2 = new Plain("test", 42);
        assertTrue(DeepEquals.deepEquals(p1, p2));

        int h1 = DeepEquals.deepHashCode(p1);
        int h2 = DeepEquals.deepHashCode(p2);
        assertEquals(h1, h2);
    }

    /* --------------------------------------------------------------------- *
     * 12. hasCustomEquals / hasCustomHashCode detection
     * --------------------------------------------------------------------- */
    @Test
    public void testHasCustomEqualsAndHashCode() {
        // Person overrides both equals and hashCode
        assertTrue(DeepEquals.hasCustomEquals(Person.class));
        assertTrue(DeepEquals.hasCustomHashCode(Person.class));

        // Plain does NOT override either
        assertFalse(DeepEquals.hasCustomEquals(Plain.class));
        assertFalse(DeepEquals.hasCustomHashCode(Plain.class));

        // Built‑in classes (e.g., String) also have custom implementations
        assertTrue(DeepEquals.hasCustomEquals(String.class));
        assertTrue(DeepEquals.hasCustomHashCode(String.class));
    }

    /* --------------------------------------------------------------------- *
     * 13. Verify that deepEquals returns false for different types
     * --------------------------------------------------------------------- */
    @Test
    public void testTypeMismatch() {
        assertFalse(DeepEquals.deepEquals("string", 123));
        Map<String, Object> opts = new HashMap<>();
        DeepEquals.deepEquals("string", 123, opts);
        String diff = (String) opts.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("[type mismatch]"));
    }

    /* --------------------------------------------------------------------- *
     * 14. Verify that numeric comparison works for BigDecimal vs Double
     * --------------------------------------------------------------------- */
    @Test
    public void testBigDecimalNumberComparison() {
        BigDecimal bd = new BigDecimal("123.456");
        double d = 123.456;
        assertTrue(DeepEquals.deepEquals(bd, d));

        double d2 = 123.4567;
        assertFalse(DeepEquals.deepEquals(bd, d2));
    }
}
