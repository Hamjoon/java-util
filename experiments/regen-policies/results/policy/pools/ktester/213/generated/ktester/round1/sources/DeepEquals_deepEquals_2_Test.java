package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DeepEquals_deepEquals_2_Test {

    /**
     * Simple POJO used for normal deep‑equals comparisons.
     */
    static class Person {

        final String name;

        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
        // No custom equals – rely on DeepEquals field comparison
    }

    /**
     * A class with a deliberately broken equals implementation.
     */
    static class Foo {

        final int x;

        Foo(int x) {
            this.x = x;
        }

        /**
         * Always returns false to force DeepEquals to use field‑by‑field comparison when not ignored.
         */
        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x);
        }
    }

    /**
     * Class that correctly implements equals based on a single field.
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
     * Class whose equals method deliberately throws an exception.
     */
    static class BadEquals {

        final int field;

        BadEquals(int field) {
            this.field = field;
        }

        @Override
        public boolean equals(Object o) {
            throw new IllegalStateException("Bad equals invoked");
        }
    }

    /**
     * Map implementation that throws on any read access.
     */
    @SuppressWarnings("unchecked")
    static class ThrowingMap implements Map<String, Object> {

        @Override
        public int size() {
            throw new RuntimeException("size");
        }

        @Override
        public boolean isEmpty() {
            throw new RuntimeException("isEmpty");
        }

        @Override
        public boolean containsKey(Object key) {
            throw new RuntimeException("containsKey");
        }

        @Override
        public boolean containsValue(Object value) {
            throw new RuntimeException("containsValue");
        }

        @Override
        public Object get(Object key) {
            throw new RuntimeException("get");
        }

        @Override
        public Object put(String key, Object value) {
            throw new RuntimeException("put");
        }

        @Override
        public Object remove(Object key) {
            throw new RuntimeException("remove");
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            throw new RuntimeException("putAll");
        }

        @Override
        public void clear() {
            throw new RuntimeException("clear");
        }

        @Override
        public Set<String> keySet() {
            throw new RuntimeException("keySet");
        }

        @Override
        public Collection<Object> values() {
            throw new RuntimeException("values");
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            throw new RuntimeException("entrySet");
        }
    }

    /**
     * Node used for cyclic‑reference tests.
     */
    static class Node {

        String name;

        Node next;

        Node(String name) {
            this.name = name;
        }
    }

    static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(Arguments.of("Both null → true", null, null, new HashMap<>(), true), Arguments.of("One null, one non‑null → false", null, "non‑null string", new HashMap<>(), false));
    }

    static Stream<Arguments> primitiveWrapperProvider() {
        return Stream.of(Arguments.of("Equal Integer objects", 42, 42, true), Arguments.of("Different Integer objects", 42, 43, false));
    }

    static Stream<Arguments> stringNumberProvider() {
        Map<String, Object> enabled = new HashMap<>();
        enabled.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        Map<String, Object> disabled = new HashMap<>();
        return Stream.of(Arguments.of("String \"123\" ↔ Integer 123 (option enabled)", "123", 123, enabled, true), Arguments.of("String \"123.45\" ↔ Double 123.45 (option enabled)", "123.45", 123.45, enabled, true), Arguments.of("String \"123\" ↔ Integer 124 (option enabled)", "123", 124, enabled, false), Arguments.of("String \"123\" ↔ Integer 123 (option disabled)", "123", 123, disabled, false));
    }

    static Stream<Arguments> customEqualsProvider() {
        CustomEquals c5a = new CustomEquals(5);
        CustomEquals c5b = new CustomEquals(5);
        CustomEquals c6 = new CustomEquals(6);
        Map<String, Object> ignore = new HashMap<>();
        ignore.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(CustomEquals.class));
        return Stream.of(Arguments.of("Custom equals used (default) – equal fields", c5a, c5b, new HashMap<>(), true), Arguments.of("Custom equals used – different fields", c5a, c6, new HashMap<>(), false), Arguments.of("Ignore custom equals – structurally equal", c5a, c5b, ignore, true), Arguments.of("Ignore custom equals – structural difference", c5a, c6, ignore, false));
    }

    static Stream<Arguments> arrayComparisonProvider() {
        return Stream.of(Arguments.of("Identical int[]", new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }, true), Arguments.of("Different element", new int[] { 1, 2, 3 }, new int[] { 1, 2, 4 }, false), Arguments.of("Different length", new int[] { 1, 2 }, new int[] { 1, 2, 3 }, false));
    }

    static Stream<Arguments> collectionComparisonProvider() {
        return Stream.of(Arguments.of("ArrayList same order", List.of("a", "b", "c"), List.of("a", "b", "c"), true), Arguments.of("ArrayList different order", List.of("a", "b", "c"), List.of("c", "b", "a"), false), Arguments.of("HashSet same elements", new HashSet<>(List.of(1, 2, 3)), new HashSet<>(List.of(3, 2, 1)), true), Arguments.of("HashSet different element", new HashSet<>(List.of(1, 2, 3)), new HashSet<>(List.of(1, 2, 4)), false));
    }

    static Stream<Arguments> mapComparisonProvider() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("k1", "v1");
        map1.put("k2", 2);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("k2", 2);
        map2.put("k1", "v1");
        Map<String, Object> mapDiff = new HashMap<>();
        mapDiff.put("k1", "v1");
        Map<String, Object> mapDiff2 = new HashMap<>();
        mapDiff2.put("k1", "different");
        return Stream.of(Arguments.of("Equal HashMaps (order irrelevant)", map1, map2, true), Arguments.of("Same key, different value", mapDiff, mapDiff2, false));
    }

    /**
     * Simple numeric comparison – numbers of different types are considered equal.
     */
    @Test
    void testNumericComparison() {
        Map<String, Object> options = new HashMap<>();
        assertTrue(DeepEquals.deepEquals(1.0d, 1, options));
        assertTrue(DeepEquals.deepEquals(42L, 42, options));
    }

    /**
     * When the option ALLOW_STRINGS_TO_MATCH_NUMBERS is true, a numeric string matches a Number.
     */
    @Test
    void testStringNumberMatchEnabled() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertTrue(DeepEquals.deepEquals("10", 10, options));
        assertTrue(DeepEquals.deepEquals(3.14, "3.14", options));
    }

    /**
     * Without the option, a numeric string does NOT match a Number.
     */
    @Test
    void testStringNumberMatchDisabled() {
        // option not set → false by default
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals("10", 10, options));
        assertFalse(DeepEquals.deepEquals("3.14", 3.14, options));
    }

    /**
     * Custom equals should be ignored when the class is listed in IGNORE_CUSTOM_EQUALS.
     */
    @Test
    void testIgnoreCustomEquals() {
        Foo f1 = new Foo(5);
        Foo f2 = new Foo(5);
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Set.of(Foo.class));
        assertTrue(DeepEquals.deepEquals(f1, f2, options));
    }

    /**
     * When custom equals is NOT ignored, the overridden equals method determines the result.
     */
    @Test
    void testCustomEqualsNotIgnored() {
        Foo f1 = new Foo(5);
        Foo f2 = new Foo(5);
        // no ignore set
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(f1, f2, options));
    }

    /**
     * When objects differ, the options map receives a non‑null diff description.
     */
    @Test
    void testDiffOutputOnMismatch() {
        Foo f1 = new Foo(5);
        Foo f2 = new Foo(6);
        Map<String, Object> options = new HashMap<>();
        boolean result = DeepEquals.deepEquals(f1, f2, options);
        assertFalse(result);
        assertNotNull(options.get(DeepEquals.DIFF), "Diff description should be present when objects differ");
        String diff = (String) options.get(DeepEquals.DIFF);
        assertTrue(diff.contains("field value mismatch") || diff.contains("VALUE_MISMATCH"), "Diff description should contain a hint about the mismatch");
    }

    /**
     * Simple POJO without a custom equals – deepEquals should compare fields.
     */
    @Test
    void testPlainObjectComparison() {
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Alice", 30);
        assertTrue(DeepEquals.deepEquals(p1, p2));
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullHandlingProvider")
    void testNullHandling(String description, Object a, Object b, Map<String, ?> options, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            assertThrows((Class<? extends Throwable>) expected, () -> DeepEquals.deepEquals(a, b, (Map<String, Object>) options));
        } else {
            boolean result = DeepEquals.deepEquals(a, b, (Map<String, Object>) options);
            assertEquals(expected, result, description);
        }
    }

    // -------------------------------------------------------------------------
    @Test
    void testIdentityShortcut() {
        Object same = new Object();
        Map<String, Object> options = new HashMap<>();
        assertTrue(DeepEquals.deepEquals(same, same, options));
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("primitiveWrapperProvider")
    void testPrimitiveWrapperEquality(String description, Object a, Object b, boolean expected) {
        assertEquals(expected, DeepEquals.deepEquals(a, b, new HashMap<>()), description);
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringNumberProvider")
    void testStringNumberMatching(String description, Object a, Object b, Map<String, Object> options, boolean expected) {
        assertEquals(expected, DeepEquals.deepEquals(a, b, options), description);
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("customEqualsProvider")
    void testCustomEqualsHandling(String description, Object a, Object b, Map<String, Object> options, boolean expected) {
        assertEquals(expected, DeepEquals.deepEquals(a, b, options), description);
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("arrayComparisonProvider")
    void testArrayComparison(String description, int[] a, int[] b, boolean expected) {
        assertEquals(expected, DeepEquals.deepEquals(a, b, new HashMap<>()), description);
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("collectionComparisonProvider")
    void testCollectionComparison(String description, Collection<?> a, Collection<?> b, boolean expected) {
        assertEquals(expected, DeepEquals.deepEquals(a, b, new HashMap<>()), description);
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("mapComparisonProvider")
    void testMapComparison(String description, Map<?, ?> a, Map<?, ?> b, boolean expected) {
        assertEquals(expected, DeepEquals.deepEquals(a, b, new HashMap<>()), description);
    }

    // -------------------------------------------------------------------------
    @Test
    void testCyclicReferenceIdentical() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = b;
        // cycle
        b.next = a;
        Node a2 = new Node("A");
        Node b2 = new Node("B");
        a2.next = b2;
        // identical shape
        b2.next = a2;
        assertTrue(DeepEquals.deepEquals(a, a2, new HashMap<>()));
    }

    @Test
    void testCyclicReferenceDifferent() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = b;
        // cycle
        b.next = a;
        Node a2 = new Node("A");
        // different name
        Node b2 = new Node("C");
        a2.next = b2;
        b2.next = a2;
        assertFalse(DeepEquals.deepEquals(a, a2, new HashMap<>()));
    }

    // -------------------------------------------------------------------------
    @Test
    void testDiffOptionPopulated() {
        Map<String, Object> options = new HashMap<>();
        // request diff output
        options.put(DeepEquals.DIFF, true);
        boolean result = DeepEquals.deepEquals(new int[] { 1, 2 }, new int[] { 1, 3 }, options);
        assertFalse(result);
        assertNotNull(options.get(DeepEquals.DIFF), "Diff should be populated when mismatch occurs");
    }

    // -------------------------------------------------------------------------
    @Test
    void testNumericTypeEqualityWithoutConversion() {
        Map<String, Object> opts = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(42, 42.0, opts));
        Map<String, Object> opts2 = new HashMap<>();
        opts2.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);
        assertFalse(DeepEquals.deepEquals(42, 42.0, opts2));
    }

    // -------------------------------------------------------------------------
    @Test
    void testDateEquality() {
        Date d1 = new Date(1609459200000L);
        Date d2 = new Date(1609459200000L);
        Date d3 = new Date(1609459201000L);
        assertTrue(DeepEquals.deepEquals(d1, d2, new HashMap<>()));
        assertFalse(DeepEquals.deepEquals(d1, d3, new HashMap<>()));
    }

    // -------------------------------------------------------------------------
    @Test
    void testUuidEquality() {
        UUID u1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID u2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID u3 = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
        assertTrue(DeepEquals.deepEquals(u1, u2, new HashMap<>()));
        assertFalse(DeepEquals.deepEquals(u1, u3, new HashMap<>()));
    }

    // -------------------------------------------------------------------------
    @Test
    void testAtomicEquality() {
        AtomicInteger ai1 = new AtomicInteger(7);
        AtomicInteger ai2 = new AtomicInteger(7);
        AtomicLong al1 = new AtomicLong(1234567890123L);
        AtomicLong al2 = new AtomicLong(1234567890124L);
        assertTrue(DeepEquals.deepEquals(ai1, ai2, new HashMap<>()));
        assertFalse(DeepEquals.deepEquals(al1, al2, new HashMap<>()));
    }

    // -------------------------------------------------------------------------
    @Test
    void testBigDecimalEquality() {
        BigDecimal bd1 = new BigDecimal("1.0");
        BigDecimal bd2 = new BigDecimal("1.00");
        BigDecimal bd3 = new BigDecimal("2.0");
        assertTrue(DeepEquals.deepEquals(bd1, bd2, new HashMap<>()));
        assertFalse(DeepEquals.deepEquals(bd1, bd3, new HashMap<>()));
    }

    // -------------------------------------------------------------------------
    @Test
    void testEnumEquality() {
        assertTrue(DeepEquals.deepEquals(SampleEnum.ONE, SampleEnum.ONE, new HashMap<>()));
        assertFalse(DeepEquals.deepEquals(SampleEnum.ONE, SampleEnum.TWO, new HashMap<>()));
    }

    // -------------------------------------------------------------------------
    @Test
    void testNestedCollectionEquality() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("id", 1);
        map1.put("tags", List.of("red", "blue"));
        Map<String, Object> map2 = new HashMap<>();
        map2.put("id", 2);
        map2.put("tags", List.of("green"));
        List<Map<String, Object>> listA = List.of(map1, map2);
        List<Map<String, Object>> listB = List.of(new HashMap<>(map1), new HashMap<>(map2));
        assertTrue(DeepEquals.deepEquals(listA, listB, new HashMap<>()));
    }

    @Test
    void testNestedCollectionInequality() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("id", 1);
        map1.put("tags", List.of("red", "blue"));
        Map<String, Object> map2a = new HashMap<>();
        map2a.put("id", 2);
        map2a.put("tags", List.of("green"));
        Map<String, Object> map2b = new HashMap<>();
        map2b.put("id", 2);
        map2b.put("tags", List.of("yellow"));
        List<Map<String, Object>> listA = List.of(map1, map2a);
        List<Map<String, Object>> listB = List.of(map1, map2b);
        assertFalse(DeepEquals.deepEquals(listA, listB, new HashMap<>()));
    }

    // -------------------------------------------------------------------------
    @Test
    void testNullElementsInLists() {
        List<String> a = Arrays.asList("a", null, "c");
        List<String> b = Arrays.asList("a", null, "c");
        List<String> c = Arrays.asList("a", "b", "c");
        assertTrue(DeepEquals.deepEquals(a, b, new HashMap<>()));
        assertFalse(DeepEquals.deepEquals(a, c, new HashMap<>()));
    }

    // -------------------------------------------------------------------------
    @Test
    void testDifferentListImplementationsEqual() {
        List<Integer> arrayList = new ArrayList<>(List.of(1, 2, 3));
        List<Integer> linkedList = new LinkedList<>(List.of(1, 2, 3));
        List<Integer> linkedListDiff = new LinkedList<>(List.of(3, 2, 1));
        assertTrue(DeepEquals.deepEquals(arrayList, linkedList, new HashMap<>()));
        assertFalse(DeepEquals.deepEquals(arrayList, linkedListDiff, new HashMap<>()));
    }

    // -------------------------------------------------------------------------
    @Test
    void testMapWithNullKeyAndValue() {
        Map<String, Object> a = new HashMap<>();
        a.put("k1", null);
        a.put(null, "v2");
        Map<String, Object> b = new HashMap<>();
        b.put("k1", null);
        b.put(null, "v2");
        Map<String, Object> bDiff = new HashMap<>();
        bDiff.put("k1", "something");
        bDiff.put(null, "v2");
        assertTrue(DeepEquals.deepEquals(a, b, new HashMap<>()));
        assertFalse(DeepEquals.deepEquals(a, bDiff, new HashMap<>()));
    }

    // -------------------------------------------------------------------------
    @Test
    void testMultiDimensionalArrayEquality() {
        int[][] a = { { 1, 2 }, { 3, 4 } };
        int[][] b = { { 1, 2 }, { 3, 4 } };
        int[][] c = { { 1, 2 }, { 4, 3 } };
        assertTrue(DeepEquals.deepEquals(a, b, new HashMap<>()));
        assertFalse(DeepEquals.deepEquals(a, c, new HashMap<>()));
    }

    // -------------------------------------------------------------------------
    @Test
    void testCyclicSharedSubgraphIdentical() {
        Node shared = new Node("shared");
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = shared;
        b.next = shared;
        // cycle that includes shared node
        shared.next = a;
        Node shared2 = new Node("shared");
        Node a2 = new Node("A");
        Node b2 = new Node("B");
        a2.next = shared2;
        b2.next = shared2;
        shared2.next = a2;
        assertTrue(DeepEquals.deepEquals(a, a2, new HashMap<>()));
    }

    @Test
    void testCyclicSharedSubgraphDifferentLeaf() {
        Node shared = new Node("shared");
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = shared;
        b.next = shared;
        // cycle
        shared.next = a;
        // leaf differs
        Node shared2 = new Node("different");
        Node a2 = new Node("A");
        Node b2 = new Node("B");
        a2.next = shared2;
        b2.next = shared2;
        shared2.next = a2;
        assertFalse(DeepEquals.deepEquals(a, a2, new HashMap<>()));
    }

    // -------------------------------------------------------------------------
    @Test
    void testNullOptionsMap() {
        assertThrows(NullPointerException.class, () -> DeepEquals.deepEquals(123, 123, null));
    }

    // -------------------------------------------------------------------------
    @Test
    void testInvalidOptionValueType() {
        Map<String, Object> badIgnore = new HashMap<>();
        // should be Set<Class<?>>
        badIgnore.put(DeepEquals.IGNORE_CUSTOM_EQUALS, "true");
        Map<String, Object> badStringMatch = new HashMap<>();
        // should be Boolean
        badStringMatch.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, 1);
        Map<String, Object> badDiff = new HashMap<>();
        // should be Boolean
        badDiff.put(DeepEquals.DIFF, List.of());
        assertAll(() -> assertThrows(ClassCastException.class, () -> DeepEquals.deepEquals(new CustomEquals(5), new CustomEquals(5), badIgnore)), () -> assertThrows(ClassCastException.class, () -> DeepEquals.deepEquals("123", 123, badStringMatch)), () -> assertThrows(ClassCastException.class, () -> DeepEquals.deepEquals(new int[] { 1, 2 }, new int[] { 1, 3 }, badDiff)));
    }

    // -------------------------------------------------------------------------
    @Test
    void testCustomEqualsThrowsException() {
        BadEquals a = new BadEquals(10);
        BadEquals b = new BadEquals(10);
        assertThrows(IllegalStateException.class, () -> DeepEquals.deepEquals(a, b, new HashMap<>()));
    }

    // -------------------------------------------------------------------------
    @Test
    void testOptionsMapThrowsOnAccess() {
        Map<String, Object> throwing = new ThrowingMap();
        assertThrows(RuntimeException.class, () -> DeepEquals.deepEquals("foo", "foo", throwing));
    }

    // -------------------------------------------------------------------------
    @Test
    void testUnsupportedOptionKeyIgnored() {
        Map<String, Object> opts = new HashMap<>();
        opts.put("unknownOption", true);
        assertTrue(DeepEquals.deepEquals(1, 1, opts));
    }
}
