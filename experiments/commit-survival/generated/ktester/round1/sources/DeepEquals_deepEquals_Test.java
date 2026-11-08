package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(MockitoExtension.class)
class DeepEquals_deepEquals_Test {

    /**
     * Options map that enables string‑to‑number matching.
     */
    private Map<String, Object> allowStringMatchOptions;

    /**
     * Options map that forces DeepEquals to ignore custom {@code equals()} methods.
     */
    private Map<String, Object> ignoreCustomEqualsOptions;

    static class WithCustomEquals {

        final int value;

        WithCustomEquals(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            // Deliberately return false to prove DeepEquals uses the custom method.
            return false;
        }
    }

    static class Node {

        String name;

        Node next;

        Node(String name) {
            this.name = name;
        }
    }

    /**
     * ------------------------------------------------------------------- *
     *  Custom equals ignored via option – additional verification
     *  -------------------------------------------------------------------
     */
    static class CustomEquals {

        final int value;

        CustomEquals(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            // deliberately incorrect implementation
            return false;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        allowStringMatchOptions = new HashMap<>();
        allowStringMatchOptions.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, Boolean.TRUE);
        ignoreCustomEqualsOptions = new HashMap<>();
        ignoreCustomEqualsOptions.put(DeepEquals.IGNORE_CUSTOM_EQUALS, Boolean.TRUE);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up per‑test state.
        allowStringMatchOptions.clear();
        ignoreCustomEqualsOptions.clear();
    }

    /**
     * ------------------------------------------------------------------- *
     *  Helper for cyclic node creation used by the JSON test case.
     *  -------------------------------------------------------------------
     */
    private static Node createCyclicNode() {
        Node n = new Node("self");
        // direct self‑reference creates a 1‑node cycle
        n.next = n;
        return n;
    }

    @Test
    void simplePrimitivesAreEqual() {
        assertTrue(DeepEquals.deepEquals(5, 5));
        assertTrue(DeepEquals.deepEquals(5L, 5L));
        assertTrue(DeepEquals.deepEquals('a', 'a'));
        assertTrue(DeepEquals.deepEquals(true, true));
    }

    @Test
    void differentPrimitivesAreNotEqual() {
        assertFalse(DeepEquals.deepEquals(5, 6));
        assertFalse(DeepEquals.deepEquals(5L, 6L));
        assertFalse(DeepEquals.deepEquals('a', 'b'));
        assertFalse(DeepEquals.deepEquals(true, false));
    }

    @Test
    void numericTypesWithSameValueAreEqual() {
        // int vs long, double vs float, etc.
        assertTrue(DeepEquals.deepEquals(42, 42L));
        assertTrue(DeepEquals.deepEquals(3.14f, 3.14d));
        assertTrue(DeepEquals.deepEquals(new BigDecimal("10"), 10));
    }

    @Test
    void stringNumberMatchingWhenOptionEnabled() {
        // "123" (String) vs 123 (Integer) should be equal only when the option is present.
        assertTrue(DeepEquals.deepEquals("123", 123, allowStringMatchOptions));
        assertTrue(DeepEquals.deepEquals("45.6", 45.6, allowStringMatchOptions));
        // Without the option the comparison falls back to normal Object.equals()
        assertFalse(DeepEquals.deepEquals("123", 123));
    }

    @Test
    void stringsAreNotComparedAsNumbersWhenBothAreStrings() {
        assertFalse(DeepEquals.deepEquals("123", "124", allowStringMatchOptions));
        assertTrue(DeepEquals.deepEquals("123", "123", allowStringMatchOptions));
    }

    @Test
    void deepEqualsUsesCustomEqualsByDefault() {
        WithCustomEquals a = new WithCustomEquals(5);
        WithCustomEquals b = new WithCustomEquals(5);
        // Because the class overrides equals() to always return false, deepEquals should also return false.
        assertFalse(DeepEquals.deepEquals(a, b));
    }

    @Test
    void deepEqualsIgnoresCustomEqualsWhenOptionSet() {
        WithCustomEquals a = new WithCustomEquals(5);
        WithCustomEquals b = new WithCustomEquals(5);
        // With the IGNORE_CUSTOM_EQUALS flag, DeepEquals falls back to field‑by‑field comparison.
        assertTrue(DeepEquals.deepEquals(a, b, ignoreCustomEqualsOptions));
    }

    @Test
    void deepEqualsHandlesCyclicGraphs() {
        Node a1 = new Node("A");
        Node a2 = new Node("B");
        a1.next = a2;
        // cycle
        a2.next = a1;
        Node b1 = new Node("A");
        Node b2 = new Node("B");
        b1.next = b2;
        // same shape, same values
        b2.next = b1;
        assertTrue(DeepEquals.deepEquals(a1, b1));
    }

