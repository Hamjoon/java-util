package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DeepEquals_deepEquals_Test {

    /**
     * Options map used by tests that need to inspect the diff output.
     */
    private Map<String, Object> options;

    /**
     * Class that deliberately implements a misleading {@code equals}.
     */
    private static class WithCustomEquals {

        private final int id;

        WithCustomEquals(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            // Intentionally wrong – always returns false.
            return false;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    /**
     * Simple POJO used for field‑by‑field comparison tests.
     */
    private static class Person {

        private final String name;

        private final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    /**
     * Point with a proper equals implementation (compares both fields).
     */
    private static class Point {

        private final int x;

        private final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Point))
                return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /**
     * Person that overrides equals to compare only the name (ignores age).
     */
    private static class PersonWithCustomEquals {

        private final String name;

        private final int age;

        PersonWithCustomEquals(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof PersonWithCustomEquals))
                return false;
            PersonWithCustomEquals p = (PersonWithCustomEquals) o;
            // only name matters
            return Objects.equals(name, p.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    /**
     * Simple node class used to build cyclic graphs.
     */
    private static class Node {

        private final int id;

        private Node next;

        private Node(int id) {
            this.id = id;
        }

        /**
         * Creates a cycle of the requested length (length >= 1).
         */
        static Node cycleOfLength(int length) {
            if (length < 1)
                throw new IllegalArgumentException("length must be >= 1");
            Node first = new Node(0);
            Node prev = first;
            for (int i = 1; i < length; i++) {
                Node cur = new Node(i);
                prev.next = cur;
                prev = cur;
            }
            // close the cycle
            prev.next = first;
            return first;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh options map for each test so that state does not leak.
        options = new HashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to aid GC and to guarantee a clean state for the next test.
        if (options != null) {
            options.clear();
        }
    }

    private static Stream<Arguments> nullHandlingCases() {
        Object obj = new Object();
        return Stream.of(Arguments.of(null, null, true, "Both arguments are null – deepEquals should consider them equal."), Arguments.of(null, new Integer(5), false, "One argument is null and the other is non‑null – should return false."), Arguments.of(obj, obj, true, "Both parameters reference the same object – shortcut equality should return true."));
    }

    private static Stream<Arguments> primitiveWrapperCases() {
        return Stream.of(Arguments.of(Integer.valueOf(5), Integer.valueOf(5), true, "Two Integer objects with the same value are considered deeply equal."), Arguments.of(Integer.valueOf(5), Long.valueOf(5L), false, "Different wrapper types (Integer vs Long) with the same numeric value are not considered equal without special options."), Arguments.of(Double.valueOf(5.0), Integer.valueOf(5), false, "A Double and an Integer with the same numeric value are not equal by default."));
    }

    private static Stream<Arguments> stringToNumberCases() {
        return Stream.of(Arguments.of("5", Integer.valueOf(5), true, "When the option is enabled, a numeric string matches an Integer with the same value."), Arguments.of("5.0", Double.valueOf(5.0), true, "A decimal string matches a Double when the option is turned on."), Arguments.of("5a", Integer.valueOf(5), false, "A non‑numeric string does not match a numeric wrapper even with the option enabled."));
    }

    private static Stream<Arguments> listEqualityCases() {
        return Stream.of(Arguments.of(Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 3), true, "Two ArrayLists with identical elements in the same order are deeply equal."), Arguments.of(Arrays.asList(1, 2, 3), Arrays.asList(3, 2, 1), false, "Lists with the same elements but different ordering are not equal."), Arguments.of(Arrays.asList(null, "a"), Arrays.asList(null, "a"), true, "Lists containing null elements are compared correctly."), Arguments.of(Arrays.asList(null, "a"), Arrays.asList("a", null), false, "Lists with the same elements in different positions are not equal."));
    }

    private static Stream<Arguments> setEqualityCases() {
        return Stream.of(Arguments.of(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(Arrays.asList(3, 2, 1)), true, "Two Sets with identical contents are equal regardless of insertion order."), Arguments.of(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(Arrays.asList(1, 2)), false, "Sets with different cardinalities are not equal."));
    }

    private static Stream<Arguments> mapEqualityCases() {
        return Stream.of(Arguments.of(Map.of("x", 1, "y", 2), Map.of("y", 2, "x", 1), true, "Maps with the same entries in different order are deeply equal."), Arguments.of(Map.of("x", 1, "y", 2), Map.of("x", 1, "y", 3), false, "Maps that differ in a value for a key are not equal."));
    }

    private static Stream<Arguments> arrayEqualityCases() {
        return Stream.of(Arguments.of(new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }, true, "Primitive int arrays with identical contents are equal."), Arguments.of(new int[] { 1, 2, 3 }, new int[] { 3, 2, 1 }, false, "Primitive int arrays with same elements in different order are not equal."), Arguments.of(new Object[] { Integer.valueOf(1), "two" }, new Object[] { Integer.valueOf(1), "two" }, true, "Object arrays containing mixed types are compared element‑wise."), Arguments.of(new Object[] { Integer.valueOf(1), "two" }, new Object[] { Integer.valueOf(1), "three" }, false, "Object arrays differ in the second element, leading to inequality."));
    }

    private static Stream<Arguments> customEqualsCases() {
        // Point uses a proper equals implementation (compares x & y)
        Point p1 = new Point(1, 2);
        Point p2 = new Point(1, 2);
        // PersonWithCustomEquals overrides equals to compare only name
        PersonWithCustomEquals pwc1 = new PersonWithCustomEquals("Alice", 30);
        PersonWithCustomEquals pwc2 = new PersonWithCustomEquals("Alice", 40);
        // options map for ignoring custom equals (empty set → ignore all)
        Map<String, Object> ignoreAll = new HashMap<>();
        ignoreAll.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of());
        // options map for NOT ignoring custom equals (null or absent)
        // pass null to use default behaviour
        Map<String, Object> honourCustom = null;
        return Stream.of(Arguments.of(p1, p2, honourCustom, true, "Point overrides equals to compare x and y; identical fields give true."), Arguments.of(pwc1, pwc2, honourCustom, true, "Person's custom equals only checks name; ages differ but equals returns true."), Arguments.of(pwc1, pwc2, ignoreAll, false, "With IGNORE_CUSTOM_EQUALS, deepEquals falls back to field comparison; differing ages cause false."));
    }

    private static Stream<Arguments> cyclicGraphCases() {
        return Stream.of(Arguments.of(Node.cycleOfLength(2), Node.cycleOfLength(2), true, "Two separate but structurally identical cyclic graphs are considered equal without infinite recursion."), Arguments.of(Node.cycleOfLength(2), Node.cycleOfLength(3), false, "Cyclic graphs with different lengths are not equal."));
    }

    private static Stream<Arguments> specialImmutableCases() {
        return Stream.of(Arguments.of(UUID.fromString("123e4567-e89b-12d3-a456-426655440000"), UUID.fromString("123e4567-e89b-12d3-a456-426655440000"), true, "Two identical UUID instances are equal."), Arguments.of(URI.create("http://example.com"), URI.create("http://example.com"), true, "Identical URIs compare equal."), Arguments.of(new Date(1620000000000L), new Date(1620000000000L), true, "Two Date objects representing the same instant are equal."), Arguments.of(new Date(1620000000000L), new Date(1620000010000L), false, "Dates with different timestamps are not equal."), Arguments.of(new AtomicInteger(5), new AtomicInteger(5), true, "AtomicInteger instances with the same value are equal."), Arguments.of(new AtomicBoolean(true), new AtomicBoolean(false), false, "AtomicBoolean values differ, resulting in inequality."));
    }

    @Test
    void testBasicNumericCompare() {
        // Simple numeric comparison – the overloaded method without options.
        assertTrue(DeepEquals.deepEquals(1.0d, 1));
    }

    @Test
    void testSameObjectEquals() {
        // The same reference must be considered equal.
        java.util.Date date = new java.util.Date();
        assertTrue(DeepEquals.deepEquals(date, date));
    }

    @Test
    void testEqualsWithNull() {
        java.util.Date date = new java.util.Date();
        assertFalse(DeepEquals.deepEquals(null, date));
        assertFalse(DeepEquals.deepEquals(date, null));
    }

    @Test
    void testDeepEqualsWithOptionsAndDiff() {
        // Two different Person objects – we expect a diff entry.
        Person p1 = new Person("Jim Bob", 27);
        Person p2 = new Person("Jim Bob", 34);
        boolean result = DeepEquals.deepEquals(p1, p2, options);
        assertFalse(result, "Objects with different field values should not be equal");
        // The diff description must be present in the options map.
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff, "Diff description should be stored under the key DeepEquals.DIFF");
        // The diff should contain the field name that differs.
        assertTrue(diff.contains(".age"), "Diff should mention the differing field 'age'");
    }

    @Test
    void testIgnoreCustomEqualsOption() {
        // Class with a custom equals that always returns false.
        WithCustomEquals a = new WithCustomEquals(1);
        WithCustomEquals b = new WithCustomEquals(1);
        // Without any options DeepEquals will fall back to field‑by‑field comparison,
        // so the objects are considered equal.
        assertTrue(DeepEquals.deepEquals(a, b));
        // When we explicitly ask DeepEquals to honour custom equals, the result must be false.
        // Empty set → ignore *all* custom equals implementations.
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of());
        assertFalse(DeepEquals.deepEquals(a, b, options));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nullHandlingCases")
    void testNullHandling(Object a, Object b, boolean expected, String description) {
        assertEquals(expected, DeepEquals.deepEquals(a, b), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("primitiveWrapperCases")
    void testPrimitiveWrapperEquality(Object a, Object b, boolean expected, String description) {
        assertEquals(expected, DeepEquals.deepEquals(a, b), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("stringToNumberCases")
    void testStringToNumberMatching(Object a, Object b, boolean expected, String description) {
        // Enable the option for this test
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertEquals(expected, DeepEquals.deepEquals(a, b, opts), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("listEqualityCases")
    void testListEquality(List<?> a, List<?> b, boolean expected, String description) {
        assertEquals(expected, DeepEquals.deepEquals(a, b), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("setEqualityCases")
    void testSetEquality(Set<?> a, Set<?> b, boolean expected, String description) {
        assertEquals(expected, DeepEquals.deepEquals(a, b), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("mapEqualityCases")
    void testMapEquality(Map<?, ?> a, Map<?, ?> b, boolean expected, String description) {
        assertEquals(expected, DeepEquals.deepEquals(a, b), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("arrayEqualityCases")
    void testArrayEquality(Object a, Object b, boolean expected, String description) {
        assertEquals(expected, DeepEquals.deepEquals(a, b), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("customEqualsCases")
    void testCustomEqualsHandling(Object a, Object b, Map<String, Object> opts, boolean expected, String description) {
        boolean result = (opts == null) ? DeepEquals.deepEquals(a, b) : DeepEquals.deepEquals(a, b, opts);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("cyclicGraphCases")
    void testCyclicGraphs(Node a, Node b, boolean expected, String description) {
        assertEquals(expected, DeepEquals.deepEquals(a, b), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("specialImmutableCases")
    void testSpecialImmutableTypes(Object a, Object b, boolean expected, String description) {
        assertEquals(expected, DeepEquals.deepEquals(a, b), description);
    }
}
