package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_tailMap_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test case
        map = new ConcurrentNavigableMapNullSafe<>();
        // Populate with a deterministic ordering (natural String ordering)
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        // null key is considered last in the ascending view
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference – not strictly required but keeps the pattern consistent
        map = null;
    }

    private static Stream<Arguments> inclusiveTrueProvider() {
        return Stream.of(Arguments.of("b", List.of("b", "c", "d", null)), // beyond highest key
        Arguments.of("e", List.of()), // null is masked to sentinel (last)
        Arguments.of(null, List.of(null)));
    }

    private static Stream<Arguments> exclusiveFalseProvider() {
        return Stream.of(Arguments.of("b", List.of("c", "d", null)), // beyond highest key
        Arguments.of("e", List.of()), // exclusive null → empty
        Arguments.of(null, List.of()));
    }

    private static Stream<Arguments> boundaryProvider() {
        return Stream.of(// inclusive = true
        Arguments.of("a", true, List.of("a", "b", "c", "d", null)), Arguments.of("d", true, List.of("d", null)), // inclusive = false
        Arguments.of("a", false, List.of("b", "c", "d", null)), Arguments.of("d", false, List.of(null)));
    }

    @Test
    void tailMap_Inclusive_FromKeyExists() {
        // fromKey = "b", inclusive = true → should contain b, c, d, null
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b", true);
        assertNotNull(tail);
        assertEquals(4, tail.size());
        assertTrue(tail.containsKey("b"));
        assertTrue(tail.containsKey("c"));
        assertTrue(tail.containsKey("d"));
        assertTrue(tail.containsKey(null));
        // Verify ordering – the first key should be "b"
        assertEquals("b", tail.firstKey());
        // Values should match the original map
        assertEquals(2, tail.get("b"));
        assertEquals(3, tail.get("c"));
        assertEquals(4, tail.get("d"));
        assertEquals(99, tail.get(null));
    }

    @Test
    void tailMap_Exclusive_FromKeyExists() {
        // fromKey = "b", inclusive = false → should contain c, d, null (but not b)
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b", false);
        assertNotNull(tail);
        assertEquals(3, tail.size());
        assertFalse(tail.containsKey("b"));
        assertTrue(tail.containsKey("c"));
        assertTrue(tail.containsKey("d"));
        assertTrue(tail.containsKey(null));
        // First key should be "c"
        assertEquals("c", tail.firstKey());
        // Values check
        assertEquals(3, tail.get("c"));
        assertEquals(4, tail.get("d"));
        assertEquals(99, tail.get(null));
    }

    @Test
    void tailMap_FromKeyDoesNotExist() {
        // fromKey = "bb" (between "b" and "c"), inclusive = true → should start at "c"
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("bb", true);
        assertNotNull(tail);
        assertEquals(3, tail.size());
        assertTrue(tail.containsKey("c"));
        assertTrue(tail.containsKey("d"));
        assertTrue(tail.containsKey(null));
        assertFalse(tail.containsKey("b"));
        assertEquals("c", tail.firstKey());
    }

    @Test
    void tailMap_FromKeyIsNull_Inclusive() {
        // fromKey = null, inclusive = true → should contain only the null key entry
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(null, true);
        assertNotNull(tail);
        assertEquals(1, tail.size());
        assertTrue(tail.containsKey(null));
        assertEquals(99, tail.get(null));
        // firstKey and lastKey both return null (the sentinel is exposed as null)
        assertNull(tail.firstKey());
        assertNull(tail.lastKey());
    }

    @Test
    void tailMap_FromKeyIsNull_Exclusive() {
        // fromKey = null, inclusive = false → should be empty because null is the last key
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(null, false);
        assertNotNull(tail);
        assertTrue(tail.isEmpty());
    }

    @Test
    void tailMap_ModificationsDoNotAffectOriginalMap() {
        // Obtain a tail map and modify it; original map should remain unchanged for those keys
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b", true);
        // add a new key beyond the original range
        tail.put("e", 5);
        // remove an existing key
        tail.remove("c");
        // Changes reflected in the tail view
        assertTrue(tail.containsKey("e"));
        assertFalse(tail.containsKey("c"));
        // Original map should also reflect the removal of "c" because the view is backed,
        // but the addition of "e" should be visible in the original map as well.
        // This behavior is defined by NavigableMap views – they are live.
        assertFalse(map.containsKey("c"));
        assertTrue(map.containsKey("e"));
        assertEquals(5, map.get("e"));
    }

    @Test
    void tailMap_ReturnsConcurrentNavigableMapNullSafeInstance() {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("c", true);
        assertTrue(tail instanceof ConcurrentNavigableMapNullSafe, "tailMap should wrap the internal view in a ConcurrentNavigableMapNullSafe");
    }

    // ---------- Inclusive = true ----------
    @ParameterizedTest(name = "{index}: tailMapInclusiveTrue fromKey={0}")
    @MethodSource("inclusiveTrueProvider")
    void tailMap_InclusiveTrue_VariousKeys(String fromKey, List<String> expectedKeys) {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(fromKey, true);
        assertNotNull(tail);
        assertEquals(expectedKeys.size(), tail.size(), "Tail map size does not match expectation for fromKey=" + fromKey);
        // Verify that the key set contains exactly the expected keys (order is natural)
        assertIterableEquals(expectedKeys, new ArrayList<>(tail.keySet()), "Key set mismatch for fromKey=" + fromKey);
    }

    // ---------- Inclusive = false ----------
    @ParameterizedTest(name = "{index}: tailMapExclusiveFalse fromKey={0}")
    @MethodSource("exclusiveFalseProvider")
    void tailMap_ExclusiveFalse_VariousKeys(String fromKey, List<String> expectedKeys) {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(fromKey, false);
        assertNotNull(tail);
        assertEquals(expectedKeys.size(), tail.size(), "Tail map size does not match expectation for fromKey=" + fromKey);
        assertIterableEquals(expectedKeys, new ArrayList<>(tail.keySet()), "Key set mismatch for fromKey=" + fromKey);
    }

    // ---------- Boundary key tests ----------
    @ParameterizedTest(name = "{index}: inclusive={1} fromKey={0}")
    @MethodSource("boundaryProvider")
    void tailMap_BoundaryKeys(String fromKey, boolean inclusive, List<String> expectedKeys) {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(fromKey, inclusive);
        assertNotNull(tail);
        assertIterableEquals(expectedKeys, new ArrayList<>(tail.keySet()));
    }

    // ---------- Empty map ----------
    @Test
    void tailMap_OnEmptyMap_ReturnsEmpty() {
        ConcurrentNavigableMapNullSafe<String, Integer> empty = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<String, Integer> tail = empty.tailMap("any", true);
        assertTrue(tail.isEmpty(), "Tail map of an empty map should be empty");
    }

    @Test
    void tailMap_OnEmptyMap_WithNullKey_Exclusive_ReturnsEmpty() {
        ConcurrentNavigableMapNullSafe<String, Integer> empty = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMap<String, Integer> tail = empty.tailMap(null, false);
        assertTrue(tail.isEmpty(), "Exclusive tail map on empty map should be empty");
    }

    // ---------- Custom comparator (reverse order) ----------
    @Test
    void tailMap_WithReverseComparator_Inclusive() {
        // Build a map with reverse order comparator
        Comparator<String> reverse = Collections.reverseOrder();
        ConcurrentNavigableMap<String, Integer> internal = new ConcurrentSkipListMap<>(reverse);
        ConcurrentNavigableMapNullSafe<String, Integer> reverseMap = new ConcurrentNavigableMapNullSafe<>(internal, reverse);
        reverseMap.put("a", 1);
        reverseMap.put("b", 2);
        reverseMap.put("c", 3);
        reverseMap.put("d", 4);
        // No null key – null handling is not relevant for reverse order test
        // Inclusive tailMap from "b" → keys >= "b" according to reverse comparator = "b", "a"
        ConcurrentNavigableMap<String, Integer> tail = reverseMap.tailMap("b", true);
        assertIterableEquals(List.of("b", "a"), new ArrayList<>(tail.keySet()));
        // Verify comparator is preserved
        assertEquals(reverse, tail.comparator(), "Comparator of the returned tail map should be the original reverse comparator");
    }

    @Test
    void tailMap_WithReverseComparator_Exclusive() {
        Comparator<String> reverse = Collections.reverseOrder();
        ConcurrentNavigableMap<String, Integer> internal = new ConcurrentSkipListMap<>(reverse);
        ConcurrentNavigableMapNullSafe<String, Integer> reverseMap = new ConcurrentNavigableMapNullSafe<>(internal, reverse);
        reverseMap.put("a", 1);
        reverseMap.put("b", 2);
        reverseMap.put("c", 3);
        reverseMap.put("d", 4);
        // Exclusive tailMap from "b" → keys > "b" in reverse order = "a"
        ConcurrentNavigableMap<String, Integer> tail = reverseMap.tailMap("b", false);
        assertIterableEquals(List.of("a"), new ArrayList<>(tail.keySet()));
        assertEquals(reverse, tail.comparator(), "Comparator of the returned tail map should be the original reverse comparator");
    }

    // ---------- Incompatible key type ----------
    @Test
    void tailMap_IncompatibleKeyType_ThrowsClassCastException() {
        // The underlying map expects String keys; passing an Integer should fail
        assertThrows(ClassCastException.class, () -> map.tailMap(1, true));
    }

    // ---------- Modification edge cases ----------
    @Test
    void tailMap_Exclusive_ModificationDoesNotIntroduceExcludedKey() {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b", false);
        // Put a new value for the excluded key "b" via the view
        tail.put("b", 20);
        // The view remains exclusive – it must not contain "b"
        assertFalse(tail.containsKey("b"));
        // Original map reflects the new value
        assertEquals(20, map.get("b"));
    }

    @Test
    void tailMap_Inclusive_ModificationReflectsInBothMaps() {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b", true);
        tail.put("b", 20);
        // Both view and original map see the updated entry
        assertTrue(tail.containsKey("b"));
        assertEquals(20, tail.get("b"));
        assertEquals(20, map.get("b"));
    }

    @Test
    void tailMap_ReturnedMapPreservesOriginalComparator() {
        // natural order (null)
        Comparator<String> original = map.comparator();
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("c", true);
        assertEquals(original, tail.comparator(), "The comparator of the returned tail map must be the same as the original map's comparator");
    }

    @Test
    void tailMap_ModificationsReflectInOriginalMap() {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b", true);
        // new key beyond original highest key
        tail.put("e", 5);
        // remove existing key
        tail.remove("c");
        // View reflects changes
        assertTrue(tail.containsKey("e"));
        assertFalse(tail.containsKey("c"));
        // Original map is a live view – changes are visible
        assertTrue(map.containsKey("e"));
        assertEquals(5, map.get("e"));
        assertFalse(map.containsKey("c"));
    }
}
