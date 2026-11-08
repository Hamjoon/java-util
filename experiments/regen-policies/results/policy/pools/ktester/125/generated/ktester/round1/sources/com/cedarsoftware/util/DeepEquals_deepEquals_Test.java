package com.cedarsoftware.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DeepEquals_deepEquals_Test {

    // options map passed to DeepEquals
    private Map<String, Object> options;

    // value for the IGNORE_CUSTOM_EQUALS option
    private Set<Class<?>> ignoreCustomEquals;

    /**
     * Minimal POJO used to demonstrate the {@code IGNORE_CUSTOM_EQUALS} option.
     */
    static class Person {

        private final String name;
        private final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        // Custom equals that only checks the name – deliberately different from field comparison.
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

    /**
     * Simple node class used to build tiny cyclic graphs for testing.
     */
    static class Node {

        final int value;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh options map for every test to guarantee isolation.
        options = new HashMap<>();
        // Initialise the set that will be used for the IGNORE_CUSTOM_EQUALS option.
        ignoreCustomEquals = new HashSet<>();
        // Populate the options map with the (currently empty) ignore‑set.
        // Individual tests can add entries or other options as required.
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, ignoreCustomEquals);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear mutable fields – helps the garbage collector and prevents
        // accidental cross‑test contamination if a test forgets to re‑initialize.
        if (options != null) {
            options.clear();
        }
        if (ignoreCustomEquals != null) {
            ignoreCustomEquals.clear();
        }
    }

    /**
     * Utility to create a self‑referencing node (a 1‑element cycle).
     */
    private static Node createCyclicNode(int value) {
        Node n = new Node(value);
        // length‑1 cycle
        n.next = n;
        return n;
    }

    private static Stream<Arguments> nullAndIdentityProvider() {
        Object obj = new Object();
        return Stream.of(
                Arguments.of(null, null, true,
                        "Both arguments are null – deepEquals should treat two nulls as equal."),
                Arguments.of(null, "non‑null string", false,
                        "One argument is null and the other is a non‑null object – should return false."),
                Arguments.of(new Object(), new Object(), false,
                        "Two distinct objects without overridden equals – deepEquals must fall back to identity and report not equal."),
                Arguments.of(obj, obj, true,
                        "Same reference passed twice – deepEquals should short‑circuit and return true."));
    }

    private static Stream<Arguments> primitiveWrapperProvider() {
        return Stream.of(
                Arguments.of(Integer.valueOf(5), new Integer(5), true,
                        "Two Integer wrapper objects with the same value should be considered deeply equal."),
                Arguments.of(Long.valueOf(5L), Integer.valueOf(5), false,
                        "Different numeric wrapper types with the same numeric value are not equal unless the string‑to‑number option is enabled."),
                Arguments.of(Double.valueOf(5.0), Float.valueOf(5.0f), false,
                        "Floating‑point wrapper types of different class are not considered equal."));
    }

    private static Stream<Arguments> stringNumberOptionProvider() {
        Map<String, Object> enabled = Map.of(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        Map<String, Object> disabled = Map.of();
        return Stream.of(
                Arguments.of("5", Integer.valueOf(5), enabled, true,
                        "When ALLOW_STRINGS_TO_MATCH_NUMBERS is true, a numeric string should match the corresponding Number wrapper."),
                Arguments.of("5.0", Double.valueOf(5.0), enabled, true,
                        "Floating‑point numeric string matches a Double when the option is enabled."),
                Arguments.of("5", Integer.valueOf(5), disabled, false,
                        "Without the option, a numeric string does NOT match a Number wrapper."));
    }

    private static Stream<Arguments> arrayProvider() {
        return Stream.of(
                Arguments.of(new int[]{1, 2, 3}, new int[]{1, 2, 3}, true,
                        "Two primitive int arrays with identical contents are deeply equal."),
                Arguments.of(new Integer[]{1, 2, 3}, new Integer[]{1, 2, 3}, true,
                        "Two object Integer arrays with identical contents are deeply equal."),
                Arguments.of(new int[]{1, 2, 3}, new int[]{3, 2, 1}, false,
                        "Arrays with same elements in different order are not equal."),
                Arguments.of(new Object[]{new int[]{1, 2}, "x"}, new Object[]{new int[]{1, 2}, "x"}, true,
                        "Nested arrays are compared recursively."));
    }

    private static Stream<Arguments> collectionProvider() {
        return Stream.of(
                Arguments.of(Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 3), true,
                        "Two List instances with identical order and elements are deeply equal."),
                Arguments.of(Arrays.asList(1, 2, 3), Arrays.asList(3, 2, 1), false,
                        "Lists with same elements but different order are not equal."),
                Arguments.of(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(Arrays.asList(3, 2, 1)), true,
                        "Two Set instances with identical elements (order irrelevant) are deeply equal."),
                Arguments.of(Collections.singletonList(null), Collections.singletonList(null), true,
                        "Lists containing a single null element are equal."));
    }

    private static Stream<Arguments> mapProvider() {
        return Stream.of(
                Arguments.of(Map.of("x", 1, "y", 2), Map.of("y", 2, "x", 1), true,
                        "Two maps with identical key/value pairs, irrespective of insertion order, are deeply equal."),
                Arguments.of(Map.of("x", 1, "y", 2), Map.of("x", 1, "y", 3), false,
                        "Maps differing by a value are not equal."),
                Arguments.of(Map.of("nested", List.of(1, 2)), Map.of("nested", List.of(1, 2)), true,
                        "Nested collection values inside maps are compared recursively."));
    }

    private static Stream<Arguments> customEqualsProvider() {
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 30);
        Map<String, Object> defaultOpts = Map.of(); // no ignore
        Map<String, Object> ignoreOpts = Map.of(DeepEquals.IGNORE_CUSTOM_EQUALS, true);
        return Stream.of(
                Arguments.of(p1, p2, defaultOpts, true,
                        "Two distinct Person objects that override equals to compare fields are considered equal when custom equals is allowed."),
                Arguments.of(p1, p2, ignoreOpts, false,
                        "When IGNORE_CUSTOM_EQUALS is true, deepEquals falls back to identity, so two different Person instances are not equal."));
    }

    private static Stream<Arguments> cyclicProvider() {
        return Stream.of(
                Arguments.of(createCyclicNode(1), createCyclicNode(1), true,
                        "Two structurally identical cyclic graphs should be reported equal without infinite recursion."),
                Arguments.of(createCyclicNode(1), createCyclicNode(2), false,
                        "Cyclic graphs that differ in a leaf value are not equal."));
    }

    private static Stream<Arguments> dateProvider() {
        return Stream.of(
                Arguments.of(new Date(1633024800000L), new Date(1633024800000L), true,
                        "Two Date instances representing the same instant are deeply equal."),
                Arguments.of(new Date(1633024800000L), new Date(1633024801000L), false,
                        "Dates with different timestamps are not equal."));
    }

    private static Stream<Arguments> atomicProvider() {
        return Stream.of(
                Arguments.of(new AtomicInteger(5), new AtomicInteger(5), true,
                        "Two AtomicInteger objects holding the same value are deeply equal."),
                Arguments.of(new AtomicBoolean(true), new AtomicBoolean(false), false,
                        "AtomicBoolean objects with different boolean values are not equal."));
    }

    @Test
    void deepEqualsSimpleSameInstance() {
        Date now = new Date();
        assertTrue(DeepEquals.deepEquals(now, now),
                "The same object reference must be considered deeply equal.");
    }

    @Test
    void deepEqualsWithNulls() {
        Object obj = new Object();
        assertFalse(DeepEquals.deepEquals(null, obj), "null vs non‑null must be false");
        assertFalse(DeepEquals.deepEquals(obj, null), "non‑null vs null must be false");
        assertTrue(DeepEquals.deepEquals(null, null), "null vs null must be true");
    }

    @Test
    void deepEqualsWithOptionsIgnoreCustomEquals() {
        Person p1 = new Person("Jim Bob", 27);
        Person p2 = new Person("Jim Bob", 34);
        // By default DeepEquals will use Person.equals(), so they are considered equal.
        assertTrue(p1.equals(p2), "Person's custom equals reports equality");
        assertTrue(DeepEquals.deepEquals(p1, p2), "DeepEquals without options uses custom equals");
        // Tell DeepEquals to ignore Person's custom equals implementation.
        ignoreCustomEquals.add(Person.class);
        boolean result = DeepEquals.deepEquals(p1, p2, options);
        assertFalse(result, "When Person.equals() is ignored, the differing fields cause inequality");
    }

    @Test
    void deepEqualsStringNumberMatchingEnabled() {
        String str = "10";
        Integer num = 10;
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.TRUE);
        boolean result = DeepEquals.deepEquals(str, num, options);
        assertTrue(result, "With ALLOW_STRINGS_TO_MATCH_NUMBERS enabled, \"10\" should match 10");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("nullAndIdentityProvider")
    void deepEqualsNullAndIdentity(Object a, Object b, boolean expected, String description) {
        boolean result = DeepEquals.deepEquals(a, b);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("primitiveWrapperProvider")
    void deepEqualsPrimitiveWrappers(Object a, Object b, boolean expected, String description) {
        boolean result = DeepEquals.deepEquals(a, b);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("stringNumberOptionProvider")
    void deepEqualsStringNumberOption(Object a, Object b, Map<String, ?> opts, boolean expected, String description) {
        boolean result = DeepEquals.deepEquals(a, b, opts);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("arrayProvider")
    void deepEqualsArrays(Object a, Object b, boolean expected, String description) {
        boolean result = DeepEquals.deepEquals(a, b);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("collectionProvider")
    void deepEqualsCollections(Object a, Object b, boolean expected, String description) {
        boolean result = DeepEquals.deepEquals(a, b);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("mapProvider")
    void deepEqualsMaps(Object a, Object b, boolean expected, String description) {
        boolean result = DeepEquals.deepEquals(a, b);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("customEqualsProvider")
    void deepEqualsCustomEquals(Object a, Object b, Map<String, ?> opts, boolean expected, String description) {
        boolean result = DeepEquals.deepEquals(a, b, opts);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("cyclicProvider")
    void deepEqualsCyclic(Node a, Node b, boolean expected, String description) {
        boolean result = DeepEquals.deepEquals(a, b);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("dateProvider")
    void deepEqualsDates(Date a, Date b, boolean expected, String description) {
        boolean result = DeepEquals.deepEquals(a, b);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("atomicProvider")
    void deepEqualsAtomic(Object a, Object b, boolean expected, String description) {
        boolean result = DeepEquals.deepEquals(a, b);
        assertEquals(expected, result, description);
    }

    // -----------------------------------------------------------------
    // Legacy JUnit4 style tests – converted to JUnit5 equivalents.
    // -----------------------------------------------------------------

    @Test
    void sameInstanceIsEqual() {
        Date now = new Date();
        assertTrue(DeepEquals.deepEquals(now, now));
    }

    @Test
    void nullHandling() {
        Object obj = new Object();
        assertFalse(DeepEquals.deepEquals(null, obj));
        assertFalse(DeepEquals.deepEquals(obj, null));
        assertTrue(DeepEquals.deepEquals(null, null));
    }

    @Test
    void nullAndIdentityCases() {
        Object shared = new Object();
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "non‑null string"));
        assertFalse(DeepEquals.deepEquals(new Object(), new Object()));
        assertTrue(DeepEquals.deepEquals(shared, shared));
    }

    @Test
    void primitiveWrapperCases() {
        assertTrue(DeepEquals.deepEquals(Integer.valueOf(5), new Integer(5)));
        assertFalse(DeepEquals.deepEquals(Long.valueOf(5L), Integer.valueOf(5)));
        assertFalse(DeepEquals.deepEquals(Double.valueOf(5.0), Float.valueOf(5.0f)));
    }

    @Test
    void stringNumberOptionCases() {
        Map<String, Object> enabled = Map.of(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("5", Integer.valueOf(5), enabled));
        assertTrue(DeepEquals.deepEquals("5.0", Double.valueOf(5.0), enabled));
        Map<String, Object> disabled = Map.of();
        assertFalse(DeepEquals.deepEquals("5", Integer.valueOf(5), disabled));
    }

    @Test
    void arrayCases() {
        assertTrue(DeepEquals.deepEquals(new int[]{1, 2, 3}, new int[]{1, 2, 3}));
        assertTrue(DeepEquals.deepEquals(new Integer[]{1, 2, 3}, new Integer[]{1, 2, 3}));
        assertFalse(DeepEquals.deepEquals(new int[]{1, 2, 3}, new int[]{3, 2, 1}));
        assertTrue(DeepEquals.deepEquals(new Object[]{new int[]{1, 2}, "x"}, new Object[]{new int[]{1, 2}, "x"}));
    }

    @Test
    void collectionCases() {
        assertTrue(DeepEquals.deepEquals(Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 3)));
        assertFalse(DeepEquals.deepEquals(Arrays.asList(1, 2, 3), Arrays.asList(3, 2, 1)));
        assertTrue(DeepEquals.deepEquals(new HashSet<>(Arrays.asList(1, 2, 3)),
                new HashSet<>(Arrays.asList(3, 2, 1))));
        assertTrue(DeepEquals.deepEquals(Collections.singletonList(null), Collections.singletonList(null)));
    }

    @Test
    void mapCases() {
        assertTrue(DeepEquals.deepEquals(Map.of("x", 1, "y", 2), Map.of("y", 2, "x", 1)));
        assertFalse(DeepEquals.deepEquals(Map.of("x", 1, "y", 2), Map.of("x", 1, "y", 3)));
        assertTrue(DeepEquals.deepEquals(Map.of("nested", List.of(1, 2)), Map.of("nested", List.of(1, 2))));
    }

    @Test
    void customEqualsOptionCases() {
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 30);
        // default – custom equals is honoured
        assertTrue(DeepEquals.deepEquals(p1, p2));
        // ignore custom equals – fall back to identity
        ignoreCustomEquals.add(Person.class);
        assertFalse(DeepEquals.deepEquals(p1, p2, options));
    }

    @Test
    void cyclicGraphCases() {
        assertTrue(DeepEquals.deepEquals(createCyclicNode(1), createCyclicNode(1)));
        assertFalse(DeepEquals.deepEquals(createCyclicNode(1), createCyclicNode(2)));
    }

    @Test
    void dateCases() {
        Date d1 = new Date(1633024800000L);
        Date d2 = new Date(1633024800000L);
        Date d3 = new Date(1633024801000L);
        assertTrue(DeepEquals.deepEquals(d1, d2));
        assertFalse(DeepEquals.deepEquals(d1, d3));
    }

    @Test
    void atomicCases() {
        assertTrue(DeepEquals.deepEquals(new AtomicInteger(5), new AtomicInteger(5)));
        assertFalse(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(false)));
    }
}