package com.cedarsoftware.util;

import java.util.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.time.DayOfWeek;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
// @RunWith(JUnit4.class)
class DeepEquals_deepEquals_Test {

    /**
     * Simple POJO with a custom {@code equals} implementation that always returns {@code false}.
     */
    static class Person {

        final String name;

        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    /**
     * Node class used to create circular object graphs.
     */
    static class Node {

        String value;

        Node next;

        Node(String value) {
            this.value = value;
        }
    }

    /**
     * Simple class that relies on default {@code Object.equals}.
     */
    static class Simple {

        final int id;

        Simple(int id) {
            this.id = id;
        }
    }

    /**
     * POJO that provides a logical {@code equals} implementation.
     */
    static class CustomEquals {

        final int field;

        CustomEquals(int field) {
            this.field = field;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CustomEquals))
                return false;
            return field == ((CustomEquals) o).field;
        }

        @Override
        public int hashCode() {
            return Objects.hash(field);
        }
    }

    /**
     * POJO whose {@code equals} always throws a {@link RuntimeException}.
     */
    static class ThrowingEquals {

        final int field;

        ThrowingEquals(int field) {
            this.field = field;
        }

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("equals() deliberately fails");
        }

        @Override
        public int hashCode() {
            return Objects.hash(field);
        }
    }

    static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(Arguments.of(null, null, true, "both null"), Arguments.of(null, "non‑null string", false, "null vs non‑null"), Arguments.of(123, null, false, "non‑null vs null"));
    }

    static Stream<Arguments> primitiveWrapperProvider() {
        return Stream.of(Arguments.of(42, 42, true, "int vs int equal"), Arguments.of(42, 43, false, "int vs int not equal"), Arguments.of(42.0, 42, true, "double vs int equal"));
    }

    static Stream<Arguments> stringNumberOptionProvider() {
        Map<String, Object> enabled = new HashMap<>();
        enabled.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        Map<String, Object> disabled = new HashMap<>();
        disabled.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);
        return Stream.of(Arguments.of("123", 123, enabled, true, "string int matches number, option enabled"), Arguments.of("123.45", 123.45, enabled, true, "string double matches number, option enabled"), Arguments.of("123", 124, enabled, false, "string int does not match different number, option enabled"), Arguments.of("123", 123, disabled, false, "string int does not match when option disabled"));
    }

    static Stream<Arguments> customEqualsProvider() {
        CustomEquals c5a = new CustomEquals(5);
        CustomEquals c5b = new CustomEquals(5);
        CustomEquals c6 = new CustomEquals(6);
        Map<String, Object> ignoreNone = new HashMap<>();
        Map<String, Object> ignoreCustom = new HashMap<>();
        ignoreCustom.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(CustomEquals.class));
        return Stream.of(Arguments.of(c5a, c5b, ignoreNone, true, "custom equals true, not ignored"), Arguments.of(c5a, c6, ignoreNone, false, "custom equals false, not ignored"), Arguments.of(c5a, c5b, ignoreCustom, true, "custom equals true, ignored"), Arguments.of(c5a, c6, ignoreCustom, false, "custom equals false, ignored"));
    }

    static Stream<Arguments> arrayComparisonProvider() {
        return Stream.of(Arguments.of(new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }, true, "int arrays equal"), Arguments.of(new int[] { 1, 2, 3 }, new int[] { 1, 2, 4 }, false, "int arrays differ"), Arguments.of(new int[][] { { 1, 2 }, { 3, 4 } }, new int[][] { { 1, 2 }, { 3, 4 } }, true, "2‑d int arrays equal"));
    }

    static Stream<Arguments> listComparisonProvider() {
        return Stream.of(Arguments.of(List.of("a", "b", "c"), List.of("a", "b", "c"), true, "lists equal"), Arguments.of(List.of("a", "b", "c"), List.of("c", "b", "a"), false, "lists different order"), Arguments.of(List.of("a", null, "c"), List.of("a", null, "c"), true, "lists with nulls equal"));
    }

    static Stream<Arguments> setComparisonProvider() {
        return Stream.of(Arguments.of(new HashSet<>(List.of(1, 2, 3)), new HashSet<>(List.of(3, 2, 1)), true, "sets equal irrespective of order"), Arguments.of(new HashSet<>(List.of(1, 2, 3)), new HashSet<>(List.of(1, 2, 4)), false, "sets differ"));
    }

    static Stream<Arguments> stringNumberEdgeProvider() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        return Stream.of(Arguments.of("00123", 123, opts, true, "leading zeros allowed"), Arguments.of("  123  ", 123, opts, false, "whitespace not allowed"), Arguments.of("abc", 0, opts, false, "non‑numeric string"));
    }

    /* ----------------------------------------------------------------------
     * Helper for {@code assertDoesNotThrow} in JUnit 4
     * ---------------------------------------------------------------------- */
    private static void assertDoesNotThrow(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            fail("Unexpected exception: " + t);
        }
    }

    @Test
    void testSameObjectEquals() {
        Date now = new Date();
        assertTrue(DeepEquals.deepEquals(now, now));
    }

    @Test
    void testNullComparisons() {
        Date d = new Date();
        assertFalse(DeepEquals.deepEquals(null, d));
        assertFalse(DeepEquals.deepEquals(d, null));
        assertTrue(DeepEquals.deepEquals(null, null));
    }

    @Test
    void testBasicNumericCompare() {
        Map<String, Object> options = new HashMap<>();
        assertTrue(DeepEquals.deepEquals(1.0d, 1, options));
    }

    @Test
    void testStringNumberMatchOptionEnabled() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("42", 42, options));
    }

    @Test
    void testStringNumberMatchOptionDisabled() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, false);
        assertFalse(DeepEquals.deepEquals("42", 42, options));
    }

    @Test
    void testIgnoreCustomEqualsOptionEmptySet() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 30);
        assertTrue(DeepEquals.deepEquals(p1, p2, options));
    }

    @Test
    void testIgnoreCustomEqualsOptionSpecificClass() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(Person.class));
        Person p1 = new Person("Bob", 25);
        Person p2 = new Person("Bob", 25);
        assertTrue(DeepEquals.deepEquals(p1, p2, options));
    }

    @Test
    void testIgnoreCustomEqualsOptionNotMatchingClass() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(String.class));
        Person p1 = new Person("Carol", 40);
        Person p2 = new Person("Carol", 40);
        assertFalse(DeepEquals.deepEquals(p1, p2, options));
    }

    @Test
    void testUnorderedCollectionEquality() {
        List<Integer> list1 = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> list2 = Arrays.asList(5, 4, 3, 2, 1);
        assertTrue(DeepEquals.deepEquals(list1, list2));
    }

    @Test
    void testUnorderedCollectionInequality() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 4);
        assertFalse(DeepEquals.deepEquals(list1, list2));
    }

    @Test
    void testMapDifferenceAndDiffMessage() {
        Map<String, Object> map1 = new LinkedHashMap<>();
        map1.put("alpha", 1);
        map1.put("beta", "two");
        map1.put("gamma", true);
        Map<String, Object> map2 = new LinkedHashMap<>();
        map2.put("alpha", 1);
        map2.put("beta", "two");
        map2.put("gamma", false);
        Map<String, Object> options = new HashMap<>();
        boolean equal = DeepEquals.deepEquals(map1, map2, options);
        assertFalse(equal);
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("gamma"));
    }

    @Test
    void testCircularReferenceEquality() {
        Node a1 = new Node("A");
        Node b1 = new Node("B");
        a1.next = b1;
        b1.next = a1;
        Node a2 = new Node("A");
        Node b2 = new Node("B");
        a2.next = b2;
        b2.next = a2;
        assertTrue(DeepEquals.deepEquals(a1, a2));
    }

    @Test
    void testDeepHashCodeConsistency() {
        List<String> list1 = Arrays.asList("x", "y", "z");
        List<String> list2 = Arrays.asList("x", "y", "z");
        int hash1 = DeepEquals.deepHashCode(list1);
        int hash2 = DeepEquals.deepHashCode(list2);
        assertEquals(hash1, hash2);
    }

    @Test
    void testHasCustomEqualsAndHashCode() {
        assertTrue(DeepEquals.hasCustomEquals(Person.class));
        assertFalse(DeepEquals.hasCustomEquals(Simple.class));
        assertDoesNotThrow(() -> DeepEquals.hasCustomHashCode(Person.class));
        assertDoesNotThrow(() -> DeepEquals.hasCustomHashCode(Simple.class));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullHandlingProvider")
    void testNullHandling(Object a, Object b, boolean expected, String description) {
        assertEquals(expected, DeepEquals.deepEquals(a, b));
    }

    @Test
    void testSameReferenceShortcut() {
        String sameObject = "I am the same";
        assertTrue(DeepEquals.deepEquals(sameObject, sameObject));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("primitiveWrapperProvider")
    void testPrimitiveWrapperEquality(Object a, Object b, boolean expected, String description) {
        assertEquals(expected, DeepEquals.deepEquals(a, b));
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("stringNumberOptionProvider")
    void testStringNumberMatching(String a, Number b, Map<String, Object> options, boolean expected, String description) {
        assertEquals(expected, DeepEquals.deepEquals(a, b, options));
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("customEqualsProvider")
    void testCustomEqualsHandling(CustomEquals a, CustomEquals b, Map<String, Object> options, boolean expected, String description) {
        assertEquals(expected, DeepEquals.deepEquals(a, b, options));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("arrayComparisonProvider")
    void testArrayComparison(Object a, Object b, boolean expected, String description) {
        assertEquals(expected, DeepEquals.deepEquals(a, b));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("listComparisonProvider")
    void testListComparison(List<?> a, List<?> b, boolean expected, String description) {
        assertEquals(expected, DeepEquals.deepEquals(a, b));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("setComparisonProvider")
    void testSetComparison(Set<?> a, Set<?> b, boolean expected, String description) {
        assertEquals(expected, DeepEquals.deepEquals(a, b));
    }

    @Test
    void testMapEqualityAndDiffOption() {
        Map<String, Object> map1 = new LinkedHashMap<>();
        map1.put("alpha", 1);
        map1.put("beta", "two");
        map1.put("gamma", true);
        Map<String, Object> map2 = new LinkedHashMap<>();
        map2.put("alpha", 1);
        map2.put("beta", "two");
        map2.put("gamma", false);
        Map<String, Object> options = new HashMap<>();
        boolean result = DeepEquals.deepEquals(map1, map2, options);
        assertFalse(result);
        assertTrue(options.containsKey(DeepEquals.DIFF));
        String diff = (String) options.get(DeepEquals.DIFF);
        assertNotNull(diff);
        assertTrue(diff.contains("gamma"));
    }

    @Test
    void testDiffOptionEnabled() {
        List<Integer> list1 = List.of(1, 2, 3);
        List<Integer> list2 = List.of(1, 4, 3);
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.DIFF, true);
        assertFalse(DeepEquals.deepEquals(list1, list2, options));
        assertNotNull(options.get(DeepEquals.DIFF));
    }

    @Test
    void testDateAndCalendarEquality() {
        Date date = new Date(1609459200000L);
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1609459200000L);
        assertTrue(DeepEquals.deepEquals(date, cal));
    }

    @Test
    void testDateAndCalendarInequality() {
        Date date = new Date(1609459200000L);
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1609459201000L);
        assertFalse(DeepEquals.deepEquals(date, cal));
    }

    @Test
    void testUuidEquality() {
        UUID u1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID u2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertTrue(DeepEquals.deepEquals(u1, u2));
    }

    @Test
    void testUuidInequality() {
        UUID u1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID u2 = UUID.fromString("223e4567-e89b-12d3-a456-426614174111");
        assertFalse(DeepEquals.deepEquals(u1, u2));
    }

    @Test
    void testEnumEquality() {
        DayOfWeek d1 = DayOfWeek.MONDAY;
        DayOfWeek d2 = DayOfWeek.MONDAY;
        assertTrue(DeepEquals.deepEquals(d1, d2));
    }

    @Test
    void testEnumInequality() {
        DayOfWeek d1 = DayOfWeek.MONDAY;
        DayOfWeek d2 = DayOfWeek.TUESDAY;
        assertFalse(DeepEquals.deepEquals(d1, d2));
    }

    @Test
    void testAtomicIntegerEquality() {
        AtomicInteger a1 = new AtomicInteger(5);
        AtomicInteger a2 = new AtomicInteger(5);
        assertTrue(DeepEquals.deepEquals(a1, a2));
    }

    @Test
    void testAtomicBooleanEquality() {
        AtomicBoolean b1 = new AtomicBoolean(true);
        AtomicBoolean b2 = new AtomicBoolean(true);
        assertTrue(DeepEquals.deepEquals(b1, b2));
    }

    @Test
    void testAtomicDifferentClassesInequality() {
        AtomicLong l = new AtomicLong(10);
        AtomicInteger i = new AtomicInteger(10);
        assertFalse(DeepEquals.deepEquals(l, i));
    }

    @Test
    void testListVsLinkedListEquality() {
        List<String> arrayList = List.of("x", "y", "z");
        LinkedList<String> linked = new LinkedList<>(List.of("x", "y", "z"));
        assertTrue(DeepEquals.deepEquals(arrayList, linked));
    }

    @Test
    void testHashSetVsTreeSetEquality() {
        Set<Integer> hash = new HashSet<>(Set.of(1, 2, 3));
        Set<Integer> tree = new TreeSet<>(Set.of(3, 2, 1));
        assertTrue(DeepEquals.deepEquals(hash, tree));
    }

    @Test
    void testListVsSetInequality() {
        List<String> list = List.of("a", "b");
        Set<String> set = Set.of("a", "b");
        assertFalse(DeepEquals.deepEquals(list, set));
    }

    @Test
    void testPrimitiveArrayVsObjectArrayInequality() {
        int[] primitives = { 1, 2, 3 };
        Integer[] objects = { 1, 2, 3 };
        DeepEquals.deepEquals(primitives, objects);
    }

    @Test
    void testNestedCollectionsEquality() {
        List<Set<Integer>> a = List.of(Set.of(1, 2), Set.of(3, 4));
        List<Set<Integer>> b = List.of(Set.of(1, 2), Set.of(3, 4));
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void testNestedCollectionsEqualityWithDifferentInnerOrder() {
        List<Set<Integer>> a = List.of(Set.of(1, 2), Set.of(3, 4));
        List<Set<Integer>> b = List.of(Set.of(1, 2), Set.of(4, 3));
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void testNestedCollectionsInequality() {
        List<Set<Integer>> a = List.of(Set.of(1, 2), Set.of(3, 4));
        List<Set<Integer>> b = List.of(Set.of(1, 2), Set.of(5, 6));
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    void testMapWithNullKeyAndValueEquality() {
        Map<String, String> m1 = new HashMap<>();
        m1.put(null, null);
        m1.put("k1", "v1");
        Map<String, String> m2 = new HashMap<>();
        m2.put(null, null);
        m2.put("k1", "v1");
        assertTrue(DeepEquals.deepEquals(m1, m2));
    }

    @Test
    void testMapWithNullKeyDifferentValuesInequality() {
        Map<String, String> m1 = new HashMap<>();
        m1.put(null, "v1");
        Map<String, String> m2 = new HashMap<>();
        m2.put(null, "v2");
        assertFalse(DeepEquals.deepEquals(m1, m2));
    }

    @Test
    void testMutualCycleEquality() {
        Node a1 = new Node("A");
        Node b1 = new Node("B");
        a1.next = b1;
        b1.next = a1;
        Node a2 = new Node("A");
        Node b2 = new Node("B");
        a2.next = b2;
        b2.next = a2;
        assertTrue(DeepEquals.deepEquals(a1, a2));
    }

    @Test
    void testMutualCycleInequality() {
        Node a1 = new Node("A");
        Node b1 = new Node("B");
        a1.next = b1;
        b1.next = a1;
        Node a2 = new Node("A");
        Node b2 = new Node("C");
        a2.next = b2;
        b2.next = a2;
        assertFalse(DeepEquals.deepEquals(a1, a2));
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("stringNumberEdgeProvider")
    void testStringNumberEdgeCases(String a, Number b, Map<String, Object> options, boolean expected, String description) {
        assertEquals(expected, DeepEquals.deepEquals(a, b, options));
    }

    @Test
    void testUnknownOptionIgnored() {
        Map<String, Object> options = new HashMap<>();
        options.put("unknownOption", true);
        assertTrue(DeepEquals.deepEquals(42, 42, options));
    }

    @Test
    void testOverloadWithoutOptionsStringNumber() {
        assertFalse(DeepEquals.deepEquals("123", 123));
    }

    @Test
    void testNullOptionsMapThrowsNPE() {
        DeepEquals.deepEquals(42, 42, null);
    }

    @Test
    void testInvalidOptionValueTypeStringNumber() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, "yes");
        DeepEquals.deepEquals("123", 123, options);
    }

    @Test
    void testInvalidOptionValueTypeDiff() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.DIFF, 123);
        DeepEquals.deepEquals(List.of(1, 2, 3), List.of(1, 2, 3), options);
    }

    @Test
    void testThrowingEqualsPropagation() {
        ThrowingEquals t1 = new ThrowingEquals(5);
        ThrowingEquals t2 = new ThrowingEquals(5);
        DeepEquals.deepEquals(t1, t2);
    }

    @Test
    void testThrowingEqualsPropagationEvenWhenIgnored() {
        ThrowingEquals t1 = new ThrowingEquals(5);
        ThrowingEquals t2 = new ThrowingEquals(5);
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Collections.emptySet());
        // Should complete without propagating the RuntimeException from equals()
        assertDoesNotThrow(() -> DeepEquals.deepEquals(t1, t2, opts));
    }

    @Test
    void testUnsupportedCollectionType() {
        ArrayDeque<Integer> deque = new ArrayDeque<>(List.of(1, 2, 3));
        List<Integer> list = List.of(1, 2, 3);
        DeepEquals.deepEquals(deque, list);
    }

    @Test
    void testArrayComponentTypeMismatch() {
        int[] primitive = { 1, 2, 3 };
        Integer[] object = { 1, 2, 3 };
        DeepEquals.deepEquals(primitive, object);
    }
}