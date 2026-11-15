package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Method;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_cloneMapOfMaps_Test {

    /**
     * Sample data used by the tests.
     */
    private Map<String, Map<Integer, Double>> original;

    /**
     * Helper to build a fresh original map before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // outer map
        original = new LinkedHashMap<>();
        // first inner map
        Map<Integer, Double> inner1 = new LinkedHashMap<>();
        inner1.put(1, 1.1);
        inner1.put(2, 2.2);
        original.put("first", inner1);
        // second inner map
        Map<Integer, Double> inner2 = new LinkedHashMap<>();
        inner2.put(3, 3.3);
        original.put("second", inner2);
    }

    @AfterEach
    void teardownAfterEach() {
        // clear reference for GC – not strictly required but keeps the template complete
        original = null;
    }

    private static Stream<Arguments> immutableTrueNormalProvider() {
        // case 1 – normal map with entries
        Map<String, Map<Integer, String>> case1 = new LinkedHashMap<>();
        case1.put("A", new LinkedHashMap<>(Map.of(1, "one", 2, "two")));
        case1.put("B", new LinkedHashMap<>(Map.of(3, "three")));
        // case 2 – empty map
        Map<String, Map<Integer, String>> case2 = new LinkedHashMap<>();
        // case 3 – inner map null (should NPE)
        Map<String, Map<Integer, String>> case3 = new LinkedHashMap<>();
        case3.put("X", null);
        return Stream.of(Arguments.of("Immutable true – normal operation (non‑empty)", case1, true, null), Arguments.of("Immutable true – empty source map", case2, true, null), Arguments.of("Immutable true – inner map null triggers NPE", case3, true, NullPointerException.class));
    }

    private static Stream<Arguments> immutableFalseMutableProvider() {
        // case 1 – normal map with entries
        Map<String, Map<String, Integer>> case1 = new LinkedHashMap<>();
        case1.put("C", new LinkedHashMap<>(Map.of("x", 10, "y", 20)));
        case1.put("D", new LinkedHashMap<>(Map.of("z", 30)));
        // case 2 – empty map
        Map<String, Map<String, Integer>> case2 = new LinkedHashMap<>();
        // case 3 – source inner map is unmodifiable, but result must be mutable
        Map<String, Map<Integer, String>> inner = Collections.unmodifiableMap(Map.of(5, "five"));
        Map<String, Map<Integer, String>> case3 = new LinkedHashMap<>();
        case3.put("E", inner);
        // adapt types for generic method (use raw cast)
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Integer>> case3Cast = (Map) case3;
        return Stream.of(Arguments.of("Immutable false – mutable copy (non‑empty)", case1, false), Arguments.of("Immutable false – mutable copy (empty)", case2, false), Arguments.of("Immutable false – source inner unmodifiable, clone mutable", case3Cast, false));
    }

    private static Stream<Arguments> nullOriginalProvider() {
        return Stream.of(Arguments.of(null, true, NullPointerException.class), Arguments.of(null, false, NullPointerException.class));
    }

    private static Stream<Arguments> nullOuterKeyProvider() {
        Map<String, Map<Integer, String>> mutableCase = new LinkedHashMap<>();
        mutableCase.put(null, new LinkedHashMap<>(Map.of(1, "one")));
        Map<String, Map<Integer, String>> immutableCase = new LinkedHashMap<>();
        immutableCase.put(null, new LinkedHashMap<>(Map.of(1, "one")));
        return Stream.of(Arguments.of(mutableCase, false, false), Arguments.of(immutableCase, true, true));
    }

    private static Stream<Arguments> emptyInnerMapProvider() {
        Map<String, Map<Integer, String>> mutableCase = new LinkedHashMap<>();
        mutableCase.put("K", Collections.emptyMap());
        Map<String, Map<Integer, String>> immutableCase = new LinkedHashMap<>();
        immutableCase.put("K", Collections.emptyMap());
        return Stream.of(Arguments.of(mutableCase, false, false), Arguments.of(immutableCase, true, true));
    }

    private static Stream<Arguments> nullValuesInnerProvider() {
        Map<String, Map<Integer, String>> mutableCase = new LinkedHashMap<>();
        Map<Integer, String> innerMutable = new LinkedHashMap<>();
        innerMutable.put(1, null);
        innerMutable.put(2, "two");
        mutableCase.put("A", innerMutable);
        Map<String, Map<Integer, String>> immutableCase = new LinkedHashMap<>();
        Map<Integer, String> innerImmutable = new LinkedHashMap<>();
        innerImmutable.put(1, null);
        innerImmutable.put(2, "two");
        immutableCase.put("A", innerImmutable);
        return Stream.of(Arguments.of(mutableCase, false), Arguments.of(immutableCase, true));
    }

    private static Stream<Arguments> nullKeysInnerProvider() {
        Map<String, Map<Integer, String>> mutableCase = new LinkedHashMap<>();
        Map<Integer, String> innerMutable = new LinkedHashMap<>();
        innerMutable.put(null, "nullKey");
        innerMutable.put(2, "two");
        mutableCase.put("A", innerMutable);
        Map<String, Map<Integer, String>> immutableCase = new LinkedHashMap<>();
        Map<Integer, String> innerImmutable = new LinkedHashMap<>();
        innerImmutable.put(null, "nullKey");
        innerImmutable.put(2, "two");
        immutableCase.put("A", innerImmutable);
        return Stream.of(Arguments.of(mutableCase, false), Arguments.of(immutableCase, true));
    }

    private static Stream<Arguments> orderPreservationProvider() {
        Map<String, Map<Integer, String>> mutableCase = new LinkedHashMap<>();
        Map<Integer, String> first = new LinkedHashMap<>();
        first.put(1, "one");
        first.put(2, "two");
        Map<Integer, String> second = new LinkedHashMap<>();
        second.put(3, "three");
        second.put(4, "four");
        mutableCase.put("first", first);
        mutableCase.put("second", second);
        Map<String, Map<Integer, String>> immutableCase = new LinkedHashMap<>();
        // reuse same maps; they will be wrapped as unmodifiable
        immutableCase.putAll(mutableCase);
        return Stream.of(Arguments.of(mutableCase, false), Arguments.of(immutableCase, true));
    }

    @Test
    void cloneMapOfMaps_mutableCopy() {
        // GIVEN – original map is prepared in @BeforeEach
        // WHEN
        Map<String, Map<Integer, Double>> clone = MapUtilities.cloneMapOfMaps(original, false);
        // THEN – top‑level map is a new instance
        assertNotSame(original, clone, "The cloned map should be a different instance");
        assertEquals(original, clone, "The cloned map should be equal to the original");
        // THEN – each inner map is also a new instance
        for (String key : original.keySet()) {
            Map<Integer, Double> originalInner = original.get(key);
            Map<Integer, Double> clonedInner = clone.get(key);
            assertNotSame(originalInner, clonedInner, "Inner map for key '" + key + "' should be a different instance");
            assertEquals(originalInner, clonedInner, "Inner map for key '" + key + "' should be equal to the original");
        }
        // MODIFY original after cloning – cloned map must stay unchanged
        original.get("first").put(99, 9.9);
        assertFalse(clone.get("first").containsKey(99), "Changes to the original inner map must not affect the cloned inner map");
    }

    @Test
    void cloneMapOfMaps_immutableCopy() {
        // GIVEN – original map is prepared in @BeforeEach
        // WHEN
        Map<String, Map<Integer, Double>> clone = MapUtilities.cloneMapOfMaps(original, true);
        // THEN – the returned map is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> clone.put("newKey", new LinkedHashMap<>()), "Top‑level map should be unmodifiable when immutable flag is true");
        // THEN – each inner map is also unmodifiable
        for (Map<Integer, Double> inner : clone.values()) {
            assertThrows(UnsupportedOperationException.class, () -> inner.put(0, 0.0), "Inner map should be unmodifiable when immutable flag is true");
        }
        // THEN – contents are still equal to the original
        // (need to unwrap the unmodifiable wrapper for comparison)
        Map<String, Map<Integer, Double>> expected = new LinkedHashMap<>();
        for (Map.Entry<String, Map<Integer, Double>> e : original.entrySet()) {
            expected.put(e.getKey(), Collections.unmodifiableMap(e.getValue()));
        }
        assertEquals(expected, clone, "Immutable clone should contain the same entries as the original");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("immutableTrueNormalProvider")
    void testImmutableTrueNormalOperation(String description, Map<String, Map<Integer, String>> originalMap, boolean immutableFlag, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            // Expect an exception during cloning
            assertThrows(expectedException, () -> MapUtilities.cloneMapOfMaps(originalMap, immutableFlag), description);
            return;
        }
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(originalMap, immutableFlag);
        // outer map must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> clone.put("new", new LinkedHashMap<>()), "Outer map should be unmodifiable");
        // each inner map must be unmodifiable and contain the same entries
        for (Map.Entry<String, Map<Integer, String>> e : originalMap.entrySet()) {
            Map<Integer, String> innerOriginal = e.getValue();
            Map<Integer, String> innerClone = clone.get(e.getKey());
            assertThrows(UnsupportedOperationException.class, () -> innerClone.put(0, "zero"), "Inner map for key '" + e.getKey() + "' should be unmodifiable");
            assertEquals(innerOriginal, innerClone, "Inner map contents must match original for key '" + e.getKey() + "'");
        }
        // overall equality (wrappers are unmodifiable, but equals works)
        assertEquals(originalMap, clone, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("immutableFalseMutableProvider")
    void testImmutableFalseMutableCopy(String description, Map<String, Map<String, Integer>> originalMap, boolean immutableFlag) {
        Map<String, Map<String, Integer>> clone = MapUtilities.cloneMapOfMaps(originalMap, immutableFlag);
        // outer map must be mutable and a different instance
        assertNotSame(originalMap, clone, "Outer map should be a new mutable instance");
        assertEquals(originalMap, clone, "Outer map contents must match original");
        // each inner map must be a new mutable LinkedHashMap
        for (String key : originalMap.keySet()) {
            Map<String, Integer> innerOriginal = originalMap.get(key);
            Map<String, Integer> innerClone = clone.get(key);
            assertNotSame(innerOriginal, innerClone, "Inner map for key '" + key + "' should be a new mutable instance");
            assertEquals(innerOriginal, innerClone, "Inner map contents must match original for key '" + key + "'");
            // verify mutability
            innerClone.put("newKey", 999);
            assertTrue(innerClone.containsKey("newKey"), "Inner map should be mutable after cloning");
        }
        // verify that modifications to clone do not affect original
        clone.put("newOuter", new LinkedHashMap<>());
        assertFalse(originalMap.containsKey("newOuter"), "Modifying clone must not affect original outer map");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: immutable={1}")
    @MethodSource("nullOriginalProvider")
    void testNullOriginalMap(Map<String, Map<Integer, String>> originalMap, boolean immutableFlag, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> MapUtilities.cloneMapOfMaps(originalMap, immutableFlag), "Passing null original map should throw " + expectedException.getSimpleName());
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: immutable={1}")
    @MethodSource("nullOuterKeyProvider")
    void testNullKeyOuterMap(Map<String, Map<Integer, String>> originalMap, boolean immutableFlag, boolean expectUnmodifiable) {
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(originalMap, immutableFlag);
        // outer map must contain null key
        assertTrue(clone.containsKey(null), "Clone must retain null outer key");
        // inner map for null key
        Map<Integer, String> inner = clone.get(null);
        assertEquals(Map.of(1, "one"), inner, "Inner map contents must match original");
        if (expectUnmodifiable) {
            assertThrows(UnsupportedOperationException.class, () -> clone.put("another", new LinkedHashMap<>()), "Outer map should be unmodifiable");
            assertThrows(UnsupportedOperationException.class, () -> inner.put(2, "two"), "Inner map should be unmodifiable");
        } else {
            // mutable – ensure we can modify
            inner.put(2, "two");
            assertEquals("two", inner.get(2));
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: immutable={1}")
    @MethodSource("emptyInnerMapProvider")
    void testEmptyInnerMap(Map<String, Map<Integer, String>> originalMap, boolean immutableFlag, boolean expectSameEmptyInstance) {
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(originalMap, immutableFlag);
        assertTrue(clone.containsKey("K"), "Clone must contain key \"K\"");
        Map<Integer, String> inner = clone.get("K");
        if (immutableFlag) {
            // when immutable, Collections.unmodifiableMap(emptyMap) returns the same emptyMap instance
            assertSame(Collections.emptyMap(), inner, "Immutable empty inner map should be the same instance as Collections.emptyMap()");
            assertThrows(UnsupportedOperationException.class, () -> inner.put(1, "one"), "Inner map should be unmodifiable");
        } else {
            // mutable copy must be a new LinkedHashMap, not the shared emptyMap
            assertNotSame(Collections.emptyMap(), inner, "Mutable empty inner map should be a new instance");
            assertTrue(inner.isEmpty(), "Mutable inner map should be empty");
            // verify mutability
            inner.put(1, "one");
            assertEquals("one", inner.get(1));
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: immutable={1}")
    @MethodSource("nullValuesInnerProvider")
    void testNullValuesInInnerMap(Map<String, Map<Integer, String>> originalMap, boolean immutableFlag) {
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(originalMap, immutableFlag);
        Map<Integer, String> innerClone = clone.get("A");
        assertEquals(2, innerClone.size(), "Inner map should contain two entries");
        assertTrue(innerClone.containsKey(1) && innerClone.containsKey(2), "Keys 1 and 2 must be present");
        assertNull(innerClone.get(1), "Value for key 1 should be null");
        assertEquals("two", innerClone.get(2), "Value for key 2 should be \"two\"");
        if (immutableFlag) {
            assertThrows(UnsupportedOperationException.class, () -> innerClone.put(3, "three"), "Inner map should be unmodifiable");
        } else {
            innerClone.put(3, "three");
            assertEquals("three", innerClone.get(3));
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: immutable={1}")
    @MethodSource("nullKeysInnerProvider")
    void testNullKeysInInnerMap(Map<String, Map<Integer, String>> originalMap, boolean immutableFlag) {
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(originalMap, immutableFlag);
        Map<Integer, String> innerClone = clone.get("A");
        assertTrue(innerClone.containsKey(null), "Inner map must contain a null key");
        assertEquals("nullKey", innerClone.get(null), "Null key should map to \"nullKey\"");
        assertEquals("two", innerClone.get(2), "Key 2 should map to \"two\"");
        if (immutableFlag) {
            assertThrows(UnsupportedOperationException.class, () -> innerClone.put(3, "three"), "Inner map should be unmodifiable");
        } else {
            innerClone.put(3, "three");
            assertEquals("three", innerClone.get(3));
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: immutable={1}")
    @MethodSource("orderPreservationProvider")
    void testOrderPreservation(Map<String, Map<Integer, String>> originalMap, boolean immutableFlag) {
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(originalMap, immutableFlag);
        // Verify outer order
        List<String> outerKeys = new ArrayList<>(clone.keySet());
        assertEquals(List.of("first", "second"), outerKeys, "Outer map must preserve insertion order");
        // Verify inner order for each key
        List<Integer> firstInnerKeys = new ArrayList<>(clone.get("first").keySet());
        assertEquals(List.of(1, 2), firstInnerKeys, "Inner map for 'first' must preserve insertion order");
        List<Integer> secondInnerKeys = new ArrayList<>(clone.get("second").keySet());
        assertEquals(List.of(3, 4), secondInnerKeys, "Inner map for 'second' must preserve insertion order");
        if (immutableFlag) {
            // mutation attempts must fail
            assertThrows(UnsupportedOperationException.class, () -> clone.put("third", new LinkedHashMap<>()), "Outer map should be unmodifiable");
            assertThrows(UnsupportedOperationException.class, () -> clone.get("first").put(5, "five"), "Inner map should be unmodifiable");
        }
    }

    // -----------------------------------------------------------------
    @Test
    void testImmutableTrueModificationAttempts() {
        Map<String, Map<Integer, String>> originalMap = new LinkedHashMap<>();
        originalMap.put("X", new LinkedHashMap<>(Map.of(1, "one")));
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(originalMap, true);
        // outer modification
        assertThrows(UnsupportedOperationException.class, () -> clone.put("Y", new LinkedHashMap<>()), "Outer map must reject put when immutable");
        // inner modification
        assertThrows(UnsupportedOperationException.class, () -> clone.get("X").put(2, "two"), "Inner map must reject put when immutable");
    }
}
