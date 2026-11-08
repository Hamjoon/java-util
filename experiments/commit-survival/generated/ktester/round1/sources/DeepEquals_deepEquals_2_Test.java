package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

class DeepEquals_deepEquals_2_Test {

    /**
     * Simple POJO with a custom {@code equals} that deliberately returns {@code false}.
     */
    static class BadEquals {

        final int value;

        BadEquals(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            // Intentionally broken – always returns false.
            return false;
        }
    }

    /**
     * Simple POJO with a correct {@code equals} implementation.
     */
    static class CustomEquals {

        final int value;

        CustomEquals(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomEquals))
                return false;
            return value == ((CustomEquals) o).value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    /**
     * POJO whose {@code equals} method always throws a {@link RuntimeException}.
     */
    static class ThrowingEquals {

        final int id;

        ThrowingEquals(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("equals() deliberately throws");
        }
    }

    /**
     * Node class used for cyclic‑graph tests (already defined in the original file).
     */
    static class Node {

        String name;

        Node next;

        Node(String name) {
            this.name = name;
        }
    }

    static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(Arguments.of("Both arguments are null – should return true.", null, null, new HashMap<>(), true), Arguments.of("First argument null, second non‑null – should return false.", null, 123, new HashMap<>(), false), Arguments.of("First argument non‑null, second null – should return false.", "test", null, new HashMap<>(), false));
    }

    static Stream<Arguments> ignoreCustomEqualsProvider() {
        Map<String, Object> ignoreOption = new HashMap<>();
        ignoreOption.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Boolean.TRUE);
        return Stream.of(Arguments.of("Option true – field‑by‑field comparison yields true.", new CustomEquals(5), new CustomEquals(5), ignoreOption, true), Arguments.of("Option true – different field values → false.", new CustomEquals(5), new CustomEquals(6), ignoreOption, false));
    }

    static Stream<Arguments> stringToNumberProvider() {
        Map<String, Object> opt = new HashMap<>();
        opt.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.TRUE);
        return Stream.of(Arguments.of("Numeric string matches Integer", "123", 123, opt, true), Arguments.of("Floating‑point string matches Double", "123.45", 123.45, opt, true), Arguments.of("Numeric string does not match different number", "123", 124, opt, false), Arguments.of("Non‑numeric string never matches a number", "abc", 123, opt, false));
    }

    static Stream<Arguments> collectionProvider() {
        return Stream.of(Arguments.of("Two Lists with same elements in same order are equal.", List.of(1, 2, 3), List.of(1, 2, 3), true), Arguments.of("Order differs – should be false for ordered collections.", List.of(1, 2, 3), List.of(3, 2, 1), false), Arguments.of("Two Sets with same elements – order irrelevant.", new HashSet<>(List.of(1, 2, 3)), new HashSet<>(List.of(3, 2, 1)), true), Arguments.of("Sets differ by one element – false.", new HashSet<>(List.of(1, 2, 3)), new HashSet<>(List.of(1, 2, 4)), false));
    }

    static Stream<Arguments> mapProvider() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("key1", 1);
        map1.put("key2", 2);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("key2", 2);
        map2.put("key1", 1);
        Map<String, Integer> mapDiff = new HashMap<>();
        mapDiff.put("key1", 2);
        return Stream.of(Arguments.of("Two Maps with identical key/value pairs – true.", map1, map2, true), Arguments.of("Same key but different values – false.", Collections.singletonMap("key1", 1), Collections.singletonMap("key1", 2), false));
    }

    static Stream<Arguments> arrayProvider() {
        int[] intArr1 = { 1, 2, 3 };
        int[] intArr2 = { 1, 2, 3 };
        int[] intArrDiff = { 3, 2, 1 };
        Object[] objArr1 = { "a", "b" };
        Object[] objArr2 = { "a", "b" };
        return Stream.of(Arguments.of("Two primitive int arrays with identical contents – true.", intArr1, intArr2, true), Arguments.of("Same length but different ordering – false.", intArr1, intArrDiff, false), Arguments.of("Object arrays with equal elements – true.", objArr1, objArr2, true));
    }

    @Test
    void deepEquals_PrimitiveWrappersWithDifferentTypes_ShouldBeEqualByValue() {
        Integer a = 5;
        Long b = 5L;
        assertTrue(DeepEquals.deepEquals(a, b, new HashMap<>()), "Integer 5 and Long 5L should be considered equal by value");
    }

    @Test
    void deepEquals_StringAndNumber_WithOptionEnabled_ShouldMatch() {
        String a = "5";
        Integer b = 5;
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.TRUE);
        assertTrue(DeepEquals.deepEquals(a, b, options), "String \"5\" should match Integer 5 when option is enabled");
    }

    @Test
    void deepEquals_StringAndNumber_WithOptionDisabled_ShouldNotMatch() {
        String a = "5";
        Integer b = 5;
        // options map left empty → default behaviour (no conversion)
        assertFalse(DeepEquals.deepEquals(a, b, new HashMap<>()), "String \"5\" should not match Integer 5 when option is disabled");
    }

    @Test
    void deepEquals_CustomEqualsIgnored_ShouldCompareFields() {
        BadEquals x = new BadEquals(10);
        BadEquals y = new BadEquals(10);
        // Without the flag the custom equals (which returns false) wins.
        assertFalse(DeepEquals.deepEquals(x, y, new HashMap<>()), "Custom equals returns false, so deepEquals should be false without ignoring it");
        // With the flag we ask DeepEquals to ignore the custom equals implementation.
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Boolean.TRUE);
        assertTrue(DeepEquals.deepEquals(x, y, options), "When ignoring custom equals, fields should be compared and be equal");
    }

    @Test
    void deepEquals_CyclicObjectGraph_ShouldHandleCyclesCorrectly() {
        // Build first cyclic graph: a -> b -> a
        Node a1 = new Node("A");
        Node b1 = new Node("B");
        a1.next = b1;
        b1.next = a1;
        // Build second cyclic graph with identical content
        Node a2 = new Node("A");
        Node b2 = new Node("B");
        a2.next = b2;
        b2.next = a2;
        assertTrue(DeepEquals.deepEquals(a1, a2, new HashMap<>()), "Cyclic graphs with identical structure and values should be equal");
    }

    @Test
    void deepEquals_NullValues_ShouldBehaveAsExpected() {
        assertTrue(DeepEquals.deepEquals(null, null, new HashMap<>()), "Both nulls should be considered equal");
        assertFalse(DeepEquals.deepEquals(null, new Object(), new HashMap<>()), "Null vs non‑null should not be equal");
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullHandlingProvider")
    void deepEquals_NullHandling(String description, Object a, Object b, Map<String, ?> options, boolean expected) {
        assertEquals(expected, DeepEquals.deepEquals(a, b, options), description);
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_SameReference() {
        String sameObject = "sameObject";
        assertTrue(DeepEquals.deepEquals(sameObject, sameObject, new HashMap<>()), "Both parameters refer to the same instance – should be equal without further checks.");
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("ignoreCustomEqualsProvider")
    void deepEquals_IgnoreCustomEquals(String description, Object a, Object b, Map<String, ?> options, boolean expected) {
        assertEquals(expected, DeepEquals.deepEquals(a, b, options), description);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringToNumberProvider")
    void deepEquals_StringToNumber(String description, Object a, Object b, Map<String, ?> options, boolean expected) {
        assertEquals(expected, DeepEquals.deepEquals(a, b, options), description);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("collectionProvider")
    void deepEquals_Collections(String description, Collection<?> a, Collection<?> b, boolean expected) {
        assertEquals(expected, DeepEquals.deepEquals(a, b, new HashMap<>()), description);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("mapProvider")
    void deepEquals_Maps(String description, Map<?, ?> a, Map<?, ?> b, boolean expected) {
        assertEquals(expected, DeepEquals.deepEquals(a, b, new HashMap<>()), description);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("arrayProvider")
    void deepEquals_Arrays(String description, Object a, Object b, boolean expected) {
        assertEquals(expected, DeepEquals.deepEquals(a, b, new HashMap<>()), description);
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_Dates() {
        Date d1 = Date.from(java.time.Instant.parse("2023-01-01T00:00:00Z"));
        Date d2 = Date.from(java.time.Instant.parse("2023-01-01T00:00:00Z"));
        Date d3 = Date.from(java.time.Instant.parse("2023-01-02T00:00:00Z"));
        assertTrue(DeepEquals.deepEquals(d1, d2, new HashMap<>()), "Same instant – true");
        assertFalse(DeepEquals.deepEquals(d1, d3, new HashMap<>()), "Different instant – false");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_BigDecimal() {
        BigDecimal bd1 = new BigDecimal("10.00");
        BigDecimal bd2 = new BigDecimal("10.0");
        BigDecimal bd3 = new BigDecimal("9.99");
        assertTrue(DeepEquals.deepEquals(bd1, bd2, new HashMap<>()), "Numerically equal despite different scale – true");
        assertFalse(DeepEquals.deepEquals(bd1, bd3, new HashMap<>()), "Different numeric values – false");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_AtomicBoolean() {
        AtomicBoolean ab1 = new AtomicBoolean(true);
        AtomicBoolean ab2 = new AtomicBoolean(true);
        AtomicBoolean ab3 = new AtomicBoolean(false);
        assertTrue(DeepEquals.deepEquals(ab1, ab2, new HashMap<>()), "Both true – true");
        assertFalse(DeepEquals.deepEquals(ab1, ab3, new HashMap<>()), "True vs false – false");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_DifferentTypes_NoOption() {
        assertFalse(DeepEquals.deepEquals(123, "123", new HashMap<>()), "Integer vs String without option – false");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_SelfCyclicReference() {
        Node n = new Node("self");
        // self‑reference
        n.next = n;
        Node m = new Node("self");
        m.next = m;
        assertTrue(DeepEquals.deepEquals(n, m, new HashMap<>()), "Two self‑referencing nodes should be equal without infinite recursion");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_MutualCyclicReference() {
        Node a = new Node("A");
        Node b = new Node("B");
        a.next = b;
        b.next = a;
        Node a2 = new Node("A");
        Node b2 = new Node("B");
        a2.next = b2;
        b2.next = a2;
        assertTrue(DeepEquals.deepEquals(a, a2, new HashMap<>()), "Two mutually cyclic graphs with identical content should be equal");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_CustomEquals_DefaultBehaviour() {
        CustomEquals x = new CustomEquals(5);
        CustomEquals y = new CustomEquals(5);
        CustomEquals z = new CustomEquals(6);
        // default – custom equals is used
        assertTrue(DeepEquals.deepEquals(x, y, new HashMap<>()), "Equal internal state → true");
        assertFalse(DeepEquals.deepEquals(x, z, new HashMap<>()), "Different internal state → false");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_DifferentCollectionImplementations() {
        List<Integer> arrayList = new ArrayList<>(List.of(1, 2, 3));
        List<Integer> linkedList = new LinkedList<>(List.of(1, 2, 3));
        List<Integer> linkedListDiff = new LinkedList<>(List.of(3, 2, 1));
        assertTrue(DeepEquals.deepEquals(arrayList, linkedList, new HashMap<>()), "Same ordered elements, different List implementations → true");
        assertFalse(DeepEquals.deepEquals(arrayList, linkedListDiff, new HashMap<>()), "Same elements but different order → false");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_EmptyCollectionsAndMaps() {
        List<Integer> emptyList1 = Collections.emptyList();
        List<Integer> emptyList2 = Collections.emptyList();
        Set<Integer> emptySet1 = Collections.emptySet();
        Set<Integer> emptySet2 = Collections.emptySet();
        Map<String, Integer> emptyMap1 = Collections.emptyMap();
        Map<String, Integer> emptyMap2 = Collections.emptyMap();
        assertTrue(DeepEquals.deepEquals(emptyList1, emptyList2, new HashMap<>()), "Two empty Lists – true");
        assertTrue(DeepEquals.deepEquals(emptySet1, emptySet2, new HashMap<>()), "Two empty Sets – true");
        assertFalse(DeepEquals.deepEquals(emptyList1, emptySet1, new HashMap<>()), "Empty List vs Empty Set – false");
        assertTrue(DeepEquals.deepEquals(emptyMap1, emptyMap2, new HashMap<>()), "Two empty Maps – true");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_NullOptionsMap() {
        assertTrue(DeepEquals.deepEquals(123, 123, null), "Null options map treated as empty – true");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_NestedStructures_Equal() {
        List<Map<String, Integer>> a = List.of(Map.of("k1", 1, "k2", 2), Map.of("k3", 3));
        List<Map<String, Integer>> b = List.of(// order inside map does not matter
        Map.of("k2", 2, "k1", 1), Map.of("k3", 3));
        assertTrue(DeepEquals.deepEquals(a, b, new HashMap<>()), "Nested List‑of‑Maps with same content – true");
    }

    @Test
    void deepEquals_NestedStructures_DifferentOrder() {
        List<Map<String, Integer>> a = List.of(Map.of("k1", 1), Map.of("k2", 2));
        List<Map<String, Integer>> b = List.of(Map.of("k2", 2), Map.of("k1", 1));
        assertFalse(DeepEquals.deepEquals(a, b, new HashMap<>()), "Same Maps but swapped List positions – false");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_EnumComparison() {
        assertTrue(DeepEquals.deepEquals(TestEnum.DAY, TestEnum.DAY, new HashMap<>()), "Identical enum constants – true");
        assertFalse(DeepEquals.deepEquals(TestEnum.DAY, TestEnum.NIGHT, new HashMap<>()), "Different enum constants – false");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_UnknownOptionKeyIgnored() {
        Map<String, Object> options = new HashMap<>();
        options.put("someUnknownFlag", Boolean.TRUE);
        List<Integer> a = List.of(1, 2);
        List<Integer> b = List.of(1, 2);
        assertTrue(DeepEquals.deepEquals(a, b, options), "Unrecognised option does not affect equality");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_BigDecimalAndString_WithOption() {
        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.TRUE);
        BigDecimal bd = new BigDecimal("10.00");
        String s = "10.00";
        assertTrue(DeepEquals.deepEquals(bd, s, options), "Numeric string matches BigDecimal when option enabled");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_PrimitiveWrapperInequality() {
        assertFalse(DeepEquals.deepEquals(5, 5.0, new HashMap<>()), "Integer vs Double without option – false");
        Map<String, Object> opt = new HashMap<>();
        opt.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.TRUE);
        assertFalse(DeepEquals.deepEquals(5, 5.0, opt), "Integer vs Double even with string‑match option – false");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_ArrayLengthMismatch() {
        int[] a = { 1, 2 };
        int[] b = { 1, 2, 3 };
        assertFalse(DeepEquals.deepEquals(a, b, new HashMap<>()), "Arrays of different lengths – false");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_SetVsList() {
        Set<Integer> set = new HashSet<>(List.of(1, 2, 3));
        List<Integer> list = List.of(1, 2, 3);
        assertFalse(DeepEquals.deepEquals(set, list, new HashMap<>()), "Set vs List – false");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_CollectionsWithNullElements() {
        List<Object> a = List.of(null, 1, "a");
        List<Object> b = List.of(null, 1, "a");
        List<Object> c = List.of(1, null, "a");
        assertTrue(DeepEquals.deepEquals(a, b, new HashMap<>()), "Identical null positions – true");
        assertFalse(DeepEquals.deepEquals(a, c, new HashMap<>()), "Different null positions – false");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_InvalidOptionValue_IgnoreCustomEquals() {
        Map<String, Object> badOptions = new HashMap<>();
        // not a Boolean
        badOptions.put(DeepEquals.IGNORE_CUSTOM_EQUALS, "yes");
        assertThrows(IllegalArgumentException.class, () -> DeepEquals.deepEquals(5, 5, badOptions), "Non‑boolean value for IGNORE_CUSTOM_EQUALS should throw IllegalArgumentException");
    }

    @Test
    void deepEquals_InvalidOptionValue_StringsCanMatchNumbers() {
        Map<String, Object> badOptions = new HashMap<>();
        // not a Boolean
        badOptions.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, 1);
        assertThrows(IllegalArgumentException.class, () -> DeepEquals.deepEquals(5, 5, badOptions), "Non‑boolean value for stringsCanMatchNumbers should throw IllegalArgumentException");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_CustomEqualsThrowsException_DefaultBehaviour() {
        ThrowingEquals a = new ThrowingEquals(1);
        ThrowingEquals b = new ThrowingEquals(2);
        assertThrows(RuntimeException.class, () -> DeepEquals.deepEquals(a, b, new HashMap<>()), "Exception from custom equals should propagate when not ignored");
    }

    @Test
    void deepEquals_CustomEqualsThrowsException_Ignored() {
        ThrowingEquals a = new ThrowingEquals(1);
        ThrowingEquals b = new ThrowingEquals(2);
        Map<String, Object> opts = new HashMap<>();
        opts.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Boolean.TRUE);
        // With ignore flag, field‑by‑field comparison (both have same field value) → true
        assertTrue(DeepEquals.deepEquals(a, b, opts), "Ignoring throwing equals should result in true");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_NullKeyInOptionsMap() {
        Map<String, Object> badOptions = new HashMap<>();
        // null key
        badOptions.put(null, Boolean.TRUE);
        badOptions.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Boolean.TRUE);
        assertThrows(IllegalArgumentException.class, () -> DeepEquals.deepEquals(10, 10, badOptions), "Options map containing a null key should throw IllegalArgumentException");
    }

    // ----------------------------------------------------------------------
    @Test
    void deepEquals_ArrayStoreException_IncompatibleTypes() {
        Object[] objArray = new Object[] { 1, "two", 3 };
        String[] strArray = new String[] { "1", "two", "3" };
        assertThrows(ArrayStoreException.class, () -> DeepEquals.deepEquals(objArray, strArray, new HashMap<>()), "Incompatible array element types should cause ArrayStoreException");
    }
}
