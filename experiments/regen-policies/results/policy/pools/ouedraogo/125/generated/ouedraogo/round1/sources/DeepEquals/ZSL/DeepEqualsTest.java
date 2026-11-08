package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

import static com.cedarsoftware.util.DeepEquals.*;
import static org.junit.Assert.*;

public class DeepEqualsTest {

    /** Simple POJO with a custom equals() implementation. */
    static class Person {
        String name;
        int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        /** Custom equals that only cares about the name field. */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person p = (Person) o;
            return Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    /** Simple node class to create circular references. */
    static class Node {
        String id;
        Node next;

        Node(String id) {
            this.id = id;
        }
    }

    private Map<String, Object> options;

    @Before
    public void setUp() {
        options = new HashMap<>();
    }

    /* ----------------------------------------------------------------------
     *  Basic primitive/value comparisons
     * ---------------------------------------------------------------------- */

    @Test
    public void testSimpleEqualPrimitives() {
        assertTrue(DeepEquals.deepEquals(10, 10));
        assertTrue(DeepEquals.deepEquals("hello", "hello"));
        assertTrue(DeepEquals.deepEquals(true, true));
    }

    @Test
    public void testSimpleMismatchedPrimitives() {
        boolean result = DeepEquals.deepEquals(10, 20, options);
        assertFalse(result);
        assertNotNull(options.get(DIFF));
        String diff = (String) options.get(DIFF);
        assertTrue(diff.contains("value mismatch"));
        assertTrue(diff.contains("Expected: 10"));
        assertTrue(diff.contains("Found: 20"));
    }

    @Test
    public void testStringNumberMatchingEnabled() {
        options.put(ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("123", 123, options));
        assertTrue(DeepEquals.deepEquals(new BigDecimal("45.6"), "45.6", options));
    }

    @Test
    public void testStringNumberMatchingDisabled() {
        options.put(ALLOW_STRINGS_TO_MATCH_NUMBERS, false);
        assertFalse(DeepEquals.deepEquals("123", 123, options));
    }

    /* ----------------------------------------------------------------------
     *  Custom equals handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testCustomEqualsUsedByDefault() {
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 99); // age differs, but custom equals ignores it
        assertTrue(DeepEquals.deepEquals(p1, p2));
    }

    @Test
    public void testCustomEqualsIgnoredGlobally() {
        // Empty set => ignore ALL custom equals implementations
        options.put(IGNORE_CUSTOM_EQUALS, Collections.emptySet());

        Person p1 = new Person("Bob", 40);
        Person p2 = new Person("Bob", 41);
        assertFalse(DeepEquals.deepEquals(p1, p2, options));

        String diff = (String) options.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("field value mismatch"));
        assertTrue(diff.contains(".age"));
    }

    @Test
    public void testCustomEqualsIgnoredForSpecificClass() {
        // Ignore only Person.class
        options.put(IGNORE_CUSTOM_EQUALS, Set.of(Person.class));

        Person p1 = new Person("Carol", 25);
        Person p2 = new Person("Carol", 26);
        assertFalse(DeepEquals.deepEquals(p1, p2, options));

        String diff = (String) options.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("field value mismatch"));
        assertTrue(diff.contains(".age"));
    }

    /* ----------------------------------------------------------------------
     *  Collection handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testOrderedCollectionEquality() {
        List<Integer> a = List.of(1, 2, 3);
        List<Integer> b = List.of(1, 2, 3);
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testOrderedCollectionElementMismatch() {
        List<Integer> a = List.of(1, 2, 3);
        List<Integer> b = List.of(1, 9, 3);
        assertFalse(DeepEquals.deepEquals(a, b, options));

        String diff = (String) options.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("collection element mismatch"));
        assertTrue(diff.contains("[1]")); // index 1 where the mismatch occurs
    }

    @Test
    public void testUnorderedCollectionEquality() {
        Set<String> a = new HashSet<>(Arrays.asList("x", "y"));
        Set<String> b = new HashSet<>(Arrays.asList("y", "x"));
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testUnorderedCollectionSizeMismatch() {
        Set<String> a = Set.of("a", "b");
        Set<String> b = Set.of("a");
        assertFalse(DeepEquals.deepEquals(a, b, options));

        String diff = (String) options.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("collection size mismatch"));
    }

    /* ----------------------------------------------------------------------
     *  Map handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testMapEquality() {
        Map<String, Integer> a = Map.of("one", 1, "two", 2);
        Map<String, Integer> b = new HashMap<>();
        b.put("two", 2);
        b.put("one", 1);
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testMapValueMismatch() {
        Map<String, Integer> a = Map.of("key", 10);
        Map<String, Integer> b = Map.of("key", 20);
        assertFalse(DeepEquals.deepEquals(a, b, options));

        String diff = (String) options.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("map value mismatch"));
        assertTrue(diff.contains("Expected: 10"));
        assertTrue(diff.contains("Found: 20"));
    }

    @Test
    public void testMapMissingKey() {
        Map<String, Integer> a = Map.of("a", 1, "b", 2);
        Map<String, Integer> b = Map.of("a", 1);
        assertFalse(DeepEquals.deepEquals(a, b, options));

        String diff = (String) options.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("missing map key"));
        assertTrue(diff.contains("\"b\""));
    }

    /* ----------------------------------------------------------------------
     *  Array handling
     * ---------------------------------------------------------------------- */

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