    @Test
    void deepEqualsDetectsDifferenceInCyclicGraphs() {
        Node a1 = new Node("A");
        Node a2 = new Node("B");
        a1.next = a2;
        a2.next = a1;
        Node b1 = new Node("A");
        // different value
        Node b2 = new Node("C");
        b1.next = b2;
        b2.next = b1;
        assertFalse(DeepEquals.deepEquals(a1, b1));
    }

    @Test
    void listDeepEquality() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        List<Integer> list3 = Arrays.asList(1, 2, 4);
        assertTrue(DeepEquals.deepEquals(list1, list2));
        assertFalse(DeepEquals.deepEquals(list1, list3));
    }

    @Test
    void mapDeepEquality() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("one", 1);
        m1.put("two", 2);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("one", 1);
        m2.put("two", 2);
        Map<String, Integer> m3 = new HashMap<>();
        m3.put("one", 1);
        // different value
        m3.put("two", 3);
        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertFalse(DeepEquals.deepEquals(m1, m3));
    }

    @Test
    void arrayDeepEquality() {
        int[] a1 = { 1, 2, 3 };
        int[] a2 = { 1, 2, 3 };
        int[] a3 = { 1, 2, 4 };
        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    @Test
    void multiDimensionalArrayEquality() {
        Integer[][] a1 = { { 1, 2 }, { 3, 4 } };
        Integer[][] a2 = { { 1, 2 }, { 3, 4 } };
        // swapped inner values
        Integer[][] a3 = { { 1, 2 }, { 4, 3 } };
        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertFalse(DeepEquals.deepEquals(a1, a3));
    }

    @Test
    void nullValuesAreHandledCorrectly() {
        assertTrue(DeepEquals.deepEquals(null, null));
        assertFalse(DeepEquals.deepEquals(null, "not null"));
        assertFalse(DeepEquals.deepEquals("not null", null));
    }

    /**
     * ------------------------------------------------------------------- *
     *  Null handling and identity
     *  -------------------------------------------------------------------
     */
    @Test
    void bothNullAreEqual() {
        assertTrue(DeepEquals.deepEquals(null, null));
    }

    @Test
    void nullVsNonNullIsNotEqual() {
        assertFalse(DeepEquals.deepEquals(null, new Object()));
        assertFalse(DeepEquals.deepEquals(new Object(), null));
    }

    @Test
    void sameReferenceIsEqualEvenWithoutDeepTraversal() {
        Object obj = new Object();
        // both parameters refer to the exact same instance
        assertTrue(DeepEquals.deepEquals(obj, obj));
    }

    /**
     * ------------------------------------------------------------------- *
     *  Primitive wrapper equality
     *  -------------------------------------------------------------------
     */
    @Test
    void primitiveWrapperEquality() {
        // distinct Integer objects with same value
        Integer i1 = Integer.valueOf(42);
        Integer i2 = new Integer(42);
        assertTrue(DeepEquals.deepEquals(i1, i2));
        // Integer vs Long – same numeric value but different wrapper types → false
        assertFalse(DeepEquals.deepEquals(Integer.valueOf(42), Long.valueOf(42L)));
        // Double vs Float – same numeric value but different wrapper classes → false
        assertFalse(DeepEquals.deepEquals(Double.valueOf(3.0), Float.valueOf(3.0f)));
    }

    /**
     * ------------------------------------------------------------------- *
     *  String‑to‑Number matching (option enabled) – extra cases
     *  -------------------------------------------------------------------
     */
    @Test
    void stringNumberMatchingAdditionalCases() {
        // numeric string vs BigDecimal
        assertTrue(DeepEquals.deepEquals("123.45", new BigDecimal("123.45"), allowStringMatchOptions));
        // non‑numeric string never matches a number
        assertFalse(DeepEquals.deepEquals("123abc", Integer.valueOf(123), allowStringMatchOptions));
    }

    /**
     * ------------------------------------------------------------------- *
     *  Set equality (order‑independent)
     *  -------------------------------------------------------------------
     */
    @Test
    void setEqualityIsOrderIndependent() {
        Set<String> s1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> s2 = new LinkedHashSet<>(Arrays.asList("c", "b", "a"));
        assertTrue(DeepEquals.deepEquals(s1, s2));
        Set<String> singleton = Collections.singleton("x");
        Set<String> empty = Collections.emptySet();
        assertFalse(DeepEquals.deepEquals(singleton, empty));
    }

    /**
     * ------------------------------------------------------------------- *
     *  Map equality with nested collections
     *  -------------------------------------------------------------------
     */
    @Test
    void mapWithNestedCollectionsEquality() {
        Map<String, Object> a = new HashMap<>();
        a.put("k1", Arrays.asList(1, 2));
        a.put("k2", "v");
        Map<String, Object> b = new HashMap<>();
        b.put("k2", "v");
        b.put("k1", Arrays.asList(1, 2));
        assertTrue(DeepEquals.deepEquals(a, b));
        Map<String, Object> c = new HashMap<>();
        // different size
        c.put("k1", Arrays.asList(1, 2, 3));
        c.put("k2", "v");
        assertFalse(DeepEquals.deepEquals(a, c));
    }

    /**
     * ------------------------------------------------------------------- *
     *  BigDecimal scale handling
     *  -------------------------------------------------------------------
     */
    @Test
    void bigDecimalScaleIsIgnoredForEquality() {
        assertTrue(DeepEquals.deepEquals(new BigDecimal("1.0"), new BigDecimal("1.00")));
        assertTrue(DeepEquals.deepEquals(new BigDecimal("1.01"), new BigDecimal("1.010")));
    }

    /**
     * ------------------------------------------------------------------- *
     *  Date equality
     *  -------------------------------------------------------------------
     */
    @Test
    void dateEquality() {
        Date d1 = new Date(1633024800000L);
        Date d2 = new Date(1633024800000L);
        assertTrue(DeepEquals.deepEquals(d1, d2));
        Date d3 = new Date(0L);
        Date d4 = new Date(1L);
        assertFalse(DeepEquals.deepEquals(d3, d4));
    }

    /**
     * ------------------------------------------------------------------- *
     *  AtomicBoolean equality
     *  -------------------------------------------------------------------
     */
    @Test
    void atomicBooleanEquality() {
        assertTrue(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(true)));
        assertFalse(DeepEquals.deepEquals(new AtomicBoolean(true), new AtomicBoolean(false)));
    }

    /**
     * ------------------------------------------------------------------- *
     *  Mixed container types (List vs Deque)
     *  -------------------------------------------------------------------
     */
    @Test
    void mixedContainerTypes() {
        List<Integer> arrayList = Arrays.asList(1, 2, 3);
        Deque<Integer> linkedList = new LinkedList<>(Arrays.asList(1, 2, 3));
        assertTrue(DeepEquals.deepEquals(arrayList, linkedList));
        Deque<Integer> arrayDeque = new ArrayDeque<>(Arrays.asList(1, 2, 3));
        assertFalse(DeepEquals.deepEquals(arrayList, arrayDeque));
    }

    /**
     * ------------------------------------------------------------------- *
     *  Invalid option value type – expecting ClassCastException
     *  -------------------------------------------------------------------
     */
    @Test
    void invalidOptionValueTypeThrowsClassCastException() {
        Map<String, Object> badIgnore = Collections.singletonMap(DeepEquals.IGNORE_CUSTOM_EQUALS, "true");
        assertThrows(ClassCastException.class, () -> DeepEquals.deepEquals(Integer.valueOf(1), Integer.valueOf(1), badIgnore));
        Map<String, Object> badStringMatch = Collections.singletonMap(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, 1);
        assertThrows(ClassCastException.class, () -> DeepEquals.deepEquals(Integer.valueOf(2), Integer.valueOf(2), badStringMatch));
    }

    /**
     * ------------------------------------------------------------------- *
     *  Null option key – expecting NullPointerException
     *  -------------------------------------------------------------------
     */
    @Test
    void nullOptionKeyThrowsNullPointerException() {
        Map<String, Object> mapWithNullKey = new HashMap<>();
        mapWithNullKey.put(null, Boolean.TRUE);
        assertThrows(NullPointerException.class, () -> DeepEquals.deepEquals(Integer.valueOf(3), Integer.valueOf(3), mapWithNullKey));
    }

    @Test
    void cyclicReferenceHandlingFromJson() {
        Node a = createCyclicNode();
        Node b = createCyclicNode();
        assertTrue(DeepEquals.deepEquals(a, b));
    }

    @Test
    void customEqualsIgnoredOptionCausesFieldComparison() {
        CustomEquals a = new CustomEquals(5);
        CustomEquals b = new CustomEquals(10);
        // With differing field values the field‑by‑field comparison must be false
        assertFalse(DeepEquals.deepEquals(a, b, ignoreCustomEqualsOptions));
        CustomEquals c = new CustomEquals(5);
        CustomEquals d = new CustomEquals(5);
        // Same field values → true when custom equals is ignored
        assertTrue(DeepEquals.deepEquals(c, d, ignoreCustomEqualsOptions));
    }
}
