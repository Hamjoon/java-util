package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class CompactSet_createSimpleMap_Test {

    /**
     * Returns the simple name of the concrete map that {@code CompactMap#getNewMap()}
     * creates for the given ordering and case‑sensitivity.
     */
    private static Class<?> expectedMapClass(String ordering, boolean caseSensitive) {
        switch(ordering) {
            case CompactMap.INSERTION:
                return caseSensitive ? LinkedHashMap.class : CaseInsensitiveMap.class;
            case CompactMap.SORTED:
            case CompactMap.REVERSE:
                // TreeMap is wrapped in CaseInsensitiveMap when not case‑sensitive
                return caseSensitive ? TreeMap.class : CaseInsensitiveMap.class;
            default:
                // UNORDERED or any other string
                return caseSensitive ? HashMap.class : CaseInsensitiveMap.class;
        }
    }

    /**
     * Extracts the underlying map from a {@code CompactMap} instance.
     * The anonymous subclass created by {@code createSimpleMap} does not expose the
     * map directly, but {@code getNewMap()} returns a fresh instance each call.
     * For the purpose of these tests we invoke it once and inspect the class.
     */
    private static Map<?, ?> newUnderlyingMap(CompactMap<?, ?> cm) {
        return cm.getNewMap();
    }

    /**
     * INSERTION ordering – both case‑sensitive and case‑insensitive cases
     */
    static Stream<Arguments> insertionOrderingProvider() {
        return Stream.of(// case‑sensitive
        Arguments.of(true, 5, CompactMap.INSERTION, LinkedHashMap.class, null, false, 5, CompactMap.INSERTION), // case‑insensitive
        Arguments.of(false, 3, CompactMap.INSERTION, CaseInsensitiveMap.class, LinkedHashMap.class, true, 3, CompactMap.INSERTION));
    }

    /**
     * SORTED ordering – both case‑sensitive and case‑insensitive cases
     */
    static Stream<Arguments> sortedOrderingProvider() {
        return Stream.of(// case‑sensitive
        Arguments.of(true, 7, CompactMap.SORTED, TreeMap.class, null, false, 7, CompactMap.SORTED), // case‑insensitive
        Arguments.of(false, 4, CompactMap.SORTED, CaseInsensitiveMap.class, TreeMap.class, true, 4, CompactMap.SORTED));
    }

    /**
     * REVERSE ordering – both case‑sensitive and case‑insensitive cases
     */
    static Stream<Arguments> reverseOrderingProvider() {
        return Stream.of(// case‑sensitive
        Arguments.of(true, 6, CompactMap.REVERSE, TreeMap.class, null, false, 6, CompactMap.REVERSE), // case‑insensitive
        Arguments.of(false, 2, CompactMap.REVERSE, CaseInsensitiveMap.class, TreeMap.class, true, 2, CompactMap.REVERSE));
    }

    /**
     * Unknown ordering string – falls back to default unordered map
     */
    static Stream<Arguments> unknownOrderingProvider() {
        return Stream.of(// case‑sensitive
        Arguments.of(true, 5, "UNKNOWN", HashMap.class, null, false, 5, "UNKNOWN"), // case‑insensitive
        Arguments.of(false, 3, "UNKNOWN", CaseInsensitiveMap.class, HashMap.class, true, 3, "UNKNOWN"));
    }

    /**
     * Size zero edge case – insertion ordering
     */
    static Stream<Arguments> zeroSizeProvider() {
        return Stream.of(// case‑sensitive
        Arguments.of(true, 0, CompactMap.INSERTION, LinkedHashMap.class, null, false, 0, CompactMap.INSERTION), // case‑insensitive
        Arguments.of(false, 0, CompactMap.INSERTION, CaseInsensitiveMap.class, LinkedHashMap.class, true, 0, CompactMap.INSERTION));
    }

    /**
     * Negative size edge case – mixed orderings
     */
    static Stream<Arguments> negativeSizeProvider() {
        return Stream.of(// case‑sensitive, INSERTION
        Arguments.of(true, -1, CompactMap.INSERTION, LinkedHashMap.class, null, false, -1, CompactMap.INSERTION), // case‑insensitive, SORTED
        Arguments.of(false, -2, CompactMap.SORTED, CaseInsensitiveMap.class, TreeMap.class, true, -2, CompactMap.SORTED));
    }

    /**
     * Null ordering string – should throw NullPointerException
     */
    static Stream<Arguments> nullOrderingProvider() {
        return Stream.of(Arguments.of(true, 5, null), Arguments.of(false, 3, null));
    }

    @Test
    void createSimpleMap_caseSensitiveInsertionOrder() {
        boolean caseSensitive = true;
        int size = 8;
        String ordering = CompactMap.INSERTION;
        CompactMap<String, Object> cm = CompactSet.createSimpleMap(caseSensitive, size, ordering);
        assertFalse(cm.isCaseInsensitive(), "Case‑insensitive flag should be false");
        assertEquals(size, cm.compactSize(), "compactSize should be the supplied size");
        assertEquals(ordering, cm.getOrdering(), "ordering should be preserved");
        Map<?, ?> underlying = newUnderlyingMap(cm);
        assertTrue(underlying instanceof LinkedHashMap, "Expected LinkedHashMap for case‑sensitive insertion order");
    }

    @Test
    void createSimpleMap_caseInsensitiveInsertionOrder() {
        boolean caseSensitive = false;
        int size = 5;
        String ordering = CompactMap.INSERTION;
        CompactMap<String, Object> cm = CompactSet.createSimpleMap(caseSensitive, size, ordering);
        assertTrue(cm.isCaseInsensitive(), "Case‑insensitive flag should be true");
        assertEquals(size, cm.compactSize());
        assertEquals(ordering, cm.getOrdering());
        Map<?, ?> underlying = newUnderlyingMap(cm);
        assertTrue(underlying instanceof CaseInsensitiveMap, "Expected CaseInsensitiveMap wrapper for case‑insensitive insertion order");
        // The delegate inside the wrapper should be a LinkedHashMap
        CaseInsensitiveMap<?, ?> ciMap = (CaseInsensitiveMap<?, ?>) underlying;
        assertTrue(getDelegateMap(ciMap) instanceof LinkedHashMap, "Delegate of CaseInsensitiveMap should be LinkedHashMap for insertion order");
    }

    @Test
    void createSimpleMap_caseSensitiveSortedOrder() {
        boolean caseSensitive = true;
        int size = 12;
        String ordering = CompactMap.SORTED;
        CompactMap<String, Object> cm = CompactSet.createSimpleMap(caseSensitive, size, ordering);
        assertFalse(cm.isCaseInsensitive());
        assertEquals(size, cm.compactSize());
        assertEquals(ordering, cm.getOrdering());
        Map<?, ?> underlying = newUnderlyingMap(cm);
        assertTrue(underlying instanceof TreeMap, "Expected TreeMap for case‑sensitive sorted order");
    }

    @Test
    void createSimpleMap_caseInsensitiveSortedOrder() {
        boolean caseSensitive = false;
        int size = 3;
        String ordering = CompactMap.SORTED;
        CompactMap<String, Object> cm = CompactSet.createSimpleMap(caseSensitive, size, ordering);
        assertTrue(cm.isCaseInsensitive());
        assertEquals(size, cm.compactSize());
        assertEquals(ordering, cm.getOrdering());
        Map<?, ?> underlying = newUnderlyingMap(cm);
        assertTrue(underlying instanceof CaseInsensitiveMap, "Expected CaseInsensitiveMap wrapper for case‑insensitive sorted order");
        CaseInsensitiveMap<?, ?> ciMap = (CaseInsensitiveMap<?, ?>) underlying;
        assertTrue(getDelegateMap(ciMap) instanceof TreeMap, "Delegate of CaseInsensitiveMap should be TreeMap for sorted order");
    }

    @Test
    void createSimpleMap_caseSensitiveReverseOrder() {
        boolean caseSensitive = true;
        int size = 7;
        String ordering = CompactMap.REVERSE;
        CompactMap<String, Object> cm = CompactSet.createSimpleMap(caseSensitive, size, ordering);
        assertFalse(cm.isCaseInsensitive());
        assertEquals(size, cm.compactSize());
        assertEquals(ordering, cm.getOrdering());
        Map<?, ?> underlying = newUnderlyingMap(cm);
        assertTrue(underlying instanceof TreeMap, "Expected TreeMap for case‑sensitive reverse order (comparator handles reverse)");
    }

    @Test
    void createSimpleMap_caseInsensitiveReverseOrder() {
        boolean caseSensitive = false;
        int size = 9;
        String ordering = CompactMap.REVERSE;
        CompactMap<String, Object> cm = CompactSet.createSimpleMap(caseSensitive, size, ordering);
        assertTrue(cm.isCaseInsensitive());
        assertEquals(size, cm.compactSize());
        assertEquals(ordering, cm.getOrdering());
        Map<?, ?> underlying = newUnderlyingMap(cm);
        assertTrue(underlying instanceof CaseInsensitiveMap, "Expected CaseInsensitiveMap wrapper for case‑insensitive reverse order");
        CaseInsensitiveMap<?, ?> ciMap = (CaseInsensitiveMap<?, ?>) underlying;
        assertTrue(getDelegateMap(ciMap) instanceof TreeMap, "Delegate of CaseInsensitiveMap should be TreeMap for reverse order");
    }

    @Test
    void createSimpleMap_caseSensitiveUnordered() {
        boolean caseSensitive = true;
        int size = 4;
        String ordering = CompactMap.UNORDERED;
        CompactMap<String, Object> cm = CompactSet.createSimpleMap(caseSensitive, size, ordering);
        assertFalse(cm.isCaseInsensitive());
        assertEquals(size, cm.compactSize());
        assertEquals(ordering, cm.getOrdering());
        Map<?, ?> underlying = newUnderlyingMap(cm);
        assertTrue(underlying instanceof HashMap, "Expected HashMap for case‑sensitive unordered map");
    }

    @Test
    void createSimpleMap_caseInsensitiveUnordered() {
        boolean caseSensitive = false;
        int size = 6;
        String ordering = "some-unknown-ordering";
        CompactMap<String, Object> cm = CompactSet.createSimpleMap(caseSensitive, size, ordering);
        assertTrue(cm.isCaseInsensitive());
        assertEquals(size, cm.compactSize());
        assertEquals(ordering, cm.getOrdering());
        Map<?, ?> underlying = newUnderlyingMap(cm);
        assertTrue(underlying instanceof CaseInsensitiveMap, "Expected CaseInsensitiveMap wrapper for case‑insensitive unordered map");
        CaseInsensitiveMap<?, ?> ciMap = (CaseInsensitiveMap<?, ?>) underlying;
        assertTrue(getDelegateMap(ciMap) instanceof HashMap, "Delegate of CaseInsensitiveMap should be HashMap for unordered map");
    }

    @ParameterizedTest(name = "[INSERTION] caseSensitive={0}, size={1}")
    @MethodSource("insertionOrderingProvider")
    void testInsertionOrdering(boolean caseSensitive, int size, String ordering, Class<?> expectedMapClass, Class<?> expectedWrappedClass, boolean expectedCaseInsensitive, int expectedCompactSize, String expectedOrdering) {
        CompactMap<String, Object> cm = CompactSet.createSimpleMap(caseSensitive, size, ordering);
        assertEquals(expectedCaseInsensitive, cm.isCaseInsensitive(), "Case‑insensitive flag mismatch");
        assertEquals(expectedCompactSize, cm.compactSize(), "compactSize mismatch");
        assertEquals(expectedOrdering, cm.getOrdering(), "ordering string mismatch");
        Map<?, ?> underlying = newUnderlyingMap(cm);
        assertTrue(expectedMapClass.isInstance(underlying), () -> "Underlying map should be instance of " + expectedMapClass.getSimpleName());
        if (expectedWrappedClass != null) {
            assertTrue(underlying instanceof CaseInsensitiveMap, "Underlying map should be a CaseInsensitiveMap wrapper");
            CaseInsensitiveMap<?, ?> ciMap = (CaseInsensitiveMap<?, ?>) underlying;
            assertTrue(expectedWrappedClass.isInstance(getDelegateMap(ciMap)), () -> "Delegate of CaseInsensitiveMap should be " + expectedWrappedClass.getSimpleName());
        }
    }

    @ParameterizedTest(name = "[SORTED] caseSensitive={0}, size={1}")
    @MethodSource("sortedOrderingProvider")
    void testSortedOrdering(boolean caseSensitive, int size, String ordering, Class<?> expectedMapClass, Class<?> expectedWrappedClass, boolean expectedCaseInsensitive, int expectedCompactSize, String expectedOrdering) {
        CompactMap<String, Object> cm = CompactSet.createSimpleMap(caseSensitive, size, ordering);
        assertEquals(expectedCaseInsensitive, cm.isCaseInsensitive());
        assertEquals(expectedCompactSize, cm.compactSize());
        assertEquals(expectedOrdering, cm.getOrdering());
        Map<?, ?> underlying = newUnderlyingMap(cm);
        assertTrue(expectedMapClass.isInstance(underlying), () -> "Underlying map should be instance of " + expectedMapClass.getSimpleName());
        if (expectedWrappedClass != null) {
            assertTrue(underlying instanceof CaseInsensitiveMap);
            CaseInsensitiveMap<?, ?> ciMap = (CaseInsensitiveMap<?, ?>) underlying;
            assertTrue(expectedWrappedClass.isInstance(getDelegateMap(ciMap)), () -> "Delegate should be " + expectedWrappedClass.getSimpleName());
        }
    }

    @ParameterizedTest(name = "[REVERSE] caseSensitive={0}, size={1}")
    @MethodSource("reverseOrderingProvider")
    void testReverseOrdering(boolean caseSensitive, int size, String ordering, Class<?> expectedMapClass, Class<?> expectedWrappedClass, boolean expectedCaseInsensitive, int expectedCompactSize, String expectedOrdering) {
        CompactMap<String, Object> cm = CompactSet.createSimpleMap(caseSensitive, size, ordering);
        assertEquals(expectedCaseInsensitive, cm.isCaseInsensitive());
        assertEquals(expectedCompactSize, cm.compactSize());
        assertEquals(expectedOrdering, cm.getOrdering());
        Map<?, ?> underlying = newUnderlyingMap(cm);
        assertTrue(expectedMapClass.isInstance(underlying), () -> "Underlying map should be instance of " + expectedMapClass.getSimpleName());
        if (expectedWrappedClass != null) {
            assertTrue(underlying instanceof CaseInsensitiveMap);
            CaseInsensitiveMap<?, ?> ciMap = (CaseInsensitiveMap<?, ?>) underlying;
            assertTrue(expectedWrappedClass.isInstance(getDelegateMap(ciMap)), () -> "Delegate should be " + expectedWrappedClass.getSimpleName());
        }
    }

    @ParameterizedTest(name = "[UNKNOWN ORDERING] caseSensitive={0}, size={1}")
    @MethodSource("unknownOrderingProvider")
    void testUnknownOrdering(boolean caseSensitive, int size, String ordering, Class<?> expectedMapClass, Class<?> expectedWrappedClass, boolean expectedCaseInsensitive, int expectedCompactSize, String expectedOrdering) {
        CompactMap<String, Object> cm = CompactSet.createSimpleMap(caseSensitive, size, ordering);
        assertEquals(expectedCaseInsensitive, cm.isCaseInsensitive());
        assertEquals(expectedCompactSize, cm.compactSize());
        assertEquals(expectedOrdering, cm.getOrdering());
        Map<?, ?> underlying = newUnderlyingMap(cm);
        assertTrue(expectedMapClass.isInstance(underlying), () -> "Underlying map should be instance of " + expectedMapClass.getSimpleName());
        if (expectedWrappedClass != null) {
            assertTrue(underlying instanceof CaseInsensitiveMap);
            CaseInsensitiveMap<?, ?> ciMap = (CaseInsensitiveMap<?, ?>) underlying;
            assertTrue(expectedWrappedClass.isInstance(getDelegateMap(ciMap)), () -> "Delegate should be " + expectedWrappedClass.getSimpleName());
        }
    }

    @ParameterizedTest(name = "[SIZE ZERO] caseSensitive={0}")
    @MethodSource("zeroSizeProvider")
    void testZeroSize(boolean caseSensitive, int size, String ordering, Class<?> expectedMapClass, Class<?> expectedWrappedClass, boolean expectedCaseInsensitive, int expectedCompactSize, String expectedOrdering) {
        CompactMap<String, Object> cm = CompactSet.createSimpleMap(caseSensitive, size, ordering);
        assertEquals(expectedCaseInsensitive, cm.isCaseInsensitive());
        assertEquals(expectedCompactSize, cm.compactSize());
        assertEquals(expectedOrdering, cm.getOrdering());
        Map<?, ?> underlying = newUnderlyingMap(cm);
        assertTrue(expectedMapClass.isInstance(underlying), () -> "Underlying map should be instance of " + expectedMapClass.getSimpleName());
        if (expectedWrappedClass != null) {
            assertTrue(underlying instanceof CaseInsensitiveMap);
            CaseInsensitiveMap<?, ?> ciMap = (CaseInsensitiveMap<?, ?>) underlying;
            assertTrue(expectedWrappedClass.isInstance(getDelegateMap(ciMap)), () -> "Delegate should be " + expectedWrappedClass.getSimpleName());
        }
    }

    @ParameterizedTest(name = "[NEGATIVE SIZE] caseSensitive={0}")
    @MethodSource("negativeSizeProvider")
    void testNegativeSize(boolean caseSensitive, int size, String ordering, Class<?> expectedMapClass, Class<?> expectedWrappedClass, boolean expectedCaseInsensitive, int expectedCompactSize, String expectedOrdering) {
        CompactMap<String, Object> cm = CompactSet.createSimpleMap(caseSensitive, size, ordering);
        assertEquals(expectedCaseInsensitive, cm.isCaseInsensitive());
        assertEquals(expectedCompactSize, cm.compactSize());
        assertEquals(expectedOrdering, cm.getOrdering());
        Map<?, ?> underlying = newUnderlyingMap(cm);
        assertTrue(expectedMapClass.isInstance(underlying), () -> "Underlying map should be instance of " + expectedMapClass.getSimpleName());
        if (expectedWrappedClass != null) {
            assertTrue(underlying instanceof CaseInsensitiveMap);
            CaseInsensitiveMap<?, ?> ciMap = (CaseInsensitiveMap<?, ?>) underlying;
            assertTrue(expectedWrappedClass.isInstance(getDelegateMap(ciMap)), () -> "Delegate should be " + expectedWrappedClass.getSimpleName());
        }
    }

    @ParameterizedTest(name = "[NULL ORDERING] caseSensitive={0}")
    @MethodSource("nullOrderingProvider")
    void testNullOrderingThrows(boolean caseSensitive, int size, String ordering) {
        assertThrows(NullPointerException.class, () -> CompactSet.createSimpleMap(caseSensitive, size, ordering), "Expected NullPointerException when ordering is null");
    }

    /**
     * Helper that extracts the delegate map from a {@code CaseInsensitiveMap}.
     * The actual implementation of {@code CaseInsensitiveMap} does not expose a
     * public accessor, so we use reflection to read the private field that holds
     * the wrapped map (commonly named "map").
     */
    @SuppressWarnings("unchecked")
    private static Map<?, ?> getDelegateMap(CaseInsensitiveMap<?, ?> ciMap) {
        try {
            Field f = ciMap.getClass().getDeclaredField("map");
            f.setAccessible(true);
            return (Map<?, ?>) f.get(ciMap);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError("Unable to access delegate map inside CaseInsensitiveMap", e);
        }
    }
}