        String diff = (String) options.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array length mismatch"));
    }

    @Test
    public void testArrayElementMismatch() {
        String[] a = {"x", "y", "z"};
        String[] b = {"x", "y", "Z"};
        assertFalse(DeepEquals.deepEquals(a, b, options));

        String diff = (String) options.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("array element mismatch"));
        assertTrue(diff.contains("[2]"));
    }

    /* ----------------------------------------------------------------------
     *  AtomicBoolean handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testAtomicBooleanEquality() {
        assertTrue(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(true)));
        assertFalse(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(false), options));

        String diff = (String) options.get(DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("value mismatch"));
    }

    /* ----------------------------------------------------------------------
     *  Circular reference handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testCircularReferenceEquality() {
        Node n1 = new Node("A");
        Node n2 = new Node("B");
        n1.next = n2;
        n2.next = n1; // circular link

        Node m1 = new Node("A");
        Node m2 = new Node("B");
        m1.next = m2;
        m2.next = m1;

        assertTrue(DeepEquals.deepEquals(n1, m1));
    }

    /* ----------------------------------------------------------------------
     *  Deep hash code consistency
     * ---------------------------------------------------------------------- */

    @Test
    public void testDeepHashCodeConsistency() {
        List<String> list1 = new ArrayList<>(List.of("a", "b"));
        List<String> list2 = new LinkedList<>(List.of("a", "b"));
        assertEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list2));

        // Different content => different hash (not guaranteed but highly likely)
        List<String> list3 = List.of("a", "c");
        assertNotEquals(DeepEquals.deepHashCode(list1), DeepEquals.deepHashCode(list3));
    }

    /* ----------------------------------------------------------------------
     *  Reflection helper methods
     * ---------------------------------------------------------------------- */

    @Test
    public void testHasCustomEqualsAndHashCode() {
        // Person overrides both equals and hashCode
        assertTrue(DeepEquals.hasCustomEquals(Person.class));
        assertTrue(DeepEquals.hasCustomHashCode(Person.class));

        // Simple class without overrides
        assertFalse(DeepEquals.hasCustomEquals(String.class));
        assertFalse(DeepEquals.hasCustomHashCode(String.class));
    }

    /* ----------------------------------------------------------------------
     *  Numeric comparison edge cases (floating point tolerance)
     * ---------------------------------------------------------------------- */

    @Test
    public void testFloatingPointNearlyEqual() {
        double a = 0.1 + 0.2; // 0.30000000000000004
        double b = 0.3;
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    public void testBigDecimalComparison() {
        BigDecimal bd1 = new BigDecimal("12345678901234567890");
        BigDecimal bd2 = new BigDecimal("12345678901234567890.0");
        assertTrue(DeepEquals.deepEquals(bd1, bd2));
    }

    /* ----------------------------------------------------------------------
     *  Verify breadcrumb (diff) formatting for a nested structure
     * ---------------------------------------------------------------------- */

    @Test
    public void testBreadcrumbFormattingForNestedStructure() {
        // Build a nested object: Person with a list of pets (strings)
        class PetOwner {
            Person owner;
            List<String> pets;

            PetOwner(Person owner, List<String> pets) {
                this.owner = owner;
                this.pets = pets;
            }
        }

        Person p1 = new Person("Dave", 40);
        Person p2 = new Person("Dave", 41); // age differs

        PetOwner o1 = new PetOwner(p1, List.of("Rex", "Fido"));
        PetOwner o2 = new PetOwner(p2, List.of("Rex", "Fido"));

        assertFalse(DeepEquals.deepEquals(o1, o2, options));
        String diff = (String) options.get(DIFF);
        assertNotNull(diff);
        // The breadcrumb should point to the mismatching field ".owner.age"
        assertTrue(diff.contains(".owner.age"));
        assertTrue(diff.contains("field value mismatch"));
        assertTrue(diff.contains("Expected: 40"));
        assertTrue(diff.contains("Found: 41"));
    }
}
