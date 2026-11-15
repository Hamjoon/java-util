package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_subMap_2_Test {

    /**
     * The map under test.
     */
    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    /**
     * A sub‑map view used by the individual test methods.
     */
    private ConcurrentNavigableMap<String, Integer> subMap;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test and populate it with a deterministic set.
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        // Null key is allowed and is considered the *last* key in the ordering.
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no cross‑test contamination.
        map = null;
        subMap = null;
    }

    /**
     * Provides a set of arguments for the basic range tests.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>fromKey</li>
     *   <li>toKey</li>
     *   <li>expected keys that should appear in the sub‑map (in insertion order)</li>
     *   <li>whether an {@code IllegalArgumentException} is expected</li>
     * </ul>
     */
    private static Stream<Arguments> basicRangeProvider() {
        return Stream.of(// normal range: "b" (inclusive) … "d" (exclusive) → b, c
        Arguments.of("b", "d", List.of("b", "c"), false), // empty range because from == to
        Arguments.of("c", "c", List.of(), false), // from > to → exception
        Arguments.of("d", "b", List.of(), true), // from null (greatest) → empty view regardless of toKey
        // will throw IllegalArgumentException because null > "c"
        Arguments.of(null, "c", List.of(), true), // to null (exclusive) → all non‑null keys from from (inclusive)
        Arguments.of("a", null, List.of("a", "b", "c", "d"), false));
    }

    @Test
    void subMapContainsCorrectRange() {
        // given: a map with keys a‑d and a null key
        // when: we request a sub‑map from "b" (inclusive) to "d" (exclusive)
        subMap = map.subMap("b", "d");
        // then: the view should contain exactly "b" and "c"
        assertEquals(2, subMap.size());
        assertTrue(subMap.containsKey("b"));
        assertTrue(subMap.containsKey("c"));
        assertFalse(subMap.containsKey("a"));
        assertFalse(subMap.containsKey("d"));
        assertFalse(subMap.containsKey(null));
        // also verify the values are the same as in the original map
        assertEquals(map.get("b"), subMap.get("b"));
        assertEquals(map.get("c"), subMap.get("c"));
    }

    @Test
    void subMapIsLiveView_ModifySubMapReflectsInOriginal() {
        // view contains "b" and "c"
        subMap = map.subMap("b", "d");
        // modify via the sub‑map
        subMap.put("b", 20);
        subMap.put("c", 30);
        subMap.remove("b");
        // original map sees the same changes
        assertNull(map.get("b"));
        assertEquals(30, map.get("c"));
        // a, c, null remain
        assertEquals(3, map.size());
    }

    @Test
    void subMapIsLiveView_ModifyOriginalReflectsInSubMap() {
        // view contains "b" and "c"
        subMap = map.subMap("b", "d");
        // modify the original map
        map.put("b", 200);
        map.remove("c");
        // outside the view – should not appear
        map.put("e", 5);
        // sub‑map reflects the changes that are within its range
        assertEquals(200, subMap.get("b"));
        assertFalse(subMap.containsKey("c"));
        assertFalse(subMap.containsKey("e"));
    }

    @Test
    void subMapWithNullFromKeyYieldsEmptyMap() {
        // Null is considered the greatest key, therefore a sub‑map starting at null
        // (inclusive) and ending at any other key should be empty.
        // "z" is after null in natural order → empty view
        subMap = map.subMap(null, "z");
        assertTrue(subMap.isEmpty());
        assertEquals(0, subMap.size());
    }

    @Test
    void subMapWithNullToKeyExcludesNullKey() {
        // Sub‑map up to null (exclusive) should contain all non‑null keys.
        subMap = map.subMap("a", null);
        // All regular keys should be present, null key must be excluded.
        assertEquals(4, subMap.size());
        assertTrue(subMap.containsKey("a"));
        assertTrue(subMap.containsKey("b"));
        assertTrue(subMap.containsKey("c"));
        assertTrue(subMap.containsKey("d"));
        assertFalse(subMap.containsKey(null));
    }

    @Test
    void subMapWithFromKeyGreaterThanToKeyThrowsException() {
        // The underlying ConcurrentSkipListMap throws IllegalArgumentException
        // when fromKey > toKey.
        assertThrows(IllegalArgumentException.class, () -> map.subMap("d", "b"));
    }

    @Test
    void subMapWithEqualFromAndToKeyReturnsEmptyMap() {
        // When fromKey == toKey the range is empty (fromInclusive = true, toInclusive = false)
        subMap = map.subMap("b", "b");
        assertTrue(subMap.isEmpty());
        assertEquals(0, subMap.size());
    }

    @ParameterizedTest(name = "[{index}] subMap({0}, {1})")
    @MethodSource("basicRangeProvider")
    void subMapBasicRangeTests(Object fromKey, Object toKey, List<String> expectedKeys, boolean expectException) {
        if (expectException) {
            assertThrows(IllegalArgumentException.class, () -> map.subMap((String) fromKey, (String) toKey));
        } else {
            subMap = map.subMap((String) fromKey, (String) toKey);
            assertEquals(expectedKeys.size(), subMap.size(), () -> "Expected size does not match for range [" + fromKey + ", " + toKey + ")");
            // verify the exact set of keys
            assertTrue(subMap.keySet().containsAll(expectedKeys), () -> "Sub‑map missing expected keys for range [" + fromKey + ", " + toKey + ")");
            // also ensure no unexpected keys are present
            assertFalse(subMap.keySet().stream().anyMatch(k -> !expectedKeys.contains(k)), () -> "Sub‑map contains unexpected keys for range [" + fromKey + ", " + toKey + ")");
        }
    }

    @Test
    void subMapOnEmptyMapReturnsEmptyView() {
        // Create an empty map with Integer keys to verify the behaviour when the backing map is empty.
        ConcurrentNavigableMapNullSafe<Integer, String> emptyMap = new ConcurrentNavigableMapNullSafe<>();
        // Both bounds null – should return a view representing the whole (empty) map.
        ConcurrentNavigableMap<Integer, String> view = emptyMap.subMap(null, null);
        assertTrue(view.isEmpty(), "Sub‑map of an empty map should be empty");
        assertEquals(0, view.size());
    }

    @Test
    void subMapWithStringFromKeyOnIntegerMapThrowsClassCastException() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        // fromKey is a String – not comparable to Integer keys.
        assertThrows(ClassCastException.class, () -> intMap.subMap("stringKey", 2));
    }

    @Test
    void subMapWithStringToKeyOnIntegerMapThrowsClassCastException() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        // toKey is a String – not comparable to Integer keys.
        assertThrows(ClassCastException.class, () -> intMap.subMap(1, "stringKey"));
    }

    @Test
    void subMapWithBothStringBoundsOnIntegerMapThrowsClassCastException() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        assertThrows(ClassCastException.class, () -> intMap.subMap("a", "b"));
    }

    @Test
    void subMapWithNonComparableObjectsThrowsClassCastException() {
        ConcurrentNavigableMapNullSafe<Object, String> objMap = new ConcurrentNavigableMapNullSafe<>();
        objMap.put(new Object(), "first");
        objMap.put(new Object(), "second");
        // Objects do not implement Comparable – any subMap call should fail.
        assertThrows(ClassCastException.class, () -> objMap.subMap(new Object(), new Object()));
    }
}
