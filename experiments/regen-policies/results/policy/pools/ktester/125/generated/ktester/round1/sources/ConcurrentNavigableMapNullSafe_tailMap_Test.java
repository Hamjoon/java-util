package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_tailMap_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test – natural order, null key treated as greatest
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null key is considered the greatest key
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to help GC; not strictly required but keeps test hygiene
        map.clear();
    }

    // -----------------------------------------------------------------------
    static Stream<Arguments> nonNullInclusiveTrueProvider() {
        return Stream.of(Arguments.of("b", List.of("b", "c", null)));
    }

    // -----------------------------------------------------------------------
    static Stream<Arguments> nonNullInclusiveFalseProvider() {
        return Stream.of(Arguments.of("b", List.of("c", null)));
    }

    // -----------------------------------------------------------------------
    static Stream<Arguments> fromKeyBeyondRangeProvider() {
        return Stream.of(Arguments.of("z", true, Collections.emptyList()), Arguments.of("z", false, Collections.emptyList()));
    }

    // -----------------------------------------------------------------------
    static Stream<Arguments> incompatibleKeyProvider() {
        return Stream.of(Arguments.of(1, true), Arguments.of(1, false));
    }

    @Test
    void tailMapInclusiveTrue_excludesKeysBeforeFromKey() {
        // fromKey = "b", inclusive = true => should contain "b", "c", null
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b", true);
        assertEquals(3, tail.size());
        assertTrue(tail.containsKey("b"));
        assertTrue(tail.containsKey("c"));
        assertTrue(tail.containsKey(null));
        assertEquals(2, tail.get("b"));
        assertEquals(3, tail.get("c"));
        assertEquals(99, tail.get(null));
        // The original map must remain unchanged
        assertEquals(4, map.size());
    }

    @Test
    void tailMapInclusiveFalse_excludesFromKeyItself() {
        // fromKey = "b", inclusive = false => should contain "c" and null only
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b", false);
        assertEquals(2, tail.size());
        assertFalse(tail.containsKey("b"));
        assertTrue(tail.containsKey("c"));
        assertTrue(tail.containsKey(null));
        assertEquals(3, tail.get("c"));
        assertEquals(99, tail.get(null));
    }

    @Test
    void tailMapWithNullKey_inclusiveTrueReturnsOnlyNullKey() {
        // fromKey = null, inclusive = true => only the null key should be present
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(null, true);
        assertEquals(1, tail.size());
        assertTrue(tail.containsKey(null));
        assertEquals(99, tail.get(null));
    }

    @Test
    void tailMapWithNullKey_inclusiveFalseReturnsEmptyMap() {
        // fromKey = null, inclusive = false => no keys are greater than null
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(null, false);
        assertTrue(tail.isEmpty());
    }

    @Test
    void modificationsOnTailMapDoNotAffectOriginalWhenRemoved() {
        // Get a tail view and remove an element from it
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b", true);
        Integer removed = tail.remove("c");
        assertEquals(3, removed);
        // The removal is reflected in the original map because the view is live
        assertFalse(map.containsKey("c"));
        // a, b, null remain
        assertEquals(3, map.size());
        // Adding a new entry via the tail view should also appear in the original map
        tail.put("d", 4);
        assertTrue(map.containsKey("d"));
        assertEquals(4, map.get("d"));
    }

    @Test
    void tailMapReturnedIsInstanceOfConcurrentNavigableMapNullSafe() {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("a", true);
        assertTrue(tail instanceof ConcurrentNavigableMapNullSafe);
    }

    @ParameterizedTest(name = "tailMap({0}, true) should contain keys {1}")
    @MethodSource("nonNullInclusiveTrueProvider")
    void tailMap_nonNull_inclusiveTrue(String fromKey, List<String> expectedKeys) {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(fromKey, true);
        assertInstanceOf(ConcurrentNavigableMapNullSafe.class, tail);
        assertEquals(expectedKeys.size(), tail.size());
        expectedKeys.forEach(k -> assertTrue(tail.containsKey(k), () -> "Expected key '" + k + "' to be present"));
        // original map must stay unchanged
        assertEquals(4, map.size());
    }

    @ParameterizedTest(name = "tailMap({0}, false) should contain keys {1}")
    @MethodSource("nonNullInclusiveFalseProvider")
    void tailMap_nonNull_inclusiveFalse(String fromKey, List<String> expectedKeys) {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(fromKey, false);
        assertInstanceOf(ConcurrentNavigableMapNullSafe.class, tail);
        assertEquals(expectedKeys.size(), tail.size());
        expectedKeys.forEach(k -> assertTrue(tail.containsKey(k)));
        assertEquals(4, map.size());
    }

    // -----------------------------------------------------------------------
    @Test
    void tailMap_nullKey_inclusiveTrue() {
        // Only the null key (the greatest) should be present
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(null, true);
        assertInstanceOf(ConcurrentNavigableMapNullSafe.class, tail);
        assertEquals(1, tail.size());
        assertTrue(tail.containsKey(null));
        assertEquals(99, tail.get(null));
    }

    // -----------------------------------------------------------------------
    @Test
    void tailMap_nullKey_inclusiveFalse() {
        // No key is greater than null, so the view must be empty
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(null, false);
        assertTrue(tail.isEmpty());
    }

    @ParameterizedTest(name = "tailMap({0}, {1}) on max key should be empty")
    @MethodSource("fromKeyBeyondRangeProvider")
    void tailMap_fromKeyBeyondRange(String fromKey, boolean inclusive, List<String> expected) {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(fromKey, inclusive);
        assertTrue(tail.isEmpty());
        assertEquals(expected.size(), tail.size());
    }

    // -----------------------------------------------------------------------
    @Test
    void tailMap_onEmptyOriginalMap() {
        // now the map is empty
        map.clear();
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("any", true);
        assertTrue(tail.isEmpty());
        assertEquals(0, map.size());
    }

    // -----------------------------------------------------------------------
    @Test
    void tailMap_customComparator_reverseOrder_inclusiveTrue() throws Exception {
        // Build a map with reverse order comparator via reflection (constructor not public)
        Constructor<ConcurrentNavigableMapNullSafe> ctor = ConcurrentNavigableMapNullSafe.class.getDeclaredConstructor(ConcurrentNavigableMap.class, Comparator.class);
        ctor.setAccessible(true);
        Comparator<String> reverse = Collections.reverseOrder();
        ConcurrentNavigableMap<String, Integer> internal = new ConcurrentSkipListMap<>(reverse);
        @SuppressWarnings("unchecked")
        ConcurrentNavigableMapNullSafe<String, Integer> revMap = (ConcurrentNavigableMapNullSafe<String, Integer>) ctor.newInstance(internal, reverse);
        // Populate
        revMap.put("a", 1);
        revMap.put("b", 2);
        revMap.put("c", 3);
        // null still greatest (appears first in descending view)
        revMap.put(null, 99);
        // In reverse order, keys are ordered: null, c, b, a
        ConcurrentNavigableMap<String, Integer> tail = revMap.tailMap("b", true);
        // Expected keys: "b", "a", null (since null is considered greatest)
        List<String> expected = List.of("b", "a", null);
        assertEquals(expected.size(), tail.size());
        assertTrue(tail.keySet().containsAll(expected));
        assertInstanceOf(ConcurrentNavigableMapNullSafe.class, tail);
    }

    // -----------------------------------------------------------------------
    @Test
    void tailMap_absentFromKey_inclusiveTrue() {
        // "b.5" does not exist; the view should start at the next higher key "c"
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b.5", true);
        // "c" and null
        assertEquals(2, tail.size());
        assertTrue(tail.containsKey("c"));
        assertTrue(tail.containsKey(null));
        assertFalse(tail.containsKey("b"));
    }

    // -----------------------------------------------------------------------
    @Test
    void tailMap_absentFromKey_inclusiveFalse() {
        // Same result as inclusive true because "b.5" is not present
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b.5", false);
        // "c" and null
        assertEquals(2, tail.size());
        assertTrue(tail.containsKey("c"));
        assertTrue(tail.containsKey(null));
    }

    // -----------------------------------------------------------------------
    @Test
    void tailMap_modificationsReflectInOriginal() {
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b", true);
        // add via view
        tail.put("d", 4);
        assertTrue(map.containsKey("d"));
        assertEquals(4, map.get("d"));
        assertEquals(5, map.size());
        // removal via view
        Integer removed = tail.remove("c");
        assertEquals(3, removed);
        assertFalse(map.containsKey("c"));
        assertEquals(4, map.size());
    }

    @ParameterizedTest(name = "tailMap({0}, {1}) throws ClassCastException")
    @MethodSource("incompatibleKeyProvider")
    void tailMap_incompatibleKeyType(Object fromKey, boolean inclusive) {
        assertThrows(ClassCastException.class, () -> map.tailMap((String) fromKey, inclusive));
    }

    // -----------------------------------------------------------------------
    @Test
    void tailMap_nonComparableKeyThrows() {
        Object nonComparable = new Object();
        assertThrows(ClassCastException.class, () -> map.tailMap((String) nonComparable, true));
    }

    // -----------------------------------------------------------------------
    @Test
    void tailMap_internalMapNull_throwsNPE() throws Exception {
        // Create a normal instance then nullify its internalMap via reflection
        ConcurrentNavigableMapNullSafe<String, Integer> brokenMap = new ConcurrentNavigableMapNullSafe<>();
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        // sabotage
        internalMapField.set(brokenMap, null);
        assertThrows(NullPointerException.class, () -> brokenMap.tailMap("any", true));
    }
}
