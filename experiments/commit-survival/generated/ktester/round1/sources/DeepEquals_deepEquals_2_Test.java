package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DeepEquals_deepEquals_2_Test {

    class Simple {

        int x;

        String y;

        Simple(int x, String y) {
            this.x = x;
            this.y = y;
        }
    }

    class Node {

        Node next;
    }

    /**
     * A class that deliberately implements a wrong {@code equals}.
     */
    static class CustomEquals {

        final int value;

        CustomEquals(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            // Intentionally incorrect: never consider two equal values equal
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    /**
     * Simple node used for graph / cycle tests.
     */
    static class Node {

        final int id;

        Node next;

        Node(int id) {
            this.id = id;
        }
    }

    static Stream<Arguments> basicNullAndIdentityProvider() {
        return Stream.of(Arguments.of("Both arguments are null; should be considered equal.", null, null, null, true), Arguments.of("One argument null, other non‑null; should be false.", null, "some string", null, false), Arguments.of("Two identical String objects; should be true.", "test", "test", null, true), Arguments.of("Different String instances with same content; should be true.", new String("test"), "test", null, true));
    }

    static Stream<Arguments> ignoreCustomEqualsProvider() {
        return Stream.of(Arguments.of("Both objects equal by fields despite broken equals, ignoreCustomEquals=true", new CustomEquals(10), new CustomEquals(10), Map.of(DeepEquals.IGNORE_CUSTOM_EQUALS, true), true), Arguments.of("Different field values, ignoreCustomEquals=true -> false", new CustomEquals(10), new CustomEquals(20), Map.of(DeepEquals.IGNORE_CUSTOM_EQUALS, true), false));
    }

    static Stream<Arguments> stringsCanMatchNumbersProvider() {
        return Stream.of(Arguments.of("Numeric string matches Long when option enabled", "123", 123L, Map.of(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true), true), Arguments.of("Numeric string matches BigDecimal when option enabled", "123.45", new BigDecimal("123.45"), Map.of(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true), true), Arguments.of("Non‑numeric string does not match number even with option", "123abc", 123, Map.of(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true), false));
    }

    static Stream<Arguments> arrayHandlingProvider() {
        return Stream.of(Arguments.of("Primitive int arrays equal", new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }, null, true), Arguments.of("String arrays differ in one element", new String[] { "a", "b" }, new String[] { "a", "c" }, null, false), Arguments.of("Nested object arrays equal", new Object[] { new int[] { 1, 2 }, "x" }, new Object[] { new int[] { 1, 2 }, "x" }, null, true));
    }

    static Stream<Arguments> listOrderingProvider() {
        return Stream.of(Arguments.of("Identical ordered lists", Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 3), null, true), Arguments.of("Same elements different order -> false", Arrays.asList(1, 2, 3), Arrays.asList(3, 2, 1), null, false));
    }

    static Stream<Arguments> setHandlingProvider() {
        return Stream.of(Arguments.of("Sets with same elements, different iteration order", new HashSet<>(Arrays.asList("x", "y", "z")), new LinkedHashSet<>(Arrays.asList("z", "y", "x")), null, true), Arguments.of("Sets differ by one element", Set.of(1, 2, 3), Set.of(1, 2, 4), null, false));
    }

    static Stream<Arguments> mapEqualityProvider() {
        return Stream.of(Arguments.of("Maps with identical entries, different internal order", Map.of("a", 1, "b", 2), new HashMap<>() {

            {
                put("b", 2);
                put("a", 1);
            }
        }, null, true), Arguments.of("Maps differ in value for key 'b'", Map.of("a", 1, "b", 2), Map.of("a", 1, "b", 3), null, false));
    }

    static Stream<Arguments> cyclicGraphProvider() {
        // Helper to build a self‑referencing node
        Node selfA = new Node(1);
        selfA.next = selfA;
        Node selfB = new Node(1);
        selfB.next = selfB;
        // Helper to build a two‑node chain (different leaf values)
        Node chainA = new Node(1);
        chainA.next = new Node(2);
        Node chainB = new Node(1);
        chainB.next = new Node(3);
        return Stream.of(Arguments.of("Two self‑referencing nodes with same id", selfA, selfB, null, true), Arguments.of("Two chains differing in leaf value", chainA, chainB, null, false));
    }

    static Stream<Arguments> numericCrossComparisonProvider() {
        return Stream.of(Arguments.of("Integer vs Double same numeric value", 123, 123.0, null, true), Arguments.of("BigDecimal vs Integer same value", new BigDecimal("123.00"), 123, null, true), Arguments.of("BigDecimal vs Integer different value", new BigDecimal("123.01"), 123, null, false));
    }

    static Stream<Arguments> atomicBooleanProvider() {
        return Stream.of(Arguments.of("Both true", new AtomicBoolean(true), new AtomicBoolean(true), null, true), Arguments.of("True vs false", new AtomicBoolean(true), new AtomicBoolean(false), null, false));
    }

    static Stream<Arguments> dateEqualityProvider() {
        return Stream.of(Arguments.of("Same instant", new Date(1620000000000L), new Date(1620000000000L), null, true), Arguments.of("Different instant", new Date(1620000000000L), new Date(1620000001000L), null, false));
    }

    static Stream<Arguments> invalidOptionValueProvider() {
        return Stream.of(Arguments.of("ignoreCustomEquals supplied as String", new CustomEquals(10), new CustomEquals(10), Map.of(DeepEquals.IGNORE_CUSTOM_EQUALS, "true"), ClassCastException.class), Arguments.of("stringsCanMatchNumbers supplied as Integer", "123", 123L, Map.of(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, 1), ClassCastException.class));
    }

    static Stream<Arguments> nullOptionValueProvider() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put(DeepEquals.IGNORE_CUSTOM_EQUALS, null);
        Map<String, Object> map2 = new HashMap<>();
        map2.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, null);
        return Stream.of(Arguments.of("null value for ignoreCustomEquals", new CustomEquals(5), new CustomEquals(5), map1, NullPointerException.class), Arguments.of("null value for stringsCanMatchNumbers", "456", 456, map2, NullPointerException.class));
    }

    @Test
    void deepEquals_withoutOptions_returnsTrueForEqualObjects() {
        // GIVEN
        Map<String, Object> options = new HashMap<>();
        // Simple POJOs without custom equals
        class Simple {

            int x;

            String y;

            Simple(int x, String y) {
                this.x = x;
                this.y = y;
            }
        }
        Simple a = new Simple(5, "test");
        Simple b = new Simple(5, "test");
        // WHEN
        boolean result = DeepEquals.deepEquals(a, b, options);
        // THEN
        assertTrue(result, "Objects with identical field values should be deep‑equal");
    }

    @Test
    void deepEquals_withStringNumberOption_allowsStringNumberMatch() {
        // GIVEN
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.TRUE);
        // A String that represents a number and a numeric type
        String numberAsString = "123.45";
        BigDecimal numberAsBigDecimal = new BigDecimal("123.45");
        // WHEN
        boolean result = DeepEquals.deepEquals(numberAsString, numberAsBigDecimal, options);
        // THEN
        assertTrue(result, "When ALLOW_STRINGS_TO_MATCH_NUMBERS is true, a numeric String should match the corresponding Number");
    }

    @Test
    void deepEquals_detectsCyclesWithoutStackOverflow() {
        // GIVEN
        Map<String, Object> options = new HashMap<>();
        // Create a simple cyclic structure: Node -> Node (self‑reference)
        class Node {

            Node next;
        }
        Node a = new Node();
        Node b = new Node();
        a.next = b;
        // cycle
        b.next = a;
        Node aCopy = new Node();
        Node bCopy = new Node();
        aCopy.next = bCopy;
        // same shape cycle
        bCopy.next = aCopy;
        // WHEN
        boolean result = DeepEquals.deepEquals(a, aCopy, options);
        // THEN
        assertTrue(result, "Cyclic object graphs with identical structure should be deep‑equal");
    }

    /* -------------------- basic null and identity handling ---------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicNullAndIdentityProvider")
    void deepEquals_basicNullAndIdentity(String description, Object a, Object b, Map<String, ?> options, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            assertThrows((Class<? extends Throwable>) expected, () -> DeepEquals.deepEquals(a, b, options), description);
        } else {
            boolean result = DeepEquals.deepEquals(a, b, options);
            assertEquals(expected, result, description);
        }
    }

    /* -------------------- ignoreCustomEquals option ---------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ignoreCustomEqualsProvider")
    void deepEquals_ignoreCustomEquals(String description, Object a, Object b, Map<String, ?> options, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            assertThrows((Class<? extends Throwable>) expected, () -> DeepEquals.deepEquals(a, b, options), description);
        } else {
            boolean result = DeepEquals.deepEquals(a, b, options);
            assertEquals(expected, result, description);
        }
    }

    /* -------------------- stringsCanMatchNumbers option ------------------ */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringsCanMatchNumbersProvider")
    void deepEquals_stringsCanMatchNumbers(String description, Object a, Object b, Map<String, ?> options, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            assertThrows((Class<? extends Throwable>) expected, () -> DeepEquals.deepEquals(a, b, options), description);
        } else {
            boolean result = DeepEquals.deepEquals(a, b, options);
            assertEquals(expected, result, description);
        }
    }

    /* -------------------- array handling --------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("arrayHandlingProvider")
    void deepEquals_arrayHandling(String description, Object a, Object b, Map<String, ?> options, Object expected) {
        boolean result = DeepEquals.deepEquals(a, b, options);
        assertEquals(expected, result, description);
    }

    /* -------------------- List ordering ---------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("listOrderingProvider")
    void deepEquals_listOrdering(String description, Object a, Object b, Map<String, ?> options, Object expected) {
        boolean result = DeepEquals.deepEquals(a, b, options);
        assertEquals(expected, result, description);
    }

    /* -------------------- Set handling (order‑independent) --------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("setHandlingProvider")
    void deepEquals_setHandling(String description, Object a, Object b, Map<String, ?> options, Object expected) {
        boolean result = DeepEquals.deepEquals(a, b, options);
        assertEquals(expected, result, description);
    }

    /* -------------------- Map equality ----------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("mapEqualityProvider")
    void deepEquals_mapEquality(String description, Object a, Object b, Map<String, ?> options, Object expected) {
        boolean result = DeepEquals.deepEquals(a, b, options);
        assertEquals(expected, result, description);
    }

    /* -------------------- cyclic object graphs --------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("cyclicGraphProvider")
    void deepEquals_cyclicGraphs(String description, Object a, Object b, Map<String, ?> options, Object expected) {
        boolean result = DeepEquals.deepEquals(a, b, options);
        assertEquals(expected, result, description);
    }

    /* -------------------- numeric cross‑comparison ----------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numericCrossComparisonProvider")
    void deepEquals_numericCrossComparison(String description, Object a, Object b, Map<String, ?> options, Object expected) {
        boolean result = DeepEquals.deepEquals(a, b, options);
        assertEquals(expected, result, description);
    }

    /* -------------------- AtomicBoolean handling ------------------------ */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("atomicBooleanProvider")
    void deepEquals_atomicBoolean(String description, Object a, Object b, Map<String, ?> options, Object expected) {
        boolean result = DeepEquals.deepEquals(a, b, options);
        assertEquals(expected, result, description);
    }

    /* -------------------- Date equality --------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("dateEqualityProvider")
    void deepEquals_dateEquality(String description, Object a, Object b, Map<String, ?> options, Object expected) {
        boolean result = DeepEquals.deepEquals(a, b, options);
        assertEquals(expected, result, description);
    }

    /* -------------------- invalid option value type ---------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidOptionValueProvider")
    void deepEquals_invalidOptionValue(String description, Object a, Object b, Map<String, ?> options, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DeepEquals.deepEquals(a, b, options), description);
    }

    /* -------------------- null option value ------------------------------ */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullOptionValueProvider")
    void deepEquals_nullOptionValue(String description, Object a, Object b, Map<String, ?> options, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DeepEquals.deepEquals(a, b, options), description);
    }

    /* -------------------- option map throws runtime exception ------------ */
    @Test
    void deepEquals_optionMapThrowsRuntimeException() {
        Map<String, Object> badMap = new AbstractMap<>() {

            @Override
            public Set<Entry<String, Object>> entrySet() {
                return Collections.emptySet();
            }

            @Override
            public Object get(Object key) {
                throw new IllegalStateException("Map failure on get");
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> DeepEquals.deepEquals("test", "test", badMap), "A map that throws on get() should propagate the exception");
        assertEquals("Map failure on get", ex.getMessage());
    }
}
