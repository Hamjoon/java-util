package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DeepEquals_deepEquals_Test {

    /**
     * Options map used for tests that need to pass configuration to {@code DeepEquals.deepEquals}.
     */
    private Map<String, Object> options;

    private static class WithCustomEquals {

        int id;

        WithCustomEquals(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof WithCustomEquals))
                return false;
            return id == ((WithCustomEquals) o).id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    /**
     * Simple node class that can form a cycle.
     */
    private static class Node {

        String name;

        Node next;

        Node(String name) {
            this.name = name;
        }

        /**
         * Helper that creates a single‑node self‑referencing cycle.
         */
        static Node createCyclic(int payload) {
            Node n = new Node(String.valueOf(payload));
            // self‑cycle
            n.next = n;
            return n;
        }
    }

    private static class Person {

        String name;

        int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        // Custom equals compares only the name.
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Person))
                return false;
            Person p = (Person) o;
            return Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    private static class SimplePojo {

        int id;

        String name;

        SimplePojo(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh options map for each test so that a test cannot affect another.
        options = new HashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to help GC and avoid accidental reuse.
        options.clear();
    }

    static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(Arguments.of("Both arguments are null – deepEquals should treat them as equal.", null, null, true), Arguments.of("Only the first argument is null – result must be false.", null, new Object(), false), Arguments.of("Only the second argument is null – result must be false.", new Object(), null, false));
    }

    @Test
    void deepEquals_numbersWithDifferentPrimitiveTypes_areEqual() {
        // int vs long – values are the same, DeepEquals should treat them as equal.
        assertTrue(DeepEquals.deepEquals(42, 42L));
        // double vs int – same numeric value.
        assertTrue(DeepEquals.deepEquals(3.0, 3));
    }

    @Test
    void deepEquals_stringAndNumber_withoutOption_areNotEqual() {
        // By default strings are not converted to numbers.
        assertFalse(DeepEquals.deepEquals("123", 123));
    }

    @Test
    void deepEquals_stringAndNumber_withOption_areEqual() {
        // Enable the option that allows strings to be compared to numbers.
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("123", 123, options));
        assertTrue(DeepEquals.deepEquals("-45.67", -45.67, options));
    }

    @Test
    void deepEquals_listsWithSameElements_areEqual() {
        List<Object> a = Arrays.asList(1, "two", 3.0);
        List<Object> b = Arrays.asList(1, "two", 3.0);
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_listsWithDifferentOrder_areNotEqual() {
        List<Object> a = Arrays.asList(1, 2, 3);
        List<Object> b = Arrays.asList(3, 2, 1);
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_mapsWithSameEntries_areEqual() {
        Map<String, Object> a = new HashMap<>();
        a.put("one", 1);
        a.put("two", "2");
        Map<String, Object> b = new HashMap<>();
        b.put("two", "2");
        b.put("one", 1);
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_mapsWithDifferentValues_areNotEqual() {
        Map<String, Object> a = new HashMap<>();
        a.put("key", 10);
        Map<String, Object> b = new HashMap<>();
        b.put("key", 20);
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_cyclicGraphsAreHandledCorrectly() {
        // Build first cyclic graph: A -> B -> C -> A
        Node a1 = new Node("A");
        Node b1 = new Node("B");
        Node c1 = new Node("C");
        a1.next = b1;
        b1.next = c1;
        // cycle
        c1.next = a1;
        // Build second cyclic graph with same data: A' -> B' -> C' -> A'
        Node a2 = new Node("A");
        Node b2 = new Node("B");
        Node c2 = new Node("C");
        a2.next = b2;
        b2.next = c2;
        // cycle
        c2.next = a2;
        assertTrue(DeepEquals.deepEquals(a1, a2));
    }

    @Test
    void deepEquals_cyclicGraphsWithDifferentData_areNotEqual() {
        // First graph
        Node a1 = new Node("A");
        Node b1 = new Node("B");
        a1.next = b1;
        // cycle
        b1.next = a1;
        // Second graph – same shape but different field value
        Node a2 = new Node("A-modified");
        Node b2 = new Node("B");
        a2.next = b2;
        // cycle
        b2.next = a2;
        assertFalse(DeepEquals.deepEquals(a1, a2));
    }

    @Test
    void deepEquals_usesCustomEqualsWhenPresent() {
        WithCustomEquals x = new WithCustomEquals(5);
        WithCustomEquals y = new WithCustomEquals(5);
        WithCustomEquals z = new WithCustomEquals(7);
        assertTrue(DeepEquals.deepEquals(x, y));
        assertFalse(DeepEquals.deepEquals(x, z));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullHandlingProvider")
    void deepEquals_nullHandling(String description, Object a, Object b, Object expected) {
        if (expected instanceof Boolean) {
            boolean exp = (Boolean) expected;
            assertEquals(exp, DeepEquals.deepEquals(a, b));
        } else if (expected instanceof Class && ((Class<?>) expected).equals(NullPointerException.class)) {
            assertThrows(NullPointerException.class, () -> DeepEquals.deepEquals(a, b, null));
        }
    }

    @Test
    void deepEquals_referenceEquality_sameReference() {
        String obj = new String("test");
        assertTrue(DeepEquals.deepEquals(obj, obj));
    }

    @Test
    void deepEquals_primitiveWrapperEquality_sameValue() {
        assertTrue(DeepEquals.deepEquals(Integer.valueOf(5), Integer.valueOf(5)));
    }

    @Test
    void deepEquals_primitiveWrapperEquality_differentTypesWithoutOption() {
        assertFalse(DeepEquals.deepEquals(Integer.valueOf(5), Long.valueOf(5L)));
    }

    @Test
    void deepEquals_stringNumberMatching_enabledOption() {
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("5", Integer.valueOf(5), options));
    }

    @Test
    void deepEquals_stringNumberMatching_disabledOption() {
        assertFalse(DeepEquals.deepEquals("5", Integer.valueOf(5), options));
    }

    @Test
    void deepEquals_primitiveIntArray_equal() {
        int[] a = new int[] { 1, 2, 3 };
        int[] b = new int[] { 1, 2, 3 };
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_primitiveIntArray_notEqual() {
        int[] a = new int[] { 1, 2, 3 };
        int[] b = new int[] { 1, 2, 4 };
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_multiDimensionalIntArray_equal() {
        int[][] a = new int[][] { { 1, 2 }, { 3, 4 } };
        int[][] b = new int[][] { { 1, 2 }, { 3, 4 } };
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_listEquality_sameElementsSameOrder() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(1, 2, 3);
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_listVsSet_notEqual() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        Set<Integer> b = new HashSet<>(Arrays.asList(1, 2, 3));
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_listDifferentOrder_notEqual() {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<Integer> b = Arrays.asList(3, 2, 1);
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_mapEquality_identicalEntries() {
        Map<String, Integer> a = Map.of("x", 1, "y", 2);
        Map<String, Integer> b = Map.of("x", 1, "y", 2);
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_mapEquality_differentSize() {
        Map<String, Integer> a = Map.of("x", 1);
        Map<String, Integer> b = Map.of("x", 1, "y", 2);
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_customEquals_defaultUsesCustomEquals() {
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 40);
        assertTrue(DeepEquals.deepEquals(p1, p2));
    }

    @Test
    void deepEquals_customEquals_ignoreOptionFallsBackToFieldComparison() {
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 40);
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, true);
        assertFalse(DeepEquals.deepEquals(p1, p2, options));
    }

    @Test
    void deepEquals_cyclicGraphsIdenticalPayload() {
        Node a = Node.createCyclic(1);
        Node b = Node.createCyclic(1);
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_cyclicGraphsDifferentPayload() {
        Node a = Node.createCyclic(1);
        Node b = Node.createCyclic(2);
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_setEquality_sameElementsDifferentInsertionOrder() {
        Set<Integer> a = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> b = new HashSet<>(Arrays.asList(3, 2, 1));
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_setEquality_differentConcreteTypesWithNull() {
        Set<Object> a = new HashSet<>(Arrays.asList("a", null, "b"));
        Set<Object> b = new LinkedHashSet<>(Arrays.asList(null, "b", "a"));
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_dateEquality_sameInstant() {
        Date d1 = new Date(1_640_995_200_000L);
        Date d2 = new Date(1_640_995_200_000L);
        assertTrue(DeepEquals.deepEquals(d1, d2));
    }

    @Test
    void deepEquals_dateEquality_oneMillisecondDifference() {
        Date d1 = new Date(1_640_995_200_000L);
        Date d2 = new Date(1_640_995_200_001L);
        assertFalse(DeepEquals.deepEquals(d1, d2));
    }

    @Test
    void deepEquals_atomicBooleanEquality_trueTrue() {
        AtomicBoolean a = new AtomicBoolean(true);
        AtomicBoolean b = new AtomicBoolean(true);
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_atomicBooleanEquality_trueFalse() {
        AtomicBoolean a = new AtomicBoolean(true);
        AtomicBoolean b = new AtomicBoolean(false);
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_dequeEquality_sameImplementationSameOrder() {
        Deque<Integer> a = new ArrayDeque<>(Arrays.asList(1, 2, 3));
        Deque<Integer> b = new ArrayDeque<>(Arrays.asList(1, 2, 3));
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_dequeEquality_differentImplementationsSameOrder() {
        Deque<Integer> a = new ArrayDeque<>(Arrays.asList(1, 2, 3));
        Deque<Integer> b = new LinkedList<>(Arrays.asList(1, 2, 3));
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_simplePojo_equalFields() {
        SimplePojo a = new SimplePojo(10, "foo");
        SimplePojo b = new SimplePojo(10, "foo");
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_simplePojo_differentFields() {
        SimplePojo a = new SimplePojo(10, "foo");
        SimplePojo b = new SimplePojo(20, "foo");
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_objectArrayWithNulls_equal() {
        Object[] a = new Object[] { null, "x", Integer.valueOf(5) };
        Object[] b = new Object[] { null, "x", Integer.valueOf(5) };
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_objectArrayWithNulls_differentOrder_notEqual() {
        Object[] a = new Object[] { null, "x", Integer.valueOf(5) };
        Object[] b = new Object[] { "x", null, Integer.valueOf(5) };
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEquals_mixedNumeric_stringNumberOptionDisabled() {
        assertFalse(DeepEquals.deepEquals("5.0", Double.valueOf(5.0), options));
    }

    @Test
    void deepEquals_mixedNumeric_differentWrapperTypes() {
        assertFalse(DeepEquals.deepEquals(Integer.valueOf(5), Long.valueOf(5L), options));
    }

    @Test
    void deepEquals_customEquals_respectedByDefault() {
        Person p1 = new Person("Bob", 25);
        Person p2 = new Person("Bob", 30);
        // default: custom equals (name only) → true
        assertTrue(DeepEquals.deepEquals(p1, p2));
    }

    @Test
    void deepEquals_customEquals_ignoredWhenOptionSet() {
        Person p1 = new Person("Bob", 25);
        Person p2 = new Person("Bob", 30);
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, true);
        // option forces field‑by‑field → false because ages differ
        assertFalse(DeepEquals.deepEquals(p1, p2, options));
    }

    @Test
    void deepEquals_optionsMapWithIrrelevantKey_hasNoEffect() {
        options.put("someIrrelevantKey", true);
        assertTrue(DeepEquals.deepEquals(Integer.valueOf(7), Integer.valueOf(7), options));
    }

    @Test
    void deepEquals_nullOptionsMap_throwsNPE() {
        Integer a = Integer.valueOf(5);
        Integer b = Integer.valueOf(5);
        assertThrows(NullPointerException.class, () -> DeepEquals.deepEquals(a, b, null));
    }
}
